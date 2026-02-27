
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

RO_CRATE_URL_PROPERTY_KEY = 'ro-crate.server.url'

def exportRoCrate(context, params):
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
            'openbis.input-body-format' : "JSON"
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

def checkStatues(context, params):
    try:
        sessionToken = context.getSessionToken()
        ro_crate_url = CommonServiceProvider.tryToGetProperty(RO_CRATE_URL_PROPERTY_KEY)
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