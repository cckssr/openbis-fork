-- Keep FTS document version at 003, but align migrated databases with the updated
-- FTS trigger functions introduced for DB version 209.

-- Safety guard: do not allow migration if tables that will be dropped still contain data.
DO $$
DECLARE
    rows_count BIGINT;
    check_query TEXT;
    detected_rows TEXT;
BEGIN
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'material_types') THEN
        check_query := 'SELECT COUNT(*) FROM material_types';
        SELECT COUNT(*) INTO rows_count FROM material_types;
        IF rows_count > 0 THEN
            SELECT string_agg(format('id=%s, code=%s', id, code), '; ')
              INTO detected_rows
              FROM (SELECT id, code FROM material_types ORDER BY id LIMIT 10) t;
            RAISE EXCEPTION 'Migration 209->210 blocked. Check: "%". Detected % row(s). Example rows: %',
                check_query, rows_count, COALESCE(detected_rows, '<none>');
        END IF;
    END IF;

    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'material_type_property_types') THEN
        check_query := 'SELECT COUNT(*) FROM material_type_property_types';
        SELECT COUNT(*) INTO rows_count FROM material_type_property_types;
        IF rows_count > 0 THEN
            SELECT string_agg(format('id=%s, maty_id=%s, prty_id=%s', id, maty_id, prty_id), '; ')
              INTO detected_rows
              FROM (SELECT id, maty_id, prty_id FROM material_type_property_types ORDER BY id LIMIT 10) t;
            RAISE EXCEPTION 'Migration 209->210 blocked. Check: "%". Detected % row(s). Example rows: %',
                check_query, rows_count, COALESCE(detected_rows, '<none>');
        END IF;
    END IF;

    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'materials') THEN
        check_query := 'SELECT COUNT(*) FROM materials';
        SELECT COUNT(*) INTO rows_count FROM materials;
        IF rows_count > 0 THEN
            SELECT string_agg(format('id=%s, code=%s, maty_id=%s', id, code, maty_id), '; ')
              INTO detected_rows
              FROM (SELECT id, code, maty_id FROM materials ORDER BY id LIMIT 10) t;
            RAISE EXCEPTION 'Migration 209->210 blocked. Check: "%". Detected % row(s). Example rows: %',
                check_query, rows_count, COALESCE(detected_rows, '<none>');
        END IF;
    END IF;

    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'material_properties') THEN
        check_query := 'SELECT COUNT(*) FROM material_properties';
        SELECT COUNT(*) INTO rows_count FROM material_properties;
        IF rows_count > 0 THEN
            SELECT string_agg(format('id=%s, mate_id=%s, mtpt_id=%s', id, mate_id, mtpt_id), '; ')
              INTO detected_rows
              FROM (SELECT id, mate_id, mtpt_id FROM material_properties ORDER BY id LIMIT 10) t;
            RAISE EXCEPTION 'Migration 209->210 blocked. Check: "%". Detected % row(s). Example rows: %',
                check_query, rows_count, COALESCE(detected_rows, '<none>');
        END IF;
    END IF;

    IF EXISTS (SELECT FROM information_schema.columns WHERE table_name = 'property_types' AND column_name = 'maty_prop_id') THEN
        check_query := 'SELECT COUNT(*) FROM property_types WHERE maty_prop_id IS NOT NULL';
        SELECT COUNT(*) INTO rows_count FROM property_types WHERE maty_prop_id IS NOT NULL;
        IF rows_count > 0 THEN
            SELECT string_agg(format('id=%s, code=%s, maty_prop_id=%s', id, code, maty_prop_id), '; ')
              INTO detected_rows
              FROM (SELECT id, code, maty_prop_id FROM property_types WHERE maty_prop_id IS NOT NULL ORDER BY id LIMIT 10) t;
            RAISE EXCEPTION 'Migration 209->210 blocked. Check: "%". Detected % row(s). Example rows: %',
                check_query, rows_count, COALESCE(detected_rows, '<none>');
        END IF;
    END IF;

    IF EXISTS (SELECT FROM information_schema.columns WHERE table_name = 'experiment_properties' AND column_name = 'mate_prop_id') THEN
        check_query := 'SELECT COUNT(*) FROM experiment_properties WHERE mate_prop_id IS NOT NULL';
        SELECT COUNT(*) INTO rows_count FROM experiment_properties WHERE mate_prop_id IS NOT NULL;
        IF rows_count > 0 THEN
            SELECT string_agg(format('id=%s, expe_id=%s, etpt_id=%s, mate_prop_id=%s', id, expe_id, etpt_id, mate_prop_id), '; ')
              INTO detected_rows
              FROM (SELECT id, expe_id, etpt_id, mate_prop_id FROM experiment_properties WHERE mate_prop_id IS NOT NULL ORDER BY id LIMIT 10) t;
            RAISE EXCEPTION 'Migration 209->210 blocked. Check: "%". Detected % row(s). Example rows: %',
                check_query, rows_count, COALESCE(detected_rows, '<none>');
        END IF;
    END IF;

    IF EXISTS (SELECT FROM information_schema.columns WHERE table_name = 'sample_properties' AND column_name = 'mate_prop_id') THEN
        check_query := 'SELECT COUNT(*) FROM sample_properties WHERE mate_prop_id IS NOT NULL';
        SELECT COUNT(*) INTO rows_count FROM sample_properties WHERE mate_prop_id IS NOT NULL;
        IF rows_count > 0 THEN
            SELECT string_agg(format('id=%s, samp_id=%s, stpt_id=%s, mate_prop_id=%s', id, samp_id, stpt_id, mate_prop_id), '; ')
              INTO detected_rows
              FROM (SELECT id, samp_id, stpt_id, mate_prop_id FROM sample_properties WHERE mate_prop_id IS NOT NULL ORDER BY id LIMIT 10) t;
            RAISE EXCEPTION 'Migration 209->210 blocked. Check: "%". Detected % row(s). Example rows: %',
                check_query, rows_count, COALESCE(detected_rows, '<none>');
        END IF;
    END IF;

    IF EXISTS (SELECT FROM information_schema.columns WHERE table_name = 'data_set_properties' AND column_name = 'mate_prop_id') THEN
        check_query := 'SELECT COUNT(*) FROM data_set_properties WHERE mate_prop_id IS NOT NULL';
        SELECT COUNT(*) INTO rows_count FROM data_set_properties WHERE mate_prop_id IS NOT NULL;
        IF rows_count > 0 THEN
            SELECT string_agg(format('id=%s, ds_id=%s, dstpt_id=%s, mate_prop_id=%s', id, ds_id, dstpt_id, mate_prop_id), '; ')
              INTO detected_rows
              FROM (SELECT id, ds_id, dstpt_id, mate_prop_id FROM data_set_properties WHERE mate_prop_id IS NOT NULL ORDER BY id LIMIT 10) t;
            RAISE EXCEPTION 'Migration 209->210 blocked. Check: "%". Detected % row(s). Example rows: %',
                check_query, rows_count, COALESCE(detected_rows, '<none>');
        END IF;
    END IF;

    IF EXISTS (SELECT FROM information_schema.columns WHERE table_name = 'metaproject_assignments_all' AND column_name = 'mate_id') THEN
        check_query := 'SELECT COUNT(*) FROM metaproject_assignments_all WHERE mate_id IS NOT NULL';
        SELECT COUNT(*) INTO rows_count FROM metaproject_assignments_all WHERE mate_id IS NOT NULL;
        IF rows_count > 0 THEN
            SELECT string_agg(format('id=%s, mepr_id=%s, mate_id=%s', id, mepr_id, mate_id), '; ')
              INTO detected_rows
              FROM (SELECT id, mepr_id, mate_id FROM metaproject_assignments_all WHERE mate_id IS NOT NULL ORDER BY id LIMIT 10) t;
            RAISE EXCEPTION 'Migration 209->210 blocked. Check: "%". Detected % row(s). Example rows: %',
                check_query, rows_count, COALESCE(detected_rows, '<none>');
        END IF;
    END IF;

    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'data_types')
       AND EXISTS (SELECT FROM information_schema.columns WHERE table_name = 'property_types' AND column_name = 'daty_id') THEN
        check_query := 'SELECT COUNT(*) FROM property_types WHERE daty_id IN (SELECT id FROM data_types WHERE code = ''MATERIAL'')';
        SELECT COUNT(*) INTO rows_count
        FROM property_types
        WHERE daty_id IN (SELECT id FROM data_types WHERE code = 'MATERIAL');
        IF rows_count > 0 THEN
            SELECT string_agg(format('id=%s, code=%s, daty_id=%s', id, code, daty_id), '; ')
              INTO detected_rows
              FROM (
                  SELECT id, code, daty_id
                  FROM property_types
                  WHERE daty_id IN (SELECT id FROM data_types WHERE code = 'MATERIAL')
                  ORDER BY id
                  LIMIT 10
              ) t;
            RAISE EXCEPTION 'Migration 209->210 blocked. Check: "%". Detected % row(s). Example rows: %',
                check_query, rows_count, COALESCE(detected_rows, '<none>');
        END IF;
    END IF;

    IF EXISTS (SELECT FROM information_schema.columns WHERE table_name = 'queries' AND column_name = 'query_type') THEN
        check_query := 'SELECT COUNT(*) FROM queries WHERE query_type = ''MATERIAL''';
        SELECT COUNT(*) INTO rows_count FROM queries WHERE query_type = 'MATERIAL';
        IF rows_count > 0 THEN
            SELECT string_agg(format('id=%s, name=%s, query_type=%s', id, name, query_type), '; ')
              INTO detected_rows
              FROM (SELECT id, name, query_type FROM queries WHERE query_type = 'MATERIAL' ORDER BY id LIMIT 10) t;
            RAISE EXCEPTION 'Migration 209->210 blocked. Check: "%". Detected % row(s). Example rows: %',
                check_query, rows_count, COALESCE(detected_rows, '<none>');
        END IF;
    END IF;

    IF EXISTS (SELECT FROM information_schema.columns WHERE table_name = 'scripts' AND column_name = 'entity_kind') THEN
        check_query := 'SELECT COUNT(*) FROM scripts WHERE entity_kind = ''MATERIAL''';
        SELECT COUNT(*) INTO rows_count FROM scripts WHERE entity_kind = 'MATERIAL';
        IF rows_count > 0 THEN
            SELECT string_agg(format('id=%s, name=%s, entity_kind=%s', id, name, entity_kind), '; ')
              INTO detected_rows
              FROM (SELECT id, name, entity_kind FROM scripts WHERE entity_kind = 'MATERIAL' ORDER BY id LIMIT 10) t;
            RAISE EXCEPTION 'Migration 209->210 blocked. Check: "%". Detected % row(s). Example rows: %',
                check_query, rows_count, COALESCE(detected_rows, '<none>');
        END IF;
    END IF;

