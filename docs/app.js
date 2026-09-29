/**
 * Fruit & Grocery Quick Billing POS - Interactive Web Simulator
 */

const QUICK_PRODUCTS = [
  { emoji: '🍎', name: 'Apple (Kashmir)', price: 180, unit: 'kg' },
  { emoji: '🍌', name: 'Banana (Robusta)', price: 60, unit: 'doz' },
  { emoji: '🥭', name: 'Mango (Alphonso)', price: 150, unit: 'kg' },
  { emoji: '🍊', name: 'Orange (Nagpur)', price: 90, unit: 'kg' },
  { emoji: '🥔', name: 'Potato (Jyoti)', price: 35, unit: 'kg' },
  { emoji: '🧅', name: 'Onion (Nashik)', price: 40, unit: 'kg' },
  { emoji: '🍅', name: 'Tomato (Local)', price: 30, unit: 'kg' },
  { emoji: '🍇', name: 'Grapes (Seedless)', price: 120, unit: 'kg' }
];

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

// Render Quick Product Chips
function renderQuickProducts() {
  quickItemsRow.innerHTML = '';
  QUICK_PRODUCTS.forEach((prod, idx) => {
    const chip = document.createElement('div');
    chip.className = `quick-item-chip ${idx === 0 ? 'selected' : ''}`;
    chip.innerHTML = `
      <span class="emoji">${prod.emoji}</span>
      <div class="info">
        <div class="name">${prod.name}</div>
        <div class="price">₹${prod.price}/${prod.unit}</div>
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
    simCalcInput.value = `${prod.name} = ${prod.price} * ${grams}g`;
  } else {
    simCalcInput.value = `${prod.name} = ${prod.price} * ${qty}`;
  }
}

// Multiplier Click Handlers
multChips.forEach(chip => {
  chip.addEventListener('click', () => {
    const qty = parseFloat(chip.dataset.qty);
    updateCalcInputFromProduct(currentSelectedProduct, qty);
  });
});

// Clear input
simClearInputBtn.addEventListener('click', () => {
  simCalcInput.value = '';
});

// Add Item
simAddItemBtn.addEventListener('click', () => {
  let val = simCalcInput.value.trim();
  if (!val) {
    // default to 1 unit of selected product
    val = `${currentSelectedProduct.name} = ${currentSelectedProduct.price} * 1`;
  }

  // Parse expression
  let name = currentSelectedProduct.name;
  let price = currentSelectedProduct.price;
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
  // reset input to default
  simCalcInput.value = '';
});

// Render Cart
function renderCart() {
  if (currentBillItems.length === 0) {
    simCartList.innerHTML = '';
    simCartList.appendChild(simEmptyCart);
    simEmptyCart.style.display = 'block';
    simItemCount.textContent = '0';
    simTotalAmount.textContent = '₹0.00';
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
        <span class="amt">₹${item.amount.toFixed(2)}</span>
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
  simTotalAmount.textContent = `₹${total.toFixed(2)}`;
  simSaveBillBtn.disabled = false;

  // Generate Smart Rounding Suggestions
  generateRoundingSuggestions(total);
}

function generateRoundingSuggestions(total) {
  const roundedInt = Math.round(total);
  if (roundedInt < 20) {
    simRoundingRow.style.display = 'none';
    return;
  }

  const suggestions = new Set();
  // nearest lower 10
  const floor10 = Math.floor(total / 10) * 10;
  if (floor10 > 0 && floor10 < total) suggestions.add(floor10);

  // nearest lower 50 or 100 for bigger amounts
  if (total >= 100) {
    const floor50 = Math.floor(total / 50) * 50;
    if (floor50 > 0 && floor50 < total) suggestions.add(floor50);
  }

  if (suggestions.size > 0) {
    simRoundingChips.innerHTML = '';
    suggestions.forEach(suggestedVal => {
      const chip = document.createElement('span');
      chip.className = 'rounding-chip';
      chip.textContent = `₹${suggestedVal}`;
      chip.title = `Apply discounted final price ₹${suggestedVal}`;
      chip.addEventListener('click', () => {
        simTotalAmount.textContent = `₹${suggestedVal.toFixed(2)}`;
        chip.style.borderColor = '#10b981';
      });
      simRoundingChips.appendChild(chip);
    });
    simRoundingRow.style.display = 'flex';
  } else {
    simRoundingRow.style.display = 'none';
  }
}

// Reset Cart
simResetBillBtn.addEventListener('click', () => {
  currentBillItems = [];
  renderCart();
});

// Payment Method Switcher
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

// Complete & Bill Click
simSaveBillBtn.addEventListener('click', () => {
  if (currentBillItems.length === 0) return;

  const totalStr = simTotalAmount.textContent;
  const custName = simCustomerName.value.trim();

  // Populate simulated modal receipt
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
      <span>₹${item.amount.toFixed(2)}</span>
    `;
    receiptItems.appendChild(itemRow);
  });

  simReceiptModal.classList.add('active');
});

// Close Modal
simCloseModalBtn.addEventListener('click', () => {
  simReceiptModal.classList.remove('active');
  // clear cart after checkout
  currentBillItems = [];
  simCustomerName.value = '';
  renderCart();
});

// WhatsApp Share Simulation
simShareReceiptBtn.addEventListener('click', () => {
  const cust = simCustomerName.value.trim() || 'Valued Customer';
  alert(`✨ WhatsApp Receipt Message Generated for ${cust}!\n\n🧾 FRESH MART & FRUITS\nTotal: ${simTotalAmount.textContent}\nStatus: ${currentPaymentMode}\nThank you for shopping with us!`);
  simReceiptModal.classList.remove('active');
  currentBillItems = [];
  simCustomerName.value = '';
  renderCart();
});

// Close modal on overlay click
simReceiptModal.addEventListener('click', (e) => {
  if (e.target === simReceiptModal) {
    simReceiptModal.classList.remove('active');
  }
});

// Initialize on Load
document.addEventListener('DOMContentLoaded', () => {
  renderQuickProducts();
  updateCalcInputFromProduct(QUICK_PRODUCTS[0], 1);
});
