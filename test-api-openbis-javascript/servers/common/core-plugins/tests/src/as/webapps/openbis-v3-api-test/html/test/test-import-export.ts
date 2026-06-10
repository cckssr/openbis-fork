/*
 *  Copyright ETH 2023 Zürich, Scientific IT Services
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

import jquery from './types/jquery'
import underscore from './types/underscore'
import common from './types/common'
import openbis from './types/openbis.esm'

exports.default = new Promise((resolve) => {
    require(["jquery", "underscore", "openbis", "test/common", "test/openbis-execute-operations", "test/dtos"], function (
        $: jquery.JQueryStatic,
        _: underscore.UnderscoreStatic,
        openbisRequireJS,
        common: common.CommonConstructor,
        openbisExecuteOperations,
        dtos
    ) {
      var fileContent = "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"

        var executeModule = function (moduleName: string, facade: openbis.openbis, dtos: openbis.bundle) {
            QUnit.module(moduleName)

            var testAction = function (c: common.CommonClass, fAction, fCheck) {
                c.start()

                c.login(facade)
                    .then(function () {
                        c.ok("Login")
                        return fAction(facade).then(function (result) {
                            c.ok("Got results")
                            var checkPromise = fCheck(facade, result)
                            if (checkPromise) {
                                return checkPromise.then(function () {
                                    c.finish()
                                })
                            } else {
                                c.finish()
                            }
                        })
                    })
                    .fail(function (error) {
                        c.fail(error.message)
                        c.finish()
                    })
            }

            QUnit.test("executeImport()", function (assert) {
                var c = new common(assert, dtos)

                var fAction = function (facade: openbis.openbis) {
                    var fileName = "model.xlsx"
                    var data = new window.File([c.base64ToBlob(fileContent)], fileName)
                    return facade.uploadToSessionWorkspace(data)
                          .then(function() {
                              c.ok("uploadToSessionWorkspace")
                              var importData = new dtos.ImportData()
                              importData.setFormat("EXCEL")
                              importData.setSessionWorkspaceFiles([fileName])

                              var importOptions = new dtos.ImportOptions()
                              importOptions.setMode("UPDATE_IF_EXISTS")

                              return facade.executeImport(importData, importOptions)
                                  .then(function() {
                                      c.ok("executeImport")
                                  })
                          })
                }

                var fCheck = function (facade: openbis.openbis) {
                    var criteria = new dtos.VocabularySearchCriteria()
                    criteria.withCode().thatEquals("VOCAB")

                    var vocabularyFetchOptions = c.createVocabularyFetchOptions()
                    vocabularyFetchOptions.withTerms()

                    return facade.searchVocabularies(criteria, vocabularyFetchOptions).then(
                        function (results) {
                            c.assertEqual(results.getTotalCount(), 1)
                            var vocabulary = results.getObjects()[0]

                            c.assertEqual(vocabulary.getCode(), "VOCAB")

                            var terms = vocabulary.getTerms()

                            c.assertEqual(terms.length, 3)

                            var codes = terms
                                .map(function (object) {
                                    return object.getCode()
                                })
                                .sort()

                            var labels = terms
                                .map(function (object) {
                                    return object.getLabel()
                                })
                                .sort()

                            c.assertEqual(codes[0], "TERM_A")
                            c.assertEqual(codes[1], "TERM_B")
                            c.assertEqual(codes[2], "TERM_C")

                            c.assertEqual(labels[0], "A")
                            c.assertEqual(labels[1], "B")
                            c.assertEqual(labels[2], "C")
                        },
                        function (error) {
                            c.fail("Error searching vocabularies. error=" + error.message)
                        }
                    )
                }

                testAction(c, fAction, fCheck)
            })

            QUnit.test("executeExport()", function (assert) {
                var c = new common(assert, dtos)

                var fAction = function (facade: openbis.openbis) {
                    var exportablePermId = new dtos.ExportablePermId(dtos.ExportableKind.SAMPLE, "200902091225616-1027")
                    var exportData = new dtos.ExportData([exportablePermId], new dtos.AllFields())

                    var exportOptions = new dtos.ExportOptions(
                        [dtos.ExportFormat.XLSX, dtos.ExportFormat.HTML, dtos.ExportFormat.PDF, dtos.ExportFormat.DATA],
                        dtos.XlsTextFormat.RICH,
                        true,
                        false,
                        true
                    )

                    return facade.executeExport(exportData, exportOptions)
                }

                var fCheck = function (facade: openbis.openbis, result: openbis.ExportResult) {
                    c.assertNotNull(result)
                    c.assertNotNull(result.getDownloadURL())
                    return null
                }

                testAction(c, fAction, fCheck)
            })
        }

        resolve(function () {
            executeModule("Export/import tests (RequireJS)", new openbisRequireJS(), dtos)
            executeModule("Export/import tests (RequireJS - executeOperations)", new openbisExecuteOperations(new openbisRequireJS(), dtos), dtos)
            executeModule("Export/import tests (module VAR)", new window.openbis.openbis(), window.openbis)
            executeModule(
                "Export/import tests (module VAR - executeOperations)",
                new openbisExecuteOperations(new window.openbis.openbis(), window.openbis),
                window.openbis
            )
            executeModule("Export/import tests (module ESM)", new window.openbisESM.openbis(), window.openbisESM)
            executeModule(
                "Export/import tests (module ESM - executeOperations)",
                new openbisExecuteOperations(new window.openbisESM.openbis(), window.openbisESM),
                window.openbisESM
            )
        })
    })
})
