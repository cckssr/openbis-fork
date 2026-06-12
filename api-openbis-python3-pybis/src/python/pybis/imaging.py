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
"""DTOs for the openBIS Imaging technology (see :class:`pybis.ImagingControl`).

These classes mirror the ``imaging.dto.*`` Java DTOs and serialize to the
JSON payloads the imaging service expects.  ``from_dict`` builds them back
from service responses.
"""

from __future__ import annotations

import abc
import base64
import json
import threading
from typing import Any, Optional


class AtomicIncrementer:
    """A thread-safe counter used to assign JSON ``@id`` references."""

    def __init__(self, value: int = 0) -> None:
        """Initialize the counter.

        Args:
            value: Starting value (default: 0).
        """
        self._value = int(value)
        self._lock = threading.Lock()

    def inc(self, d: int = 1) -> int:
        """Increment the counter and return the new value.

        Args:
            d: Increment step (default: 1).
        """
        with self._lock:
            self._value += int(d)
            return self._value


class AbstractImagingClass(metaclass=abc.ABCMeta):
    """Base of every imaging DTO: JSON serialization helpers."""

    def to_json(self) -> str:
        """Serialize this DTO (recursively) to an ``@id``-tagged JSON string."""
        c = AtomicIncrementer()

        def dictionary_creator(x: Any) -> dict[str, Any]:
            dictionary: dict[str, Any] = x.__dict__
            val = c.inc()
            dictionary["@id"] = val
            return dictionary

        return json.dumps(self, default=dictionary_creator, sort_keys=True, indent=4)

    def __str__(self) -> str:
        """Return the JSON representation."""
        return json.dumps(self.__dict__, default=lambda x: x.__dict__)

    def __repr__(self) -> str:
        """Return the JSON representation."""
        return json.dumps(self.__dict__, default=lambda x: x.__dict__)


class AbstractImagingRequest(AbstractImagingClass, metaclass=abc.ABCMeta):
    """Base of imaging DTOs that validate themselves on construction."""

    @abc.abstractmethod
    def _validate_data(self) -> None:
        return


class ImagingSemanticAnnotation(AbstractImagingClass):
    """An ontology reference attached to an imaging control or filter.

    Attributes:
        ontologyId (Optional[str]): Identifier of the ontology.
        ontologyVersion (Optional[str]): Version of the ontology.
        ontologyAnnotationId (Optional[str]): Identifier of the term within
            the ontology.
    """

    ontologyId: Optional[str]
    ontologyVersion: Optional[str]
    ontologyAnnotationId: Optional[str]

    def __init__(
        self,
        ontologyId: Optional[str] = None,
        ontologyVersion: Optional[str] = None,
        ontologyAnnotationId: Optional[str] = None,
    ) -> None:
        """Create a semantic annotation reference.

        Args:
            ontologyId: Identifier of the ontology.
            ontologyVersion: Version of the ontology.
            ontologyAnnotationId: Identifier of the term within the ontology.
        """
        self.__dict__["@type"] = "imaging.dto.ImagingSemanticAnnotation"
        self.ontologyId = ontologyId
        self.ontologyVersion = ontologyVersion
        self.ontologyAnnotationId = ontologyAnnotationId

    @classmethod
    def from_dict(
        cls, data: Optional[dict[str, Any]]
    ) -> Optional[ImagingSemanticAnnotation]:
        """Build an instance from a service-response dict (or None)."""
        if data is None:
            return None
        if "@id" in data:
            del data["@id"]
        semantic_annotation = cls(None, None, None)
        for prop in cls.__annotations__.keys():
            attribute = data.get(prop)
            semantic_annotation.__dict__[prop] = attribute
        return semantic_annotation


