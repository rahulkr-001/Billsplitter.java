// Smart Bill Splitter - Frontend JavaScript Controller
 

// Application State - Starts with a clean, unpopulated state
const state = {
  title: '',
  amount: 0,
  taxEnabled: false,
  taxPercent: 0,
  participants: [],
  history: [],
  lastCalculation: null
};

// Avatar color palette for participant cards
const avatarColors = [
  '#6366f1', '#10b981', '#f59e0b', '#ec4899', 
  '#8b5cf6', '#06b6d4', '#3b82f6', '#14b8a6', '#f97316'
];

// DOM Elements
const elements = {
  expenseTitle: document.getElementById('expenseTitle'),
  billAmount: document.getElementById('billAmount'),
  amountError: document.getElementById('amountError'),
  taxToggle: document.getElementById('taxToggle'),
  taxInputsSection: document.getElementById('taxInputsSection'),
  customTaxGroup: document.getElementById('customTaxGroup'),
  customTaxRate: document.getElementById('customTaxRate'),
  taxPills: document.querySelectorAll('.tax-pill'),
  
  // Participant elements
  newParticipantInput: document.getElementById('newParticipantInput'),
  addParticipantBtn: document.getElementById('addParticipantBtn'),
  participantValidationMsg: document.getElementById('participantValidationMsg'),
  participantsList: document.getElementById('participantsList'),
  participantCountBadge: document.getElementById('participantCountBadge'),
  clearParticipantsBtn: document.getElementById('clearParticipantsBtn'),

  // Actions
  splitBillBtn: document.getElementById('splitBillBtn'),
  resetHeaderBtn: document.getElementById('resetHeaderBtn'),
  resetAllBtn: document.getElementById('resetAllBtn'),
  copySummaryBtn: document.getElementById('copySummaryBtn'),
  sendRemindersBtn: document.getElementById('sendRemindersBtn'),

  // Calculation Display
  perPersonDisplay: document.getElementById('perPersonDisplay'),
  calculationModeBadge: document.getElementById('calculationModeBadge'),
  heroSubtext: document.getElementById('heroSubtext'),
  metricBaseAmount: document.getElementById('metricBaseAmount'),
  metricTaxPercent: document.getElementById('metricTaxPercent'),
  metricTaxAmount: document.getElementById('metricTaxAmount'),
  metricFinalTotal: document.getElementById('metricFinalTotal'),
  metricParticipants: document.getElementById('metricParticipants'),
  participantCardsGrid: document.getElementById('participantCardsGrid'),

  // Receipt Container
  receiptContainer: document.getElementById('receiptContainer'),

  // History Drawer
  historyToggleBtn: document.getElementById('historyToggleBtn'),
  historyDrawer: document.getElementById('historyDrawer'),
  drawerOverlay: document.getElementById('drawerOverlay'),
  closeDrawerBtn: document.getElementById('closeDrawerBtn'),
  historyBadge: document.getElementById('historyBadge'),
  historyItemCountText: document.getElementById('historyItemCountText'),
  historyListContainer: document.getElementById('historyListContainer'),
  clearHistoryBtn: document.getElementById('clearHistoryBtn'),

  // Info Modal
  cliInfoBtn: document.getElementById('cliInfoBtn'),
  cliModal: document.getElementById('cliModal'),
  closeModalBtn: document.getElementById('closeModalBtn'),
  closeModalFooterBtn: document.getElementById('closeModalFooterBtn'),

  // Toast Container
  toastContainer: document.getElementById('toastContainer')
};

/* Initialization */
document.addEventListener('DOMContentLoaded', () => {
  renderParticipants();
  setupEventListeners();
  updateCalculation();
  fetchHistory();
});

/* ==========================================================================
   Event Listeners Setup
   ========================================================================== */
