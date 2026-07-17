// TradePulse Dashboard Logic

document.addEventListener('DOMContentLoaded', () => {
    // DOM Elements
    const apiInput = document.getElementById('api-url');
    const connStatus = document.getElementById('connection-status');
    const streamStatus = document.getElementById('price-stream-status');
    const clockEl = document.getElementById('clock');
    
    // Ticker Elements
    const priceBTC = document.getElementById('price-BTCUSDT');
    const changeBTC = document.getElementById('change-BTCUSDT');
    const priceETH = document.getElementById('price-ETHUSDT');
    const changeETH = document.getElementById('change-ETHUSDT');
    
    // Order Form Elements
    const orderForm = document.getElementById('order-form');
    const typeSelect = document.getElementById('type-select');
    const priceGroup = document.getElementById('price-group');
    const priceInput = document.getElementById('order-price');
    const qtyInput = document.getElementById('order-qty');
    const symbolSelect = document.getElementById('symbol-select');
    const idempotencyToggle = document.getElementById('idempotency-toggle');
    const idempotencyBody = document.getElementById('idempotency-body');
    const idempotencyInput = document.getElementById('idempotency-key');
    const regenKeyBtn = document.getElementById('regen-key-btn');
    
    // Tables
    const positionsBody = document.querySelector('#positions-table tbody');
    const activeOrdersBody = document.querySelector('#active-orders-table tbody');
    const allOrdersBody = document.querySelector('#all-orders-table tbody');
    const auditLogList = document.getElementById('audit-log-list');
    
    // Settlement Elements
    const runSettlementBtn = document.getElementById('run-settlement-btn');
    const clearSettlementBtn = document.getElementById('clear-settlement-report-btn');
    const settlementReportContainer = document.getElementById('settlement-report-container');
    const reportStatus = document.getElementById('report-status');
    const reportTime = document.getElementById('report-time');
    const reportText = document.getElementById('report-text');
    
    // Tabs
    const tabBtns = document.querySelectorAll('.tab-btn');
    const tabContents = document.querySelectorAll('.tab-content');
    
    // Toast
    const toast = document.getElementById('toast');
    const toastTitle = document.getElementById('toast-title');
    const toastMsg = document.getElementById('toast-message');

    // Global State
    let previousPrices = {
        BTCUSDT: null,
        ETHUSDT: null
    };
    
    let isApiConnected = false;

    // --- HELPER FUNCTIONS ---

    // Generate UUID v4 for Idempotency Key
    function generateUUID() {
        try {
            return crypto.randomUUID();
        } catch (e) {
            // Fallback UUID generator
            return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
                var r = Math.random() * 16 | 0, v = c == 'x' ? r : (r & 0x3 | 0x8);
                return v.toString(16);
            });
        }
    }

    // Set Idempotency Key input value
    function resetIdempotencyKey() {
        idempotencyInput.value = generateUUID();
    }

    // Toast Notifications
    function showToast(title, message, type = 'info') {
        toastTitle.textContent = title;
        toastMsg.textContent = message;
        
        toast.className = 'toast'; // Reset
        if (type === 'success') toast.classList.add('success');
        if (type === 'error') toast.classList.add('error');
        
        toast.classList.add('show');
        
        setTimeout(() => {
            toast.classList.remove('show');
        }, 4000);
    }

    // Update Clock
    function updateClock() {
        const now = new Date();
        clockEl.textContent = now.toTimeString().split(' ')[0];
    }
    setInterval(updateClock, 1000);
    updateClock();

    // --- BINANCE WEBSOCKET (LIVE PRICES) ---
    let ws = null;
    function connectPriceStream() {
        streamStatus.className = 'status-indicator disconnected';
        streamStatus.querySelector('.status-text').textContent = 'TICKSTREAM DISCONNECTED';
        
        console.log('Connecting to Binance WebSocket...');
        ws = new WebSocket('wss://stream.binance.com:9443/ws/btcusdt@ticker/ethusdt@ticker');
        
        ws.onopen = () => {
            console.log('Binance WebSocket Connected!');
            streamStatus.className = 'status-indicator active-stream';
            streamStatus.querySelector('.status-text').textContent = 'TICKSTREAM LIVE';
        };
        
        ws.onmessage = (event) => {
            const data = JSON.parse(event.data);
            const symbol = data.s; // Symbol e.g. BTCUSDT
            const price = parseFloat(data.c).toFixed(2); // Last price
            const changePercent = parseFloat(data.P).toFixed(2); // Change percent
            
            updateTickerUI(symbol, price, changePercent);
        };
        
        ws.onclose = () => {
            console.warn('Binance WebSocket closed. Reconnecting in 5s...');
            setTimeout(connectPriceStream, 5000);
        };
        
        ws.onerror = (err) => {
            console.error('Binance WebSocket error:', err);
            ws.close();
        };
    }

    function updateTickerUI(symbol, price, changePercent) {
        const priceEl = symbol === 'BTCUSDT' ? priceBTC : priceETH;
        const changeEl = symbol === 'BTCUSDT' ? changeBTC : changeETH;
        const cardEl = document.getElementById(`ticker-${symbol}`);
        const arrowEl = cardEl.querySelector('.ticker-arrow');
        
        // Price animation
        const prevPrice = previousPrices[symbol];
        priceEl.textContent = '$' + parseFloat(price).toLocaleString(undefined, {minimumFractionDigits: 2});
        
        if (prevPrice) {
            priceEl.className = 'ticker-price'; // reset
            if (parseFloat(price) > parseFloat(prevPrice)) {
                priceEl.classList.add('flash-green');
                arrowEl.textContent = '▲';
                arrowEl.style.color = 'var(--color-green)';
            } else if (parseFloat(price) < parseFloat(prevPrice)) {
                priceEl.classList.add('flash-red');
                arrowEl.textContent = '▼';
                arrowEl.style.color = 'var(--color-red)';
            }
        }
        previousPrices[symbol] = price;
        
        // Percent change
        const isPos = parseFloat(changePercent) >= 0;
        changeEl.textContent = `${isPos ? '+' : ''}${changePercent}% (24h)`;
        changeEl.className = 'ticker-change ' + (isPos ? 'pos' : 'neg');
    }

    // --- API POLLING ---
    
    async function apiRequest(endpoint, options = {}) {
        const baseUrl = apiInput.value.trim() || 'http://localhost:8080';
        const url = `${baseUrl}${endpoint}`;
        
        const response = await fetch(url, options);
        if (!response.ok) {
            let errorText = 'API Error';
            try {
                const errData = await response.json();
                errorText = errData.message || errorText;
            } catch (e) {
                errorText = await response.text() || errorText;
            }
            throw new Error(errorText);
        }
        
        if (response.status === 204) return null;
        return response.json();
    }

    async function refreshDashboardData() {
        try {
            await Promise.all([
                loadPositions(),
                loadOrders(),
                loadAuditLogs()
            ]);
            
            if (!isApiConnected) {
                isApiConnected = true;
                connStatus.className = 'status-indicator connected';
                connStatus.querySelector('.status-text').textContent = 'ORDER SERVICE ONLINE';
                showToast('Connected', 'Successfully connected to order-service API.', 'success');
            }
        } catch (error) {
            console.error('Failed to poll dashboard data:', error);
            if (isApiConnected) {
                isApiConnected = false;
                connStatus.className = 'status-indicator disconnected';
                connStatus.querySelector('.status-text').textContent = 'ORDER SERVICE OFFLINE';
                showToast('API Connection Lost', 'Cannot reach ' + apiInput.value, 'error');
            }
            
            // Set tables to display connection error
            const errorRow = `<tr class="no-data"><td colspan="10" style="color: var(--color-red);">Error connecting to API. Checking connection...</td></tr>`;
            positionsBody.innerHTML = errorRow;
            activeOrdersBody.innerHTML = errorRow;
            allOrdersBody.innerHTML = errorRow;
            auditLogList.innerHTML = `<div class="audit-entry-placeholder" style="color: var(--color-red);">Unable to retrieve system logs.</div>`;
        }
    }

    // Load Positions
    async function loadPositions() {
        const positions = await apiRequest('/api/positions');
        
        if (!positions || positions.length === 0) {
            positionsBody.innerHTML = `
                <tr class="no-data">
                    <td colspan="4">No open positions found.</td>
                </tr>`;
            return;
        }
        
        positionsBody.innerHTML = positions.map(pos => {
            const qty = parseFloat(pos.quantity);
            const avgPrice = parseFloat(pos.averagePrice);
            const formattedTime = new Date(pos.updatedAt).toLocaleString();
            
            // Highlight position direction
            const qtyClass = qty > 0 ? 'font-mono' : 'font-mono';
            const qtyStyle = qty > 0 ? 'color: var(--color-green); font-weight: 700;' : qty < 0 ? 'color: var(--color-red); font-weight: 700;' : '';
            
            return `
                <tr>
                    <td style="font-weight: 700; color: var(--color-cyan);">${pos.symbol}</td>
                    <td class="${qtyClass}" style="${qtyStyle}">${qty > 0 ? '+' : ''}${qty.toFixed(4)}</td>
                    <td class="font-mono">$${avgPrice.toLocaleString(undefined, {minimumFractionDigits: 2})}</td>
                    <td class="text-muted" style="font-size: 0.75rem;">${formattedTime}</td>
                </tr>`;
        }).join('');
    }

    // Load Orders (Pending vs History)
    async function loadOrders() {
        const orders = await apiRequest('/api/orders');
        
        if (!orders || orders.length === 0) {
            const emptyPending = `<tr class="no-data"><td colspan="6">No pending orders.</td></tr>`;
            const emptyHistory = `<tr class="no-data"><td colspan="7">No order history available.</td></tr>`;
            activeOrdersBody.innerHTML = emptyPending;
            allOrdersBody.innerHTML = emptyHistory;
            return;
        }

        // Sort orders by updated date descending
        orders.sort((a, b) => new Date(b.updatedAt) - new Date(a.updatedAt));

        const pendingOrders = orders.filter(o => o.status === 'PENDING');
        
        // Render Pending Orders
        if (pendingOrders.length === 0) {
            activeOrdersBody.innerHTML = `<tr class="no-data"><td colspan="6">No pending orders.</td></tr>`;
        } else {
            activeOrdersBody.innerHTML = pendingOrders.map(o => {
                const priceStr = o.type === 'MARKET' ? 'MARKET' : '$' + parseFloat(o.price).toLocaleString();
                const shortId = o.id.substring(0, 8) + '...';
                return `
                    <tr>
                        <td class="font-mono text-muted" title="${o.id}">${shortId}</td>
                        <td style="font-weight: 600;">${o.symbol}</td>
                        <td><span class="badge-side ${o.side}">${o.side}</span></td>
                        <td class="font-mono">${priceStr}</td>
                        <td class="font-mono">${parseFloat(o.quantity).toFixed(4)}</td>
                        <td><button class="btn-cancel" onclick="cancelOrder('${o.id}')">Cancel</button></td>
                    </tr>`;
            }).join('');
        }

        // Render History Orders (All orders)
        allOrdersBody.innerHTML = orders.map(o => {
            const priceStr = o.type === 'MARKET' ? 'MARKET' : '$' + parseFloat(o.price).toLocaleString(undefined, {minimumFractionDigits: 2});
            const formattedTime = new Date(o.createdAt).toLocaleTimeString();
            return `
                <tr>
                    <td class="text-muted" style="font-size: 0.75rem;">${formattedTime}</td>
                    <td style="font-weight: 600;">${o.symbol}</td>
                    <td><span class="badge-side ${o.side}">${o.side}</span></td>
                    <td style="font-size: 0.75rem; font-weight:600;">${o.type}</td>
                    <td class="font-mono">${priceStr}</td>
                    <td class="font-mono">${parseFloat(o.quantity).toFixed(4)}</td>
                    <td><span class="badge-status ${o.status}">${o.status}</span></td>
                </tr>`;
        }).join('');
    }

    // Load Audit Logs
    async function loadAuditLogs() {
        const logs = await apiRequest('/api/audit-logs');
        
        if (!logs || logs.length === 0) {
            auditLogList.innerHTML = `<div class="audit-entry-placeholder">No logs recorded yet.</div>`;
            return;
        }

        auditLogList.innerHTML = logs.map(log => {
            const timeStr = new Date(log.createdAt).toLocaleTimeString();
            
            let typeClass = '';
            if (log.eventType.includes('FILLED')) typeClass = 'FILLED';
            else if (log.eventType.includes('PLACED')) typeClass = 'PLACED';
            else if (log.eventType.includes('SUCCESS')) typeClass = 'SETTLEMENT_SUCCESS';
            else if (log.eventType.includes('FAILED')) typeClass = 'SETTLEMENT_FAILED';
            
            return `
                <div class="audit-entry ${typeClass}">
                    <div class="audit-entry-header">
                        <span class="audit-type">${log.eventType}</span>
                        <span class="audit-time">${timeStr}</span>
                    </div>
                    <div class="audit-desc">${log.description}</div>
                </div>`;
        }).join('');
    }

    // --- FORM EVENTS ---

    // Toggle Price field based on Order Type
    typeSelect.addEventListener('change', () => {
        if (typeSelect.value === 'MARKET') {
            priceGroup.style.opacity = '0.3';
            priceInput.disabled = true;
            priceInput.required = false;
            priceInput.value = '';
        } else {
            priceGroup.style.opacity = '1';
            priceInput.disabled = false;
            priceInput.required = true;
            // Autofill with ticker price if available
            const sym = symbolSelect.value;
            if (previousPrices[sym]) {
                priceInput.value = Math.round(previousPrices[sym]);
            }
        }
    });

    // Toggle Idempotency details
    idempotencyToggle.addEventListener('click', () => {
        idempotencyBody.classList.toggle('hidden');
        idempotencyToggle.querySelector('.toggle-icon').textContent = 
            idempotencyBody.classList.contains('hidden') ? '＋' : '－';
    });

    regenKeyBtn.addEventListener('click', resetIdempotencyKey);

    // Submit Order
    orderForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        
        const side = document.querySelector('input[name="side"]:checked').value;
        const symbol = symbolSelect.value;
        const type = typeSelect.value;
        const quantity = parseFloat(qtyInput.value);
        const price = type === 'LIMIT' ? parseFloat(priceInput.value) : null;
        const key = idempotencyInput.value.trim() || generateUUID();
        
        const orderPayload = { symbol, side, type, quantity, price };
        
        try {
            submitOrderBtn(true);
            const savedOrder = await apiRequest('/api/orders', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Idempotency-Key': key
                },
                body: JSON.stringify(orderPayload)
            });
            
            showToast('Order Placed', `Order ${savedOrder.id.substring(0, 8)} placed successfully!`, 'success');
            
            // Clear inputs
            qtyInput.value = '';
            if (type === 'LIMIT') priceInput.value = '';
            
            // Regen idempotency key for next order
            resetIdempotencyKey();
            
            // Reload immediately
            await refreshDashboardData();
        } catch (error) {
            console.error('Order submission failed:', error);
            showToast('Submission Failed', error.message, 'error');
        } finally {
            submitOrderBtn(false);
        }
    });

    function submitOrderBtn(loading) {
        const btn = document.getElementById('submit-order-btn');
        btn.disabled = loading;
        btn.textContent = loading ? 'Submitting...' : 'Submit Order';
    }

    // Cancel Order (attached globally)
    window.cancelOrder = async function(id) {
        try {
            await apiRequest(`/api/orders/${id}`, {
                method: 'DELETE'
            });
            showToast('Order Cancelled', `Successfully cancelled order: ${id.substring(0, 8)}`, 'success');
            await refreshDashboardData();
        } catch (error) {
            console.error('Cancellation failed:', error);
            showToast('Cancellation Failed', error.message, 'error');
        }
    };

    // --- SETTLEMENT ---

    runSettlementBtn.addEventListener('click', async () => {
        try {
            runSettlementBtn.disabled = true;
            runSettlementBtn.textContent = 'Processing...';
            
            const resultText = await apiRequest('/api/settlement', {
                method: 'POST'
            });
            
            const isSuccess = resultText.includes('SUCCESS');
            
            // Display report
            settlementReportContainer.classList.remove('hidden');
            reportStatus.textContent = isSuccess ? 'SUCCESS' : 'FAILED';
            reportStatus.className = 'report-status-badge ' + (isSuccess ? 'SUCCESS' : 'FAILED');
            reportTime.textContent = new Date().toLocaleTimeString();
            reportText.textContent = resultText;
            
            showToast('Settlement Run Complete', isSuccess ? 'Reconciliation passed with 0 discrepancies!' : 'Reconciliation failed! Check report.', isSuccess ? 'success' : 'error');
            await refreshDashboardData();
        } catch (error) {
            console.error('Settlement request failed:', error);
            showToast('Settlement Failed', error.message, 'error');
        } finally {
            runSettlementBtn.disabled = false;
            runSettlementBtn.textContent = 'Run EOD Settlement';
        }
    });

    clearSettlementBtn.addEventListener('click', () => {
        settlementReportContainer.classList.add('hidden');
        reportText.textContent = '';
    });

    // --- TABS CONTROL ---
    tabBtns.forEach(btn => {
        btn.addEventListener('click', () => {
            // Deactivate other tabs
            tabBtns.forEach(b => b.classList.remove('active'));
            tabContents.forEach(c => c.classList.remove('active'));
            
            // Activate current
            btn.classList.add('active');
            const tabId = btn.getAttribute('data-tab');
            document.getElementById(`tab-${tabId}`).classList.add('active');
        });
    });

    // --- INITIALIZATION ---
    resetIdempotencyKey();
    connectPriceStream();
    
    // Initial fetch
    refreshDashboardData();
    
    // Auto-refresh poll every 3 seconds
    const refreshInterval = 3000;
    let refreshCountdown = refreshInterval / 1000;
    const refreshStatusEl = document.getElementById('refresh-status');
    
    setInterval(() => {
        refreshCountdown--;
        if (refreshCountdown <= 0) {
            refreshStatusEl.textContent = 'Auto refreshing...';
            refreshDashboardData().then(() => {
                refreshCountdown = refreshInterval / 1000;
                refreshStatusEl.textContent = `Auto refreshing in ${refreshCountdown}s`;
            });
        } else {
            refreshStatusEl.textContent = `Auto refreshing in ${refreshCountdown}s`;
        }
    }, 1000);
});
