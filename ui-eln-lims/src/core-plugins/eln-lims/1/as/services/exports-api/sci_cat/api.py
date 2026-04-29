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
from ch.ethz.sis.openbis.generic.asapi.v3.dto.service import CustomASServiceExecutionOptions
from ch.ethz.sis.openbis.generic.asapi.v3.dto.service.id import CustomASServiceCode
from ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id import EntityTypePermId
from ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.id import ExperimentIdentifier
from ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.create import SampleCreation
from ch.ethz.sis.openbis.generic.asapi.v3.dto.space.id import SpacePermId

import traceback
import json
import datetime
from java.lang import Throwable, String
from java.util import ArrayList
import time

import ch.systemsx.cisd.openbis.generic.server.CommonServiceProvider as CommonServiceProvider



from java.util import Map
from java.net import URL, URLEncoder, Proxy, InetSocketAddress
from javax.net.ssl import HttpsURLConnection
from java.io import BufferedInputStream, FileOutputStream, OutputStreamWriter
from java.io import BufferedReader, InputStreamReader, OutputStreamWriter, FileInputStream, BufferedOutputStream
from java.net import SocketTimeoutException
from java.nio.file import Path


from ro_crate.api import exportRoCrate, checkStatues, downloadRoCrate, RO_CRATE_EXPORT_ZIP_NAME
from util import sendMail, sendMailFailure, resultDict


OPERATION_LOG = LogFactory.getLogger(LogCategory.OPERATION, LogFactory)

REQUIRED_PUBLICATION_PROPS = ["NAME", "PUBLICATION.DESCRIPTION", "PUBLICATION.ABSTRACT", "PUBLICATION.CREATOR", "PUBLICATION.PUBLISHER"]
SEMI_REQUIRED_PROPS = {
    "PUBLICATION.TYPE" : "--PLACEHOLDER--",
    "PUBLICATION.STATUS" : "--PLACEHOLDER--",
    "PUBLICATION.PUBLICATION_YEAR" : datetime.date.today().strftime('%Y-%m-%d'),
}

def isSciCatEnabled(context, params):
    is_enabled = CommonServiceProvider.tryToGetProperty('exports-api.sci-cat.enabled')
    return is_enabled

def exportSciCat(context, params):

    import threading
    import time

    def worker():
        sessionToken = context.getSessionToken()
        date = datetime.date.today().strftime('%Y-%m-%dT %H:%M:%S')
        print(date, "SciCat export thread started by", sessionToken)

        exportSciCat_withEmail(context, params)

        print("SciCat export thread done. Starting time: "+ date+" token: "+sessionToken)

    t = threading.Thread(target=worker)
    t.start()

    return resultDict("STARTED")



def createNewPublication(sessionToken, v3, properties):

    groupPrefix = ''
    # groupPrefix = 'ERROR_'
    sampleCreation = SampleCreation()
    sampleCreation.setTypeId(EntityTypePermId('PUBLICATION'))
    sampleCreation.setExperimentId(ExperimentIdentifier('/' + groupPrefix + 'PUBLICATIONS/' + groupPrefix
                                                        + 'PUBLIC_REPOSITORIES/' + groupPrefix + 'PUBLICATIONS_COLLECTION'))
    sampleCreation.setSpaceId(SpacePermId(groupPrefix + 'PUBLICATIONS'))
    from java.util import Arrays
    from java.lang.reflect import Array

    for key in properties.keys():
        prop = properties[key]
        print(key, prop, prop.__class__, isinstance(prop, unicode))
        if isinstance(prop, ArrayList):
            sampleCreation.setProperty(key, prop)
        else:
            sampleCreation.setProperty(key, prop)

    for required_prop in REQUIRED_PUBLICATION_PROPS:
        if sampleCreation.getProperty(required_prop) is None:
            return resultDict(None, "Missing required property:" + required_prop)

    for property in SEMI_REQUIRED_PROPS.keys():
        if sampleCreation.getProperty(property) is None:
            sampleCreation.setProperty(property, SEMI_REQUIRED_PROPS[property])

    try:
        id = v3.createSamples(sessionToken, [sampleCreation])
        print("ID:", id)
        return resultDict(id.get(0).getPermId())
    except Throwable as e:
        return resultDict(None, e)


