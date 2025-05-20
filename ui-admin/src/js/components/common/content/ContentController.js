import _ from 'lodash'
import autoBind from 'auto-bind'
import ComponentController from '@src/js/components/common/ComponentController.js'
import util from '@src/js/common/util.js'

export default class ContentController extends ComponentController {

    constructor() {
        super()
        autoBind(this)

        const controller = this
    }

}