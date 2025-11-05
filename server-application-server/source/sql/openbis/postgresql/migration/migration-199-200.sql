-- Migration from 199 to 200

-- Make xxx_properties rules insert into xxx_properties_history tables sample tech id
-- as a sample property value if the referenced sample does not exist anymore
-- and its perm id cannot be found

CREATE OR REPLACE RULE experiment_properties_update AS
    ON UPDATE TO experiment_properties
    WHERE (OLD.VALUE IS NOT NULL AND decode(replace(substring(OLD.value from 1 for 1), '\', '\\'), 'escape') != E'\\xefbfbd' AND OLD.VALUE != NEW.VALUE)
        OR (OLD.CVTE_ID IS NOT NULL AND OLD.CVTE_ID != NEW.CVTE_ID)
        OR (OLD.MATE_PROP_ID IS NOT NULL AND OLD.MATE_PROP_ID != NEW.MATE_PROP_ID)
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
         (select (m.code || ' [' || mt.code || ']') from materials as m join material_types as mt on m.maty_id = mt.id where m.id = OLD.MATE_PROP_ID),
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
        OR OLD.MATE_PROP_ID IS NOT NULL
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
         (select (m.code || ' [' || mt.code || ']') from materials as m join material_types as mt on m.maty_id = mt.id where m.id = OLD.MATE_PROP_ID),
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
        OR (OLD.MATE_PROP_ID IS NOT NULL AND OLD.MATE_PROP_ID != NEW.MATE_PROP_ID)
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
         (select (m.code || ' [' || mt.code || ']') from materials as m join material_types as mt on m.maty_id = mt.id where m.id = OLD.MATE_PROP_ID),
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
        OR OLD.MATE_PROP_ID IS NOT NULL
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
         (select (m.code || ' [' || mt.code || ']') from materials as m join material_types as mt on m.maty_id = mt.id where m.id = OLD.MATE_PROP_ID),
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
        OR (OLD.MATE_PROP_ID IS NOT NULL AND OLD.MATE_PROP_ID != NEW.MATE_PROP_ID)
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
         (select (m.code || ' [' || mt.code || ']') from materials as m join material_types as mt on m.maty_id = mt.id where m.id = OLD.MATE_PROP_ID),
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
        OR OLD.MATE_PROP_ID IS NOT NULL
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
         (select (m.code || ' [' || mt.code || ']') from materials as m join material_types as mt on m.maty_id = mt.id where m.id = OLD.MATE_PROP_ID),
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

-- Delete sample property values that reference samples that do not exist anymore (needs to be done before foreign keys creation)

DELETE FROM experiment_properties WHERE samp_prop_id IS NOT NULL AND samp_prop_id NOT IN (SELECT id FROM SAMPLES_ALL);
DELETE FROM sample_properties WHERE samp_prop_id IS NOT NULL AND samp_prop_id NOT IN (SELECT id FROM SAMPLES_ALL);
DELETE FROM data_set_properties WHERE samp_prop_id IS NOT NULL AND samp_prop_id NOT IN (SELECT id FROM SAMPLES_ALL);

-- Create foreign keys for sample property values (i.e. values that point to samples_all.id table)

ALTER TABLE IF EXISTS experiment_properties
    ADD CONSTRAINT expr_samp_prop_fk FOREIGN KEY (samp_prop_id)
    REFERENCES samples_all (id)
    ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE IF EXISTS sample_properties
    ADD CONSTRAINT sapr_samp_prop_fk FOREIGN KEY (samp_prop_id)
    REFERENCES samples_all (id)
    ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE IF EXISTS data_set_properties
    ADD CONSTRAINT dspr_samp_prop_fk FOREIGN KEY (samp_prop_id)
    REFERENCES samples_all (id)
    ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED;
