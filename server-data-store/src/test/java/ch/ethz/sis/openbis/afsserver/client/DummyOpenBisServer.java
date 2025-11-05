/*
 *  Copyright ETH 2023 Zürich, Scientific IT Services
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package ch.ethz.sis.openbis.afsserver.client;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;

import org.springframework.remoting.httpinvoker.SimpleHttpInvokerServiceExporter;
import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.remoting.support.RemoteInvocationResult;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

public final class DummyOpenBisServer
{
    private final HttpServer httpServer;

    private OperationExecutor operationExecutor;

    public DummyOpenBisServer(int httpServerPort, String httpServerPath) throws IOException
    {
        httpServer = HttpServer.create(new InetSocketAddress(httpServerPort), 0);
        httpServer.createContext(httpServerPath, exchange ->
        {
            DummyInvoker inv = new DummyInvoker();
            inv.handle(exchange);
        });
    }

    public void start()
    {
        httpServer.start();
    }

    public void stop()
    {
        httpServer.stop(0);
    }

    public void setOperationExecutor(OperationExecutor operationExecutor)
    {
        this.operationExecutor = operationExecutor;
    }

    private class DummyInvoker extends SimpleHttpInvokerServiceExporter
    {

        @Override
        public void handle(HttpExchange exchange) throws IOException
        {
            try
            {
                RemoteInvocation invocation = super.readRemoteInvocation(exchange);

                final String method = invocation.getMethodName();
                final Object[] arguments = invocation.getArguments();

                RemoteInvocationResult remoteInvocationResult;

                try
                {
                    final Object result = operationExecutor.executeOperation(exchange.getRequestURI().toString(), method, arguments);
                    remoteInvocationResult = new RemoteInvocationResult(result);
                }catch(Throwable e){
                    remoteInvocationResult = new RemoteInvocationResult(new InvocationTargetException(e));
                }

                this.writeRemoteInvocationResult(exchange, remoteInvocationResult);
                exchange.close();
            } catch (ClassNotFoundException var4)
            {
                exchange.sendResponseHeaders(500, -1L);
                this.logger.error("Class not found during deserialization", var4);
            }
        }

    }

    public interface OperationExecutor {
        Object executeOperation(String url, String methodName, Object[] methodArguments);
    }

}
