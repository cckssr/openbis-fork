#   Copyright ETH 2023 - 2025 Zürich, Scientific IT Services
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# MasterDataRegistrationTransaction Class
from ch.ethz.sis.openbis.generic.server.asapi.v3 import ApplicationServerApi
from ch.systemsx.cisd.openbis.generic.server import CommonServiceProvider
from ch.systemsx.cisd.openbis.generic.server.jython.api.v1.impl import MasterDataRegistrationHelper
from ch.ethz.sis.openbis.generic.asapi.v3.dto.importer.data import ImportData
from ch.ethz.sis.openbis.generic.asapi.v3.dto.importer.options import ImportOptions
from ch.ethz.sis.openbis.generic.asapi.v3.dto.importer.data import ImportFormat
from ch.ethz.sis.openbis.generic.asapi.v3.dto.importer.options import ImportMode

from ch.systemsx.cisd.openbis.generic.server.hotfix import ImagingFixes

from java.util import ArrayList
import sys
import os

helper = MasterDataRegistrationHelper(sys.path)
api = CommonServiceProvider.getApplicationContext().getBean(ApplicationServerApi.INTERNAL_SERVICE_NAME)
sessionToken = api.loginAsSystem()
uploadRequired = ImagingFixes.isUploadRequired(sessionToken, "/IMAGING/NANONIS/SXM_COLLECTION")
print("======================== eln-lims-imaging-nanonis-adapter data-model xls ingestion start ========================")
sessionWorkspaceFiles = helper.uploadToAsSessionWorkspace(sessionToken, "imaging-nanonis-data-model.xls")
importData = ImportData(ImportFormat.EXCEL, [sessionWorkspaceFiles[0]])
importOptions = ImportOptions(ImportMode.UPDATE_IF_EXISTS)
importResult = api.executeImport(sessionToken, importData, importOptions)

print("======================== eln-lims-imaging-nanonis-adapter data-model xls ingestion end ========================")
print(importResult.getObjectIds())

print("======================== eln-lims-imaging-nanonis-adapter data upload start ========================")

def get_property(key, default_value):
    property_configurer = CommonServiceProvider.getApplicationContext().getBean("propertyConfigurer")
    properties = property_configurer.getResolvedProps()
    return properties.getProperty(key, default_value)

from java.lang import ProcessBuilder, String
from java.io import BufferedReader, InputStreamReader
from java.nio.charset import StandardCharsets

venv_path = get_property("imaging-nanonis.venv-path", None)
print("VENV_PATH", venv_path)


# skip if just upgrade
if uploadRequired:

    if venv_path is None:
        raise ValueError("Venv path not configured!")

    if not os.path.exists(venv_path) or len(os.listdir(venv_path)) <= 1:
        command = "python3 -m venv " + venv_path + " && "

        command += venv_path + "/bin/pip3 install -r " + sys.path[-1] + "/../scripts/python_requirements.txt"
        command += " && " + venv_path + "/bin/pip3 list"

        full_command = ["bash", "-c", command]

        pb = ProcessBuilder(full_command)
        process = pb.start()

        reader = BufferedReader(InputStreamReader(process.getInputStream()))

        line = reader.readLine()
        while line:
            print(line)
            line = reader.readLine()

        exitCode = process.waitFor()

        if exitCode != 0:
            print("Error during")
            error = String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8)
            raise ValueError("Error during virtual environment setup:" + error)


    print("registering imaging-nanonis data!")
    ImagingFixes.registerExamples(sys.path[-1], "imaging-nanonis", venv_path)

print("======================== eln-lims-imaging-nanonis-adapter data upload end ========================")

api.logout(sessionToken)



