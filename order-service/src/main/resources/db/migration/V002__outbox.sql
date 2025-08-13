CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS outbox (
  id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  type         VARCHAR(64) NOT NULL,
  aggregate_id VARCHAR(64) NOT NULL,
  payload      JSONB NOT NULL,
  trace_id     VARCHAR(64),
  created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  processed_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS ix_outbox_processed_created
  ON outbox (processed_at, created_at);
