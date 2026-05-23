/*
 * Copyright ETH 2015 - 2023 Zürich, Scientific IT Services
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
package ch.systemsx.cisd.openbis.generic.server.dataaccess.util;

import java.time.Instant;
import java.util.Date;

import org.hibernate.Session;
import org.hibernate.SessionFactory;

/**
 * @author Franz-Josef Elmer
 */
public class UpdateUtils
{
    public static Instant getTransactionInstant(SessionFactory sessionFactory)
    {
        return getTransactionTimestamp(sessionFactory).toInstant();
    }

    public static Date getTransactionTimeStamp(SessionFactory sessionFactory) {
        return getTransactionTimestamp(sessionFactory);
    }

    private static java.sql.Timestamp getTransactionTimestamp(SessionFactory sessionFactory)
    {
        Session session = sessionFactory.getCurrentSession();
        return session.doReturningWork(conn -> {
            try (var st = conn.createStatement();
                    // On PostgreSQL, CURRENT_TIMESTAMP == transaction_timestamp()
                    var rs = st.executeQuery("select current_timestamp")) {
                rs.next();
                return rs.getTimestamp(1);
            }
        });
    }

}