class ImagingDataSetFilter(AbstractImagingClass):
    """A named filter with parameters applied to an imaging preview.

    Attributes:
        name (str): Name of the filter.
        parameters (dict): Filter parameters.
    """

    name: str
    parameters: dict[str, Any]

    def __init__(self, name: str, parameters: Optional[dict[str, Any]] = None) -> None:
        """Create a filter reference.

        Args:
            name: Name of the filter.
            parameters: Filter parameters (default: empty).
        """
        self.__dict__["@type"] = "imaging.dto.ImagingDataSetFilter"
        self.name = name
        self.parameters = parameters if parameters is not None else dict()

    @classmethod
    def from_dict(
        cls, data: Optional[dict[str, Any]]
    ) -> Optional[ImagingDataSetFilter]:
        """Build an instance from a service-response dict (or None)."""
        if data is None:
            return None
        if "@id" in data:
            del data["@id"]
        imaging_filter = cls("", None)
        for prop in cls.__annotations__.keys():
            attribute = data.get(prop)
            imaging_filter.__dict__[prop] = attribute
        return imaging_filter


class ImagingDataSetPreview(AbstractImagingRequest):
    """One preview image of an imaging dataset.

    Attributes:
        config (dict): Adaptor-specific preview parameters.
        format (str): Image format, e.g. ``"png"``.
        bytes (Optional[str]): Base64-encoded image bytes.
        width (int): Image width in pixels.
        height (int): Image height in pixels.
        index (int): Preview index within the image.
        show (bool): Whether the preview is shown in the gallery view.
        metadata (dict): Arbitrary metadata.
        comment (str): Free-text comment.
        tags (list): Tags assigned to the preview.
        filterConfig (list): Filters applied to the preview.
    """

    config: dict[str, Any]
    format: str
    bytes: Optional[str]
    width: int
    height: int
    index: int
    show: bool
    metadata: dict[str, Any]
    comment: str
    tags: list[Any]
    filterConfig: list[Any]

    def __init__(
        self,
        preview_format: str,
        config: Optional[dict[str, Any]] = None,
        metadata: Optional[dict[str, Any]] = None,
        index: int = 0,
        comment: str = "",
        tags: Optional[list[Any]] = None,
        filterConfig: Optional[list[Any]] = None,
    ) -> None:
        """Create a preview request.

        Args:
            preview_format: Image format, e.g. ``"png"``.
            config: Adaptor-specific preview parameters.
            metadata: Arbitrary metadata.
            index: Preview index within the image (default: 0).
            comment: Free-text comment.
            tags: Tags assigned to the preview.
            filterConfig: Filters applied to the preview.
        """
        self.__dict__["@type"] = "imaging.dto.ImagingDataSetPreview"
        self.bytes = None
        self.format = preview_format
        self.config = config if config is not None else dict()
        self.metadata = metadata if metadata is not None else dict()
        self.index = index
        self.comment = comment
        self.tags = tags if tags is not None else []
        self.filterConfig = filterConfig if filterConfig is not None else []
        self._validate_data()

    def set_preview_image_bytes(self, width: int, height: int, bytes: str) -> None:
        """Set the (base64-encoded) image content and its dimensions.

        Args:
            width: Image width in pixels.
            height: Image height in pixels.
            bytes: Base64-encoded image bytes.
        """
        self.width = width
        self.height = height
        self.bytes = bytes

    def _validate_data(self) -> None:
        assert self.format is not None, "Format can not be null"

    def save_to_file(self, file_path: str) -> None:
        """Decode the preview bytes and write them to ``file_path``.

        Args:
            file_path: Destination path of the image file.
        """
        assert self.bytes is not None, "There is no image information!"
        img_data = bytearray(self.bytes, encoding="utf-8")
        with open(file_path, "wb") as fh:
            fh.write(base64.decodebytes(img_data))

    @classmethod
    def from_dict(
        cls, data: Optional[dict[str, Any]]
    ) -> Optional[ImagingDataSetPreview]:
        """Build an instance from a service-response dict (or None)."""
        if data is None:
            return None
        if "@id" in data:
            del data["@id"]
        preview = cls("", None, None)
        for prop in cls.__annotations__.keys():
            attribute = data.get(prop)
            preview.__dict__[prop] = attribute
        return preview


