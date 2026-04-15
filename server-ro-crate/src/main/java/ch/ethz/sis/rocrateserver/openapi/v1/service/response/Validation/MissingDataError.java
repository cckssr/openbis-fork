package ch.ethz.sis.rocrateserver.openapi.v1.service.response.Validation;

public class MissingDataError implements ValidationError
{
    String errorType = "MissingDataError";

    String type;

    String path;

    public MissingDataError(String type, String path)
    {
        this.type = type;
        this.path = path;
    }

    @Override
    public String getErrorType()
    {
        return errorType;
    }

    public void setErrorType(String errorType)
    {
        this.errorType = errorType;
    }

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public String getPath()
    {
        return path;
    }

    public void setPath(String path)
    {
        this.path = path;
    }
}
