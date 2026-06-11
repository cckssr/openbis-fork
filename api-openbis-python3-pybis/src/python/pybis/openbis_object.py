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
from __future__ import annotations

from collections import defaultdict
from typing import Optional, Union, Any, TYPE_CHECKING

from .attribute import AttrHolder
from .entities.base import EntityBehavior
from .definitions import (
    get_definition_for_entity,
    get_type_for_entity,
    get_method_for_entity,
)
from .openbis_typing import PropertyDataArrayTypes, PermId
from .property import PropertyHolder
from .property_reformatter import PropertyReformatter
from .utils import VERBOSE

if TYPE_CHECKING:
    from .pybis import Openbis
    from .space import Space
    from .project import Project
    from .experiment import Experiment
    from .sample import Sample


class OpenBisObject(EntityBehavior):
    """Base class for all OpenBIS entities, such as Space, Project, Sample, etc.

    Contains the common attributes and methods for all entities,
    such as properties, attributes, save(), delete(), etc.
    """

    def __init_subclass__(
        cls, entity: Optional[str] = None, single_item_method_name: Optional[str] = None
    ):
        """Register entity metadata on subclasses at class definition time.

        Called automatically by Python when a subclass of OpenBisObject is created.
        Stores the openBIS entity type name (e.g. ``"SAMPLE"``, ``"EXPERIMENT"``) and
        the Openbis method name used to fetch a single instance of that entity
        (e.g. ``"get_sample"``) as class-level attributes, so every instance can
        look them up without repeating the values in each subclass ``__init__``.

        Args:
            entity: The openBIS entity type string, e.g. ``"SAMPLE"`` or ``"PROJECT"``.
                Passed as a keyword argument in the class definition:
                ``class Sample(OpenBisObject, entity="SAMPLE", ...)``.
            single_item_method_name: Name of the ``Openbis`` method that retrieves
                one instance of this entity, e.g. ``"get_sample"``.
        """
        cls._entity = entity
        cls._single_item_method_name = single_item_method_name

    def __init__(
        self,
        openbis_obj: Any,
        type: Optional[str] = None,
        data: Optional[dict] = None,
        props: Optional[dict] = None,
        **kwargs,
    ):
        """Initialize an OpenBIS entity object.

        Sets up the core namespaces used throughout the object:

        - ``self.openbis`` — the connected :class:`Openbis` session
        - ``self.type``    — the entity type (e.g. a SampleType or DataSetType object)
        - ``self.p``       — :class:`PropertyHolder`: access/set entity properties \
            via ``obj.p.<code>``
        - ``self.a``       — :class:`AttrHolder`: access/set entity attributes (code, space, …)
        - ``self.formatter`` — :class:`PropertyReformatter`: coerces property values to API format

        Uses ``__dict__`` assignment directly to bypass ``__setattr__`` on the entity.

        Args:
            openbis_obj: Active :class:`Openbis` connection used to resolve types and
                perform API calls.
            type: Entity type object (e.g. ``SampleType``). When provided, the
                ``PropertyHolder`` validates property codes and data types against it.
            data: Raw JSON-like dict returned by the V3 API for an existing entity.
                When given, attributes and properties are populated from it immediately.
            props: Dict of ``{property_code: value}`` to set on creation, applied
                to ``self.p`` after ``data`` is loaded.
            **kwargs: Additional attribute key/value pairs set directly on the object
                (e.g. ``space="MY_SPACE"``).
        """
        self.__dict__["openbis"] = openbis_obj
        self.__dict__["type"] = type
        self.__dict__["p"] = PropertyHolder(openbis_obj, type)
        self.__dict__["a"] = AttrHolder(openbis_obj, self._entity, type)
        self.__dict__["formatter"] = PropertyReformatter(openbis_obj)

        # existing OpenBIS object
        if data is not None:
            self._set_data(data)

        if props is not None:
            for key in props:
                setattr(self.p, key, props[key])

        if kwargs is not None:
            for key in kwargs:
                setattr(self, key, kwargs[key])

    def __dir__(self) -> list[str]:
        """Return the attribute names relevant to the current lifecycle state.

        Drives tab-completion and ``dir()`` output with only the attributes that
        make sense at this point:

        - **New (unsaved) object** — returns ``attrs_new``: the subset of attributes
          accepted on creation (e.g. ``code``, ``type``, ``space``).
        - **Existing object** — returns the union of ``attrs`` (read-only attributes
          such as ``permId``, ``registrator``) and ``attrs_up`` (mutable attributes
          such as ``description``, ``tags``).

        The attribute sets are defined per entity in :func:`get_definition_for_entity`.

        Returns:
            List of attribute name strings for the current entity state.
        """
        defs = get_definition_for_entity(self.entity)
        if self.is_new:
            return defs["attrs_new"]
        else:
            return list(set(defs["attrs"] + defs["attrs_up"]))

    def _set_data(self, data: dict):
        # assign the attribute data to self.a by calling it
        # (invoking the AttrHolder.__call__ function)
        self.a(data)
        self.__dict__["data"] = data

        # put the properties in the self.p namespace (without checking them)
        array_types = PropertyDataArrayTypes.__args__
        if "properties" in data:
            for key, value in data["properties"].items():
                if self.p.type:
                    property_type = self.p._property_names[key.lower()]
                    data_type = property_type["dataType"]
                    if "multiValue" in property_type:
                        if property_type["multiValue"] is True:
                            if type(value) is not list:
                                value = [value]
                            if data_type in array_types:
                                value = [
                                    self.formatter.to_array(data_type, x) for x in value
                                ]
                            else:
                                value = self.formatter.to_array(data_type, value)
                        else:
                            if type(value) is list and data_type not in array_types:
                                raise ValueError(
                                    f"Property type {property_type} is not a multi-value property!"
                                )
                            if data_type in array_types:
                                value = self.formatter.to_array(data_type, value)
                    else:
                        if data_type in array_types:
                            value = self.formatter.to_array(data_type, value)
                    if (
                        data_type == "XML"
                        and "metaData" in property_type
                        and "custom_widget" in property_type["metaData"]
                        and property_type["metaData"]["custom_widget"].upper()
                        == "SPREADSHEET"
                    ):
                        if key.lower() in self.p.__dict__:
                            old_spreadsheet = self.p.__dict__[key.lower()]
                            old_spreadsheet._set_data(
                                self.formatter.to_spreadsheet(value)
                            )
                            value = old_spreadsheet
                        else:
                            value = self.formatter.to_spreadsheet(value)
                self.p.__dict__[key.lower()] = value

        # object is already saved to openBIS, so it is not new anymore
        self.a.__dict__["_is_new"] = False

    @property
    def attrs(self) -> list[str]:
        """List of all attribute names available on this entity.

        e.g., ``code``, ``space``, ``permId``, etc.
        See :func:`__dir__` for the attributes relevant to the current lifecycle state.
        """
        return self.__dict__["a"]

    @property
    def space(self) -> Optional[Space]:
        """Return the Space this entity belongs to, if applicable and available."""
        try:
            return self.openbis.get_space(self._space["permId"])
        except Exception:
            pass

    @property
    def project(self) -> Optional[Project]:
        """Return the Project this entity belongs to, if applicable and available."""
        try:
            return self.openbis.get_project(self._project["identifier"])
        except Exception:
            pass

    @property
    def experiment(self) -> Optional[Experiment]:
        """Return the Experiment this entity belongs to, if applicable and available."""
        try:
            return self.openbis.get_experiment(self._experiment["identifier"])
        except Exception:
            pass

    collection = experiment  # Alias

    @property
    def sample(self) -> Optional[Sample]:
        """Return the Sample this entity belongs to, if applicable and available."""
        try:
            return self.openbis.get_sample(self._sample["identifier"])
        except Exception:
            pass

    object = sample  # Alias

    @property
    def _permId(self) -> Union[dict, str]:
        """Return the permId of this entity, if available."""
        try:
            return self.data["permId"]
        except Exception:
            return ""

    @property
    def permId(self) -> Union[PermId, str]:
        """Return the permId of this entity, if available."""
        try:
            return self.data["permId"]["permId"]
        except Exception:
            try:
                return self.a.__dict__["_permId"]["permId"]
            except Exception:
                return ""

    def __getattr__(self, name: str) -> Any:
        """Delegate attribute access to self.a (AttrHolder) for attributes not found."""
        return getattr(self.__dict__["a"], name)

    def __setattr__(self, name: str, value: Union[str, list, dict]) -> Any:
        """Delegate attribute setting to self.a (AttrHolder) for attributes not found."""
        if name in ["set_properties", "set_tags", "add_tags"]:
            raise ValueError("These are methods which should not be overwritten")
        setattr(self.__dict__["a"], name, value)

    def _repr_html_(self) -> str:
        """Print a human-readable HTML representation of this entity.

        Shows the most important attributes and properties in a nicely formatted table.
        See :class:`AttrHolder` for the attributes and :class:`PropertyHolder` for the properties
        included in the default representation.
        """
        return self.a._repr_html_()

    def __repr__(self) -> str:
        """Print a human-readable HTML representation of this entity for IPython.

        Shows the most important attributes and properties in a nicely formatted table.
        See :class:`AttrHolder` for the attributes and :class:`PropertyHolder` for the properties
        included in the default representation.
        """
        return self.a.__repr__()

    def mark_to_be_deleted(self) -> None:
        """Mark this entity to be deleted in a transaction."""
        self.__dict__["mark_to_be_deleted"] = True

    def unmark_to_be_deleted(self) -> None:
        """Unmark this entity to be deleted in a transaction."""
        self.__dict__["mark_to_be_deleted"] = False

    def is_marked_to_be_deleted(self) -> bool:
        """Check if this entity is marked to be deleted in a transaction."""
        return self.__dict__.get("mark_to_be_deleted", False)

    def delete(self, reason: str, permanently: bool = False) -> None:
        """Delete this entity from openBIS.

        Args:
            reason: Reason for deletion, required by openBIS.
            permanently (bool): If True, the entity will be deleted permanently.
                If False, the entity will be marked as deleted but can be recovered.
        """
        if not self.data:
            return

        deletion_id = self.openbis.delete_openbis_entity(
            entity=self._entity, objectId=self.data["permId"], reason=reason
        )
        # the transparent object cache must never serve deleted entities
        self.openbis.clear_cache(self._entity)
        if VERBOSE:
            print(f"{self._entity} {self.permId} successfully deleted.")

        if permanently:
            self.openbis.confirm_deletions([deletion_id])
            if VERBOSE:
                print(f"{self._entity} {self.permId} successfully deleted permanently.")

    def _get_single_item_method(self) -> Any:
        """Return the Openbis method to fetch a single instance of this entity."""
        single_item_method = None
        if self._single_item_method_name:
            single_item_method = getattr(self.openbis, self._single_item_method_name)
        else:
            # try to guess the method...
            single_item_method = getattr(self.openbis, "get_" + self.entity)

        return single_item_method

    def save(self) -> OpenBisObject:
        """Persist the entity to openBIS, either creating or updating it.

        Behaves differently depending on whether the object is new or already exists:

        - **New object** (``is_new=True``): calls the appropriate V3 API ``create*``
          method, then re-fetches the created entity so that server-assigned fields
          (``permId``, ``registrationDate``, ``registrator``, …) are populated on
          ``self``.
        - **Existing object**: calls the corresponding ``update*`` method using the
          object's ``permId``, then re-fetches to reflect any server-side changes.

        Before sending the request, all mandatory properties are validated — a
        :exc:`ValueError` is raised for any that are ``None`` or empty.

        **Version compatibility:** Fields introduced after openBIS 6.x
        (``multiValue``, ``metaData``, ``pattern``, ``patternType``) are stripped
        from the request payload when connecting to older server versions to avoid
        API errors.

        Returns:
            ``self`` — the same object, updated in-place with the server response.
                Allows chaining: ``obj.save().permId``.

        Raises:
            ValueError: If a mandatory property is unset or empty.
            requests.HTTPError / Exception: Propagated from
                :meth:`Openbis._post_request` on API or network failure.

        Example:
            >>> sample = openbis.new_sample(type="UNKNOWN", space="/MY_SPACE")
            >>> sample.props.name = "My Sample"
            >>> sample.save()  # creates; sample.permId is now set
            >>> sample.props.name = "Renamed"
            >>> sample.save()  # updates
        """
        get_single_item = self._get_single_item_method()
        # check for mandatory properties before saving the object
        props = None
        if self.p:
            for prop_name, prop in self.p._property_names.items():
                if prop["mandatory"]:
                    if (
                        getattr(self.p, prop_name) is None
                        or getattr(self.p, prop_name) == ""
                    ):
                        raise ValueError(
                            f"Property '{prop_name}' is mandatory and must not be None"
                        )

            props = self.formatter.format(self.p._all_props())

        # NEW
        if self.is_new:
            request = self._new_attrs()

            version = self.openbis.get_server_information().openbis_version
            if version is not None:
                if (
                    "SNAPSHOT" not in version
                    and not version.startswith("6")
                    and "UNKNOWN" not in version
                ):
                    if (
                        request["method"] == "createPropertyTypes"
                        and "multiValue" in request["params"][1][0]
                    ):
                        del request["params"][1][0]["multiValue"]
                    if (
                        request["method"]
                        in (
                            "createSampleTypes",
                            "createSamples",
                            "createExperimentTypes",
                            "createExperiments",
                            "createDataSetTypes",
                            "createDataSets",
                        )
                        and "metaData" in request["params"][1][0]
                    ):
                        del request["params"][1][0]["metaData"]
                    if "pattern" in request["params"][1][0]:
                        del request["params"][1][0]["pattern"]
                    if "patternType" in request["params"][1][0]:
                        del request["params"][1][0]["patternType"]

            if props:
                request["params"][1][0]["properties"] = props

            resp = self.openbis._post_request(self.openbis.as_v3, request)

            if VERBOSE:
                print(f"{self.entity} successfully created.")
            new_entity_data = self._refetch_data(get_single_item, resp[0]["permId"])
            self._set_data(new_entity_data)
            return self

        # UPDATE
        else:
            request = self._up_attrs(method_name=None, permId=self._permId)

            version = self.openbis.get_server_information().openbis_version
            if version is not None:
                if (
                    "SNAPSHOT" not in version
                    and not version.startswith("6")
                    and "UNKNOWN" not in version
                ):
                    if (
                        request["method"]
                        in (
                            "updateSampleTypes",
                            "updateSamples",
                            "updateExperimentTypes",
                            "updateExperiments",
                            "updateDataSetTypes",
                            "updateDataSets",
                        )
                        and "metaData" in request["params"][1][0]
                    ):
                        del request["params"][1][0]["metaData"]
                    if "pattern" in request["params"][1][0]:
                        del request["params"][1][0]["pattern"]
                    if "patternType" in request["params"][1][0]:
                        del request["params"][1][0]["patternType"]

            if props:
                request["params"][1][0]["properties"] = props

            resp = self.openbis._post_request(self.openbis.as_v3, request)
            if VERBOSE:
                print(f"{self.entity} successfully updated.")
            new_entity_data = self._refetch_data(get_single_item, self.permId)
            self._set_data(new_entity_data)
            return self

    def _refetch_data(self, get_single_item: Any, perm_id: Any) -> dict:
        """Re-fetch the raw entity data after a save.

        Bridges the two getter generations: legacy getters take
        ``only_data=True`` (and ``use_cache=False``), migrated v2 getters
        return the entity whose ``.data`` carries the raw dict.
        """
        import inspect

        parameters = inspect.signature(get_single_item).parameters
        if "only_data" in parameters:  # legacy getter
            kwargs = {"only_data": True}
            if "use_cache" in parameters and not self.is_new:
                kwargs["use_cache"] = False
            return get_single_item(perm_id, **kwargs)
        # migrated getter: invalidate the transparent cache, then use .data
        self.openbis.clear_cache(self._entity)
        return get_single_item(perm_id).data


