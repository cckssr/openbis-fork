# pybis v2 Refactor Plan (Phase 0 Audit)

Status: **approved baseline** for the clean-break major-version refactor specified in
`../pybis_refactor_prompt.md`. Supersedes `CHANGE_PLAN.md` (kept for history).

## 1. Current module structure (line counts at checkpoint commit)

| Module | Lines | Role | Target |
|---|---:|---|---|
| `pybis/pybis.py` | 6,601 | God object `Openbis` (110 public methods, 15 aliases, 3 properties, 25 private) + module-level search builders + `ServerInformation` | split → `client.py`, `auth.py`, `api/rpc.py`, `api/search.py`, `entities/server.py` |
| `pybis/dataset.py` | 2,464 | `DataSet` entity + upload/download queues, zip handling | `entities/dataset.py` + `api/transfer.py` |
| `pybis/attribute.py` | 1,226 | `AttrHolder` — attribute store + create/update payload engine | `entities/_attrs.py` (private, ~80% kept) |
| `pybis/entity_type.py` | 914 | `EntityType` mixin + Sample/Experiment/DataSet/Material types | `entities/entity_type.py` (`ObjectType`, `CollectionType`, …) |
| `pybis/openbis_object.py` | 736 | `OpenBisObject` base + `Transaction` | `entities/base.py` + `api/transaction.py` |
| `pybis/spreadsheet.py` | 709 | ELN spreadsheet value type (XML/base64) | retrofit in place |
| `pybis/afs_client.py` | 623 | AFS file storage client | `afs.py` |
| `pybis/fast_download.py` | 581 | Parallel download protocol | `api/transfer.py` (retype only) |
| `pybis/definitions.py` | 562 | Per-entity schema registry + `fetch_option` dict | `api/definitions.py` (internal, kept) |
| `pybis/imaging.py` | 492 | Imaging technology API | retrofit in place |
| `pybis/sample.py` | 480 | `Sample` entity | `entities/object.py` (`Object`) |
| `pybis/vocabulary.py` | 463 | `Vocabulary` + `VocabularyTerm` | `entities/vocabulary.py` |
| `pybis/property.py` | 421 | `PropertyHolder` (validation) | merged into `entities/_properties.py` (`PropertyBag`) |
| `pybis/utils.py` | 402 | Jackson @id (de)ref, is_identifier/is_permid, extract_* | Jackson → `api/rpc.py`; classification → `api/identifiers.py`; rest internal `utils.py` |
| `pybis/data_set.py` | 354 | Legacy git-dataset creation | merged into `entities/dataset.py` (git section) |
| `pybis/experiment.py` | 329 | `Experiment` entity | `entities/collection.py` (`Collection`) |
| `pybis/person.py` | 325 | `Person` | `entities/person.py` (+ RoleAssignment) |
| `pybis/semantic_annotation.py` | 311 | `SemanticAnnotation` | `entities/semantic_annotation.py` |
| `pybis/group.py` | 295 | `Group` (authorizationGroup) | `entities/group.py` |
| `pybis/space.py` | 269 | `Space` | `entities/space.py` |
| `pybis/things.py` | 255 | `Things` DataFrame container | `types/results.py` (`SearchResult[T]` + `.df`) |
| `pybis/type_group.py` | 208 | Type groups | `entities/type_group.py` |
| `pybis/chunk.py` | 208 | Transfer chunking | `api/transfer.py` |
| `pybis/material.py` | 188 | `Material` (deprecated) | `entities/material.py` (deprecated-on-arrival) |
| `pybis/project.py` | 164 | `Project` | `entities/project.py` |
| `pybis/property_reformatter.py` | 147 | Value coercion (TIMESTAMP/ARRAY/spreadsheet) | merged into `entities/_properties.py` |
| `pybis/tag.py` | 119 | `Tag` | `entities/tag.py` |
| `pybis/openbis_typing.py` | 114 | NewTypes/Literals seed (this branch) | dissolved into `types/` |
| `pybis/role_assignment.py` | 113 | `RoleAssignment` | `entities/person.py` |
| `pybis/attachment.py` | 109 | `Attachment` | `entities/attachment.py` |

