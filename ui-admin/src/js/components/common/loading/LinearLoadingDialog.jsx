import React from 'react';
import { Box, LinearProgress, DialogContent, DialogTitle, DialogContentText } from '@mui/material';
import withStyles from '@mui/styles/withStyles';
import autoBind from 'auto-bind'
import Dialog from '@src/js/components/common/dialog/Dialog.jsx'
import Button from '@src/js/components/common/form/Button.jsx'
import messages from '@src/js/common/messages.js'

const styles = (theme) => ({
  contentText: {
    fontFamily: theme.typography.body2.fontFamily,
    fontSize: theme.typography.body2.fontSize,    
    color: theme.palette.text.primary,   
    overflow: 'hidden', 
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap',
    width: '100%',
  },
  highlight: {
    color: theme.palette.primary.main, 
  },
  fileText: {
    fontFamily: theme.typography.body2.fontFamily,
    fontSize: theme.typography.body2.fontSize,    
    color: theme.palette.text.primary,
    marginBottom: theme.spacing(1),
    overflow: 'hidden', 
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap',
    width: '100%',
  },
  progressContainer: {
    position: 'relative',    
  },
  progressPercentage: {
    fontFamily: theme.typography.body2.fontFamily,
    fontSize: theme.typography.body2.fontSize,   
    position: 'absolute',
    top: '50%',
    left: '50%',
    transform: 'translate(-50%, -50%)',    
    fontWeight: 'bold',
    color:'white'
  },
  progressDetails: {
    fontFamily: theme.typography.body2.fontFamily,
    fontSize: theme.typography.body2.fontSize,
    color: theme.palette.text.secondary,
    
  },
  progressBar: {
    height: 20,
    borderRadius: 10,    
    '& .MuiLinearProgress-bar': {
      borderRadius: 10,
    }
  },
  progressBarIndeterminate: {
    height: 20,
    borderRadius: 10,    
    '& .MuiLinearProgress-bar': {
        borderRadius: 10        
    }
  },
  button: {
    marginLeft: theme.spacing(1)
  }

});
class LinearLoadingDialog extends React.Component {

  constructor(props, context) {
      super(props, context)
      autoBind(this)
  
      this.controller = this.props.controller      
  }
  render() {
    const {
        open,
        title,
      } = this.props

  return (
        <Dialog
          open={open}
          onClose={this.handleClose}
          title={title}
          content={this.renderContent()}
          actions={this.renderButtons()}
        />
  )};

  renderContent() {
    const {      
        from,
        to,
        fileName,
        progress,      
        classes,
        variant,
        customProgressDetails,
        showPercentageAtEnd = false,
        progressStatus,
        totalSize,
        currentSize,
        timeLeft,
        speed
      } = this.props


    const formattedTotalSize = (this.controller && totalSize)
    ? this.controller.formatSize(totalSize)
    : ''

    const formattedCurrentSize = (this.controller && currentSize)
      ? this.controller.formatSize(currentSize)
      : ''

    const formattedTimeLeft = (this.controller && timeLeft)
      ? this.controller.formatTime(timeLeft)
      : ''

    const formattedSpeed = (this.controller && speed)
      ? this.controller.formatSpeed(speed)
      : ''

    return (
      <>
        <DialogContentText className={classes.contentText}>
          From <span className={classes.highlight}>{from}</span> to{' '}
          <span className={classes.highlight}>{to}</span>
        </DialogContentText>

        <DialogContentText className={classes.fileText}>
          File: {fileName}
        </DialogContentText>

        {!showPercentageAtEnd && (
          <Box className={variant === 'determinate' ? classes.progressContainer : classes.progressBarIndeterminate}>
            <LinearProgress
              variant={variant}
              value={progress}
              className={classes.progressBar}
            />

            {variant === "determinate" && (
              <span className={classes.progressPercentage}>{progress}%</span>
            )}

          </Box>
        )}

        {showPercentageAtEnd && (
          <Box display="flex" alignItems="center" height={15}>
            <LinearProgress
              variant={variant}
              value={progress}                   
              style={{ flexGrow: 1 }}              
            />
            {variant === "determinate" && (
              <Box minWidth={35} ml={1}>
                <span>{progress}%</span>
              </Box>
            )}

        </Box>
        )}
        
        <DialogContentText className={classes.progressDetails}>
          {customProgressDetails || (
            <>
              {formattedCurrentSize && formattedTotalSize && (
                <>
                  {`${formattedCurrentSize} / ${formattedTotalSize}`}
                </>
              )}
              {formattedCurrentSize && formattedTotalSize && (formattedTimeLeft || progressStatus || formattedSpeed) && ', '}
              {formattedTimeLeft && `${formattedTimeLeft} left`}
              {formattedTimeLeft && (progressStatus || formattedSpeed) && ', '}
              {progressStatus || formattedSpeed}

            </>
          )}
        </DialogContentText>
      </>
    )
  }

  renderButtons() {
    const { onCancel, classes } = this.props
    return (       
      <Button
        name='cancel'
        label={messages.get(messages.CANCEL)}
        styles={{ root: classes.button }}
        color='inherit'
        variant='outlined'
        onClick={onCancel}
      />        
    )
  }

}


export default withStyles(styles)(LinearLoadingDialog);
