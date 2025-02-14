import * as React from 'react';
import PublishIcon from '@mui/icons-material/Publish'
import messages from '@src/js/common/messages.js';
import UploadButton from '@src/js/components/common/data-browser/UploadButton.jsx'

export default function InputFileUpload({onInputFile}) {

    const fileChangedHandler = async (event) => {
        let file = event.target.files[0];
        let reader = new FileReader();
        reader.readAsArrayBuffer(event.target.files[0]);
        onInputFile(file);
    };

    return (
        <UploadButton
          color='inherited'
          size='small'
          variant='outlined'
          startIcon={<PublishIcon />}
          folderSelector={false}
          onClick={fileChangedHandler}
          accept='.png,.jpg,.jpeg'
        >
          {messages.get(messages.FILE_UPLOAD)}
        </UploadButton>
    );
}