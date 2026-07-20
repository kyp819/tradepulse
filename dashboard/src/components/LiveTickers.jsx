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

  let arrowSymbol = '−';
  let arrowColor = 'var(--color-text-muted)';
  if (flashClass === 'flash-green') {
    arrowSymbol = '▲';
    arrowColor = 'var(--color-green)';
  } else if (flashClass === 'flash-red') {
    arrowSymbol = '▼';
    arrowColor = 'var(--color-red)';
  } else if (changePercent !== null) {
    if (isPos) {
      arrowSymbol = '▲';
      arrowColor = 'var(--color-green)';
    } else {
      arrowSymbol = '▼';
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
