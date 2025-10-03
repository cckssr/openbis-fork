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
import AppController from '@src/js/components/AppController.js'

class TrashcanForm extends React.PureComponent {
  constructor(props) {
    super(props)
    autoBind(this)

    this.state = {}

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

  render() {
    logger.log(logger.DEBUG, 'TrashcanForm.render')

    const { loadId, loading, loaded } = this.state

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
    const { rows } = this.state
    return (
      <GridContainer>
        <TrashcanGrid
          controllerRef={this.handleGridControllerRef}
          rows={rows}
          facade={this.controller.facade}
          onReload={this.controller.load}
        />
      </GridContainer>
    )
  }

  renderButtons() {
    const { controller } = this
    const { rows } = this.state

    return (
      <TrashcanFormFormButtons
        rows={rows}
        mode={PageMode.EDIT}
        onEmptyTrashcan={controller.handleEmptyTrashcan}
      />
    )
  }

}

export default AppController.getInstance().withState(() => ({
  lastObjectModifications:
    AppController.getInstance().getLastObjectModifications()
}))(TrashcanForm)
