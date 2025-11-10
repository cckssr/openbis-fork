#   Copyright ETH 2018 - 2024 Zürich, Scientific IT Services
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
import time

import pytest

from pybis import Openbis
from pybis import AfsClient

openbis_url = "https://localhost:8443"
admin_username = "admin"
admin_password = "admin"

@pytest.fixture(scope="module")
def openbis_instance():
    instance = Openbis(
        url=openbis_url,
        verify_certificates=False,
        allow_http_but_do_not_use_this_in_production_and_only_within_safe_networks=True
    )
    print("\nLOGGING IN...")
    instance.login(admin_username, admin_password)
    yield instance
    instance.logout()
    print("LOGGED OUT...")


@pytest.fixture(scope="module")
def other_openbis_instance():
    instance = Openbis(
        url=openbis_url,
        verify_certificates=False,
        allow_http_but_do_not_use_this_in_production_and_only_within_safe_networks=True
    )
    print("\nLOGGING IN...")
    instance.login(admin_username, admin_password)
    yield instance
    instance.logout()
    print("LOGGED OUT...")


@pytest.fixture(scope="session")
def space():
    o = Openbis(
        url=openbis_url,
        verify_certificates=False,
        allow_http_but_do_not_use_this_in_production_and_only_within_safe_networks=True
    )
    o.login(admin_username, admin_password)

    # create a space
    timestamp = time.strftime("%a_%y%m%d_%H%M%S").upper()
    space_name = "test_space_" + timestamp
    space = o.new_space(code=space_name)
    space.save()
    yield space

    # teardown
    o.logout()

@pytest.fixture(scope="session")
def afs(space):
    token = space.openbis.token
    o = space.openbis

    server_info = o.get_server_information()

    afs_url = getattr(server_info, "server-public-information.afs-server.url") + "/api"

    afs_client = AfsClient(afs_url, token, False)

    yield (space, afs_client)
