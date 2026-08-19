# -*- coding: utf-8 -*-
"""Seed the Play Store review demo society into societysarv-app-db."""
import json, urllib.request

KEY = "AIzaSyCjodhNA-w07k72T_tpyMZ8YT99veKOngg"
DOCPATH = "projects/multisarv-mobileapps/databases/societysarv-app-db/documents"
BASE = "https://firestore.googleapis.com/v1/" + DOCPATH

def v(x):
    if x is None:            return {"nullValue": None}
    if isinstance(x, bool):  return {"booleanValue": x}
    if isinstance(x, int):   return {"integerValue": str(x)}
    if isinstance(x, float): return {"doubleValue": x}
    if isinstance(x, str):   return {"stringValue": x}
    raise TypeError(x)

SOCIETY = {
    "id": 1, "name": "SocietySarv Demo Society",
    "registrationNumber": "DEMO/HSG/TC/0001/2026", "societyCode": "DEMO1",
    "address": "Demo Address, Pune - 411045",
    "totalWings": 2, "totalFlats": 12,
    "bankAccountName": "SocietySarv Demo Society A/C",
    "bankAccountNumber": "00000000000000", "ifscCode": "DEMO0000001",
    "currentBalanceCash": 5000.0, "currentBalanceBank": 125000.0,
    "lateFeePercentPerMonth": 2.0, "lateFeeGraceDays": 10,
    "tariff1BHK": 1500.0, "tariff2BHK": 2200.0, "tariff3BHK": 3000.0,
    "tariffPenthouse": 4500.0, "tariffShop": 3500.0,
    "sinkingFundPercent": 5.0, "repairFundPercent": 5.0, "logoBase64": "",
}

def resident(i, flat, wing, name, phone, role, pin, ftype, sqft, due, slot="", tw="", fw=""):
    return (flat, {
        "id": i, "flatNumber": flat, "wing": wing, "ownerName": name, "tenantName": "",
        "contactNumber": phone, "email": "", "role": role, "pin": pin,
        "familyMemberCount": 3, "vehicleTwoWheelerCount": 1 if tw else 0,
        "vehicleFourWheelerCount": 1 if fw else 0, "twoWheelerNumber": tw,
        "fourWheelerNumber": fw, "parkingSlot": slot, "flatType": ftype,
        "squareFeet": sqft, "balanceDue": due, "isOwnerLiving": True, "photoBase64": "",
    })

RESIDENTS = [
    resident(1, "A-101", "A", "Demo Chairman", "9000000001", "CHAIRMAN", "1234",
             "THREE_BHK", 1250, 0.0, "P-01", "MH12DM0001", "MH12DM1001"),
    resident(2, "A-102", "A", "Demo Secretary", "9000000002", "SECRETARY", "2345",
             "TWO_BHK", 950, 0.0, "P-02"),
    resident(3, "B-201", "B", "Demo Resident", "9000000003", "RESIDENT", "5678",
             "TWO_BHK", 980, 2200.0, "P-07", "MH12DM0002"),
    resident(4, "GATE-01", "Gate", "Demo Gate Guard", "9000000004", "GUARD", "9012",
             "ONE_BHK", 120, 0.0, "Guard Cabin"),
]

def bill(i, flat, name, base, total, paid, status, mode=None, ref="", receipt="", pd=""):
    return ("demo-bill-%d" % i, {
        "id": i, "flatNumber": flat, "residentName": name, "billMonth": "August 2026",
        "baseAmount": base, "waterCharges": 300.0, "sinkingFund": 110.0,
        "repairFund": 110.0, "parkingCharges": 200.0, "nonOccupancyCharges": 0.0,
        "lateFeeAmount": 0.0, "totalAmount": total, "amountPaid": paid,
        "status": status, "dueDate": "10 Aug 2026", "paidDate": pd,
        "paymentMode": mode, "transactionRef": ref, "receiptNumber": receipt,
        "receiptImageBase64": "",
    })

BILLS = [
    bill(1, "A-101", "Demo Chairman", 3000.0, 3720.0, 3720.0, "PAID", "UPI",
         "UPI/DEMO/0001", "REC-DEMO-0001", "04 Aug 2026"),
    bill(2, "A-102", "Demo Secretary", 2200.0, 2920.0, 2920.0, "PAID", "BANK_TRANSFER",
         "NEFT/DEMO/0002", "REC-DEMO-0002", "06 Aug 2026"),
    bill(3, "B-201", "Demo Resident", 2200.0, 2920.0, 0.0, "UNPAID",
         receipt="BILL-DEMO-0003"),
]

EXPENSES = [("demo-exp-1", {
    "id": 1, "title": "Lift AMC quarterly service", "category": "LIFT_AMC",
    "amount": 8500.0, "date": "05 Aug 2026", "vendorName": "Demo Lift Services",
    "vendorContact": "9000000010", "paymentMode": "BANK_TRANSFER",
    "approvedBy": "CHAIRMAN", "notes": "Quarterly maintenance contract.",
    "receiptNumber": "EXP-DEMO-001", "status": "PAID", "billImageBase64": "",
}), ("demo-exp-2", {
    "id": 2, "title": "Common area electricity bill", "category": "ELECTRICITY",
    "amount": 6200.0, "date": "07 Aug 2026", "vendorName": "Demo Power Utility",
    "vendorContact": "1912", "paymentMode": "BANK_TRANSFER", "approvedBy": "SECRETARY",
    "notes": "Staircase lighting and water pumps.", "receiptNumber": "EXP-DEMO-002",
    "status": "PAID", "billImageBase64": "",
})]