class ImagingDataSetExportConfig(AbstractImagingClass):
    """Parameters of an imaging export (archive/image format, resolution).

    Attributes:
        archiveFormat (str): Archive format, e.g. ``"zip"``.
        imageFormat (str): Image format, e.g. ``"png"``.
        resolution (str): Export resolution (default: ``"original"``).
        include (list): Content kinds to include (``"IMAGE"``,
            ``"RAW_DATA"``).
    """

    archiveFormat: str
    imageFormat: str
    resolution: str
    include: list[str]

    def __init__(
        self,
        archive_format: str,
        image_format: str,
        resolution: Optional[str],
        include: Optional[list[str]] = None,
    ) -> None:
        """Create an export configuration.

        Args:
            archive_format: Archive format, e.g. ``"zip"``.
            image_format: Image format, e.g. ``"png"``.
            resolution: Export resolution (``None`` means ``"original"``).
            include: Content kinds to include (default: image and raw data).
        """
        if include is None:
            include = ["IMAGE", "RAW_DATA"]
        self.__dict__["@type"] = "imaging.dto.ImagingDataSetExportConfig"
        self.imageFormat = image_format
        self.archiveFormat = archive_format
        if resolution is None:
            resolution = "original"
        self.resolution = resolution
        self.include = include
        self._validate_data()

    def _validate_data(self) -> None:
        assert self.imageFormat is not None, "image format can not be null"
        assert self.archiveFormat is not None, "image format can not be null"

    @classmethod
    def from_dict(
        cls, data: Optional[dict[str, Any]]
    ) -> Optional[ImagingDataSetExportConfig]:
        """Build an instance from a service-response dict (or None)."""
        if data is None:
            return None
        if "@id" in data:
            del data["@id"]
        preview = cls("", "", None)
        for prop in cls.__annotations__.keys():
            attribute = data.get(prop)
            preview.__dict__[prop] = attribute
        return preview


class ImagingDataSetExport(AbstractImagingRequest):
    """An export request for a single imaging dataset.

    Attributes:
        config (ImagingDataSetExportConfig): Export parameters.
        metadata (dict): Arbitrary metadata.
    """

    config: ImagingDataSetExportConfig
    metadata: dict[str, Any]

    def __init__(
        self,
        config: ImagingDataSetExportConfig,
        metadata: Optional[dict[str, Any]] = None,
    ) -> None:
        """Create an export request.

        Args:
            config: Export parameters.
            metadata: Arbitrary metadata.
        """
        self.__dict__["@type"] = "imaging.dto.ImagingDataSetExport"
        self.config = config
        self.metadata = metadata if metadata is not None else dict()
        self._validate_data()

    def _validate_data(self) -> None:
        assert self.config is not None, "Config can not be null"


class ImagingDataSetMultiExport(AbstractImagingRequest):
    """An export request for one image/preview of one dataset.

    Attributes:
        permId (str): PermId of the dataset.
        imageIndex (int): Index of the image to export.
        previewIndex (int): Index of the preview to export.
        config (ImagingDataSetExportConfig): Export parameters.
        metadata (dict): Arbitrary metadata.
    """

    permId: str
    imageIndex: int
    previewIndex: int
    config: ImagingDataSetExportConfig
    metadata: dict[str, Any]

    def __init__(
        self,
        permId: str,
        imageIndex: int,
        previewIndex: int,
        config: ImagingDataSetExportConfig,
        metadata: Optional[dict[str, Any]] = None,
    ) -> None:
        """Create a multi-export entry.

        Args:
            permId: PermId of the dataset.
            imageIndex: Index of the image to export.
            previewIndex: Index of the preview to export.
            config: Export parameters.
            metadata: Arbitrary metadata.
        """
        self.__dict__["@type"] = "imaging.dto.ImagingDataSetMultiExport"
        self.permId = permId
        self.imageIndex = imageIndex
        self.previewIndex = previewIndex
        self.config = config
        self.metadata = metadata if metadata is not None else dict()
        self._validate_data()

    def _validate_data(self) -> None:
        assert self.permId is not None, "PermId can not be null"
        assert self.imageIndex is not None, "imageIndex can not be null"
        assert self.previewIndex is not None, "previewIndex can not be null"


