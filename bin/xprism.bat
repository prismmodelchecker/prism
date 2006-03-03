@echo off

rem Startup script for PRISM (Windows)
echo Loading PRISM...

rem PRISM home directory
rem Default is .. so it can be run directly from the bin directory.
rem Change to the actual prism directory to allow it to be run from anywhere.
set PRISM_DIR=..

rem Add PRISM to path
path=%PRISM_DIR%\lib;%path%

rem Setup classpath - jar file (for binary versions) gets priority
set CP=%PRISM_DIR%\lib\prism.jar;%PRISM_DIR%\classes;%PRISM_DIR%\lib\pepa.zip

rem Run PRISM through Java
java -classpath %CP% userinterface/GUIPrism %*

