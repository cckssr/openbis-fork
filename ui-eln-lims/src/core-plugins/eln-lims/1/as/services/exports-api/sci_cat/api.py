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



from java.net import URL, URLEncoder
from javax.net.ssl import HttpsURLConnection
from java.io import BufferedInputStream, FileOutputStream, OutputStreamWriter
from java.io import BufferedReader, InputStreamReader, OutputStreamWriter
from java.net import SocketTimeoutException


OPERATION_LOG = LogFactory.getLogger(LogCategory.OPERATION, LogFactory)

def exportSciCat(context, params):

    sciCatUrl = CommonServiceProvider.tryToGetProperty('sci-cat-exports-api.sci-cat-url')
    httpProxyURL = CommonServiceProvider.tryToGetProperty('sci-cat-exports-api.httpProxyURL')
    httpProxyPort = CommonServiceProvider.tryToGetProperty('sci-cat-exports-api.httpProxyPort')

    accessToken = params.get('accessToken')

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

    # login_url = sciCatUrl +  '/api/v3/auth/login'
    # https_get()

    result = {
        "result": "HELLO SCI-CAT!" + str(metadata),
        "error": None
    }


    return result

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