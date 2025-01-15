package ch.ethz.sis.afsserver.worker.proxy;

import java.util.List;
import java.util.Map;

import ch.ethz.sis.afsapi.dto.File;
import ch.ethz.sis.afsapi.dto.FreeSpace;
import ch.ethz.sis.afsserver.server.observer.APICall;
import ch.ethz.sis.afsserver.server.observer.APIServerObserver;
import ch.ethz.sis.afsserver.server.observer.impl.DummyServerObserver;
import ch.ethz.sis.afsserver.startup.AtomicFileSystemServerParameter;
import ch.ethz.sis.afsserver.worker.AbstractProxy;
import ch.ethz.sis.shared.startup.Configuration;
import lombok.NonNull;

public class InterceptorProxy extends AbstractProxy
{

    private APIServerObserver apiServerObserver;

    public InterceptorProxy(Configuration configuration, AbstractProxy nextProxy) throws Exception
    {
        super(nextProxy);

        this.apiServerObserver = configuration.getSharableInstance(AtomicFileSystemServerParameter.apiServerObserver);

        if (apiServerObserver == null)
        {
            apiServerObserver = new DummyServerObserver();
        }
    }

    @Override
    public List<File> list(@NonNull String sourceOwner, @NonNull String source, @NonNull Boolean recursively) throws Exception
    {
        return (List<File>) apiServerObserver.duringAPICall(nextProxy,
                new APICallImpl("list", Map.of("sourceOwner", sourceOwner, "source", source, "recursively", recursively))
                {
                    @Override public Object executeDefault() throws Exception
                    {
                        return nextProxy.list(sourceOwner, source, recursively);
                    }
                });
    }

    @Override
    public byte[] read(@NonNull String sourceOwner, @NonNull String source, @NonNull Long offset, @NonNull Integer limit) throws Exception
    {
        return (byte[]) apiServerObserver.duringAPICall(nextProxy,
                new APICallImpl("read", Map.of("sourceOwner", sourceOwner, "source", source, "offset", offset, "limit", limit))
                {
                    @Override public Object executeDefault() throws Exception
                    {
                        return nextProxy.read(sourceOwner, source, offset, limit);
                    }
                });
    }

    @Override
    public Boolean write(@NonNull String sourceOwner, @NonNull String source, @NonNull Long offset, @NonNull byte[] data) throws Exception
    {
        return (Boolean) apiServerObserver.duringAPICall(nextProxy,
                new APICallImpl("write", Map.of("sourceOwner", sourceOwner, "source", source, "offset", offset, "data", data))
                {
                    @Override public Object executeDefault() throws Exception
                    {
                        return nextProxy.write(sourceOwner, source, offset, data);
                    }
                });
    }

    @Override
    public Boolean delete(@NonNull String sourceOwner, @NonNull String source) throws Exception
    {
        return (Boolean) apiServerObserver.duringAPICall(nextProxy,
                new APICallImpl("delete", Map.of("sourceOwner", sourceOwner, "source", source))
                {
                    @Override public Object executeDefault() throws Exception
                    {
                        return nextProxy.delete(sourceOwner, source);
                    }
                });
    }

    @Override
    public Boolean copy(@NonNull String sourceOwner, @NonNull String source, @NonNull String targetOwner, @NonNull String target) throws Exception
    {
        return (Boolean) apiServerObserver.duringAPICall(nextProxy,
                new APICallImpl("copy", Map.of("sourceOwner", sourceOwner, "source", source, "targetOwner", targetOwner, "target", target))
                {
                    @Override public Object executeDefault() throws Exception
                    {
                        return nextProxy.copy(sourceOwner, source, targetOwner, target);
                    }
                });
    }

    @Override
    public Boolean move(@NonNull String sourceOwner, @NonNull String source, @NonNull String targetOwner, @NonNull String target) throws Exception
    {
        return (Boolean) apiServerObserver.duringAPICall(nextProxy,
                new APICallImpl("move", Map.of("sourceOwner", sourceOwner, "source", source, "targetOwner", targetOwner, "target", target))
                {
                    @Override public Object executeDefault() throws Exception
                    {
                        return nextProxy.move(sourceOwner, source, targetOwner, target);
                    }
                });
    }

    @Override public @NonNull Boolean create(@NonNull final String owner, @NonNull final String source, @NonNull final Boolean directory)
            throws Exception
    {
        return (Boolean) apiServerObserver.duringAPICall(nextProxy,
                new APICallImpl("create", Map.of("owner", owner, "source", source, "directory", directory))
                {
                    @Override public Object executeDefault() throws Exception
                    {
                        return nextProxy.create(owner, source, directory);
                    }
                });
    }

    @Override public @NonNull FreeSpace free(@NonNull final String owner, @NonNull final String source) throws Exception
    {
        return (FreeSpace) apiServerObserver.duringAPICall(nextProxy,
                new APICallImpl("free", Map.of("owner", owner, "source", source))
                {
                    @Override public Object executeDefault() throws Exception
                    {
                        return nextProxy.free(owner, source);
                    }
                });
    }

    private static abstract class APICallImpl implements APICall
    {

        private final String method;

        private final Map<String, Object> params;

        public APICallImpl(String method, Map<String, Object> params)
        {
            this.method = method;
            this.params = params;
        }

        @Override public String getMethod()
        {
            return method;
        }

        @Override public Map<String, Object> getParams()
        {
            return params;
        }
    }
}
