@echo off
setlocal
:PROMPT
SET AREYOUSURE=N
SET /P AREYOUSURE=Are you sure you want to install openBIS Drive application under %USERPROFILE%\AppData\Local\openbis-drive\ (Y/[N])?
IF /I "%AREYOUSURE%" NEQ "Y" GOTO END


mkdir  "%USERPROFILE%\AppData\Local\openbis-drive\launch-scripts"
xcopy  .\launch-scripts "%USERPROFILE%\AppData\Local\openbis-drive\launch-scripts"

echo Application files copied to %USERPROFILE%\AppData\Local\openbis-drive

powershell.exe -command "$WshShell = New-Object -COMObject WScript.Shell ; $Shortcut = $WshShell.CreateShortcut(\"openbis-drive.lnk\") ; $Shortcut.TargetPath = \"%USERPROFILE%\AppData\Local\openbis-drive\launch-scripts\openbis-drive-gui.bat\" ; $Shortcut.IconLocation = \"%USERPROFILE%\AppData\Local\openbis-drive\launch-scripts\openbis-drive-icon-small.ico\" ; $Shortcut.Save() "

echo You can copy openbis-drive.lnk to your desktop to use it as a launch-icon for the graphical interface

:END
SET /P FINISH=Finish
endlocal
