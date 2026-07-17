-- Sum trades (BUY as positive, SELL as negative) and compare with current positions
WITH trade_sums AS (
    SELECT 
        o.symbol,
        SUM(CASE WHEN o.side = 'BUY' THEN t.quantity ELSE -t.quantity END) as calculated_qty
    FROM trades t
    JOIN orders o ON t.order_id = o.id
    GROUP BY o.symbol
),
mismatches AS (
    SELECT 
        COALESCE(ts.symbol, p.symbol) AS symbol,
        COALESCE(ts.calculated_qty, 0.0) AS calculated_qty,
        COALESCE(p.quantity, 0.0) AS position_qty
    FROM trade_sums ts
    FULL OUTER JOIN positions p ON ts.symbol = p.symbol
    WHERE ABS(COALESCE(ts.calculated_qty, 0.0) - COALESCE(p.quantity, 0.0)) > 1e-8
)
SELECT * FROM mismatches;
