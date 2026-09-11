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
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.entity.MaintenanceBill
import com.example.data.entity.Resident
import com.example.data.entity.SocietyInfo
import com.example.data.entity.chargesSubtotal
import com.example.data.entity.payableTotal
import com.example.data.entity.pendingAmount
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * A member's ledger as a real PDF: the society letterhead, the member's unit,
 * and one row per billed month - bill amount, late fee, total, paid and
 * pending - closing with the totals the committee reads out at a meeting.
 *
 * Rolls onto a second page when a member has more months than one page holds,
 * so a ledger is never silently cut short.
 */
object LedgerPdf {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 40f
    private const val ROW_HEIGHT = 18f

    private val TEAL = Color.rgb(2, 124, 117)
    private val INK = Color.rgb(17, 24, 39)
    private val MUTED = Color.rgb(107, 114, 128)
    private val RULE = Color.rgb(209, 213, 219)
    private val RED = Color.rgb(185, 28, 28)
    private val GREEN = Color.rgb(21, 128, 61)

    /** Writes the ledger and returns the file, or null if it could not be written. */
    fun generate(
        context: Context,
        member: Resident,
        bills: List<MaintenanceBill>,
        society: SocietyInfo?
    ): File? = runCatching {
        val document = PdfDocument()
        val ordered = bills.sortedBy { monthKey(it.billMonth) }

        var index = 0
        var pageNumber = 1
        do {
            val page = document.startPage(
                PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            )
            index = drawPage(context, page.canvas, member, ordered, society, index, pageNumber)
            document.finishPage(page)
            pageNumber++
        } while (index < ordered.size)

        val dir = File(context.cacheDir, "receipts").apply { mkdirs() }
        val safeUnit = member.flatNumber.ifBlank { "UNIT" }.replace(Regex("[^A-Za-z0-9-]"), "-")
        val file = File(dir, "SocietySarv-Ledger-$safeUnit.pdf")
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()
        file
    }.getOrNull()

