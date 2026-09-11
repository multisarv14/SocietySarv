package com.example.data.localization

/**
 * Offline Country -> State -> City lists backing the three dropdowns in the
 * Society Details step of the registration wizard.
 *
 * India is filled in properly (all states and union territories, plus the
 * cities a housing society is realistically registered in). The handful of
 * other countries carry their top-level regions only - the city dropdown for
 * those falls back to free text, which every dropdown here already allows, so
 * a society in a town that is not on the list is never blocked from
 * registering.
 */
object LocationData {

    val countries: List<String> = listOf(
        "India",
        "United Arab Emirates",
        "United States",
        "United Kingdom",
        "Canada",
        "Australia",
        "Singapore",
        "Other"
    )

    const val DEFAULT_COUNTRY = "India"

    /** States / union territories for a country, empty when only free text applies. */
    fun statesOf(country: String): List<String> = when (country) {
        "India" -> INDIA_STATES.keys.sorted()
        "United Arab Emirates" -> UAE_EMIRATES
        "United States" -> US_STATES
        "United Kingdom" -> UK_REGIONS
        "Canada" -> CANADA_PROVINCES
        "Australia" -> AUSTRALIA_STATES
        else -> emptyList()
    }

    /** Cities for a state, empty when the user should simply type one in. */
    fun citiesOf(country: String, state: String): List<String> = when (country) {
        "India" -> INDIA_STATES[state].orEmpty()
        else -> emptyList()
    }

    private val UAE_EMIRATES = listOf(
        "Abu Dhabi", "Ajman", "Dubai", "Fujairah", "Ras Al Khaimah", "Sharjah", "Umm Al Quwain"
    )

    private val UK_REGIONS = listOf("England", "Northern Ireland", "Scotland", "Wales")

    private val CANADA_PROVINCES = listOf(
        "Alberta", "British Columbia", "Manitoba", "New Brunswick",
        "Newfoundland and Labrador", "Nova Scotia", "Ontario",
        "Prince Edward Island", "Quebec", "Saskatchewan"
    )

    private val AUSTRALIA_STATES = listOf(
        "Australian Capital Territory", "New South Wales", "Northern Territory",
        "Queensland", "South Australia", "Tasmania", "Victoria", "Western Australia"
    )

    private val US_STATES = listOf(
        "Alabama", "Alaska", "Arizona", "Arkansas", "California", "Colorado", "Connecticut",
        "Delaware", "Florida", "Georgia", "Hawaii", "Idaho", "Illinois", "Indiana", "Iowa",
        "Kansas", "Kentucky", "Louisiana", "Maine", "Maryland", "Massachusetts", "Michigan",
        "Minnesota", "Mississippi", "Missouri", "Montana", "Nebraska", "Nevada",
        "New Hampshire", "New Jersey", "New Mexico", "New York", "North Carolina",
        "North Dakota", "Ohio", "Oklahoma", "Oregon", "Pennsylvania", "Rhode Island",
        "South Carolina", "South Dakota", "Tennessee", "Texas", "Utah", "Vermont",
        "Virginia", "Washington", "West Virginia", "Wisconsin", "Wyoming"
    )

