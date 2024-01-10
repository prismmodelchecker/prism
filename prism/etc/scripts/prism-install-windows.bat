@REM Script to prepare for installation of PRISM on a clean Windows install
@REM Run this in a Windows Command Prompt
@REM e.g. cmd /K prism-install-windows.bat
@REM Then run prism-install-cygwin in a Cygwin window

@REM First, install Chocolatey
"%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe" -Command "Set-ExecutionPolicy Bypass -Scope Process -Force; [System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor 3072; iex ((New-Object System.Net.WebClient).DownloadString('https://community.chocolatey.org/install.ps1'))"
@REM Chocolatey binaries now on path, but this avoids starting a new prompt
call %ALLUSERSPROFILE%\chocolatey\bin\RefreshEnv.cmd

@REM Install some pre-requisites
choco install -y wget
choco install -y openjdk11
choco install -y nsis
setx PATH "%ProgramFiles(x86)%\NSIS;%PATH%"

@REM Get and install Cygwin, with core packages needed for PRISM install
"%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe" -Command "wget -Uri  http://cygwin.com/setup-x86_64.exe -Outfile setup-x86_64.exe
.\setup-x86_64.exe -P make -P mingw64-x86_64-gcc-g++ -P binutils -P dos2unix -P git -P wget -P unzip -P python -P nano -q -s http://ftp.inf.tu-dresden.de/software/windows/cygwin/

@REM Launch a Cygwin terminal
"%HOMEDRIVE%\cygwin64\bin\mintty.exe" -
