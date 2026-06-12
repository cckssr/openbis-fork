"""Realistic script mixing several issue types."""
from pybis import Openbis
# TODO[pybis-migrate]: 'Sample' is now 'Object'; the alias keeps old code working — consider renaming usages
from pybis.entities import Object as Sample


def main():
    o = Openbis("https://openbis.example.com")
    o.login("user", "password")
    samples = o.search_objects(
        space="MY_SPACE",
        type="MOLECULE",
        with_parents=True,
        perm_id=None)
    for sample in samples:
        print(sample.permId)
    # TODO[pybis-migrate]: removed argument only_data= dropped from get_sample(); the call now returns entities — review the usage
    one = o.get_object("/SPACE/PROJ/S1")
    terms = o.search_terms(vocabulary="MY_VOC")
    o.logout()
