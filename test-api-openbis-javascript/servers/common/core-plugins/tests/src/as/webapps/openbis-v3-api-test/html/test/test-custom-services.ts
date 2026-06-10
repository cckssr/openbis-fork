/**
 * Test searching and executing custom AS services.
 */
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

            var testAction = function (c: common.CommonClass, fAction, fCheck) {
                c.start()

                c.login(facade)
                    .then(function () {
                        c.ok("Login")
                        return fAction(facade).then(function (result) {
                            c.ok("Got results")
                            fCheck(facade, result)
                            c.finish()
                        })
                    })
                    .fail(function (error) {
                        c.fail(error.message)
                        c.finish()
                    })
            }

            QUnit.test("searchCustomASServices()", function (assert) {
                var c = new common(assert, dtos)

                var fAction = function (facade: openbis.openbis) {
                    var criteria = new dtos.CustomASServiceSearchCriteria()
                    criteria.withCode().thatStartsWith("simple")
                    return facade.searchCustomASServices(criteria, new dtos.CustomASServiceFetchOptions())
                }

                var fCheck = function (facade: openbis.openbis, result: openbis.SearchResult<openbis.CustomASService>) {
                    var services = result.getObjects()
                    c.assertEqual(services.length, 1)
                    var service = services[0]
                    c.assertEqual(service.getCode().getPermId(), "simple-service", "Code")
                    c.assertEqual(service.getDescription(), "a simple service", "Description")
                }

                testAction(c, fAction, fCheck)
            })

            QUnit.test("executeCustomASService()", function (assert) {
                var c = new common(assert, dtos)

                var fAction = function (facade: openbis.openbis) {
                    var id = new dtos.CustomASServiceCode("simple-service")
                    var options = new dtos.CustomASServiceExecutionOptions().withParameter("a", "1").withParameter("space-code", "TEST")
                    return facade.executeCustomASService(id, options)
                }

                var fCheck = function (facade: openbis.openbis, result: any) {
                    c.assertEqual(1, result.getTotalCount())
                    var space = result.getObjects()[0]
                    c.assertEqual(space.getPermId(), "TEST", "PermId")
                    c.assertEqual(space.getCode(), "TEST", "Code")
                    c.assertEqual(space.getDescription(), null, "Description")
                    c.assertDate(space.getRegistrationDate(), "Registration date", 2013, 4, 12, 12, 59)
                }

                testAction(c, fAction, fCheck)
            })
        }

        resolve(function () {
            executeModule("Custom AS service tests (RequireJS)", new openbisRequireJS(), dtos)
            executeModule("Custom AS service tests (RequireJS - executeOperations)", new openbisExecuteOperations(new openbisRequireJS(), dtos), dtos)
            executeModule("Custom AS service tests (module VAR)", new window.openbis.openbis(), window.openbis)
            executeModule(
                "Custom AS service tests (module VAR - executeOperations)",
                new openbisExecuteOperations(new window.openbis.openbis(), window.openbis),
                window.openbis
            )
            executeModule("Custom AS service tests (module ESM)", new window.openbisESM.openbis(), window.openbisESM)
            executeModule(
                "Custom AS service tests (module ESM - executeOperations)",
                new openbisExecuteOperations(new window.openbisESM.openbis(), window.openbisESM),
                window.openbisESM
            )
        })
    })
})
