"""Uses the old sample/experiment vocabulary."""


def run(o):
    sample = o.get_object("/SPACE/PROJ/S1")
    samples = o.search_objects(space="MY_SPACE", type="MOLECULE")
    exp = o.get_collection("/SPACE/PROJ/E1")
    exps = o.search_collections(project="/SPACE/PROJ")
    new = o.new_object("MOLECULE", code="S-2")
    coll = o.new_collection("DEFAULT_EXPERIMENT", code="E-2", project="/SPACE/PROJ")
    types = o.search_object_types()
    et = o.get_collection_type("DEFAULT_EXPERIMENT")
    return sample, samples, exp, exps, new, coll, types, et
