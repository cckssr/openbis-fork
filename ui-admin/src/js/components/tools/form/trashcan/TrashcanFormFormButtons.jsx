import React from 'react'
import PageMode from '@src/js/components/common/page/PageMode.js'
import PageButtons from '@src/js/components/common/page/PageButtons.jsx'
import Button from '@src/js/components/common/form/Button.jsx'
import messages from '@src/js/common/messages.js'
import logger from '@src/js/common/logger.js'

class TrashcanFormFormButtons extends React.PureComponent {
  constructor(props) {
    super(props)
  }

  render() {
    logger.log(logger.DEBUG, 'TrashcanFormFormButtons.render')

    const { mode } = this.props

    return (
      <PageButtons
        mode={mode}
        renderAdditionalButtons={params => this.renderAdditionalButtons(params)}
      />
    )
  }

  renderAdditionalButtons({ classes }) {
      const { onEmptyTrashcan } = this.props

      return (
          <Button
            name='emptyTrashcan'
            label={messages.get(messages.EMPTY_TRASHCAN)}
            styles={{ root: classes.button }}
            onClick={onEmptyTrashcan}
          />
      )
  }
}

export default TrashcanFormFormButtons
