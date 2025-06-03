import _ from 'lodash'
import React from 'react'
import autoBind from 'auto-bind'
import withStyles from '@mui/styles/withStyles';
import ErrorBoundary from '@src/js/components/common/error/ErrorBoundary.jsx'
import ContentTabs from '@src/js/components/common/content/ContentTabs.jsx'
import ContentTab from '@src/js/components/common/content/ContentTab.jsx'
import util from '@src/js/common/util.js'
import logger from '@src/js/common/logger.js'
import Tabs from '@mui/material/Tabs'
import Tab from '@mui/material/Tab'
import CloseIcon from '@mui/icons-material/Close'
import UnsavedChangesDialog from '@src/js/components/common/dialog/UnsavedChangesDialog.jsx'

const styles = {
  container: {
    display: 'flex',
    flexDirection: 'column',
    flex: 1,
    zIndex: 200,
    overflow: 'auto',
  },
  component: {
    height: 0,
    flex: '1 1 100%',
    overflow: 'auto'
  },
  visible: {
    display: 'block'
  },
  hidden: {
    display: 'none'
  },
  tabsRoot: {
      borderBottomStyle: 'solid',
      borderBottomWidth: '1px',
      borderBottomColor: 'white',
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
}

class SimpleContent extends React.PureComponent {



  constructor(props) {
    super(props)
    autoBind(this)

    let tabs = [];
    tabs = props.openTabs;
    let selected = props.selectedTab;

    if(props.controller) {
        props.controller.attach(this)
        const {openTabs, selectedTab} = props.controller.state;
        tabs = openTabs;
        selected = selectedTab;
    }

  this.setState({
        openTabs: tabs,
        selectedTab: selected,
        page: props.page,
        loaded: false,
      })

  }

  componentDidMount () {
         this.setState({ loaded: true });
  }

  handleTabSelect(tab) {
      this.setState({
              selectedTab: x => x.id === tab.id,
              loaded: true,
          })
      if(this.props.handleTabSelect) {
         this.props.handleTabSelect(tab);
      }
  }

  handleTabClose(tab) {
      let tabs = this.state.openTabs;
      const selectedIndex = parseInt(_.findIndex(tabs, x => x.id === tab.id))
      const currentIndex = parseInt(_.findIndex(tabs, this.state.selectedTab))
      let value = currentIndex;
      if(selectedIndex <= currentIndex && currentIndex !== 0) {
          value = currentIndex-1;
      }


      tabs = tabs.filter(x => x.id !== tab.id);

      let selectedTabFunction = x => false;
      let newSelectedTab = null;
      if(tabs.length > 0) {
        selectedTabFunction = x => x.id === tabs[value].id;
        newSelectedTab = tabs[value];
      }
      this.setState({
          openTabs: tabs,
          selectedTab: selectedTabFunction,
        })
      if(this.props.handleTabClose) {
        this.props.handleTabClose(tab, newSelectedTab);
      }

  }

  renderTab(tab) {
      let label = tab.label;
      let changed = tab.changed;
      let icon = tab.icon ? tab.icon : '';

      return <ContentTab label={label} changed={changed} icon={icon} />
    }


    render() {
        logger.log(logger.DEBUG, 'Content.render')

        const { classes } = this.props;
        const _this = this;

        const { openTabs, selectedTab, loaded, page } = this.state;

        let tabs = openTabs;
        if(!tabs) {
            tabs = []
        }

        var value = -1;
        if (selectedTab) {
          value = _.findIndex(tabs, selectedTab)
        }


        return (<div className={classes.container}>
                    <ContentTabs
                      tabs={tabs}
                      selectedTab={selectedTab}
                      tabSelect={_this.handleTabSelect}
                      tabClose={_this.handleTabClose}
                      renderTab={_this.renderTab}
                      sx={_this.props.style}
                    />
                    { tabs.map((openTab, index ) => {
                      let ObjectComponent = _this.props.controller.renderComponent(openTab.id)
                      if (ObjectComponent) {
                        let visible = index === value;
                        return (
                          <div
                            key={openTab.id}
                            className={util.classNames(
                              classes.component,
                              visible ? classes.visible : classes.hidden
                            )}
                          >
                             <ErrorBoundary>{ObjectComponent}</ErrorBoundary>
                          </div>
                        )
                      }
                    })}
                  </div>
                )
    }
}





export default withStyles(styles)(SimpleContent)
