import _ from 'lodash'
import React from 'react'
import autoBind from 'auto-bind'
import withStyles from '@mui/styles/withStyles';
import GridWithOpenbis from '@src/js/components/common/grid/GridWithOpenbis.jsx'
import GridExportOptions from '@src/js/components/common/grid/GridExportOptions.js'
import GridUtil from '@src/js/components/common/grid/GridUtil.js'
import Message from '@src/js/components/common/form/Message.jsx'
import date from '@src/js/common/date.js'
import messages from '@src/js/common/messages.js'
import logger from '@src/js/common/logger.js'

const styles = () => ({})

class UsersGrid extends React.PureComponent {
  constructor(props) {
    super(props)
    autoBind(this)
  }

  render() {
    logger.log(logger.DEBUG, 'UsersGrid.render')

    const { id, rows, selectedRowId, onSelectedRowChange, controllerRef } =
      this.props

    return (
      <GridWithOpenbis
        id={id}
        settingsId={id}
        controllerRef={controllerRef}
        header={messages.get(messages.USERS)}
        sort='userId'
        columns={[
          GridUtil.userColumn({
            name: 'userId',
            label: messages.get(messages.USER_ID),
            path: 'userId.value'
          }),
          {
            name: 'firstName',
            label: messages.get(messages.FIRST_NAME),
            getValue: ({ row }) => row.firstName.value
          },
          {
            name: 'lastName',
            label: messages.get(messages.LAST_NAME),
            getValue: ({ row }) => row.lastName.value
          },
          {
            name: 'email',
            label: messages.get(messages.EMAIL),
            nowrap: true,
            getValue: ({ row }) => row.email.value
          },
          {
            name: 'space',
            label: messages.get(messages.HOME_SPACE),
            getValue: ({ row }) => row.space.value
          },
          GridUtil.registratorColumn({ path: 'registrator.value' }),
          GridUtil.registrationDateColumn({ path: 'registrationDate.value' }),
          {
            name: 'userStatus',
            label: messages.get(messages.USER_STATUS),
            getValue: ({ row }) => this.userStatusValue(row.userId.value, row.active.value, row.expiryDate.value)
          },
          GridUtil.dateObjectColumn({ name: 'expiryDate',
                    label: messages.get(messages.EXPIRY_DATE),
                    path: 'expiryDate.value' }),
          {
           name: 'validityLeft',
           label: messages.get(messages.VALIDITY_LEFT),
           getValue: ({ row }) => {
             if (
               !row.expiryDate.value ||
               !row.expiryDate.value.dateObject
             ) {
               return null
             }

             return (
               row.expiryDate.value.dateObject.getTime() -
               new Date().getTime()
             )
           },
           renderValue: ({ value }) => {
             if (value) {
                 if(value < 0) {
                    return (
                           <Message type='error'>
                             {messages.get(messages.EXPIRED)}
                           </Message>
                         )
                 }
               return date.duration(value)
             } else {
               return null
             }
           },
           filterable: false,
           nowrap: true
          }
        ]}
        rows={rows}
        exportable={{
          fileFormat: GridExportOptions.FILE_FORMAT.TSV,
          filePrefix: 'users'
        }}
        selectable={true}
        selectedRowId={selectedRowId}
        onSelectedRowChange={onSelectedRowChange}
      />
    )
  }

  userStatusValue(userId, active, expiryDate) {
      if(!userId) {
        return null;
      }
      if(active) {
          if(expiryDate && expiryDate.dateObject) {
            return messages.get(messages.ACTIVE_UNTIL_EXPIRY_DATE);
          }
        return messages.get(messages.ACTIVE);
      } else {
        return messages.get(messages.INACTIVE);
      }
  }
}

export default _.flow(withStyles(styles))(UsersGrid)
