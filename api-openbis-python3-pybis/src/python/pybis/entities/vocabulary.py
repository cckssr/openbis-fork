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
"""Controlled vocabularies and their terms: client API."""

from __future__ import annotations

from collections.abc import Sequence
from typing import TYPE_CHECKING, Any, cast

from ..api.rpc import parse_jackson, type_for_id
from ..api.search import explicit_id_criterion
from ..definitions import get_fetchoption_for_entity
from ..exceptions import NotFoundError
from ..types.results import SearchResult
from ..utils import extract_attr, extract_person, format_timestamp
from ..vocabulary import Vocabulary, VocabularyTerm
from ._mixin import ClientApiMixin

if TYPE_CHECKING:
    import pandas as pd


def _vocabularies_df(items: Sequence[Any]) -> "pd.DataFrame":
    """Build the notebook DataFrame view for a page of vocabularies."""
    from pandas import DataFrame

    attrs = (
        "code description managedInternally chosenFromList urlTemplate"
        " registrator registrationDate modificationDate"
    ).split()
    if not items:
        return DataFrame(columns=attrs)
    df = DataFrame([item.data for item in items])
    for column, mapper in [
        ("registrationDate", format_timestamp),
        ("modificationDate", format_timestamp),
        ("registrator", extract_person),
    ]:
        if column in df:
            df[column] = df[column].map(mapper)
    return cast("pd.DataFrame", df[df.columns.intersection(attrs)])


def _terms_df(items: Sequence[Any]) -> "pd.DataFrame":
    """Build the notebook DataFrame view for a page of vocabulary terms."""
    from pandas import DataFrame

    attrs = (
        "code vocabularyCode label description registrationDate"
        " modificationDate official ordinal"
    ).split()
    if not items:
        return DataFrame(columns=attrs)
    df = DataFrame([item.data for item in items])
    if "permId" in df:
        df["vocabularyCode"] = df["permId"].map(extract_attr("vocabularyCode"))
    for column in ["registrationDate", "modificationDate"]:
        if column in df:
            df[column] = df[column].map(format_timestamp)
    return cast("pd.DataFrame", df[df.columns.intersection(attrs)])


