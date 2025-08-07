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

package ch.ethz.sis.openbis.generic.server.xls.importer.helper;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.TypeGroup;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.create.TypeGroupCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.fetchoptions.TypeGroupFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.id.ITypeGroupId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.id.TypeGroupId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.typegroup.update.TypeGroupUpdate;
import ch.ethz.sis.openbis.generic.server.xls.importer.ImportOptions;
import ch.ethz.sis.openbis.generic.server.xls.importer.delay.DelayedExecutionDecorator;
import ch.ethz.sis.openbis.generic.server.xls.importer.enums.ImportModes;
import ch.ethz.sis.openbis.generic.server.xls.importer.enums.ImportTypes;
import ch.ethz.sis.openbis.generic.server.xls.importer.utils.AttributeValidator;
import ch.ethz.sis.openbis.generic.server.xls.importer.utils.IAttribute;
import ch.ethz.sis.openbis.generic.server.xls.importer.utils.ImportUtils;
import ch.systemsx.cisd.common.exceptions.UserFailureException;

import java.util.List;
import java.util.Map;

public class TypeGroupImportHelper extends BasicImportHelper
{
    private enum Attribute implements IAttribute
    {
        Code("Code", true, true),
        Internal("Internal", false, false);

        private final String headerName;

        private final boolean mandatory;

        private final boolean upperCase;

        Attribute(String headerName, boolean mandatory, boolean upperCase)
        {
            this.headerName = headerName;
            this.mandatory = mandatory;
            this.upperCase = upperCase;
        }

        public String getHeaderName()
        {
            return headerName;
        }

        @Override
        public boolean isMandatory()
        {
            return mandatory;
        }

        @Override
        public boolean isUpperCase()
        {
            return upperCase;
        }
    }

    private final DelayedExecutionDecorator delayedExecutor;

    private final AttributeValidator<Attribute> attributeValidator;

    public TypeGroupImportHelper(DelayedExecutionDecorator delayedExecutor, ImportModes mode, ImportOptions options)
    {
        super(mode, options);
        this.delayedExecutor = delayedExecutor;
        this.attributeValidator = new AttributeValidator<>(Attribute.class);
    }

    @Override
    protected ImportTypes getTypeName()
    {
        return ImportTypes.TYPE_GROUP;
    }

    @Override
    protected void validateLine(Map<String, Integer> header, List<String> values) {
        String name = getValueByColumnName(header, values, Attribute.Code);
        String internal = getValueByColumnName(header, values, Attribute.Internal);
        if(!delayedExecutor.isSystem() && ImportUtils.isTrue(internal))
        {
            TypeGroup
                    st = delayedExecutor.getTypeGroup(new TypeGroupId(name), new TypeGroupFetchOptions());
            if(st == null) {
                throw new UserFailureException("Non-system user can not create new internal type groups!");
            }
        }
    }



    @Override
    protected boolean isObjectExist(Map<String, Integer> header, List<String> values)
    {
        String id = getValueByColumnName(header, values, Attribute.Code);

        final ITypeGroupId typeGroupId = new TypeGroupId(id);
        return delayedExecutor.getTypeGroup(typeGroupId, new TypeGroupFetchOptions()) != null;
    }

    @Override
    protected void createObject(Map<String, Integer> header, List<String> values, int page,
            int line)
    {
        String code = getValueByColumnName(header, values, Attribute.Code);
        String internal = getValueByColumnName(header, values, Attribute.Internal);

        TypeGroupCreation creation = new TypeGroupCreation();
        creation.setCode(code);
        creation.setManagedInternally(ImportUtils.isTrue(internal));

        delayedExecutor.createTypeGroup(creation);
    }

    @Override
    protected void updateObject(Map<String, Integer> header, List<String> values, int page,
            int line)
    {
        String code = getValueByColumnName(header, values, Attribute.Code);

        TypeGroupUpdate update = new TypeGroupUpdate();
        TypeGroupId typeGroupId = new TypeGroupId(code);
        update.setTypeGroupId(typeGroupId);
        update.setCode(code);

        delayedExecutor.updateTypeGroup(update);
    }

    @Override
    protected void validateHeader(Map<String, Integer> header)
    {
        attributeValidator.validateHeaders(Attribute.values(), header);
    }
}
