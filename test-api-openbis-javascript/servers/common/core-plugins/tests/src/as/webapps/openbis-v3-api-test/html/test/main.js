define([
    "test-compiled/test-login",
    "test/test-jsVSjava",
    "test-compiled/test-create",
    "test-compiled/test-update",
    "test-compiled/test-search",

    "test-compiled/test-freezing",
    "test-compiled/test-get",
    "test-compiled/test-delete",
    "test-compiled/test-execute",
    "test-compiled/test-evaluate",
    "test/test-json",

    // 'test/test-dto',
    "test/test-dto-roundtrip",
    "test-compiled/test-custom-services",
    "test-compiled/test-dss-services",
    "test-compiled/test-archive-unarchive",
    "test-compiled/test-import-export",
    "test-compiled/test-typescript",
    "test-compiled/test-afs",
    "test-compiled/test-transactions",
], function () {
    var testSuites = arguments
    return async function () {
        for (var i = 0; i < testSuites.length; i++) {
            var suite = testSuites[i]

            if (typeof suite === "object") {
                var suite = await suite.default
                suite()
            } else if (typeof suite === "function") {
                suite()
            } else {
                throw Error("Unsupported suite format " + suite)
            }
        }
    }
})