class _VocabularyApi(ClientApiMixin):
    """Vocabulary and vocabulary-term methods of the Openbis client."""

    def get_vocabulary(self, code: str) -> Vocabulary | None:
        """Get a single Vocabulary (including its terms) by code, or None."""
        code = str(code).upper()
        cached = self._object_cache(entity="vocabulary", code=code)
        if cached is not None:
            return cached  # type: ignore[no-any-return]  # reason: heterogeneous legacy cache

        request = {
            "method": "getVocabularies",
            "params": [
                self.token,
                [type_for_id(code, "vocabulary")],
                get_fetchoption_for_entity("vocabulary"),
            ],
        }
        resp = self._post_request(self.as_v3, request)
        parse_jackson(resp)
        for ident in resp:
            vocabulary = Vocabulary(openbis_obj=self, data=resp[ident])
            self._object_cache(entity="vocabulary", code=code, value=vocabulary)
            return vocabulary
        return None

    def get_vocabulary_or_raise(self, code: str) -> Vocabulary:
        """Get a single Vocabulary; raise if it does not exist.

        Raises:
            NotFoundError: No vocabulary exists with this code.
        """
        vocabulary = self.get_vocabulary(code)
        if vocabulary is None:
            raise NotFoundError("vocabulary", code)
        return vocabulary

    def search_vocabularies(
        self,
        *,
        code: str | None = None,
        count: int = 25,
        start_with: int = 0,
    ) -> SearchResult[Vocabulary]:
        """Search for Vocabularies.

        Args:
            code: Filter by vocabulary code (exact match).
            count: Maximum number of results to return (default: 25).
            start_with: Pagination offset (default: 0).
        """
        criteria: dict[str, Any] = {
            "@type": "as.dto.vocabulary.search.VocabularySearchCriteria",
            "operator": "AND",
            "criteria": [],
        }
        if code is not None:
            criteria["criteria"].append(explicit_id_criterion("code", code))
        fetchopts = get_fetchoption_for_entity("vocabulary")
        fetchopts["from"] = start_with
        fetchopts["count"] = count
        fetchopts["registrator"] = get_fetchoption_for_entity("registrator")
        request = {
            "method": "searchVocabularies",
            "params": [self.token, criteria, fetchopts],
        }
        resp = self._post_request(self.as_v3, request)
        parse_jackson(resp)
        items = [Vocabulary(openbis_obj=self, data=data) for data in resp["objects"]]
        return SearchResult(
            items, int(resp.get("totalCount", len(items))), _vocabularies_df
        )

    def new_vocabulary(
        self,
        code: str,
        terms: list[dict[str, Any]],
        *,
        description: str | None = None,
        managed_internally: bool = False,
        chosen_from_list: bool = True,
        url_template: str | None = None,
    ) -> Vocabulary:
        """Construct an unsaved Vocabulary; call ``.save()`` to persist it.

        Args:
            code: Code of the new vocabulary.
            terms: Term dicts, e.g.
                ``[{"code": "T1", "label": "...", "description": "..."}]``.
            description: Free-text description.
            managed_internally: Vocabulary is system-managed.
            chosen_from_list: Values must come from the term list.
            url_template: Optional URL template for the terms.
        """
        kwargs: dict[str, Any] = {
            "code": code,
            "managedInternally": managed_internally,
            "chosenFromList": chosen_from_list,
        }
        if description is not None:
            kwargs["description"] = description
        if url_template is not None:
            kwargs["urlTemplate"] = url_template
        return Vocabulary(self, data=None, terms=terms, **kwargs)

    # --- terms ------------------------------------------------------------------

    def get_term(self, code: str, vocabulary_code: str) -> VocabularyTerm | None:
        """Get a single VocabularyTerm by code and vocabulary code, or None."""
        request = {
            "method": "getVocabularyTerms",
            "params": [
                self.token,
                [
                    {
                        "code": code,
                        "vocabularyCode": vocabulary_code,
                        "@type": "as.dto.vocabulary.id.VocabularyTermPermId",
                    }
                ],
                {
                    **get_fetchoption_for_entity("vocabularyTerm"),
                    "registrator": get_fetchoption_for_entity("registrator"),
                },
            ],
        }
        resp = self._post_request(self.as_v3, request)
        if not resp:
            return None
        parse_jackson(resp)
        for ident in resp:
            return VocabularyTerm(self, resp[ident])
        return None

    def get_term_or_raise(self, code: str, vocabulary_code: str) -> VocabularyTerm:
        """Get a single VocabularyTerm; raise if it does not exist.

        Raises:
            NotFoundError: No term with this code exists in the vocabulary.
        """
        term = self.get_term(code, vocabulary_code)
        if term is None:
            raise NotFoundError("vocabulary term", f"{vocabulary_code}/{code}")
        return term

    def search_terms(
        self,
        *,
        vocabulary: str | None = None,
        count: int = 25,
        start_with: int = 0,
    ) -> SearchResult[VocabularyTerm]:
        """Search for VocabularyTerms.

        Results for a plain by-vocabulary query are cached transparently
        (term lists are read on every entity construction with controlled-
        vocabulary properties).

        Args:
            vocabulary: Restrict to the terms of this vocabulary code.
            count: Maximum number of results to return (default: 25).
            start_with: Pagination offset (default: 0).
        """
        cache_key = None
        if vocabulary is not None and start_with == 0:
            cache_key = f"{vocabulary}:{count}"
            cached = self._object_cache(entity="term", code=cache_key)
            if cached is not None:
                return cached  # type: ignore[no-any-return]  # reason: heterogeneous legacy cache

        criteria: dict[str, Any] = {
            "@type": "as.dto.vocabulary.search.VocabularyTermSearchCriteria",
            "operator": "AND",
            "criteria": [],
        }
        if vocabulary is not None:
            criteria["criteria"].append(
                {
                    "@type": "as.dto.vocabulary.search.VocabularySearchCriteria",
                    "operator": "AND",
                    "criteria": [explicit_id_criterion("code", vocabulary)],
                }
            )
        fetchopts = get_fetchoption_for_entity("vocabularyTerm")
        fetchopts["from"] = start_with
        fetchopts["count"] = count
        request = {
            "method": "searchVocabularyTerms",
            "params": [self.token, criteria, fetchopts],
        }
        resp = self._post_request(self.as_v3, request)
        parse_jackson(resp)
        items = [VocabularyTerm(self, data) for data in resp["objects"]]
        result = SearchResult(items, int(resp.get("totalCount", len(items))), _terms_df)
        if cache_key is not None:
            self._object_cache(entity="term", code=cache_key, value=result)
        return result

    def new_term(
        self,
        code: str,
        vocabulary_code: str,
        *,
        label: str | None = None,
        description: str | None = None,
    ) -> VocabularyTerm:
        """Construct an unsaved VocabularyTerm; call ``.save()`` to persist it.

        Args:
            code: Code of the new term.
            vocabulary_code: Vocabulary the term belongs to.
            label: Display label.
            description: Free-text description.
        """
        return VocabularyTerm(
            self,
            data=None,
            code=code,
            vocabularyCode=vocabulary_code.upper(),
            label=label,
            description=description,
            managedInternally=False,
        )


__all__ = ["Vocabulary", "VocabularyTerm", "_VocabularyApi"]
