import React from 'react'
import autoBind from 'auto-bind'
import ComponentContext from '@src/js/components/common/ComponentContext.js'
import PageWithTwoPanels from '@src/js/components/common/page/PageWithTwoPanels.jsx'
import GridContainer from '@src/js/components/common/grid/GridContainer.jsx'
import TrashcanGrid from '@src/js/components/tools/common/TrashcanGrid.jsx'
import TrashcanFormController from '@src/js/components/tools/form/trashcan/TrashcanFormController.js'
import TrashcanFormFacade from '@src/js/components/tools/form/trashcan/TrashcanFormFacade.js'
import TrashcanFormFormButtons from '@src/js/components/tools/form/trashcan/TrashcanFormFormButtons.jsx'
import PageMode from '@src/js/components/common/page/PageMode.js'
import ids from '@src/js/common/consts/ids.js'
import logger from '@src/js/common/logger.js'
import ConfirmationDialog from '@src/js/components/common/dialog/ConfirmationDialog.jsx'

class TrashcanForm extends React.PureComponent {
  constructor(props) {
    super(props)
    autoBind(this)

    this.state = {
      emptyTrashcanDialogOpen: false,
      operationDialogOpen: false,
      selectedOperation: null,
      selectedRow: null,
      operationLoading: false
    }

    if (this.props.controller) {
      this.controller = this.props.controller
    } else {
      this.controller = new TrashcanFormController(
        new TrashcanFormFacade()
      )
    }

    this.controller.init(new ComponentContext(this))
  }

  componentDidMount() {
    this.controller.load()
  }

  handleGridControllerRef(gridController) {
    this.controller.gridController = gridController
  }

  handleEmptyTrashcan() {
    this.controller.facade.emptyTrashcan(this.state.rows)
    this.setState({ loading: true, emptyTrashcanDialogOpen: false })
    setTimeout(() => {
      this.controller.load()
    }, 1000)
  }

  handleOperationSelect(event, row) {
    const selectedOperation = event.target.value
    if (!selectedOperation || !row) {
      return
    }

    this.setState({
      operationDialogOpen: true,
      selectedOperation,
      selectedRow: row
    })
  }

  handleOperationConfirm() {
    const { selectedOperation, selectedRow } = this.state
    this.setState({ operationLoading: true, operationDialogOpen: false })

    this.executeOperation(selectedOperation, selectedRow)
  }

  async executeOperation(operation, row) {
    try {
      switch (operation) {
        case 'revert':
          await this.controller.facade.revertDeletions(row)
          break
        case 'delete':
          await this.controller.facade.deletePermanently(row, false)
          break
        case 'deleteWithDependents':
          await this.controller.facade.deletePermanently(row, true)
          break
        default:
          console.log('Unknown operation:', operation)
      }
    } catch (error) {
      console.error('Operation failed:', error)
    } finally {
      this.setState({ operationLoading: false })
      this.controller.load()
    }
  }

  getOperationDescription(operation) {
    switch (operation) {
      case 'revert':
        return 'Revert Deletions'
      case 'delete':
        return 'Delete Permanently'
      case 'deleteWithDependents':
        return 'Delete Permanently (including dependent entries in trashcan)'
      default:
        return 'Unknown operation'
    }
  }

  render() {
    logger.log(logger.DEBUG, 'TrashcanForm.render')

    const { loadId, loading, loaded } = this.state
    console.log('TrashcanForm', this.state, this.props)
    return (
      <PageWithTwoPanels
        key={loadId}
        id={ids.TRASHCAN_FORM_ID}
        loading={loading}
        loaded={loaded}
        object={{}}
        renderMainPanel={() => this.renderMainPanel()}
        renderButtons={() => this.renderButtons()}
      />
    )
  }

  renderMainPanel() {
    const { rows, operationLoading } = this.state
    const { facade, load } = this.controller
    return (
      <GridContainer>
        <TrashcanGrid
          controllerRef={this.handleGridControllerRef}
          rows={rows}
          facade={facade}
          onReload={load}
          onOperationSelect={this.handleOperationSelect}
          operationLoading={operationLoading}
        />
      </GridContainer>
    )
  }

  renderButtons() {
    const { facade } = this.controller
    const { rows } = this.state
    const {
      emptyTrashcanDialogOpen,
      operationDialogOpen,
      selectedOperation,
      selectedRow,
      mode
    } = this.state


    const operationDescription = this.getOperationDescription(selectedOperation)

    const emptyTrashcanContent = (
      <>
        All entities in the trashcan will be deleted permanently. This action cannot be undone!
        <br /><br />
        Are you sure you want to continue?
      </>
    )

    const deleteEntityContent = (
      <>
        The selected entity in the trashcan will be {selectedOperation === 'revert' ? 'restored.' : 'deleted permanently. This action cannot be undone!'}
        <br /><br />
        Are you sure you want to continue?
      </>
    )


    return (<>
      <TrashcanFormFormButtons
        rows={rows}
        mode={PageMode.EDIT}
        onEmptyTrashcan={() => this.setState({ emptyTrashcanDialogOpen: true })}
      />
      <ConfirmationDialog
        open={emptyTrashcanDialogOpen || operationDialogOpen}
        onConfirm={emptyTrashcanDialogOpen ? this.handleEmptyTrashcan : this.handleOperationConfirm}
        onCancel={() => this.setState({
          emptyTrashcanDialogOpen: false,
          operationDialogOpen: false,
          selectedOperation: null,
          selectedRow: null
        })}
        title={emptyTrashcanDialogOpen ? "Empty Trashcan" : operationDescription}
         content={emptyTrashcanDialogOpen ? emptyTrashcanContent : deleteEntityContent}
        type='warning'
      />
    </>
    )
  }

}

export default TrashcanForm
