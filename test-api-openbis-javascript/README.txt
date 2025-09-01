For development and bug fixing the tests have to run in a Web browser. 

Start up everything by executing inside the build/ directory:

./gradlew -Ddev=yes test-api-openbis-javascript:clean test-api-openbis-javascript:test

This will start up openBIS AS and two DSSs. 

To run ELN tests with the following command:

./gradlew -Deln=yes -Ddev=yes test-api-openbis-javascript:clean test-api-openbis-javascript:test

This will start up openBIS AS and only one DSS. 

When the console output is no longer busy Firefox should be started.
Enter the following URLs:

http://localhost:20000/openbis/webapp/openbis-v3-api-test/?webapp-code=openbis-v3-api-test
http://localhost:20000/openbis/webapp/openbis-test/?webapp-code=openbis-test
http://localhost:20000/openbis/webapp/eln-test/?webapp-code=eln-test


If a test fails you can click on the test and only the failed test will be shown.

You can change the test code (e.g. in servers/common/core-plugins/tests/1/as/webapps/openbis-test/html/openbis-test.js).
To see the changes you have to reload the frame (not the application) in the Web browser.

Jenkins
=======

On Jenkins screenshots are taken every 20 seconds they are stored inside the workspace: 
test-api-openbis-javascript/servers/common/openBIS-server/targets/dist/JsTestElnSelenium/runELNTests

Geckodriver
==========

If you want to develop tests on your machine make sure that you use correct Geckodriver for your operation system.

You can find it here:
https://github.com/mozilla/geckodriver/releases

If you are using MAC OS you can easily install Geckodriver using command:
brew install geckodriver

Otherwise you need to specify the path to your Geckodriver in makeGeckodriverExecutable task.

Some Tips:
==========

TypeScript:
-----------

All tests that have ".ts" extension are written in TypeScript. Therefore, before then can be run, they need to be compiled to JavaScript.
To do that you need to execute Gradle task named "compileTypeScript" in "test-api-openbis-javascript" module.

The TypeScript compilation task is run automatically as part of the main "test" task that boots up the whole JavaScript tests machinery,
still if you develop your tests without shutting everything down, after each change of the TypeScript code you need to remember to run the TypeScript compilation again.
Otherwise, the browser will execute an old JavaScript code that does not contain your change.

Developing:
-----------

Here are some tips for speed up development:

* Change the code not only in the original Javascript file but also in the file in targets/gradle/webapps/webapp. 
  Otherwise the old code is executed. This isn't necessary for testing classes.
  If you change only API code you can do the following command:
  
  cp -R <root folder>/server-application-server/source/java/ch/systemsx/cisd/openbis/public/resources/ <root folder>/test-api-openbis-javascript/targets/gradle/webapps/webapp/resources/
  
* In case of changes of Java classes stop and restart (using ./gradlew test-api-openbis-javascript:clean test-api-openbis-javascript:test) is needed.
  The test server is available much faster after outcommenting the following lines in build.gradle of 
  project core-plugin-openbis:
  
  war.dependsOn compileGwt
  war.dependsOn signWebStartJars
  
  Note, that when compileGwt is not executed changes in JS code should be copy after server start up with 
  the above mentioned copy command.  

Debugging:
----------

Comment the line

require.urlArgs = 'now=' + Date.now();

in servers/common/core-plugins/tests/1/as/webapps/openbis-v3-api-test/html/index.html if you want to debug
Javascript in the browser. Don't forget to bring the statement back before you do development again.


