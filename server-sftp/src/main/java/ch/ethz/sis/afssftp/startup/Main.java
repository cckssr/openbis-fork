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
package ch.ethz.sis.afssftp.startup;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import ch.ethz.sis.afssftp.server.Server;
import ch.ethz.sis.shared.exception.ThrowableReason;
import ch.ethz.sis.shared.startup.Configuration;

public class Main
{

    public static void main(String[] args) throws Exception
    {
        try
        {
            Configuration configuration =
                    new Configuration(List.of(AfsSftpServerParameter.class), args[0]);

            Server server = new Server(configuration);
            Thread.currentThread().join();
        } catch (Exception e)
        {
            if (e.getCause() instanceof ThrowableReason)
            {
                System.out.println(((ThrowableReason) e.getCause()).getReason());
            }
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            System.out.println(sw);
            throw e;
        }
    }
}
