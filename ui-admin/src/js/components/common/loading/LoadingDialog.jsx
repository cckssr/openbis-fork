import React from 'react'
import withStyles from '@mui/styles/withStyles';
import logger from '@src/js/common/logger.js'
import Dialog from '@mui/material/Dialog'
import CircularProgress from '@mui/material/CircularProgress'
import DialogContent from '@mui/material/DialogContent'
import DialogContentText from '@mui/material/DialogContentText';
import DialogTitle from '@mui/material/DialogTitle'

const styles = theme => ({
  dialogPaper: {
    padding: theme.spacing(1),
    maxWidth: '80vw',
    maxHeight: '80vh',
    borderRadius: '60px',
    backgroundColor: theme.palette.background.paper,
    boxShadow: "none",
  },
  title: {
    fontFamily: theme.typography.h6.fontFamily,
    fontSize: theme.typography.h6.fontSize,
    textAlign: 'center',
    padding: theme.spacing(2)
  },
  titleWithSubtitle: {
    paddingBottom: theme.spacing(0.01) 
  },
  subtitle: {
    fontFamily: theme.typography.caption.fontFamily,
    fontSize: theme.typography.caption.fontSize,
    color: theme.palette.text.secondary,
    textAlign: 'center',
    whiteSpace: 'nowrap', 
    overflow: 'hidden', 
    textOverflow: 'ellipsis', 
    maxWidth: '100%', 
    display: 'block', 
  },
  content: {
    fontFamily: theme.typography.body2.fontFamily,
    fontSize: theme.typography.body2.fontSize,
    textAlign: 'center',
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    padding: '0 !important'
  },
  progressBackground: {
      color: theme.palette.grey[300], // Grey background color
      position: 'absolute',
    },
  icon: {
    margin: theme.spacing(2)
  }
})

class LoadingDialog extends React.Component {
  
  static defaultProps = {
    detailPrimaryMaxLength: 15,
    detailSecondaryMaxLength: 15,
  };

  truncateText(text, maxLength) {
    if (!text) return '';
    return text.length > maxLength ? `${text.substring(0, maxLength)}...` : text;
  }

  render() {
    logger.log(logger.DEBUG, 'LoadingDialog.render')

    const { 
      loading, 
      classes, 
      variant, 
      value, 
      message, 
      showBackground, 
      detailPrimary,
      detailPrimaryMaxLength,
      detailSecondary,
      detailSecondaryMaxLength,
    } = this.props


    const truncatedDetailPrimary = this.truncateText(detailPrimary, detailPrimaryMaxLength);
    const truncatedDetailSecondary = this.truncateText(detailSecondary, detailSecondaryMaxLength);
    const hasSubtitles = truncatedDetailPrimary || truncatedDetailSecondary;

    return (
      (<Dialog
        open={loading}
        classes={{ paper: classes.dialogPaper }}
        keepMounted
      >
        <DialogContent className={classes.content}>
          {/* Show Background Circle only when the `showBackground` prop is true */}
          {showBackground && variant === "determinate" && (
            <CircularProgress
              size={68}
              variant="determinate"
              value={100} // Always full for the background
              className={classes.progressBackground}
            />
          )}
          {/* Foreground Progress */}
          <CircularProgress
            size={68}
            variant={variant}
            value={value}
            className={classes.icon}
          />
        </DialogContent>
        {message ? (
        <DialogTitle className={`${classes.title} ${hasSubtitles ? classes.titleWithSubtitle : ''}`}>
            {message}
          </DialogTitle>) : null}
          {/* File and Speed Information */}
              {(truncatedDetailPrimary) && (
                <DialogContentText className={classes.subtitle}>
                  {truncatedDetailPrimary}                  
                </DialogContentText>
              )}
              {(truncatedDetailSecondary) && (
                <DialogContentText className={classes.subtitle}>
                  {truncatedDetailSecondary}                  
              </DialogContentText>
              )}
       
      </Dialog>)
    );
  }
}

export default withStyles(styles)(LoadingDialog)
