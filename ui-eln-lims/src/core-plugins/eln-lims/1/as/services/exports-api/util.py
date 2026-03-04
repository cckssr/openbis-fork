#
# Copyright 2016-2026 ETH Zuerich, Scientific IT Services
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
import jarray

import ch.systemsx.cisd.openbis.generic.server.CommonServiceProvider as CommonServiceProvider

# Zip Format
from java.io import File, BufferedInputStream
from java.io import FileInputStream
from java.util.zip import ZipEntry, ZipOutputStream, CRC32
from org.apache.commons.io import FileUtils

from HTMLParser import HTMLParser


from ch.systemsx.cisd.common.mail import EMailAddress

#V3 API - Files
from ch.ethz.sis.openbis.generic.asapi.v3.dto.service import CustomASServiceExecutionOptions
from ch.ethz.sis.openbis.generic.asapi.v3.dto.service.id import CustomASServiceCode

#Logging
import ch.ethz.sis.shared.log.classic.core.LogCategory as LogCategory
import ch.ethz.sis.shared.log.classic.impl.LogFactory as LogFactory
from java.lang import Throwable


OPERATION_LOG = LogFactory.getLogger(LogCategory.OPERATION, LogFactory)


def sendMail(mailClient, userEmail, downloadURL):
    replyTo = None
    fromAddress = None
    recipient1 = EMailAddress(userEmail)
    topic = "Export Ready"
    message = "Download a zip file with your exported data at: " + downloadURL
    mailClient.sendEmailMessage(topic, message, replyTo, fromAddress, recipient1)
    OPERATION_LOG.info("--- MAIL ---" + " Recipient: " + userEmail + " Topic: " + topic + " Message: " + message)


def getDownloadUrlFromASService(sessionToken, exportModel, v3):
    id = CustomASServiceCode('xls-export-extended')
    options = CustomASServiceExecutionOptions()

    # The new export service doesn't understand nodes without a type and of type GROUP used by the Zenodo form to deal with publications
    nodeExportListForService = []
    for node in exportModel.get('nodeExportList'):
        if node.get('type') != 'GROUP':
            nodeExportListForService.append(node)
    options.withParameter('nodeExportList', nodeExportListForService)
    #options.withParameter('nodeExportList', exportModel.get('nodeExportList'))
    options.withParameter('withEmail', exportModel.get('withEmail'))
    options.withParameter('withImportCompatibility', exportModel.get('withImportCompatibility'))
    options.withParameter('formats', exportModel.get('formats'))

    downloadResultMap = v3.executeCustomASService(sessionToken, id, options)
    return downloadResultMap



# Removes temporal folder and zip
def cleanUp(tempDirPath, tempZipFilePath):
    FileUtils.forceDelete(File(tempDirPath))
    FileUtils.forceDelete(File(tempZipFilePath))


def addToZipFile(path, file, zos, deflated=True):
    zipEntry = ZipEntry(path[1:]) # Making paths relative to make them compatible with Windows zip implementation
    if not deflated:
        zipEntry.setMethod(ZipOutputStream.STORED)
        zipEntry.setSize(file.length())
        zipEntry.setCompressedSize(-1)
        crc = getFileCRC(file)
        zipEntry.setCrc(crc)
    else:
        zipEntry.setMethod(ZipOutputStream.DEFLATED)

    zos.putNextEntry(zipEntry)

    try:
        bis = BufferedInputStream(FileInputStream(file))
        bytes = jarray.zeros(1024, "b")
        length = bis.read(bytes)
        while length >= 0:
            zos.write(bytes, 0, length)
            length = bis.read(bytes)
    finally:
        zos.closeEntry()
        if bis is not None:
            bis.close()


def getFileCRC(file):
    bis = None
    crc = CRC32()
    try:
        bis = BufferedInputStream(FileInputStream(file))
        b = jarray.zeros(1024, "b")
        length = bis.read(b)
        while length != -1:
            crc.update(b, 0, length)
            length = bis.read(b)
    finally:
        if bis is not None:
            bis.close()
    return crc.getValue()


def getConfigurationProperty(transaction, propertyName):
    threadProperties = transaction.getGlobalState().getThreadParameters().getThreadProperties()
    try:
        return threadProperties.getProperty(propertyName)
    except:
        return None


def checkResponseStatus(response):
    status = response.getStatus()
    if status >= 300:
        reason = response.getReason()
        raise ValueError('Unsuccessful response from the server: %s %s' % (status, reason))


def isNonEmptyString(s):
    return isinstance(s, str) and bool(s.strip())


