/**
 * Fruit & Grocery Quick Billing POS - Interactive Web POS & Simulator
 * Multi-Platform Hub, Multi-Currency & Multi-Language Engine
 */

// Register PWA Service Worker
if ('serviceWorker' in navigator) {
  window.addEventListener('load', () => {
    navigator.serviceWorker.register('./sw.js').catch(err => {
      console.log('SW registration failed:', err);
    });
  });
}

let deferredPrompt;
const btnInstallPwa = document.getElementById('btnInstallPwa');
window.addEventListener('beforeinstallprompt', (e) => {
  e.preventDefault();
  deferredPrompt = e;
  if (btnInstallPwa) {
    btnInstallPwa.style.display = 'inline-flex';
  }
});

if (btnInstallPwa) {
  btnInstallPwa.addEventListener('click', async () => {
    if (deferredPrompt) {
      deferredPrompt.prompt();
      const { outcome } = await deferredPrompt.userChoice;
      console.log(`PWA install outcome: ${outcome}`);
      deferredPrompt = null;
    } else {
      alert("💡 To install this app on your PC (Windows, Mac, or Linux):\n\n1. In Chrome / Edge / Brave, click the 'Install' icon (🖥️ or ⬇️) in the address bar.\n2. Or click the 3-dots menu -> 'Install Fruit POS'.\n\nIt will run as a standalone desktop app!");
    }
  });
}

// Multi-Currency & Multi-Language State
let currentCurrencySymbol = '₹';
let currentCurrencyCode = 'INR';
let currentLanguage = 'en';

const QUICK_PRODUCTS = [
  { emoji: '🍎', name: 'Apple (Kashmir)', basePrice: 180, unit: 'kg' },
  { emoji: '🍌', name: 'Banana (Robusta)', basePrice: 60, unit: 'doz' },
  { emoji: '🥭', name: 'Mango (Alphonso)', basePrice: 150, unit: 'kg' },
  { emoji: '🍊', name: 'Orange (Nagpur)', basePrice: 90, unit: 'kg' },
  { emoji: '🥔', name: 'Potato (Jyoti)', basePrice: 35, unit: 'kg' },
  { emoji: '🧅', name: 'Onion (Nashik)', basePrice: 40, unit: 'kg' },
  { emoji: '🍅', name: 'Tomato (Local)', basePrice: 30, unit: 'kg' },
  { emoji: '🍇', name: 'Grapes (Seedless)', basePrice: 120, unit: 'kg' }
];

