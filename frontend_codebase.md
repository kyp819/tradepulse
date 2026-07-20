# TradePulse Frontend Codebase

## File: C:\Users\kyp81\.gemini\antigravity-ide\scratch\tradepulse\dashboard\src\components\AuditLog.jsx
``javascript
import React from 'react';

export default function AuditLog({ logs, isLoading }) {
  return (
    <div className="dashboard-card flex-grow-card">
      <h2 className="card-title">System Audit Log</h2>
      <div className="audit-log-container" id="audit-log-list">
        {isLoading ? (
          <div className="audit-entry-placeholder">Loading audit events...</div>
        ) : logs.length === 0 ? (
          <div className="audit-entry-placeholder">No logs recorded yet.</div>
        ) : (
          logs.map((log, index) => {
            const timeStr = new Date(log.createdAt).toLocaleTimeString();
            
            let typeClass = '';
            if (log.eventType.includes('FILLED')) typeClass = 'FILLED';
            else if (log.eventType.includes('PLACED')) typeClass = 'PLACED';
            else if (log.eventType.includes('SUCCESS')) typeClass = 'SETTLEMENT_SUCCESS';
            else if (log.eventType.includes('FAILED')) typeClass = 'SETTLEMENT_FAILED';

            return (
              <div className={`audit-entry ${typeClass}`} key={log.id || `${log.createdAt}-${index}`}>
                <div className="audit-entry-header">
                  <span className="audit-type">{log.eventType}</span>
                  <span className="audit-time">{timeStr}</span>
                </div>
                <div className="audit-desc">{log.description}</div>
              </div>
            );
          })
        )}
      </div>
    </div>
  );
}

``

## File: C:\Users\kyp81\.gemini\antigravity-ide\scratch\tradepulse\dashboard\src\components\Footer.jsx
``javascript
import React, { useEffect, useState } from 'react';

export default function Footer({ countdown, isRefreshing }) {
  const [time, setTime] = useState(new Date().toTimeString().split(' ')[0]);

  useEffect(() => {
    const timer = setInterval(() => {
      setTime(new Date().toTimeString().split(' ')[0]);
    }, 1000);
    return () => clearInterval(timer);
  }, []);

  return (
    <footer className="main-footer">
      <span className="system-time">
        System Time: <span id="clock">{time}</span>
      </span>
      <span className="refresh-indicator" id="refresh-status">
        {isRefreshing ? 'Auto refreshing...' : `Auto refreshing in ${countdown}s`}
      </span>
    </footer>
  );
}

``

## File: C:\Users\kyp81\.gemini\antigravity-ide\scratch\tradepulse\dashboard\src\components\Header.jsx
``javascript
import React from 'react';
import { Wifi, WifiOff, Sun, Moon } from 'lucide-react';

export default function Header({ apiUrl, setApiUrl, isApiConnected, isStreamConnected, theme, toggleTheme }) {
  return (
    <header className="main-header">
      <div className="logo-area">
        <div className={`pulse-ring ${isApiConnected ? 'active' : 'disconnected'}`}></div>
        <span className="logo-text">TRADE<span className="highlight">PULSE</span></span>
        <span className="badge">OMS React v2.0</span>
      </div>
      
      <div className="header-status">
        <div className={`status-indicator ${isApiConnected ? 'connected' : 'disconnected'}`} id="connection-status">
          <span className="status-dot"></span>
          <span className="status-text">
            {isApiConnected ? 'ORDER SERVICE ONLINE' : 'ORDER SERVICE OFFLINE'}
          </span>
        </div>
        
        <div className={`status-indicator ${isStreamConnected ? 'active-stream' : 'disconnected'}`} id="price-stream-status">
          <span className="status-dot"></span>
          <span className="status-text">
            {isStreamConnected ? 'TICKSTREAM LIVE' : 'TICKSTREAM DISCONNECTED'}
          </span>
        </div>

        {/* Theme Toggle Button */}
        <button 
          className="btn-subtle" 
          onClick={toggleTheme} 
          title={`Switch to ${theme === 'dark' ? 'light' : 'dark'} mode`}
          style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '0.65rem' }}
        >
          {theme === 'dark' ? <Sun size={16} /> : <Moon size={16} />}
        </button>

        <div className="api-config">
          <input 
            type="text" 
            id="api-url" 
            value={apiUrl} 
            onChange={(e) => setApiUrl(e.target.value)}
            placeholder="API URL" 
            title="Configure Order Service API base URL"
          />
        </div>
      </div>
    </header>
  );
}

``

## File: C:\Users\kyp81\.gemini\antigravity-ide\scratch\tradepulse\dashboard\src\components\LiveTickers.jsx
``javascript
import React, { useEffect, useRef, useState } from 'react';

function TickerCard({ symbol, displayName, data }) {
  const [flashClass, setFlashClass] = useState('');
  const prevPriceRef = useRef(null);

  const price = data?.price ? parseFloat(data.price) : null;
  const changePercent = data?.changePercent ? parseFloat(data.changePercent) : null;

  useEffect(() => {
    if (price !== null) {
      if (prevPriceRef.current !== null) {
        if (price > prevPriceRef.current) {
          setFlashClass('flash-green');
        } else if (price < prevPriceRef.current) {
          setFlashClass('flash-red');
        }
        
        const timer = setTimeout(() => {
          setFlashClass('');
        }, 800); // 800ms corresponds to flash animation duration
        return () => clearTimeout(timer);
      }
      prevPriceRef.current = price;
    }
  }, [price]);

  const isPos = changePercent >= 0;
  const formattedPrice = price ? price.toLocaleString(undefined, { minimumFractionDigits: 2 }) : 'Loading...';
  const formattedChange = changePercent !== null ? `${isPos ? '+' : ''}${changePercent.toFixed(2)}% (24h)` : 'Waiting for stream';

  let arrowSymbol = 'âˆ’';
  let arrowColor = 'var(--color-text-muted)';
  if (flashClass === 'flash-green') {
    arrowSymbol = 'â–²';
    arrowColor = 'var(--color-green)';
  } else if (flashClass === 'flash-red') {
    arrowSymbol = 'â–¼';
    arrowColor = 'var(--color-red)';
  } else if (changePercent !== null) {
    if (isPos) {
      arrowSymbol = 'â–²';
      arrowColor = 'var(--color-green)';
    } else {
      arrowSymbol = 'â–¼';
      arrowColor = 'var(--color-red)';
    }
  }

  return (
    <div className="ticker-card" id={`ticker-${symbol}`}>
      <div className="ticker-header">
        <span className="ticker-symbol">{displayName}</span>
        <span className="ticker-arrow" style={{ color: arrowColor }}>{arrowSymbol}</span>
      </div>
      <div className={`ticker-price ${flashClass}`} id={`price-${symbol}`}>
        {price ? `$${formattedPrice}` : formattedPrice}
      </div>
      <div className={`ticker-change ${changePercent !== null ? (isPos ? 'pos' : 'neg') : ''}`} id={`change-${symbol}`}>
        {formattedChange}
      </div>
    </div>
  );
}

export default function LiveTickers({ prices }) {
  return (
    <div className="dashboard-card card-glow">
      <h2 className="card-title">Live Tickers</h2>
      <div className="ticker-container">
        <TickerCard symbol="BTCUSDT" displayName="BTC/USDT" data={prices.BTCUSDT} />
        <TickerCard symbol="ETHUSDT" displayName="ETH/USDT" data={prices.ETHUSDT} />
      </div>
    </div>
  );
}

``

