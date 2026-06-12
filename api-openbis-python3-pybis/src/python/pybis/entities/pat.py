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
"""Personal access tokens (PATs)."""

from __future__ import annotations

from collections.abc import Sequence
from datetime import datetime
from typing import TYPE_CHECKING, Any, cast

from dateutil.relativedelta import relativedelta

from ..api.rpc import parse_jackson, type_for_id
from ..auth import get_token_for_hostname, is_session_token, save_pats_to_disk
from ..definitions import get_fetchoption_for_entity
from ..exceptions import AuthenticationError, FeatureNotAvailableError, NotFoundError
from ..openbis_object import OpenBisObject
from ..types.results import SearchResult
from ..utils import extract_permid, extract_person, format_timestamp
from ._mixin import ClientApiMixin

if TYPE_CHECKING:
    import pandas as pd

_PAT_DATE_FORMAT = "%Y-%m-%d %H:%M:%S"


class PersonalAccessToken(
    OpenBisObject,
    entity="personalAccessToken",
    single_item_method_name="get_personal_access_token",
):
    """A long-lived token authenticating one user session name."""

    def renew(
        self,
        valid_from: datetime | None = None,
        valid_to: datetime | None = None,
    ) -> "PersonalAccessToken":
        """Create a new PAT based on this one, regardless of its validity.

        The same session name and validity period are reused, starting from
        now.

        Args:
            valid_from: Begin of the validity period (default: now).
            valid_to: End of the validity period (default: valid_from plus
                this token's validity period).
        """
        if not valid_from:
            valid_from = datetime.now()
        if not valid_to:
            valid_from_orig = datetime.strptime(self.validFromDate, _PAT_DATE_FORMAT)
            valid_to_orig = datetime.strptime(self.validToDate, _PAT_DATE_FORMAT)
            days_delta = abs(valid_from_orig - valid_to_orig).days
            valid_to = valid_from + relativedelta(days=days_delta)
        return cast(
            "PersonalAccessToken",
            self.openbis.get_or_create_personal_access_token(
                session_name=self.sessionName,
                valid_from=valid_from,
                valid_to=valid_to,
                force=True,
            ),
        )


def _pats_df(items: Sequence[Any]) -> "pd.DataFrame":
    """Build the notebook DataFrame view for a page of PATs."""
    from pandas import DataFrame

    attrs = [
        "permId",
        "hash",
        "sessionName",
        "validFromDate",
        "validToDate",
        "accessDate",
        "owner",
        "registrator",
        "modifier",
        "registrationDate",
        "modificationDate",
    ]
    if not items:
        return DataFrame(columns=attrs)
    df = DataFrame([item.data for item in items])
    if "permId" in df:
        df["permId"] = df["permId"].map(extract_permid)
    for date in [
        "validFromDate",
        "validToDate",
        "accessDate",
        "registrationDate",
        "modificationDate",
    ]:
        if date in df:
            df[date] = df[date].map(format_timestamp)
    for person in ["owner", "registrator", "modifier"]:
        if person in df:
            df[person] = df[person].map(extract_person)
    return cast("pd.DataFrame", df[df.columns.intersection(attrs)])


