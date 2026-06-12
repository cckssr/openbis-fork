"""Uses camelCase parameters."""


def run(o):
    a = o.search_objects(perm_id="20240101000000000-1", with_parents=True)
    b = o.search_objects(children="/SPACE/PROJ/S1")
    # TODO[pybis-migrate]: withParents= became parents= (relationship filter); pass with_parents=True if a fetch flag was intended
    c = o.search_objects(parents=some_flag)
    d = o.new_object_type("T", generated_code_prefix="X", subcode_unique=True)
    return a, b, c, d
