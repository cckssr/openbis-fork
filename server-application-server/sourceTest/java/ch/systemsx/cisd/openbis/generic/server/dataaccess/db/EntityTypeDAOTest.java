/*
 * Copyright ETH 2009 - 2023 Zürich, Scientific IT Services
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

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.testng.annotations.Test;

import ch.systemsx.cisd.openbis.generic.server.dataaccess.IEntityTypeDAO;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.DataTypeCode;
import ch.systemsx.cisd.openbis.generic.shared.dto.EntityTypePE;
import ch.systemsx.cisd.openbis.generic.shared.dto.EntityTypePropertyTypePE;
import ch.systemsx.cisd.openbis.generic.shared.dto.ExperimentPropertyPE;
import ch.systemsx.cisd.openbis.generic.shared.dto.ExperimentTypePE;
import ch.systemsx.cisd.openbis.generic.shared.dto.PropertyTypePE;
import ch.systemsx.cisd.openbis.generic.shared.dto.properties.EntityKind;
import junit.framework.Assert;

/**
 * Test cases for corresponding {@link EntityTypeDAO} class.
 * 
 * @author Christian Ribeaud
 */
@Test(groups = { "db", "materialType" })
public final class EntityTypeDAOTest extends AbstractDAOTest
{

    private static final String MATERIAL = "MATERIAL";

    private static final String MATERIAL_TYPE = "material-type";

    static final void checkEntityType(final EntityTypePE entityType, final String entityTypeCode)
    {
        assertNotNull(entityType);
        if (entityTypeCode != null)
        {
            assertEquals(entityTypeCode, entityType.getCode());
        } else
        {
            assertNotNull(entityType.getCode());
        }
        assertNotNull(entityType.getDescription());
        assertNotNull(entityType.getId());
    }

    static final List<EntityTypePE> listSortedEntyTypes(final IEntityTypeDAO entityTypeDAO)
    {
        final List<EntityTypePE> entityTypes = entityTypeDAO.listEntityTypes();
        Collections.sort(entityTypes, new Comparator<EntityTypePE>()
            {
                //
                // Comparator
                //

                @Override
                public final int compare(final EntityTypePE o1, final EntityTypePE o2)
                {
                    return o1.getCode().compareTo(o2.getCode());
                }
            });
        return entityTypes;
    }


    private EntityTypePropertyTypePE assignPropertyType(ExperimentTypePE type,
            PropertyTypePE propertyType)
    {
        EntityTypePropertyTypePE assignment =
                createAssignment(EntityKind.EXPERIMENT, type, propertyType);
        daoFactory.getEntityPropertyTypeDAO(EntityKind.EXPERIMENT)
                .createEntityPropertyTypeAssignment(assignment);
        return assignment;
    }

}
