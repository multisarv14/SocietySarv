package com.example.data.localization

enum class AppLanguage(val code: String, val displayName: String, val nativeName: String) {
    ENGLISH("en", "English", "English"),
    HINDI("hi", "Hindi", "हिन्दी"),
    MARATHI("mr", "Marathi", "मराठी")
}

object AppStrings {
    fun get(key: String, language: AppLanguage): String {
        val entry = dictionary[key] ?: return key
        return when (language) {
            AppLanguage.ENGLISH -> entry.english
            AppLanguage.HINDI -> entry.hindi
            AppLanguage.MARATHI -> entry.marathi
        }
    }

    private data class Translation(val english: String, val hindi: String, val marathi: String)

    private val dictionary = mapOf(
        // App identity & Top Bar
        "app_title" to Translation("SocietySarv", "सोसायटीसर्व", "सोसायटीसर्व"),
        "app_tagline" to Translation(
            "Smart Housing Society Management & Accounting",
            "स्मार्ट हाउसिंग सोसाइटी प्रबंधन एवं लेखा प्रणाली",
            "स्मार्ट गृहनिर्माण संस्था व्यवस्थापन व लेखाप्रणाली"
        ),
        "switch_language" to Translation("Language", "भाषा", "भाषा"),
        "society_code" to Translation("Society Code", "सोसायटी कोड", "संस्था कोड"),
        "unit_number" to Translation("Flat / Unit No.", "फ्लैट / इकाई सं.", "फ्लॅट / सदनिका क्र."),
        "mobile_number" to Translation("Mobile Number", "मोबाइल नंबर", "मोबाईल क्रमांक"),
        "security_pin" to Translation("4-Digit PIN", "4-अंकीय पिन", "४-अंकी सुरक्षा पिन"),
        "login_button" to Translation("Secure Login", "सुरक्षित लॉगिन", "सुरक्षित प्रवेश करा"),
        "quick_demo_login" to Translation("Quick Demo Logins", "त्वरित डेमो लॉगिन", "चाचणी प्रोफाइल निवडा"),
        "register_new_society" to Translation("Register New Society", "नई सोसाइटी पंजीकृत करें", "नवीन संस्था नोंदणी करा"),
        "logout" to Translation("Logout", "लॉग आउट", "बाहेर पडा"),
        "switch_profile" to Translation("Switch Role", "भूमिका बदलें", "भूमिका बदला"),

        // Roles
        "role_chairman" to Translation("Chairman", "अध्यक्ष", "अध्यक्ष"),
        "role_secretary" to Translation("Secretary", "सचिव", "सचिव"),
        "role_treasurer" to Translation("Treasurer", "कोषाध्यक्ष", "खजिनदार"),
        "role_committee" to Translation("Committee Member", "समिति सदस्य", "समिती सदस्य"),
        "role_resident" to Translation("Resident Member", "निवासी सदस्य", "सभासद"),
        "role_tenant" to Translation("Tenant", "किरायेदार", "भाडेकरू"),
        "role_guard" to Translation("Security Guard", "सुरक्षा गार्ड", "सुरक्षा रक्षक"),

        // Navigation Tabs & Screen Headers
        "nav_dashboard" to Translation("Dashboard", "डैशबोर्ड", "मुख्य डॅशबोर्ड"),
        "nav_accounts" to Translation("Accounts & Ledger", "खाता और बहीखाता", "हिशोब व ताळेबंद"),
        "nav_maintenance" to Translation("Maintenance", "रखरखाव शुल्क", "देखभाल शुल्क"),
        "nav_gate" to Translation("Gate Security", "गेट सुरक्षा", "गेट सुरक्षा"),
        "nav_community" to Translation("Community", "समुदाय और शिकायतें", "समाज व तक्रारी"),
        "nav_resident_portal" to Translation("My Flat Portal", "मेरा फ्लैट", "माझा फ्लॅट"),
        "gate_security" to Translation("Gate Security", "गेट सुरक्षा", "गेट सुरक्षा"),
        "community_forum" to Translation("Community", "समुदाय", "समाज व तक्रारी"),
        "my_flat_portal" to Translation("My Flat Portal", "मेरा फ्लैट पोर्टल", "माझा फ्लॅट"),

        // Financial KPIs & Maintenance Status Dashboard
        "kpi_total_collection" to Translation("Total Collection", "कुल संग्रह", "एकूण जमा रक्कम"),
        "kpi_operating_expenses" to Translation("Total Expenses", "कुल व्यय", "एकूण खर्च"),
        "kpi_net_treasury" to Translation("Net Treasury Balance", "शुद्ध कोष शेष", "शिल्लक निधी (बँक + रोख)"),
        "kpi_outstanding_dues" to Translation("Outstanding Dues", "बकाया राशि", "एकूण येणे / थकबाकी"),
        "kpi_bank_balance" to Translation("Bank Balance", "बैंक शेष", "बँक शिल्लक"),
        "kpi_cash_balance" to Translation("Cash in Hand", "नकद शेष", "रोख शिल्लक"),
        "maintenance_status_title" to Translation("Maintenance Status", "रखरखाव स्थिति", "देखभाल शुल्क स्थिती"),
        "monthly_dues" to Translation("Monthly Dues", "मासिक देय", "मासिक देय शुल्क"),
        "total_pending_amount" to Translation("Total Pending Amount", "कुल लंबित राशि", "एकूण थकीत रक्कम"),
        "collected_amount" to Translation("Collected", "जमा राशि", "जमा रक्कम"),
        "collection_progress" to Translation("Collection Rate", "संग्रह दर", "वसुली प्रमाण"),

        // Accounting & Balance Sheet
        "balance_sheet_title" to Translation("Society General Balance Sheet", "सोसायटी का सामान्य तुलन पत्र", "संस्थेचा सर्वसाधारण ताळेबंद"),
        "income_expenditure" to Translation("Income & Expenditure", "आय और व्यय", "उत्पन्न व खर्च अहवाल"),
        "member_ledger" to Translation("Member Ledger Statements", "सदस्य खाता विवरण", "सभासद खातेवही व विवरण"),
        "expense_journal" to Translation("Expense Journal & Audit", "व्यय जर्नल", "खर्च नोंदवही"),
        "non_fee_revenue" to Translation("Non-Fee Revenue", "अन्य राजस्व", "इतर महसूल व पावत्या"),
        "financial_year" to Translation("Financial Year", "वित्तीय वर्ष", "आर्थिक वर्ष"),
        "audit_month" to Translation("Audit Month", "लेखापरीक्षा माह", "लेखापरीक्षण महिना"),
        "export_pdf_text" to Translation("Export Summary (PDF/Text)", "विवरण निर्यात करें", "विवरण डाऊनलोड करा"),
        "total_income" to Translation("Total Income", "कुल आय", "एकूण उत्पन्न"),
        "net_surplus" to Translation("Net Surplus", "शुद्ध अधिशेष", "निव्वळ शिल्लक / नफा"),
        "search_flat" to Translation("Search by Flat No. or Name", "फ्लैट नंबर या नाम से खोजें", "फ्लॅट किंवा नावाने शोधा"),

        // Operations & Maintenance
        "record_payment" to Translation("Record Payment", "भुगतान दर्ज करें", "पावती नोंदवा"),
        "record_expense" to Translation("Record Expense", "खर्च दर्ज करें", "खर्च नोंदवा"),
        "record_non_fee" to Translation("Record Non-Fee Revenue", "अन्य आय दर्ज करें", "इतर उत्पन्न नोंदवा"),
        "tariff_configuration" to Translation("Tariff Slabs & Sinking Fund", "टैरिफ स्लैब और सिंकिंग फंड", "दरपत्रक व निधी आकारणी"),
        "late_fee_rules" to Translation("Late Fee & Penalty Rules", "विलंब शुल्क व जुर्माना नियम", "व्याज व दंड नियम"),
        "member_directory" to Translation("Resident Directory", "निवासी निर्देशिका", "सभासद व रहिवासी यादी"),
        "add_member" to Translation("Add New Resident", "नया सदस्य जोड़ें", "नवीन सभासद जोडा"),
        "payment_mode" to Translation("Payment Mode", "भुगतान का प्रकार", "भरणा पद्धत"),
        "cash" to Translation("Cash", "नकद", "रोख"),
        "upi" to Translation("UPI", "यूपीआई", "यूपीआय"),
        "cheque" to Translation("Cheque", "चेक", "धनादेश"),
        "bank_transfer" to Translation("Bank Transfer (NEFT/IMPS)", "बैंक ट्रांसफर", "बँक ट्रान्सफर"),
        "transaction_ref" to Translation("Ref / Cheque No.", "संदर्भ / चेक क्र.", "संदर्भ / धनादेश क्र."),
        "receipt_generated" to Translation("Receipt Generated", "रसीद तैयार", "पावती तयार झाली"),
        "status_paid" to Translation("Paid", "भुगतान संपन्न", "भरणा पूर्ण"),
        "status_unpaid" to Translation("Unpaid", "अदत्त / बकाया", "थकीत"),
        "status_partial" to Translation("Partial", "आंशिक", "अंशतः"),

        // Expense Categories
        "cat_water_tanker" to Translation("Water Tanker", "पानी का टैंकर", "पाण्याचे टँकर"),
        "cat_lift_amc" to Translation("Lift AMC & Repairs", "लिफ्ट एएमसी एवं मरम्मत", "लिफ्ट देखभाल (AMC)"),
        "cat_security_salary" to Translation("Security Agency Salary", "सुरक्षा गार्ड वेतन", "सुरक्षा रक्षक मानधन"),
        "cat_gardening" to Translation("Gardening & Landscape", "बागवानी", "बागकाम व सुशोभीकरण"),
        "cat_housekeeping" to Translation("Housekeeping & Cleaning", "हाउसकीपिंग एवं सफाई", "स्वच्छता व कचरा व्यवस्थापन"),
        "cat_electricity" to Translation("Common Electricity Bill", "साझा बिजली बिल", "सामायिक वीज बिल"),
        "cat_repairs" to Translation("General Repairs & Painting", "सामान्य मरम्मत एवं पेंटिंग", "दुरुस्ती व रंगकाम"),
        "cat_miscellaneous" to Translation("Miscellaneous Audit / Office", "विविध / कार्यालयीन व्यय", "इतर / कार्यालयीन खर्च"),

        // Non-fee revenue
        "rev_clubhouse" to Translation("Clubhouse Booking", "क्लबहाउस बुकिंग", "क्लबहाऊस आरक्षण"),
        "rev_parking" to Translation("Visitor / Extra Parking", "अतिरिक्त पार्किंग शुल्क", "अतिरिक्त पार्किंग शुल्क"),
        "rev_tower" to Translation("Mobile Tower Rent", "मोबाइल टॉवर किराया", "मोबाईल टॉवर भाडे"),
        "rev_solar" to Translation("Solar Net Metering Credit", "सौर ऊर्जा क्रेडिट", "सौर ऊर्जा क्रेडिट"),
        "rev_hall" to Translation("Community Hall Rental", "कम्युनिटी हॉल किराया", "सभागृह भाडे"),
        "rev_fine" to Translation("Late Penalty / Rule Fine", "जुर्माना वसूली", "दंड वसुली"),

        // Gate & Security & Feed
        "gate_terminal_title" to Translation("Gate Security Terminal", "गेट सुरक्षा टर्मिनल", "गेट सुरक्षा केंद्र"),
        "gate_activity_feed" to Translation("Gate Activity Feed", "गेट गतिविधि फ़ीड", "गेट सुरक्षा हालचाली"),
        "recent_visitor_entries" to Translation("Recent Visitor Entries", "हाल के आगंतुक", "नुकतेच आलेले अभ्यागत"),
        "visitor_log" to Translation("Visitor Log & Passcodes", "आगंतुक लॉग एवं पासकोड", "अभ्यागत नोंदवही व पासकोड"),
        "check_in_visitor" to Translation("Check-in Visitor", "आगंतुक प्रविष्टि दर्ज करें", "नवीन अभ्यागत नोंद"),
        "check_out" to Translation("Mark Exit", "निकास दर्ज करें", "बाहेर पडले"),
        "verify_passcode" to Translation("Verify 6-Digit Passcode", "6-अंकीय पासकोड सत्यापित करें", "६-अंकी पासकोड तपासा"),
        "generate_passcode" to Translation("Generate Visitor Pass", "विज़िटर पास बनाएं", "अभ्यागत पासकोड तयार करा"),
        "call_resident" to Translation("Call Flat", "फ्लैट पर कॉल करें", "फ्लॅटवर फोन लावा"),
        "visitor_inside" to Translation("Inside Premises", "परिसर में उपस्थित", "परिसरात आहेत"),
        "visitor_exited" to Translation("Exited", "बाहर गए", "बाहेर पडले"),
        "purpose_delivery" to Translation("Delivery", "डिलीवरी", "डिलिव्हरी"),
        "purpose_guest" to Translation("Family / Guest", "परिवार / अतिथि", "पाहुणे / नातेवाईक"),
        "purpose_cab" to Translation("Cab / Taxi", "कैब / टैक्सी", "कॅब / टॅक्सी"),
        "purpose_service" to Translation("Service Staff", "सेवा कर्मचारी", "सेवा कर्मचारी"),
        "vehicle_number" to Translation("Vehicle No.", "वाहन क्रमांक", "वाहन क्रमांक"),

        // Community & Helpdesk
        "notice_board" to Translation("Notices & Circulars", "सूचना पट्ट एवं परिपत्र", "सूचना फलक व परिपत्रके"),
        "post_notice" to Translation("Post New Notice", "नई सूचना जारी करें", "नवीन सूचना प्रसिद्ध करा"),
        "bye_laws_title" to Translation("Society Bye-Laws & Rules", "सोसायटी के उपनियम एवं नियम", "संस्थेचे नियम व उपविधी"),
        "complaint_box" to Translation("Helpdesk & Complaints", "हेल्पडेस्क एवं शिकायतें", "तक्रार निवारण केंद्र"),
        "helpdesk_title" to Translation("Helpdesk & Complaints", "हेल्पडेस्क एवं शिकायतें", "तक्रार निवारण केंद्र"),
        "file_complaint" to Translation("Submit Maintenance Complaint", "रखरखाव शिकायत दर्ज करें", "देखभाल तक्रार दाखल करा"),
        "vehicle_directory" to Translation("Vehicle & Parking Registry", "वाहन एवं पार्किंग पंजी", "वाहन व पार्किंग यादी"),
        "emergency_contacts" to Translation("Emergency Directory", "आपातकालीन निर्देशिका", "तातडीचे संपर्क क्रमांक"),
        "emergency_title" to Translation("Emergency Directory", "आपातकालीन निर्देशिका", "तातडीचे संपर्क क्रमांक"),
        "status_open" to Translation("Open", "खुला / लंबित", "प्रलंबित"),
        "status_in_progress" to Translation("In Progress", "प्रगति पर", "कार्यवाही सुरू"),
        "status_resolved" to Translation("Resolved", "निस्तारित", "निवारण झाले"),

        // Registration Wizard
        "wizard_step1" to Translation("1. Society Details", "1. सोसायटी विवरण", "१. संस्थेची माहिती"),
        "wizard_step2" to Translation("2. Office Bearers", "2. पदाधिकारी", "२. पदाधिकारी नियुक्ती"),
        "wizard_step3" to Translation("3. Tariff & Slabs", "3. मासिक शुल्क स्लैब", "३. मासिक दरपत्रक"),
        "wizard_step4" to Translation("4. Confirmation", "4. पुष्टि एवं शुरुआत", "४. खात्री व सुरुवात"),
        "save_continue" to Translation("Save & Continue", "सहेजें और आगे बढ़ें", "जतन करा व पुढे चला"),
        "finish_setup" to Translation("Finish & Launch Society", "पंजीकरण पूरा करें", "नोंदणी पूर्ण करा"),

        // Common Actions & Form fields
        "amount" to Translation("Amount (₹)", "राशि (₹)", "रक्कम (₹)"),
        "date" to Translation("Date", "दिनांक", "तारीख"),
        "vendor_name" to Translation("Vendor / Payee Name", "विक्रेता / प्राप्तकर्ता का नाम", "विक्रेते / व्यक्तीचे नाव"),
        "notes" to Translation("Notes / Description", "विवरण / टिप्पणी", "तपशील / शेरा"),
        "submit" to Translation("Submit", "जमा करें", "सादर करा"),
        "cancel" to Translation("Cancel", "रद्द करें", "रद्द करा"),
        "close" to Translation("Close", "बंद करें", "बंद करा"),
        "save" to Translation("Save", "सहेजें", "जतन करा"),
        "delete" to Translation("Delete", "हटाएं", "हटवा"),
        "success" to Translation("Successful", "सफल", "यशस्वी"),
        "receipt_no" to Translation("Receipt No.", "रसीद क्र.", "पावती क्र."),
        "flat_type" to Translation("Flat Type", "फ्लैट का प्रकार", "सदनिका प्रकार"),
        "sqft" to Translation("Square Feet", "वर्ग फुट", "चौरस फूट"),
        "parking_slot" to Translation("Parking Slot", "पार्किंग स्लॉट", "पार्किंग जागा क्र."),
        "select_language" to Translation("Select Language", "भाषा चुनें", "भाषा निवडा")
    )
}