## File: C:\Users\kyp81\.gemini\antigravity-ide\scratch\tradepulse\dashboard\src\components\OrderForm.jsx
``javascript
import React, { useState, useEffect } from 'react';

export default function OrderForm({ prices, onSubmitOrder, isSubmitting }) {
  const [side, setSide] = useState('BUY');
  const [symbol, setSymbol] = useState('BTCUSDT');
  const [type, setType] = useState('LIMIT');
  const [quantity, setQuantity] = useState('');
  const [price, setPrice] = useState('');
  const [idempotencyKey, setIdempotencyKey] = useState('');
  const [idempotencyOpen, setIdempotencyOpen] = useState(false);

  // Generate UUID v4 for Idempotency Key
  const generateUUID = () => {
    try {
      return crypto.randomUUID();
    } catch (e) {
      return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
        const r = Math.random() * 16 | 0;
        const v = c === 'x' ? r : (r & 0x3 | 0x8);
        return v.toString(16);
      });
    }
  };

  const regenKey = () => {
    setIdempotencyKey(generateUUID());
  };

  // Generate key on mount
  useEffect(() => {
    regenKey();
  }, []);

  // Autofill price if changing type or symbol
  useEffect(() => {
    if (type === 'LIMIT' && prices[symbol]?.price) {
      setPrice(Math.round(parseFloat(prices[symbol].price)).toString());
    } else if (type === 'MARKET') {
      setPrice('');
    }
  }, [type, symbol, prices]);

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!quantity) return;
    const payload = {
      symbol,
      side,
      type,
      quantity: parseFloat(quantity),
      price: type === 'LIMIT' ? parseFloat(price) : null
    };
    onSubmitOrder(payload, idempotencyKey.trim() || generateUUID()).then((success) => {
      if (success) {
        setQuantity('');
        // Regenerate key for the next order
        regenKey();
      }
    });
  };

  return (
    <div className="dashboard-card">
      <h2 className="card-title">Place Limit/Market Order</h2>
      <form onSubmit={handleSubmit} className="order-form">
        <div className="form-row side-toggle-row">
          <input 
            type="radio" 
            name="side" 
            id="side-buy" 
            value="BUY" 
            checked={side === 'BUY'} 
            onChange={() => setSide('BUY')}
          />
          <label htmlFor="side-buy" className="toggle-btn toggle-buy">BUY</label>
          
          <input 
            type="radio" 
            name="side" 
            id="side-sell" 
            value="SELL"
            checked={side === 'SELL'}
            onChange={() => setSide('SELL')}
          />
          <label htmlFor="side-sell" className="toggle-btn toggle-sell">SELL</label>
        </div>

        <div className="form-group">
          <label htmlFor="symbol-select">Asset Symbol</label>
          <select 
            id="symbol-select" 
            value={symbol}
            onChange={(e) => setSymbol(e.target.value)}
            required
          >
            <option value="BTCUSDT">BTCUSDT (Bitcoin / USDT)</option>
            <option value="ETHUSDT">ETHUSDT (Ethereum / USDT)</option>
          </select>
        </div>

        <div className="form-row">
          <div className="form-group col-half">
            <label htmlFor="type-select">Order Type</label>
            <select 
              id="type-select" 
              value={type}
              onChange={(e) => setType(e.target.value)}
              required
            >
              <option value="LIMIT">LIMIT</option>
              <option value="MARKET">MARKET</option>
            </select>
          </div>
          <div className="form-group col-half">
            <label htmlFor="order-qty">Quantity</label>
            <input 
              type="number" 
              id="order-qty" 
              step="any" 
              min="0.00000001" 
              placeholder="0.5" 
              value={quantity}
              onChange={(e) => setQuantity(e.target.value)}
              required
            />
          </div>
        </div>

        <div className="form-group" id="price-group" style={{ opacity: type === 'MARKET' ? 0.3 : 1 }}>
          <label htmlFor="order-price">Limit Price (USDT)</label>
          <input 
            type="number" 
            id="order-price" 
            step="any" 
            min="0.01" 
            placeholder="60000"
            value={price}
            onChange={(e) => setPrice(e.target.value)}
            disabled={type === 'MARKET'}
            required={type === 'LIMIT'}
          />
        </div>

        <div className="form-group collapsable-idempotency">
          <div 
            className="idempotency-header" 
            id="idempotency-toggle"
            onClick={() => setIdempotencyOpen(!idempotencyOpen)}
          >
            <span>Advanced: Idempotency Key</span>
            <span className="toggle-icon">{idempotencyOpen ? 'ï¼' : 'ï¼‹'}</span>
          </div>
          <div className={`idempotency-body ${idempotencyOpen ? '' : 'hidden'}`} id="idempotency-body">
            <input 
              type="text" 
              id="idempotency-key" 
              placeholder="Automatic UUID"
              value={idempotencyKey}
              onChange={(e) => setIdempotencyKey(e.target.value)}
            />
            <button 
              type="button" 
              className="btn-subtle" 
              id="regen-key-btn"
              onClick={regenKey}
            >
              Regenerate Key
            </button>
          </div>
        </div>

        <button 
          type="submit" 
          className="btn-primary" 
          id="submit-order-btn"
          disabled={isSubmitting}
        >
          {isSubmitting ? 'Submitting...' : 'Submit Order'}
        </button>
      </form>
    </div>
  );
}

``

## File: C:\Users\kyp81\.gemini\antigravity-ide\scratch\tradepulse\dashboard\src\components\OrdersTabs.jsx
``javascript
import React, { useState } from 'react';

