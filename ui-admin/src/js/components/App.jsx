import React from 'react'
import _ from 'lodash'
import autoBind from 'auto-bind'
import withStyles from '@mui/styles/withStyles';
import logger from '@src/js/common/logger.js'
import util from '@src/js/common/util.js'
import pages from '@src/js/common/consts/pages.js'
import messages from '@src/js/common/messages.js'

import Loading from '@src/js/components/common/loading/Loading.jsx'
import Error from '@src/js/components/common/error/Error.jsx'
import Menu from '@src/js/components/common/menu/Menu.jsx'

import Login from '@src/js/components/login/Login.jsx'
import Database from '@src/js/components/database/Database.jsx'
import Users from '@src/js/components/users/Users.jsx'
import Types from '@src/js/components/types/Types.jsx'
import Tools from '@src/js/components/tools/Tools.jsx'

import AppController from '@src/js/components/AppController.js'
import ComponentContext from '@src/js/components/common/ComponentContext.js'

import { library } from '@fortawesome/fontawesome-svg-core'
import { fab } from '@fortawesome/free-brands-svg-icons'
import { faFolder, faFile, faFileArchive, faFileAudio, faFileImage, faFileText,
  faFileVideo, faFileCode, faFilePdf, faFileWord, faFileExcel,
  faFilePowerpoint } from '@fortawesome/free-regular-svg-icons'

import LogoutIcon from '@mui/icons-material/PowerSettingsNew'

library.add(fab, faFolder, faFile, faFileAudio, faFileText, faFileVideo,
  faFileCode, faFileImage, faFileArchive, faFilePdf, faFileWord, faFileExcel,
  faFilePowerpoint)

const styles = {
  container: {
    height: '100%',
    display: 'flex',
    flexDirection: 'column'
  },
  page: {
    flex: '1 1 100%',
    display: 'flex',
    overflow: 'hidden'
  },
  visible: {
    display: 'flex'
  },
  hidden: {
    display: 'none'
  }
}

const pageToComponent = {
  [pages.DATABASE]: Database,
  [pages.TYPES]: Types,
  [pages.USERS]: Users,
  [pages.TOOLS]: Tools
}

const tabs = [
        {page: pages.DATABASE, label: messages.get(messages.DATABASE)},
        {page: pages.TYPES, label: messages.get(messages.TYPES)},
        {page: pages.USERS, label: messages.get(messages.USERS)},
        {page: pages.TOOLS, label: messages.get(messages.TOOLS)}
    ]


class App extends React.Component {
  constructor(props) {
    super(props)
    autoBind(this)

    this.state = {}

    if (this.props.controller) {
      this.controller = this.props.controller
    } else {
      this.controller = AppController.getInstance()
    }

    this.controller.init(new ComponentContext(this))
  }

  componentDidMount() {
    this.controller.load()
  }

  handleErrorClosed() {
    AppController.getInstance().errorChange(null)
  }

  render() {
    logger.log(logger.DEBUG, 'App.render')

    return (
      <AppController.AppContext.Provider value={this.state}>
        <Loading loading={AppController.getInstance().getLoading()}>
          <Error
            error={AppController.getInstance().getError()}
            errorClosed={this.handleErrorClosed}
          >
            {AppController.getInstance().getLoaded() && this.renderPage()}
          </Error>
        </Loading>
      </AppController.AppContext.Provider>
    )
  }

  handlePageChange(event, value) {
    AppController.getInstance().pageChange(value)
  }

  searchFunction(page, searchText) {
    AppController.getInstance().search(
      page,
      searchText
    )
  }

  handleLogout() {
    AppController.getInstance().logout()
  }

  userNameLoginFunction(userName, password) {
      AppController.getInstance().login(
          userName,
          password
     )
  }

  renderPage() {
    const classes = this.props.classes
    let menuStyles = {
        searchField: {
            width: '200px',
            transition: "width 0.3s",
            '&:focus-within': {
                  width: '300px',
                },
            '&:hover': {
                width: '300px',
                },
        }
    }
    if (AppController.getInstance().getSession()) {
      return (
        <div className={classes.container}>
          <Menu userName={AppController.getInstance().getSession().userName}
                tabs={tabs}
                pageChangeFunction={this.handlePageChange}
                searchFunction={this.searchFunction}
                logoutFunction={this.handleLogout}
                currentPage={AppController.getInstance().getCurrentPage()}
                searchText={AppController.getInstance().getSearch()}
                menuStyles={menuStyles}
                />
          {_.map(pageToComponent, (PageComponent, page) => {
            let visible = AppController.getInstance().getCurrentPage() === page
            return (
              <div
                key={page}
                className={util.classNames(
                  classes.page,
                  visible ? classes.visible : classes.hidden
                )}
              >
                <PageComponent />
              </div>
            )
          })}
        </div>
      )
    } else {
      return <Login disabled={AppController.getInstance().getLoading()}
                    title={'Admin Dashboard'}
                    loginFunction={this.userNameLoginFunction}

                    />
    }
  }
}

export default _.flow(
  withStyles(styles),
  AppController.getInstance().withState()
)(App)
