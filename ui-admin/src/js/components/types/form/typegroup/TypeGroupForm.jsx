import autoBind from 'auto-bind'
import React from 'react'
import ComponentContext from '@src/js/components/common/ComponentContext.js'
import TypeGroupFormController from '@src/js/components/types/form/typegroup/TypeGroupFormController.js'
import TypeGroupFormFacade from '@src/js/components/types/form/typegroup/TypeGroupFormFacade.js'
import logger from '@src/js/common/logger.js'
import PageWithTwoPanels from '@src/js/components/common/page/PageWithTwoPanels.jsx'
import TypeGroupFormParameters from '@src/js/components/types/form/typegroup/TypeGroupFormParameters.jsx'
import ids from '@src/js/common/consts/ids.js'
import TypeGroupFormButtons from '@src/js/components/types/form/typegroup/TypeGroupFormButtons.jsx'
import TypeGroupFormSelectionType from '@src/js/components/types/form/typegroup/TypeGroupFormSelectionType.js'
import GridContainer from '@src/js/components/common/grid/GridContainer.jsx'
import GridWithOpenbis from '@src/js/components/common/grid/GridWithOpenbis.jsx'
import GridExportOptions from '@src/js/components/common/grid/GridExportOptions.js'
import messages from '@src/js/common/messages.js'
import GridUtil from '@src/js/components/common/grid/GridUtil.js'
import LockLabel from '@src/js/components/common/form/LockLabel.jsx'

const columns = [
  {
    name: 'code',
    label: messages.get(messages.CODE),
    getValue: ({ row }) => row.code.value
  },
  {
    name: 'Internal',
    label: messages.get(messages.INTERNAL),
    getValue: ({ row }) => {
      return row.internal.value;
    },
    renderValue: ({ value }) => {
      if(value) {
          return <LockLabel fontSize='small' color='disabled' />
      }
      return null;
    }
  },
  {
    name: 'description',
    label: messages.get(messages.DESCRIPTION),
    getValue: ({ row }) => row.description.value
  },
  GridUtil.registratorColumn({ path: 'registrator.value' }),
  GridUtil.registrationDateColumn({ path: 'registrationDate.value' })
]

class TypeGroupForm extends React.PureComponent {
  constructor(props) {
    super(props)
    autoBind(this)

    this.state = {}

    if (this.props.controller) {
      this.controller = this.props.controller
    } else {
      this.controller = new TypeGroupFormController(
        new TypeGroupFormFacade()
      )
    }

    this.controller.init(new ComponentContext(this))
  }

  componentDidMount() {
    this.controller.load()
  }


  handleClickContainer() {
    this.controller.handleSelectionChange()
  }

  handleSelectedRowChange(row) {
    const { controller } = this
    if (row) {
      controller.handleSelectionChange(TypeGroupFormSelectionType.OBJECT_TYPE, {
        id: row.id
      })
    } else {
      controller.handleSelectionChange()
    }
  }

  handleGridControllerRef(gridController) {
    this.controller.gridController = gridController
  }

  render() {
    logger.log(logger.DEBUG, 'TypeGroupForm.render')

    const { loadId, loading, loaded, typeGroup } = this.state

    return (
      <PageWithTwoPanels
        id={ids.OBJECT_TYPE_GROUP_FORM_ID}
        key={loadId}
        loading={loading}
        loaded={loaded}
        object={typeGroup}
        renderMainPanel={() => this.renderMainPanel()}
        renderAdditionalPanel={() => this.renderAdditionalPanel()}
        renderButtons={() => this.renderButtons()}
      />
    )
  }

  renderMainPanel() {
    const { objectTypes, selection } = this.state
    const id = ids.TYPE_GROUP_OBJECT_TYPES_GRID_ID

    return (
      <GridContainer onClick={this.handleClickContainer}>
        <GridWithOpenbis
          id={id}
          settingsId={id}
          controllerRef={this.handleGridControllerRef}
          header={messages.get(messages.OBJECT_TYPE_GROUP_ASSIGNMENTS)}
          columns={columns}
          rows={objectTypes}
          sort='code'
          exportable={{
            fileFormat: GridExportOptions.FILE_FORMAT.TSV,
            filePrefix: 'type-group-object-types'
          }}
          selectable={true}
          selectedRowId={
            selection && selection.type === TypeGroupFormSelectionType.OBJECT_TYPE
              ? selection.params.id
              : null
          }
          onSelectedRowChange={this.handleSelectedRowChange}
        />
      </GridContainer>
    )
  }

  renderAdditionalPanel() {
    const { controller } = this
    const { typeGroup, objectTypes, selection, selectedRow, mode } = this.state

    return (
      <TypeGroupFormParameters
        controller={controller}
        typeGroup={typeGroup}
        objectTypes={objectTypes}
        selection={selection}
        selectedRow={selectedRow}
        mode={mode}
        onChange={controller.handleChange}
        onSelectionChange={controller.handleSelectionChange}
        onBlur={controller.handleBlur}
      />
    )
  }

  renderButtons() {
    const { controller } = this
    const { typeGroup, objectTypes, selection, changed, mode } = this.state

    return (
      <TypeGroupFormButtons
        onEdit={controller.handleEdit}
        onSave={controller.handleSave}
        onCancel={controller.handleCancel}
        onAdd={controller.handleAdd}
        onRemove={controller.handleRemove}
        typeGroup={typeGroup}
        objectTypes={objectTypes}
        selection={selection}
        changed={changed}
        mode={mode}
      />
    )
  }
}

export default TypeGroupForm	