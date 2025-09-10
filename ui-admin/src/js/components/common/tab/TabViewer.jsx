import React from 'react'
import withStyles from '@mui/styles/withStyles';
import { Tab } from '@mui/material';
import { TabContext, TabList, TabPanel } from '@mui/lab';

const styles = theme => ({
  tabsRoot: {
    borderBottomStyle: 'solid',
    borderBottomWidth: '1px',
    borderBottomColor: theme.palette.border.primary,
  },
  tabPanelRoot: {
    padding: 0,
  }
})

class TabViewer extends React.PureComponent {
  constructor(props) {
    super(props)
    
    this.state = {
      tabSelected: (props.defaultTab || 0).toString()
    }
  }

  handleTabChange = (event, newValue) => {
    this.setState({
      tabSelected: newValue,
    })
    
    if (this.props.onTabChange) {
      this.props.onTabChange(newValue)
    }
  }

  render() {
    const { classes, tabs, children } = this.props
    const { tabSelected } = this.state

    return (
      <TabContext value={tabSelected}>
        <TabList
          variant='fullWidth'
          onChange={this.handleTabChange}
          classes={{ root: classes.tabsRoot }}
          textColor='inherit'
          indicatorColor='secondary'
          slotProps={{
            indicator: {
              style: {
                transition: 'none',
              }
            }
          }}
        >
          {tabs.map((tab, index) => (
            <Tab
              key={tab.key || `tab-${index}`}
              value={index.toString()}
              label={tab.label}
              sx={{ textTransform: 'none', padding: 'unset' }}
            />
          ))}
        </TabList>
        {children.map((child, index) => (
          <TabPanel 
            key={index}
            classes={{ root: classes.tabPanelRoot }} 
            value={index.toString()}
          >
            {child}
          </TabPanel>
        ))}
      </TabContext>
    )
  }
}

export default withStyles(styles)(TabViewer)