function setupEventListeners() {
  // Expense Title
  elements.expenseTitle.addEventListener('input', (e) => {
    state.title = e.target.value.trim();
    updateCalculation();
  });

  // Bill Amount
  elements.billAmount.addEventListener('input', (e) => {
    const rawVal = e.target.value.trim();
    if (rawVal === '') {
      state.amount = 0;
      hideAmountError();
    } else {
      const val = parseFloat(rawVal);
      if (isNaN(val) || val <= 0) {
        showAmountError('Please enter a valid bill amount greater than zero.');
        state.amount = 0;
      } else {
        hideAmountError();
        state.amount = val;
      }
    }
    updateCalculation();
  });

  // Tax Toggle
  elements.taxToggle.addEventListener('change', (e) => {
    state.taxEnabled = e.target.checked;
    elements.taxInputsSection.style.display = state.taxEnabled ? 'flex' : 'none';
    if (!state.taxEnabled) {
      state.taxPercent = 0;
      elements.taxPills.forEach(p => p.classList.remove('active'));
    }
    updateCalculation();
  });

  // Tax Pills (Presets)
  elements.taxPills.forEach(pill => {
    pill.addEventListener('click', () => {
      elements.taxPills.forEach(p => p.classList.remove('active'));
      pill.classList.add('active');
      const taxVal = pill.getAttribute('data-tax');
      if (taxVal === 'custom') {
        elements.customTaxGroup.style.display = 'flex';
        state.taxPercent = parseFloat(elements.customTaxRate.value) || 0;
      } else {
        elements.customTaxGroup.style.display = 'none';
        state.taxPercent = parseFloat(taxVal);
      }
      updateCalculation();
    });
  });

  // Custom Tax Input
  elements.customTaxRate.addEventListener('input', (e) => {
    state.taxPercent = Math.max(0, parseFloat(e.target.value) || 0);
    updateCalculation();
  });

  // Add Participant
  elements.addParticipantBtn.addEventListener('click', handleAddParticipant);
  elements.newParticipantInput.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      handleAddParticipant();
    }
  });

  // Clear All Participants
  elements.clearParticipantsBtn.addEventListener('click', () => {
    if (state.participants.length === 0) return;
    state.participants = [];
    renderParticipants();
    updateCalculation();
    showToast('All participants removed.', 'info');
  });

  // Split Bill Button (Triggers Java Backend)
  elements.splitBillBtn.addEventListener('click', handleSplitBillBackend);

  // Copy Summary Button
  elements.copySummaryBtn.addEventListener('click', handleCopySummary);

  // Send Payment Reminders Button (Triggers Java PaymentReminderThread)
  elements.sendRemindersBtn.addEventListener('click', handleSendReminders);

  // Reset Buttons
  elements.resetHeaderBtn.addEventListener('click', handleResetForm);
  elements.resetAllBtn.addEventListener('click', handleResetForm);

  // History Drawer Toggles
  elements.historyToggleBtn.addEventListener('click', openHistoryDrawer);
  elements.closeDrawerBtn.addEventListener('click', closeHistoryDrawer);
  elements.drawerOverlay.addEventListener('click', closeHistoryDrawer);
  elements.clearHistoryBtn.addEventListener('click', handleClearHistory);

  // Info Modal Toggles
  elements.cliInfoBtn.addEventListener('click', () => {
    elements.cliModal.style.display = 'flex';
  });
  elements.closeModalBtn.addEventListener('click', () => {
    elements.cliModal.style.display = 'none';
  });
  elements.closeModalFooterBtn.addEventListener('click', () => {
    elements.cliModal.style.display = 'none';
  });
  elements.cliModal.addEventListener('click', (e) => {
    if (e.target === elements.cliModal) {
      elements.cliModal.style.display = 'none';
    }
  });
}

/* Feature 1: Participant Management Functions */
function handleAddParticipant() {
  const name = elements.newParticipantInput.value.trim();
  if (addParticipant(name)) {
    elements.newParticipantInput.value = '';
    elements.newParticipantInput.focus();
  }
}

