// Ledgerly Web Companion & Interactive Demo Logic

document.addEventListener('DOMContentLoaded', () => {
  // 1. Natural Language Parser Simulator
  const parseBtn = document.getElementById('btn-simulate-parse');
  const parseInput = document.getElementById('nlp-input');
  const parseResult = document.getElementById('nlp-result');

  if (parseBtn && parseInput && parseResult) {
    parseBtn.addEventListener('click', () => {
      const text = parseInput.value.trim();
      if (!text) return;

      parseResult.innerHTML = '<span style="color: #a5b4fc;">⚡ Gemini 2.5 Flash is parsing financial intent...</span>';

      setTimeout(() => {
        const parsed = simulateAiParse(text);
        parseResult.innerHTML = `
          <div style="display:flex; flex-direction:column; gap:4px; width:100%;">
            <div style="display:flex; justify-content:space-between; align-items:center;">
              <strong style="color: ${parsed.type === 'INCOME' ? '#34d399' : '#f87171'}; font-size:15px;">
                ${parsed.type === 'INCOME' ? '+' : '-'}$${parsed.amount.toFixed(2)} (${parsed.type})
              </strong>
              <span style="background:rgba(99,102,241,0.25); color:#c7d2fe; padding:2px 8px; border-radius:6px; font-size:11px; font-weight:700;">
                ${parsed.category}
              </span>
            </div>
            <div style="color:#94a3b8; font-size:12px;">"${parsed.description}"</div>
          </div>
        `;
      }, 500);
    });
  }

  function simulateAiParse(query) {
    const lower = query.toLowerCase();
    let type = "EXPENSE";
    let amount = 15.00;
    let category = "Food & Dining";
    let description = query;

    // Extract numbers if present
    const match = query.match(/\$?(\d+(?:\.\d{1,2})?)/);
    if (match) {
      amount = parseFloat(match[1]);
    }

    if (lower.includes("received") || lower.includes("earned") || lower.includes("salary") || lower.includes("retainer") || lower.includes("bonus") || lower.includes("income") || lower.includes("invoice")) {
      type = "INCOME";
      category = "Income";
      if (lower.includes("retainer") || lower.includes("client")) category = "Freelance";
      if (lower.includes("salary")) category = "Salary";
    } else if (lower.includes("coffee") || lower.includes("lunch") || lower.includes("dinner") || lower.includes("chipotle") || lower.includes("starbucks") || lower.includes("food")) {
      category = "Food & Dining";
    } else if (lower.includes("uber") || lower.includes("lyft") || lower.includes("gas") || lower.includes("train") || lower.includes("flight")) {
      category = "Transportation";
    } else if (lower.includes("rent") || lower.includes("electric") || lower.includes("wifi") || lower.includes("utility")) {
      category = "Bills & Utilities";
    } else if (lower.includes("amazon") || lower.includes("shopping") || lower.includes("clothes")) {
      category = "Shopping";
    }

    return { type, amount, category, description };
  }

  // 2. Interactive Future Income Pipeline Calculator
  const addForecastBtn = document.getElementById('btn-add-forecast');
  if (addForecastBtn) {
    addForecastBtn.addEventListener('click', () => {
      const titleInput = document.getElementById('forecast-title');
      const amountInput = document.getElementById('forecast-amount');
      const statusInput = document.getElementById('forecast-status');
      const listContainer = document.getElementById('forecast-items-list');
      const totalDisplay = document.getElementById('forecast-total-display');

      if (!titleInput || !amountInput || !statusInput || !listContainer) return;

      const title = titleInput.value.trim() || "Consulting Payment";
      const amount = parseFloat(amountInput.value) || 750.00;
      const status = statusInput.value;

      const itemDiv = document.createElement('div');
      itemDiv.className = 'pipeline-item';
      
      let statusClass = 'tag-expected';
      if (status === 'CONFIRMED') statusClass = 'tag-confirmed';
      if (status === 'TENTATIVE') statusClass = 'tag-tentative';

      itemDiv.innerHTML = `
        <span><strong>${title}</strong></span>
        <span>
          <span class="${statusClass}">${status}</span> · 
          <strong>$${amount.toLocaleString('en-US', { minimumFractionDigits: 2 })}</strong>
        </span>
      `;

      listContainer.prepend(itemDiv);

      // Recalculate total
      if (totalDisplay) {
        let currentTotal = parseFloat(totalDisplay.getAttribute('data-total') || '5650.00');
        currentTotal += amount;
        totalDisplay.setAttribute('data-total', currentTotal.toString());
        totalDisplay.textContent = '$' + currentTotal.toLocaleString('en-US', { minimumFractionDigits: 2 });
      }

      titleInput.value = '';
      amountInput.value = '';
    });
  }
});