class ImagingDataSetControlVisibility(AbstractImagingClass):
    """Visibility rule: which values of one control reveal another.

    Attributes:
        label (str): Label of the controlling input.
        values (list): Values of the controlling input that activate this
            rule.
        range (list): Value range shown when the rule is active.
        unit (Optional[str]): Unit of the range values.
    """

    label: str
    values: list[Any]
    range: list[Any]
    unit: Optional[str]

    def __init__(
        self,
        label: str,
        values: list[Any],
        values_range: list[Any],
        unit: Optional[str] = None,
    ) -> None:
        """Create a visibility rule.

        Args:
            label: Label of the controlling input.
            values: Values of the controlling input that activate this rule.
            values_range: Value range shown when the rule is active.
            unit: Unit of the range values.
        """
        self.__dict__["@type"] = "imaging.dto.ImagingDataSetControlVisibility"
        self.label = label
        self.values = values
        self.range = values_range
        self.unit = unit

    @classmethod
    def from_dict(
        cls, data: Optional[dict[str, Any]]
    ) -> Optional[ImagingDataSetControlVisibility]:
        """Build an instance from a service-response dict (or None)."""
        if data is None:
            return None
        if "@id" in data:
            del data["@id"]
        control = cls("", [], [], None)
        for prop in cls.__annotations__.keys():
            attribute = data.get(prop)
            control.__dict__[prop] = attribute
        return control


class ImagingDataSetControl(AbstractImagingClass):
    """One UI control (slider, dropdown, …) of an imaging adaptor.

    Attributes:
        label (str): Display label of the control.
        section (Optional[str]): UI section the control belongs to.
        type (str): Control type: ``"Slider"``, ``"Range"``, ``"Dropdown"``,
            ``"Colormap"``, etc.
        values (list): Choices for dropdown-like controls.
        unit (Optional[str]): Unit of the control values.
        range (list): Value range for slider-like controls.
        multiselect (bool): Whether multiple values may be selected.
        playable (bool): Whether the control can be animated.
        speeds (list): Playback speeds for playable controls.
        visibility (list): Conditional visibility rules.
        metadata (dict): Arbitrary metadata.
        semanticAnnotation (Optional[ImagingSemanticAnnotation]): Ontology
            reference of the control.
    """

    label: str
    section: Optional[str]
    type: str
    values: Optional[list[Any]]
    unit: Optional[str]
    range: Optional[list[Any]]
    multiselect: bool
    playable: bool
    speeds: Optional[list[int]]
    visibility: Optional[list[Any]]
    metadata: Optional[dict[str, Any]]
    semanticAnnotation: Optional[ImagingSemanticAnnotation]

    def __init__(
        self,
        label: str,
        control_type: str,
        section: Optional[str] = None,
        values: Optional[list[Any]] = None,
        unit: Optional[str] = None,
        values_range: Optional[list[Any]] = None,
        multiselect: Optional[bool] = None,
        playable: bool = False,
        speeds: Optional[list[int]] = None,
        visibility: Optional[list[Any]] = None,
        metadata: Optional[dict[str, Any]] = None,
        semanticAnnotation: Optional[ImagingSemanticAnnotation] = None,
    ) -> None:
        """Create a control description.

        Args:
            label: Display label of the control.
            control_type: Control type: ``"Slider"``, ``"Range"``,
                ``"Dropdown"``, ``"Colormap"``, etc.
            section: UI section the control belongs to.
            values: Choices for dropdown-like controls.
            unit: Unit of the control values.
            values_range: Value range for slider-like controls.
            multiselect: Whether multiple values may be selected
                (dropdown-like controls only; default: False).
            playable: Whether the control can be animated.
            speeds: Playback speeds for playable controls.
            visibility: Conditional visibility rules.
            metadata: Arbitrary metadata.
            semanticAnnotation: Ontology reference of the control.
        """
        self.__dict__["@type"] = "imaging.dto.ImagingDataSetControl"
        self.label = label
        self.type = control_type
        self.section = section
        self.unit = unit
        if control_type.lower() in ["slider", "range"]:
            self.range = values_range
        elif control_type.lower() in ["dropdown", "colormap"]:
            self.values = values
            if multiselect is None:
                self.multiselect = False
            else:
                self.multiselect = multiselect

        if playable is True:
            self.playable = True
            self.speeds = speeds
        self.visibility = visibility
        self.metadata = metadata
        self.semanticAnnotation = semanticAnnotation

    @classmethod
    def from_dict(
        cls, data: Optional[dict[str, Any]]
    ) -> Optional[ImagingDataSetControl]:
        """Build an instance from a service-response dict (or None)."""
        if data is None:
            return None
        if "@id" in data:
            del data["@id"]
        control = cls("", "", None, None)
        for prop in cls.__annotations__.keys():
            attribute = data.get(prop)
            if attribute is not None:
                if prop == "visibility":
                    attribute = [
                        ImagingDataSetControlVisibility.from_dict(visibility)
                        for visibility in attribute
                    ]
                elif prop == "semanticAnnotation":
                    attribute = ImagingSemanticAnnotation.from_dict(attribute)
            control.__dict__[prop] = attribute
        return control


