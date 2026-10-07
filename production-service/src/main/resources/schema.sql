CREATE TABLE IF NOT EXISTS production_runs (
  run_id TEXT PRIMARY KEY,
  configuration JSONB NOT NULL,
  expected_events INTEGER NOT NULL,
  end_millis BIGINT NOT NULL,
  status TEXT NOT NULL,
  error TEXT,
  input_version TEXT NOT NULL DEFAULT 'simulator-v1',
  mapping_version TEXT NOT NULL DEFAULT 'mapping-v1',
  format_version TEXT NOT NULL DEFAULT 'format-v1',
  calculation_version TEXT NOT NULL DEFAULT 'flink-metrics-v1'
);
CREATE TABLE IF NOT EXISTS production_facts (
  event_id TEXT PRIMARY KEY,
  run_id TEXT NOT NULL REFERENCES production_runs(run_id),
  batch_id TEXT NOT NULL,
  station INTEGER NOT NULL,
  original_millis BIGINT NOT NULL,
  good_delta INTEGER NOT NULL,
  rejected_delta INTEGER NOT NULL,
  wip_delta INTEGER NOT NULL,
  state TEXT NOT NULL,
  evidence JSONB NOT NULL
);
CREATE INDEX IF NOT EXISTS production_facts_run_time ON production_facts(run_id, original_millis);
