@echo off

rem Startup script for PRISM (Windows)
echo Loading PRISM...

rem PRISM home directory
rem Default is .. so it can be run directly from the bin directory.
rem An example would be: set PRISM_DIR=c:\Program Files\prism-3.2
rem Note: Do not put quotes ("...") around the path.
set PRISM_DIR=..

rem Add PRISM to path
path=%PRISM_DIR%\lib;%path%

rem Set up CLASSPATH:
rem  - PRISM jar file (for binary versions) (gets priority)
rem  - classes directory (most PRISM classes)
rem  - top-level directory (for images, dtds)
rem  - lib/pepa.zip (PEPA stuff)
rem  - lib/{jcommon,jfreechart}.jar (JFreeChart stuff)
rem  - lib/epsgraphics.jar (Java EPS Graphics library)
set CP=%PRISM_DIR%\lib\prism.jar;%PRISM_DIR%\classes;%PRISM_DIR%;%PRISM_DIR%\lib\pepa.zip;%PRISM_DIR%\lib\jcommon.jar;%PRISM_DIR%\lib\jfreechart.jar;%PRISM_DIR%\lib\epsgraphics.jar

rem Run PRISM through Java
rem start javaw -Djava.library.path=%PRISM_DIR%\lib -classpath %CP% userinterface/GUIPrism %*
start javaw -Djava.library.path="%PRISM_DIR%\lib" -classpath "%CP%" userinterface/GUIPrism %*
exit