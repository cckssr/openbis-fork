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
from java.io import FileInputStream, BufferedOutputStream
from java.net import SocketTimeoutException
import jarray

import ch.systemsx.cisd.openbis.generic.server.CommonServiceProvider as CommonServiceProvider


OPERATION_LOG = LogFactory.getLogger(LogCategory.OPERATION, LogFactory)

RO_CRATE_URL_PROPERTY_KEY = 'imports-api.ro-crate.url'
RO_CRATE_EXPORT_ZIP_NAME = "ro_crate_result.zip"

def importRoCrate(context, params):
    try:
        sessionToken = context.getSessionToken()

        ro_crate_url = CommonServiceProvider.tryToGetProperty(RO_CRATE_URL_PROPERTY_KEY)

        ro_crate_import_url = ro_crate_url + "/import"

        importData = params.get("importData")

        importMode = importData['importMode']
        fileName = importData['fileName']
        projectIdentifier = importData['projectIdentifier']


        if fileName.endswith('.json'):
            contentType = "application/ld+json"
        else:
            contentType = "application/zip"


        headers = {
            'Accept': 'application/json',
            'Content-Type': contentType,
            'api-key': sessionToken,
            'openbis.import-mode' : importMode,
            'openbis.project-identifier' : projectIdentifier,
        }

        sessionWorkspaceProvider = CommonServiceProvider.getSessionWorkspaceProvider()
        workspaceFile = sessionWorkspaceProvider.getCanonicalFile(sessionToken, fileName)

        file_path = workspaceFile.getCanonicalPath()

        error = None
        code, output = https_post_file(ro_crate_import_url, file_path, headers)

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

def statusRoCrateImport(context, params):
    try:
        sessionToken = context.getSessionToken()
        ro_crate_url = CommonServiceProvider.tryToGetProperty(RO_CRATE_URL_PROPERTY_KEY)

        jobId = params.get("jobId")
        status_url = ro_crate_url + "/status/" + jobId

        headers = {
            'Accept': 'application/json',
            'Content-Type': 'application/json',
            'api-key': sessionToken
        }

        OPERATION_LOG.info("Requesting status for RO-Crate job:" + jobId)
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


def https_post_file(url_str, file_path, headers=None):
    try:
        url = URL(url_str)
        conn = url.openConnection()
        conn.setRequestMethod("POST")
        conn.setDoOutput(True)

        timeout = 30 * 1000
        conn.setConnectTimeout(timeout)
        conn.setReadTimeout(timeout)

        if headers:
            for key, value in headers.items():
                conn.setRequestProperty(key, value)

        # Stream file contents directly to the request body
        in_stream = BufferedInputStream(FileInputStream(file_path))
        out_stream = BufferedOutputStream(conn.getOutputStream())
        try:
            buf = jarray.zeros(8192, "b")  # byte array buffer
            n = in_stream.read(buf)
            while n != -1:
                out_stream.write(buf, 0, n)
                n = in_stream.read(buf)
            out_stream.flush()
        finally:
            in_stream.close()
            out_stream.close()

        code = conn.getResponseCode()

        output = ""
        reader = BufferedReader(InputStreamReader(conn.getInputStream(), "UTF-8"))
        line = reader.readLine()
        while line is not None:
            output += line
            line = reader.readLine()
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