New files: `client.py`, `auth.py`, `exceptions.py`, `_compat.py`,
`types/{identifiers,literals,typed_dicts,values,protocols,results}.py`,
`api/{rpc,search,filters,identifiers,definitions,transaction,transfer}.py`,
`entities/*`, plus `MIGRATION_GUIDE.md` and `migrate.py`.

## 2. Entity class hierarchy (current)

```
OpenBisObject (openbis_object.py:40)          # __init_subclass__(entity=…, single_item_method_name=…)
 ├ composes self.a = AttrHolder (attribute.py:38)
 │    • definitions.py-driven attrs_new/attrs_up payload engine
 │    • FieldUpdateValue, ListUpdateAction(Add/Remove/Set), freeze flags
 │    • ELN 4-part identifier normalization; _is_new flag; parent/child delta tracking
 ├ composes self.p = PropertyHolder (property.py:34)
 │    • per-type validation (CONTROLLEDVOCABULARY terms preloaded, ARRAY_*, multiValue)
 │    • PropertyReformatter: TIMESTAMP/DATE/SAMPLE-link/ARRAY_*/spreadsheet-XML-base64 coercion
 ├ Sample, Experiment, DataSet, Space, Project, Vocabulary, Tag, Group, Material(deprecated)
 ├ SampleType/ExperimentType/DataSetType/MaterialType (also mix in EntityType)
 └ Transaction (openbis_object.py:471) — batched create/update/delete

Not OpenBisObject: Person, RoleAssignment (bare AttrHolder), SemanticAnnotation,
Things (DataFrame container), Spreadsheet, ImagingControl, AfsClient/File.
```

Old↔new vocabulary today: bare assignment aliases on `Openbis` (15, see §3) plus
`object = sample` / `collection = experiment` properties on `OpenBisObject` and a
`name_map` in `AttrHolder.__getattr__`.

## 3. Complete public-method inventory & disposition

Legend: **S** = renamed with `_compat` shim · **K** = kept (params snake_cased,
`only_data`/`use_cache`/`raw_response`/`attrs` removed) · **D** = deleted, shim only ·
**I** = becomes internal.

### Auth & session → `auth.py`
| Method | Disposition |
|---|---|
| `login`, `logout`, `token` (prop), `set_token`, `is_token_valid`, `is_session_active`, `get_session_info` | K (+ `__enter__`/`__exit__`) |
| `gen_token_path`, `save_token_on_behalf` | K |
| `get_or_create_personal_access_token` | K (`sessionName→session_name`, `validFrom/validTo→valid_from/valid_to`) |
| `get_personal_access_token` | K (`permId→perm_id`) |
| `get_personal_access_tokens` | S → `search_personal_access_tokens` |

### Single-entity getters (all gain `classify_id` + `…_or_raise`, return `X | None`)
| Current | New |
|---|---|
| `get_sample` (+alias `get_object`) | `get_object` (S; `sample_ident→identifier` positional) |
| `get_experiment` (+alias `get_collection`) | `get_collection` (S) |
| `get_dataset`, `get_project`, `get_space`, `get_person` (+alias `get_user`), `get_group`, `get_role_assignment`, `get_term`, `get_vocabulary`, `get_tag`, `get_plugin`, `get_semantic_annotation`, `get_type_group`, `get_type_group_assignment` | K |
| `get_external_data_management_system` (+alias `get_externalDms`) | K (alias D) |

### Plural searches → `search_*`, return `SearchResult[T]`, gain `iter_*`
| Current | New |
|---|---|
| `get_samples`/`get_objects` | `search_objects` (S) |
| `get_experiments`/`get_collections` | `search_collections` (S) |
| `get_datasets` | `search_datasets` (S; `sample=→object=`, `experiment=→collection=`) |
| `get_spaces`, `get_projects`, `get_persons`/`get_users`, `get_groups`, `get_role_assignments`, `get_terms`, `get_vocabularies`, `get_tags`, `get_plugins`, `get_deletions`, `get_external_data_management_systems`, `get_semantic_annotations`, `get_datastores` | `search_<plural>` (S) |
| `search_semantic_annotations` (already search-named) | merged into `search_semantic_annotations` (K) |
| `search_type_group`, `search_type_group_assignment` | K (pluralize: `search_type_groups`, `search_type_group_assignments`) |
| `search_files` | K (`data_set_id→dataset_id`) |

