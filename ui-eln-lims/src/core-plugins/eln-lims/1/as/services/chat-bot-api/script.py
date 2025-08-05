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

    answer = "This is your answer with " + str(session_id) + " to: " + str(message)
    return {
    "answer" : answer,
    "sessionId" : session_id
    }