"""Uses camelCase parameters."""


def run(o):
    a = o.get_samples(permId="20240101000000000-1", withParents=True)
    b = o.get_samples(withChildren="/SPACE/PROJ/S1")
    c = o.get_samples(withParents=some_flag)
    d = o.new_sample_type("T", generatedCodePrefix="X", subcodeUnique=True)
    return a, b, c, d
