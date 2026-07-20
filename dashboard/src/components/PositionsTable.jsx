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
