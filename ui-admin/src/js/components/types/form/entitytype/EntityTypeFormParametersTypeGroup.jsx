import React from 'react'
import withStyles from '@mui/styles/withStyles';
import Container from '@src/js/components/common/form/Container.jsx'
import AppController from '@src/js/components/AppController.js'
import Header from '@src/js/components/common/form/Header.jsx'
import EntityTypeFormSelectionType from '@src/js/components/types/form/entitytype/EntityTypeFormSelectionType.js'
import objectTypes from '@src/js/common/consts/objectType.js'
import Message from '@src/js/components/common/form/Message.jsx'
import messages from '@src/js/common/messages.js'
import logger from '@src/js/common/logger.js'
import MultipleSelectChip from '@src/js/components/common/form/MultipleSelectChip.jsx'

const styles = theme => ({
  field: {
    paddingBottom: theme.spacing(1)
  }
})

class EntityTypeFormParametersTypeGroup extends React.PureComponent {
  constructor(props) {
    super(props)
    this.state = {}
    this.references = {
    }
    this.handleChange = this.handleChange.bind(this)
    this.handleFocus = this.handleFocus.bind(this)
    this.handleBlur = this.handleBlur.bind(this)
  }

  componentDidMount() {
    this.focus()
  }

  componentDidUpdate(prevProps) {
    const prevSelection = prevProps.selection
    const selection = this.props.selection

    if (prevSelection !== selection) {
      this.focus()
    }
  }

  focus() {
    const type = this.getType(this.props)
    if (type && this.props.selection) {
      const { part } = this.props.selection.params
      if (part) {
        const reference = this.references[part]
        if (reference && reference.current) {
          reference.current.focus()
        }
      }
    }
  }

  handleChange(event) {
    this.props.onChange(EntityTypeFormSelectionType.TYPE, {
      field: event.target.name,
      value: event.target.value
    })
  }

  handleFocus(event) {
    this.props.onSelectionChange(EntityTypeFormSelectionType.TYPE, {
      part: event.target.name
    })
  }

  handleBlur() {
    this.props.onBlur()
  }

  render() {
    logger.log(logger.DEBUG, 'EntityTypeFormParametersTypeGroup.render')
    const { typeGroups = [] } = this.props.controller.getDictionaries()
    const { mode } = this.props
    const type = this.getType(this.props)
    if (!type) {
      return null
    }

    return (
      <Container>
        <Header>Type Groups for Object Type {type.code.value}</Header>
        
        <MultipleSelectChip
          mode={mode}
          label="Type Groups"
          options={typeGroups}
          value={type.typeGroupsAssignments.value}
          onChange={this.handleChange}
          onFocus={this.handleFocus}
          onBlur={this.handleBlur}
        />
      </Container>
    )
  }

  getType(props) {
    let { type, selection } = props

    if (!selection || selection.type === EntityTypeFormSelectionType.TYPE) {
      return type
    } else {
      return null
    }
  }
}

export default withStyles(styles)(EntityTypeFormParametersTypeGroup)
