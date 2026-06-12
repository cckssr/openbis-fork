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
"""Capture real openBIS JSON-RPC responses as unit-test fixtures.

Run manually against a live (test!) openBIS instance; commits raw wire
payloads (before Jackson dereferencing) under ``tests/unit/fixtures/captured/``
so the transport and entity-hydration code can be golden-tested offline::

    python tests/unit/fixtures/capture.py \
        --url https://localhost:8443 --user admin --password admin

The captures intentionally bypass ``Openbis`` and speak raw JSON-RPC, so the
files contain the unprocessed ``@id`` reference graphs.
"""

from __future__ import annotations

import argparse
import json
from pathlib import Path
from typing import Any

import requests

HERE = Path(__file__).parent
OUT = HERE / "captured"

AS_V3 = "/openbis/openbis/rmi-application-server-v3.json"


def rpc(session: requests.Session, url: str, method: str, params: list[Any]) -> Any:
    """Send one raw JSON-RPC request and return the raw result."""
    resp = session.post(
        url + AS_V3,
        json.dumps({"id": "1", "jsonrpc": "2.0", "method": method, "params": params}),
    )
    resp.raise_for_status()
    body = resp.json()
    if "error" in body:
        raise RuntimeError(body["error"])
    return body["result"]


def main() -> None:
    """Capture a fixed set of representative responses."""
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--url", required=True)
    parser.add_argument("--user", required=True)
    parser.add_argument("--password", required=True)
    parser.add_argument("--insecure", action="store_true", default=True)
    args = parser.parse_args()

    session = requests.Session()
    session.verify = not args.insecure
    token = rpc(session, args.url, "login", [args.user, args.password])

    fetch_all = {"@type": "as.dto.sample.fetchoptions.SampleFetchOptions"}
    captures: dict[str, tuple[str, list[Any]]] = {
        "server_information": ("getServerInformation", [token]),
        "search_samples_with_relations": (
            "searchSamples",
            [
                token,
                {"@type": "as.dto.sample.search.SampleSearchCriteria"},
                {
                    "@type": "as.dto.sample.fetchoptions.SampleFetchOptions",
                    "type": {
                        "@type": "as.dto.sample.fetchoptions.SampleTypeFetchOptions"
                    },
                    "properties": {
                        "@type": "as.dto.property.fetchoptions.PropertyFetchOptions"
                    },
                    "registrator": {
                        "@type": "as.dto.person.fetchoptions.PersonFetchOptions"
                    },
                    "parents": fetch_all,
                    "children": fetch_all,
                    "from": 0,
                    "count": 10,
                },
            ],
        ),
        "search_experiments": (
            "searchExperiments",
            [
                token,
                {"@type": "as.dto.experiment.search.ExperimentSearchCriteria"},
                {
                    "@type": "as.dto.experiment.fetchoptions.ExperimentFetchOptions",
                    "type": {
                        "@type": "as.dto.experiment.fetchoptions.ExperimentTypeFetchOptions"
                    },
                    "properties": {
                        "@type": "as.dto.property.fetchoptions.PropertyFetchOptions"
                    },
                    "from": 0,
                    "count": 10,
                },
            ],
        ),
        "search_datasets_with_physical_data": (
            "searchDataSets",
            [
                token,
                {"@type": "as.dto.dataset.search.DataSetSearchCriteria"},
                {
                    "@type": "as.dto.dataset.fetchoptions.DataSetFetchOptions",
                    "type": {
                        "@type": "as.dto.dataset.fetchoptions.DataSetTypeFetchOptions"
                    },
                    "physicalData": {
                        "@type": "as.dto.dataset.fetchoptions.PhysicalDataFetchOptions"
                    },
                    "properties": {
                        "@type": "as.dto.property.fetchoptions.PropertyFetchOptions"
                    },
                    "from": 0,
                    "count": 10,
                },
            ],
        ),
        "search_vocabularies_with_terms": (
            "searchVocabularies",
            [
                token,
                {"@type": "as.dto.vocabulary.search.VocabularySearchCriteria"},
                {
                    "@type": "as.dto.vocabulary.fetchoptions.VocabularyFetchOptions",
                    "terms": {
                        "@type": "as.dto.vocabulary.fetchoptions.VocabularyTermFetchOptions"
                    },
                    "from": 0,
                    "count": 5,
                },
            ],
        ),
        "search_spaces": (
            "searchSpaces",
            [
                token,
                {"@type": "as.dto.space.search.SpaceSearchCriteria"},
                {
                    "@type": "as.dto.space.fetchoptions.SpaceFetchOptions",
                    "from": 0,
                    "count": 10,
                },
            ],
        ),
        "search_projects": (
            "searchProjects",
            [
                token,
                {"@type": "as.dto.project.search.ProjectSearchCriteria"},
                {
                    "@type": "as.dto.project.fetchoptions.ProjectFetchOptions",
                    "space": {"@type": "as.dto.space.fetchoptions.SpaceFetchOptions"},
                    "from": 0,
                    "count": 10,
                },
            ],
        ),
    }

    OUT.mkdir(exist_ok=True)
    for name, (method, params) in captures.items():
        try:
            result = rpc(session, args.url, method, params)
        except Exception as exc:  # capture what we can, report the rest
            print(f"SKIP {name}: {exc}")
            continue
        out_file = OUT / f"{name}.json"
        out_file.write_text(json.dumps(result, indent=2, sort_keys=True))
        print(f"captured {out_file}")

    rpc(session, args.url, "logout", [token])


if __name__ == "__main__":
    main()
