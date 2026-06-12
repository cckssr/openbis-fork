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
"""External data management systems (link-data targets)."""

from __future__ import annotations

from collections.abc import Sequence
from typing import TYPE_CHECKING, Any, cast

from ..api.rpc import parse_jackson
from ..definitions import get_fetchoption_for_entity, get_type_for_entity
from ..exceptions import NotFoundError
from ..types.results import SearchResult
from ..utils import extract_permid
from ._mixin import ClientApiMixin

if TYPE_CHECKING:
    import pandas as pd


class ExternalDMS:
    """An external data management system registered in openBIS."""

    def __init__(self, openbis_obj: Any, data: Any = None, **kwargs: Any) -> None:
        """Wrap one external DMS record.

        Args:
            openbis_obj: The Openbis connection instance.
            data: The raw server response dict for this DMS.
            **kwargs: Extra attributes to set on the instance.
        """
        self.__dict__["openbis"] = openbis_obj
        if data is not None:
            self.__dict__["data"] = data
        if kwargs is not None:
            for key in kwargs:
                setattr(self, key, kwargs[key])

    def __getattr__(self, name: str) -> Any:
        """Delegate attribute access to the raw data dict."""
        return self.__dict__["data"].get(name)

    def __dir__(self) -> list[str]:
        """Return the attributes shown by Jupyter tab-completion."""
        return ["code", "label", "urlTemplate", "address", "addressType", "openbis"]

    def __str__(self) -> str:
        """Return the DMS code."""
        return cast(str, self.data.get("code", None))


def _external_dms_df(items: Sequence[Any]) -> "pd.DataFrame":
    """Build the notebook DataFrame view for a page of external DMS."""
    from pandas import DataFrame

    attrs = ["code", "label", "address", "addressType", "urlTemplate", "openbis"]
    if not items:
        return DataFrame(columns=attrs)
    df = DataFrame([item.data for item in items])
    if "permId" in df:
        df["permId"] = df["permId"].map(extract_permid)
    return cast("pd.DataFrame", df[df.columns.intersection(attrs)])


class _ExternalDmsApi(ClientApiMixin):
    """External-DMS methods of the Openbis client."""

    def get_external_data_management_system(self, perm_id: str) -> ExternalDMS | None:
        """Get a single external DMS by permId, or None if it does not exist.

        Args:
            perm_id: The permId of the external DMS.
        """
        request = {
            "method": "getExternalDataManagementSystems",
            "params": [
                self.token,
                [
                    {
                        "@type": "as.dto.externaldms.id.ExternalDmsPermId",
                        "permId": perm_id,
                    }
                ],
                {"@type": "as.dto.externaldms.fetchoptions.ExternalDmsFetchOptions"},
            ],
        }
        resp = self._post_request(self.as_v3, request)
        parse_jackson(resp)
        for ident in resp:
            return ExternalDMS(self, resp[ident])
        return None

    def get_external_data_management_system_or_raise(self, perm_id: str) -> ExternalDMS:
        """Get a single external DMS by permId; raise if it does not exist.

        Raises:
            NotFoundError: No external DMS exists with this permId.
        """
        dms = self.get_external_data_management_system(perm_id)
        if dms is None:
            raise NotFoundError("external data management system", perm_id)
        return dms

    def search_external_data_management_systems(
        self,
        *,
        count: int = 25,
        start_with: int = 0,
    ) -> SearchResult[ExternalDMS]:
        """Search for external data management systems.

        Args:
            count: Maximum number of results to return (default: 25).
            start_with: Pagination offset (default: 0).
        """
        criteria = get_type_for_entity("externalDms", "search")
        fetchopts = get_fetchoption_for_entity("externalDms")
        fetchopts["from"] = start_with
        fetchopts["count"] = count
        request = {
            "method": "searchExternalDataManagementSystems",
            "params": [self.token, criteria, fetchopts],
        }
        resp = self._post_request(self.as_v3, request)
        parse_jackson(resp)
        items = [ExternalDMS(self, data) for data in resp["objects"]]
        return SearchResult(
            items, int(resp.get("totalCount", len(items))), _external_dms_df
        )

    def create_external_data_management_system(
        self,
        code: str,
        label: str,
        address: str,
        *,
        address_type: str = "FILE_SYSTEM",
    ) -> ExternalDMS:
        """Create an external DMS on the server and return it.

        Args:
            code: An openBIS code for the external DMS.
            label: A human-readable label.
            address: The address for accessing the external DMS, e.g. a URL.
            address_type: One of ``OPENBIS``, ``URL``, or ``FILE_SYSTEM``.
        """
        request = {
            "method": "createExternalDataManagementSystems",
            "params": [
                self.token,
                [
                    {
                        "code": code,
                        "label": label,
                        "addressType": address_type,
                        "address": address,
                        "@type": "as.dto.externaldms.create.ExternalDmsCreation",
                    }
                ],
            ],
        }
        resp = cast("list[dict[str, Any]]", self._post_request(self.as_v3, request))
        return self.get_external_data_management_system_or_raise(resp[0]["permId"])


__all__ = ["ExternalDMS", "_ExternalDmsApi"]
