-- Migration from 198 to 199

ALTER TABLE external_data ALTER COLUMN size TYPE bigint;
