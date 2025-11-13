import datetime
import os
import time
import uuid

import pytest
import tempfile
import filecmp

from pybis.things import Things
from pybis import AfsClient


def test_afs_client(afs):

    (space, client) = afs

    if client.is_session_valid():

        o = space.openbis

        timestamp = time.strftime("afs_test_%a_%y%m%d_%H%M%S").lower()

        sample = o.new_sample('UNKNOWN', code=timestamp , space=space)
        sample.save()

        permId = sample.permId

        files = client.list(permId, "/", True)

        assert files == []

        testfile_path = os.path.join(os.path.dirname(__file__), "testdir")
        client.upload_files(permId, '/', [testfile_path], wait_until_finished=True)

        files = client.list(permId, "/", True)
        assert len(files) == 3

        with tempfile.TemporaryDirectory() as tmpdirname:
            client.download_files(permId, "/", tmpdirname, wait_until_finished=True)
            base_file = os.path.dirname(__file__)
            for file in files:
                if not file.directory:
                    assert filecmp.cmp(os.path.join(base_file, file.path[1:]), os.path.join(tmpdirname, file.path[1:]))

    else:
        print("AFS IS NOT AVAILABLE - TEST SKIPPED")




