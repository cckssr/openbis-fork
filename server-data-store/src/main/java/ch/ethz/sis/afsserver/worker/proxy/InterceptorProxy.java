package ch.ethz.sis.afsserver.worker.proxy;

import java.util.List;
import java.util.Map;

import ch.ethz.sis.afsapi.dto.Chunk;
import ch.ethz.sis.afsapi.dto.File;
import ch.ethz.sis.afsapi.dto.FreeSpace;
import ch.ethz.sis.afsserver.server.observer.APICall;
import ch.ethz.sis.afsserver.server.observer.APIServerObserver;
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
    }

    @Override
    public File[] list(@NonNull String sourceOwner, @NonNull String source, @NonNull Boolean recursively) throws Exception
    {
        if (apiServerObserver != null)
        {
            return (File[]) apiServerObserver.duringAPICall(nextProxy,
                    new APICallImpl("list", Map.of("sourceOwner", sourceOwner, "source", source, "recursively", recursively))
                    {
                        @Override public Object executeDefault() throws Exception
                        {
                            return nextProxy.list(sourceOwner, source, recursively);
                        }
                    });
        } else
        {
            return nextProxy.list(sourceOwner, source, recursively);
        }
    }

    @Override
    public Chunk[] read(@NonNull Chunk[] chunks) throws Exception
    {
        if (apiServerObserver != null)
        {
            return (Chunk[]) apiServerObserver.duringAPICall(nextProxy,
                    new APICallImpl("read", Map.of("chunks", chunks))
                    {
                        @Override public Object executeDefault() throws Exception
                        {
                            return nextProxy.read(chunks);
                        }
                    });
        } else
        {
            return nextProxy.read(chunks);
        }
    }

    @Override
    public Boolean write(@NonNull Chunk[] chunks) throws Exception
    {
        if (apiServerObserver != null)
        {
            return (Boolean) apiServerObserver.duringAPICall(nextProxy,
                    new APICallImpl("write", Map.of("chunks", chunks))
                    {
                        @Override public Object executeDefault() throws Exception
                        {
                            return nextProxy.write(chunks);
                        }
                    });
        } else
        {
            return nextProxy.write(chunks);
        }
    }

    @Override
    public Boolean delete(@NonNull String sourceOwner, @NonNull String source) throws Exception
    {
        if (apiServerObserver != null)
        {
            return (Boolean) apiServerObserver.duringAPICall(nextProxy,
                    new APICallImpl("delete", Map.of("sourceOwner", sourceOwner, "source", source))
                    {
                        @Override public Object executeDefault() throws Exception
                        {
                            return nextProxy.delete(sourceOwner, source);
                        }
                    });
        } else
        {
            return nextProxy.delete(sourceOwner, source);
        }
    }

    @Override
    public Boolean copy(@NonNull String sourceOwner, @NonNull String source, @NonNull String targetOwner, @NonNull String target) throws Exception
    {
        if (apiServerObserver != null)
        {
            return (Boolean) apiServerObserver.duringAPICall(nextProxy,
                    new APICallImpl("copy", Map.of("sourceOwner", sourceOwner, "source", source, "targetOwner", targetOwner, "target", target))
                    {
                        @Override public Object executeDefault() throws Exception
                        {
                            return nextProxy.copy(sourceOwner, source, targetOwner, target);
                        }
                    });
        } else
        {
            return nextProxy.copy(sourceOwner, source, targetOwner, target);
        }
    }

    @Override
    public Boolean move(@NonNull String sourceOwner, @NonNull String source, @NonNull String targetOwner, @NonNull String target) throws Exception
    {
        if (apiServerObserver != null)
        {
            return (Boolean) apiServerObserver.duringAPICall(nextProxy,
                    new APICallImpl("move", Map.of("sourceOwner", sourceOwner, "source", source, "targetOwner", targetOwner, "target", target))
                    {
                        @Override public Object executeDefault() throws Exception
                        {
                            return nextProxy.move(sourceOwner, source, targetOwner, target);
                        }
                    });
        } else
        {
            return nextProxy.move(sourceOwner, source, targetOwner, target);
        }
    }

    @Override public @NonNull Boolean create(@NonNull final String owner, @NonNull final String source, @NonNull final Boolean directory)
            throws Exception
    {
        if (apiServerObserver != null)
        {
            return (Boolean) apiServerObserver.duringAPICall(nextProxy,
                    new APICallImpl("create", Map.of("owner", owner, "source", source, "directory", directory))
                    {
                        @Override public Object executeDefault() throws Exception
                        {
                            return nextProxy.create(owner, source, directory);
                        }
                    });
        } else
        {
            return nextProxy.create(owner, source, directory);
        }
    }

    @Override public @NonNull FreeSpace free(@NonNull final String owner, @NonNull final String source) throws Exception
    {
        if (apiServerObserver != null)
        {
            return (FreeSpace) apiServerObserver.duringAPICall(nextProxy,
                    new APICallImpl("free", Map.of("owner", owner, "source", source))
                    {
                        @Override public Object executeDefault() throws Exception
                        {
                            return nextProxy.free(owner, source);
                        }
                    });
        } else
        {
            return nextProxy.free(owner, source);
        }
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