def exportSciCat_withEmail(context, params):

    sessionToken = context.getSessionToken()
    v3 = context.getApplicationService()
    userEmail = v3.getSessionInformation(sessionToken).getPerson().getEmail()
    mailClient = CommonServiceProvider.createEMailClient()

    publicationProps = params.get('exportData')["publicationProps"]
    print("Received publication properties:", publicationProps)
    publicationResult = createNewPublication(sessionToken, v3, publicationProps)
    print("PUBLICATION_RESULT", publicationResult)
    if publicationResult["error"] is not None:
        sendMailFailure(mailClient, userEmail, "SciCat export failed during creation of publication with exception:\n" + publicationResult["error"])
        return

    print("PUBLICATION_RESULT", publicationResult)
    publicationPermId = publicationResult["result"]

    exportData = params.get("exportData")
    nodeExportList = exportData['nodeExportList']
    nodeExportList.append({'kind': "SAMPLE", 'permId': publicationPermId})
    print("nodeExportList", nodeExportList)

    roCrateExport = exportRoCrate(context, params, False)

    if roCrateExport["error"] is not None:
        sendMailFailure(mailClient, userEmail, "SciCat export failed during RO-Crate step with exception:\n" + roCrateExport["error"])
        return

    jobId = roCrateExport["result"]["jobId"]
    download_result = getRoCrateExportToWorkspace(context, Map.of("jobId", jobId))

    if download_result["error"] is not None:
        sendMailFailure(mailClient, userEmail, "SciCat export failed while getting RO-Crate export with exception:\n" + download_result["error"])
        return

    sciCatOutput = sendToSciCat(context, Map.of("accessToken", params.get("accessToken")))

    print("SCI_CAT_OUTPUT", sciCatOutput)


    if "error" in sciCatOutput:
        sendMailFailure(mailClient, userEmail, "SciCat export failed during sending data with exception:\n" + sciCatOutput["error"])
        return

    sendMail(mailClient, userEmail, "Your export has been received by SciCat, once it is imported, you will receive another email.", "SciCat received your export:\n")

    response = sciCatOutput["result"]
    status = response.statusCode()
    body = json.loads(response.body())
    if status == 201:
        sciCatDetailUrl = CommonServiceProvider.tryToGetProperty('exports-api.sci-cat.detail.url') + "/detail/"
        links = ""
        for key in body.keys():
            value = str(body[key])
            links += "\t" + key + " -> " + sciCatDetailUrl + URLEncoder.encode(value, "UTF-8") + "\n"
        sendMail(mailClient, userEmail, links, "SciCat export results:\n")
    elif status == 202:

        jobId = body["jobId"]
        pollResult = pollSciCatImport(context, Map.of("jobId", jobId, "accessToken", params.get("accessToken")))

        if pollResult["error"] is not None:
            sendMailFailure(mailClient, userEmail, "SciCat export failed while getting results with exception:\n" + pollResult["error"])
            return

        #TODO get ids and prepare EMAIL once SciCat implements this path
        sendMail(mailClient, userEmail, "", "SciCat export results:\n")

    else:
        status = "Status:" + str(status) + "\n"
        sendMailFailure(mailClient, userEmail, "SciCat returned unexpected response:\n" + status + body)


