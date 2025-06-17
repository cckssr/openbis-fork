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
import jarray

# To obtain the openBIS URL
from ch.systemsx.cisd.openbis.dss.generic.server import DataStoreServer
from ch.systemsx.cisd.openbis.dss.generic.shared import ServiceProvider

# Zip Format
from java.io import File, BufferedInputStream
from java.io import FileInputStream
from java.util.zip import ZipEntry, ZipOutputStream, CRC32
from org.apache.commons.io import FileUtils

from HTMLParser import HTMLParser

#V3 API - Files
from ch.ethz.sis.openbis.generic.asapi.v3.dto.service import CustomASServiceExecutionOptions
from ch.ethz.sis.openbis.generic.asapi.v3.dto.service.id import CustomASServiceCode

#Logging
from ch.systemsx.cisd.common.logging import LogCategory
from org.apache.log4j import Logger
operationLog = Logger.getLogger(str(LogCategory.OPERATION) + ".exportsApi.py")

OPENBISURL = DataStoreServer.getConfigParameters().getServerURL() + "/openbis/openbis"
V3_DSS_BEAN = "data-store-server_INTERNAL"

def displayResult(isOk, tableBuilder, result=None, errorMessage="Operation Failed"):
    if isOk:
        tableBuilder.addHeader("STATUS")
        tableBuilder.addHeader("MESSAGE")
        tableBuilder.addHeader("RESULT")
        row = tableBuilder.addRow()
        row.setCell("STATUS", "OK")
        row.setCell("MESSAGE", "Operation Successful")
        row.setCell("RESULT", result)
    else:
        tableBuilder.addHeader("STATUS")
        tableBuilder.addHeader("Error")
        row = tableBuilder.addRow()
        row.setCell("STATUS", "FAIL")
        row.setCell("Error", errorMessage)


def getDownloadUrlFromASService(sessionToken, exportModel):
    v3 = ServiceProvider.getV3ApplicationService()
    id = CustomASServiceCode('xls-export-extended')

    options = CustomASServiceExecutionOptions()
    options.withParameter('nodeExportList', exportModel.get('nodeExportList'))
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