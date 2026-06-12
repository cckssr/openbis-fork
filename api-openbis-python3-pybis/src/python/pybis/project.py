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
"""Project entity for openBIS."""

from typing import Any
from urllib.parse import quote

from .openbis_object import OpenBisObject
from .things import Things
from .utils import is_identifier, is_permid


class Project(OpenBisObject, entity="project", single_item_method_name="get_project"):
    """An openBIS project, grouping related experiments and samples.

    Projects live inside a :class:`~pybis.space.Space` and serve as the
    organisational unit for experiments (collections) and directly-assigned
    samples (objects).  Datasets can be linked indirectly via experiments.

    Fetch projects via ``openbis.get_project()`` / ``openbis.get_projects()``
    and create new ones with ``openbis.new_project()`` or
    :meth:`~pybis.space.Space.new_project`.

    Attributes:
        permId (str): Server-assigned permanent identifier.
        identifier (str): Human-readable path, e.g. ``"/MY_SPACE/MY_PROJECT"``.
        code (str): Short project code (upper-case letters/digits/underscores).
        description (Optional[str]): Free-text description.
        space (str): The :class:`~pybis.space.Space` this project belongs to.

    Example:
        >>> project = openbis.new_project(
        ...     space="MY_SPACE", code="CRISPR_STUDY", description="CRISPR screen 2025"
        ... )
        >>> project.save()
        >>> project.get_collections().df
    """

    def _modifiable_attrs(self) -> None:
        return

    def __dir__(self) -> list[str]:
        """Return public attributes and methods for tab-completion.

        Returns:
            A list of attribute and method names available on this project.
        """
        return [
            "add_attachment()",
            "get_attachments()",
            "download_attachments()",
            "get_collections()",
            "get_objects()",
            "get_datasets()",
            "save()",
            "delete()",
        ] + super().__dir__()

    @property
    def props(self) -> Any:
        """The legacy property holder of this project."""
        return self.__dict__["p"]

    def get_samples(self, **kwargs: Any) -> Any:
        """Return all samples (objects) that belong directly to this project.

        Args:
            **kwargs: Additional search criteria forwarded to
                ``openbis.get_samples()``, e.g. ``type="MY_TYPE"``.

        Returns:
            A :class:`~pybis.things.Things` container with matching samples.

        Example:
            >>> project.get_samples().df
            >>> project.get_samples(type="EXPERIMENTAL_STEP").df
        """
        return self.openbis.get_samples(project=self.permId, **kwargs)

    get_objects = get_samples

    def get_sample(self, sample_code: str) -> Any:
        """Retrieve a single sample (object) from this project.

        The ``sample_code`` can be any of:

        - A full identifier, e.g. ``"/SPACE/PROJECT/SAMPLE_CODE"``
        - A permanent identifier (permId), e.g. ``"20251218172409814-1"``
        - Just the sample code — the method then searches within this project.

        Args:
            sample_code: Code, identifier, or permId of the sample.

        Returns:
            The matching :class:`~pybis.sample.Sample` object.

        Example:
            >>> project.get_sample("SAMPLE_001")
            >>> project.get_sample("/MY_SPACE/MY_PROJECT/SAMPLE_001")
        """
        if is_identifier(sample_code) or is_permid(sample_code):
            return self.openbis.get_sample(sample_code)
        else:
            return self.openbis.get_sample(project=self, code=sample_code)

    get_object = get_sample

    def get_experiments(self) -> Any:
        """Return all experiments (collections) in this project.

        Returns:
            A :class:`~pybis.things.Things` container with matching
            experiments.

        Example:
            >>> project.get_experiments().df
        """
        return self.openbis.get_experiments(project=self.permId)

    get_collections = get_experiments

    def get_datasets(self) -> Any:
        """Return all datasets linked to this project (via experiments or directly).

        Returns:
            A :class:`~pybis.things.Things` container with matching datasets.

        Example:
            >>> project.get_datasets().df
        """
        return self.openbis.get_datasets(project=self.permId)

    def get_eln_url(self) -> str:
        """Return the direct URL to this project in the ELN-LIMS web UI.

        Returns:
            A URL string that opens the project page in a browser.

        Example:
            >>> print(project.get_eln_url())
            https://my-openbis-instance.org/webapp/eln-lims/?menuUniqueId=...
        """
        query = {"type": "PROJECT", "id": self.permId}
        return (
            f"{self.openbis.url}/webapp/eln-lims/?menuUniqueId={quote(str(query))}"
            f"&viewName=showProjectPageFromPermId&viewData={self.permId}"
        )
