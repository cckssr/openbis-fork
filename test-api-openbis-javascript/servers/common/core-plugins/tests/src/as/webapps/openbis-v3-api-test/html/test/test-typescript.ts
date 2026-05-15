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
        var executeModule = function (moduleName: string, bundle: openbis.bundle) {
            QUnit.module(moduleName)

            QUnit.test("create facade", async function (assert) {
                try {
                    var c = new common(assert, bundle)
                    c.start()

                    var facadeWithoutParameters: openbis.openbis = new bundle.openbis()
                    c.assertNotNull(await c.login(facadeWithoutParameters), "Session token")

                    var facadeWithUrlParameter: openbis.openbis = new bundle.openbis("/openbis/openbis/rmi-application-server-v3.json")
                    c.assertNotNull(await c.login(facadeWithUrlParameter), "Session token")

                    c.finish()
                } catch (e) {
                    c.fail(e)
                }
            })

            QUnit.test("get DSS facade", async function (assert) {
                try {
                    var c = new common(assert, bundle)
                    c.start()

                    var facade: openbis.openbis = new bundle.openbis()

                    var dssFacadeWithoutCodes = facade.getDataStoreFacade()
                    var dssFacadeWithCodes = facade.getDataStoreFacade(["DSS1", "DSS2"])

                    c.assertNotNull(dssFacadeWithoutCodes)
                    c.assertNotNull(dssFacadeWithCodes)

                    c.finish()
                } catch (e) {
                    c.fail(e)
                }
            })

            QUnit.test("use short and long names of classes", async function (assert) {
                try {
                    var c = new common(assert, bundle)
                    c.start()

                    var shortShort: openbis.SampleCreation = new bundle.SampleCreation()
                    var shortLong: openbis.SampleCreation = new bundle.as_dto_sample_create_SampleCreation()
                    var longShort: openbis.as_dto_sample_create_SampleCreation = new bundle.SampleCreation()
                    var longLong: openbis.as_dto_sample_create_SampleCreation = new bundle.as_dto_sample_create_SampleCreation()

                    shortShort.setCreationId(new bundle.CreationId("shortShort"))
                    shortLong.setCreationId(new bundle.CreationId("shortLong"))
                    longShort.setCreationId(new bundle.CreationId("longShort"))
                    longLong.setCreationId(new bundle.CreationId("longLong"))

                    c.assertTrue(true)
                    c.finish()
                } catch (e) {
                    c.fail(e)
                }
            })

            QUnit.test("use enums", async function (assert) {
                try {
                    var c = new common(assert, bundle)
                    c.start()

                    var shortShort: openbis.DataType = bundle.DataType.INTEGER
                    var shortLong: openbis.DataType = bundle.as_dto_property_DataType.INTEGER
                    var shortString: openbis.DataType = "INTEGER"
                    var longShort: openbis.as_dto_property_DataType = bundle.DataType.INTEGER
                    var longLong: openbis.as_dto_property_DataType = bundle.as_dto_property_DataType.INTEGER
                    var longString: openbis.as_dto_property_DataType = "INTEGER"

                    var expected = "INTEGER"
                    c.assertEqual(shortShort, expected)
                    c.assertEqual(shortLong, expected)
                    c.assertEqual(shortString, expected)
                    c.assertEqual(longShort, expected)
                    c.assertEqual(longLong, expected)
                    c.assertEqual(longString, expected)

                    c.assertTrue(true)
                    c.finish()
                } catch (e) {
                    c.fail(e)
                }
            })

            QUnit.test("use dates", async function (assert) {
                try {
                    var c = new common(assert, bundle)
                    c.start()

                    var fo = new bundle.SampleFetchOptions()
                    fo.withProperties()
                    var object = new bundle.Sample()
                    object.setFetchOptions(fo)

                    var date: number = new Date().getTime()

                    object.setRegistrationDate(date)
                    object.setTimestampProperty("TEST", date)
                    object.setTimestampArrayProperty("TEST_ARRAY", [date])
                    object.setMultiValueTimestampProperty("TEST_MULTIVALUE", [date])
                    object.setMultiValueTimestampArrayProperty("TEST_MULTIVALUE_ARRAY", [[date]])

                    var registrationDate: number = object.getRegistrationDate()
                    var propertyValue: number = object.getTimestampProperty("TEST")
                    var propertyArrayValue: number[] = object.getTimestampArrayProperty("TEST_ARRAY")
                    var propertyMultiValue: number[] = object.getMultiValueTimestampProperty("TEST_MULTIVALUE")
                    var propertyMultiArrayValue: number[][] = object.getMultiValueTimestampArrayProperty("TEST_MULTIVALUE_ARRAY")

                    c.assertEqual(registrationDate, date)
                    c.assertEqual(propertyValue, date)
                    c.assertDeepEqual(propertyArrayValue, [date])
                    c.assertDeepEqual(propertyMultiValue, [date])
                    c.assertDeepEqual(propertyMultiArrayValue, [[date]])

                    c.finish()
                } catch (e) {
                    c.fail(e)
                }
            })

            QUnit.test("use generics", async function (assert) {
                try {
                    var c = new common(assert, bundle)
                    c.start()

                    var valueShort: openbis.AbstractValue<string> = { getValue: null, setValue: null }
                    var valueLong: openbis.as_dto_common_search_AbstractValue<string> = { getValue: null, setValue: null }

                    c.assertNotNull(valueShort)
                    c.assertNotNull(valueLong)

                    var searchShortShort: openbis.SearchObjectsOperationResult<openbis.Sample> = new bundle.SearchSamplesOperationResult(
                        new bundle.SearchResult<openbis.Sample>([], 0)
                    )

                    var searchShortLong: openbis.SearchObjectsOperationResult<openbis.Sample> =
                        new bundle.as_dto_common_search_SearchObjectsOperationResult(
                            new bundle.as_dto_common_search_SearchResult<openbis.as_dto_sample_Sample>([], 0)
                        )

                    var searchLongShort: openbis.as_dto_common_search_SearchObjectsOperationResult<openbis.as_dto_sample_Sample> =
                        new bundle.SearchSamplesOperationResult(new bundle.SearchResult<openbis.Sample>([], 0))

                    var searchLongLong: openbis.as_dto_common_search_SearchObjectsOperationResult<openbis.as_dto_sample_Sample> =
                        new bundle.as_dto_common_search_SearchObjectsOperationResult(
                            new bundle.as_dto_common_search_SearchResult<openbis.as_dto_sample_Sample>([], 0)
                        )

                    c.assertNotNull(searchShortShort)
                    c.assertNotNull(searchShortLong)
                    c.assertNotNull(searchLongShort)
                    c.assertNotNull(searchLongLong)

                    c.finish()
                } catch (e) {
                    c.fail(e)
                }
            })
        }

        resolve(function () {
            executeModule("TypeScript tests (RequireJS)", Object.assign({}, dtos, { openbis: openbisRequireJS }))
            executeModule("TypeScript tests (module VAR)", window.openbis)
            executeModule("TypeScript tests (module ESM)", window.openbisESM)
        })
    })
})