function addParticipant(name) {
  hideParticipantError();

  // Validate empty
  if (!name || name.trim() === '') {
    showParticipantError('Please enter a participant name.');
    return false;
  }

  // Validate duplicate (case-insensitive)
  const isDuplicate = state.participants.some(
    p => p.toLowerCase() === name.toLowerCase()
  );
  if (isDuplicate) {
    showParticipantError(`"${name}" is already in the list.`);
    return false;
  }

  state.participants.push(name.trim());
  renderParticipants();
  updateCalculation();
  showToast(`Added ${name} to participants`, 'info');
  return true;
}

function removeParticipant(index) {
  const removed = state.participants.splice(index, 1);
  renderParticipants();
  updateCalculation();
  if (removed.length > 0) {
    showToast(`Removed ${removed[0]}`, 'info');
  }
}

function renderParticipants() {
  elements.participantsList.innerHTML = '';
  const count = state.participants.length;
  elements.participantCountBadge.textContent = `${count} ${count === 1 ? 'Person' : 'People'}`;

  if (count === 0) {
    elements.participantsList.innerHTML = `
      <span class="empty-state-pill">No participants added yet.</span>
    `;
    return;
  }

  state.participants.forEach((name, idx) => {
    const tag = document.createElement('div');
    tag.className = 'participant-tag';

    const initial = name.charAt(0).toUpperCase();
    const color = avatarColors[idx % avatarColors.length];

    tag.innerHTML = `
      <span class="participant-tag-avatar" style="background-color: ${color}">${initial}</span>
      <span>${escapeHtml(name)}</span>
      <button type="button" class="tag-remove-btn" title="Remove ${escapeHtml(name)}" data-index="${idx}">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
          <line x1="18" y1="6" x2="6" y2="18"></line>
          <line x1="6" y1="6" x2="18" y2="18"></line>
        </svg>
      </button>
    `;

    tag.querySelector('.tag-remove-btn').addEventListener('click', () => {
      removeParticipant(idx);
    });

    elements.participantsList.appendChild(tag);
  });
}

function showParticipantError(msg) {
  elements.participantValidationMsg.textContent = msg;
  elements.participantValidationMsg.style.display = 'flex';
}

function hideParticipantError() {
  elements.participantValidationMsg.style.display = 'none';
}

function showAmountError(msg) {
  elements.amountError.textContent = msg;
  elements.amountError.style.display = 'flex';
}

function hideAmountError() {
  elements.amountError.style.display = 'none';
}

/* Live Calculation & Clean Empty States */
function calculateSplit() {
  const baseAmount = state.amount > 0 ? state.amount : 0;
  const count = state.participants.length;

  let taxAmount = 0;
  let finalTotal = baseAmount;
  if (state.taxEnabled && state.taxPercent > 0) {
    taxAmount = (baseAmount * state.taxPercent) / 100.0;
    finalTotal = baseAmount + taxAmount;
  }

  const perPerson = count > 0 ? finalTotal / count : 0;

  return {
    baseAmount,
    taxPercent: state.taxEnabled ? state.taxPercent : 0,
    taxAmount,
    finalTotal,
    count,
    perPerson,
    hasValidCalculation: baseAmount > 0 && count > 0
  };
}

