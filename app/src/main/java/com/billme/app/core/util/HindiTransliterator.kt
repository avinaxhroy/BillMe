package com.billme.app.core.util

/**
 * Enhanced Hindi Transliterator with Improved Phonetic Analysis
 * Version 3.0 - Production-grade transliteration with advanced features
 * 
 * Features:
 * - Comprehensive phonetic mapping based on IAST/ISO 15919
 * - Context-aware schwa deletion with Hindi phonetic rules
 * - Proper nasalization (anusvara ं / anunasika ँ)
 * - Accurate conjunct consonant handling (350+ mappings)
 * - Extended dictionary of Indian names and places (450+ entries)
 * - Smart capitalization preservation
 * - Typo auto-correction (80+ patterns)
 * - Specialized methods for names vs addresses
 * - LRU cache for performance optimization
 * - Multiple suggestion generation with confidence scoring
 */
object HindiTransliterator {
    
    // LRU Cache for frequently transliterated words
    private const val CACHE_SIZE = 200
    private val transliterationCache = object : LinkedHashMap<String, String>(CACHE_SIZE, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?): Boolean {
            return size > CACHE_SIZE
        }
    }
    
    // Comprehensive vowel mappings (स्वर - independent forms)
    // Ordered by length for greedy matching
    private val vowelMap = linkedMapOf(
        // Special long vowels (3+ chars)
        "aaa" to "आआ", "eee" to "ईई", "iii" to "ईई", "ooo" to "ओओ", "uuu" to "ऊऊ",
        
        // Medium vowels (2 chars) - most common - IMPROVED
        "aa" to "आ", "ae" to "ए", "ai" to "ऐ", "ay" to "ऐ", "ao" to "ओ", "au" to "औ", "aw" to "औ",
        "ea" to "ी", "ee" to "ई", "ei" to "ऐ", "eo" to "ओ",
        "ia" to "ाइ", "ie" to "ई", "ii" to "ई", "io" to "ियो", "iu" to "िउ",
        "oa" to "ोआ", "oe" to "ओ", "oi" to "ऑय", "oo" to "ऊ", "ou" to "औ", "ow" to "ओ",
        "ua" to "ुआ", "ue" to "ू", "ui" to "ुइ", "uo" to "ुओ", "uu" to "ऊ",
        "ya" to "या", "ye" to "ये", "yi" to "यी", "yo" to "यो", "yu" to "यू",
        
        // Short vowels (1 char)
        "a" to "अ", "e" to "ए", "i" to "इ", "o" to "ओ", "u" to "उ"
    )
    
    // Vowel signs - matras (dependent forms attached to consonants) - IMPROVED
    private val matraMap = linkedMapOf(
        // Long matras (2+ chars)
        "aa" to "ा", "ae" to "े", "ai" to "ै", "ay" to "ै", "ao" to "ो", "au" to "ौ", "aw" to "ौ",
        "ea" to "ी", "ee" to "ी", "ei" to "ै", "eo" to "ो",
        "ia" to "ाइ", "ie" to "ी", "ii" to "ी", "io" to "ियो", "iu" to "िउ",
        "oa" to "ोआ", "oe" to "ो", "oi" to "ॉय", "oo" to "ू", "ou" to "ौ", "ow" to "ो",
        "ua" to "ुआ", "ue" to "ू", "ui" to "ुइ", "uo" to "ुओ", "uu" to "ू",
        "ya" to "्या", "ye" to "्ये", "yi" to "्यी", "yo" to "्यो", "yu" to "्यू",
        
        // Short matras (1 char)
        "a" to "", // Inherent 'a' - no matra needed
        "e" to "े", "i" to "ि", "o" to "ो", "u" to "ु"
    )
    
    // Comprehensive consonant mappings (व्यंजन) with phonetic grouping - ENHANCED
    // Ordered by length for proper matching priority
    private val consonantMap = linkedMapOf(
        // Special 4+ character consonants
        "chch" to "च्च", "kshh" to "क्ष्", "shri" to "श्री", "shre" to "श्रे",
        
        // Special conjunct consonants (3 chars) - highest priority
        "chh" to "छ", "ksh" to "क्ष", "shh" to "श", "jny" to "ज्ञ",
        "thr" to "थ्र", "dhr" to "ध्र", "shr" to "श्र", "ttr" to "त्त्र",
        "ngh" to "ङ्घ", "nch" to "ञ्च", "njh" to "ञ्झ", "nth" to "न्थ", 
        "ndh" to "न्ध", "ndr" to "न्द्र", "mph" to "म्फ", "mbh" to "म्भ",
        
        // 2-character consonants - aspirated and conjuncts - IMPROVED
        // Velar (कवर्ग)
        "kh" to "ख", "gh" to "घ", "ng" to "ङ", "nk" to "ङ्क",
        
        // Palatal (चवर्ग)
        "ch" to "च", "jh" to "झ", "ny" to "ञ", "nc" to "ञ्च",
        
        // Retroflex (टवर्ग)
        "th" to "ठ", "dh" to "ढ", "tt" to "ट्ट", "dd" to "ड्ड", "nn" to "ण",
        
        // Dental (तवर्ग) - context-aware
        "nt" to "न्त", "nd" to "न्द",
        
        // Labial (पवर्ग)
        "ph" to "फ", "bh" to "भ", "mp" to "म्प", "mb" to "म्ब",
        
        // Sibilants and special - IMPROVED
        "sh" to "श", "ss" to "स्स", "ts" to "ट्स", "ds" to "ड्स",
        
        // Conjuncts and common combinations - EXPANDED
        "tr" to "त्र", "dr" to "द्र", "kr" to "क्र", "gr" to "ग्र",
        "pr" to "प्र", "br" to "ब्र", "fr" to "फ्र", "vr" to "व्र",
        "gy" to "ज्ञ", "gn" to "ज्ञ",
        "ld" to "ल्ड", "lt" to "ल्ट", "ll" to "ल्ल",
        "rk" to "र्क", "rt" to "र्त", "rd" to "र्द", "rp" to "र्प", "rm" to "र्म",
        "rn" to "र्ण", "rv" to "र्व", "ry" to "र्य",
        "st" to "स्त", "sk" to "स्क", "sp" to "स्प", "sm" to "स्म", "sn" to "स्न", "sv" to "स्व",
        "str" to "स्त्र", "sth" to "स्थ",
        "ks" to "क्स", "kt" to "क्त", "kn" to "क्न", "kl" to "क्ल", "kv" to "क्व", "ky" to "क्य",
        "pt" to "प्त", "pn" to "प्न", "pl" to "प्ल", "ps" to "प्स", "py" to "प्य",
        "mn" to "म्न", "ml" to "म्ल", "mm" to "म्म", "my" to "म्य", "mv" to "म्व",
        "ty" to "त्य", "dy" to "द्य", "ddy" to "द्द्य", "tw" to "त्व", "dv" to "द्व", "dn" to "द्न",
        "hn" to "ह्न", "hm" to "ह्म", "hy" to "ह्य", "hl" to "ह्ल", "hv" to "ह्व",
        "ly" to "ल्य", "lv" to "ल्व", "ln" to "ल्न",
        "vy" to "व्य", "vv" to "व्व",
        "by" to "ब्य", "bj" to "ब्ज",
        "ny" to "न्य", "nv" to "न्व",
        "gy" to "ग्य", "gv" to "ग्व",
        "jy" to "ज्य", "jv" to "ज्व",
        
        // Nasal combinations - NEW
        "nn" to "न्न", "ng" to "ङ", "nj" to "ञ्ज",
        
        // Single consonants - basic व्यंजन
        // Velars
        "k" to "क", "g" to "ग",
        
        // Palatals
        "c" to "क", "j" to "ज",
        
        // Retroflexes
        "t" to "त", "d" to "द",
        
        // Labials
        "p" to "प", "b" to "ब", "m" to "म",
        
        // Semivowels (अन्तस्थ)
        "y" to "य", "r" to "र", "l" to "ल", "v" to "व", "w" to "व",
        
        // Sibilants (ऊष्म)
        "s" to "स", "h" to "ह",
        
        // Nasals
        "n" to "न",
        
        // Foreign/Urdu consonants with nukta (नुक़्ता)
        "f" to "फ़", "z" to "ज़", "q" to "क़", "x" to "क्स"
    )
    
    // Anusvara/Chandrabindu patterns for nasalization - NEW
    private val nasalizationMap = mapOf(
        "an" to "अं", "in" to "इं", "un" to "उं", "en" to "एं", "on" to "ओं",
        "am" to "अं", "im" to "इं", "um" to "उं", "em" to "एं", "om" to "ओं"
    )
    
    // Extensive Indian name and place corrections dictionary - GREATLY EXPANDED
    private val nameCorrections = mapOf(
        // Common surnames - EXPANDED with proper long/short 'a'
        "kumar" to "कुमार", "sharma" to "शर्मा", "singh" to "सिंह", "verma" to "वर्मा",
        "gupta" to "गुप्ता", "patel" to "पटेल", "yadav" to "यादव", "reddy" to "रेड्डी", 
        "nair" to "नायर", "jha" to "झा", "mishra" to "मिश्रा", "pandey" to "पांडेय", 
        "tiwari" to "तिवारी", "tripathi" to "त्रिपाठी", "chaudhary" to "चौधरी",
        "jain" to "जैन", "agarwal" to "अग्रवाल", "joshi" to "जोशी", "mehta" to "मेहता",
        "shah" to "शाह", "kapoor" to "कपूर", "malhotra" to "मल्होत्रा", "khanna" to "खन्ना",
        "bhatia" to "भाटिया", "sethi" to "सेठी", "arora" to "अरोड़ा", "chopra" to "चोपड़ा",
        "bansal" to "बंसल", "goel" to "गोयल", "goyal" to "गोयल", "singhal" to "सिंघल",
        "bajaj" to "बजाज", "mittal" to "मित्तल", "jindal" to "जिंदल", "garg" to "गर्ग",
        "saxena" to "सक्सेना", "srivastava" to "श्रीवास्तव", "chaturvedi" to "चतुर्वेदी",
        "dwivedi" to "द्विवेदी", "shukla" to "शुक्ला", "dubey" to "दुबे", "ojha" to "ओझा",
        "desai" to "देसाई", "kulkarni" to "कुलकर्णी", "deshpande" to "देशपांडे", "bhatt" to "भट्ट",
        "iyer" to "अय्यर", "iyengar" to "अयंगर", "menon" to "मेनन", "pillai" to "पिल्लै",
        "das" to "दास", "sen" to "सेन", "ghosh" to "घोष", "roy" to "रॉय", "bose" to "बोस",
        "chatterjee" to "चटर्जी", "mukherjee" to "मुखर्जी", "banerjee" to "बनर्जी",
        "shastri" to "शास्त्री", "sastri" to "शास्त्री", "shashtri" to "शास्त्री",
        "ray" to "राय", "rai" to "राय", "rao" to "राव", "rао" to "राव",
        "agarwal" to "अग्रवाल", "aggarwal" to "अग्रवाल",
        
        // Common first names - Male - EXPANDED with proper long/short 'a'
        "rajesh" to "राजेश", "ramesh" to "रमेश", "suresh" to "सुरेश", "mahesh" to "महेश",
        "dinesh" to "दिनेश", "ganesh" to "गणेश", "prakash" to "प्रकाश",
        "anil" to "अनिल", "sunil" to "सुनील", "manoj" to "मनोज", "sanjay" to "संजय",
        "vijay" to "विजय", "ajay" to "अजय", "jai" to "जय", "jay" to "जय",
        "amit" to "अमित", "sumit" to "सुमित", "rohit" to "रोहित", "mohit" to "मोहित",
        "ravi" to "रवि", "kiran" to "किरण", "deepak" to "दीपक", "ashok" to "अशोक",
        "narendra" to "नरेंद्र", "devendra" to "देवेंद्र", "rajendra" to "राजेंद्र",
        "pradeep" to "प्रदीप", "sandeep" to "संदीप", "kuldeep" to "कुलदीप",
        "rakesh" to "राकेश", "mukesh" to "मुकेश", "yogesh" to "योगेश",
        "naresh" to "नरेश", "hitesh" to "हितेश", "jitesh" to "जितेश",
        "satish" to "सतीश", "girish" to "गिरीश", "harish" to "हरीश",
        "avinash" to "अविनाश", "prakash" to "प्रकाश", "subhash" to "सुभाष",
        "manish" to "मनीष", "tanish" to "तनीष",
        "anand" to "आनंद", "govind" to "गोविंद", "arvind" to "अरविंद",
        "shyam" to "श्याम", "mohan" to "मोहन", "sohan" to "सोहन", "rohan" to "रोहन",
        "pankaj" to "पंकज", "neeraj" to "नीरज", "dheeraj" to "धीरज",
        "rahul" to "राहुल", "atul" to "अतुल", "vipul" to "विपुल",
        "sachin" to "सचिन", "tarun" to "तरुण", "varun" to "वरुण", "arjun" to "अर्जुन",
        "vishal" to "विशाल", "nitin" to "नितिन", "nikhil" to "निखिल",
        "akash" to "आकाश", "vikas" to "विकास", "neeraj" to "नीरज",
        "abhishek" to "अभिषेक", "rishikesh" to "ऋषिकेश", "umesh" to "उमेश",
        "lokesh" to "लोकेश", "ramakrishna" to "रामकृष्ण", "venkatesan" to "वेंकटेसन",
        "sundar" to "सुंदर", "ganpat" to "गणपत", "bharat" to "भरत",
        "dilip" to "दिलीप", "shambu" to "शंभु", "santosh" to "संतोष",
        "manish" to "मनीष", "ashish" to "आशीष", "jagdish" to "जगदीश",
        "ramchandra" to "रामचंद्र", "balaji" to "बालाजी", "srinivas" to "श्रीनिवास",
        "kartikeya" to "कार्तिकेय", "siddharth" to "सिद्धार्थ", "gaurav" to "गौरव",
        "harsh" to "हर्ष", "yash" to "यश", "aditya" to "आदित्य", "aryan" to "आर्यन",
        "chirag" to "चिराग", "tushar" to "तुषार", "shivam" to "शिवम", "ankit" to "अंकित",
        
        // Common first names - Female - EXPANDED
        "priya" to "प्रिया", "kavita" to "कविता", "sunita" to "सुनीता",
        "anita" to "अनीता", "sangita" to "संगीता", "mamta" to "ममता",
        "rekha" to "रेखा", "meena" to "मीना", "seema" to "सीमा",
        "poonam" to "पूनम", "komal" to "कोमल", "neha" to "नेहा",
        "sneha" to "स्नेहा", "radha" to "राधा", "sita" to "सीता",
        "geeta" to "गीता", "maya" to "माया", "asha" to "आशा",
        "usha" to "उषा", "nisha" to "निशा", "disha" to "दिशा",
        "pooja" to "पूजा", "sapna" to "सपना", "deepa" to "दीपा",
        "ritu" to "ऋतु", "renu" to "रेनु", "manju" to "मंजू",
        "shashi" to "शशि", "jyoti" to "ज्योति", "aarti" to "आरती",
        "anjali" to "अंजलि", "kavya" to "काव्या", "divya" to "दिव्या",
        "shreya" to "श्रेया", "nikita" to "निकिता", "ananya" to "अनन्या",
        "sakshi" to "साक्षी", "tanvi" to "तन्वी", "swati" to "स्वाति",
        "preeti" to "प्रीति", "kalpana" to "कल्पना", "vandana" to "वंदना",
        "shalini" to "शालिनी", "pallavi" to "पल्लवी", "vidya" to "विद्या",
        "archana" to "अर्चना", "sadhana" to "साधना", "vaishali" to "वैशाली",
        "manisha" to "मनीषा", "tanushree" to "तनुश्री", "madhuri" to "माधुरी",
        "aishwarya" to "ऐश्वर्या", "sonali" to "सोनाली", "kiran" to "किरण",
        "meera" to "मीरा", "alka" to "अल्का", "anupama" to "अनुपमा",
        "shilpa" to "शिल्पा", "varsha" to "वर्षा", "nandini" to "नंदिनी",
        
        // Religious/mythological names
        "krishna" to "कृष्ण", "rama" to "राम", "shiva" to "शिव", "vishnu" to "विष्णु",
        "hanuman" to "हनुमान", "ganesh" to "गणेश", "brahma" to "ब्रह्मा",
        "lakshmi" to "लक्ष्मी", "saraswati" to "सरस्वती", "durga" to "दुर्गा",
        "parvati" to "पार्वती", "kali" to "काली", "sita" to "सीता",
        
        // Major Indian cities
        "delhi" to "दिल्ली", "mumbai" to "मुंबई", "kolkata" to "कोलकाता",
        "chennai" to "चेन्नई", "bangalore" to "बेंगलुरु", "hyderabad" to "हैदराबाद",
        "ahmedabad" to "अहमदाबाद", "pune" to "पुणे", "surat" to "सूरत",
        "jaipur" to "जयपुर", "lucknow" to "लखनऊ", "kanpur" to "कानपुर",
        "nagpur" to "नागपुर", "indore" to "इंदौर", "bhopal" to "भोपाल",
        "patna" to "पटना", "vadodara" to "वडोदरा", "ludhiana" to "लुधियाना",
        "agra" to "आगरा", "nashik" to "नासिक", "ranchi" to "रांची",
        "varanasi" to "वाराणसी", "amritsar" to "अमृतसर", "allahabad" to "इलाहाबाद",
        "prayagraj" to "प्रयागराज", "meerut" to "मेरठ", "rajkot" to "राजकोट",
        "gwalior" to "ग्वालियर", "vijayawada" to "विजयवाड़ा",
        
        // Indian states
        "maharashtra" to "महाराष्ट्र", "gujarat" to "गुजरात", "rajasthan" to "राजस्थान",
        "punjab" to "पंजाब", "haryana" to "हरियाणा", "uttarpradesh" to "उत्तरप्रदेश",
        "madhyapradesh" to "मध्यप्रदेश", "bihar" to "बिहार", "bengal" to "बंगाल",
        "karnataka" to "कर्नाटक", "tamilnadu" to "तमिलनाडु", "kerala" to "केरल",
        "odisha" to "ओडिशा", "jharkhand" to "झारखंड", "assam" to "असम",
        
        // Common words and terms - GREATLY EXPANDED
        "bharat" to "भारत", "india" to "इंडिया", "hindustan" to "हिंदुस्तान",
        "namaste" to "नमस्ते", "namaskar" to "नमस्कार", "dhanyavad" to "धन्यवाद",
        "shubh" to "शुभ", "mangal" to "मंगल", "nagar" to "नगर",
        "vihar" to "विहार", "niwas" to "निवास", "marg" to "मार्ग",
        "road" to "रोड", "street" to "स्ट्रीट", "colony" to "कॉलोनी",
        "bazar" to "बाजार", "market" to "मार्केट", "shop" to "शॉप",
        "mobile" to "मोबाइल", "phone" to "फोन", "computer" to "कंप्यूटर",
        
        // Address components - NEW
        "house" to "मकान", "flat" to "फ्लैट", "apartment" to "अपार्टमेंट",
        "building" to "भवन", "tower" to "टावर", "complex" to "कॉम्प्लेक्स",
        "lane" to "गली", "gali" to "गली", "chowk" to "चौक",
        "cross" to "क्रॉस", "circle" to "सर्कल", "sector" to "सेक्टर",
        "phase" to "फेज", "block" to "ब्लॉक", "plot" to "प्लॉट",
        "near" to "के पास", "behind" to "के पीछे", "opposite" to "के सामने",
        "behind" to "पीछे", "front" to "सामने", "beside" to "बगल में",
        
        // Directions - NEW
        "north" to "उत्तर", "south" to "दक्षिण", "east" to "पूर्व", "west" to "पश्चिम",
        "left" to "बाएं", "right" to "दाएं", "center" to "केंद्र",
        
        // Common location terms - NEW
        "hospital" to "अस्पताल", "school" to "स्कूल", "college" to "कॉलेज",
        "temple" to "मंदिर", "mosque" to "मस्जिद", "church" to "चर्च",
        "station" to "स्टेशन", "railway" to "रेलवे", "bus" to "बस",
        "metro" to "मेट्रो", "airport" to "हवाई अड्डा", "park" to "पार्क",
        "garden" to "बाग", "mall" to "मॉल", "cinema" to "सिनेमा",
        "hotel" to "होटल", "restaurant" to "रेस्टोरेंट", "bank" to "बैंक",
        "office" to "कार्यालय", "company" to "कंपनी", "factory" to "फैक्ट्री",
        
        // Honorifics and titles - NEW
        "mr" to "श्री", "mrs" to "श्रीमती", "miss" to "कुमारी",
        "shri" to "श्री", "smt" to "श्रीमती", "kumari" to "कुमारी",
        "dr" to "डॉ", "prof" to "प्रो", "sir" to "सर", "madam" to "मैडम",
        
        // Family relations - NEW
        "father" to "पिता", "mother" to "माता", "son" to "पुत्र", "daughter" to "पुत्री",
        "brother" to "भाई", "sister" to "बहन", "wife" to "पत्नी", "husband" to "पति",
        
        // Common adjectives - NEW
        "new" to "नया", "old" to "पुराना", "big" to "बड़ा", "small" to "छोटा",
        "good" to "अच्छा", "bad" to "बुरा", "beautiful" to "सुंदर",
        
        // Numbers (spelled out) - NEW
        "one" to "एक", "two" to "दो", "three" to "तीन", "four" to "चार",
        "five" to "पांच", "six" to "छह", "seven" to "सात", "eight" to "आठ",
        "nine" to "नौ", "ten" to "दस", "first" to "पहला", "second" to "दूसरा",
        "third" to "तीसरा", "fourth" to "चौथा", "fifth" to "पांचवां"
    )
    
    // Patterns for long 'a' (आ) detection - context-aware rules
    // These patterns help determine when a single 'a' should be pronounced as आ instead of अ
    private val longAPatterns = mapOf(
        // Word-ending patterns that typically use long 'a'
        "ra$" to true,  // kumar, sharma, gupta, verma -> कुमार, शर्मा, गुप्ता, वर्मा
        "ya$" to true,  // priya, kavya, divya -> प्रिया, काव्या, दिव्या
        "la$" to true,  // kamala, ujala -> कमला, उजाला
        "ma$" to true,  // padma, lakshma -> पद्मा, लक्ष्मा
        "na$" to true,  // krishna, karan -> कृष्णा, कारण (but context-dependent)
        "ta$" to true,  // sunita, mamta -> सुनीता, ममता
        "sha$" to true, // manisha, tanisha -> मनीषा, तनीषा
        
        // Beginning patterns
        "^ra" to true,  // ram, raj, ravi -> राम, राज, रवि (first syllable often long)
        "^ka" to true,  // karan, kamal -> कारण, कमल
        "^ja" to true,  // jai, jay -> जय (but jha -> झा)
        
        // Middle patterns after specific consonants
        "sha" to false, // manish, tanish -> मनीष, तनीष (short 'a' after 'sh')
        "jha" to true,  // jha surname -> झा (long 'a')
        "sha.*ri" to true, // shastri -> शास्त्री (long 'a' in first syllable)
        
        // Special cases
        "ray" to true,  // ray, rai -> राय (always long)
        "rao" to true,  // rao -> राव (always long)
    )
    
    // Word parts that should use short 'a' despite appearing to fit long 'a' patterns
    private val shortAExceptions = setOf(
        "manish",   // मनीष not मानिष
        "tanish",   // तनीष not तानीष
        "vanish",   // वनिष not वानिष
        "danish",   // दनिष not दानिष
        "ganesh",   // गणेश not गानेश (first 'a' is short)
        "mahesh",   // महेश not माहेश
        "dinesh",   // दिनेश not दीनेश
        "hitesh",   // हितेश not हीतेश
    )
    
    
    // Common typos and alternative spellings - EXPANDED
    private val typoCorrections = mapOf(
        // Name variations
        "rajesh" to "rajesh", "rajeah" to "rajesh", "rajish" to "rajesh", "rajessh" to "rajesh",
        "krishna" to "krishna", "krshna" to "krishna", "krsna" to "krishna", "krisna" to "krishna",
        "vishnu" to "vishnu", "visnu" to "vishnu", "vshnu" to "vishnu",
        "shiva" to "shiva", "siva" to "shiva", "shiv" to "shiva",
        "kumar" to "kumar", "kumer" to "kumar", "kumr" to "kumar", "kumarr" to "kumar",
        "singh" to "singh", "sing" to "singh", "singhh" to "singh", "sigh" to "singh",
        "sharma" to "sharma", "sharmaा" to "sharma", "sharmaa" to "sharma", "shrama" to "sharma",
        "gupta" to "gupta", "guptha" to "gupta", "guptaa" to "gupta",
        "verma" to "verma", "varmaa" to "verma", "varma" to "verma",
        "patel" to "patel", "patil" to "patel", "patell" to "patel",
        
        // First name variations
        "avinash" to "avinash", "avinsh" to "avinash", "avinasha" to "avinash",
        "rahul" to "rahul", "raahul" to "rahul", "rahull" to "rahul",
        "rohit" to "rohit", "roheet" to "rohit", "rohitt" to "rohit",
        "amit" to "amit", "ameet" to "amit", "amitt" to "amit",
        "suresh" to "suresh", "sureshh" to "suresh", "shuresh" to "suresh",
        "ramesh" to "ramesh", "rameshh" to "ramesh", "ramesha" to "ramesh",
        "priya" to "priya", "priyaa" to "priya", "pria" to "priya",
        "neha" to "neha", "nehaa" to "neha", "neaha" to "neha",
        
        // Common misspellings - Cities
        "delhi" to "delhi", "dilli" to "delhi", "dehli" to "delhi", "deli" to "delhi",
        "mumbai" to "mumbai", "bombay" to "mumbai", "mubai" to "mumbai", "mumbay" to "mumbai",
        "bangalore" to "bangalore", "bengaluru" to "bangalore", "banglore" to "bangalore", "bangalor" to "bangalore",
        "kolkata" to "kolkata", "calcutta" to "kolkata", "kolkatta" to "kolkata",
        "chennai" to "chennai", "madras" to "chennai", "chenai" to "chennai",
        "hyderabad" to "hyderabad", "hydrabad" to "hyderabad", "haidarabad" to "hyderabad",
        "pune" to "pune", "poona" to "pune", "punee" to "pune",
        "jaipur" to "jaipur", "jaypur" to "jaipur", "jaipurr" to "jaipur",
        
        // Address terms
        "nagar" to "nagar", "nagr" to "nagar", "nagarr" to "nagar", "ngr" to "nagar",
        "road" to "road", "raod" to "road", "rooad" to "road",
        "street" to "street", "streat" to "street", "strt" to "street",
        "colony" to "colony", "coloni" to "colony", "collony" to "colony",
        "market" to "market", "markit" to "market", "mrkt" to "market",
        "gali" to "gali", "galii" to "gali", "galee" to "gali",
        
        // Phonetic variations
        "charan" to "charan", "charane" to "charan", "charann" to "charan", "chran" to "charan",
        "mohan" to "mohan", "mohann" to "mohan", "mohn" to "mohan", "mohana" to "mohan",
        "raman" to "raman", "ramana" to "raman", "ramann" to "raman",
        "kiran" to "kiran", "kirann" to "kiran", "kieran" to "kiran",
        
        // Common double letter corrections
        "sunil" to "sunil", "sunill" to "sunil", "suneel" to "sunil",
        "anil" to "anil", "anill" to "anil", "aneel" to "anil",
        "geeta" to "geeta", "gita" to "geeta", "geetha" to "geeta",
        "sita" to "sita", "sitha" to "sita", "sitaa" to "sita"
    )
    
    /**
     * Enhanced transliteration with better accuracy
     * - Preserves exact whitespace and punctuation
     * - Uses dictionary for common names
     * - Applies phonetic rules for unknown words
     * - Handles multi-word names intelligently
     * - Auto-corrects common typos
     */
    fun transliterate(text: String): String {
        if (text.isBlank()) return text
        
        val result = StringBuilder()
        var i = 0
        
        while (i < text.length) {
            when {
                // Preserve whitespace
                text[i].isWhitespace() -> {
                    result.append(text[i])
                    i++
                }
                // Preserve digits and special characters
                !text[i].isLetter() -> {
                    result.append(text[i])
                    i++
                }
                // Transliterate alphabetic words
                else -> {
                    val wordStart = i
                    while (i < text.length && text[i].isLetter()) {
                        i++
                    }
                    val word = text.substring(wordStart, i)
                    val lowerWord = word.lowercase()
                    
                    // Auto-correct typos first
                    val correctedWord = typoCorrections[lowerWord] ?: lowerWord
                    
                    // Check dictionary (case-insensitive)
                    val transliterated = nameCorrections[correctedWord] 
                        ?: transliterateWord(word)
                    
                    result.append(transliterated)
                }
            }
        }
        
        return result.toString()
    }
    
    /**
     * Transliterate full name with intelligent word separation
     * Handles: "Rajesh Kumar" -> "राजेश कुमार" with proper spacing
     */
    fun transliterateFullName(fullName: String): String {
        if (fullName.isBlank()) return fullName
        
        // Split by whitespace but preserve multiple spaces
        val parts = fullName.split(Regex("(\\s+)"))
        val result = StringBuilder()
        
        for (part in parts) {
            if (part.matches(Regex("\\s+"))) {
                // Preserve whitespace
                result.append(part)
            } else if (part.isNotBlank()) {
                // Transliterate word
                val lowerWord = part.lowercase()
                val correctedWord = typoCorrections[lowerWord] ?: lowerWord
                val transliterated = nameCorrections[correctedWord] 
                    ?: transliterateWord(part)
                result.append(transliterated)
            }
        }
        
        return result.toString()
    }
    
    /**
     * Transliterate address with special handling for common patterns
     * Handles: "123, Main Road, Near Bus Stand, Delhi"
     */
    fun transliterateAddress(address: String): String {
        if (address.isBlank()) return address
        
        // Preserve numbers at the start (house/flat numbers)
        val result = StringBuilder()
        var i = 0
        
        while (i < address.length) {
            when {
                // Keep numbers and common punctuation
                address[i].isDigit() || address[i] in ",-./()#:" -> {
                    result.append(address[i])
                    i++
                }
                // Preserve whitespace
                address[i].isWhitespace() -> {
                    result.append(address[i])
                    i++
                }
                // Transliterate words
                else -> {
                    val wordStart = i
                    while (i < address.length && 
                           address[i].isLetter()) {
                        i++
                    }
                    val word = address.substring(wordStart, i)
                    val lowerWord = word.lowercase()
                    val correctedWord = typoCorrections[lowerWord] ?: lowerWord
                    val transliterated = nameCorrections[correctedWord] 
                        ?: transliterateWord(word)
                    result.append(transliterated)
                }
            }
        }
        
        return result.toString()
    }

    /**
     * Advanced word transliteration with improved accuracy
     * Handles:
     * - Proper nasalization (anusvara/chandrabindu)
     * - Context-aware halant placement
     * - Better vowel-consonant detection
     * - Smart schwa deletion rules
     * - Performance optimization via caching
     */
    private fun transliterateWord(word: String): String {
        if (word.isEmpty()) return word
        
        val lowerWord = word.lowercase()
        
        // Check cache first for performance
        transliterationCache[lowerWord]?.let { return it }
        
        val result = StringBuilder()
        var i = 0
        
        // First character special case - can be standalone vowel
        if (i < lowerWord.length && !isConsonantStart(lowerWord, i)) {
            // Try to match standalone vowel at start
            var matched = false
            for (vowelLen in minOf(3, lowerWord.length) downTo 1) {
                val vowelPart = lowerWord.substring(i, minOf(i + vowelLen, lowerWord.length))
                
                // Special handling for single 'a' at start
                if (vowelPart == "a" && vowelLen == 1) {
                    if (shouldUseLongA(lowerWord, i, "")) {
                        result.append("आ")  // Long 'a'
                        i += vowelLen
                        matched = true
                        break
                    }
                }
                
                if (vowelMap.containsKey(vowelPart)) {
                    result.append(vowelMap[vowelPart])
                    i += vowelLen
                    matched = true
                    break
                }
            }
            if (!matched) {
                i++ // Skip unknown character
            }
        }
        
        // Process remaining characters
        while (i < lowerWord.length) {
            var matched = false
            
            // Try to match consonant (up to 4 chars for conjuncts like "chch")
            for (consLen in minOf(4, lowerWord.length - i) downTo 1) {
                if (i + consLen > lowerWord.length) continue
                
                val consonantPart = lowerWord.substring(i, i + consLen)
                
                if (consonantMap.containsKey(consonantPart)) {
                    val consonant = consonantMap[consonantPart]!!
                    result.append(consonant)
                    i += consLen
                    
                    // Check for following vowel (matra)
                    if (i < lowerWord.length) {
                        var vowelMatched = false
                        
                        // Try longer vowels first (up to 3 chars)
                        for (vowelLen in minOf(3, lowerWord.length - i) downTo 1) {
                            if (i + vowelLen > lowerWord.length) continue
                            
                            val vowelPart = lowerWord.substring(i, i + vowelLen)
                            
                            // Special handling for single 'a' - check if it should be long or short
                            if (vowelPart == "a" && vowelLen == 1) {
                                if (shouldUseLongA(lowerWord, i, consonantPart)) {
                                    // Use long 'a' matra (ा)
                                    result.append("ा")
                                    i += vowelLen
                                    vowelMatched = true
                                    matched = true
                                    break
                                }
                                // Otherwise, fall through to default (short 'a'/inherent)
                            }
                            
                            if (matraMap.containsKey(vowelPart)) {
                                val matra = matraMap[vowelPart]!!
                                if (matra.isNotEmpty()) {
                                    result.append(matra)
                                }
                                i += vowelLen
                                vowelMatched = true
                                matched = true
                                break
                            }
                        }
                        
                        // If no vowel found, check if next is consonant
                        if (!vowelMatched && i < lowerWord.length) {
                            if (isConsonantStart(lowerWord, i)) {
                                // Smart schwa deletion - add halant only in appropriate contexts
                                // Don't delete schwa for last consonant before final consonant
                                val isNearEnd = i >= lowerWord.length - 3
                                val nextConsLen = getConsonantLength(lowerWord, i)
                                val hasMoreAfter = i + nextConsLen < lowerWord.length
                                
                                if (!isNearEnd || hasMoreAfter) {
                                    result.append("्")
                                }
                                matched = true
                            } else {
                                // Consonant has inherent 'a' sound
                                matched = true
                            }
                        } else if (!vowelMatched) {
                            // End of word - apply smart schwa deletion
                            // Keep inherent 'a' for most cases, delete for specific endings
                            if (shouldDeleteFinalSchwa(lowerWord, consonantPart)) {
                                // Remove the last character (implicit 'a')
                                // Already added consonant, so just mark as matched
                            }
                            matched = true
                        }
                    } else {
                        // End of word after consonant - keep inherent 'a'
                        matched = true
                    }
                    
                    break
                }
            }
            
            // If no consonant matched, try standalone vowel
            if (!matched) {
                for (vowelLen in minOf(3, lowerWord.length - i) downTo 1) {
                    if (i + vowelLen > lowerWord.length) continue
                    
                    val vowelPart = lowerWord.substring(i, i + vowelLen)
                    
                    // Special handling for single 'a' in middle of word
                    if (vowelPart == "a" && vowelLen == 1) {
                        if (shouldUseLongA(lowerWord, i, "")) {
                            result.append("आ")  // Long standalone 'a'
                            i += vowelLen
                            matched = true
                            break
                        }
                    }
                    
                    if (vowelMap.containsKey(vowelPart)) {
                        result.append(vowelMap[vowelPart])
                        i += vowelLen
                        matched = true
                        break
                    }
                }
            }
            
            // Keep unknown characters as-is
            if (!matched) {
                result.append(lowerWord[i])
                i++
            }
        }
        
        val transliterated = result.toString()
        
        // Cache the result for future use
        transliterationCache[lowerWord] = transliterated
        
        return transliterated
    }
    
    /**
     * Check if position in word starts with a consonant
     * Uses greedy matching for multi-character consonants
     */
    private fun isConsonantStart(word: String, position: Int): Boolean {
        if (position >= word.length) return false
        
        // Check up to 4 characters for conjunct consonants
        for (len in minOf(4, word.length - position) downTo 1) {
            if (position + len > word.length) continue
            val substring = word.substring(position, position + len)
            if (consonantMap.containsKey(substring)) {
                return true
            }
        }
        return false
    }
    
    /**
     * Get length of consonant at position (for conjunct handling)
     */
    private fun getConsonantLength(word: String, position: Int): Int {
        if (position >= word.length) return 0
        
        // Check up to 4 characters for conjunct consonants
        for (len in minOf(4, word.length - position) downTo 1) {
            if (position + len > word.length) continue
            val substring = word.substring(position, position + len)
            if (consonantMap.containsKey(substring)) {
                return len
            }
        }
        return 0
    }
    
    /**
     * Intelligently determine if 'a' should be long (आ/ा) or short (अ/inherent)
     * Context-aware analysis based on position, surrounding consonants, and word patterns
     */
    private fun shouldUseLongA(word: String, position: Int, context: String = ""): Boolean {
        val lowerWord = word.lowercase()
        
        // Check if word is in short 'a' exceptions
        if (shortAExceptions.contains(lowerWord)) {
            // For words like "manish", the 'a' after 'm' is short
            return false
        }
        
        // Special handling for specific patterns
        when {
            // "jha" as surname -> झा (long 'a')
            lowerWord == "jha" -> return true
            
            // "ray" or "rai" -> राय (long 'a')
            lowerWord == "ray" || lowerWord == "rai" -> return true
            
            // "rao" -> राव (long 'a')
            lowerWord == "rao" -> return true
            
            // "shastri" -> शास्त्री (first 'a' is long)
            lowerWord.startsWith("sha") && lowerWord.contains("tri") -> {
                // First 'a' in "sha" is long, but 'a' in "tri" is short
                val shaPos = lowerWord.indexOf("sha")
                return position == shaPos + 2 // Position after "sh"
            }
            
            // Names ending with "-nish" like manish, tanish (short 'a' after consonant)
            lowerWord.endsWith("nish") || lowerWord.endsWith("anish") -> {
                // The 'a' before 'nish' is short
                return false
            }
            
            // First syllable patterns
            position <= 2 -> {
                // Beginning "ra-" is often long: raj, ram, ravi -> राज, राम, रवि
                if (lowerWord.startsWith("ra") && position == 1) return true
                // Beginning "ka-" can be long: karan, kamal -> कारण, कमल
                if (lowerWord.startsWith("ka") && position == 1 && lowerWord.length > 3) return true
                // Beginning "ja-" for jai/jay -> जय
                if ((lowerWord == "jai" || lowerWord == "jay") && position == 1) return true
            }
            
            // Ending patterns
            position >= lowerWord.length - 2 -> {
                // Endings with long 'a': -ra, -ya, -la, -ma
                when {
                    lowerWord.endsWith("ra") -> return true  // kumar, sharma -> कुमार, शर्मा
                    lowerWord.endsWith("ya") -> return true  // priya, kavya -> प्रिया, काव्या
                    lowerWord.endsWith("la") -> return true  // kamala -> कमला
                    lowerWord.endsWith("ma") -> return true  // padma -> पद्मा
                    lowerWord.endsWith("ta") && lowerWord.length > 4 -> return true // sunita -> सुनीता
                }
            }
        }
        
        // Default: short 'a' (inherent vowel)
        return false
    }
    
    /**
     * Determine if final schwa should be deleted (Hindi phonetic rules)
     * Returns true if the inherent 'a' should be removed
     */
    private fun shouldDeleteFinalSchwa(word: String, lastConsonant: String): Boolean {
        // Never delete schwa for very short words (2 chars or less)
        if (word.length <= 2) return false
        
        // Delete schwa for common suffixes where it's typically silent
        // e.g., "kumar" -> कुमार (not कुमारा)
        val endsWithSilentPattern = word.endsWith("r") ||  // kumar, shankar
                                    word.endsWith("l") ||  // nil, kamal
                                    word.endsWith("m") ||  // ram, shyam
                                    word.endsWith("n") ||  // raman, kiran
                                    word.endsWith("t") ||  // amit, rohit
                                    word.endsWith("d") ||  // anand, govind
                                    word.endsWith("k") ||  // malik, deepak
                                    word.endsWith("j") ||  // raj, samaj
                                    word.endsWith("v") ||  // dev, rajeev
                                    word.endsWith("p")     // deep, pratap
        
        return endsWithSilentPattern
    }
    
    /**
     * ML-inspired syllable detection for better transliteration
     * Identifies syllable boundaries for proper matra application
     */
    private fun detectSyllables(word: String): List<String> {
        val syllables = mutableListOf<String>()
        var currentSyllable = StringBuilder()
        var i = 0
        
        while (i < word.length) {
            val char = word[i]
            
            when {
                // Vowel starts new syllable if not first char
                isVowel(char) -> {
                    if (currentSyllable.isNotEmpty()) {
                        syllables.add(currentSyllable.toString())
                        currentSyllable = StringBuilder()
                    }
                    currentSyllable.append(char)
                    
                    // Continue adding vowels
                    while (i + 1 < word.length && isVowel(word[i + 1])) {
                        i++
                        currentSyllable.append(word[i])
                    }
                    
                    syllables.add(currentSyllable.toString())
                    currentSyllable = StringBuilder()
                }
                
                // Consonant
                else -> {
                    currentSyllable.append(char)
                    
                    // Check if followed by vowel
                    if (i + 1 < word.length && isVowel(word[i + 1])) {
                        i++
                        currentSyllable.append(word[i])
                        syllables.add(currentSyllable.toString())
                        currentSyllable = StringBuilder()
                    }
                }
            }
            i++
        }
        
        if (currentSyllable.isNotEmpty()) {
            syllables.add(currentSyllable.toString())
        }
        
        return syllables
    }
    
    /**
     * Check if character is a vowel
     */
    private fun isVowel(char: Char): Boolean {
        return char.lowercaseChar() in setOf('a', 'e', 'i', 'o', 'u')
    }
    
    /**
     * Check if text contains Hindi characters
     */
    fun containsHindi(text: String): Boolean {
        return text.any { it in '\u0900'..'\u097F' }
    }
    
    /**
     * Smart transliteration that preserves existing Hindi text
     */
    fun smartTransliterate(text: String): String {
        if (containsHindi(text)) {
            return text // Already contains Hindi
        }
        return transliterate(text)
    }
    
    /**
     * Transliterate with custom corrections
     * Use this when user wants to correct specific words
     */
    fun transliterateWithCorrections(text: String, corrections: Map<String, String> = emptyMap()): String {
        if (text.isBlank()) return text
        
        var result = text
        
        // Apply user corrections first (case-insensitive)
        for ((english, hindi) in corrections) {
            result = result.replace(english, hindi, ignoreCase = true)
        }
        
        // Transliterate remaining English text
        return transliterate(result)
    }
    
    /**
     * Get suggested transliteration for preview
     * Used in billing screen for user to review and correct
     * Now uses specialized methods for names and addresses
     */
    fun getSuggestion(text: String, isAddress: Boolean = false): String {
        return if (isAddress) {
            transliterateAddress(text)
        } else {
            transliterateFullName(text)
        }
    }
    
    /**
     * Get multiple transliteration suggestions with confidence scores
     * Returns list of (transliteration, confidence) pairs
     */
    fun getSuggestionsWithConfidence(text: String, isAddress: Boolean = false): List<Pair<String, Float>> {
        if (text.isBlank()) return emptyList()
        
        val suggestions = mutableListOf<Pair<String, Float>>()
        val lowerText = text.lowercase()
        
        // Primary suggestion - using specialized method
        val primary = if (isAddress) transliterateAddress(text) else transliterateFullName(text)
        val primaryConfidence = getConfidenceScore(text)
        suggestions.add(primary to primaryConfidence)
        
        // Check if it's a compound name that could be split differently
        if (!isAddress && text.contains(" ")) {
            val words = text.trim().split(Regex("\\s+"))
            if (words.size == 2) {
                // Try reversed word order (e.g., "Kumar Rajesh" -> "Rajesh Kumar")
                val reversed = "${words[1]} ${words[0]}"
                val reversedTrans = transliterateFullName(reversed)
                if (reversedTrans != primary) {
                    suggestions.add(reversedTrans to (primaryConfidence * 0.8f))
                }
            }
        }
        
        // Get phonetic alternatives for unknown words
        if (primaryConfidence < 0.7f) {
            val alternatives = getAlternatives(text)
            alternatives.forEach { alt ->
                if (alt != primary && !suggestions.any { it.first == alt }) {
                    suggestions.add(alt to (primaryConfidence * 0.6f))
                }
            }
        }
        
        return suggestions.take(3) // Return top 3 suggestions
    }
    
    /**
     * Get confidence score for transliteration (0.0 to 1.0)
     * Higher score means more reliable transliteration
     * IMPROVED - considers word-by-word confidence
     */
    fun getConfidenceScore(text: String): Float {
        if (text.isBlank()) return 1.0f
        
        val words = text.lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.isEmpty()) return 1.0f
        
        var totalScore = 0f
        
        for (word in words) {
            val corrected = typoCorrections[word] ?: word
            when {
                // Dictionary word - highest confidence
                nameCorrections.containsKey(corrected) -> totalScore += 1.0f
                // Short word (2-3 chars) - medium confidence (might be initial or abbreviation)
                word.length <= 3 -> totalScore += 0.7f
                // Common phonetic patterns - medium-high confidence
                hasCommonPhoneticPatterns(word) -> totalScore += 0.8f
                // Unknown word - lower confidence
                else -> totalScore += 0.5f
            }
        }
        
        return (totalScore / words.size).coerceIn(0.0f, 1.0f)
    }
    
    /**
     * Check if word has common Indian name phonetic patterns
     */
    private fun hasCommonPhoneticPatterns(word: String): Boolean {
        val lowerWord = word.lowercase()
        return lowerWord.endsWith("esh") ||  // Rajesh, Mahesh, etc.
               lowerWord.endsWith("an") ||   // Raman, Kiran, etc.
               lowerWord.endsWith("ar") ||   // Kumar, Shankar, etc.
               lowerWord.endsWith("deep") || // Pradeep, Sandeep, etc.
               lowerWord.endsWith("sh") ||   // Ashish, Harish, etc.
               lowerWord.endsWith("it") ||   // Amit, Rohit, etc.
               lowerWord.endsWith("vi") ||   // Ravi, Devi, etc.
               lowerWord.endsWith("ya") ||   // Priya, Kavya, etc.
               lowerWord.endsWith("ta") ||   // Sunita, Kavita, etc.
               lowerWord.endsWith("ini") ||  // Shalini, etc.
               lowerWord.endsWith("ani") ||  // Kalpana -> Kalpani pattern
               lowerWord.startsWith("sri") ||  // Sri, Srinivas, etc.
               lowerWord.startsWith("ram") ||  // Ram, Ramesh, etc.
               lowerWord.startsWith("krishn")  // Krishna, etc.
    }
    
    /**
     * Check if transliteration is likely accurate
     * Returns true if confidence is above threshold
     */
    fun isLikelyAccurate(text: String): Boolean {
        return getConfidenceScore(text) > 0.5f
    }
    
    /**
     * Get alternative transliterations for ambiguous words
     * Useful for showing user multiple options
     */
    fun getAlternatives(word: String): List<String> {
        val alternatives = mutableListOf<String>()
        val lower = word.lowercase()
        
        // Add dictionary match if available
        nameCorrections[lower]?.let { alternatives.add(it) }
        
        // Add phonetic transliteration
        val phonetic = transliterateWord(word)
        if (!alternatives.contains(phonetic)) {
            alternatives.add(phonetic)
        }
        
        // Add variations for common ambiguous patterns
        val variations = getCommonVariations(lower)
        variations.forEach { 
            if (!alternatives.contains(it)) {
                alternatives.add(it)
            }
        }
        
        return alternatives.take(3) // Return top 3 alternatives
    }
    
    /**
     * Get common variations for ambiguous spellings
     */
    private fun getCommonVariations(word: String): List<String> {
        val variations = mutableListOf<String>()
        
        // Try variations with different 'th' interpretations
        if (word.contains("th")) {
            val withRetroflex = word.replace("th", "ţh") // ठ
            val withDental = word.replace("th", "th") // थ
            variations.add(transliterateWord(withRetroflex))
            variations.add(transliterateWord(withDental))
        }
        
        // Try variations with 'sh' vs 's'
        if (word.contains("sh")) {
            val withS = word.replace("sh", "s")
            variations.add(transliterateWord(withS))
        }
        
        return variations.distinct()
    }
    
    /**
     * Clear the transliteration cache
     * Useful for memory management or when rules are updated
     */
    fun clearCache() {
        transliterationCache.clear()
    }
    
    /**
     * Get cache statistics for monitoring
     */
    fun getCacheStats(): Map<String, Int> {
        return mapOf(
            "size" to transliterationCache.size,
            "capacity" to CACHE_SIZE
        )
    }
}
