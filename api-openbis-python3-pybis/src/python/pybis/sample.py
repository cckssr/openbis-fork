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
"""Sample (Object) entity for openBIS."""

import copy
from typing import Any, Optional, cast
from urllib.parse import quote

from .attribute import AttrHolder
from .openbis_object import OpenBisObject
from .property import PropertyHolder
from .property_reformatter import PropertyReformatter


class Sample(OpenBisObject, entity="sample", single_item_method_name="get_object"):
    """An openBIS sample, also called an *Object* in the ELN-LIMS UI.

    Samples are the primary data-bearing entities in openBIS.  They live in
    a :class:`~pybis.space.Space` (and optionally a
    :class:`~pybis.project.Project` and :class:`~pybis.experiment.Experiment`),
    carry typed **properties**, and can form parent/child hierarchies as well
    as container/component structures.

    Fetch samples via ``openbis.get_sample()`` / ``openbis.get_samples()``
    and create new ones with ``openbis.new_sample()``.

    **Properties** are accessed via the ``props`` (alias ``p``) namespace::

        sample.props.name = "My sample"
        sample.props["my.property"] = 42
        sample.save()

    **Attributes** (space, project, experiment, parents, …) are set directly::

        sample.space = "MY_SPACE"
        sample.experiment = "/MY_SPACE/MY_PROJECT/EXP_001"

    Attributes:
        permId (str): Server-assigned permanent identifier.
        identifier (str): Human-readable path, e.g.
            ``"/SPACE/PROJECT/SAMPLE_CODE"``.
        code (str): Short sample code.
        type (Any): The :class:`~pybis.entity_type.SampleType` of this sample.
        props (PropertyHolder): :class:`~pybis.property.PropertyHolder` for typed properties.
        space (Optional[str]): The space this sample belongs to.
        project (Optional[str]): The project identifier.
        experiment (Optional[:class:`~pybis.experiment.Experiment`]):
            The linked experiment.
        parents (list): Identifiers of parent samples.
        children (list): Identifiers of child samples.
        components (list): Identifiers of component samples.
        container (Optional[:class:`Sample`]): The container sample.
        tags (list): Tag codes attached to this sample.

    Example:
        >>> sample = openbis.new_sample(
        ...     type="EXPERIMENTAL_STEP",
        ...     space="MY_SPACE",
        ...     experiment="/MY_SPACE/MY_PROJECT/EXP_001",
        ...     props={"name": "Step 1"},
        ... )
        >>> sample.save()
        >>> sample.get_datasets().df
    """

    def __init__(
        self,
        openbis_obj: Any,
        type: Any,
        project: Optional[Any] = None,
        data: Optional[dict[str, Any]] = None,
        props: Optional[dict[str, Any]] = None,
        attrs: Optional[list[str]] = None,
        **kwargs: Any,
    ) -> None:
        """Initialise a Sample instance.

        Args:
            openbis_obj: The :class:`~pybis.Openbis` connection instance.
            type: The sample type object (controls which properties are valid).
            project: Optional project object or identifier to assign the
                sample to.  Inferred from ``experiment`` if not given.
            data: Raw sample dict from the V3 API.  When provided, attributes
                and properties are populated automatically.
            props: Initial property values as a ``{code: value}`` dict.
            attrs: List of attribute names that were explicitly fetched.
                Used to avoid accidentally clearing unfetched relationship
                lists such as ``parents`` or ``children``.
            **kwargs: Additional attribute key/value pairs (e.g.
                ``experiment``, ``space``).
        """
        self.__dict__["openbis"] = openbis_obj
        self.__dict__["type"] = type
        ph = PropertyHolder(openbis_obj, type)
        self.__dict__["p"] = ph
        self.__dict__["a"] = AttrHolder(openbis_obj, "sample", type)
        self.__dict__["formatter"] = PropertyReformatter(openbis_obj)

        if data is not None:
            self._set_data(data)

        if kwargs is not None:
            for key in kwargs:
                setattr(self, key, kwargs[key])

            if "experiment" in kwargs:
                try:
                    experiment = self.experiment
                    if experiment is not None and "space" not in kwargs:
                        project = experiment.project
                        self.a.space = project.space
                except Exception:
                    pass

        if project is None:
            if self.experiment:
                self.project = self.experiment.project  # type: ignore[misc]  # reason: __setattr__ routes to AttrHolder at runtime
        else:
            self.project = project  # type: ignore[misc]  # reason: __setattr__ routes to AttrHolder at runtime

        if props is not None:
            for key in props:
                setattr(self.p, key, props[key])

        if getattr(self, "parents") is None:
            self.a.__dict__["_parents"] = []
        else:
            if not self.is_new:
                if attrs is not None and "parents" not in attrs:
                    self.a.__dict__["_parents"] = None
                else:
                    self.a.__dict__["_parents_orig"] = copy.copy(
                        self.a.__dict__["_parents"]
                    )

        if getattr(self, "children") is None:
            self.a.__dict__["_children"] = []
        else:
            if not self.is_new:
                if attrs is not None and "children" not in attrs:
                    self.a.__dict__["_children"] = None
                else:
                    self.a.__dict__["_children_orig"] = copy.copy(
                        self.a.__dict__["_children"]
                    )

        if getattr(self, "components") is None:
            self.a.__dict__["_components"] = []
        else:
            if not self.is_new:
                self.a.__dict__["_components_orig"] = self.a.__dict__["_components"]

    def _set_data(self, data: dict[str, Any]) -> None:
        """Populate attributes and properties from a raw V3 API response dict.

        Handles multi-value properties, array types, and spreadsheet widgets.

        Args:
            data: Raw sample dict as returned by the V3 API.
        """
        self.a(data)
        self.__dict__["data"] = data

        for key, value in data["properties"].items():
            property_type = self.p._property_names[key.lower()]
            data_type = property_type["dataType"]
            if "multiValue" in property_type:
                if property_type["multiValue"] is True:
                    if type(value) is not list:
                        value = [value]
                    if data_type in (
                        "ARRAY_INTEGER",
                        "ARRAY_REAL",
                        "ARRAY_STRING",
                        "ARRAY_TIMESTAMP",
                    ):
                        value = [self.formatter.to_array(data_type, x) for x in value]
                    else:
                        value = self.formatter.to_array(data_type, value)
                else:
                    if type(value) is list and data_type not in (
                        "ARRAY_INTEGER",
                        "ARRAY_REAL",
                        "ARRAY_STRING",
                        "ARRAY_TIMESTAMP",
                    ):
                        raise ValueError(
                            f"Property type {property_type} is not a multi-value property!"
                        )
                    if data_type in (
                        "ARRAY_INTEGER",
                        "ARRAY_REAL",
                        "ARRAY_STRING",
                        "ARRAY_TIMESTAMP",
                    ):
                        value = self.formatter.to_array(data_type, value)
            else:
                if data_type in (
                    "ARRAY_INTEGER",
                    "ARRAY_REAL",
                    "ARRAY_STRING",
                    "ARRAY_TIMESTAMP",
                ):
                    value = self.formatter.to_array(data_type, value)
            if (
                data_type == "XML"
                and "metaData" in property_type
                and "custom_widget" in property_type["metaData"]
                and property_type["metaData"]["custom_widget"].upper() == "SPREADSHEET"
            ):
                if key.lower() in self.p.__dict__:
                    old_spreadsheet = self.p.__dict__[key.lower()]
                    old_spreadsheet._set_data(self.formatter.to_spreadsheet(value))
                    value = old_spreadsheet
                else:
                    value = self.formatter.to_spreadsheet(value)
            self.p.__dict__[key.lower()] = value

    def __dir__(self) -> list[str]:
        """Return public attributes and methods for tab-completion.

        Returns:
            A list of attribute and method names available on this sample.
        """
        return [
            "type",
            "get_parents()",
            "get_children()",
            "get_components()",
            "add_parents()",
            "add_children()",
            "add_components()",
            "del_parents()",
            "del_children()",
            "del_components()",
            "set_parents()",
            "set_children()",
            "set_components()",
            "get_datasets()",
            "space",
            "project",
            "experiment",
            "container",
            "tags",
            "set_tags()",
            "add_tags()",
            "del_tags()",
            "add_attachment()",
            "get_attachments()",
            "download_attachments()",
            "save()",
            "delete()",
            "mark_to_be_deleted()",
            "unmark_to_be_deleted()",
            "is_marked_to_be_deleted()",
            "attrs",
            "props",
        ] + super().__dir__()

    def _container(self, value: Optional[Any] = None) -> Optional["Sample"]:
        """Internal getter/setter for the ``container`` attribute.

        Fetches the container sample object when called without arguments,
        or updates the internal reference when a value is supplied.

        Args:
            value: A sample identifier string, a :class:`Sample` object, or
                ``""`` to clear the container.  ``None`` to read the current
                container.

        Returns:
            The container :class:`Sample`, or ``None`` if unset.
        """
        if value is not None:
            if value == "":
                if self.is_new:
                    pass
                else:
                    self.a.__dict__["_container"] = {}
            else:
                obj = None
                if isinstance(value, str):
                    obj = getattr(self._openbis, "get_sample")(value)
                elif value is None:
                    self.a.__dict__["_container"] = {}
                else:
                    obj = value

                assert obj is not None
                self.a.__dict__["_container"] = obj.data["identifier"]

                if self.is_new:
                    pass
                else:
                    self.a.__dict__["_container"]["isModified"] = True
        else:
            try:
                return cast(
                    "Sample",
                    self.openbis.get_sample(self.a._container["identifier"]),
                )
            except Exception:
                pass
        return None

    @property
    def type(self) -> Any:
        """The :class:`~pybis.entity_type.SampleType` of this sample."""
        return self.__dict__["type"]

    @type.setter
    def type(self, type_name: str) -> None:
        """Change the sample type.

        Args:
            type_name: Code of the new sample type.
        """
        sample_type = self.openbis.get_sample_type(type_name)
        self.p.__dict__["_type"] = sample_type
        self.a.__dict__["_type"] = sample_type

    def __getattr__(self, name: str) -> Any:
        """Delegate attribute access to the attribute holder."""
        if name in ["container"]:
            return getattr(self, "_" + name)()

        return getattr(self.__dict__["a"], name)

    def __setattr__(self, name: str, value: Any) -> None:
        """Set an entity attribute (or replace all properties via ``props``)."""
        if name in ["set_properties", "set_tags", "add_tags"]:
            raise ValueError("These are methods which should not be overwritten")

        if name in ["container"]:
            getattr(self, "_" + name)(value)
            return

        if name in ["p", "props"]:
            if isinstance(value, dict):
                for p in value:
                    setattr(self.__dict__["p"], p, value[p])
            else:
                raise ValueError("please provide a dictionary for setting properties")
        else:
            setattr(self.__dict__["a"], name, value)

    def _repr_html_(self) -> str:
        return cast(str, self.a._repr_html_())

    def __repr__(self) -> str:
        """Return a table of all attributes."""
        return cast(str, self.a.__repr__())

    def set_properties(self, properties: dict[str, Any]) -> None:
        """Set multiple properties at once from a dictionary.

        Does not save the sample — call :meth:`save` afterwards.

        Args:
            properties: A ``{property_code: value}`` dictionary.

        Example:
            >>> sample.set_properties({"name": "Control", "concentration": 1.5})
            >>> sample.save()
        """
        for prop in properties.keys():
            setattr(self.p, prop, properties[prop])

    set_props = set_properties

    def get_datasets(self, **kwargs: Any) -> Any:
        """Return all datasets linked to this sample.

        Args:
            **kwargs: Additional search filters forwarded to
                ``openbis.get_datasets()``.

        Returns:
            A :class:`~pybis.things.Things` container with matching datasets.

        Example:
            >>> sample.get_datasets().df
            >>> sample.get_datasets(type="RAW_DATA").df
        """
        return self.openbis.get_datasets(sample=self.permId, **kwargs)

    def get_projects(self, **kwargs: Any) -> Any:
        """Return the project(s) this sample is associated with.

        Args:
            **kwargs: Additional filters forwarded to ``openbis.get_project()``.

        Returns:
            A :class:`~pybis.things.Things` container with matching projects.
        """
        return self.openbis.get_project(withSamples=[self.permId], **kwargs)

    @property
    def experiment(self) -> Optional[Any]:
        """The :class:`~pybis.experiment.Experiment` this sample belongs to.

        Returns ``None`` if the sample is not assigned to any experiment.
        """
        try:
            return self.openbis.get_experiment(self._experiment["identifier"])
        except Exception:
            return None

    def save(self) -> "Sample":
        """Persist this sample to openBIS (create or update).

        For **new** samples whose type uses auto-generated codes but where an
        explicit code has been set, the old V1 API method is used to honour
        the provided code.  All other samples follow the standard V3 path
        from :class:`~pybis.openbis_object.OpenBisObject`.

        Returns:
            This :class:`Sample` instance, updated with server-assigned
            fields.

        Raises:
            ValueError: If a mandatory property is missing or other
                validation fails.

        Example:
            >>> sample = openbis.new_sample(type="MY_TYPE", space="MY_SPACE")
            >>> sample.props.name = "Test"
            >>> sample.save()
        """
        if self.is_new and self.code is not None and self.type.autoGeneratedCode:
            request = self._new_attrs()
            if self.props:
                for prop_name, prop in self.props._property_names.items():
                    if prop["mandatory"]:
                        if (
                            getattr(self.props, prop_name) is None
                            or getattr(self.props, prop_name) == ""
                        ):
                            raise ValueError(
                                f"Property '{prop_name}' is mandatory and must not be None"
                            )
            properties = PropertyReformatter(self.openbis).format(self.p())

            for attr in request["params"][1][0]:
                if (
                    request["params"][1][0][attr] is not None
                    and "isModified" in request["params"][1][0][attr]
                ):
                    del request["params"][1][0][attr]["isModified"]

            request["params"][1][0]["properties"] = properties

            resp = self.openbis._post_request(self.openbis.as_v3, request)

            permId = resp[0]["permId"]
            new_entity_data = self.openbis.get_object_or_raise(permId).data
            self._set_data(new_entity_data)
            return self

        else:
            super().save()
        return self

    def get_eln_url(self) -> str:
        """Return the direct URL to this sample in the ELN-LIMS web UI.

        Returns:
            A URL string that opens the sample page in a browser.

        Example:
            >>> print(sample.get_eln_url())
        """
        return (
            f"{self.openbis.url}/webapp/eln-lims/?menuUniqueId=null&"
            f"viewName=showViewSamplePageFromPermId&viewData={self.permId}"
        )
