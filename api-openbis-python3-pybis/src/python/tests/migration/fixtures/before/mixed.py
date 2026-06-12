"""Realistic script mixing several issue types."""
from pybis.pybis import Openbis
from pybis.sample import Sample


def main():
    o = Openbis("https://openbis.example.com")
    o.login("user", "password")
    samples = o.get_samples(
        space="MY_SPACE",
        type="MOLECULE",
        withParents=True,
        permId=None,
        props="*",
    )
    for sample in samples:
        print(sample.permId)
    one = o.get_sample("/SPACE/PROJ/S1", only_data=True)
    terms = o.get_terms(vocabulary="MY_VOC")
    o.logout()
