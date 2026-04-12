@echo off
REM ----------------------------------------------------------------------------
REM Maven Wrapper
REM ----------------------------------------------------------------------------
REM Copyright (c) 2010-2024 The Apache Software Foundation
REM
SET MAVEN_PROJECTBASEDIR=%~dp0
IF "%MAVEN_PROJECTBASEDIR%"=="" SET MAVEN_PROJECTBASEDIR=.
SET MAVEN_PROJECTBASEDIR=%MAVEN_PROJECTBASEDIR:~0,-1%

IF DEFINED JAVA_HOME (
    SET "JAVA_EXE=%JAVA_HOME%\bin\java"
) ELSE (
    SET "JAVA_EXE=java"
)

SET "WRAPPER_JAR=%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar"
SET "WRAPPER_PROPERTIES=%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.properties"

IF NOT EXIST "%WRAPPER_JAR%" (
    echo Maven Wrapper jar not found in %WRAPPER_JAR%
    echo Please ensure .mvn\wrapper\maven-wrapper.jar is present.
    exit /b 1
)

"%JAVA_EXE%" -Dmaven.multiModuleProjectDirectory="%MAVEN_PROJECTBASEDIR%" -cp "%WRAPPER_JAR%" org.apache.maven.wrapper.MavenWrapperMain %*