class _PersonalAccessTokenApi(ClientApiMixin):
    """Personal-access-token methods of the Openbis client."""

    def get_personal_access_token(self, perm_id: str) -> PersonalAccessToken | None:
        """Get a single PAT by permId, or None if it does not exist.

        To fetch the latest PAT for a session name (or create one), use
        :meth:`get_or_create_personal_access_token` instead.

        Args:
            perm_id: The id of the PAT.
        """
        fetchopts = get_fetchoption_for_entity("personalAccessToken")
        for person in ["owner", "registrator", "modifier"]:
            fetchopts[person] = get_fetchoption_for_entity(person)
        request = {
            "method": "getPersonalAccessTokens",
            "params": [
                self.token,
                [type_for_id(perm_id, "personalAccessToken")],
                fetchopts,
            ],
        }
        resp = self._post_request(self.as_v3, request)
        parse_jackson(resp)
        for ident in resp:
            return PersonalAccessToken(openbis_obj=self, data=resp[ident])
        return None

    def get_personal_access_token_or_raise(self, perm_id: str) -> PersonalAccessToken:
        """Get a single PAT by permId; raise if it does not exist.

        Raises:
            NotFoundError: No PAT exists with this permId.
        """
        pat = self.get_personal_access_token(perm_id)
        if pat is None:
            raise NotFoundError("personal access token", perm_id)
        return pat

    def search_personal_access_tokens(
        self,
        *,
        session_name: str | None = None,
        count: int = 25,
        start_with: int = 0,
        save_to_disk: bool = False,
    ) -> SearchResult[PersonalAccessToken]:
        """Search for personal access tokens.

        Args:
            session_name: Filter by session-name prefix.
            count: Maximum number of results to return (default: 25).
            start_with: Pagination offset (default: 0).
            save_to_disk: Also store the returned PATs in the local pybis
                token store (``~/.pybis``).

        Raises:
            FeatureNotAvailableError: The server does not support PATs.
        """
        criteria: dict[str, Any] = {
            "@type": "as.dto.pat.search.PersonalAccessTokenSearchCriteria",
            "operator": "AND",
            "criteria": [],
        }
        if session_name:
            criteria["criteria"].append(
                {
                    "fieldName": "sessionName",
                    "fieldType": "ATTRIBUTE",
                    "fieldValue": {
                        "value": session_name,
                        "@type": "as.dto.common.search.StringStartsWithValue",
                    },
                    "@type": (
                        "as.dto.pat.search.PersonalAccessTokenSessionNameSearchCriteria"
                    ),
                }
            )
        fetchopts = get_fetchoption_for_entity("personalAccessToken")
        fetchopts["from"] = start_with
        fetchopts["count"] = count
        for person in ["owner", "registrator", "modifier"]:
            fetchopts[person] = get_fetchoption_for_entity(person)
        request = {
            "method": "searchPersonalAccessTokens",
            "params": [self.token, criteria, fetchopts],
        }
        try:
            resp = self._post_request(self.as_v3, request)
        except ValueError as exc:
            raise FeatureNotAvailableError(
                "personal access tokens are not supported by this openBIS"
                " instance; please upgrade your server and activate them"
            ) from exc
        if save_to_disk:
            save_pats_to_disk(hostname=self.hostname or "", url=self.url, resp=resp)
        parse_jackson(resp)
        items = [
            PersonalAccessToken(openbis_obj=self, data=data) for data in resp["objects"]
        ]
        return SearchResult(items, int(resp.get("totalCount", len(items))), _pats_df)

    def get_or_create_personal_access_token(
        self,
        session_name: str,
        *,
        valid_from: datetime | None = None,
        valid_to: datetime | None = None,
        force: bool = False,
    ) -> PersonalAccessToken:
        """Create a personal access token, or return a matching existing one.

        If a PAT with the given session name already exists, belongs to the
        current user, and its expiry is not within the server's warning
        period, the existing PAT is returned instead.

        Args:
            session_name: Session name to identify the PAT.
            valid_from: Start of the validity period (default: now).
            valid_to: End of the validity period (default: the maximum
                validity period configured on the server).
            force: Create a new PAT even when a valid one exists.

        Raises:
            AuthenticationError: No valid session token is available (PATs
                can only be managed with a session token).
            FeatureNotAvailableError: The server does not support PATs.
        """
        server_info = self.get_server_information()
        session_token: str | None = self.token
        if not is_session_token(session_token or ""):
            session_token = get_token_for_hostname(
                self.hostname or "", session_token_needed=True
            )
        if not session_token or not self.is_token_valid(session_token):
            raise AuthenticationError(
                "you need a session token to create a personal access token"
            )
        if valid_from is None:
            valid_from = datetime.now()

        if not force:
            warning_period = relativedelta(
                seconds=server_info.personal_access_tokens_validity_warning_period
            )
            user = self._get_username()
            for existing_pat in self.search_personal_access_tokens(
                session_name=session_name
            ):
                expiry = datetime.strptime(existing_pat.validToDate, _PAT_DATE_FORMAT)
                if expiry > datetime.now() + warning_period and (
                    user == existing_pat.owner
                ):
                    return existing_pat

        if valid_to is None:
            valid_to = datetime.now() + relativedelta(
                seconds=server_info.personal_access_tokens_max_validity_period
            )
        request = {
            "method": "createPersonalAccessTokens",
            "params": [
                self.token,
                [
                    {
                        "@type": "as.dto.pat.create.PersonalAccessTokenCreation",
                        "sessionName": session_name,
                        "validFromDate": int(valid_from.timestamp() * 1000),
                        "validToDate": int(valid_to.timestamp() * 1000),
                    }
                ],
            ],
        }
        try:
            created = cast(
                "list[dict[str, Any]]", self._post_request(self.as_v3, request)
            )
        except ValueError as exc:
            raise FeatureNotAvailableError(
                "personal access tokens are not supported by this openBIS"
                " instance; please upgrade your server and activate them"
            ) from exc
        return self.get_personal_access_token_or_raise(created[0]["permId"])


__all__ = ["PersonalAccessToken", "_PersonalAccessTokenApi"]
