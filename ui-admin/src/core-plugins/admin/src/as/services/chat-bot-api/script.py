from ch.systemsx.cisd.openbis.generic.server import CommonServiceProvider

import ch.ethz.sis.shared.log.classic.core.LogCategory as LogCategory
import ch.ethz.sis.shared.log.classic.impl.LogFactory as LogFactory

from java.net import URL
import json

OPERATION_LOG = LogFactory.getLogger(LogCategory.OPERATION, LogFactory)

chatBotLlmServerUrl = CommonServiceProvider.tryToGetProperty("admin.as.chat-bot-api.chat-bot-llm-server-url", "http://localhost:8080/api/v1/query")

def process(context, parameters):
    method = parameters.get("method")
    result = None

    if method == "ask":
        result = getAsk(context, parameters)

    return result


def getAsk(context, parameters):

    message = parameters.get("query")
    sessionToken = parameters.get("sessionToken")
    OPERATION_LOG.info("Asking chabot %s question: %s" % (chatBotLlmServerUrl, message))
    code, response = http_post(chatBotLlmServerUrl, sessionToken, json_data=json.dumps({
        "question": message,
        "openbis_version": "7.x"
    }))
    return {
        "answer" : response,
    }

def http_post(url, sessionToken, json_data):

    from java.net import URL
    from java.io import BufferedReader, InputStreamReader

    connection = URL(url).openConnection()
    connection.setRequestMethod("POST")
    connection.setDoOutput(True)
    connection.setRequestProperty("Content-Type", "application/json")
    connection.setRequestProperty("Accept", "application/json")
    connection.setRequestProperty("sessionToken", sessionToken)
    connection.setConnectTimeout(10000)
    connection.setReadTimeout(130000)

    output = connection.getOutputStream()
    output.write(json_data.encode("UTF-8"))
    output.close()

    response_code = connection.getResponseCode()

    OPERATION_LOG.info("Chabot response code: %s" % (response_code))
    print("Chabot response code: %s" % response_code)


    if response_code == 200:
        reader = BufferedReader(
            InputStreamReader(connection.getInputStream(), "UTF-8")
        )
    else:
        reader = BufferedReader(
            InputStreamReader(connection.getErrorStream(), "UTF-8")
        )

    lines = []
    line = reader.readLine()

    while line is not None:
        lines.append(line)
        line = reader.readLine()

    reader.close()
    connection.disconnect()

    response = json.loads("\n".join(lines))

    if response_code == 200:
        answer = response["answer"]
    else:
        answer = response["detail"]


    return response_code, answer