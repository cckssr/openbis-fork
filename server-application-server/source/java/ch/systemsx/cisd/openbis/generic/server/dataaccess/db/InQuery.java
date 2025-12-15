/*
 * Copyright ETH 2016 - 2023 Zürich, Scientific IT Services
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
package ch.systemsx.cisd.openbis.generic.server.dataaccess.db;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.query.NativeQuery;

public class InQuery<I, O>
{
    public List<O> withBatch(Session session,
            String inQuery,
            String inParameter,
            List<I> inArguments,
            Map<String, Object> fixParams)
    {
        List<O> result = new ArrayList<O>(inArguments.size());
        int fixParamsSize = (fixParams == null) ? 0 : fixParams.size();

        InQueryScroller<I> scroller = new InQueryScroller<>(inArguments, fixParamsSize);
        List<I> partialInArguments;

        while ((partialInArguments = scroller.next()) != null)
        {

            NativeQuery<O> query = session.createNativeQuery(inQuery);

            query.setParameter(inParameter, partialInArguments);

            if (fixParams != null)
            {
                for (Map.Entry<String, Object> e : fixParams.entrySet())
                {
                    query.setParameter(e.getKey(), e.getValue());
                }
            }

            result.addAll(query.getResultList());
        }

        return result;
    }
}
