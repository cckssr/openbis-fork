#
#   Copyright ETH 2026 Zürich, Scientific IT Services
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
"""Token storage and classification helpers for openBIS sessions.

pybis persists session tokens as ``~/.pybis/<hostname>.token`` and personal
access tokens (PATs) as ``~/.pybis/<hostname>/<hash>.pat``.  The helpers in
this module read and write that store; the session lifecycle itself
(``login`` / ``logout`` / ``set_token``) lives on :class:`pybis.Openbis`.
"""

from __future__ import annotations

import json
from pathlib import Path

from .api.rpc import JsonPayload, parse_jackson
from .utils import format_timestamp

PYBIS_FOLDER = Path.home() / ".pybis"
"""Directory where pybis persists tokens and PATs."""

CONFIG_FILENAME = ".pybis.json"
"""Per-project configuration file name (legacy)."""


def is_session_token(token: str) -> bool:
    """True if ``token`` is a session token (as opposed to a PAT)."""
    return not token.startswith("$pat")


def is_personal_access_token(token: str) -> bool:
    """True if ``token`` is a personal access token (``$pat-...``)."""
    return token.startswith("$pat")


def get_saved_tokens() -> dict[str, str]:
    """Read all session tokens stored on disk.

    Returns:
        Mapping of hostname to stored session token.
    """
    tokens: dict[str, str] = {}
    for filepath in PYBIS_FOLDER.glob("*.token"):
        if filepath.is_file():
            token = filepath.read_text()
            tokens[filepath.stem] = token
    return tokens


def get_token_for_hostname(
    hostname: str, session_token_needed: bool = True
) -> str | None:
    """Look up a stored token for a host (``~/.pybis/<hostname>.token``).

    Args:
        hostname: Host the token was stored for.
        session_token_needed: When True, only return session tokens and
            ignore stored PATs.

    Returns:
        The stored token, or None if there is none (of the required kind).
    """
    tokens = get_saved_tokens()
    if hostname in tokens:
        if session_token_needed:
            if is_session_token(tokens[hostname]):
                return tokens[hostname]
        else:
            return tokens[hostname]
    return None


def save_pats_to_disk(hostname: str, url: str, resp: JsonPayload) -> None:
    """Persist the personal access tokens of a search response to disk.

    Replaces all previously stored PATs for the host with the ones in
    ``resp`` (one ``<hash>.pat`` JSON file each).

    Args:
        hostname: Host the PATs belong to.
        url: Full server URL, stored inside each PAT file.
        resp: Raw ``searchPersonalAccessTokens`` JSON-RPC response.
    """
    pats = resp["objects"]
    parse_jackson(pats)
    path = PYBIS_FOLDER / hostname
    path.mkdir(exist_ok=True)
    for existing_file in path.glob("*.pat"):
        existing_file.unlink()

    for token in pats:
        data = {
            "url": url,
            "hostname": hostname,
            "owner": token["owner"]["userId"],
            "registrationDate": format_timestamp(token["owner"]["registrationDate"]),
            "validFromDate": format_timestamp(token["validFromDate"]),
            "validToDate": format_timestamp(token["validToDate"]),
            "sessionName": token["sessionName"],
            "permId": token["permId"]["permId"],
        }
        with open(path / (token["hash"] + ".pat"), "w", encoding="utf-8") as fh:
            fh.write(json.dumps(data, indent=4))


def get_saved_pats(
    hostname: str | None = None, session_name: str | None = None
) -> list[JsonPayload]:
    """Read all personal access tokens stored on disk.

    Args:
        hostname: Restrict to PATs of this host; None scans all hosts.
        session_name: Restrict to PATs with this session name.

    Returns:
        The stored PAT records (parsed JSON dicts).
    """
    if hostname is None:
        hostname = ""
    path = PYBIS_FOLDER / hostname
    tokens: list[JsonPayload] = []
    for filepath in path.rglob("*.pat"):
        if filepath.is_file():
            pat = json.loads(filepath.read_text())
            if session_name and pat["sessionName"] != session_name:
                continue
            tokens.append(pat)
    return tokens


__all__ = [
    "CONFIG_FILENAME",
    "PYBIS_FOLDER",
    "get_saved_pats",
    "get_saved_tokens",
    "get_token_for_hostname",
    "is_personal_access_token",
    "is_session_token",
    "save_pats_to_disk",
]
