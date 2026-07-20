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
