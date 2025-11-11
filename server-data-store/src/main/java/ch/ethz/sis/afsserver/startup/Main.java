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
package ch.ethz.sis.afsserver.startup;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import ch.ethz.sis.afsserver.server.Server;
import ch.ethz.sis.shared.startup.Configuration;

public class Main
{

    public static void main(String[] args) throws Exception
    {
        try
        {
            System.out.println("Current Working Directory: " + (new File("")).getCanonicalPath());
            System.out.println("Configuration Location: " + (new File(args[0])).getCanonicalPath());

            System.setProperty("java.awt.headless", "true");
            Configuration configuration =
                    new Configuration(List.of(AtomicFileSystemServerParameter.class), args[0]);

            Server server = new Server(configuration);
            Thread.currentThread().join();
        } catch (Exception e)
        {
            System.out.println(e);
            Arrays.stream(e.getStackTrace()).forEach(System.out::println);
            throw e;
        }
    }
}
