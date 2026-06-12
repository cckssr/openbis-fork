#
#   Copyright ETH 2018 - 2026 Zürich, Scientific IT Services
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

"""
pybis.py

Work with openBIS using Python.

"""

import json
import os
import re
import subprocess
import time
import sys
import zlib
from datetime import datetime
from pathlib import Path
from typing import Optional, Literal, Any
from urllib.parse import urljoin, urlparse

import requests
import urllib3
from dateutil.relativedelta import relativedelta
from pandas import DataFrame

from . import data_set as pbds
from .api.rpc import RpcClient
from .entities.server import ServerInformation
from .entities.admin import _AdminApi
from .entities.misc import _MiscApi
from .entities.vocabulary import _VocabularyApi
from .entities.collection import _CollectionApi
from .entities.dataset import _DataSetApi
from .entities.entity_type import _EntityTypeApi
from .entities.external_dms import ExternalDMS, _ExternalDmsApi
from .entities.object import _ObjectApi
from .entities.pat import PersonalAccessToken, _PersonalAccessTokenApi
from .entities.plugin import Plugin, _PluginApi
from .entities.project import _ProjectApi
from .entities.semantic_annotation import _SemanticAnnotationApi
from .entities.space import _SpaceApi
from .api.rpc import type_for_id as _type_for_id
from .auth import (
    get_saved_pats,
    get_saved_tokens,
    get_token_for_hostname,
    is_session_token,
    save_pats_to_disk,
)
from .openbis_typing import *
from .definitions import (
    get_definition_for_entity,
    get_fetchoption_for_entity,
    get_method_for_entity,
    get_type_for_entity,
    openbis_definitions,
)
from .entity_type import EntityType
from .openbis_object import OpenBisObject
from .things import Things
from .utils import (
    VERBOSE,
    extract_code,
    extract_identifiers,
    extract_nested_identifier,
    extract_nested_permid,
    extract_nested_permids,
    extract_permid,
    extract_person,
    format_timestamp,
    is_identifier,
    is_number,
    is_permid,
    parse_jackson,
    split_identifier,
)
from .vocabulary import Vocabulary
from .spreadsheet import Spreadsheet
from .type_group import TypeGroup
from .imaging import *
from .afs_client import AfsClient

try:
    from warnings import deprecated  # Python 3.13+
except ImportError:
    import functools
    from warnings import warn

    def deprecated(msg: str):
        def decorator(cls):
            orig_init = cls.__init__

            @functools.wraps(orig_init)
            def __init__(self, *args, **kwargs):
                warn(msg, DeprecationWarning, stacklevel=2)
                orig_init(self, *args, **kwargs)

            cls.__init__ = __init__
            return cls

        return decorator


if sys.version_info < (3, 12):
    from typing_extensions import Unpack
else:
    from typing import Unpack


# import the various openBIS entities

LOG_NONE = 0
LOG_SEVERE = 1
LOG_ERROR = 2
LOG_WARNING = 3
LOG_INFO = 4
LOG_ENTRY = 5
LOG_PARM = 6
LOG_DEBUG = 7

DEBUG_LEVEL = LOG_NONE


def now():
    return time.time()


def get_search_type_for_entity(entity, operator=None):
    """Returns a dictionary containing the correct search criteria type
    for a given entity.

    Example:
        >>> get_search_type_for_entity("space")
        >>> # returns:
        >>> {"@type": "as.dto.space.search.SpaceSearchCriteria"}
    """
    search_criteria = {
        "personalAccessToken": "as.dto.pat.search.PersonalAccessTokenSearchCriteria",
        "space": "as.dto.space.search.SpaceSearchCriteria",
        "userId": "as.dto.person.search.UserIdSearchCriteria",
        "email": "as.dto.person.search.EmailSearchCriteria",
        "firstName": "as.dto.person.search.FirstNameSearchCriteria",
        "lastName": "as.dto.person.search.LastNameSearchCriteria",
        "project": "as.dto.project.search.ProjectSearchCriteria",
        "experiment": "as.dto.experiment.search.ExperimentSearchCriteria",
        "experiment_type": "as.dto.experiment.search.ExperimentTypeSearchCriteria",
        "sample": "as.dto.sample.search.SampleSearchCriteria",
        "sample_type": "as.dto.sample.search.SampleTypeSearchCriteria",
        "dataset": "as.dto.dataset.search.DataSetSearchCriteria",
        "dataset_type": "as.dto.dataset.search.DataSetTypeSearchCriteria",
        "external_dms": "as.dto.externaldms.search.ExternalDmsSearchCriteria",
        "material": "as.dto.material.search.MaterialSearchCriteria",
        "material_type": "as.dto.material.search.MaterialTypeSearchCriteria",
        "vocabulary_term": "as.dto.vocabulary.search.VocabularyTermSearchCriteria",
        "tag": "as.dto.tag.search.TagSearchCriteria",
        "authorizationGroup": "as.dto.authorizationgroup.search.AuthorizationGroupSearchCriteria",
        "person": "as.dto.person.search.PersonSearchCriteria",
        "code": "as.dto.common.search.CodeSearchCriteria",
        "global": "as.dto.global.GlobalSearchObject",
        "plugin": "as.dto.plugin.search.PluginSearchCriteria",
        "propertyType": "as.dto.property.search.PropertyTypeSearchCriteria",
    }

    sc = {"@type": search_criteria[entity]}
    if operator is not None:
        sc["operator"] = operator

    return sc


def get_search_criteria(entity, **search_args):
    search_criteria = get_search_type_for_entity(entity)

    criteria = []
    attrs = openbis_definitions(entity)["attrs"]
    for attr in attrs:
        if attr in search_args:
            sub_crit = get_search_type_for_entity(attr)
            sub_crit["fieldValue"] = get_field_value_search(attr, search_args[attr])
            criteria.append(sub_crit)

    search_criteria["criteria"] = criteria
    search_criteria["operator"] = "AND"

    return search_criteria


def crc32(fileName):
    """since Python3 the zlib module returns unsigned integers (2.7: signed int)"""
    prev = 0
    for eachLine in open(fileName, "rb"):
        prev = zlib.crc32(eachLine, prev)
    # return as hex
    return "%x" % (prev & 0xFFFFFFFF)


def _tagIds_for_tags(tags=None, action="Add"):
    """creates an action item to add or remove tags.
    Action is either 'Add', 'Remove' or 'Set'
    """
    if tags is None:
        return
    if not isinstance(tags, list):
        tags = [tags]

    items = list(map(lambda tag: {"code": tag, "@type": "as.dto.tag.id.TagCode"}, tags))

    tagIds = {
        "actions": [
            {
                "items": items,
                "@type": f"as.dto.common.update.ListUpdateAction{action.capitalize()}",
            }
        ],
        "@type": "as.dto.common.update.IdListUpdateValue",
    }
    return tagIds


def _list_update(ids=None, entity=None, action="Add"):
    """creates an action item to add, set or remove ids."""
    if ids is None:
        return
    if not isinstance(ids, list):
        ids = [ids]

    items = list(
        map(
            lambda id: {
                "code": id,
                "@type": f"as.dto.{entity.lower()}.id.{entity}Code",
            },
            ids,
        )
    )

    list_update = {
        "actions": [
            {
                "items": items,
                "@type": f"as.dto.common.update.ListUpdateAction{action.capitalize()}",
            }
        ],
        "@type": "as.dto.common.update.IdListUpdateValue",
    }
    return list_update


def get_field_value_search(field, value, comparison="StringEqualToValue"):
    return {"value": value, "@type": f"as.dto.common.search.{comparison}"}


def _common_search(search_type, value, comparison="StringEqualToValue"):
    sreq = {
        "@type": search_type,
        "fieldValue": {
            "value": value,
            "@type": f"as.dto.common.search.{comparison}",
        },
    }
    return sreq


def _criteria_for_code(code):
    return {
        "fieldValue": {
            "value": code.upper(),
            "@type": "as.dto.common.search.StringEqualToValue",
        },
        "@type": "as.dto.common.search.CodeSearchCriteria",
    }


def _criteria_for_permId(permId):
    return {
        "fieldName": "perm_id",
        "fieldType": "ATTRIBUTE",
        "fieldValue": {
            "value": permId,
            "@type": "as.dto.common.search.StringEqualToValue",
        },
        "@type": "as.dto.common.search.PermIdSearchCriteria",
    }


def _subcriteria_for_userId(userId):
    return {
        "criteria": [
            {
                "fieldName": "userId",
                "fieldType": "ATTRIBUTE",
                "fieldValue": {
                    "value": userId,
                    "@type": "as.dto.common.search.StringEqualToValue",
                },
                "@type": "as.dto.person.search.UserIdSearchCriteria",
            }
        ],
        "@type": "as.dto.person.search.PersonSearchCriteria",
        "operator": "AND",
    }


def _subcriteria_for_type(code, entity):
    return {
        "@type": f"as.dto.{entity.lower()}.search.{entity}TypeSearchCriteria",
        "criteria": [
            {
                "@type": "as.dto.common.search.CodeSearchCriteria",
                "fieldValue": {
                    "value": code.upper(),
                    "@type": "as.dto.common.search.StringEqualToValue",
                },
            }
        ],
    }


def _subcriteria_for_status(status_value):
    status_value = status_value.upper()
    valid_status = "AVAILABLE LOCKED ARCHIVED UNARCHIVE_PENDING ARCHIVE_PENDING BACKUP_PENDING".split()
    if not status_value in valid_status:
        raise ValueError(
            "status must be one of the following: " + ", ".join(valid_status)
        )

    return {
        "@type": "as.dto.dataset.search.PhysicalDataSearchCriteria",
        "operator": "AND",
        "criteria": [
            {
                "@type": "as.dto.dataset.search.StatusSearchCriteria",
                "fieldName": "status",
                "fieldType": "ATTRIBUTE",
                "fieldValue": status_value,
            }
        ],
    }


def _gen_search_criteria(req):
    sreq = {}
    for key, val in req.items():
        if key == "criteria":
            sreq["criteria"] = list(
                map(lambda item: _gen_search_criteria(item), req["criteria"])
            )
        elif key == "code":
            sreq["criteria"] = [
                _common_search("as.dto.common.search.CodeSearchCriteria", val.upper())
            ]
        elif key == "identifier":
            if is_identifier(val):
                # if we have an identifier, we need to search in Space and Code separately
                si = split_identifier(val)
                sreq["criteria"] = []
                if "space" in si:
                    sreq["criteria"].append(
                        _gen_search_criteria({"space": "Space", "code": si["space"]})
                    )
                if "experiment" in si:
                    pass

                if "code" in si:
                    sreq["criteria"].append(
                        _common_search(
                            "as.dto.common.search.CodeSearchCriteria",
                            si["code"].upper(),
                        )
                    )
            elif is_permid(val):
                sreq["criteria"] = [
                    _common_search("as.dto.common.search.PermIdSearchCriteria", val)
                ]
            else:
                # we assume we just got a code
                sreq["criteria"] = [
                    _common_search(
                        "as.dto.common.search.CodeSearchCriteria", val.upper()
                    )
                ]

        elif key == "operator":
            sreq["operator"] = val.upper()
        else:
            sreq["@type"] = f"as.dto.{key}.search.{val}SearchCriteria"
    return sreq


