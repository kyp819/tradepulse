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
