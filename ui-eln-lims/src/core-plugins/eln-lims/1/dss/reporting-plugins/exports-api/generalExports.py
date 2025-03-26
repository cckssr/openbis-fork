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
from java.lang import Throwable
from ch.systemsx.cisd.common.logging import LogCategory
from ch.systemsx.cisd.common.mail import EMailAddress
from ch.systemsx.cisd.openbis.dss.generic.shared import ServiceProvider
from org.apache.log4j import Logger

from exportsApi import displayResult, getDownloadUrlFromASService

operationLog = Logger.getLogger(str(LogCategory.OPERATION) + ".generalExports.py")

def process(tr, params, tableBuilder):
    method = params.get("method")
    isOk = False

    # Set user using the service

    tr.setUserId(userId)
    if method == "exportAll":
        #isOk = expandAndExport(tr, params)
        isOk = exportAll(tr, params)

    displayResult(isOk, tableBuilder)

def exportAll(tr, params):
    try:
        sessionToken = params.get('sessionToken')
        v3 = ServiceProvider.getV3ApplicationService()

        downloadResultMap = getDownloadUrlFromASService(sessionToken, params.get("entities"))

        userEmail = v3.getSessionInformation(sessionToken).getPerson().getEmail()
        mailClient = tr.getGlobalState().getMailClient()
        #Send Email
        sendMail(mailClient, userEmail, downloadResultMap.get('downloadURL'))

        return True
    except BaseException as e:
        operationLog.error("Error occurred: %s" % traceback.format_exc())
    except Throwable as e:
        operationLog.error("Error occurred: %s" % e, e)

def sendMail(mailClient, userEmail, downloadURL):
    replyTo = None
    fromAddress = None
    recipient1 = EMailAddress(userEmail)
    topic = "Export Ready"
    message = "Download a zip file with your exported data at: " + downloadURL
    mailClient.sendEmailMessage(topic, message, replyTo, fromAddress, recipient1)
    operationLog.info("--- MAIL ---" + " Recipient: " + userEmail + " Topic: " + topic + " Message: " + message)
