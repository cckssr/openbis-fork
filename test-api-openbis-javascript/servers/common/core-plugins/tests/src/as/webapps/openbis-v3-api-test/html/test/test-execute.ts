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

            QUnit.test("executeQuery()", function (assert) {
                var c = new common(assert, dtos)
                c.start()

                c.login(facade)
                    .then(function () {
                        var creation = new dtos.QueryCreation()
                        creation.setName(c.generateId("query"))
                        creation.setDatabaseId(new dtos.QueryDatabaseName("openbisDB"))
                        creation.setQueryType(dtos.QueryType.GENERIC)
                        creation.setSql("select perm_id, code from projects where perm_id = ${perm_id}")

                        return facade.createQueries([creation]).then(function (techIds) {
                            var options = new dtos.QueryExecutionOptions()
                            options.withParameter("perm_id", "20130412150031345-203")

                            return facade.executeQuery(techIds[0], options).then(function (table) {
                                c.assertEqual(table.getColumns().length, 2, "Columns count")
                                c.assertEqual(table.getColumns()[0].getTitle(), "perm_id", "Column[0] title")
                                c.assertEqual(table.getColumns()[1].getTitle(), "code", "Column[1] title")
                                c.assertEqual(table.getRows().length, 1, "Rows count")
                                c.assertEqual((<openbis.TableStringCell>table.getRows()[0][0]).getValue(), "20130412150031345-203", "Value[0][0]")
                                c.assertEqual((<openbis.TableStringCell>table.getRows()[0][1]).getValue(), "TEST-PROJECT", "Value[0][1]")

                                c.finish()
                            })
                        })
                    })
                    .fail(function (error) {
                        c.fail(error.message)
                        c.finish()
                    })
            })

            QUnit.test("executeSql()", function (assert) {
                var c = new common(assert, dtos)
                c.start()

                c.login(facade)
                    .then(function () {
                        var options = new dtos.SqlExecutionOptions()
                        options.withDatabaseId(new dtos.QueryDatabaseName("openbisDB"))
                        options.withParameter("perm_id", "20130412150031345-203")

                        return facade.executeSql("select perm_id, code from projects where perm_id = ${perm_id}", options).then(function (table) {
                            c.assertEqual(table.getColumns().length, 2, "Columns count")
                            c.assertEqual(table.getColumns()[0].getTitle(), "perm_id", "Column[0] title")
                            c.assertEqual(table.getColumns()[1].getTitle(), "code", "Column[1] title")
                            c.assertEqual(table.getRows().length, 1, "Rows count")
                            c.assertEqual((<openbis.TableStringCell>table.getRows()[0][0]).getValue(), "20130412150031345-203", "Value[0][0]")
                            c.assertEqual((<openbis.TableStringCell>table.getRows()[0][1]).getValue(), "TEST-PROJECT", "Value[0][1]")

                            c.finish()
                        })
                    })
                    .fail(function (error) {
                        c.fail(error.message)
                        c.finish()
                    })
            })
        }

        resolve(function () {
            executeModule("Execute tests (RequireJS)", new openbisRequireJS(), dtos)
            executeModule("Execute tests (RequireJS - executeOperations)", new openbisExecuteOperations(new openbisRequireJS(), dtos), dtos)
            executeModule("Execute tests (module VAR)", new window.openbis.openbis(), window.openbis)
            executeModule(
                "Execute tests (module VAR - executeOperations)",
                new openbisExecuteOperations(new window.openbis.openbis(), window.openbis),
                window.openbis
            )
            executeModule("Execute tests (module ESM)", new window.openbisESM.openbis(), window.openbisESM)
            executeModule(
                "Execute tests (module ESM - executeOperations)",
                new openbisExecuteOperations(new window.openbisESM.openbis(), window.openbisESM),
                window.openbisESM
            )
        })
    })
})
