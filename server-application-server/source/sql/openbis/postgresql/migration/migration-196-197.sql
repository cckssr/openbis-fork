-- Migration from 196 to 197

-- modify data_all table

ALTER TABLE data_all ADD COLUMN afs_data boolean_char NOT NULL DEFAULT 'F';
CREATE UNIQUE INDEX data_afs_data_expe_id_samp_id_uk ON data_all (COALESCE(expe_id,-1), COALESCE(samp_id,-1)) WHERE (afs_data = 'T');

-- recreate data and data_deleted views

DROP VIEW data;
DROP VIEW data_deleted;

CREATE VIEW data AS
     SELECT id, code, dsty_id, dast_id, expe_id, expe_frozen, data_producer_code, production_timestamp, samp_id, samp_frozen,
            registration_timestamp, access_timestamp, pers_id_registerer, pers_id_modifier, is_valid, modification_timestamp,
            is_derived, del_id, orig_del, version, data_set_kind,
            frozen, frozen_for_children, frozen_for_parents, frozen_for_comps, frozen_for_conts, tsvector_document, meta_data, afs_data
       FROM data_all
      WHERE del_id IS NULL AND afs_data = 'F';

CREATE VIEW data_deleted AS
     SELECT id, code, dsty_id, dast_id, expe_id, data_producer_code, production_timestamp, samp_id, registration_timestamp, access_timestamp, pers_id_registerer, pers_id_modifier, is_valid, modification_timestamp, is_derived, del_id, orig_del, version, data_set_kind
       FROM data_all
      WHERE del_id IS NOT NULL AND afs_data = 'F';

-- recreate rules for data and data_deleted views

CREATE OR REPLACE RULE data_insert AS
  ON INSERT TO data DO INSTEAD
     INSERT INTO data_all (
       id,
       frozen,
       frozen_for_children,
       frozen_for_parents,
       frozen_for_comps,
       frozen_for_conts,
       code,
       del_id,
       orig_del,
       expe_id,
       expe_frozen,
       dast_id,
       data_producer_code,
       dsty_id,
       is_derived,
       is_valid,
       modification_timestamp,
       access_timestamp,
       pers_id_registerer,
       pers_id_modifier,
       production_timestamp,
       registration_timestamp,
       samp_id,
       samp_frozen,
       version,
       data_set_kind,
       meta_data,
       afs_data
     ) VALUES (
       NEW.id,
       NEW.frozen,
       NEW.frozen_for_children,
       NEW.frozen_for_parents,
       NEW.frozen_for_comps,
       NEW.frozen_for_conts,
       NEW.code,
       NEW.del_id,
       NEW.orig_del,
       NEW.expe_id,
       NEW.expe_frozen,
       NEW.dast_id,
       NEW.data_producer_code,
       NEW.dsty_id,
       NEW.is_derived,
       NEW.is_valid,
       NEW.modification_timestamp,
       NEW.access_timestamp,
       NEW.pers_id_registerer,
       NEW.pers_id_modifier,
       NEW.production_timestamp,
       NEW.registration_timestamp,
       NEW.samp_id,
       NEW.samp_frozen,
       NEW.version,
       NEW.data_set_kind,
       NEW.meta_data,
       NEW.afs_data
     );

CREATE OR REPLACE RULE data_update AS
    ON UPDATE TO data DO INSTEAD
       UPDATE data_all
          SET code = NEW.code,
              frozen = NEW.frozen,
              frozen_for_children = NEW.frozen_for_children,
              frozen_for_parents = NEW.frozen_for_parents,
              frozen_for_comps = NEW.frozen_for_comps,
              frozen_for_conts = NEW.frozen_for_conts,
              del_id = NEW.del_id,
              orig_del = NEW.orig_del,
              expe_id = NEW.expe_id,
              expe_frozen = NEW.expe_frozen,
              dast_id = NEW.dast_id,
              data_producer_code = NEW.data_producer_code,
              dsty_id = NEW.dsty_id,
              is_derived = NEW.is_derived,
              is_valid = NEW.is_valid,
              modification_timestamp = NEW.modification_timestamp,
              access_timestamp = NEW.access_timestamp,
              pers_id_registerer = NEW.pers_id_registerer,
              pers_id_modifier = NEW.pers_id_modifier,
              production_timestamp = NEW.production_timestamp,
              registration_timestamp = NEW.registration_timestamp,
              samp_id = NEW.samp_id,
              samp_frozen = NEW.samp_frozen,
              version = NEW.version,
              data_set_kind = NEW.data_set_kind,
              meta_data = NEW.meta_data,
              afs_data = NEW.afs_data
       WHERE id = NEW.id;

CREATE OR REPLACE RULE data_all AS
    ON DELETE TO data DO INSTEAD
       DELETE FROM data_all
              WHERE id = OLD.id;

CREATE OR REPLACE RULE data_deleted_update AS
    ON UPDATE TO data_deleted DO INSTEAD
       UPDATE data_all
          SET del_id = NEW.del_id,
              orig_del = NEW.orig_del,
              modification_timestamp = NEW.modification_timestamp,
              version = NEW.version
          WHERE id = NEW.id;

CREATE OR REPLACE RULE data_deleted_delete AS
    ON DELETE TO data_deleted DO INSTEAD
       DELETE FROM data_all
              WHERE id = OLD.id;
