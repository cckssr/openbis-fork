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
        var executeModule = function (moduleName: string, facade: openbis.openbis, dtos: openbis.bundle) {
            QUnit.module(moduleName)

            var testInteractiveSessionKey = "test-interactive-session-key"

            var testList = async function (assert, useTransaction) {
                const testContent1 = new TextEncoder().encode("test-content-1-abc")
                const testContent2 = new TextEncoder().encode("test-content-2-abcd")
                const testContent3 = new TextEncoder().encode("test-content-3-abcde")
                const testContent4 = new TextEncoder().encode("test-content-4-abcdef")

                try {
                    var startDate = new Date()

                    var c = new common(assert, dtos)
                    c.start()

                    await c.login(facade)

                    var ownerPermId = (await c.createSample(facade)).getPermId()
                    await c.deleteFile(facade, ownerPermId, "")

                    await facade.getAfsServerFacade().write(ownerPermId, "test-file-1", 0, testContent1)
                    await facade.getAfsServerFacade().write(ownerPermId, "/test-folder-1/test-file-2", 0, testContent2)
                    await facade.getAfsServerFacade().write(ownerPermId, "/test-folder-1/test-file-3", 0, testContent3)
                    await facade.getAfsServerFacade().write(ownerPermId, "/test-folder-2/test-file-4", 0, testContent4)

                    if (useTransaction) {
                        facade.setInteractiveSessionKey(testInteractiveSessionKey)
                        await facade.beginTransaction()
                    }

                    var listAll = await facade.getAfsServerFacade().list(ownerPermId, "", true)

                    if (useTransaction) {
                        await facade.commitTransaction()
                    }

                    listAll.sort((file1, file2) => {
                        return file1.getPath().localeCompare(file2.getPath())
                    })

                    c.assertEqual(listAll.length, 6, "Number of files")

                    c.assertFileEquals(listAll[0], {
                        path: "/test-file-1",
                        owner: ownerPermId,
                        name: "test-file-1",
                        size: testContent1.length,
                        directory: false,
                        lastModifiedTime: [startDate, new Date()],
                    })
                    c.assertFileEquals(listAll[1], {
                        path: "/test-folder-1",
                        owner: ownerPermId,
                        name: "test-folder-1",
                        size: null,
                        directory: true,
                        lastModifiedTime: [startDate, new Date()],
                    })
                    c.assertFileEquals(listAll[2], {
                        path: "/test-folder-1/test-file-2",
                        owner: ownerPermId,
                        name: "test-file-2",
                        size: testContent2.length,
                        directory: false,
                        lastModifiedTime: [startDate, new Date()],
                    })
                    c.assertFileEquals(listAll[3], {
                        path: "/test-folder-1/test-file-3",
                        owner: ownerPermId,
                        name: "test-file-3",
                        size: testContent3.length,
                        directory: false,
                        lastModifiedTime: [startDate, new Date()],
                    })
                    c.assertFileEquals(listAll[4], {
                        path: "/test-folder-2",
                        owner: ownerPermId,
                        name: "test-folder-2",
                        size: null,
                        directory: true,
                        lastModifiedTime: [startDate, new Date()],
                    })
                    c.assertFileEquals(listAll[5], {
                        path: "/test-folder-2/test-file-4",
                        owner: ownerPermId,
                        name: "test-file-4",
                        size: testContent4.length,
                        directory: false,
                        lastModifiedTime: [startDate, new Date()],
                    })

                    var listFile1WithoutSlash = await facade.getAfsServerFacade().list(ownerPermId, "test-file-1", false)

                    c.assertFileEquals(listFile1WithoutSlash[0], {
                        path: "/test-file-1",
                        owner: ownerPermId,
                        name: "test-file-1",
                        size: testContent1.length,
                        directory: false,
                        lastModifiedTime: [startDate, new Date()],
                    })

                    var listFile1WithSlash = await facade.getAfsServerFacade().list(ownerPermId, "/test-file-1", false)

                    c.assertFileEquals(listFile1WithSlash[0], {
                        path: "/test-file-1",
                        owner: ownerPermId,
                        name: "test-file-1",
                        size: testContent1.length,
                        directory: false,
                        lastModifiedTime: [startDate, new Date()],
                    })

                    var listFile2WithoutSlash = await facade.getAfsServerFacade().list(ownerPermId, "test-folder-1/test-file-2", false)

                    c.assertFileEquals(listFile2WithoutSlash[0], {
                        path: "/test-folder-1/test-file-2",
                        owner: ownerPermId,
                        name: "test-file-2",
                        size: testContent2.length,
                        directory: false,
                        lastModifiedTime: [startDate, new Date()],
                    })

                    var listFile2WithSlash = await facade.getAfsServerFacade().list(ownerPermId, "/test-folder-1/test-file-2", false)

                    c.assertFileEquals(listFile2WithSlash[0], {
                        path: "/test-folder-1/test-file-2",
                        owner: ownerPermId,
                        name: "test-file-2",
                        size: testContent2.length,
                        directory: false,
                        lastModifiedTime: [startDate, new Date()],
                    })

                    c.finish()
                } catch (error) {
                    c.fail(error)
                    c.finish()
                }
            }

            var testRead = async function (assert, useTransaction) {
                const testFile = "test-file"
                const testContent = new TextEncoder().encode("test-content")

                try {
                    var c = new common(assert, dtos)
                    c.start()

                    await c.login(facade)

                    var ownerPermId = (await c.createSample(facade)).getPermId()
                    await c.deleteFile(facade, ownerPermId, "")

                    await facade.getAfsServerFacade().write(ownerPermId, testFile, 0, testContent)

                    if (useTransaction) {
                        facade.setInteractiveSessionKey(testInteractiveSessionKey)
                        await facade.beginTransaction()
                    }

                    var content = await facade.getAfsServerFacade().read(ownerPermId, testFile, 0, testContent.length)
                    c.assertEqual(await content.text(), "test-content")

                    if (useTransaction) {
                        await facade.commitTransaction()
                    }

                    c.finish()
                } catch (error) {
                    c.fail(error)
                    c.finish()
                }
            }

            var testWrite = async function (assert, useTransaction) {
                const testFile = "test-file"
                const chunkSize = 250000 // max content length is configured to 512kB in AFS server and the file is 1MB

                function loadBinaryFile(url): Promise<Blob> {
                    return new Promise(function (resolve, reject) {
                        const req = new XMLHttpRequest()
                        req.open("GET", url, true)
                        req.responseType = "blob"
                        req.onload = () => {
                            resolve(req.response)
                        }
                        req.onerror = () => {
                            reject()
                        }
                        req.send(null)
                    })
                }

                function arrayBufferToString(buffer: ArrayBuffer, startIndex: number, finishIndex: number) {
                    var string = ""
                    var bytes = new Uint8Array(buffer)
                    for (var i = startIndex; i < finishIndex; i++) {
                        string += String.fromCharCode(bytes[i])
                    }
                    return string
                }

                try {
                    var c = new common(assert, dtos)
                    c.start()

                    var binaryFile = await loadBinaryFile("/openbis/webapp/openbis-v3-api-test/test/data/test-binary-file")
                    var binaryFileAsBuffer = await binaryFile.arrayBuffer()
                    var binaryFileAsByteArray = new Uint8Array(binaryFileAsBuffer)

                    await c.login(facade)

                    var ownerPermId = (await c.createSample(facade)).getPermId()
                    await c.deleteFile(facade, ownerPermId, "")

                    if (useTransaction) {
                        facade.setInteractiveSessionKey(testInteractiveSessionKey)
                        await facade.beginTransaction()
                    }

                    // write in chunks
                    var index = 0
                    while (index < binaryFileAsBuffer.byteLength) {
                       var chunk = binaryFileAsBuffer.slice(index, Math.min(index + chunkSize, binaryFileAsBuffer.byteLength));
                       let uint8Chunk = new Uint8Array(chunk)
                       await facade.getAfsServerFacade().write(ownerPermId, testFile, index, uint8Chunk)
                       index += chunkSize;
                    }

                    if (useTransaction) {
                        await facade.commitTransaction()
                    }

                    // read in chunks
                    var index = 0
                    var readFileBuffers = []
                    while (index < binaryFileAsBuffer.byteLength) {
                       var chunkBlob = await facade
                               .getAfsServerFacade()
                               .read(ownerPermId, testFile, index, Math.min(chunkSize, binaryFileAsBuffer.byteLength - index));

                       let chunkBuffer = await chunkBlob.arrayBuffer();
                       readFileBuffers.push(new Uint8Array(chunkBuffer));  // Store the binary data directly
                       index += chunkSize;
                    }

                    var readFileAsBuffer = await new Blob(readFileBuffers).arrayBuffer()
                    var readFileAsByteArray = new Uint8Array(readFileAsBuffer)

                    c.assertEqual(binaryFileAsByteArray.length, readFileAsByteArray.length, "Read file length")

                    for (var i = 0; i < binaryFileAsByteArray.length; i++) {
                        if (binaryFileAsByteArray[i] !== readFileAsByteArray[i]) {
                            c.fail("Original and read files are different")
                            break
                        }
                    }

                    c.finish()
                } catch (error) {
                    c.fail(error)
                    c.finish()
                }
            }

            var testDelete = async function (assert, useTransaction) {
                const testFile = "test-file"
                const testContent = new TextEncoder().encode("test-content")

                try {
                    var c = new common(assert, dtos)
                    c.start()

                    await c.login(facade)

                    var ownerPermId = (await c.createSample(facade)).getPermId()
                    await c.deleteFile(facade, ownerPermId, "")

                    await facade.getAfsServerFacade().write(ownerPermId, testFile, 0, testContent)

                    var content = await facade.getAfsServerFacade().read(ownerPermId, testFile, 0, testContent.length)
                    c.assertEqual(await content.text(), "test-content")

                    if (useTransaction) {
                        facade.setInteractiveSessionKey(testInteractiveSessionKey)
                        await facade.beginTransaction()
                    }

                    await facade.getAfsServerFacade().delete(ownerPermId, testFile)

                    if (useTransaction) {
                        await facade.commitTransaction()
                    }

                    await c.assertFileDoesNotExist(facade, ownerPermId, testFile)

                    c.finish()
                } catch (error) {
                    c.fail(error)
                    c.finish()
                }
            }

            var testCopy = async function (assert, useTransaction) {
                const testFileToCopy = "test-file-to-copy"
                const testFileCopied = "test-file-copied"
                const testContent = new TextEncoder().encode("test-content")

                try {
                    var c = new common(assert, dtos)
                    c.start()

                    await c.login(facade)

                    var ownerPermId = (await c.createSample(facade)).getPermId()
                    await c.deleteFile(facade, ownerPermId, "")

                    await facade.getAfsServerFacade().write(ownerPermId, testFileToCopy, 0, testContent)

                    if (useTransaction) {
                        facade.setInteractiveSessionKey(testInteractiveSessionKey)
                        await facade.beginTransaction()
                    }

                    await facade.getAfsServerFacade().copy(ownerPermId, testFileToCopy, ownerPermId, testFileCopied)

                    if (useTransaction) {
                        await facade.commitTransaction()
                    }

                    var contentToCopy = await facade.getAfsServerFacade().read(ownerPermId, testFileToCopy, 0, testContent.length)
                    c.assertEqual(await contentToCopy.text(), "test-content")

                    var contentCopied = await facade.getAfsServerFacade().read(ownerPermId, testFileCopied, 0, testContent.length)
                    c.assertEqual(await contentCopied.text(), "test-content")

                    c.finish()
                } catch (error) {
                    c.fail(error)
                    c.finish()
                }
            }

            var testMove = async function (assert, useTransaction) {
                const testFileToMove = "test-file-to-move"
                const testFileMoved = "test-file-moved"
                const testContent = new TextEncoder().encode("test-content")

                try {
                    var c = new common(assert, dtos)
                    c.start()

                    await c.login(facade)

                    var ownerPermId = (await c.createSample(facade)).getPermId()
                    await c.deleteFile(facade, ownerPermId, "")

                    await facade.getAfsServerFacade().write(ownerPermId, testFileToMove, 0, testContent)

                    if (useTransaction) {
                        facade.setInteractiveSessionKey(testInteractiveSessionKey)
                        await facade.beginTransaction()
                    }

                    await facade.getAfsServerFacade().move(ownerPermId, testFileToMove, ownerPermId, testFileMoved)

                    if (useTransaction) {
                        await facade.commitTransaction()
                    }

                    await c.assertFileDoesNotExist(facade, ownerPermId, testFileToMove)

                    var content = await facade.getAfsServerFacade().read(ownerPermId, testFileMoved, 0, testContent.length)
                    c.assertEqual(await content.text(), "test-content")

                    c.finish()
                } catch (error) {
                    c.fail(error)
                    c.finish()
                }
            }

            var testCreate = async function (assert, useTransaction) {
                const testFile = "test-file"

                try {
                    var c = new common(assert, dtos)
                    c.start()

                    await c.login(facade)

                    var ownerPermId = (await c.createSample(facade)).getPermId()
                    await c.deleteFile(facade, ownerPermId, "")
                    await c.assertFileDoesNotExist(facade, ownerPermId, testFile)

                    if (useTransaction) {
                        facade.setInteractiveSessionKey(testInteractiveSessionKey)
                        await facade.beginTransaction()
                    }

                    await facade.getAfsServerFacade().create(ownerPermId, testFile, false)

                    if (useTransaction) {
                        await facade.commitTransaction()
                    }

                    await c.assertFileExists(facade, ownerPermId, testFile)

                    c.finish()
                } catch (error) {
                    c.fail(error)
                    c.finish()
                }
            }

            var testFree = async function (assert, useTransaction) {
                const testFile = "test-file"
                const testContent = new TextEncoder().encode("test-content")

                try {
                    var c = new common(assert, dtos)
                    c.start()

                    await c.login(facade)

                    var ownerPermId = (await c.createSample(facade)).getPermId()
                    await c.deleteFile(facade, ownerPermId, "")

                    await facade.getAfsServerFacade().write(ownerPermId, testFile, 0, testContent)

                    if (useTransaction) {
                        facade.setInteractiveSessionKey(testInteractiveSessionKey)
                        await facade.beginTransaction()
                    }

                    var freeSpace = await facade.getAfsServerFacade().free(ownerPermId, testFile)

                    if (useTransaction) {
                        await facade.commitTransaction()
                    }

                    c.assertNotNull(freeSpace, "Free space not null")
                    c.assertTrue(freeSpace.getFree() > 0, "Free space > 0")
                    c.assertTrue(freeSpace.getTotal() > 0, "Total space > 0")
                    c.assertTrue(freeSpace.getFree() < freeSpace.getTotal(), "Free space < Total space")

                    c.finish()
                } catch (error) {
                    c.fail(error)
                    c.finish()
                }
            }

            var testHash = async function (assert, useTransaction) {
                const testFile = "test-file"
                const testContent = new TextEncoder().encode("test-content")

                try {
                    var c = new common(assert, dtos)
                    c.start()

                    await c.login(facade)

                    var ownerPermId = (await c.createSample(facade)).getPermId()
                    await c.deleteFile(facade, ownerPermId, "")

                    await facade.getAfsServerFacade().write(ownerPermId, testFile, 0, testContent)

                    if (useTransaction) {
                        facade.setInteractiveSessionKey(testInteractiveSessionKey)
                        await facade.beginTransaction()
                    }

                    var md5Checksum = await facade.getAfsServerFacade().hash(ownerPermId, testFile)
                    c.assertEqual("9749fad13d6e7092a6337c4af9d83764", md5Checksum)

                    await facade.getAfsServerFacade().delete(ownerPermId, testFile)

                    if (useTransaction) {
                        await facade.commitTransaction()
                    }

                    c.finish()
                } catch (error) {
                    c.fail(error)
                    c.finish()
                }
            }

            var testPreview = async function (assert, useTransaction) {
                var testFile = "minimal-image.png";
                var minimalImage = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABAQAAAAA3bvkkAAAACklEQVR4AWNgAAAAAgABc3UBGAAAAABJRU5ErkJggg==";
                var testContent = await fetch(minimalImage).then(res => res.blob()).then(blob => blob.arrayBuffer()).then(arrayBuffer => new Uint8Array(arrayBuffer));

                try {
                    var c = new common(assert, dtos)
                    c.start()

                    await c.login(facade)

                    var ownerPermId = (await c.createSample(facade)).getPermId()
                    await c.deleteFile(facade, ownerPermId, "")

                    await facade.getAfsServerFacade().write(ownerPermId, testFile, 0, testContent)

                    if (useTransaction) {
                        facade.setInteractiveSessionKey(testInteractiveSessionKey)
                        await facade.beginTransaction()
                    }

                    var previewImage = await facade.getAfsServerFacade().preview(ownerPermId, testFile);
                    var arrayBuffer = await previewImage.arrayBuffer();
                    var bytes = new Uint8Array(arrayBuffer);

                    c.assertTrue(bytes.length > 0)

                    await facade.getAfsServerFacade().delete(ownerPermId, testFile)

                    if (useTransaction) {
                        await facade.commitTransaction()
                    }

                    c.finish()
                } catch (error) {
                    c.fail(error)
                    c.finish()
                }
            }

            QUnit.test("list() without transaction", async function (assert) {
                await testList(assert, false)
            })

            QUnit.test("list() with transaction", async function (assert) {
                await testList(assert, true)
            })

            QUnit.test("read() without transaction", async function (assert) {
                await testRead(assert, false)
            })

            QUnit.test("read() with transaction", async function (assert) {
                await testRead(assert, true)
            })

            QUnit.test("read() original DSS data set", async function (assert) {
                const testContent = "Hello World!"

                try {
                    var c = new common(assert, dtos)
                    c.start()

                    await c.login(facade)

                    var dataSetPermId = await c.createDataSet(facade, "UNKNOWN", testContent)

                    var files = await facade.getAfsServerFacade().list(dataSetPermId.getPermId(), "", true)

                    files.sort((file1, file2) => {
                        return file1.getPath().localeCompare(file2.getPath())
                    })

                    c.assertEqual(files.length, 2, "Number of files")

                    c.assertFileEquals(files[0], {
                        path: "/original",
                        owner: dataSetPermId.getPermId(),
                        name: "original",
                        size: null,
                        directory: true,
                    })
                    c.assertFileEquals(files[1], {
                        path: "/original/test",
                        owner: dataSetPermId.getPermId(),
                        name: "test",
                        size: testContent.length,
                        directory: false,
                    })

                    var content = await facade.getAfsServerFacade().read(dataSetPermId.getPermId(), "/original/test", 0, testContent.length)

                    c.assertEqual(await content.text(), testContent)

                    c.finish()
                } catch (error) {
                    c.fail(error)
                    c.finish()
                }
            })

            QUnit.test("write() without transaction", async function (assert) {
                await testWrite(assert, false)
            })

            QUnit.test("write() with transaction", async function (assert) {
                await testWrite(assert, true)
            })

            QUnit.test("delete() without transaction", async function (assert) {
                await testDelete(assert, false)
            })

            QUnit.test("delete() with transaction", async function (assert) {
                await testDelete(assert, true)
            })

            QUnit.test("copy() without transaction", async function (assert) {
                await testCopy(assert, false)
            })

            QUnit.test("copy() with transaction", async function (assert) {
                await testCopy(assert, true)
            })

            QUnit.test("move() without transaction", async function (assert) {
                await testMove(assert, false)
            })

            QUnit.test("move() with transaction", async function (assert) {
                await testMove(assert, true)
            })

            QUnit.test("create() without transaction", async function (assert) {
                await testCreate(assert, false)
            })

            QUnit.test("create() with transaction", async function (assert) {
                await testCreate(assert, true)
            })

            QUnit.test("free() without transaction", async function (assert) {
                await testFree(assert, false)
            })

            QUnit.test("free() with transaction", async function (assert) {
                await testFree(assert, true)
            })

            QUnit.test("hash() without transaction", async function (assert) {
                await testHash(assert, false)
            })

            QUnit.test("hash() with transaction", async function (assert) {
                await testHash(assert, true)
            })

            QUnit.test("preview() without transaction", async function (assert) {
                await testPreview(assert, false)
            })

            QUnit.test("preview() with transaction", async function (assert) {
                await testPreview(assert, true)
            })
        }

        resolve(function () {
            var afsServerUrl = "http://localhost:8085/afs-server"
            executeModule("Afs tests (RequireJS)", new openbisRequireJS(null, afsServerUrl), dtos)
            executeModule("Afs tests (module VAR)", new window.openbis.openbis(null, afsServerUrl), window.openbis)
            executeModule("Afs tests (module ESM)", new window.openbisESM.openbis(null, afsServerUrl), window.openbisESM)
        })
    })
})
