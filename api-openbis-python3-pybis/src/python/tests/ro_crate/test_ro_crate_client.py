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
import datetime
import time


def test_echo(ro_crate):

    space, client = ro_crate
    message = "test echo"
    result = client.echo(message)

    assert result == message

def test_connection(ro_crate):

    space, client = ro_crate
    token = space.openbis.token

    result = client.test_connection()

    assert token.startswith(result.lower())

def test_export_1_non_existing_id(ro_crate):
    space, client = ro_crate

    jobId = client.export(['UNKNOWN-ID'])

    assert jobId is not None

    time.sleep(5)
    status = client.check_status(jobId)

    assert status is not None
    assert status.status == 'FAILED'

def test_export_2_check_status(ro_crate):
    space, client = ro_crate

    openbis = space.openbis
    timestamp = datetime.datetime.now().strftime("ro_crate_test_%Y_%m_%d_%H%M%S.%f")
    sample = openbis.new_sample('UNKNOWN', code=timestamp, space=space)
    sample.save()


    jobId = client.export([sample.permId])

    assert jobId is not None

    time.sleep(3)
    status = client.check_status(jobId)

    assert status is not None
    assert status.status in ['RUNNING', 'COMPLETED']

def test_export_3_check_statuses(ro_crate):
    space, client = ro_crate

    openbis = space.openbis
    timestamp = datetime.datetime.now().strftime("ro_crate_test_%Y_%m_%d_%H%M%S.%f")
    sample = openbis.new_sample('UNKNOWN', code=timestamp, space=space)
    sample.save()


    jobId = client.export([sample.permId])

    assert jobId is not None

    stats = client.check_statuses()

    assert stats is not None
    assert len(stats) > 0
    assert len(list(filter(lambda x: x.jobId == jobId, stats))) == 1


