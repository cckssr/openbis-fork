#   Copyright ETH 2023 Zürich, Scientific IT Services
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
from java.lang import ProcessBuilder
from java.lang import String
from java.nio.charset import StandardCharsets
import sys
import inspect
import os

from ch.systemsx.cisd.openbis.generic.server.hotfix import ImagingFixes

helper = MasterDataRegistrationHelper(sys.path)
api = CommonServiceProvider.getApplicationContext().getBean(ApplicationServerApi.INTERNAL_SERVICE_NAME)
sessionToken = api.loginAsSystem()
sessionWorkspaceFiles = helper.uploadToAsSessionWorkspace(sessionToken, "imaging-test-data-model.xls")
print(sessionWorkspaceFiles)
importData = ImportData(ImportFormat.EXCEL, [sessionWorkspaceFiles[0]])
importOptions = ImportOptions(ImportMode.UPDATE_IF_EXISTS)
importResult = api.executeImport(sessionToken, importData, importOptions)

print("======================== imaging-test-data-model xls ingestion result ========================")
print(importResult.getObjectIds())


ImagingFixes.registerExamples(sys.path[-1], "imaging-test")

# raise ValueError("test break point")


api.logout(sessionToken)
print("======================== imaging-test-data-model xls ingestion result ========================")


