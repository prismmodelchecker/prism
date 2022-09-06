@echo off

REM This batch file compiles the lp_solve driver program with the Borland C++ 5.5 compiler for Windows


set src=../shared/commonlib.c ../shared/mmio.c ../shared/myblas.c ../ini.c ../lp_rlp.c ../lp_crash.c ../bfp/bfp_LUSOL/lp_LUSOL.c ../bfp/bfp_LUSOL/LUSOL/lusol.c ../lp_Hash.c ../lp_lib.c ../lp_wlp.c ../lp_matrix.c ../lp_mipbb.c ../lp_MPS.c ../lp_params.c ../lp_presolve.c ../lp_price.c ../lp_pricePSE.c ../lp_report.c ../lp_scale.c ../lp_simplex.c lp_solve.c ../lp_SOS.c ../lp_utils.c ../yacc_read.c ../lp_MDO.c ../colamd/colamd.c
set c=bcc32 -w-8004 -w-8057
rem  -DLLONG=__int64

set PLATFORM=win32
if not exist bin\%PLATFORM%\*.* md bin\%PLATFORM%

%c% -I.. -I../bfp -I../bfp/bfp_LUSOL -I../bfp/bfp_LUSOL/LUSOL -I../colamd -I../shared -a8 -DWIN32 -DYY_NEVER_INTERACTIVE=1 -DPARSER_LP -DINVERSE_ACTIVE=INVERSE_LUSOL -DRoleIsExternalInvEngine -ebin\%PLATFORM%\lp_solve.exe %src%

if exist *.obj del *.obj
set PLATFORM=