def getRoCrateExportToWorkspace(context, params):

    jobId = params.get('jobId')

    flag = False
    count  = int(CommonServiceProvider.tryToGetProperty('exports-api.sci-cat.timeout.count'))
    sleep  = int(CommonServiceProvider.tryToGetProperty('exports-api.sci-cat.timeout.sleep'))
    while flag == False:
        if count < 0:
            return {
                "error": "timeout waiting for response"
            }

        result = checkStatues(context, params, jobId)
        print(result)
        if result["error"] is not None:
            return resultDict(None, result["error"])
        if result["result"]["status"] and result["result"]["status"] == "COMPLETED":
            OPERATION_LOG.info(result)
            flag = True
        elif result["result"]["status"] and result["result"]["status"] == "FAILED":
            OPERATION_LOG.error(result)
            return resultDict(None, result["result"]["errors"][0])
        count -= 1

        time.sleep(sleep)


    download_result = downloadRoCrate(context, params)

    if download_result["error"] is not None:
        return resultDict(None, download_result["error"])

    sessionWorkspaceProvider = CommonServiceProvider.getSessionWorkspaceProvider()
    sessionToken = context.getSessionToken()

    session_workspace = sessionWorkspaceProvider.getSessionWorkspace(sessionToken)

    file_path = Path.of(session_workspace.toPath().toString(), RO_CRATE_EXPORT_ZIP_NAME)

    if file_path.toFile().exists() == False:
        return resultDict(None, "Could not find file: " + path)

    return resultDict("COMPLETED")

def sendToSciCat(context, params):

    accessToken = params.get('accessToken')
    sessionWorkspaceProvider = CommonServiceProvider.getSessionWorkspaceProvider()
    sessionToken = context.getSessionToken()

    session_workspace = sessionWorkspaceProvider.getSessionWorkspace(sessionToken)

    file_path = Path.of(session_workspace.toPath().toString(), RO_CRATE_EXPORT_ZIP_NAME)

    print("Session Workspace:", file_path)

    sciCatUrl = CommonServiceProvider.tryToGetProperty('exports-api.sci-cat.export.url') + "/api/v1/ro-crate"
    httpProxyURL = CommonServiceProvider.tryToGetProperty('exports-api.sci-cat.http.proxy.url')
    httpProxyPort = CommonServiceProvider.tryToGetProperty('exports-api.sci-cat.http.proxy.port')

    sciCatUrl = sciCatUrl + "/import"
    output = upload_file_with_proxy(sciCatUrl, file_path.toString(), accessToken, proxy_host=httpProxyURL, proxy_port=httpProxyPort)


    # print("Validation flow")
    # sciCatUrl = sciCatUrl + "/validate"
    # output = upload_file_with_proxy(sciCatUrl, file_path.toString(), accessToken, proxy_host=httpProxyURL, proxy_port=httpProxyPort)
    #
    # jobId = output['result']["jobId"]
    # print("JOB_ID:", jobId)
    # pollResult = pollSciCatImport(context, Map.of("jobId", jobId, "accessToken", params.get("accessToken")))
    # print("POLL_RESULT:", pollResult)



    return output

def pollSciCatImport(context, params):
    jobId = params.get('jobId')
    accessToken = params.get('accessToken')

    sciCatUrl = CommonServiceProvider.tryToGetProperty('exports-api.sci-cat.export.url')

    flag = False
    count  = int(CommonServiceProvider.tryToGetProperty('exports-api.sci-cat.timeout.count'))
    sleep  = int(CommonServiceProvider.tryToGetProperty('exports-api.sci-cat.timeout.sleep'))
    result = {}
    while flag == False:
        if count < 0:
            return resultDict(None, "timeout waiting for response")

        result = checkStatusSciCat(sciCatUrl, accessToken, jobId)
        print("SciCat Result:", result)
        if result["error"] is not None:
            return resultDict(None, result["error"])
        if result["result"]["status"] and result["result"]["status"] == "COMPLETED":
            OPERATION_LOG.info(result)
            flag = True
        elif result["result"]["status"] and result["result"]["status"] == "FAILED":
            flag = True
        count -= 1

        time.sleep(sleep)

    return result

