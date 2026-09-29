/**
 * Fruit & Grocery POS - Windows Desktop Application Logic
 */

// Clock
function updateClock() {
  const now = new Date();
  document.getElementById('topClock').textContent = now.toLocaleTimeString();
}
setInterval(updateClock, 1000);
updateClock();

// Products Catalog
const CATALOG_ITEMS = [
  { id: 1, emoji: '🍎', name: 'Apple (Kashmir)', basePrice: 180, unit: 'kg' },
  { id: 2, emoji: '🍌', name: 'Banana (Robusta)', basePrice: 60, unit: 'doz' },
  { id: 3, emoji: '🥭', name: 'Mango (Alphonso)', basePrice: 150, unit: 'kg' },
  { id: 4, emoji: '🍊', name: 'Orange (Nagpur)', basePrice: 90, unit: 'kg' },
  { id: 5, emoji: '🥔', name: 'Potato (Jyoti)', basePrice: 35, unit: 'kg' },
  { id: 6, emoji: '🧅', name: 'Onion (Nashik)', basePrice: 40, unit: 'kg' },
  { id: 7, emoji: '🍅', name: 'Tomato (Local)', basePrice: 30, unit: 'kg' },
  { id: 8, emoji: '🍇', name: 'Grapes (Seedless)', basePrice: 120, unit: 'kg' },
  { id: 9, emoji: '🥕', name: 'Carrot (Ooty)', basePrice: 50, unit: 'kg' },
  { id: 10, emoji: '🍉', name: 'Watermelon', basePrice: 25, unit: 'kg' },
  { id: 11, emoji: '🥥', name: 'Coconut (Large)', basePrice: 40, unit: 'pc' },
  { id: 12, emoji: '🍋', name: 'Lemon (Fresh)', basePrice: 10, unit: 'pc' }
];

let activeCurrencySymbol = '₹';
let activeCurrencyCode = 'INR';
let activeLanguage = 'en';

let currentBillItems = [];
let currentPaymentMode = 'CASH';
let billSequence = 1001;

// Sales Records (for Z-Report)
let completedSales = [];
let recycleBin = [];

// DOM Elements
const productsGrid = document.getElementById('productsGrid');
const calcInput = document.getElementById('calcInput');
const btnClearInput = document.getElementById('btnClearInput');
const btnAddItem = document.getElementById('btnAddItem');
const billItemsBody = document.getElementById('billItemsBody');
const emptyCartRow = document.getElementById('emptyCartRow');
const totalItemsCount = document.getElementById('totalItemsCount');
const subTotalDisplay = document.getElementById('subTotalDisplay');
const grandTotalDisplay = document.getElementById('grandTotalDisplay');
const btnCompleteAndPrint = document.getElementById('btnCompleteAndPrint');
const btnResetCart = document.getElementById('btnResetCart');
const roundingBar = document.getElementById('roundingBar');
const roundingChips = document.getElementById('roundingChips');
const customerNameInput = document.getElementById('customerNameInput');
const currencySelect = document.getElementById('currencySelect');
const languageSelect = document.getElementById('languageSelect');
const payOptions = document.querySelectorAll('.pay-option');
const multChips = document.querySelectorAll('.mult-chip');

// Modals
const receiptModal = document.getElementById('receiptModal');
const btnCloseReceipt = document.getElementById('btnCloseReceipt');
const btnPrintPhysical = document.getElementById('btnPrintPhysical');
const zReportModal = document.getElementById('zReportModal');
const btnZReport = document.getElementById('btnZReport');
const btnCloseZReport = document.getElementById('btnCloseZReport');
const btnDismissZ = document.getElementById('btnDismissZ');
const btnShareZReport = document.getElementById('btnShareZReport');
const drawerCashInput = document.getElementById('drawerCashInput');
const varianceResult = document.getElementById('varianceResult');
const recycleBinModal = document.getElementById('recycleBinModal');
const btnRecycleBin = document.getElementById('btnRecycleBin');
const btnCloseRecycle = document.getElementById('btnCloseRecycle');
const btnDismissRecycle = document.getElementById('btnDismissRecycle');
const deletedBillsList = document.getElementById('deletedBillsList');

// Render Catalog
let selectedCatalogProduct = CATALOG_ITEMS[0];

