import datetime
import os
import time
import uuid

import pytest
import tempfile
import filecmp

from pybis.afs import AfsClient


def test_afs_detection(openbis_instance):
    data_stores = openbis_instance.get_datastores(with_afs=True)
    print(f'[TEST] Detected data stores: {data_stores}')
    data_store = data_stores[data_stores["code"] == "AFS"]
    print(f'[TEST] Found AFS datastore: {data_store}')

    # workaround because jenkins test server is not handling DataFrame properly
    import numpy as np
    data_store_list = np.array(data_store).tolist()
    print(f'[TEST] AFS data after conversion: {data_store_list}')

    afs_url = data_store_list[0][1] + "/api" if len(data_store_list) > 0 else None

    print(f'[TEST] Configured OpenBIS AFS url is: {afs_url}')
    afs_client = AfsClient(afs_url, openbis_instance.token, False)

    assert afs_client.is_session_valid()


def test_afs_client(afs):

    (space, client) = afs

    # assert client.is_session_valid()
    print("[TEST] CHECKING AFS AVAILABILITY")
    if client.is_session_valid():
        print("[TEST] AFS IS AVAILABLE")

        o = space.openbis

        timestamp = time.strftime("afs_test_%a_%y%m%d_%H%M%S").lower()

        sample = o.new_sample('UNKNOWN', code=timestamp , space=space)
        sample.save()

        # permId = sample.permId
        #
        # files = client.list(permId, "/", True)
        #
        # assert files == []
        #
        # testfile_path = os.path.join(os.path.dirname(__file__), "testdir")
        # client.upload_files(permId, '/', [testfile_path], wait_until_finished=True)
        #
        # files = client.list(permId, "/", True)
        # assert len(files) == 3
        #
        # with tempfile.TemporaryDirectory() as tmpdirname:
        #     client.download_files(permId, "/", tmpdirname, wait_until_finished=True)
        #     base_file = os.path.dirname(__file__)
        #     for file in files:
        #         if not file.directory:
        #             assert filecmp.cmp(os.path.join(base_file, file.path[1:]), os.path.join(tmpdirname, file.path[1:]))

    else:
        print("[TEST] AFS IS NOT AVAILABLE - TEST SKIPPED")




