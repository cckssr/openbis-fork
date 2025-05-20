import _ from 'lodash'
import React from 'react'
import withStyles from '@mui/styles/withStyles';
import Tabs from '@mui/material/Tabs'
import Tab from '@mui/material/Tab'
import CloseIcon from '@mui/icons-material/Close'
import UnsavedChangesDialog from '@src/js/components/common/dialog/UnsavedChangesDialog.jsx'
import logger from '@src/js/common/logger.js'

const styles = theme => ({
  tabsRoot: {
    borderBottomStyle: 'solid',
    borderBottomWidth: '1px',
    borderBottomColor: theme.palette.border.primary,
  },
  tabRoot: {
    textTransform: 'none',
    minHeight: '38px',
    maxInlineSize: 'fit-content'
  },
  iconRoot: {
    marginLeft: '6px'
  },
  tabLabel: {
    display: 'inline-flex',
    alignItems: 'center'
  }
})

class ContentTabs extends React.PureComponent {
  constructor(props) {
    super(props)
    this.state = {
      tabToClose: null,
      unsavedChangesDialogOpen: false
    }
  }

  handleTabChange = (event, value) => {
    const tab = this.props.tabs[value]
    this.props.tabSelect(tab)
  }

  handleTabClose = (event, tab) => {
    if (tab.changed) {
      this.setState({
        tabToClose: tab,
        unsavedChangesDialogOpen: true
      })
    } else {
      this.props.tabClose(tab)
    }
    event.stopPropagation()
  }

  handleTabCloseConfirm = () => {
    const { tabToClose } = this.state
    if (tabToClose) {
      this.props.tabClose(tabToClose)
      this.setState({
        tabToClose: null,
        unsavedChangesDialogOpen: false
      })
    }
  }

  handleTabCloseCancel = () => {
    this.setState({
      tabToClose: null,
      unsavedChangesDialogOpen: false
    })
  }

  render() {
    logger.log(logger.DEBUG, 'ContentTabs.render')

    const { tabs, selectedTab, classes } = this.props

    let value = false

    if (selectedTab) {
      const selectedIndex = _.findIndex(tabs, selectedTab)
      if (selectedIndex !== -1) {
        value = selectedIndex
      }
    }

    const { unsavedChangesDialogOpen } = this.state

    return (
      (<React.Fragment>
        <Tabs
          value={value}
          variant='scrollable'
          scrollButtons='auto'
          allowScrollButtonsMobile={true}
          onChange={this.handleTabChange}
          classes={{ root: classes.tabsRoot }}
          textColor='inherit'
          indicatorColor='secondary'
          TabIndicatorProps={{
            style: {
              transition: 'none',
            }
          }}
          sx={this.props.sx}
            >
          {this.props.tabs.map(tab => (
            <Tab
              key={tab.id}
              label={this.renderLabel(tab)}
              classes={{
                root: classes.tabRoot
              }}
            />
          ))}
        </Tabs>
        <UnsavedChangesDialog
          open={unsavedChangesDialogOpen}
          onConfirm={this.handleTabCloseConfirm}
          onCancel={this.handleTabCloseCancel}
        />
      </React.Fragment>)
    );
  }

  renderLabel(tab) {
    return (
      <span className={this.props.classes.tabLabel}>
        {this.props.renderTab(tab)}
        {this.renderIcon(tab)}
      </span>
    )
  }

  renderIcon(tab) {
    return (
      <CloseIcon
        onClick={event => this.handleTabClose(event, tab)}
        classes={{
          root: this.props.classes.iconRoot
        }}
        fontSize='small'
      />
    )
  }
}

export default withStyles(styles)(ContentTabs)
