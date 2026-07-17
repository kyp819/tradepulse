#!/usr/bin/env bash
set -euo pipefail

# Database settings (configurable via environment variables)
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-tradepulse}"
DB_USER="${DB_USER:-postgres}"
export PGPASSWORD="${DB_PASSWORD:-postgres}"

# Setup directories
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPORT_DIR="${SCRIPT_DIR}/../reports"
mkdir -p "${REPORT_DIR}"

DATE_STR="$(date +%Y-%m-%d)"
REPORT_FILE="${REPORT_DIR}/settlement_${DATE_STR}.txt"

echo "=========================================" > "${REPORT_FILE}"
echo "TradePulse EOD Settlement Report: ${DATE_STR}" >> "${REPORT_FILE}"
echo "Timestamp: $(date '+%Y-%m-%d %H:%M:%S')" >> "${REPORT_FILE}"
echo "=========================================" >> "${REPORT_FILE}"

echo "Running EOD settlement..."

# 1. Generate Settle Report
echo "" >> "${REPORT_FILE}"
echo "--- Trade Summary ---" >> "${REPORT_FILE}"
psql -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USER}" -d "${DB_NAME}" -f "${SCRIPT_DIR}/settle_report.sql" >> "${REPORT_FILE}" 2>&1

# 2. Run Reconciliation Checks
echo "" >> "${REPORT_FILE}"
echo "--- Reconciliation Checks ---" >> "${REPORT_FILE}"
RECONCILE_OUT=$(psql -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USER}" -d "${DB_NAME}" -f "${SCRIPT_DIR}/reconcile.sql" 2>&1)
echo "${RECONCILE_OUT}" >> "${REPORT_FILE}"

# Count lines in reconcile output to check for mismatches. 
# postgres output typically contains "(0 rows)" if no mismatches exist.
if echo "${RECONCILE_OUT}" | grep -q "(0 rows)"; then
    echo "Reconciliation: SUCCESS (0 mismatches found)"
    echo "Reconciliation: SUCCESS (0 mismatches found)" >> "${REPORT_FILE}"
    
    # Log success to DB
    psql -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USER}" -d "${DB_NAME}" -c "INSERT INTO audit_log (event_type, description, created_at) VALUES ('EOD_SETTLEMENT_SUCCESS', 'EOD settlement completed successfully. No reconciliation mismatches.', NOW());" > /dev/null
    exit 0
else
    echo "Reconciliation: FAILED (mismatches found or DB error occurred)"
    echo "Reconciliation: FAILED (mismatches found or DB error occurred)" >> "${REPORT_FILE}"
    
    # Log failure to DB
    psql -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USER}" -d "${DB_NAME}" -c "INSERT INTO audit_log (event_type, description, created_at) VALUES ('EOD_SETTLEMENT_FAILED', 'EOD settlement failed. Reconciliation mismatch or DB query error detected.', NOW());" > /dev/null
    exit 1
fi
