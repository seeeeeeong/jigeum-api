CREATE INDEX IF NOT EXISTS idx_cafes_location
ON cafes USING GIST(location);

CREATE INDEX IF NOT EXISTS idx_cafes_place_id
ON cafes(place_id);

CREATE INDEX IF NOT EXISTS idx_cafe_operating_hours_composite
ON cafe_operating_hours(place_id, day_of_week, open_time, close_time);

CREATE INDEX IF NOT EXISTS idx_cafe_operating_hours_place_id
ON cafe_operating_hours(place_id);

CREATE INDEX IF NOT EXISTS idx_cafe_raw_data_processed_batch
ON cafe_raw_data(processed, batch_id);

CREATE INDEX IF NOT EXISTS idx_cafe_raw_data_place_id
ON cafe_raw_data(place_id);

CREATE INDEX IF NOT EXISTS idx_batch_jobs_status_type
ON batch_jobs(status, job_type, started_at);

CREATE INDEX IF NOT EXISTS idx_batch_jobs_batch_id
ON batch_jobs(batch_id);

ANALYZE cafes;
ANALYZE cafe_operating_hours;
ANALYZE cafe_raw_data;
ANALYZE batch_jobs;

DO $$
BEGIN
    RAISE NOTICE 'Indexes created successfully';
    RAISE NOTICE 'Run EXPLAIN ANALYZE on your queries to verify index usage';
END $$;
