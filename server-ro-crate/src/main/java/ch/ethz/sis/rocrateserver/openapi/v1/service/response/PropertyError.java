package ch.ethz.sis.rocrateserver.openapi.v1.service.response;

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

}