def _subcriteria_for_tags(tags):
    if not isinstance(tags, list):
        tags = [tags]

    criteria = list(
        map(
            lambda tag: {
                "fieldName": "code",
                "fieldType": "ATTRIBUTE",
                "fieldValue": {
                    "value": tag,
                    "@type": "as.dto.common.search.StringEqualToValue",
                },
                "@type": "as.dto.common.search.CodeSearchCriteria",
            },
            tags,
        )
    )

    return {
        "@type": "as.dto.tag.search.TagSearchCriteria",
        "operator": "AND",
        "criteria": criteria,
    }


def _subcriteria_for_is_finished(is_finished):
    return {
        "@type": "as.dto.common.search.StringPropertySearchCriteria",
        "fieldName": "FINISHED_FLAG",
        "fieldType": "PROPERTY",
        "fieldValue": {
            "value": is_finished,
            "@type": "as.dto.common.search.StringEqualToValue",
        },
    }


def _subcriteria_for_properties(prop, value, entity):
    """This internal method creates the JSON RPC criterias for searching
    in properties. It distinguishes between numbers, dates and strings
    and uses the comparative operator (< > >= <=), if available.
    creationDate and modificationDate attributes can be searched as well.
    To search in the properties of parents, children, etc. the user has to
    prefix the propery accordingly:

    - parent_propertyName
    - child_propertyName
    - container_propertyName
    """
    additional_attr = {}
    if "*" in str(value):
        additional_attr["useWildcards"] = True
    else:
        additional_attr["useWildcards"] = False

    search_types = {
        "sample": {
            "parent": "as.dto.sample.search.SampleParentsSearchCriteria",
            "parents": "as.dto.sample.search.SampleParentsSearchCriteria",
            "child": "as.dto.sample.search.SampleChildrenSearchCriteria",
            "children": "as.dto.sample.search.SampleChildrenSearchCriteria",
            "container": "as.dto.sample.search.SampleContainerSearchCriteria",
        },
        "dataset": {
            "parent": "as.dto.dataset.search.DataSetParentsSearchCriteria",
            "parents": "as.dto.dataset.search.DataSetParentsSearchCriteria",
            "child": "as.dto.dataset.search.DataSetChildrenSearchCriteria",
            "children": "as.dto.dataset.search.DataSetChildrenSearchCriteria",
            "container": "as.dto.dataset.search.DataSetContainerSearchCriteria",
        },
    }

    # default values of fieldType, str_type and eq_type
    fieldType = "PROPERTY"
    eq_type = "as.dto.common.search.StringEqualToValue"
    str_type = "as.dto.common.search.StringPropertySearchCriteria"

    is_date = False
    if "date" in prop.lower() and re.search(r"\d{4}\-\d{2}\-\d{2}", value):
        is_date = True
        eq_type = "as.dto.common.search.DateEqualToValue"
        if prop.lower().endswith("registrationdate"):
            str_type = "as.dto.common.search.RegistrationDateSearchCriteria"
            fieldType = "ATTRIBUTE"
        elif prop.lower().endswith("modificationdate"):
            str_type = "as.dto.common.search.ModificationDateSearchCriteria"
            fieldType = "ATTRIBUTE"
        else:
            str_type = "as.dto.common.search.DatePropertySearchCriteria"

    if any(str(value).startswith(operator) for operator in [">", "<", "="]):
        match = re.search(
            r"""
            ^
            (?P<comp_operator>\>\=|\>|\<\=|\<|\=\=|\=)  # extract the comparative operator
            \s*
            (?P<value>.*)                           # extract the value
            """,
            value,
            flags=re.X,
        )
        if match:
            comp_operator = match.groupdict()["comp_operator"]
            value = match.groupdict()["value"]

            # date comparison
            if is_date:
                if comp_operator == ">":
                    eq_type = "as.dto.common.search.DateLaterThanOrEqualToValue"
                elif comp_operator == ">=":
                    eq_type = "as.dto.common.search.DateLaterThanOrEqualToValue"
                elif comp_operator == "<":
                    eq_type = "as.dto.common.search.DateEarlierThanOrEqualToValue"
                elif comp_operator == "<=":
                    eq_type = "as.dto.common.search.DateEarlierThanOrEqualToValue"
                else:
                    eq_type = "as.dto.common.search.DateEqualToValue"

            # numeric comparison
            elif is_number(value):
                str_type = "as.dto.common.search.NumberPropertySearchCriteria"
                if comp_operator == ">":
                    eq_type = "as.dto.common.search.NumberGreaterThanValue"
                elif comp_operator == ">=":
                    eq_type = "as.dto.common.search.NumberGreaterThanOrEqualToValue"
                elif comp_operator == "<":
                    eq_type = "as.dto.common.search.NumberLessThanValue"
                elif comp_operator == "<=":
                    eq_type = "as.dto.common.search.NumberLessThanOrEqualToValue"
                else:
                    eq_type = "as.dto.common.search.NumberEqualToValue"

            # string comparison
            else:
                if comp_operator == ">":
                    eq_type = "as.dto.common.search.StringGreaterThanValue"
                elif comp_operator == ">=":
                    eq_type = "as.dto.common.search.StringGreaterThanOrEqualToValue"
                elif comp_operator == "<":
                    eq_type = "as.dto.common.search.StringLessThanValue"
                elif comp_operator == "<=":
                    eq_type = "as.dto.common.search.StringLessThanOrEqualToValue"
                elif comp_operator == "=":
                    eq_type = "as.dto.common.search.StringEqualToValue"
                    additional_attr["useWildcards"] = False
                else:
                    eq_type = "as.dto.common.search.StringEqualToValue"

    # searching for parent/child/container identifier
    if any(
        relation == prop.lower()
        for relation in [
            "parent",
            "child",
            "container",
            "parents",
            "children",
            "containers",
        ]
    ):
        relation = prop.lower()
        if is_identifier(value):
            identifier_search_type = "as.dto.common.search.IdentifierSearchCriteria"
        # find any parent, child, container
        elif value == "*":
            return {
                "@type": search_types[entity][relation],
                "criteria": [
                    {
                        "@type": "as.dto.common.search.AnyFieldSearchCriteria",
                        "fieldValue": {
                            "@type": "as.dto.common.search.AnyStringValue",
                        },
                    }
                ],
            }
        elif is_permid(value):
            identifier_search_type = "as.dto.common.search.PermIdSearchCriteria"
        else:
            identifier_search_type = "as.dto.common.search.CodeSearchCriteria"
        return {
            "@type": search_types[entity][relation],
            "criteria": [
                {
                    "@type": identifier_search_type,
                    "fieldType": "ATTRIBUTE",
                    "fieldValue": {
                        "@type": "as.dto.common.search.StringEqualToValue",
                        "value": value,
                    },
                    **additional_attr,
                }
            ],
        }

    # searching for parent/child/container property:
    elif any(
        prop.lower().startswith(relation)
        for relation in ["parent_", "child_", "container_"]
    ):
        match = re.search(r"^(\w+?)_(.*)", prop.lower())
        if match:
            relation, property_name = match.groups()
            return {
                "@type": search_types[entity][relation],
                "criteria": [
                    {
                        "@type": str_type,
                        "fieldName": property_name.upper(),
                        "fieldType": fieldType,
                        "fieldValue": {
                            "@type": eq_type,
                            "value": value,
                        },
                        **additional_attr,
                    }
                ],
            }

    # searching for properties
    if prop.startswith("_"):
        fieldName = "$" + prop[1:]
    else:
        fieldName = prop
    return {
        "@type": str_type,
        "fieldName": fieldName.upper(),
        "fieldType": fieldType,
        "fieldValue": {"value": value, "@type": eq_type},
        **additional_attr,
    }


def _subcriteria_for(thing, entity, parents_or_children="", operator="AND"):
    """Returns the sub-search criteria for «thing», which can be either:
    - a python object (sample, dataSet, experiment)
    - a permId
    - an identifier
    - a code
    """

    entity, *_ = entity.split(".")
    if _:
        new_entity = ".".join(_)
        subcrit = _subcriteria_for(thing, new_entity)

        search_type = get_type_for_entity(entity, "search", parents_or_children)
        return {"criteria": subcrit, **search_type, "operator": operator}

    if isinstance(thing, str):
        if is_permid(thing):
            return _subcriteria_for_permid(
                thing,
                entity=entity,
                parents_or_children=parents_or_children,
                operator=operator,
            )
        elif is_identifier(thing):
            return _subcriteria_for_identifier(
                thing,
                entity=entity,
                parents_or_children=parents_or_children,
                operator=operator,
            )
        else:
            # look for code
            return _subcriteria_for_code_new(
                thing,
                entity=entity,
                parents_or_children=parents_or_children,
                operator=operator,
            )

    elif isinstance(thing, list):
        criteria = []
        for element in thing:
            crit = _subcriteria_for(element, entity, parents_or_children, operator)
            criteria += crit["criteria"]

        return {"criteria": criteria, "@type": crit["@type"], "operator": "OR"}
    elif thing is None:
        # we just need the type
        search_type = get_type_for_entity(entity, "search", parents_or_children)
        return {"criteria": [], **search_type, "operator": operator}
    else:
        # we passed an object
        return _subcriteria_for_permid(
            thing.permId,
            entity=entity,
            parents_or_children=parents_or_children,
            operator=operator,
        )


def _subcriteria_for_identifier(ids, entity, parents_or_children="", operator="AND"):
    if not isinstance(ids, list):
        ids = [ids]

    criteria = list(
        map(
            lambda id: {
                "@type": "as.dto.common.search.IdentifierSearchCriteria",
                "fieldValue": {
                    "value": id,
                    "@type": "as.dto.common.search.StringEqualToValue",
                },
                "fieldType": "ATTRIBUTE",
                "fieldName": "identifier",
            },
            ids,
        )
    )

    search_type = get_type_for_entity(entity, "search", parents_or_children)
    return {"criteria": criteria, **search_type, "operator": operator}


def _subcriteria_for_permid(permids, entity, parents_or_children="", operator="AND"):
    if not isinstance(permids, list):
        permids = [permids]

    criteria = list(
        map(
            lambda permid: {
                "@type": "as.dto.common.search.PermIdSearchCriteria",
                "fieldValue": {
                    "value": permid,
                    "@type": "as.dto.common.search.StringEqualToValue",
                },
                "fieldType": "ATTRIBUTE",
                "fieldName": "perm_id",
            },
            permids,
        )
    )

    search_type = get_type_for_entity(entity, "search", parents_or_children)
    return {"criteria": criteria, **search_type, "operator": operator}


