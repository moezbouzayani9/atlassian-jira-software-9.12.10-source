@echo off
set SETTINGSFILE=settings.xml
set LOCALREPO=localrepo
set PATH=%cd%;%PATH%
call mvn388.bat clean install -f jira-project/pom.xml -Pbuild-from-source-dist -Dmaven.test.skip -DskipTests -Dmaven.test.skip -s %SETTINGSFILE% -Dmaven.repo.local=%cd%\%LOCALREPO% %* 
if %errorlevel% neq 0 exit /b %errorlevel%
call mvn388.bat clean install -f jira-project/jira-components/jira-webapp/pom.xml -Pbuild-from-source-dist -Dmaven.test.skip -DskipTests -Dmaven.test.skip -s %SETTINGSFILE% -Dmaven.repo.local=%cd%\%LOCALREPO% %* 
if %errorlevel% neq 0 exit /b %errorlevel%
call mvn388.bat clean package -f jira-project/jira-distribution/jira-webapp-dist/pom.xml -Pbuild-from-source-dist -Dmaven.test.skip -DskipTests -Dmaven.test.skip -s %SETTINGSFILE% -Dmaven.repo.local=%cd%\%LOCALREPO% %* 
if %errorlevel% neq 0 exit /b %errorlevel%
