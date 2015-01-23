##############################################
#  NB: This is the main Makefile for PRISM.  #
#      It calls all the other Makefiles in   #
#      subdirectories, passing in all the    #
#      options configured here.              #
##############################################

####################
# Operating system #
####################

# OSTYPE needs to be one of: linux, solaris, cygwin, darwin
# This makefile will try to detect which one of these is appropriate.
# If this detection does not work, or you wish to override it,
# either uncomment one of the lines directly below
# or pass a value to make directly, e.g.: make OSTYPE=linux

#OSTYPE = linux
#OSTYPE = solaris
#OSTYPE = cygwin
#OSTYPE = darwin

ifdef OSTYPE
	# Look for common variants, e.g. gnu-linux -> linux
	ifneq (,$(findstring linux, $(OSTYPE)))
	  OSTYPE = linux
	endif
	ifneq (,$(findstring solaris, $(OSTYPE)))
	  OSTYPE = solaris
	endif
	ifneq (,$(findstring cygwin, $(OSTYPE)))
	  OSTYPE = cygwin
	endif
	# For Cygwin , OSTYPE is sometimes set to "posix"
	ifneq (,$(findstring posix, $(OSTYPE)))
	  OSTYPE = cygwin
	endif
	ifneq (,$(findstring darwin, $(OSTYPE)))
	  OSTYPE = darwin
	endif
else
	# If OSTYPE is not defined/available, try uname
	ifneq (,$(findstring Linux, $(shell uname -s)))
		OSTYPE = linux
	endif
	ifneq (,$(findstring SunOS, $(shell uname -s)))
		OSTYPE = solaris
	endif
	ifneq (,$(findstring CYGWIN, $(shell uname -s)))
		OSTYPE = cygwin
	endif
	ifneq (,$(findstring Darwin, $(shell uname -s)))
		OSTYPE = darwin
	endif
endif

################
# Architecture #
################

# For Linux, we use uname to see if we are on a 64-bit (AMD64 or Itanium) machine
ifeq ($(OSTYPE),linux)
	ifneq (,$(findstring 86_64, $(shell uname -m)))
		ARCH = amd64
	endif
	ifneq (,$(findstring ia64, $(shell uname -m)))
		ARCH = ia64
	endif
endif
# For Mac/Windows, we decide whether to build in 64-bit mode based on
# whether java is 32/64-bit (since these need to match)
ifeq ($(OSTYPE),darwin)
    JAVA_VERSION_STRING = $(shell java -version 2>&1)
    ifneq (,$(findstring 64-bit, $(JAVA_VERSION_STRING)))
        ARCH = x86_64
    endif
    ifneq (,$(findstring 64-Bit, $(JAVA_VERSION_STRING)))
        ARCH = x86_64
    endif
endif
ifeq ($(OSTYPE),cygwin)
    JAVA_VERSION_STRING = $(shell java -version 2>&1)
    ifneq (,$(findstring 64-bit, $(JAVA_VERSION_STRING)))
        ARCH = x86_64
    endif
    ifneq (,$(findstring 64-Bit, $(JAVA_VERSION_STRING)))
        ARCH = x86_64
    endif
endif

########
# Java #
########

# JAVA_DIR needs to be set to the location of your Java installation.
# This makefile will try to detect this automatically based on the location of the javac command.
# If this detection does not work, or you wish to override it,
# either set the variable yourself by uncommenting and/or modifying one of the lines below
# or pass a value to make directly, e.g.: make JAVA_DIR=/usr/java

# Find javac
DETECT_JAVAC = $(shell src/scripts/findjavac.sh 2> /dev/null)

# Find directory containing javac
ifeq ("$(DETECT_JAVAC)","")
  JAVA_DIR =
else
  ifneq (darwin,$(OSTYPE))
    JAVA_DIR = $(shell dirname "$(DETECT_JAVAC)" | sed 's/\/bin//')
  else
    JAVA_DIR = $(shell dirname "$(DETECT_JAVAC)" | sed 's/\/Commands//')
  endif
endif

# As a backup way of detecting JAVA_DIR, run java_home
JAVA_DIR_BACKUP = $(shell \
	if [ -f /usr/libexec/java_home ]; then /usr/libexec/java_home; \
	else echo ""; fi )

#JAVA_DIR =	/usr/java
#JAVA_DIR =	/usr/java/j2sdk1.4.2
#JAVA_DIR =	/bham/java/packages/j2sdk1.4.2
#JAVA_DIR =	/cygdrive/c/java/j2sdk1.4.2
#JAVA_DIR =	/System/Library/Frameworks/JavaVM.framework

##################
# Compilers etc. #
##################

C = gcc
CPP = g++
LD = $(CPP)
JAVAC = javac
JAVACC = javacc
JAVAH = javah