def _subcriteria_for_permid_new(codes, entity, parents_or_children="", operator="AND"):
    if not isinstance(codes, list):
        codes = [codes]

    criteria = list(
        map(
            lambda code: {
                "@type": "as.dto.common.search.PermIdSearchCriteria",
                "fieldValue": {
                    "value": code,
                    "@type": "as.dto.common.search.StringEqualToValue",
                },
                "fieldType": "ATTRIBUTE",
                "fieldName": "perm_id",
            },
            codes,
        )
    )

    search_type = get_type_for_entity(entity, "search", parents_or_children)
    return {"criteria": criteria, **search_type, "operator": operator}


def _subcriteria_for_code_new(codes, entity, parents_or_children="", operator="AND"):
    if not isinstance(codes, list):
        codes = [codes]

    criteria = list(
        map(
            lambda code: {
                "@type": "as.dto.common.search.CodeSearchCriteria",
                "fieldValue": {
                    "value": code,
                    "@type": "as.dto.common.search.StringEqualToValue",
                },
                "fieldType": "ATTRIBUTE",
                "fieldName": "code",
            },
            codes,
        )
    )

    search_type = get_type_for_entity(entity, "search", parents_or_children)
    return {"criteria": criteria, **search_type, "operator": operator}


def _subcriteria_for_code(code, entity):
    """Creates the often used search criteria for code values. Returns a dictionary.

    Example:
        >>> _subcriteria_for_code("username", "space")

    {
        "criteria": [
            {
                "fieldType": "ATTRIBUTE",
                "@type": "as.dto.common.search.CodeSearchCriteria",
                "fieldName": "code",
                "fieldValue": {
                    "@type": "as.dto.common.search.StringEqualToValue",
                    "value": "USERNAME"
                }
            }
        ],
        "operator": "AND",
        "@type": "as.dto.space.search.SpaceSearchCriteria"
    }
    """
    if code is not None:
        if is_permid(code):
            fieldname = "permId"
            fieldtype = "as.dto.common.search.PermIdSearchCriteria"
        else:
            fieldname = "code"
            fieldtype = "as.dto.common.search.CodeSearchCriteria"

        # search_criteria = get_search_type_for_entity(entity.lower())
        search_criteria = get_type_for_entity(entity, "search")
        search_criteria["criteria"] = [
            {
                "fieldName": fieldname,
                "fieldType": "ATTRIBUTE",
                "fieldValue": {
                    "value": code.upper(),
                    "@type": "as.dto.common.search.StringEqualToValue",
                },
                "@type": fieldtype,
            }
        ]

        search_criteria["operator"] = "AND"
        return search_criteria
    else:
        return get_type_for_entity(entity, "search")
        # return get_search_type_for_entity(entity.lower())


