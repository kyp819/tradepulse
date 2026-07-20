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
