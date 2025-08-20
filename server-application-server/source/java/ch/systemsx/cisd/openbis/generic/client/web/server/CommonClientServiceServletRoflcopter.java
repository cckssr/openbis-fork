/*
 * Copyright ETH 2008 - 2023 Zürich, Scientific IT Services
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
package ch.systemsx.cisd.openbis.generic.client.web.server;

//import ch.systemsx.cisd.common.servlet.GWTRPCServiceExporter;
import ch.systemsx.cisd.openbis.JavelinMessage;
import ch.systemsx.cisd.openbis.generic.client.web.client.ICommonClientService;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The {@link GWTRPCServiceExporter} for the <i>generic</i> service.
 * <p>
 * <i>URL</i> mappings are: <code>/common</code> and <code>/genericopenbis/common</code>. The
 * encapsulated {@link ICommonClientService} service
 * implementation is expected to be defined as bean with name <code>common-service</code>.
 * </p>
 *
 * @author Christian Ribeaud
 */
@Controller
public final class CommonClientServiceServletRoflcopter
{

    @RequestMapping(value = { "/roflcopter", "/openbis/roflcopter" }, produces = {
            MediaType.APPLICATION_JSON_VALUE })
    public final String handleRequestExposed(final HttpEntity<String> request,
            final HttpServletResponse response) throws Exception
    {
        System.out.println("Got roflcopter: " + request.getBody());
        response.getOutputStream().write(request.getBody().getBytes());
        response.setStatus(200);
        return request.getBody() + " echoed for great justice!";

    }

    @PostMapping(value = { "/javelin", "/openbis/javelin" }, consumes = {
            MediaType.APPLICATION_JSON_VALUE })
    public final String handleJavelins(@RequestBody() JavelinMessage javelinMessage)
            throws Exception
    {
        return javelinMessage.javelinMessage + " num javelins: " + javelinMessage.numJavelins;
    }

    @PostMapping(value = { "/jsonld", "/openbis/jsonld" }, consumes = {
            "application/ld+json" }, produces = {
            "application/ld+json" })
    public final ResponseEntity<String> jsonLd(HttpEntity<String> entity) throws Exception
    {
        String val = entity.getBody() + " echoed for great justice!";
        ResponseEntity<String> stringResponseEntity = new ResponseEntity<>(val, HttpStatus.OK);
        return stringResponseEntity;
    }

    @PostMapping(value = { "/listeroo", "/openbis/listeroo" }, consumes = {
            MediaType.APPLICATION_JSON_VALUE })
    public final ResponseEntity<String> listeroo(HttpEntity<List<String>> entity) throws Exception
    {
        String val = entity.getBody().stream()
                .collect(Collectors.joining(";")) + " echoed for great justice!";
        ResponseEntity<String> stringResponseEntity =
                new ResponseEntity<>
                        (val, HttpStatus.ACCEPTED);
        return stringResponseEntity;


    }



}
