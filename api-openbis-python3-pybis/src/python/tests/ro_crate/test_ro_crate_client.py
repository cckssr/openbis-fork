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

# def test_export(ro_crate):
#     space, client = ro_crate
#
#     openbis = space.openbis
#     timestamp = datetime.datetime.now().strftime("afs_test_%Y_%m_%d_%H%M%S.%f")
#     sample = openbis.new_sample('UNKNOWN', code=timestamp, space=space)
#     sample.save()
#
#
#     jobId = client.export([sample.permId])
#
#     client.check_status(jobId)


