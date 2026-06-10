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
"""Space entity for openBIS."""

from typing import Any, Optional
from urllib.parse import quote

from .openbis_object import OpenBisObject
from .things import Things
from .utils import is_identifier, is_permid


class Space(OpenBisObject, entity="space", single_item_method_name="get_space"):
    """An openBIS space — the top-level organisational container.

    A space groups :class:`~pybis.project.Project` objects and acts as the
    primary access-control boundary.  Users and groups can be granted roles
    at the space level, restricting what they can see and modify.

    Fetch spaces via ``openbis.get_space()`` / ``openbis.get_spaces()`` and
    create new ones with ``openbis.new_space()``.

    Attributes:
        code (str): Unique space code (upper-case letters/digits/underscores).
        description (Optional[str]): Human-readable description.

    Example:
        >>> space = openbis.new_space(code="MY_SPACE", description="Team workspace")
        >>> space.save()

        >>> project = space.new_project(code="STUDY_01", description="First study")
        >>> project.save()

        >>> space.get_projects().df
    """

    def __dir__(self) -> list[str]:
        """Return public attributes and methods for tab-completion.

        Returns:
            A list of attribute and method names available on this space.
        """
        return [
            "get_projects()",
            "new_project()",
            "get_project()",
            "get_experiments()",
            "get_experiment()",
            "get_collections()",
            "get_collection()",
            "get_samples()",
            "get_sample()",
            "get_objects()",
            "get_object()",
            "delete()",
        ] + super().__dir__()

    def __str__(self) -> str:
        return self.data.get("code", None)

    def get_samples(self, **kwargs: Any) -> Things:
        """Return all samples (objects) in this space.

        Args:
            **kwargs: Additional search criteria forwarded to
                ``openbis.get_samples()``, e.g. ``type="MY_TYPE"``.

        Returns:
            A :class:`~pybis.things.Things` container with matching samples.

        Example:
            >>> space.get_samples().df
            >>> space.get_samples(type="EXPERIMENTAL_STEP").df
        """
        return self.openbis.get_samples(space=self.code, **kwargs)

    get_objects = get_samples

    def get_sample(self, sample_code: str, project_code: Optional[str] = None) -> Any:
        """Retrieve a single sample (object) from this space.

        The ``sample_code`` can be a full identifier, a permId, or just the
        sample code (optionally combined with a ``project_code`` to narrow
        the path).

        Args:
            sample_code: Full identifier, permId, or short sample code.
            project_code: Optional project code used to build the full
                identifier when only a short code is given.

        Returns:
            The matching :class:`~pybis.sample.Sample` object.

        Example:
            >>> space.get_sample("SAMPLE_001")
            >>> space.get_sample("SAMPLE_001", project_code="MY_PROJECT")
        """
        if is_identifier(sample_code) or is_permid(sample_code):
            return self.openbis.get_sample(sample_code)
        else:
            if project_code is None:
                return self.openbis.get_sample(f"/{self.code}/{sample_code}")
            else:
                return self.openbis.get_sample(
                    f"/{self.code}/{project_code}/{sample_code}"
                )

    get_object = get_sample

    def get_project(self, project_code: str) -> Things:
        """Return a single project from this space by code or identifier.

        Args:
            project_code: Full identifier (e.g. ``"/MY_SPACE/MY_PROJECT"``)
                or short project code.

        Returns:
            The matching :class:`~pybis.project.Project` object.

        Raises:
            ValueError: If no project with the given code exists in this space.

        Example:
            >>> space.get_project("MY_PROJECT")
        """
        if is_identifier(project_code):
            return self.openbis.get_project(project_code)
        else:
            projects = self.openbis.get_projects(code=project_code, space=self.code)
            if len(projects) == 0:
                raise ValueError(
                    f"No project named {project_code} in space {self.code}"
                )

            return projects[0]

    def get_projects(self, **kwargs: Any) -> Things:
        """Return all projects in this space.

        Args:
            **kwargs: Additional search filters forwarded to
                ``openbis.get_projects()``.

        Returns:
            A :class:`~pybis.things.Things` container with matching projects.

        Example:
            >>> space.get_projects().df
        """
        return self.openbis.get_projects(space=self.code, **kwargs)

    def new_project(
        self,
        code: str,
        description: Optional[str] = None,
        **kwargs: Any,
    ) -> Things:
        """Create a new project in this space (not yet saved).

        Args:
            code: Project code (upper-case letters/digits/underscores).
            description: Optional human-readable description.
            **kwargs: Additional attributes forwarded to
                ``openbis.new_project()``.

        Returns:
            A new :class:`~pybis.project.Project` object ready to be saved.

        Example:
            >>> project = space.new_project(code="PILOT", description="Pilot study")
            >>> project.save()
        """
        return self.openbis.new_project(self.code, code, description, **kwargs)

    def get_experiments(self, **kwargs: Any) -> Things:
        """Return all experiments (collections) in this space.

        Args:
            **kwargs: Additional search filters forwarded to
                ``openbis.get_experiments()``.

        Returns:
            A :class:`~pybis.things.Things` container with matching
            experiments.

        Example:
            >>> space.get_experiments().df
        """
        return self.openbis.get_experiments(space=self.code, **kwargs)

    get_collections = get_experiments

    def get_experiment(self, experiment_code: str) -> Things:
        """Return a single experiment (collection) from this space by code or identifier.

        Args:
            experiment_code: Full identifier or short experiment code.

        Returns:
            The matching :class:`~pybis.experiment.Experiment` object.

        Raises:
            ValueError: If no experiment with the given code exists in this
                space.

        Example:
            >>> space.get_experiment("EXP_001")
        """
        if is_identifier(experiment_code):
            return self.openbis.get_experiment(experiment_code)
        else:
            experiments = self.openbis.get_experiments(
                code=experiment_code, space=self.code
            )
            if len(experiments) == 0:
                raise ValueError(
                    f"No project named {experiment_code} in space {self.code}"
                )

            return experiments[0]

    get_collection = get_experiments

    def new_sample(self, **kwargs: Any) -> Any:
        """Create a new sample (object) in this space (not yet saved).

        Args:
            **kwargs: Attributes forwarded to ``openbis.new_sample()``
                (e.g. ``type``, ``experiment``, ``props``).

        Returns:
            A new :class:`~pybis.sample.Sample` object ready to be saved.

        Example:
            >>> sample = space.new_sample(type="MY_TYPE", props={"name": "Test"})
            >>> sample.save()
        """
        return self.openbis.new_sample(space=self, **kwargs)

    new_object = new_sample

    def get_eln_url(self) -> str:
        """Return the direct URL to this space in the ELN-LIMS web UI.

        Returns:
            A URL string that opens the space page in a browser.

        Example:
            >>> print(space.get_eln_url())
            https://my-openbis-instance.org/webapp/eln-lims/?menuUniqueId=...
        """
        query = {"type": "SPACE", "id": self.code}
        return (
            f"{self.openbis.url}/webapp/eln-lims/?menuUniqueId={quote(str(query))}"
            f"&viewName=showSpacePage&viewData={self.code}"
        )
