package com.fruitbilling.app.util

data class LanguageItem(
    val code: String,
    val displayName: String,
    val nativeName: String,
    val flagEmoji: String
)

data class CurrencyItem(
    val code: String,
    val symbol: String,
    val name: String,
    val country: String
)

data class TermsContent(
    val title: String,
    val intro: String,
    val sections: List<Pair<String, String>>,
    val acceptButton: String,
    val closeButton: String
)

data class OnboardingContent(
    val welcomeTitle: String,
    val welcomeSubtitle: String,
    val feature1: String,
    val feature2: String,
    val feature3: String,
    val googleSignIn: String,
    val continueOffline: String,
    val agreementNotice: String,
    val termsLink: String,
    val privacyLink: String
)

object AppLocalization {

    val SUPPORTED_LANGUAGES = listOf(
        LanguageItem("en", "English", "English", "🇬🇧"),
        LanguageItem("ta", "Tamil", "தமிழ்", "🇮🇳"),
        LanguageItem("hi", "Hindi", "हिंदी", "🇮🇳"),
        LanguageItem("ml", "Malayalam", "മലയാളം", "🇮🇳"),
        LanguageItem("te", "Telugu", "తెలుగు", "🇮🇳"),
        LanguageItem("es", "Spanish", "Español", "🇪🇸"),
        LanguageItem("ar", "Arabic", "العربية", "🇸🇦"),
        LanguageItem("fr", "French", "Français", "🇫🇷")
    )

    val SUPPORTED_CURRENCIES = listOf(
        CurrencyItem("INR", "₹", "Indian Rupee", "India 🇮🇳"),
        CurrencyItem("USD", "$", "US Dollar", "United States 🇺🇸"),
        CurrencyItem("EUR", "€", "Euro", "European Union 🇪🇺"),
        CurrencyItem("GBP", "£", "British Pound", "United Kingdom 🇬🇧"),
        CurrencyItem("AED", "د.إ", "UAE Dirham", "United Arab Emirates 🇦🇪"),
        CurrencyItem("SAR", "SR", "Saudi Riyal", "Saudi Arabia 🇸🇦"),
        CurrencyItem("BDT", "৳", "Bangladeshi Taka", "Bangladesh 🇧🇩"),
        CurrencyItem("PKR", "₨", "Pakistani Rupee", "Pakistan 🇵🇰"),
        CurrencyItem("LKR", "Rs", "Sri Lankan Rupee", "Sri Lanka 🇱🇰"),
        CurrencyItem("MYR", "RM", "Malaysian Ringgit", "Malaysia 🇲🇾"),
        CurrencyItem("SGD", "S$", "Singapore Dollar", "Singapore 🇸🇬"),
        CurrencyItem("CAD", "C$", "Canadian Dollar", "Canada 🇨🇦"),
        CurrencyItem("AUD", "A$", "Australian Dollar", "Australia 🇦🇺")
    )

