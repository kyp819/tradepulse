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
            <span className="toggle-icon">{idempotencyOpen ? '－' : '＋'}</span>
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
