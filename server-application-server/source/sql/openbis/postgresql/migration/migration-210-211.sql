-- Align migrated databases with schema changes in 211:
-- remove file format types and the external_data reference to them.

ALTER TABLE IF EXISTS external_data DROP CONSTRAINT IF EXISTS exda_ffty_fk;

DROP INDEX IF EXISTS exda_ffty_fk_i;

ALTER TABLE IF EXISTS external_data DROP COLUMN IF EXISTS ffty_id;

DROP TABLE IF EXISTS file_format_types CASCADE;

DROP SEQUENCE IF EXISTS file_format_type_id_seq;
