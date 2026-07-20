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
