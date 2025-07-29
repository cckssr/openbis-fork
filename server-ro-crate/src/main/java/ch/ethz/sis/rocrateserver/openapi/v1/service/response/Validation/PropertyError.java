package ch.ethz.sis.rocrateserver.openapi.v1.service.response.Validation;

public class PropertyError implements ValidationError
{

    /**
     * Gets or Sets errorType
     */
    public enum ErrorTypeEnum
    {
        PROPERTY_ERROR("PropertyError");

        private final String value;

        ErrorTypeEnum(String value)
        {
            this.value = value;
        }

        public String getValue()
        {
            return value;
        }

        @Override
        public String toString()
        {
            return String.valueOf(value);
        }

        public static ErrorTypeEnum fromValue(String value)
        {
            for (ErrorTypeEnum b : ErrorTypeEnum.values())
            {
                if (b.value.equals(value))
                {
                    return b;
                }
            }
            throw new IllegalArgumentException("Unexpected value '" + value + "'");
        }
    }

    private ErrorTypeEnum errorType;

    private String nodeId;

    private String property;

    private String message;

    @Override
    public String getErrorType()
    {
        return errorType.getValue();
    }

    public PropertyError(String nodeId, String property, String message)
    {
        this.errorType = ErrorTypeEnum.PROPERTY_ERROR;
        this.nodeId = nodeId;
        this.property = property;
        this.message = message;
    }

    public void setErrorType(
            ErrorTypeEnum errorType)
    {
        this.errorType = errorType;
    }

    public String getNodeId()
    {
        return nodeId;
    }

    public void setNodeId(String nodeId)
    {
        this.nodeId = nodeId;
    }

    public String getProperty()
    {
        return property;
    }

    public void setProperty(String property)
    {
        this.property = property;
    }

    public String getMessage()
    {
        return message;
    }

    public void setMessage(String message)
    {
        this.message = message;
    }
}
