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

            QUnit.test("loginAs()", async function (assert) {
                var c = new common(assert, dtos)
                c.start()

                var facade = createFacade()

                var criteria = new dtos.SpaceSearchCriteria()
                var fetchOptions = new dtos.SpaceFetchOptions()

                facade.login("openbis_test_js", "password").then(
                    function () {
                        return facade.searchSpaces(criteria, fetchOptions).then(function (spacesForInstanceAdmin) {
                            return facade.loginAs("openbis_test_js", "password", "test_space_admin").then(function () {
                                return facade.searchSpaces(criteria, fetchOptions).then(function (spacesForSpaceAdmin) {
                                    c.assertTrue(spacesForInstanceAdmin.getTotalCount() > spacesForSpaceAdmin.getTotalCount())
                                    c.assertObjectsWithValues(spacesForSpaceAdmin.getObjects(), "code", ["TEST"])
                                    c.finish()
                                })
                            })
                        })
                    },
                    function (error) {
                        c.fail(error.message)
                        c.finish()
                    }
                )
            })

            QUnit.test("getSessionInformation()", async function (assert) {
                var c = new common(assert, dtos)
                c.start()

                var facade = createFacade()

                facade.login("openbis_test_js", "password").then(
                    function () {
                        return facade.getSessionInformation().then(function (sessionInformation) {
                            c.assertTrue(sessionInformation != null)
                            c.assertTrue(sessionInformation.getPerson() != null)
                            c.finish()
                        })
                    },
                    function (error) {
                        c.fail(error.message)
                        c.finish()
                    }
                )
            })

            QUnit.test("loginAsAnonymousUser()", async function (assert) {
                var c = new common(assert, dtos)
                c.start()

                var facade = createFacade()

                var criteria = new dtos.SpaceSearchCriteria()
                var fetchOptions = new dtos.SpaceFetchOptions()

                facade.loginAsAnonymousUser().then(
                    function () {
                        return facade.searchSpaces(criteria, fetchOptions).then(function (spaces) {
                            c.assertTrue(spaces.getTotalCount() == 1)
                            c.finish()
                        })
                    },
                    function (error) {
                        c.fail(error.message)
                        c.finish()
                    }
                )
            })

            QUnit.test("setSessionToken() with session token", async function (assert) {
                var c = new common(assert, dtos)
                c.start()

                try {
                    var userId = "openbis_test_js";

                    var facade = createFacade()
                    var facade2 = createFacade()

                    var sessionToken = await facade.login(userId, "password")
                    facade2.setSessionToken(sessionToken)

                    var sessionInformation = await facade2.getSessionInformation()
                    c.assertEqual(sessionInformation.getUserName(), userId)
                } finally {
                    c.finish()
                }
            })

            QUnit.test("setSessionToken() with personal access token", async function (assert) {
                var c = new common(assert, dtos)
                c.start()

                try {
                    var userId = "openbis_test_js"

                    var facade = createFacade()
                    var facade2 = createFacade()

                    await facade.login(userId, "password")

                    var patCreation = new dtos.PersonalAccessTokenCreation()
                    patCreation.setOwnerId(new dtos.PersonPermId(userId))
                    patCreation.setSessionName("test-session")
                    patCreation.setValidFromDate(new Date().getTime())
                    patCreation.setValidToDate(new Date().getTime() + 24 * 60 * 60 * 1000)

                    var patPermIds = await facade.createPersonalAccessTokens([patCreation])

                    await facade.logout()

                    facade2.setSessionToken(patPermIds[0].getPermId())

                    var sessionInformation = await facade2.getSessionInformation()
                    c.assertEqual(sessionInformation.getUserName(), userId)
                } finally {
                    c.finish()
                }
            })

        }

        resolve(function () {
            executeModule("Login tests (RequireJS)", () => new openbisRequireJS(), dtos)
            executeModule("Login tests (module VAR)", () => new window.openbis.openbis(), window.openbis)
            executeModule("Login tests (module ESM)", () => new window.openbisESM.openbis(), window.openbisESM)
        })
    })
})
