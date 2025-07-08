/*
 *  Copyright ETH 2025 Zürich, Scientific IT Services
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

package ch.ethz.sis.openbis.generic.server.asapi.openapi.v1.service;

import ch.ethz.sis.openbis.generic.asapi.v3.IApplicationServerApi;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.SearchResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.Space;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.fetchoptions.SpaceFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.search.SpaceSearchCriteria;
import ch.systemsx.cisd.common.logging.LogInitializer;
import ch.systemsx.cisd.common.spring.HttpInvokerUtils;

import java.util.logging.LogManager;
import java.util.logging.Logger;

public class TestOpenBisConnection
{


    private static final String URL = "http://localhost:8888/openbis/openbis" + IApplicationServerApi.SERVICE_URL;

    private static final int TIMEOUT = 10000;

    public static void main(String[] args)
    {
        LogInitializer.init();
        Logger logger = Logger.getLogger("TestOpenBisConnection");
        // get a reference to AS API
        IApplicationServerApi v3 = HttpInvokerUtils.createServiceStub(IApplicationServerApi.class, URL, TIMEOUT);

        // login to obtain a session token
        String sessionToken = v3.login("admin", "password");

        // invoke other API methods using the session token, for instance search for spaces
        SearchResult<Space>
                spaces = v3.searchSpaces(sessionToken, new SpaceSearchCriteria(), new SpaceFetchOptions());
        System.out.println("Number of spaces: " + spaces.getObjects().size());
        logger.info("Number of spaces: " + spaces.getObjects().size());
        // logout to release the resources related with the session
        v3.logout(sessionToken);
    }



}
