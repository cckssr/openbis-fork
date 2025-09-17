Imaging technology
==================================

## Introduction

imaging technology is an extension that allows to process raw scientific data stored in datasets into easy to analyse images. 

This technology is split into following parts:
- Imaging Service
- Imaging Gallery Viewer
- Imaging DataSet Viewer


## How to enable this technology

"imaging" core plugin, together with simple dataset type can be found here: https://sissource.ethz.ch/sispub/openbis/-/tree/master/core-plugin-openbis/dist/core-plugins/imaging/1?ref_type=heads

1. It needs to be downloaded in the installation's `servers/core-plugins` folder
2. 'imaging' needs to be enabled in the `servers/core-plugins/core-plugins.properties` 


## Data Model

The new imaging extension follows the current eln-lims data model.

This structure could initially seem to have a couple of additional levels that not everybody will actively use, but in practice is the most flexible since allows to use all openBIS linking features between Experiments, Experimental Steps and other Objects.

Space (Space): Used for rights management\
&nbsp; &rdsh; Project (Project): Used for rights management\
&nbsp; &nbsp; &nbsp; &nbsp; &rdsh; Collection (Collection): Allows Object Aggregation\
&nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &rdsh; Experiment (Object): Allows Objects linking\
&nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &rdsh; Exp. Step (Object): Allows Objects linking and DataSets\
&nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &rdsh; DataSet (DataSet): Allows to attach data

Different DataSet Types can have different properties and metadata sections. A default template type called IMAGING_DATA is provided. Additionally, each lab can create their own types with different metadata - a core requirement for dataset type is to contain an internal property called $IMAGING_DATA_CONFIG.

### $IMAGING_DATA_CONFIG

To fulfill the visualization requirements, including the flexibility of updating these over time it is needed for every DataSet to include certain data and mutable metadata.
* Original RAW data, on any format, open or proprietary.
* A property $IMAGING _DATA_CONFIG if type JSON containing:
  * Indicating the number of inputs and their components.
  * The number of images (at least one).
  * The number of previews per image (at least one).
  * The config with the inputs selected to recalculate each preview.
  * The preview image byte array in png or jpeg format.
  * Any Custom Metadata fields.

Example of `$IMAGING _DATA_CONFIG`:

```json 
{
"config" : { 
    "@type" : "dss.dto.imaging.ImagingDataSetConfig",
    "adaptor" : "java.package.ImagingDataSetExampleAdaptor", //Adapter to be used by the service
    "version" : 1.0, //non-null
    "speeds" : [1000, 2000, 5000], // (UI-specific) Available values expressed in milliseconds or null
    "resolutions": ["200x200", "2000x2000"], //(UI-specific) Available values expressed in pixels or null
    "playable" : true, // (UI-specific)true or false
    "exports" : [ // parameters for export
            {   
                "@type" : "dss.dto.imaging.ImagingDataSetControl",  //non-null
                "label": "Include",  //non-null
                "type": "Dropdown", // non-null
                "values": ["Data", "Metadata"], // nullable 
                "multiselect" : true
            },
            {   
                "@type" : "dss.dto.imaging.ImagingDataSetControl",
                "label": "Resolutions", 
                "type": "Dropdown",
                "values": ["original", "300dpi", "150dpi", "72dpi"],
                "multiselect" : false
            },
            {   
                "@type" : "dss.dto.imaging.ImagingDataSetControl",
                "label": "Format", 
                "type": "Dropdown",
                "values": ["zip/original", "zip/jpeg", "zip/png", "zip/svg"],
                "multiselect" : false
            }
        ],
    "inputs" : [  // parameters for the adapter
        {   
            "@type" : "dss.dto.imaging.ImagingDataSetControl",
            "label": "Dimension 1", 
            "section": "Channels",
            "type": "Dropdown", 
            "values": ["Channel A", "Channel B", "Channel C"],
            "multiselect" : false,
            "playable" : false,
            "speeds": [1000, 2000, 5000]
        },
        {   
            "@type" : "dss.dto.imaging.ImagingDataSetControl",
            "label": "Dimension 2",
            "section": "Channels", 
            "type": "Slider",
            "range": null, //If range of a component is null, visibility should be used instead.
	        "unit": null, //optional parameter 
            "playable": true,
            "speeds": [1000, 2000, 5000],
	        "visibility": [{
                "label": "Dimension 1", 
                "values": ["Channel A"],
                "range": [1,2,1], //From 1 to 2 with a step of 1
		        "unit": "nm" //optional 
            }, {
                "label": "Dimension 1", 
                "values":["Channel B", "Channel C"],
                "range": [4,6,1], //From 4 to 6 with a step of 1
                "unit": "px" //optional 
            }]
        },
        {   
            "@type" : "dss.dto.imaging.ImagingDataSetControl",
            "label": "Dimension 3", 
            "section": "Channels",
            "type": "Slider",
            "range": [1,2,0.5], // From 1 to 2 with a step of 0.5
            "playable": true,
            "speeds": [1000, 2000, 5000]
        }
    ],
    "metadata": {} // Custom Metadata to use by UI
},
"images" : [  //non-null
    { /* Image */
        "@type" : "dss.dto.imaging.ImagingDataSetImage", //non-null
        "config" : {} //nullable – custom parameters to be provided to the adaptor
        "index" : 0,
        "previews" : [ 
            { /* Preview */
                "@type" : "dss.dto.imaging.ImagingDataSetPreview", //non-null
                "config" : { //nullable
                "Dimension 1": "Channel A", 
                "Dimension 2": [2, 1.5]
        	    },
                "format": "jpeg", //non-null
                "bytes": "FFD8 … FFD9", // base64-encoded image
                "show" : true, /* flag used to indicate if should be shown by default */ non-null 
                "metadata": {} /* Custom Metadata to use by UI */
            }
        ],
        "metadata": {} /* Custom Metadata to use by UI */
    }]
}
```


