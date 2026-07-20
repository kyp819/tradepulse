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