class ImagingDataSetConfig(AbstractImagingClass):
    """Adaptor configuration of an imaging dataset.

    Attributes:
        adaptor (str): Name of the imaging adaptor class.
        version (float): Configuration format version.
        speeds (list): Playback speeds when the dataset is playable.
        resolutions (list): Offered preview resolutions, e.g.
            ``["original", "200x200"]``.
        playable (bool): Whether previews can be animated.
        exports (list): Export controls.
        inputs (list): Preview parameter controls.
        metadata (dict): Arbitrary metadata.
        filters (dict): Filter controls keyed by filter name.
        filterSemanticAnnotation (dict): Ontology references keyed by
            filter name.
    """

    adaptor: str
    version: float
    speeds: Optional[list[int]]
    resolutions: Optional[list[str]]
    playable: bool
    exports: Optional[list[Any]]
    inputs: Optional[list[Any]]
    metadata: dict[str, Any]
    filters: dict[str, Any]
    filterSemanticAnnotation: dict[str, Any]

    def __init__(
        self,
        adaptor: str,
        version: float,
        resolutions: Optional[list[str]],
        playable: bool,
        speeds: Optional[list[int]] = None,
        exports: Optional[list[Any]] = None,
        inputs: Optional[list[Any]] = None,
        metadata: Optional[dict[str, Any]] = None,
        filters: Optional[dict[str, Any]] = None,
        filterSemanticAnnotation: Optional[dict[str, Any]] = None,
    ) -> None:
        """Create an adaptor configuration.

        Args:
            adaptor: Name of the imaging adaptor class.
            version: Configuration format version.
            resolutions: Offered preview resolutions.
            playable: Whether previews can be animated.
            speeds: Playback speeds (playable datasets only).
            exports: Export controls.
            inputs: Preview parameter controls.
            metadata: Arbitrary metadata.
            filters: Filter controls keyed by filter name.
            filterSemanticAnnotation: Ontology references keyed by filter
                name.
        """
        self.__dict__["@type"] = "imaging.dto.ImagingDataSetConfig"
        self.adaptor = adaptor
        self.version = version
        self.resolutions = resolutions
        self.playable = playable
        if playable:
            self.speeds = speeds
        self.exports = exports
        self.inputs = inputs
        self.metadata = metadata if metadata is not None else dict()
        self.filters = filters if filters is not None else dict()
        self.filterSemanticAnnotation = (
            filterSemanticAnnotation if filterSemanticAnnotation is not None else dict()
        )

    @classmethod
    def from_dict(
        cls, data: Optional[dict[str, Any]]
    ) -> Optional[ImagingDataSetConfig]:
        """Build an instance from a service-response dict (or None)."""
        if data is None:
            return None
        if "@id" in data:
            del data["@id"]
        config = cls("", 0.0, None, False)
        for prop in cls.__annotations__.keys():
            attribute = data.get(prop)
            if attribute is not None:
                if prop in ["exports", "inputs"]:
                    attribute = [
                        ImagingDataSetControl.from_dict(control)
                        for control in attribute
                    ]
                elif prop in ["filters"]:
                    filters: dict[str, Any] = {}
                    for attr in attribute:
                        filters[attr] = [
                            ImagingDataSetControl.from_dict(control)
                            for control in attribute[attr]
                        ]
                    attribute = filters
                elif prop in ["filterSemanticAnnotation"]:
                    filter_semantic_annotation: dict[str, Any] = {}
                    for attr in attribute:
                        filter_semantic_annotation[attr] = (
                            ImagingSemanticAnnotation.from_dict(attribute[attr])
                        )
                    attribute = filter_semantic_annotation
            config.__dict__[prop] = attribute
        return config