class Transaction:
    """Batch multiple entity operations into a minimal set of V3 API calls.

    Groups :class:`OpenBisObject` instances by entity type (``sample``,
    ``dataSet``, …) and operation mode (``create``, ``update``, ``delete``),
    then merges all items of the same type+mode into a single JSON-RPC request
    on :meth:`commit`. This is more efficient than calling
    :meth:`OpenBisObject.save` or :meth:`OpenBisObject.delete` individually
    when operating on many entities at once.

    Attributes:
        entities (dict): Nested dict of
            ``{entity_type: {mode: [OpenBisObject, ...]}}`` built up by
            successive :meth:`add` calls.
        reason: Deletion reason string sent with ``delete`` requests.
            Defaults to ``"no reason"``; set it before calling :meth:`commit`
            when deleting entities.

    Example:
        >>> s1 = openbis.new_sample(type="UNKNOWN", space="/MY_SPACE")
        >>> s2 = openbis.new_sample(type="UNKNOWN", space="/MY_SPACE")
        >>> existing = openbis.get_sample("20210101000000000-1")
        >>> existing.props.description = "updated"
        >>> t = Transaction(s1, s2, existing)
        >>> t.commit()  # two create* calls + one update* call
    """

    def __init__(self, *entities: OpenBisObject):
        """Initialize the transaction, optionally pre-loading entities.

        Args:
            *entities: Zero or more :class:`OpenBisObject` instances to add
                immediately. Equivalent to calling :meth:`add` for each one
                after construction.
        """
        self.entities = {}
        self.reason = "no reason"

        if not entities:
            return

        for entity in entities:
            self.add(entity)

    def add(self, entity_obj: OpenBisObject) -> None:
        """Register an entity for batched processing on the next :meth:`commit`.

        The operation mode is determined automatically from the entity's state:

        - **new** (``is_new=True``) → ``"create"``
        - **marked for deletion** → ``"delete"``
        - **existing, not deleted** → ``"update"``

        Entities are grouped internally as::

            {
                "sample": {"create": [...], "update": [...]},
                "dataSet": {"update": [...]},
            }

        so that all items of the same type and mode can be merged into a single
        API request by :meth:`commit`.

        Args:
            entity_obj: Any :class:`OpenBisObject` subclass instance (Sample,
                Experiment, DataSet, …).

        Raises:
            ValueError: If ``entity_obj`` is a new (unsaved) dataSet, which is
                not yet supported in transactions.
        """
        if entity_obj.entity == "dataSet" and entity_obj.is_new:
            raise ValueError(
                "pyBIS currently does not support transactions for new dataSets yet."
            )

        if not entity_obj.entity in self.entities:
            self.entities[entity_obj.entity] = defaultdict(list)

        mode = "update"
        if entity_obj.is_new:
            mode = "create"
        elif entity_obj.is_marked_to_be_deleted():
            mode = "delete"
        else:
            mode = "update"

        self.entities[entity_obj.entity][mode].append(entity_obj)

    def commit(self) -> None:
        """Send all batched operations to openBIS as merged V3 API requests.

        For each ``(entity_type, mode)`` group collected by :meth:`add`, all
        individual entity payloads are merged into a single JSON-RPC call whose
        ``params[1]`` list contains one entry per entity. This minimises round
        trips compared to saving each entity separately.

        **Processing order per group:**

        1. Validate mandatory properties — raises :exc:`ValueError` early if any
           required property is ``None`` or empty.
        2. Format properties via :class:`PropertyReformatter`.
        3. Build the per-entity request payload (``_new_attrs`` or ``_up_attrs``
           for create/update; a direct delete request for deletes). The
           ``self.reason`` string is attached to delete requests.
        4. Strip fields unsupported by pre-6.x servers (``multiValue``,
           ``metaData``, ``pattern``, ``patternType``) when the connected server
           version does not start with ``"6"`` and is not a ``SNAPSHOT`` or
           ``UNKNOWN`` build.
        5. Merge all payloads into one batch request by appending each entity's
           payload dict into ``batch_request["params"][1]``.
        6. Recursively remove ``@id`` keys injected by the JSON serialiser, which
           would confuse the server when the same sub-object appears in multiple
           entity payloads.
        7. POST the merged request. On success, update each created/updated
           entity in-place: clear ``_is_new`` and store the server-assigned
           ``permId``. The response order is assumed to match the submission order.

        Raises:
            ValueError: If a mandatory property is unset, or if the API call
                fails (re-raised from the underlying request).
        """
        import copy

        version = None
        for entity_type in self.entities:
            for mode in self.entities[entity_type]:
                request_coll = []
                for entity in self.entities[entity_type][mode]:
                    if version is None:
                        version = (
                            entity.openbis.get_server_information().openbis_version
                        )
                    if mode == "delete":
                        delete_options = get_type_for_entity(entity_type, "delete")
                        delete_options["reason"] = self.reason
                        method = get_method_for_entity(entity_type, "delete")
                        request = {
                            "method": method,
                            "params": [
                                entity.openbis.token,
                                [entity.data["permId"]],
                                delete_options,
                            ],
                        }
                        request_coll.append(request)
                        continue
                    props = None
                    if entity.p:
                        for prop_name, prop in entity.p._property_names.items():
                            if prop["mandatory"]:
                                if (
                                    getattr(entity.p, prop_name) is None
                                    or getattr(entity.p, prop_name) == ""
                                ):
                                    raise ValueError(
                                        f"Property '{prop_name}' is mandatory and must not be None"
                                    )

                    props = PropertyReformatter(entity.openbis).format(
                        entity.p._all_props()
                    )

                    if mode == "create":
                        request = entity._new_attrs()
                        if props:
                            request["params"][1][0]["properties"] = props

                    elif mode == "update":
                        request = entity._up_attrs(
                            method_name=None, permId=entity._permId
                        )
                        if props:
                            request["params"][1][0]["properties"] = props

                    else:
                        raise ValueError(f"Unkown mode: {mode}")

                    if version is not None:
                        if (
                            "SNAPSHOT" not in version
                            and not version.startswith("6")
                            and "UNKNOWN" not in version
                        ):
                            if (
                                request["method"] == "createPropertyTypes"
                                and "multiValue" in request["params"][1][0]
                            ):
                                del request["params"][1][0]["multiValue"]
                            if (
                                request["method"]
                                in (
                                    "createSampleTypes",
                                    "createSamples",
                                    "createExperimentTypes",
                                    "createExperiments",
                                    "createDataSetTypes",
                                    "createDataSets",
                                    "updateSampleTypes",
                                    "updateSamples",
                                    "updateExperimentTypes",
                                    "updateExperiments",
                                    "updateDataSetTypes",
                                    "updateDataSets",
                                )
                                and "metaData" in request["params"][1][0]
                            ):
                                del request["params"][1][0]["metaData"]

                    request_coll.append(request)

                if request_coll:
                    # copy the first item of all requests
                    batch_request = copy.deepcopy(request_coll[0])
                    # merge all requests into one
                    for i, request in enumerate(request_coll):
                        if i == 0:
                            continue
                        batch_request["params"][1].append(request["params"][1][0])

                    try:

                        def remove_at_id(request):
                            if request is not None:
                                if type(request) == dict:
                                    for key in list(request.keys()):
                                        if key == "@id":
                                            del request[key]
                                        else:
                                            request[key] = remove_at_id(request[key])
                                    return request
                                elif type(request) == str:
                                    return request
                                elif type(request) == list:
                                    tmp = []
                                    for element in request:
                                        tmp += [remove_at_id(element)]
                                    return tmp
                            return request

                        batch_request = remove_at_id(batch_request)

                        resp = entity.openbis._post_request(
                            entity.openbis.as_v3, batch_request
                        )
                        if VERBOSE:
                            print(f"{i + 1} {entity_type}(s) {mode}d.")

                        # mark every sample as not being new anymore
                        # and add the permId attribute received by the response
                        # we assume the response permIds are the same order as we sent them
                        if resp:
                            for i, resp_item in enumerate(resp):
                                if mode == "delete":
                                    continue
                                self.entities[entity_type][mode][i].a.__dict__[
                                    "_is_new"
                                ] = False
                                self.entities[entity_type][mode][i].a.__dict__[
                                    "_permId"
                                ] = resp_item

                    except ValueError as err:
                        if VERBOSE:
                            print(f"ERROR: {mode} of {i + 1} {entity_type}(s) FAILED")
                        raise ValueError(err)
