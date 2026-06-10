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

            var testUpdate = function (c: common.CommonClass, entityKind, fCreate, fUpdate, fFind, freezings) {
                c.start()

                var ctx = {
                    facade: null,
                    permIds: null,
                    freezingMethodsByPermIds: {},
                }

                c.login(facade)
                    .then(function () {
                        ctx.facade = facade
                        var codePrefix = c.generateId(entityKind)
                        var codes = []
                        $.each(Object.keys(freezings), function (index, method) {
                            codes.push(codePrefix + "-" + method)
                        })
                        return fCreate(facade, codes)
                    })
                    .then(function (permIds) {
                        c.ok("Entities created: " + permIds)
                        ctx.permIds = permIds
                        $.each(Object.keys(freezings), function (index, method) {
                            ctx.freezingMethodsByPermIds[permIds[index].getPermId()] = method
                        })
                        return fUpdate(ctx.facade, ctx.freezingMethodsByPermIds)
                    })
                    .then(function () {
                        c.ok("Entities frozen: " + ctx.permIds)
                        return fFind(ctx.facade, ctx.permIds)
                    })
                    .then(function (entities) {
                        $.each(ctx.freezingMethodsByPermIds, function (permId, method) {
                            var entity = entities[permId]
                            $.each(freezings[method], function (flagName, expectedValue) {
                                var m = "is" + flagName.charAt(0).toUpperCase() + flagName.slice(1)
                                c.assertEqual(entity[m](), expectedValue, entity.getCode() + ": " + flagName)
                            })
                        })
                        c.finish()
                    })
                    .fail(function (error) {
                        c.fail(error.message)
                        c.finish()
                    })
            }

            QUnit.test("freeze spaces()", function (assert) {
                var c = new common(assert, dtos)

                var fCreate = function (facade: openbis.openbis, codes: string[]) {
                    var creations: openbis.SpaceCreation[] = []
                    $.each(codes, function (index: number, code: string) {
                        var creation = new dtos.SpaceCreation()
                        creation.setCode(code)
                        creations.push(creation)
                    })
                    return facade.createSpaces(creations)
                }

                var fUpdate = function (facade: openbis.openbis, freezingMethodsByPermIds: { [index: string]: string }) {
                    var updates: openbis.SpaceUpdate[] = []
                    $.each(freezingMethodsByPermIds, function (permId: string, method: string) {
                        var update = new dtos.SpaceUpdate()
                        update.setSpaceId(new dtos.SpacePermId(permId))
                        update[method]()
                        updates.push(update)
                    })
                    return facade.updateSpaces(updates)
                }

                var fFind = function (facade: openbis.openbis, permIds: openbis.SpacePermId[]) {
                    return facade.getSpaces(permIds, c.createSpaceFetchOptions())
                }

                testUpdate(c, "SPACE", fCreate, fUpdate, fFind, {
                    freeze: { frozen: true, frozenForProjects: false, frozenForSamples: false },
                    freezeForProjects: { frozen: true, frozenForProjects: true, frozenForSamples: false },
                    freezeForSamples: { frozen: true, frozenForProjects: false, frozenForSamples: true },
                })
            })

            QUnit.test("freeze projects()", function (assert) {
                var c = new common(assert, dtos)

                var fCreate = function (facade: openbis.openbis, codes: string[]) {
                    var creations = []
                    $.each(codes, function (index: number, code: string) {
                        var creation = new dtos.ProjectCreation()
                        creation.setSpaceId(new dtos.SpacePermId("TEST"))
                        creation.setCode(code)
                        creations.push(creation)
                    })
                    return facade.createProjects(creations)
                }

                var fUpdate = function (facade: openbis.openbis, freezingMethodsByPermIds: { [index: string]: string }) {
                    var updates: openbis.ProjectUpdate[] = []
                    $.each(freezingMethodsByPermIds, function (permId: string, method: string) {
                        var update = new dtos.ProjectUpdate()
                        update.setProjectId(new dtos.ProjectPermId(permId))
                        update[method]()
                        updates.push(update)
                    })
                    return facade.updateProjects(updates)
                }

                var fFind = function (facade: openbis.openbis, permIds: openbis.ProjectPermId[]) {
                    return facade.getProjects(permIds, c.createProjectFetchOptions())
                }

                testUpdate(c, "PROJECT", fCreate, fUpdate, fFind, {
                    freeze: { frozen: true, frozenForExperiments: false, frozenForSamples: false },
                    freezeForExperiments: { frozen: true, frozenForExperiments: true, frozenForSamples: false },
                    freezeForSamples: { frozen: true, frozenForExperiments: false, frozenForSamples: true },
                })
            })

            QUnit.test("freeze experiments()", function (assert) {
                var c = new common(assert, dtos)

                var fCreate = function (facade: openbis.openbis, codes: string[]) {
                    var creations: openbis.ExperimentCreation[] = []
                    $.each(codes, function (index: number, code: string) {
                        var creation = new dtos.ExperimentCreation()
                        creation.setCode(code)
                        creation.setTypeId(new dtos.EntityTypePermId("HT_SEQUENCING"))
                        creation.setProjectId(new dtos.ProjectIdentifier("/TEST/TEST-PROJECT"))
                        creations.push(creation)
                    })
                    return facade.createExperiments(creations)
                }

                var fUpdate = function (facade: openbis.openbis, freezingMethodsByPermIds: { [index: string]: string }) {
                    var updates: openbis.ExperimentUpdate[] = []
                    $.each(freezingMethodsByPermIds, function (permId: string, method: string) {
                        var update = new dtos.ExperimentUpdate()
                        update.setExperimentId(new dtos.ExperimentPermId(permId))
                        update[method]()
                        updates.push(update)
                    })
                    return facade.updateExperiments(updates)
                }

                var fFind = function (facade: openbis.openbis, permIds: openbis.ExperimentPermId[]) {
                    return facade.getExperiments(permIds, c.createExperimentFetchOptions())
                }

                testUpdate(c, "EXPERIMENT", fCreate, fUpdate, fFind, {
                    freeze: { frozen: true, frozenForDataSets: false, frozenForSamples: false, immutableData: true },
                    freezeForDataSets: { frozen: true, frozenForDataSets: true, frozenForSamples: false, immutableData: true },
                    freezeForSamples: { frozen: true, frozenForDataSets: false, frozenForSamples: true, immutableData: true },
                    makeDataImmutable: { frozen: false, frozenForDataSets: false, frozenForSamples: false, immutableData: true },
                })
            })

            QUnit.test("freeze samples()", function (assert) {
                var c = new common(assert, dtos)

                var fCreate = function (facade: openbis.openbis, codes: string[]) {
                    var creations: openbis.SampleCreation[] = []
                    $.each(codes, function (index: number, code: string) {
                        var creation = new dtos.SampleCreation()
                        creation.setSpaceId(new dtos.SpacePermId("TEST"))
                        creation.setCode(code)
                        creation.setTypeId(new dtos.EntityTypePermId("UNKNOWN"))
                        creations.push(creation)
                    })
                    return facade.createSamples(creations)
                }

                var fUpdate = function (facade: openbis.openbis, freezingMethodsByPermIds: { [index: string]: string }) {
                    var updates: openbis.SampleUpdate[] = []
                    $.each(freezingMethodsByPermIds, function (permId: string, method: string) {
                        var update = new dtos.SampleUpdate()
                        update.setSampleId(new dtos.SamplePermId(permId))
                        update[method]()
                        updates.push(update)
                    })
                    return facade.updateSamples(updates)
                }

                var fFind = function (facade: openbis.openbis, permIds: openbis.SamplePermId[]) {
                    return facade.getSamples(permIds, c.createSampleFetchOptions())
                }

                testUpdate(c, "SAMPLE", fCreate, fUpdate, fFind, {
                    freeze: { frozen: true, frozenForDataSets: false, frozenForComponents: false, frozenForChildren: false, frozenForParents: false, immutableData: true },
                    freezeForDataSets: {
                        frozen: true,
                        frozenForDataSets: true,
                        frozenForComponents: false,
                        frozenForChildren: false,
                        frozenForParents: false,
                        immutableData: true
                    },
                    freezeForComponents: {
                        frozen: true,
                        frozenForDataSets: false,
                        frozenForComponents: true,
                        frozenForChildren: false,
                        frozenForParents: false,
                        immutableData: true
                    },
                    freezeForChildren: {
                        frozen: true,
                        frozenForDataSets: false,
                        frozenForComponents: false,
                        frozenForChildren: true,
                        frozenForParents: false,
                        immutableData: true
                    },
                    freezeForParents: {
                        frozen: true,
                        frozenForDataSets: false,
                        frozenForComponents: false,
                        frozenForChildren: false,
                        frozenForParents: true,
                        immutableData: true
                    },
                    makeDataImmutable: {
                        frozen: false,
                        frozenForDataSets: false,
                        frozenForComponents: false,
                        frozenForChildren: false,
                        frozenForParents: false,
                        immutableData: true
                    },
                })
            })

            QUnit.test("freeze data sets()", function (assert) {
                var c = new common(assert, dtos)

                var fCreate = function (facade: openbis.openbis, codes: string[]) {
                    var creations = []
                    $.each(codes, function (index: number, code: string) {
                        creations.push(c.createDataSet(facade, "UNKNOWN"))
                    })
                    return $.when.apply($, creations).then(function () {
                        return Array.prototype.slice.call(arguments)
                    })
                }

                var fUpdate = function (facade: openbis.openbis, freezingMethodsByPermIds: { [index: string]: string }) {
                    var updates: openbis.DataSetUpdate[] = []
                    $.each(freezingMethodsByPermIds, function (permId: string, method: string) {
                        var update = new dtos.DataSetUpdate()
                        update.setDataSetId(new dtos.DataSetPermId(permId))
                        update[method]()
                        updates.push(update)
                    })
                    return facade.updateDataSets(updates)
                }

                var fFind = function (facade: openbis.openbis, permIds: openbis.DataSetPermId[]) {
                    return facade.getDataSets(permIds, c.createDataSetFetchOptions())
                }

                testUpdate(c, "DATA_SET", fCreate, fUpdate, fFind, {
                    freeze: {
                        frozen: true,
                        frozenForContainers: false,
                        frozenForComponents: false,
                        frozenForChildren: false,
                        frozenForParents: false,
                    },
                    freezeForContainers: {
                        frozen: true,
                        frozenForContainers: true,
                        frozenForComponents: false,
                        frozenForChildren: false,
                        frozenForParents: false,
                    },
                    freezeForComponents: {
                        frozen: true,
                        frozenForContainers: false,
                        frozenForComponents: true,
                        frozenForChildren: false,
                        frozenForParents: false,
                    },
                    freezeForChildren: {
                        frozen: true,
                        frozenForContainers: false,
                        frozenForComponents: false,
                        frozenForChildren: true,
                        frozenForParents: false,
                    },
                    freezeForParents: {
                        frozen: true,
                        frozenForContainers: false,
                        frozenForComponents: false,
                        frozenForChildren: false,
                        frozenForParents: true,
                    },
                })
            })
        }

        resolve(function () {
            executeModule("Freezing tests (RequireJS)", new openbisRequireJS(), dtos)
            executeModule("Freezing tests (RequireJS - executeOperations)", new openbisExecuteOperations(new openbisRequireJS(), dtos), dtos)
            executeModule("Freezing tests (module VAR)", new window.openbis.openbis(), window.openbis)
            executeModule(
                "Freezing tests (module VAR - executeOperations)",
                new openbisExecuteOperations(new window.openbis.openbis(), window.openbis),
                window.openbis
            )
            executeModule("Freezing tests (module ESM)", new window.openbisESM.openbis(), window.openbisESM)
            executeModule(
                "Freezing tests (module ESM - executeOperations)",
                new openbisExecuteOperations(new window.openbisESM.openbis(), window.openbisESM),
                window.openbisESM
            )
        })
    })
})
