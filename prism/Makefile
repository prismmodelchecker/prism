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

# To build on AMD64 or Itanium machines, ARCH should be set.
# We attempt to auto-detect this here.
ifneq (,$(findstring 86_64, $(shell uname -m)))
	ARCH = amd64
endif
ifneq (,$(findstring ia64, $(shell uname -m)))
	ARCH = ia64
endif
# For Macs, we decide whether to build in 64-bit mode based on
# whether java is 32/64-bit (since these need to match)
ifeq ($(OSTYPE),darwin)
	JAVA_VERSION_STRING = $(shell java -version 2>&1)
	ifneq (, $(or $(findstring 64-bit, $(JAVA_VERSION_STRING)), $(findstring 64-Bit, $(JAVA_VERSION_STRING)) ))
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
		CFLAGS = $(DEBUG) $(OPTIMISE) -DPIC -fPIC
		CPPFLAGS = $(DEBUG) $(OPTIMISE) -DPIC -fPIC
		LDFLAGS = $(DEBUG) $(OPTIMISE) -DPIC -fPIC
		CUDD_XCFLAGS = -m64 -fPIC -DPIC -DHAVE_IEEE_754 -DBSD -DSIZEOF_VOID_P=8 -DSIZEOF_LONG=8
	else
	ifeq ($(ARCH),ia64)
		# Position Independent Code required on AMD64/Itanium
		CFLAGS = $(DEBUG) $(OPTIMISE) -DPIC -fPIC
		CPPFLAGS = $(DEBUG) $(OPTIMISE) -DPIC -fPIC
		LDFLAGS = $(DEBUG) $(OPTIMISE) -DPIC -fPIC
		# Note: We omit the -m64 flag from here since it seems to be unsupported by gcc on IA64
		CUDD_XCFLAGS = -fPIC -DPIC -DHAVE_IEEE_754 -DBSD -DSIZEOF_VOID_P=8 -DSIZEOF_LONG=8
	else
		CFLAGS = -m32 $(DEBUG) $(OPTIMISE)
		CPPFLAGS = -m32 $(DEBUG) $(OPTIMISE)
		LDFLAGS = -m32 $(DEBUG) $(OPTIMISE)
		CUDD_XCFLAGS = -m32 -mtune=native -malign-double -DHAVE_IEEE_754 -DBSD
	endif
	endif
	SHARED = -shared
	#SHARED = -G
	EXE =
	LIBPREFIX = lib
	LIBSUFFIX = .so
	BINDISTSUFFIX = linux
	OSTYPE_INCLUDE = include
	CLASSPATHSEP = :
endif
# Solaris
ifeq ($(OSTYPE),solaris)
	CFLAGS = $(DEBUG) $(OPTIMISE)
	CPPFLAGS = $(DEBUG) $(OPTIMISE)
	LDFLAGS = $(DEBUG) $(OPTIMISE)
	CUDD_XCFLAGS = -mcpu=ultrasparc -DHAVE_IEEE_754 -DUNIX100 -DEPD_BIG_ENDIAN
	SHARED = -shared -mimpure-text
	EXE =
	LIBPREFIX = lib
	LIBSUFFIX = .so
	BINDISTSUFFIX = solaris
	OSTYPE_INCLUDE = include
	CLASSPATHSEP = :
endif
# Cygwin
ifeq ($(OSTYPE),cygwin)
	CFLAGS = -mno-cygwin $(DEBUG) $(OPTIMISE)
	CPPFLAGS = -mno-cygwin $(DEBUG) $(OPTIMISE)
	LDFLAGS = -mno-cygwin -Wl,--add-stdcall-alias $(DEBUG) $(OPTIMISE)
	CUDD_XCFLAGS = -mno-cygwin -march=i686 -malign-double -DHAVE_IEEE_754 -DHAVE_GETRLIMIT=0 -DRLIMIT_DATA_DEFAULT=268435456 -DHAVE_SYS_RESOURCE_H=0 -DHAVE_SYS_WAIT_H=0
	SHARED = -shared
	#SHARED = -G
	EXE = .exe
	LIBPREFIX =
	LIBSUFFIX = .dll
	BINDISTSUFFIX = win
	OSTYPE_INCLUDE = include
	CLASSPATHSEP = ;
