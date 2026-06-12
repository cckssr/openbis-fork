"""Uses removed implementation-detail flags."""


def run(o):
    data = o.get_sample("/SPACE/PROJ/S1", only_data=True)
    raw = o.get_samples(space="X", raw_response=True)
    space = o.get_space("MY_SPACE", use_cache=False)
    cols = o.get_samples(space="X", attrs=["parents", "children"])
    return data, raw, space, cols