##############
# Flags etc. #
##############

DEBUG = 
#DEBUG = -g

OPTIMISE = -O3
#OPTIMISE =

# Flags for compilation/linking
# Flags to generate shared libraries
# Executable/library naming conventions
# Option to pass to CUDD makefile
# Suffix for binary distribution directory
# Place to look for (JNI) headers
# (requires GNU make for conditional evaluation)

# Linux
ifeq ($(OSTYPE),linux)
	ifeq ($(ARCH),amd64)
		# Position Independent Code required on AMD64/Itanium
		CUDD_XCFLAGS = -m64 -fPIC -DPIC -DHAVE_IEEE_754 -DBSD -DSIZEOF_VOID_P=8 -DSIZEOF_LONG=8 $(DEBUG)
		CFLAGS = $(CUDD_XCFLAGS) $(OPTIMISE)
		CPPFLAGS = $(CUDD_XCFLAGS) $(OPTIMISE)
		LDFLAGS = $(CUDD_XCFLAGS) $(OPTIMISE)
		BINDISTSUFFIX = linux64
	else
	ifeq ($(ARCH),ia64)
		# Position Independent Code required on AMD64/Itanium
		# Note: We omit the -m64 flag from here since it seems to be unsupported by gcc on IA64
		CUDD_XCFLAGS = -fPIC -DPIC -DHAVE_IEEE_754 -DBSD -DSIZEOF_VOID_P=8 -DSIZEOF_LONG=8 $(DEBUG)
		CFLAGS = $(CUDD_XCFLAGS) $(OPTIMISE)
		CPPFLAGS = $(CUDD_XCFLAGS) $(OPTIMISE)
		LDFLAGS = $(CUDD_XCFLAGS) $(OPTIMISE)
		BINDISTSUFFIX = linux64
	else
		CUDD_XCFLAGS = -m32 -malign-double -DHAVE_IEEE_754 -DBSD $(DEBUG)
		CFLAGS = $(CUDD_XCFLAGS) $(OPTIMISE)
		CPPFLAGS = $(CUDD_XCFLAGS) $(OPTIMISE)
		LDFLAGS = $(CUDD_XCFLAGS) $(OPTIMISE)
		BINDISTSUFFIX = linux32
	endif
	endif
	BIN_TARGETS=prism.linux xprism.linux
	JFLAGS = -encoding UTF8
	SHARED = -shared
	#SHARED = -G
	EXE =
	LIBPREFIX = lib
	LIBSUFFIX = .so
	LIBMATH = -lm
	CLASSPATHSEP = :
endif
# Solaris
ifeq ($(OSTYPE),solaris)
	CUDD_XCFLAGS = -mcpu=ultrasparc -DHAVE_IEEE_754 -DUNIX100 -DEPD_BIG_ENDIAN $(DEBUG)
	CFLAGS = $(CUDD_XCFLAGS) $(OPTIMISE)
	CPPFLAGS = $(CUDD_XCFLAGS) $(OPTIMISE)
	LDFLAGS = $(CUDD_XCFLAGS) $(OPTIMISE)
	BINDISTSUFFIX = solaris
	BIN_TARGETS=prism.linux xprism.linux
	JFLAGS = -encoding UTF8
	SHARED = -shared -mimpure-text
	EXE =
	LIBPREFIX = lib
	LIBSUFFIX = .so
	LIBMATH = -lm
	CLASSPATHSEP = :
endif
# Cygwin
ifeq ($(OSTYPE),cygwin)
	ifeq ($(ARCH),x86_64)
		C = /usr/bin/x86_64-w64-mingw32-gcc
		CPP = /usr/bin/x86_64-w64-mingw32-g++
		CUDD_XCFLAGS = -fPIC -DPIC -malign-double -DHAVE_IEEE_754 -DHAVE_GETRLIMIT=0 -DRLIMIT_DATA_DEFAULT=268435456 -DHAVE_SYS_RESOURCE_H=0 -DHAVE_SYS_WAIT_H=0 -DSIZEOF_VOID_P=8 -DSIZEOF_LONG=4 -fpermissive $(DEBUG) -static-libgcc -static-libstdc++
		CFLAGS = $(CUDD_XCFLAGS) $(OPTIMISE)
		CPPFLAGS = $(CUDD_XCFLAGS) $(OPTIMISE)
		LDFLAGS = $(CUDD_XCFLAGS) $(OPTIMISE) -Wl,--add-stdcall-alias
		BINDISTSUFFIX = win64
	else
		C = /usr/bin/i686-w64-mingw32-gcc
		CPP = /usr/bin/i686-w64-mingw32-g++
		CUDD_XCFLAGS = -march=i686 -malign-double -DHAVE_IEEE_754 -DHAVE_GETRLIMIT=0 -DRLIMIT_DATA_DEFAULT=268435456 -DHAVE_SYS_RESOURCE_H=0 -DHAVE_SYS_WAIT_H=0 $(DEBUG) -static-libgcc -static-libstdc++
		CFLAGS = $(CUDD_XCFLAGS) $(OPTIMISE)
		CPPFLAGS = $(CUDD_XCFLAGS) $(OPTIMISE)
		LDFLAGS = $(CUDD_XCFLAGS) $(OPTIMISE) -Wl,--add-stdcall-alias
		BINDISTSUFFIX = win32
	endif
	BIN_TARGETS=prism.cygwin xprism.linux prism.bat.win xprism.bat.win
	JFLAGS = -encoding UTF8
	JAVACC = javacc.bat
	SHARED = -shared
	#SHARED = -G
	EXE = .exe
	LIBPREFIX =
	LIBSUFFIX = .dll
	LIBMATH = 
	CLASSPATHSEP = ;
