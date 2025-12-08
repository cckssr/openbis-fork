import React from 'react'
import withStyles from '@mui/styles/withStyles';
import Content from '@src/js/components/common/content/Content.jsx'
import DatabaseBrowser from '@src/js/components/database/browser/DatabaseBrowser.jsx'
import DatabaseTab from '@src/js/components/database/DatabaseTab.jsx'
import DatabaseComponent from '@src/js/components/database/DatabaseComponent.jsx'
import pages from '@src/js/common/consts/pages.js'
import logger from '@src/js/common/logger.js' 
import ContentTab from '@src/js/components/common/content/ContentTab.jsx'
import objectType from '@src/js/common/consts/objectType.js'

const styles = () => ({
  container: {
    display: 'flex',
    width: '100%'
  },
  component: {
    height: 0,
    flex: '1 1 100%',
    overflow: 'hidden'
  },
})

class Database extends React.PureComponent {
  render() {
    logger.log(logger.DEBUG, 'Database.render')

    const classes = this.props.classes
    console.log( 'Database.render: ', this.props );
    return (
      <div className={classes.container}>
        <DatabaseBrowser />
        <Content
          classes={{component: classes.component}}
          page={pages.DATABASE}
          renderComponent={this.renderComponent}
          renderTab={this.renderTab}
        />
      </div>
    )
  }

  renderComponent(tab) {
    return <DatabaseComponent object={tab.object} />
  }

  renderTab(tab) {
    return <DatabaseTab tab={tab} />
  }
}

export default withStyles(styles)(Database)
