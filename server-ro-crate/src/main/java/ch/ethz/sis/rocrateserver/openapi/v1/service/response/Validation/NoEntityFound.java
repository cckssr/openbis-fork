package ch.ethz.sis.rocrateserver.openapi.v1.service.response.Validation;

public class NoEntityFound implements ValidationError
{

    @Override
    public String getErrorType()
    {
        return "NoEntityFound";
    }

}
