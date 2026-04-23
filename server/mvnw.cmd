@ECHO OFF
SETLOCAL
SET "MAVEN_PROJECTBASEDIR=%~dp0"
IF "%MAVEN_PROJECTBASEDIR:~-1%"=="\" SET "MAVEN_PROJECTBASEDIR=%MAVEN_PROJECTBASEDIR:~0,-1%"
SET "WRAPPER_JAR=%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar"

SET "JAVACMD=java"
IF DEFINED JAVA_HOME SET "JAVACMD=%JAVA_HOME%\bin\java.exe"

"%JAVACMD%" -version >NUL 2>&1
IF ERRORLEVEL 1 (
  ECHO java not found. Install JDK 17+ and set JAVA_HOME or PATH.
  EXIT /B 1
)

"%JAVACMD%" -classpath "%WRAPPER_JAR%" "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" org.apache.maven.wrapper.MavenWrapperMain %*
EXIT /B %ERRORLEVEL%
