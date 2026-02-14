-- Sample data for fraud detection system

-- Insert sample transactions
INSERT INTO transactions (transaction_id, account_id, amount, currency, merchant_name, merchant_category, payment_method, location, description, risk_level, status, created_at) VALUES
  ('TXN001', 'ACC1001', 4999.99, 'USD', 'Amazon', 'RETAIL', 'CREDIT_CARD', 'USA', 'Online shopping', 'LOW', 'CLEARED', NOW()),
  ('TXN002', 'ACC1002', 299.00, 'USD', 'Uber', 'TRANSPORT', 'CREDIT_CARD', 'USA', 'Ride payment', 'LOW', 'CLEARED', NOW()),
  ('TXN003', 'ACC1001', 250000.00, 'USD', 'Luxury Store', 'RETAIL', 'WIRE_TRANSFER', 'Switzerland', 'High-value purchase', 'CRITICAL', 'REVIEW', NOW());
