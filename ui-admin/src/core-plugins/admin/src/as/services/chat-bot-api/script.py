from ch.systemsx.cisd.openbis.generic.server import CommonServiceProvider

chatBotLlmServerUrl = CommonServiceProvider.tryToGetProperty("admin.as.chat-bot-api.chat-bot-llm-server-url", "https://chat-bot-server:8080/query")

def process(context, parameters):
    method = parameters.get("method")
    result = None

    if method == "ask":
        result = getAsk(context, parameters)

    return result

def getAsk(context, parameters):
    message = parameters.get("query")
    session_id = parameters.get("session_id")
    if session_id is None:
        session_id = "test-session-id"

    answer = "This is your answer with " + str(session_id) + " and " + str(chatBotLlmServerUrl) + " to: " + str(message)
    return {
    "answer" : answer,
    "sessionId" : session_id
    }