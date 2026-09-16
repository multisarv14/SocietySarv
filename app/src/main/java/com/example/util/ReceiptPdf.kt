package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.entity.MaintenanceBill
import com.example.data.entity.SocietyInfo
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Renders a maintenance payment receipt as a real PDF, laid out the way a
 * housing society's receipt book is: society letterhead with its logo, the
 * receipt number and date, a particulars table that adds up to the bill total,
 * what was actually paid against it, and what is still outstanding.
 *
 * The file is written into `cacheDir/receipts` and handed to other apps through
 * the FileProvider declared in the manifest, so sharing never leaks a raw
 * file:// path.
 */
object ReceiptPdf {

    // A4 at 72dpi, the size every PDF viewer and printer agrees on.
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 40f

    private val TEAL = Color.rgb(2, 124, 117)
    private val INK = Color.rgb(17, 24, 39)
    private val MUTED = Color.rgb(107, 114, 128)
    private val RULE = Color.rgb(209, 213, 219)

    /** Writes the receipt and returns the file, or null if it could not be written. */
    fun generate(context: Context, bill: MaintenanceBill, society: SocietyInfo?): File? = runCatching {
        val document = PdfDocument()
        val page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create())
        drawReceipt(context, page.canvas, bill, society)
        document.finishPage(page)

