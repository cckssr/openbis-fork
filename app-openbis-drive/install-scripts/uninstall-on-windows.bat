@echo off
setlocal
:PROMPT
SET AREYOUSURE=N
SET /P AREYOUSURE=Are you sure you want to remove openBIS Drive application under %USERPROFILE%\AppData\Local\openbis-drive\launch-scripts (Y/[N])?
IF /I "%AREYOUSURE%" NEQ "Y" GOTO DELETECONF
powershell.exe -command "$result = Get-WmiObject -Class win32_process -Filter \"Name LIKE 'javaw.exe'\" | Select ProcessId, CommandLine ; foreach ( $i in $result ) { if ( $i.CommandLine -Match '-cp app-openbis-drive-full.jar ch.openbis.drive.DriveAPIService' ) { Stop-Process -Force $i.ProcessId ; }}"

del /F /Q %USERPROFILE%\AppData\Local\openbis-drive\launch-scripts\**
rmdir /S /Q %USERPROFILE%\AppData\Local\openbis-drive\launch-scripts

if not exist %USERPROFILE%\AppData\Local\openbis-drive\launch-scripts\ (
	echo Application files deleted from %USERPROFILE%\AppData\Local\openbis-drive\launch-scripts
) else (
	echo Error deleting %USERPROFILE%\AppData\Local\openbis-drive\launch-scripts
)

:DELETECONF
SET AREYOUSURE=N
SET /P AREYOUSURE=Do you  want to remove openBIS Drive configuration under %USERPROFILE%\AppData\Local\openbis-drive\state (Y/[N])?
IF /I "%AREYOUSURE%" NEQ "Y" GOTO END
 
del /F /Q %USERPROFILE%\AppData\Local\openbis-drive\state\**
rmdir /S /Q %USERPROFILE%\AppData\Local\openbis-drive\state

if not exist %USERPROFILE%\AppData\Local\openbis-drive\state\ (
	echo Application files deleted from %USERPROFILE%\AppData\Local\openbis-drive\state
) else (
	echo Error deleting %USERPROFILE%\AppData\Local\openbis-drive\state
)


:END
SET /P FINISH=Finish
endlocal
