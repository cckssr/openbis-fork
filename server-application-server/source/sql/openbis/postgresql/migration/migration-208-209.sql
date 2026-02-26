-- Keep FTS document version at 003, but align migrated databases with the updated
-- FTS trigger functions introduced for DB version 209.

CREATE OR REPLACE FUNCTION samples_all_tsvector_document_trigger() RETURNS trigger AS $$
DECLARE proj_code VARCHAR;
        space_code VARCHAR;
        container_code VARCHAR;
        sample_code VARCHAR;
        sample_type_code VARCHAR;
        registrator_user_id VARCHAR;
        modifier_user_id VARCHAR;
        identifier VARCHAR := '/';
BEGIN
    IF TG_OP != 'DELETE' THEN
        SELECT code INTO sample_type_code FROM sample_types WHERE id = NEW.saty_id;
        SELECT user_id INTO registrator_user_id FROM persons WHERE id = NEW.pers_id_registerer;
        SELECT user_id INTO modifier_user_id FROM persons WHERE id = NEW.pers_id_modifier;
        IF NEW.space_id IS NOT NULL THEN
            SELECT code INTO STRICT space_code FROM spaces WHERE id = NEW.space_id;
            identifier := identifier || space_code || '/';
        END IF;

        IF NEW.proj_id IS NOT NULL THEN
            SELECT code INTO STRICT proj_code FROM projects WHERE id = NEW.proj_id;
            identifier := identifier || proj_code || '/';
        END IF;

        IF NEW.samp_id_part_of IS NOT NULL THEN
            SELECT code INTO STRICT container_code FROM samples_all WHERE id = NEW.samp_id_part_of;
            sample_code := container_code || ':' || NEW.code;
            NEW.sample_identifier := identifier || sample_code;
            NEW.tsvector_document := setweight((escape_tsvector_string(NEW.perm_id) || ':1')::tsvector, 'A') ||
                                     setweight((escape_tsvector_string(NEW.sample_identifier) || ':1')::tsvector,
                                         'A') ||
                                     setweight((escape_tsvector_string(sample_code) || ':1')::tsvector, 'B') ||
                                     setweight((escape_tsvector_string(container_code) || ':1')::tsvector, 'B') ||
                                     setweight((escape_tsvector_string(NEW.code) || ':1')::tsvector, 'B') ||
                                     coalesce(setweight((escape_tsvector_string(sample_type_code) || ':1')::tsvector,
                                         'D'), ''::tsvector) ||
                                     coalesce(setweight((escape_tsvector_string(registrator_user_id) || ':1')::tsvector,
                                         'D'), ''::tsvector) ||
                                     coalesce(setweight((escape_tsvector_string(modifier_user_id) || ':1')::tsvector,
                                         'D'), ''::tsvector);
        ELSE
            NEW.sample_identifier := identifier || NEW.code;
            NEW.tsvector_document := setweight((escape_tsvector_string(NEW.perm_id) || ':1')::tsvector, 'A') ||
                                     setweight((escape_tsvector_string(NEW.sample_identifier) || ':1')::tsvector,
                                         'A') ||
                                     setweight((escape_tsvector_string(NEW.code) || ':1')::tsvector, 'B') ||
                                     coalesce(setweight((escape_tsvector_string(sample_type_code) || ':1')::tsvector,
                                         'D'), ''::tsvector) ||
                                     coalesce(setweight((escape_tsvector_string(registrator_user_id) || ':1')::tsvector,
                                         'D'), ''::tsvector) ||
                                     coalesce(setweight((escape_tsvector_string(modifier_user_id) || ':1')::tsvector,
                                         'D'), ''::tsvector);
        END IF;
    END IF;
    RETURN NEW;
END
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION experiments_all_tsvector_document_trigger() RETURNS trigger AS $$
DECLARE proj_code VARCHAR;
        space_code VARCHAR;
        experiment_type_code VARCHAR;
        registrator_user_id VARCHAR;
        modifier_user_id VARCHAR;
BEGIN
    SELECT p.code, s.code INTO STRICT proj_code, space_code FROM projects p
            INNER JOIN spaces s ON p.space_id = s.id WHERE p.id = NEW.proj_id;
    SELECT code INTO experiment_type_code FROM experiment_types WHERE id = NEW.exty_id;
    SELECT user_id INTO registrator_user_id FROM persons WHERE id = NEW.pers_id_registerer;
    SELECT user_id INTO modifier_user_id FROM persons WHERE id = NEW.pers_id_modifier;
    NEW.tsvector_document := setweight((escape_tsvector_string(NEW.perm_id) || ':1')::tsvector, 'A') ||
            setweight((escape_tsvector_string('/' || space_code || '/' || proj_code || '/' || NEW.code)
                    || ':1')::tsvector, 'A') ||
            setweight((escape_tsvector_string(NEW.code) || ':1')::tsvector, 'B') ||
            coalesce(setweight((escape_tsvector_string(experiment_type_code) || ':1')::tsvector, 'D'), ''::tsvector) ||
            coalesce(setweight((escape_tsvector_string(registrator_user_id) || ':1')::tsvector, 'D'), ''::tsvector) ||
            coalesce(setweight((escape_tsvector_string(modifier_user_id) || ':1')::tsvector, 'D'), ''::tsvector);
    RETURN NEW;
