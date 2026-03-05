from ch.ethz.sis.openbis.generic.server.xls.importer import ImportOptions
from ch.ethz.sis.openbis.generic.server.xls.importer import XLSImport
from ch.ethz.sis.openbis.generic.server.xls.importer.enums import ImportModes
from ch.systemsx.cisd.common.exceptions import UserFailureException
from java.util import ArrayList

import ch.systemsx.cisd.openbis.generic.server.CommonServiceProvider as CommonServiceProvider

import ch.ethz.sis.shared.log.classic.core.LogCategory as LogCategory
import ch.ethz.sis.shared.log.classic.impl.LogFactory as LogFactory

OPERATION_LOG = LogFactory.getLogger(LogCategory.OPERATION, LogFactory)

def getMode(parameters):
    update_mode = parameters.get("mode", "FAIL_IF_EXISTS")
    if update_mode == "IGNORE_EXISTING":
        return ImportModes.IGNORE_EXISTING
    elif update_mode == "FAIL_IF_EXISTS":
        return ImportModes.FAIL_IF_EXISTS
    elif update_mode == "UPDATE_IF_EXISTS":
        return ImportModes.UPDATE_IF_EXISTS
    else:
        raise UserFailureException("Update mode has to be one of following: IGNORE_EXISTING FAIL_IF_EXISTS UPDATE_IF_EXISTS but was " + (
            str(update_mode) if update_mode else "None"))


def getImportOptions(parameters):
    options = ImportOptions()
    allowedSampleTypes = parameters.get("allowedSampleTypes")

    options.setExperimentsByType(parameters.get("experimentsByType", None))
    options.setSpacesByType(parameters.get("spacesByType", None))
    options.setDefinitionsOnly(False)
    options.setDisallowEntityCreations(False)
    options.setIgnoreVersioning(False)
    options.setRenderResult(True)
    return options


def process(context, parameters):
    method = parameters.get("method")
    result = None

    if method == "import":
        result = _import(context, parameters)
    return result


def _import(context, parameters):
    fileName = parameters.get("fileName")

    if not fileName:
        raise UserFailureException("fileName parameter is mandatory.")

    session_token = context.sessionToken
    mode = getMode(parameters)
    options = getImportOptions(parameters)

    importExecutor = CommonServiceProvider.tryToGetBean("importExecutor")
    result = importExecutor.executeImport(session_token, mode, options, [fileName])

    return result.getObjectIds()
