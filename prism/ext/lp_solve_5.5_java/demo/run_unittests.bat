@echo off

set c=cl

REM determine platform (win32/win64)
echo main(){printf("SET PLATFORM=win%%d\n", (int) (sizeof(void *)*8));}>platform.c
%c% /nologo platform.c /Feplatform.exe
del platform.c
platform.exe >platform.bat
del platform.exe
call platform.bat
del platform.bat

set patho=%path%
set path=..\..\..\xli\xli_CPLEX\bin\%PLATFORM%;..\..\..\xli\xli_LINDO\bin\%PLATFORM%;..\..\..\bfp\bfp_LUSOL\bin\%PLATFORM%;%path%

java -cp .\unittests.jar;..\lib\lpsolve55j.jar;..\lib\junit.jar LpSolveTest

set path=%patho%