import constants from '@src/js/components/common/imaging/constants.js';

export const convertToBase64 = (file) => {
    return new Promise((resolve, reject) => {
        const fileReader = new FileReader();
        fileReader.readAsDataURL(file);
        fileReader.onload = () => {
            resolve(fileReader.result);
        };
        fileReader.onerror = (error) => {
            reject(error);
        };
    });
};

export const isObjectEmpty = (objectName) => {
    return (
        objectName &&
        Object.keys(objectName).length === 0 &&
        objectName.constructor === Object
    );
};

export const inRange = (x, min, max) => {
    return ((x - min) * (x - max) <= 0);
};

export const createInitValues = (inputsConfig, activeConfig) => {
    const isActiveConfig = isObjectEmpty(activeConfig);
    return Object.fromEntries(inputsConfig.map(input => {
        switch (input.type) {
            case constants.DROPDOWN:
                if (!isActiveConfig)
                    return [input.label, activeConfig[input.label] ? activeConfig[input.label] : input.multiselect ? [input.values[0]] : input.values[0]];
                else
                    return [input.label, input.multiselect ? [input.values[0]] : input.values[0]];
            case constants.SLIDER:
                if (!isActiveConfig) {
                    if (input.visibility) {
                        for (const condition of input.visibility) {
                            if (condition.values.includes(activeConfig[condition.label])) {
                                input.range = condition.range;
                                input.unit = condition.unit;
                            }
                        }
                    }
                    return [input.label, activeConfig[input.label] ? activeConfig[input.label] : input.range[0]];
                } else {
                    if (input.visibility) {
                        input.range = input.visibility[0].range[0];
                    }
                    return [input.label, input.range[0]];
                }
            case constants.RANGE:
                if (!isActiveConfig) {
                    if (input.visibility) {
                        for (const condition of input.visibility) {
                            if (condition.values.includes(activeConfig[condition.label])) {
                                input.range = condition.range;
                                input.unit = condition.unit;
                            }
                        }
                    }
                    let rangeInitValue = [inRange(activeConfig[input.label][0], input.range[0], input.range[1]) ? activeConfig[input.label][0] : input.range[0],
                    inRange(activeConfig[input.label][1], input.range[0], input.range[1]) ? activeConfig[input.label][1] : input.range[1]]
                    return [input.label, rangeInitValue];
                } else {
                    if (input.visibility) {
                        return [input.label, [input.visibility[0].range[0], input.visibility[0].range[1]]];
                    }
                    return [input.label, [input.range[0], input.range[1]]];
                }
            case constants.COLORMAP:
                if (!isActiveConfig)
                    return [input.label, activeConfig[input.label] ? activeConfig[input.label] : input.values[0]];
                else
                    return [input.label, input.values[0]];
        }
    }));
};