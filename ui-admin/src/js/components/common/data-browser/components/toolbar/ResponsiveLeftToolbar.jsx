import React from 'react';
import ResizeObserver from 'rc-resize-observer';
import LeftToolbarButtons from '@src/js/components/common/data-browser/components/toolbar/LeftToolbarButtons.jsx';
import UploadSection from '@src/js/components/common/data-browser/components/upload/UploadSection.jsx';
import { debounce } from '@mui/material';
import withStyles from '@mui/styles/withStyles';

const styles = theme => ({
  buttons: {
    flex: '1 1 auto',
    display: 'flex',
    alignItems: 'center',
    whiteSpace: 'nowrap',
    overflow: 'hidden',
    '& > button': {
      marginRight: theme.spacing(1),
    },
    '& > button:nth-last-child(1)': {
      marginRight: 0,
    },
  },
  buttonLeft: {    
    border: '1px solid #ccc',
    borderRadius: '4px',
    backgroundColor: '#fff',
    color: '#555',
    textTransform: 'none',
    padding: '4px 8px',
    minWidth: 0,
    boxShadow: 'none',
    marginRight: theme.spacing(1),
    display: 'inline-flex',
    alignItems: 'center',
    justifyContent: 'center',
    '&:hover': {
      backgroundColor: '#f5f5f5',
      boxShadow: 'none',
    },
  },
  toggleButton: {},
  collapsedButtonsContainer: {
    display: 'flex',
    flexDirection: 'column',
    '& > button': {
      marginBottom: theme.spacing(1),
    },
    '& > button:nth-last-child(1)': {
      marginBottom: 0,
    },
  },
});

class ResponsiveLeftToolbar extends React.Component {
  constructor(props) {
    super(props);
    this.state = { width: 0 };
    this.onResize = debounce(this.onResize.bind(this), 1);
  }
  
  onResize({ width }) {
    if (width !== this.state.width) {
      this.setState({ width });
    }
  }
  
  render() {    
    const { classes, uploadSectionProps, ...other } = this.props;
    return (
      <ResizeObserver onResize={this.onResize}>
        <div className={classes.buttons}>
          {uploadSectionProps ? (
            <UploadSection {...uploadSectionProps} />
          ) : null}
          <LeftToolbarButtons
            width={this.state.width}
            containerClassName={classes.buttons}
            {...other}
          />
        </div>
      </ResizeObserver>
    );
  }
}

export default withStyles(styles)(ResponsiveLeftToolbar);