    fun getTerms(langCode: String): TermsContent {
        return when (langCode) {
            "ta" -> TermsContent(
                title = "பில்லிங் பிஓஎஸ் — விதிமுறைகள் மற்றும் ஒப்பந்தம்",
                intro = "பில்லிங் பிஓஎஸ் செயலியைப் பயன்படுத்தியதற்கு நன்றி. இந்த செயலியைப் பயன்படுத்துவதன் மூலம், பின்வரும் வணிக விதிமுறைகளை ஏற்கிறீர்கள்:",
                sections = listOf(
                    "1. ஆஃப்லைன் பிஓஎஸ் கட்டமைப்பு" to "இந்த செயலி முழுமையாக ஆஃப்லைனில் செயல்படும் வேகமான காசாளர் முனையம் ஆகும். அனைத்து விற்பனை கணக்குகள், விலை விவரங்கள் மற்றும் பில்கள் உங்கள் சாதனத்தில் உள்ள பாதுகாப்பான SQLite தரவுத்தளத்தில் மட்டுமே சேமிக்கப்படும்.",
                    "2. கூகிள் கிளவுட் காப்புப்பிரதி" to "கூகிள் கணக்கு மூலம் தானியங்கி காப்புப்பிரதி எடுத்து மற்றொரு சாதனத்தில் தரவை எளிதாக மீட்டெடுக்கலாம்.",
                    "3. வணிகரின் பொறுப்பு" to "பொருட்களின் விலை நிர்ணயம், எடை சரிபார்ப்பு, தள்ளுபடி மற்றும் பணம் வசூலிப்பது முழுவதும் வணிகரின் நேரடி பொறுப்பாகும்.",
                    "4. தரவு உரிமை & ரகசியத்தன்மை" to "உங்கள் பரிவர்த்தனைகள் 100% உங்களுக்கே சொந்தமானது. எந்த நேரத்திலும் எக்செல்/பிடிஎப் ஆக ஏற்றுமதி செய்யலாம் அல்லது நீக்கலாம்."
                ),
                acceptButton = "விதிமுறைகளை ஏற்கிறேன்",
                closeButton = "மூடு"
            )
            "hi" -> TermsContent(
                title = "बिलिंग पीओएस — नियम एवं शर्तें",
                intro = "बिलिंग पीओएस में आपका स्वागत है। इस एप्लिकेशन का उपयोग करके, आप निम्नलिखित व्यापारिक शर्तों से सहमत होते हैं:",
                sections = listOf(
                    "1. ऑफ़लाइन पीओएस सिस्टम" to "यह एप्लिकेशन पूरी तरह से ऑफ़लाइन काम करने वाला हाई-स्पीड कैशियर टर्मिनल है। आपका सभी बिक्री डेटा और बिल आपके डिवाइस पर सुरक्षित रूप से संग्रहीत रहते हैं।",
                    "2. गूगल क्लाउड बैकअप" to "गूगल साइन-इन के जरिए आप अपने डेटा का सुरक्षित बैकअप ले सकते हैं और डिवाइस बदलने पर रीस्टोर कर सकते हैं।",
                    "3. व्यापारी की जिम्मेदारी" to "मूल्य निर्धारण, वजन प्रविष्टि, छूट और भुगतान संग्रह की पूरी जिम्मेदारी व्यापारी की है।",
                    "4. डेटा स्वामित्व" to "आपके डेटा पर 100% आपका अधिकार है। आप कभी भी रिपोर्ट एक्सेल या पीडीएफ में डाउनलोड कर सकते हैं।"
                ),
                acceptButton = "शर्तें स्वीकार करें",
                closeButton = "बंद करें"
            )
            "ml" -> TermsContent(
                title = "ബില്ലിംഗ് പി‌ഒ‌എസ് — നിബന്ധനകളും വ്യവസ്ഥകളും",
                intro = "ബില്ലിംഗ് പി‌ഒ‌എസിലേക്ക് സ്വാഗതം. ഈ ആപ്ലിക്കേഷൻ ഉപയോഗിക്കുന്നതിലൂടെ, നിങ്ങൾ ഇനിപ്പറയുന്ന നിബന്ധനകൾ അംഗീകരിക്കുന്നു:",
                sections = listOf(
                    "1. ഓഫ്‌ലൈൻ ആർക്കിടെക്ചർ" to "ഇന്റർനെറ്റ് ഇല്ലാതെ പ്രവർത്തിക്കുന്ന അതിവേഗ ബില്ലിംഗ് സംവിധാനമാണിത്. എല്ലാ ഡാറ്റയും നിങ്ങളുടെ ഫോണിൽ സുരക്ഷിതമായി സൂക്ഷിക്കുന്നു.",
                    "2. ബാക്കപ്പ് & സുരക്ഷ" to "ഗൂഗിൾ അക്കൗണ്ട് വഴി ആവശ്യമെങ്കിൽ ക്ലൗഡ് ബാക്കപ്പ് എടുക്കാനും പുനഃസ്ഥാപിക്കാനും കഴിയും.",
                    "3. വ്യാപാരിയുടെ ഉത്തരവാദിത്തം" to "ഉൽ‌പ്പന്ന വിലകളും ബിൽ തുകയും പൂർണ്ണമായും വ്യാപാരിയുടെ നിയന്ത്രണത്തിലാണ്.",
                    "4. ഡാറ്റ ഉടമസ്ഥത" to "നിങ്ങളുടെ ബിസിനസ്സ് ഇടപാടുകൾ 100% നിങ്ങളുടേത് മാത്രമാണ്."
                ),
                acceptButton = "നിബന്ധനകൾ സ്വീകരിക്കുന്നു",
                closeButton = "അടയ്ക്കുക"
            )
            "te" -> TermsContent(
                title = "బిల్లింగ్ పిఓఎస్ — నిబంధనలు మరియు ఒప్పందం",
                intro = "బిల్లింగ్ పిఓఎస్ కు స్వాగతం. ఈ యాప్‌ను ఉపయోగించడం ద్వారా, మీరు కింది నిబంధనలకు అంగీకరిస్తున్నారు:",
                sections = listOf(
                    "1. ఆఫ్‌లైన్ పిఓఎస్ విధానం" to "ఇంటర్నెట్ లేకుండా పూర్తి వేగంతో పనిచేసే నగదు బిల్లింగ్ టెర్మినల్. డేటా అంతా మీ పరికరంలోనే భద్రంగా ఉంటుంది.",
                    "2. గూగుల్ క్లౌడ్ బ్యాకప్" to "మీ డేటాను గూగుల్ క్లౌడ్ ద్వారా సురక్షితంగా బ్యాకప్ చేసుకోవచ్చు.",
                    "3. వ్యాపారి బాధ్యత" to "ధరల నిర్ణయం మరియు బిల్లుల నిర్వహణ పూర్తిగా వ్యాపారి బాధ్యత.",
                    "4. డేటా యాజమాన్యం" to "మీ వ్యాపార లెక్కలు 100% మీకే చెందుతాయి."
                ),
                acceptButton = "నిబంధనలను అంగీకరిస్తున్నాను",
                closeButton = "మూసివేయి"
            )
            "es" -> TermsContent(
                title = "Fruit Billing POS — Términos y Condiciones",
                intro = "Bienvenido a Fruit Billing POS. Al utilizar esta aplicación de punto de venta, acepta las siguientes condiciones operativas:",
                sections = listOf(
                    "1. Arquitectura 100% Fuera de Línea" to "Funciona como un terminal de caja de alta velocidad sin conexión. Todos los registros y cálculos se guardan localmente en su dispositivo.",
                    "2. Sincronización en la Nube Opcional" to "Puede realizar copias de seguridad automáticas en Google Drive para restaurar en otros dispositivos.",
                    "3. Responsabilidad Comercial" to "El comerciante es el único responsable de fijar precios, pesos y cobros.",
                    "4. Propiedad de Datos" to "Usted posee el 100% de sus datos comerciales en todo momento."
                ),
                acceptButton = "Aceptar Términos",
                closeButton = "Cerrar"
            )
            "ar" -> TermsContent(
                title = "نقاط البيع السريعة — الشروط والأحكام",
                intro = "مرحبًا بك في تطبيق نقاط البيع. باستخدام هذا التطبيق، فإنك توافق على الشروط التشغيلية التالية:",
                sections = listOf(
                    "١. نظام يعمل دون اتصال بالإنترنت" to "يعمل كجهاز كاشير سريع دون الحاجة لشبكة الإنترنت، مع حفظ آمن للبيانات في هاتفك.",
                    "٢. النسخ الاحتياطي السحابي" to "إمكانية أخذ نسخ احتياطية واستعادتها بأمان عبر حساب Google.",
                    "٣. مسؤولية التاجر" to "تحديد الأسعار والأوزان والتحصيل هي مسؤولية التاجر بالكامل.",
                    "٤. ملكية البيانات والخصوصية" to "بياناتك وسجلات مبيعاتك ملك لك بنسبة ١٠٠٪."
                ),
                acceptButton = "أوافق على الشروط",
                closeButton = "إغلاق"
            )
            "fr" -> TermsContent(
                title = "Fruit Billing POS — Conditions d'Utilisation",
                intro = "Bienvenue sur Fruit Billing POS. En utilisant cette application, vous acceptez les conditions générales suivantes :",
                sections = listOf(
                    "1. POS 100% Hors Ligne" to "Fonctionne ultra-rapidement sans connexion Internet. Vos données sont stockées localement de manière sécurisée.",
                    "2. Sauvegarde Cloud Sécurisée" to "Sauvegardez vos ventes sur Google Drive pour restaurer facilement sur un nouvel appareil.",
                    "3. Responsabilité du Commerçant" to "Le commerçant assume l'entière responsabilité des prix, pesées et encaissements.",
                    "4. Confidentialité des Données" to "Vous êtes le propriétaire exclusif de l'ensemble de vos données de caisse."
                ),
                acceptButton = "Accepter les Conditions",
                closeButton = "Fermer"
            )
            else -> TermsContent(
                title = "Fruit Billing POS — Terms & Agreement",
                intro = "Welcome to Fruit Billing POS. By using this point-of-sale application, you agree to the following merchant terms and operational guidelines:",
                sections = listOf(
                    "1. Offline-First POS Architecture" to "Fruit Billing POS operates as a high-speed, offline-first cashier terminal. All daily sales, bill calculations, pricing data, customer phone numbers, and fruit catalogs are stored locally on your device in a secure SQLite database. The POS operates fully without an active internet connection.",
                    "2. Google Cloud Sync & Multi-Account Isolation" to "Google Account sign-in enables automatic, secure Google Cloud synchronization so your business data can be restored across device reinstalls. Each Google account functions as an isolated store workspace.",
                    "3. Merchant Responsibility for Billing & Pricing" to "You as the merchant/cashier hold sole discretion and responsibility for managing fruit unit prices (per kg, piece, or box), entering scale weights, applying round-offs or manual discounts, collecting payments (Cash / UPI), and issuing receipts to customers.",
                    "4. Data Ownership & Portability" to "You retain 100% ownership of your business transactions. You may export your sales history to CSV spreadsheets, generate PDF invoices, share receipts via WhatsApp, or delete records at any time."
                ),
                acceptButton = "Accept & Agree",
                closeButton = "Close"
            )
        }
    }