function renderCatalog() {
  productsGrid.innerHTML = '';
  CATALOG_ITEMS.forEach(prod => {
    const tile = document.createElement('div');
    tile.className = `product-tile ${prod.id === selectedCatalogProduct.id ? 'selected' : ''}`;
    tile.innerHTML = `
      <span class="emoji">${prod.emoji}</span>
      <span class="name">${prod.name}</span>
      <span class="price">${activeCurrencySymbol}${prod.basePrice}/${prod.unit}</span>
    `;
    tile.addEventListener('click', () => {
      document.querySelectorAll('.product-tile').forEach(t => t.classList.remove('selected'));
      tile.classList.add('selected');
      selectedCatalogProduct = prod;
      calcInput.value = `${prod.name} = ${prod.basePrice} * 1`;
      calcInput.focus();
    });
    productsGrid.appendChild(tile);
  });
}

// Multipliers
multChips.forEach(chip => {
  chip.addEventListener('click', () => {
    const val = parseFloat(chip.dataset.val);
    if (val < 1) {
      const g = Math.round(val * 1000);
      calcInput.value = `${selectedCatalogProduct.name} = ${selectedCatalogProduct.basePrice} * ${g}g`;
    } else {
      calcInput.value = `${selectedCatalogProduct.name} = ${selectedCatalogProduct.basePrice} * ${val}`;
    }
    calcInput.focus();
  });
});

// Clear input
btnClearInput.addEventListener('click', () => {
  calcInput.value = '';
  calcInput.focus();
});

// Add Item
function addItemToCart() {
  let text = calcInput.value.trim();
  if (!text) {
    text = `${selectedCatalogProduct.name} = ${selectedCatalogProduct.basePrice} * 1`;
  }

  let name = selectedCatalogProduct.name;
  let price = selectedCatalogProduct.basePrice;
  let qtyText = '1';
  let amount = price;

  if (text.includes('*')) {
    const parts = text.split('=');
    if (parts.length > 1) {
      name = parts[0].trim();
      const mathPart = parts[1].trim();
      const tokens = mathPart.split('*');
      price = parseFloat(tokens[0]) || price;
      const second = tokens[1]?.trim() || '1';
      if (second.toLowerCase().endsWith('g')) {
        const grams = parseFloat(second) || 500;
        amount = Math.round((price * (grams / 1000)) * 100) / 100;
        qtyText = `${grams}g`;
      } else {
        const units = parseFloat(second) || 1;
        amount = Math.round((price * units) * 100) / 100;
        qtyText = `${units}`;
      }
    }
  }

  currentBillItems.push({
    id: Date.now() + Math.random(),
    name: name,
    calc: `${price} × ${qtyText}`,
    qtyText: qtyText,
    amount: amount
  });

  renderBillTable();
  calcInput.value = '';
  calcInput.focus();
}

btnAddItem.addEventListener('click', addItemToCart);

// Keyboard Shortcuts: Enter to Add, F2 to Complete, Esc to Clear
window.addEventListener('keydown', (e) => {
  if (e.key === 'Enter') {
    if (document.activeElement === calcInput) {
      e.preventDefault();
      addItemToCart();
    }
  } else if (e.key === 'F2') {
    e.preventDefault();
    if (currentBillItems.length > 0) {
      completeAndShowReceipt();
    }
  } else if (e.key === 'Escape') {
    if (receiptModal.classList.contains('active')) {
      receiptModal.classList.remove('active');
    } else if (zReportModal.classList.contains('active')) {
      zReportModal.classList.remove('active');
    } else if (recycleBinModal.classList.contains('active')) {
      recycleBinModal.classList.remove('active');
    } else {
      calcInput.value = '';
    }
  }
});

