"""Uses old-vocabulary parameter names in dataset calls."""


def run(o):
    a = o.get_datasets(sample="/SPACE/PROJ/S1")
    b = o.get_datasets(experiment="/SPACE/PROJ/E1", props="*")
    c = o.new_dataset(type="RAW_DATA", sample="/SPACE/PROJ/S1", props={"NOTES": "x"})
    d = o.get_datasets(where={"ATOMS": ">= 3"})
    return a, b, c, d
