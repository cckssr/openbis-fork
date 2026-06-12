"""Uses old-vocabulary parameter names in dataset calls."""


def run(o):
    a = o.search_datasets(object="/SPACE/PROJ/S1")
    b = o.search_datasets(collection="/SPACE/PROJ/E1")
    c = o.new_dataset(type="RAW_DATA", object="/SPACE/PROJ/S1", properties={"NOTES": "x"})
    # TODO[pybis-migrate]: magic operator string in properties — use pybis.api.filters (e.g. filters.gte(3)) instead
    d = o.search_datasets(properties={"ATOMS": ">= 3"})
    return a, b, c, d