export default function OrdersTabs({ orders, onCancelOrder, isLoading }) {
  const [activeTab, setActiveTab] = useState('active-orders');

  const pendingOrders = orders.filter((o) => o.status === 'PENDING');

  return (
    <div className="dashboard-card tabbed-card">
      <div className="card-header-tabs">
        <button 
          className={`tab-btn ${activeTab === 'active-orders' ? 'active' : ''}`}
          onClick={() => setActiveTab('active-orders')}
        >
          Active (Pending) ({pendingOrders.length})
        </button>
        <button 
          className={`tab-btn ${activeTab === 'all-orders' ? 'active' : ''}`}
          onClick={() => setActiveTab('all-orders')}
        >
          Order History ({orders.length})
        </button>
      </div>

      {/* Tab: Active Orders */}
      <div className={`tab-content ${activeTab === 'active-orders' ? 'active' : ''}`} id="tab-active-orders">
        <div className="table-container max-height-300">
          <table className="data-table" id="active-orders-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Symbol</th>
                <th>Side</th>
                <th>Price</th>
                <th>Qty</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              {isLoading ? (
                <tr className="no-data">
                  <td colSpan="6">Loading orders...</td>
                </tr>
              ) : pendingOrders.length === 0 ? (
                <tr className="no-data">
                  <td colSpan="6">No pending orders.</td>
                </tr>
              ) : (
                pendingOrders.map((order) => {
                  const priceStr = order.type === 'MARKET' ? 'MARKET' : '$' + parseFloat(order.price).toLocaleString();
                  const shortId = order.id.substring(0, 8) + '...';
                  return (
                    <tr key={order.id}>
                      <td className="font-mono text-muted" title={order.id}>{shortId}</td>
                      <td style={{ fontWeight: 600 }}>{order.symbol}</td>
                      <td><span className={`badge-side ${order.side}`}>{order.side}</span></td>
                      <td className="font-mono">{priceStr}</td>
                      <td className="font-mono">{parseFloat(order.quantity).toFixed(4)}</td>
                      <td>
                        <button 
                          className="btn-cancel" 
                          onClick={() => onCancelOrder(order.id)}
                        >
                          Cancel
                        </button>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Tab: All Orders History */}
      <div className={`tab-content ${activeTab === 'all-orders' ? 'active' : ''}`} id="tab-all-orders">
        <div className="table-container max-height-300">
          <table className="data-table" id="all-orders-table">
            <thead>
              <tr>
                <th>Created At</th>
                <th>Symbol</th>
                <th>Side</th>
                <th>Type</th>
                <th>Price</th>
                <th>Qty</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {isLoading ? (
                <tr className="no-data">
                  <td colSpan="7">Loading orders...</td>
                </tr>
              ) : orders.length === 0 ? (
                <tr className="no-data">
                  <td colSpan="7">No order history available.</td>
                </tr>
              ) : (
                orders.map((order) => {
                  const priceStr = order.type === 'MARKET' ? 'MARKET' : '$' + parseFloat(order.price).toLocaleString(undefined, { minimumFractionDigits: 2 });
                  const formattedTime = new Date(order.createdAt).toLocaleTimeString();
                  return (
                    <tr key={order.id}>
                      <td className="text-muted" style={{ fontSize: '0.75rem' }}>{formattedTime}</td>
                      <td style={{ fontWeight: 600 }}>{order.symbol}</td>
                      <td><span className={`badge-side ${order.side}`}>{order.side}</span></td>
                      <td style={{ fontSize: '0.75rem', fontWeight: 600 }}>{order.type}</td>
                      <td className="font-mono">{priceStr}</td>
                      <td className="font-mono">{parseFloat(order.quantity).toFixed(4)}</td>
                      <td><span className={`badge-status ${order.status}`}>{order.status}</span></td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

``

## File: C:\Users\kyp81\.gemini\antigravity-ide\scratch\tradepulse\dashboard\src\components\PositionsTable.jsx
``javascript
import React from 'react';

export default function PositionsTable({ positions, isLoading }) {
  return (
    <div className="dashboard-card card-glow-cyan">
      <h2 className="card-title">Net Positions</h2>
      <div className="table-container">
        <table className="data-table" id="positions-table">
          <thead>
            <tr>
              <th>Symbol</th>
              <th>Quantity</th>
              <th>Avg Price</th>
              <th>Updated At</th>
            </tr>
          </thead>
          <tbody>
            {isLoading ? (
              <tr className="no-data">
                <td colSpan="4">Loading positions...</td>
              </tr>
            ) : positions.length === 0 ? (
              <tr className="no-data">
                <td colSpan="4">No open positions found.</td>
              </tr>
            ) : (
              positions.map((pos) => {
                const qty = parseFloat(pos.quantity);
                const avgPrice = parseFloat(pos.averagePrice);
                const formattedTime = new Date(pos.updatedAt).toLocaleString();

                const qtyStyle = qty > 0 
                  ? { color: 'var(--color-green)', fontWeight: 700 } 
                  : qty < 0 
                    ? { color: 'var(--color-red)', fontWeight: 700 } 
                    : {};

                return (
                  <tr key={pos.symbol}>
                    <td style={{ fontWeight: 700, color: 'var(--color-cyan)' }}>{pos.symbol}</td>
                    <td className="font-mono" style={qtyStyle}>
                      {qty > 0 ? '+' : ''}{qty.toFixed(4)}
                    </td>
                    <td className="font-mono">
                      ${avgPrice.toLocaleString(undefined, { minimumFractionDigits: 2 })}
                    </td>
                    <td className="text-muted" style={{ fontSize: '0.75rem' }}>
                      {formattedTime}
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}

``

## File: C:\Users\kyp81\.gemini\antigravity-ide\scratch\tradepulse\dashboard\src\components\SettlementPanel.jsx
``javascript
import React from 'react';

export default function SettlementPanel({ onRunSettlement, settlementReport, setSettlementReport, isProcessing }) {
  const handleClearLog = () => {
    setSettlementReport(null);
  };

  return (
    <div className="dashboard-card card-glow-yellow">
      <h2 className="card-title">End-of-Day Settlement</h2>
      <div className="settlement-controls">
        <p className="settlement-desc">
          Run EOD reconciliation check to verify execution logs match position totals.
        </p>
        <div className="button-row">
          <button 
            className="btn-primary btn-yellow" 
            id="run-settlement-btn"
            onClick={onRunSettlement}
            disabled={isProcessing}
          >
            {isProcessing ? 'Processing...' : 'Run EOD Settlement'}
          </button>
          <button 
            className="btn-subtle" 
            id="clear-settlement-report-btn"
            onClick={handleClearLog}
            disabled={!settlementReport}
          >
            Clear Log
          </button>
        </div>
        {settlementReport && (
          <div className="settlement-report-display" id="settlement-report-container">
            <div className="report-header">
              <span className={`report-status-badge ${settlementReport.status}`}>
                {settlementReport.status}
              </span>
              <span id="report-time">{settlementReport.time}</span>
            </div>
            <pre className="report-content" id="report-text">
              {settlementReport.text}
            </pre>
          </div>
        )}
      </div>
    </div>
  );
}

``

## File: C:\Users\kyp81\.gemini\antigravity-ide\scratch\tradepulse\dashboard\src\components\Toast.jsx
``javascript
import React, { useEffect } from 'react';

export default function Toast({ toast, setToast }) {
  useEffect(() => {
    if (toast.show) {
      const timer = setTimeout(() => {
        setToast(prev => ({ ...prev, show: false }));
      }, 4000);
      return () => clearTimeout(timer);
    }
  }, [toast.show, setToast]);

  return (
    <div className={`toast ${toast.show ? 'show' : ''} ${toast.type}`} id="toast">
      <div className="toast-title" id="toast-title">{toast.title}</div>
      <div className="toast-message" id="toast-message">{toast.message}</div>
    </div>
  );
}

``

## File: C:\Users\kyp81\.gemini\antigravity-ide\scratch\tradepulse\dashboard\src\App.css
``css
.counter {
  font-size: 16px;
  padding: 5px 10px;
  border-radius: 5px;
  color: var(--accent);
  background: var(--accent-bg);
  border: 2px solid transparent;
  transition: border-color 0.3s;
  margin-bottom: 24px;

  &:hover {
    border-color: var(--accent-border);
  }
  &:focus-visible {
    outline: 2px solid var(--accent);
    outline-offset: 2px;
  }
}

.hero {
  position: relative;

  .base,
  .framework,
  .vite {
    inset-inline: 0;
    margin: 0 auto;
  }

  .base {
    width: 170px;
    position: relative;
    z-index: 0;
  }

  .framework,
  .vite {
    position: absolute;
  }

  .framework {
    z-index: 1;
    top: 34px;
    height: 28px;
    transform: perspective(2000px) rotateZ(300deg) rotateX(44deg) rotateY(39deg)
      scale(1.4);
  }

  .vite {
    z-index: 0;
    top: 107px;
    height: 26px;
    width: auto;
    transform: perspective(2000px) rotateZ(300deg) rotateX(40deg) rotateY(39deg)
      scale(0.8);
  }
}

#center {
  display: flex;
  flex-direction: column;
  gap: 25px;
  place-content: center;
  place-items: center;
  flex-grow: 1;

  @media (max-width: 1024px) {
    padding: 32px 20px 24px;
    gap: 18px;
  }
}

#next-steps {
  display: flex;
  border-top: 1px solid var(--border);
  text-align: left;

  & > div {
    flex: 1 1 0;
    padding: 32px;
    @media (max-width: 1024px) {
      padding: 24px 20px;
    }
  }

  .icon {
    margin-bottom: 16px;
    width: 22px;
    height: 22px;
  }

  @media (max-width: 1024px) {
    flex-direction: column;
    text-align: center;
  }
}

#docs {
  border-right: 1px solid var(--border);

  @media (max-width: 1024px) {
    border-right: none;
    border-bottom: 1px solid var(--border);
  }
}

#next-steps ul {
  list-style: none;
  padding: 0;
  display: flex;
  gap: 8px;
  margin: 32px 0 0;

  .logo {
    height: 18px;
  }

  a {
    color: var(--text-h);
    font-size: 16px;
    border-radius: 6px;
    background: var(--social-bg);
    display: flex;
    padding: 6px 12px;
    align-items: center;
    gap: 8px;
    text-decoration: none;
    transition: box-shadow 0.3s;

    &:hover {
      box-shadow: var(--shadow);
    }
    .button-icon {
      height: 18px;
      width: 18px;
    }
  }

  @media (max-width: 1024px) {
    margin-top: 20px;
    flex-wrap: wrap;
    justify-content: center;

    li {
      flex: 1 1 calc(50% - 8px);
    }

    a {
      width: 100%;
      justify-content: center;
      box-sizing: border-box;
    }
  }
}

#spacer {
  height: 88px;
  border-top: 1px solid var(--border);
  @media (max-width: 1024px) {
    height: 48px;
  }
}

.ticks {
  position: relative;
  width: 100%;

  &::before,
  &::after {
    content: '';
    position: absolute;
    top: -4.5px;
    border: 5px solid transparent;
  }

  &::before {
    left: 0;
    border-left-color: var(--border);
  }
  &::after {
    right: 0;
    border-right-color: var(--border);
  }
}

``

## File: C:\Users\kyp81\.gemini\antigravity-ide\scratch\tradepulse\dashboard\src\App.jsx
``javascript
import React, { useState, useEffect, useCallback } from 'react';
import Header from './components/Header';
import LiveTickers from './components/LiveTickers';
import OrderForm from './components/OrderForm';
import PositionsTable from './components/PositionsTable';
import OrdersTabs from './components/OrdersTabs';
import SettlementPanel from './components/SettlementPanel';
import AuditLog from './components/AuditLog';
import Footer from './components/Footer';
import Toast from './components/Toast';

export default function App() {
  // Configuration
  const [apiUrl, setApiUrl] = useState(() => {
    return localStorage.getItem('tradePulseApiUrl') || 'http://localhost:8080';
  });

  // Persist API URL configuration changes
  useEffect(() => {
    localStorage.setItem('tradePulseApiUrl', apiUrl);
  }, [apiUrl]);

  // Theme Management
  const [theme, setTheme] = useState(() => {
    return localStorage.getItem('tradePulseTheme') || 'light';
  });

  useEffect(() => {
    localStorage.setItem('tradePulseTheme', theme);
    if (theme === 'dark') {
      document.documentElement.classList.add('dark-theme');
    } else {
      document.documentElement.classList.remove('dark-theme');
    }
  }, [theme]);

  const toggleTheme = () => {
    setTheme(prev => prev === 'dark' ? 'light' : 'dark');
  };

  // States
  const [isApiConnected, setIsApiConnected] = useState(false);
  const [isStreamConnected, setIsStreamConnected] = useState(false);
  
  const [prices, setPrices] = useState({
    BTCUSDT: { price: null, changePercent: null },
    ETHUSDT: { price: null, changePercent: null }
  });

  const [positions, setPositions] = useState([]);
  const [orders, setOrders] = useState([]);
  const [auditLogs, setAuditLogs] = useState([]);
  const [settlementReport, setSettlementReport] = useState(null);

  // Loading States
  const [isSubmittingOrder, setIsSubmittingOrder] = useState(false);
  const [isProcessingSettlement, setIsProcessingSettlement] = useState(false);
  const [isLoadingData, setIsLoadingData] = useState(true);

  // Countdown Polling
  const [countdown, setCountdown] = useState(3);
  const [isRefreshing, setIsRefreshing] = useState(false);

  // Toast Notification State
  const [toast, setToast] = useState({
    show: false,
    title: '',
    message: '',
    type: 'info'
  });

  const showToast = useCallback((title, message, type = 'info') => {
    setToast({
      show: true,
      title,
      message,
      type
    });
  }, []);

  // API Client helper
  const apiRequest = useCallback(async (endpoint, options = {}) => {
    const cleanBase = apiUrl.trim().replace(/\/+$/, '');
    const url = `${cleanBase}${endpoint}`;
    
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
    const contentType = response.headers.get('content-type');
    if (contentType && contentType.includes('application/json')) {
      return response.json();
    }
    return response.text();
  }, [apiUrl]);

  // Data Fetching functions
  const loadPositions = useCallback(async () => {
    const data = await apiRequest('/api/positions');
    setPositions(data || []);
  }, [apiRequest]);

  const loadOrders = useCallback(async () => {
    const data = await apiRequest('/api/orders');
    if (data) {
      data.sort((a, b) => new Date(b.updatedAt) - new Date(a.updatedAt));
    }
    setOrders(data || []);
  }, [apiRequest]);

  const loadAuditLogs = useCallback(async () => {
    const data = await apiRequest('/api/audit-logs');
    setAuditLogs(data || []);
  }, [apiRequest]);

  // Refresh all dashboard metrics
  const refreshData = useCallback(async () => {
    setIsRefreshing(true);
    try {
      await Promise.all([
        loadPositions(),
        loadOrders(),
        loadAuditLogs()
      ]);
      setIsLoadingData(false);
      
      if (!isApiConnected) {
        setIsApiConnected(true);
        showToast('Connected', 'Successfully connected to order-service API.', 'success');
      }
    } catch (err) {
      console.error('API Poll failed:', err);
      setIsLoadingData(false);
      
      if (isApiConnected) {
        setIsApiConnected(false);
        showToast('API Connection Lost', 'Cannot reach ' + apiUrl, 'error');
      }
      
      setPositions([]);
      setOrders([]);
      setAuditLogs([]);
    } finally {
      setIsRefreshing(false);
    }
  }, [apiUrl, isApiConnected, loadPositions, loadOrders, loadAuditLogs, showToast]);

  // Initial Data Load
  useEffect(() => {
    setIsLoadingData(true);
    refreshData();
  }, [apiUrl]);

  // Countdown timer for automatic polling
  useEffect(() => {
    const interval = setInterval(() => {
      setCountdown((prev) => {
        if (prev <= 1) {
          refreshData();
          return 3;
        }
        return prev - 1;
      });
    }, 1000);
    return () => clearInterval(interval);
  }, [refreshData]);

  // Binance Websocket Feed connection
  useEffect(() => {
    let ws = null;
    let reconnectTimeout = null;

    const connectStream = () => {
      setIsStreamConnected(false);
      console.log('Connecting to Binance WebSocket...');
      
      ws = new WebSocket('wss://stream.binance.com:9443/ws/btcusdt@ticker/ethusdt@ticker');
      
      ws.onopen = () => {
        console.log('Binance WebSocket Connected!');
        setIsStreamConnected(true);
      };
      
      ws.onmessage = (event) => {
        try {
          const data = JSON.parse(event.data);
          const symbol = data.s;
          const price = parseFloat(data.c).toFixed(2);
          const changePercent = parseFloat(data.P).toFixed(2);
          
          setPrices((prev) => ({
            ...prev,
            [symbol]: { price, changePercent }
          }));
        } catch (e) {
          console.error('Failed to parse ticker payload:', e);
        }
      };
      
      ws.onclose = () => {
        console.warn('Binance WebSocket closed. Reconnecting in 5s...');
        setIsStreamConnected(false);
        reconnectTimeout = setTimeout(connectStream, 5000);
      };
      
      ws.onerror = (err) => {
        console.error('Binance WebSocket error:', err);
        ws.close();
      };
    };

    connectStream();

    // Clean up
    return () => {
      if (ws) ws.close();
      if (reconnectTimeout) clearTimeout(reconnectTimeout);
    };
  }, []);

  // Submitting Order Action
  const handleSubmitOrder = async (orderPayload, idempotencyKey) => {
    setIsSubmittingOrder(true);
    try {
      const savedOrder = await apiRequest('/api/orders', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Idempotency-Key': idempotencyKey
        },
        body: JSON.stringify(orderPayload)
      });

      showToast('Order Placed', `Order ${savedOrder.id.substring(0, 8)} placed successfully!`, 'success');
      await refreshData();
      setIsSubmittingOrder(false);
      return true;
    } catch (error) {
      console.error('Order submission failed:', error);
      showToast('Submission Failed', error.message, 'error');
      setIsSubmittingOrder(false);
      return false;
    }
  };

  // Cancelling Order Action
  const handleCancelOrder = async (id) => {
    try {
      await apiRequest(`/api/orders/${id}`, {
        method: 'DELETE'
      });
      showToast('Order Cancelled', `Successfully cancelled order: ${id.substring(0, 8)}`, 'success');
      await refreshData();
    } catch (error) {
      console.error('Cancellation failed:', error);
      showToast('Cancellation Failed', error.message, 'error');
    }
  };

  // Run EOD Settlement Action
  const handleRunSettlement = async () => {
    setIsProcessingSettlement(true);
    try {
      const resultText = await apiRequest('/api/settlement', {
        method: 'POST'
      });
      
      const isSuccess = resultText.includes('SUCCESS');
      
      setSettlementReport({
        status: isSuccess ? 'SUCCESS' : 'FAILED',
        time: new Date().toLocaleTimeString(),
        text: resultText
      });
      
      showToast(
        'Settlement Run Complete', 
        isSuccess ? 'Reconciliation passed with 0 discrepancies!' : 'Reconciliation failed! Check report.', 
        isSuccess ? 'success' : 'error'
      );
      
      await refreshData();
    } catch (error) {
      console.error('Settlement request failed:', error);
      showToast('Settlement Failed', error.message, 'error');
    } finally {
      setIsProcessingSettlement(false);
    }
  };

  return (
    <>
      <div className="glow-bg"></div>
      <div className="app-container">
        
        {/* Header */}
        <Header 
          apiUrl={apiUrl} 
          setApiUrl={setApiUrl} 
          isApiConnected={isApiConnected} 
          isStreamConnected={isStreamConnected} 
          theme={theme}
          toggleTheme={toggleTheme}
        />

        {/* Dashboard Grid */}
        <main className="dashboard-grid">
          
          {/* Column Left: Live Tickers & Order Entry */}
          <section className="grid-section col-left">
            <LiveTickers prices={prices} />
            <OrderForm 
              prices={prices} 
              onSubmitOrder={handleSubmitOrder} 
              isSubmitting={isSubmittingOrder} 
            />
          </section>

          {/* Column Middle: Net Positions & Orders Tabs */}
          <section className="grid-section col-mid">
            <PositionsTable 
              positions={positions} 
              isLoading={isLoadingData} 
            />
            <OrdersTabs 
              orders={orders} 
              onCancelOrder={handleCancelOrder} 
              isLoading={isLoadingData} 
            />
          </section>

          {/* Column Right: Settlement & System Audit Log */}
          <section className="grid-section col-right">
            <SettlementPanel 
              onRunSettlement={handleRunSettlement} 
              settlementReport={settlementReport} 
              setSettlementReport={setSettlementReport}
              isProcessing={isProcessingSettlement} 
            />
            <AuditLog 
              logs={auditLogs} 
              isLoading={isLoadingData} 
            />
          </section>

        </main>

        {/* Footer */}
        <Footer countdown={countdown} isRefreshing={isRefreshing} />

      </div>

      {/* Global Toast Alerts */}
      <Toast toast={toast} setToast={setToast} />
    </>
  );
}

``

## File: C:\Users\kyp81\.gemini\antigravity-ide\scratch\tradepulse\dashboard\src\index.css
``css
:root {
  /* Canvas & surfaces */
  --bg-primary: #eef1f5;
  --bg-secondary: #ffffff;
  --bg-card: #ffffff;
  --bg-card-hover: #fbfcfe;

  /* Dark chrome (header/footer instrument panel) */
  --chrome-bg: #0b1220;
  --chrome-bg-soft: #131b30;
  --chrome-border: rgba(255, 255, 255, 0.08);
  --chrome-text: #dbe2ee;
  --chrome-text-muted: #7b8494;
  --chrome-green: #34d399;
  --chrome-red: #f87171;
  --chrome-blue: #5b9dff;

  --border-color: #e1e5eb;
  --border-hover: #c7cdd6;

  --color-text: #0b1220;
  --color-text-muted: #5b6472;

  /* Accent Colors (light-canvas usage: cards, badges, tables) */
  --color-primary: #1e3a8a;
  --color-primary-hover: #16295e;
  --color-cyan: #0b72b9;
  --color-cyan-glow: rgba(11, 114, 185, 0.1);
  --color-green: #0f7b4b;
  --color-green-glow: rgba(15, 123, 75, 0.1);
  --color-red: #b3261e;
  --color-red-glow: rgba(179, 38, 30, 0.1);
  --color-yellow: #a9660a;
  --color-yellow-glow: rgba(169, 102, 10, 0.1);

  /* Fonts */
  --font-display: 'Space Grotesk', 'Inter', sans-serif;
  --font-sans: 'Inter', sans-serif;
  --font-mono: 'JetBrains Mono', monospace;

  --card-shadow: 0 1px 2px rgba(11, 18, 32, 0.04);
  --card-shadow-hover: 0 4px 16px rgba(11, 18, 32, 0.08);
  --transition-speed: 0.18s;
}

:root.dark-theme {
  /* Canvas & surfaces */
  --bg-primary: #070a13;
  --bg-secondary: #0c1020;
  --bg-card: rgba(16, 22, 42, 0.65);
  --bg-card-hover: rgba(24, 32, 64, 0.8);

  /* Dark chrome overrides */
  --chrome-bg: #040712;
  --chrome-bg-soft: #080d1a;

  --border-color: rgba(255, 255, 255, 0.07);
  --border-hover: rgba(255, 255, 255, 0.14);

  --color-text: #f1f5f9;
  --color-text-muted: #94a3b8;

  /* Accent Colors (dark-canvas usage) */
  --color-primary: #6366f1; /* Neon indigo */
  --color-primary-hover: #4f46e5;
  --color-cyan: #06b6d4;
  --color-cyan-glow: rgba(6, 182, 212, 0.15);
  --color-green: #10b981;
  --color-green-glow: rgba(16, 185, 129, 0.15);
  --color-red: #f43f5e;
  --color-red-glow: rgba(244, 63, 94, 0.15);
  --color-yellow: #f59e0b;
  --color-yellow-glow: rgba(245, 158, 11, 0.15);

  --card-shadow: 0 8px 32px 0 rgba(0, 0, 0, 0.37);
  --card-shadow-hover: 0 12px 48px 0 rgba(0, 0, 0, 0.5);
}

* {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
  scrollbar-width: thin;
  scrollbar-color: rgba(11, 18, 32, 0.18) transparent;
}

::-webkit-scrollbar { width: 6px; height: 6px; }
::-webkit-scrollbar-track { background: transparent; }
::-webkit-scrollbar-thumb { background: rgba(11, 18, 32, 0.18); border-radius: 4px; }
::-webkit-scrollbar-thumb:hover { background: rgba(11, 18, 32, 0.32); }

body {
  font-family: var(--font-sans);
  background-color: var(--bg-primary);
  color: var(--color-text);
  min-height: 100vh;
  position: relative;
  overflow-x: hidden;
  line-height: 1.5;
  font-size: 14px;
}

/* Signature: faint instrument-panel grid instead of a color-glow gradient */
.glow-bg {
  position: fixed;
  inset: 0;
  z-index: -1;
  pointer-events: none;
  background-color: var(--bg-primary);
  background-image: radial-gradient(rgba(11, 18, 32, 0.05) 1px, transparent 1px);
  background-size: 24px 24px;
  transition: background-color 0.3s ease;
}

:root.dark-theme .glow-bg {
  background-image: radial-gradient(rgba(255, 255, 255, 0.05) 1px, transparent 1px);
}

.app-container {
  max-width: 1600px;
  margin: 0 auto;
  padding: 0 1.5rem 1.5rem;
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  gap: 1.25rem;
}

/* --- HEADER (dark chrome) --- */
.main-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.9rem 1.5rem;
  background: var(--chrome-bg);
  border: 1px solid var(--chrome-border);
  border-top: none;
  border-radius: 0 0 10px 10px;
  margin: 0 -1.5rem;
}

/* --- GRID SYSTEM --- */
.dashboard-grid {
  display: grid;
  grid-template-columns: 1fr 1.25fr 1fr;
  gap: 1.25rem;
  flex-grow: 1;
  padding-top: 1.25rem;
}

@media (max-width: 1200px) {
  .dashboard-grid { grid-template-columns: 1fr 1.25fr; }
  .col-right { grid-column: span 2; }
}

@media (max-width: 800px) {
  .dashboard-grid { grid-template-columns: 1fr; }
  .col-right, .col-mid, .col-left { grid-column: span 1; }
  .main-header {
    flex-direction: column;
    gap: 0.75rem;
    align-items: stretch;
    border-radius: 0 0 10px 10px;
  }
  .header-status { flex-direction: column; align-items: stretch; }
}

.grid-section {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.logo-area {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.pulse-ring {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--chrome-green);
  box-shadow: 0 0 0 0 rgba(52, 211, 153, 0.5);
  animation: pulse-green 2s infinite;
}

.pulse-ring.disconnected {
  background: var(--chrome-red);
  box-shadow: 0 0 0 0 rgba(248, 113, 113, 0.5);
  animation: pulse-red 2s infinite;
}

.logo-text {
  font-family: var(--font-display);
  font-size: 1.2rem;
  font-weight: 700;
  letter-spacing: 0.5px;
  color: var(--chrome-text);
}

.logo-text .highlight {
  color: var(--chrome-blue);
}

.badge {
  background: rgba(91, 157, 255, 0.12);
  border: 1px solid rgba(91, 157, 255, 0.3);
  color: var(--chrome-blue);
  padding: 0.15rem 0.45rem;
  font-family: var(--font-mono);
  font-size: 0.65rem;
  font-weight: 600;
  border-radius: 3px;
  letter-spacing: 0.3px;
}

.header-status {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.status-indicator {
  display: flex;
  align-items: center;
  gap: 0.45rem;
  font-family: var(--font-mono);
  font-size: 0.68rem;
  font-weight: 600;
  letter-spacing: 0.4px;
  padding: 0.4rem 0.7rem;
  border-radius: 4px;
  background: var(--chrome-bg-soft);
  border: 1px solid var(--chrome-border);
  color: var(--chrome-text-muted);
}

.status-indicator .status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
}

.status-indicator.connected {
  border-color: rgba(52, 211, 153, 0.3);
  color: var(--chrome-green);
}
.status-indicator.connected .status-dot {
  background: var(--chrome-green);
  box-shadow: 0 0 6px rgba(52, 211, 153, 0.6);
}

.status-indicator.disconnected {
  border-color: rgba(248, 113, 113, 0.3);
  color: var(--chrome-red);
}
.status-indicator.disconnected .status-dot {
  background: var(--chrome-red);
  box-shadow: 0 0 6px rgba(248, 113, 113, 0.6);
}

.status-indicator.active-stream {
  border-color: rgba(91, 157, 255, 0.3);
  color: var(--chrome-blue);
}
.status-indicator.active-stream .status-dot {
  background: var(--chrome-blue);
  box-shadow: 0 0 6px rgba(91, 157, 255, 0.6);
}

.api-config { display: flex; align-items: center; }

.api-config input {
  background: #060a14;
  border: 1px solid var(--chrome-border);
  color: var(--chrome-text);
  padding: 0.45rem 0.7rem;
  font-size: 0.72rem;
  border-radius: 4px;
  width: 190px;
  outline: none;
  font-family: var(--font-mono);
  transition: all var(--transition-speed);
}

.api-config input::placeholder { color: var(--chrome-text-muted); }

.api-config input:focus {
  border-color: var(--chrome-blue);
  box-shadow: 0 0 0 3px rgba(91, 157, 255, 0.15);
}

/* --- CARDS & PANELS --- */
.dashboard-card {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  padding: 1.35rem;
  box-shadow: var(--card-shadow);
  display: flex;
  flex-direction: column;
  gap: 1.1rem;
  position: relative;
  transition: border-color var(--transition-speed), box-shadow var(--transition-speed);
}

.dashboard-card:hover {
  border-color: var(--border-hover);
  box-shadow: var(--card-shadow-hover);
}

/* Marker + eyebrow title system replaces gradient top-bars */
.card-title {
  font-family: var(--font-display);
  font-size: 0.82rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 1px;
  display: flex;
  align-items: center;
  gap: 0.55rem;
  color: var(--color-text);
  padding-bottom: 0.85rem;
  border-bottom: 1px solid var(--border-color);
}

.card-title::before {
  content: '';
  width: 8px;
  height: 8px;
  border-radius: 2px;
  background: var(--color-primary);
  flex-shrink: 0;
}

.dashboard-card.card-glow-cyan .card-title::before { background: var(--color-cyan); }
.dashboard-card.card-glow-yellow .card-title::before { background: var(--color-yellow); }

/* --- LIVE TICKERS --- */
.ticker-container { display: flex; gap: 1rem; }

.ticker-card {
  flex: 1;
  background: var(--bg-primary);
  border: 1px solid var(--border-color);
  border-radius: 6px;
  padding: 0.9rem 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
  transition: all var(--transition-speed);
}

.ticker-card:hover { border-color: var(--border-hover); }

.ticker-header { display: flex; justify-content: space-between; align-items: center; }

.ticker-symbol {
  font-family: var(--font-mono);
  font-size: 0.72rem;
  font-weight: 600;
  letter-spacing: 0.4px;
  color: var(--color-text-muted);
}

.ticker-arrow { font-size: 0.8rem; transition: color var(--transition-speed); }

.ticker-price {
  font-family: var(--font-mono);
  font-size: 1.5rem;
  font-weight: 700;
  letter-spacing: -0.5px;
  color: var(--color-text);
  font-variant-numeric: tabular-nums;
  transition: background-color 0.3s ease;
  border-radius: 3px;
  padding: 1px 3px;
  margin-left: -3px;
}

.ticker-change { font-family: var(--font-mono); font-size: 0.75rem; font-weight: 600; }
.ticker-change.pos { color: var(--color-green); }
.ticker-change.neg { color: var(--color-red); }

@keyframes flashGreen { 0% { background-color: rgba(15, 123, 75, 0.16); } 100% { background-color: transparent; } }
@keyframes flashRed { 0% { background-color: rgba(179, 38, 30, 0.16); } 100% { background-color: transparent; } }
.flash-green { animation: flashGreen 0.8s ease-out; }
.flash-red { animation: flashRed 0.8s ease-out; }

/* --- ORDER FORM --- */
.order-form { display: flex; flex-direction: column; gap: 1rem; }

.side-toggle-row {
  display: flex;
  background: var(--bg-primary);
  padding: 3px;
  border-radius: 6px;
  border: 1px solid var(--border-color);
}

.side-toggle-row input[type="radio"] { display: none; }

.side-toggle-row label {
  flex: 1;
  text-align: center;
  padding: 0.55rem;
  font-family: var(--font-mono);
  font-size: 0.8rem;
  font-weight: 700;
  letter-spacing: 0.5px;
  border-radius: 4px;
  cursor: pointer;
  transition: all var(--transition-speed);
  color: var(--color-text-muted);
}

.side-toggle-row input[id="side-buy"]:checked + label { background: var(--color-green); color: #fff; }
.side-toggle-row input[id="side-sell"]:checked + label { background: var(--color-red); color: #fff; }

.form-group { display: flex; flex-direction: column; gap: 0.4rem; }

.form-group label {
  font-size: 0.7rem;
  font-weight: 600;
  color: var(--color-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.6px;
}

.form-row { display: flex; gap: 1rem; }
.col-half { flex: 1; }

.form-group input, .form-group select {
  background: #ffffff;
  border: 1px solid var(--border-color);
  color: var(--color-text);
  padding: 0.6rem 0.75rem;
  font-size: 0.88rem;
  border-radius: 5px;
  outline: none;
  font-family: var(--font-sans);
  transition: all var(--transition-speed);
}

.form-group select option { background: #ffffff; color: var(--color-text); }
.form-group input[type="number"] { font-family: var(--font-mono); }

.form-group input:focus, .form-group select:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(30, 58, 138, 0.1);
}

.form-group input:disabled { opacity: 0.5; cursor: not-allowed; }

.collapsable-idempotency {
  border: 1px solid var(--border-color);
  border-radius: 5px;
  background: var(--bg-primary);
}

.idempotency-header {
  padding: 0.55rem 0.75rem;
  font-size: 0.78rem;
  font-weight: 600;
  color: var(--color-text-muted);
  display: flex;
  justify-content: space-between;
  align-items: center;
  cursor: pointer;
  user-select: none;
  transition: color var(--transition-speed);
}

.idempotency-header:hover { color: var(--color-text); }

.idempotency-body {
  padding: 0.75rem;
  border-top: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  gap: 0.65rem;
}

.idempotency-body input { font-size: 0.75rem; width: 100%; }
.hidden { display: none !important; }

/* --- BUTTONS --- */
.btn-primary {
  background: var(--color-primary);
  color: #fff;
  border: 1px solid var(--color-primary);
  border-radius: 5px;
  padding: 0.7rem 1rem;
  font-family: var(--font-mono);
  font-size: 0.82rem;
  font-weight: 600;
  letter-spacing: 0.4px;
  cursor: pointer;
  transition: all var(--transition-speed);
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 0.5rem;
}

.btn-primary:hover:not(:disabled) {
  background: var(--color-primary-hover);
  border-color: var(--color-primary-hover);
}

.btn-primary:disabled { opacity: 0.5; cursor: not-allowed; }

.btn-yellow { background: var(--color-yellow); border-color: var(--color-yellow); }
.btn-yellow:hover:not(:disabled) { background: #8a5209; border-color: #8a5209; }

.btn-subtle {
  background: #ffffff;
  color: var(--color-text-muted);
  border: 1px solid var(--border-color);
  border-radius: 5px;
  padding: 0.5rem 0.8rem;
  font-family: var(--font-mono);
  font-size: 0.75rem;
  font-weight: 600;
  cursor: pointer;
  transition: all var(--transition-speed);
}

.btn-subtle:hover:not(:disabled) {
  background: var(--bg-primary);
  color: var(--color-text);
  border-color: var(--border-hover);
}

.btn-cancel {
  background: #ffffff;
  color: var(--color-red);
  border: 1px solid rgba(179, 38, 30, 0.35);
  padding: 0.25rem 0.5rem;
  font-family: var(--font-mono);
  font-size: 0.7rem;
  font-weight: 700;
  border-radius: 3px;
  cursor: pointer;
  transition: all var(--transition-speed);
}

.btn-cancel:hover { background: var(--color-red); color: #fff; }

/* --- DATA TABLES --- */
.table-container { width: 100%; overflow-x: auto; }
.max-height-300 { max-height: 290px; overflow-y: auto; }

.data-table { width: 100%; border-collapse: collapse; text-align: left; font-size: 0.82rem; }

.data-table th {
  padding: 0.6rem 0.85rem;
  font-weight: 700;
  color: var(--color-text-muted);
  border-bottom: 2px solid var(--border-color);
  font-family: var(--font-mono);
  font-size: 0.66rem;
  text-transform: uppercase;
  letter-spacing: 0.6px;
}

.data-table td {
  padding: 0.65rem 0.85rem;
  border-bottom: 1px solid var(--border-color);
  vertical-align: middle;
  color: var(--color-text);
}

.data-table tbody tr:hover { background: var(--bg-primary); }

.data-table tr.no-data td {
  text-align: center;
  padding: 2rem;
  color: var(--color-text-muted);
  font-style: italic;
}

.font-mono { font-family: var(--font-mono); font-variant-numeric: tabular-nums; }
.text-muted { color: var(--color-text-muted); }

/* --- BADGES --- */
.badge-side {
  font-family: var(--font-mono);
  font-size: 0.65rem;
  font-weight: 700;
  letter-spacing: 0.3px;
  padding: 0.15rem 0.4rem;
  border-radius: 3px;
  display: inline-block;
}
.badge-side.BUY { background: rgba(15, 123, 75, 0.08); color: var(--color-green); border: 1px solid rgba(15, 123, 75, 0.3); }
.badge-side.SELL { background: rgba(179, 38, 30, 0.08); color: var(--color-red); border: 1px solid rgba(179, 38, 30, 0.3); }

.badge-status {
  font-family: var(--font-mono);
  font-size: 0.65rem;
  font-weight: 700;
  padding: 0.15rem 0.4rem;
  border-radius: 3px;
  display: inline-block;
  text-transform: uppercase;
  letter-spacing: 0.3px;
}
.badge-status.PENDING { background: rgba(169, 102, 10, 0.08); color: var(--color-yellow); border: 1px solid rgba(169, 102, 10, 0.3); }
.badge-status.FILLED { background: rgba(15, 123, 75, 0.08); color: var(--color-green); border: 1px solid rgba(15, 123, 75, 0.3); }
.badge-status.CANCELLED { background: var(--bg-primary); color: var(--color-text-muted); border: 1px solid var(--border-color); }

/* --- TABBED CARD --- */
.tabbed-card { padding: 0; gap: 0; }
.tabbed-card .card-title { display: none; }

.card-header-tabs {
  display: flex;
  border-bottom: 1px solid var(--border-color);
  background: var(--bg-primary);
  padding: 0 1rem;
  border-radius: 8px 8px 0 0;
}

.tab-btn {
  background: transparent;
  border: none;
  color: var(--color-text-muted);
  font-family: var(--font-mono);
  font-weight: 600;
  font-size: 0.76rem;
  letter-spacing: 0.3px;
  padding: 0.9rem 0.5rem;
  margin-right: 1.25rem;
  cursor: pointer;
  position: relative;
  transition: color var(--transition-speed);
}

.tab-btn:hover { color: var(--color-text); }
.tab-btn.active { color: var(--color-primary); }

.tab-btn.active::after {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 0;
  width: 100%;
  height: 2px;
  background: var(--color-primary);
}

.tab-content { display: none; padding: 1.1rem; }
.tab-content.active { display: block; }

/* --- SETTLEMENT --- */
.settlement-controls { display: flex; flex-direction: column; gap: 0.75rem; }
.settlement-desc { font-size: 0.82rem; color: var(--color-text-muted); }
.button-row { display: flex; gap: 0.75rem; }

.settlement-report-display {
  border: 1px solid var(--border-color);
  border-radius: 6px;
  background: var(--bg-primary);
  padding: 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.report-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-family: var(--font-mono);
  font-size: 0.72rem;
  font-weight: 600;
  color: var(--color-text-muted);
}

.report-status-badge { font-family: var(--font-mono); font-size: 0.65rem; font-weight: 800; padding: 0.15rem 0.4rem; border-radius: 3px; }
.report-status-badge.SUCCESS { background: rgba(15, 123, 75, 0.08); color: var(--color-green); border: 1px solid rgba(15, 123, 75, 0.3); }
.report-status-badge.FAILED { background: rgba(179, 38, 30, 0.08); color: var(--color-red); border: 1px solid rgba(179, 38, 30, 0.3); }

.report-content {
  font-family: var(--font-mono);
  font-size: 0.72rem;
  white-space: pre-wrap;
  max-height: 180px;
  overflow-y: auto;
  color: #333c4a;
  background: #ffffff;
  border: 1px solid var(--border-color);
  padding: 0.75rem;
  border-radius: 4px;
}

/* --- AUDIT LOGS --- */
.flex-grow-card { flex-grow: 1; }

.audit-log-container {
  display: flex;
  flex-direction: column;
  gap: 0.65rem;
  max-height: 250px;
  overflow-y: auto;
  padding-right: 4px;
}

.audit-entry-placeholder {
  text-align: center;
  font-style: italic;
  color: var(--color-text-muted);
  padding: 1.5rem;
  font-size: 0.82rem;
}

.audit-entry {
  background: var(--bg-primary);
  border: 1px solid var(--border-color);
  border-left: 2px solid var(--color-primary);
  border-radius: 4px;
  padding: 0.65rem 0.75rem;
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  transition: all var(--transition-speed);
}

.audit-entry:hover { border-color: var(--border-hover); }

.audit-entry.FILLED { border-left-color: var(--color-green); }
.audit-entry.PLACED { border-left-color: var(--color-cyan); }
.audit-entry.SETTLEMENT_SUCCESS { border-left-color: var(--color-green); }
.audit-entry.SETTLEMENT_FAILED { border-left-color: var(--color-red); }

.audit-entry-header { display: flex; justify-content: space-between; align-items: center; }

.audit-type { font-family: var(--font-mono); font-size: 0.7rem; font-weight: 700; letter-spacing: 0.3px; color: var(--color-text); }
.audit-time { font-size: 0.68rem; color: var(--color-text-muted); font-family: var(--font-mono); }
.audit-desc { font-size: 0.78rem; color: var(--color-text-muted); word-break: break-word; }

/* --- FOOTER (dark chrome, matches header) --- */
.main-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.65rem 1.5rem;
  background: var(--chrome-bg);
  border: 1px solid var(--chrome-border);
  border-radius: 8px;
  font-family: var(--font-mono);
  font-size: 0.7rem;
  color: var(--chrome-text-muted);
  margin: 0 -1.5rem -1.5rem;
}

.system-time #clock { font-weight: 700; color: var(--chrome-text); }
.refresh-indicator { font-weight: 600; }

/* --- TOAST NOTIFICATIONS --- */
.toast {
  position: fixed;
  bottom: 2rem;
  right: 2rem;
  background: #ffffff;
  border: 1px solid var(--border-color);
  border-left: 3px solid var(--color-primary);
  box-shadow: 0 12px 32px rgba(11, 18, 32, 0.18);
  border-radius: 6px;
  padding: 0.9rem 1.1rem;
  z-index: 1000;
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  transform: translateY(150%);
  opacity: 0;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
  min-width: 280px;
  max-width: 400px;
}

.toast.show { transform: translateY(0); opacity: 1; }
.toast.success { border-left-color: var(--color-green); }
.toast.error { border-left-color: var(--color-red); }

.toast-title { font-family: var(--font-mono); font-weight: 700; font-size: 0.85rem; color: var(--color-text); }
.toast.success .toast-title { color: var(--color-green); }
.toast.error .toast-title { color: var(--color-red); }
.toast-message { font-size: 0.78rem; color: var(--color-text-muted); }

/* Animations */
@keyframes pulse-green {
  0% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(52, 211, 153, 0.5); }
  70% { transform: scale(1); box-shadow: 0 0 0 6px rgba(52, 211, 153, 0); }
  100% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(52, 211, 153, 0); }
}
@keyframes pulse-red {
  0% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(248, 113, 113, 0.5); }
  70% { transform: scale(1); box-shadow: 0 0 0 6px rgba(248, 113, 113, 0); }
  100% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(248, 113, 113, 0); }
}

``

## File: C:\Users\kyp81\.gemini\antigravity-ide\scratch\tradepulse\dashboard\src\main.jsx
``javascript
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.jsx'

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <App />
  </StrictMode>,
)

``

## File: C:\Users\kyp81\.gemini\antigravity-ide\scratch\tradepulse\dashboard\.oxlintrc.json
``json
{
  "$schema": "./node_modules/oxlint/configuration_schema.json",
  "plugins": ["react", "oxc"],
  "rules": {
    "react/rules-of-hooks": "error",
    "react/only-export-components": ["warn", { "allowConstantExport": true }]
  }
}

``

## File: C:\Users\kyp81\.gemini\antigravity-ide\scratch\tradepulse\dashboard\index.html
``html
<!doctype html>
<html lang="en">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <meta name="description" content="TradePulse Real-Time Order Management System and Market Data Stream Dashboard" />
    <title>TradePulse â€” Real-Time OMS & Market Data Platform</title>

    <!-- Google Fonts -->
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Space+Grotesk:wght@500;600;700&family=Inter:wght@400;500;600;700&family=JetBrains+Mono:wght@400;500;600;700&display=swap" rel="stylesheet">
  </head>
  <body>
    <div id="root"></div>
    <script type="module" src="/src/main.jsx"></script>
  </body>
</html>

``

## File: C:\Users\kyp81\.gemini\antigravity-ide\scratch\tradepulse\dashboard\package.json
``json
{
  "name": "dashboard",
  "private": true,
  "version": "0.0.0",
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "vite build",
    "lint": "oxlint",
    "preview": "vite preview"
  },
  "dependencies": {
    "lucide-react": "^1.25.0",
    "react": "^19.2.7",
    "react-dom": "^19.2.7"
  },
  "devDependencies": {
    "@types/react": "^19.2.17",
    "@types/react-dom": "^19.2.3",
    "@vitejs/plugin-react": "^6.0.3",
    "oxlint": "^1.71.0",
    "vite": "^8.1.1"
  }
}

``


