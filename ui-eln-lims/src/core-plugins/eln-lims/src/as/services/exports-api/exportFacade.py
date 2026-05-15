#
# Copyright 2016 ETH Zuerich, Scientific IT Services
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
import traceback
import json
from java.lang import Throwable


import ch.ethz.sis.shared.log.classic.core.LogCategory as LogCategory
import ch.ethz.sis.shared.log.classic.impl.LogFactory as LogFactory




from general.api import exportAll
from ro_crate.api import exportRoCrate, checkStatues, getRoCrateUrl
from sci_cat.api import exportSciCat, isSciCatEnabled
from zenodo.api import exportZenodo
from research_collection.api import exportResearchCollection, isResearchCollectionEnabled




OPERATION_LOG = LogFactory.getLogger(LogCategory.OPERATION, LogFactory)



def process(context, params):
    method = params.get("method")

    if method == "exportAll":
        return exportAll(context, params)

    elif method == "exportRoCrate":
        return exportRoCrate(context, params)
    elif method == "statusRoCrateJobs":
        return checkStatues(context, params)
    elif method == "getRoCrateUrl":
        return getRoCrateUrl(context, params)

    elif method == "exportSciCat":
        return exportSciCat(context, params)
    elif method == "isSciCatEnabled":
        return isSciCatEnabled(context, params)

    elif method == "exportZenodo":
        return exportZenodo(context, params)

    elif method == "exportResearchCollection":
        return exportResearchCollection(context, params)
    elif method == "isResearchCollectionEnabled":
        return isResearchCollectionEnabled(context, params)
    else:
        OPERATION_LOG.error("No such method '%s'" % method)
        return {
            "result": None,
            "error": "No such method '%s'" % method
        }

