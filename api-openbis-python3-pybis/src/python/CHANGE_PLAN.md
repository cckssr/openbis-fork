# CHANGE PLAN: Modernizing pybis for maintainability and user experience

> **SUPERSEDED** — kept for history. The active plan for the v2 clean-break
> refactor is `REFACTOR_PLAN.md` (spec: `../pybis_refactor_prompt.md`).

The Core Problem: Three Unrelated Concerns Fused Together

pybis conflates three distinct responsibilities in one package, and that root cause explains every symptom you and your colleagues have identified:

1. HTTP transport — JSON-RPC calls to the openBIS AS v3 API
2. Entity model — Sample, Experiment, DataSet, etc.
3. User-facing query API — the get*\*, new*\_, delete\_\_ surface

The Openbis class in pybis.py (6,197 lines, 113 public methods) is a god object that does all three. Everything else flows from there.

---

The Five Concrete Problems

1. The AttrHolder / PropertyHolder split namespace

Every object carries self.a (system attributes) and self.p (user-defined properties), both using **dict** manipulation, with a **call** invoked as self.a(data) to initialize from API responses. **getattr** and
**setattr** on OpenBisObject forward silently to self.a. This means:

- sample.space goes through **getattr** → AttrHolder.**getattr** → self.a.\_space
- sample.my_property goes through... nothing, because it's actually sample.p.my_property
- IDEs have no idea what attributes exist; autocomplete is dead

2. definitions.py as a schema registry

Entity schemas (what can be created, updated, displayed) are plain Python dicts keyed by string. There's no type checking, no discoverability, and validation only happens at runtime. When you add a new attribute, you
touch this dict, then attribute.py, then pybis.py, with no compile-time feedback.

3. Version-check spaghetti duplicated across files

The 'SNAPSHOT' not in version and not version.startswith('6') guard appears nearly verbatim in both openbis_object.py:save() and openbis_object.py:Transaction.commit(). It's the kind of thing that gets copy-pasted a
third time.

4. Things as an intermediate collection type

Things wraps a pandas DataFrame with lazy init. Users have to know to call .df, or iterate (which issues one get\_\* call per row — N+1 query). It's functional but it's a custom type that behaves like neither a list nor a
DataFrame.

5. Two dataset files

Both data_set.py (357 lines) and dataset.py (1,779 lines) exist in the same package. This is the clearest sign of accumulated technical debt.

---

What I Would Recommend

Phase 1 — Static types + tooling (2–3 weeks, zero API change)

Add pyproject.toml with mypy in strict mode, ruff for lint. Add type annotations to the existing code, even if it's just Any placeholders for now. This alone surfaces dozens of bugs and makes every subsequent refactor
safer. Generate .pyi stubs so IDEs start working.

Effort: medium. Risk: near zero. This is also the precondition for everything else — you cannot safely refactor 15k lines of untyped Python without type annotations.

Phase 2 — Break up pybis.py into a repository pattern (4–6 weeks)

Extract OpenBisConnection (auth, HTTP, session token management) as the base object. Build per-entity repositories on top:

ob = Openbis("https://...", user="...", password="...")
ob.samples.get("/SPACE/SAMPLE_CODE")
ob.experiments.list(project="/SPACE/PROJECT")
ob.spaces.create(code="NEW_SPACE")

The repositories are thin — they build the JSON-RPC request, call the connection, deserialize the response. The god class becomes a facade that delegates to them. Existing ob.get_sample(...) stays as a deprecated alias
pointing to ob.samples.get(...).

Effort: significant but incremental. You can do one repository at a time and ship. The hard part is the search criteria builder (the \_subcriteria_for family of functions in pybis.py), which is complex and must be
preserved exactly.

Phase 3 — Replace AttrHolder/PropertyHolder with Pydantic models (4–8 weeks, breaking change)

This is the most valuable and riskiest phase. Replace the dual-namespace trick with proper pydantic.BaseModel subclasses per entity:

class Sample(BaseModel):
code: str
perm_id: str | None = None
space: str | None = None
experiment: str | None = None
properties: dict[str, Any] = {}

properties can remain a dict for user-defined fields (since they're dynamic by nature), but system attributes become proper typed fields. sample.space works as expected. IDE autocomplete works. sample.model_dump()
replaces \_to_json() in the MCP server.

Effort: high, and requires a deprecation period. Users with Jupyter notebooks that do sample.p.my_property need a migration path. The most pragmatic approach is to add **getattr** compatibility shims that emit
DeprecationWarning for one major version.

Phase 4 — Replace Things with typed list subclass (1–2 weeks)

class SampleList(list[Sample]):
def to_dataframe(self) -> pd.DataFrame: ...
def by_identifier(self, ident: str) -> Sample: ...

This is cheap to do alongside Phase 3 since you're already rewriting the model layer.

---

What Not To Do

- Don't rewrite the HTTP transport. The JSON-RPC serialization with Jackson @id handling, the parse_jackson utility, the chunked file upload — this is working and non-trivial. Leave it alone or wrap it.
- Don't add async yet. It's tempting but doubles the surface area. httpx with sync client is a better first step if performance is a concern.
- Don't delete Things in Phase 3. Mark it deprecated, keep it working. Users who iterate over Things in notebooks will break if it disappears silently.

---

Honest Effort Summary

| Phase             | Calendar time | Risk     | User impact                        |
| ----------------- | ------------- | -------- | ---------------------------------- |
| Types + tooling   | 2–3 weeks     | Very low | None                               |
| Repository split  | 4–6 weeks     | Low      | None (additive)                    |
| Pydantic models   | 4–8 weeks     | Medium   | Breaking (deprecation path needed) |
| Clean collections | 1–2 weeks     | Low      | Minor                              |

Total realistic modernization: 3–4 months of focused work for one engineer, with Phases 1–2 shippable independently. Phases 3–4 need a version bump (2.0) and a clear migration guide.

The single highest-leverage first step is Phase 1 — typing. It costs little, immediately improves IDE experience, and makes every subsequent change safer and faster to review.

## Possible improvements and changes

The following list is a non-exhaustive set of possible improvements and changes that could be made to pyBIS. Each item would need to be evaluated for feasibility, impact, and alignment with the overall goals of the project.

- Multiple instances of `get_eln_url()` across different classes (Sample, Experiment, DataSet) could be refactored into a single method in a common base class or utility function to reduce code duplication and improve maintainability.
- Modernize the naming conventions regarding `sample -> object`, `experiment -> collection` with pluralization. Necessary changes are alias creations, documentation updates. Careful changes, since JSON-RPC API still uses the old naming.