endif
# Darwin
ifeq ($(OSTYPE),darwin)
	ifeq ($(ARCH),x86_64)
		CUDD_XCFLAGS = -arch x86_64 -fPIC -DPIC -DHAVE_IEEE_754 -DBSD -DSIZEOF_VOID_P=8 -DSIZEOF_LONG=8 -fno-common $(DEBUG)
		CFLAGS = $(CUDD_XCFLAGS) $(OPTIMISE)
		CPPFLAGS = $(CUDD_XCFLAGS) $(OPTIMISE)
		LDFLAGS = $(CUDD_XCFLAGS) $(OPTIMISE) -Wl,-search_paths_first
		BINDISTSUFFIX = osx64
		BIN_TARGETS=prism.darwin64 xprism.linux
	else
		CUDD_XCFLAGS = -arch i386 -DHAVE_IEEE_754 -DBSD -fno-common $(DEBUG)
		CFLAGS = $(CUDD_XCFLAGS) $(OPTIMISE)
		CPPFLAGS = $(CUDD_XCFLAGS) $(OPTIMISE)
		LDFLAGS = $(CUDD_XCFLAGS) $(OPTIMISE) -Wl,-search_paths_first
		BINDISTSUFFIX = osx32
		BIN_TARGETS=prism.darwin32 xprism.linux
	endif
	JFLAGS = -encoding UTF8
	SHARED = -dynamiclib
	EXE =
	LIBPREFIX = lib
	LIBSUFFIX = .dylib
	LIBMATH = -lm
	CLASSPATHSEP = :
endif

###############
# Directories #
###############

# Note that these are all relative to the PRISM directory
# to make the distribution more 'portable'.
# If this is a problem, the best solution is to create symlinks.

# For CUDD, we default either to ./cudd or, if that does not exist, ../cudd
# To override, comment out the first line and use the second (or specify from the command-line)
CUDD_DIR =		$(shell if [ -d cudd ]; then echo cudd; else echo ../cudd; fi )
#CUDD_DIR =		cudd

SRC_DIR =		src
CLASSES_DIR =	classes
OBJ_DIR =		obj
LIB_DIR =		lib
INCLUDE_DIR =	include

# Now we locate the JNI header files jni.h and jni_md.h
# (in fact this is the only reason we need JAVA_DIR)
JAVA_JNI_H_DIR = $(shell \
	if [ -f "$(JAVA_DIR)"/include/jni.h ]; then echo "$(JAVA_DIR)"/include; \
	elif [ -f "$(JAVA_DIR)"/Headers/jni.h ]; then echo "$(JAVA_DIR)"/Headers; \
	elif [ -f "$(JAVA_DIR_BACKUP)"/include/jni.h ]; then echo "$(JAVA_DIR_BACKUP)"/include; \
	elif [ -f "$(JAVA_DIR_BACKUP)"/Headers/jni.h ]; then echo "$(JAVA_DIR_BACKUP)"/Headers; \
	else echo ""; fi )
