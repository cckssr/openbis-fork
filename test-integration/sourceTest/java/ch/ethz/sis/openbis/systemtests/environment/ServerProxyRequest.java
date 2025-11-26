package ch.ethz.sis.openbis.systemtests.environment;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.apache.commons.io.IOUtils;

public class ServerProxyRequest extends HttpServletRequestWrapper
{
    private boolean read;

    private byte[] bytes;

    public ServerProxyRequest(final HttpServletRequest request)
    {
        super(request);
    }

    @Override public ServletInputStream getInputStream() throws IOException
    {
        if (!read)
        {
            bytes = IOUtils.toByteArray(super.getInputStream());
            read = true;
        }
        return new ServletInputStream()
        {
            private final ByteArrayInputStream bytesStream = new ByteArrayInputStream(bytes);

            @Override public int read()
            {
                return bytesStream.read();
            }

            @Override public boolean isReady()
            {
                return true;
            }

            @Override public boolean isFinished()
            {
                return bytesStream.available() == 0;
            }

            @Override public void setReadListener(final ReadListener readListener)
            {
            }

        };
    }
}