END
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION experiments_all_in_project_tsvector_document_trigger() RETURNS trigger AS $$
DECLARE new_space_code VARCHAR;
        tsv tsvector;
        exp RECORD;
BEGIN
    IF TG_OP = 'UPDATE' AND NEW.space_id IS DISTINCT FROM OLD.space_id THEN
        SELECT code
        INTO new_space_code
        FROM spaces
        WHERE id = NEW.space_id;

        FOR exp IN
            SELECT expe.id, expe.code, expe.perm_id,
                   exty.code AS experiment_type_code,
                   reg.user_id AS registrator_user_id,
                   mod.user_id AS modifier_user_id
            FROM experiments_all expe
                LEFT JOIN experiment_types exty ON expe.exty_id = exty.id
                LEFT JOIN persons reg ON expe.pers_id_registerer = reg.id
                LEFT JOIN persons mod ON expe.pers_id_modifier = mod.id
            WHERE expe.proj_id = NEW.id
            LOOP
                tsv := setweight((escape_tsvector_string(exp.perm_id) || ':1')::tsvector, 'A') ||
                       setweight((escape_tsvector_string('/' || new_space_code || '/' || NEW.code || '/' || exp.code)
                           || ':1')::tsvector, 'A') ||
                       setweight((escape_tsvector_string(exp.code) || ':1')::tsvector, 'B') ||
                       coalesce(setweight((escape_tsvector_string(exp.experiment_type_code) || ':1')::tsvector, 'D'),
                           ''::tsvector) ||
                       coalesce(setweight((escape_tsvector_string(exp.registrator_user_id) || ':1')::tsvector, 'D'),
                           ''::tsvector) ||
                       coalesce(setweight((escape_tsvector_string(exp.modifier_user_id) || ':1')::tsvector, 'D'),
                           ''::tsvector);
                UPDATE experiments_all
                SET tsvector_document = tsv
                WHERE id = exp.id;
            END LOOP;
    END IF;
    RETURN NEW;
END
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION data_all_tsvector_document_trigger() RETURNS trigger AS $$
DECLARE data_set_type_code VARCHAR;
        registrator_user_id VARCHAR;
        modifier_user_id VARCHAR;
BEGIN
    SELECT code INTO data_set_type_code FROM data_set_types WHERE id = NEW.dsty_id;
    SELECT user_id INTO registrator_user_id FROM persons WHERE id = NEW.pers_id_registerer;
    SELECT user_id INTO modifier_user_id FROM persons WHERE id = NEW.pers_id_modifier;
    NEW.tsvector_document := setweight(('/' || escape_tsvector_string(NEW.code) || ':1')::tsvector, 'A') ||
            setweight((escape_tsvector_string(NEW.code) || ':1')::tsvector, 'B') ||
            coalesce(setweight((escape_tsvector_string(data_set_type_code) || ':1')::tsvector, 'D'), ''::tsvector) ||
            coalesce(setweight((escape_tsvector_string(registrator_user_id) || ':1')::tsvector, 'D'), ''::tsvector) ||
            coalesce(setweight((escape_tsvector_string(modifier_user_id) || ':1')::tsvector, 'D'), ''::tsvector);
    RETURN NEW;
END
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS samples_all_tsvector_document ON samples_all;
CREATE TRIGGER samples_all_tsvector_document BEFORE INSERT OR UPDATE
    ON samples_all FOR EACH ROW EXECUTE PROCEDURE
    samples_all_tsvector_document_trigger();

DROP TRIGGER IF EXISTS experiments_all_tsvector_document ON experiments_all;
CREATE TRIGGER experiments_all_tsvector_document BEFORE INSERT OR UPDATE
    ON experiments_all FOR EACH ROW EXECUTE PROCEDURE
    experiments_all_tsvector_document_trigger();

DROP TRIGGER IF EXISTS experiments_all_in_project_tsvector_document ON projects;
CREATE TRIGGER experiments_all_in_project_tsvector_document AFTER UPDATE
    ON projects FOR EACH ROW EXECUTE PROCEDURE experiments_all_in_project_tsvector_document_trigger();

DROP TRIGGER IF EXISTS data_all_tsvector_document ON data_all;
CREATE TRIGGER data_all_tsvector_document BEFORE INSERT OR UPDATE
    ON data_all FOR EACH ROW EXECUTE PROCEDURE
    data_all_tsvector_document_trigger();

-- Recompute indexed values so migrated DB content matches scratch DB content.
UPDATE samples_all SET id = id;
UPDATE experiments_all SET id = id;
UPDATE data_all SET id = id;
UPDATE projects SET id = id;
