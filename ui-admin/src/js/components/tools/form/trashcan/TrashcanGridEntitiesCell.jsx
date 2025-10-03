import React from 'react'
import withStyles from '@mui/styles/withStyles';
import Collapse from '@mui/material/Collapse'
import Link from '@mui/material/Link'
import openbis from '@src/js/services/openbis.js'
import messages from '@src/js/common/messages.js'
import logger from '@src/js/common/logger.js'

const styles = theme => ({
  entities: {
    padding: 0,
    margin: 0,
    marginTop: theme.spacing(1)
  },
  entity: {
    listStyle: 'none',
    margin: 0,
    padding: 0
  }
})

class TrashcanGridEntitiesCell extends React.PureComponent {
  constructor(props) {
    super(props)
    this.state = {
      visible: false
    }
    this.handleVisibilityChange = this.handleVisibilityChange.bind(this)
  }

  handleVisibilityChange() {
    this.setState(state => ({
      visible: !state.visible
    }))
  }

  render() {
    logger.log(logger.DEBUG, 'TrashcanGridEntitiesCell.render')

    const { value } = this.props
    const { visible } = this.state
    console.log('TrashcanGridEntitiesCell.value', value)
    if (value) {
      return (
        <div>
          <div>
            {value.count} (
            <Link
            underline='none'
              onClick={() => {
                this.handleVisibilityChange()
              }}
            >
              {visible
                ? messages.get(messages.HIDE)
                : messages.get(messages.SHOW)}
            </Link>
            )
          </div>
          <Collapse in={visible} mountOnEnter={true} unmountOnExit={true}>
            <div>
              {this.renderEntities(
                messages.get(messages.COLLECTIONS),
                value.experiments
              )}
              {this.renderEntities(
                messages.get(messages.OBJECTS),
                value.samples
              )}
              {this.renderEntities(
                messages.get(messages.DATA_SETS),
                value.datasets
              )}
            </div>
          </Collapse>
        </div>
      )
    } else {
      return 0
    }
  }

  renderEntities(entitiesHeader, entitiesList) {
    if (entitiesList.length === 0) {
      return null
    }

    const { classes } = this.props

    return (
      <ul className={classes.entities}>
        {entitiesHeader}:
        {entitiesList.map((entity, index) => (
          <li key={`${entitiesHeader}-${index}`} className={classes.entity}>
            {entity}
          </li>
        ))}
      </ul>
    )
  }
}

export default withStyles(styles)(TrashcanGridEntitiesCell)
