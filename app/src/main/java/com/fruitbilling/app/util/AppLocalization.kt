package com.fruitbilling.app.util

import android.content.Context
import androidx.compose.runtime.compositionLocalOf
import java.util.Locale

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

data class AppUiStrings(
    // Bottom Nav
    val navCalc: String,
    val navBilling: String,
    val navHistory: String,
    val navMenu: String,

    // Header Titles
    val calcHeader: String,
    val billingHeader: String,
    val historyHeader: String,
    val menuHeader: String,

    // Actions & POS
    val addToBill: String,
    val clear: String,
    val subtotal: String,
    val total: String,
    val saveBill: String,
    val itemsInBill: String,
    val searchProducts: String,
    val qty: String,
    val rate: String,
    val cash: String,
    val upi: String,
    val pending: String,
    val customerNameOptional: String,
    val tendered: String,
    val balanceToReturn: String,
    val saveAndPrint: String,

    // History & Bin
    val allBills: String,
    val paidBills: String,
    val pendingBills: String,
    val recycleBin: String,
    val restore: String,
    val restoreSuccess: String,
    val deletePermanent: String,
    val emptyBin: String,
    val daysLeft: String,
    val exportPdf: String,
    val exportCsv: String,
    val shareReceipt: String,

    // Menu & Settings
    val shopProfile: String,
    val languageAndCurrency: String,
    val activeLanguage: String,
    val activeCurrency: String,
    val changeLanguage: String,
    val changeCurrency: String,
    val cloudBackup: String,
    val appSurvey: String,
    val termsNotice: String,
    val checkUpdates: String,

    // Common
    val confirm: String,
    val cancel: String,
    val close: String,
    val englishPrimaryBadge: String
)

