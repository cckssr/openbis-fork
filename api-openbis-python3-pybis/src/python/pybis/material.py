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
"""Material entity for openBIS.

.. deprecated::
    The *Material* concept has been deprecated by openBIS and is retained
    here only for backward compatibility with older openBIS instances.
    New integrations should use :class:`~pybis.sample.Sample` (Object)
    instead.
"""

from typing import Any, Optional

from .attribute import AttrHolder
from .openbis_object import OpenBisObject
from .property import PropertyHolder
from .property_reformatter import PropertyReformatter
from .utils import VERBOSE

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


@deprecated("Material is deprecated; use Object instead")
class Material(OpenBisObject):
    """An openBIS material entity.

    .. deprecated::
        The Material concept is deprecated in openBIS.  Use
        :class:`~pybis.sample.Sample` for new data management workflows.
        This class is retained for backward compatibility with legacy
        openBIS instances that still expose materials via the V3 API.

    Materials are typed entities with properties, similar to samples, but
    without hierarchical organisation into spaces and projects.

    Attributes:
        entity (str): Fixed to ``"material"``.
        code (str): Unique material code within its material type.
        description (str): Human-readable description.

    Example:
        >>> warnings.warn("Material is deprecated", DeprecationWarning)
        >>> mat = openbis.get_material("COMPOUND_X", material_type="CHEMICAL")
        >>> print(mat.code)
        COMPOUND_X
    """

    def __init__(
        self,
        openbis_obj: Any,
        type: Any,
        data: Optional[dict] = None,
        props: Optional[dict] = None,
        **kwargs: Any,
    ) -> None:
        """Initialise a Material instance.

        .. deprecated::
            Prefer :class:`~pybis.sample.Sample` for new code.

        Args:
            openbis_obj: The :class:`~pybis.Openbis` connection instance.
            type: The material type object describing allowed properties.
            data: Raw material dict from the V3 API.  When provided the
                attributes and properties are populated automatically.
            props: Initial property values as a ``{code: value}`` dict.
            **kwargs: Additional attribute key/value pairs.
        """
        self.__dict__["entity"] = "material"
        self.__dict__["openbis"] = openbis_obj
        self.__dict__["type"] = type
        ph = PropertyHolder(openbis_obj, type)
        self.__dict__["p"] = ph
        self.__dict__["a"] = AttrHolder(openbis_obj, "material", type)
        self.__dict__["formatter"] = PropertyReformatter(openbis_obj)

        if data is not None:
            self._set_data(data)

        if props is not None:
            for key in props:
                setattr(self.p, key, props[key])

        if kwargs is not None:
            for key in kwargs:
                setattr(self, key, kwargs[key])

    def __dir__(self) -> list[str]:
        """Return public attributes for tab-completion.

        Returns:
            A list of attribute names.
        """
        return ["code", "description", "set_tags()", "add_tags()", "del_tags()"]

    def save(self) -> "Material":
        """Persist this material to openBIS (create or update).

        .. deprecated::
            Prefer :class:`~pybis.sample.Sample` for new code.

        Validates that all mandatory properties are filled before calling
        the V3 API.

        Returns:
            This :class:`Material` instance, updated with server-assigned
            fields.

        Raises:
            ValueError: If a mandatory property is missing.
        """
        for prop_name, prop in self.props._property_names.items():
            if prop["mandatory"]:
                if (
                    getattr(self.props, prop_name) is None
                    or getattr(self.props, prop_name) == ""
                ):
                    raise ValueError(
                        f"Property '{prop_name}' is mandatory and must not be None"
                    )

        props = self.formatter.format(self.p._all_props())

        if self.is_new:
            request = self._new_attrs()
            request["params"][1][0]["properties"] = props
            resp = self.openbis._post_request(self.openbis.as_v3, request)

            if VERBOSE:
                print("Material successfully created.")
            new_material_data = self.openbis.get_tag(resp[0]["permId"], only_data=True)
            self._set_data(new_material_data)
            return self

        else:
            request = self._up_attrs()
            request["params"][1][0]["properties"] = props
            self.openbis._post_request(self.openbis.as_v3, request)
            if VERBOSE:
                print("Material successfully updated.")
            new_material_data = self.openbis.get_tag(self.permId, only_data=True)
            self._set_data(new_material_data)

    def delete(self, reason: str = "no reason") -> None:
        """Delete this material from openBIS.

        .. deprecated::
            Prefer :class:`~pybis.sample.Sample` for new code.

        Args:
            reason: Human-readable reason recorded with the deletion.
                Defaults to ``"no reason"``.
        """
        self.openbis.delete_entity(entity=self.entity, id=self.permId, reason=reason)
        if VERBOSE:
            print(f"Material {self.permId} successfully deleted.")
