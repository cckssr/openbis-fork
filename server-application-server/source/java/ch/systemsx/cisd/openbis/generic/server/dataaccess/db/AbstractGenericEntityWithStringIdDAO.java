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

package ch.systemsx.cisd.openbis.generic.server.dataaccess.db;

import ch.ethz.sis.shared.log.classic.core.LogCategory;
import ch.ethz.sis.shared.log.classic.impl.LogFactory;
import ch.systemsx.cisd.openbis.generic.server.dataaccess.IStringIdDAO;
import ch.systemsx.cisd.openbis.generic.server.dataaccess.db.deletion.EntityHistoryCreator;
import ch.systemsx.cisd.openbis.generic.shared.basic.*;
import ch.ethz.sis.shared.log.classic.impl.Logger;
import org.hibernate.SessionFactory;

public abstract class AbstractGenericEntityWithStringIdDAO <T extends IStringIdHolder> extends AbstractGenericEntityWithCustomIdDAO<T, StringId> implements
        IStringIdDAO<T>
{
    private static final Logger operationLog = LogFactory.getLogger(LogCategory.OPERATION,
            AbstractGenericEntityWithStringIdDAO.class);

    protected AbstractGenericEntityWithStringIdDAO(final SessionFactory sessionFactory, final Class<T> entityClass,
            EntityHistoryCreator historyCreator)
    {
        super(sessionFactory, entityClass, historyCreator);
    }
}
