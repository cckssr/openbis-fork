import _ from 'lodash'
import autoBind from 'auto-bind'
import React from 'react'
import AppBar from '@mui/material/AppBar'
import Toolbar from '@mui/material/Toolbar'
import Tabs from '@mui/material/Tabs'
import Tab from '@mui/material/Tab'
import TextField from '@mui/material/TextField'
import InputAdornment from '@mui/material/InputAdornment'

import IconButton from '@mui/material/IconButton'

import SvgIcon from '@mui/material/SvgIcon';
import Logo from '@src/resources/img/openbis-logo-transparent-white.png'

import KeyboardArrowDownIcon from '@mui/icons-material/KeyboardArrowDown';
import { Menu as DropdownMenu } from '@mui/material';
import MenuItem from '@mui/material/MenuItem';
import ListItemIcon from '@mui/material/ListItemIcon';
import Check from '@mui/icons-material/Check';

import SearchIcon from '@mui/icons-material/Search'
import CloseIcon from '@mui/icons-material/Close'
import LogoutIcon from '@mui/icons-material/PowerSettingsNew'
import { alpha } from '@mui/material/styles';
import withStyles from '@mui/styles/withStyles';
import AppController from '@src/js/components/AppController.js'
import Button from '@src/js/components/common/form/Button.jsx'
import pages from '@src/js/common/consts/pages.js'
import messages from '@src/js/common/messages.js'
import logger from '@src/js/common/logger.js'
import Typography from "@mui/material/Typography";

import { faBarcode } from '@fortawesome/free-solid-svg-icons'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'

const styles = theme => ({
  appBar: {
    position: 'relative',
    zIndex: 400
  },
  toolBar: {
    paddingLeft: theme.spacing(2),
    paddingRight: theme.spacing(2),
    backgroundColor: '#3f51b5'
  },
  tabs: {
    flexGrow: 1,
    textColor: theme.palette.background.secondary
  },
  search: {
    color: theme.palette.background.paper,
    backgroundColor: alpha(theme.palette.background.paper, 0.15),
    '&:hover': {
      backgroundColor: alpha(theme.palette.background.paper, 0.25)
    },
    borderRadius: theme.shape.borderRadius,
    paddingLeft: theme.spacing(1),
    paddingRight: theme.spacing(1),
    marginRight: theme.spacing(1),
    fontSize: '14px'
  },
  searchIcon: {
    paddingLeft: theme.spacing(1) / 2,
    paddingRight: theme.spacing(1),
    cursor: 'default',
    color:'white'
  },
  searchClear: {
    cursor: 'pointer'
  },
  userInfo: {
    margin: '0px 10px 0px 5px'
  },
  button: {
      marginRight: theme.spacing(1),
      backgroundColor: '#3f51b5',
      color: 'white',
    '&:hover': {
      backgroundColor: alpha(theme.palette.background.paper, 0.15)
    }
  },
  logo: {
    marginRight: theme.spacing(1),
    width:'80px',
    height:'35px'
  }
})

class Menu extends React.PureComponent {
  constructor(props) {
    super(props)
    autoBind(this)
    this.searchRef = React.createRef()

    let searchDomain = null;
    if(this.props.searchDomains && this.props.searchDomains.length > 0) {
        searchDomain = this.props.searchDomains[0].label;
    }

    this.state = {
        currentPage: this.props.currentPage,
        searchText: this.props.searchText || '',
        searchDomain: searchDomain,
        anchorEl: null,
        menuOpen: false,
        selectedIndex: 0,
    }

  }

  getSearchDomainLabel() {
    if(this.props.searchDomains && this.props.searchDomains.length > 0) {
            return this.props.searchDomains[this.state.selectedIndex].label + " ";
        }
    return "";
  }

  handlePageChange(event, value) {
    this.setState({currentPage: value})
    this.props.pageChangeFunction(event, value)
  }

  handleSearchChange(event) {
      this.setState({searchText: event.target.value})
      AppController.getInstance().searchChange(event.target.value)
  }

  handleSearchKeyPress(event) {
    if (event.key === 'Enter') {
      this.props.searchFunction(this.state.currentPage, this.state.searchText)
    }
  }

  handleSearchClear(event) {
    event.preventDefault()
    this.setState({searchText: ''})
    AppController.getInstance().searchChange('')
    this.searchRef.current.focus()
  }

  handleSearchDomainChange(event) {
      this.setState({searchDomain: event.target.value})
      this.props.searchDomainChangeFunction(event)
  }