REVENUES = [("demo-nfr-1", {
    "id": 1, "title": "Community hall booking", "sourceCategory": "HALL_RENTAL",
    "amount": 3500.0, "date": "10 Aug 2026", "payerName": "Demo Resident",
    "flatNumber": "B-201", "paymentMode": "UPI", "receiptNumber": "NFR-DEMO-001",
    "notes": "Weekend function booking.",
})]

VISITORS = [("demo-visit-1", {
    "id": 1, "visitorName": "Demo Delivery Agent", "phone": "9000000020",
    "flatNumber": "B-201", "purpose": "DELIVERY", "vehicleNumber": "MH12DM9999",
    "entryTime": "11:20 AM", "exitTime": "", "status": "INSIDE",
    "passcode": "123456", "guardName": "Demo Gate Guard", "photoBase64": "",
}), ("demo-visit-2", {
    "id": 2, "visitorName": "Demo Guest", "phone": "9000000021",
    "flatNumber": "A-101", "purpose": "GUEST", "vehicleNumber": "",
    "entryTime": "09:30 AM", "exitTime": "10:45 AM", "status": "EXITED",
    "passcode": "654321", "guardName": "Demo Gate Guard", "photoBase64": "",
})]

NOTICES = [("demo-notice-1", {
    "id": 1, "title": "Annual General Body Meeting",
    "content": "The annual general body meeting is scheduled for Sunday at 10:30 AM "
               "in the society clubhouse. All members are requested to attend.",
    "publishedDate": "15 Aug 2026", "isPinned": True, "category": "AGM",
    "authorRole": "CHAIRMAN", "imageBase64": "",
}), ("demo-notice-2", {
    "id": 2, "title": "Water tank cleaning schedule",
    "content": "Overhead water tanks will be cleaned on Thursday between 10:00 AM "
               "and 4:00 PM. Please store water in advance.",
    "publishedDate": "14 Aug 2026", "isPinned": False, "category": "WATER",
    "authorRole": "SECRETARY", "imageBase64": "",
})]

COMPLAINTS = [("demo-cmp-1", {
    "id": 1, "flatNumber": "B-201", "residentName": "Demo Resident",
    "category": "ELECTRICAL", "description": "Corridor light on the second floor is not working.",
    "filedDate": "16 Aug 2026", "status": "IN_PROGRESS",
    "resolutionNotes": "Electrician scheduled for tomorrow.", "attachmentBase64": "",
})]

CONTACTS = [
    ("demo-emg-1", {"id": 1, "titleEn": "Police Control Room", "titleMr": "पोलीस नियंत्रण कक्ष",
                    "name": "Local Police Station", "phoneNumber": "112", "category": "POLICE"}),
    ("demo-emg-2", {"id": 2, "titleEn": "Fire Brigade", "titleMr": "अग्निशामक दल",
                    "name": "Fire Station", "phoneNumber": "101", "category": "FIRE"}),
    ("demo-emg-3", {"id": 3, "titleEn": "Ambulance", "titleMr": "रुग्णवाहिका",
                    "name": "Emergency Ambulance", "phoneNumber": "108", "category": "AMBULANCE"}),
    ("demo-emg-4", {"id": 4, "titleEn": "Society Manager", "titleMr": "सोसायटी व्यवस्थापक",
                    "name": "Demo Estate Manager", "phoneNumber": "9000000030", "category": "MANAGER"}),
]

COLLECTIONS = {
    "society_info": [("main", SOCIETY)],
    "residents": RESIDENTS,
    "maintenance_bills": BILLS,
    "expense_records": EXPENSES,
    "non_fee_revenues": REVENUES,
    "visitor_logs": VISITORS,
    "notices": NOTICES,
    "complaints": COMPLAINTS,
    "emergency_contacts": CONTACTS,
}
COUNTERS = {k: len(v) for k, v in COLLECTIONS.items() if k != "society_info"}

writes = []
for coll, docs in COLLECTIONS.items():
    for doc_id, data in docs:
        d = dict(data); d["docId"] = doc_id
        writes.append({"update": {"name": "%s/%s/%s" % (DOCPATH, coll, doc_id),
                                  "fields": {k: v(x) for k, x in d.items()}}})
for coll, n in COUNTERS.items():
    writes.append({"update": {"name": "%s/counters/%s" % (DOCPATH, coll),
                              "fields": {"value": v(n), "collection": v(coll), "docId": v(coll)}}})

req = urllib.request.Request(BASE + ":commit?key=" + KEY,
                             data=json.dumps({"writes": writes}).encode(),
                             headers={"Content-Type": "application/json"}, method="POST")
res = json.load(urllib.request.urlopen(req))
print("committed %d documents" % len(res.get("writeResults", [])))
