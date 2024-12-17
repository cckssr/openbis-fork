import React from "react";
import { isObjectEmpty } from "../../utils";

const DefaultMetadataField = ({keyProp, valueProp}) => {
    if (!isObjectEmpty(valueProp) || valueProp.length > 0)
        return <p> <strong>{keyProp}: </strong>{JSON.stringify(valueProp)} </p>
}

export default DefaultMetadataField;