// Render Bill Table
function renderBillTable() {
  if (currentBillItems.length === 0) {
    billItemsBody.innerHTML = '';
    billItemsBody.appendChild(emptyCartRow);
    totalItemsCount.textContent = '0';
    subTotalDisplay.textContent = `${activeCurrencySymbol}0.00`;
    grandTotalDisplay.textContent = `${activeCurrencySymbol}0.00`;
    btnCompleteAndPrint.disabled = true;
    roundingBar.style.display = 'none';
    return;
  }

  billItemsBody.innerHTML = '';
  let subtotal = 0;

  currentBillItems.forEach(item => {
    subtotal += item.amount;
    const row = document.createElement('tr');
    row.innerHTML = `
      <td>
        <strong>${item.name}</strong><br>
        <span style="font-size:0.75rem; color:var(--text-dim);">${item.calc}</span>
      </td>
      <td style="text-align: center;">${item.qtyText}</td>
      <td style="text-align: right; font-weight: bold; color: #34d399;">${activeCurrencySymbol}${item.amount.toFixed(2)}</td>
      <td style="text-align: center;">
        <button class="item-del-btn" title="Delete">✕</button>
      </td>
    `;
    row.querySelector('.item-del-btn').addEventListener('click', () => {
      currentBillItems = currentBillItems.filter(i => i.id !== item.id);
      renderBillTable();
    });
    billItemsBody.appendChild(row);
  });

  totalItemsCount.textContent = currentBillItems.length.toString();
  subTotalDisplay.textContent = `${activeCurrencySymbol}${subtotal.toFixed(2)}`;
  grandTotalDisplay.textContent = `${activeCurrencySymbol}${subtotal.toFixed(2)}`;
  btnCompleteAndPrint.disabled = false;

  generateRoundingChips(subtotal);
}

function generateRoundingChips(total) {
  const roundedInt = Math.round(total);
  if (roundedInt < 20) {
    roundingBar.style.display = 'none';
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
    roundingChips.innerHTML = '';
    suggestions.forEach(val => {
      const chip = document.createElement('span');
      chip.className = 'rounding-chip-btn';
      chip.textContent = `${activeCurrencySymbol}${val}`;
      chip.title = `Apply discounted price ${activeCurrencySymbol}${val}`;
      chip.addEventListener('click', () => {
        grandTotalDisplay.textContent = `${activeCurrencySymbol}${val.toFixed(2)}`;
      });
      roundingChips.appendChild(chip);
    });
    roundingBar.style.display = 'flex';
  } else {
    roundingBar.style.display = 'none';
  }
}

// Payment Selection
payOptions.forEach(opt => {
  opt.addEventListener('click', () => {
    payOptions.forEach(o => o.classList.remove('active'));
    opt.classList.add('active');
    const radio = opt.querySelector('input');
    radio.checked = true;
    currentPaymentMode = radio.value;
    if (currentPaymentMode === 'PENDING') {
      customerNameInput.placeholder = 'Customer Name / Phone (Required for Pending Khata)';
      customerNameInput.focus();
    } else {
      customerNameInput.placeholder = 'Customer Name / Mobile (Optional)';
    }
  });
});

// Discard Bill
btnResetCart.addEventListener('click', () => {
  if (currentBillItems.length === 0) return;
  if (confirm("Discard current bill?")) {
    currentBillItems = [];
    customerNameInput.value = '';
    renderBillTable();
  }
});

// Complete and Show Receipt
function completeAndShowReceipt() {
  if (currentBillItems.length === 0) return;

  const totalStr = grandTotalDisplay.textContent;
  const custName = customerNameInput.value.trim();

  const billRecord = {
    id: Date.now(),
    billNumber: billSequence++,
    items: [...currentBillItems],
    totalStr: totalStr,
    totalVal: parseFloat(totalStr.replace(activeCurrencySymbol, '')) || 0,
    paymentMode: currentPaymentMode,
    customerName: custName,
    time: new Date().toLocaleTimeString(),
    date: new Date().toLocaleDateString()
  };

  completedSales.push(billRecord);

  // Fill receipt modal
  document.getElementById('receiptDateTime').textContent = `#${billRecord.billNumber} • ${billRecord.date} ${billRecord.time}`;
  document.getElementById('rItemCount').textContent = billRecord.items.length.toString();
  document.getElementById('rTotalAmt').textContent = billRecord.totalStr;
  document.getElementById('rPayMode').textContent = billRecord.paymentMode;

  const rCustRow = document.getElementById('rCustRow');
  if (custName) {
    document.getElementById('rCustName').textContent = custName;
    rCustRow.style.display = 'flex';
  } else {
    rCustRow.style.display = 'none';
  }

  const receiptList = document.getElementById('receiptList');
  receiptList.innerHTML = '';
  billRecord.items.forEach(it => {
    const rRow = document.createElement('div');
    rRow.style.display = 'flex';
    rRow.style.justifyContent = 'space-between';
    rRow.style.margin = '4px 0';
    rRow.innerHTML = `
      <span>${it.name} (${it.calc})</span>
      <span>${activeCurrencySymbol}${it.amount.toFixed(2)}</span>
    `;
    receiptList.appendChild(rRow);
  });

  receiptModal.classList.add('active');

  // Reset Bill
  currentBillItems = [];
  customerNameInput.value = '';
  renderBillTable();
  document.getElementById('billOrderMeta').textContent = `Bill #${billSequence} • Counter 1`;
}

