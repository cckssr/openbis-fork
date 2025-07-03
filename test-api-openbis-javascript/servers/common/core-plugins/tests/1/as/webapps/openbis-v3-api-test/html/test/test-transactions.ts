import jquery from "./types/jquery"
import underscore from "./types/underscore"
import common from "./types/common"
import openbis from "./types/openbis.esm"

exports.default = new Promise((resolve) => {
    require(["jquery", "underscore", "openbis", "test/common", "test/dtos"], function (
        $: jquery.JQueryStatic,
        _: underscore.UnderscoreStatic,
        openbisRequireJS,
        common: common.CommonConstructor,
        dtos
    ) {
        var executeModule = function (moduleName: string, createFacade: () => openbis.openbis, dtos: openbis.bundle) {
            QUnit.module(moduleName)

            QUnit.test("begin() without session token", async function (assert) {
                const testInteractiveSessionKey = "test-interactive-session-key"

                try {
                    var c = new common(assert, dtos)
                    c.start()

                    var facade = createFacade()

                    facade.setInteractiveSessionKey(testInteractiveSessionKey)

                    try {
                        await facade.beginTransaction()
                        c.fail()
                    } catch (error) {
                        c.assertEqual(error.message, "Session token hasn't been set")
                    }

                    c.finish()
                } catch (error) {
                    c.fail(error)
                    c.finish()
                }
            })

            QUnit.test("begin() without interactive session key", async function (assert) {
                try {
                    var c = new common(assert, dtos)
                    c.start()

                    var facade = createFacade()

                    await c.login(facade)

                    try {
                        await facade.beginTransaction()
                        c.fail()
                    } catch (error) {
                        c.assertEqual(error.message, "Interactive session token hasn't been set")
                    }

                    c.finish()
                } catch (error) {
                    c.fail(error)
                    c.finish()
                }
            })

            QUnit.test("begin() with incorrect interactive session key", async function (assert) {
                const incorrectInteractiveSessionKey = "incorrect-interactive-session-key"

                try {
                    var c = new common(assert, dtos)
                    c.start()

                    var facade = createFacade()

                    await c.login(facade)
                    facade.setInteractiveSessionKey(incorrectInteractiveSessionKey)

                    try {
                        await facade.beginTransaction()
                        c.fail()
                    } catch (error) {
                        c.assertEqual(error.message, "Invalid interactive session key")
                    }

                    c.finish()
                } catch (error) {
                    c.fail(error)
                    c.finish()
                }
            })

            QUnit.test("begin() with already started transaction", async function (assert) {
                const testInteractiveSessionKey = "test-interactive-session-key"

                try {
                    var c = new common(assert, dtos)
                    c.start()

                    var facade = createFacade()

                    await c.login(facade)
                    facade.setInteractiveSessionKey(testInteractiveSessionKey)

                    var transactionId = await facade.beginTransaction()

                    try {
                        await facade.beginTransaction()
                        c.fail()
                    } catch (error) {
                        c.assertEqual(
                            error.message,
                            "Operation cannot be executed. Expected no active transactions, but found transaction '" + transactionId + "'."
                        )
                    }

                    c.finish()
                } catch (error) {
                    c.fail(error)
                    c.finish()
                }
            })

            QUnit.test("begin() more than one transaction per session token", async function (assert) {
                const testInteractiveSessionKey = "test-interactive-session-key"

                try {
                    var c = new common(assert, dtos)
                    c.start()

                    var facade1 = createFacade()
                    var facade2 = createFacade()

                    facade1.setInteractiveSessionKey(testInteractiveSessionKey)
                    facade2.setInteractiveSessionKey(testInteractiveSessionKey)

                    await c.login(facade1)

                    // make both facades use the same session token
                    var facade1Any = <any>facade1
                    var facade2Any = <any>facade2
                    facade2Any._private.sessionToken = facade1Any._private.sessionToken

                    var firstTransactionId = await facade1.beginTransaction()

                    try {
                        await facade2.beginTransaction()
                        c.fail()
                    } catch (error) {
                        c.assertTrue(error.message.startsWith("Cannot create more than one transaction for the same session token."))
                        c.assertTrue(error.message.endsWith("The already existing and still active transaction: '" + firstTransactionId + "'."))
                    }

                    c.finish()
                } catch (error) {
                    c.fail(error)
                    c.finish()
                }
            })

            QUnit.test("begin() and rollback() with AS and AFS methods", async function (assert) {
                const testInteractiveSessionKey = "test-interactive-session-key"
                const testFile = "test-file"
                const testContent = new TextEncoder().encode("test-content")

                try {
                    var c = new common(assert, dtos)
                    c.start()

                    var facade = createFacade()

                    await c.login(facade)

                    var ownerPermId = (await c.createSample(facade)).getPermId()
                    await c.deleteFile(facade, ownerPermId, "")

                    facade.setInteractiveSessionKey(testInteractiveSessionKey)

                    await facade.beginTransaction()

                    const spaceCreation = new dtos.SpaceCreation()
                    spaceCreation.setCode(c.generateId("TRANSACTION_TEST_"))

                    const spaceIds = await facade.createSpaces([spaceCreation])

                    const spacesBeforeRollback = await facade.getSpaces(spaceIds, new dtos.SpaceFetchOptions())
                    c.assertEqual(Object.keys(spacesBeforeRollback).length, 1, "Space exists in the transaction")

                    const projectCreation = new dtos.ProjectCreation()
                    projectCreation.setCode(c.generateId("TRANSACTION_TEST_"))
                    projectCreation.setSpaceId(spaceIds[0])

                    const projectIds = await facade.createProjects([projectCreation])

                    const projectsBeforeRollback = await facade.getProjects(projectIds, new dtos.ProjectFetchOptions())
                    c.assertEqual(Object.keys(projectsBeforeRollback).length, 1, "Project exists in the transction")

                    await facade.getAfsServerFacade().write(ownerPermId, testFile, 0, testContent)

                    await facade.rollbackTransaction()

                    const spacesAfterRollback = await facade.getSpaces(spaceIds, new dtos.SpaceFetchOptions())
                    c.assertEqual(Object.keys(spacesAfterRollback).length, 0, "Space does not exist after a rollback")

                    const projectsAfterRollback = await facade.getProjects(projectIds, new dtos.ProjectFetchOptions())
                    c.assertEqual(Object.keys(projectsAfterRollback).length, 0, "Project does not exist after a rollback")

                    await c.assertFileDoesNotExist(facade, ownerPermId, testFile)

                    c.finish()
                } catch (error) {
                    c.fail(error)
                    c.finish()
                }
            })

            QUnit.test("begin() and commit() with AS and AFS methods", async function (assert) {
                const testInteractiveSessionKey = "test-interactive-session-key"
                const testFile = "test-file"
                const testContent = new TextEncoder().encode("test-content")

                try {
                    var c = new common(assert, dtos)
                    c.start()

                    var facade = createFacade()

                    await c.login(facade)

                    var ownerPermId = (await c.createSample(facade)).getPermId()
                    await c.deleteFile(facade, ownerPermId, "")

                    facade.setInteractiveSessionKey(testInteractiveSessionKey)

                    await facade.beginTransaction()

                    const spaceCreation = new dtos.SpaceCreation()
                    spaceCreation.setCode(c.generateId("TRANSACTION_TEST_"))

                    const spaceIds = await facade.createSpaces([spaceCreation])

                    const spacesBeforeCommit = await facade.getSpaces(spaceIds, new dtos.SpaceFetchOptions())
                    c.assertEqual(Object.keys(spacesBeforeCommit).length, 1, "Space exists in the transaction")

                    const projectCreation = new dtos.ProjectCreation()
                    projectCreation.setCode(c.generateId("TRANSACTION_TEST_"))
                    projectCreation.setSpaceId(spaceIds[0])

                    const projectIds = await facade.createProjects([projectCreation])

                    const projectsBeforeCommit = await facade.getProjects(projectIds, new dtos.ProjectFetchOptions())
                    c.assertEqual(Object.keys(projectsBeforeCommit).length, 1, "Project exists in the transction")

                    await facade.getAfsServerFacade().write(ownerPermId, testFile, 0, testContent)

                    await facade.commitTransaction()

                    const spacesAfterCommit = await facade.getSpaces(spaceIds, new dtos.SpaceFetchOptions())
                    c.assertEqual(Object.keys(spacesAfterCommit).length, 1, "Space exist after commit")

                    const projectsAfterCommit = await facade.getProjects(projectIds, new dtos.ProjectFetchOptions())
                    c.assertEqual(Object.keys(projectsAfterCommit).length, 1, "Project exists after commit")

                    await c.assertFileExists(facade, ownerPermId, testFile)

                    var filesAfterCommit = await facade.getAfsServerFacade().list(ownerPermId, "", false)

                    c.assertFileEquals(filesAfterCommit[0], {
                        path: "/" + testFile,
                        owner: ownerPermId,
                        name: testFile,
                        size: "test-content".length,
                        directory: false,
                    })

                    var fileContentAfterCommit = await facade.getAfsServerFacade().read(ownerPermId, testFile, 0, "test-content".length)
                    c.assertEqual(await fileContentAfterCommit.text(), "test-content")

                    c.finish()
                } catch (error) {
                    c.fail(error)
                    c.finish()
                }
            })

            QUnit.test("rollback() without transaction", async function (assert) {
                const testInteractiveSessionKey = "test-interactive-session-key"

                try {
                    var c = new common(assert, dtos)
                    c.start()

                    var facade = createFacade()

                    await c.login(facade)
                    facade.setInteractiveSessionKey(testInteractiveSessionKey)

                    try {
                        await facade.rollbackTransaction()
                        c.fail()
                    } catch (error) {
                        c.assertEqual(error.message, "Operation cannot be executed. No active transaction found.")
                    }

                    c.finish()
                } catch (error) {
                    c.fail(error)
                    c.finish()
                }
            })

            QUnit.test("commit() without transaction", async function (assert) {
                const testInteractiveSessionKey = "test-interactive-session-key"

                try {
                    var c = new common(assert, dtos)
                    c.start()

                    var facade = createFacade()

                    await c.login(facade)
                    facade.setInteractiveSessionKey(testInteractiveSessionKey)

                    try {
                        await facade.commitTransaction()
                        c.fail()
                    } catch (error) {
                        c.assertEqual(error.message, "Operation cannot be executed. No active transaction found.")
                    }

                    c.finish()
                } catch (error) {
                    c.fail(error)
                    c.finish()
                }
            })

            QUnit.test("AS method failing", async function (assert) {
                const testInteractiveSessionKey = "test-interactive-session-key"

                try {
                    var c = new common(assert, dtos)
                    c.start()

                    var facade = createFacade()

                    await c.login(facade)

                    facade.setInteractiveSessionKey(testInteractiveSessionKey)

                    var transactionId = await facade.beginTransaction()

                    try {
                        await facade.createSpaces([new dtos.SpaceCreation()])
                        c.fail()
                    } catch (error) {
                        c.assertTrue(
                            error.message.startsWith(
                                "Transaction '" +
                                    transactionId +
                                    "' execute operation 'createSpaces' for participant 'application-server' failed with error: Code cannot be empty."
                            )
                        )
                    }

                    await facade.rollbackTransaction()

                    c.finish()
                } catch (error) {
                    c.fail(error)
                    c.finish()
                }
            })

            QUnit.test("AFS method failing", async function (assert) {
                const testInteractiveSessionKey = "test-interactive-session-key"

                try {
                    var c = new common(assert, dtos)
                    c.start()

                    var facade = createFacade()

                    await c.login(facade)

                    var ownerPermId = (await c.createSample(facade)).getPermId()
                    await c.deleteFile(facade, ownerPermId, "")

                    facade.setInteractiveSessionKey(testInteractiveSessionKey)

                    var transactionId = await facade.beginTransaction()

                    try {
                        await facade.getAfsServerFacade().read(ownerPermId, "i-dont-exist", 0, 0)
                        c.fail()
                    } catch (error) {
                        c.assertTrue(
                            error.message.startsWith(
                                "Transaction '" + transactionId + "' execute operation 'read' for participant 'afs-server' failed with error:"
                            )
                        )
                        c.assertTrue(error.message.includes("NoSuchFileException"))
                    }

                    await facade.rollbackTransaction()

                    c.finish()
                } catch (error) {
                    c.fail(error)
                    c.finish()
                }
            })

            QUnit.test("DSS methods cannot be used in transactions", async function (assert) {
                const testInteractiveSessionKey = "test-interactive-session-key"

                try {
                    var c = new common(assert, dtos)
                    c.start()

                    var facade = createFacade()

                    await c.login(facade)

                    facade.setInteractiveSessionKey(testInteractiveSessionKey)

                    await facade.beginTransaction()

                    try {
                        await facade.getDataStoreFacade().searchFiles(new dtos.DataSetFileSearchCriteria(), new dtos.DataSetFileFetchOptions())
                        c.fail()
                    } catch (error) {
                        c.assertEqual(error.message, "Transactions are not supported for data store methods.")
                    }

                    await facade.rollbackTransaction()

                    c.finish()
                } catch (error) {
                    c.fail(error)
                    c.finish()
                }
            })
        }

        resolve(function () {
            var afsServerUrl = "http://localhost:8085/afs-server"
            executeModule("Transactions tests (RequireJS)", () => new openbisRequireJS(null, afsServerUrl), dtos)
            executeModule("Transactions tests (module VAR)", () => new window.openbis.openbis(null, afsServerUrl), window.openbis)
            executeModule("Transactions tests (module ESM)", () => new window.openbisESM.openbis(null, afsServerUrl), window.openbisESM)
        })
    })
})
