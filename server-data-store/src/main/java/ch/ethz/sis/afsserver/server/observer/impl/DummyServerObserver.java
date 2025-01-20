/*
 * Copyright ETH 2022 - 2023 Zürich, Scientific IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.ethz.sis.afsserver.server.observer.impl;

import ch.ethz.sis.afsserver.server.APIServer;
import ch.ethz.sis.afsserver.server.Request;
import ch.ethz.sis.afsserver.server.Response;
import ch.ethz.sis.afsserver.server.Worker;
import ch.ethz.sis.afsserver.server.observer.APICall;
import ch.ethz.sis.afsserver.server.observer.APIServerObserver;
import ch.ethz.sis.afsserver.server.observer.ServerObserver;
import ch.ethz.sis.shared.startup.Configuration;

public class DummyServerObserver<CONNECTION> implements ServerObserver<CONNECTION>, APIServerObserver<CONNECTION>
{

    @Override
    public void init(Configuration configuration) throws Exception
    {

    }

    @Override
    public void beforeAPICall(Worker<CONNECTION> worker, Request request) throws Exception
    {

    }

    @Override public Object duringAPICall(final Worker<CONNECTION> worker, final APICall apiCall) throws Exception
    {
        return apiCall.executeDefault();
    }

    @Override
    public void afterAPICall(Worker<CONNECTION> worker, Request request, Response response) throws Exception
    {

    }

    @Override
    public void init(APIServer<CONNECTION, ?, ?, ?> apiServer, Configuration configuration) throws Exception
    {

    }

    @Override
    public void beforeStartup() throws Exception
    {

    }

    @Override
    public void beforeShutdown() throws Exception
    {

    }
}
