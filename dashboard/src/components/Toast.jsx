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
