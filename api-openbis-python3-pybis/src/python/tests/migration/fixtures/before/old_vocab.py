"""Uses the old sample/experiment vocabulary."""


def run(o):
    sample = o.get_sample("/SPACE/PROJ/S1")
    samples = o.get_samples(space="MY_SPACE", type="MOLECULE")
    exp = o.get_experiment("/SPACE/PROJ/E1")
    exps = o.get_experiments(project="/SPACE/PROJ")
    new = o.new_sample("MOLECULE", code="S-2")
    coll = o.new_experiment("DEFAULT_EXPERIMENT", "E-2", "/SPACE/PROJ")
    types = o.get_sample_types()
    et = o.get_experiment_type("DEFAULT_EXPERIMENT")
    return sample, samples, exp, exps, new, coll, types, et
