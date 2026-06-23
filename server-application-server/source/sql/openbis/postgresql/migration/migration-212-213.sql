-- Migration from 212 to 213

-- check if any to-be-removed predeployed scripts are still used, if so then abort the migration
-- (we don't want to automatically remove such scripts, they should be migrated first or deliberately removed)

DO $$
DECLARE
    rows_count BIGINT;
    check_query TEXT;
BEGIN
    -- experiment types using predeployed validation scripts
    check_query := 'SELECT code FROM experiment_types WHERE validation_script_id IN (SELECT id FROM scripts WHERE plugin_type = ''PREDEPLOYED'')';
    EXECUTE 'SELECT count(*) FROM (' || check_query || ') AS subquery' INTO rows_count;
    IF rows_count > 0 THEN
        RAISE EXCEPTION 'Migration 212->213 blocked. Some experiment types use to-be-removed predeployed validation scripts. SQL: "%" found % row(s). Change the configuration not to use such scripts and rerun the migration.', check_query, rows_count;
    END IF;

    -- sample types using predeployed validation scripts
    check_query := 'SELECT code FROM sample_types WHERE validation_script_id IN (SELECT id FROM scripts WHERE plugin_type = ''PREDEPLOYED'')';
    EXECUTE 'SELECT count(*) FROM (' || check_query || ') AS subquery' INTO rows_count;
    IF rows_count > 0 THEN
        RAISE EXCEPTION 'Migration 212->213 blocked. Some sample types use to-be-removed predeployed validation scripts. SQL: "%" found % row(s). Change the configuration not to use such scripts and rerun the migration.', check_query, rows_count;
    END IF;

    -- data set types using predeployed validation scripts
    check_query := 'SELECT code FROM data_set_types WHERE validation_script_id IN (SELECT id FROM scripts WHERE plugin_type = ''PREDEPLOYED'')';
    EXECUTE 'SELECT count(*) FROM (' || check_query || ') AS subquery' INTO rows_count;
    IF rows_count > 0 THEN
        RAISE EXCEPTION 'Migration 212->213 blocked. Some data set types use to-be-removed predeployed validation scripts. SQL: "%" found % row(s). Change the configuration not to use such scripts and rerun the migration.', check_query, rows_count;
    END IF;

    -- experiment type property assignments using predeployed dynamic scripts
    check_query := 'SELECT et.code as entity_type, pt.code as property_type FROM experiment_type_property_types etpt JOIN experiment_types et ON etpt.exty_id = et.id JOIN property_types pt ON etpt.prty_id = pt.id WHERE etpt.script_id in (SELECT id FROM scripts WHERE plugin_type = ''PREDEPLOYED'')';
    EXECUTE 'SELECT count(*) FROM (' || check_query || ') AS subquery' INTO rows_count;
    IF rows_count > 0 THEN
        RAISE EXCEPTION 'Migration 212->213 blocked. Some experiment type property assignments use to-be-removed predeployed dynamic scripts. SQL: "%" found % row(s). Change the configuration not to use such scripts and rerun the migration.', check_query, rows_count;
    END IF;

    -- sample type property assignments using predeployed dynamic scripts
    check_query := 'SELECT et.code as entity_type, pt.code as property_type FROM sample_type_property_types etpt JOIN sample_types et ON etpt.saty_id = et.id JOIN property_types pt ON etpt.prty_id = pt.id WHERE etpt.script_id in (SELECT id FROM scripts WHERE plugin_type = ''PREDEPLOYED'')';
    EXECUTE 'SELECT count(*) FROM (' || check_query || ') AS subquery' INTO rows_count;
    IF rows_count > 0 THEN
        RAISE EXCEPTION 'Migration 212->213 blocked. Some sample type property assignments use to-be-removed predeployed dynamic scripts. SQL: "%" found % row(s). Change the configuration not to use such scripts and rerun the migration.', check_query, rows_count;
    END IF;

    -- data set type property assignments using predeployed dynamic scripts
    check_query := 'SELECT et.code as entity_type, pt.code as property_type FROM data_set_type_property_types etpt JOIN data_set_types et ON etpt.dsty_id = et.id JOIN property_types pt ON etpt.prty_id = pt.id WHERE etpt.script_id in (SELECT id FROM scripts WHERE plugin_type = ''PREDEPLOYED'')';
    EXECUTE 'SELECT count(*) FROM (' || check_query || ') AS subquery' INTO rows_count;
    IF rows_count > 0 THEN
        RAISE EXCEPTION 'Migration 212->213 blocked. Some data set type property assignments use to-be-removed predeployed dynamic scripts. SQL: "%" found % row(s). Change the configuration not to use such scripts and rerun the migration.', check_query, rows_count;
    END IF;
END
$$;

-- delete predeployed plugins
DELETE FROM scripts WHERE plugin_type = 'PREDEPLOYED';

-- change 'script' column to not null
ALTER TABLE SCRIPTS DROP CONSTRAINT IF EXISTS SCRIPT_NN_CK;
ALTER TABLE SCRIPTS ALTER COLUMN SCRIPT SET NOT NULL;

-- change 'pluginType' domain not to accept 'PREDEPLOYED' value anymore
ALTER DOMAIN plugin_type DROP CONSTRAINT IF EXISTS plugin_type_check;
ALTER DOMAIN plugin_type ADD CONSTRAINT plugin_type_check CHECK (VALUE IN ('JYTHON'));