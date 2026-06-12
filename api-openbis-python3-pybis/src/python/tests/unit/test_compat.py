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
"""Every deprecated shim must warn, delegate, and translate parameters.

Parametrized over the full rename table in pybis._compat — new wave entries
are covered automatically.
"""

import warnings

import pytest

from pybis import Openbis
from pybis._compat import _METHOD_RENAMES, _translate_kwargs


@pytest.mark.parametrize("old_name,new_name", sorted(_METHOD_RENAMES.items()))
def test_deprecated_shim_warns(client, monkeypatch, old_name, new_name):
    calls = []
    monkeypatch.setattr(Openbis, new_name, lambda self, **kwargs: calls.append(kwargs))
    method = getattr(client, old_name)
    with warnings.catch_warnings(record=True) as caught:
        warnings.simplefilter("always")
        method()
    assert len(caught) >= 1
    assert issubclass(caught[0].category, DeprecationWarning)
    assert new_name in str(caught[0].message)


@pytest.mark.parametrize("old_name,new_name", sorted(_METHOD_RENAMES.items()))
def test_deprecated_shim_delegates(client, monkeypatch, old_name, new_name):
    calls = []
    monkeypatch.setattr(Openbis, new_name, lambda self, **kwargs: calls.append(kwargs))
    with warnings.catch_warnings():
        warnings.simplefilter("ignore")
        getattr(client, old_name)()
    assert len(calls) == 1, f"{old_name} did not delegate to {new_name}"


def test_shim_translates_positional_arguments(client, monkeypatch):
    calls = []
    monkeypatch.setattr(
        Openbis, "search_spaces", lambda self, **kwargs: calls.append(kwargs)
    )
    with warnings.catch_warnings():
        warnings.simplefilter("ignore")
        client.get_spaces("MY_SPACE")
    assert calls == [{"code": "MY_SPACE"}]


def test_shim_drops_explicit_none_arguments(client, monkeypatch):
    """1.x signatures defaulted everything to None; the new defaults apply."""
    calls = []
    monkeypatch.setattr(
        Openbis, "search_spaces", lambda self, **kwargs: calls.append(kwargs)
    )
    with warnings.catch_warnings():
        warnings.simplefilter("ignore")
        client.get_spaces(code=None, start_with=None, count=None)
    assert calls == [{}]


def test_removed_parameter_raises_pointed_type_error(client):
    with warnings.catch_warnings():
        warnings.simplefilter("ignore")
        with pytest.raises(TypeError, match="MIGRATION_GUIDE"):
            client.get_spaces(use_cache=False)


def test_translate_kwargs_renames_camel_case():
    assert _translate_kwargs("m", {"permId": "X", "withParents": True}) == {
        "perm_id": "X",
        "with_parents": True,
    }


def test_translate_kwargs_renames_old_vocabulary():
    assert _translate_kwargs("m", {"experiment": "/S/P/E", "sample": "/S/X"}) == {
        "collection": "/S/P/E",
        "object": "/S/X",
    }


@pytest.mark.parametrize("removed", ["only_data", "raw_response", "use_cache", "attrs"])
def test_translate_kwargs_hard_fails_on_removed(removed):
    with pytest.raises(TypeError, match=removed):
        _translate_kwargs("m", {removed: True})