const LOCALIZED_TERMS = {
  en: {
    title: "Fruit Billing POS — Terms & Agreement",
    intro: "Welcome to Fruit Billing POS. By using this point-of-sale application, you agree to the following merchant terms and operational guidelines:",
    sections: [
      { h: "1. 100% Offline POS Architecture", b: "All daily sales, bill calculations, pricing data, customer phone numbers, and catalogs are stored locally on your device in a secure SQLite/IndexedDB database without requiring constant internet." },
      { h: "2. Cloud Sync & Multi-Platform Support", b: "Supports mobile and desktop PC terminals with Google Cloud backup and cross-platform export." },
      { h: "3. Merchant Responsibility for Billing & Pricing", b: "You hold sole discretion and responsibility for managing unit prices, entering scale weights, applying round-offs or manual discounts, and collecting payments." },
      { h: "4. Data Ownership & Privacy", b: "You retain 100% ownership of your business transactions. You may export records to Excel or PDF at any time." }
    ],
    accept: "I Understand & Agree"
  },
  ta: {
    title: "பில்லிங் பிஓஎஸ் — விதிமுறைகள் மற்றும் ஒப்பந்தம்",
    intro: "பில்லிங் பிஓஎஸ் செயலியைப் பயன்படுத்தியதற்கு நன்றி. இந்த செயலியைப் பயன்படுத்துவதன் மூலம், பின்வரும் வணிக விதிமுறைகளை ஏற்கிறீர்கள்:",
    sections: [
      { h: "1. ஆஃப்லைன் பிஓஎஸ் கட்டமைப்பு", b: "இந்த செயலி முழுமையாக ஆஃப்லைனில் செயல்படும் வேகமான காசாளர் முனையம் ஆகும். அனைத்து விற்பனை கணக்குகள், விலை விவரங்கள் மற்றும் பில்கள் உங்கள் சாதனத்தில் மட்டுமே பாதுகாப்பாக சேமிக்கப்படும்." },
      { h: "2. கிளவுட் காப்புப்பிரதி & பல சாதனங்கள்", b: "மொபைல் மற்றும் கணினியில் எளிதாக இயங்கும். கூகிள் டிரைவ் மூலம் பாதுகாப்பான காப்புப்பிரதி எடுக்கலாம்." },
      { h: "3. வணிகரின் பொறுப்பு", b: "பொருட்களின் விலை நிர்ணயம், எடை சரிபார்ப்பு, தள்ளுபடி மற்றும் பணம் வசூலிப்பது முழுவதும் வணிகரின் நேரடி பொறுப்பாகும்." },
      { h: "4. தரவு உரிமை & ரகசியத்தன்மை", b: "உங்கள் வணிகக் கணக்குகள் 100% உங்களுக்கே சொந்தமானது. எந்த நேரத்திலும் எக்செல் அல்லது பிடிஎப் ஆக பதிவிறக்கலாம்." }
    ],
    accept: "விதிமுறைகளை ஏற்கிறேன்"
  },
  hi: {
    title: "बिलिंग पीओएस — नियम एवं शर्तें",
    intro: "बिलिंग पीओएस में आपका स्वागत है। इस एप्लिकेशन का उपयोग करके, आप निम्नलिखित व्यापारिक शर्तों से सहमत होते हैं:",
    sections: [
      { h: "1. ऑफ़लाइन पीओएस सिस्टम", b: "यह एप्लिकेशन पूरी तरह से ऑफ़लाइन काम करने वाला हाई-स्पीड कैशियर टर्मिनल है। आपका सभी बिक्री डेटा और बिल आपके डिवाइस पर सुरक्षित रूप से संग्रहीत रहते हैं।" },
      { h: "2. क्लाउड बैकअप और बहु-उपकरण सहायता", b: "मोबाइल और डेस्कटॉप पीसी दोनों पर काम करता है। Google Drive बैकअप से डेटा सुरक्षित रहता है।" },
      { h: "3. व्यापारी की जिम्मेदारी", b: "मूल्य निर्धारण, वजन प्रविष्टि, छूट और भुगतान संग्रह की पूरी जिम्मेदारी व्यापारी की है।" },
      { h: "4. डेटा स्वामित्व", b: "आपके डेटा पर 100% आपका अधिकार है। आप कभी भी रिपोर्ट एक्सेल या पीडीएफ में डाउनलोड कर सकते हैं।" }
    ],
    accept: "शर्तें स्वीकार करें"
  },
  ml: {
    title: "ബില്ലിംഗ് പി‌ഒ‌എസ് — നിബന്ധനകളും വ്യവസ്ഥകളും",
    intro: "ബില്ലിംഗ് പി‌ഒ‌എസിലേക്ക് സ്വാഗതം. ഈ ആപ്ലിക്കേഷൻ ഉപയോഗിക്കുന്നതിലൂടെ, നിങ്ങൾ ഇനിപ്പറയുന്ന നിബന്ധനകൾ അംഗീകരിക്കുന്നു:",
    sections: [
      { h: "1. ഓഫ്‌ലൈൻ ആർക്കിടെക്ചർ", b: "ഇന്റർനെറ്റ് ഇല്ലാതെ പ്രവർത്തിക്കുന്ന അതിവേഗ ബില്ലിംഗ് സംവിധാനമാണിത്. എല്ലാ ഡാറ്റയും നിങ്ങളുടെ ഫോണിൽ സുരക്ഷിതമായി സൂക്ഷിക്കുന്നു." },
      { h: "2. ഡാറ്റ സുരക്ഷ", b: "നിങ്ങളുടെ ബിസിനസ്സ് ഇടപാടുകൾ 100% നിങ്ങളുടേത് മാത്രമാണ്." }
    ],
    accept: "നിബന്ധനകൾ സ്വീകരിക്കുന്നു"
  },
  te: {
    title: "బిల్లింగ్ పిఓఎస్ — నిబంధనలు మరియు ఒప్పందం",
    intro: "బిల్లింగ్ పిఓఎస్ కు స్వాగతం. ఈ యాప్‌ను ఉపయోగించడం ద్వారా, మీరు కింది నిబంధనలకు అంగీకరిస్తున్నారు:",
    sections: [
      { h: "1. ఆఫ్‌లైన్ పిఓఎస్ విధానం", b: "ఇంటర్నెట్ లేకుండా పూర్తి వేగంతో పనిచేసే నగదు బిల్లింగ్ టెర్మినల్. డేటా అంతా మీ పరికరంలోనే భద్రంగా ఉంటుంది." },
      { h: "2. డేటా యాజమాన్యం", b: "మీ వ్యాపార లెక్కలు 100% మీకే చెందుతాయి." }
    ],
    accept: "నిబంధనలను అంగీకరిస్తున్నాను"
  },
  es: {
    title: "Fruit Billing POS — Términos y Condiciones",
    intro: "Bienvenido a Fruit Billing POS. Al utilizar esta aplicación de punto de venta, acepta las siguientes condiciones operativas:",
    sections: [
      { h: "1. Arquitectura Fuera de Línea", b: "Funciona como un terminal de caja de alta velocidad sin conexión. Todos los registros y cálculos se guardan localmente en su dispositivo." },
      { h: "2. Propiedad de Datos", b: "Usted posee el 100% de sus datos comerciales en todo momento." }
    ],
    accept: "Aceptar Términos"
  },
  ar: {
    title: "نقاط البيع السريعة — الشروط والأحكام",
    intro: "مرحبًا بك في تطبيق نقاط البيع. باستخدام هذا التطبيق، فإنك توافق على الشروط التشغيلية التالية:",
    sections: [
      { h: "١. نظام يعمل دون اتصال بالإنترنت", b: "يعمل كجهاز كاشير سريع دون الحاجة لشبكة الإنترنت، مع حفظ آمن للبيانات في جهازك." },
      { h: "٢. خصوصية وسرية البيانات", b: "بياناتك وسجلات مبيعاتك ملك لك بنسبة ١٠٠٪." }
    ],
    accept: "أوافق على الشروط"
  },
  fr: {
    title: "Fruit Billing POS — Conditions d'Utilisation",
    intro: "Bienvenue sur Fruit Billing POS. En utilisant cette application, vous acceptez les conditions générales suivantes :",
    sections: [
      { h: "1. POS 100% Hors Ligne", b: "Fonctionne ultra-rapidement sans connexion Internet. Vos données sont stockées localement de manière sécurisée." },
      { h: "2. Confidentialité des Données", b: "Vous êtes le propriétaire exclusif de l'ensemble de vos données de caisse." }
    ],
    accept: "Accepter les Conditions"
  }
};

