package ch.ethz.sis.openbis.generic.asapi.v3.dto.deletion.search;

import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.id.IObjectId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.IdSearchCriteria;
import ch.systemsx.cisd.base.annotation.JsonObject;

@JsonObject("as.dto.deletion.search.DeletedObjectIdSearchCriteria")
public class DeletedObjectIdSearchCriteria extends IdSearchCriteria<IObjectId>
{
}
