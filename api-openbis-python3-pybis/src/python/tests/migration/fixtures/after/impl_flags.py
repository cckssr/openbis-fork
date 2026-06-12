"""Uses removed implementation-detail flags."""


def run(o):
    # TODO[pybis-migrate]: removed argument only_data= dropped from get_sample(); the call now returns entities — review the usage
    data = o.get_object("/SPACE/PROJ/S1")
    # TODO[pybis-migrate]: removed argument raw_response= dropped from get_samples(); the call now returns entities — review the usage
    raw = o.search_objects(space="X")
    # TODO[pybis-migrate]: removed argument use_cache= dropped from get_space(); the call now returns entities — review the usage
    space = o.get_space("MY_SPACE")
    # TODO[pybis-migrate]: removed argument attrs= dropped from get_samples(); the call now returns entities — review the usage
    cols = o.search_objects(space="X")
    return data, raw, space, cols