    /** Generates the ledger and opens the system share sheet for it. */
    fun share(
        context: Context,
        member: Resident,
        bills: List<MaintenanceBill>,
        society: SocietyInfo?
    ): String? {
        val file = generate(context, member, bills, society)
            ?: return "Could not create the ledger PDF."
        return runCatching {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(
                    Intent.EXTRA_SUBJECT,
                    "${society?.name.orEmpty()} - Member Ledger ${member.flatNumber}".trim()
                )
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Month-wise maintenance ledger for ${member.flatNumber} (${member.ownerName})."
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(
                Intent.createChooser(intent, "Share ledger").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            null
        }.getOrElse { "No app available to share the ledger." }
    }

    // ------------------------------------------------------------- rendering

    /** Draws one page and returns the index of the first row that did not fit. */
    private fun drawPage(
        context: Context,
        canvas: Canvas,
        member: Resident,
        bills: List<MaintenanceBill>,
        society: SocietyInfo?,
        from: Int,
        pageNumber: Int
    ): Int {
        val title = paint(16f, INK, bold = true)
        val body = paint(9f, INK)
        val bodyBold = paint(9f, INK, bold = true)
        val small = paint(8.5f, MUTED)

        var y = MARGIN

        if (pageNumber == 1) {
            val logo = societyLogo(context, society)
            if (logo != null) {
                canvas.drawBitmap(
                    logo,
                    null,
                    Rect(MARGIN.toInt(), y.toInt(), (MARGIN + 58).toInt(), (y + 58).toInt()),
                    null
                )
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
                society?.state?.takeIf { it.isNotBlank() }
            ).joinToString(", ")
            if (where.isNotBlank()) canvas.drawText(where, textLeft, headerY, small)

            y = maxOf(headerY, y + 66)
            canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, rule(2f, TEAL))
            y += 26

            val centre = paint(13f, TEAL, bold = true).apply { textAlign = Paint.Align.CENTER }
            canvas.drawText("MEMBER LEDGER - MONTH WISE", PAGE_WIDTH / 2f, y, centre)
            y += 22

            canvas.drawText("Unit: ${member.flatNumber}", MARGIN, y, bodyBold)
            canvas.drawText(
                "Generated: ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())}",
                PAGE_WIDTH - MARGIN,
                y,
                bodyBold.rightAligned()
            )
            y += 15
            canvas.drawText("Member: ${member.ownerName}", MARGIN, y, body)
            y += 14
            if (member.contactNumber.isNotBlank()) {
                canvas.drawText("Contact: ${member.contactNumber}", MARGIN, y, body)
                y += 14
            }
            y += 8
        } else {
            canvas.drawText("Member Ledger - ${member.flatNumber} (continued)", MARGIN, y + 10, bodyBold)
            y += 30
        }

        // Column layout: Month | Bill | Late Fee | Total | Paid | Pending
        val columns = listOf(
            MARGIN + 6f to Paint.Align.LEFT,
            MARGIN + 160f to Paint.Align.RIGHT,
            MARGIN + 240f to Paint.Align.RIGHT,
            MARGIN + 330f to Paint.Align.RIGHT,
            MARGIN + 415f to Paint.Align.RIGHT,
            PAGE_WIDTH - MARGIN - 6f to Paint.Align.RIGHT
        )
        val headers = listOf("MONTH", "BILL (₹)", "LATE FEE (₹)", "TOTAL (₹)", "PAID (₹)", "PENDING (₹)")

        canvas.drawRect(MARGIN, y - 12, PAGE_WIDTH - MARGIN, y + 6, fill(Color.rgb(240, 247, 246)))
        headers.forEachIndexed { i, header ->
            val (x, align) = columns[i]
            canvas.drawText(header, x, y, paint(8f, TEAL, bold = true).apply { textAlign = align })
        }
        y += 20

        val bottomLimit = PAGE_HEIGHT - MARGIN - 120
        var index = from
        while (index < bills.size && y + ROW_HEIGHT < bottomLimit) {
            val bill = bills[index]
            val cells = listOf(
                bill.billMonth,
                money(bill.chargesSubtotal),
                money(bill.lateFeeAmount),
                money(bill.payableTotal),
                money(bill.amountPaid),
                money(bill.pendingAmount)
            )
            cells.forEachIndexed { i, cell ->
                val (x, align) = columns[i]
                val cellPaint = when {
                    i == 5 && bill.pendingAmount > 0.009 -> paint(9f, RED)
                    i == 5 -> paint(9f, GREEN)
                    else -> paint(9f, INK)
                }
                canvas.drawText(cell, x, y, cellPaint.apply { textAlign = align })
            }
            canvas.drawLine(MARGIN, y + 5, PAGE_WIDTH - MARGIN, y + 5, rule(0.5f, RULE))
            y += ROW_HEIGHT
            index++
        }

        // Totals go on the page that carries the last row.
        if (index >= bills.size) {
            y += 8
            canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, rule(1f, RULE))
            y += 16
            val totals = listOf(
                "TOTAL",
                money(bills.sumOf { it.chargesSubtotal }),
                money(bills.sumOf { it.lateFeeAmount }),
                money(bills.sumOf { it.payableTotal }),
                money(bills.sumOf { it.amountPaid }),
                money(bills.sumOf { it.pendingAmount })
            )
            totals.forEachIndexed { i, cell ->
                val (x, align) = columns[i]
                canvas.drawText(cell, x, y, paint(9.5f, INK, bold = true).apply { textAlign = align })
            }
            y += 26
            canvas.drawText(
                "Outstanding as on date: ₹${money(bills.sumOf { it.pendingAmount })}",
                MARGIN,
                y,
                paint(10f, if (bills.sumOf { it.pendingAmount } > 0.009) RED else GREEN, bold = true)
            )

            val signY = PAGE_HEIGHT - MARGIN - 60
            canvas.drawLine(PAGE_WIDTH - MARGIN - 160, signY, PAGE_WIDTH - MARGIN, signY, rule(1f, RULE))
            canvas.drawText("Hon. Treasurer / Secretary", PAGE_WIDTH - MARGIN, signY + 14, small.rightAligned())
        }

        val footer = paint(7.5f, MUTED).apply { textAlign = Paint.Align.CENTER }
        canvas.drawText(
            "Computer-generated ledger from SocietySarv - page $pageNumber",
            PAGE_WIDTH / 2f,
            PAGE_HEIGHT - MARGIN,
            footer
        )
        return index
    }

    private fun monthKey(billMonth: String): Long = runCatching {
        SimpleDateFormat("MMMM yyyy", Locale.ENGLISH).parse(billMonth)?.time ?: 0L
    }.getOrDefault(0L)

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