        val dir = File(context.cacheDir, "receipts").apply { mkdirs() }
        val safeNumber = bill.receiptNumber.ifBlank { "RECEIPT" }.replace(Regex("[^A-Za-z0-9-]"), "-")
        val file = File(dir, "SocietySarv-Receipt-$safeNumber.pdf")
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()
        file
    }.getOrNull()

    /** Generates the receipt and opens the system share sheet for it. */
    fun share(context: Context, bill: MaintenanceBill, society: SocietyInfo?): String? {
        val file = generate(context, bill, society)
            ?: return "Could not create the receipt PDF."
        return runCatching {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(
                    Intent.EXTRA_SUBJECT,
                    "${society?.name.orEmpty()} - Payment Receipt ${bill.receiptNumber}".trim()
                )
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Receipt ${bill.receiptNumber} for ${bill.flatNumber} (${bill.billMonth})."
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(
                Intent.createChooser(intent, "Share receipt").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            null
        }.getOrElse { "No app available to share the receipt." }
    }

    /**
     * Saves the receipt into the phone's own Downloads folder and says where it
     * went, or explains why it could not.
     *
     * "Download" used to write into the app's cache directory and report the
     * file name. Nothing was wrong with the PDF, but no file manager could
     * reach it and Android could clear it at any time, so from the member's
     * side the receipt simply was not downloadable. This puts it where every
     * other download on the phone lands.
     */
    fun saveToDownloads(context: Context, bill: MaintenanceBill, society: SocietyInfo?): SaveResult {
        val source = generate(context, bill, society)
            ?: return SaveResult(false, "Could not create the receipt PDF.")
        val name = source.name

        // MediaStore owns Downloads from Android 10 onwards, and needs no
        // permission for a file the app puts there itself.
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            return runCatching {
                val values = android.content.ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, name)
                    put(MediaStore.Downloads.MIME_TYPE, PDF_MIME)
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: return SaveResult(false, "Could not open the Downloads folder.")
                resolver.openOutputStream(uri).use { out ->
                    requireNotNull(out).write(source.readBytes())
                }
                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                SaveResult(true, "Saved to Downloads as $name", uri)
            }.getOrElse { SaveResult(false, "Could not save to Downloads.") }
        }

        /*
         * Before Android 10 the public Downloads folder needs a storage
         * permission this app never asks for. Rather than bolt a permission
         * prompt onto a receipt, the file is opened straight away - the viewer
         * that handles it can save or print it from there.
         */
        return runCatching {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", source)
            SaveResult(true, "Opened the receipt - use Share to keep a copy.", uri).also { open(context, uri) }
        }.getOrElse { SaveResult(false, "Could not open the receipt.") }
    }

    /**
     * Opens the receipt in whatever PDF viewer the phone has.
     *
     * Returns null when it opened, or a message when nothing on the phone can
     * show a PDF.
     */
    fun view(context: Context, bill: MaintenanceBill, society: SocietyInfo?): String? {
        val file = generate(context, bill, society) ?: return "Could not create the receipt PDF."
        return runCatching {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            open(context, uri)
            null
        }.getOrElse { "No app on this phone can open a PDF. Use Share instead." }
    }

    private fun open(context: Context, uri: android.net.Uri) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, PDF_MIME)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /** Where a receipt ended up, and what to tell the member about it. */
    data class SaveResult(
        val isSaved: Boolean,
        val message: String,
        val uri: android.net.Uri? = null
    )

    private const val PDF_MIME = "application/pdf"

    // ------------------------------------------------------------- rendering

    private fun drawReceipt(context: Context, canvas: Canvas, bill: MaintenanceBill, society: SocietyInfo?) {
        val title = paint(16f, INK, bold = true)
        val heading = paint(13f, TEAL, bold = true)
        val body = paint(10f, INK)
        val bodyBold = paint(10f, INK, bold = true)
        val small = paint(8.5f, MUTED)

        var y = MARGIN

        // --- letterhead -----------------------------------------------------
        val logo = societyLogo(context, society)
        if (logo != null) {
            canvas.drawBitmap(logo, null, Rect(MARGIN.toInt(), y.toInt(), (MARGIN + 58).toInt(), (y + 58).toInt()), null)
        }
        val textLeft = MARGIN + if (logo != null) 70f else 0f

        canvas.drawText(society?.name.orEmpty().ifBlank { "Housing Society" }, textLeft, y + 16, title)
        var headerY = y + 32
        society?.registrationNumber?.takeIf { it.isNotBlank() }?.let {
            canvas.drawText("Reg. No: $it", textLeft, headerY, small); headerY += 12
        }
        val where = listOfNotNull(
            society?.address?.takeIf { it.isNotBlank() },
            society?.city?.takeIf { it.isNotBlank() },
            society?.state?.takeIf { it.isNotBlank() },
            society?.pinCode?.takeIf { it.isNotBlank() }
        ).joinToString(", ")
        if (where.isNotBlank()) { canvas.drawText(where, textLeft, headerY, small); headerY += 12 }
        val reach = listOfNotNull(
            society?.contactNumber?.takeIf { it.isNotBlank() }?.let { "Tel: $it" },
            society?.email?.takeIf { it.isNotBlank() }
        ).joinToString("  •  ")
        if (reach.isNotBlank()) { canvas.drawText(reach, textLeft, headerY, small); headerY += 12 }

        y = maxOf(headerY, y + 66)
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, rule(2f, TEAL))
        y += 26

        // --- receipt title & meta -------------------------------------------
        val centre = paint(13f, TEAL, bold = true).apply { textAlign = Paint.Align.CENTER }
        canvas.drawText("MAINTENANCE PAYMENT RECEIPT", PAGE_WIDTH / 2f, y, centre)
        y += 24

        val paidOn = bill.paidDate.ifBlank {
            SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
        }
        canvas.drawText("Receipt No: ${bill.receiptNumber.ifBlank { "-" }}", MARGIN, y, bodyBold)
        canvas.drawText("Date: $paidOn", PAGE_WIDTH - MARGIN, y, bodyBold.rightAligned())
        y += 22

        canvas.drawText("Received with thanks from:", MARGIN, y, small); y += 15
        canvas.drawText("${bill.residentName}  (Unit ${bill.flatNumber})", MARGIN, y, bodyBold); y += 15
        canvas.drawText("Towards maintenance for: ${bill.billMonth}", MARGIN, y, body); y += 22

        // --- particulars table ----------------------------------------------
        val amountX = PAGE_WIDTH - MARGIN
        canvas.drawRect(MARGIN, y - 13, PAGE_WIDTH - MARGIN, y + 6, fill(Color.rgb(240, 247, 246)))
        canvas.drawText("PARTICULARS", MARGIN + 8, y, paint(9f, TEAL, bold = true))
        canvas.drawText("AMOUNT (₹)", amountX - 8, y, paint(9f, TEAL, bold = true).rightAligned())
        y += 20

        val lines = buildList {
            add("Other / Common Maintenance" to bill.baseAmount)
            if (bill.waterCharges > 0) add("Water Charges" to bill.waterCharges)
            if (bill.sinkingFund > 0) add("Sinking Fund" to bill.sinkingFund)
            if (bill.repairFund > 0) add("Repair & Maintenance Fund" to bill.repairFund)
            if (bill.parkingCharges > 0) add("Parking Charges" to bill.parkingCharges)
            if (bill.nonOccupancyCharges > 0) add("Non-Occupancy Charges" to bill.nonOccupancyCharges)
            if (bill.lateFeeAmount > 0) add("Late Payment Fee" to bill.lateFeeAmount)
        }
        lines.forEach { (label, amount) ->
            canvas.drawText(label, MARGIN + 8, y, body)
            canvas.drawText(money(amount), amountX - 8, y, body.rightAligned())
            y += 16
        }

        y += 4
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, rule(1f, RULE)); y += 18
        canvas.drawText("Total Bill Amount", MARGIN + 8, y, bodyBold)
        canvas.drawText(money(bill.totalAmount), amountX - 8, y, bodyBold.rightAligned())
        y += 18
        canvas.drawText("Amount Paid", MARGIN + 8, y, bodyBold)
        canvas.drawText(money(bill.amountPaid), amountX - 8, y, bodyBold.rightAligned())
        y += 18

        val outstanding = (bill.totalAmount - bill.amountPaid).coerceAtLeast(0.0)
        canvas.drawText("Balance Outstanding", MARGIN + 8, y, bodyBold)
        canvas.drawText(
            money(outstanding),
            amountX - 8,
            y,
            paint(10f, if (outstanding > 0) Color.rgb(185, 28, 28) else Color.rgb(21, 128, 61), bold = true).rightAligned()
        )
        y += 10
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, rule(1f, RULE)); y += 24

        canvas.drawText("Amount in words: ${AmountWords.rupees(bill.amountPaid)}", MARGIN, y, body); y += 22

        // --- payment details -------------------------------------------------
        canvas.drawText("PAYMENT DETAILS", MARGIN, y, heading); y += 16
        canvas.drawText("Mode: ${bill.paymentMode?.labelEn ?: "-"}", MARGIN, y, body); y += 14
        canvas.drawText("Reference / Txn No: ${bill.transactionRef.ifBlank { "-" }}", MARGIN, y, body); y += 14
        canvas.drawText("Bill Due Date: ${bill.dueDate.ifBlank { "-" }}", MARGIN, y, body); y += 14
        canvas.drawText("Status: ${bill.status.name}", MARGIN, y, body)

        // --- signature & footer ----------------------------------------------
        val signY = PAGE_HEIGHT - MARGIN - 70
        canvas.drawLine(PAGE_WIDTH - MARGIN - 160, signY, PAGE_WIDTH - MARGIN, signY, rule(1f, RULE))
        canvas.drawText("Authorised Signatory", PAGE_WIDTH - MARGIN, signY + 14, small.rightAligned())
        canvas.drawText("Hon. Treasurer / Secretary", PAGE_WIDTH - MARGIN, signY + 26, small.rightAligned())

        val footer = paint(7.5f, MUTED).apply { textAlign = Paint.Align.CENTER }
        canvas.drawText(
            "This is a computer-generated receipt from SocietySarv and does not require a physical signature.",
            PAGE_WIDTH / 2f,
            PAGE_HEIGHT - MARGIN,
            footer
        )
    }

    /** The society's own logo when it uploaded one, else the SocietySarv mark. */
    private fun societyLogo(context: Context, society: SocietyInfo?): Bitmap? =
        ImageBlob.toBitmap(society?.logoBase64)
            ?: runCatching {
                BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher_foreground)
            }.getOrNull()

    private fun money(amount: Double) = "%,.2f".format(amount)

    private fun paint(size: Float, colour: Int, bold: Boolean = false) = Paint().apply {
        isAntiAlias = true
        textSize = size
        color = colour
        typeface = if (bold) {
            android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        } else {
            android.graphics.Typeface.DEFAULT
        }
    }

    private fun Paint.rightAligned(): Paint = Paint(this).apply { textAlign = Paint.Align.RIGHT }

    private fun rule(width: Float, colour: Int) = Paint().apply {
        isAntiAlias = true
        strokeWidth = width
        color = colour
    }

    private fun fill(colour: Int) = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = colour
    }
}

