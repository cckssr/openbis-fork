import datetime
import os
import time
import uuid

import tempfile
import filecmp

from pybis.afs import AfsClient


def get_sample_for_test(space):
    openbis = space.openbis
    timestamp = time.strftime("afs_test_%a_%y%m%d_%H%M%S.%f").lower()
    sample = openbis.new_sample('UNKNOWN', code=timestamp, space=space)
    sample.save()
    return sample

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


def test_upload_download(afs):

    (space, client) = afs
    assert client.is_session_valid()

    sample = get_sample_for_test(space)
    permId = sample.permId

    files = client.list(permId, "/", True)

    assert files == []

    testfile_path = os.path.join(os.path.dirname(__file__), "..", "testdir")
    client.upload_files(permId, '/', [testfile_path], wait_until_finished=True)

    files = client.list(permId, "/", True)
    assert len(files) == 3

    # with tempfile.TemporaryDirectory() as tmpdirname:
    #     client.download_files(permId, "/", tmpdirname, wait_until_finished=True)
    #     base_file = os.path.dirname(__file__)
    #     for file in files:
    #         if not file.directory:
    #             assert filecmp.cmp(os.path.join(base_file, "..", file.path[1:]), os.path.join(tmpdirname, file.path[1:]))

def test_write(afs):
    (space, client) = afs
    sample = get_sample_for_test(space)
    permId = sample.permId
    files = client.list(permId, "/", True)
    assert len(files) == 0

    text = "hello test_write!".encode("utf-8")
    client.write(permId, '/test.txt', 0, len(text), text)

    files = client.list(permId, "/", True)
    assert len(files) == 1
    assert files[0].path == "/test.txt"
    assert files[0].name == "test.txt"
    assert files[0].size == len(text)
    assert files[0].directory == False
    assert files[0].owner == permId


def test_read(afs):
    (space, client) = afs
    sample = get_sample_for_test(space)
    permId = sample.permId
    files = client.list(permId, "/", True)
    assert len(files) == 0

    text = "hello test_read!".encode("utf-8")
    client.write(permId, '/test.txt', 0, len(text), text)

    files = client.list(permId, "/", True)
    assert len(files) == 1

    content = client.read(permId, '/test.txt', 0, files[0].size)
    assert content == text

    content_limited = client.read(permId, '/test.txt', 0, 5)
    assert content_limited == "hello".encode("utf-8")

def test_create(afs):
    (space, client) = afs
    sample = get_sample_for_test(space)
    permId = sample.permId
    files = client.list(permId, "/", True)
    assert len(files) == 0

    client.create(permId, '/test_dir', True)

    files = client.list(permId, "/", True)
    assert len(files) == 1
    assert files[0].path == "/test_dir"
    assert files[0].name == "test_dir"
    assert files[0].size == 0
    assert files[0].directory == True
    assert files[0].owner == permId

def test_copy(afs):
    (space, client) = afs
    sample = get_sample_for_test(space)
    permId = sample.permId
    files = client.list(permId, "/", True)
    assert len(files) == 0

    client.create(permId, '/test_dir', True)
    text = "hello test_copy!".encode("utf-8")
    client.write(permId, '/test_dir/test.txt', 0, len(text), text)

    files = client.list(permId, "/", True)
    assert len(files) == 2

    success = client.copy(permId, '/test_dir', permId, '/test_dir_copy')
    assert success == True
    files = client.list(permId, "/", True)
    assert len(files) == 4

    content = client.read(permId, '/test_dir_copy/test.txt', 0, files[0].size)
    assert content == text

def test_move(afs):
    (space, client) = afs
    sample = get_sample_for_test(space)
    permId = sample.permId
    files = client.list(permId, "/", True)
    assert len(files) == 0

    client.create(permId, '/test_dir', True)
    text = "hello test_move!".encode("utf-8")
    client.write(permId, '/test_dir/test.txt', 0, len(text), text)

    files = client.list(permId, "/", True)
    assert len(files) == 2

    success = client.move(permId, '/test_dir', permId, '/test_dir_move')
    assert success == True
    files = client.list(permId, "/", True)
    assert len(files) == 2

    content = client.read(permId, '/test_dir_move/test.txt', 0, files[0].size)
    assert content == text


def test_truncate(afs):
    (space, client) = afs
    sample = get_sample_for_test(space)
    permId = sample.permId
    files = client.list(permId, "/", True)
    assert len(files) == 0

    text = "hello test_truncate!".encode("utf-8")
    client.write(permId, '/test.txt', 0, len(text), text)

    files = client.list(permId, "/", True)
    assert len(files) == 1
    assert files[0].size == len(text)

    success = client.truncate(permId, '/test.txt', 5)
    assert success == True
    files_after = client.list(permId, "/", True)

    assert len(files_after) == 1
    assert files_after[0].path == files[0].path
    assert files_after[0].name == files[0].name
    assert files_after[0].size == 5
    assert files_after[0].directory == files[0].directory
    assert files_after[0].owner == files[0].owner

    content = client.read(permId, '/test.txt', 0, files_after[0].size)
    assert content == "hello".encode("utf-8")

def test_delete_without_trash(afs):
    (space, client) = afs
    sample = get_sample_for_test(space)
    permId = sample.permId
    files = client.list(permId, "/", True)
    assert len(files) == 0

    client.create(permId, '/test_dir', True)
    text = "hello test_delete!".encode("utf-8")
    client.write(permId, '/test_dir/test.txt', 0, len(text), text)

    files = client.list(permId, "/", True)
    assert len(files) == 2

    success = client.delete(permId, '/test_dir', False)
    assert success == True

    files = client.list(permId, "/", True)
    assert len(files) == 0


def test_delete_with_trash(afs):
    (space, client) = afs
    sample = get_sample_for_test(space)
    permId = sample.permId
    files = client.list(permId, "/", True)
    assert len(files) == 0

    client.create(permId, '/test_dir', True)
    text = "hello test_delete!".encode("utf-8")
    client.write(permId, '/test_dir/test.txt', 0, len(text), text)

    files = client.list(permId, "/", True)
    assert len(files) == 2

    success = client.delete(permId, '/test_dir', True)
    assert success == True

    files = client.list(permId, "/", True)
    assert len(files) == 3
    for file in files:
        assert file.path.startswith("/.afs.trash")

def test_snapshot(afs):
    (space, client) = afs
    sample = get_sample_for_test(space)
    permId = sample.permId
    files = client.list(permId, "/", True)
    assert len(files) == 0

    text = "hello test_snapshot!".encode("utf-8")
    client.write(permId, '/test.txt', 0, len(text), text)

    files = client.list(permId, "/", True)
    assert len(files) == 1

    success = client.snapshot(permId, '/test.txt')
    assert success == True

    files = client.list(permId, "/", True)
    assert len(files) == 4
    for file in files:
        assert file.path.startswith("/.afs.snapshots") or file.path == "/test.txt"



