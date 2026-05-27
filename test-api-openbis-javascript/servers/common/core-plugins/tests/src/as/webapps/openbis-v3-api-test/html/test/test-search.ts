import jquery from './types/jquery'
import underscore from './types/underscore'
import common from './types/common'
import openbis from './types/openbis.esm'

exports.default = new Promise((resolve) => {
    require(["jquery", "underscore", "openbis", "test/common", "test/openbis-execute-operations", "test/dtos", "test/naturalsort"], function (
        $: jquery.JQueryStatic,
        _: underscore.UnderscoreStatic,
        openbisRequireJS,
        common: common.CommonConstructor,
        openbisExecuteOperations,
        dtos,
        naturalsort
    ) {
        var executeModule = function (moduleName: string, facade: openbis.openbis, dtos: openbis.bundle) {
            QUnit.module(moduleName)

            var testSearch = function (c: common.CommonClass, fSearch, fCheck, fCheckError?) {
                c.start()

                c.login(facade)
                    .then(function () {
                        c.ok("Login")
                        return fSearch(facade).then(function (results) {
                            c.ok("Got results")
                            try {
                                fCheck(facade, results.getObjects())
                            } catch (e) {
                                if (fCheckError) {
                                    fCheckError(e)
                                } else {
                                    c.fail(e.message)
                                }
                                console.error("Exception.", e.message)
                                throw e
                            } finally {
                                c.finish()
                            }
                        })
                    })
                    .fail(function (error) {
                        if (fCheckError) {
                            fCheckError(error)
                        } else {
                            c.fail(error ? error.message : "Unknown error.")
                        }
                        c.finish()
                    })
            }

            var testSearchWithPagingAndSortingByAll = function (c: common.CommonClass, fSearch, fetchOptions) {
                testSearchWithPagingAndSorting(c, fSearch, fetchOptions, "code").then(function () {
                    testSearchWithPagingAndSorting(c, fSearch, fetchOptions, "registrationDate")
                })
            }

            var testSearchWithPagingAndGenericSorting = function (
                c: common.CommonClass,
                fSearch,
                fetchOptions,
                fieldName,
                fieldParameters,
                disableSortCheck,
                codeOfFirstAsc,
                comparator
            ) {
                c.start()

                fetchOptions.from(null).count(null)
                fetchOptions.sort = null

                return c
                    .login(facade)
                    .then(function () {
                        c.ok("Login")

                        return fSearch(facade).then(function (results) {
                            var objects = results.getObjects()

                            c.assertTrue(objects.length > 1, "Got at least 2 objects")

                            c.ok("Sorting by " + fieldName)
                            var fieldGetterName = "get" + fieldName.substr(0, 1).toUpperCase() + fieldName.substr(1)

                            if (disableSortCheck && codeOfFirstAsc) {
                                fetchOptions.from(0).count(1)
                            } else {
                                var comparatorAsc = function (o1, o2) {
                                    var v1 = o1[fieldGetterName](fieldParameters)
                                    var v2 = o2[fieldGetterName](fieldParameters)

                                    return comparator(v1, v2)
                                }

                                var comparatorDesc = function (o1, o2) {
                                    return comparatorAsc(o2, o1)
                                }

                                objects.sort(comparatorAsc)
                                var codesAsc = objects.map(function (object) {
                                    return object.code
                                })

                                objects.sort(comparatorDesc)
                                var codesDesc = objects.map(function (object) {
                                    return object.code
                                })

                                fetchOptions.from(1).count(1)
                            }

                            fetchOptions.sortBy()[fieldName](fieldParameters)
                            return fSearch(facade).then(function (results) {
                                c.ok("Got results ASC")
                                if (disableSortCheck && codeOfFirstAsc) {
                                    c.assertObjectsWithValues(results.getObjects(), "code", [codeOfFirstAsc])
                                    fetchOptions.from(null).count(null)
                                } else {
                                    c.assertObjectsWithValues(results.getObjects(), "code", [codesAsc[1]])
                                }

                                fetchOptions.sortBy()[fieldName](fieldParameters).desc()
                                return fSearch(facade).then(function (results) {
                                    c.ok("Got results DESC")
                                    if (disableSortCheck && codeOfFirstAsc) {
                                        var lastObject = results.getObjects()[results.getObjects().length - 1]
                                        c.assertObjectsWithValues([lastObject], "code", [codeOfFirstAsc])
                                    } else {
                                        c.assertObjectsWithValues(results.getObjects(), "code", [codesDesc[1]])
                                    }
                                    c.finish()
                                })
                            })
                        })
                    })
                    .fail(function (error) {
                        c.fail(error.message)
                        c.finish()
                    })
            }

            var testSearchWithPagingAndSorting = function (
                c: common.CommonClass,
                fSearch,
                fetchOptions,
                fieldName,
                fieldParameters?,
                disableSortCheck?,
                codeOfFirstAsc?
            ) {
                return testSearchWithPagingAndGenericSorting(
                    c,
                    fSearch,
                    fetchOptions,
                    fieldName,
                    fieldParameters,
                    disableSortCheck,
                    codeOfFirstAsc,
                    naturalsort
                )
            }

            var testSearchWithPagingAndStringSorting = function (
                c: common.CommonClass,
                fSearch,
                fetchOptions,
                fieldName,
                fieldParameters,
                disableSortCheck?,
                codeOfFirstAsc?
            ) {
                return testSearchWithPagingAndGenericSorting(
                    c,
                    fSearch,
                    fetchOptions,
                    fieldName,
                    fieldParameters,
                    disableSortCheck,
                    codeOfFirstAsc,
                    (v1, v2) => (v1 > v2 ? -1 : v2 > v1 ? 1 : 0)
                )
            }

            QUnit.test("searchSpaces()", function (assert) {
                var c = new common(assert, dtos)

                var fSearch = function (facade: openbis.openbis) {
                    var criteria = new dtos.SpaceSearchCriteria()
                    criteria.withCode().thatEquals("TEST")
                    return facade.searchSpaces(criteria, c.createSpaceFetchOptions())
                }

                var fCheck = function (facade: openbis.openbis, spaces: openbis.Space[]) {
                    c.assertEqual(spaces.length, 1)
                    var space = spaces[0]
                    c.assertEqual(space.getPermId(), "TEST", "PermId")
                    c.assertEqual(space.getCode(), "TEST", "Code")
                    c.assertEqual(space.getDescription(), null, "Description")
                    c.assertDate(space.getRegistrationDate(), "Registration date", 2013, 4, 12, 12, 59)
                    c.assertEqual(space.getRegistrator().getUserId(), "admin", "Registrator userId")
                    c.assertObjectsWithCollections([space], function (object) {
                        return object.getSamples()
                    })
                    c.assertObjectsWithCollections([space], function (object) {
                        return object.getProjects()
                    })
                }

                testSearch(c, fSearch, fCheck)
            })

            QUnit.test("searchSpaces() with codes", function (assert) {
                var c = new common(assert, dtos)

                var fSearch = function (facade: openbis.openbis) {
                    var criteria = new dtos.SpaceSearchCriteria()
                    criteria.withCodes().thatIn(["TEST"])
                    return facade.searchSpaces(criteria, c.createSpaceFetchOptions())
                }

                var fCheck = function (facade: openbis.openbis, spaces: openbis.Space[]) {
                    c.assertEqual(spaces.length, 1)
                    var space = spaces[0]
                    c.assertEqual(space.getCode(), "TEST", "Code")
                    c.assertEqual(space.getDescription(), null, "Description")
                    c.assertDate(space.getRegistrationDate(), "Registration date", 2013, 4, 12, 12, 59)
                }

                testSearch(c, fSearch, fCheck)
            })

            QUnit.test("searchSpaces() with paging and sorting", function (assert) {
                var c = new common(assert, dtos)

                var criteria = new dtos.SpaceSearchCriteria()
                criteria.withOrOperator()
                criteria.withCode().thatEquals("TEST")
                criteria.withCode().thatEquals("PLATONIC")

                var fo = c.createSpaceFetchOptions()

                testSearchWithPagingAndSortingByAll(
                    c,
                    function (facade: openbis.openbis) {
                        return facade.searchSpaces(criteria, fo)
                    },
                    fo
                )
            })

            QUnit.test("searchProjects()", function (assert) {
                var c = new common(assert, dtos)

                var fSearch = function (facade: openbis.openbis) {
                    var criteria = new dtos.ProjectSearchCriteria()
                    criteria.withSpace().withCode().thatEquals("PLATONIC")
                    criteria.withCode().thatEquals("SCREENING-EXAMPLES")
                    return facade.searchProjects(criteria, c.createProjectFetchOptions())
                }

                var fCheck = function (facade: openbis.openbis, projects: openbis.Project[]) {
                    c.assertEqual(projects.length, 1)
                    var project = projects[0]
                    c.assertEqual(project.getPermId().getPermId(), "20130412103942912-1", "PermId")
                    c.assertEqual(project.getIdentifier().getIdentifier(), "/PLATONIC/SCREENING-EXAMPLES", "Identifier")
                    c.assertEqual(project.getCode(), "SCREENING-EXAMPLES", "Code")
                    c.assertEqual(project.getDescription(), null, "Description")
                    c.assertDate(project.getRegistrationDate(), "Registration date", 2013, 4, 12, 8, 39)
                    c.assertObjectsWithCollections([project], function (object) {
                        return object.getExperiments()
                    })
                    c.assertEqual(project.getSpace().getCode(), "PLATONIC", "Space code")
                    c.assertEqual(project.getRegistrator().getUserId(), "admin", "Registrator userId")
                    c.assertEqual(project.getLeader(), null, "Leader")
                    c.assertObjectsWithoutCollections([project], function (object) {
                        return object.getAttachments()
                    })
                }

                testSearch(c, fSearch, fCheck)
            })

            QUnit.test("searchProjects() with codes", function (assert) {
                var c = new common(assert, dtos)

                var fSearch = function (facade: openbis.openbis) {
                    var criteria = new dtos.ProjectSearchCriteria()
                    criteria.withCodes().thatIn(["SCREENING-EXAMPLES"])
                    return facade.searchProjects(criteria, c.createProjectFetchOptions())
                }

                var fCheck = function (facade: openbis.openbis, projects: openbis.Project[]) {
                    c.assertEqual(projects.length, 1)
                    var project = projects[0]
                    c.assertEqual(project.getCode(), "SCREENING-EXAMPLES", "Code")
                }

                testSearch(c, fSearch, fCheck)
            })

            QUnit.test("searchProjects() with paging and sorting", function (assert) {
                var c = new common(assert, dtos)

                var criteria = new dtos.ProjectSearchCriteria()
                criteria.withOrOperator()
                criteria.withCode().thatEquals("TEST-PROJECT")
                criteria.withCode().thatEquals("SCREENING-EXAMPLES")

                var fo = c.createProjectFetchOptions()

                testSearchWithPagingAndSortingByAll(
                    c,
                    function (facade: openbis.openbis) {
                        return facade.searchProjects(criteria, fo)
                    },
                    fo
                )
            })

            QUnit.test("searchProjects() with sorting by identifier", function (assert) {
                var c = new common(assert, dtos)

                var criteria = new dtos.ProjectSearchCriteria()
                criteria.withOrOperator()
                criteria.withId().thatEquals(new dtos.ProjectIdentifier("/TEST/TEST-PROJECT"))
                criteria.withId().thatEquals(new dtos.ProjectIdentifier("/PLATONIC/SCREENING-EXAMPLES"))

                var fo = c.createProjectFetchOptions()

                testSearchWithPagingAndSorting(
                    c,
                    function (facade: openbis.openbis) {
                        return facade.searchProjects(criteria, fo)
                    },
                    fo,
                    "identifier"
                )
            })

            QUnit.test("searchExperiments()", function (assert) {
                var c = new common(assert, dtos)

                var fSearch = function (facade: openbis.openbis) {
                    var criteria = new dtos.ExperimentSearchCriteria()
                    criteria.withPermId().thatEquals("20130412105232616-2")
                    return facade.searchExperiments(criteria, c.createExperimentFetchOptions())
                }

                var fCheck = function (facade: openbis.openbis, experiments: openbis.Experiment[]) {
                    c.assertEqual(experiments.length, 1)
                    var experiment = experiments[0]
                    c.assertEqual(experiment.getCode(), "EXP-1", "Experiment code")
                    c.assertEqual(experiment.getType().getCode(), "HCS_PLATONIC", "Type code")
                    c.assertEqual(experiment.getProject().getCode(), "SCREENING-EXAMPLES", "Project code")
                    c.assertEqual(experiment.getProject().getSpace().getCode(), "PLATONIC", "Space code")
                }

                testSearch(c, fSearch, fCheck)
            })

            QUnit.test("searchExperiments() with codes", function (assert) {
                var c = new common(assert, dtos)

                var fSearch = function (facade: openbis.openbis) {
                    var criteria = new dtos.ExperimentSearchCriteria()
                    criteria.withCodes().thatIn(["EXP-1"])
                    return facade.searchExperiments(criteria, c.createExperimentFetchOptions())
                }

                var fCheck = function (facade: openbis.openbis, experiments: openbis.Experiment[]) {
                    c.assertEqual(experiments.length, 1)
                    var experiment = experiments[0]
                    c.assertEqual(experiment.getCode(), "EXP-1", "Experiment code")
                    c.assertEqual(experiment.getType().getCode(), "HCS_PLATONIC", "Type code")
                    c.assertEqual(experiment.getProject().getCode(), "SCREENING-EXAMPLES", "Project code")
                    c.assertEqual(experiment.getProject().getSpace().getCode(), "PLATONIC", "Space code")
                }

                testSearch(c, fSearch, fCheck)
            })

            QUnit.test("searchExperiments() with paging and sorting", function (assert) {
                var c = new common(assert, dtos)

                var criteria = new dtos.ExperimentSearchCriteria()
                criteria.withOrOperator()
                criteria.withCode().thatEquals("EXP-1")
                criteria.withCode().thatEquals("EXP-2")

                var fo = c.createExperimentFetchOptions()

                testSearchWithPagingAndSortingByAll(
                    c,
                    function (facade: openbis.openbis) {
                        return facade.searchExperiments(criteria, fo)
                    },
                    fo
                )
            })

            QUnit.test("searchExperiments() with paging and sorting by code", function (assert) {
                var c = new common(assert, dtos)

                var criteria = new dtos.ExperimentSearchCriteria()
                criteria.withOrOperator()
                criteria.withCode().thatContains("EXP")
                criteria.withCode().thatContains("-1")

                var fo = c.createExperimentFetchOptions()

                testSearchWithPagingAndStringSorting(
                    c,
                    function (facade: openbis.openbis) {
                        return facade.searchExperiments(criteria, fo)
                    },
                    fo,
                    "code",
                    null,
                    true,
                    "DEFAULT_EXPERIMENT"
                )
            })

            QUnit.test("searchExperiments() with sorting by identifier", function (assert) {
                var c = new common(assert, dtos)

                var criteria = new dtos.ExperimentSearchCriteria()
                criteria.withOrOperator()
                criteria.withId().thatEquals(new dtos.ExperimentIdentifier("/TEST/TEST-PROJECT/TEST-EXPERIMENT"))
                criteria.withId().thatEquals(new dtos.ExperimentIdentifier("/PLATONIC/SCREENING-EXAMPLES/EXP-2"))

                var fo = c.createExperimentFetchOptions()

                testSearchWithPagingAndSorting(
                    c,
                    function (facade: openbis.openbis) {
                        return facade.searchExperiments(criteria, fo)
                    },
                    fo,
                    "identifier"
                )
            })

            QUnit.test("searchExperiments() with sorting by type", function (assert) {
                var c = new common(assert, dtos)

                var criteria = new dtos.ExperimentSearchCriteria()
                criteria.withOrOperator()
                criteria.withId().thatEquals(new dtos.ExperimentIdentifier("/TEST/TEST-PROJECT/TEST-EXPERIMENT"))
                criteria.withId().thatEquals(new dtos.ExperimentIdentifier("/PLATONIC/SCREENING-EXAMPLES/EXP-2"))

                var fo = c.createExperimentFetchOptions()

                testSearchWithPagingAndSorting(
                    c,
                    function (facade: openbis.openbis) {
                        return facade.searchExperiments(criteria, fo)
                    },
                    fo,
                    "type"
                )
            })

            QUnit.test("searchExperiments() withRegistrator withUserId", function (assert) {
                var c = new common(assert, dtos)

                var fSearch = function (facade: openbis.openbis) {
                    var criteria = new dtos.ExperimentSearchCriteria()
                    criteria.withRegistrator().withUserId().thatEquals("etlserver")
                    return facade.searchExperiments(criteria, c.createExperimentFetchOptions())
                }

                var fCheck = function (facade: openbis.openbis, experiments: openbis.Experiment[]) {
                    c.assertObjectsWithValues(experiments, "code", ["TEST-EXPERIMENT"])
                }

                testSearch(c, fSearch, fCheck)
            })

            QUnit.test("searchExperiments() withModifier withUserId", function (assert) {
                var c = new common(assert, dtos)

                var fSearch = function (facade: openbis.openbis) {
                    var criteria = new dtos.ExperimentSearchCriteria()
                    criteria.withModifier().withUserId().thatEquals("etlserver")
                    return facade.searchExperiments(criteria, c.createExperimentFetchOptions())
                }

                var fCheck = function (facade: openbis.openbis, experiments: openbis.Experiment[]) {
                    c.assertObjectsWithValues(experiments, "code", ["EXP-1", "EXP-2", "TEST-EXPERIMENT-3"])
                }

                testSearch(c, fSearch, fCheck)
            })

            QUnit.test("searchExperiments() withModifier withUserId negated", function (assert) {
                var c = new common(assert, dtos)

                var fSearch = function (facade: openbis.openbis) {
                    var criteria = new dtos.ExperimentSearchCriteria().withAndOperator()
                    criteria.withModifier().withUserId().thatEquals("etlserver")
                    criteria.withSubcriteria().negate().withCode().thatEndsWith("-2")
                    return facade.searchExperiments(criteria, c.createExperimentFetchOptions())
                }

                var fCheck = function (facade: openbis.openbis, experiments: openbis.Experiment[]) {
                    c.assertObjectsWithValues(experiments, "code", ["EXP-1", "TEST-EXPERIMENT-3"])
                }

                testSearch(c, fSearch, fCheck)
            })

            QUnit.test("searchExperiments() withIdentifier", function (assert) {
                var c = new common(assert, dtos)

                var fSearch = function (facade: openbis.openbis) {
                    var criteria = new dtos.ExperimentSearchCriteria()
                    criteria.withIdentifier().thatEquals("/TEST/TEST-PROJECT/TEST-EXPERIMENT")
                    return facade.searchExperiments(criteria, c.createExperimentFetchOptions())
                }

                var fCheck = function (facade: openbis.openbis, experiments: openbis.Experiment[]) {
                    c.assertEqual(experiments.length, 1)
                    c.assertEqual(experiments[0].getIdentifier(), "/TEST/TEST-PROJECT/TEST-EXPERIMENT")
                }

                testSearch(c, fSearch, fCheck)
            })

            QUnit.test("searchExperiments() with nested logical operators", function (assert) {
                var c = new common(assert, dtos)

                var fSearch = function (facade: openbis.openbis) {
                    var criteria = new dtos.ExperimentSearchCriteria().withAndOperator()

                    var subCriteria1 = criteria.withSubcriteria().withOrOperator()
                    subCriteria1.withCode().thatStartsWith("TEST")
                    subCriteria1.withCode().thatStartsWith("EXP")

                    var subCriteria2 = criteria.withSubcriteria().withOrOperator()
                    subCriteria2.withCode().thatEndsWith("1")
                    subCriteria2.withCode().thatEndsWith("2")

                    return facade.searchExperiments(criteria, c.createExperimentFetchOptions())
                }

                var fCheck = function (facade: openbis.openbis, experiments: openbis.Experiment[]) {
                    var identifiers = c.extractIdentifiers(experiments)
                    c.assertEqual(identifiers.length, 3)
                    c.assertEqual(
                        identifiers.toString(),
                        "/PLATONIC/SCREENING-EXAMPLES/EXP-1,/PLATONIC/SCREENING-EXAMPLES/EXP-2,/TEST/TEST-PROJECT/TEST-EXPERIMENT-2"
                    )
                }

                testSearch(c, fSearch, fCheck)
            })

            QUnit.test("searchExperimentTypes() with and operator", function (assert) {
                var c = new common(assert, dtos)

                var fSearch = function (facade: openbis.openbis) {
                    var criteria = new dtos.ExperimentTypeSearchCriteria()
                    criteria.withAndOperator()
                    criteria.withCode().thatStartsWith("H")
                    criteria.withCode().thatContains("SEQUENCING")
                    var fetchOptions = new dtos.ExperimentTypeFetchOptions()
                    fetchOptions.withPropertyAssignments().withPropertyType()
                    return facade.searchExperimentTypes(criteria, fetchOptions)
                }

                var fCheck = function (facade: openbis.openbis, experimentTypes: openbis.ExperimentType[]) {
                    c.assertEqual(experimentTypes.length, 1, "Number of experiment types")
                    var type = experimentTypes[0]
                    c.assertEqual(type.getCode(), "HT_SEQUENCING", "Experiment type code")
                    c.assertEqual(type.getFetchOptions().hasPropertyAssignments(), true)
                    c.assertEqual(type.getFetchOptions().withPropertyAssignments().withPropertyType().hasVocabulary(), false)
                    var assignments = type.getPropertyAssignments()
                    c.assertEqual(assignments.length, 1, "Number of property assignments")
                    c.assertEqual(assignments[0].isMandatory(), false, "Mandatory property assignment?")
                    var propertyType = assignments[0].getPropertyType()
                    c.assertEqual(propertyType.getCode(), "EXPERIMENT_DESIGN", "Property type code")
                    c.assertEqual(propertyType.getLabel(), "Experiment Design", "Property type label")
                    c.assertEqual(propertyType.getDescription(), "", "Property type description")
                    c.assertEqual(propertyType.getDataType(), "CONTROLLEDVOCABULARY", "Property data type")
                    c.assertEqual(propertyType.isManagedInternally(), false, "Property type managed internally?")
                }

                testSearch(c, fSearch, fCheck)
            })

            QUnit.test("searchExperimentTypes() with or operator", function (assert) {
                var c = new common(assert, dtos)

                var fSearch = function (facade: openbis.openbis) {
                    var criteria = new dtos.ExperimentTypeSearchCriteria()
                    criteria.withOrOperator()
                    criteria.withCode().thatStartsWith("H")
                    criteria.withCode().thatContains("PLATONIC")
                    var fetchOptions = new dtos.ExperimentTypeFetchOptions()
                    return facade.searchExperimentTypes(criteria, fetchOptions)
                }

                var fCheck = function (facade: openbis.openbis, experimentTypes: openbis.ExperimentType[]) {
                    experimentTypes.sort()
                    c.assertEqual(experimentTypes.toString(), "HCS_PLATONIC,HT_SEQUENCING,MICROSCOPY_PLATONIC", "Experiment types")
                }

                testSearch(c, fSearch, fCheck)
            })

            QUnit.test("searchExperimentTypes() with vocabularies", function (assert) {
                var c = new common(assert, dtos)

                var fSearch = function (facade: openbis.openbis) {
                    var criteria = new dtos.ExperimentTypeSearchCriteria()
                    criteria.withCode().thatStartsWith("HT")
                    var fetchOptions = new dtos.ExperimentTypeFetchOptions()
                    fetchOptions.withPropertyAssignments().withPropertyType().withVocabulary()
                    return facade.searchExperimentTypes(criteria, fetchOptions)
                }

                var fCheck = function (facade: openbis.openbis, experimentTypes: openbis.ExperimentType[]) {
                    c.assertEqual(experimentTypes.length, 1, "Number of experiment types")
                    var type = experimentTypes[0]
                    c.assertEqual(type.getCode(), "HT_SEQUENCING", "Experiment type code")
                    c.assertEqual(type.getFetchOptions().hasPropertyAssignments(), true)
                    c.assertEqual(type.getFetchOptions().withPropertyAssignments().withPropertyType().hasVocabulary(), true)
                    var assignments = type.getPropertyAssignments()
                    c.assertEqual(assignments.length, 1, "Number of property assignments")
                    c.assertEqual(assignments[0].isMandatory(), false, "Mandatory property assignment?")
                    var propertyType = assignments[0].getPropertyType()
                    c.assertEqual(propertyType.getCode(), "EXPERIMENT_DESIGN", "Property type code")
                    c.assertEqual(propertyType.getLabel(), "Experiment Design", "Property type label")
                    c.assertEqual(propertyType.getDescription(), "", "Property type description")
                    c.assertEqual(propertyType.getDataType(), "CONTROLLEDVOCABULARY", "Property data type")
                    c.assertEqual(propertyType.getVocabulary().getCode(), "EXPERIMENT_DESIGN", "Vocabulary code")
                    c.assertEqual(propertyType.isManagedInternally(), false, "Property type managed internally?")
                }

                testSearch(c, fSearch, fCheck)
            })

            QUnit.test("searchSamples()", function (assert) {
                var c = new common(assert, dtos)

                var fSearch = function (facade: openbis.openbis) {
                    var criteria = new dtos.SampleSearchCriteria()
                    criteria.withPermId().thatEquals("20130415095748527-404")
                    return facade.searchSamples(criteria, c.createSampleFetchOptions())
                }

                var fCheck = function (facade: openbis.openbis, samples: openbis.Sample[]) {
                    c.assertEqual(samples.length, 1)
                    var sample = samples[0]
                    c.assertEqual(sample.getCode(), "TEST-SAMPLE-2-PARENT", "Sample code")
                    c.assertEqual(sample.getType().getCode(), "UNKNOWN", "Type code")
                    c.assertEqual(sample.getExperiment().getCode(), "TEST-EXPERIMENT-2", "Experiment code")
                    c.assertEqual(sample.getExperiment().getProject().getCode(), "TEST-PROJECT", "Project code")
                    c.assertEqual(sample.getSpace().getCode(), "TEST", "Space code")
                    c.assertNotEqual(sample.getChildren(), null, "Children expected")
                    if (sample.getChildren() !== null) {
                        console.log("Children %s", sample.getChildren())
                        var child = sample.getChildren()[0]
                        c.assertEqual(sample.getChildren().length, 1, "Number of children")
                        c.assertEqual(child.getCode(), "TEST-SAMPLE-2", "Child sample code")
                        c.assertEqual(child.getType().getCode(), "UNKNOWN", "Child type code")
                        c.assertEqual(child.getExperiment().getCode(), "TEST-EXPERIMENT-2", "Child experiment code")
                        c.assertNotEqual(child.getChildren(), null, "Grand children expected")
                        if (child.getChildren() !== null) {
                            c.assertEqual(child.getChildren().length, 2, "Number of grand children")
                        }
                    }
                }

                testSearch(c, fSearch, fCheck)
            })

            QUnit.test("searchSamples() with codes", function (assert) {
                var c = new common(assert, dtos)

                var fSearch = function (facade: openbis.openbis) {
                    var criteria = new dtos.SampleSearchCriteria()
                    criteria.withCodes().thatIn(["TEST-SAMPLE-2-PARENT"])
                    return facade.searchSamples(criteria, c.createSampleFetchOptions())
                }

                var fCheck = function (facade: openbis.openbis, samples: openbis.Sample[]) {
                    c.assertEqual(samples.length, 1)
                    var sample = samples[0]
                    c.assertEqual(sample.getCode(), "TEST-SAMPLE-2-PARENT", "Sample code")
                }

                testSearch(c, fSearch, fCheck)
            })

            QUnit.test("searchSamples() with paging and sorting", function (assert) {
                var c = new common(assert, dtos)

                var criteria = new dtos.SampleSearchCriteria()
                criteria.withOrOperator()
                criteria.withCode().thatEquals("TEST-SAMPLE-1")
                criteria.withCode().thatEquals("TEST-SAMPLE-2")

                var fo = c.createSampleFetchOptions()

                testSearchWithPagingAndSortingByAll(
                    c,
                    function (facade: openbis.openbis) {
                        return facade.searchSamples(criteria, fo)
                    },
                    fo
                )
            })

            QUnit.test("searchSamples() with paging and sorting by code", function (assert) {
                var c = new common(assert, dtos)

                var criteria = new dtos.SampleSearchCriteria()
                criteria.withOrOperator()
                criteria.withCode().thatContains("TEST-SAMPLE-1")

                var fo = c.createSampleFetchOptions()

                testSearchWithPagingAndSorting(
                    c,
                    function (facade: openbis.openbis) {
                        return facade.searchSamples(criteria, fo)
                    },
                    fo,
                    "code",
                    null,
                    true,
                    "TEST-SAMPLE-1"
                )
            })

            QUnit.test("searchSamples() withoutSpace", function (assert) {
                var c = new common(assert, dtos)
                var code = c.generateId("SAMPLE")
                var waitUntilIndexed = function (facade: openbis.openbis, samplePermId, timeout, action) {
                    if (timeout < 0) {
                        c.fail("Sample " + samplePermId + " after " + timeout + " msec.")
                    }
                    setTimeout(function () {
                        c.ok("Wait until " + samplePermId + " indexed. " + timeout)
                        var criteria = new dtos.SampleSearchCriteria()
                        criteria.withPermId().thatEquals(samplePermId)
                        facade.searchSamples(criteria, c.createSampleFetchOptions()).then(function (result) {
                            if (result.getTotalCount() == 0) {
                                waitUntilIndexed(facade, samplePermId, timeout - 1000, action)
                            } else {
                                action()
                            }
                        })
                    }, 1000)
                }

                c.start()
                c.login(facade)
                    .then(function () {
                        c.ok("Login")
                        var creation = new dtos.SampleCreation()
                        creation.setTypeId(new dtos.EntityTypePermId("UNKNOWN"))
                        creation.setCode(code)
                        facade.createSamples([creation]).then(function (permIds) {
                            var permId = permIds[0]
                            c.ok("Shared sample created: " + permId)
                            waitUntilIndexed(facade, permId.getPermId(), 10000, function () {
                                var criteria = new dtos.SampleSearchCriteria()
                                criteria.withoutSpace()
                                facade.searchSamples(criteria, c.createSampleFetchOptions()).then(function (results) {
                                    c.ok("Got results")
                                    var samples = results.getObjects()
                                    c.assertObjectsWithValues(samples, "identifier", ["/" + code])
                                    c.deleteSample(facade, permId).then(function () {
                                        c.ok("Sample " + permId + " trashed")
                                        c.finish()
                                    })
                                })
                            })
                        })
                    })
                    .fail(function (error) {
                        c.fail(error.message)
                        c.finish()
                    })
            })

            QUnit.test("searchSamples() withoutExperiment", function (assert) {
                var c = new common(assert, dtos)

                var fSearch = function (facade: openbis.openbis) {
                    var criteria = new dtos.SampleSearchCriteria()
                    criteria.withCode().thatStartsWith("TEST-SAMPLE")
                    criteria.withoutExperiment()
                    return facade.searchSamples(criteria, c.createSampleFetchOptions())
                }

                var fCheck = function (facade: openbis.openbis, samples: openbis.Sample[]) {
                    c.assertObjectsWithValues(samples, "code", ["TEST-SAMPLE-1-CONTAINED-1", "TEST-SAMPLE-1-CONTAINED-2", "TEST-SAMPLE-1"])
                }

                testSearch(c, fSearch, fCheck)
            })

            QUnit.test("searchSamples() withExperiment", function (assert) {
                var c = new common(assert, dtos)

                var fSearch = function (facade: openbis.openbis) {
                    var criteria = new dtos.SampleSearchCriteria()
                    criteria.withCode().thatStartsWith("TEST-SAMPLE")
                    criteria.withExperiment()
                    return facade.searchSamples(criteria, c.createSampleFetchOptions())
                }

                var fCheck = function (facade: openbis.openbis, samples: openbis.Sample[]) {
                    c.assertObjectsWithValues(samples, "code", [
                        "TEST-SAMPLE-2-CHILD-2",
                        "TEST-SAMPLE-2-CHILD-1",
                        "TEST-SAMPLE-2-PARENT",
                        "TEST-SAMPLE-2",
                    ])
                }

                testSearch(c, fSearch, fCheck)
            })

            QUnit.test("searchSamples() withoutContainer", function (assert) {
                var c = new common(assert, dtos)

                var fSearch = function (facade: openbis.openbis) {
                    var criteria = new dtos.SampleSearchCriteria()
                    criteria.withCode().thatStartsWith("TEST-SAMPLE")
                    criteria.withoutContainer()
                    return facade.searchSamples(criteria, c.createSampleFetchOptions())
                }

                var fCheck = function (facade: openbis.openbis, samples: openbis.Sample[]) {
                    c.assertObjectsWithValues(samples, "code", [
                        "TEST-SAMPLE-2",
                        "TEST-SAMPLE-1",
                        "TEST-SAMPLE-2-PARENT",
                        "TEST-SAMPLE-2-CHILD-1",
                        "TEST-SAMPLE-2-CHILD-2",
                    ])
                }

                testSearch(c, fSearch, fCheck)
            })

            QUnit.test("searchSamples() withoutContainer negated", function (assert) {
                var c = new common(assert, dtos)

                var fSearch = function (facade: openbis.openbis) {
                    var criteria = new dtos.SampleSearchCriteria().withAndOperator()
                    criteria.withCode().thatStartsWith("TEST-SAMPLE")
                    criteria.withoutContainer()
                    criteria.withSubcriteria().negate().withCode().thatEndsWith("-1")
                    return facade.searchSamples(criteria, c.createSampleFetchOptions())
                }

                var fCheck = function (facade: openbis.openbis, samples: openbis.Sample[]) {
                    c.assertObjectsWithValues(samples, "code", ["TEST-SAMPLE-2", "TEST-SAMPLE-2-PARENT", "TEST-SAMPLE-2-CHILD-2"])
                }

                testSearch(c, fSearch, fCheck)
            })

            QUnit.test("searchSamples() withContainer", function (assert) {
                var c = new common(assert, dtos)

                var fSearch = function (facade: openbis.openbis) {
                    var criteria = new dtos.SampleSearchCriteria()
                    criteria.withCode().thatStartsWith("TEST-SAMPLE")
                    criteria.withContainer()
                    return facade.searchSamples(criteria, c.createSampleFetchOptions())
                }

                var fCheck = function (facade: openbis.openbis, samples: openbis.Sample[]) {
                    c.assertObjectsWithValues(samples, "code", ["TEST-SAMPLE-1-CONTAINED-1", "TEST-SAMPLE-1-CONTAINED-2"])
                }

                testSearch(c, fSearch, fCheck)
            })

            QUnit.test("searchSamples() withChildren", function (assert) {
                var c = new common(assert, dtos)

                var fSearch = function (facade: openbis.openbis) {
                    var criteria = new dtos.SampleSearchCriteria()
                    criteria.withChildren().withCode().thatContains("CHILD")
                    return facade.searchSamples(criteria, c.createSampleFetchOptions())
                }

                var fCheck = function (facade: openbis.openbis, samples: openbis.Sample[]) {
                    c.assertObjectsWithValues(samples, "code", ["TEST-SAMPLE-2"])
                }

                testSearch(c, fSearch, fCheck)
            })

            QUnit.test("searchSamples() withParents", function (assert) {
                var c = new common(assert, dtos)

                var fSearch = function (facade: openbis.openbis) {
                    var criteria = new dtos.SampleSearchCriteria()
                    criteria.withParents().withCode().thatContains("ARE")
                    return facade.searchSamples(criteria, c.createSampleFetchOptions())
                }

                var fCheck = function (facade: openbis.openbis, samples: openbis.Sample[]) {
                    c.assertObjectsWithValues(samples, "code", ["TEST-SAMPLE-2"])
                }

                testSearch(c, fSearch, fCheck)
            })

            QUnit.test("searchSamples() with sorting by identifier", function (assert) {
                var c = new common(assert, dtos)

                var criteria = new dtos.SampleSearchCriteria()
                criteria.withOrOperator()
                criteria.withId().thatEquals(new dtos.SampleIdentifier("/PLATONIC/SCREENING-EXAMPLES/PLATE-1"))
                criteria.withId().thatEquals(new dtos.SampleIdentifier("/TEST/TEST-SAMPLE-1"))

                var fo = c.createSampleFetchOptions()

                testSearchWithPagingAndSorting(
                    c,
                    function (facade: openbis.openbis) {
                        return facade.searchSamples(criteria, fo)
                    },
                    fo,
                    "identifier"
                )
            })

            QUnit.test("searchSamples() with sorting by type", function (assert) {
                var c = new common(assert, dtos)

                var criteria = new dtos.SampleSearchCriteria()
                criteria.withOrOperator()
                criteria.withId().thatEquals(new dtos.SampleIdentifier("/PLATONIC/SCREENING-EXAMPLES/PLATE-1"))
                criteria.withId().thatEquals(new dtos.SampleIdentifier("/TEST/TEST-SAMPLE-1"))

                var fo = c.createSampleFetchOptions()

                testSearchWithPagingAndSorting(
                    c,
                    function (facade: openbis.openbis) {
                        return facade.searchSamples(criteria, fo)
                    },
                    fo,
                    "type"
                )
            })

            QUnit.test("searchSamples() withRegistrator withUserId", function (assert) {
                var c = new common(assert, dtos)

                var fSearch = function (facade: openbis.openbis) {
                    var criteria = new dtos.SampleSearchCriteria()
                    criteria.withRegistrator().withUserId().thatEquals("etlserver")
                    return facade.searchSamples(criteria, c.createSampleFetchOptions())
                }

                var fCheck = function (facade: openbis.openbis, samples: openbis.Sample[]) {
                    c.assertObjectsWithValues(samples, "code", ["TEST-SAMPLE-1", "TEST-SAMPLE-2"])
                }

                testSearch(c, fSearch, fCheck)
            })

            QUnit.test("searchSamples() withModifier withUserId", function (assert) {
                var c = new common(assert, dtos)

                var fSearch = function (facade: openbis.openbis) {
                    var criteria = new dtos.SampleSearchCriteria()
                    criteria.withModifier().withUserId().thatEquals("etlserver")
                    return facade.searchSamples(criteria, c.createSampleFetchOptions())
                }

                var fCheck = function (facade: openbis.openbis, samples: openbis.Sample[]) {
                    c.assertObjectsWithValues(samples, "code", ["PLATE-1A", "PLATE-2", "SERIES-1"])
                }

                testSearch(c, fSearch, fCheck)
            })

            QUnit.test("searchSamples() withIdentifier", function (assert) {
                var c = new common(assert, dtos)

                var fSearch = function (facade: openbis.openbis) {
                    var criteria = new dtos.SampleSearchCriteria()
                    criteria.withIdentifier().thatEquals("/PLATONIC/SCREENING-EXAMPLES/PLATE-1")
                    return facade.searchSamples(criteria, c.createSampleFetchOptions())
                }

                var fCheck = function (facade: openbis.openbis, samples: openbis.Sample[]) {
                    c.assertEqual(samples.length, 1)
                    c.assertEqual(samples[0].getIdentifier(), "/PLATONIC/SCREENING-EXAMPLES/PLATE-1")
                }

                testSearch(c, fSearch, fCheck)
            })

            QUnit.test("searchSamples() with code that is less than", function (assert) {
                var c = new common(assert, dtos)

                var fSearch = function (facade: openbis.openbis) {
                    var criteria = new dtos.SampleSearchCriteria()
                    criteria.withCode().thatIsLessThan("A1")
                    return facade.searchSamples(criteria, c.createSampleFetchOptions())
                }

                var fCheck = function (facade: openbis.openbis, samples: openbis.Sample[]) {
                    var identifiers = c.extractIdentifiers(samples)
                    c.assertEqual(identifiers.length, 0)
                }

                testSearch(c, fSearch, fCheck)
            })

            QUnit.test("searchSamples() with code that is less than or equal to", function (assert) {
                var c = new common(assert, dtos)

                var fSearch = function (facade: openbis.openbis) {
                    var criteria = new dtos.SampleSearchCriteria()
                    criteria.withCode().thatIsLessThanOrEqualTo("PLATE-2:A1")
                    return facade.searchSamples(criteria, c.createSampleFetchOptions())
                }

                var fCheck = function (facade: openbis.openbis, samples: openbis.Sample[]) {
                    var identifiers = c.extractIdentifiers(samples)
                    var identifiersString = identifiers.toString()
                    c.assertTrue(identifiersString.indexOf("/PLATONIC/SCREENING-EXAMPLES/PLATE-1:A1") >= 0)
                    c.assertTrue(identifiersString.indexOf("/PLATONIC/SCREENING-EXAMPLES/PLATE-2:A1") >= 0)
                    c.assertTrue(identifiersString.indexOf("/TEST/TEST-PROJECT/PLATE-1A:A1") >= 0)
                    c.assertTrue(identifiersString.indexOf("/TEST/TEST-PROJECT/PLATE-2:A1") >= 0)
                }

                testSearch(c, fSearch, fCheck)
            })

            QUnit.test("searchSamples() with code that is greater than", function (assert) {
                var c = new common(assert, dtos)

                var fSearch = function (facade: openbis.openbis) {
                    var criteria = new dtos.SampleSearchCriteria()
                    criteria.withCode().thatIsGreaterThan("TEST-SAMPLE-2-CHILD-2")
                    criteria.withCode().thatIsLessThan("V")
                    return facade.searchSamples(criteria, c.createSampleFetchOptions())
                }

                var fCheck = function (facade: openbis.openbis, samples: openbis.Sample[]) {
                    var identifiers = c.extractIdentifiers(samples)
                    c.assertEqual(identifiers.length, 2)
                    c.assertEqual(identifiers.toString(), "/ELN_SETTINGS/STORAGES/TEST_STORAGE,/TEST/TEST-PROJECT/TEST-SAMPLE-2-PARENT")
                }

                testSearch(c, fSearch, fCheck)
            })

            QUnit.test("searchSamples() with code that is greater than or equal to", function (assert) {
                var c = new common(assert, dtos)

                var fSearch = function (facade: openbis.openbis) {
                    var criteria = new dtos.SampleSearchCriteria()
                    criteria.withCode().thatIsGreaterThanOrEqualTo("TEST-SAMPLE-2-CHILD-2")
                    criteria.withCode().thatIsLessThan("V")
                    return facade.searchSamples(criteria, c.createSampleFetchOptions())
                }

                var fCheck = function (facade: openbis.openbis, samples: openbis.Sample[]) {
                    var identifiers = c.extractIdentifiers(samples)
                    c.assertEqual(identifiers.length, 3)
                    c.assertEqual(
                        identifiers.toString(),
                        "/ELN_SETTINGS/STORAGES/TEST_STORAGE,/TEST/TEST-PROJECT/TEST-SAMPLE-2-CHILD-2,/TEST/TEST-PROJECT/TEST-SAMPLE-2-PARENT"
                    )
                }

                testSearch(c, fSearch, fCheck)
            })

            QUnit.test("searchSamples() with nested logical operators", function (assert) {
                var c = new common(assert, dtos)

                var fSearch = function (facade: openbis.openbis) {
                    var criteria = new dtos.SampleSearchCriteria().withAndOperator()

                    var subCriteria1 = criteria.withSubcriteria().withOrOperator()
                    subCriteria1.withIdentifier().thatStartsWith("/PLATONIC/SCREENING-EXAMPLES/PLATE-1")
                    subCriteria1.withIdentifier().thatStartsWith("/TEST/TEST-PROJECT/PLATE-2")

                    var subCriteria2 = criteria.withSubcriteria().withOrOperator()
                    subCriteria2.withIdentifier().thatEndsWith(":A1")
                    subCriteria2.withIdentifier().thatEndsWith(":A2")

                    return facade.searchSamples(criteria, c.createSampleFetchOptions())
                }

                var fCheck = function (facade: openbis.openbis, samples: openbis.Sample[]) {
                    var identifiers = c.extractIdentifiers(samples)
                    c.assertEqual(identifiers.length, 4)
                    c.assertEqual(
                        identifiers.toString(),
                        "/PLATONIC/SCREENING-EXAMPLES/PLATE-1:A1,/PLATONIC/SCREENING-EXAMPLES/PLATE-1:A2,/TEST/TEST-PROJECT/PLATE-2:A1,/TEST/TEST-PROJECT/PLATE-2:A2"
                    )
                }

                testSearch(c, fSearch, fCheck)
            })

            QUnit.test("searchSampleTypes()", function (assert) {
                var c = new common(assert, dtos)

                var fSearch = function (facade: openbis.openbis) {
                    var criteria = new dtos.SampleTypeSearchCriteria()
                    criteria.withCode().thatStartsWith("MA")

                    var fetchOptions = new dtos.SampleTypeFetchOptions()

                    var assignmentFetchOptions = fetchOptions.withPropertyAssignments()
                    assignmentFetchOptions.withRegistrator()
                    assignmentFetchOptions.sortBy().label().desc()

                    var propertyTypeFetchOptions = assignmentFetchOptions.withPropertyType()
                    propertyTypeFetchOptions.withVocabulary()
                    propertyTypeFetchOptions.withRegistrator()

                    return facade.searchSampleTypes(criteria, fetchOptions)
                }

                var fCheck = function (facade: openbis.openbis, sampleTypes: openbis.SampleType[]) {
                    c.assertEqual(sampleTypes.length, 1, "Number of sample types")
                    var type = sampleTypes[0]
                    c.assertEqual(type.getCode(), "MASTER_SAMPLE", "Sample type code")
                    c.assertEqual(type.getFetchOptions().hasPropertyAssignments(), true)
                    var assignments = type.getPropertyAssignments()
                    c.assertEqual(assignments.length, 8, "Number of property assignments")

                    var assignment = assignments[0]
                    c.assertEqual(assignment.isMandatory(), true, "Mandatory property assignment?")
                    c.assertEqual(assignment.getOrdinal(), 22, "Ordinal")
                    c.assertEqual(assignment.getSection(), null, "Section")
                    c.assertEqual(assignment.isShowInEditView(), true, "Show in edit view")
                    c.assertEqual(assignment.isShowRawValueInForms(), false, "Show raw value in forms")
                    c.assertDate(assignment.getRegistrationDate(), "Registration date", 2013, 4, 12, 8, 4)
                    c.assertEqual(assignment.getRegistrator().getUserId(), "system", "Registrator user id")

                    var propertyType = assignment.getPropertyType()
                    c.assertEqual(propertyType.getCode(), "SAMPLE_KIND", "Property type code")
                    c.assertEqual(propertyType.getLabel(), "Sample Kind", "Property type label")
                    c.assertEqual(propertyType.getDescription(), "", "Property type description")
                    c.assertEqual(propertyType.getDataType(), "CONTROLLEDVOCABULARY", "Property data type")
                    c.assertEqual(propertyType.isManagedInternally(), false, "Property type managed internally?")
                    c.assertEqual(propertyType.isManagedInternally(), false, "Property type managed internally?")
                    c.assertEqual(propertyType.getVocabulary().getCode(), "SAMPLE_TYPE", "Property type vocabulary code")
                    c.assertEqual(propertyType.getSchema(), null, "Property type schema")
                    c.assertEqual(propertyType.getTransformation(), null, "Property type transformation")
                    c.assertEqual(propertyType.getRegistrator().getUserId(), "system", "Registrator user id")
                    c.assertDate(propertyType.getRegistrationDate(), "Registration date", 2013, 4, 12, 8, 4)

                    c.assertEqual(assignments[1].getPropertyType().getCode(), "NCBI_ORGANISM_TAXONOMY", "Second property type code")
                }

                testSearch(c, fSearch, fCheck)
            })

            QUnit.test("searchSampleTypes() with and operator", function (assert) {
                var c = new common(assert, dtos)

                var fSearch = function (facade: openbis.openbis) {
                    var criteria = new dtos.SampleTypeSearchCriteria()
                    criteria.withAndOperator()
                    criteria.withCode().thatStartsWith("ILL")
                    criteria.withCode().thatEndsWith("LL")
                    var fetchOptions = new dtos.SampleTypeFetchOptions()
                    return facade.searchSampleTypes(criteria, fetchOptions)
                }

                var fCheck = function (facade: openbis.openbis, sampleTypes: openbis.SampleType[]) {
                    c.assertEqual(sampleTypes.toString(), "ILLUMINA_FLOW_CELL", "Sample types")
                }

                testSearch(c, fSearch, fCheck)
            })

            QUnit.test("searchSampleTypes() with or operator", function (assert) {
                var c = new common(assert, dtos)

                var fSearch = function (facade: openbis.openbis) {
                    var criteria = new dtos.SampleTypeSearchCriteria()
                    criteria.withOrOperator()
                    criteria.withCode().thatStartsWith("ILL")
                    criteria.withCode().thatEndsWith("LL")
                    var fetchOptions = new dtos.SampleTypeFetchOptions()
                    return facade.searchSampleTypes(criteria, fetchOptions)
                }

                var fCheck = function (facade: openbis.openbis, sampleTypes: openbis.SampleType[]) {
                    sampleTypes.sort()
                    c.assertEqual(sampleTypes.toString(), "CONTROL_WELL,ILLUMINA_FLOW_CELL,ILLUMINA_FLOW_LANE,SIRNA_WELL", "Sample types")
                }

                testSearch(c, fSearch, fCheck)
            })

            QUnit.test("searchSampleTypes() withSemanticAnnotations", function (assert) {
                var c = new common(assert, dtos)

                var fSearch = function (facade: openbis.openbis) {
                    var criteria = new dtos.SampleTypeSearchCriteria()
                    criteria.withSemanticAnnotations().withPermId().thatEquals("ST_SIRNA_WELL")
                    var fetchOptions = new dtos.SampleTypeFetchOptions()
                    return facade.searchSampleTypes(criteria, fetchOptions)
                }

                var fCheck = function (facade: openbis.openbis, sampleTypes: openbis.SampleType[]) {
                    c.assertEqual(sampleTypes.length, 1, "Number of sample types")
                    c.assertEqual(sampleTypes[0].getCode(), "SIRNA_WELL", "Sample type code")
                }

                testSearch(c, fSearch, fCheck)
            })

            QUnit.test("searchSampleTypes() withPropertyAssignments withSemanticAnnotations", function (assert) {
                var c = new common(assert, dtos)

                var fSearch = function (facade: openbis.openbis) {
                    var criteria = new dtos.SampleTypeSearchCriteria()
                    criteria.withPropertyAssignments().withSemanticAnnotations().withPermId().thatEquals("ST_ILLUMINA_FLOW_CELL_PT_CREATED_ON_CS")
                    var fetchOptions = new dtos.SampleTypeFetchOptions()
                    return facade.searchSampleTypes(criteria, fetchOptions)
                }

                var fCheck = function (facade: openbis.openbis, sampleTypes: openbis.SampleType[]) {
                    c.assertEqual(sampleTypes.length, 1, "Number of sample types")
                    c.assertEqual(sampleTypes[0].getCode(), "ILLUMINA_FLOW_CELL", "Sample type code")
                }

                testSearch(c, fSearch, fCheck)
            })

            QUnit.test("searchSampleTypes() withPropertyAssignments withPropertyType withSemanticAnnotations", function (assert) {
                var c = new common(assert, dtos)

                var fSearch = function (facade: openbis.openbis) {
                    var criteria = new dtos.SampleTypeSearchCriteria()
                    criteria.withPropertyAssignments().withPropertyType().withSemanticAnnotations().withPermId().thatEquals("PT_AGILENT_KIT")
                    var fetchOptions = new dtos.SampleTypeFetchOptions()
                    return facade.searchSampleTypes(criteria, fetchOptions)
                }

                var fCheck = function (facade: openbis.openbis, sampleTypes: openbis.SampleType[]) {
                    c.assertEqual(sampleTypes.length, 2, "Number of sample types")
                    c.assertObjectsWithValues(sampleTypes, "code", ["LIBRARY", "LIBRARY_POOL"])
                }

                testSearch(c, fSearch, fCheck)
            })

            QUnit.test("searchDataSets()", function (assert) {
                var c = new common(assert, dtos)

                var fSearch = function (facade: openbis.openbis) {
                    var criteria = new dtos.DataSetSearchCriteria()
                    criteria.withPermId().thatEquals("20130415093804724-403")
                    return facade.searchDataSets(criteria, c.createDataSetFetchOptions())
                }

                var fCheck = function (facade: openbis.openbis, dataSets: openbis.DataSet[]) {
                    c.assertEqual(dataSets.length, 1)
                    var dataSet = dataSets[0]
                    c.assertEqual(dataSet.getCode(), "20130415093804724-403", "Code")
                    c.assertEqual(dataSet.getType().getCode(), "UNKNOWN", "Type code")
                    c.assertEqual(dataSet.getExperiment().getCode(), "TEST-EXPERIMENT-2", "Experiment code")
                    c.assertEqual(dataSet.getSample().getCode(), "TEST-SAMPLE-2", "Sample code")
                    c.assertEqual(dataSet.getProperties()["DESCRIPTION"], "403 description", "Property DESCRIPTION")
                    c.assertEqual(dataSet.isPostRegistered(), true, "post registered")

                    var physicalData = dataSet.getPhysicalData()
                    c.assertEqual(physicalData.getShareId(), "1", "Share id")
                    c.assertEqual(physicalData.getLocation(), "1FD3FF61-1576-4908-AE3D-296E60B4CE06/06/e5/ad/20130415093804724-403", "Location")
                    c.assertEqual(physicalData.getStatus(), "AVAILABLE", "Status")
                    c.assertEqual(physicalData.getStorageFormat().getCode(), "PROPRIETARY", "Storage format")
                    c.assertEqual(physicalData.getLocatorType().getCode(), "RELATIVE_LOCATION", "Locator type")

                    c.assertObjectsWithValues(dataSet.getParents(), "code", ["20130415100158230-407"])
                    c.assertObjectsWithValues(dataSet.getChildren(), "code", ["20130415100238098-408", "20130415100308111-409"])
                }

                testSearch(c, fSearch, fCheck)
            })

            QUnit.test("searchDataSets() with codes", function (assert) {
                var c = new common(assert, dtos)

                var fSearch = function (facade: openbis.openbis) {
                    var criteria = new dtos.DataSetSearchCriteria()
                    criteria.withCodes().thatIn(["20130412142543232-197"])
                    return facade.searchDataSets(criteria, c.createDataSetFetchOptions())
                }

                var fCheck = function (facade: openbis.openbis, dataSets: openbis.DataSet[]) {
                    c.assertEqual(dataSets.length, 1)
                    var dataSet = dataSets[0]
                    c.assertEqual(dataSet.getCode(), "20130412142543232-197", "Code")
                }

                testSearch(c, fSearch, fCheck)
            })

            QUnit.test("searchDataSets() with paging and sorting", function (assert) {
                var c = new common(assert, dtos)

                var criteria = new dtos.DataSetSearchCriteria()
                criteria.withOrOperator()
                criteria.withCode().thatEquals("20130412142205843-196")
                criteria.withCode().thatEquals("20130412142543232-197")

                var fo = c.createDataSetFetchOptions()

                testSearchWithPagingAndSortingByAll(
                    c,
                    function (facade: openbis.openbis) {
                        return facade.searchDataSets(criteria, fo)
                    },
                    fo
                )
            })

            QUnit.test("searchDataSets() with sorting by property", function (assert) {
                var c = new common(assert, dtos)

                var criteria = new dtos.DataSetSearchCriteria()
                criteria.withOrOperator()
                criteria.withPermId().thatEquals("20130412142543232-197")
                criteria.withPermId().thatEquals("20130412142205843-196")
                criteria.withPermId().thatEquals("20130412142942295-198")

                var fo = c.createDataSetFetchOptions()

                testSearchWithPagingAndStringSorting(
                    c,
                    function (facade: openbis.openbis) {
                        return facade.searchDataSets(criteria, fo)
                    },
                    fo,
                    "property",
                    "RESOLUTION"
                )
            })

            QUnit.test("searchDataSets() with sorting by type", function (assert) {
                var c = new common(assert, dtos)

                var criteria = new dtos.DataSetSearchCriteria()
                criteria.withOrOperator()
                criteria.withPermId().thatEquals("20130412142543232-197")
                criteria.withPermId().thatEquals("20130412143121081-200")

                var fo = c.createDataSetFetchOptions()

                testSearchWithPagingAndSorting(
                    c,
                    function (facade: openbis.openbis) {
                        return facade.searchDataSets(criteria, fo)
                    },
                    fo,
                    "type"
                )
            })

            QUnit.test("searchDataSets() withoutSample", function (assert) {
                var c = new common(assert, dtos)

                var fSearch = function (facade: openbis.openbis) {
                    var criteria = new dtos.DataSetSearchCriteria()
                    criteria.withCode().thatContains("-40")
                    criteria.withoutSample()
                    return facade.searchDataSets(criteria, c.createDataSetFetchOptions())
                }

                var fCheck = function (facade: openbis.openbis, dataSets: openbis.DataSet[]) {
                    c.assertObjectsWithValues(dataSets, "code", ["20130415100158230-407", "20130415100308111-409"])
                }

                testSearch(c, fSearch, fCheck)
            })

            QUnit.test("searchDataSets() withSample", function (assert) {
                var c = new common(assert, dtos)

                var fSearch = function (facade: openbis.openbis) {
                    var criteria = new dtos.DataSetSearchCriteria()
                    criteria.withCode().thatContains("-40")
                    criteria.withSample()
                    return facade.searchDataSets(criteria, c.createDataSetFetchOptions())
                }

                var fCheck = function (facade: openbis.openbis, dataSets: openbis.DataSet[]) {
                    c.assertObjectsWithValues(dataSets, "code", ["20130415093804724-403", "20130415100238098-408"])
                }

                testSearch(c, fSearch, fCheck)
            })

            QUnit.test("searchDataSets() withSample negated", function (assert) {
                var c = new common(assert, dtos)

                var fSearch = function (facade: openbis.openbis) {
                    var criteria = new dtos.DataSetSearchCriteria().withAndOperator()
                    criteria.withCode().thatContains("-40")
                    criteria.withSample()
                    criteria.withSubcriteria().negate().withCode().thatEndsWith("8")
                    return facade.searchDataSets(criteria, c.createDataSetFetchOptions())
                }

                var fCheck = function (facade: openbis.openbis, dataSets: openbis.DataSet[]) {
                    c.assertObjectsWithValues(dataSets, "code", ["20130415093804724-403"])
                }

                testSearch(c, fSearch, fCheck)
            })

            QUnit.test("searchDataSets() withSampleWithWildcardsEnabled", function (assert) {
                var c = new common(assert, dtos)

                var fSearch2 = function (facade: openbis.openbis) {
                    var criteria = new dtos.DataSetSearchCriteria()
                    criteria.withCode().withWildcards().thatEquals("*-40?")
                    criteria.withSample()
                    return facade.searchDataSets(criteria, c.createDataSetFetchOptions())
                }

                var fCheck2 = function (facade: openbis.openbis, dataSets: openbis.DataSet[]) {
                    c.assertObjectsWithValues(dataSets, "code", ["20130415093804724-403", "20130415100238098-408"])
                }

                testSearch(c, fSearch2, fCheck2)
            })

            QUnit.test("searchDataSets() withSampleWithWildcardsDisabled", function (assert) {
                var c = new common(assert, dtos)

                var fSearch1 = function (facade: openbis.openbis) {
                    var criteria = new dtos.DataSetSearchCriteria()
                    criteria.withCode().withoutWildcards().thatEquals("*-40?")
                    criteria.withSample()
                    return facade.searchDataSets(criteria, c.createDataSetFetchOptions())
                }

                var fCheck1 = function (facade: openbis.openbis, dataSets: openbis.DataSet[]) {
                    c.assertObjectsWithValues(dataSets, "code", [])
                }

                testSearch(c, fSearch1, fCheck1)
            })

            QUnit.test("searchDataSets() withoutExperiment", function (assert) {
                var c = new common(assert, dtos)

                var fSearch = function (facade: openbis.openbis) {
                    var criteria = new dtos.DataSetSearchCriteria()
                    criteria.withCode().thatContains("-40")
                    criteria.withoutExperiment()
                    return facade.searchDataSets(criteria, c.createDataSetFetchOptions())
                }

                var fCheck = function (facade: openbis.openbis, dataSets: openbis.DataSet[]) {
                    c.assertObjectsWithValues(dataSets, "code", ["20130415100238098-408"])
                }

                testSearch(c, fSearch, fCheck)
            })

            QUnit.test("searchDataSets() withExperiment", function (assert) {
                var c = new common(assert, dtos)

                var fSearch = function (facade: openbis.openbis) {
                    var criteria = new dtos.DataSetSearchCriteria()
                    criteria.withCode().thatContains("-40")
                    criteria.withExperiment()
                    return facade.searchDataSets(criteria, c.createDataSetFetchOptions())
                }

                var fCheck = function (facade: openbis.openbis, dataSets: openbis.DataSet[]) {
                    c.assertObjectsWithValues(dataSets, "code", ["20130415093804724-403", "20130415100158230-407", "20130415100308111-409"])
                }

                testSearch(c, fSearch, fCheck)
            })

            QUnit.test("searchDataSets() withChildren", function (assert) {
                var c = new common(assert, dtos)

                var fSearch = function (facade: openbis.openbis) {
                    var criteria = new dtos.DataSetSearchCriteria()
                    criteria.withChildren().withCode().thatEquals("20130415100238098-408")
                    return facade.searchDataSets(criteria, c.createDataSetFetchOptions())
                }

                var fCheck = function (facade: openbis.openbis, dataSets: openbis.DataSet[]) {
                    c.assertObjectsWithValues(dataSets, "code", ["20130415093804724-403"])
                }

                testSearch(c, fSearch, fCheck)
            })

            QUnit.test("searchDataSets() withParents", function (assert) {
                var c = new common(assert, dtos)

                var fSearch = function (facade: openbis.openbis) {
                    var criteria = new dtos.DataSetSearchCriteria()
                    criteria.withParents().withCode().thatEquals("20130415100158230-407")
                    return facade.searchDataSets(criteria, c.createDataSetFetchOptions())
                }

                var fCheck = function (facade: openbis.openbis, dataSets: openbis.DataSet[]) {
                    c.assertObjectsWithValues(dataSets, "code", ["20130415093804724-403"])
                }

                testSearch(c, fSearch, fCheck)
            })

            QUnit.test("searchDataSets() withContainer", function (assert) {
                var c = new common(assert, dtos)

                var fSearch = function (facade: openbis.openbis) {
                    var criteria = new dtos.DataSetSearchCriteria()
                    criteria.withContainer().withCode().thatEquals("20130412153119864-385")
                    return facade.searchDataSets(criteria, c.createDataSetFetchOptions())
                }

                var fCheck = function (facade: openbis.openbis, dataSets: openbis.DataSet[]) {
                    c.assertObjectsWithValues(dataSets, "code", ["20130412153118625-384"])
                }

                testSearch(c, fSearch, fCheck)
            })

            QUnit.test("searchDataSets() withPhysicalData", function (assert) {
                var c = new common(assert, dtos)

                var fSearch = function (facade: openbis.openbis) {
                    var criteria = new dtos.DataSetSearchCriteria()
                    var pdCriteria = criteria.withPhysicalData()
                    pdCriteria.withLocation().thatEquals('"1FD3FF61-1576-4908-AE3D-296E60B4CE06/2e/ac/5a/20130412153118625-384"')
                    pdCriteria.withStorageFormat().withCode().thatContains("PROPRIETARY")
                    pdCriteria.withLocatorType().withCode().thatContains("RELATIVE_LOCATION")
                    pdCriteria.withComplete().thatEquals("YES")
                    pdCriteria.withStatus().thatEquals("AVAILABLE")
                    pdCriteria.withPresentInArchive().thatEquals(false)
                    pdCriteria.withStorageConfirmation().thatEquals(true)
                    pdCriteria.withSpeedHint().thatEquals(-50)

                    return facade.searchDataSets(criteria, c.createDataSetFetchOptions())
                }

                var fCheck = function (facade: openbis.openbis, dataSets: openbis.DataSet[]) {
                    c.assertObjectsWithValues(dataSets, "code", ["20130412153118625-384"])
                }

                testSearch(c, fSearch, fCheck)
            })

            QUnit.test("searchDataSets() withPhysicalData with archiving requested", function (assert) {
                var c = new common(assert, dtos)

                var fSearch = function (facade: openbis.openbis) {
                    var criteria = new dtos.DataSetSearchCriteria()
                    criteria.withCode().thatStartsWith("2013")
                    var pdCriteria = criteria.withPhysicalData()
                    pdCriteria.withArchivingRequested().thatEquals(true)

                    return facade.searchDataSets(criteria, c.createDataSetFetchOptions())
                }

                var fCheck = function (facade: openbis.openbis, dataSets: openbis.DataSet[]) {
                    c.assertObjectsWithValues(dataSets, "code", ["20130412152036861-380"])
                }

                testSearch(c, fSearch, fCheck)
            })

            QUnit.test("searchDataSets() withLinkedData", function (assert) {
                var c = new common(assert, dtos)

                var fSearch = function (facade: openbis.openbis) {
                    var criteria = new dtos.DataSetSearchCriteria()
                    var ldCriteria = criteria.withLinkedData()
                    ldCriteria.withExternalDms().withCode().thatEquals("DMS_1")

                    return facade.searchDataSets(criteria, c.createDataSetFetchOptions())
                }

                var fCheck = function (facade: openbis.openbis, dataSets: openbis.DataSet[]) {
                    c.assertObjectsWithValues(dataSets, "code", ["20160613195437233-437"])
                }

                testSearch(c, fSearch, fCheck)
            })

            QUnit.test("searchDataSets() withRegistrator withUserId", function (assert) {
                var c = new common(assert, dtos)

                var fSearch = function (facade: openbis.openbis) {
                    var criteria = new dtos.DataSetSearchCriteria()
                    criteria.withRegistrator().withUserId().thatEquals("selenium")
                    return facade.searchDataSets(criteria, c.createDataSetFetchOptions())
                }

                var fCheck = function (facade: openbis.openbis, dataSets: openbis.DataSet[]) {
                    c.assertObjectsWithValues(dataSets, "code", ["20130417094936021-428", "20130417094934693-427"])
                }

                testSearch(c, fSearch, fCheck)
            })

            QUnit.test("searchDataSets() withModifier withUserId", function (assert) {
                var c = new common(assert, dtos)

                var fSearch = function (facade: openbis.openbis) {
                    var criteria = new dtos.DataSetSearchCriteria()
                    criteria.withModifier().withUserId().thatEquals("selenium")
                    return facade.searchDataSets(criteria, c.createDataSetFetchOptions())
                }

                var fCheck = function (facade: openbis.openbis, dataSets: openbis.DataSet[]) {
                    c.assertObjectsWithValues(dataSets, "code", ["20130412143121081-200", "20130412153119864-385"])
                }

                testSearch(c, fSearch, fCheck)
            })

            QUnit.test("searchDataSets() with nested logical operators", function (assert) {
                var c = new common(assert, dtos)

                var fSearch = function (facade: openbis.openbis) {
                    var criteria = new dtos.DataSetSearchCriteria().withAndOperator()

                    var subCriteria1 = criteria.withSubcriteria().withOrOperator()
                    subCriteria1.withCode().thatStartsWith("2016")
                    subCriteria1.withCode().thatStartsWith("2013")

                    var subCriteria2 = criteria.withSubcriteria().withOrOperator()
                    subCriteria2.withCode().thatEndsWith("1")
                    subCriteria2.withCode().thatEndsWith("7")

                    return facade.searchDataSets(criteria, c.createDataSetFetchOptions())
                }

                var fCheck = function (facade: openbis.openbis, dataSets: openbis.DataSet[]) {
                    var codes = c.extractCodes(dataSets)
                    c.assertEqual(codes.length, 7)
                    c.assertEqual(
                        codes.toString(),
                        "20130412142543232-197,20130412152038345-381,20130412153659994-391,20130415100158230-407,20130417094934693-427,20130424111751432-431,20160613195437233-437"
                    )
                }

                testSearch(c, fSearch, fCheck)
            })

            QUnit.test("searchDataSetTypes()", function (assert) {
                var c = new common(assert, dtos)

                var fSearch = function (facade: openbis.openbis) {
                    var criteria = new dtos.DataSetTypeSearchCriteria()
                    criteria.withCode().thatStartsWith("MA")
                    var fetchOptions = new dtos.DataSetTypeFetchOptions()
                    fetchOptions.withPropertyAssignments().sortBy().code().asc()
                    return facade.searchDataSetTypes(criteria, fetchOptions)
                }

                var fCheck = function (facade: openbis.openbis, dataSetTypes: openbis.DataSetType[]) {
                    c.assertEqual(dataSetTypes.length, 1, "Number of data set types")
                    var type = dataSetTypes[0]
                    c.assertEqual(type.getCode(), "MACS_OUTPUT", "Data set type code")
                    c.assertEqual(type.getFetchOptions().hasPropertyAssignments(), true)
                    var assignments = type.getPropertyAssignments()
                    c.assertEqual(assignments.length, 2, "Number of property assignments")
                    c.assertEqual(assignments[0].isMandatory(), false, "Mandatory property assignment?")
                    var propertyType = assignments[0].getPropertyType()
                    c.assertEqual(propertyType.getCode(), "MACS_VERSION", "Property type code")
                    c.assertEqual(propertyType.getLabel(), "MACS VERSION", "Property type label")
                    c.assertEqual(propertyType.getDescription(), "", "Property type description")
                    c.assertEqual(propertyType.getDataType(), "CONTROLLEDVOCABULARY", "Property data type")
                    c.assertEqual(propertyType.isManagedInternally(), false, "Property type managed internally?")
                    c.assertEqual(assignments[1].getPropertyType().getCode(), "NOTES", "Second property type code")
                    c.assertEqual(assignments[1].getPropertyType().getDataType(), "MULTILINE_VARCHAR", "Second property data type")
                }

                testSearch(c, fSearch, fCheck)
            })

            QUnit.test("searchDataSetTypes() with and operator", function (assert) {
                var c = new common(assert, dtos)

                var fSearch = function (facade: openbis.openbis) {
                    var criteria = new dtos.DataSetTypeSearchCriteria()
                    criteria.withAndOperator()
                    criteria.withCode().thatStartsWith("T")
                    criteria.withCode().thatContains("V")
                    var fetchOptions = new dtos.DataSetTypeFetchOptions()
                    return facade.searchDataSetTypes(criteria, fetchOptions)
                }

                var fCheck = function (facade: openbis.openbis, dataSetTypes: openbis.DataSetType[]) {
                    c.assertEqual(dataSetTypes.toString(), "TSV", "Data set types")
                }

                testSearch(c, fSearch, fCheck)
            })

            QUnit.test("searchDataSetTypes() with or operator", function (assert) {
                var c = new common(assert, dtos)

                var fSearch = function (facade: openbis.openbis) {
                    var criteria = new dtos.DataSetTypeSearchCriteria()
                    criteria.withOrOperator()
                    criteria.withCode().thatStartsWith("T")
                    criteria.withCode().thatContains("RV")
                    var fetchOptions = new dtos.DataSetTypeFetchOptions()
                    return facade.searchDataSetTypes(criteria, fetchOptions)
                }

                var fCheck = function (facade: openbis.openbis, dataSetTypes: openbis.DataSetType[]) {
                    dataSetTypes.sort()
                    c.assertEqual(dataSetTypes.toString(), "HCS_IMAGE_OVERVIEW,MICROSCOPY_IMG_OVERVIEW,THUMBNAILS,TSV", "Data set types")
                }

                testSearch(c, fSearch, fCheck)
            })

            QUnit.test("searchGlobally() withText thatContains", function (assert) {
                var c = new common(assert, dtos)

                var fSearch = function (facade: openbis.openbis) {
                    var criteria = new dtos.GlobalSearchCriteria()
                    criteria.withOrOperator()
                    criteria.withText().thatContains("20130412150049446-204 20130412140147735-20 20130417094936021-428 H2O")
                    var fo = c.createGlobalSearchObjectFetchOptions()
                    fo.withMatch()
                    return facade.searchGlobally(criteria, fo)
                }

                var fCheck = function (facade: openbis.openbis, objects: openbis.GlobalSearchObject[]) {
                    objects = objects.filter((o) => o.getObjectIdentifier().toString().search("V3") < 0)
                    c.assertEqual(objects.length, 3)
                    var prepopulatedExperimentsCount = 0
                    var prepopulatedSamplesCount = 0
                    for (var oIdx = 0; oIdx < objects.length; oIdx++) {
                        var result = objects[oIdx]
                        var match = result.getMatch()
                        switch (result.getObjectKind()) {
                            case "DATA_SET":
                                var dataSetPermId = (<openbis.DataSetPermId>result.getObjectPermId()).getPermId()
                                var dataSetIdentifier = (<openbis.DataSetPermId>result.getObjectIdentifier()).getPermId()

                                c.assertTrue(
                                    dataSetPermId === "20130417094936021-428" || dataSetPermId.startsWith("V3_DATA_SET_"),
                                    "DataSetPermId (" + dataSetPermId + ")"
                                )
                                c.assertEqual(dataSetIdentifier, dataSetPermId, "ObjectIdentifier")
                                c.assertTrue(
                                    match === "Perm ID: " + dataSetPermId || match === "Property 'Test Property Type': 20130412140147735-20",
                                    "Match (case DATA_SET). Actual value: " + match
                                )
                                c.assertNotNull(result.getScore(), "Score (case DATA_SET)")
                                c.assertNull(result.getExperiment(), "Experiment (case DATA_SET)")
                                c.assertNull(result.getSample(), "Sample (case DATA_SET)")
                                c.assertEqual(result.getDataSet().getCode(), dataSetPermId, "DataSet (case DATA_SET)")
                                break
                            case "EXPERIMENT":
                                var experimentPermId = (<openbis.ExperimentPermId>result.getObjectPermId()).getPermId()
                                var experimentIdentifier = (<openbis.ExperimentIdentifier>result.getObjectIdentifier()).getIdentifier()

                                if (experimentPermId === "20130412150049446-204") {
                                    prepopulatedExperimentsCount++
                                }

                                c.assertTrue(
                                    experimentIdentifier === "/TEST/TEST-PROJECT/TEST-EXPERIMENT" ||
                                        experimentIdentifier.startsWith("/TEST/TEST-PROJECT/V3_EXPERIMENT_"),
                                    "ObjectIdentifier"
                                )
                                c.assertTrue(
                                    match === "Perm ID: " + experimentPermId || match === "Property 'Test Property Type': 20130412140147735-20",
                                    "Match (case EXPERIMENT). Actual value: " + match
                                )
                                c.assertNotNull(result.getScore(), "Score (case EXPERIMENT)")
                                c.assertTrue(
                                    result.getExperiment().getCode() === "TEST-EXPERIMENT" ||
                                        result.getExperiment().getCode().startsWith("V3_EXPERIMENT_"),
                                    "Experiment (case EXPERIMENT)"
                                )
                                c.assertNull(result.getSample(), "Sample (case EXPERIMENT)")
                                c.assertNull(result.getDataSet(), "DataSet (case EXPERIMENT)")
                                break
                            case "SAMPLE":
                                var samplePermId = (<openbis.SamplePermId>result.getObjectPermId()).getPermId()
                                var sampleIdentifier = (<openbis.SampleIdentifier>result.getObjectIdentifier()).getIdentifier()

                                if (samplePermId === "20130412140147735-20") {
                                    prepopulatedSamplesCount++
                                }

                                c.assertTrue(
                                    sampleIdentifier === "/PLATONIC/SCREENING-EXAMPLES/PLATE-1" ||
                                        sampleIdentifier.startsWith("/TEST/TEST-PROJECT/V3_SAMPLE_"),
                                    "ObjectIdentifier"
                                )
                                c.assertTrue(
                                    match === "Perm ID: " + samplePermId || match === "Property 'Test Property Type': 20130412140147735-20",
                                    "Match (case SAMPLE). Actual value: " + match
                                )
                                c.assertNotNull(result.getScore(), "Score (case SAMPLE)")
                                c.assertNull(result.getExperiment(), "Experiment (case SAMPLE)")
                                c.assertTrue(
                                    result.getSample().getCode() === "PLATE-1" || result.getSample().getCode().startsWith("V3_SAMPLE_"),
                                    "Sample (case SAMPLE)"
                                )
                                c.assertNull(result.getDataSet(), "DataSet (case SAMPLE)")
                                break
                        }
                    }
                    c.assertEqual(prepopulatedExperimentsCount, 1, "ExperimentPermId")
                    c.assertEqual(prepopulatedSamplesCount, 1, "SamplePermId")
                }

                testSearch(c, fSearch, fCheck)
            })

            QUnit.test("searchGlobally() withText thatContainsExactly", function (assert) {
                var c = new common(assert, dtos)

                var fSearch = function (facade: openbis.openbis) {
                    var criteria = new dtos.GlobalSearchCriteria()
                    criteria.withOrOperator()
                    criteria.withText().thatContainsExactly("407 description")
                    var fo = c.createGlobalSearchObjectFetchOptions()
                    fo.withMatch()
                    return facade.searchGlobally(criteria, fo)
                }

                var fCheck = function (facade: openbis.openbis, objects: openbis.GlobalSearchObject[]) {
                    objects = objects.filter((o) => o.getObjectIdentifier().toString().search("V3") < 0)
                    c.assertEqual(objects.length, 1)

                    var object0 = objects[0]
                    var objectPermId = <openbis.DataSetPermId>object0.getObjectPermId()
                    var objectIdentifier = <openbis.DataSetPermId>object0.getObjectIdentifier()

                    c.assertEqual(object0.getObjectKind(), "DATA_SET", "ObjectKind")
                    c.assertEqual(objectPermId.getPermId(), "20130415100158230-407", "ObjectPermId")
                    c.assertEqual(objectIdentifier.getPermId(), "20130415100158230-407", "ObjectIdentifier")
                    // c.assertEqual(object0.getMatch(), "Property 'Description': 407 description", "Match");
                    c.assertNotNull(object0.getScore(), "Score")
                    c.assertNull(object0.getExperiment(), "Experiment")
                    c.assertNull(object0.getSample(), "Sample")
                    c.assertEqual(object0.getDataSet().getCode(), "20130415100158230-407", "DataSet")
                }

                testSearch(c, fSearch, fCheck)
            })

            QUnit.test("searchGlobally() withObjectKind thatIn", function (assert) {
                var c = new common(assert, dtos)

                var fSearch = function (facade: openbis.openbis) {
                    var criteria = new dtos.GlobalSearchCriteria()
                    criteria.withOrOperator()
                    criteria.withText().thatContains("20130412150049446-204 20130412140147735-20 20130417094936021-428 H2O")
                    criteria.withObjectKind().thatIn(["EXPERIMENT"])
                    var fo = c.createGlobalSearchObjectFetchOptions()
                    fo.withMatch()
                    return facade.searchGlobally(criteria, fo)
                }

                var fCheck = function (facade: openbis.openbis, objects: openbis.GlobalSearchObject[]) {
                    objects = objects.filter((o) => o.getObjectIdentifier().toString().search("V3") < 0)
                    c.assertEqual(objects.length, 1)

                    var prepopulatedExperimentsCount = 0
                    for (var i = 0; i < objects.length; i++) {
                        var objectExperiment = objects[i]

                        c.assertEqual(objectExperiment.getObjectKind(), "EXPERIMENT", "ObjectKind")

                        var experimentPermId = (<openbis.ExperimentPermId>objectExperiment.getObjectPermId()).getPermId()
                        var experimentIdentifier = (<openbis.ExperimentIdentifier>objectExperiment.getObjectIdentifier()).getIdentifier()

                        if (experimentPermId === "20130412150049446-204") {
                            prepopulatedExperimentsCount++
                        }

                        c.assertTrue(
                            experimentIdentifier === "/TEST/TEST-PROJECT/TEST-EXPERIMENT" ||
                                experimentIdentifier.startsWith("/TEST/TEST-PROJECT/V3_EXPERIMENT_"),
                            "ObjectIdentifier"
                        )

                        var match = objectExperiment.getMatch()

                        c.assertTrue(
                            match === "Perm ID: " + experimentPermId || match === "Property 'Test Property Type': 20130412140147735-20",
                            "Match. Actual value: " + match
                        )
                        c.assertNotNull(objectExperiment.getScore(), "Score")
                        c.assertTrue(
                            objectExperiment.getExperiment().getCode() === "TEST-EXPERIMENT" ||
                                objectExperiment.getExperiment().getCode().startsWith("V3_EXPERIMENT_"),
                            "Experiment"
                        )
                        c.assertNull(objectExperiment.getSample(), "Sample")
                        c.assertNull(objectExperiment.getDataSet(), "DataSet")
                    }
                }

                testSearch(c, fSearch, fCheck)
            })

            QUnit.test("searchObjectKindModifications()", function (assert) {
                var c = new common(assert, dtos)

                var fSearch = function (facade: openbis.openbis) {
                    var criteria = new dtos.ObjectKindModificationSearchCriteria()
                    criteria.withObjectKind().thatIn(["SAMPLE", "EXPERIMENT"])
                    criteria.withOperationKind().thatIn(["CREATE_OR_DELETE"])
                    return facade.searchObjectKindModifications(criteria, c.createObjectKindModificationFetchOptions())
                }

                var fCheck = function (facade: openbis.openbis, objects: openbis.ObjectKindModification[]) {
                    c.assertEqual(objects.length, 2)

                    var object0 = objects[0]
                    c.assertEqual(object0.getObjectKind(), "SAMPLE", "ObjectKind")
                    c.assertNotNull(object0.getLastModificationTimeStamp(), "LastModificationTimeStamp")

                    var object1 = objects[1]
                    c.assertEqual(object1.getObjectKind(), "EXPERIMENT", "ObjectKind")
                    c.assertNotNull(object1.getLastModificationTimeStamp(), "LastModificationTimeStamp")
                }

                testSearch(c, fSearch, fCheck)
            })

            QUnit.test("searchPlugins()", function (assert) {
                var c = new common(assert, dtos)

                var fSearch = function (facade: openbis.openbis) {
                    var criteria = new dtos.PluginSearchCriteria()
                    criteria.withName().thatContains("Has")
                    criteria.withPluginType().thatEquals(dtos.PluginType.ENTITY_VALIDATION)
                    var fo = c.createPluginFetchOptions()
                    fo.withScript()
                    fo.sortBy().name().desc()
                    return facade.searchPlugins(criteria, fo)
                }

                var fCheck = function (facade: openbis.openbis, plugins: openbis.Plugin[]) {
                    c.assertEqual(plugins.length, 1)
                    var plugin = plugins[0]
                    c.assertEqual(plugin.getName(), "Has_Parents", "Name")
                    c.assertEqual(plugin.getDescription(), "Check if the Entity has a parent", "Description")
                    c.assertEqual(plugin.getPluginKind(), dtos.PluginKind.JYTHON, "Plugin kind")
                    c.assertEqual(plugin.getPluginType(), dtos.PluginType.ENTITY_VALIDATION, "Plugin type")
                    c.assertEqual(plugin.getFetchOptions().hasScript(), true, "Has script")
                    c.assertEqual(
                        plugin.getScript(),
                        "def validate(entity, isNew):\n" +
                            "  parents = entity.entityPE().parents\n" +
                            "  if parents:\n" +
                            "    return None\n" +
                            "  else:\n" +
                            '    return "No Parents have been selected!"\n',
                        "Script"
                    )
                }

                testSearch(c, fSearch, fCheck)
            })

            QUnit.test("searchVocabularies()", function (assert) {
                var c = new common(assert, dtos)

                var fSearch = function (facade: openbis.openbis) {
                    var criteria = new dtos.VocabularySearchCriteria()
                    criteria.withCode().thatEquals("STORAGE_FORMAT")
                    return facade.searchVocabularies(criteria, c.createVocabularyFetchOptions())
                }

                var fCheck = function (facade: openbis.openbis, vocabularies: openbis.Vocabulary[]) {
                    c.assertEqual(vocabularies.length, 1)
                    var vocabulary = vocabularies[0]
                    c.assertEqual(vocabulary.getCode(), "STORAGE_FORMAT", "Code")
                    c.assertEqual(vocabulary.getDescription(), "The on-disk storage format of a data set", "Description")
                    c.assertEqual(vocabulary.isManagedInternally(), true, "Managed internally")
                    c.assertEqual(vocabulary.getTerms().length, 2, "# of terms")
                }

                testSearch(c, fSearch, fCheck)
            })

            QUnit.test("searchVocabularies() with codes", function (assert) {
                var c = new common(assert, dtos)

                var fSearch = function (facade: openbis.openbis) {
                    var criteria = new dtos.VocabularySearchCriteria()
                    criteria.withCodes().thatIn(["END_TYPE", "KIT", "BLABLA"])
                    return facade.searchVocabularies(criteria, c.createVocabularyFetchOptions())
                }

                var fCheck = function (facade: openbis.openbis, vocabularies: openbis.Vocabulary[]) {
                    var codes = []
                    for (var i = 0; i < vocabularies.length; i++) {
                        codes.push(vocabularies[i].getCode())
                    }
                    codes.sort()
                    c.assertEqual(codes.toString(), "END_TYPE,KIT", "Vocabularies")
                }

                testSearch(c, fSearch, fCheck)
            })

            QUnit.test("searchVocabularies() with and operator", function (assert) {
                var c = new common(assert, dtos)

                var fSearch = function (facade: openbis.openbis) {
                    var criteria = new dtos.VocabularySearchCriteria()
                    criteria.withAndOperator()
                    criteria.withCode().thatEndsWith("KIT")
                    criteria.withCode().thatContains("LE")
                    return facade.searchVocabularies(criteria, c.createVocabularyFetchOptions())
                }

                var fCheck = function (facade: openbis.openbis, vocabularies: openbis.Vocabulary[]) {
                    var codes = []
                    for (var i = 0; i < vocabularies.length; i++) {
                        codes.push(vocabularies[i].getCode())
                    }
                    codes.sort()
                    c.assertEqual(codes.toString(), "AGILENT_KIT", "Vocabularies")
                }

                testSearch(c, fSearch, fCheck)
            })

            QUnit.test("searchVocabularies() with or operator", function (assert) {
                var c = new common(assert, dtos)

                var fSearch = function (facade: openbis.openbis) {
                    var criteria = new dtos.VocabularySearchCriteria()
                    criteria.withOrOperator()
                    criteria.withCode().thatEndsWith("KIT")
                    criteria.withCode().thatContains("LE")
                    return facade.searchVocabularies(criteria, c.createVocabularyFetchOptions())
                }

                var fCheck = function (facade: openbis.openbis, vocabularies: openbis.Vocabulary[]) {
                    var codes = []
                    for (var i = 0; i < vocabularies.length; i++) {
                        codes.push(vocabularies[i].getCode())
                    }
                    codes.sort()
                    var actual = codes.toString()
                    var expected = "AGILENT_KIT,DEFAULT_COLLECTION_VIEWS,KIT,SAMPLE_TYPE,STORAGE.STORAGE_VALIDATION_LEVEL"
                    c.assertEqual(actual, expected, "Vocabularies Expected: [" + expected + "] - Actual: [" + actual + "]")
                }

                testSearch(c, fSearch, fCheck)
            })

            QUnit.test("searchVocabularyTerms()", function (assert) {
                var c = new common(assert, dtos)

                var fSearch = function (facade: openbis.openbis) {
                    var criteria = new dtos.VocabularyTermSearchCriteria()
                    criteria.withCode().thatEquals("BDS_DIRECTORY")
                    return facade.searchVocabularyTerms(criteria, c.createVocabularyTermFetchOptions())
                }

                var fCheck = function (facade: openbis.openbis, terms: openbis.VocabularyTerm[]) {
                    c.assertEqual(terms.length, 1)
                    var term = terms[0]
                    c.assertEqual(term.getCode(), "BDS_DIRECTORY", "Code")
                    c.assertEqual(term.getVocabulary().getCode(), "STORAGE_FORMAT", "Vocabulary code")
                    c.assertEqual(term.getOrdinal(), 2, "Ordinal")
                    c.assertEqual(term.isOfficial(), true, "Official")
                }

                testSearch(c, fSearch, fCheck)
            })

            QUnit.test("searchVocabularyTerms() with codes", function (assert) {
                var c = new common(assert, dtos)

                var fSearch = function (facade: openbis.openbis) {
                    var criteria = new dtos.VocabularyTermSearchCriteria()
                    criteria.withCodes().thatIn(["BDS_DIRECTORY"])
                    return facade.searchVocabularyTerms(criteria, c.createVocabularyTermFetchOptions())
                }

                var fCheck = function (facade: openbis.openbis, terms: openbis.VocabularyTerm[]) {
                    c.assertEqual(terms.length, 1)
                    var term = terms[0]
                    c.assertEqual(term.getCode(), "BDS_DIRECTORY", "Code")
                    c.assertEqual(term.getVocabulary().getCode(), "STORAGE_FORMAT", "Vocabulary code")
                    c.assertEqual(term.getOrdinal(), 2, "Ordinal")
                    c.assertEqual(term.isOfficial(), true, "Official")
                }

                testSearch(c, fSearch, fCheck)
            })

            QUnit.test("searchExternalDms()", function (assert) {
                var c = new common(assert, dtos)

                var fSearch = function (facade: openbis.openbis) {
                    var criteria = new dtos.as_dto_externaldms_search_ExternalDmsSearchCriteria()
                    criteria.withCode().thatEquals("DMS_2")
                    return facade.searchExternalDataManagementSystems(criteria, c.createExternalDmsFetchOptions())
                }

                var fCheck = function (facade: openbis.openbis, entities: openbis.ExternalDms[]) {
                    c.assertEqual(entities.length, 1)
                    var edms = entities[0]
                    c.assertEqual(edms.getCode(), "DMS_2", "Code")
                    c.assertEqual(edms.getLabel(), "Test External openBIS instance", "Label")
                    c.assertEqual(edms.getAddress(), "http://www.openbis.ch/perm_id=${code}", "Address")
                    c.assertEqual(edms.getUrlTemplate(), "http://www.openbis.ch/perm_id=${code}", "URL template")
                    c.assertEqual(edms.getAddressType(), "OPENBIS", "Address type")
                    c.assertEqual(edms.isOpenbis(), true, "is openBIS?")
                }

                testSearch(c, fSearch, fCheck)
            })

            QUnit.test("searchExternalDms() with codes", function (assert) {
                var c = new common(assert, dtos)

                var fSearch = function (facade: openbis.openbis) {
                    var criteria = new dtos.as_dto_externaldms_search_ExternalDmsSearchCriteria()
                    criteria.withCodes().thatIn(["DMS_2"])
                    return facade.searchExternalDataManagementSystems(criteria, c.createExternalDmsFetchOptions())
                }

                var fCheck = function (facade: openbis.openbis, entities: openbis.ExternalDms[]) {
                    c.assertEqual(entities.length, 1)
                    var edms = entities[0]
                    c.assertEqual(edms.getCode(), "DMS_2", "Code")
                    c.assertEqual(edms.getLabel(), "Test External openBIS instance", "Label")
                    c.assertEqual(edms.getAddress(), "http://www.openbis.ch/perm_id=${code}", "Address")
                    c.assertEqual(edms.getUrlTemplate(), "http://www.openbis.ch/perm_id=${code}", "URL template")
                    c.assertEqual(edms.getAddressType(), "OPENBIS", "Address type")
                    c.assertEqual(edms.isOpenbis(), true, "is openBIS?")
                }

                testSearch(c, fSearch, fCheck)
            })

            QUnit.test("searchTags()", function (assert) {
                var c = new common(assert, dtos)

                var fSearch = function (facade: openbis.openbis) {
                    var criteria = new dtos.TagSearchCriteria()
                    criteria.withCode().thatEquals("JS_TEST_METAPROJECT")
                    return facade.searchTags(criteria, c.createTagFetchOptions())
                }

                var fCheck = function (facade: openbis.openbis, tags: openbis.Tag[]) {
                    c.assertEqual(tags.length, 1)
                    var tag = tags[0]
                    c.assertEqual(tag.getCode(), "JS_TEST_METAPROJECT", "Code")
                    c.assertEqual(tag.getPermId().getPermId(), "/openbis_test_js/JS_TEST_METAPROJECT", "PermId")
                }

                testSearch(c, fSearch, fCheck)
            })

            QUnit.test("searchTags() with codes", function (assert) {
                var c = new common(assert, dtos)

                var fSearch = function (facade: openbis.openbis) {
                    var criteria = new dtos.TagSearchCriteria()
                    criteria.withCodes().thatIn(["JS_TEST_METAPROJECT"])
                    return facade.searchTags(criteria, c.createTagFetchOptions())
                }

                var fCheck = function (facade: openbis.openbis, tags: openbis.Tag[]) {
                    c.assertEqual(tags.length, 1)
                    var tag = tags[0]
                    c.assertEqual(tag.getCode(), "JS_TEST_METAPROJECT", "Code")
                    c.assertEqual(tag.getPermId().getPermId(), "/openbis_test_js/JS_TEST_METAPROJECT", "PermId")
                }

                testSearch(c, fSearch, fCheck)
            })

            QUnit.test("searchAuthorizationGroups()", function (assert) {
                var c = new common(assert, dtos)
                var code

                var fSearch = function (facade: openbis.openbis) {
                    return c.createAuthorizationGroup(facade).then(function (permId) {
                        var criteria = new dtos.AuthorizationGroupSearchCriteria()
                        code = permId.getPermId()
                        criteria.withCode().thatEquals(code)
                        return facade.searchAuthorizationGroups(criteria, c.createAuthorizationGroupFetchOptions())
                    })
                }

                var fCheck = function (facade: openbis.openbis, groups: openbis.AuthorizationGroup[]) {
                    c.assertEqual(groups.length, 1)
                    var group = groups[0]
                    c.assertEqual(group.getCode(), code, "Code")
                    var users = group.getUsers()
                    c.assertEqual(users[0].getUserId(), "power_user", "User")
                    c.assertEqual(users.length, 1, "# Users")
                }

                testSearch(c, fSearch, fCheck)
            })

            QUnit.test("searchAuthorizationGroups() with codes", function (assert) {
                var c = new common(assert, dtos)
                var code

                var fSearch = function (facade: openbis.openbis) {
                    return c.createAuthorizationGroup(facade).then(function (permId) {
                        var criteria = new dtos.AuthorizationGroupSearchCriteria()
                        code = permId.getPermId()
                        criteria.withCodes().thatIn([code])
                        return facade.searchAuthorizationGroups(criteria, c.createAuthorizationGroupFetchOptions())
                    })
                }

                var fCheck = function (facade: openbis.openbis, groups: openbis.AuthorizationGroup[]) {
                    c.assertEqual(groups.length, 1)
                    var group = groups[0]
                    c.assertEqual(group.getCode(), code, "Code")
                    var users = group.getUsers()
                    c.assertEqual(users[0].getUserId(), "power_user", "User")
                    c.assertEqual(users.length, 1, "# Users")
                }

                testSearch(c, fSearch, fCheck)
            })

            QUnit.test("searchAuthorizationGroups() existing with role assigments", function (assert) {
                var c = new common(assert, dtos)
                var code

                var fSearch = function (facade: openbis.openbis) {
                    var criteria = new dtos.AuthorizationGroupSearchCriteria()
                    criteria.withCode().thatEquals("TEST-GROUP")
                    return facade.searchAuthorizationGroups(criteria, c.createAuthorizationGroupFetchOptions())
                }

                var fCheck = function (facade: openbis.openbis, groups: openbis.AuthorizationGroup[]) {
                    c.assertEqual(groups.length, 1)
                    var group = groups[0]
                    c.assertEqual(group.getCode(), "TEST-GROUP", "Code")
                    var users = group.getUsers()
                    c.assertEqual(users.length, 0, "# Users")
                    var roleAssignments = group.getRoleAssignments()
                    var numberOfTestSpaceAssignments = 0
                    var numberOfProjectAssignments = 0
                    for (var i = 0; i < roleAssignments.length; i++) {
                        var ra = roleAssignments[i]
                        if (ra.getSpace() && ra.getSpace().getCode() === "TEST") {
                            c.assertEqual(ra.getRole(), "OBSERVER", "Role of assignment for space TEST")
                            numberOfTestSpaceAssignments++
                        }
                        if (ra.getProject()) {
                            c.assertEqual(ra.getRole(), "ADMIN", "Role of assignment for project")
                            c.assertEqual(ra.getProject().getCode(), "TEST-PROJECT", "Project code of assignment for project")
                            c.assertEqual(ra.getProject().getSpace().getCode(), "TEST", "Project space of assignment for project")
                            numberOfProjectAssignments++
                        }
                    }
                    c.assertEqual(numberOfTestSpaceAssignments, 1, "Number of TEST space assignments")
                    c.assertEqual(numberOfProjectAssignments, 0, "Number of project assignments")
                }

                testSearch(c, fSearch, fCheck)
            })

            QUnit.test("searchRoleAssignments()", function (assert) {
                var c = new common(assert, dtos)
                var code

                var fSearch = function (facade: openbis.openbis) {
                    var criteria = new dtos.RoleAssignmentSearchCriteria()
                    criteria.withSpace().withCode().thatEquals("TEST")
                    criteria.withUser().withUserId().thatEquals("observer")
                    return facade.searchRoleAssignments(criteria, c.createRoleAssignmentFetchOptions())
                }

                var fCheck = function (facade: openbis.openbis, assignments: openbis.RoleAssignment[]) {
                    c.assertEqual(assignments.length, 1, "# Role Assignments")
                    var assignment = assignments[0]
                    c.assertEqual(assignment.getRole(), "OBSERVER", "Role")
                    c.assertEqual(assignment.getRoleLevel(), "SPACE", "Role level")
                    c.assertEqual(assignment.getSpace().getCode(), "TEST", "Space")
                }

                testSearch(c, fSearch, fCheck)
            })

            QUnit.test("searchRoleAssignments() with group with user", function (assert) {
                var c = new common(assert, dtos)
                var code

                var fSearch = function (facade: openbis.openbis) {
                    return c.createAuthorizationGroup(facade).then(function (permId) {
                        var creation = new dtos.RoleAssignmentCreation()
                        creation.setRole(dtos.Role.POWER_USER)
                        creation.setAuthorizationGroupId(permId)
                        creation.setSpaceId(new dtos.SpacePermId("TEST"))
                        return facade.createRoleAssignments([creation]).then(function (permIds) {
                            var criteria = new dtos.RoleAssignmentSearchCriteria()
                            criteria.withSpace().withCode().thatEquals("TEST")
                            criteria.withAuthorizationGroup().withUser().withUserId().thatEquals("power_user")
                            var fo = c.createRoleAssignmentFetchOptions()
                            fo.withAuthorizationGroup().withUsers()
                            return facade.searchRoleAssignments(criteria, fo)
                        })
                    })
                }

                var fCheck = function (facade: openbis.openbis, assignments: openbis.RoleAssignment[]) {
                    c.assertEqual(assignments.length > 0, true, "At least one Role Assignment?")
                    var assignment = assignments[0]
                    c.assertEqual(assignment.getRole(), "POWER_USER", "Role")
                    c.assertEqual(assignment.getRoleLevel(), "SPACE", "Role level")
                    c.assertEqual(assignment.getSpace().getCode(), "TEST", "Space")
                }

                testSearch(c, fSearch, fCheck)
            })

            QUnit.test("searchPersons()", function (assert) {
                var c = new common(assert, dtos)
                var code

                var fSearch = function (facade: openbis.openbis) {
                    var criteria = new dtos.PersonSearchCriteria()
                    criteria.withUserId().thatContains("bser")
                    return facade.searchPersons(criteria, c.createPersonFetchOptions())
                }

                var fCheck = function (facade: openbis.openbis, persons: openbis.Person[]) {
                    c.assertEqual(persons.length, 1, "# persons")
                    var person = persons[0]
                    c.assertEqual(person.getUserId(), "observer", "User id")
                    c.assertEqual(person.getRegistrator().getUserId(), "system", "Registrator")
                    var assignments = person.getRoleAssignments()
                    c.assertEqual(assignments.length, 1, "# Role Assignments")
                    var assignment = assignments[0]
                    c.assertEqual(assignment.getRole(), "OBSERVER", "Role")
                    c.assertEqual(assignment.getRoleLevel(), "SPACE", "Role level")
                    c.assertEqual(assignment.getSpace().getCode(), "TEST", "Space")
                }

                testSearch(c, fSearch, fCheck)
            })

            QUnit.test("searchOperationExecutions()", function (assert) {
                var c = new common(assert, dtos)

                c.start()

                // We want to start only once. Because testSearch() also calls
                // start() let's make the start() do nothing.
                c.start = function () {}

                c.login(facade)
                    .then(function () {
                        return c.createOperationExecution(facade).then(function (permId) {
                            var fSearch = function (facade: openbis.openbis) {
                                var criteria = new dtos.OperationExecutionSearchCriteria()
                                return facade.searchOperationExecutions(criteria, new dtos.OperationExecutionFetchOptions())
                            }

                            var fCheck = function (facade: openbis.openbis, executions: openbis.OperationExecution[]) {
                                c.assertTrue(executions.length >= 1)
                                var found = false

                                executions.forEach(function (execution) {
                                    if (execution.getPermId().getPermId() == permId.getPermId()) {
                                        found = true
                                    }
                                })

                                c.assertTrue(found)
                            }

                            return testSearch(c, fSearch, fCheck)
                        })
                    })
                    .fail(function () {
                        c.fail()
                        c.finish()
                    })
            })

            const CREATED_DATA_STORE = "CREATED_DATA_STORE"

            QUnit.test("searchDataStores() withEmptyCriteria", function (assert) {
                var c = new common(assert, dtos)

                var fSearch = function (facade: openbis.openbis) {
                    var criteria = new dtos.DataStoreSearchCriteria()
                    return facade.searchDataStores(criteria, c.createDataStoreFetchOptions())
                }

                var fCheck = function (facade: openbis.openbis, dataStores: openbis.DataStore[]) {
                    dataStores = dataStores.filter(ds => !ds.getCode().includes(CREATED_DATA_STORE))
                    c.assertEqual(dataStores.length, 2)
                    c.assertObjectsWithValues(dataStores, "code", ["DSS1", "DSS2"])
                }

                testSearch(c, fSearch, fCheck)
            })

            QUnit.test("searchDataStores() withCodeThatEquals", function (assert) {
                var c = new common(assert, dtos)

                var fSearch = function (facade: openbis.openbis) {
                    var criteria = new dtos.DataStoreSearchCriteria()
                    criteria.withCode().thatEquals("DSS1")
                    return facade.searchDataStores(criteria, c.createDataStoreFetchOptions())
                }

                var fCheck = function (facade: openbis.openbis, dataStores: openbis.DataStore[]) {
                    dataStores = dataStores.filter(ds => !ds.getCode().includes(CREATED_DATA_STORE))
                    c.assertEqual(dataStores.length, 1)
                    var dataStore = dataStores[0]
                    c.assertEqual(dataStore.getCode(), "DSS1", "Code")
                }

                testSearch(c, fSearch, fCheck)
            })

            QUnit.test("testSearchDataStore() withKind none", function (assert) {
                var c = new common(assert, dtos)

                var fSearch = function (facade: openbis.openbis) {
                    var criteria = new dtos.DataStoreSearchCriteria()
                    criteria.withKind().thatIn([])
                    return facade.searchDataStores(criteria, new dtos.DataStoreFetchOptions())
                }

                var fCheck = function (facade: openbis.openbis, dataStores: openbis.DataStore[]) {
                    dataStores = dataStores.filter(ds => !ds.getCode().includes(CREATED_DATA_STORE))
                    var codes: string[] = dataStores.map((dataStore: openbis.DataStore): string => dataStore.getCode())
                    c.assertEqual(codes.length, 0)
                }

                testSearch(c, fSearch, fCheck)
            })

            QUnit.test("testSearchDataStore() withKind DSS", function (assert) {
                var c = new common(assert, dtos)

                var fSearch = function (facade: openbis.openbis) {
                    var criteria = new dtos.DataStoreSearchCriteria()
                    criteria.withKind().thatIn([dtos.DataStoreKind.DSS])
                    return facade.searchDataStores(criteria, new dtos.DataStoreFetchOptions())
                }

                var fCheck = function (facade: openbis.openbis, dataStores: openbis.DataStore[]) {
                    dataStores = dataStores.filter(ds => !ds.getCode().includes(CREATED_DATA_STORE))
                    var codes: string[] = dataStores.map((dataStore: openbis.DataStore): string => dataStore.getCode())
                    c.assertTrue(arraySetsEqual(codes, ["DSS1", "DSS2"]))
                }

                testSearch(c, fSearch, fCheck)
            })

            QUnit.test("testSearchDataStore() withKind AFS", function (assert) {
                var c = new common(assert, dtos)

                var fSearch = function (facade: openbis.openbis) {
                    var criteria = new dtos.DataStoreSearchCriteria()
                    criteria.withKind().thatIn([dtos.DataStoreKind.AFS])
                    return facade.searchDataStores(criteria, new dtos.DataStoreFetchOptions())
                }

                var fCheck = function (facade: openbis.openbis, dataStores: openbis.DataStore[]) {
                    dataStores = dataStores.filter(ds => !ds.getCode().includes(CREATED_DATA_STORE))
                    var codes: string[] = dataStores.map((dataStore: openbis.DataStore): string => dataStore.getCode())
                    c.assertTrue(arraySetsEqual(codes, ["AFS"]))
                }

                testSearch(c, fSearch, fCheck)
            })

            QUnit.test("testSearchDataStore() withKind AFS and DSS", function (assert) {
                var c = new common(assert, dtos)

                var fSearch = function (facade: openbis.openbis) {
                    var criteria = new dtos.DataStoreSearchCriteria()
                    criteria.withKind().thatIn([dtos.DataStoreKind.AFS, dtos.DataStoreKind.DSS])
                    return facade.searchDataStores(criteria, new dtos.DataStoreFetchOptions())
                }

                var fCheck = function (facade: openbis.openbis, dataStores: openbis.DataStore[]) {
                    dataStores = dataStores.filter(ds => !ds.getCode().includes(CREATED_DATA_STORE))
                    var codes: string[] = dataStores.map((dataStore: openbis.DataStore): string => dataStore.getCode())
                    c.assertTrue(arraySetsEqual(codes, ["AFS", "DSS1", "DSS2"]))
                }

                testSearch(c, fSearch, fCheck)
            })

            function arraySetsEqual(arr1, arr2) {
                return new Set(arr1).size === new Set(arr2).size &&
                  [...new Set(arr1)].every(value => arr2.includes(value));
            }

            function checkExceptionsThrown(assert, dataTypes, createCriteria) {
                var c = new common(assert, dtos)
                c.start = function () {}
                c.finish = function () {}

                var finish = assert.async()

                var fRecursion = function (i) {
                    if (i < dataTypes.length - 1) {
                        testDataType(i + 1)
                    } else {
                        finish()
                    }
                }

                var testDataType = function (i) {
                    var dataType = dataTypes[i]
                    var fSearch = function (facade: openbis.openbis) {
                        return createPropertyType(c, facade, dataType).then(function (propertyTypeIds) {
                            var criteria = createCriteria(propertyTypeIds[0].getPermId())
                            return facade.searchSamples(criteria, c.createSampleFetchOptions())
                        })
                    }

                    var fCheck = function () {
                        c.fail("Expected exception not thrown for data type " + dataType + ".")
                        fRecursion(i)
                    }

                    testSearch(c, fSearch, fCheck, function (e) {
                        c.ok("Expected exception thrown for data type " + dataType + ".")
                        fRecursion(i)
                    })
                }

                testDataType(0)
            }

            function cleanup(c, facade: openbis.openbis, samplePermId, propertyTypeId, sampleTypeId) {
                var options = new dtos.SampleDeletionOptions()
                options.setReason("Test reason.")
                facade.deleteSamples([samplePermId], options).then(
                    function (deletionId) {
                        facade.confirmDeletions([deletionId]).then(
                            function () {
                                var options = new dtos.SampleTypeDeletionOptions()
                                options.setReason("Test reason.")
                                facade.deleteSampleTypes([sampleTypeId], options).then(
                                    function () {
                                        var options = new dtos.PropertyTypeDeletionOptions()
                                        options.setReason("Test reason.")
                                        facade.deletePropertyTypes([propertyTypeId], options).then(null, function (error) {
                                            c.fail("Error deleting property type. error.message=" + error.message)
                                        })
                                    },
                                    function (error) {
                                        c.fail("Error deleting sample type. error.message=" + error.message)
                                    }
                                )
                            },
                            function (error) {
                                c.fail("Error confirming sample deletion. error.message=" + error.message)
                            }
                        )
                    },
                    function (error) {
                        c.fail("Error deleting sample. error.message=" + error.message)
                    }
                )
            }

            function createSampleType(c, facade: openbis.openbis, mandatory) {
                var creation = new dtos.SampleTypeCreation()
                creation.setCode("SAMPLE-TYPE-" + Date.now())

                var assignments = []
                for (var i = 3; i < arguments.length; i++) {
                    var propertyAssignmentCreation = new dtos.PropertyAssignmentCreation()
                    propertyAssignmentCreation.setPropertyTypeId(arguments[i])
                    propertyAssignmentCreation.setMandatory(mandatory)
                    assignments.push(propertyAssignmentCreation)
                }
                creation.setPropertyAssignments(assignments)

                return facade.createSampleTypes([creation])
            }

            function createPropertyType(c, facade: openbis.openbis, dataType, vocabularyPermId?) {
                var creation = new dtos.PropertyTypeCreation()
                creation.setCode("TYPE-" + Date.now())
                creation.setDataType(dataType)
                creation.setDescription("description")
                creation.setLabel("label")
                creation.setMultiValue(false)

                if (dataType === dtos.DataType.CONTROLLEDVOCABULARY) {
                    creation.setVocabularyId(vocabularyPermId)
                }
                return facade.createPropertyTypes([creation])
            }

            function createSample(c, facade: openbis.openbis, sampleTypeId, propertyType, value) {
                var creation = new dtos.SampleCreation()
                creation.setTypeId(sampleTypeId)
                creation.setCode("V3-TST-SAMPLE-" + Date.now())
                creation.setSpaceId(new dtos.SpacePermId("TEST"))
                creation.setProperty(propertyType, value)
                return facade.createSamples([creation])
            }
        }

        resolve(function () {
            executeModule("Search tests (RequireJS)", new openbisRequireJS(), dtos)
            executeModule("Search tests (RequireJS - executeOperations)", new openbisExecuteOperations(new openbisRequireJS(), dtos), dtos)
            executeModule("Search tests (module VAR)", new window.openbis.openbis(), window.openbis)
            executeModule(
                "Search tests (module VAR - executeOperations)",
                new openbisExecuteOperations(new window.openbis.openbis(), window.openbis),
                window.openbis
            )
            executeModule("Search tests (module ESM)", new window.openbisESM.openbis(), window.openbisESM)
            executeModule(
                "Search tests (module ESM - executeOperations)",
                new openbisExecuteOperations(new window.openbisESM.openbis(), window.openbisESM),
                window.openbisESM
            )
        })
    })
})