### Entity types
| Current | New |
|---|---|
| `get_sample_type(s)`/`get_object_type(s)` | `get_object_type` / `search_object_types` (S) |
| `get_experiment_type(s)`/`get_collection_type(s)` | `get_collection_type` / `search_collection_types` (S) |
| `get_dataset_type(s)`, `get_material_type(s)`, `get_property_type(s)` | `get_*` / `search_*s` (S for plurals) |
| `get_entity_type`, `get_entity_types` | I (generic delegates) |
| `get_sample_type_new` | D (experimental leftover) |

### Constructors (`new_*`, return unsaved entity)
| Current | New |
|---|---|
| `new_sample`/`new_object` | `new_object` (S) |
| `new_experiment`/`new_collection` | `new_collection` (S) |
| `new_sample_type`/`new_object_type`, `new_experiment_type`/`new_collection_type` | `new_object_type`, `new_collection_type` (S; `generatedCodePrefix` etc. snake_cased) |
| `new_dataset`, `new_dataset_type`, `new_material_type` (deprecated), `new_property_type`, `new_space`, `new_project`, `new_person`, `new_group`, `new_vocabulary`, `new_term`, `new_tag`, `new_plugin`, `new_semantic_annotation`, `new_type_group`, `new_type_group_assignment`, `new_transaction`, `new_spreadsheet` | K (snake_case params) |
| `new_git_data_set` | K (`sample=→object=`, `experiment=→collection=`) |
| `new_content_copy`, `delete_content_copy`, `create_external_data_management_system` | K |

### Misc
| Current | New |
|---|---|
| `assign_role` | K (`userId→user_id` etc.) |
| `confirm_deletions`, `revert_deletions` | K |
| `delete_entity`, `delete_openbis_entity` | I |
| `delete_type_group`, `delete_type_group_assignment` | K |
| `execute_custom_as_service`, `execute_custom_dss_service` | K (typed) |
| `gen_code`, `gen_codes` | K |
| `gen_permId`, `create_permId` | S → `gen_perm_ids`, `create_perm_id` |
| `get_server_information` | K (returns new `ServerInformation`) |
| `clear_cache` | K (`use_cache` params removed everywhere; cache transparent) |
| `mount`, `unmount`, `is_mounted`, `get_mountpoint` | K |
| `sample_to_sample_id`, `experiment_to_experiment_id`, `data_set_to_data_set_id`, `external_data_managment_system_to_dms_id` (note typo) | D (internal `api/rpc.py` helpers) |
| `decode_attribute` | I |
| `spaces`, `projects` properties | D (use `search_spaces()` / `search_projects()`) |
| `get_objects`/`get_object`/… 15 assignment aliases | replaced by canonical new names + shims |

## 4. Additional issues found beyond the pre-audit

1. **Date operator semantics bug**: `_subcriteria_for_properties` maps `>` to
   `DateLaterThanOrEqualToValue` (inclusive) and `<` to `DateEarlierThanOrEqualToValue` —
   strict and non-strict comparisons are conflated for dates. The typed filter DSL fixes
   this; document the behavior change in MIGRATION_GUIDE.
2. **Typo in public API**: `external_data_managment_system_to_dms_id`.
3. **`get_sample_type_new`** — experimental duplicate of `get_sample_type`; delete.
4. **Aliases not in the spec's 14-method list**: `get_users`/`get_user`, `get_externalDms`,
   `get_objects`, `get_collections`, `get_object_types`, `get_collection_types` — all shimmed.
5. **Cache staleness**: `_object_cache` is never invalidated on `save()`/`delete()`;
   new transparent cache invalidates on write.
6. **Packaging rot**: `python_requires>=3.6` (code already uses 3.10 syntax),
   `pytest` in `install_requires`, stub CLI (`scripts/cli.py` prints "called"),
   version string `6.8.1.0-rc0` with non-PEP440 history.
7. **`Things.get_parents()/get_children()`** set-level relational queries have no spec
   equivalent — documented hard break (use per-entity `.parents`/`.children` or `search_*`).
