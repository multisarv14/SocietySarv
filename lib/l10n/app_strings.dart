// Translations for the three app languages, ported verbatim from the Kotlin
// AppStrings dictionary. Missing keys fall back to the key itself.
// GENERATED from AppLanguage.kt - edit entries here directly from now on.

enum AppLanguage {
  english('en', 'English', 'English'),
  hindi('hi', 'Hindi', 'हिन्दी'),
  marathi('mr', 'Marathi', 'मराठी');

  const AppLanguage(this.code, this.displayName, this.nativeName);
  final String code;
  final String displayName;
  final String nativeName;
}

class _T {
  const _T(this.english, this.hindi, this.marathi);
  final String english;
  final String hindi;
  final String marathi;
}

class AppStrings {
  AppStrings._();

  static String get(String key, AppLanguage language) {
    final entry = _dictionary[key];
    if (entry == null) return key;
    switch (language) {
      case AppLanguage.english:
        return entry.english;
      case AppLanguage.hindi:
        return entry.hindi;
      case AppLanguage.marathi:
        return entry.marathi;
    }
  }

  static const Map<String, _T> _dictionary = {
    "app_title": _T("SocietySarv", "सोसायटीसर्व", "सोसायटीसर्व"),
    "app_tagline": _T(
      "Smart Housing Society Management & Accounting",
      "स्मार्ट हाउसिंग सोसाइटी प्रबंधन एवं लेखा प्रणाली",
      "स्मार्ट गृहनिर्माण संस्था व्यवस्थापन व लेखाप्रणाली",
    ),
    "switch_language": _T("Language", "भाषा", "भाषा"),
    "society_code": _T(
      "Society Login Code",
      "सोसायटी लॉगिन कोड",
      "संस्था लॉगिन कोड",
    ),
    "unit_number": _T("Unit No.", "इकाई सं.", "सदनिका क्र."),
    "mobile_number": _T("Mobile Number", "मोबाइल नंबर", "मोबाईल क्रमांक"),
    "security_pin": _T(
      "Enter 4-Digit Login PIN",
      "4-अंकीय लॉगिन पिन डालें",
      "४-अंकी लॉगिन पिन टाका",
    ),
    "login_button": _T("Login", "लॉगिन", "प्रवेश करा"),
    "quick_demo_login": _T(
      "Quick Demo Logins",
      "त्वरित डेमो लॉगिन",
      "चाचणी प्रोफाइल निवडा",
    ),
    "register_new_society": _T(
      "Register Your Society",
      "अपनी सोसाइटी पंजीकृत करें",
      "तुमची संस्था नोंदणी करा",
    ),
    "forgot_pin": _T("Forgot PIN?", "पिन भूल गए?", "पिन विसरलात?"),
    "contact_chairman_note": _T(
      "Contact your Society Chairman for login details.",
      "लॉगिन विवरण के लिए अपने सोसायटी अध्यक्ष से संपर्क करें।",
      "लॉगिन तपशीलासाठी तुमच्या संस्थेच्या अध्यक्षांशी संपर्क साधा.",
    ),
    "reset_pin_title": _T(
      "Reset Login PIN",
      "लॉगिन पिन रीसेट करें",
      "लॉगिन पिन रीसेट करा",
    ),
    "send_otp": _T("Send OTP", "ओटीपी भेजें", "ओटीपी पाठवा"),
    "resend_otp": _T("Resend OTP", "ओटीपी दोबारा भेजें", "ओटीपी पुन्हा पाठवा"),
    "verify_otp": _T("Verify OTP", "ओटीपी सत्यापित करें", "ओटीपी तपासा"),
    "enter_otp": _T("6-Digit OTP", "6-अंकीय ओटीपी", "६-अंकी ओटीपी"),
    "new_pin": _T("New 4-Digit PIN", "नया 4-अंकीय पिन", "नवीन ४-अंकी पिन"),
    "confirm_new_pin": _T(
      "Confirm New PIN",
      "नया पिन दोबारा डालें",
      "नवीन पिन पुन्हा टाका",
    ),
    "save_new_pin": _T("Save New PIN", "नया पिन सहेजें", "नवीन पिन जतन करा"),
    "registration_done": _T(
      "Registration Successful",
      "पंजीकरण सफल",
      "नोंदणी यशस्वी",
    ),
    "send_to_chairman_sms": _T(
      "Send to Chairman (SMS)",
      "अध्यक्ष को भेजें (एसएमएस)",
      "अध्यक्षांना पाठवा (एसएमएस)",
    ),
    "send_to_chairman_email": _T(
      "Email to Chairman",
      "अध्यक्ष को ईमेल करें",
      "अध्यक्षांना ईमेल करा",
    ),
    "society_profile": _T(
      "Society Profile",
      "सोसायटी प्रोफ़ाइल",
      "संस्था प्रोफाइल",
    ),
    "user_profile": _T("My Profile", "मेरी प्रोफ़ाइल", "माझे प्रोफाइल"),
    "edit_profile": _T("Edit", "संपादित करें", "संपादन"),
    "add_missing_details": _T(
      "Add Missing Details",
      "छूटी जानकारी जोड़ें",
      "राहिलेली माहिती भरा",
    ),
    "add_missing_only_note": _T(
      "You can fill in details the society does not have yet. Anything already on record is shown locked - ask the committee to correct it.",
      "आप वह जानकारी भर सकते हैं जो सोसायटी के पास नहीं है। जो पहले से दर्ज है वह लॉक दिखेगी - उसे बदलवाने के लिए समिति से संपर्क करें।",
      "संस्थेकडे नसलेली माहिती तुम्ही भरू शकता. आधीच नोंद असलेली माहिती लॉक दिसेल - ती बदलण्यासाठी समितीशी संपर्क साधा.",
    ),
    "society_financial_overview": _T(
      "Society Financial Overview",
      "सोसायटी वित्तीय अवलोकन",
      "संस्थेचा वित्तीय आढावा",
    ),
    "my_receipts": _T(
      "My Payment Receipts",
      "मेरी भुगतान रसीदें",
      "माझ्या पावत्या",
    ),
    "save_profile": _T("Save Changes", "बदलाव सहेजें", "बदल जतन करा"),
    "chairman_only_edit": _T(
      "Only the Chairman can edit these details.",
      "केवल अध्यक्ष ही ये विवरण संपादित कर सकते हैं।",
      "फक्त अध्यक्षच ही माहिती बदलू शकतात.",
    ),
    "add_emergency_contact": _T("Add Contact", "संपर्क जोड़ें", "संपर्क जोडा"),
    "share_receipt": _T("Share", "साझा करें", "शेअर"),
    "download_receipt": _T("Download", "डाउनलोड", "डाउनलोड"),
    "view_receipt": _T("View", "देखें", "पहा"),
    "total_pending": _T("Total Pending", "कुल बकाया", "एकूण थकबाकी"),
    "unit_number_label": _T("Unit Number", "इकाई संख्या", "सदनिका क्रमांक"),
    "pay": _T("Pay", "भुगतान", "भरणा"),
    "visitor_rejected": _T("Rejected", "अस्वीकृत", "नाकारले"),
    "receipt_list": _T("Receipts", "रसीदें", "पावत्या"),
    "payment_date": _T("Payment Date", "भुगतान तिथि", "भरणा दिनांक"),
    "upload_screenshot": _T(
      "Upload Screenshot",
      "स्क्रीनशॉट अपलोड करें",
      "स्क्रीनशॉट अपलोड करा",
    ),
    "replace_screenshot": _T(
      "Replace Screenshot",
      "स्क्रीनशॉट बदलें",
      "स्क्रीनशॉट बदला",
    ),
    "gate_activity_log": _T(
      "Gate Activity Log",
      "गेट गतिविधि लॉग",
      "गेट नोंदवही",
    ),
    "financial_year": _T("Financial Year", "वित्तीय वर्ष", "आर्थिक वर्ष"),
    "emergency_contacts_admin": _T(
      "Emergency Contacts",
      "आपातकालीन संपर्क",
      "तातडीचे संपर्क",
    ),
    "charge_setup": _T("Charge Setup", "शुल्क संरचना", "शुल्क रचना"),
    "remember_me": _T(
      "Keep me signed in",
      "मुझे साइन इन रखें",
      "मला साइन इन ठेवा",
    ),
    "select_unit": _T("Select Unit", "इकाई चुनें", "सदनिका निवडा"),
    "units_you_own": _T(
      "units registered to you",
      "इकाइयां आपके नाम पर",
      "सदनिका तुमच्या नावावर",
    ),
    "edit_expense": _T("Edit Expense", "व्यय संपादित करें", "खर्च संपादित करा"),
    "download_pdf": _T("Save PDF", "पीडीएफ सहेजें", "पीडीएफ जतन करा"),
    "share_pdf": _T("Share PDF", "पीडीएफ साझा करें", "पीडीएफ शेअर करा"),
    "bill_start_month": _T(
      "Bill Starting Month & Year",
      "बिल प्रारंभ माह एवं वर्ष",
      "बिल सुरू होण्याचा महिना व वर्ष",
    ),
    "logout": _T("Logout", "लॉग आउट", "बाहेर पडा"),
    "switch_profile": _T("Switch Role", "भूमिका बदलें", "भूमिका बदला"),
    "role_chairman": _T("Chairman", "अध्यक्ष", "अध्यक्ष"),
    "role_secretary": _T("Secretary", "सचिव", "सचिव"),
    "role_treasurer": _T("Treasurer", "कोषाध्यक्ष", "खजिनदार"),
    "role_committee": _T("Committee Member", "समिति सदस्य", "समिती सदस्य"),
    "role_resident": _T("Resident Member", "निवासी सदस्य", "सभासद"),
    "role_tenant": _T("Tenant", "किरायेदार", "भाडेकरू"),
    "role_guard": _T("Security Guard", "सुरक्षा गार्ड", "सुरक्षा रक्षक"),
    "nav_dashboard": _T("Home", "होम", "मुख्यपृष्ठ"),
    "nav_accounts": _T("Accounts", "खाता", "हिशोब"),
    "nav_maintenance": _T("Bills", "बिल", "बिले"),
    "nav_gate": _T("Security", "सुरक्षा", "सुरक्षा"),
    "view_only": _T("View only", "केवल देखें", "फक्त पाहण्यासाठी"),
    "filter_label": _T("Filter", "फ़िल्टर", "फिल्टर"),
    "filter_all": _T("All", "सभी", "सर्व"),
    "insufficient_balance": _T(
      "Insufficient balance. Please add funds before recording this expense.",
      "अपर्याप्त शेष राशि। यह व्यय दर्ज करने से पहले कृपया धनराशि जोड़ें।",
      "पुरेशी शिल्लक नाही. हा खर्च नोंदवण्यापूर्वी कृपया निधी जमा करा.",
    ),
    "available_balance": _T("Available Balance", "उपलब्ध शेष", "उपलब्ध शिल्लक"),
    "fund_sinking": _T("Sinking Fund", "सिंकिंग फंड", "सिंकिंग फंड"),
    "fund_major_repair": _T(
      "Major Repair Fund",
      "मुख्य मरम्मत निधि",
      "मोठ्या दुरुस्ती निधी",
    ),
    "fund_csr": _T("CSR Fund", "सीएसआर फंड", "सीएसआर निधी"),
    "nav_residents": _T("Residents", "निवासी", "रहिवासी"),
    "tab_members": _T("Members", "सदस्य", "सभासद"),
    "tab_parking": _T("Parking", "पार्किंग", "पार्किंग"),
    "tab_society_staff": _T(
      "Society Staff",
      "सोसायटी स्टाफ",
      "संस्था कर्मचारी",
    ),
    "add_staff": _T("Add Staff", "स्टाफ जोड़ें", "कर्मचारी जोडा"),
    "parking_details": _T(
      "Parking Details",
      "पार्किंग विवरण",
      "पार्किंग तपशील",
    ),
    "parking_type": _T("Parking Type", "पार्किंग प्रकार", "पार्किंग प्रकार"),
    "parking_list": _T("Parking List", "पार्किंग सूची", "पार्किंग यादी"),
    "role_housekeeping": _T("Housekeeping", "हाउसकीपिंग", "स्वच्छता कर्मचारी"),
    "role_maintenance_staff": _T(
      "Maintenance Staff",
      "रखरखाव कर्मचारी",
      "देखभाल कर्मचारी",
    ),
    "role_accountant": _T("Accountant", "लेखाकार", "लेखापाल"),
    "role_manager": _T(
      "Society Manager",
      "सोसायटी प्रबंधक",
      "संस्था व्यवस्थापक",
    ),
    "nav_community": _T("Community", "समुदाय और शिकायतें", "समाज व तक्रारी"),
    "nav_resident_portal": _T("My Unit", "मेरी इकाई", "माझी सदनिका"),
    "gate_security": _T("Security", "सुरक्षा", "सुरक्षा"),
    "community_forum": _T("Community", "समुदाय", "समाज व तक्रारी"),
    "my_unit_portal": _T("My Unit", "मेरी इकाई", "माझी सदनिका"),
    "kpi_total_collection": _T(
      "Total Collection",
      "कुल संग्रह",
      "एकूण जमा रक्कम",
    ),
    "kpi_operating_expenses": _T("Total Expenses", "कुल व्यय", "एकूण खर्च"),
    "kpi_net_treasury": _T(
      "Net Treasury Balance",
      "शुद्ध कोष शेष",
      "शिल्लक निधी (बँक + रोख)",
    ),
    "kpi_outstanding_dues": _T(
      "Outstanding Dues",
      "बकाया राशि",
      "एकूण येणे / थकबाकी",
    ),
    "kpi_bank_balance": _T("Bank Balance", "बैंक शेष", "बँक शिल्लक"),
    "kpi_cash_balance": _T("Cash in Hand", "नकद शेष", "रोख शिल्लक"),
    "maintenance_status_title": _T(
      "Maintenance Status",
      "रखरखाव स्थिति",
      "देखभाल शुल्क स्थिती",
    ),
    "monthly_dues": _T("Monthly Dues", "मासिक देय", "मासिक देय शुल्क"),
    "total_pending_amount": _T("Pending", "लंबित", "थकीत"),
    "collected_amount": _T("Collected", "जमा राशि", "जमा रक्कम"),
    "collection_progress": _T("Collection Rate", "संग्रह दर", "वसुली प्रमाण"),
    "balance_sheet_title": _T("Balance Sheet", "तुलन पत्र", "ताळेबंद"),
    "income_expenditure": _T(
      "Income & Expenditure",
      "आय और व्यय",
      "उत्पन्न व खर्च अहवाल",
    ),
    "member_ledger": _T("Member Ledger", "सदस्य खाता", "सभासद खातेवही"),
    "expense_journal": _T("Expense Journal", "व्यय जर्नल", "खर्च नोंदवही"),
    "non_fee_revenue": _T(
      "Non-Fee Revenue",
      "अन्य राजस्व",
      "इतर महसूल व पावत्या",
    ),
    "audit_month": _T("Audit Month", "लेखापरीक्षा माह", "लेखापरीक्षण महिना"),
    "export_pdf_text": _T(
      "Export Summary (PDF/Text)",
      "विवरण निर्यात करें",
      "विवरण डाऊनलोड करा",
    ),
    "total_income": _T("Total Income", "कुल आय", "एकूण उत्पन्न"),
    "net_surplus": _T("Net Surplus", "शुद्ध अधिशेष", "निव्वळ शिल्लक / नफा"),
    "search_flat": _T(
      "Search by Unit No. or Name",
      "इकाई संख्या या नाम से खोजें",
      "सदनिका क्रमांक किंवा नावाने शोधा",
    ),
    "record_payment": _T("Record Payment", "भुगतान दर्ज करें", "पावती नोंदवा"),
    "record_expense": _T("Record Expense", "खर्च दर्ज करें", "खर्च नोंदवा"),
    "record_non_fee": _T(
      "Record Non-Fee Revenue",
      "अन्य आय दर्ज करें",
      "इतर उत्पन्न नोंदवा",
    ),
    "tariff_configuration": _T(
      "Tariff Slabs & Sinking Fund",
      "टैरिफ स्लैब और सिंकिंग फंड",
      "दरपत्रक व निधी आकारणी",
    ),
    "late_fee_rules": _T(
      "Late Fee & Penalty Rules",
      "विलंब शुल्क व जुर्माना नियम",
      "व्याज व दंड नियम",
    ),
    "member_directory": _T(
      "Resident Directory",
      "निवासी निर्देशिका",
      "सभासद व रहिवासी यादी",
    ),
    "add_member": _T("Add New Resident", "नया सदस्य जोड़ें", "नवीन सभासद जोडा"),
    "payment_mode": _T("Payment Mode", "भुगतान का प्रकार", "भरणा पद्धत"),
    "cash": _T("Cash", "नकद", "रोख"),
    "upi": _T("UPI", "यूपीआई", "यूपीआय"),
    "cheque": _T("Cheque", "चेक", "धनादेश"),
    "bank_transfer": _T(
      "Bank Transfer (NEFT/IMPS)",
      "बैंक ट्रांसफर",
      "बँक ट्रान्सफर",
    ),
    "transaction_ref": _T(
      "Ref / Cheque No.",
      "संदर्भ / चेक क्र.",
      "संदर्भ / धनादेश क्र.",
    ),
    "receipt_generated": _T(
      "Receipt Generated",
      "रसीद तैयार",
      "पावती तयार झाली",
    ),
    "status_paid": _T("Paid", "भुगतान संपन्न", "भरणा पूर्ण"),
    "status_unpaid": _T("Unpaid", "अदत्त / बकाया", "थकीत"),
    "status_partial": _T("Partial", "आंशिक", "अंशतः"),
    "cat_water_tanker": _T("Water Tanker", "पानी का टैंकर", "पाण्याचे टँकर"),
    "cat_lift_amc": _T(
      "Lift AMC & Repairs",
      "लिफ्ट एएमसी एवं मरम्मत",
      "लिफ्ट देखभाल (AMC)",
    ),
    "cat_security_salary": _T(
      "Security Agency Salary",
      "सुरक्षा गार्ड वेतन",
      "सुरक्षा रक्षक मानधन",
    ),
    "cat_gardening": _T(
      "Gardening & Landscape",
      "बागवानी",
      "बागकाम व सुशोभीकरण",
    ),
    "cat_housekeeping": _T(
      "Housekeeping & Cleaning",
      "हाउसकीपिंग एवं सफाई",
      "स्वच्छता व कचरा व्यवस्थापन",
    ),
    "cat_electricity": _T(
      "Common Electricity Bill",
      "साझा बिजली बिल",
      "सामायिक वीज बिल",
    ),
    "cat_repairs": _T(
      "General Repairs & Painting",
      "सामान्य मरम्मत एवं पेंटिंग",
      "दुरुस्ती व रंगकाम",
    ),
    "cat_miscellaneous": _T(
      "Miscellaneous Audit / Office",
      "विविध / कार्यालयीन व्यय",
      "इतर / कार्यालयीन खर्च",
    ),
    "rev_clubhouse": _T(
      "Clubhouse Booking",
      "क्लबहाउस बुकिंग",
      "क्लबहाऊस आरक्षण",
    ),
    "rev_parking": _T(
      "Visitor / Extra Parking",
      "अतिरिक्त पार्किंग शुल्क",
      "अतिरिक्त पार्किंग शुल्क",
    ),
    "rev_tower": _T(
      "Mobile Tower Rent",
      "मोबाइल टॉवर किराया",
      "मोबाईल टॉवर भाडे",
    ),
    "rev_solar": _T(
      "Solar Net Metering Credit",
      "सौर ऊर्जा क्रेडिट",
      "सौर ऊर्जा क्रेडिट",
    ),
    "rev_hall": _T(
      "Community Hall Rental",
      "कम्युनिटी हॉल किराया",
      "सभागृह भाडे",
    ),
    "rev_fine": _T("Late Penalty / Rule Fine", "जुर्माना वसूली", "दंड वसुली"),
    "gate_terminal_title": _T(
      "Gate Security Terminal",
      "गेट सुरक्षा टर्मिनल",
      "गेट सुरक्षा केंद्र",
    ),
    "gate_activity_feed": _T(
      "Gate Activity Feed",
      "गेट गतिविधि फ़ीड",
      "गेट सुरक्षा हालचाली",
    ),
    "recent_visitor_entries": _T(
      "Recent Visitor Entries",
      "हाल के आगंतुक",
      "नुकतेच आलेले अभ्यागत",
    ),
    "visitor_log": _T(
      "Visitor Log & Passcodes",
      "आगंतुक लॉग एवं पासकोड",
      "अभ्यागत नोंदवही व पासकोड",
    ),
    "check_in_visitor": _T(
      "Check-in Visitor",
      "आगंतुक प्रविष्टि दर्ज करें",
      "नवीन अभ्यागत नोंद",
    ),
    "check_out": _T("Mark Exit", "निकास दर्ज करें", "बाहेर पडले"),
    "verify_passcode": _T(
      "Verify 6-Digit Passcode",
      "6-अंकीय पासकोड सत्यापित करें",
      "६-अंकी पासकोड तपासा",
    ),
    "generate_passcode": _T(
      "Generate Visitor Pass",
      "विज़िटर पास बनाएं",
      "अभ्यागत पासकोड तयार करा",
    ),
    "call_resident": _T("Call Unit", "इकाई पर कॉल करें", "सदनिकेवर फोन लावा"),
    "visitor_inside": _T(
      "Inside Premises",
      "परिसर में उपस्थित",
      "परिसरात आहेत",
    ),
    "visitor_exited": _T("Exited", "बाहर गए", "बाहेर पडले"),
    "purpose_delivery": _T("Delivery", "डिलीवरी", "डिलिव्हरी"),
    "purpose_guest": _T(
      "Family / Guest",
      "परिवार / अतिथि",
      "पाहुणे / नातेवाईक",
    ),
    "purpose_cab": _T("Cab / Taxi", "कैब / टैक्सी", "कॅब / टॅक्सी"),
    "purpose_service": _T("Service Staff", "सेवा कर्मचारी", "सेवा कर्मचारी"),
    "vehicle_number": _T("Vehicle No.", "वाहन क्रमांक", "वाहन क्रमांक"),
    "notice_board": _T("Notices", "सूचना", "सूचना"),
    "post_notice": _T(
      "Post New Notice",
      "नई सूचना जारी करें",
      "नवीन सूचना प्रसिद्ध करा",
    ),
    "bye_laws_title": _T(
      "Society Bye-Laws & Rules",
      "सोसायटी के उपनियम एवं नियम",
      "संस्थेचे नियम व उपविधी",
    ),
    "complaint_box": _T(
      "Helpdesk & Complaints",
      "हेल्पडेस्क एवं शिकायतें",
      "तक्रार निवारण केंद्र",
    ),
    "helpdesk_title": _T("Complaints", "शिकायतें", "तक्रारी"),
    "file_complaint": _T("New Complaint", "नई शिकायत", "नवीन तक्रार"),
    "vehicle_directory": _T("Parking", "पार्किंग", "पार्किंग"),
    "emergency_contacts": _T(
      "Emergency Directory",
      "आपातकालीन निर्देशिका",
      "तातडीचे संपर्क क्रमांक",
    ),
    "emergency_title": _T(
      "Emergency Contacts",
      "आपातकालीन संपर्क",
      "तातडीचे संपर्क",
    ),
    "status_open": _T("Open", "खुला / लंबित", "प्रलंबित"),
    "status_in_progress": _T("In Progress", "प्रगति पर", "कार्यवाही सुरू"),
    "status_resolved": _T("Resolved", "निस्तारित", "निवारण झाले"),
    "wizard_step1": _T(
      "1. Society Details",
      "1. सोसायटी विवरण",
      "१. संस्थेची माहिती",
    ),
    "wizard_step2": _T("2. Unit Details", "2. इकाई विवरण", "२. सदनिका माहिती"),
    "wizard_step3": _T("3. Floor Inventory", "3. मंजिल सूची", "३. मजला रचना"),
    "wizard_step4": _T(
      "4. Office Bearers",
      "4. पदाधिकारी",
      "४. पदाधिकारी नियुक्ती",
    ),
    "wizard_step5": _T(
      "5. Confirmation",
      "5. पुष्टि एवं शुरुआत",
      "५. खात्री व सुरुवात",
    ),
    "save_continue": _T(
      "Save & Continue",
      "सहेजें और आगे बढ़ें",
      "जतन करा व पुढे चला",
    ),
    "finish_setup": _T(
      "Finish & Launch Society",
      "पंजीकरण पूरा करें",
      "नोंदणी पूर्ण करा",
    ),
    "amount": _T("Amount (₹)", "राशि (₹)", "रक्कम (₹)"),
    "date": _T("Date", "दिनांक", "तारीख"),
    "vendor_name": _T(
      "Vendor / Payee Name",
      "विक्रेता / प्राप्तकर्ता का नाम",
      "विक्रेते / व्यक्तीचे नाव",
    ),
    "notes": _T("Notes / Description", "विवरण / टिप्पणी", "तपशील / शेरा"),
    "submit": _T("Submit", "जमा करें", "सादर करा"),
    "cancel": _T("Cancel", "रद्द करें", "रद्द करा"),
    "close": _T("Close", "बंद करें", "बंद करा"),
    "save": _T("Save", "सहेजें", "जतन करा"),
    "delete": _T("Delete", "हटाएं", "हटवा"),
    "success": _T("Successful", "सफल", "यशस्वी"),
    "amount_paid": _T("Amount Paid", "भुगतान राशि", "भरलेली रक्कम"),
    "receipt_no": _T("Receipt No.", "रसीद क्र.", "पावती क्र."),
    "flat_type": _T("Unit Type", "इकाई का प्रकार", "सदनिका प्रकार"),
    "sqft": _T("Square Feet", "वर्ग फुट", "चौरस फूट"),
    "parking_slot": _T("Parking Slot", "पार्किंग स्लॉट", "पार्किंग जागा क्र."),
    "select_language": _T("Select Language", "भाषा चुनें", "भाषा निवडा"),
  };
}

/// Shorthand: `tr("key", lang)`.
String tr(String key, AppLanguage language) => AppStrings.get(key, language);
