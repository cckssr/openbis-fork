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

            var testAction = function (c: common.CommonClass, fAction, actionType) {
                c.start()

                c.login(facade)
                    .then(function () {
                        c.ok("Login")
                        return fAction(facade).then(function (result) {
                            c.ok(actionType)
                            c.finish()
                        })
                    })
                    .fail(function (error) {
                        c.fail(error.message)
                        c.finish()
                    })
            }

            QUnit.test("archiveDataSets()", function (assert) {
                var c = new common(assert, dtos)

                var fAction = function (facade: openbis.openbis) {
                    return $.when(c.createDataSet(facade, "ALIGNMENT"), c.createDataSet(facade, "UNKNOWN")).then(function (permId1, permId2) {
                        var ids = [permId1, permId2]
                        return facade.archiveDataSets(ids, new dtos.DataSetArchiveOptions())
                    })
                }

                testAction(c, fAction, "Archived")
            })

            QUnit.test("unarchiveDataSets()", function (assert) {
                var c = new common(assert, dtos)

                var fAction = function (facade: openbis.openbis) {
                    return $.when(c.createDataSet(facade, "ALIGNMENT"), c.createDataSet(facade, "UNKNOWN")).then(function (permId1, permId2) {
                        var ids = [permId1, permId2]
                        return facade.archiveDataSets(ids, new dtos.DataSetArchiveOptions()).then(function () {
                            return facade.unarchiveDataSets(ids, new dtos.DataSetUnarchiveOptions())
                        })
                    })
                }

                testAction(c, fAction, "Unarchived")
            })

            QUnit.test("lockDataSets()", function (assert) {
                var c = new common(assert, dtos)

                var fAction = function (facade: openbis.openbis) {
                    return $.when(c.createDataSet(facade, "ALIGNMENT"), c.createDataSet(facade, "UNKNOWN")).then(function (permId1, permId2) {
                        var ids = [permId1, permId2]
                        return facade.lockDataSets(ids, new dtos.DataSetLockOptions())
                    })
                }

                testAction(c, fAction, "Lock")
            })

            QUnit.test("unlockDataSets()", function (assert) {
                var c = new common(assert, dtos)

                var fAction = function (facade: openbis.openbis) {
                    return $.when(c.createDataSet(facade, "ALIGNMENT"), c.createDataSet(facade, "UNKNOWN")).then(function (permId1, permId2) {
                        var ids = [permId1, permId2]
                        return facade.lockDataSets(ids, new dtos.DataSetLockOptions()).then(function () {
                            return facade.unlockDataSets(ids, new dtos.DataSetUnlockOptions())
                        })
                    })
                }

                testAction(c, fAction, "Unlock")
            })
        }

        resolve(function () {
            executeModule("Archive/Unarchive (RequireJS)", new openbisRequireJS(), dtos)
            executeModule("Archive/Unarchive (RequireJS - executeOperations)", new openbisExecuteOperations(new openbisRequireJS(), dtos), dtos)
            executeModule("Archive/Unarchive (module VAR)", new window.openbis.openbis(), window.openbis)
            executeModule(
                "Archive/Unarchive (module VAR - executeOperations)",
                new openbisExecuteOperations(new window.openbis.openbis(), window.openbis),
                window.openbis
            )
            executeModule("Archive/Unarchive (module ESM)", new window.openbisESM.openbis(), window.openbisESM)
            executeModule(
                "Archive/Unarchive (module ESM - executeOperations)",
                new openbisExecuteOperations(new window.openbisESM.openbis(), window.openbisESM),
                window.openbisESM
            )
        })
    })
})