def checkStatusSciCat(url, accessToken, jobId):
    try:

        httpProxyURL = CommonServiceProvider.tryToGetProperty('exports-api.sci-cat.http.proxy.url')
        httpProxyPort = CommonServiceProvider.tryToGetProperty('exports-api.sci-cat.http.proxy.port')

        if jobId is not None:
            status_url = url + "/status/" + jobId
        else:
            status_url = url + "/status"
        headers = {
            'Accept': 'application/json',
            'Content-Type': 'application/json',
            'api-key': accessToken
        }

        code, output = https_get(status_url, headers, proxy_host=httpProxyURL, proxy_port=httpProxyPort)

        error = None
        if code < 300:
            output = json.loads(output)
        else:
            error = output
            output = None

        return resultDict(output, error)
    except Throwable as e:
        OPERATION_LOG.error("Error occurred: %s" % e, e)
        return resultDict(None, e)

def upload_file_with_proxy(url, file_path, accessToken, proxy_host=None, proxy_port=None):
    '''
        Pure Java implementation of multi-part upload of data to zenodo.org
    '''
    import java.net.URI as URI
    import java.net.InetSocketAddress as InetSocketAddress
    import java.net.ProxySelector as ProxySelector
    import java.net.http.HttpClient as HttpClient
    import java.net.http.HttpRequest as HttpRequest
    import java.net.http.HttpResponse as HttpResponse
    import java.nio.file.Path as Path
    import java.lang.System as System

    path = Path.of(file_path)

    if path.toFile().exists() == False:
        return resultDict(None, "Could not find file: " + path)

    if proxy_host is None or proxy_host == "":
        client = HttpClient.newHttpClient()
    else:
        client = HttpClient.newBuilder() \
            .proxy(ProxySelector.of(InetSocketAddress(proxy_host, int(proxy_port)))) \
            .build()

    BodyPublishers = HttpRequest.BodyPublishers
    request = (HttpRequest.newBuilder()
           .uri(URI.create(url))
           .header("Content-Type", 'application/zip')
           .header('api-key', accessToken)
           .POST(BodyPublishers.ofFile(path))
           # .POST(BodyPublishers.concat(BodyPublishers.ofFile(path)))
           .build())

    response = client.send(request, HttpResponse.BodyHandlers.ofString())

    status = response.statusCode()
    print("UPLOAD_RESPONSE", status, response.body())
    if status >= 300:
        if response.body() == u'':
            error_message = "HTTP status:" + str(status)
        else:
            body = json.loads(response.body())
            if 'message' in body:
                error_message = body['message']
            else:
                error_message = body['errors'][0]['message']
            print(error_message)
        return {
            "error": error_message
        }

    return {
        "result": response
    }


def https_get(base_url, headers, message=None, proxy_host=None, proxy_port=None):
    try:
        if message is not None:
            # URL-encode the message to make it safe for query strings
            encoded_msg = URLEncoder.encode(message, "UTF-8")

            # Build the full URL with the parameter
            full_url = "%s?message=%s" % (base_url, encoded_msg)
        else:
            full_url = base_url
        print("Requesting:", full_url)

        # Open HTTPS connection
        url = URL(full_url)
        # --- Proxy setup ---
        if proxy_host and proxy_port:
            proxy = Proxy(Proxy.Type.HTTP, InetSocketAddress(proxy_host, proxy_port))
            conn = url.openConnection(proxy)
        else:
            conn = url.openConnection()

        # conn = url.openConnection()
        conn.setRequestMethod("GET")

        # --- Timeouts ---
        timeout = 30 * 1000
        conn.setConnectTimeout(timeout)
        conn.setReadTimeout(timeout)

        # Add any extra headers
        if headers:
            for key, value in headers.items():
                conn.setRequestProperty(key, value)
        # conn.setRequestProperty("Accept", "application/json")  # optional

        # Read response
        code = conn.getResponseCode()
        print("Response Code:", code)

        reader = BufferedReader(InputStreamReader(conn.getInputStream(), "UTF-8"))

        output = ""
        line = reader.readLine()
        output += line
        while line is not None:
            line = reader.readLine()
            if line is not None:
                output += line

        conn.disconnect()

        return code, output
    except Throwable as e:
        OPERATION_LOG.error("Error occurred: %s" % e, e)
        return 999, e