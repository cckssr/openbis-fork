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
"""Tag (metaproject) entity for openBIS."""

from typing import Any

from ._deprecated import deprecated


from .openbis_object import OpenBisObject


class Tag(OpenBisObject, entity="tag", single_item_method_name="get_tag"):
    """An openBIS tag (also called *metaproject*) used to label entities.

    Tags are user-owned labels that can be attached to samples, experiments,
    and materials.  They are created via ``openbis.new_tag()`` and retrieved
    via ``openbis.get_tag()`` or ``openbis.get_tags()``.

    The tag owner can be fetched with :meth:`get_owner`, and all tagged
    entities can be listed via :meth:`get_samples` or
    :meth:`get_experiments`.

    Example:
        >>> tag = openbis.new_tag(code="MY_TAG", description="Important samples")
        >>> tag.save()

        >>> # Attach to a sample
        >>> sample.add_tags("MY_TAG")
        >>> sample.save()

        >>> # List all samples with this tag
        >>> tag.get_samples().df
    """

    def __dir__(self) -> list[str]:
        """Return public attributes and methods for tab-completion.

        Returns:
            A list of attribute and method names.
        """
        return [
            "get_samples()",
            "get_experiments()",
            "get_materials()",
            "get_owner()",
        ] + super().__dir__()

    def get_owner(self) -> Any:
        """Return the :class:`~pybis.person.Person` who owns this tag.

        Returns:
            The :class:`~pybis.person.Person` object representing the owner.
        """
        return self.openbis.get_person(self.owner)

    def get_samples(self) -> Any:
        """Return all samples that carry this tag.

        Returns:
            A :class:`~pybis.things.Things` container whose ``.df`` gives
            a :class:`~pandas.DataFrame` of matching samples.

        Example:
            >>> tag.get_samples().df
        """
        return self.openbis.get_samples(tags=[self.code])

    def get_experiments(self) -> Any:
        """Return all experiments (collections) that carry this tag.

        Returns:
            A :class:`~pybis.things.Things` container whose ``.df`` gives
            a :class:`~pandas.DataFrame` of matching experiments.

        Example:
            >>> tag.get_experiments().df
        """
        return self.openbis.get_experiments(tags=[self.code])

    @deprecated("Material is deprecated; use Object instead")
    def get_materials(self) -> None:
        """Return all materials that carry this tag.

        Raises:
            NotImplementedError: Always — this method is not yet implemented.
        """
        raise NotImplementedError("Not yet implemented.")