/** Rupee amounts spelled out, the way a receipt book writes them. */
object AmountWords {

    private val ONES = arrayOf(
        "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten",
        "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen",
        "Eighteen", "Nineteen"
    )
    private val TENS = arrayOf(
        "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"
    )

    fun rupees(amount: Double): String {
        val whole = amount.toLong()
        val paise = Math.round((amount - whole) * 100).toInt()
        val rupeePart = if (whole == 0L) "Zero" else indian(whole)
        return if (paise > 0) {
            "Rupees $rupeePart and ${indian(paise.toLong())} Paise Only"
        } else {
            "Rupees $rupeePart Only"
        }
    }

    /** Crore / lakh / thousand grouping, not the western million system. */
    private fun indian(value: Long): String {
        if (value == 0L) return ""
        val parts = mutableListOf<String>()
        var left = value

        val crore = left / 10_000_000; left %= 10_000_000
        val lakh = left / 100_000; left %= 100_000
        val thousand = left / 1_000; left %= 1_000
        val hundred = left / 100; left %= 100

        if (crore > 0) parts += "${indian(crore)} Crore"
        if (lakh > 0) parts += "${indian(lakh)} Lakh"
        if (thousand > 0) parts += "${indian(thousand)} Thousand"
        if (hundred > 0) parts += "${ONES[hundred.toInt()]} Hundred"
        if (left > 0) parts += underHundred(left.toInt())
        return parts.joinToString(" ")
    }

    private fun underHundred(value: Int): String = when {
        value < 20 -> ONES[value]
        value % 10 == 0 -> TENS[value / 10]
        else -> "${TENS[value / 10]} ${ONES[value % 10]}"
    }
}
