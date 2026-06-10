package org.apache.logging.log4j;

/**
 * Handles the small part of Log4j's parameterized message syntax used by POI.
 * JUL does not substitute "{}" placeholders, so the bridge expands them before
 * delegating to the JUL-backed logger.
 */
final class Log4jMessageFormatter
{
    private Log4jMessageFormatter()
    {
    }

    static String format(String message, Object... params)
    {
        String template = String.valueOf(message);
        if (params == null || params.length == 0)
        {
            return template;
        }

        StringBuilder builder = new StringBuilder(template.length() + params.length * 8);
        int position = 0;
        int paramIndex = 0;
        while (paramIndex < params.length)
        {
            int placeholder = template.indexOf("{}", position);
            if (placeholder < 0)
            {
                break;
            }
            builder.append(template, position, placeholder);
            builder.append(String.valueOf(params[paramIndex++]));
            position = placeholder + 2;
        }
        builder.append(template.substring(position));

        // Preserve values without matching "{}" placeholders instead of dropping context.
        while (paramIndex < params.length)
        {
            builder.append(' ').append(String.valueOf(params[paramIndex++]));
        }
        return builder.toString();
    }
}
