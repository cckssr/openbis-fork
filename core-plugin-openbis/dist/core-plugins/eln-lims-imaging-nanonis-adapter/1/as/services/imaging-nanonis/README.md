# Imaging-nanonis service

This repository contains source code of imaging-nanonis service for OpenBIS, it contains:

- `lib` directory with java project implementing classes needed for custom logic for handling imaging adapters. Java classes are responsible for communication with core `imaging` plugin, they allow for additional pre/post processing.
- python scripts that convert `.SXM` and `.DAT` files into images, they are triggered by Java adaptors specified in `lib` directory.


## Python scripts

- `nanonis_core.py` - core script for converting raw data into image, applying filters and computing output.
- `nanonis_sxm.py` - script for converting and validating input parameters for SXM image generation. 
- `nanonis_dat.py` - script for converting and validating input parameters for DAT and Spectra image generation.
- `spmpy` - new library for image generation.
- `spmpy_terry.py` - deprecated library for image generation. 

# Adding new adaptor

1. Create new Java class implementing interface `IImagingDataSetAdaptor` from imaging core technology in `./lib/imaging-nanonis-adapters-sources/source/java/ch/ethz/sis/openbis/generic/server/as/plugins/imaging/adaptor`
2. Rebuild the `premise-adapters.jar`
3. Add new adaptor (full package name) in `plugin.properties` file
4. Restart OpenBIS

# Development environment notes
In development environment, please remember to link jar files in the compilation paths of the AS in the build.gradle:
```java
asExecRuntime files("../core-plugin-openbis/dist/core-plugins/eln-lims-imaging-core/1/as/api-listener/imaging-dataset-interceptor/lib/imaging-dataset-interceptor.jar"),
      files("../core-plugin-openbis/dist/core-plugins/eln-lims-imaging-core/1/as/services/imaging/lib/openBIS-imaging-technology.jar"),
      files("../core-plugin-openbis/dist/core-plugins/eln-lims-imaging-nanonis-adapter/1/as/services/imaging-nanonis/lib/premise-adapters.jar"),
      files("../core-plugin-openbis/dist/core-plugins/eln-lims-imaging-test-adapter/1/as/services/imaging-test/lib/imaging-test-adapters.jar")

```