let currentSelectedProduct = QUICK_PRODUCTS[0];
let currentBillItems = [];
let currentPaymentMode = 'CASH';
let billCounter = 1001;

// DOM Elements
const quickItemsRow = document.getElementById('quickItemsRow');
const simCalcInput = document.getElementById('simCalcInput');
const simClearInputBtn = document.getElementById('simClearInputBtn');
const simAddItemBtn = document.getElementById('simAddItemBtn');
const simCartList = document.getElementById('simCartList');
const simEmptyCart = document.getElementById('simEmptyCart');
const simItemCount = document.getElementById('simItemCount');
const simTotalAmount = document.getElementById('simTotalAmount');
const simSaveBillBtn = document.getElementById('simSaveBillBtn');
const simResetBillBtn = document.getElementById('simResetBillBtn');
const simRoundingRow = document.getElementById('simRoundingRow');
const simRoundingChips = document.getElementById('simRoundingChips');
const simCustomerName = document.getElementById('simCustomerName');
const payTabs = document.querySelectorAll('.pay-tab');
const multChips = document.querySelectorAll('.mult-chip');

// Global Selectors
const webLangSelect = document.getElementById('webLangSelect');
const webCurrencySelect = document.getElementById('webCurrencySelect');
const btnViewTerms = document.getElementById('btnViewTerms');
const termsModal = document.getElementById('termsModal');
const termsModalTitle = document.getElementById('termsModalTitle');
const termsModalBody = document.getElementById('termsModalBody');
const btnCloseTermsModal = document.getElementById('btnCloseTermsModal');
const btnAcceptTermsModal = document.getElementById('btnAcceptTermsModal');

