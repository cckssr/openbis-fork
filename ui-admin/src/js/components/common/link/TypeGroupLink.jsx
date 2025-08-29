import React from 'react'
import LinkToObject from '@src/js/components/common/form/LinkToObject.jsx'
import pages from '@src/js/common/consts/pages.js'
import objectTypes from '@src/js/common/consts/objectType.js'
import logger from '@src/js/common/logger.js'

class TypeGroupLink extends React.PureComponent {
  render() {
    logger.log(logger.DEBUG, 'TypeGroupLink.render')

    const { typeGroupCode } = this.props

    if (typeGroupCode) {
      return (
        <LinkToObject
          page={pages.TYPES}
          object={{
            type: objectTypes.OBJECT_TYPE_GROUP,
            id: typeGroupCode
          }}
        >
          {typeGroupCode}
        </LinkToObject>
      )
    } else {
      return null
    }
  }
}

export default TypeGroupLink