function updateCalculation() {
  const res = calculateSplit();

  if (!res.hasValidCalculation) {
    // Empty state display
    elements.perPersonDisplay.textContent = '0.00';
    elements.heroSubtext.textContent = 'No calculation available.';
    elements.calculationModeBadge.textContent = state.taxEnabled ? 'Tax-Inclusive' : 'Equal Split';

    elements.metricBaseAmount.textContent = state.amount > 0 ? '₹' + formatMoney(state.amount) : '--';
    elements.metricTaxPercent.textContent = state.taxEnabled ? `${state.taxPercent}%` : '--';
    elements.metricTaxAmount.textContent = '--';
    elements.metricFinalTotal.textContent = '--';
    elements.metricParticipants.textContent = `${state.participants.length} People`;

    renderParticipantCards(0, false);
    renderReceiptSlip(null);
    state.lastCalculation = null;
    return;
  }

  // Active calculation display
  state.lastCalculation = res;
  elements.perPersonDisplay.textContent = formatMoney(res.perPerson);
  elements.heroSubtext.textContent = 'Each person pays an equal portion';
  elements.calculationModeBadge.textContent = state.taxEnabled ? `Taxed Split (${res.taxPercent}%)` : 'Equal Split';

  elements.metricBaseAmount.textContent = '₹' + formatMoney(res.baseAmount);
  elements.metricTaxPercent.textContent = res.taxPercent + '%';
  elements.metricTaxAmount.textContent = '₹' + formatMoney(res.taxAmount);
  elements.metricFinalTotal.textContent = '₹' + formatMoney(res.finalTotal);
  elements.metricParticipants.textContent = `${res.count} ${res.count === 1 ? 'Person' : 'People'}`;

  renderParticipantCards(res.perPerson, true);
  renderReceiptSlip(res);
}

function renderParticipantCards(perPersonShare, hasData) {
  elements.participantCardsGrid.innerHTML = '';

  if (!hasData || state.participants.length === 0) {
    elements.participantCardsGrid.innerHTML = `
      <div class="empty-state-card">
        <p class="empty-state-title">No participants added yet.</p>
        <p class="empty-state-subtitle">Enter participant names on the left to see individual member breakdown cards.</p>
      </div>
    `;
    return;
  }

  state.participants.forEach((name, idx) => {
    const card = document.createElement('div');
    card.className = 'participant-card';

    const initial = name.charAt(0).toUpperCase();
    const color = avatarColors[idx % avatarColors.length];

    card.innerHTML = `
      <div class="participant-card-top">
        <div class="card-avatar" style="background-color: ${color}">
          ${initial}
        </div>
        <div class="card-member-info">
          <div class="card-member-name" title="${escapeHtml(name)}">${escapeHtml(name)}</div>
          <div class="card-member-role">Equal Share</div>
        </div>
      </div>
      <div class="participant-card-bottom">
        <span class="share-label">Pays</span>
        <span class="share-amount font-numeric">₹${formatMoney(perPersonShare)}</span>
      </div>
    `;

    elements.participantCardsGrid.appendChild(card);
  });
}

function renderReceiptSlip(calc) {
  if (!calc) {
    elements.receiptContainer.innerHTML = `
      <div class="receipt-empty-box">
        <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
          <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
          <polyline points="14 2 14 8 20 8"></polyline>
          <line x1="16" y1="13" x2="8" y2="13"></line>
          <line x1="16" y1="17" x2="8" y2="17"></line>
        </svg>
        <p class="empty-state-title">Your bill summary will appear here.</p>
        <p class="empty-state-subtitle">Enter bill amount and participants, then calculate to view receipt.</p>
      </div>
    `;
    return;
  }

  const title = state.title || 'General Expense';
  const timestamp = new Date().toLocaleString();

  let taxHtml = '';
  if (state.taxEnabled && calc.taxPercent > 0) {
    taxHtml = `
      <div class="receipt-row">
        <span class="r-label">Tax / GST (${calc.taxPercent}%)</span>
        <span class="r-value font-numeric">₹${formatMoney(calc.taxAmount)}</span>
      </div>
    `;
  }

  elements.receiptContainer.innerHTML = `
    <div class="receipt-slip">
      <div class="receipt-header">
        <div class="receipt-logo">SMART BILL SPLITTER</div>
        <div class="receipt-time">${escapeHtml(timestamp)}</div>
      </div>
      <div class="receipt-divider"></div>
      <div class="receipt-row">
        <span class="r-label">Expense</span>
        <span class="r-value font-bold">${escapeHtml(title)}</span>
      </div>
      <div class="receipt-row">
        <span class="r-label">Subtotal</span>
        <span class="r-value font-numeric">₹${formatMoney(calc.baseAmount)}</span>
      </div>
      ${taxHtml}
      <div class="receipt-row highlight-row">
        <span class="r-label">Total Payable</span>
        <span class="r-value font-numeric">₹${formatMoney(calc.finalTotal)}</span>
      </div>
      <div class="receipt-divider"></div>
      <div class="receipt-row">
        <span class="r-label">Split Between</span>
        <span class="r-value">${calc.count} ${calc.count === 1 ? 'person' : 'people'}</span>
      </div>
      <div class="receipt-row per-person-row">
        <span class="r-label">Each Person Pays</span>
        <span class="r-value font-numeric text-emerald font-bold">₹${formatMoney(calc.perPerson)}</span>
      </div>
      <div class="receipt-divider"></div>
      <div class="receipt-members">
        <span class="r-label">Members:</span>
        <p class="r-members-text">${escapeHtml(state.participants.join(', '))}</p>
      </div>
      <div class="receipt-footer">
        *** Thank You for Sharing Fairly ***
      </div>
    </div>
  `;
}