class ImagingDataSetImage(AbstractImagingClass):
    """One image of an imaging dataset, with its previews.

    Attributes:
        config (ImagingDataSetConfig): Adaptor configuration.
        previews (list): The image's previews
            (:class:`ImagingDataSetPreview`).
        imageConfig (dict): Adaptor-specific image parameters.
        index (int): Image index within the dataset.
        metadata (dict): Arbitrary metadata.
    """

    config: ImagingDataSetConfig
    previews: list[Any]
    imageConfig: dict[str, Any]
    index: int
    metadata: dict[str, Any]

    def __init__(
        self,
        config: ImagingDataSetConfig,
        imageConfig: Optional[dict[str, Any]] = None,
        previews: Optional[list[Any]] = None,
        metadata: Optional[dict[str, Any]] = None,
        index: int = 0,
    ) -> None:
        """Create an image description.

        Args:
            config: Adaptor configuration (must not be None).
            imageConfig: Adaptor-specific image parameters.
            previews: The image's previews (default: one ``"png"`` preview).
            metadata: Arbitrary metadata.
            index: Image index within the dataset (default: 0).
        """
        self.__dict__["@type"] = "imaging.dto.ImagingDataSetImage"
        assert config is not None, "Config must not be None!"
        self.config = config
        self.imageConfig = imageConfig if imageConfig is not None else dict()
        self.previews = (
            previews if previews is not None else [ImagingDataSetPreview("png")]
        )
        self.metadata = metadata if metadata is not None else dict()
        self.index = index if index is not None else 0
        assert isinstance(self.previews, list), "Previews must be a list!"

    def add_preview(self, preview: ImagingDataSetPreview) -> None:
        """Append a preview to this image.

        Args:
            preview: The preview to add.
        """
        self.previews += [preview]

    @classmethod
    def from_dict(cls, data: Optional[dict[str, Any]]) -> Optional[ImagingDataSetImage]:
        """Build an instance from a service-response dict (or None)."""
        if data is None:
            return None
        if "@id" in data:
            del data["@id"]
        config = ImagingDataSetConfig.from_dict(data.get("config"))
        assert config is not None, "Config must not be None!"
        image = cls(config, None, None, None)
        for prop in cls.__annotations__.keys():
            attribute = data.get(prop)
            if prop == "previews" and attribute is not None:
                attribute = [
                    ImagingDataSetPreview.from_dict(preview) for preview in attribute
                ]
            if prop not in ["config"]:
                image.__dict__[prop] = attribute
        return image


class ImagingDataSetPropertyConfig(AbstractImagingClass):
    """The top-level imaging property value: all images of a dataset.

    Attributes:
        images (list): The dataset's images (:class:`ImagingDataSetImage`).
        metadata (dict): Arbitrary metadata.
    """

    images: list[Any]
    metadata: dict[str, Any]

    def __init__(
        self,
        images: Optional[list[Any]],
        metadata: Optional[dict[str, Any]] = None,
    ) -> None:
        """Create an imaging property value.

        Args:
            images: The dataset's images.
            metadata: Arbitrary metadata.
        """
        self.__dict__["@type"] = "imaging.dto.ImagingDataSetPropertyConfig"
        self.images = images if images is not None else []
        self.metadata = metadata if metadata is not None else dict()

    @classmethod
    def from_dict(cls, data: dict[str, Any]) -> ImagingDataSetPropertyConfig:
        """Build an instance from a service-response dict.

        Args:
            data: The raw property value dict (must be non-empty).
        """
        assert data is not None and any(data), "There is no property config found!"
        if "@id" in data:
            del data["@id"]
        attr = data.get("images")
        images = (
            [ImagingDataSetImage.from_dict(image) for image in attr]
            if attr is not None
            else None
        )
        metadata = data.get("metadata")
        return cls(images, metadata)

    def add_image(self, image: ImagingDataSetImage) -> None:
        """Append an image to this property value.

        Args:
            image: The image to add.
        """
        if self.images is None:
            self.images = []
        self.images += [image]
