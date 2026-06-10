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
"""File attachment support for openBIS entities."""

import base64
import os
from typing import Optional


class Attachment:
    """A local file prepared for upload as an openBIS attachment.

    Attachments wrap a local file path and encode the binary content as
    Base64 so it can be sent to the openBIS V3 API.  Use
    ``entity.add_attachment(att)`` followed by ``entity.save()`` to persist
    the attachment against a :class:`~pybis.sample.Sample`,
    :class:`~pybis.experiment.Experiment`, or
    :class:`~pybis.project.Project`.

    Attributes:
        fileName (str): Absolute or relative path to the local file.
        title (Optional[str]): Human-readable title shown in the openBIS UI.
        description (Optional[str]): Optional free-text description.

    Raises:
        ValueError: If ``filename`` does not point to an existing file.

    Example:
        >>> att = Attachment("report.pdf", title="Final Report")
        >>> sample.add_attachment(att)
        >>> sample.save()
    """

    fileName: str
    title: Optional[str]
    description: Optional[str]

    def __init__(
        self,
        filename: str,
        title: Optional[str] = None,
        description: Optional[str] = None,
    ) -> None:
        """Create an Attachment from a local file.

        Args:
            filename: Path to the local file.  The file must exist at the
                time of construction.
            title: Optional display title shown in the openBIS UI.
            description: Optional free-text description of the attachment.

        Raises:
            ValueError: If ``filename`` does not exist on disk.
        """
        if not os.path.exists(filename):
            raise ValueError(f"File not found: {filename}")
        self.fileName = filename
        self.title = title
        self.description = description

    def get_data_short(self) -> dict:
        """Return a lightweight metadata dict (no file content).

        Used internally when listing existing attachments where the binary
        payload is not needed.

        Returns:
            A dict with keys ``fileName``, ``title``, and ``description``.
        """
        return {
            "fileName": self.fileName,
            "title": self.title,
            "description": self.description,
        }

    def get_data(self) -> dict:
        """Return a full attachment dict with Base64-encoded file content.

        Reads ``fileName`` from disk, encodes it as Base64, and returns a
        dictionary formatted as an openBIS ``AttachmentCreation`` DTO ready
        for the V3 API.

        Returns:
            A dict with keys ``fileName``, ``title``, ``description``,
            ``content`` (Base64-encoded string), and ``@type``.
        """
        with open(self.fileName, "rb") as att:
            content = att.read()
            contentb64 = base64.b64encode(content).decode()
        return {
            "fileName": self.fileName,
            "title": self.title,
            "description": self.description,
            "content": contentb64,
            "@type": "as.dto.attachment.create.AttachmentCreation",
        }
