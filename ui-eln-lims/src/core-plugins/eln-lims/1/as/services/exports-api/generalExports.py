#
# Copyright 2016 ETH Zuerich, Scientific IT Services
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
import traceback
import json
from java.lang import Throwable

from java.net import URL, URLEncoder
from javax.net.ssl import HttpsURLConnection
from java.io import BufferedReader, InputStreamReader, OutputStreamWriter

import ch.ethz.sis.shared.log.classic.core.LogCategory as LogCategory
import ch.ethz.sis.shared.log.classic.impl.LogFactory as LogFactory
import ch.systemsx.cisd.openbis.generic.server.CommonServiceProvider as CommonServiceProvider


from exportsApi import getDownloadUrlFromASService, sendMail


OPERATION_LOG = LogFactory.getLogger(LogCategory.OPERATION, LogFactory)


def exportAll(executionContext, params):
    try:
        sessionToken = executionContext.getSessionToken()
        v3 = executionContext.getApplicationService()

        downloadResultMap = getDownloadUrlFromASService(sessionToken, params.get("entities"), v3)

        userEmail = v3.getSessionInformation(sessionToken).getPerson().getEmail()
        mailClient = CommonServiceProvider.createEMailClient()
        #Send Email
        sendMail(mailClient, userEmail, downloadResultMap.get('downloadURL'))

        return {
            "result": "Operation Successful",
            "error": None
        }
    except Throwable as e:
        OPERATION_LOG.error("Error occurred: %s" % e, e)
        return {
            "result": None,
            "error": "operation failed %s" % e
        }
