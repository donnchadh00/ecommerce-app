ALTER TABLE inventory_reservation
  DROP CONSTRAINT IF EXISTS chk_inv_res_status;

ALTER TABLE inventory_reservation
  ADD CONSTRAINT chk_inv_res_status
  CHECK (status IN ('PENDING','RESERVED','RELEASED','REJECTED','CONSUMED'));
