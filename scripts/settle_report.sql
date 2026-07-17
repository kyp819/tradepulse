-- Total trades and total volume (USD)
SELECT 
    COUNT(*) AS total_trades,
    COALESCE(SUM(price * quantity), 0.0) AS total_volume_usd
FROM trades;

-- Net positions
SELECT 
    symbol,
    quantity AS net_position,
    average_price AS avg_execution_price
FROM positions
ORDER BY symbol;
