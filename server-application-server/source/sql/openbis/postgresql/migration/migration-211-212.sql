-- Type change jsonb -> json for json_value column on all *_properties and *_properties_history tables.
--
-- PostgreSQL refuses ALTER COLUMN TYPE if any rule or view references the column.
-- All 6 rules (3 update + 3 delete) and 3 history views must be dropped first,
-- then the column types are changed, then everything is recreated.
--
-- The recreated *_update rules cast json_value to ::text for comparison because
-- the json type has no equality operator (unlike jsonb).

-- Step 1: Drop all rules and views that depend on json_value.
DROP RULE experiment_properties_update ON experiment_properties;
DROP RULE sample_properties_update ON sample_properties;
DROP RULE data_set_properties_update ON data_set_properties;

DROP RULE experiment_properties_delete ON experiment_properties;
DROP RULE sample_properties_delete ON sample_properties;
DROP RULE data_set_properties_delete ON data_set_properties;

DROP VIEW sample_history_view;
DROP VIEW data_set_history_view;
DROP VIEW experiment_history_view;

-- Step 2: Change column types from jsonb to json.
ALTER TABLE data_set_properties_history ALTER COLUMN json_value TYPE json USING json_value::text::json;
ALTER TABLE data_set_properties ALTER COLUMN json_value TYPE json USING json_value::text::json;
ALTER TABLE experiment_properties_history ALTER COLUMN json_value TYPE json USING json_value::text::json;
ALTER TABLE experiment_properties ALTER COLUMN json_value TYPE json USING json_value::text::json;
ALTER TABLE sample_properties_history ALTER COLUMN json_value TYPE json USING json_value::text::json;
ALTER TABLE sample_properties ALTER COLUMN json_value TYPE json USING json_value::text::json;

-- Step 3: Recreate update rules with ::text cast on json_value comparison.
CREATE OR REPLACE RULE experiment_properties_update AS
    ON UPDATE TO experiment_properties
       WHERE (OLD.VALUE IS NOT NULL AND decode(replace(substring(OLD.value from 1 for 1), '\', '\\'), 'escape') != E'\\xefbfbd' AND OLD.VALUE != NEW.VALUE)
          OR (OLD.CVTE_ID IS NOT NULL AND OLD.CVTE_ID != NEW.CVTE_ID)
          OR (OLD.SAMP_PROP_ID IS NOT NULL AND OLD.SAMP_PROP_ID != NEW.SAMP_PROP_ID)
          OR (OLD.INTEGER_ARRAY_VALUE IS NOT NULL AND OLD.INTEGER_ARRAY_VALUE != NEW.INTEGER_ARRAY_VALUE)
          OR (OLD.REAL_ARRAY_VALUE IS NOT NULL AND OLD.REAL_ARRAY_VALUE != NEW.REAL_ARRAY_VALUE)
          OR (OLD.STRING_ARRAY_VALUE IS NOT NULL AND OLD.STRING_ARRAY_VALUE != NEW.STRING_ARRAY_VALUE)
          OR (OLD.TIMESTAMP_ARRAY_VALUE IS NOT NULL AND OLD.TIMESTAMP_ARRAY_VALUE != NEW.TIMESTAMP_ARRAY_VALUE)
          OR (OLD.JSON_VALUE IS NOT NULL AND OLD.JSON_VALUE::text != NEW.JSON_VALUE::text)
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

CREATE OR REPLACE RULE sample_properties_update AS
    ON UPDATE TO sample_properties
       WHERE (OLD.VALUE IS NOT NULL AND decode(replace(substring(OLD.value from 1 for 1), '\', '\\'), 'escape') != E'\\xefbfbd' AND OLD.VALUE != NEW.VALUE)
          OR (OLD.CVTE_ID IS NOT NULL AND OLD.CVTE_ID != NEW.CVTE_ID)
          OR (OLD.SAMP_PROP_ID IS NOT NULL AND OLD.SAMP_PROP_ID != NEW.SAMP_PROP_ID)
          OR (OLD.INTEGER_ARRAY_VALUE IS NOT NULL AND OLD.INTEGER_ARRAY_VALUE != NEW.INTEGER_ARRAY_VALUE)
          OR (OLD.REAL_ARRAY_VALUE IS NOT NULL AND OLD.REAL_ARRAY_VALUE != NEW.REAL_ARRAY_VALUE)
          OR (OLD.STRING_ARRAY_VALUE IS NOT NULL AND OLD.STRING_ARRAY_VALUE != NEW.STRING_ARRAY_VALUE)
          OR (OLD.TIMESTAMP_ARRAY_VALUE IS NOT NULL AND OLD.TIMESTAMP_ARRAY_VALUE != NEW.TIMESTAMP_ARRAY_VALUE)
          OR (OLD.JSON_VALUE IS NOT NULL AND OLD.JSON_VALUE::text != NEW.JSON_VALUE::text)
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

CREATE OR REPLACE RULE data_set_properties_update AS
    ON UPDATE TO data_set_properties
       WHERE (OLD.VALUE IS NOT NULL AND decode(replace(substring(OLD.value from 1 for 1), '\', '\\'), 'escape') != E'\\xefbfbd' AND OLD.VALUE != NEW.VALUE)
          OR (OLD.CVTE_ID IS NOT NULL AND OLD.CVTE_ID != NEW.CVTE_ID)
          OR (OLD.SAMP_PROP_ID IS NOT NULL AND OLD.SAMP_PROP_ID != NEW.SAMP_PROP_ID)
          OR (OLD.INTEGER_ARRAY_VALUE IS NOT NULL AND OLD.INTEGER_ARRAY_VALUE != NEW.INTEGER_ARRAY_VALUE)
          OR (OLD.REAL_ARRAY_VALUE IS NOT NULL AND OLD.REAL_ARRAY_VALUE != NEW.REAL_ARRAY_VALUE)
          OR (OLD.STRING_ARRAY_VALUE IS NOT NULL AND OLD.STRING_ARRAY_VALUE != NEW.STRING_ARRAY_VALUE)
          OR (OLD.TIMESTAMP_ARRAY_VALUE IS NOT NULL AND OLD.TIMESTAMP_ARRAY_VALUE != NEW.TIMESTAMP_ARRAY_VALUE)
          OR (OLD.JSON_VALUE IS NOT NULL AND OLD.JSON_VALUE::text != NEW.JSON_VALUE::text)
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

-- Step 4: Recreate delete rules.
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

-- Step 5: Recreate history views with UNION ALL (no equality operator needed on json_value).
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
    SAMPLE_RELATIONSHIPS_HISTORY
  WHERE
    valid_until_timestamp IS NOT NULL)
UNION ALL
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
    SAMPLE_PROPERTIES_HISTORY;

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
UNION ALL
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
UNION ALL
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
    EXPERIMENT_RELATIONSHIPS_HISTORY
  WHERE valid_until_timestamp IS NOT NULL)
UNION ALL
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
    EXPERIMENT_PROPERTIES_HISTORY;
