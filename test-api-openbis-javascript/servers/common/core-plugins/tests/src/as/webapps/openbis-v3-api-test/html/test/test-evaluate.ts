import jquery from "./types/jquery"
import underscore from "./types/underscore"
import common from "./types/common"
import openbis from "./types/openbis.esm"

exports.default = new Promise((resolve) => {
    require(["jquery", "underscore", "openbis", "test/common", "test/openbis-execute-operations", "test/dtos"], function (
        $: jquery.JQueryStatic,
        _: underscore.UnderscoreStatic,
        openbisRequireJS,
        common: common.CommonConstructor,
        openbisExecuteOperations,
        dtos
    ) {
        var executeModule = function (moduleName: string, facade: openbis.openbis, dtos: openbis.bundle) {
            QUnit.module(moduleName)

            var testDynamicPropertyPlugin = function (assert, databasePlugin) {
                var c = new common(assert, dtos)
                c.start()

                c.login(facade)
                    .then(function () {
                        var creation = new dtos.PluginCreation()
                        creation.setName(c.generateId("plugin"))
                        creation.setPluginType(dtos.PluginType.DYNAMIC_PROPERTY)
                        creation.setScript("def calculate():\n  return 'testValue'")

                        return Promise.all([facade.createPlugins([creation]), c.createSample(facade)]).then(function ([pluginIds, sampleId]) {
                            var options = new dtos.DynamicPropertyPluginEvaluationOptions()
                            if (databasePlugin) {
                                options.setPluginId(pluginIds[0])
                            } else {
                                options.setPluginScript(creation.getScript())
                            }
                            options.setObjectId(sampleId)

                            return facade.evaluatePlugin(options).then(function (result) {
                                c.assertEqual(
                                    (<openbis.DynamicPropertyPluginEvaluationResult>result).getValue(),
                                    "testValue",
                                    "Evaluation result value"
                                )
                                c.finish()
                            })
                        })
                    })
                    .fail(function (error) {
                        c.fail(error.message)
                        c.finish()
                    })
            }

            var testEntityValidationPlugin = function (assert, databasePlugin) {
                var c = new common(assert, dtos)
                c.start()

                c.login(facade)
                    .then(function () {
                        var creation = new dtos.PluginCreation()
                        creation.setName(c.generateId("plugin"))
                        creation.setPluginType(dtos.PluginType.ENTITY_VALIDATION)
                        creation.setScript(
                            "def validate(entity, isNew):\n  requestValidation(entity)\n  if isNew:\n    return 'testError'\n  else:\n    return None"
                        )

                        return Promise.all([facade.createPlugins([creation]), c.createSample(facade)]).then(function ([pluginIds, sampleId]) {
                            var options = new dtos.EntityValidationPluginEvaluationOptions()
                            if (databasePlugin) {
                                options.setPluginId(pluginIds[0])
                            } else {
                                options.setPluginScript(creation.getScript())
                            }
                            options.setNew(true)
                            options.setObjectId(sampleId)

                            return Promise.all([facade.evaluatePlugin(options), c.findSample(facade, sampleId)]).then(function ([result, sample]) {
                                c.assertEqual(
                                    (<openbis.EntityValidationPluginEvaluationResult>result).getError(),
                                    "testError",
                                    "Evaluation result error"
                                )
                                c.assertObjectsWithValues(
                                    (<openbis.EntityValidationPluginEvaluationResult>result).getRequestedValidations(),
                                    "identifier",
                                    [sample.getIdentifier().getIdentifier()]
                                )

                                c.finish()
                            })
                        })
                    })
                    .fail(function (error) {
                        c.fail(error.message)
                        c.finish()
                    })
            }

            QUnit.test("evaluatePlugin() dynamic property plugin from database", function (assert) {
                return testDynamicPropertyPlugin(assert, true)
            })

            QUnit.test("evaluatePlugin() dynamic property plugin from script", function (assert) {
                return testDynamicPropertyPlugin(assert, false)
            })

            QUnit.test("evaluatePlugin() entity validation plugin from database", function (assert) {
                return testEntityValidationPlugin(assert, true)
            })

            QUnit.test("evaluatePlugin() entity validation plugin from script", function (assert) {
                return testEntityValidationPlugin(assert, false)
            })
        }

        resolve(function () {
            executeModule("Evaluate tests (RequireJS)", new openbisRequireJS(), dtos)
            executeModule("Evaluate tests (RequireJS - executeOperations)", new openbisExecuteOperations(new openbisRequireJS(), dtos), dtos)
            executeModule("Evaluate tests (module VAR)", new window.openbis.openbis(), window.openbis)
            executeModule(
                "Evaluate tests (module VAR - executeOperations)",
                new openbisExecuteOperations(new window.openbis.openbis(), window.openbis),
                window.openbis
            )
            executeModule("Evaluate tests (module ESM)", new window.openbisESM.openbis(), window.openbisESM)
            executeModule(
                "Evaluate tests (module ESM - executeOperations)",
                new openbisExecuteOperations(new window.openbisESM.openbis(), window.openbisESM),
                window.openbisESM
            )
        })
    })
})