// Modal Elements
const simReceiptModal = document.getElementById('simReceiptModal');
const simCloseModalBtn = document.getElementById('simCloseModalBtn');
const simShareReceiptBtn = document.getElementById('simShareReceiptBtn');
const receiptItems = document.getElementById('receiptItems');
const receiptItemCount = document.getElementById('receiptItemCount');
const receiptTotal = document.getElementById('receiptTotal');
const receiptPayment = document.getElementById('receiptPayment');
const receiptCustomerRow = document.getElementById('receiptCustomerRow');
const receiptCustomerName = document.getElementById('receiptCustomerName');
const receiptMeta = document.getElementById('receiptMeta');

// Platform Selector Tabs
const platTabs = document.querySelectorAll('.plat-tab');
const downloadCards = document.querySelectorAll('.download-card');

platTabs.forEach(tab => {
  tab.addEventListener('click', () => {
    platTabs.forEach(t => t.classList.remove('active'));
    tab.classList.add('active');
    const target = tab.dataset.target;
    downloadCards.forEach(card => {
      if (target === 'plat-all' || card.dataset.plat === target) {
        card.style.display = 'flex';
      } else {
        card.style.display = 'none';
      }
    });
  });
});

// Currency Switcher Handler
webCurrencySelect.addEventListener('change', () => {
  const selectedOpt = webCurrencySelect.options[webCurrencySelect.selectedIndex];
  currentCurrencyCode = webCurrencySelect.value;
  currentCurrencySymbol = selectedOpt.dataset.sym || '₹';
  renderQuickProducts();
  updateCalcInputFromProduct(currentSelectedProduct, 1);
  renderCart();
});

// Language Switcher Handler
webLangSelect.addEventListener('change', () => {
  currentLanguage = webLangSelect.value;
  updateLanguageUI();
});

function updateLanguageUI() {
  const t = LOCALIZED_TERMS[currentLanguage] || LOCALIZED_TERMS.en;
  if (currentLanguage === 'ta') {
    document.getElementById('simTitle').textContent = "பில்லிங் பிஓஎஸ்";
    document.getElementById('shelfLabel').textContent = "பழங்கள் & காய்கறிகள்";
    document.getElementById('simCartLabel').childNodes[0].nodeValue = "பில் விவரம் ";
  } else if (currentLanguage === 'hi') {
    document.getElementById('simTitle').textContent = "बिलिंग पीओएस";
    document.getElementById('shelfLabel').textContent = "फल और सब्जियां";
    document.getElementById('simCartLabel').childNodes[0].nodeValue = "बिल सूची ";
  } else {
    document.getElementById('simTitle').textContent = "Quick Billing POS";
    document.getElementById('shelfLabel').textContent = "Quick Fruits & Veggies";
    document.getElementById('simCartLabel').childNodes[0].nodeValue = "Items in Current Bill ";
  }
}

// Terms Modal Handlers
btnViewTerms.addEventListener('click', () => {
  const t = LOCALIZED_TERMS[currentLanguage] || LOCALIZED_TERMS.en;
  termsModalTitle.textContent = t.title;
  let bodyHtml = `<p>${t.intro}</p>`;
  t.sections.forEach(sec => {
    bodyHtml += `<h4>${sec.h}</h4><p>${sec.b}</p>`;
  });
  termsModalBody.innerHTML = bodyHtml;
  btnAcceptTermsModal.textContent = t.accept;
  termsModal.classList.add('active');
});

btnCloseTermsModal.addEventListener('click', () => {
  termsModal.classList.remove('active');
});

btnAcceptTermsModal.addEventListener('click', () => {
  termsModal.classList.remove('active');
});

termsModal.addEventListener('click', (e) => {
  if (e.target === termsModal) termsModal.classList.remove('active');
});

