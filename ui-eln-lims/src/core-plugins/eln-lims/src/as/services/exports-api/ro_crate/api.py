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

from java.net import URL, URLEncoder
from javax.net.ssl import HttpsURLConnection
from java.io import BufferedInputStream, FileOutputStream, OutputStreamWriter
from java.io import BufferedReader, InputStreamReader, OutputStreamWriter
from java.net import SocketTimeoutException

import ch.systemsx.cisd.openbis.generic.server.CommonServiceProvider as CommonServiceProvider


OPERATION_LOG = LogFactory.getLogger(LogCategory.OPERATION, LogFactory)

RO_CRATE_URL_PROPERTY_KEY = 'exports-api.ro-crate.url'
RO_CRATE_EXPORT_ZIP_NAME = "ro_crate_result.zip"

def exportRoCrate(context, params, withEmail=True):
    try:
        sessionToken = context.getSessionToken()

        ro_crate_url = CommonServiceProvider.tryToGetProperty(RO_CRATE_URL_PROPERTY_KEY)

        ro_crate_export = ro_crate_url + "/export"

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

        headers = {
            'Accept': 'application/json',
            'Content-Type': 'application/json',
            'Export': 'application/zip',
            'api-key': sessionToken,
            'openbis.import-compatible': "True",
            'openbis.metadata-pdf': str(formats['pdf']),
            'openbis.metadata-xlsx': str(formats['xlsx']),
            'openbis.dataset-data': str(formats['data']),
            'openbis.afs-data': str(formats['afsData']),
            'openbis.with-levels-above': "True",
            'openbis.with-levels-below': str(withLevelsBelow),
            'openbis.with-objects-and-dataSets-children': str(withObjectsAndDataSetsChildren),
            'openbis.with-objects-and-dataSets-parents': str(withObjectsAndDataSetsParents),
            'openbis.with-objects-and-dataSets-other-spaces': str(withObjectsAndDataSetsOtherSpaces),
            'openbis.input-body-format' : "JSON",
            'openbis.send-email' : str(withEmail),
        }

        error = None
        code, output = https_post(ro_crate_export, identifiers, headers)

        if code < 300:
            output = json.loads(output)
        else:
            error = output
            output = None

        result = {
            "result": output,
            "error": error
        }
        return result
    except Throwable as e:
        OPERATION_LOG.error("Error occurred: %s" % e, e)
        return {
            "result": None,
            "error": e
        }

def checkStatues(context, params, jobId=None):
    try:
        sessionToken = context.getSessionToken()
        ro_crate_url = CommonServiceProvider.tryToGetProperty(RO_CRATE_URL_PROPERTY_KEY)
        if jobId is not None:
            status_url = ro_crate_url + "/status/" + jobId
        else:
            status_url = ro_crate_url + "/status"
        headers = {
            'Accept': 'application/json',
            'Content-Type': 'application/json',
            'api-key': sessionToken
        }

        code, output = https_get(status_url, headers)

        error = None
        if code < 300:
            output = json.loads(output)
        else:
            error = output
            output = None

        result = {
            "result": output,
            "error": error
        }

        return result
    except Throwable as e:
        OPERATION_LOG.error("Error occurred: %s" % e, e)
        return {
            "result": None,
            "error": e
        }

def getRoCrateUrl(context, params):
    ro_crate_url = CommonServiceProvider.tryToGetProperty(RO_CRATE_URL_PROPERTY_KEY)
    return ro_crate_url

def downloadRoCrate(context, params):
    sessionWorkspaceProvider = CommonServiceProvider.getSessionWorkspaceProvider()
    sessionToken = context.getSessionToken()


    ro_crate_url = CommonServiceProvider.tryToGetProperty(RO_CRATE_URL_PROPERTY_KEY)
    download_url = ro_crate_url + "/download"
    headers = {
        'Accept': 'application/json',
        'Content-Type': 'application/json',
        'api-key': sessionToken,
        'jobid': params.get("jobId")
    }

    dateStr = params.get("openbis.job.time")
    if dateStr is None:
        zip_name = RO_CRATE_EXPORT_ZIP_NAME
    else:
        zip_name = "ro_crate." + dateStr + ".zip"
    print("Detected zip name:", zip_name)

    conn = None
    try:
        url = URL(download_url)
        conn = url.openConnection()
        conn.setRequestMethod("GET")
        conn.setDoOutput(True)
        # conn.setConnectTimeout(connect_timeout)
        # conn.setReadTimeout(read_timeout)

        # --- Default headers ---
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        conn.setRequestProperty("Accept", "*/*")  # accept any type (for file)

        # --- Add custom headers ---
        if headers:
            for key, value in headers.items():
                conn.setRequestProperty(key, value)

        # --- Get HTTP response code ---
        code = conn.getResponseCode()
        print("Response Code:", code)

        if code < 300:
            print("Saving response to session workspace")
            input_stream = BufferedInputStream(conn.getInputStream())
            sessionWorkspaceProvider.write(sessionToken, zip_name, input_stream)

            download_url = CommonServiceProvider.tryToGetProperty("download-url")
            return {
                "result": download_url + "/openbis/openbis/download?sessionID=" + sessionToken + "&filePath=" + zip_name,
                "fileName": zip_name,
                "error": None
            }
        else:
            return {
                "result": None,
                "error": "RO-CRATE server returned error: " +str(code) + ": " + conn.getResponseMessage()
            }

    except SocketTimeoutException as e:
        print("Request timed out:", e)
    except Exception as e:
        print("Error during request:", e)
        return {
            "result": None,
            "error": e
        }
    finally:

        try:
            if conn:
                conn.disconnect()
        except:
            pass



def https_post(url_str, data, headers=None):
    try:
        url = URL(url_str)
        conn = url.openConnection()
        conn.setRequestMethod("POST")
        conn.setDoOutput(True)

        # --- Timeouts ---
        timeout = 30 * 1000
        conn.setConnectTimeout(timeout)
        conn.setReadTimeout(timeout)

        # Add any extra headers
        if headers:
            for key, value in headers.items():
                conn.setRequestProperty(key, value)

        # Write POST body
        if data is not None:
            writer = OutputStreamWriter(conn.getOutputStream(), "UTF-8")
            writer.write(data)
            writer.flush()
            writer.close()

        # Read response
        code = conn.getResponseCode()

        output = ""
        reader = BufferedReader(InputStreamReader(conn.getInputStream(), "UTF-8"))
        line = reader.readLine()
        output += line
        while line is not None:
            line = reader.readLine()
            if line is not None:
                output += line
        reader.close()

        conn.disconnect()

        return code, output
    except Throwable as e:
        OPERATION_LOG.error("Error occurred: %s" % e, e)
        return 999, e


def https_get(base_url, headers, message=None):
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
        conn = url.openConnection()
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