endif
# Darwin
ifeq ($(OSTYPE),darwin)
	ifeq ($(ARCH),x86_64)
		CFLAGS = -arch x86_64 -fPIC -DPIC $(DEBUG) $(OPTIMISE)
		CPPFLAGS = -arch x86_64 -fPIC -DPIC $(DEBUG) $(OPTIMISE)
		LDFLAGS = -arch x86_64 -fPIC -DPIC -Wl,-search_paths_first $(DEBUG) $(OPTIMISE)
		CUDD_XCFLAGS = -arch x86_64 -fPIC -DPIC -DHAVE_IEEE_754 -DBSD -DSIZEOF_VOID_P=8 -DSIZEOF_LONG=8 -fno-common
	else
		CFLAGS = -arch i386 $(DEBUG) $(OPTIMISE)
		CPPFLAGS = -arch i386 $(DEBUG) $(OPTIMISE)
		LDFLAGS = -arch i386 -Wl,-search_paths_first $(DEBUG) $(OPTIMISE)
		CUDD_XCFLAGS = -arch i386 -DHAVE_IEEE_754 -DBSD -fno-common
	endif
	SHARED = -dynamiclib
	EXE =
	LIBPREFIX = lib
	LIBSUFFIX = .dylib
	BINDISTSUFFIX = osx
	OSTYPE_INCLUDE = Headers
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
JAVA_JNI_H_DIR = $(shell (ls "$(JAVA_DIR)"/$(OSTYPE_INCLUDE)/jni.h | sed 's/\/jni.h//') 2>/dev/null)
JAVA_JNI_MD_H_DIR = $(shell (ls "$(JAVA_DIR)"/$(OSTYPE_INCLUDE)/jni_md.h "$(JAVA_DIR)"/$(OSTYPE_INCLUDE)/*/jni_md.h | sed 's/\/jni_md.h//') 2>/dev/null)
JAVA_INCLUDES = -I $(JAVA_JNI_H_DIR) -I $(JAVA_JNI_MD_H_DIR)

#########################
# Main part of Makefile #
#########################

MAKE_DIRS = dd jdd odd dv prism mtbdd sparse hybrid parser settings userinterface pepa/compiler simulator jltl2ba jltl2dstar explicit pta

ifeq ($(OSTYPE),linux)
	BIN_PRISM=bin/prism
	BIN_PRISM_SRC=src/bin/prism.linux
	BIN_XPRISM=bin/xprism
	BIN_XPRISM_SRC=src/bin/xprism.linux
	BIN_TARGETS=$(BIN_PRISM) $(BIN_XPRISM)
endif
ifeq ($(OSTYPE),solaris)
	BIN_PRISM=bin/prism
	BIN_PRISM_SRC=src/bin/prism.linux
	BIN_XPRISM=bin/xprism
	BIN_XPRISM_SRC=src/bin/xprism.linux
	BIN_TARGETS=$(BIN_PRISM) $(BIN_XPRISM)
endif
ifeq ($(OSTYPE),cygwin)
	BIN_PRISM=bin/prism
	BIN_PRISM_SRC=src/bin/prism.cygwin
	BIN_XPRISM=bin/xprism
	BIN_XPRISM_SRC=src/bin/xprism.linux
	BIN_PRISM_BAT=bin/prism.bat
	BIN_PRISM_BAT_SRC=src/bin/prism.bat
	BIN_XPRISM_BAT=bin/xprism.bat
	BIN_XPRISM_BAT_SRC=src/bin/xprism.bat
	BIN_TARGETS=$(BIN_PRISM) $(BIN_XPRISM) $(BIN_PRISM_BAT) $(BIN_XPRISM_BAT)
endif
ifeq ($(OSTYPE),darwin)
	BIN_PRISM=bin/prism
	BIN_PRISM_SRC=src/bin/prism.darwin
	BIN_XPRISM=bin/xprism
	BIN_XPRISM_SRC=src/bin/xprism.linux
	BIN_TARGETS=$(BIN_PRISM) $(BIN_XPRISM)
endif

.PHONY: clean javadoc

default: all

all: cuddpackage prism

cuddpackage: checks
	@if [ "$(CUDD_DIR)" = "" ]; then echo "Error: Cannot find CUDD"; exit 1; fi
	@if [ ! -d "$(CUDD_DIR)" ]; then echo "Error: Cannot find CUDD"; exit 1; fi
	@(if [ ! -h $(CUDD_DIR) ]; then \
	  echo Making cudd ...; \
	  cd $(CUDD_DIR) && \
	  $(MAKE) C="$(C)" CC="$(C)" CPP="$(CPP)" \
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
	  JAVAC="$(JAVAC)" \
	  JAVAH="$(JAVAH)" \
	  CFLAGS="$(CFLAGS)" \
	  CPPFLAGS="$(CPPFLAGS)" \
	  LDFLAGS="$(LDFLAGS)" \
	  SHARED="$(SHARED)" \
	  EXE="$(EXE)" \
	  LIBPREFIX="$(LIBPREFIX)" \
	  LIBSUFFIX="$(LIBSUFFIX)" \
	  CLASSPATHSEP="$(CLASSPATHSEP)") \
	  || exit 1; \
	done; \
	if [ "$(OSTYPE)" = "darwin" ]; then \
	  echo Creating shared library symlinks...; \
	  (cd $(LIB_DIR) && \
	  for lib in `ls *$(LIBSUFFIX)`; do ln -fs $$lib `echo $$lib | sed s/$(LIBSUFFIX)/.jnilib/`; done;); \
	fi

bin_scripts: $(BIN_TARGETS)
	@./install.sh silent

$(BIN_PRISM): $(BIN_PRISM_SRC)
	@echo "$< -> $@"; cp $< $@

$(BIN_XPRISM): $(BIN_XPRISM_SRC)
	@echo "$< -> $@"; cp $< $@

$(BIN_PRISM_BAT): $(BIN_PRISM_BAT_SRC)
	@echo "$< -> $@"; cp $< $@

$(BIN_XPRISM_BAT): $(BIN_XPRISM_BAT_SRC)
	@echo "$< -> $@"; cp $< $@

VERSION=# default value for VERSION is blank to force provision at command-line 

dist: dist_check_version dist_copy clean_all dist_tidy

dist_bin: dist_check_version binary dist_tidy dist_bin_copy

dist_check_version:
	@if [ "$(VERSION)" = "" ]; then echo "Usage: make dist/dist_bin VERSION=3.1"; exit 1; fi

dist_copy:
#	@echo Adding cudd...; rm -rf cudd && svn -q export https://subversion.cs.bham.ac.uk/svn/dxp/prismsvn/prism/tags/prism-$(VERSION)/cudd cudd
#	@echo Adding examples...; rm -rf examples && svn -q export https://subversion.cs.bham.ac.uk/svn/dxp/prismsvn/prism/tags/prism-$(VERSION)/prism-examples examples
#	@echo Adding doc...; test -f ~dxp/prism-dev/doc/manual.pdf && rm -rf doc && mkdir doc && cp ~dxp/prism-dev/doc/manual.pdf doc

dist_bin_copy:
	@if [ "$(BINDISTSUFFIX)" = "win" ]; then \
		echo Building NSIS Windows installer...	&& \
		makensis /DPRISM_NAME="PRISM $(VERSION)" /DPRISM_BUILD="prism-$(VERSION)" /DPRISM_DIR="" src/nsis_script.nsi; \
	else \
		BIN_DIST_DIR=`/bin/pwd | sed 's/-src$$//'`"-$(BINDISTSUFFIX)" && \
		BIN_DIST_DIR_NAME=`basename $$BIN_DIST_DIR` && \
		echo Creating binary distribution in $$BIN_DIST_DIR... && \
		mkdir $$BIN_DIST_DIR && \
		tar cf - README.txt VERSIONS.txt CHANGELOG.txt COPYING.txt install.sh bin etc lib examples doc | ( cd $$BIN_DIST_DIR; tar xfp -) && \
		echo Zipping $$BIN_DIST_DIR_NAME... && \
		(cd $$BIN_DIST_DIR/..; tar cfz $$BIN_DIST_DIR_NAME.tar.gz $$BIN_DIST_DIR_NAME); \
	fi
#	(cd $$BIN_DIST_DIR/..; zip -rq $$BIN_DIST_DIR_NAME.zip $$BIN_DIST_DIR_NAME);

dist_tidy:
	@echo Detecting unwanted files...
	@find . \( -name '*.o' -o -name '*.so' -o -name '*.dll' -o -name '*.exe' \)
	@find . \( -name '*~*' -o -name '*bak*' -o -name '*CVS*' \)
	@find . \( -name '*NEW*' -o -name '*new*' -o -name '*old*' \) | grep -v old\.gif || test 1
	@find . -name '*tmp*' | grep -v cudd/util/tmpfile.c || test 1
	@find . -name 'log*' | grep -v userinterface/log || test 1
	@find . -name '.nbattrs'
	@find . -name '.xv*'
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

javadoc:
	@JAVADOC_DIRS=`echo $(MAKE_DIRS) | sed 's/\//./g' | sed 's/ /:/g'` && \mkdir -p javadoc; javadoc -d javadoc -classpath $(SRC_DIR) -subpackages $$JAVADOC_DIRS

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
	if [ ! -f "$(JAVA_DIR)"/$(OSTYPE_INCLUDE)/jni.h ]; then \
	  echo "Could not locate JNI header jni.h in \"$(JAVA_DIR)/$(OSTYPE_INCLUDE)\"."; \
	  echo "You may need to set JAVA_DIR by hand. See the PRISM manual for details."; \
	  exit 1; \
	fi; \
	if [ ! -f "$(JAVA_DIR)"/$(OSTYPE_INCLUDE)/jni_md.h -a ! -f "$(JAVA_DIR)"/$(OSTYPE_INCLUDE)/*/jni_md.h ]; then \
	  echo "Could not locate JNI header jni_md.h in \"$(JAVA_DIR)/$(OSTYPE_INCLUDE)\" or ...."; \
	  echo "You may need to set JAVA_DIR by hand. See the PRISM manual for details."; \
	  exit 1; \
	fi; \
	echo "OSTYPE/ARCH: $(OSTYPE) $(ARCH)"; \
	echo "JAVA_DIR: $(JAVA_DIR)"; \
	echo "JAVAC: "`which $(JAVAC)` \
	)

#################################################