  render() {
    logger.log(logger.DEBUG, 'Menu.render')

    const { classes, userName, tabs } = this.props
    return (
      (<AppBar position='static' classes={{ root: classes.appBar }}>

        <Toolbar variant='dense' classes={{ root: classes.toolBar }}>
          <div className={classes.logo}>
            <img src={Logo} height='100%' width='100%' />
          </div>
          <Tabs
            value={this.state.currentPage}
            onChange={this.handlePageChange}
            classes={{ root: classes.tabs }}
            textColor='inherit'
            indicatorColor='secondary'
          >
            {_.map(tabs, tab => {
                return this.renderTab(tab)
              })}
          </Tabs>
          {this.renderBarcode()}
          {this.renderSearchField()}
          <Typography variant="body1" classes={{ root: classes.userInfo }}>
            {userName}
          </Typography>
          {this.renderLogout()}

        </Toolbar>
      </AppBar>)
    );
  }

  renderSearchField() {
     if(!this.props.searchFunction){
       return null;
     }
    const { classes, searchDomains } = this.props

    return (
            <>
            <div >
                <TextField
                    placeholder={this.getSearchDomainLabel() + messages.get(messages.SEARCH)}
                    value={this.state.searchText || ''}
                    onChange={this.handleSearchChange}
                    onKeyPress={this.handleSearchKeyPress}
                    variant='standard'
                    slotProps={{
                      input: {
                        inputRef: this.searchRef,
                        disableUnderline: true,
                        startAdornment: this.renderStartAdornment(searchDomains),
                        endAdornment: this.renderSearchClearIcon(),
                        classes: {
                          root: classes.search
                        }
                      }
                    }}
                    sx={this.props.menuStyles.searchField}
                  />
                  </div>
              </>
              )
  }

  renderLogout() {
    if(!this.props.logoutFunction){
      return null;
    }
    const { classes } = this.props
    return (<IconButton
                onClick={this.props.logoutFunction}
                classes={{ root: classes.button }}
              >
                <LogoutIcon fontSize='small' />
              </IconButton>
              )
  }

  renderBarcode() {
    if(!this.props.barcodeFunction) {
        return null;
    }
    const { classes } = this.props
    return (<IconButton
                  onClick={this.props.barcodeFunction}
                  classes={{ root: classes.button }}
                >
                  <FontAwesomeIcon icon={faBarcode} />
                </IconButton>)

  }

  renderTab(tab) {
      return (
            <Tab value={tab.page} label={tab.label} icon={tab.icon}/>
          )
  }

  renderStartAdornment(searchDomains) {
      const { classes, searchText } = this.props
      return (
          <>
              {this.renderSearchDomainIcon(searchDomains)}
              {this.renderSearchIcon()}
          </>
          )
  }

  handleMenu = event => {
      this.setState({ anchorEl: event.currentTarget });
  };

  handleClose = () => {
      this.setState({ anchorEl: null, menuOpen: !this.state.menuOpen });
  };

  handleMenuItemClick = (event, index) => {
      this.setState({ anchorEl: null,
          selectedIndex: index,
          menuOpen: !this.state.menuOpen,
          searchDomain: event.target.value
      });
      this.props.searchDomainChangeFunction(event)
  };

  renderSearchDomainIcon(searchDomains) {
      if(!this.props.searchDomainChangeFunction || !searchDomains) {
          return null;
      }

      return (
          <div>
              <IconButton
                  onClick={(event) => {
                          this.searchRef.current.focus()
                          const anchorEl = event.currentTarget
                          this.setState({ anchorEl: anchorEl, menuOpen: !this.state.menuOpen });
                      } }
              >
                 <KeyboardArrowDownIcon fontSize='medium' />
              </IconButton>
              <DropdownMenu
                    anchorEl={this.state.anchorEl}
                    anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
                    transformOrigin={{ vertical: 'top', horizontal: 'center' }}
                    open={this.state.menuOpen}
                    onClose={this.handleClose}
              >
                  {searchDomains.map((option, index) => {
                     return (<MenuItem value={index}
                               selected={index === this.state.selectedIndex}
                               onClick={(event) => this.handleMenuItemClick(event, index)}
                             >
                               { index === this.state.selectedIndex && (
                                   <ListItemIcon>
                                    <Check />
                                   </ListItemIcon>)}
                               {option.label}
                             </MenuItem>)
                         })}
              </DropdownMenu>
           </div>
      )

  }



  renderSearchIcon() {
      const { classes } = this.props
      return (
        <InputAdornment position='start' sx={{height: '100%'}}>
          <SearchIcon classes={{ root: classes.searchIcon }} fontSize='medium' />
        </InputAdornment>
      )
    }

  renderSearchClearIcon() {
    const { classes, searchText } = this.props
    if (this.state.searchText) {
      return (
            <InputAdornment position='end'>
              <CloseIcon
                classes={{ root: classes.searchClear }}
                onMouseDown={this.handleSearchClear}
                fontSize='small'
              />
            </InputAdornment>
      )
    } else {
      return <React.Fragment></React.Fragment>
    }
  }
}

export default withStyles(styles)(Menu)