    private val INDIA_STATES: Map<String, List<String>> = mapOf(
        "Andaman and Nicobar Islands" to listOf("Port Blair", "Diglipur", "Mayabunder", "Rangat"),
        "Andhra Pradesh" to listOf(
            "Visakhapatnam", "Vijayawada", "Guntur", "Nellore", "Kurnool", "Rajahmundry",
            "Tirupati", "Kakinada", "Kadapa", "Anantapur", "Eluru", "Ongole"
        ),
        "Arunachal Pradesh" to listOf("Itanagar", "Naharlagun", "Pasighat", "Tawang", "Ziro"),
        "Assam" to listOf(
            "Guwahati", "Silchar", "Dibrugarh", "Jorhat", "Nagaon", "Tinsukia", "Tezpur", "Bongaigaon"
        ),
        "Bihar" to listOf(
            "Patna", "Gaya", "Bhagalpur", "Muzaffarpur", "Darbhanga", "Purnia", "Ara",
            "Begusarai", "Katihar", "Chapra"
        ),
        "Chandigarh" to listOf("Chandigarh"),
        "Chhattisgarh" to listOf(
            "Raipur", "Bhilai", "Bilaspur", "Korba", "Durg", "Rajnandgaon", "Jagdalpur", "Raigarh"
        ),
        "Dadra and Nagar Haveli and Daman and Diu" to listOf("Silvassa", "Daman", "Diu"),
        "Delhi" to listOf(
            "New Delhi", "Dwarka", "Rohini", "Saket", "Pitampura", "Janakpuri",
            "Vasant Kunj", "Mayur Vihar", "Karol Bagh", "Shahdara"
        ),
        "Goa" to listOf("Panaji", "Margao", "Vasco da Gama", "Mapusa", "Ponda", "Porvorim"),
        "Gujarat" to listOf(
            "Ahmedabad", "Surat", "Vadodara", "Rajkot", "Bhavnagar", "Jamnagar", "Gandhinagar",
            "Junagadh", "Anand", "Nadiad", "Bharuch", "Navsari", "Vapi", "Mehsana"
        ),
        "Haryana" to listOf(
            "Gurugram", "Faridabad", "Panipat", "Ambala", "Karnal", "Hisar", "Rohtak",
            "Sonipat", "Yamunanagar", "Panchkula", "Bahadurgarh"
        ),
        "Himachal Pradesh" to listOf("Shimla", "Dharamshala", "Solan", "Mandi", "Kullu", "Baddi", "Una"),
        "Jammu and Kashmir" to listOf("Srinagar", "Jammu", "Anantnag", "Baramulla", "Udhampur", "Katra"),
        "Jharkhand" to listOf(
            "Ranchi", "Jamshedpur", "Dhanbad", "Bokaro Steel City", "Deoghar", "Hazaribagh", "Giridih"
        ),
        "Karnataka" to listOf(
            "Bengaluru", "Mysuru", "Hubballi", "Mangaluru", "Belagavi", "Davanagere",
            "Ballari", "Tumakuru", "Shivamogga", "Kalaburagi", "Udupi", "Hassan"
        ),
        "Kerala" to listOf(
            "Thiruvananthapuram", "Kochi", "Kozhikode", "Thrissur", "Kollam", "Alappuzha",
            "Kannur", "Palakkad", "Kottayam", "Malappuram"
        ),
        "Ladakh" to listOf("Leh", "Kargil"),
        "Lakshadweep" to listOf("Kavaratti", "Agatti", "Minicoy"),
        "Madhya Pradesh" to listOf(
            "Indore", "Bhopal", "Jabalpur", "Gwalior", "Ujjain", "Sagar", "Dewas",
            "Satna", "Ratlam", "Rewa", "Katni", "Singrauli"
        ),
        "Maharashtra" to listOf(
            "Mumbai", "Navi Mumbai", "Thane", "Pune", "Pimpri-Chinchwad", "Nagpur", "Nashik",
            "Aurangabad", "Solapur", "Kolhapur", "Amravati", "Sangli", "Jalgaon", "Akola",
            "Latur", "Ahmednagar", "Nanded", "Chandrapur", "Satara", "Ratnagiri",
            "Dombivli", "Kalyan", "Vasai-Virar", "Panvel", "Badlapur", "Ambernath"
        ),
        "Manipur" to listOf("Imphal", "Thoubal", "Bishnupur", "Churachandpur"),
        "Meghalaya" to listOf("Shillong", "Tura", "Jowai", "Nongstoin"),
        "Mizoram" to listOf("Aizawl", "Lunglei", "Champhai", "Serchhip"),
        "Nagaland" to listOf("Kohima", "Dimapur", "Mokokchung", "Tuensang"),
        "Odisha" to listOf(
            "Bhubaneswar", "Cuttack", "Rourkela", "Berhampur", "Sambalpur", "Puri", "Balasore"
        ),
        "Puducherry" to listOf("Puducherry", "Karaikal", "Mahe", "Yanam"),
        "Punjab" to listOf(
            "Ludhiana", "Amritsar", "Jalandhar", "Patiala", "Bathinda", "Mohali",
            "Hoshiarpur", "Pathankot", "Moga"
        ),
        "Rajasthan" to listOf(
            "Jaipur", "Jodhpur", "Udaipur", "Kota", "Ajmer", "Bikaner", "Bhilwara",
            "Alwar", "Sikar", "Sri Ganganagar"
        ),
        "Sikkim" to listOf("Gangtok", "Namchi", "Gyalshing", "Mangan"),
        "Tamil Nadu" to listOf(
            "Chennai", "Coimbatore", "Madurai", "Tiruchirappalli", "Salem", "Tirunelveli",
            "Tiruppur", "Erode", "Vellore", "Thoothukudi", "Thanjavur", "Hosur"
        ),
        "Telangana" to listOf(
            "Hyderabad", "Secunderabad", "Warangal", "Nizamabad", "Karimnagar",
            "Khammam", "Ramagundam", "Mahbubnagar"
        ),
        "Tripura" to listOf("Agartala", "Udaipur", "Dharmanagar", "Kailashahar"),
        "Uttar Pradesh" to listOf(
            "Lucknow", "Kanpur", "Ghaziabad", "Noida", "Greater Noida", "Agra", "Varanasi",
            "Meerut", "Prayagraj", "Bareilly", "Aligarh", "Moradabad", "Gorakhpur",
            "Saharanpur", "Jhansi", "Mathura", "Ayodhya"
        ),
        "Uttarakhand" to listOf(
            "Dehradun", "Haridwar", "Rishikesh", "Haldwani", "Roorkee", "Nainital", "Rudrapur"
        ),
        "West Bengal" to listOf(
            "Kolkata", "Howrah", "Durgapur", "Asansol", "Siliguri", "Bardhaman",
            "Malda", "Kharagpur", "Haldia"
        )
    )
}
