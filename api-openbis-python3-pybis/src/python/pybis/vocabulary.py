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
"""Vocabulary and VocabularyTerm entities for openBIS controlled vocabularies."""

from typing import Any, Optional

from .attribute import AttrHolder
from .definitions import openbis_definitions, get_type_for_entity, get_method_for_entity
from .openbis_object import OpenBisObject
from .utils import VERBOSE


class Vocabulary(
    OpenBisObject, entity="vocabulary", single_item_method_name="get_vocabulary"
):
    """An openBIS controlled vocabulary defining a closed set of allowed values.

    Vocabularies back ``CONTROLLEDVOCABULARY`` property types.  Each
    vocabulary owns a list of :class:`VocabularyTerm` objects; when a
    property of this type is set on an entity, the value must match one of
    those terms.

    Fetch vocabularies via ``openbis.get_vocabulary()`` /
    ``openbis.get_vocabularies()`` and create new ones with
    ``openbis.new_vocabulary()``.

    Attributes:
        code (str): Unique vocabulary code (used as the property-type
            ``vocabulary`` reference).
        description (Optional[str]): Human-readable description.
        chosenFromList (bool): If ``True`` the ELN UI enforces selection
            from the list rather than free-text entry.
        urlTemplate (Optional[str]): URL pattern for external term references.

    Example:
        >>> voc = openbis.new_vocabulary(code="ORGANISM", description="Organisms")
        >>> voc.add_term("HUMAN", label="Homo sapiens")
        >>> voc.add_term("MOUSE", label="Mus musculus")
        >>> voc.save()

        >>> # Later, list all terms
        >>> voc.get_terms().df
    """

    def __init__old_(
        self,
        openbis_obj: Any,
        data: Optional[dict] = None,
        terms: Optional[list] = None,
        **kwargs: Any,
    ) -> None:
        self.__dict__["openbis"] = openbis_obj
        self.__dict__["a"] = AttrHolder(openbis_obj, "vocabulary")

        if data is not None:
            self._set_data(data)
            self.__dict__["terms"] = data["terms"]

        if terms is None:
            self.__dict__["terms"] = []
        else:
            self.__dict__["terms"] = terms

        if self.is_new:
            allowed_attrs = openbis_definitions(self.entity)["attrs_new"]
            for key in kwargs:
                if key not in allowed_attrs:
                    raise ValueError(
                        f"{key} is an unknown Vocabulary attribute. Allowed attributes are: {', '.join(allowed_attrs)}"
                    )

        if kwargs is not None:
            for key in kwargs:
                setattr(self, key, kwargs[key])

    def __dir__(self) -> list[str]:
        """Return public attributes and methods for tab-completion.

        Returns:
            A list of method names available on this vocabulary.
        """
        return [
            "get_terms()",
            "add_term(code, label, description)",
            "save()",
        ]

    def get_terms(self) -> Any:
        """Return all terms of this vocabulary.

        Returns:
            A :class:`~pybis.things.Things` container whose ``.df`` gives a
            :class:`~pandas.DataFrame` of :class:`VocabularyTerm` objects.

        Example:
            >>> voc.get_terms().df
        """
        return self.openbis.get_terms(vocabulary=self.code)

    def add_term(
        self,
        code: str,
        label: Optional[str] = None,
        description: Optional[str] = None,
    ) -> None:
        """Add a term to this vocabulary.

        If the vocabulary has not yet been saved, the term is queued in an
        internal list and submitted with :meth:`save`.  If the vocabulary
        already exists in openBIS, :meth:`save` must be called afterwards
        to persist the new term.

        Args:
            code: Unique term code (upper-case letters/digits/underscores).
            label: Human-readable label displayed in the UI.
            description: Optional free-text description.

        Example:
            >>> voc.add_term("HUMAN", label="Homo sapiens", description="Modern human")
            >>> voc.save()
        """
        self.__dict__["terms"].append(
            {"code": code, "label": label, "description": description}
        )

    def delete(self, reason: str) -> None:
        """Delete this vocabulary from openBIS.

        Args:
            reason: Human-readable reason recorded with the deletion.

        Raises:
            ValueError: Propagated from the V3 API if deletion fails (e.g.
                the vocabulary is still referenced by a property type).

        Example:
            >>> voc.delete("Replaced by ORGANISM_V2")
        """
        if not self.data:
            return

        delete_type = get_type_for_entity("vocabulary", "delete")
        method = get_method_for_entity("vocabulary", "delete")

        request = {
            "method": method,
            "params": [
                self.openbis.token,
                [
                    {
                        "permId": self.code,
                        "@type": "as.dto.vocabulary.id.VocabularyPermId",
                    }
                ],
                {"reason": reason, **delete_type},
            ],
        }
        resp = self.openbis._post_request(self.openbis.as_v3, request)
        self.openbis.clear_cache("vocabulary")
        if VERBOSE:
            print(f"{self.entity} {self.code} successfully deleted.")

    def save(self) -> "Vocabulary":
        """Persist this vocabulary to openBIS (create or update).

        For **new** vocabularies, also submits any terms added via
        :meth:`add_term`.

        Returns:
            This :class:`Vocabulary` instance, updated with server-assigned
            fields.

        Example:
            >>> voc = openbis.new_vocabulary(code="STATUS")
            >>> voc.add_term("ACTIVE")
            >>> voc.add_term("INACTIVE")
            >>> voc.save()
        """
        terms = self._terms or []
        for term in terms:
            term["@type"] = "as.dto.vocabulary.create.VocabularyTermCreation"

        if self.is_new:
            request = self._new_attrs("createVocabularies")
            if terms:
                request["params"][1][0]["terms"] = terms
            resp = self.openbis._post_request(self.openbis.as_v3, request)

            if VERBOSE:
                print("Vocabulary successfully created.")
            self.openbis.clear_cache("vocabulary")
            data = self.openbis.get_vocabulary_or_raise(resp[0]["permId"]).data
            self._set_data(data)
            return self

        else:
            request = self._up_attrs("updateVocabularies")
            request["params"][1][0]["vocabularyId"] = {
                "@type": "as.dto.vocabulary.id.VocabularyPermId",
                "permId": self.code,
            }
            request["params"][1][0]["chosenFromList"] = {
                "value": self.chosenFromList,
                "isModified": True,
                "@type": "as.dto.common.update.FieldUpdateValue",
            }
            if terms:
                request["params"][1][0]["terms"] = terms
            self.openbis._post_request(self.openbis.as_v3, request)
            self.openbis.clear_cache("vocabulary")
            if VERBOSE:
                print("Vocabulary successfully updated.")
            data = self.openbis.get_vocabulary_or_raise(self.code).data
            self._set_data(data)