// Render Quick Product Chips
function renderQuickProducts() {
  quickItemsRow.innerHTML = '';
  QUICK_PRODUCTS.forEach((prod, idx) => {
    const chip = document.createElement('div');
    chip.className = `quick-item-chip ${prod.name === currentSelectedProduct.name ? 'selected' : ''}`;
    chip.innerHTML = `
      <span class="emoji">${prod.emoji}</span>
      <div class="info">
        <div class="name">${prod.name}</div>
        <div class="price">${currentCurrencySymbol}${prod.basePrice}/${prod.unit}</div>
      </div>
    `;
    chip.addEventListener('click', () => {
      document.querySelectorAll('.quick-item-chip').forEach(c => c.classList.remove('selected'));
      chip.classList.add('selected');
      currentSelectedProduct = prod;
      updateCalcInputFromProduct(prod, 1);
    });
    quickItemsRow.appendChild(chip);
  });
}

function updateCalcInputFromProduct(prod, qty = 1) {
  if (qty < 1) {
    const grams = Math.round(qty * 1000);
    simCalcInput.value = `${prod.name} = ${prod.basePrice} * ${grams}g`;
  } else {
    simCalcInput.value = `${prod.name} = ${prod.basePrice} * ${qty}`;
  }
}

// Multipliers
multChips.forEach(chip => {
  chip.addEventListener('click', () => {
    const qty = parseFloat(chip.dataset.qty);
    updateCalcInputFromProduct(currentSelectedProduct, qty);
  });
});

simClearInputBtn.addEventListener('click', () => {
  simCalcInput.value = '';
});

// Add Item
simAddItemBtn.addEventListener('click', () => {
  let val = simCalcInput.value.trim();
  if (!val) {
    val = `${currentSelectedProduct.name} = ${currentSelectedProduct.basePrice} * 1`;
  }

  let name = currentSelectedProduct.name;
  let price = currentSelectedProduct.basePrice;
  let qtyText = '1';
  let amount = price;

  if (val.includes('*')) {
    const parts = val.split('=');
    if (parts.length > 1) {
      name = parts[0].trim();
      const mathPart = parts[1].trim();
      const mathTokens = mathPart.split('*');
      price = parseFloat(mathTokens[0]) || price;
      const secondToken = mathTokens[1]?.trim() || '1';
      if (secondToken.toLowerCase().endsWith('g')) {
        const grams = parseFloat(secondToken) || 500;
        amount = Math.round((price * (grams / 1000)) * 100) / 100;
        qtyText = `${grams}g`;
      } else {
        const units = parseFloat(secondToken) || 1;
        amount = Math.round((price * units) * 100) / 100;
        qtyText = `${units}`;
      }
    }
  }

  currentBillItems.push({
    id: Date.now() + Math.random(),
    name: name,
    calc: `${price} × ${qtyText}`,
    amount: amount
  });

  renderCart();
  simCalcInput.value = '';
});

// Render Cart
function renderCart() {
  if (currentBillItems.length === 0) {
    simCartList.innerHTML = '';
    simCartList.appendChild(simEmptyCart);
    simEmptyCart.style.display = 'block';
    simItemCount.textContent = '0';
    simTotalAmount.textContent = `${currentCurrencySymbol}0.00`;
    simSaveBillBtn.disabled = true;
    simRoundingRow.style.display = 'none';
    return;
  }

  simEmptyCart.style.display = 'none';
  simCartList.innerHTML = '';

  let total = 0;
  currentBillItems.forEach(item => {
    total += item.amount;
    const row = document.createElement('div');
    row.className = 'cart-item-row';
    row.innerHTML = `
      <div class="details">
        <span class="name">${item.name}</span>
        <span class="calc">${item.calc}</span>
      </div>
      <div class="right-actions">
        <span class="amt">${currentCurrencySymbol}${item.amount.toFixed(2)}</span>
        <button class="cart-item-del-btn" title="Remove">✕</button>
      </div>
    `;
    row.querySelector('.cart-item-del-btn').addEventListener('click', () => {
      currentBillItems = currentBillItems.filter(i => i.id !== item.id);
      renderCart();
    });
    simCartList.appendChild(row);
  });

  simItemCount.textContent = currentBillItems.length.toString();
  simTotalAmount.textContent = `${currentCurrencySymbol}${total.toFixed(2)}`;
  simSaveBillBtn.disabled = false;

  generateRoundingSuggestions(total);
}

