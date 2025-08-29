import PageController from '@src/js/components/common/page/PageController.js'
import TypeGroupFormControllerLoad from '@src/js/components/types/form/typegroup/TypeGroupFormControllerLoad.js'
import TypeGroupFormControllerValidate from '@src/js/components/types/form/typegroup/TypeGroupFormControllerValidate.js'
import TypeGroupFormControllerAdd from '@src/js/components/types/form/typegroup/TypeGroupFormControllerAdd.js'
import TypeGroupFormControllerRemove from '@src/js/components/types/form/typegroup/TypeGroupFormControllerRemove.js'
import TypeGroupFormControllerChange from '@src/js/components/types/form/typegroup/TypeGroupFormControllerChange.js'
import TypeGroupFormControllerSelectionChange from '@src/js/components/types/form/typegroup/TypeGroupFormControllerSelectionChange.js'
import TypeGroupFormControllerSave from '@src/js/components/types/form/typegroup/TypeGroupFormControllerSave.js'
import pages from '@src/js/common/consts/pages.js'
import objectTypes from '@src/js/common/consts/objectType.js'

export default class TypeGroupFormController extends PageController {
	constructor(facade) {
		super(facade)
	}

	getPage() {
		return pages.TYPES
	}

	getNewObjectType() {
		return objectTypes.NEW_OBJECT_TYPE_GROUP
	}

	getExistingObjectType() {
		return objectTypes.OBJECT_TYPE_GROUP
	}

	load() {
		return new TypeGroupFormControllerLoad(this).execute()
	}

	validate(autofocus) {
		return new TypeGroupFormControllerValidate(this).execute(autofocus)
	}

	handleAdd() {
		return new TypeGroupFormControllerAdd(this).execute()
	}

	handleRemove() {
		return new TypeGroupFormControllerRemove(this).execute()
	}

	handleChange(type, params) {
		return new TypeGroupFormControllerChange(this).execute(type, params)
	}

	handleSelectionChange(type, params) {
		return new TypeGroupFormControllerSelectionChange(this).execute(
			type,
			params
		)
	}

	handleSave() {
		return new TypeGroupFormControllerSave(this).execute()
	}
}