END
$$;

-- Align migrated databases with schema changes in 210:
-- remove material-related tables and sequences.
DROP TRIGGER IF EXISTS materials_tsvector_document ON materials;
DROP TRIGGER IF EXISTS material_properties_tsvector_document ON material_properties;
DROP TRIGGER IF EXISTS data_set_property_with_material_data_type_check ON data_set_properties;
DROP TRIGGER IF EXISTS experiment_property_with_material_data_type_check ON experiment_properties;
DROP TRIGGER IF EXISTS material_property_with_material_data_type_check ON material_properties;
DROP TRIGGER IF EXISTS sample_property_with_material_data_type_check ON sample_properties;

DROP FUNCTION IF EXISTS data_set_property_with_material_data_type_check();
DROP FUNCTION IF EXISTS experiment_property_with_material_data_type_check();
DROP FUNCTION IF EXISTS material_property_with_material_data_type_check();
DROP FUNCTION IF EXISTS materials_tsvector_document_trigger();
DROP FUNCTION IF EXISTS sample_property_with_material_data_type_check();

DO $$
BEGIN
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'material_properties_history') THEN
        DELETE FROM material_properties_history;
    END IF;
END $$;

DROP TABLE IF EXISTS material_properties_history CASCADE;
DROP TABLE IF EXISTS material_properties CASCADE;
DROP TABLE IF EXISTS materials CASCADE;
DROP TABLE IF EXISTS material_type_property_types CASCADE;
DROP TABLE IF EXISTS material_types CASCADE;

DROP SEQUENCE IF EXISTS material_property_id_seq;
DROP SEQUENCE IF EXISTS material_id_seq;
DROP SEQUENCE IF EXISTS mtpt_id_seq;
DROP SEQUENCE IF EXISTS material_type_id_seq;

-- Align migrated databases with the property history rules from DB version 210.

