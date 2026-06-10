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
"""Experiment (Collection) entity for openBIS."""

from typing import Any
from urllib.parse import quote

from .openbis_object import OpenBisObject
from .openbis_typing import PropertyDataArrayTypes


class Experiment(
    OpenBisObject, entity="experiment", single_item_method_name="get_experiment"
):
    """An openBIS experiment, also called a *Collection* in the ELN-LIMS UI.

    Experiments group related :class:`~pybis.sample.Sample` objects and
    :class:`~pybis.dataset.DataSet` objects under a typed, property-rich
    container within a :class:`~pybis.project.Project`.

    Fetch experiments via ``openbis.get_experiment()`` /
    ``openbis.get_experiments()`` and create new ones with
    ``openbis.new_experiment()``.

    **Properties** are accessed through the ``props`` (or ``p``) namespace::

        exp.props.my_property = "value"
        exp.save()

    **Attributes** (identifier, project, tags, …) are accessed directly::

        exp.identifier  # '/SPACE/PROJECT/EXPERIMENT_CODE'
        exp.project  # project object
        exp.tags  # list of tag codes

    Attributes:
        permId (str): Server-assigned permanent identifier.
        identifier (str): Human-readable path, e.g.
            ``"/SPACE/PROJECT/EXP_CODE"``.
        code (str): Short code of this experiment.
        type (Any): The :class:`~pybis.entity_type.ExperimentType` of this
            experiment.
        props (PropertyHolder): :class:`~pybis.property.PropertyHolder` for typed properties.

    Example:
        >>> exp = openbis.new_experiment(
        ...     type="DEFAULT_EXPERIMENT",
        ...     project="/MY_SPACE/MY_PROJECT",
        ...     props={"name": "Dose-response study"},
        ... )
        >>> exp.save()
        >>> exp.get_datasets().df
    """

    def _set_data(self, data: dict) -> None:
        """Populate attributes and properties from a raw V3 API response dict.

        Handles multi-value properties, array types, and spreadsheet widgets.

        Args:
            data: Raw experiment dict as returned by the V3 API.
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
                    if data_type in PropertyDataArrayTypes.__args__:
                        value = [self.formatter.to_array(data_type, x) for x in value]
                    else:
                        value = self.formatter.to_array(data_type, value)
                else:
                    if (
                        type(value) is list
                        and data_type not in PropertyDataArrayTypes.__args__
                    ):
                        raise ValueError(
                            f"Property type {property_type} is not a multi-value property!"
                        )
                    if data_type in PropertyDataArrayTypes.__args__:
                        value = self.formatter.to_array(data_type, value)
            else:
                if data_type in PropertyDataArrayTypes.__args__:
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

    def __str__(self) -> str:
        """String representation of this experiment returns its code."""
        return self.data["code"]

    def __dir__(self) -> list[str]:
        """Return public attributes and methods for tab-completion.

        Returns:
            A list of attribute and method names available on this experiment.
        """
        return [
            "get_datasets()",
            "get_samples()",
            "set_tags()",
            "add_tags()",
            "del_tags()",
            "add_attachment()",
            "get_attachments()",
            "download_attachments()",
            "save()",
            "attrs",
            "props",
        ] + super().__dir__()

    @property
    def props(self) -> Any:
        """The :class:`~pybis.property.PropertyHolder` for this experiment's properties.

        Use attribute-style or item-style access to read or set values::

            exp.props.name = "My experiment"
            exp.props["my.property"] = "value"
        """
        return self.__dict__["p"]

    @property
    def type(self) -> Any:
        """The :class:`~pybis.entity_type.ExperimentType` of this experiment."""
        return self.__dict__["type"]

    @type.setter
    def type(self, type_name: str) -> None:
        """Change the experiment type.

        Args:
            type_name: Code of the new experiment type.
        """
        experiment_type = self.openbis.get_experiment_type(type_name)
        self.p.__dict__["_type"] = experiment_type
        self.a.__dict__["_type"] = experiment_type

    def __getattr__(self, name: str) -> Any:
        return getattr(self.__dict__["a"], name)

    def __setattr__(self, name: str, value: Any) -> None:
        if name in ["set_properties", "add_tags()", "del_tags()", "set_tags()"]:
            raise ValueError("These are methods which should not be overwritten")
        elif name in ["p", "props"]:
            if isinstance(value, dict):
                for p in value:
                    setattr(self.__dict__["p"], p, value[p])
            else:
                raise ValueError("please provide a dictionary for setting properties")
        else:
            setattr(self.__dict__["a"], name, value)

    def _repr_html_(self) -> str:
        html = self.a._repr_html_()
        return html

    def set_properties(self, properties: dict) -> None:
        """Set multiple properties at once from a dictionary.

        Does not save the experiment — call :meth:`save` afterwards.

        Args:
            properties: A ``{property_code: value}`` dictionary.

        Example:
            >>> exp.set_properties({"name": "Study A", "status": "ONGOING"})
            >>> exp.save()
        """
        for prop in properties.keys():
            setattr(self.p, prop, properties[prop])

    set_props = set_properties

    def get_datasets(self, **kwargs: Any) -> Any:
        """Return all datasets linked to this experiment.

        Args:
            **kwargs: Additional search filters forwarded to
                ``openbis.get_datasets()``.

        Returns:
            A :class:`~pybis.things.Things` container with matching datasets.

        Example:
            >>> exp.get_datasets().df
            >>> exp.get_datasets(type="RAW_DATA").df
        """
        if self.permId is None:
            return None
        return self.openbis.get_datasets(experiment=self.permId, **kwargs)

    def get_projects(self, **kwargs: Any) -> Any:
        """Return the project this experiment belongs to.

        Args:
            **kwargs: Additional filters forwarded to ``openbis.get_project()``.

        Returns:
            The :class:`~pybis.project.Project` object, or ``None`` if
            the experiment has no ``permId`` yet.
        """
        if self.permId is None:
            return None
        return self.openbis.get_project(experiment=self.permId, **kwargs)

    def get_samples(self, **kwargs: Any) -> Any:
        """Return all samples (objects) linked to this experiment.

        Args:
            **kwargs: Additional search filters forwarded to
                ``openbis.get_samples()``.

        Returns:
            A :class:`~pybis.things.Things` container with matching samples.

        Example:
            >>> exp.get_samples().df
            >>> exp.get_samples(type="EXPERIMENTAL_STEP").df
        """
        if self.permId is None:
            return None
        return self.openbis.get_samples(experiment=self.permId, **kwargs)

    get_objects = get_samples

    def add_samples(self, *samples: Any) -> None:
        """Assign one or more existing samples to this experiment.

        Each sample must not already belong to another experiment.  The
        experiment must be saved before samples can be assigned.

        Args:
            *samples: Sample objects or identifier strings to add.

        Raises:
            ValueError: If a sample already belongs to a different experiment.
            ValueError: If this experiment has not been saved yet.

        Example:
            >>> exp.add_samples(sample1, "/MY_SPACE/MY_PROJECT/SAMPLE_CODE")
        """
        for sample in samples:
            if isinstance(sample, str):
                obj = self.openbis.get_sample(sample)
            else:
                obj = sample

            if obj.experiment is not None:
                raise ValueError(
                    f"sample {obj.code} already belongs to experiment {obj.experiment}"
                )
            else:
                if self.is_new:
                    raise ValueError(
                        "You need to save this experiment first before you can assign any samples to it"
                    )
                else:
                    obj.experiment = self.identifier
                    obj.save()
                    self.a.__dict__["_samples"].append(obj._identifier)

    add_objects = add_samples

    def del_samples(self, samples: Any) -> None:
        """Remove samples from this experiment.

        Raises:
            NotImplementedError: Always — this method is not yet implemented.
        """
        raise NotImplementedError("Not yet implemented.")
        # if not isinstance(samples, list):
        #     samples = [samples]

        # objects = []
        # for sample in samples:
        #     if isinstance(sample, str):
        #         obj = self.openbis.get_sample(sample)
        #         objects.append(obj)
        #     else:
        #         objects.append(obj)
        # self.samples = objects

    del_objects = del_samples

    def get_eln_url(self) -> str:
        """Return the direct URL to this experiment in the ELN-LIMS web UI.

        Returns:
            A URL string that opens the experiment page in a browser.

        Example:
            >>> print(exp.get_eln_url())
            https://my-openbis-instance.org/webapp/eln-lims/?menuUniqueId=...
        """
        query = {"type": "EXPERIMENT", "id": self.permId}
        return (
            f"{self.openbis.url}/webapp/eln-lims/?menuUniqueId={quote(str(query))}"
            f'&viewName=showExperimentPageFromIdentifier&viewData=["{self.identifier}",false]'
        )
