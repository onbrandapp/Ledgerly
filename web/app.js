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

  // 3. D3.js 6-Month Monthly Spending & Income Overview Visualization
  initD3SpendingChart();
});

function initD3SpendingChart() {
  const chartWrapper = document.getElementById('d3-spending-chart');
  if (!chartWrapper || typeof d3 === 'undefined') return;

  // Initial 6-month historical dataset (March - August 2026)
  const defaultMonthlyData = [
    { month: 'Mar', year: 2026, income: 4350.00, expense: 2820.00 },
    { month: 'Apr', year: 2026, income: 4800.00, expense: 3150.00 },
    { month: 'May', year: 2026, income: 5200.00, expense: 2940.00 },
    { month: 'Jun', year: 2026, income: 4950.00, expense: 3420.00 },
    { month: 'Jul', year: 2026, income: 5600.00, expense: 3280.00 },
    { month: 'Aug', year: 2026, income: 5850.00, expense: 3100.00 }
  ];

  let currentData = JSON.parse(JSON.stringify(defaultMonthlyData));
  let currentMode = 'grouped'; // 'grouped', 'stacked', 'net'

  const tooltip = document.getElementById('chart-tooltip');
  const simMonthSelect = document.getElementById('sim-month');

  // Populate month select
  if (simMonthSelect) {
    simMonthSelect.innerHTML = '';
    currentData.forEach(d => {
      const opt = document.createElement('option');
      opt.value = d.month;
      opt.textContent = `${d.month} ${d.year}`;
      simMonthSelect.appendChild(opt);
    });
    simMonthSelect.value = 'Aug';
  }

  function updateMetrics() {
    const totalIncome = currentData.reduce((acc, d) => acc + d.income, 0);
    const totalExpense = currentData.reduce((acc, d) => acc + d.expense, 0);
    const netSurplus = totalIncome - totalExpense;
    const savingsRate = totalIncome > 0 ? ((netSurplus / totalIncome) * 100).toFixed(1) : '0.0';
    const avgExpense = totalExpense / currentData.length;

    const metricIncome = document.getElementById('metric-income');
    const metricExpense = document.getElementById('metric-expense');
    const metricNet = document.getElementById('metric-net');
    const metricRate = document.getElementById('metric-rate');
    const metricAvg = document.getElementById('metric-avg');

    if (metricIncome) metricIncome.textContent = '$' + totalIncome.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    if (metricExpense) metricExpense.textContent = '$' + totalExpense.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    if (metricNet) {
      metricNet.textContent = (netSurplus >= 0 ? '+' : '-') + '$' + Math.abs(netSurplus).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
      metricNet.style.color = netSurplus >= 0 ? '#34d399' : '#f87171';
    }
    if (metricRate) metricRate.textContent = `${savingsRate}% 6-Month Savings Rate`;
    if (metricAvg) metricAvg.textContent = '$' + avgExpense.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }

  function renderChart() {
    chartWrapper.innerHTML = '';

    const containerWidth = chartWrapper.parentElement.clientWidth || 800;
    const margin = { top: 30, right: 30, bottom: 45, left: 65 };
    const width = Math.max(containerWidth - margin.left - margin.right, 320);
    const height = 300;

    const svg = d3.select('#d3-spending-chart')
      .append('svg')
      .attr('viewBox', `0 0 ${width + margin.left + margin.right} ${height + margin.top + margin.bottom}`)
      .attr('preserveAspectRatio', 'xMidYMid meet')
      .append('g')
      .attr('transform', `translate(${margin.left},${margin.top})`);

    // Gradients & Defs
    const defs = svg.append('defs');

    // Income Gradient
    const incomeGrad = defs.append('linearGradient')
      .attr('id', 'income-gradient')
      .attr('x1', '0%').attr('y1', '0%')
      .attr('x2', '0%').attr('y2', '100%');
    incomeGrad.append('stop').attr('offset', '0%').attr('stop-color', '#10b981');
    incomeGrad.append('stop').attr('offset', '100%').attr('stop-color', '#059669').attr('stop-opacity', 0.85);

    // Expense Gradient
    const expenseGrad = defs.append('linearGradient')
      .attr('id', 'expense-gradient')
      .attr('x1', '0%').attr('y1', '0%')
      .attr('x2', '0%').attr('y2', '100%');
    expenseGrad.append('stop').attr('offset', '0%').attr('stop-color', '#f87171');
    expenseGrad.append('stop').attr('offset', '100%').attr('stop-color', '#ef4444').attr('stop-opacity', 0.85);

    // Net Area Gradient
    const netGrad = defs.append('linearGradient')
      .attr('id', 'net-gradient')
      .attr('x1', '0%').attr('y1', '0%')
      .attr('x2', '0%').attr('y2', '100%');
    netGrad.append('stop').attr('offset', '0%').attr('stop-color', '#6366f1').attr('stop-opacity', 0.35);
    netGrad.append('stop').attr('offset', '100%').attr('stop-color', '#6366f1').attr('stop-opacity', 0.0);

    // Scales
    const x0 = d3.scaleBand()
      .domain(currentData.map(d => d.month))
      .rangeRound([0, width])
      .paddingInner(0.25)
      .paddingOuter(0.15);

    const x1 = d3.scaleBand()
      .domain(['income', 'expense'])
      .rangeRound([0, x0.bandwidth()])
      .padding(0.08);

    let yMax;
    if (currentMode === 'stacked') {
      yMax = d3.max(currentData, d => d.income + d.expense) * 1.15;
    } else if (currentMode === 'net') {
      yMax = d3.max(currentData, d => Math.max(d.income - d.expense, 1000)) * 1.25;
    } else {
      yMax = d3.max(currentData, d => Math.max(d.income, d.expense)) * 1.15;
    }

    const y = d3.scaleLinear()
      .domain([0, yMax])
      .nice()
      .rangeRound([height, 0]);

    // Horizontal Grid lines
    svg.append('g')
      .attr('class', 'd3-grid')
      .call(d3.axisLeft(y)
        .ticks(5)
        .tickSize(-width)
        .tickFormat('')
      );

    // X Axis
    svg.append('g')
      .attr('class', 'd3-axis')
      .attr('transform', `translate(0,${height})`)
      .call(d3.axisBottom(x0))
      .selectAll('text')
      .style('font-weight', '600')
      .style('font-size', '13px');

    // Y Axis with Currency formatting
    svg.append('g')
      .attr('class', 'd3-axis')
      .call(d3.axisLeft(y).ticks(5).tickFormat(d => `$${d3.format('~s')(d)}`));

    // Tooltip Helpers
    const showTooltip = (event, d, extraInfo = '') => {
      if (!tooltip) return;
      const net = d.income - d.expense;
      const savingsRate = d.income > 0 ? ((net / d.income) * 100).toFixed(0) : '0';
      tooltip.style.opacity = '1';
      tooltip.innerHTML = `
        <div style="font-weight:700; margin-bottom:4px; font-size:13px; color:#c7d2fe;">${d.month} ${d.year} Breakdown</div>
        <div style="display:flex; justify-content:space-between; gap:12px; margin-bottom:2px;">
          <span style="color:#34d399;">▲ Income:</span>
          <strong>$${d.income.toLocaleString('en-US', { minimumFractionDigits: 2 })}</strong>
        </div>
        <div style="display:flex; justify-content:space-between; gap:12px; margin-bottom:4px;">
          <span style="color:#f87171;">▼ Expenses:</span>
          <strong>$${d.expense.toLocaleString('en-US', { minimumFractionDigits: 2 })}</strong>
        </div>
        <div style="border-top:1px solid rgba(255,255,255,0.1); padding-top:4px; display:flex; justify-content:space-between; gap:12px;">
          <span style="color:#a5b4fc;">Net Surplus:</span>
          <strong style="color:${net >= 0 ? '#34d399' : '#f87171'}">${net >= 0 ? '+' : '-'}$${Math.abs(net).toLocaleString('en-US', { minimumFractionDigits: 2 })} (${savingsRate}%)</strong>
        </div>
        ${extraInfo ? `<div style="font-size:10px; color:#94a3b8; margin-top:3px;">${extraInfo}</div>` : ''}
      `;

      const wrapperRect = chartWrapper.getBoundingClientRect();
      const left = event.clientX - wrapperRect.left + 15;
      const top = event.clientY - wrapperRect.top - 30;
      tooltip.style.left = `${Math.min(left, containerWidth - 220)}px`;
      tooltip.style.top = `${Math.max(top, 10)}px`;
    };

    const hideTooltip = () => {
      if (tooltip) tooltip.style.opacity = '0';
    };

    if (currentMode === 'grouped') {
      // GROUPED BAR CHART
      const monthGroups = svg.selectAll('.month-group')
        .data(currentData)
        .enter()
        .append('g')
        .attr('class', 'month-group')
        .attr('transform', d => `translate(${x0(d.month)},0)`);

      // Income Bars
      monthGroups.append('rect')
        .attr('class', 'd3-bar')
        .attr('x', x1('income'))
        .attr('width', x1.bandwidth())
        .attr('y', height)
        .attr('height', 0)
        .attr('rx', 5)
        .attr('fill', 'url(#income-gradient)')
        .on('mousemove', (event, d) => showTooltip(event, d, 'Bar: Inflow Volume'))
        .on('mouseleave', hideTooltip)
        .transition()
        .duration(700)
        .ease(d3.easeCubicOut)
        .attr('y', d => y(d.income))
        .attr('height', d => Math.max(height - y(d.income), 2));

      // Expense Bars
      monthGroups.append('rect')
        .attr('class', 'd3-bar')
        .attr('x', x1('expense'))
        .attr('width', x1.bandwidth())
        .attr('y', height)
        .attr('height', 0)
        .attr('rx', 5)
        .attr('fill', 'url(#expense-gradient)')
        .on('mousemove', (event, d) => showTooltip(event, d, 'Bar: Outflow Volume'))
        .on('mouseleave', hideTooltip)
        .transition()
        .duration(700)
        .delay(100)
        .ease(d3.easeCubicOut)
        .attr('y', d => y(d.expense))
        .attr('height', d => Math.max(height - y(d.expense), 2));

    } else if (currentMode === 'stacked') {
      // STACKED BAR CHART
      const monthGroups = svg.selectAll('.month-group')
        .data(currentData)
        .enter()
        .append('g')
        .attr('class', 'month-group')
        .attr('transform', d => `translate(${x0(d.month)},0)`);

      const barWidth = Math.min(x0.bandwidth() * 0.7, 48);
      const barOffset = (x0.bandwidth() - barWidth) / 2;

      // Bottom: Expense
      monthGroups.append('rect')
        .attr('class', 'd3-bar')
        .attr('x', barOffset)
        .attr('width', barWidth)
        .attr('y', height)
        .attr('height', 0)
        .attr('rx', 4)
        .attr('fill', 'url(#expense-gradient)')
        .on('mousemove', (event, d) => showTooltip(event, d, 'Stacked: Bottom Layer = Expenses'))
        .on('mouseleave', hideTooltip)
        .transition()
        .duration(700)
        .attr('y', d => y(d.expense))
        .attr('height', d => height - y(d.expense));

      // Top: Income
      monthGroups.append('rect')
        .attr('class', 'd3-bar')
        .attr('x', barOffset)
        .attr('width', barWidth)
        .attr('y', height)
        .attr('height', 0)
        .attr('rx', 4)
        .attr('fill', 'url(#income-gradient)')
        .on('mousemove', (event, d) => showTooltip(event, d, 'Stacked: Top Layer = Income'))
        .on('mouseleave', hideTooltip)
        .transition()
        .duration(700)
        .delay(100)
        .attr('y', d => y(d.income + d.expense))
        .attr('height', d => height - y(d.income));

    } else if (currentMode === 'net') {
      // NET TREND LINE + AREA CHART
      const area = d3.area()
        .x(d => (x0(d.month) || 0) + x0.bandwidth() / 2)
        .y0(height)
        .y1(d => y(Math.max(d.income - d.expense, 0)))
        .curve(d3.curveMonotoneX);

      const line = d3.line()
        .x(d => (x0(d.month) || 0) + x0.bandwidth() / 2)
        .y(d => y(Math.max(d.income - d.expense, 0)))
        .curve(d3.curveMonotoneX);

      // Area fill
      svg.append('path')
        .datum(currentData)
        .attr('fill', 'url(#net-gradient)')
        .attr('d', area);

      // Line stroke
      svg.append('path')
        .datum(currentData)
        .attr('fill', 'none')
        .attr('stroke', '#818cf8')
        .attr('stroke-width', 3.5)
        .attr('d', line);

      // Circles for each month
      svg.selectAll('.net-dot')
        .data(currentData)
        .enter()
        .append('circle')
        .attr('class', 'net-dot')
        .attr('cx', d => (x0(d.month) || 0) + x0.bandwidth() / 2)
        .attr('cy', d => y(Math.max(d.income - d.expense, 0)))
        .attr('r', 6)
        .attr('fill', '#6366f1')
        .attr('stroke', '#fff')
        .attr('stroke-width', 2)
        .style('cursor', 'pointer')
        .on('mousemove', (event, d) => showTooltip(event, d, 'Net Cashflow (Income - Expenses)'))
        .on('mouseleave', hideTooltip);
    }
  }

  // Toggle View Modes
  const toggleBtns = document.querySelectorAll('.toggle-btn');
  toggleBtns.forEach(btn => {
    btn.addEventListener('click', (e) => {
      toggleBtns.forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      currentMode = btn.getAttribute('data-mode') || 'grouped';
      renderChart();
    });
  });

  // Simulator Add Transaction
  const addSimTxBtn = document.getElementById('btn-add-sim-tx');
  if (addSimTxBtn) {
    addSimTxBtn.addEventListener('click', () => {
      const amountInput = document.getElementById('sim-amount');
      const typeSelect = document.getElementById('sim-type');
      const monthSelect = document.getElementById('sim-month');

      if (!amountInput || !typeSelect || !monthSelect) return;
      const amount = parseFloat(amountInput.value);
      if (!amount || isNaN(amount) || amount <= 0) return;

      const type = typeSelect.value;
      const targetMonth = monthSelect.value;

      const item = currentData.find(d => d.month === targetMonth);
      if (item) {
        if (type === 'INCOME') {
          item.income += amount;
        } else {
          item.expense += amount;
        }
        updateMetrics();
        renderChart();
      }

      amountInput.value = '';
    });
  }

  // Reset Data
  const resetBtn = document.getElementById('btn-reset-data');
  if (resetBtn) {
    resetBtn.addEventListener('click', () => {
      currentData = JSON.parse(JSON.stringify(defaultMonthlyData));
      updateMetrics();
      renderChart();
    });
  }

  // Resize handler
  window.addEventListener('resize', () => {
    renderChart();
  });

  // Initial draw
  updateMetrics();
  renderChart();
}

