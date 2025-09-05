ALTER TABLE payments
  DROP CONSTRAINT IF EXISTS chk_payments_status;

ALTER TABLE payments
  ADD CONSTRAINT chk_payments_status
  CHECK (status IN ('PENDING','AUTHORIZED','SUCCESSFUL','FAILED','REFUNDED'));
