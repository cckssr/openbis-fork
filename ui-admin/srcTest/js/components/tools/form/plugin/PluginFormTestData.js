import openbis from '@srcTest/js/services/openbis.js'

const testDynamicPropertyJythonPlugin = new openbis.Plugin()
testDynamicPropertyJythonPlugin.setName('TEST_DYNAMIC_PROPERTY_JYTHON')
testDynamicPropertyJythonPlugin.setPluginKind(openbis.PluginKind.JYTHON)
testDynamicPropertyJythonPlugin.setPluginType(
  openbis.PluginType.DYNAMIC_PROPERTY
)
testDynamicPropertyJythonPlugin.setEntityKinds([openbis.EntityKind.SAMPLE])
testDynamicPropertyJythonPlugin.setDescription(
  'Description of TEST_DYNAMIC_PROPERTY_JYTHON'
)
testDynamicPropertyJythonPlugin.setScript('def calculate():\n  return "abc"')

const testEntityValidationJythonPlugin = new openbis.Plugin()
testEntityValidationJythonPlugin.setName('TEST_ENTITY_VALIDATION_JYTHON')
testEntityValidationJythonPlugin.setPluginKind(openbis.PluginKind.JYTHON)
testEntityValidationJythonPlugin.setPluginType(
  openbis.PluginType.ENTITY_VALIDATION
)
testEntityValidationJythonPlugin.setEntityKinds([openbis.EntityKind.DATA_SET])
testEntityValidationJythonPlugin.setDescription(
  'Description of TEST_ENTITY_VALIDATION_JYTHON'
)
testEntityValidationJythonPlugin.setScript('def validate():\n  return True')

export default {
  testDynamicPropertyJythonPlugin,
  testEntityValidationJythonPlugin
}
