import openbis from "./test/types/openbis.esm"

declare global {
    interface Window {
        openbis: openbis.bundle
        openbisESM: openbis.bundle
    }

    interface QUnit {
        module: (moduleName: string) => void
        test: (name: string, callback: (assert: any) => void) => void
    }

    var QUnit: QUnit
    var exports: any

    function define(names: string[], callback: (...modules: any) => void): void
    function require(names: string[], callback: (...modules: any) => void): void
}

export {}