    fun getOnboarding(langCode: String): OnboardingContent {
        return when (langCode) {
            "ta" -> OnboardingContent(
                welcomeTitle = "பில்லிங் பிஓஎஸ்",
                welcomeSubtitle = "சில்லறை கடைகள் மற்றும் பழக்கடைகளுக்கான அதிவேக பில்லிங் செயலி",
                feature1 = "⚡ 100% ஆஃப்லைன் கணக்கீட்டு பில்லிங்",
                feature2 = "🤝 கடன் (Pending) மற்றும் ரொக்க கணக்குகள்",
                feature3 = "🖨️ உடனடி தெர்மல் & வாட்ஸ்அப் ரசீதுகள்",
                googleSignIn = "Google மூலம் உள்நுழைக",
                continueOffline = "ஆஃப்லைனில் தொடரவும்",
                agreementNotice = "தொடர்வதன் மூலம், நீங்கள் எங்கள்",
                termsLink = "விதிமுறைகள் & நிபந்தனைகள்",
                privacyLink = "தனியுரிமைக் கொள்கை"
            )
            "hi" -> OnboardingContent(
                welcomeTitle = "बिलिंग पीओएस",
                welcomeSubtitle = "किराना, फल और खुदरा दुकानों के लिए सुपरफास्ट बिलिंग ऐप",
                feature1 = "⚡ 100% ऑफ़लाइन कैलकुलेटर बिलिंग",
                feature2 = "🤝 उधार (Pending) और नकद प्रबंधन",
                feature3 = "🖨️ त्वरित थर्मल व व्हाट्सएप रसीद",
                googleSignIn = "Google से साइन इन करें",
                continueOffline = "ऑफ़लाइन शुरू करें",
                agreementNotice = "आगे बढ़कर, आप सहमत होते हैं",
                termsLink = "नियम एवं शर्तें",
                privacyLink = "गोपनीयता नीति"
            )
            "ml" -> OnboardingContent(
                welcomeTitle = "ബില്ലിംഗ് പി‌ഒ‌എസ്",
                welcomeSubtitle = "കടകൾക്കുള്ള അതിവേഗ ഓഫ്‌ലൈൻ ബില്ലിംഗ് ആപ്പ്",
                feature1 = "⚡ 100% ഓഫ്‌ലൈൻ കാൽക്കുലേറ്റർ സ്പീഡ്",
                feature2 = "🤝 കടം (Pending) & ക്യാഷ് മാനേജ്‌മെന്റ്",
                feature3 = "🖨️ തെർമൽ പ്രിന്റിംഗ് & വാട്ട്‌സ്ആപ്പ്",
                googleSignIn = "Google ഉപയോഗിച്ച് പ്രവേശിക്കുക",
                continueOffline = "ഓഫ്‌ലൈനായി തുടരുക",
                agreementNotice = "തുടരുന്നതിലൂടെ, നിങ്ങൾ അംഗീകരിക്കുന്നു",
                termsLink = "നിബന്ധനകൾ",
                privacyLink = "സ്വകാര്യതാ നയം"
            )
            "te" -> OnboardingContent(
                welcomeTitle = "బిల్లింగ్ పిఓఎస్",
                welcomeSubtitle = "రిటైల్ మరియు పండ్ల దుకాణాల కోసం వేగవంతమైన బిల్లింగ్",
                feature1 = "⚡ 100% ఆఫ్‌లైన్ కాలిక్యులేటర్ బిల్లింగ్",
                feature2 = "🤝 అప్పు (Pending) & నగదు లెక్కలు",
                feature3 = "🖨️ థర్మల్ & వాట్సాప్ రశీదులు",
                googleSignIn = "Google తో సైన్ ఇన్ చేయండి",
                continueOffline = "ఆఫ్‌లైన్‌లో ప్రారంభించండి",
                agreementNotice = "కొనసాగించడం ద్వారా, మీరు అంగీకరిస్తున్నారు",
                termsLink = "నిబంధనలు",
                privacyLink = "గోప్యతా విధానం"
            )
            "es" -> OnboardingContent(
                welcomeTitle = "Fruit Billing POS",
                welcomeSubtitle = "TPV ultrarrápido y fuera de línea para tiendas y fruterías",
                feature1 = "⚡ Facturación rápida estilo calculadora 100% offline",
                feature2 = "🤝 Control de pagos pendientes (fiado) y efectivo",
                feature3 = "🖨️ Impresión térmica Bluetooth y recibos WhatsApp",
                googleSignIn = "Iniciar sesión con Google",
                continueOffline = "Continuar sin conexión",
                agreementNotice = "Al continuar, usted acepta los",
                termsLink = "Términos y Condiciones",
                privacyLink = "Política de Privacidad"
            )
            "ar" -> OnboardingContent(
                welcomeTitle = "نقاط البيع السريعة",
                welcomeSubtitle = "تطبيق فواتير فائق السرعة وبدون إنترنت لمحلات الخضار والفواكه",
                feature1 = "⚡ فواتير سريعة بنظام الآلة الحاسبة ١٠٠٪ دون إنترنت",
                feature2 = "🤝 تتبع الديون (الآجل) والمدفوعات النقدية والشبكة",
                feature3 = "🖨️ طباعة إيصالات حرارية ومشاركة فورية عبر واتساب",
                googleSignIn = "تسجيل الدخول عبر Google",
                continueOffline = "المتابعة دون اتصال",
                agreementNotice = "بالمتابعة، فإنك توافق على",
                termsLink = "الشروط والأحكام",
                privacyLink = "سياسة الخصوصية"
            )
            "fr" -> OnboardingContent(
                welcomeTitle = "Fruit Billing POS",
                welcomeSubtitle = "Caisse enregistreuse rapide et hors ligne pour commerces",
                feature1 = "⚡ Saisie ultra-rapide style calculatrice 100% hors ligne",
                feature2 = "🤝 Suivi des arriérés (crédit) et paiements espèces",
                feature3 = "🖨️ Impression thermique et tickets WhatsApp",
                googleSignIn = "Connexion avec Google",
                continueOffline = "Continuer hors ligne",
                agreementNotice = "En continuant, vous acceptez nos",
                termsLink = "Conditions Générales",
                privacyLink = "Politique de Confidentialité"
            )
            else -> OnboardingContent(
                welcomeTitle = "Fruit Billing",
                welcomeSubtitle = "Fast, offline-first POS for fruit stalls & market vendors",
                feature1 = "⚡ Calculator-speed item entry (e.g. 200 × 500g)",
                feature2 = "🤝 Credit & pending payments tracker with customer names",
                feature3 = "🖨️ 1-tap thermal Bluetooth printing & WhatsApp receipts",
                googleSignIn = "Sign in with Google",
                continueOffline = "Start Billing Offline",
                agreementNotice = "By continuing, you agree to our",
                termsLink = "Terms & Conditions",
                privacyLink = "Privacy Policy"
            )
        }
    }
}
