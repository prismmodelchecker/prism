;==============================================================================

; You should always call makensis in the following way:
;
; > makensis /NOCD /DPRISM_NAME="PRISM X.y" /DPRISM_BUILD="prism-X.y"
;   /DPRISM_BINDISTSUFFIX="winXX" /DPRISM_DIR="" installer_script.nsi
;
; where:
; * PRISM X.y" is the name of the program displayed to the user
; * prism-3.0 is the4 name of the build (no spaces)
; * winXX is win32 or win6
; The PRISM_DIR variable should be set to "" if the working directory
; is the prism directory or to the prism directory otherwise (ending with `\').

# --------------------------------------------------------------------------- #

;Page license 	; Uncomment if you want the license page to be shown. 
Page components	; Choice in components (shortcuts mostly).
Page directory	; Prompts for directory of installation.
Page instfiles	; Copies the files.

UninstPage uninstConfirm
UninstPage instfiles

# --------------------------------------------------------------------------- #

Name            "${PRISM_NAME}"
OutFile         "..\${PRISM_BUILD}-${PRISM_BINDISTSUFFIX}-installer.exe"

CRCCheck        on

LicenseText     "For your information:" "Next >"
LicenseData     "${PRISM_DIR}COPYING.txt"

InstallDir      "$PROGRAMFILES64\${PRISM_BUILD}"

SubCaption		0 ": Licensing Information"

DirText         "Please select an installation folder for ${PRISM_NAME}."

UninstallText   "This will uninstall ${PRISM_NAME}, including any examples."

ComponentText 	"Please select the components you wish to install."
			
# --------------------------------------------------------------------------- #

Section ""
    SetOutPath "$INSTDIR"

    ;WriteRegStr HKEY_LOCAL_MACHINE "SOFTWARE\University of Oxford\${PRISM_BUILD}" "" "$INSTDIR"
    ;WriteRegStr HKEY_LOCAL_MACHINE "Software\Microsoft\Windows\CurrentVersion\Uninstall\${PRISM_BUILD}" "DisplayName" "${PRISM_NAME}(remove only)"
    ;WriteRegStr HKEY_LOCAL_MACHINE "Software\Microsoft\Windows\CurrentVersion\Uninstall\${PRISM_BUILD}" "UninstallString" '"$INSTDIR\uninstall.exe"'

    WriteUninstaller "$INSTDIR\uninstall.exe"

SectionEnd

Section "${PRISM_NAME}"
	SectionIn RO ; RO=compulsory
	
    SetOutPath "$INSTDIR\etc"
    File /r etc\*.*
    
    SetOutPath "$INSTDIR\lib"
    File /r lib\*.*
    
    SetOutPath "$INSTDIR"
    File "CHANGELOG.txt"
    File "COPYING.txt"
    FILE "install.sh"
    FILE "README.txt"

    SetOutPath "$INSTDIR\bin"
    File bin\prism.bat
    File bin\xprism.bat
    
    SetOutPath "$INSTDIR\examples"
    File /r examples\*.*

    SetOutPath "$INSTDIR\doc"
    File /r doc\*.*
SectionEnd

Section "Desktop shortcut"
	
	SetOutPath "$INSTDIR\bin"
	
	CreateShortCut  "$DESKTOP\${PRISM_NAME}.lnk" \
                    "$INSTDIR\bin\xprism.bat" ""                            \
                    "$INSTDIR\etc\icons\prism.ico" 0                                \
                    SW_SHOWMINIMIZED "" "${PRISM_NAME} (GUI mode)"
                    
SectionEnd

Section "Start menu shortcuts"
                    
    CreateDirectory "$SMPROGRAMS\${PRISM_NAME}"

    SetOutPath "$INSTDIR\bin"

    CreateShortCut  "$SMPROGRAMS\${PRISM_NAME}\PRISM (GUI).lnk" \
                    "$INSTDIR\bin\xprism.bat" ""                            \
                    "$INSTDIR\etc\icons\prism.ico" 0                                \
                    SW_SHOWMINIMIZED "" "Runs the PRISM GUI"

    CreateShortCut  "$SMPROGRAMS\${PRISM_NAME}\PRISM (console).lnk" \
                    "$SYSDIR\cmd.exe" ""                            \
                    "$SYSDIR\cmd.exe" 0                             \
                    SW_SHOWNORMAL "" "Opens a console for command-line usage of PRISM"

    SetOutPath "$INSTDIR\doc"

    CreateShortCut  "$SMPROGRAMS\${PRISM_NAME}\Manual (local).lnk" \
                    "$INSTDIR\doc\manual\index.html" ""                            \
                    "" 0                             \
                    SW_SHOWNORMAL "" "The PRISM manual (local copy)"

    CreateShortCut  "$SMPROGRAMS\${PRISM_NAME}\Manual (online).lnk" \
                    "http://www.prismmodelchecker.org/manual/" ""                            \
                    "" ""                             \
                    SW_SHOWNORMAL "" "The PRISM manual (online version)"

    CreateShortCut  "$SMPROGRAMS\${PRISM_NAME}\Web site.lnk" \
                    "http://www.prismmodelchecker.org/" ""                            \
                    "" 0                             \
                    SW_SHOWNORMAL "" "The PRISM web site"

    SetOutPath "$INSTDIR"

    CreateShortCut  "$SMPROGRAMS\${PRISM_NAME}\Uninstall.lnk" \
                    "$INSTDIR\uninstall.exe" ""                             \
                    "$INSTDIR\uninstall.exe" 0                              \
                    SW_SHOWNORMAL "" "Uninstalls ${PRISM_NAME} from your system"
    SectionEnd

Section Uninstall

    RMDir /r "$INSTDIR\bin\"
    RMDIR /r "$INSTDIR\etc\"
    RMDIR /r "$INSTDIR\lib\"

    Delete "$INSTDIR\CHANGELOG.txt"
    Delete "$INSTDIR\COPYING.txt"
    Delete "$INSTDIR\install.sh"
    Delete "$INSTDIR\README.txt"

    RMDir /r "$INSTDIR\examples\"

    RMDIR /r "$INSTDIR\doc\"

    RMDir /r "$SMPROGRAMS\${PRISM_NAME}"

    Delete "$DESKTOP\${PRISM_NAME}.lnk"

    ;DeleteRegKey HKEY_LOCAL_MACHINE "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\${PRISM_BUILD}"

    Delete $INSTDIR\uninstall.exe

    RMDir "$INSTDIR"

SectionEnd




