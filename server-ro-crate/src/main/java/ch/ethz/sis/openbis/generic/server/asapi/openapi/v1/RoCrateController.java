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

package ch.ethz.sis.openbis.generic.server.asapi.openapi.v1;

import ch.ethz.sis.openbis.generic.asapi.v3.IApplicationServerApi;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.SearchResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.Space;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.fetchoptions.SpaceFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.search.SpaceSearchCriteria;
import ch.ethz.sis.openbis.generic.server.config.ServicePropertiesReader;
import ch.systemsx.cisd.common.spring.HttpInvokerUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.util.List;
import java.util.logging.Logger;

@RestController
@RequestMapping("/ro-crate")
//@Tag(name = "ro-crate", description = "RO-Crate related operations")
public class RoCrateController {

    private Logger logger = Logger.getLogger("RoCrateController");

    @Autowired
    ServicePropertiesReader servicePropertiesReader;

//    @Operation(summary = "Export RO-Crate from identifiers",
//            description = "Exports a RO-Crate using provided DOI identifiers.",
//            responses = {
//                    @ApiResponse(responseCode = "200", description = "RO-Crate exported",
//                            content = @Content(mediaType = "application/ld+json")),
//                    @ApiResponse(responseCode = "400", description = "Invalid identifiers",
//                            content = @Content(mediaType = "text/plain")),
//                    @ApiResponse(responseCode = "501", description = "ZIP export not implemented",
//                            content = @Content(mediaType = "text/plain"))
//            })
    @PostMapping(value = "/export",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = "application/ld+json")
    public ResponseEntity<?> exportPublication(
            @RequestBody List<String> identifiers,
            @RequestHeader("Accept") List<MediaType> accept) {
        return null;
    }

//    @Operation(summary = "Validate a RO-Crate",
//            description = "Validates an uploaded RO-Crate in JSON-LD or ZIP format.",
//            responses = {
//                    @ApiResponse(responseCode = "200", description = "Validation result returned",
//                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
//                    @ApiResponse(responseCode = "400", description = "Invalid RO-Crate",
//                            content = @Content)
//            })
    @PostMapping(value = "/validate",
            consumes = { "application/ld+json", "application/zip" },
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> validateRoCrate(InputStream inputStream) {
        return null;
    }

//    @Operation(summary = "Import a RO-Crate",
//            description = "Imports and processes a RO-Crate from a JSON-LD document.",
//            responses = {
//                    @ApiResponse(responseCode = "201", description = "Import successful",
//                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
//            })
    @PostMapping(value = "/import",
            consumes = "application/ld+json",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> importRoCrate(InputStream inputStream) {
        return null;
    }

    private static final String URL = "http://localhost:8888/openbis/openbis" + IApplicationServerApi.SERVICE_URL;

    @GetMapping("/ping")
//    @Operation(summary = "Ping test", description = "Simple health check to verify the API is running")
    public ResponseEntity<String> ping() {
        IApplicationServerApi v3 =
                HttpInvokerUtils.createServiceStub(IApplicationServerApi.class, URL, 15234234);

        // login to obtain a session token
        String sessionToken = v3.login("system", "changeit");

        // invoke other API methods using the session token, for instance search for spaces
        SearchResult<Space>
                spaces = v3.searchSpaces(sessionToken, new SpaceSearchCriteria(), new SpaceFetchOptions());
        System.out.println("Number of spaces: " + spaces.getObjects().size());

        // logout to release the resources related with the session
        v3.logout(sessionToken);

        String URL = servicePropertiesReader.getOpenBISUrl() +"/openbis/openbis" + IApplicationServerApi.SERVICE_URL;
//        IApplicationServerApi
//                v3 = HttpInvokerUtils.createServiceStub(IApplicationServerApi.class,
//                servicePropertiesReader.getOpenBISUrl(),
//                servicePropertiesReader.getOpenBISTimeout());

        try
        {
            // login to obtain a session token
          //  String sessionToken = v3.login("admin", "password");
            logger.info("testing logging");
            return ResponseEntity.ok("RO-Crate API is up and running! : " + sessionToken);
        } catch (Exception e){
            e.printStackTrace();
            throw e;
        }


    }

//    @GetMapping("/debug")
//    @Operation(summary = "Ping test", description = "Simple health check to verify the API is running")
//    public ResponseEntity<String> debug() {
//        return ResponseEntity.ok("RO-Crate API is up and running! : " + servicePropertiesReader.toString());
//    }

}
