Development mode
================

This mode works out of the box (it doesn't require building any bundles). It loads ELN JS files and libraries with separate HTTP requests. It also uses a non-bundled version of V3 API. Therefore, any changes to those files will be immediately visible in the browser. 

Enter the following url to use the Development Mode (i.e. index.dev.html):
http://localhost:8888/openbis-test/webapp/eln-lims/index.dev.html

Production mode
===============

This mode requires ELN JS and V3 API bundles to be built first.
To build the ELN JS bundles locally please run the following Gradle task:

In ui-eln-lims:
```
./gradlew bundleJavascript
```

Enter the following url to use the Production Mode (i.e. index.html):
http://localhost:8888/openbis-test/webapp/eln-lims/

Optional:

The V3 API bundle is automatically created when Application Server `openBISDevelopmentEnvironmentASPrepare` Gradle task is executed.
In case you would like to regenerate the V3 API bundle manually please run:

In api-openbis-javascript:

```
./gradlew bundleOpenbisStaticResources
```

NOTE: The bundles should NOT be commited into the Git repository. They will be automatically generated during the build and will be part of the installer.
