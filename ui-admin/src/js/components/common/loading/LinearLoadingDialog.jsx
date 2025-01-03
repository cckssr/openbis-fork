import React from 'react';
import { Box, LinearProgress, Dialog, DialogContent, DialogTitle, DialogContentText } from '@mui/material';
import withStyles from '@mui/styles/withStyles';
import autoBind from 'auto-bind'

const styles = (theme) => ({
  dialogPaper: {
    borderRadius: theme.shape.borderRadius,    
  },
  contentText: {
    fontFamily: theme.typography.body2.fontFamily,
    fontSize: theme.typography.body2.fontSize,    
    color: theme.palette.text.primary,   
    overflow: 'hidden', 
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap',
    width: '100%',
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
            onClose,
            from,
            to,
            fileName,
            progress,
            currentSize,
            totalSize,
            timeLeft,
            speed,
            classes,
            variant,
            customProgressDetails,
            showPercentageAtEnd = false,
            progressStatus} = this.props
  
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
    <Dialog
      open={open}
      maxWidth="md"
      fullWidth
      classes={{ paper: classes.dialogPaper }}
    >
      
      <DialogTitle>{title}</DialogTitle>

      <DialogContent>
        <DialogContentText className={classes.contentText}>
          From <span style={{ color: 'blue' }}>{from}</span> to{' '}
          <span style={{ color: 'blue' }}>{to}</span>
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
      </DialogContent>
    </Dialog>
  )};
}


export default withStyles(styles)(LinearLoadingDialog);
