import _ from 'lodash'
import React from 'react'
import AppController from '@src/js/components/AppController.js'
import ContentTab from '@src/js/components/common/content/ContentTab.jsx'
import openbis from '@src/js/services/openbis.js'
import messages from '@src/js/common/messages.js'
import objectType from '@src/js/common/consts/objectType.js'
import pages from '@src/js/common/consts/pages.js'
import logger from '@src/js/common/logger.js'


class DatabaseTab extends React.PureComponent {
  constructor(props) {
    super(props)
    this.state = {
      text: ''
    }
  }

  async componentDidMount() {
    await this.componentDidUpdate()
  }

  async componentDidUpdate() {
    try {
      const { tab } = this.props
      const { object, label } = tab

      if (!_.isEmpty(label)) {
        return;
      }

      let typeText = null
      let idText = null

      if (object.type === objectType.SPACES) {
        typeText = messages.get(messages.SPACES)
        idText = null
      } else if (object.type === objectType.SPACE) {
        typeText = messages.get(messages.SPACE)
        idText = object.id
      } else if (object.type == objectType.NEW_SPACE) {
        typeText = messages.get(messages.NEW_SPACE)
      } else if (object.type === objectType.PROJECT) {
        typeText = messages.get(messages.PROJECT)
        const projects = await openbis.getProjects(
          [new openbis.ProjectPermId(object.id)],
          new openbis.ProjectFetchOptions()
        )

        if (projects[object.id]) {
          idText = projects[object.id].getCode()
        }
      } else if (object.type === objectType.NEW_PROJECT) {
        typeText = messages.get(messages.NEW_PROJECT)
      } else if (object.type === objectType.COLLECTION) {
        typeText = messages.get(messages.COLLECTION)
        const experiments = await openbis.getExperiments(
          [new openbis.ExperimentPermId(object.id)],
          new openbis.ExperimentFetchOptions()
        )
        if (experiments[object.id]) {
          idText = experiments[object.id].getCode()
        }
      } else if (object.type === objectType.OBJECT) {
        typeText = messages.get(messages.OBJECT)
        const samples = await openbis.getSamples(
          [new openbis.SamplePermId(object.id)],
          new openbis.SampleFetchOptions()
        )
        if (samples[object.id]) {
          idText = samples[object.id].getCode()
        }
      } else if (object.type === objectType.DATA_SET) {
        typeText = messages.get(messages.DATA_SET)
        idText = object.id
      }

      //label: (object.params.entityType ? ('new ' + object.params.entityType) : (typeText || object.type)) + ': ' + (idText || object.id)
      const newLabel = idText
        ? (typeText || object.type) + ': ' + idText
        : (typeText || object.type)
      const tabWithLabel = {
        ...tab,
        label: newLabel
      }

      AppController.getInstance().replaceOpenTab(
        pages.DATABASE,
        tabWithLabel.id,
        tabWithLabel
      )
    } catch (error) {
      AppController.getInstance().errorChange(error)
    }
  }

  render() {
    logger.log(logger.DEBUG, 'DatabaseTab.render')
    return <ContentTab label={this.props.tab.label} changed={this.props.tab.changed} />
  }
}

export default DatabaseTab