btnCompleteAndPrint.addEventListener('click', completeAndShowReceipt);

// Close Receipt Modal
btnCloseReceipt.addEventListener('click', () => {
  receiptModal.classList.remove('active');
});

// Physical Thermal Print
btnPrintPhysical.addEventListener('click', () => {
  if (window.electronAPI && window.electronAPI.printReceipt) {
    window.electronAPI.printReceipt();
  } else {
    window.print();
  }
  receiptModal.classList.remove('active');
});

// Currency Switcher
currencySelect.addEventListener('change', () => {
  const opt = currencySelect.options[currencySelect.selectedIndex];
  activeCurrencyCode = currencySelect.value;
  activeCurrencySymbol = opt.dataset.sym || '₹';
  renderCatalog();
  renderBillTable();
});

// Language Switcher
languageSelect.addEventListener('change', () => {
  activeLanguage = languageSelect.value;
  if (activeLanguage === 'ta') {
    document.getElementById('shelfTitle').textContent = "பொருட்கள் பட்டியல்";
  } else if (activeLanguage === 'hi') {
    document.getElementById('shelfTitle').textContent = "उत्पाद सूची";
  } else {
    document.getElementById('shelfTitle').textContent = "Quick Select Catalog";
  }
});

// Z-Report Modal
btnZReport.addEventListener('click', () => {
  let cashTotal = 0;
  let upiTotal = 0;
  let pendingTotal = 0;

  completedSales.forEach(s => {
    if (s.paymentMode === 'CASH') cashTotal += s.totalVal;
    else if (s.paymentMode === 'UPI') upiTotal += s.totalVal;
    else if (s.paymentMode === 'PENDING') pendingTotal += s.totalVal;
  });

  const totalRev = cashTotal + upiTotal + pendingTotal;

  document.getElementById('zCashSales').textContent = `${activeCurrencySymbol}${cashTotal.toFixed(2)}`;
  document.getElementById('zUpiSales').textContent = `${activeCurrencySymbol}${upiTotal.toFixed(2)}`;
  document.getElementById('zPendingSales').textContent = `${activeCurrencySymbol}${pendingTotal.toFixed(2)}`;
  document.getElementById('zTotalSales').textContent = `${activeCurrencySymbol}${totalRev.toFixed(2)}`;

  drawerCashInput.value = '';
  varianceResult.style.display = 'none';

  drawerCashInput.oninput = () => {
    const counted = parseFloat(drawerCashInput.value) || 0;
    const diff = counted - cashTotal;
    varianceResult.style.display = 'block';
    if (Math.abs(diff) < 0.01) {
      varianceResult.className = 'variance-result balanced';
      varianceResult.textContent = '✅ Drawer Cash is Perfectly Balanced!';
    } else if (diff < 0) {
      varianceResult.className = 'variance-result shortage';
      varianceResult.textContent = `⚠️ Cash Shortage: ${activeCurrencySymbol}${Math.abs(diff).toFixed(2)}`;
    } else {
      varianceResult.className = 'variance-result surplus';
      varianceResult.textContent = `🟢 Cash Surplus (Excess): +${activeCurrencySymbol}${diff.toFixed(2)}`;
    }
  };

  zReportModal.classList.add('active');
});

btnCloseZReport.addEventListener('click', () => zReportModal.classList.remove('active'));
btnDismissZ.addEventListener('click', () => zReportModal.classList.remove('active'));

btnShareZReport.addEventListener('click', () => {
  alert(`📋 End-of-Day Z-Report Generated!\n\nCash: ${document.getElementById('zCashSales').textContent}\nUPI: ${document.getElementById('zUpiSales').textContent}\nPending: ${document.getElementById('zPendingSales').textContent}\nTotal: ${document.getElementById('zTotalSales').textContent}`);
  zReportModal.classList.remove('active');
});

// Recycle Bin Modal
btnRecycleBin.addEventListener('click', () => {
  recycleBinModal.classList.add('active');
});
btnCloseRecycle.addEventListener('click', () => recycleBinModal.classList.remove('active'));
btnDismissRecycle.addEventListener('click', () => recycleBinModal.classList.remove('active'));

// Init
renderCatalog();
renderBillTable();