## Imaging Service
This section describes how Imaging Service works and how it can be extended.

Imaging service is implemented using Custom Services technology for DSS (For more details see [Custom Datastore Server Services](./dss-services.md)). It is a special service that, when requested, runs special "adaptor" java class (specified in $IMAGING_DATA_CONFIG) which computes images based on associated dataset files and some input parameters.

### Adaptors
Currently, there are 3 types of adaptors that are implemented:
- [ImagingDataSetExampleAdaptor](https://sissource.ethz.ch/sispub/openbis/-/blob/master/core-plugin-openbis/dist/core-plugins/imaging/1/dss/services/imaging/lib/imaging-technology-sources/source/java/ch/ethz/sis/openbis/generic/server/dss/plugins/imaging/adaptor/ImagingDataSetExampleAdaptor.java) - an example adaptor written in Java, it produces a random image.
- [ImagingDataSetJythonAdaptor](https://sissource.ethz.ch/sispub/openbis/-/blob/master/core-plugin-openbis/dist/core-plugins/imaging/1/dss/services/imaging/lib/imaging-technology-sources/source/java/ch/ethz/sis/openbis/generic/server/dss/plugins/imaging/adaptor/ImagingDataSetJythonAdaptor.java) - an adaptor that makes use of Jython.
- [ImagingDataSetPythonAdaptor](https://sissource.ethz.ch/sispub/openbis/-/blob/master/core-plugin-openbis/dist/core-plugins/imaging/1/dss/services/imaging/lib/imaging-technology-sources/source/java/ch/ethz/sis/openbis/generic/server/dss/plugins/imaging/adaptor/ImagingDataSetPythonAdaptor.java) - abstract adaptor that allows to implement image computation logic as a python script. More can be read here: [Python adaptor] 

All of these adaptor have one thing in common: they implement [IImagingDataSetAdaptor](https://sissource.ethz.ch/sispub/openbis/-/blob/master/core-plugin-openbis/dist/core-plugins/imaging/1/dss/services/imaging/lib/premise-sources/source/java/ch/ethz/sis/openbis/generic/server/dss/plugins/imaging/adaptor/IImagingDataSetAdaptor.java?ref_type=heads) interface.

Writing a completely new adaptor requires:
1. Writing a Java class that implements IImagingDataSetAdaptor interface (by either interface realisation or extension).
2. Compiling java classes into a .jar file.
3. Including .jar library in  `servers/core-plugins/imaging/<version number>/dss/services/imaging/lib` folder.
4. Restarting Openbis.

#### Python adaptor
ImagingDataSetPythonAdaptor is an abstract class that contains logic for handling adaptor logic written in a python script. A concrete implementation may look like this:

```java
package ch.ethz.sis.openbis.generic.server.dss.plugins.imaging.adaptor;

import ch.systemsx.cisd.common.exceptions.UserFailureException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public final class NanonisSxmAdaptor extends ImagingDataSetPythonAdaptor
{
   private final String SXM_SCRIPT_PROPERTY = "nanonis-sxm"; //propertyPath

   public NanonisSxmAdaptor(Properties properties)
   {
      String scriptProperty = properties.getProperty(SXM_SCRIPT_PROPERTY, "");
      if (scriptProperty.trim().isEmpty())
      {
         throw new UserFailureException(
                 "There is no script path property called '" + SXM_SCRIPT_PROPERTY + "' defined for this adaptor!");
      }
      Path script = Paths.get(scriptProperty);
      if (!Files.exists(script))
      {
         throw new UserFailureException("Script file " + script + " does not exists!");
      }
      this.scriptPath = script.toString();
      this.pythonPath = properties.getProperty("python3-path", "python3");
   }

}

```
In this example 3 elements are defined: propertyPath, *scriptPath* and *pythonPath*

propertyPath - name of a property in `servers/core-plugins/imaging/<version number>/dss/services/imaging/plugin.properties`
*scriptPath* - path to a python script to be executed by this adaptor, this path is defined by propertyPath property.
*pythonPath* - path to a python environment to execute script, defined in `plugin.properties` file as `python3-path`. If such property is not found, a default python3 environment is used.


Link to existing adaptor: [Nanonis SXM adaptor](https://sissource.ethz.ch/sispub/openbis/-/blob/master/core-plugin-openbis/dist/core-plugins/imaging/1/dss/services/imaging/lib/premise-sources/source/java/ch/ethz/sis/openbis/generic/server/dss/plugins/imaging/adaptor/NanonisSxmAdaptor.java?ref_type=heads)

### Communication with the service
To send request to an OpenBIS service, it is required to send POST message with special JSON in the body, the recipe is as follows:

```json 
{
  "method": "executeCustomDSSService",
  "id": "2",
  "jsonrpc": "2.0",
  "params": [
    OPENBIS_TOKEN,
    {
      "@type": "dss.dto.service.id.CustomDssServiceCode",
      "permId": SERVICE_NAME
    },
    {
      "@type": "dss.dto.service.CustomDSSServiceExecutionOptions",
      "parameters": PARAMETERS
    }
  ]
}
```

*OPENBIS_TOKEN* is a user session token\
*SERVICE_NAME* is a name of the plugin we are sending our requests. For Imaging technology it should be *imaging*\
*PARAMETERS* is a command-specific object to be used by the service.

Imaging service provides 3 of commands that can process the data:
- preview
- export
- multi-export



#### Preview
Preview command triggers computation of a preview images based on a config parameters

*PARAMETERS* section looks like this:
```json 
{
    "type" : "preview", // preview command type
    "permId" : "999999999-9999", // permId of the dataset
    "error" : null, // (response) exception details, if error occurs
    "index" : 0, // index of an image in dataset
    "preview" : { // preview definition
        "@type" : "dss.dto.imaging.ImagingDataSetPreview",
        "config" : { // config to be passed to the adapter, it is format-specific
            "Parameter 1“: “Channel A", 
            "Parameter 2": [2, 1.5]
        },
        "bytes": null, // (response) base64 encoded bytes of the image
        "width": null, // (response) width of generated image (in pixels)
        "height": null, // (response) height of generated image (in pixels)
        "index" : 0, // index of the preview in the UI (UI-specific requirement)
        "metadata": {} // metadata map to be used by the adapter 
    }
}
```

In the response, Imaging Service will send the same JSON object with the bytes, width, height filled. 

#### Export
Export command triggers re-computation of existing previews of a single image and packs them into an archive file to be downloaded.

*PARAMETERS* section looks like this:
```json 
{
    "type" : "export", // export command type
    "permId" : "999999999-9999", // permId of the dataset
    "error" : null, // (response) exception details, if error occurs
    "index" : 0, // index of an image in dataset
    "url": null, // (response) download url where archive is located
    "export" : { // export parameters
        "@type" : "dss.dto.imaging.ImagingDataSetExport",
        "config" : {
            "Include": ["Data"], // What kind of data needs to be exported
            "Resolution": "300dpi", // DPI to be used for images 
            "Format": "Zip/jpeg" // What kind of format to be used for archive/image
    	},        
        "metadata": {} // optional metadata map to be used by adaptor
    }
}
```

#### Multi-Export
Multi-export allows to download multiple images in a single zip file.

*PARAMETERS* section looks like this:
```json 
{
    "type" : "multi-export", // export command type
    "error" : null, // (response) exception details, if error occurs
    "exports" : [{ // export parameters
        "@type" : "dss.dto.imaging.ImagingDataSetMultiExport",
        "permId" : "999999999-1111", // permId of the dataset
        "imageIndex" : 0, // image index
        "previewIndex": 0, // preview index
        "config" : {
          "Include": ["Data"] // what kind of data needs to be exported
        },
        "metadata": {} // optional metadata map to be used by adaptor
      },
      {
        "@type" : "dss.dto.imaging.ImagingDataSetMultiExport",
        "permId" : "999999999-2222", // permId of the dataset
        "imageIndex" : 0, // image index
        "previewIndex": 0, // preview index
        "config" : {
            "Include": ["Image", "Data"],  // what kind of data needs to be exported
            "Resolutions": "300dpi",  // DPI to be used for images 
            "Format": "jpeg" // what kind of image format to be used 
	    },
        "metadata": {} // optional metadata map to be used by adaptor
    }],
    "url": null // (response) download url where archive is located
}
```

### Helpful tools
[imaging-test-data](https://sissource.ethz.ch/sispub/openbis/-/tree/master/core-plugin-openbis/dist/core-plugins/imaging-test-data?ref_type=heads) is another core plugin containing some data that can be used for the tests. There is also a special python script [imaging.py](https://sissource.ethz.ch/sispub/openbis/-/blob/master/core-plugin-openbis/dist/core-plugins/imaging-test-data/1/as/master-data/nanonis_example/imaging.py?ref_type=heads) containing helpful methods for creating, updating and exporting imaging data.



## Imaging DataSet Viewer
Work-in-progress

## Imaging Gallery Viewer
Work-in-progress





