#
# Copyright 2026 ETH Zuerich, Scientific IT Services
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

import ch.ethz.sis.shared.log.classic.core.LogCategory as LogCategory
import ch.ethz.sis.shared.log.classic.impl.LogFactory as LogFactory

import traceback
import json
from java.lang import Throwable

import ch.systemsx.cisd.openbis.generic.server.CommonServiceProvider as CommonServiceProvider


OPERATION_LOG = LogFactory.getLogger(LogCategory.OPERATION, LogFactory)

def exportSciCat(context, params):

    sciCatUrl = CommonServiceProvider.tryToGetProperty('sci-cat-exports-api.sci-cat-url')
    httpProxyURL = CommonServiceProvider.tryToGetProperty('sci-cat-exports-api.httpProxyURL')
    httpProxyPort = CommonServiceProvider.tryToGetProperty('sci-cat-exports-api.httpProxyPort')

    exportData = params.get("exportData")

    nodeExportList = exportData['nodeExportList']
    withLevelsBelow = exportData['withLevelsBelow']
    withObjectsAndDataSetsParents = exportData['withObjectsAndDataSetsParents']
    withObjectsAndDataSetsChildren = exportData['withObjectsAndDataSetsChildren']
    withObjectsAndDataSetsOtherSpaces = exportData['withObjectsAndDataSetsOtherSpaces']
    formats = exportData['formats']

    identifiers = []
    for identifier in nodeExportList:
        identifiers.append({
            'kind': identifier['kind'],
            'permId': identifier['permId'],
        })

    identifiers = json.dumps(identifiers)

    derived = exportData["derived"]
    metadata = exportData["metaData"]


    result = {
        "result": "HELLO SCI-CAT!" + str(metadata),
        "error": None
    }


    return result