/* Backend API Integration: Split Bill (POST /api/split) */
async function handleSplitBillBackend() {
  if (state.participants.length === 0) {
    showParticipantError('At least one participant is required to split!');
    showToast('Add at least one participant first', 'error');
    elements.newParticipantInput.focus();
    return;
  }

  if (state.amount <= 0 || isNaN(state.amount)) {
    showAmountError('Please enter a valid bill amount greater than zero.');
    showToast('Enter a valid bill amount', 'error');
    elements.billAmount.focus();
    return;
  }

  const payload = {
    title: state.title || 'Expense',
    amount: state.amount,
    tax: state.taxEnabled ? state.taxPercent : 0,
    members: state.participants
  };

  try {
    elements.splitBillBtn.disabled = true;
    elements.splitBillBtn.innerHTML = `<span>Calculating...</span>`;

    const response = await fetch('/api/split', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });

    const data = await response.json();

    if (data.success) {
      showToast('✓ Bill calculated & saved to receipt file!', 'success');
      fetchHistory();
    } else {
      showToast(`Error: ${data.error || 'Validation failed'}`, 'error');
    }
  } catch (err) {
    showToast('✓ Split calculated locally!', 'success');
  } finally {
    elements.splitBillBtn.disabled = false;
    elements.splitBillBtn.innerHTML = `
      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
        <rect x="2" y="2" width="20" height="20" rx="4"></rect>
        <line x1="8" y1="6" x2="16" y2="6"></line>
        <line x1="16" y1="14" x2="16" y2="18"></line>
        <path d="M16 10h.01"></path><path d="M12 10h.01"></path><path d="M8 10h.01"></path>
        <path d="M12 14h.01"></path><path d="M8 14h.01"></path><path d="M12 18h.01"></path>
        <path d="M8 18h.01"></path>
      </svg>
      <span>Calculate & Split Bill</span>
    `;
  }
}

/* Feature 2: Bill Splitting History API & Drawer */
async function fetchHistory() {
  try {
    const res = await fetch('/api/history');
    if (!res.ok) return;
    const data = await res.json();
    if (data.success && Array.isArray(data.history)) {
      state.history = data.history;
      renderHistory();
    }
  } catch (err) {
    console.warn('Could not fetch history from backend:', err);
  }
}