class VocabularyTerm(OpenBisObject):
    """A single term within an openBIS :class:`Vocabulary`.

    Vocabulary terms define the closed set of allowed values for a
    ``CONTROLLEDVOCABULARY`` property type.  They can be reordered via
    :meth:`move_to_top` and :meth:`move_after_term`.

    Fetch terms via ``openbis.get_term()`` / ``openbis.get_terms()`` and
    create new ones with ``openbis.new_term()`` or
    :meth:`~Vocabulary.add_term`.

    Attributes:
        code (str): Term code (upper-case, unique within its vocabulary).
        vocabularyCode (str): Code of the parent vocabulary.
        label (Optional[str]): Human-readable label shown in dropdowns.
        description (Optional[str]): Free-text description.
        official (bool): Whether the term is marked as official.
        ordinal (int): Display order within the vocabulary.
        registrator (str): User who created the term.
        registrationDate (str): ISO-8601 creation timestamp.
        modifier (str): User who last modified the term.
        modificationDate (str): ISO-8601 last-modification timestamp.

    Example:
        >>> term = openbis.get_term("HUMAN", vocabulary_code="ORGANISM")
        >>> term.label = "Homo sapiens (updated)"
        >>> term.save()
    """

    def __init__(
        self, openbis_obj: Any, data: Optional[dict] = None, **kwargs: Any
    ) -> None:
        """Initialise a VocabularyTerm.

        Args:
            openbis_obj: The :class:`~pybis.Openbis` connection instance.
            data: Raw term dict from the V3 API.
            **kwargs: Additional attribute key/value pairs.
        """
        self.__dict__["openbis"] = openbis_obj
        self.__dict__["a"] = AttrHolder(openbis_obj, "vocabularyTerm")

        if data is not None:
            self._set_data(data)

        if kwargs is not None:
            for key in kwargs:
                setattr(self, key, kwargs[key])

    @property
    def vocabularyCode(self) -> str:
        """The code of the parent vocabulary.

        Returns:
            The vocabulary code as a string.
        """
        if self.is_new:
            return self.__dict__["a"].vocabularyCode
        else:
            return self.data["permId"]["vocabularyCode"]

    def __dir__(self) -> list[str]:
        """Return public attributes and methods for tab-completion.

        Returns:
            A list of attribute and method names.
        """
        return [
            "code",
            "vocabularyCode",
            "label",
            "description",
            "official",
            "ordinal",
            "registrator",
            "registrationDate",
            "modifier",
            "modificationDate",
            "move_to_top()",
            "move_after_term()",
        ]

    def move_to_top(self) -> None:
        """Schedule this term to move to position 1 in the vocabulary.

        The change is applied when :meth:`save` is called.

        Example:
            >>> term.move_to_top()
            >>> term.save()
        """
        self.previousTermId = ""

    def move_after_term(self, term: str) -> None:
        """Schedule this term to be placed immediately after another term.

        The change is applied when :meth:`save` is called.

        Args:
            term: Code of the term this term should follow.

        Example:
            >>> term.move_after_term("MOUSE")
            >>> term.save()
        """
        self.previousTermId = term

    def _up_attrs(self) -> dict:
        """Build an update request for this vocabulary term.

        VocabularyTerms use a different update mechanism from other openBIS
        entities, so this method overrides the base class implementation.

        Returns:
            A V3 API request dict for ``updateVocabularyTerms``.
        """
        attrs = {}
        for attr in "label description official".split():
            attrs[attr] = {
                "value": getattr(self, attr),
                "isModified": True,
                "@type": "as.dto.common.update.FieldUpdateValue",
            }

        if not getattr(self, "previousTermId") == None:
            value = self.previousTermId
            if value == "":
                value = None
            else:
                permId = {
                    "@type": "as.dto.vocabulary.id.VocabularyTermPermId",
                    "vocabularyCode": self.vocabularyCode,
                    "code": value,
                }
                value = permId

            attrs["previousTermId"] = {
                "isModified": True,
                "@type": "as.dto.common.update.FieldUpdateValue",
                "value": value,
            }

        attrs["vocabularyTermId"] = self.vocabularyTermId()
        attrs["@type"] = "as.dto.vocabulary.update.VocabularyTermUpdate"
        request = {
            "method": "updateVocabularyTerms",
            "params": [self.openbis.token, [attrs]],
        }
        return request

    def _new_attrs(self) -> dict:
        """Build a creation request for this vocabulary term.

        Returns:
            A V3 API request dict for ``createVocabularyTerms``.
        """
        attrs = {
            "@type": "as.dto.vocabulary.create.VocabularyTermCreation",
            "vocabularyId": self.vocabularyTermId(),
        }
        for attr in "code label description".split():
            attrs[attr] = getattr(self, attr)

        request = {
            "method": "createVocabularyTerms",
            "params": [self.openbis.token, [attrs]],
        }
        return request

    def vocabularyTermId(self) -> dict:
        """Return the vocabulary identifier dict used in V3 API requests.

        Returns:
            A dict identifying either the parent vocabulary (for new terms)
            or the term itself (for existing terms).
        """
        if self.is_new:
            return {
                "permId": getattr(self, "vocabularyCode"),
                "@type": "as.dto.vocabulary.id.VocabularyPermId",
            }
        else:
            permId = self.data["permId"]
            permId.pop("@id", None)
            return permId

    def save(self) -> "VocabularyTerm":
        """Persist this term to openBIS (create or update).

        Returns:
            This :class:`VocabularyTerm` instance, updated with server data.

        Example:
            >>> term = openbis.new_term(
            ...     code="RAT", vocabulary_code="ORGANISM", label="Rattus norvegicus"
            ... )
            >>> term.save()
        """
        if self.is_new:
            request = self._new_attrs()
            resp = self.openbis._post_request(self.openbis.as_v3, request)

            self.openbis.clear_cache("term")
            if VERBOSE:
                print("Vocabulary Term successfully created.")
            data = self.openbis.get_term_or_raise(
                resp[0]["code"], resp[0]["vocabularyCode"]
            ).data
            self._set_data(data)
            return self

        else:
            request = self._up_attrs()
            self.openbis._post_request(self.openbis.as_v3, request)
            self.openbis.clear_cache("term")
            if VERBOSE:
                print("Vocabulary Term successfully updated.")
            data = self.openbis.get_term_or_raise(self.code, self.vocabularyCode).data
            self._set_data(data)

    def delete(self, reason: str = "no particular reason") -> None:
        """Delete this vocabulary term from openBIS.

        Args:
            reason: Human-readable reason recorded with the deletion.
                Defaults to ``"no particular reason"``.

        Example:
            >>> term.delete("Term no longer used")
        """
        self.openbis.delete_openbis_entity(
            entity="vocabularyTerm", objectId=self.data["permId"], reason=reason
        )
        self.openbis.clear_cache("term")
        if VERBOSE:
            print("VocabularyTerm successfully deleted.")
