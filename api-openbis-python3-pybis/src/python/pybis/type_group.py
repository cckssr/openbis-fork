#   Copyright ETH 2025 Zürich, Scientific IT Services
#
#   Licensed under the Apache License, Version 2.0 (the "License");
#   you may not use this file except in compliance with the License.
#   You may obtain a copy of the License at
#
#        http://www.apache.org/licenses/LICENSE-2.0
#
#   Unless required by applicable law or agreed to in writing, software
#   distributed under the License is distributed on an "AS IS" BASIS,
#   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#   See the License for the specific language governing permissions and
#   limitations under the License.
#
from .openbis_object import OpenBisObject
from .things import Things
from .utils import VERBOSE, extract_permid, extract_nested_permid, format_timestamp
from .definitions import openbis_definitions, get_type_for_entity, get_method_for_entity

import copy

from pandas import DataFrame
from tabulate import tabulate

from .definitions import (
    get_method_for_entity,
    get_type_for_entity,
)
from .openbis_object import OpenBisObject
from .things import Things
from .utils import (
    format_timestamp,
    extract_code,
    extract_person,
    VERBOSE,
    nvl,
)

class TypeGroupAssignment:
    def __init__(
            self, openbis_obj, data=None, **kwargs
    ):
        self.openbis = openbis_obj
        self.data = data

    def __getattr__(self, name):
        if name in self._attrs():
            if name in self.data:
                return self.data[name]
            else:
                return ""

    def __repr__(self):
        """same thing as _repr_html_() but for IPython"""
        headers = ["attribute", "value"]
        lines = []
        for attr in self._attrs():
            lines.append([attr, nvl(getattr(self, attr, ""))])

        return tabulate(lines, headers=headers)

    # def get_property_type(self):
    #     return self.openbis.get_property_type(self.data["propertyType"]["code"])

    def _attrs(self):
        return [
            "sampleType",
            "typeGroup",
            "registrator",
            "registrationDate",
            "managedInternally",
        ]

class TypeGroup(
    OpenBisObject, entity="typeGroup", single_item_method_name="get_type_group"
):
    """Managing openBIS authorization groups"""

    def __dir__(self):
        return [
            "id",
            "code",
            "registrator",
            "registrationDate",
            "modifier",
            "modificationDate",
            "managedInternally",
            "metaData",
            "delete()",
            "save()",
        ]

    def get_assignments(self):
        """Get all roles that are assigned to this group.
        Provide additional search arguments to refine your search.

        Usage::
            group.get_roles()
            group.get_roles(space='TEST_SPACE')
        """
        attrs = [
            "sampleType",
            "typeGroup",
            "registrator",
            "registrationDate",
            "managedInternally",
        ]

        pas = self.openbis.search_type_group_assignment(type_group=self.id, sample_type="*")
        pas = pas['objects']


        def create_data_frame(attrs, props, response):
            df = DataFrame(response, columns=attrs)

            if "sampleType" in df:
                df["sampleType"] = df["sampleType"].map(extract_code)

            if "typeGroup" in df:
                df["typeGroup"] = df["typeGroup"].map(extract_code)

            if "registrationDate" in df:
                df["registrationDate"] = df["registrationDate"].map(format_timestamp)
                df["registrator"] = df["registrator"].map(extract_person)

            return df

        def create_objects(response):
            result = []
            for element in response:
                obj = copy.deepcopy(element)
                obj["sampleType"] = extract_code(obj["sampleType"])
                obj["typeGroup"] = extract_code(obj["typeGroup"])
                obj["registrationDate"] = format_timestamp(obj["registrationDate"])
                obj["registrator"] = extract_person(obj["registrator"])
                result += [TypeGroupAssignment(openbis_obj=self.openbis, data=obj)]
            return result

        return Things(
            openbis_obj=self.openbis,
            entity="typeGroup",
            single_item_method=self.openbis.get_type_group,
            identifier_name="typeGroup",
            start_with=1,
            count=len(pas),
            totalCount=len(pas),
            response=pas,
            df_initializer=create_data_frame,
            objects_initializer=create_objects,
            attrs=attrs
        )

    def delete(self, reason='pybis delete'):
        """Delete this type group"""
        if not self.data:
            return

        delete_type = get_type_for_entity("typeGroup", "delete")
        method = get_method_for_entity("typeGroup", "delete")

        request = {
            "method": method,
            "params": [
                self.openbis.token,
                [
                    {
                        "permId": self.id,
                        "@type": "as.dto.typegroup.id.TypeGroupId",
                    }
                ],
                {"reason": reason, **delete_type},
            ],
        }
        resp = self.openbis._post_request(self.openbis.as_v3, request)
        if VERBOSE:
            print(f"{self.entity} {self.code} successfully deleted.")


    def save(self):
        if self.is_new:
            request = self._new_attrs("createTypeGroups")
            resp = self.openbis._post_request(self.openbis.as_v3, request)

            if VERBOSE:
                print("Type group successfully created.")
            data = self.openbis.get_type_group(resp[0]["permId"], only_data=True)
            self._set_data(data)
            return self

        else:
            request = self._up_attrs("updateTypeGroups")
            request["params"][1][0]["typeGroupId"] = {
                "@type": "as.dto.typegroup.id.TypeGroupId",
                "permId": self.id,
            }
            request["params"][1][0]["code"] = {
                "value": self.code,
                "isModified": True,
                "@type": "as.dto.common.update.FieldUpdateValue",
            }
            self.openbis._post_request(self.openbis.as_v3, request)
            if VERBOSE:
                print("Type group successfully updated.")
            # re-fetch type group from openBIS
            new_data = self.openbis.get_type_group(self.code, only_data=True)
            self._set_data(new_data)
            return self