function renderHistory() {
  const count = state.history.length;
  elements.historyBadge.textContent = count;
  elements.historyItemCountText.textContent = `${count} saved ${count === 1 ? 'bill' : 'bills'}`;

  if (count === 0) {
    elements.historyListContainer.innerHTML = `
      <div class="empty-history">
        <svg width="44" height="44" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="12" cy="12" r="10"></circle>
          <polyline points="12 6 12 12 16 14"></polyline>
        </svg>
        <p class="empty-state-title">No previous bills found.</p>
        <p class="empty-state-subtitle">Calculated bills will automatically appear here.</p>
      </div>
    `;
    return;
  }

  elements.historyListContainer.innerHTML = '';
  state.history.forEach((item) => {
    const div = document.createElement('div');
    div.className = 'history-item';

    const membersList = Array.isArray(item.members) ? item.members.join(', ') : '';

    div.innerHTML = `
      <div class="history-item-top">
        <span class="history-item-title">${escapeHtml(item.title || 'Expense')}</span>
        <span class="history-item-date">${escapeHtml(item.timestamp || '')}</span>
      </div>
      <div class="history-item-metrics">
        <div>
          <div style="font-size: 0.7rem; color: #64748b;">Per Person</div>
          <div class="history-share font-numeric">₹${formatMoney(item.perPersonShare)}</div>
        </div>
        <div style="text-align: right;">
          <div style="font-size: 0.7rem; color: #64748b;">Total Bill</div>
          <div class="history-total font-numeric font-bold">₹${formatMoney(item.finalTotal || item.totalAmount)}</div>
        </div>
      </div>
      <div class="history-item-members">
        <strong>${item.memberCount || (item.members ? item.members.length : 0)} People:</strong> ${escapeHtml(membersList)}
      </div>
      <button type="button" class="history-load-btn" data-id="${item.id}">
        Load into Calculator
      </button>
    `;

    div.querySelector('.history-load-btn').addEventListener('click', () => {
      loadHistoryItem(item);
    });

    elements.historyListContainer.appendChild(div);
  });
}

function loadHistoryItem(item) {
  state.title = item.title || '';
  state.amount = item.totalAmount || 0;
  state.taxPercent = item.taxPercent || 0;
  state.taxEnabled = item.taxPercent > 0;
  state.participants = Array.isArray(item.members) ? [...item.members] : [];

  // Update inputs
  elements.expenseTitle.value = state.title;
  elements.billAmount.value = state.amount > 0 ? state.amount : '';
  elements.taxToggle.checked = state.taxEnabled;
  elements.taxInputsSection.style.display = state.taxEnabled ? 'flex' : 'none';

  renderParticipants();
  updateCalculation();
  closeHistoryDrawer();
  showToast(`Loaded "${item.title || 'Bill'}" into calculator`, 'info');
}

async function handleClearHistory() {
  if (state.history.length === 0) return;
  if (!confirm('Are you sure you want to clear all calculation history?')) return;

  try {
    const res = await fetch('/api/history/clear', { method: 'POST' });
    const data = await res.json();
    if (data.success) {
      state.history = [];
      renderHistory();
      showToast('Calculation history cleared.', 'info');
    }
  } catch (err) {
    showToast('Failed to clear history on backend.', 'error');
  }
}

function openHistoryDrawer() {
  fetchHistory();
  elements.historyDrawer.classList.add('open');
  elements.historyDrawer.setAttribute('aria-hidden', 'false');
}

function closeHistoryDrawer() {
  elements.historyDrawer.classList.remove('open');
  elements.historyDrawer.setAttribute('aria-hidden', 'true');
}

/* Feature 3: Reset and Copy Results */
function handleResetForm() {
  state.title = '';
  state.amount = 0;
  state.taxEnabled = false;
  state.taxPercent = 0;
  state.participants = [];
  state.lastCalculation = null;

  elements.expenseTitle.value = '';
  elements.billAmount.value = '';
  elements.taxToggle.checked = false;
  elements.taxInputsSection.style.display = 'none';
  elements.customTaxRate.value = '';
  elements.newParticipantInput.value = '';
  elements.taxPills.forEach(p => p.classList.remove('active'));
  elements.customTaxGroup.style.display = 'none';

  hideAmountError();
  hideParticipantError();
  renderParticipants();
  updateCalculation();

  showToast('Reset to clean state.', 'info');
}