8. **`data_set.py` vs `dataset.py`** duplication (git-dataset creation lives in the former).
9. **Entities lack `__eq__`/`__hash__`/useful `__repr__`** — fixed by `entities/base.py`.
10. **`search_semantic_annotations` precedent**: one method already uses the new naming
    convention while its `get_semantic_annotations` twin coexists — exactly the dual-API
    confusion this refactor removes.
11. **Property keys are silently lower-cased** by `PropertyHolder`; `PropertyBag` keeps
    case-insensitive lookup but preserves server casing in iteration.
12. **No mocked tests**: all 109 tests need a live server at `https://localhost:8443`.

## 5. Execution: work packages

WP0 checkpoint/tooling → WP1 `types/` → WP2 exceptions/rpc/auth/client-move →
WP3 mock-test infra + captured fixtures → WP4 identifiers/filters/search builders →
WP5 entity core (payload-snapshot goldens → base/_attrs/PropertyBag/transaction) →
WP6 server info + `@requires_version` → WP7 Space+Project (template wave) →
WP8 Collection+Object+types → WP9 DataSet+transfer+AFS+git → WP10 remaining entities →
WP11 imaging/spreadsheet/services/cache → WP12 compat consolidation + public surface →
WP13 MIGRATION_GUIDE.md → WP14 migrate.py → WP15 integration-test port + coverage + release.

Invariants at every commit:
- exactly one `Openbis` class; legacy integration suite green via `_compat` shims;
- shims added in the same commit as each rename;
- `mypy pybis/ --strict` clean (override list only shrinks);
- no old vocabulary outside `_compat.py` once a wave completes (grep gate; `@type`
  protocol strings in `api/definitions.py`/`api/rpc.py` exempt).

## 6. Status (as of 2026-06-12)

**Done:** WP0–WP10, WP13, WP14, and the WP11 rename wave (wave 5: plugins,
property types, material types, semantic annotations, external DMS,
personal access tokens — new mixins in `entities/{plugin,semantic_annotation,
external_dms,pat}.py` and property/material types in
`entities/entity_type.py`; `Plugin`/`ExternalDMS`/`PersonalAccessToken`
classes moved out of client.py). 580 offline tests green (unit + migration);
`mypy pybis/ --strict` clean (66 files; legacy modules still on the
override list); ruff clean. Deliverables in place: `MIGRATION_GUIDE.md`,
`migrate.py` (+ fixture tests), `_compat.py` shims for every rename,
payload-snapshot goldens, mock-RPC test infra.

**Strategy deviations from the original plan (intentional):**
- *Wrap-first instead of file moves:* AttrHolder/PropertyHolder/definitions/
  transfer stack stay in their legacy files as the proven engine; the new
  typed surface wraps them. Payload goldens guard the engine. The physical
  moves into `entities/_attrs.py` etc. now belong to WP11/12.
- Exception classes dual-inherit `ValueError` for the migration window.
- `get_datastores` not renamed (low value); `kind=` filtering on
  `search_datasets` raises (server cannot search by kind).

**Open (next session):**
- WP11 (remaining): strict-typing the legacy modules to empty the mypy
  override list. (Imaging/spreadsheet typing and the transparent-cache
  consolidation are done: `_object_cache` honors `use_cache`, write paths
  invalidate, `clear_cache("vocabulary")` also drops term lists, and all
  internal `only_data=True` refetches were ported to the v2 getters —
  type_group.py still uses its unmigrated legacy getter.)
- WP12: compat audit vs §3 inventory, `__init__.py` public surface,
  delete the `pybis/pybis.py` stub, old-vocabulary grep gate.
- WP15: live verification — no openBIS instance was reachable in the dev
  environment (Docker not installed); the legacy integration suite
  (74 tests, `--integration`) has NOT yet run against the refactor. Start
  one via the CONTRIBUTING.md docker command, then:
  `pytest tests/ --integration -q`. Then port the legacy tests with
  `migrate.py --write` into `tests/integration/`, backfill unit coverage
  (currently 45% vs the 85% gate — the untested mass is the legacy half:
  client.py 4k lines, dataset/transfer, attribute.py), jenkinstest update,
  CHANGELOG, README.
