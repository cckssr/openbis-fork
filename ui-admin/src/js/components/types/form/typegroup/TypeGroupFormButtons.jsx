import React from 'react'
import AppController from '@src/js/components/AppController.js'
import PageMode from '@src/js/components/common/page/PageMode.js'
import PageButtons from '@src/js/components/common/page/PageButtons.jsx'
import Button from '@src/js/components/common/form/Button.jsx'
import TypeGroupFormSelectionType from '@src/js/components/types/form/typegroup/TypeGroupFormSelectionType.js'
import messages from '@src/js/common/messages.js'
import logger from '@src/js/common/logger.js'

class TypeGroupFormButtons extends React.PureComponent {
  constructor(props) {
    super(props)
  }

  render() {
    logger.log(logger.DEBUG, 'TypeGroupFormButtons.render')

    const { mode, onEdit, onSave, onCancel, changed, typeGroup } = this.props

    return (
      <PageButtons
        mode={mode}
        changed={changed}
        onEdit={onEdit}
        onSave={onSave}
        onCancel={typeGroup.id ? onCancel : null}
        renderAdditionalButtons={params => this.renderAdditionalButtons(params)}
      />
    )
  }

  renderAdditionalButtons({ mode, classes }) {
    if (mode === PageMode.EDIT) {
      const { onAdd, onRemove } = this.props

      return (
        <React.Fragment>
          <Button
            name='addObjectType'
            label={messages.get(messages.ADD_OBJECT_TYPE)}
            styles={{ root: classes.button }}
            onClick={onAdd}
          />
          <Button
            name='removeObjectType'
            label={messages.get(messages.REMOVE_OBJECT_TYPE)}
            styles={{ root: classes.button }}
            disabled={
              !(
                this.isNonSystemInternalObjectTypeSelected() ||
                AppController.getInstance().isSystemUser()
              )
            }
            onClick={onRemove}
          />
        </React.Fragment>
      )
    } else {
      return null
    }
  }

  isNonSystemInternalObjectTypeSelected() {
    const { selection, typeGroup, objectTypes } = this.props

    if (selection && selection.type === TypeGroupFormSelectionType.OBJECT_TYPE) {
      const objectType = objectTypes.find(objectType => objectType.id === selection.params.id)
      return !(
        typeGroup.internal.value && objectType.internal.value
      )
    } else {  
      return false
    }
  }
}

export default TypeGroupFormButtons
