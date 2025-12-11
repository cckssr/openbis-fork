package ch.ethz.sis.rocrateserver.openapi.v1.service.response;

public class ErrorResponse
{
    String message;

    public ErrorResponse(String message)
    {
        this.message = message;
    }

    public ErrorResponse()
    {
    }

    public String getMessage()
    {
        return message;
    }
}
