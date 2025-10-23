import _ from 'lodash'
import openbis from '@src/js/services/openbis.js'
import FormUtil from '@src/js/components/common/form/FormUtil.js'

export default class TrashcanFormControllerLoad {
	constructor(controller) {
		this.controller = controller
		this.context = controller.context
		this.facade = controller.facade
	}

	async execute() {
		return Promise.all([this._loadTrashcan()])
	}

	async _loadTrashcan() {
		const deletions = await this.facade.loadDeletions()
		console.log('TrashcanFormControllerLoad._loadTrashcan.deletions', deletions)

		const rows = deletions.map(deletion => {
			return this._createRowDeletion(deletion)
		})

		console.log('loadTrashcan.rows', rows)
		return this.context.setState({
			rows,
			totalCount: deletions.length,
			loadId: _.uniqueId('load'),
			loaded: true,
			loading: false
		})
	}

	_createRowDeletion(deletion) {
		const experiments = this._formatDeletedObjectsByEntityKind(deletion.deletedObjects, openbis.EntityKind.EXPERIMENT,)
		const samples = this._formatDeletedObjectsByEntityKind(deletion.deletedObjects, openbis.EntityKind.SAMPLE,)
		const datasets = this._formatDeletedObjectsByEntityKind(deletion.deletedObjects, openbis.EntityKind.DATA_SET,)

		return {
			id: _.get(deletion, 'id.techId', null),
			deletionDate: FormUtil.createField({
				value: _.get(deletion, 'deletionDate', null)
			}),
			deletedObjects: FormUtil.createField({
				value: { experiments, samples, datasets, count: experiments.length + samples.length + datasets.length }
			}),
			reason: FormUtil.createField({
				value: _.get(deletion, 'reason', null)
			}),
			operations: FormUtil.createField({})
		}
	}

	_formatDeletedObjectsByEntityKind(deletedObjects, entityKind) {
		if (!deletedObjects || !Array.isArray(deletedObjects)) {
			return []
		}

		return deletedObjects
			.filter(deletedObject => deletedObject.entityKind === entityKind)
			.map(deletedObject => `${deletedObject.identifier} (${deletedObject.entityTypeCode})`)
	}
}