class Openbis(
    _SpaceApi,
    _ProjectApi,
    _ObjectApi,
    _CollectionApi,
    _DataSetApi,
    _EntityTypeApi,
    _AdminApi,
    _VocabularyApi,
    _MiscApi,
    _PluginApi,
    _SemanticAnnotationApi,
    _ExternalDmsApi,
    _PersonalAccessTokenApi,
):
    """Interface for communicating with openBIS.

    Note:
        * A recent version of openBIS is required (minimum 16.05.2).
        * For creation of datasets, the dataset-uploader-api ingestion plugin must be present.

    """

    token: str

    def __init__(
        self,
        url: Optional[str] = None,
        verify_certificates: bool = True,
        token: Optional[str] = None,
        use_cache: bool = True,
        allow_http_but_do_not_use_this_in_production_and_only_within_safe_networks: bool = False,
    ):
        """Initialize a new connection to an openBIS server.

        Examples:
            o = Openbis('https://openbis.example.com')
            o_test = Openbis('https://test_openbis.example.com:8443', verify_certificates=False)

        Args:
            url: https://openbis.example.com
            verify_certificates: set to False when you use self-signed certificates
            token: a valid openBIS token. If not set, pybis will try to read a valid token from ~/.pybis
            use_cache: make openBIS to store spaces, projects, sample types, vocabulary terms and oder more-or-less static objects to optimise speed
            allow_http_but_do_not_use_this_in_production_and_only_within_safe_networks: False
        """
        self.as_v3 = "/openbis/openbis/rmi-application-server-v3.json"
        self.as_v1 = "/openbis/openbis/rmi-general-information-v1.json"
        self.reg_v1 = "/openbis/openbis/rmi-query-v1.json"
        self.dss_v3 = "/datastore_server/rmi-data-store-server-v3.json"
        self.verify_certificates = verify_certificates
        if not verify_certificates:
            urllib3.disable_warnings()

        if url is None:
            url = os.environ.get("OPENBIS_URL") or os.environ.get("OPENBIS_HOST")
            if url is None:
                raise ValueError("please provide a URL you want to connect to.")

        if not url.startswith("http"):
            url = "https://" + url

        url_obj = urlparse(url)
        if url_obj.netloc is None or url_obj.netloc == "":
            raise ValueError(
                "please provide the url in this format: https://openbis.host.ch:8443"
            )
        if url_obj.hostname is None:
            raise ValueError("hostname is missing")
        if (
            url_obj.scheme == "http"
            and not allow_http_but_do_not_use_this_in_production_and_only_within_safe_networks
        ):
            raise ValueError("always use https!")

        self.url = url_obj.geturl()
        self.port = url_obj.port
        self.hostname = url_obj.hostname
        self._rpc = RpcClient(self.url, verify_certificates=verify_certificates)
        self.download_prefix = os.path.join("data", self.hostname)
        self.use_cache = use_cache
        self.cache = {}
        self.server_information = None
        if token is not None:
            try:
                self.set_token(token)
            except ValueError:
                raise ValueError(
                    "This token is no longer valid. Please provide an valid token or use the login method."
                )
        else:
            # We try to set the saved token, during initialisation instead of errors, a message is printed
            try:
                token = self._get_saved_token()
                self.token = token
            except ValueError:
                pass

    def _get_username(self):
        if self.token:
            match = re.search(r"(\$pat-)?(?P<username>.*)-.*", self.token)
            username = match.groupdict()["username"]
            return username
        return ""

    def __enter__(self) -> "Openbis":
        """Enter a context that logs out automatically on exit.

        Example:
            >>> with Openbis("https://openbis.example.com") as client:
            ...     client.login("user", "password")
        """
        return self

    def __exit__(self, *exc_info) -> None:
        """Log out of openBIS when leaving the context."""
        try:
            self.logout()
        except Exception:
            # Logging out of an already-expired session must not mask the
            # original exception (or fail a clean exit).
            pass

    @property
    def token(self):
        return self.__dict__.get("token")

    @token.setter
    def token(self, token: str):
        self.set_token(token)

    def __dir__(self):
        return [
            "url",
            "port",
            "hostname",
            "token",
            "login()",
            "logout()",
            "is_session_active()",
            "is_token_valid()",
            "mount()",
            "unmount()",
            "use_cache",
            "clear_cache()",
            "download_prefix",
            "get_mountpoint()",
            "get_server_information()",
            "get_dataset()",
            "get_datasets()",
            "get_dataset_type()",
            "get_dataset_types()",
            "get_datastores()",
            "gen_code()",
            "get_deletions()",
            "get_experiment()",
            "get_experiments()",
            "get_experiment_type()",
            "get_experiment_types()",
            "get_collection()",
            "get_collections()",
            "get_collection_type()",
            "get_collection_types()",
            "get_external_data_management_systems()",
            "get_external_data_management_system()",
            "get_material_type()",
            "get_material_types()",
            "get_project()",
            "get_projects()",
            "get_sample()",
            "get_object()",
            "get_samples()",
            "get_objects()",
            "get_sample_type()",
            "get_object_type()",
            "get_sample_types()",
            "get_object_types()",
            "get_property_types()",
            "get_property_type()",
            "get_personal_access_tokens()",
            "new_property_type()",
            "get_semantic_annotations()",
            "get_semantic_annotation()",
            "get_space()",
            "get_spaces()",
            "get_tags()",
            "get_tag()",
            "new_tag()",
            "get_terms()",
            "get_term()",
            "get_vocabularies()",
            "get_vocabulary()",
            "new_person()",
            "get_persons()",
            "get_person()",
            "get_groups()",
            "get_group()",
            "get_role_assignments()",
            "get_role_assignment()",
            "get_plugins()",
            "get_plugin()",
            "new_plugin()",
            "new_group()",
            "new_space()",
            "get_type_groups()",
            "get_type_group()",
            "new_project()",
            "new_experiment()",
            "new_collection()",
            "new_sample()",
            "new_object()",
            "new_sample_type()",
            "new_object_type()",
            "new_dataset()",
            "new_dataset_type()",
            "new_experiment_type()",
            "new_collection_type()",
            "new_material_type()",
            "new_semantic_annotation()",
            "new_transaction()",
            "get_or_create_personal_access_token()",
            "set_token()",
        ]

    def _repr_html_(self):
        html = """
            <table border="1" class="dataframe">
            <thead>
                <tr style="text-align: right;">
                <th>attribute</th>
                <th>value</th>
                </tr>
            </thead>
            <tbody>
        """

        attrs = [
            "url",
            "port",
            "hostname",
            "verify_certificates",
            "as_v3",
            "as_v1",
            "reg_v1",
            "token",
        ]
        for attr in attrs:
            html += f"<tr> <td>{attr}</td> <td>{getattr(self, attr, '')}</td> </tr>"

        html += """
            </tbody>
            </table>
        """
        return html

    def gen_token_path(self, os_home: Optional[str] = None) -> str:
        """Generate the path to the saved token file.

        Default is ~/.pybis/hostname.token

        Args:
            os_home: Override the home directory. Defaults to None.

        Returns:
            str: The path to the token file.
        """
        if self.hostname is None:
            raise ValueError(
                "hostname needs to be set before retrieving the token path."
            )

        if os_home is None:
            home = os.path.expanduser("~")
        else:
            home = os_home
        parent_folder = os.path.join(home, ".pybis")
        path = os.path.join(parent_folder, self.hostname + ".token")
        return path

    def save_token_on_behalf(self, os_home: Optional[str] = None) -> None:
        """Save the token to disk and set the correct user permissions.

        Args:
            os_home: Override the home directory. Defaults to None.
        """
        token_path = self._save_token_to_disk(os_home)

        lastIndexOfMinus = (
            len(self.token) - "".join(reversed(self.token)).index("-") - 1
        )
        token_user_name = self.token[0:lastIndexOfMinus]
        if token_user_name.startswith("$pat-"):
            token_user_name = token_user_name[5:]
        from pwd import getpwnam

        token_user_name_uid = getpwnam(token_user_name).pw_uid
        token_user_name_gid = getpwnam(token_user_name).pw_gid

        os.chown(token_path, token_user_name_uid, token_user_name_gid)

        path = Path(token_path)
        token_parent_path = path.parent.absolute()
        os.chown(token_parent_path, token_user_name_uid, token_user_name_gid)

    def _save_token_to_disk(self, os_home: Optional[str] = None) -> str:
        """Save the token to disk for later access.

        Default location is ~/.pybis/hostname.token. After initialisation of an
        Openbis instance, pybis tries to read this saved token by default.

        Args:
            os_home: Override the home directory. Defaults to None.

        Returns:
            str: The path to the saved token file.
        """
        token_path = self.gen_token_path(os_home)
        # create the necessary directories, if they don't exist yet
        os.makedirs(os.path.dirname(token_path), exist_ok=True)
        with open(token_path, "w") as f:
            f.write(self.token)
        # prevent other users to be able to read the token
        os.chmod(token_path, 0o600)
        return token_path

    def _delete_saved_token(self, os_home: Optional[str] = None) -> None:
        """Delete the saved token from disk.

        Default location is ~/.pybis/hostname.token

        Args:
            os_home: Override the home directory. Defaults to None.
        """
        token_path = self.gen_token_path(os_home)
        if os.path.exists(token_path):
            os.unlink(token_path)

    def _get_saved_token(self) -> Optional[SessionToken]:
        """Read the token from the .pybis, on the default user location.

        Returns:
            SessionToken: The saved token, if it exists and is not empty. Otherwise, None.
        """
        token_path = self.gen_token_path()
        if not os.path.exists(token_path):
            return None
        try:
            with open(token_path) as f:
                token = f.read()
                if token == "":
                    return None
                else:
                    return token
        except FileNotFoundError:
            return None

    def _post_request(self, resource: str, request: dict) -> dict:
        """Serialize and send a post request to openBIS.

        Args:
            resource: resource path, e.g. /openbis/openbis/rmi-application-server-v3.json
            request (dict): the request dictionary to be serialized and sent
        Returns:
            dict: the response from openBIS, deserialized
        """
        return self._rpc.post(resource, request)

    def _recover_session(self, full_url, request):
        """Current token seems to be expired, try to use other means to connect."""
        if is_session_token(self.token):
            for session_token in get_saved_tokens():
                pass

        else:
            for token in get_saved_pats(hostname=self.hostname):
                if self.is_token_valid(token=token):
                    return requests.post(
                        full_url, json.dumps(request), verify=self.verify_certificates
                    )

    def _post_request_full_url(
        self, full_url: str, request: dict[str, str]
    ) -> dict[str, Any]:
        """Handle all post requests to openBIS.

        Args:
            full_url: full url including resource path
            request (dict): the request dictionary to be serialized and sent
        Returns:
            dict: the response from openBIS, deserialized
        Raises:
            AuthenticationError: if the session token is missing
            ConnectionError: if the server is unreachable or TLS validation fails
            ServerError: if openBIS reports an error
        """
        return self._rpc.post_full_url(full_url, request)

    def logout(self) -> Optional[dict]:
        """Log out of openBIS.

        After logout, the session token is no longer valid.

        Returns:
            dict: the response from openBIS, deserialized, or None if there was no active session
        """
        if self.token is None:
            return None

        logout_request = {
            "method": "logout",
            "params": [self.token],
        }
        resp = self._post_request(self.as_v3, logout_request)
        self.token = None
        return resp

    def login(
        self,
        username: Optional[str] = None,
        password: Optional[str] = None,
        save_token: bool = False,
    ) -> SessionToken:
        """Logs into OpenBIS with given username and password.

        On success, the session token is stored in the Openbis instance and returned.

        Args:
            username: openBIS username. If not provided, the current username.
            password: openBIS password. If not provided, prompted for securely.
            save_token (bool): If True, the token is saved to disk for later use. Default is False.

        Returns:
            SessionToken: Server-generated session token for authentication on successful login.

        Raises:
            ValueError: If login fails.
        """
        if password is None:
            import getpass

            password = getpass.getpass()

        def is_different_login():
            return username != self._get_username()

        login_request = {
            "method": "login",
            "params": [username, password],
        }
        token = self._post_request(self.as_v3, login_request)
        if token is None or (is_different_login() and token == self.token):
            raise ValueError("login to openBIS failed")
        self.token = token
        if save_token:
            self._save_token_to_disk()
            self._password(password)
            self.username = username
        return self.token

    def _password(
        self, password: Optional[str] = None, pstore: dict = {}
    ) -> Optional[str]:
        """Store or retrieve the password from an internal store.

        Args:
            password: If provided, the password is stored. If None, the password is retrieved.
            pstore (dict): Internal password store.

        Returns:
            str: The stored password, if retrieving.

        Raises:
            Exception: If trying to retrieve the password from an unauthorized method.
        """
        import inspect

        allowed_methods = ["mount"]

        if password is not None:
            pstore["password"] = password
        else:
            if inspect.stack()[1][3] in allowed_methods:
                return pstore.get("password")
            else:
                raise Exception(
                    f"This method can only be called from these internal methods: {allowed_methods}"
                )

    def unmount(self, mountpoint: Optional[str] = None) -> None:
        """Unmounts the openBIS dataStore from the given mountpoint.

        If the unmount fails, the process is killed and unmount is retried.

        Args:
            mountpoint: The mountpoint to unmount. If None, the mountpoint
                stored in the Openbis instance is used.

        Raises:
            ValueError: If no mountpoint is provided and none is stored in the instance.
            OSError: If unmounting fails.
        """
        if mountpoint is None and not getattr(self, "mountpoint", None):
            raise ValueError("please provide a mountpoint to unmount")

        if mountpoint is None:
            mountpoint = self.mountpoint

        full_mountpoint_path = os.path.abspath(os.path.expanduser(mountpoint))

        if not os.path.exists(full_mountpoint_path):
            return

        # mountpoint is not a mountpoint path
        if not os.path.ismount(full_mountpoint_path):
            return

        status = subprocess.call(f"umount {full_mountpoint_path}", shell=True)
        if status == 1:
            status = subprocess.call(
                f'pkill -9 sshfs && umount "{full_mountpoint_path}"', shell=True
            )

        if status == 1:
            raise OSError(
                f"could not unmount mountpoint: {full_mountpoint_path} Please try to unmount manually"
            )
        else:
            if VERBOSE:
                print(f"Successfully unmounted {full_mountpoint_path}")
            self.mountpoint = None

    def is_mounted(self, mountpoint: Optional[str] = None) -> bool:
        """Check whether OpenBIS DataStore is mounted or not.

        Args:
            mountpoint: The mountpoint to check. If None, the mountpoint
                                        stored in the Openbis instance is used.

        Returns:
            bool: True if mounted, False otherwise.
        """
        if mountpoint is None:
            mountpoint = getattr(self, "mountpoint", None)

        if mountpoint is None:
            return False

        return os.path.ismount(mountpoint)

    def get_mountpoint(self, search_mountpoint: bool = False) -> Optional[str]:
        """Retrieve the active mountpoint path.

        Args:
            search_mountpoint: If True, tries to find an existing mountpoint
                for the given hostname. Default is False.

        Returns:
            str: The path to the active mountpoint, or None if not found or not mounted.

        Raises:
            Exception: If no mountpoint is set and search_mountpoint is False.
        """
        mountpoint = getattr(self, "mountpoint", None)
        if mountpoint:
            if self.is_mounted(mountpoint):
                return mountpoint
            else:
                return None
        else:
            if not search_mountpoint:
                return None

        # try to find out the mountpoint
        p1 = subprocess.Popen(["mount", "-d"], stdout=subprocess.PIPE)
        p2 = subprocess.Popen(
            ["grep", "--fixed-strings", self.hostname],
            stdin=p1.stdout,
            stdout=subprocess.PIPE,
        )
        p1.stdout.close()  # Allow p1 to receive a SIGPIPE if p2 exits.
        output = p2.communicate()[0]
        output = output.decode()
        # output will either be '' (=not mounted) or a string like this:
        # {username}@{hostname}:{path} on {mountpoint} (osxfuse, nodev, nosuid, synchronous, mounted by vermeul)
        try:
            mountpoint = output.split()[2]
            self.mountpoint = mountpoint
            return mountpoint
        except Exception:
            return None

    def mount(
        self,
        username: Optional[str] = None,
        password: Optional[str] = None,
        hostname: Optional[str] = None,
        mountpoint: Optional[str] = None,
        volname: Optional[str] = None,
        path: str = "/",
        port: int = 2222,
        kex_algorithms: str = "+diffie-hellman-group1-sha1",
    ) -> str:
        """Mount the openBIS dataStore using sshfs and fuse.

        Uses the provided system username and password instead of root privileges.
        SSHFS and FUSE have to be installed on your system. If not installed, please follow the instructions below:

        Mac OS X: Follow instruction on https://osxfuse.github.io

        Unix Cent OS 7:
            $ sudo yum install epel-release
            $ sudo yum --enablerepo=epel -y install fuse-sshfs
            $ user="$(whoami)"
            $ usermod -a -G fuse "$user"

        Args:
            username: openBIS username.
                If not provided, the current username is used.
            password: openBIS password.
                If not provided, the user is prompted to enter it.
            hostname: The openBIS hostname.
                If not provided, the hostname from the Openbis instance is used.
            mountpoint: The mountpoint to mount the dataStore.
                If not provided, ~/hostname is used.

        Returns:
            str: The path to the mountpoint.

        Raises:
            ValueError: If required parameters are missing or if the platform is not supported.
            OSError: If mounting fails.
        """
        if self.is_mounted():
            if VERBOSE:
                print(f"openBIS dataStore is already mounted on {self.mountpoint}")
            return

        def check_sshfs_is_installed():
            import errno
            import subprocess

            try:
                subprocess.call("sshfs --help", shell=True)
            except OSError as e:
                if e.errno == errno.ENOENT:
                    raise ValueError(
                        'Your system seems not to have SSHFS installed. For Mac OS X, see installation instructions on https://osxfuse.github.io For Unix: $ sudo yum install epel-release && sudo yum --enablerepo=epel -y install fuse-sshfs && user="$(whoami)" && usermod -a -G fuse "$user"'
                    )

        check_sshfs_is_installed()

        is_pat = self.token is not None and self.token.startswith("$pat")
        if is_pat is True:
            username = "?"
            # PAT start with '$' so an escape character is needed
            password = "\\" + self.token
        else:
            if username is None:
                username = self._get_username()
            if not username:
                raise ValueError("no token available - please provide a username")
            if password is None:
                password = self._password()
            if not password:
                raise ValueError("please provide a password")

        if hostname is None:
            hostname = self.hostname
        if not hostname:
            raise ValueError("please provide a hostname")

        if mountpoint is None:
            mountpoint = os.path.join("~", self.hostname)

        # check if mountpoint exists, otherwise create it
        full_mountpoint_path = os.path.abspath(os.path.expanduser(mountpoint))
        if not os.path.exists(full_mountpoint_path):
            os.makedirs(full_mountpoint_path)

        print("full_mountpoint_path: ", full_mountpoint_path)

        from sys import platform

        supported_platforms = ["darwin", "linux"]
        if platform not in supported_platforms:
            raise ValueError(
                f"This method is not yet supported on {platform} plattform"
            )

        os_options = {
            "darwin": f"-oauto_cache,reconnect,defer_permissions,noappledouble,negative_vncache,volname={hostname} -oStrictHostKeyChecking=no ",
            "linux": "-oauto_cache,reconnect -oStrictHostKeyChecking=no",
        }

        if volname is None:
            volname = hostname

        import subprocess

        args = {
            "username": username,
            "password": password,
            "hostname": hostname,
            "port": port,
            "path": path,
            "mountpoint": mountpoint,
            "volname": volname,
            "os_options": os_options[platform],
            "kex_algorithms": kex_algorithms,
        }

        cmd = (
            'echo "{password}" | sshfs'
            " {username}@{hostname}:{path} {mountpoint}"
            ' -o port={port} -o ssh_command="ssh -oKexAlgorithms={kex_algorithms}" -o password_stdin'
            " {os_options}".format(**args)
        )

        status = subprocess.call(cmd, shell=True)

        if status == 0:
            if VERBOSE:
                print(f"Mounted successfully to {full_mountpoint_path}")
            self.mountpoint = full_mountpoint_path
            return self.mountpoint
        else:
            raise OSError("mount failed, exit status: ", status)

    def get_server_information(self) -> "ServerInformation":
        """Retrieve general information about the openBIS server.

        Following attributes are available:
            api-version, archiving-configured, authentication-service, enabled-technologies, project-samples-enabled

        Returns:
            ServerInformation: An object containing the server information.

        Raises:
            ValueError: If the server information could not be retrieved.
        """
        if self.server_information is not None:
            return self.server_information

        request = {
            "method": "getServerInformation",
            "params": [self.token],
        }
        resp = self._post_request(self.as_v3, request)
        if resp is not None:
            self.server_information = ServerInformation(resp)
            return self.server_information
        else:
            raise ValueError("Could not get the server information")

    def create_permId(self) -> PermId:
        """Create a new permId on server side.

        Returns:
            PermId: The created permId.

        Raises:
            ValueError: If the permId could not be created.
        """
        # Request just 1 permId
        request = {
            "method": "createPermIdStrings",
            "params": [self.token, 1],
        }
        resp = self._post_request(self.as_v3, request)
        if resp is not None:
            return resp[0]
        else:
            raise ValueError("Could not create permId")

    def get_datastores(self) -> DataFrame:
        """Get available datastores.

        Usually there is only one datastore, but in some cases there might be multiple servers.
        If you upload a file, you need to specify the datastore you want the file uploaded to.

        Returns:
            DataFrame: A DataFrame containing the available datastores with
                columns 'code', 'downloadUrl', and 'remoteUrl'.

        Raises:
            ValueError: If no datastore is found.
        """
        if hasattr(self, "datastores"):
            return self.datastores  # pylint: disable=E0203

        request = {
            "method": "searchDataStores",
            "params": [
                self.token,
                {"@type": "as.dto.datastore.search.DataStoreSearchCriteria"},
                {"@type": "as.dto.datastore.fetchoptions.DataStoreFetchOptions"},
            ],
        }
        resp = self._post_request(self.as_v3, request)
        attrs = ["code", "downloadUrl", "remoteUrl"]
        if len(resp["objects"]) == 0:
            raise ValueError("No datastore found!")
        else:
            objects = resp["objects"]
            parse_jackson(objects)
            datastores = DataFrame(objects)
            self.datastores = datastores[attrs]
            return datastores[attrs]

    def gen_codes(
        self, entity: EntityKindCode, prefix: str = "", count: int = 1
    ) -> list[str]:
        """Create multiple codes for the given entity type.

        Get the next prefix + sequence numbers for a the given entity type.

        Args:
            entity: The entity type for which to generate codes.
                Old naming is still supported, e.g., SAMPLE, EXPERIMENT, MATERIAL.
            prefix: The prefix to use for the generated codes.
            count: The number of codes to generate.

        Returns:
            List[str]: A list of generated codes.

        Raises:
            ValueError: If the entity type is not supported or if code generation fails.

        Examples:
            >>> gen_code("OBJECT", "OBJ-")
            ['OBJ-0001']
            >>> gen_code("COLLECTION", "COL-", 3)
            ['COL-0001', 'COL-0002', 'COL-0003']
            >>> gen_code("DATASET", "")
            ['0001']
        """
        entity = entity.upper()

        entity2enum = {
            "DATASET": "DATA_SET",  # Inconsistency in openBIS API
            "OBJECT": "SAMPLE",
            "SAMPLE": "SAMPLE",  # Old naming
            "EXPERIMENT": "EXPERIMENT",  # Old naming
            "COLLECTION": "EXPERIMENT",
            "MATERIAL": "MATERIAL",  # Deprecated
        }

        if entity not in entity2enum:
            raise ValueError(
                "No such entity: {}. Allowed entities are: DATA_SET, SAMPLE, EXPERIMENT, MATERIAL"
            )

        request = {
            "method": "createCodes",
            "params": [self.token, prefix, entity2enum[entity], count],
        }
        try:
            return self._post_request(self.as_v3, request)
        except Exception as e:
            raise ValueError(f"Could not generate a code(s) for {entity}: {e}")

    def gen_code(self, entity: EntityKindCode, prefix: str = "") -> str:
        """Create a code for the given entity type.

        Get the next sequence number for a the given entity type.

        Args:
            entity: The entity type for which to generate a code.
                Old naming is still supported, e.g., SAMPLE, EXPERIMENT, MATERIAL.
            prefix: The prefix to use for the generated code.

        Returns:
            str: The generated code.

        Raises:
            ValueError: If the entity type is not supported or if code generation fails.

        Examples:
            >>> gen_code("OBJECT", "OBJ-")
            'OBJ-0001'
            >>> gen_code("DATASET", "")
            '0001'
        """
        return self.gen_codes(entity=entity, prefix=prefix)[0]

    def gen_permId(self, count: int = 1) -> list[PermId]:
        """Create a list of new PermIds on server side and return them.

        Args:
            count: The number of permIds to generate.

        Returns:
            list[PermId]: A list of newly created permIds.

        Raises:
            ValueError: If the permIds could not be created.

        Examples:
            >>> gen_permId(2)
            ['20251213184712345-89', '20251213184712346-90', '20251213184712347-91']
        """
        request = {"method": "createPermIdStrings", "params": [self.token, count]}
        try:
            return self._post_request(self.as_v3, request)
        except Exception as exc:
            raise ValueError(f"Could not generate a code: {exc}")

    def assign_role(
        self,
        role: AuthorizationRoles,
        **args: Literal["person", "group", "space", "project"],
    ) -> None:
        """general method to assign a role to either
            - a person
            - a group
        The scope is either
            - the whole instance
            - a space
            - a project
        """
        role = role.upper()
        defs = get_definition_for_entity("roleAssignment")
        if role not in defs["role"]:
            raise ValueError(f"Role should be one of these: {defs['role']}")
        userId = None
        groupId = None
        spaceId = None
        projectId = None

        for arg in args:
            if arg in ["person", "group", "space", "project"]:
                permId = args[arg] if isinstance(args[arg], str) else args[arg].permId
                if arg == "person":
                    userId = {
                        "permId": permId,
                        "@type": "as.dto.person.id.PersonPermId",
                    }
                elif arg == "group":
                    groupId = {
                        "permId": permId,
                        "@type": "as.dto.authorizationgroup.id.AuthorizationGroupPermId",
                    }
                elif arg == "space":
                    spaceId = {"permId": permId, "@type": "as.dto.space.id.SpacePermId"}
                elif arg == "project":
                    projectId = {
                        "permId": permId,
                        "@type": "as.dto.project.id.ProjectPermId",
                    }

        request = {
            "method": "createRoleAssignments",
            "params": [
                self.token,
                [
                    {
                        "role": role,
                        "userId": userId,
                        "authorizationGroupId": groupId,
                        "spaceId": spaceId,
                        "projectId": projectId,
                        "@type": "as.dto.roleassignment.create.RoleAssignmentCreation",
                    }
                ],
            ],
        }
        self._post_request(self.as_v3, request)
        return

    def _get_fetchopts_for_attrs(self, attrs=None):
        if attrs is None:
            return []

        fetchopts = []
        for attr in attrs:
            if attr.startswith("space"):
                fetchopts.append("space")
            if attr.startswith("project"):
                fetchopts.append("project")
            if attr.startswith("experiment"):
                fetchopts.append("experiment")
            if attr.startswith("sample"):
                fetchopts.append("sample")
            if attr.startswith("registrator"):
                fetchopts.append("registrator")
            if attr.startswith("modifier"):
                fetchopts.append("modifier")

        return fetchopts

    def execute_custom_dss_service(self, code, parameters):
        """Executes a custom Datastore service with the provided service id. Additional execution options can be set via parameters.
        code: serviceId of the custom Datastore service
        parameters: parameters to be sent to the custom service
        """
        serviceId = {"@type": "dss.dto.service.id.CustomDssServiceCode", "permId": code}
        options = {
            "@type": "dss.dto.service.CustomDSSServiceExecutionOptions",
            "parameters": parameters,
        }
        request = {
            "method": "executeCustomDSSService",
            "params": [self.token, serviceId, options],
        }
        return self._post_request_full_url(
            urljoin(self._get_dss_url(), self.dss_v3), request
        )

    def execute_custom_as_service(self, code, parameters):
        """Executes a custom Application Server service with the provided service id. Additional execution options can be set via parameters.
        code: serviceId of the custom Application Server service
        parameters: parameters to be sent to the custom service
        """
        serviceId = {"@type": "as.dto.service.id.CustomASServiceCode", "permId": code}
        options = {
            "@type": "as.dto.service.CustomASServiceExecutionOptions",
            "parameters": parameters,
        }
        request = {
            "method": "executeCustomASService",
            "params": [self.token, serviceId, options],
        }
        resp = self._post_request(self.as_v3, request)
        return resp

    def delete_entity(self, entity, id, reason, id_name="permId"):
        """Deletes Spaces, Projects, Experiments, Samples and DataSets"""

        type = get_type_for_entity(entity, "delete")
        method = get_method_for_entity(entity, "delete")
        request = {
            "method": method,
            "params": [
                self.token,
                [{id_name: id, "@type": type}],
                {"reason": reason, "@type": type},
            ],
        }
        self._post_request(self.as_v3, request)

    def delete_openbis_entity(self, entity, objectId, reason="No reason given"):
        method = get_method_for_entity(entity, "delete")
        delete_options = get_type_for_entity(entity, "delete")
        delete_options["reason"] = reason

        request = {"method": method, "params": [self.token, [objectId], delete_options]}
        return self._post_request(self.as_v3, request)

    def confirm_deletions(self, deletion_ids):
        """Confirms performed deletions"""
        deletions = [
            (
                x
                if "@type" in x
                else {"@type": "as.dto.deletion.id.DeletionTechId", "id": x}
            )
            for x in deletion_ids
        ]
        request = {
            "method": "confirmDeletions",
            "params": [
                self.token,
                deletions,
            ],
        }
        self._post_request(self.as_v3, request)

    def revert_deletions(self, deletion_ids):
        """Confirms performed deletions"""
        request = {
            "method": "revertDeletions",
            "params": [
                self.token,
                [
                    {"@type": "as.dto.deletion.id.DeletionTechId", "id": x}
                    for x in deletion_ids
                ],
            ],
        }
        self._post_request(self.as_v3, request)

    def _gen_fetchoptions(self, options, foType):
        fo = {"@type": foType}
        for option in options:
            fo[option] = get_fetchoption_for_entity(option)
        return fo

    def _create_get_request(self, method_name, entity, permids, options, foType):

        if not isinstance(permids, list):
            permids = [permids]

        type = f"as.dto.{entity.lower()}.id.{entity.capitalize()}"
        search_params = []
        for permid in permids:
            # decide if we got a permId or an identifier
            match = re.match("/", permid)
            if match:
                search_params.append(
                    {"identifier": permid, "@type": type + "Identifier"}
                )
            else:
                search_params.append({"permId": permid, "@type": type + "PermId"})

        fo = {"@type": foType}
        for option in options:
            fo[option] = get_fetchoption_for_entity(option)

        request = {
            "method": method_name,
            "params": [self.token, search_params, fo],
        }
        return request

    def clear_cache(self, entity=None):
        """Empty the internal object cache
        If you do not specify any entity, the complete cache is cleared.
        As entity, you can specify either:
        space, project, vocabulary, term, sampleType, experimentType, dataSetType
        """
        if entity:
            self.cache[entity] = {}
            if entity == "vocabulary":
                # term lists are cached per vocabulary; a vocabulary change
                # (e.g. added terms) invalidates them too
                self.cache["term"] = {}
        else:
            self.cache = {}

    def _object_cache(self, entity=None, code=None, value=None):
        if not self.use_cache:
            return None

        # return the value, if no value provided
        if value is None:
            if entity in self.cache:
                return self.cache[entity].get(code)
        else:
            if entity not in self.cache:
                self.cache[entity] = {}

            self.cache[entity][code] = value

    def _tag_list_for_response(self, response, totalCount=0):
        def create_data_frame(attrs, props, response):
            parse_jackson(response)
            attrs = [
                "permId",
                "code",
                "description",
                "owner",
                "private",
                "registrationDate",
            ]
            if len(response) == 0:
                tags = DataFrame(columns=attrs)
            else:
                tags = DataFrame(response)
                tags["registrationDate"] = tags["registrationDate"].map(
                    format_timestamp
                )
                tags["permId"] = tags["permId"].map(extract_permid)
                tags["description"] = tags["description"].map(
                    lambda x: "" if x is None else x
                )
                tags["owner"] = tags["owner"].map(extract_person)
            return tags[tags.columns.intersection(attrs)]

        return Things(
            openbis_obj=self,
            entity="tag",
            identifier_name="permId",
            totalCount=totalCount,
            response=response,
            df_initializer=create_data_frame,
        )

    def new_spreadsheet(self, columns=10, rows=10):
        """Creates a new instance of Spreadsheet that can be used in the property with the spreadsheet widget"""
        return Spreadsheet(columns, rows)

    def _get_types_of(
        self,
        method_name,
        entity,
        type_name=None,
        start_with=None,
        count=None,
        additional_attributes=None,
        optional_attributes=None,
    ):
        """Returns a list of all available types of an entity.
        If the name of the entity-type is given, it returns a PropertyAssignments object
        """
        if additional_attributes is None:
            additional_attributes = []

        if optional_attributes is None:
            optional_attributes = []

        search_request = {
            "@type": f"as.dto.{entity.lower()}.search.{entity}TypeSearchCriteria"
        }
        fetch_options = {
            "@type": f"as.dto.{entity.lower()}.fetchoptions.{entity}TypeFetchOptions"
        }
        fetch_options["from"] = start_with
        fetch_options["count"] = count

        if type_name is not None:
            search_request = _gen_search_criteria(
                {entity.lower(): entity + "Type", "operator": "AND", "code": type_name}
            )
            fetch_options["propertyAssignments"] = get_fetchoption_for_entity(
                "propertyAssignments"
            )
            if self.get_server_information().is_version_greater_than(3, 3):
                fetch_options["validationPlugin"] = get_fetchoption_for_entity("plugin")

        request = {
            "method": method_name,
            "params": [self.token, search_request, fetch_options],
        }
        resp = self._post_request(self.as_v3, request)

        def create_data_frame(attrs, props, response):
            parse_jackson(response)

            if type_name is not None:
                if len(response["objects"]) == 1:
                    return EntityType(openbis_obj=self, data=response["objects"][0])
                elif len(response["objects"]) == 0:
                    raise ValueError(f"No such {entity} type: {type_name}")
                else:
                    raise ValueError(
                        f"There is more than one entry for entity={entity} and type={type_name}"
                    )

            types = []
            attrs = self._get_attributes(
                type_name, types, additional_attributes, optional_attributes
            )
            objects = response["objects"]
            if len(objects) == 0:
                types = DataFrame(columns=attrs)
            else:
                parse_jackson(objects)
                types = DataFrame(objects)
                types["modificationDate"] = types["modificationDate"].map(
                    format_timestamp
                )
            return types[types.columns.intersection(attrs)]

        return Things(
            openbis_obj=self,
            entity=entity.lower() + "_type",
            start_with=start_with,
            count=count,
            totalCount=resp.get("totalCount"),
            response=resp,
            df_initializer=create_data_frame,
        )

    def _get_attributes(
        self, type_name, types, additional_attributes, optional_attributes
    ):
        attributes = ["code", "description"] + additional_attributes
        attributes += [
            attribute for attribute in optional_attributes if attribute in types
        ]
        attributes += ["modificationDate"]
        if type_name is not None:
            attributes += ["propertyAssignments"]
        return attributes

    def is_session_active(self):
        """checks whether a session is still active. Returns true or false."""
        return self.is_token_valid(self.token)

    def is_token_valid(self, token: str = None):
        """Check if the connection to openBIS is valid.
        This method is useful to check if a token is still valid or if it has timed out,
        requiring the user to login again.
        :return: Return True if the token is valid, False if it is not valid.
        """
        if token is None:
            token = self.token

        if token is None:
            return False

        request = {
            "method": "isSessionActive",
            "params": [token],
        }
        resp = self._post_request(self.as_v3, request)
        return resp

    def get_session_info(self, token=None):
        """Returns detailed infromation regarding current session with Openbis instance"""
        if token is None:
            token = self.token

        if token is None:
            return None

        request = {"method": "getSessionInformation", "params": [token]}
        try:
            resp = self._post_request(self.as_v3, request)
            parse_jackson(resp)
        except Exception as exc:
            return None
        return SessionInformation(openbis_obj=self, data=resp)

    def set_token(self, token, save_token=False):
        """Checks the validity of a token, sets it as the current token and (by default) saves it
        to the disk, i.e. in the ~/.pybis directory
        """
        if not token:
            # clearing the token (e.g. on logout) must actually clear it
            self.__dict__["token"] = None
            return
        if type(token) is PersonalAccessToken:
            token = token.permId
        if not self.is_token_valid(token):
            raise ValueError("Session is no longer valid. Please log in again.")
        else:
            self.__dict__["token"] = token
        if save_token:
            self._save_token_to_disk()

    def _dataset_list_for_response(
        self,
        response,
        attrs=None,
        props=None,
        start_with=None,
        count=None,
        totalCount=0,
        objects=None,
        parsed=False,
    ):
        """returns a Things object, containing a DataFrame plus some additional information"""

        def extract_attribute(attribute_to_extract):
            def return_attribute(obj):
                if obj is None:
                    return ""
                return obj.get(attribute_to_extract, "")

            return return_attribute

        if not parsed:
            parse_jackson(response)

        if attrs is None:
            attrs = []

        def extract_project(attr):
            entity, _, attr = attr.partition(".")

            def extract_attr(obj):
                try:
                    if attr:
                        return obj["project"][attr]
                    else:
                        return obj["project"]["identifier"]["identifier"]
                except KeyError:
                    return ""

            return extract_attr

        def extract_space(attr):
            entity, _, attr = attr.partition(".")

            def extract_attr(obj):
                try:
                    if attr:
                        return obj["project"]["space"][attr]
                    else:
                        return obj["project"]["space"]["code"]
                except KeyError:
                    return ""

            return extract_attr

        def create_data_frame(attrs, props, response):
            default_attrs = [
                "permId",
                "type",
                "experiment",
                "sample",
                "registrationDate",
                "modificationDate",
                "location",
                "status",
                "presentInArchive",
                "size",
            ]
            display_attrs = default_attrs + attrs

            if props is None:
                props = []
            else:
                if isinstance(props, str):
                    props = [props]

            if len(response) == 0:
                for prop in props:
                    if prop == "*":
                        continue
                    display_attrs.append(prop)
                datasets = DataFrame(columns=display_attrs)
            else:
                datasets = DataFrame(response)
                for attr in attrs:
                    if "project" in attr:
                        datasets[attr] = datasets["experiment"].map(
                            extract_project(attr)
                        )
                    elif "space" in attr:
                        datasets[attr] = datasets["experiment"].map(extract_space(attr))
                    elif "." in attr:
                        entity, attribute_to_extract = attr.split(".")
                        datasets[attr] = datasets[entity].map(
                            extract_attribute(attribute_to_extract)
                        )
                for attr in attrs:
                    # if no dot supplied, just display the code of the space, project or experiment
                    if any(entity == attr for entity in ["experiment", "sample"]):
                        datasets[attr] = datasets[attr].map(extract_nested_identifier)

                datasets["registrationDate"] = datasets["registrationDate"].map(
                    format_timestamp
                )
                datasets["modificationDate"] = datasets["modificationDate"].map(
                    format_timestamp
                )
                datasets["experiment"] = datasets["experiment"].map(
                    extract_nested_identifier
                )
                datasets["sample"] = datasets["sample"].map(extract_nested_identifier)
                datasets["type"] = datasets["type"].map(extract_code)
                datasets["permId"] = datasets["code"]
                for column in ["parents", "children", "components", "containers"]:
                    if column in datasets:
                        datasets[column] = datasets[column].map(extract_identifiers)
                datasets["size"] = datasets["physicalData"].map(
                    lambda x: x.get("size") if x else ""
                )
                datasets["status"] = datasets["physicalData"].map(
                    lambda x: x.get("status") if x else ""
                )
                datasets["presentInArchive"] = datasets["physicalData"].map(
                    lambda x: x.get("presentInArchive") if x else ""
                )
                datasets["location"] = datasets["physicalData"].map(
                    lambda x: x.get("location") if x else ""
                )

                for prop in props:
                    if prop == "*":
                        # include all properties in dataFrame.
                        # expand the dataFrame by adding new columns
                        columns = []
                        for i, dataSet in enumerate(response):
                            for prop_name, val in dataSet.get("properties", {}).items():
                                datasets.loc[i, prop_name.upper()] = val
                                columns.append(prop_name.upper())

                        display_attrs += set(columns)
                        continue

                    else:
                        # property name is provided
                        for i, dataSet in enumerate(response):
                            val = dataSet.get("properties", {}).get(
                                prop, ""
                            ) or dataSet.get("properties", {}).get(prop.upper(), "")
                            datasets.loc[i, prop.upper()] = val
                        display_attrs.append(prop.upper())
            return datasets[datasets.columns.intersection(display_attrs)]

        def create_objects(response):
            return objects

        return Things(
            openbis_obj=self,
            entity="dataset",
            identifier_name="permId",
            start_with=start_with,
            count=count,
            totalCount=totalCount,
            attrs=attrs,
            props=props,
            response=response,
            df_initializer=create_data_frame,
            objects_initializer=create_objects,
        )

    def new_git_data_set(
        self,
        data_set_type,
        path,
        commit_id,
        repository_id,
        dms,
        sample=None,
        experiment=None,
        properties={},
        dss_code=None,
        parents=None,
        data_set_code=None,
        contents=[],
    ):
        """Create a link data set.
        :param data_set_type: The type of the data set
        :param data_set_type: The type of the data set
        :param path: The path to the git repository
        :param commit_id: The git commit id
        :param repository_id: The git repository id - same for copies
        :param dms: An external data managment system object or external_dms_id
        :param sample: A sample object or sample id.
        :param dss_code: Code for the DSS -- defaults to the first dss if none is supplied.
        :param properties: Properties for the data set.
        :param parents: Parents for the data set.
        :param data_set_code: A data set code -- used if provided, otherwise generated on the server
        :param contents: A list of dicts that describe the contents:
            {'file_length': [file length],
             'crc32': [crc32 checksum],
             'directory': [is path a directory?]
             'path': [the relative path string]}
        :return: A DataSet object
        """
        return pbds.GitDataSetCreation(
            self,
            data_set_type,
            path,
            commit_id,
            repository_id,
            dms,
            sample,
            experiment,
            properties,
            dss_code,
            parents,
            data_set_code,
            contents,
        ).new_git_data_set()

    def new_content_copy(self, path, commit_id, repository_id, edms_id, data_set_id):
        """
        Create a content copy in an existing link data set.
        :param path: path of the new content copy
        "param commit_id: commit id of the new content copy
        "param repository_id: repository id of the content copy
        "param edms_id: Id of the external data managment system of the content copy
        "param data_set_id: Id of the data set to which the new content copy belongs
        """
        return pbds.GitDataSetUpdate(self, data_set_id).new_content_copy(
            path, commit_id, repository_id, edms_id
        )

    def search_files(self, dataset_id, dss_code=None):
        return pbds.GitDataSetFileSearch(self, dataset_id).search_files()

    def delete_content_copy(self, dataset_id, content_copy):
        """
        Deletes a content copy from a data set.
        :param data_set_id: Id of the data set containing the content copy
        :param content_copy: The content copy to be deleted
        """
        return pbds.GitDataSetUpdate(self, dataset_id).delete_content_copy(content_copy)

    @staticmethod
    def sample_to_sample_id(sample):
        """Take sample which may be a string or object and return an identifier for it."""
        return Openbis._object_to_object_id(
            sample, "as.dto.sample.id.SampleIdentifier", "as.dto.sample.id.SamplePermId"
        )

    @staticmethod
    def experiment_to_experiment_id(experiment):
        """Take experiment which may be a string or object and return an identifier for it."""
        return Openbis._object_to_object_id(
            experiment,
            "as.dto.experiment.id.ExperimentIdentifier",
            "as.dto.experiment.id.SamplePermId",
        )

    @staticmethod
    def _object_to_object_id(obj, identifierType, permIdType):
        object_id = None
        if isinstance(obj, str):
            if is_identifier(obj):
                object_id = {"identifier": obj, "@type": identifierType}
            else:
                object_id = {"permId": obj, "@type": permIdType}
        else:
            object_id = {"identifier": obj.identifier, "@type": identifierType}
        return object_id

    @staticmethod
    def data_set_to_data_set_id(data_set):
        if isinstance(data_set, str):
            code = data_set
        else:
            code = data_set.permId
        return {"permId": code, "@type": "as.dto.dataset.id.DataSetPermId"}

    def external_data_managment_system_to_dms_id(self, dms):
        if isinstance(dms, str):
            dms_id = {"permId": dms, "@type": "as.dto.externaldms.id.ExternalDmsPermId"}
        else:
            dms_id = {
                "identifier": dms.code,
                "@type": "as.dto.sample.id.SampleIdentifier",
            }
        return dms_id

    def new_vocabulary(
        self, code, terms, managedInternally=False, chosenFromList=True, **kwargs
    ):
        """Creates a new vocabulary
        Usage::
            new_vocabulary(
                code="vocabulary_code",
                description="",
                terms=[
                    {"code": "term1", "label": "label1", "description": "description1"},
                    {"code": "term2", "label": "label2", "description": "description2"},
                ],
            )
        """
        kwargs["code"] = code
        kwargs["managedInternally"] = managedInternally
        kwargs["chosenFromList"] = chosenFromList
        return Vocabulary(self, data=None, terms=terms, **kwargs)

    def _get_dss_url(self, dss_code=None):
        """internal method to get the downloadURL of a datastore."""
        dss = self.get_datastores()
        if dss_code is None:
            return dss["downloadUrl"][0]
        else:
            return dss[dss["code"] == dss_code]["downloadUrl"][0]

    def new_type_group(self, name, **kwargs):
        return TypeGroup(self, code=name, **kwargs)

    def delete_type_group(self, id):
        attrs = {"@type": "as.dto.typegroup.id.TypeGroupId", "permId": id}

        del_options = {
            "@type": "as.dto.typegroup.delete.TypeGroupDeletionOptions",
            "reason": "pybis deletion",
        }

        request = {
            "method": "deleteTypeGroups",
            "params": [self.token, [attrs], del_options],
        }

        resp = self._post_request(self.as_v3, request)
        if resp is not None:
            print(resp)
            return resp

    def get_type_group(self, type_group_id, only_data=False):
        ids = {"@type": "as.dto.typegroup.id.TypeGroupId", "permId": type_group_id}

        fetch_options = {
            "@type": "as.dto.typegroup.fetchoptions.TypeGroupFetchOptions",
            "registrator": {"@type": "as.dto.person.fetchoptions.PersonFetchOptions"},
            "modifier": {"@type": "as.dto.person.fetchoptions.PersonFetchOptions"},
            "typeGroupAssignments": {
                "@type": "as.dto.typegroup.fetchoptions.TypeGroupAssignmentFetchOptions",
                "typeGroup": {
                    "@type": "as.dto.typegroup.fetchoptions.TypeGroupFetchOptions",
                    "registrator": {
                        "@type": "as.dto.person.fetchoptions.PersonFetchOptions"
                    },
                },
                "sampleType": {
                    "@type": "as.dto.sample.fetchoptions.SampleTypeFetchOptions",
                    "registrator": {
                        "@type": "as.dto.person.fetchoptions.PersonFetchOptions"
                    },
                },
                "registrator": {
                    "@type": "as.dto.person.fetchoptions.PersonFetchOptions"
                },
            },
            # 'sort': {
            #     '@type' : 'as.dto.typegroup.fetchoptions.TypeGroupSortOptions',
            #
            # }
        }

        request = {
            "method": "getTypeGroups",
            "params": [self.token, [ids], fetch_options],
        }

        resp = self._post_request(self.as_v3, request)

        if len(resp) == 0:
            raise ValueError("No type group found!")

        for id in resp:
            group = resp[id]
            parse_jackson(group)

            if only_data:
                return group
            else:
                return TypeGroup(self, data=group)

    def search_type_group(self, name):

        criteria = {
            "@type": "as.dto.typegroup.search.TypeGroupSearchCriteria",
            "criteria": [
                {
                    "@type": "as.dto.typegroup.search.TypeGroupCodeSearchCriteria",
                    "fieldName": "code",
                    "fieldType": "ATTRIBUTE",
                    "fieldValue": {
                        "value": name,
                        "@type": "as.dto.common.search.StringStartsWithValue",
                    },
                }
            ],
        }

        fetch_options = {
            "@type": "as.dto.typegroup.fetchoptions.TypeGroupFetchOptions",
            "registrator": {"@type": "as.dto.person.fetchoptions.PersonFetchOptions"},
            "modifier": {"@type": "as.dto.person.fetchoptions.PersonFetchOptions"},
            # 'sort': {
            #     '@type' : 'as.dto.typegroup.fetchoptions.TypeGroupSortOptions',
            #
            # }
        }

        request = {
            "method": "searchTypeGroups",
            "params": [self.token, criteria, fetch_options],
        }

        resp = self._post_request(self.as_v3, request)
        if resp is not None:
            print(resp)
            return resp

    def new_type_group_assignment(self, type_group_name, sample_type_code):
        """ """
        attrs = {
            "@type": "as.dto.typegroup.create.TypeGroupAssignmentCreation",
            "sampleTypeId": {
                "@type": "as.dto.entitytype.id.EntityTypePermId",
                "entityKind": "SAMPLE",
                "permId": sample_type_code,
            },
            "typeGroupId": {
                "@type": "as.dto.typegroup.id.TypeGroupId",
                "permId": type_group_name,
            },
            "managedInternally": False,
        }

        request = {
            "method": "createTypeGroupAssignments",
            "params": [self.token, [attrs]],
        }

        resp = self._post_request(self.as_v3, request)
        if resp is not None:
            print(resp)
            return resp
        else:
            raise ValueError("Could not get the server information")

    def delete_type_group_assignment(self, type_group, sample_type):
        """ """
        attrs = {
            "@type": "as.dto.typegroup.id.TypeGroupAssignmentId",
            "sampleTypeId": {
                "@type": "as.dto.entitytype.id.EntityTypePermId",
                "entityKind": "SAMPLE",
                "permId": sample_type,
            },
            "typeGroupId": {
                "@type": "as.dto.typegroup.id.TypeGroupId",
                "permId": type_group,
            },
        }

        del_options = {
            "@type": "as.dto.typegroup.delete.TypeGroupAssignmentDeletionOptions",
            "reason": "test",
        }

        request = {
            "method": "deleteTypeGroupAssignments",
            "params": [self.token, [attrs], del_options],
        }

        resp = self._post_request(self.as_v3, request)
        if resp is not None:
            print(resp)
            return resp

    def get_type_group_assignment(self, type_group, sample_type):
        """ """
        attrs = {
            "@type": "as.dto.typegroup.id.TypeGroupAssignmentId",
            "sampleTypeId": {
                "@type": "as.dto.entitytype.id.EntityTypePermId",
                "entityKind": "SAMPLE",
                "permId": sample_type,
            },
            "typeGroupId": {
                "@type": "as.dto.typegroup.id.TypeGroupId",
                "permId": type_group,
            },
        }

        fetch_options = {
            "@type": "as.dto.typegroup.fetchoptions.TypeGroupAssignmentFetchOptions",
            "registrator": {"@type": "as.dto.person.fetchoptions.PersonFetchOptions"},
            "typeGroup": {
                "@type": "as.dto.typegroup.fetchoptions.TypeGroupFetchOptions"
            },
            "sampleType": {
                "@type": "as.dto.sample.fetchoptions.SampleTypeFetchOptions",
                "propertyAssignments": {
                    "@type": "as.dto.property.fetchoptions.PropertyAssignmentFetchOptions"
                },
            },
        }

        request = {
            "method": "getTypeGroupAssignments",
            "params": [self.token, [attrs], fetch_options],
        }

        resp = self._post_request(self.as_v3, request)
        if resp is not None:
            print(resp)
            return resp

    def search_type_group_assignment(self, type_group, sample_type):

        criteria = {
            "@type": "as.dto.typegroup.search.TypeGroupAssignmentSearchCriteria",
            "criteria": [
                {
                    "@type": "as.dto.typegroup.search.TypeGroupSearchCriteria",
                    "criteria": [
                        {
                            "@type": "as.dto.common.search.CodeSearchCriteria",
                            "fieldName": "code",
                            "fieldType": "ATTRIBUTE",
                            "fieldValue": {
                                "value": type_group,
                                "@type": "as.dto.common.search.StringStartsWithValue",
                            },
                        }
                    ],
                },
                {
                    "@type": "as.dto.sample.search.SampleTypeSearchCriteria",
                    "criteria": [
                        {
                            "@type": "as.dto.common.search.CodeSearchCriteria",
                            "fieldName": "code",
                            "fieldType": "ATTRIBUTE",
                            "fieldValue": {
                                "value": sample_type,
                                "@type": "as.dto.common.search.StringStartsWithValue",
                            },
                        }
                    ],
                },
            ],
        }

        fetch_options = {
            "@type": "as.dto.typegroup.fetchoptions.TypeGroupAssignmentFetchOptions",
            "registrator": {"@type": "as.dto.person.fetchoptions.PersonFetchOptions"},
            "typeGroup": {
                "@type": "as.dto.typegroup.fetchoptions.TypeGroupFetchOptions"
            },
            "sampleType": {
                "@type": "as.dto.sample.fetchoptions.SampleTypeFetchOptions",
                "propertyAssignments": {
                    "@type": "as.dto.property.fetchoptions.PropertyAssignmentFetchOptions"
                },
            },
        }

        request = {
            "method": "searchTypeGroupAssignments",
            "params": [self.token, criteria, fetch_options],
        }

        resp = self._post_request(self.as_v3, request)
        if resp is not None:
            print(resp)
            return resp


