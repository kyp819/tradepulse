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
