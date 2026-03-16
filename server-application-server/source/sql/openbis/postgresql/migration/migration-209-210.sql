-- Keep FTS document version at 003, but align migrated databases with the updated
-- FTS trigger functions introduced for DB version 209.

-- Safety guard: do not allow migration if tables that will be dropped still contain data.
DO $$
DECLARE
    rows_count BIGINT;
BEGIN
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'material_types') THEN
        SELECT COUNT(*) INTO rows_count FROM material_types;
        IF rows_count > 0 THEN
            RAISE EXCEPTION 'Migration 209->210 blocked: material_types contains % row(s).', rows_count;
        END IF;
    END IF;

    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'material_type_property_types') THEN
        SELECT COUNT(*) INTO rows_count FROM material_type_property_types;
        IF rows_count > 0 THEN
            RAISE EXCEPTION 'Migration 209->210 blocked: material_type_property_types contains % row(s).', rows_count;
        END IF;
    END IF;

    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'materials') THEN
        SELECT COUNT(*) INTO rows_count FROM materials;
        IF rows_count > 0 THEN
            RAISE EXCEPTION 'Migration 209->210 blocked: materials contains % row(s).', rows_count;
        END IF;
    END IF;

    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'material_properties') THEN
        SELECT COUNT(*) INTO rows_count FROM material_properties;
        IF rows_count > 0 THEN
            RAISE EXCEPTION 'Migration 209->210 blocked: material_properties contains % row(s).', rows_count;
        END IF;
    END IF;

    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'material_properties_history') THEN
        SELECT COUNT(*) INTO rows_count FROM material_properties_history;
        IF rows_count > 0 THEN
            RAISE EXCEPTION 'Migration 209->210 blocked: material_properties_history contains % row(s).', rows_count;
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
    MATERIAL,
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
                                       NULL::text,
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
         MATERIAL,
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
         NULL::text,
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
    MATERIAL,
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
                                       NULL::text,
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
         MATERIAL,
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
         NULL::text,
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
    MATERIAL,
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
                                       NULL::text,
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
         MATERIAL,
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
         NULL::text,
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
