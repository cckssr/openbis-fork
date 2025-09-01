import React from 'react'
import withStyles from '@mui/styles/withStyles';
import TypeGroupFormParametersTypeGroup from '@src/js/components/types/form/typegroup/TypeGroupFormParametersTypeGroup.jsx'
import TypeGroupFormParametersObjectType from '@src/js/components/types/form/typegroup/TypeGroupFormParametersObjectType.jsx'
import logger from '@src/js/common/logger.js'

const styles = () => ({})

class TypeGroupFormParameters extends React.PureComponent {
  constructor(props) {
    super(props)
  }

  render() {
    logger.log(logger.DEBUG, 'TypeGroupFormParameters.render')

    const {
      controller,
      typeGroup,
      objectTypes,
      selection,
      selectedRow,
      mode,
      onChange,
      onSelectionChange,
      onBlur
    } = this.props

    return (
      <div>
        <TypeGroupFormParametersTypeGroup
          controller={controller}
          typeGroup={typeGroup}
          selection={selection}
          mode={mode}
          onChange={onChange}
          onSelectionChange={onSelectionChange}
          onBlur={onBlur}
        />
        <TypeGroupFormParametersObjectType
          controller={controller}
          typeGroup={typeGroup}
          objectTypes={objectTypes}
          selection={selection}
          selectedRow={selectedRow}
          mode={mode}
          onChange={onChange}
          onSelectionChange={onSelectionChange}
          onBlur={onBlur}
        />
      </div>
    )
  }
}

export default withStyles(styles)(TypeGroupFormParameters)