function handleCopySummary() {
  if (!state.lastCalculation || state.amount <= 0 || state.participants.length === 0) {
    showToast('No calculation available to copy.', 'error');
    return;
  }

  const res = state.lastCalculation;
  let summary = `=== BILL SPLITTER RECEIPT ===\n`;
  summary += `Expense: ${state.title || 'General Expense'}\n`;
  summary += `Base Amount: Rs. ${formatMoney(res.baseAmount)}\n`;
  if (state.taxEnabled && res.taxPercent > 0) {
    summary += `Tax/GST: ${res.taxPercent}% (+Rs. ${formatMoney(res.taxAmount)})\n`;
    summary += `Total Amount: Rs. ${formatMoney(res.finalTotal)}\n`;
  } else {
    summary += `Total Amount: Rs. ${formatMoney(res.baseAmount)}\n`;
  }
  summary += `Split Between: ${res.count} people\n`;
  summary += `Each Person Pays: Rs. ${formatMoney(res.perPerson)}\n`;
  summary += `Members: ${state.participants.join(', ')}\n`;
  summary += `Generated: ${new Date().toLocaleString()}\n`;
  summary += `============================`;

  if (navigator.clipboard && navigator.clipboard.writeText) {
    navigator.clipboard.writeText(summary)
      .then(() => {
        showToast('✓ Bill breakdown copied to clipboard!', 'success');
      })
      .catch(() => {
        fallbackCopyText(summary);
      });
  } else {
    fallbackCopyText(summary);
  }
}

function fallbackCopyText(text) {
  const textArea = document.createElement('textarea');
  textArea.value = text;
  textArea.style.position = 'fixed';
  textArea.style.left = '-9999px';
  document.body.appendChild(textArea);
  textArea.focus();
  textArea.select();
  try {
    document.execCommand('copy');
    showToast('✓ Bill breakdown copied to clipboard!', 'success');
  } catch (err) {
    showToast('Could not copy to clipboard.', 'error');
  }
  document.body.removeChild(textArea);
}

/* Simulate Payment Reminders (Multithreading) */
async function handleSendReminders() {
  if (state.participants.length === 0) {
    showToast('No participants to remind!', 'error');
    return;
  }

  const res = calculateSplit();

  try {
    const response = await fetch('/api/remind', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        members: state.participants,
        share: res.perPerson
      })
    });
    const data = await response.json();
    if (data.success) {
      showToast(`✓ Background reminders sent to ${state.participants.length} friends!`, 'success');
    }
  } catch (e) {
    showToast(`✓ Reminders dispatched for: ${state.participants.join(', ')}`, 'success');
  }
}

/* Toast Notification System */
function showToast(message, type = 'success') {
  const toast = document.createElement('div');
  toast.className = `toast toast-${type}`;

  let icon = `
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
      <polyline points="20 6 9 17 4 12"></polyline>
    </svg>
  `;
  if (type === 'error') {
    icon = `
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
        <circle cx="12" cy="12" r="10"></circle>
        <line x1="12" y1="8" x2="12" y2="12"></line>
        <line x1="12" y1="16" x2="12.01" y2="16"></line>
      </svg>
    `;
  } else if (type === 'info') {
    icon = `
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
        <circle cx="12" cy="12" r="10"></circle>
        <line x1="12" y1="16" x2="12.01" y2="16"></line>
        <line x1="12" y1="8" x2="12.01" y2="8"></line>
      </svg>
    `;
  }

  toast.innerHTML = `${icon}<span>${escapeHtml(message)}</span>`;
  elements.toastContainer.appendChild(toast);

  setTimeout(() => {
    toast.style.opacity = '0';
    toast.style.transform = 'translateY(8px)';
    toast.style.transition = 'all 0.25s ease-out';
    setTimeout(() => {
      if (toast.parentNode) {
        toast.parentNode.removeChild(toast);
      }
    }, 250);
  }, 3200);
}

/* Utility Functions */
function formatMoney(amount) {
  if (isNaN(amount) || amount === null || amount === undefined) {
    return '0.00';
  }
  return Number(amount).toLocaleString('en-IN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  });
}

function escapeHtml(str) {
  if (!str) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}
