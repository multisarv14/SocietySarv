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
        "society_code" to Translation("Society Login Code", "सोसायटी लॉगिन कोड", "संस्था लॉगिन कोड"),
        "unit_number" to Translation("Unit No.", "इकाई सं.", "सदनिका क्र."),
        "mobile_number" to Translation("Mobile Number", "मोबाइल नंबर", "मोबाईल क्रमांक"),
        "security_pin" to Translation("Enter 4-Digit Login PIN", "4-अंकीय लॉगिन पिन डालें", "४-अंकी लॉगिन पिन टाका"),
        "login_button" to Translation("Login", "लॉगिन", "प्रवेश करा"),
        "quick_demo_login" to Translation("Quick Demo Logins", "त्वरित डेमो लॉगिन", "चाचणी प्रोफाइल निवडा"),
        "register_new_society" to Translation("Register Your Society", "अपनी सोसाइटी पंजीकृत करें", "तुमची संस्था नोंदणी करा"),
        "forgot_pin" to Translation("Forgot PIN?", "पिन भूल गए?", "पिन विसरलात?"),
        "contact_chairman_note" to Translation(
            "Contact your Society Chairman for login details.",
            "लॉगिन विवरण के लिए अपने सोसायटी अध्यक्ष से संपर्क करें।",
            "लॉगिन तपशीलासाठी तुमच्या संस्थेच्या अध्यक्षांशी संपर्क साधा."
        ),
        "reset_pin_title" to Translation("Reset Login PIN", "लॉगिन पिन रीसेट करें", "लॉगिन पिन रीसेट करा"),
        "send_otp" to Translation("Send OTP", "ओटीपी भेजें", "ओटीपी पाठवा"),
        "resend_otp" to Translation("Resend OTP", "ओटीपी दोबारा भेजें", "ओटीपी पुन्हा पाठवा"),
        "verify_otp" to Translation("Verify OTP", "ओटीपी सत्यापित करें", "ओटीपी तपासा"),
        "enter_otp" to Translation("6-Digit OTP", "6-अंकीय ओटीपी", "६-अंकी ओटीपी"),
        "new_pin" to Translation("New 4-Digit PIN", "नया 4-अंकीय पिन", "नवीन ४-अंकी पिन"),
        "confirm_new_pin" to Translation("Confirm New PIN", "नया पिन दोबारा डालें", "नवीन पिन पुन्हा टाका"),
        "save_new_pin" to Translation("Save New PIN", "नया पिन सहेजें", "नवीन पिन जतन करा"),
        "registration_done" to Translation(
            "Registration Successful",
            "पंजीकरण सफल",
            "नोंदणी यशस्वी"
        ),
        "send_to_chairman_sms" to Translation("Send to Chairman (SMS)", "अध्यक्ष को भेजें (एसएमएस)", "अध्यक्षांना पाठवा (एसएमएस)"),
        "send_to_chairman_email" to Translation("Email to Chairman", "अध्यक्ष को ईमेल करें", "अध्यक्षांना ईमेल करा"),
        "society_profile" to Translation("Society Profile", "सोसायटी प्रोफ़ाइल", "संस्था प्रोफाइल"),
        "user_profile" to Translation("My Profile", "मेरी प्रोफ़ाइल", "माझे प्रोफाइल"),
        "edit_profile" to Translation("Edit", "संपादित करें", "संपादन"),
        // A member fills in blanks rather than editing the register, so their
        // button says so.
        "add_missing_details" to Translation(
            "Add Missing Details",
            "छूटी जानकारी जोड़ें",
            "राहिलेली माहिती भरा"
        ),
        "add_missing_only_note" to Translation(
            "You can fill in details the society does not have yet. Anything already on record is shown locked - ask the committee to correct it.",
            "आप वह जानकारी भर सकते हैं जो सोसायटी के पास नहीं है। जो पहले से दर्ज है वह लॉक दिखेगी - उसे बदलवाने के लिए समिति से संपर्क करें।",
            "संस्थेकडे नसलेली माहिती तुम्ही भरू शकता. आधीच नोंद असलेली माहिती लॉक दिसेल - ती बदलण्यासाठी समितीशी संपर्क साधा."
        ),
        "society_financial_overview" to Translation(
            "Society Financial Overview",
            "सोसायटी वित्तीय अवलोकन",
            "संस्थेचा वित्तीय आढावा"
        ),
        "my_receipts" to Translation("My Payment Receipts", "मेरी भुगतान रसीदें", "माझ्या पावत्या"),
        "save_profile" to Translation("Save Changes", "बदलाव सहेजें", "बदल जतन करा"),
        "chairman_only_edit" to Translation(
            "Only the Chairman can edit these details.",
            "केवल अध्यक्ष ही ये विवरण संपादित कर सकते हैं।",
            "फक्त अध्यक्षच ही माहिती बदलू शकतात."
        ),
        "add_emergency_contact" to Translation("Add Contact", "संपर्क जोड़ें", "संपर्क जोडा"),
        "share_receipt" to Translation("Share", "साझा करें", "शेअर"),
        "download_receipt" to Translation("Download", "डाउनलोड", "डाउनलोड"),
        "view_receipt" to Translation("View", "देखें", "पहा"),
        "total_pending" to Translation("Total Pending", "कुल बकाया", "एकूण थकबाकी"),
        "unit_number_label" to Translation("Unit Number", "इकाई संख्या", "सदनिका क्रमांक"),
        "pay" to Translation("Pay", "भुगतान", "भरणा"),
        "visitor_rejected" to Translation("Rejected", "अस्वीकृत", "नाकारले"),
        "receipt_list" to Translation("Receipts", "रसीदें", "पावत्या"),
        "payment_date" to Translation("Payment Date", "भुगतान तिथि", "भरणा दिनांक"),
        "upload_screenshot" to Translation("Upload Screenshot", "स्क्रीनशॉट अपलोड करें", "स्क्रीनशॉट अपलोड करा"),
        "replace_screenshot" to Translation("Replace Screenshot", "स्क्रीनशॉट बदलें", "स्क्रीनशॉट बदला"),
        "gate_activity_log" to Translation("Gate Activity Log", "गेट गतिविधि लॉग", "गेट नोंदवही"),
        "financial_year" to Translation("Financial Year", "वित्तीय वर्ष", "आर्थिक वर्ष"),
        "emergency_contacts_admin" to Translation(
            "Emergency Contacts",
            "आपातकालीन संपर्क",
            "तातडीचे संपर्क"
        ),
        "charge_setup" to Translation(
            "Charge Setup",
            "शुल्क संरचना",
            "शुल्क रचना"
        ),
        "remember_me" to Translation("Keep me signed in", "मुझे साइन इन रखें", "मला साइन इन ठेवा"),
        "select_unit" to Translation("Select Unit", "इकाई चुनें", "सदनिका निवडा"),
        "units_you_own" to Translation("units registered to you", "इकाइयां आपके नाम पर", "सदनिका तुमच्या नावावर"),
        "edit_expense" to Translation("Edit Expense", "व्यय संपादित करें", "खर्च संपादित करा"),
        "download_pdf" to Translation("Save PDF", "पीडीएफ सहेजें", "पीडीएफ जतन करा"),
        "share_pdf" to Translation("Share PDF", "पीडीएफ साझा करें", "पीडीएफ शेअर करा"),
        "bill_start_month" to Translation(
            "Bill Starting Month & Year",
            "बिल प्रारंभ माह एवं वर्ष",
            "बिल सुरू होण्याचा महिना व वर्ष"
        ),
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
        "nav_dashboard" to Translation("Home", "होम", "मुख्यपृष्ठ"),
        "nav_accounts" to Translation("Accounts", "खाता", "हिशोब"),
        "nav_maintenance" to Translation("Bills", "बिल", "बिले"),
        "nav_gate" to Translation("Security", "सुरक्षा", "सुरक्षा"),
        "view_only" to Translation("View only", "केवल देखें", "फक्त पाहण्यासाठी"),
        "filter_label" to Translation("Filter", "फ़िल्टर", "फिल्टर"),
        "filter_all" to Translation("All", "सभी", "सर्व"),
        "insufficient_balance" to Translation(
            "Insufficient balance. Please add funds before recording this expense.",
            "अपर्याप्त शेष राशि। यह व्यय दर्ज करने से पहले कृपया धनराशि जोड़ें।",
            "पुरेशी शिल्लक नाही. हा खर्च नोंदवण्यापूर्वी कृपया निधी जमा करा."
        ),
        "available_balance" to Translation("Available Balance", "उपलब्ध शेष", "उपलब्ध शिल्लक"),
        "fund_sinking" to Translation("Sinking Fund", "सिंकिंग फंड", "सिंकिंग फंड"),
        "fund_major_repair" to Translation("Major Repair Fund", "मुख्य मरम्मत निधि", "मोठ्या दुरुस्ती निधी"),
        "fund_csr" to Translation("CSR Fund", "सीएसआर फंड", "सीएसआर निधी"),
        "nav_residents" to Translation("Residents", "निवासी", "रहिवासी"),
        "tab_members" to Translation("Members", "सदस्य", "सभासद"),
        "tab_parking" to Translation("Parking", "पार्किंग", "पार्किंग"),
        "tab_society_staff" to Translation("Society Staff", "सोसायटी स्टाफ", "संस्था कर्मचारी"),
        "add_staff" to Translation("Add Staff", "स्टाफ जोड़ें", "कर्मचारी जोडा"),
        "parking_details" to Translation("Parking Details", "पार्किंग विवरण", "पार्किंग तपशील"),
        "parking_type" to Translation("Parking Type", "पार्किंग प्रकार", "पार्किंग प्रकार"),
        "parking_list" to Translation("Parking List", "पार्किंग सूची", "पार्किंग यादी"),
        "role_housekeeping" to Translation("Housekeeping", "हाउसकीपिंग", "स्वच्छता कर्मचारी"),
        "role_maintenance_staff" to Translation("Maintenance Staff", "रखरखाव कर्मचारी", "देखभाल कर्मचारी"),
        "role_accountant" to Translation("Accountant", "लेखाकार", "लेखापाल"),
        "role_manager" to Translation("Society Manager", "सोसायटी प्रबंधक", "संस्था व्यवस्थापक"),
        "nav_community" to Translation("Community", "समुदाय और शिकायतें", "समाज व तक्रारी"),
        "nav_resident_portal" to Translation("My Unit", "मेरी इकाई", "माझी सदनिका"),
        "gate_security" to Translation("Security", "सुरक्षा", "सुरक्षा"),
        "community_forum" to Translation("Community", "समुदाय", "समाज व तक्रारी"),
        "my_unit_portal" to Translation("My Unit", "मेरी इकाई", "माझी सदनिका"),

        // Financial KPIs & Maintenance Status Dashboard
        "kpi_total_collection" to Translation("Total Collection", "कुल संग्रह", "एकूण जमा रक्कम"),
        "kpi_operating_expenses" to Translation("Total Expenses", "कुल व्यय", "एकूण खर्च"),
        "kpi_net_treasury" to Translation("Net Treasury Balance", "शुद्ध कोष शेष", "शिल्लक निधी (बँक + रोख)"),
        "kpi_outstanding_dues" to Translation("Outstanding Dues", "बकाया राशि", "एकूण येणे / थकबाकी"),
        "kpi_bank_balance" to Translation("Bank Balance", "बैंक शेष", "बँक शिल्लक"),
        "kpi_cash_balance" to Translation("Cash in Hand", "नकद शेष", "रोख शिल्लक"),
        "maintenance_status_title" to Translation("Maintenance Status", "रखरखाव स्थिति", "देखभाल शुल्क स्थिती"),
        "monthly_dues" to Translation("Monthly Dues", "मासिक देय", "मासिक देय शुल्क"),
        "total_pending_amount" to Translation("Pending", "लंबित", "थकीत"),
        "collected_amount" to Translation("Collected", "जमा राशि", "जमा रक्कम"),
        "collection_progress" to Translation("Collection Rate", "संग्रह दर", "वसुली प्रमाण"),

        // Accounting & Balance Sheet
        "balance_sheet_title" to Translation("Balance Sheet", "तुलन पत्र", "ताळेबंद"),
        "income_expenditure" to Translation("Income & Expenditure", "आय और व्यय", "उत्पन्न व खर्च अहवाल"),
        "member_ledger" to Translation("Member Ledger", "सदस्य खाता", "सभासद खातेवही"),
        "expense_journal" to Translation("Expense Journal", "व्यय जर्नल", "खर्च नोंदवही"),
        "non_fee_revenue" to Translation("Non-Fee Revenue", "अन्य राजस्व", "इतर महसूल व पावत्या"),
        "financial_year" to Translation("Financial Year", "वित्तीय वर्ष", "आर्थिक वर्ष"),
        "audit_month" to Translation("Audit Month", "लेखापरीक्षा माह", "लेखापरीक्षण महिना"),
        "export_pdf_text" to Translation("Export Summary (PDF/Text)", "विवरण निर्यात करें", "विवरण डाऊनलोड करा"),
        "total_income" to Translation("Total Income", "कुल आय", "एकूण उत्पन्न"),
        "net_surplus" to Translation("Net Surplus", "शुद्ध अधिशेष", "निव्वळ शिल्लक / नफा"),
        "search_flat" to Translation("Search by Unit No. or Name", "इकाई संख्या या नाम से खोजें", "सदनिका क्रमांक किंवा नावाने शोधा"),

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
        "call_resident" to Translation("Call Unit", "इकाई पर कॉल करें", "सदनिकेवर फोन लावा"),
        "visitor_inside" to Translation("Inside Premises", "परिसर में उपस्थित", "परिसरात आहेत"),
        "visitor_exited" to Translation("Exited", "बाहर गए", "बाहेर पडले"),
        "purpose_delivery" to Translation("Delivery", "डिलीवरी", "डिलिव्हरी"),
        "purpose_guest" to Translation("Family / Guest", "परिवार / अतिथि", "पाहुणे / नातेवाईक"),
        "purpose_cab" to Translation("Cab / Taxi", "कैब / टैक्सी", "कॅब / टॅक्सी"),
        "purpose_service" to Translation("Service Staff", "सेवा कर्मचारी", "सेवा कर्मचारी"),
        "vehicle_number" to Translation("Vehicle No.", "वाहन क्रमांक", "वाहन क्रमांक"),

        // Community & Helpdesk
        "notice_board" to Translation("Notices", "सूचना", "सूचना"),
        "post_notice" to Translation("Post New Notice", "नई सूचना जारी करें", "नवीन सूचना प्रसिद्ध करा"),
        "bye_laws_title" to Translation("Society Bye-Laws & Rules", "सोसायटी के उपनियम एवं नियम", "संस्थेचे नियम व उपविधी"),
        "complaint_box" to Translation("Helpdesk & Complaints", "हेल्पडेस्क एवं शिकायतें", "तक्रार निवारण केंद्र"),
        "helpdesk_title" to Translation("Complaints", "शिकायतें", "तक्रारी"),
        "file_complaint" to Translation("New Complaint", "नई शिकायत", "नवीन तक्रार"),
        "vehicle_directory" to Translation("Parking", "पार्किंग", "पार्किंग"),
        "emergency_contacts" to Translation("Emergency Directory", "आपातकालीन निर्देशिका", "तातडीचे संपर्क क्रमांक"),
        "emergency_title" to Translation("Emergency Contacts", "आपातकालीन संपर्क", "तातडीचे संपर्क"),
        "status_open" to Translation("Open", "खुला / लंबित", "प्रलंबित"),
        "status_in_progress" to Translation("In Progress", "प्रगति पर", "कार्यवाही सुरू"),
        "status_resolved" to Translation("Resolved", "निस्तारित", "निवारण झाले"),

        // Registration Wizard
        "wizard_step1" to Translation("1. Society Details", "1. सोसायटी विवरण", "१. संस्थेची माहिती"),
        "wizard_step2" to Translation("2. Unit Details", "2. इकाई विवरण", "२. सदनिका माहिती"),
        "wizard_step3" to Translation("3. Floor Inventory", "3. मंजिल सूची", "३. मजला रचना"),
        "wizard_step4" to Translation("4. Office Bearers", "4. पदाधिकारी", "४. पदाधिकारी नियुक्ती"),
        "wizard_step5" to Translation("5. Confirmation", "5. पुष्टि एवं शुरुआत", "५. खात्री व सुरुवात"),
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
        "flat_type" to Translation("Unit Type", "इकाई का प्रकार", "सदनिका प्रकार"),
        "sqft" to Translation("Square Feet", "वर्ग फुट", "चौरस फूट"),
        "parking_slot" to Translation("Parking Slot", "पार्किंग स्लॉट", "पार्किंग जागा क्र."),
        "select_language" to Translation("Select Language", "भाषा चुनें", "भाषा निवडा")
    )
}
