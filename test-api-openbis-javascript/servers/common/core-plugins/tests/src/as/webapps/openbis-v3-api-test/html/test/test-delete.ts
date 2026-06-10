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

            var testDeleteWithoutTrash = function (c: common.CommonClass, fCreate, fFind, fDelete) {
                c.start()

                c.login(facade)
                    .then(function () {
                        return fCreate(facade).then(function (permId) {
                            c.assertNotNull(permId, "Entity was created")
                            return fFind(facade, permId).then(function (entity) {
                                c.assertNotNull(entity, "Entity can be found")
                                return facade
                                    .searchDeletions(new dtos.DeletionSearchCriteria(), new dtos.DeletionFetchOptions())
                                    .then(function (beforeDeletions) {
                                        c.ok("Got before deletions")
                                        return fDelete(facade, permId).then(function () {
                                            c.ok("Entity was deleted")
                                            return facade
                                                .searchDeletions(new dtos.DeletionSearchCriteria(), new dtos.DeletionFetchOptions())
                                                .then(function (afterDeletions) {
                                                    c.ok("Got after deletions")
                                                    c.assertEqual(
                                                        beforeDeletions.getObjects().length,
                                                        afterDeletions.getObjects().length,
                                                        "No new deletions found"
                                                    )
                                                    return fFind(facade, permId).then(function (entityAfterDeletion) {
                                                        c.assertNull(entityAfterDeletion, "Entity was deleted")
                                                        c.finish()
                                                    })
                                                })
                                        })
                                    })
                            })
                        })
                    })
                    .fail(function (error) {
                        c.fail(error.message)
                        c.finish()
                    })
            }

            var testDeleteWithTrashAndRevert = function (c: common.CommonClass, fCreate, fFind, fDelete) {
                c.start()

                c.login(facade)
                    .then(function () {
                        return fCreate(facade).then(function (permIdAndMore) {
                            var permId = null
                            if (permIdAndMore.identifier) {
                                permId = permIdAndMore.permId
                            } else {
                                permId = permIdAndMore
                            }
                            c.assertNotNull(permId, "Entity was created")
                            return fFind(facade, permId).then(function (entity) {
                                c.assertNotNull(entity, "Entity can be found")
                                return facade
                                    .searchDeletions(new dtos.DeletionSearchCriteria(), new dtos.DeletionFetchOptions())
                                    .then(function (beforeDeletions) {
                                        c.ok("Got before deletions")
                                        return fDelete(facade, permId).then(function (deletionId) {
                                            c.ok("Entity was deleted")
                                            c.assertNotEqual(deletionId.getTechId(), "", "Deletion tech id not an empty string")
                                            var fo = new dtos.DeletionFetchOptions()
                                            fo.withDeletedObjects()
                                            return facade.searchDeletions(new dtos.DeletionSearchCriteria(), fo).then(function (afterDeletions) {
                                                var objects = afterDeletions.getObjects()
                                                c.ok("Got after deletions")
                                                c.assertEqual(objects.length, beforeDeletions.getObjects().length + 1, "One new deletion")
                                                var newDeletion = objects[afterDeletions.getObjects().length - 1]
                                                if (permIdAndMore.identifier) {
                                                    var deletedObject = newDeletion.getDeletedObjects()[0]
                                                    c.assertEqual(deletedObject.getIdentifier(), permIdAndMore.identifier, "Entity identifier match")
                                                    c.assertEqual(
                                                        deletedObject.getEntityTypeCode(),
                                                        permIdAndMore.entityTypeCode,
                                                        "Entity type match"
                                                    )
                                                    c.assertEqual(deletedObject.getEntityKind(), permIdAndMore.entityKind, "Entity kind match")
                                                }
                                                c.assertEqual(
                                                    (<openbis.DeletionTechId>newDeletion.getId()).getTechId(),
                                                    deletionId.getTechId(),
                                                    "Deletion ids match"
                                                )
                                                return fFind(facade, permId).then(function (entityAfterDeletion) {
                                                    c.assertNull(entityAfterDeletion, "Entity was deleted")
                                                    return facade.revertDeletions([deletionId]).then(function () {
                                                        c.ok("Reverted deletion")
                                                        return fFind(facade, permId).then(function (entityAfterRevert) {
                                                            c.assertNotNull(entityAfterRevert, "Entity is back")
                                                            c.finish()
                                                        })
                                                    })
                                                })
                                            })
                                        })
                                    })
                            })
                        })
                    })
                    .fail(function (error) {
                        c.fail(error.message)
                        c.finish()
                    })
            }

            var testDeleteWithTrashAndConfirm = function (c: common.CommonClass, fCreate, fFind, fDelete) {
                c.start()

                c.login(facade)
                    .then(function () {
                        return fCreate(facade).then(function (permId) {
                            c.assertNotNull(permId, "Entity was created")
                            return fFind(facade, permId).then(function (entity) {
                                c.assertNotNull(entity, "Entity can be found")
                                return facade
                                    .searchDeletions(new dtos.DeletionSearchCriteria(), new dtos.DeletionFetchOptions())
                                    .then(function (deletionsBeforeDeletion) {
                                        c.ok("Got before deletions")
                                        return fDelete(facade, permId).then(function (deletionId) {
                                            c.ok("Entity was deleted")
                                            return facade
                                                .searchDeletions(new dtos.DeletionSearchCriteria(), new dtos.DeletionFetchOptions())
                                                .then(function (deletionsAfterDeletion) {
                                                    c.ok("Got after deletions")
                                                    c.assertEqual(
                                                        deletionsAfterDeletion.getObjects().length,
                                                        deletionsBeforeDeletion.getObjects().length + 1,
                                                        "One new deletion"
                                                    )
                                                    c.assertEqual(
                                                        (<openbis.DeletionTechId>(
                                                            deletionsAfterDeletion
                                                                .getObjects()
                                                                [deletionsAfterDeletion.getObjects().length - 1].getId()
                                                        )).getTechId(),
                                                        deletionId.getTechId(),
                                                        "Deletion ids match"
                                                    )
                                                    return fFind(facade, permId).then(function (entityAfterDeletion) {
                                                        c.assertNull(entityAfterDeletion, "Entity was deleted")
                                                        return facade.confirmDeletions([deletionId]).then(function () {
                                                            c.ok("Confirmed deletion")
                                                            return fFind(facade, permId).then(function (entityAfterConfirm) {
                                                                c.assertNull(entityAfterConfirm, "Entity is still gone")
                                                                return facade
                                                                    .searchDeletions(
                                                                        new dtos.DeletionSearchCriteria(),
                                                                        new dtos.DeletionFetchOptions()
                                                                    )
                                                                    .then(function (deletionsAfterConfirm) {
                                                                        c.assertEqual(
                                                                            deletionsAfterConfirm.getObjects().length,
                                                                            deletionsBeforeDeletion.getObjects().length,
                                                                            "New deletion is also gone"
                                                                        )
                                                                        c.finish()
                                                                    })
                                                            })
                                                        })
                                                    })
                                                })
                                        })
                                    })
                            })
                        })
                    })
                    .fail(function (error) {
                        c.fail(error.message)
                        c.finish()
                    })
            }

            QUnit.test("deleteSpaces()", function (assert) {
                var c = new common(assert, dtos)
                testDeleteWithoutTrash(c, c.createSpace, c.findSpace, c.deleteSpace)
            })

            QUnit.test("deleteProjects()", function (assert) {
                var c = new common(assert, dtos)
                testDeleteWithoutTrash(c, c.createProject, c.findProject, c.deleteProject)
            })

            QUnit.test("deleteExperiments() with revert", function (assert) {
                var c = new common(assert, dtos)
                testDeleteWithTrashAndRevert(
                    c,
                    function (facade: openbis.openbis) {
                        return c.createExperiment(facade).then(function (permId) {
                            var fo = new dtos.ExperimentFetchOptions()
                            fo.withType()
                            return facade.getExperiments([permId], fo).then(function (map) {
                                var experiment = map[permId.toString()]
                                return {
                                    permId: permId,
                                    identifier: experiment.getIdentifier(),
                                    entityTypeCode: experiment.getType().getCode(),
                                    entityKind: dtos.EntityKind.EXPERIMENT,
                                }
                            })
                        })
                    },
                    c.findExperiment,
                    c.deleteExperiment
                )
            })

            QUnit.test("deleteExperiments() with confirm", function (assert) {
                var c = new common(assert, dtos)
                testDeleteWithTrashAndConfirm(c, c.createExperiment, c.findExperiment, c.deleteExperiment)
            })

            QUnit.test("deleteSamples() with revert", function (assert) {
                var c = new common(assert, dtos)
                testDeleteWithTrashAndRevert(
                    c,
                    function (facade: openbis.openbis) {
                        return c.createSample(facade).then(function (permId) {
                            var fo = new dtos.SampleFetchOptions()
                            fo.withType()
                            return facade.getSamples([permId], fo).then(function (map) {
                                var sample = map[permId.toString()]
                                return {
                                    permId: permId,
                                    identifier: sample.getIdentifier(),
                                    entityTypeCode: sample.getType().getCode(),
                                    entityKind: dtos.EntityKind.SAMPLE,
                                }
                            })
                        })
                    },
                    c.findSample,
                    c.deleteSample
                )
            })

            QUnit.test("deleteSamples() with confirm", function (assert) {
                var c = new common(assert, dtos)
                testDeleteWithTrashAndConfirm(c, c.createSample, c.findSample, c.deleteSample)
            })

            QUnit.test("deleteDataSets() with revert", function (assert) {
                var c = new common(assert, dtos)
                testDeleteWithTrashAndRevert(
                    c,
                    function (facade: openbis.openbis) {
                        return c.createDataSet(facade, "ALIGNMENT").then(function (permId) {
                            var fo = new dtos.DataSetFetchOptions()
                            fo.withType()
                            return facade.getDataSets([permId], fo).then(function (map) {
                                var dataSet = map[permId.toString()]
                                return {
                                    permId: permId,
                                    identifier: dataSet.getCode(),
                                    entityTypeCode: dataSet.getType().getCode(),
                                    entityKind: dtos.EntityKind.DATA_SET,
                                }
                            })
                        })
                    },
                    c.findDataSet,
                    c.deleteDataSet
                )
            })

            QUnit.test("deleteDataSets() with confirm", function (assert) {
                var c = new common(assert, dtos)
                testDeleteWithTrashAndConfirm(c, c.createDataSet, c.findDataSet, c.deleteDataSet)
            })

            QUnit.test("deleteDataSets() with disallowed type without force flag", function (assert) {
                var c = new common(assert, dtos)

                c.start()

                c.login(facade)
                    .then(function () {
                        return c.createDataSetType(facade).then(function (typeId) {
                            c.assertNotNull(typeId, "Data set type created")
                            return c.createDataSet(facade, typeId.getPermId()).then(function (permId) {
                                c.assertNotNull(permId, "Entity was created")
                                return c.deleteDataSet(facade, permId).then(function (deletionId) {
                                    c.assertNotNull(deletionId, "Entity was moved to trash")
                                    var update = new dtos.DataSetTypeUpdate()
                                    update.setTypeId(typeId)
                                    update.setDisallowDeletion(true)
                                    return facade.updateDataSetTypes([update]).then(function () {
                                        c.ok("Data set type updated")
                                        return facade.confirmDeletions([deletionId]).then(function () {
                                            c.fail("Confirmation of deletion should fail without the force flag")
                                            c.finish()
                                        })
                                    })
                                })
                            })
                        })
                    })
                    .fail(function (error) {
                        c.assertContains(
                            error.message,
                            "Deletion failed because the following data sets have 'Disallow deletion' flag set to true in their type",
                            "Expected error message"
                        )
                        c.finish()
                    })
            })

            QUnit.test("deleteDataSets() with disallowed type with force flag", function (assert) {
                var c = new common(assert, dtos)

                c.start()

                c.login(facade)
                    .then(function () {
                        return c.createDataSetType(facade).then(function (typeId) {
                            c.assertNotNull(typeId, "Data set type created")
                            return c.createDataSet(facade, typeId.getPermId()).then(function (permId) {
                                c.assertNotNull(permId, "Entity was created")
                                return c.deleteDataSet(facade, permId).then(function (deletionId) {
                                    c.assertNotNull(deletionId, "Entity was moved to trash")
                                    var update = new dtos.DataSetTypeUpdate()
                                    update.setTypeId(typeId)
                                    update.setDisallowDeletion(true)
                                    return facade.updateDataSetTypes([update]).then(function () {
                                        c.ok("Data set type updated")
                                        var operation = new dtos.ConfirmDeletionsOperation([deletionId])
                                        operation.setForceDeletion(true)
                                        var options = new dtos.SynchronousOperationExecutionOptions()
                                        return facade.executeOperations([operation], options).then(function () {
                                            c.finish()
                                        })
                                    })
                                })
                            })
                        })
                    })
                    .fail(function (error) {
                        c.fail("Confirmation of deletion should not fail with the force flag")
                        c.finish()
                    })
            })

            QUnit.test("deletePlugins()", function (assert) {
                var c = new common(assert, dtos)
                testDeleteWithoutTrash(c, c.createPlugin, c.findPlugin, c.deletePlugin)
            })

            QUnit.test("deletePropertyTypes()", function (assert) {
                var c = new common(assert, dtos)
                testDeleteWithoutTrash(c, c.createPropertyType, c.findPropertyType, c.deletePropertyType)
            })

            QUnit.test("deleteVocabularies()", function (assert) {
                var c = new common(assert, dtos)
                testDeleteWithoutTrash(c, c.createVocabulary, c.findVocabulary, c.deleteVocabulary)
            })

            QUnit.test("deleteVocabularyTerms()", function (assert) {
                var c = new common(assert, dtos)
                testDeleteWithoutTrash(c, c.createVocabularyTerm, c.findVocabularyTerm, c.deleteVocabularyTerm)
            })

            QUnit.test("deleteExperimentTypes()", function (assert) {
                var c = new common(assert, dtos)
                testDeleteWithoutTrash(c, c.createExperimentType, c.findExperimentType, c.deleteExperimentType)
            })

            QUnit.test("deleteSampleTypes()", function (assert) {
                var c = new common(assert, dtos)
                testDeleteWithoutTrash(c, c.createSampleType, c.findSampleType, c.deleteSampleType)
            })

            QUnit.test("deleteDataSetTypes()", function (assert) {
                var c = new common(assert, dtos)
                testDeleteWithoutTrash(c, c.createDataSetType, c.findDataSetType, c.deleteDataSetType)
            })

            QUnit.test("deleteExternalDms()", function (assert) {
                var c = new common(assert, dtos)
                testDeleteWithoutTrash(c, c.createExternalDms, c.findExternalDms, c.deleteExternalDms)
            })

            QUnit.test("replaceVocabularyTerms()", function (assert) {
                var c = new common(assert, dtos)
                testDeleteWithoutTrash(c, c.createVocabularyTerm, c.findVocabularyTerm, c.replaceVocabularyTerm)
            })

            QUnit.test("deleteTags()", function (assert) {
                var c = new common(assert, dtos)
                testDeleteWithoutTrash(c, c.createTag, c.findTag, c.deleteTag)
            })

            QUnit.test("deleteAuthorizationGroups()", function (assert) {
                var c = new common(assert, dtos)
                testDeleteWithoutTrash(c, c.createAuthorizationGroup, c.findAuthorizationGroup, c.deleteAuthorizationGroup)
            })

            QUnit.test("deleteRoleAssignments()", function (assert) {
                var c = new common(assert, dtos)
                testDeleteWithoutTrash(c, c.createRoleAssignment, c.findRoleAssignment, c.deleteRoleAssignment)
            })

            QUnit.test("deleteOperationExecutions()", function (assert) {
                var c = new common(assert, dtos)

                var findNotDeletedOrDeletePending = function (facade: openbis.openbis, permId) {
                    return c.findOperationExecution(facade, permId).then(function (execution) {
                        if (!execution || execution.getAvailability() == "DELETE_PENDING" || execution.getAvailability() == "DELETED") {
                            return null
                        } else {
                            return execution
                        }
                    })
                }

                testDeleteWithoutTrash(c, c.createOperationExecution, findNotDeletedOrDeletePending, c.deleteOperationExecution)
            })

            QUnit.test("deleteSemanticAnnotations()", function (assert) {
                var c = new common(assert, dtos)
                testDeleteWithoutTrash(c, c.createSemanticAnnotation, c.findSemanticAnnotation, c.deleteSemanticAnnotation)
            })

            QUnit.test("deleteQueries()", function (assert) {
                var c = new common(assert, dtos)
                testDeleteWithoutTrash(c, c.createQuery, c.findQuery, c.deleteQuery)
            })

            QUnit.test("deletePersons()", function (assert) {
                var c = new common(assert, dtos)
                testDeleteWithoutTrash(c, c.createPerson, c.findPerson, c.deletePerson)
            })

            QUnit.test("deletePersonalAccessTokens()", function (assert) {
                var c = new common(assert, dtos)
                testDeleteWithoutTrash(c, c.createPersonalAccessToken, c.findPersonalAccessToken, c.deletePersonalAccessToken)
            })
        }

        resolve(function () {
            executeModule("Deletion tests (RequireJS)", new openbisRequireJS(), dtos)
            executeModule("Deletion tests (RequireJS - executeOperations)", new openbisExecuteOperations(new openbisRequireJS(), dtos), dtos)
            executeModule("Deletion tests (module VAR)", new window.openbis.openbis(), window.openbis)
            executeModule(
                "Deletion tests (module VAR - executeOperations)",
                new openbisExecuteOperations(new window.openbis.openbis(), window.openbis),
                window.openbis
            )
            executeModule("Deletion tests (module ESM)", new window.openbisESM.openbis(), window.openbisESM)
            executeModule(
                "Deletion tests (module ESM - executeOperations)",
                new openbisExecuteOperations(new window.openbisESM.openbis(), window.openbisESM),
                window.openbisESM
            )
        })
    })
})