CREATE OR REPLACE RULE experiment_properties_update AS
    ON UPDATE TO experiment_properties
       WHERE (OLD.VALUE IS NOT NULL AND decode(replace(substring(OLD.value from 1 for 1), '\', '\\'), 'escape') != E'\\xefbfbd' AND OLD.VALUE != NEW.VALUE)
          OR (OLD.CVTE_ID IS NOT NULL AND OLD.CVTE_ID != NEW.CVTE_ID)
          OR (OLD.SAMP_PROP_ID IS NOT NULL AND OLD.SAMP_PROP_ID != NEW.SAMP_PROP_ID)
          OR (OLD.INTEGER_ARRAY_VALUE IS NOT NULL AND OLD.INTEGER_ARRAY_VALUE != NEW.INTEGER_ARRAY_VALUE)
          OR (OLD.REAL_ARRAY_VALUE IS NOT NULL AND OLD.REAL_ARRAY_VALUE != NEW.REAL_ARRAY_VALUE)
          OR (OLD.STRING_ARRAY_VALUE IS NOT NULL AND OLD.STRING_ARRAY_VALUE != NEW.STRING_ARRAY_VALUE)
          OR (OLD.TIMESTAMP_ARRAY_VALUE IS NOT NULL AND OLD.TIMESTAMP_ARRAY_VALUE != NEW.TIMESTAMP_ARRAY_VALUE)
          OR (OLD.JSON_VALUE IS NOT NULL AND OLD.JSON_VALUE != NEW.JSON_VALUE)
           DO ALSO
       INSERT INTO experiment_properties_history (
    ID,
    EXPE_ID,
    ETPT_ID,
    VALUE,
    VOCABULARY_TERM,
    SAMPLE,
    PERS_ID_AUTHOR,
    VALID_FROM_TIMESTAMP,
    VALID_UNTIL_TIMESTAMP,
    INTEGER_ARRAY_VALUE,
    REAL_ARRAY_VALUE,
    STRING_ARRAY_VALUE,
    TIMESTAMP_ARRAY_VALUE,
    JSON_VALUE
) VALUES (
                                       nextval('EXPERIMENT_PROPERTY_ID_SEQ'),
                                       OLD.EXPE_ID,
                                       OLD.ETPT_ID,
                                       OLD.VALUE,
                                       (select (t.code || ' [' || v.code || ']') from controlled_vocabulary_terms as t join controlled_vocabularies as v on t.covo_id = v.id where t.id = OLD.CVTE_ID),
                                       COALESCE((select perm_id from samples_all where id = OLD.SAMP_PROP_ID), OLD.SAMP_PROP_ID::text),
                                       OLD.PERS_ID_AUTHOR,
                                       OLD.MODIFICATION_TIMESTAMP,
                                       NEW.MODIFICATION_TIMESTAMP,
                                       OLD.INTEGER_ARRAY_VALUE,
                                       OLD.REAL_ARRAY_VALUE,
                                       OLD.STRING_ARRAY_VALUE,
                                       OLD.TIMESTAMP_ARRAY_VALUE,
                                       OLD.JSON_VALUE
                                       );

CREATE OR REPLACE RULE experiment_properties_delete AS
    ON DELETE TO experiment_properties
    WHERE (OLD.VALUE IS NOT NULL AND decode(replace(substring(OLD.value from 1 for 1), '\', '\\'), 'escape') != E'\\xefbfbd')
        OR OLD.CVTE_ID IS NOT NULL
        OR OLD.SAMP_PROP_ID IS NOT NULL
        OR OLD.INTEGER_ARRAY_VALUE IS NOT NULL
        OR OLD.REAL_ARRAY_VALUE IS NOT NULL
        OR OLD.STRING_ARRAY_VALUE IS NOT NULL
        OR OLD.TIMESTAMP_ARRAY_VALUE IS NOT NULL
        OR OLD.JSON_VALUE IS NOT NULL
    DO ALSO
       INSERT INTO experiment_properties_history (
         ID,
         EXPE_ID,
         ETPT_ID,
         VALUE,
         VOCABULARY_TERM,
         SAMPLE,
         PERS_ID_AUTHOR,
         VALID_FROM_TIMESTAMP,
         VALID_UNTIL_TIMESTAMP,
         INTEGER_ARRAY_VALUE,
         REAL_ARRAY_VALUE,
         STRING_ARRAY_VALUE,
         TIMESTAMP_ARRAY_VALUE,
         JSON_VALUE
       ) VALUES (
         nextval('EXPERIMENT_PROPERTY_ID_SEQ'),
         OLD.EXPE_ID,
         OLD.ETPT_ID,
         OLD.VALUE,
         (select (t.code || ' [' || v.code || ']') from controlled_vocabulary_terms as t join controlled_vocabularies as v on t.covo_id = v.id where t.id = OLD.CVTE_ID),
         COALESCE((select perm_id from samples_all where id = OLD.SAMP_PROP_ID), OLD.SAMP_PROP_ID::text),
         OLD.PERS_ID_AUTHOR,
         OLD.MODIFICATION_TIMESTAMP,
         current_timestamp,
         OLD.INTEGER_ARRAY_VALUE,
         OLD.REAL_ARRAY_VALUE,
         OLD.STRING_ARRAY_VALUE,
         OLD.TIMESTAMP_ARRAY_VALUE,
         OLD.JSON_VALUE
       );

CREATE OR REPLACE RULE sample_properties_update AS
    ON UPDATE TO sample_properties
       WHERE (OLD.VALUE IS NOT NULL AND decode(replace(substring(OLD.value from 1 for 1), '\', '\\'), 'escape') != E'\\xefbfbd' AND OLD.VALUE != NEW.VALUE)
          OR (OLD.CVTE_ID IS NOT NULL AND OLD.CVTE_ID != NEW.CVTE_ID)
          OR (OLD.SAMP_PROP_ID IS NOT NULL AND OLD.SAMP_PROP_ID != NEW.SAMP_PROP_ID)
          OR (OLD.INTEGER_ARRAY_VALUE IS NOT NULL AND OLD.INTEGER_ARRAY_VALUE != NEW.INTEGER_ARRAY_VALUE)
          OR (OLD.REAL_ARRAY_VALUE IS NOT NULL AND OLD.REAL_ARRAY_VALUE != NEW.REAL_ARRAY_VALUE)
          OR (OLD.STRING_ARRAY_VALUE IS NOT NULL AND OLD.STRING_ARRAY_VALUE != NEW.STRING_ARRAY_VALUE)
          OR (OLD.TIMESTAMP_ARRAY_VALUE IS NOT NULL AND OLD.TIMESTAMP_ARRAY_VALUE != NEW.TIMESTAMP_ARRAY_VALUE)
          OR (OLD.JSON_VALUE IS NOT NULL AND OLD.JSON_VALUE != NEW.JSON_VALUE)
           DO ALSO
       INSERT INTO sample_properties_history (
    ID,
    SAMP_ID,
    STPT_ID,
    VALUE,
    VOCABULARY_TERM,
    SAMPLE,
    PERS_ID_AUTHOR,
    VALID_FROM_TIMESTAMP,
    VALID_UNTIL_TIMESTAMP,
    INTEGER_ARRAY_VALUE,
    REAL_ARRAY_VALUE,
    STRING_ARRAY_VALUE,
    TIMESTAMP_ARRAY_VALUE,
    JSON_VALUE
) VALUES (
                                       nextval('SAMPLE_PROPERTY_ID_SEQ'),
                                       OLD.SAMP_ID,
                                       OLD.STPT_ID,
                                       OLD.VALUE,
                                       (select (t.code || ' [' || v.code || ']') from controlled_vocabulary_terms as t join controlled_vocabularies as v on t.covo_id = v.id where t.id = OLD.CVTE_ID),
                                       COALESCE((select perm_id from samples_all where id = OLD.SAMP_PROP_ID), OLD.SAMP_PROP_ID::text),
                                       OLD.PERS_ID_AUTHOR,
                                       OLD.MODIFICATION_TIMESTAMP,
                                       NEW.MODIFICATION_TIMESTAMP,
                                       OLD.INTEGER_ARRAY_VALUE,
                                       OLD.REAL_ARRAY_VALUE,
                                       OLD.STRING_ARRAY_VALUE,
                                       OLD.TIMESTAMP_ARRAY_VALUE,
                                       OLD.JSON_VALUE
                                       );

CREATE OR REPLACE RULE sample_properties_delete AS
    ON DELETE TO sample_properties
    WHERE ((OLD.VALUE IS NOT NULL AND decode(replace(substring(OLD.value from 1 for 1), '\', '\\'), 'escape') != E'\\xefbfbd')
        OR OLD.CVTE_ID IS NOT NULL
        OR OLD.SAMP_PROP_ID IS NOT NULL
        OR OLD.INTEGER_ARRAY_VALUE IS NOT NULL
        OR OLD.REAL_ARRAY_VALUE IS NOT NULL
        OR OLD.STRING_ARRAY_VALUE IS NOT NULL
        OR OLD.TIMESTAMP_ARRAY_VALUE IS NOT NULL
        OR OLD.JSON_VALUE IS NOT NULL)
       AND (SELECT DEL_ID FROM SAMPLES_ALL WHERE ID = OLD.SAMP_ID) IS NULL
     DO ALSO
       INSERT INTO sample_properties_history (
         ID,
         SAMP_ID,
         STPT_ID,
         VALUE,
         VOCABULARY_TERM,
         SAMPLE,
         PERS_ID_AUTHOR,
         VALID_FROM_TIMESTAMP,
         VALID_UNTIL_TIMESTAMP,
         INTEGER_ARRAY_VALUE,
         REAL_ARRAY_VALUE,
         STRING_ARRAY_VALUE,
         TIMESTAMP_ARRAY_VALUE,
         JSON_VALUE
       ) VALUES (
         nextval('SAMPLE_PROPERTY_ID_SEQ'),
         OLD.SAMP_ID,
         OLD.STPT_ID,
         OLD.VALUE,
         (select (t.code || ' [' || v.code || ']') from controlled_vocabulary_terms as t join controlled_vocabularies as v on t.covo_id = v.id where t.id = OLD.CVTE_ID),
         COALESCE((select perm_id from samples_all where id = OLD.SAMP_PROP_ID), OLD.SAMP_PROP_ID::text),
         OLD.PERS_ID_AUTHOR,
         OLD.MODIFICATION_TIMESTAMP,
         current_timestamp,
         OLD.INTEGER_ARRAY_VALUE,
         OLD.REAL_ARRAY_VALUE,
         OLD.STRING_ARRAY_VALUE,
         OLD.TIMESTAMP_ARRAY_VALUE,
         OLD.JSON_VALUE
       );

CREATE OR REPLACE RULE data_set_properties_update AS
    ON UPDATE TO data_set_properties
       WHERE (OLD.VALUE IS NOT NULL AND decode(replace(substring(OLD.value from 1 for 1), '\', '\\'), 'escape') != E'\\xefbfbd' AND OLD.VALUE != NEW.VALUE)
          OR (OLD.CVTE_ID IS NOT NULL AND OLD.CVTE_ID != NEW.CVTE_ID)
          OR (OLD.SAMP_PROP_ID IS NOT NULL AND OLD.SAMP_PROP_ID != NEW.SAMP_PROP_ID)
          OR (OLD.INTEGER_ARRAY_VALUE IS NOT NULL AND OLD.INTEGER_ARRAY_VALUE != NEW.INTEGER_ARRAY_VALUE)
          OR (OLD.REAL_ARRAY_VALUE IS NOT NULL AND OLD.REAL_ARRAY_VALUE != NEW.REAL_ARRAY_VALUE)
          OR (OLD.STRING_ARRAY_VALUE IS NOT NULL AND OLD.STRING_ARRAY_VALUE != NEW.STRING_ARRAY_VALUE)
          OR (OLD.TIMESTAMP_ARRAY_VALUE IS NOT NULL AND OLD.TIMESTAMP_ARRAY_VALUE != NEW.TIMESTAMP_ARRAY_VALUE)
          OR (OLD.JSON_VALUE IS NOT NULL AND OLD.JSON_VALUE != NEW.JSON_VALUE)
           DO ALSO
       INSERT INTO data_set_properties_history (
    ID,
    DS_ID,
    DSTPT_ID,
    VALUE,
    VOCABULARY_TERM,
    SAMPLE,
    PERS_ID_AUTHOR,
    VALID_FROM_TIMESTAMP,
    VALID_UNTIL_TIMESTAMP,
    INTEGER_ARRAY_VALUE,
    REAL_ARRAY_VALUE,
    STRING_ARRAY_VALUE,
    TIMESTAMP_ARRAY_VALUE,
    JSON_VALUE
) VALUES (
                                       nextval('DATA_SET_PROPERTY_ID_SEQ'),
                                       OLD.DS_ID,
                                       OLD.DSTPT_ID,
                                       OLD.VALUE,
                                       (select (t.code || ' [' || v.code || ']') from controlled_vocabulary_terms as t join controlled_vocabularies as v on t.covo_id = v.id where t.id = OLD.CVTE_ID),
                                       COALESCE((select perm_id from samples_all where id = OLD.SAMP_PROP_ID), OLD.SAMP_PROP_ID::text),
                                       OLD.PERS_ID_AUTHOR,
                                       OLD.MODIFICATION_TIMESTAMP,
                                       NEW.MODIFICATION_TIMESTAMP,
                                       OLD.INTEGER_ARRAY_VALUE,
                                       OLD.REAL_ARRAY_VALUE,
                                       OLD.STRING_ARRAY_VALUE,
                                       OLD.TIMESTAMP_ARRAY_VALUE,
                                       OLD.JSON_VALUE
                                       );

CREATE OR REPLACE RULE data_set_properties_delete AS
    ON DELETE TO data_set_properties
    WHERE ((OLD.VALUE IS NOT NULL AND decode(replace(substring(OLD.value from 1 for 1), '\', '\\'), 'escape') != E'\\xefbfbd')
        OR OLD.CVTE_ID IS NOT NULL
        OR OLD.SAMP_PROP_ID IS NOT NULL
        OR OLD.INTEGER_ARRAY_VALUE IS NOT NULL
        OR OLD.REAL_ARRAY_VALUE IS NOT NULL
        OR OLD.STRING_ARRAY_VALUE IS NOT NULL
        OR OLD.TIMESTAMP_ARRAY_VALUE IS NOT NULL
        OR OLD.JSON_VALUE IS NOT NULL)
       AND (SELECT DEL_ID FROM DATA_ALL WHERE ID = OLD.DS_ID) IS NULL
    DO ALSO
       INSERT INTO data_set_properties_history (
         ID,
         DS_ID,
         DSTPT_ID,
         VALUE,
         VOCABULARY_TERM,
         SAMPLE,
         PERS_ID_AUTHOR,
         VALID_FROM_TIMESTAMP,
         VALID_UNTIL_TIMESTAMP,
         INTEGER_ARRAY_VALUE,
         REAL_ARRAY_VALUE,
         STRING_ARRAY_VALUE,
         TIMESTAMP_ARRAY_VALUE,
         JSON_VALUE
       ) VALUES (
         nextval('DATA_SET_PROPERTY_ID_SEQ'),
         OLD.DS_ID,
         OLD.DSTPT_ID,
         OLD.VALUE,
         (select (t.code || ' [' || v.code || ']') from controlled_vocabulary_terms as t join controlled_vocabularies as v on t.covo_id = v.id where t.id = OLD.CVTE_ID),
         COALESCE((select perm_id from samples_all where id = OLD.SAMP_PROP_ID), OLD.SAMP_PROP_ID::text),
         OLD.PERS_ID_AUTHOR,
         OLD.MODIFICATION_TIMESTAMP,
         current_timestamp,
         OLD.INTEGER_ARRAY_VALUE,
         OLD.REAL_ARRAY_VALUE,
         OLD.STRING_ARRAY_VALUE,
         OLD.TIMESTAMP_ARRAY_VALUE,
         OLD.JSON_VALUE
       );

DROP VIEW IF EXISTS sample_history_view;
DROP VIEW IF EXISTS data_set_history_view;
DROP VIEW IF EXISTS experiment_history_view;
DROP VIEW IF EXISTS metaproject_assignments;

ALTER TABLE experiment_properties DROP CONSTRAINT IF EXISTS expr_ck;
ALTER TABLE experiment_properties_history DROP CONSTRAINT IF EXISTS exprh_ck;
ALTER TABLE sample_properties DROP CONSTRAINT IF EXISTS sapr_ck;
ALTER TABLE sample_properties_history DROP CONSTRAINT IF EXISTS saprh_ck;
ALTER TABLE data_set_properties DROP CONSTRAINT IF EXISTS dspr_ck;
ALTER TABLE data_set_properties_history DROP CONSTRAINT IF EXISTS dsprh_ck;
ALTER TABLE events DROP CONSTRAINT IF EXISTS evnt_et_enum_ck;
ALTER TABLE events_search DROP CONSTRAINT IF EXISTS events_search_entity_type_ck;
ALTER TABLE metaproject_assignments_all DROP CONSTRAINT IF EXISTS metaproject_assignments_all_check_nn;
ALTER TABLE metaproject_assignments_all DROP CONSTRAINT IF EXISTS metaproject_assignments_all_mepr_id_mate_id_uk;

DELETE FROM events WHERE entity_type = 'MATERIAL';
DELETE FROM events_search WHERE entity_type = 'MATERIAL';
DELETE FROM project_relationships_history WHERE entity_kind = 'MATERIAL';
DELETE FROM experiment_relationships_history WHERE entity_kind = 'MATERIAL';
DELETE FROM sample_relationships_history WHERE entity_kind = 'MATERIAL';
DELETE FROM data_set_relationships_history WHERE entity_kind = 'MATERIAL';
/**
  Clear display settings only for persons whose settings reference MATERIAL entity visits
  which cause EntityKind.MATERIAL enum lookup failures on authentication
  */
UPDATE persons SET display_settings = NULL WHERE position('MATERIAL'::bytea IN display_settings) > 0;

ALTER TABLE experiment_properties DROP CONSTRAINT IF EXISTS expr_mapr_fk;
ALTER TABLE sample_properties DROP CONSTRAINT IF EXISTS sapr_mapr_fk;
ALTER TABLE data_set_properties DROP CONSTRAINT IF EXISTS dspr_mapr_fk;
ALTER TABLE property_types DROP CONSTRAINT IF EXISTS prty_maty_fk;
ALTER TABLE metaproject_assignments_all DROP CONSTRAINT IF EXISTS metaproject_assignments_all_mate_id_fk;

DROP INDEX IF EXISTS expr_mapr_fk_i;
DROP INDEX IF EXISTS sapr_mapr_fk_i;
DROP INDEX IF EXISTS dspr_mapr_fk_i;
DROP INDEX IF EXISTS metaproject_assignments_all_mate_fk_i;
DROP INDEX IF EXISTS sample_properties_unique_mate;
DROP INDEX IF EXISTS experiment_properties_unique_mate;
DROP INDEX IF EXISTS data_set_properties_unique_mate;

ALTER TABLE experiment_properties DROP COLUMN IF EXISTS mate_prop_id;
ALTER TABLE sample_properties DROP COLUMN IF EXISTS mate_prop_id;
ALTER TABLE data_set_properties DROP COLUMN IF EXISTS mate_prop_id;
ALTER TABLE property_types DROP COLUMN IF EXISTS maty_prop_id;
ALTER TABLE metaproject_assignments_all DROP COLUMN IF EXISTS mate_id;
DELETE FROM experiment_properties_history WHERE material IS NOT NULL;
DELETE FROM sample_properties_history WHERE material IS NOT NULL;
DELETE FROM data_set_properties_history WHERE material IS NOT NULL;
ALTER TABLE experiment_properties_history DROP COLUMN IF EXISTS material;
ALTER TABLE sample_properties_history DROP COLUMN IF EXISTS material;
ALTER TABLE data_set_properties_history DROP COLUMN IF EXISTS material;

ALTER TABLE experiment_properties ADD CONSTRAINT expr_ck CHECK
    ((VALUE IS NOT NULL AND CVTE_ID IS NULL AND SAMP_PROP_ID IS NULL AND INTEGER_ARRAY_VALUE IS NULL AND REAL_ARRAY_VALUE IS NULL AND TIMESTAMP_ARRAY_VALUE IS NULL AND STRING_ARRAY_VALUE IS NULL AND JSON_VALUE IS NULL) OR
     (VALUE IS NULL AND CVTE_ID IS NOT NULL AND SAMP_PROP_ID IS NULL AND INTEGER_ARRAY_VALUE IS NULL AND REAL_ARRAY_VALUE IS NULL AND TIMESTAMP_ARRAY_VALUE IS NULL AND STRING_ARRAY_VALUE IS NULL AND JSON_VALUE IS NULL) OR
     (VALUE IS NULL AND CVTE_ID IS NULL AND SAMP_PROP_ID IS NOT NULL AND INTEGER_ARRAY_VALUE IS NULL AND REAL_ARRAY_VALUE IS NULL AND TIMESTAMP_ARRAY_VALUE IS NULL AND STRING_ARRAY_VALUE IS NULL AND JSON_VALUE IS NULL) OR
     (VALUE IS NULL AND CVTE_ID IS NULL AND SAMP_PROP_ID IS NULL AND INTEGER_ARRAY_VALUE IS NOT NULL AND REAL_ARRAY_VALUE IS NULL AND TIMESTAMP_ARRAY_VALUE IS NULL AND STRING_ARRAY_VALUE IS NULL AND JSON_VALUE IS NULL) OR
     (VALUE IS NULL AND CVTE_ID IS NULL AND SAMP_PROP_ID IS NULL AND INTEGER_ARRAY_VALUE IS NULL AND REAL_ARRAY_VALUE IS NOT NULL AND TIMESTAMP_ARRAY_VALUE IS NULL AND STRING_ARRAY_VALUE IS NULL AND JSON_VALUE IS NULL) OR
     (VALUE IS NULL AND CVTE_ID IS NULL AND SAMP_PROP_ID IS NULL AND INTEGER_ARRAY_VALUE IS NULL AND REAL_ARRAY_VALUE IS NULL AND TIMESTAMP_ARRAY_VALUE IS NOT NULL AND STRING_ARRAY_VALUE IS NULL AND JSON_VALUE IS NULL) OR
     (VALUE IS NULL AND CVTE_ID IS NULL AND SAMP_PROP_ID IS NULL AND INTEGER_ARRAY_VALUE IS NULL AND REAL_ARRAY_VALUE IS NULL AND TIMESTAMP_ARRAY_VALUE IS NULL AND STRING_ARRAY_VALUE IS NOT NULL AND JSON_VALUE IS NULL) OR
     (VALUE IS NULL AND CVTE_ID IS NULL AND SAMP_PROP_ID IS NULL AND INTEGER_ARRAY_VALUE IS NULL AND REAL_ARRAY_VALUE IS NULL AND TIMESTAMP_ARRAY_VALUE IS NULL AND STRING_ARRAY_VALUE IS NULL AND JSON_VALUE IS NOT NULL)
    );

ALTER TABLE experiment_properties_history ADD CONSTRAINT exprh_ck CHECK
    ((VALUE IS NOT NULL AND VOCABULARY_TERM IS NULL AND SAMPLE IS NULL AND INTEGER_ARRAY_VALUE IS NULL AND REAL_ARRAY_VALUE IS NULL AND TIMESTAMP_ARRAY_VALUE IS NULL AND STRING_ARRAY_VALUE IS NULL AND JSON_VALUE IS NULL) OR
     (VALUE IS NULL AND VOCABULARY_TERM IS NOT NULL AND SAMPLE IS NULL AND INTEGER_ARRAY_VALUE IS NULL AND REAL_ARRAY_VALUE IS NULL AND TIMESTAMP_ARRAY_VALUE IS NULL AND STRING_ARRAY_VALUE IS NULL AND JSON_VALUE IS NULL) OR
     (VALUE IS NULL AND VOCABULARY_TERM IS NULL AND SAMPLE IS NOT NULL AND INTEGER_ARRAY_VALUE IS NULL AND REAL_ARRAY_VALUE IS NULL AND TIMESTAMP_ARRAY_VALUE IS NULL AND STRING_ARRAY_VALUE IS NULL AND JSON_VALUE IS NULL) OR
     (VALUE IS NULL AND VOCABULARY_TERM IS NULL AND SAMPLE IS NULL AND INTEGER_ARRAY_VALUE IS NOT NULL AND REAL_ARRAY_VALUE IS NULL AND TIMESTAMP_ARRAY_VALUE IS NULL AND STRING_ARRAY_VALUE IS NULL AND JSON_VALUE IS NULL) OR
     (VALUE IS NULL AND VOCABULARY_TERM IS NULL AND SAMPLE IS NULL AND INTEGER_ARRAY_VALUE IS NULL AND REAL_ARRAY_VALUE IS NOT NULL AND TIMESTAMP_ARRAY_VALUE IS NULL AND STRING_ARRAY_VALUE IS NULL AND JSON_VALUE IS NULL) OR
     (VALUE IS NULL AND VOCABULARY_TERM IS NULL AND SAMPLE IS NULL AND INTEGER_ARRAY_VALUE IS NULL AND REAL_ARRAY_VALUE IS NULL AND TIMESTAMP_ARRAY_VALUE IS NOT NULL AND STRING_ARRAY_VALUE IS NULL AND JSON_VALUE IS NULL) OR
     (VALUE IS NULL AND VOCABULARY_TERM IS NULL AND SAMPLE IS NULL AND INTEGER_ARRAY_VALUE IS NULL AND REAL_ARRAY_VALUE IS NULL AND TIMESTAMP_ARRAY_VALUE IS NULL AND STRING_ARRAY_VALUE IS NOT NULL AND JSON_VALUE IS NULL) OR
     (VALUE IS NULL AND VOCABULARY_TERM IS NULL AND SAMPLE IS NULL AND INTEGER_ARRAY_VALUE IS NULL AND REAL_ARRAY_VALUE IS NULL AND TIMESTAMP_ARRAY_VALUE IS NULL AND STRING_ARRAY_VALUE IS NULL AND JSON_VALUE IS NOT NULL)
    );

ALTER TABLE sample_properties ADD CONSTRAINT sapr_ck CHECK
    ((VALUE IS NOT NULL AND CVTE_ID IS NULL AND SAMP_PROP_ID IS NULL AND INTEGER_ARRAY_VALUE IS NULL AND REAL_ARRAY_VALUE IS NULL AND TIMESTAMP_ARRAY_VALUE IS NULL AND STRING_ARRAY_VALUE IS NULL AND JSON_VALUE IS NULL) OR
     (VALUE IS NULL AND CVTE_ID IS NOT NULL AND SAMP_PROP_ID IS NULL AND INTEGER_ARRAY_VALUE IS NULL AND REAL_ARRAY_VALUE IS NULL AND TIMESTAMP_ARRAY_VALUE IS NULL AND STRING_ARRAY_VALUE IS NULL AND JSON_VALUE IS NULL) OR
     (VALUE IS NULL AND CVTE_ID IS NULL AND SAMP_PROP_ID IS NOT NULL AND INTEGER_ARRAY_VALUE IS NULL AND REAL_ARRAY_VALUE IS NULL AND TIMESTAMP_ARRAY_VALUE IS NULL AND STRING_ARRAY_VALUE IS NULL AND JSON_VALUE IS NULL) OR
     (VALUE IS NULL AND CVTE_ID IS NULL AND SAMP_PROP_ID IS NULL AND INTEGER_ARRAY_VALUE IS NOT NULL AND REAL_ARRAY_VALUE IS NULL AND TIMESTAMP_ARRAY_VALUE IS NULL AND STRING_ARRAY_VALUE IS NULL AND JSON_VALUE IS NULL) OR
     (VALUE IS NULL AND CVTE_ID IS NULL AND SAMP_PROP_ID IS NULL AND INTEGER_ARRAY_VALUE IS NULL AND REAL_ARRAY_VALUE IS NOT NULL AND TIMESTAMP_ARRAY_VALUE IS NULL AND STRING_ARRAY_VALUE IS NULL AND JSON_VALUE IS NULL) OR
     (VALUE IS NULL AND CVTE_ID IS NULL AND SAMP_PROP_ID IS NULL AND INTEGER_ARRAY_VALUE IS NULL AND REAL_ARRAY_VALUE IS NULL AND TIMESTAMP_ARRAY_VALUE IS NOT NULL AND STRING_ARRAY_VALUE IS NULL AND JSON_VALUE IS NULL) OR
     (VALUE IS NULL AND CVTE_ID IS NULL AND SAMP_PROP_ID IS NULL AND INTEGER_ARRAY_VALUE IS NULL AND REAL_ARRAY_VALUE IS NULL AND TIMESTAMP_ARRAY_VALUE IS NULL AND STRING_ARRAY_VALUE IS NOT NULL AND JSON_VALUE IS NULL) OR
     (VALUE IS NULL AND CVTE_ID IS NULL AND SAMP_PROP_ID IS NULL AND INTEGER_ARRAY_VALUE IS NULL AND REAL_ARRAY_VALUE IS NULL AND TIMESTAMP_ARRAY_VALUE IS NULL AND STRING_ARRAY_VALUE IS NULL AND JSON_VALUE IS NOT NULL)
    );

ALTER TABLE sample_properties_history ADD CONSTRAINT saprh_ck CHECK
    ((VALUE IS NOT NULL AND VOCABULARY_TERM IS NULL AND SAMPLE IS NULL AND INTEGER_ARRAY_VALUE IS NULL AND REAL_ARRAY_VALUE IS NULL AND TIMESTAMP_ARRAY_VALUE IS NULL AND STRING_ARRAY_VALUE IS NULL AND JSON_VALUE IS NULL) OR
     (VALUE IS NULL AND VOCABULARY_TERM IS NOT NULL AND SAMPLE IS NULL AND INTEGER_ARRAY_VALUE IS NULL AND REAL_ARRAY_VALUE IS NULL AND TIMESTAMP_ARRAY_VALUE IS NULL AND STRING_ARRAY_VALUE IS NULL AND JSON_VALUE IS NULL) OR
     (VALUE IS NULL AND VOCABULARY_TERM IS NULL AND SAMPLE IS NOT NULL AND INTEGER_ARRAY_VALUE IS NULL AND REAL_ARRAY_VALUE IS NULL AND TIMESTAMP_ARRAY_VALUE IS NULL AND STRING_ARRAY_VALUE IS NULL AND JSON_VALUE IS NULL) OR
     (VALUE IS NULL AND VOCABULARY_TERM IS NULL AND SAMPLE IS NULL AND INTEGER_ARRAY_VALUE IS NOT NULL AND REAL_ARRAY_VALUE IS NULL AND TIMESTAMP_ARRAY_VALUE IS NULL AND STRING_ARRAY_VALUE IS NULL AND JSON_VALUE IS NULL) OR
     (VALUE IS NULL AND VOCABULARY_TERM IS NULL AND SAMPLE IS NULL AND INTEGER_ARRAY_VALUE IS NULL AND REAL_ARRAY_VALUE IS NOT NULL AND TIMESTAMP_ARRAY_VALUE IS NULL AND STRING_ARRAY_VALUE IS NULL AND JSON_VALUE IS NULL) OR
     (VALUE IS NULL AND VOCABULARY_TERM IS NULL AND SAMPLE IS NULL AND INTEGER_ARRAY_VALUE IS NULL AND REAL_ARRAY_VALUE IS NULL AND TIMESTAMP_ARRAY_VALUE IS NOT NULL AND STRING_ARRAY_VALUE IS NULL AND JSON_VALUE IS NULL) OR
     (VALUE IS NULL AND VOCABULARY_TERM IS NULL AND SAMPLE IS NULL AND INTEGER_ARRAY_VALUE IS NULL AND REAL_ARRAY_VALUE IS NULL AND TIMESTAMP_ARRAY_VALUE IS NULL AND STRING_ARRAY_VALUE IS NOT NULL AND JSON_VALUE IS NULL) OR
     (VALUE IS NULL AND VOCABULARY_TERM IS NULL AND SAMPLE IS NULL AND INTEGER_ARRAY_VALUE IS NULL AND REAL_ARRAY_VALUE IS NULL AND TIMESTAMP_ARRAY_VALUE IS NULL AND STRING_ARRAY_VALUE IS NULL AND JSON_VALUE IS NOT NULL)
    );

ALTER TABLE data_set_properties ADD CONSTRAINT dspr_ck CHECK
    ((VALUE IS NOT NULL AND CVTE_ID IS NULL AND SAMP_PROP_ID IS NULL AND INTEGER_ARRAY_VALUE IS NULL AND REAL_ARRAY_VALUE IS NULL AND TIMESTAMP_ARRAY_VALUE IS NULL AND STRING_ARRAY_VALUE IS NULL AND JSON_VALUE IS NULL) OR
     (VALUE IS NULL AND CVTE_ID IS NOT NULL AND SAMP_PROP_ID IS NULL AND INTEGER_ARRAY_VALUE IS NULL AND REAL_ARRAY_VALUE IS NULL AND TIMESTAMP_ARRAY_VALUE IS NULL AND STRING_ARRAY_VALUE IS NULL AND JSON_VALUE IS NULL) OR
     (VALUE IS NULL AND CVTE_ID IS NULL AND SAMP_PROP_ID IS NOT NULL AND INTEGER_ARRAY_VALUE IS NULL AND REAL_ARRAY_VALUE IS NULL AND TIMESTAMP_ARRAY_VALUE IS NULL AND STRING_ARRAY_VALUE IS NULL AND JSON_VALUE IS NULL) OR
     (VALUE IS NULL AND CVTE_ID IS NULL AND SAMP_PROP_ID IS NULL AND INTEGER_ARRAY_VALUE IS NOT NULL AND REAL_ARRAY_VALUE IS NULL AND TIMESTAMP_ARRAY_VALUE IS NULL AND STRING_ARRAY_VALUE IS NULL AND JSON_VALUE IS NULL) OR
     (VALUE IS NULL AND CVTE_ID IS NULL AND SAMP_PROP_ID IS NULL AND INTEGER_ARRAY_VALUE IS NULL AND REAL_ARRAY_VALUE IS NOT NULL AND TIMESTAMP_ARRAY_VALUE IS NULL AND STRING_ARRAY_VALUE IS NULL AND JSON_VALUE IS NULL) OR
     (VALUE IS NULL AND CVTE_ID IS NULL AND SAMP_PROP_ID IS NULL AND INTEGER_ARRAY_VALUE IS NULL AND REAL_ARRAY_VALUE IS NULL AND TIMESTAMP_ARRAY_VALUE IS NOT NULL AND STRING_ARRAY_VALUE IS NULL AND JSON_VALUE IS NULL) OR
     (VALUE IS NULL AND CVTE_ID IS NULL AND SAMP_PROP_ID IS NULL AND INTEGER_ARRAY_VALUE IS NULL AND REAL_ARRAY_VALUE IS NULL AND TIMESTAMP_ARRAY_VALUE IS NULL AND STRING_ARRAY_VALUE IS NOT NULL AND JSON_VALUE IS NULL) OR
     (VALUE IS NULL AND CVTE_ID IS NULL AND SAMP_PROP_ID IS NULL AND INTEGER_ARRAY_VALUE IS NULL AND REAL_ARRAY_VALUE IS NULL AND TIMESTAMP_ARRAY_VALUE IS NULL AND STRING_ARRAY_VALUE IS NULL AND JSON_VALUE IS NOT NULL)
    );

ALTER TABLE data_set_properties_history ADD CONSTRAINT dsprh_ck CHECK
    ((VALUE IS NOT NULL AND VOCABULARY_TERM IS NULL AND SAMPLE IS NULL AND INTEGER_ARRAY_VALUE IS NULL AND REAL_ARRAY_VALUE IS NULL AND TIMESTAMP_ARRAY_VALUE IS NULL AND STRING_ARRAY_VALUE IS NULL AND JSON_VALUE IS NULL) OR
     (VALUE IS NULL AND VOCABULARY_TERM IS NOT NULL AND SAMPLE IS NULL AND INTEGER_ARRAY_VALUE IS NULL AND REAL_ARRAY_VALUE IS NULL AND TIMESTAMP_ARRAY_VALUE IS NULL AND STRING_ARRAY_VALUE IS NULL AND JSON_VALUE IS NULL) OR
     (VALUE IS NULL AND VOCABULARY_TERM IS NULL AND SAMPLE IS NOT NULL AND INTEGER_ARRAY_VALUE IS NULL AND REAL_ARRAY_VALUE IS NULL AND TIMESTAMP_ARRAY_VALUE IS NULL AND STRING_ARRAY_VALUE IS NULL AND JSON_VALUE IS NULL) OR
     (VALUE IS NULL AND VOCABULARY_TERM IS NULL AND SAMPLE IS NULL AND INTEGER_ARRAY_VALUE IS NOT NULL AND REAL_ARRAY_VALUE IS NULL AND TIMESTAMP_ARRAY_VALUE IS NULL AND STRING_ARRAY_VALUE IS NULL AND JSON_VALUE IS NULL) OR
     (VALUE IS NULL AND VOCABULARY_TERM IS NULL AND SAMPLE IS NULL AND INTEGER_ARRAY_VALUE IS NULL AND REAL_ARRAY_VALUE IS NOT NULL AND TIMESTAMP_ARRAY_VALUE IS NULL AND STRING_ARRAY_VALUE IS NULL AND JSON_VALUE IS NULL) OR
     (VALUE IS NULL AND VOCABULARY_TERM IS NULL AND SAMPLE IS NULL AND INTEGER_ARRAY_VALUE IS NULL AND REAL_ARRAY_VALUE IS NULL AND TIMESTAMP_ARRAY_VALUE IS NOT NULL AND STRING_ARRAY_VALUE IS NULL AND JSON_VALUE IS NULL) OR
     (VALUE IS NULL AND VOCABULARY_TERM IS NULL AND SAMPLE IS NULL AND INTEGER_ARRAY_VALUE IS NULL AND REAL_ARRAY_VALUE IS NULL AND TIMESTAMP_ARRAY_VALUE IS NULL AND STRING_ARRAY_VALUE IS NOT NULL AND JSON_VALUE IS NULL) OR
     (VALUE IS NULL AND VOCABULARY_TERM IS NULL AND SAMPLE IS NULL AND INTEGER_ARRAY_VALUE IS NULL AND REAL_ARRAY_VALUE IS NULL AND TIMESTAMP_ARRAY_VALUE IS NULL AND STRING_ARRAY_VALUE IS NULL AND JSON_VALUE IS NOT NULL)
    );

ALTER TABLE events ADD CONSTRAINT evnt_et_enum_ck CHECK
    (entity_type IN ('ATTACHMENT', 'DATASET', 'EXPERIMENT', 'SPACE', 'PROJECT', 'PROPERTY_TYPE', 'SAMPLE', 'VOCABULARY', 'AUTHORIZATION_GROUP', 'METAPROJECT'));

ALTER TABLE events_search ADD CONSTRAINT events_search_entity_type_ck CHECK
    (entity_type IN ('ATTACHMENT', 'DATASET', 'EXPERIMENT', 'SPACE', 'PROJECT', 'PROPERTY_TYPE', 'SAMPLE', 'VOCABULARY', 'AUTHORIZATION_GROUP', 'METAPROJECT'));

ALTER TABLE metaproject_assignments_all ADD CONSTRAINT metaproject_assignments_all_check_nn CHECK (
    (EXPE_ID IS NOT NULL AND SAMP_ID IS NULL AND DATA_ID IS NULL) OR
    (EXPE_ID IS NULL AND SAMP_ID IS NOT NULL AND DATA_ID IS NULL) OR
    (EXPE_ID IS NULL AND SAMP_ID IS NULL AND DATA_ID IS NOT NULL)
);

CREATE VIEW metaproject_assignments AS
   SELECT ID, MEPR_ID, EXPE_ID, SAMP_ID, DATA_ID, DEL_ID, CREATION_DATE
   FROM metaproject_assignments_all
   WHERE DEL_ID IS NULL;

CREATE OR REPLACE RULE metaproject_assignments_insert AS
    ON INSERT TO metaproject_assignments DO INSTEAD
       INSERT INTO metaproject_assignments_all (
         ID,
         MEPR_ID,
         EXPE_ID,
         SAMP_ID,
         DATA_ID,
         DEL_ID,
         CREATION_DATE
       ) VALUES (
         NEW.ID,
         NEW.MEPR_ID,
         NEW.EXPE_ID,
         NEW.SAMP_ID,
         NEW.DATA_ID,
         NEW.DEL_ID,
         NEW.CREATION_DATE
       );

CREATE OR REPLACE RULE metaproject_assignments_update AS
    ON UPDATE TO metaproject_assignments DO INSTEAD
UPDATE metaproject_assignments_all
SET
    ID = NEW.ID,
    MEPR_ID = NEW.MEPR_ID,
    EXPE_ID = NEW.EXPE_ID,
    SAMP_ID = NEW.SAMP_ID,
    DATA_ID = NEW.DATA_ID,
    DEL_ID = NEW.DEL_ID,
    CREATION_DATE = NEW.CREATION_DATE
WHERE ID = NEW.ID;

CREATE OR REPLACE RULE metaproject_assignments_delete AS
    ON DELETE TO metaproject_assignments DO INSTEAD
DELETE FROM metaproject_assignments_all
WHERE ID = OLD.ID;

CREATE VIEW sample_history_view AS (
  SELECT
    2*id as id,
    main_samp_id,
    relation_type,
    space_id,
    expe_id,
    samp_id,
    proj_id,
    data_id,
    entity_kind,
    entity_perm_id,
    annotations,
    null as stpt_id,
    null as value,
    null as vocabulary_term,
    null as sample,
    pers_id_author,
    valid_from_timestamp,
    valid_until_timestamp,
    null as integer_array_value,
    null as real_array_value,
    null as string_array_value,
    null as timestamp_array_value,
    null as json_value
  FROM
    sample_relationships_history
  WHERE
    valid_until_timestamp IS NOT NULL)
UNION
  SELECT
    2*id+1 as id,
    samp_id as main_samp_id,
    null as relation_type,
    null as space_id,
    null as expe_id,
    null as samp_id,
    null as proj_id,
    null as data_id,
    null as entity_kind,
    null as entity_perm_id,
    null as annotations,
    stpt_id,
    value,
    vocabulary_term,
    sample,
    pers_id_author,
    valid_from_timestamp,
    valid_until_timestamp,
    integer_array_value,
    real_array_value,
    string_array_value,
    timestamp_array_value,
    json_value
  FROM
    sample_properties_history;

CREATE VIEW data_set_history_view AS (
  SELECT
    3*id as id,
    main_data_id,
    relation_type,
    ordinal,
    expe_id,
    samp_id,
    data_id,
    entity_kind,
    entity_perm_id,
    null as dstpt_id,
    null as value,
    null as vocabulary_term,
    null as sample,
    null as external_code,
    null as path,
    null as git_commit_hash,
    null as git_repository_id,
    null::TECH_ID as edms_id,
    null as edms_code,
    null as edms_label,
    null as edms_address,
    pers_id_author,
    valid_from_timestamp,
    valid_until_timestamp,
    null as integer_array_value,
    null as real_array_value,
    null as string_array_value,
    null as timestamp_array_value,
    null as json_value
  FROM
    data_set_relationships_history
  WHERE
    valid_until_timestamp IS NOT NULL)
UNION
  SELECT
    3*id+1 as id,
    ds_id as main_data_id,
    null as relation_type,
    null as ordinal,
    null as expe_id,
    null as samp_id,
    null as data_id,
    null as entity_kind,
    null as entity_perm_id,
    dstpt_id,
    value,
    vocabulary_term,
    sample,
    null as external_code,
    null as path,
    null as git_commit_hash,
    null as git_repository_id,
    null as edms_id,
    null as edms_code,
    null as edms_label,
    null as edms_address,
    pers_id_author,
    valid_from_timestamp,
    valid_until_timestamp,
    integer_array_value,
    real_array_value,
    string_array_value,
    timestamp_array_value,
    json_value
  FROM
    data_set_properties_history
 UNION
  (SELECT
   3*id+2 as id,
    data_id as main_data_id,
    null as relation_type,
    null as ordinal,
    null as expe_id,
    null as samp_id,
    null as data_id,
    null as entity_kind,
    null as entity_perm_id,
    null as dstpt_id,
    null as value,
    null as vocabulary_term,
    null as sample,
    external_code,
    path,
    git_commit_hash,
    git_repository_id,
    edms_id,
    edms_code,
    edms_label,
    edms_address,
    pers_id_author,
    valid_from_timestamp,
    valid_until_timestamp,
    null as integer_array_value,
    null as real_array_value,
    null as string_array_value,
    null as timestamp_array_value,
    null as json_value
  FROM
    data_set_copies_history
  WHERE
    valid_until_timestamp IS NOT NULL);

CREATE VIEW experiment_history_view AS (
  SELECT
    2*id as id,
    main_expe_id,
    relation_type,
    proj_id,
    samp_id,
    data_id,
    entity_kind,
    entity_perm_id,
    null as etpt_id,
    null as value,
    null as vocabulary_term,
    null as sample,
    pers_id_author,
    valid_from_timestamp,
    valid_until_timestamp,
    null as integer_array_value,
    null as real_array_value,
    null as string_array_value,
    null as timestamp_array_value,
    null as json_value
  FROM
    experiment_relationships_history
  WHERE valid_until_timestamp IS NOT NULL)
UNION
  SELECT
    2*id+1 as id,
    expe_id as main_expe_id,
    null as relation_type,
    null as proj_id,
    null as samp_id,
    null as data_id,
    null as entity_kind,
    null as entity_perm_id,
    etpt_id,
    value,
    vocabulary_term,
    sample,
    pers_id_author,
    valid_from_timestamp,
    valid_until_timestamp,
    integer_array_value,
    real_array_value,
    string_array_value,
    timestamp_array_value,
    json_value
  FROM
    experiment_properties_history;

ALTER DOMAIN query_type DROP CONSTRAINT IF EXISTS query_type_check;
ALTER DOMAIN query_type ADD CONSTRAINT query_type_check CHECK (VALUE IN ('GENERIC', 'EXPERIMENT', 'SAMPLE', 'DATA_SET'));

ALTER DOMAIN entity_kind DROP CONSTRAINT IF EXISTS entity_kind_check;
ALTER DOMAIN entity_kind ADD CONSTRAINT entity_kind_check CHECK (VALUE IN ('SAMPLE', 'EXPERIMENT', 'DATA_SET'));

DELETE FROM data_types WHERE code = 'MATERIAL';
