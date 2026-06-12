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
"""Transitional re-export of :mod:`pybis.client`.

Deprecated: the ``Openbis`` client moved to :mod:`pybis.client`.  Import
``from pybis import Openbis`` (preferred) or ``from pybis.client import
Openbis`` instead of ``pybis.pybis``.  This module disappears at the end of
the v2 refactor.
"""

from .client import *  # noqa: F401,F403
from .client import Openbis, SessionInformation  # noqa: F401
from .entities.pat import PersonalAccessToken  # noqa: F401
from .entities.server import ServerInformation  # noqa: F401
from .auth import (  # noqa: F401
    CONFIG_FILENAME,
    PYBIS_FOLDER,
    get_saved_pats,
    get_saved_tokens,
    get_token_for_hostname,
    is_personal_access_token,
    is_session_token,
    save_pats_to_disk,
)
from .api.rpc import type_for_id as _type_for_id  # noqa: F401