function generateRoundingSuggestions(total) {
  const roundedInt = Math.round(total);
  if (roundedInt < 20) {
    simRoundingRow.style.display = 'none';
    return;
  }

  const suggestions = new Set();
  const floor10 = Math.floor(total / 10) * 10;
  if (floor10 > 0 && floor10 < total) suggestions.add(floor10);

  if (total >= 100) {
    const floor50 = Math.floor(total / 50) * 50;
    if (floor50 > 0 && floor50 < total) suggestions.add(floor50);
  }

  if (suggestions.size > 0) {
    simRoundingChips.innerHTML = '';
    suggestions.forEach(suggestedVal => {
      const chip = document.createElement('span');
      chip.className = 'rounding-chip';
      chip.textContent = `${currentCurrencySymbol}${suggestedVal}`;
      chip.title = `Apply discounted final price ${currentCurrencySymbol}${suggestedVal}`;
      chip.addEventListener('click', () => {
        simTotalAmount.textContent = `${currentCurrencySymbol}${suggestedVal.toFixed(2)}`;
        chip.style.borderColor = '#10b981';
      });
      simRoundingChips.appendChild(chip);
    });
    simRoundingRow.style.display = 'flex';
  } else {
    simRoundingRow.style.display = 'none';
  }
}

simResetBillBtn.addEventListener('click', () => {
  currentBillItems = [];
  renderCart();
});

payTabs.forEach(tab => {
  tab.addEventListener('click', () => {
    payTabs.forEach(t => t.classList.remove('active'));
    tab.classList.add('active');
    currentPaymentMode = tab.dataset.mode;
    if (currentPaymentMode === 'PENDING') {
      simCustomerName.placeholder = 'Customer Name / Phone (Required for Pending)';
      simCustomerName.focus();
    } else {
      simCustomerName.placeholder = 'Customer Name / Phone (Optional)';
    }
  });
});

simSaveBillBtn.addEventListener('click', () => {
  if (currentBillItems.length === 0) return;

  const totalStr = simTotalAmount.textContent;
  const custName = simCustomerName.value.trim();

  receiptMeta.textContent = `#${billCounter++} • Just now`;
  receiptItemCount.textContent = currentBillItems.length.toString();
  receiptTotal.textContent = totalStr;
  receiptPayment.textContent = currentPaymentMode === 'PENDING' ? 'PENDING (KHATA)' : currentPaymentMode;

  if (custName) {
    receiptCustomerName.textContent = custName;
    receiptCustomerRow.style.display = 'flex';
  } else {
    receiptCustomerRow.style.display = 'none';
  }

  receiptItems.innerHTML = '';
  currentBillItems.forEach(item => {
    const itemRow = document.createElement('div');
    itemRow.className = 'receipt-item-row';
    itemRow.innerHTML = `
      <span>${item.name} (${item.calc})</span>
      <span>${currentCurrencySymbol}${item.amount.toFixed(2)}</span>
    `;
    receiptItems.appendChild(itemRow);
  });

  simReceiptModal.classList.add('active');
});

simCloseModalBtn.addEventListener('click', () => {
  simReceiptModal.classList.remove('active');
  currentBillItems = [];
  simCustomerName.value = '';
  renderCart();
});

simShareReceiptBtn.addEventListener('click', () => {
  const cust = simCustomerName.value.trim() || 'Valued Customer';
  alert(`✨ Receipt Created for ${cust}!\n\n🧾 FRESH MART & FRUITS\nTotal: ${simTotalAmount.textContent}\nCurrency: ${currentCurrencyCode}\nPayment: ${currentPaymentMode}\nThank you for shopping with us!`);
  simReceiptModal.classList.remove('active');
  currentBillItems = [];
  simCustomerName.value = '';
  renderCart();
});

simReceiptModal.addEventListener('click', (e) => {
  if (e.target === simReceiptModal) {
    simReceiptModal.classList.remove('active');
  }
});

// PC Download Placeholder Helpers
['btnWinDownload', 'btnMacDownload', 'btnLinuxDownload'].forEach(id => {
  const btn = document.getElementById(id);
  if (btn) {
    btn.addEventListener('click', (e) => {
      // If release asset is not yet packaged on GitHub, guide user to Web POS or PWA
      console.log(`Requested download: ${id}`);
    });
  }
});

// Initialize on Load
document.addEventListener('DOMContentLoaded', () => {
  renderQuickProducts();
  updateCalcInputFromProduct(QUICK_PRODUCTS[0], 1);
});