JAVA_JNI_MD_H_DIR = $(shell (ls "$(JAVA_JNI_H_DIR)"/jni_md.h "$(JAVA_JNI_H_DIR)"/*/jni_md.h | head -n 1 | sed 's/\/jni_md.h//') 2>/dev/null)
JAVA_INCLUDES = -I $(JAVA_JNI_H_DIR) -I $(JAVA_JNI_MD_H_DIR)

#########################
# Main part of Makefile #
#########################

MAKE_DIRS = dd jdd odd dv prism mtbdd sparse hybrid parser settings userinterface pepa/compiler simulator jltl2ba jltl2dstar explicit pta param strat

EXT_PACKAGES = lpsolve55 lp_solve_5.5_java

.PHONY: clean javadoc tests

default: all

all: cuddpackage extpackages prism

cuddpackage: checks
	@if [ "$(CUDD_DIR)" = "" ]; then echo "Error: Cannot find CUDD"; exit 1; fi
	@if [ ! -d "$(CUDD_DIR)" ]; then echo "Error: Cannot find CUDD"; exit 1; fi
	@(if [ ! -h $(CUDD_DIR) ]; then \
	  echo Making cudd ...; \
	  cd $(CUDD_DIR) && \
	  $(MAKE) C="$(C)" CC="$(C)" CPP="$(CPP)" CXX="$(CPP)" \
	  XCFLAGS="$(CUDD_XCFLAGS)"; \
	else \
	  echo Skipping cudd make since it is a symlink...; \
	fi)

# use this to force build of cudd (even if dir is just a symlink)
cuddpackageforce: checks
	  @echo Making cudd ...; \
	  cd $(CUDD_DIR) && \
	  $(MAKE) C="$(C)" CC="$(C)" CPP="$(CPP)" \
	  XCFLAGS="$(CUDD_XCFLAGS)";

extpackages: checks
	@for ext in $(EXT_PACKAGES); do \
	  echo Making $$ext ...; \
	  (cd ext/$$ext && \
	  $(MAKE) \
	  OSTYPE="$(OSTYPE)" \
	  ARCH="$(ARCH)" \
	  C="$(C)" \
	  CPP="$(CPP)" \
	  LD="$(LD)" \
	  CFLAGS="$(CFLAGS)" \
	  CPPFLAGS="$(CPPFLAGS)" \
	  LDFLAGS="$(LDFLAGS)" \
	  SHARED="$(SHARED)" \
	  LIBPREFIX="$(LIBPREFIX)" \
	  LIBSUFFIX="$(LIBSUFFIX)" \
	  LIBMATH="$(LIBMATH)" \
	  BINDISTSUFFIX="$(BINDISTSUFFIX)" \
	  JAVA_DIR="$(JAVA_DIR)" \
	  JAVA_JNI_H_DIR="$(JAVA_JNI_H_DIR)" \
	  JAVA_JNI_MD_H_DIR="$(JAVA_JNI_MD_H_DIR)" \
	  ) || exit 1; \
	done

prism: checks make_dirs bin_scripts

make_dirs:
	@mkdir -p bin classes obj/dd obj/jdd obj/odd obj/dv obj/prism obj/mtbdd obj/sparse obj/hybrid obj/simulator
	@for dir in $(MAKE_DIRS); do \
	  echo Making src/$$dir ...; \
	  (cd src/$$dir && \
	  $(MAKE) \
	  CUDD_DIR="$(CUDD_DIR)" \
	  SRC_DIR="$(SRC_DIR)" \
	  CLASSES_DIR="$(CLASSES_DIR)" \
	  OBJ_DIR="$(OBJ_DIR)" \
	  LIB_DIR="$(LIB_DIR)" \
	  INCLUDE_DIR="$(INCLUDE_DIR)" \
	  JAVA_INCLUDES="$(JAVA_INCLUDES)" \
	  JAVA_JNI_H_DIR="$(JAVA_JNI_H_DIR)" \
	  JAVA_JNI_MD_H_DIR="$(JAVA_JNI_MD_H_DIR)" \
	  C="$(C)" \
	  CPP="$(CPP)" \
	  LD="$(LD)" \
	  JAVAC="$(JAVAC) $(JFLAGS)" \
	  JAVACC="$(JAVACC)" \
	  JAVAH="$(JAVAH) $(JFLAGS)" \
	  CFLAGS="$(CFLAGS)" \
	  CPPFLAGS="$(CPPFLAGS)" \
	  LDFLAGS="$(LDFLAGS)" \
	  SHARED="$(SHARED)" \
	  EXE="$(EXE)" \
	  LIBPREFIX="$(LIBPREFIX)" \
	  LIBSUFFIX="$(LIBSUFFIX)" \
	  LIBMATH="$(LIBMATH)" \
	  CLASSPATHSEP="$(CLASSPATHSEP)") \
	  || exit 1; \
	done; \
	if [ "$(OSTYPE)" = "darwin" ]; then \
	  echo Creating shared library symlinks...; \
	  (cd $(LIB_DIR) && \
	  for lib in `ls *$(LIBSUFFIX)`; do ln -fs $$lib `echo $$lib | sed s/$(LIBSUFFIX)/.jnilib/`; done;); \
	fi

tests: testslocal
	@if [ -d ~/prism/prism-tests ]; then \
	  cd ~/prism/prism-tests && $(PWD)/etc/scripts/prism-auto -t -m . -p $(PWD)/bin/prism; \
	else \
	  echo "Skipping tests"; \
	fi

testslocal:
	@if [ -d tests ]; then \
	  cd tests && $(PWD)/etc/scripts/prism-auto -t -m . -p $(PWD)/bin/prism; \
	else \
	  echo "Skipping local tests"; \
	fi

bin_scripts:
	@for target in $(BIN_TARGETS); do \
	  target_trunc=`echo $$target | sed 's/\.[^.]*$$//'` && \
	  echo Copying "$(SRC_DIR)/bin/$$target -> bin/$$target_trunc" && \
	  cp $(SRC_DIR)/bin/$$target bin/$$target_trunc; \
	done;
	@./install.sh silent

# Unless VERSION has been passed in (as VERSION=xxx),
# extract version number from Java code using printversion
VERSION = $(shell SRC_DIR=$(SRC_DIR) $(SRC_DIR)/scripts/printversion.sh 2> /dev/null)

# Grab svn revision number from svnversion, if possible
REVISION = $(shell svnversion 2> /dev/null)

# Add Revision.java with current svn revision
add_rev:
	@echo "Creating $(SRC_DIR)/prism/Revision.java (REVISION = $(REVISION))"
	echo 'package prism;\npublic class Revision\n{\n\tpublic static String svnRevision = "$(REVISION)";\n}' > $(SRC_DIR)/prism/Revision.java

dist_src: dist_check_version dist_copy clean_all dist_tidy

dist_bin: JFLAGS += -source 1.7 -target 1.7
dist_bin: dist_check_version all binary dist_tidy dist_bin_copy

dist_check_version:
	@if [ "$(VERSION)" = "" ]; then echo "Usage: make dist_src/dist_bin VERSION=3.1"; exit 1; fi

# Unless already present, copy cudd/examples/doc to this dir
# By default, cudd/examples from svn trunk and doc from ~/prism-doc; other options commented out below
dist_copy:
	@if [ -e cudd ]; then \
	  echo "Warning: Not copying cudd since directory already exists"; \
	else \
	  echo "Installing CUDD from svn trunk..."; svn -q export https://www.prismmodelchecker.org/svn/prism/prism/trunk/cudd cudd; \
	fi
#	  echo "Installing CUDD from ../cudd..."; (SRC_DIST_DIR=`/bin/pwd`; cd ..; tar cf - cudd | tar xf - -C "$$SRC_DIST_DIR");
	@if [ -e examples ]; then \
	  echo "Warning: Not copying examples since directory already exists"; \
	else \
	  echo "Installing examples from svn trunk..."; svn -q export https://www.prismmodelchecker.org/svn/prism/prism/trunk/prism-examples examples; \
	fi
#	  echo "Installing examples from ../prism-examples..."; (SRC_DIST_DIR=`/bin/pwd`; cd ..; tar cf - prism-examples | tar xf - -C "$$SRC_DIST_DIR"); mv prism-examples examples;
	@if [ -e doc ]; then \
	  echo "Warning: Not copying manual since directory doc already exists"; \
	else \
	  echo "Installing manual from ~/prism-doc..."; \
	  mkdir doc; \
	  (SRC_DIST_DIR=`/bin/pwd`; cd ~/prism-doc; tar cf - manual | tar xf - -C "$$SRC_DIST_DIR"/doc); \
	  cp ~/prism-doc/manual.pdf doc; \
	fi

dist_bin_copy:
	@if [ "$(BINDISTSUFFIX)" = "win32" -o "$(BINDISTSUFFIX)" = "win64" ]; then \
		echo Building NSIS Windows installer...	&& \
		makensis /NOCD /DPRISM_NAME="PRISM $(VERSION)" /DPRISM_BUILD="prism-$(VERSION)" /DPRISM_BINDISTSUFFIX="$(BINDISTSUFFIX)" /DPRISM_DIR="" src/nsis_script.nsi; \
	else \
		BIN_DIST_DIR=`/bin/pwd | sed 's/-src$$//'`"-$(BINDISTSUFFIX)" && \
		BIN_DIST_DIR_NAME=`basename $$BIN_DIST_DIR` && \
		echo Creating binary distribution in $$BIN_DIST_DIR... && \
		mkdir $$BIN_DIST_DIR && \
		tar cf - README.txt CHANGELOG.txt COPYING.txt install.sh bin etc lib examples doc | ( cd $$BIN_DIST_DIR; tar xfp -) && \
		echo Zipping $$BIN_DIST_DIR_NAME... && \
		(cd $$BIN_DIST_DIR/..; tar cfz $$BIN_DIST_DIR_NAME.tar.gz $$BIN_DIST_DIR_NAME); \
	fi
#	(cd $$BIN_DIST_DIR/..; zip -rq $$BIN_DIST_DIR_NAME.zip $$BIN_DIST_DIR_NAME);

dist_tidy:
	@echo Detecting unwanted files...
	@find . \( -name '*.o' -o -name '*.so' -o -name '*.dll' -o -name '*.exe' \)
	@find . \( -name '*~*' -o -name '*bak*' \)
	@find . -name 'log*' | grep -v userinterface/log || test 1
	@find . -name '*NOTES*' | grep -v src/parser/NOTES | grep -v cudd/RELEASE.NOTES || test 1
	@echo Processing text files...
	@find . -type f -name '*.txt' -exec unix2dos {} {} \; 2> /dev/null
	@find examples -type f ! -name auto -exec unix2dos {} {} \; 2> /dev/null || test 1
	@echo Processing file permissions...
	@find . -type f -exec chmod 644 {} \;
	@find . \( -type d -o -type s \) -exec chmod 755 {} \;
	@find . -type f  \( -name '*.sh' -o -name '*.so' -o -name '*.dll' \) -exec chmod 755 {} \;
	@find examples -type f -name 'auto' -exec chmod 755 {} \; || test 1
	@find bin -type f -exec chmod 755 {} \;
	@find src/bin -type f -exec chmod 755 {} \;
	@find etc/scripts -type f -exec chmod 755 {} \;

binary:
	@echo Generating jar file...
	@jar cmf src/manifest.txt lib/prism.jar -C classes . -C . images dtds

undist:
	@rm -rf cudd && ln -s ../cudd cudd
	@rm -rf doc
	@rm -rf examples && ln -s ../prism-examples examples

tarcf:
	@TARCF_DIR=`/bin/pwd | sed 's/.\+\///'` && \
	if [ $$TARCF_DIR = "." ]; then exit 1; fi && \
	echo Building tar file "../"$$TARCF_DIR".tar.gz" && \
	(cd ..; tar cfz $$TARCF_DIR".tar.gz" $$TARCF_DIR)

PRISM_CLASSPATH = "$(THIS_DIR)/$(PRISM_DIR_REL)/$(CLASSES_DIR)$(CLASSPATHSEP)$(THIS_DIR)/$(PRISM_DIR_REL)/lib/*"

javadoc:
	@JAVADOC_DIRS=`echo $(MAKE_DIRS) | sed 's/\//./g' | sed 's/ /:/g'` && \mkdir -p javadoc; javadoc $(JFLAGS) -d javadoc -overview $(SRC_DIR)/overview.html -sourcepath $(SRC_DIR) -classpath $(SRC_DIR)$(CLASSPATHSEP)$(PRISM_CLASSPATH) -subpackages $$JAVADOC_DIRS

clean: checks
	@(for dir in $(MAKE_DIRS); do \
	  echo Cleaning src/$$dir ...; \
	  (cd src/$$dir && \
	  $(MAKE) -s SRC_DIR="$(SRC_DIR)" CLASSES_DIR="$(CLASSES_DIR)" OBJ_DIR="$(OBJ_DIR)" LIB_DIR="$(LIB_DIR)" EXE="$(EXE)" LIBPREFIX="$(LIBPREFIX)" LIBSUFFIX="$(LIBSUFFIX)" clean) \
	  || exit 1; \
	done; \
	find $(CLASSES_DIR) -name '*.class' -exec rm {} \; ; \
	rm -f lib/*jnilib; \
	rm -f $(BIN_PRISM) $(BIN_XPRISM) $(BIN_PRISM_BAT) $(BIN_XPRISM_BAT) )

celan: clean

clean_all: checks clean_cudd clean

clean_cudd:
	@(cd $(CUDD_DIR) && $(MAKE) distclean)

clean_ext:
	@(for ext in $(EXT_PACKAGES); do \
	  echo Cleaning $$ext ...; \
	  (cd ext/$$ext && \
	  $(MAKE) -s LIBPREFIX="$(LIBPREFIX)" LIBSUFFIX="$(LIBSUFFIX)" clean) \
	  || exit 1; \
	done )

clean_dd: checks
	@(cd src/dd && $(MAKE) -s SRC_DIR="$(SRC_DIR)" CLASSES_DIR="$(CLASSES_DIR)" OBJ_DIR="$(OBJ_DIR)" LIB_DIR="$(LIB_DIR)" EXE="$(EXE)" LIBPREFIX="$(LIBPREFIX)" LIBSUFFIX="$(LIBSUFFIX)" clean)
clean_jdd: checks
	@(cd src/jdd && $(MAKE) -s SRC_DIR="$(SRC_DIR)" CLASSES_DIR="$(CLASSES_DIR)" OBJ_DIR="$(OBJ_DIR)" LIB_DIR="$(LIB_DIR)" EXE="$(EXE)" LIBPREFIX="$(LIBPREFIX)" LIBSUFFIX="$(LIBSUFFIX)" clean)
clean_odd: checks
	@(cd src/odd && $(MAKE) -s SRC_DIR="$(SRC_DIR)" CLASSES_DIR="$(CLASSES_DIR)" OBJ_DIR="$(OBJ_DIR)" LIB_DIR="$(LIB_DIR)" EXE="$(EXE)" LIBPREFIX="$(LIBPREFIX)" LIBSUFFIX="$(LIBSUFFIX)" clean)
clean_dv: checks
	@(cd src/dv && $(MAKE) -s SRC_DIR="$(SRC_DIR)" CLASSES_DIR="$(CLASSES_DIR)" OBJ_DIR="$(OBJ_DIR)" LIB_DIR="$(LIB_DIR)" EXE="$(EXE)" LIBPREFIX="$(LIBPREFIX)" LIBSUFFIX="$(LIBSUFFIX)" clean)
clean_prism: checks
	@(cd src/prism && $(MAKE) -s SRC_DIR="$(SRC_DIR)" CLASSES_DIR="$(CLASSES_DIR)" OBJ_DIR="$(OBJ_DIR)" LIB_DIR="$(LIB_DIR)" EXE="$(EXE)" LIBPREFIX="$(LIBPREFIX)" LIBSUFFIX="$(LIBSUFFIX)" clean)
clean_mtbdd: checks
	@(cd src/mtbdd && $(MAKE) -s SRC_DIR="$(SRC_DIR)" CLASSES_DIR="$(CLASSES_DIR)" OBJ_DIR="$(OBJ_DIR)" LIB_DIR="$(LIB_DIR)" EXE="$(EXE)" LIBPREFIX="$(LIBPREFIX)" LIBSUFFIX="$(LIBSUFFIX)" clean)
clean_sparse: checks
	@(cd src/sparse && $(MAKE) -s SRC_DIR="$(SRC_DIR)" CLASSES_DIR="$(CLASSES_DIR)" OBJ_DIR="$(OBJ_DIR)" LIB_DIR="$(LIB_DIR)" EXE="$(EXE)" LIBPREFIX="$(LIBPREFIX)" LIBSUFFIX="$(LIBSUFFIX)" clean)
clean_hybrid: checks
	@(cd src/hybrid && $(MAKE) -s SRC_DIR="$(SRC_DIR)" CLASSES_DIR="$(CLASSES_DIR)" OBJ_DIR="$(OBJ_DIR)" LIB_DIR="$(LIB_DIR)" EXE="$(EXE)" LIBPREFIX="$(LIBPREFIX)" LIBSUFFIX="$(LIBSUFFIX)" clean)
clean_parser: checks
	@(cd src/parser && $(MAKE) -s SRC_DIR="$(SRC_DIR)" CLASSES_DIR="$(CLASSES_DIR)" OBJ_DIR="$(OBJ_DIR)" LIB_DIR="$(LIB_DIR)" EXE="$(EXE)" LIBPREFIX="$(LIBPREFIX)" LIBSUFFIX="$(LIBSUFFIX)" clean)
clean_userinterface: checks
	@(cd src/userinterface && $(MAKE) -s SRC_DIR="$(SRC_DIR)" CLASSES_DIR="$(CLASSES_DIR)" OBJ_DIR="$(OBJ_DIR)" LIB_DIR="$(LIB_DIR)" EXE="$(EXE)" LIBPREFIX="$(LIBPREFIX)" LIBSUFFIX="$(LIBSUFFIX)" clean)
clean_simulator: checks
	@(cd src/simulator && $(MAKE) -s SRC_DIR="$(SRC_DIR)" CLASSES_DIR="$(CLASSES_DIR)" OBJ_DIR="$(OBJ_DIR)" LIB_DIR="$(LIB_DIR)" EXE="$(EXE)" LIBPREFIX="$(LIBPREFIX)" LIBSUFFIX="$(LIBSUFFIX)" clean)
clean_jltl2ba: checks
	@(cd src/jltl2ba && $(MAKE) -s SRC_DIR="$(SRC_DIR)" CLASSES_DIR="$(CLASSES_DIR)" OBJ_DIR="$(OBJ_DIR)" LIB_DIR="$(LIB_DIR)" EXE="$(EXE)" LIBPREFIX="$(LIBPREFIX)" LIBSUFFIX="$(LIBSUFFIX)" clean)
clean_jltl2dstar: checks
	@(cd src/jltl2dstar && $(MAKE) -s SRC_DIR="$(SRC_DIR)" CLASSES_DIR="$(CLASSES_DIR)" OBJ_DIR="$(OBJ_DIR)" LIB_DIR="$(LIB_DIR)" EXE="$(EXE)" LIBPREFIX="$(LIBPREFIX)" LIBSUFFIX="$(LIBSUFFIX)" clean)
clean_explicit: checks
	@(cd src/explicit && $(MAKE) -s SRC_DIR="$(SRC_DIR)" CLASSES_DIR="$(CLASSES_DIR)" OBJ_DIR="$(OBJ_DIR)" LIB_DIR="$(LIB_DIR)" EXE="$(EXE)" LIBPREFIX="$(LIBPREFIX)" LIBSUFFIX="$(LIBSUFFIX)" clean)
clean_pta: checks
	@(cd src/pta && $(MAKE) -s SRC_DIR="$(SRC_DIR)" CLASSES_DIR="$(CLASSES_DIR)" OBJ_DIR="$(OBJ_DIR)" LIB_DIR="$(LIB_DIR)" EXE="$(EXE)" LIBPREFIX="$(LIBPREFIX)" LIBSUFFIX="$(LIBSUFFIX)" clean)
clean_param: checks
	@(cd src/param && $(MAKE) -s SRC_DIR="$(SRC_DIR)" CLASSES_DIR="$(CLASSES_DIR)" OBJ_DIR="$(OBJ_DIR)" LIB_DIR="$(LIB_DIR)" EXE="$(EXE)" LIBPREFIX="$(LIBPREFIX)" LIBSUFFIX="$(LIBSUFFIX)" clean)
clean_strat: checks
	@(cd src/strat && $(MAKE) -s SRC_DIR="$(SRC_DIR)" CLASSES_DIR="$(CLASSES_DIR)" OBJ_DIR="$(OBJ_DIR)" LIB_DIR="$(LIB_DIR)" EXE="$(EXE)" LIBPREFIX="$(LIBPREFIX)" LIBSUFFIX="$(LIBSUFFIX)" clean)

checks:
	@(if [ "$(OSTYPE)" != "linux" -a "$(OSTYPE)" != "solaris" -a "$(OSTYPE)" != "cygwin" -a "$(OSTYPE)" != "darwin" ]; then \
	  echo "To compile PRISM, the environment variable OSTYPE"; \
	  echo "must be set to one of: linux, solaris, cygwin or darwin,"; \
	  echo "depending on which operating system you are using."; \
	  echo "This is not the case on your system. Please specify"; \
	  echo "the value of OSTYPE manually to make, e.g.:"; \
	  echo; \
	  echo "  make OSTYPE=linux"; \
	  echo; \
	  echo "Alternatively, if you wish, you can set the environment"; \
	  echo "variable yourself (using setenv or export) or you"; \
	  echo "can edit the value of OSTYPE directly in the Makefile."; \
	  exit 1; \
	fi; \
	if [ "$(JAVA_DIR)" = "" ]; then \
	  echo "PRISM was unable to find the directory which contains"; \
	  echo "your Java distribution. Please specify this manually to"; \
	  echo "make, as in these examples:"; \
	  echo; \
	  echo "  make JAVA_DIR=/usr/java/j2sdk1.4.2"; \
	  echo "  make JAVA_DIR=\"/cygdrive/c/Program Files/Java/jdk1.4.2\""; \
	  echo; \
	  echo "See the PRISM manual for further information."; \
	  echo; \
	  echo "Alternatively, if you wish, you can set the environment"; \
	  echo "variable yourself (using setenv or export) or you"; \
	  echo "can edit the value of JAVA_DIR directly in the Makefile."; \
	  exit 1; \
	fi; \
	if [ ! -d "$(JAVA_DIR)" ]; then \
	  echo "Java directory \"$(JAVA_DIR)\" does not exist."; \
	  exit 1; \
	fi; \
	if [ ! -f "$(JAVA_JNI_H_DIR)"/jni.h ]; then \
	  echo "Could not locate JNI header jni.h within \"$(JAVA_DIR)\"."; \
	  echo "You may need to set JAVA_DIR by hand. See the PRISM manual for details."; \
	  exit 1; \
	fi; \
	if [ ! -f "$(JAVA_JNI_MD_H_DIR)"/jni_md.h ]; then \
	  echo "Could not locate JNI header jni_md.h within \"$(JAVA_DIR)\"."; \
	  echo "You may need to set JAVA_DIR by hand. See the PRISM manual for details."; \
	  exit 1; \
	fi; \
	echo "VERSION: $(VERSION)"; \
	echo "OSTYPE/ARCH: $(OSTYPE) $(ARCH)"; \
	echo "JAVA_DIR: $(JAVA_DIR)"; \
	echo "JAVA_DIR_BACKUP: $(JAVA_DIR_BACKUP)"; \
	echo "JAVAC: "`which $(JAVAC)` \
	)

#################################################
