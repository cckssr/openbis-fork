from pybis import Openbis
from pybis.openbis_typing import PropertyDataArrayTypes

url = "https://openbis-test1.physik.tu-berlin.de"
token = "admin-260524134445774xF8421C9AD91D8EBA566929DA289AAE81"

o = Openbis(url, token=token)
space = o.get_space("DEVICES")
# print(
#     o.get_objects(
#         attrs=["registrator.email", "type.generatedCodePrefix"],
#         props=["$NAME", "$SUPPLIER.COMPANY_EMAIL"],
#         count=20,
#         raw_response=True,
#     )
# )

print(o.get_project("20260428132502678-2488").get_eln_url())

print("ARRAY_INTEGER" in PropertyDataArrayTypes.__args__)
