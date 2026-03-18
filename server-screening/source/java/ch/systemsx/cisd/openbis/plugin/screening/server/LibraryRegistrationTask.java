/*
 * Copyright ETH 2011 - 2023 Zürich, Scientific IT Services
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
package ch.systemsx.cisd.openbis.plugin.screening.server;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;

import ch.rinn.restrictions.Private;
import ch.systemsx.cisd.base.exceptions.IOExceptionUnchecked;
import ch.systemsx.cisd.common.collection.TableMap;
import ch.systemsx.cisd.common.shared.basic.string.StringUtils;
import ch.systemsx.cisd.openbis.generic.server.IASyncAction;
import ch.systemsx.cisd.openbis.generic.server.dataaccess.IDAOFactory;
import ch.systemsx.cisd.openbis.generic.server.dataaccess.util.KeyExtractorFactory;
import ch.systemsx.cisd.openbis.generic.shared.ICommonServer;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.IEntityProperty;
import ch.systemsx.cisd.openbis.generic.shared.basic.dto.NewSamplesWithTypes;
import ch.systemsx.cisd.openbis.generic.shared.dto.EntityTypePE;
import ch.systemsx.cisd.openbis.generic.shared.dto.properties.EntityKind;
import ch.systemsx.cisd.openbis.generic.shared.util.EntityHelper;
import ch.systemsx.cisd.openbis.plugin.generic.shared.IGenericServer;
import ch.systemsx.cisd.openbis.plugin.screening.shared.basic.dto.ScreeningConstants;

/**
 * Saves genes, oligos and plates. Sends an email to specified address upon error or completion.
 * 
 * @author Izabela Adamczyk
 */
class LibraryRegistrationTask implements IASyncAction
{

    private static final String DELIM = " ";

    private final String sessionToken;

    private final List<NewSamplesWithTypes> newSamplesWithType;

    private final IGenericServer genericServer;

    private final ICommonServer commonServer;

    private final IDAOFactory daoFactory;

    public LibraryRegistrationTask(String sessionToken, List<NewSamplesWithTypes> newSamplesWithType,
            ICommonServer commonServer, IGenericServer server, IDAOFactory daoFactory)
    {
        this.sessionToken = sessionToken;
        this.newSamplesWithType = newSamplesWithType;
        this.commonServer = commonServer;
        this.genericServer = server;
        this.daoFactory = daoFactory;
    }

    private void registerOrUpdateSamples(Writer message) throws IOException
    {
        try
        {
            if (newSamplesWithType != null)
            {
                genericServer.registerOrUpdateSamples(sessionToken, newSamplesWithType);
                for (NewSamplesWithTypes s : newSamplesWithType)
                {
                    message.write("Successfuly saved " + s.getNewEntities().size()
                            + " samples of type " + s.getEntityType() + ".\n");
                }
            }
        } catch (RuntimeException ex)
        {
            message.write("ERROR: Plates and wells could not be saved!\n");
            message.write(ex.getMessage());
            throw ex;
        }
    }

    private String mergeGeneSymbols(String existingSymbols, String newSymbolString)
    {
        if (StringUtils.isBlank(newSymbolString) || newSymbolString.equals(existingSymbols))
        {
            return existingSymbols;
        }

        String[] newSymbols = newSymbolString.split("\\s+");
        StringBuilder result = new StringBuilder(existingSymbols);
        for (String newSymbol : newSymbols)
        {
            if (StringUtils.isBlank(newSymbol))
            {
                continue;
            }
            boolean alreadyExisting =
                    existingSymbols.startsWith(newSymbol + DELIM) || existingSymbols.endsWith(DELIM + newSymbol)
                            || (existingSymbols.indexOf(DELIM + newSymbol + DELIM) > 0);

            if (false == alreadyExisting)
            {
                result.append(DELIM);
                result.append(newSymbol);
            }
        }
        return result.toString();
    }

    @Override
    public boolean doAction(Writer messageWriter)
    {
        try
        {
            registerOrUpdateSamples(messageWriter);

            return true;
        } catch (IOException ex)
        {
            throw new IOExceptionUnchecked(ex);
        }
    }

    @Override
    public String getName()
    {
        return "Library registration";
    }

}