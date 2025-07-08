///*
// *  Copyright ETH 2025 Zürich, Scientific IT Services
// *
// *  Licensed under the Apache License, Version 2.0 (the "License");
// *  you may not use this file except in compliance with the License.
// *  You may obtain a copy of the License at
// *
// *       http://www.apache.org/licenses/LICENSE-2.0
// *
// *  Unless required by applicable law or agreed to in writing, software
// *  distributed under the License is distributed on an "AS IS" BASIS,
// *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// *  See the License for the specific language governing permissions and
// *  limitations under the License.
// *
// */
//
//package ch.ethz.sis.openbis.generic.server.config;
//
//import io.swagger.v3.oas.models.OpenAPI;
//import io.swagger.v3.oas.models.info.Info;
//import io.swagger.v3.oas.models.servers.Server;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//import java.util.List;
//import java.util.stream.Collectors;
//
//@Configuration
//public class OpenApiConfig {
//
//    @Bean
//    public OpenAPI customOpenAPI(OpenApiProperties props) {
//        Info info = new Info()
//                .title(props.getInfo().getTitle())
//                .version(props.getInfo().getVersion())
//                .description(props.getInfo().getDescription());
//
//        List<Server> servers = props.getServers().stream()
//                .map(s -> new Server().url(s.getUrl()).description(s.getDescription()))
//                .collect(Collectors.toList());
//
//        return new OpenAPI().info(info).servers(servers);
//    }
//}
