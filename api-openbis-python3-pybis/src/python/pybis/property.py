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
"""Property namespace for openBIS entity objects.

The :class:`PropertyHolder` is exposed as the ``props`` (and ``p``) attribute
on :class:`~pybis.sample.Sample`, :class:`~pybis.experiment.Experiment`, and
:class:`~pybis.dataset.DataSet`.  It validates values against the property
type definitions before storing them.
"""

from typing import Any, Optional, TYPE_CHECKING

from tabulate import tabulate

from .utils import check_datatype

if TYPE_CHECKING:
    from .pybis import Openbis


class PropertyHolder:
    """A namespace for the typed properties of an openBIS entity.

    Each openBIS entity type (sample type, experiment type, …) declares a
    set of :class:`~pybis.entity_type.PropertyType` assignments with
    associated data types, mandatory flags, and — for controlled vocabularies
    — an allowed term list.

    A :class:`PropertyHolder` is created from the entity type and attached
    to the entity as ``entity.props`` (alias ``entity.p``).  Attribute-style
    and item-style access are both supported::

        sample.props.my_property = "Hello"
        sample.props["my.weird-property"] = 42
        sample.props()  # → dict of all properties
        sample.props("MY_PROP")  # → current value of MY_PROP

    **Hints** — append an underscore to a property name to inspect its type
    metadata instead of its value::

        sample.props.my_property_  # → {'Label': 'REAL'} or list of terms

    Attributes:
        _property_names (dict): Internal mapping of lower-cased property
            codes to their type descriptor dicts.

    Args:
        openbis_obj: The :class:`~pybis.Openbis` connection (used to
            resolve controlled-vocabulary terms).
        type: An entity type object whose ``propertyAssignments`` describe
            the allowed properties.  Pass ``None`` for a type-less holder.
    """

    def __init__(self, openbis_obj: Openbis, type: Optional[Any] = None) -> None:
        """Initialise a PropertyHolder.

        Iterates the ``propertyAssignments`` of ``type`` and pre-loads
        controlled-vocabulary term lists so that assignments can be validated
        immediately without additional API calls.

        Args:
            openbis_obj: The :class:`~pybis.Openbis` connection instance.
            type: Entity type object with a ``propertyAssignments`` list in
                its ``data`` dict.  If ``None``, the holder starts empty.
        """
        self.__dict__["_openbis"] = openbis_obj
        self.__dict__["_property_names"] = {}
        if type is None:
            return

        self.__dict__["_type"] = type
        if (
            "propertyAssignments" in type.data
            and type.data["propertyAssignments"] is not None
        ):
            for prop in type.data["propertyAssignments"]:
                property_name = prop["propertyType"]["code"].lower()
                self._property_names[property_name] = prop["propertyType"]
                self._property_names[property_name]["mandatory"] = prop["mandatory"]
                self._property_names[property_name]["showInEditView"] = prop[
                    "showInEditView"
                ]
                if prop["propertyType"]["dataType"] == "CONTROLLEDVOCABULARY":
                    pt = self._openbis.get_property_type(prop["propertyType"]["code"])
                    voc = self._openbis.get_vocabulary(pt.vocabulary)
                    terms = voc.get_terms()
                    self._property_names[property_name]["terms"] = terms

    def _all_props(self) -> dict:
        """Return all properties including ``None`` values.

        Returns:
            A ``{code: value}`` dict for every property defined by the type,
            including those that have not been set.
        """
        props = {}
        if not getattr(self, "_type"):
            return props
        for code in self._type.codes():
            props[code] = getattr(self, code)
        return props

    def all(self) -> dict:
        """Return all properties as a dictionary, including ``None`` values.

        Returns:
            A ``{code: value}`` dict for every property defined by the type.

        Example:
            >>> sample.props.all()
        """
        props = {}
        for code in self._type.codes():
            props[code] = getattr(self, code)
        return props

    def all_nonempty(self) -> dict:
        """Return only properties that have a non-``None`` value.

        Returns:
            A ``{code: value}`` dict containing only set properties.

        Example:
            >>> sample.props.all_nonempty()
        """
        props = {}
        for code in self._type.codes():
            value = getattr(self, code)
            if value is not None:
                props[code] = value
        return props

    def __call__(self, *args: Any) -> Any:
        """Call-style access to get or set a property value.

        - ``props()`` → :meth:`all`
        - ``props("CODE")`` → get value of ``"CODE"``
        - ``props("CODE", value)`` → set value of ``"CODE"``

        Args:
            *args: Zero, one, or two positional arguments.

        Returns:
            Dict (no args), property value (one arg), or ``None`` (two args).

        Raises:
            ValueError: If more than two arguments are supplied.
        """
        if len(args) == 0:
            return self.all()
        elif len(args) == 1:
            return getattr(self, args[0])
        elif len(args) == 2:
            return setattr(self, args[0], args[1])
        else:
            raise ValueError("called properties with more than 2 arguments")

    def get(self, *args: Any) -> Any:
        """Retrieve one or several property values.

        - ``props.get()`` → :meth:`all`
        - ``props.get("CODE")`` → single value
        - ``props.get(["A", "B"])`` or ``props.get("A", "B")`` → ``{"A": …, "B": …}``

        Args:
            *args: Property code(s) — a single string, a list, or multiple
                positional strings.

        Returns:
            A single value or a ``{code: value}`` dict.

        Example:
            >>> sample.props.get("name")
            >>> sample.props.get(["name", "description"])
        """
        if len(args) == 0:
            return self.all()
        elif len(args) == 1 and not isinstance(args[0], list):
            return getattr(self, args[0])
        else:
            if isinstance(args[0], list):
                args = args[0]
            return {arg: getattr(self, arg, None) for arg in args}

    def set(self, *args: Any) -> None:
        """Set one or more property values.

        - ``props.set("CODE", value)`` — set a single property.
        - ``props.set({"A": val1, "B": val2})`` — set multiple properties.

        Args:
            *args: Either ``(code, value)`` or a single ``dict``.

        Example:
            >>> sample.props.set("name", "My sample")
            >>> sample.props.set({"name": "X", "description": "Y"})
        """
        if len(args) == 2:
            setattr(self, args[0], args[1])
        elif len(args) == 1 and isinstance(args[0], dict):
            for key in args[0]:
                setattr(self, key, args[0][key])

    def __getitem__(self, key: str) -> Any:
        """Item-style property access for names with special characters.

        Use this when the property code contains dots, dashes, or other
        characters that are not valid Python identifiers.

        Args:
            key: Property code (case-sensitive as stored in openBIS).

        Returns:
            The current property value.

        Example:
            >>> sample.props["my.weird-property"]
        """
        return getattr(self, key)

    def __getattr__(self, name: str) -> Any:
        """Attribute-style property access with optional hint mode.

        - ``props.my_property`` → current value.
        - ``props.my_property_`` (trailing underscore) → type metadata dict
          or vocabulary term list rather than the value.

        Args:
            name: Property code or property code followed by ``_``.

        Returns:
            The property value, a type hint dict, or a vocabulary terms object.
        """
        if name == "_ipython_canary_method_should_not_exist_":
            return
        if name.endswith("_"):
            name = name.rstrip("_")
            if name in self._property_names:
                property_type = self._property_names[name]
                if property_type["dataType"] == "CONTROLLEDVOCABULARY":
                    return property_type["terms"]
                else:
                    syntax = {property_type["label"]: property_type["dataType"]}
                    if property_type["dataType"] == "TIMESTAMP":
                        syntax["syntax"] = "YYYY-MM-DD HH:MIN:SS"
                    return syntax
            else:
                return

    def __setattr__(self, name: str, value: Any) -> None:
        """Attribute-style property setter with type validation.

        Validates ``value`` against the property's declared data type and,
        for ``CONTROLLEDVOCABULARY`` properties, against the allowed term
        list.

        Args:
            name: Property code (lower-cased).
            value: New value.  Pass ``None`` or ``""`` to clear the property.

        Raises:
            KeyError: If ``name`` is not a known property of this entity type.
            ValueError: If ``value`` fails type or vocabulary validation.
        """
        if name not in self._property_names:
            raise KeyError(
                f"No such property: «{name}». Allowed properties are: {', '.join(self._property_names.keys())}"
            )
        property_type = self._property_names[name]
        data_type = property_type["dataType"]
        if (
            "multiValue" in property_type
            and property_type["multiValue"] is not True
            and type(value) == list
            and data_type.startswith("ARRAY_") is False
        ):
            raise ValueError(
                f"Property type {property_type['code']} is not a multi-value property!"
            )
        if value == "":
            value = None
        if value is not None:
            if data_type == "CONTROLLEDVOCABULARY":
                terms = property_type["terms"]
                if (
                    "multiValue" in property_type
                    and property_type["multiValue"] is True
                ):
                    if type(value) != list:
                        value = [value]
                    for single_value in value:
                        if str(single_value).upper() not in terms.df["code"].values:
                            raise ValueError(
                                f"Value for attribute «{name}» must be one of these terms: {', '.join(terms.df['code'].values)}"
                            )
                else:
                    value = str(value).upper()
                    if value not in terms.df["code"].values:
                        raise ValueError(
                            f"Value for attribute «{name}» must be one of these terms: {', '.join(terms.df['code'].values)} VALUE:{value}"
                        )
            elif data_type == "SAMPLE":
                if (
                    "multiValue" in property_type
                    and property_type["multiValue"] is True
                ):
                    if type(value) != list:
                        value = [value]
            elif data_type in (
                "INTEGER",
                "BOOLEAN",
                "VARCHAR",
                "REAL",
                "ARRAY_INTEGER",
                "ARRAY_REAL",
                "ARRAY_STRING",
            ):
                is_multi_value = (
                    property_type["multiValue"] is True
                    if "multiValue" in property_type
                    else False
                )
                if not check_datatype(data_type, value, is_multi_value):
                    if is_multi_value:
                        raise ValueError(
                            f"Multi-value property '{property_type['code']}' must be of type {data_type} - Provided value:{value}"
                        )
                    else:
                        raise ValueError(
                            f"Property '{property_type['code']}' must be of type {data_type} - Provided value:{value}"
                        )
        self.__dict__[name] = value

    def __setitem__(self, key: str, value: Any) -> None:
        """Item-style property setter for names with special characters.

        Args:
            key: Property code.
            value: New value.

        Example:
            >>> sample.props["my.weird-property"] = "hello"
        """
        return setattr(self, key, value)

    def __dir__(self) -> Any:
        return self._property_names

    def _repr_html_(self) -> str:
        def nvl(val: Any, string: str = "") -> Any:
            if val is None:
                return string
            elif val == "true":
                return True
            elif val == "false":
                return False
            return val

        html = """
            <table border="1" class="dataframe">
            <thead>
                <tr style="text-align: right;">
                <th>property</th>
                <th>value</th>
                <th>description</th>
                <th>type</th>
                <th>mandatory</th>
                </tr>
            </thead>
            <tbody>
        """

        for prop_name, prop in self._property_names.items():
            html += "<tr>"
            html += "".join(
                f"<td>{item}</td>"
                for item in [
                    prop_name,
                    nvl(getattr(self, prop_name, ""), ""),
                    prop.get("description"),
                    prop.get("dataType"),
                    prop.get("mandatory"),
                ]
            )
            html += "</tr>"

        html += """
            </tbody>
            </table>
        """
        return html

    def __repr__(self) -> str:
        def nvl(val: Any, string: str = "") -> Any:
            if val is None:
                return string
            elif val == "true":
                return True
            elif val == "false":
                return False
            return str(val)

        headers = ["property", "value", "mandatory"]

        lines = []
        for prop_name in self._property_names:
            lines.append([prop_name, nvl(getattr(self, prop_name, ""))])
        return tabulate(lines, headers=headers)