val LocalAppStrings = compositionLocalOf { AppLocalization.getUiStrings("en") }


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

    fun updateAppLocale(context: Context, languageCode: String) {
        try {
            val locale = Locale(languageCode.lowercase().trim())
            Locale.setDefault(locale)
            val resources = context.resources
            val config = resources.configuration
            config.setLocale(locale)
            resources.updateConfiguration(config, resources.displayMetrics)
        } catch (_: Exception) {}
    }

    fun getUiStrings(langCode: String): AppUiStrings {
        return when (langCode.lowercase().trim()) {
            "ta" -> AppUiStrings(
                navCalc = "கணக்கீடு",
                navBilling = "பில்லிங்",
                navHistory = "வரலாறு",
                navMenu = "மெனு",
                calcHeader = "வேக கணக்கீடு பிஓஎஸ்",
                billingHeader = "பொருட்கள் & பில்லிங்",
                historyHeader = "விற்பனை வரலாறு",
                menuHeader = "மெனு & அமைப்புகள்",
                addToBill = "பில்லில் சேர்",
                clear = "அழி",
                subtotal = "கூட்டுத்தொகை",
                total = "மொத்தம்",
                saveBill = "பில் சேமி",
                itemsInBill = "பொருட்கள்",
                searchProducts = "பொருட்களைத் தேடு...",
                qty = "அளவு",
                rate = "விலை",
                cash = "ரொக்கம்",
                upi = "யுபிஐ (UPI)",
                pending = "கடன் (Pending)",
                customerNameOptional = "வாடிக்கையாளர் பெயர் (விருப்பம்)",
                tendered = "கொடுத்த ரொக்கம்",
                balanceToReturn = "மீதி கொடுக்க வேண்டியது",
                saveAndPrint = "சேமி & அச்சிடு",
                allBills = "அனைத்தும்",
                paidBills = "செலுத்தப்பட்டவை",
                pendingBills = "கடன் பில்கள்",
                recycleBin = "மறுசுழற்சி கூடை (30 நாட்கள்)",
                restore = "மீட்டெடு",
                restoreSuccess = "பில் மீட்கப்பட்டது!",
                deletePermanent = "நிரந்தரமாக நீக்கு",
                emptyBin = "கூடையை காலி செய்",
                daysLeft = "நாட்கள் மீதம்",
                exportPdf = "PDF ஏற்றுமதி",
                exportCsv = "Excel CSV ஏற்றுமதி",
                shareReceipt = "வாட்ஸ்அப் ரசீது",
                shopProfile = "கடை விவரங்கள்",
                languageAndCurrency = "மொழி மற்றும் நாணயம்",
                activeLanguage = "தற்போதைய மொழி",
                activeCurrency = "தற்போதைய நாணயம்",
                changeLanguage = "🌐 மொழியை மாற்று",
                changeCurrency = "💱 நாணயத்தை மாற்று",
                cloudBackup = "கூகிள் கிளவுட் காப்புப்பிரதி",
                appSurvey = "கருத்துக் கணிப்பு & பின்னூட்டம்",
                termsNotice = "விதிமுறைகள் & தனியுரிமை",
                checkUpdates = "புதுப்பிப்புகளைச் சரிபார்",
                confirm = "உறுதிசெய்",
                cancel = "ரத்து",
                close = "மூடு",
                englishPrimaryBadge = "முதன்மையானது / Default"
            )
            "hi" -> AppUiStrings(
                navCalc = "कैलकुलेटर",
                navBilling = "बिलिंग",
                navHistory = "इतिहास",
                navMenu = "मेनू",
                calcHeader = "स्मार्ट कैलकुलेटर पीओएस",
                billingHeader = "उत्पाद एवं बिलिंग",
                historyHeader = "बिक्री इतिहास",
                menuHeader = "मेनू एवं सेटिंग्स",
                addToBill = "बिल में जोड़ें",
                clear = "साफ़ करें",
                subtotal = "उप-योग",
                total = "कुल योग",
                saveBill = "बिल सहेजें",
                itemsInBill = "आइटम",
                searchProducts = "उत्पाद खोजें...",
                qty = "मात्रा",
                rate = "दर",
                cash = "नकद",
                upi = "यूपीआई (UPI)",
                pending = "उधार (Pending)",
                customerNameOptional = "ग्राहक का नाम (वैकल्पिक)",
                tendered = "प्राप्त नकद",
                balanceToReturn = "वापस करने योग्य शेष",
                saveAndPrint = "सहेजें और प्रिंट करें",
                allBills = "सभी",
                paidBills = "भुगतान किया",
                pendingBills = "उधार",
                recycleBin = "रीसायकल बिन (30 दिन)",
                restore = "पुनर्स्थापित करें",
                restoreSuccess = "बिल वापस लाया गया!",
                deletePermanent = "हमेशा के लिए हटाएं",
                emptyBin = "खाली करें",
                daysLeft = "दिन शेष",
                exportPdf = "पीडीएफ निर्यात",
                exportCsv = "सीएसवी निर्यात",
                shareReceipt = "व्हाट्सएप रसीद",
                shopProfile = "दुकान विवरण",
                languageAndCurrency = "भाषा एवं मुद्रा",
                activeLanguage = "सक्रिय भाषा",
                activeCurrency = "सक्रिय मुद्रा",
                changeLanguage = "🌐 भाषा बदलें",
                changeCurrency = "💱 मुद्रा बदलें",
                cloudBackup = "गूगल क्लाउड बैकअप",
                appSurvey = "प्रतिक्रिया एवं सर्वेक्षण",
                termsNotice = "नियम एवं गोपनीयता",
                checkUpdates = "अपडेट जांचें",
                confirm = "पुष्टि करें",
                cancel = "रद्द करें",
                close = "बंद करें",
                englishPrimaryBadge = "प्राथमिक / Default"
            )
            "ml" -> AppUiStrings(
                navCalc = "കാൽക്കുലേറ്റർ",
                navBilling = "ബില്ലിംഗ്",
                navHistory = "ചരിത്രം",
                navMenu = "മെനു",
                calcHeader = "സ്മാർട്ട് കാൽക്കുലേറ്റർ",
                billingHeader = "ഉൽപ്പന്നങ്ങൾ & ബില്ലിംഗ്",
                historyHeader = "വിൽപ്പന ചരിത്രം",
                menuHeader = "മെനു & ക്രമീകരണങ്ങൾ",
                addToBill = "ബില്ലിൽ ചേർക്കുക",
                clear = "മായ്ക്കുക",
                subtotal = "തുക",
                total = "ആകെ തുക",
                saveBill = "ബിൽ സേവ് ചെയ്യുക",
                itemsInBill = "ഇനങ്ങൾ",
                searchProducts = "തിരയുക...",
                qty = "അളവ്",
                rate = "വില",
                cash = "പണം (Cash)",
                upi = "യുപിഐ (UPI)",
                pending = "കടം (Pending)",
                customerNameOptional = "ഉപഭോക്താവിന്റെ പേര്",
                tendered = "ലഭിച്ച തുക",
                balanceToReturn = "ബാക്കി നൽകാൻ",
                saveAndPrint = "സേവ് & പ്രിന്റ്",
                allBills = "എല്ലാം",
                paidBills = "പണമടച്ചത്",
                pendingBills = "ബാക്കി",
                recycleBin = "റീസൈക്കിൾ ബിൻ (30 ദിവസം)",
                restore = "വീണ്ടെടുക്കുക",
                restoreSuccess = "ബിൽ പുനഃസ്ഥാപിച്ചു!",
                deletePermanent = "പൂർണ്ണമായി ഇല്ലാതാക്കുക",
                emptyBin = "ബിൻ ശൂന്യമാക്കുക",
                daysLeft = "ദിവസം ബാക്കി",
                exportPdf = "പിഡിഎഫ് എക്സ്പോർട്ട്",
                exportCsv = "സിഎസ്വി എക്സ്പോർട്ട്",
                shareReceipt = "വാട്ട്‌സ്ആപ്പ് രസീത്",
                shopProfile = "സ്ഥാപന വിവരങ്ങൾ",
                languageAndCurrency = "ഭാഷയും കറൻസിയും",
                activeLanguage = "നിലവിലെ ഭാഷ",
                activeCurrency = "നിലവിലെ കറൻസി",
                changeLanguage = "🌐 ഭാഷ മാറ്റുക",
                changeCurrency = "💱 കറൻസി മാറ്റുക",
                cloudBackup = "ക്ലൗഡ് ബാക്കപ്പ്",
                appSurvey = "അഭിപ്രായങ്ങൾ",
                termsNotice = "നിബന്ധനകൾ",
                checkUpdates = "അപ്ഡേറ്റ് പരിശോധിക്കുക",
                confirm = "സ്ഥിരീകരിക്കുക",
                cancel = "റദ്ദാക്കുക",
                close = "അടയ്ക്കുക",
                englishPrimaryBadge = "പ്രധാനം / Default"
            )
            "te" -> AppUiStrings(
                navCalc = "కాలిక్యులేటర్",
                navBilling = "బిల్లింగ్",
                navHistory = "చరిత్ర",
                navMenu = "మెనూ",
                calcHeader = "స్మార్ట్ కాలిక్యులేటర్ పిఓఎస్",
                billingHeader = "వస్తువులు & బిల్లింగ్",
                historyHeader = "అమ్మకాల చరిత్ర",
                menuHeader = "మెనూ & సెట్టింగులు",
                addToBill = "బిల్లుకు జోడించు",
                clear = "తుడిచివేయి",
                subtotal = "ఉపమొత్తం",
                total = "మొత్తం",
                saveBill = "బిల్లు సేవ్ చేయి",
                itemsInBill = "వస్తువులు",
                searchProducts = "శోధించండి...",
                qty = "పరిమాణం",
                rate = "ధర",
                cash = "నగదు (Cash)",
                upi = "యూపీఐ (UPI)",
                pending = "అప్పు (Pending)",
                customerNameOptional = "కస్టమర్ పేరు",
                tendered = "అందుకున్న నగదు",
                balanceToReturn = "తిరిగి ఇవ్వాల్సిన మొత్తం",
                saveAndPrint = "సేవ్ & ప్రింట్",
                allBills = "అన్నీ",
                paidBills = "చెల్లించినవి",
                pendingBills = "బాకీ",
                recycleBin = "రీసైకిల్ బిన్ (30 రోజులు)",
                restore = "పునరుద్ధరించు",
                restoreSuccess = "బిల్లు పునరుద్ధరించబడింది!",
                deletePermanent = "శాశ్వతంగా తొలగించు",
                emptyBin = "ఖాళీ చేయి",
                daysLeft = "రోజులు మిగిలి ఉన్నాయి",
                exportPdf = "పీడీఎఫ్ ఎగుమతి",
                exportCsv = "సీఎస్వీ ఎగుమతి",
                shareReceipt = "వాట్సాప్ రశీదు",
                shopProfile = "దుకాణం వివరాలు",
                languageAndCurrency = "భాష & కరెన్సీ",
                activeLanguage = "ప్రస్తుత భాష",
                activeCurrency = "ప్రస్తుత కరెన్సీ",
                changeLanguage = "🌐 భాష మార్చండి",
                changeCurrency = "💱 కరెన్సీ మార్చండి",
                cloudBackup = "క్లౌడ్ బ్యాకప్",
                appSurvey = "అభిప్రాయ సర్వే",
                termsNotice = "నిబంధనలు & విధానం",
                checkUpdates = "అప్‌డేట్‌లు చూడండి",
                confirm = "నిర్ధారించు",
                cancel = "రద్దు చేయి",
                close = "మూసివేయి",
                englishPrimaryBadge = "ప్రాథమిక / Default"
            )
            "es" -> AppUiStrings(
                navCalc = "Calculadora",
                navBilling = "Facturación",
                navHistory = "Historial",
                navMenu = "Menú",
                calcHeader = "Calculadora TPV",
                billingHeader = "Catálogo y Venta",
                historyHeader = "Historial de Ventas",
                menuHeader = "Menú y Configuración",
                addToBill = "Añadir a la cuenta",
                clear = "Borrar",
                subtotal = "Subtotal",
                total = "Total",
                saveBill = "Guardar Cuenta",
                itemsInBill = "artículos",
                searchProducts = "Buscar producto...",
                qty = "Cant.",
                rate = "Precio",
                cash = "Efectivo",
                upi = "Tarjeta / Online",
                pending = "Pendiente / Fiado",
                customerNameOptional = "Nombre del Cliente",
                tendered = "Entregado",
                balanceToReturn = "Cambio a devolver",
                saveAndPrint = "Guardar e Imprimir",
                allBills = "Todas",
                paidBills = "Pagadas",
                pendingBills = "Pendientes",
                recycleBin = "Papelera (30 días)",
                restore = "Restaurar",
                restoreSuccess = "Cuenta restaurada con éxito",
                deletePermanent = "Eliminar definitivamente",
                emptyBin = "Vaciar papelera",
                daysLeft = "días restantes",
                exportPdf = "Exportar PDF",
                exportCsv = "Exportar CSV",
                shareReceipt = "Recibo WhatsApp",
                shopProfile = "Datos de la Tienda",
                languageAndCurrency = "Idioma y Moneda",
                activeLanguage = "Idioma Actual",
                activeCurrency = "Moneda Actual",
                changeLanguage = "🌐 Cambiar Idioma",
                changeCurrency = "💱 Cambiar Moneda",
                cloudBackup = "Copia de Seguridad Google",
                appSurvey = "Encuesta y Opinión",
                termsNotice = "Términos y Privacidad",
                checkUpdates = "Buscar Actualizaciones",
                confirm = "Confirmar",
                cancel = "Cancelar",
                close = "Cerrar",
                englishPrimaryBadge = "Principal / Predeterminado"
            )
            "ar" -> AppUiStrings(
                navCalc = "الحاسبة",
                navBilling = "الفواتير",
                navHistory = "السجل",
                navMenu = "القائمة",
                calcHeader = "كاشير الحاسبة السريعة",
                billingHeader = "المنتجات والفواتير",
                historyHeader = "سجل المبيعات",
                menuHeader = "القائمة والإعدادات",
                addToBill = "إضافة للفاتورة",
                clear = "مسح",
                subtotal = "المجموع الفرعي",
                total = "الإجمالي",
                saveBill = "حفظ الفاتورة",
                itemsInBill = "أصناف",
                searchProducts = "البحث عن صنف...",
                qty = "الكمية",
                rate = "السعر",
                cash = "نقداً",
                upi = "شبكة / إلكتروني",
                pending = "آجل (معلق)",
                customerNameOptional = "اسم العميل (اختياري)",
                tendered = "المبلغ المدفوع",
                balanceToReturn = "المتبقي للعميل",
                saveAndPrint = "حفظ وطباعة",
                allBills = "الكل",
                paidBills = "مدفوعة",
                pendingBills = "آجلة",
                recycleBin = "سلة المحذوفات (٣٠ يوماً)",
                restore = "استعادة",
                restoreSuccess = "تمت استعادة الفاتورة!",
                deletePermanent = "حذف نهائي",
                emptyBin = "إفراغ السلة",
                daysLeft = "أيام متبقية",
                exportPdf = "تصدير PDF",
                exportCsv = "تصدير CSV",
                shareReceipt = "إيصال واتساب",
                shopProfile = "بيانات المحل",
                languageAndCurrency = "اللغة والعملة",
                activeLanguage = "اللغة الحالية",
                activeCurrency = "العملة الحالية",
                changeLanguage = "🌐 تغيير اللغة",
                changeCurrency = "💱 تغيير العملة",
                cloudBackup = "نسخ احتياطي سحابي",
                appSurvey = "استطلاع الرأي والملاحظات",
                termsNotice = "الشروط والخصوصية",
                checkUpdates = "التحقق من التحديثات",
                confirm = "تأكيد",
                cancel = "إلغاء",
                close = "إغلاق",
                englishPrimaryBadge = "الأساسية / الافتراضية"
            )
            "fr" -> AppUiStrings(
                navCalc = "Calculatrice",
                navBilling = "Facturation",
                navHistory = "Historique",
                navMenu = "Menu",
                calcHeader = "Calculatrice Caisse",
                billingHeader = "Catalogue & Ventes",
                historyHeader = "Historique des Ventes",
                menuHeader = "Menu & Paramètres",
                addToBill = "Ajouter au ticket",
                clear = "Effacer",
                subtotal = "Sous-total",
                total = "Total",
                saveBill = "Enregistrer Ticket",
                itemsInBill = "articles",
                searchProducts = "Rechercher...",
                qty = "Qté",
                rate = "Prix",
                cash = "Espèces",
                upi = "Carte / Virtuel",
                pending = "En attente / Crédit",
                customerNameOptional = "Nom du client (optionnel)",
                tendered = "Reçu",
                balanceToReturn = "Monnaie à rendre",
                saveAndPrint = "Enregistrer & Imprimer",
                allBills = "Tous",
                paidBills = "Payés",
                pendingBills = "En attente",
                recycleBin = "Corbeille (30 jours)",
                restore = "Restaurer",
                restoreSuccess = "Ticket restauré avec succès !",
                deletePermanent = "Supprimer définitivement",
                emptyBin = "Vider la corbeille",
                daysLeft = "jours restants",
                exportPdf = "Exporter PDF",
                exportCsv = "Exporter CSV",
                shareReceipt = "Ticket WhatsApp",
                shopProfile = "Détails du Magasin",
                languageAndCurrency = "Langue & Devise",
                activeLanguage = "Langue Active",
                activeCurrency = "Devise Active",
                changeLanguage = "🌐 Changer de Langue",
                changeCurrency = "💱 Changer de Devise",
                cloudBackup = "Sauvegarde Cloud Google",
                appSurvey = "Avis & Enquête",
                termsNotice = "Conditions & Confidentialité",
                checkUpdates = "Vérifier Mises à Jour",
                confirm = "Confirmer",
                cancel = "Annuler",
                close = "Fermer",
                englishPrimaryBadge = "Primaire / Défaut"
            )
            else -> AppUiStrings(
                navCalc = "Calc",
                navBilling = "Billing",
                navHistory = "History",
                navMenu = "Menu",
                calcHeader = "Smart Calc POS",
                billingHeader = "Catalog & Billing",
                historyHeader = "Sales History",
                menuHeader = "Menu & Settings",
                addToBill = "Add to Bill",
                clear = "Clear",
                subtotal = "Subtotal",
                total = "Total",
                saveBill = "Save Bill",
                itemsInBill = "items in bill",
                searchProducts = "Search products...",
                qty = "Qty",
                rate = "Rate",
                cash = "Cash",
                upi = "UPI",
                pending = "Pending / Due",
                customerNameOptional = "Customer Name (optional)",
                tendered = "Tendered",
                balanceToReturn = "Change to Return",
                saveAndPrint = "Save & Print",
                allBills = "All",
                paidBills = "Paid",
                pendingBills = "Pending",
                recycleBin = "Recycle Bin (30 Days)",
                restore = "Restore",
                restoreSuccess = "Bill restored successfully!",
                deletePermanent = "Delete Permanently",
                emptyBin = "Empty Bin",
                daysLeft = "days left",
                exportPdf = "Export PDF",
                exportCsv = "Export CSV",
                shareReceipt = "WhatsApp Receipt",
                shopProfile = "Shop Details",
                languageAndCurrency = "Language & Currency",
                activeLanguage = "Active Language",
                activeCurrency = "Active Currency",
                changeLanguage = "🌐 Change Language",
                changeCurrency = "💱 Change Currency",
                cloudBackup = "Google Cloud Backup",
                appSurvey = "App Survey & Feedback",
                termsNotice = "Terms of Service & Privacy",
                checkUpdates = "Check for Updates",
                confirm = "Confirm",
                cancel = "Cancel",
                close = "Close",
                englishPrimaryBadge = "Primary / Default"
            )
        }
    }
}

