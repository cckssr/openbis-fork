# Custom imaging adapters configuration


In order to create custom adapters you need to do following things:

1. Create a new DSS-service core-plugin and enable it in the main core-plugin.properties
2. Write java classes([*](#generic-imaging-technology-jar)), it must either: 
   * implement ch.ethz.sis.openbis.generic.server.as.plugins.imaging.adaptor.imaging.IImagingDataSetAdaptor interface 
   * extend a class that does it (e.g. ch.ethz.sis.openbis.generic.server.as.plugins.imaging.adaptor.imaging.ImagingDataSetAbstractPythonAdaptor)
3. Compile it into .jar file and drop it in the lib directory (e.g. imaging-nanonis/1/dss/services/imaging-nanonis/lib/my_custom_adapters.jar)
4. Create plugin.properties - it can be empty
5. (*Optional*) if your class makes use of java.util.Properties, you have to add them to the service.properties of the DSS with a prefix `imaging`, e.g:
   ```properties
    imaging.nanonis.my-python-script-path=${core-plugins-folder}/path/to/the/script/in/my/newly/created/plugin/script.py
    ```




# Development environment notes
Please remember to link jar files in the compilation paths of the DSS in the build.gradle:
```java
 datastoreExecRuntime files("../core-plugin-openbis/dist/core-plugins/imaging/1/dss/services/imaging/lib/openBIS-imaging-technology.jar"),
            files("../core-plugin-openbis/dist/core-plugins/imaging-nanonis/1/dss/services/imaging-nanonis/lib/premise-adapters.jar")
```


### Generic Imaging Technology jar
To access GenericImagingTechnology classes for custom adapter compilation, you need to link technology jar:
core-plugin-openbis/dist/core-plugins/imaging/`VERSION_NUMBER`/dss/services/imaging/lib/openBIS-imaging-technology.jar