class SessionInformation(
    OpenBisObject,
    entity="sessionInformation",
):
    pass


class ImagingControl:
    DEFAULT_SERVICE_NAME = "imaging"
    IMAGING_CONFIG_PROP_NAME = "IMAGING_DATA_CONFIG".lower()
    DEFAULT_DATASET_VIEW_PROP_NAME = "default_dataset_view"
    DEFAULT_OBJECT_VIEW_PROP_NAME = "default_object_view"
    IMAGING_DATASET_VIEWER = "IMAGING_DATASET_VIEWER"

    def __init__(
        self,
        openbis_instance,
        service_name=DEFAULT_SERVICE_NAME,
        service_type="AS",
        afs_url=None,
    ):
        self._openbis = openbis_instance
        self._service_name = service_name
        self._service_type = service_type
        self.afs_client = None
        if afs_url is not None:
            self.afs_client = AfsClient(
                afs_url, openbis_instance.token, openbis_instance.verify_certificates
            )

    def _execute_service(self, parameters):
        if self._service_type == "AS":
            return self._openbis.execute_custom_as_service(
                self._service_name, parameters
            )
        else:
            return self._openbis.execute_custom_dss_service(
                self._service_name, parameters
            )

    def make_preview(
        self, perm_id: str, index: int, preview: ImagingDataSetPreview
    ) -> ImagingDataSetPreview:
        """Execute preview generation of preview of imaging dataset with the config parameters"""
        preview_params = preview.__dict__
        filter_config = (
            []
            if preview_params["filterConfig"] is None
            else preview_params["filterConfig"]
        )
        filter_params = [f.__dict__ for f in filter_config]
        preview_params["filterConfig"] = filter_params
        parameters = {
            "type": "preview",
            "permId": perm_id,
            "index": index,
            "error": None,
            "preview": preview_params,
        }
        service_response = self._execute_service(parameters)
        if service_response["error"] is None:
            if "@id" in service_response:
                del service_response["@id"]
            if "@id" in service_response["preview"]:
                del service_response["preview"]["@id"]
            for config in service_response["preview"]["filterConfig"]:
                if "@id" in config:
                    del config["@id"]
            preview.__dict__ = service_response["preview"]
            return preview
        else:
            raise ValueError(service_response["error"])

    def _get_export_url(
        self, perm_id: str, export: ImagingDataSetExport, image_index: int = 0
    ) -> str:
        export_params = export.__dict__
        export_params["config"] = export_params["config"].__dict__
        parameters = {
            "type": "export",
            "permId": perm_id,
            "index": image_index,
            "error": None,
            "url": None,
            "export": export_params,
        }
        service_response = self._execute_service(parameters)
        if service_response["error"] is None:
            return service_response["url"]
        else:
            raise ValueError(service_response["error"])

    def _get_multi_export_url(self, exports: list) -> str:
        export_params = [export.__dict__ for export in exports]
        for param in export_params:
            param["config"] = param["config"].__dict__
        parameters = {
            "type": "multi-export",
            "error": None,
            "url": None,
            "exports": [export.__dict__ for export in exports],
        }
        service_response = self._execute_service(parameters)
        if service_response["error"] is None:
            return service_response["url"]
        else:
            raise ValueError(service_response["error"])

    def export_image(
        self,
        perm_id: str,
        image_id: int,
        path_to_download: str,
        include=None,
        image_format="original",
        archive_format="zip",
        resolution="original",
    ):
        """Export particular image  with all its previews of imaging dataset"""
        if include is None:
            include = ["IMAGE", "RAW_DATA"]
        else:
            include = [x.upper() for x in include]

        export_config = ImagingDataSetExportConfig(
            archive_format, image_format, resolution, include
        )
        self._export_image(perm_id, image_id, path_to_download, export_config)

    def _export_image(
        self,
        perm_id: str,
        image_id: int,
        path_to_download: str,
        export_config: ImagingDataSetExportConfig,
    ):

        imaging_export = ImagingDataSetExport(export_config)
        self._single_export_download(
            perm_id, imaging_export, image_id, path_to_download
        )

    def export_previews(
        self,
        perm_ids,
        image_ids,
        preview_ids,
        path_to_download,
        include=None,
        image_format="original",
        archive_format="zip",
        resolution="original",
    ):
        """Export multiple previews of imaging datasets"""
        if include is None:
            include = ["IMAGE", "RAW_DATA"]

        export_config = ImagingDataSetExportConfig(
            archive_format, image_format, resolution, include
        )

        imaging_multi_exports = []
        for i in range(len(perm_ids)):
            imaging_multi_exports += [
                ImagingDataSetMultiExport(
                    perm_ids[i], image_ids[i], preview_ids[i], export_config
                )
            ]
        self._multi_export_download(imaging_multi_exports, path_to_download)

    def _single_export_download(
        self,
        perm_id: str,
        export: ImagingDataSetExport,
        image_index: int = 0,
        directory_path="",
    ):
        export_url = self._get_export_url(perm_id, export, image_index)
        self._download(export_url, directory_path)

    def _multi_export_download(self, exports: list, directory_path=""):
        export_url = self._get_multi_export_url(exports)
        self._download(export_url, directory_path)

    def _download(self, url, directory_path=""):
        get_response = requests.get(
            url, stream=True, verify=self._openbis.verify_certificates
        )
        file_name = url.split("/")[-1]
        if "%2F" in file_name:
            # Flow for cases where name is more complex
            file_name = file_name.split("%2F")[-1]
        path = os.path.join(directory_path, file_name)
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "wb") as f:
            for chunk in get_response.iter_content(chunk_size=1024):
                if chunk:
                    f.write(chunk)

    def get_property_config(self, perm_id: str) -> ImagingDataSetPropertyConfig:
        """Returns imaging property config of given imaging dataset."""
        sample = self._openbis.get_samples(permId=perm_id)
        if len(sample) > 0:
            entity = sample[0]
        else:
            entity = self._openbis.get_dataset(perm_id)
        imaging_property = json.loads(
            entity.props[ImagingControl.IMAGING_CONFIG_PROP_NAME]
        )
        return ImagingDataSetPropertyConfig.from_dict(imaging_property)

    def update_property_config(
        self, perm_id: str, config: ImagingDataSetPropertyConfig
    ):
        """Update imaging dataset with given imaging property config."""
        sample = self._openbis.get_samples(permId=perm_id)
        if len(sample) > 0:
            entity = sample[0]
        else:
            entity = self._openbis.get_dataset(perm_id)
        entity.props[ImagingControl.IMAGING_CONFIG_PROP_NAME] = config.to_json()
        entity.save()

    def create_imaging_dataset(
        self,
        dataset_type: str,
        config: ImagingDataSetPropertyConfig,
        experiment: str,
        sample: str,
        files: list,
        other_properties=None,
    ):
        """Create new imaging dataset with given files and property config."""
        if other_properties is None:
            other_properties = {}
        assert dataset_type is not None
        assert files is not None and len(files) > 0, (
            "Files parameter must not be empty!"
        )
        assert config is not None
        if self.afs_client is not None and self.afs_client.is_session_valid():
            props = other_properties
            props[ImagingControl.IMAGING_CONFIG_PROP_NAME] = config.to_json()
            props[ImagingControl.DEFAULT_OBJECT_VIEW_PROP_NAME] = (
                ImagingControl.IMAGING_DATASET_VIEWER
            )
            sample = self._openbis.new_sample(
                dataset_type, experiment=experiment, props=props
            )
            sample.save()
            self.afs_client.upload_files(
                sample.permId, "/", files, wait_until_finished=True
            )
            return sample

        else:
            props = other_properties
            props[ImagingControl.IMAGING_CONFIG_PROP_NAME] = config.to_json()
            props[ImagingControl.DEFAULT_DATASET_VIEW_PROP_NAME] = (
                ImagingControl.IMAGING_DATASET_VIEWER
            )
            dataset = self._openbis.new_dataset(
                dataset_type,
                experiment=experiment,
                sample=sample,
                files=files,
                props=props,
            )
            return dataset.save()


from ._compat import install_compat  # noqa: E402

install_compat(Openbis)
