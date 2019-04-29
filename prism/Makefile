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

##############
# Flags etc. #
##############

DEBUG = 
#DEBUG = -g

OPTIMISE = -O3
#OPTIMISE =

# warnings we'd like to get
WARNINGS = -Wformat

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
		CUDD_XCFLAGS = -m64 -fPIC -DPIC -DHAVE_IEEE_754 -DBSD -DSIZEOF_VOID_P=8 -DSIZEOF_LONG=8 $(DEBUG) $(WARNINGS)
		CFLAGS = $(CUDD_XCFLAGS) $(OPTIMISE)
		CPPFLAGS = --std=c++11 $(CUDD_XCFLAGS) $(OPTIMISE)
		LDFLAGS = $(CUDD_XCFLAGS) $(OPTIMISE)
		BINDISTSUFFIX = linux64
	else
	ifeq ($(ARCH),ia64)
		# Position Independent Code required on AMD64/Itanium
		# Note: We omit the -m64 flag from here since it seems to be unsupported by gcc on IA64
		CUDD_XCFLAGS = -fPIC -DPIC -DHAVE_IEEE_754 -DBSD -DSIZEOF_VOID_P=8 -DSIZEOF_LONG=8 $(DEBUG) $(WARNINGS)
		CFLAGS = $(CUDD_XCFLAGS) $(OPTIMISE)
		CPPFLAGS = --std=c++11 $(CUDD_XCFLAGS) $(OPTIMISE)
		LDFLAGS = $(CUDD_XCFLAGS) $(OPTIMISE)
		BINDISTSUFFIX = linux64
	else
		CUDD_XCFLAGS = -m32 -malign-double -DHAVE_IEEE_754 -DBSD $(DEBUG) $(WARNINGS)
		CFLAGS = $(CUDD_XCFLAGS) $(OPTIMISE)
		CPPFLAGS = --std=c++11 $(CUDD_XCFLAGS) $(OPTIMISE)
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
	CUDD_XCFLAGS = -mcpu=ultrasparc -DHAVE_IEEE_754 -DUNIX100 -DEPD_BIG_ENDIAN $(DEBUG) $(WARNINGS)
	CFLAGS = $(CUDD_XCFLAGS) $(OPTIMISE)
	CPPFLAGS = --std=c++11 $(CUDD_XCFLAGS) $(OPTIMISE)
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
		CUDD_XCFLAGS = -malign-double -DHAVE_IEEE_754 -DHAVE_GETRLIMIT=0 -DRLIMIT_DATA_DEFAULT=268435456 -DHAVE_SYS_RESOURCE_H=0 -DHAVE_SYS_WAIT_H=0 -DSIZEOF_VOID_P=8 -DSIZEOF_LONG=4 $(DEBUG) $(WARNINGS) -static-libgcc -static-libstdc++
		CFLAGS = $(CUDD_XCFLAGS) $(OPTIMISE)
		# need -DWIN32 for lpsolve (WIN32 is only defined by mingw/gcc without --std=c++11)
		CPPFLAGS = --std=c++11 -DWIN32 $(CUDD_XCFLAGS) $(OPTIMISE)
		LDFLAGS = $(CUDD_XCFLAGS) $(OPTIMISE) -Wl,--add-stdcall-alias -Wl,-Bstatic,--whole-archive -lpthread -Wl,-Bdynamic,--no-whole-archive
		BINDISTSUFFIX = win64
	else
		C = /usr/bin/i686-w64-mingw32-gcc
		CPP = /usr/bin/i686-w64-mingw32-g++
		CUDD_XCFLAGS = -march=i686 -malign-double -DHAVE_IEEE_754 -DHAVE_GETRLIMIT=0 -DRLIMIT_DATA_DEFAULT=268435456 -DHAVE_SYS_RESOURCE_H=0 -DHAVE_SYS_WAIT_H=0 $(DEBUG) $(WARNINGS) -static-libgcc -static-libstdc++
		CFLAGS = $(CUDD_XCFLAGS) $(OPTIMISE)
		# need -DWIN32 for lpsolve (WIN32 is only defined by mingw/gcc without --std=c++11)
		CPPFLAGS = --std=c++11 -DWIN32 $(CUDD_XCFLAGS) $(OPTIMISE)
		LDFLAGS = $(CUDD_XCFLAGS) $(OPTIMISE) -Wl,--add-stdcall-alias -Wl,-Bstatic,--whole-archive -lpthread -Wl,-Bdynamic,--no-whole-archive
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
		CUDD_XCFLAGS = -arch x86_64 -fPIC -DPIC -DHAVE_IEEE_754 -DBSD -DSIZEOF_VOID_P=8 -DSIZEOF_LONG=8 -fno-common $(DEBUG) $(WARNINGS)
		CFLAGS = $(CUDD_XCFLAGS) $(OPTIMISE)
		CPPFLAGS = --std=c++11 $(CUDD_XCFLAGS) $(OPTIMISE)
		LDFLAGS = $(CUDD_XCFLAGS) $(OPTIMISE) -Wl,-search_paths_first
		BINDISTSUFFIX = osx64
		BIN_TARGETS=prism.darwin64 xprism.linux
	else
		CUDD_XCFLAGS = -arch i386 -DHAVE_IEEE_754 -DBSD -fno-common $(DEBUG) $(WARNINGS)
		CFLAGS = $(CUDD_XCFLAGS) $(OPTIMISE)
		CPPFLAGS = --std=c++11 $(CUDD_XCFLAGS) $(OPTIMISE)
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
IMAGES_DIR =    images
DTDS_DIR =      dtds

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

##########################################
# Main part of Makefile: Compiling PRISM #
##########################################

MAKE_DIRS = dd jdd odd dv prism mtbdd sparse hybrid parser settings userinterface pepa/compiler simulator jltl2ba jltl2dstar explicit pta param strat automata common cex

EXT_PACKAGES = lpsolve55 lp_solve_5.5_java

.PHONY: clean javadoc tests release

# inhibit building in parallel (-j option)
.NOTPARALLEL:

default: all

all: cuddpackage extpackages prism

# Build our customised version of CUDD
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

# Use this to force a build of CUDD, even if dir is just a symlink
cuddpackageforce: checks
	  @echo Making cudd ...; \
	  cd $(CUDD_DIR) && \
	  $(MAKE) C="$(C)" CC="$(C)" CPP="$(CPP)" \
	  XCFLAGS="$(CUDD_XCFLAGS)";

# Use this to force a rebuild (with javacc) of the main parser
parser:
	@echo Making parser ...; \
	cd src/parser && \
	$(MAKE) SRC_DIR="$(SRC_DIR)" touch && \
	$(MAKE) \
	SRC_DIR="$(SRC_DIR)" \
	CLASSES_DIR="$(CLASSES_DIR)" \
	JAVAC="$(JAVAC) $(JFLAGS)" \
	JAVACC="$(JAVACC)" \
	CLASSPATHSEP="$(CLASSPATHSEP)"

# Build various external libraries needed by PRISM
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

# Compile main PRISM code
# (we also do some preparatory checks, and build launch scripts afterwards)
prism: checks make_dirs bin_scripts

# Compile each (top-level) source directory separately
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
# On Windows, convert the generated JNI headers using dos2unix
	@if [ "$(LIBSUFFIX)" = ".dll" ]; then \
		(dos2unix include/jni/*.h) \
	fi;

# Copy/modify the launch scripts and put in the bin directory
bin_scripts:
	@for target in $(BIN_TARGETS); do \
	  target_trunc=`echo $$target | sed 's/\.[^.]*$$//'` && \
	  echo Copying "$(SRC_DIR)/bin/$$target -> bin/$$target_trunc" && \
	  cp $(SRC_DIR)/bin/$$target bin/$$target_trunc; \
	done;
	@echo Copying "$(OBJ_DIR)/prism/ngprism$(EXE) -> bin/ngprism$(EXE)" && \
	cp $(OBJ_DIR)/prism/ngprism$(EXE) bin/ngprism$(EXE)
	@./install.sh silent

# Some checks to make sure that the main settings are valid
checks:
	@(if [ "$(OSTYPE)" != "linux" -a "$(OSTYPE)" != "solaris" -a "$(OSTYPE)" != "cygwin" -a "$(OSTYPE)" != "darwin" ]; then \
	  echo "\033[33mTo compile PRISM, the environment variable OSTYPE"; \
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
	  echo "\033[0m"; \
	  exit 1; \
	fi; \
	if [ "$(JAVA_DIR)" = "" ]; then \
	  echo "\033[33mPRISM was unable to find the directory which contains"; \
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
	  echo "\033[0m"; \
	  exit 1; \
	fi; \
	if [ ! -d "$(JAVA_DIR)" ]; then \
	  echo "\033[33mJava directory \"$(JAVA_DIR)\" does not exist."; \
	  echo "\033[0m"; \
	  exit 1; \
	fi; \
	if [ ! -f "$(JAVA_JNI_H_DIR)"/jni.h ]; then \
	  echo "\033[33mCould not locate JNI header jni.h within \"$(JAVA_DIR)\"."; \
	  echo "You may need to set JAVA_DIR by hand. See the PRISM manual for details."; \
	  echo "\033[0m"; \
	  exit 1; \
	fi; \
	if [ ! -f "$(JAVA_JNI_MD_H_DIR)"/jni_md.h ]; then \
	  echo "\033[33mCould not locate JNI header jni_md.h within \"$(JAVA_DIR)\"."; \
	  echo "You may need to set JAVA_DIR by hand. See the PRISM manual for details."; \
	  echo "\033[0m"; \
	  exit 1; \
	fi; \
	echo "VERSION: $(VERSION)"; \
	echo "OSTYPE/ARCH: $(OSTYPE) $(ARCH)"; \
	echo "JAVA_DIR: $(JAVA_DIR)"; \
	echo "JAVA_DIR_BACKUP: $(JAVA_DIR_BACKUP)"; \
	echo "JAVAC: "`which $(JAVAC)` \
	)

# Misc: count the number of lines of code
count_loc:
	find $(SRC_DIR) -name '*.java' -o -name '*.cc' | xargs wc -l

###########
# Testing #
###########

# Run a single test case from the test suite (useful quick check that the build was ok)
test:
	bin/prism ../prism-tests/functionality/verify/dtmcs/dtmc_pctl.pm ../prism-tests/functionality/verify/dtmcs/dtmc_pctl.pm.props -prop 2 -test

# Run all tests from the test suite (in ../prism-tests and ./tests)
# Optionally, extra arguments for prism-auto are picked up via variable TESTS_ARGS
tests: testslocal
	@if [ -d ../prism-tests ]; then \
	  cd ../prism-tests && "$(PWD)"/etc/scripts/prism-auto -t -m . -p "$(PWD)"/bin/prism --nailgun --ngprism "$(PWD)"/bin/ngprism $(TESTS_ARGS); \
	else \
	  echo "Skipping tests"; \
	fi

# Just display the command to run the test suite on this version of PRISM
# Optionally, extra arguments for prism-auto are picked up via variable TESTS_ARGS
testsecho:
	@echo etc/scripts/prism-auto -t -m --nailgun --ngprism bin/ngprism ../prism-tests -p bin/prism $(TESTS_ARGS)

# Run local tests (in ./tests)
# Optionally, extra arguments for prism-auto are picked up via variable TESTS_ARGS
testslocal:
	@if [ -d tests ]; then \
	  cd tests && "$(PWD)"/etc/scripts/prism-auto -t -m . -p "$(PWD)"/bin/prism --nailgun --ngprism "$(PWD)"/bin/ngprism $(TESTS_ARGS); \
	else \
	  echo "Skipping local tests"; \
	fi

# Run the tests from ../prism-tests (with different engine settings, picked up from ../prism-tests/all-engines.args)
# - Export tests are disabled, as there is currently no robust test mechanism for dealing with the
#   variations in the output of the different engines.
# - We run with --test-all, as failures for some engines should not abort the tests
# - We run with a timeout of 1 minute, as some engines take a long time for some properties
testsfull:
	cd ../prism-tests && \
	"$(PWD)"/etc/scripts/prism-auto -t -m . \
	--skip-export-runs --skip-duplicate-runs --test-all -a all-engines.args --timeout 1m \
	-p "$(PWD)"/bin/prism --nailgun $(TESTS_ARGS);

##########################
# Building distributions #
##########################

# Build prism.jar
binary:
	@echo "Generating JAR file ($(LIB_DIR)/prism.jar)..."
	@jar cmf $(SRC_DIR)/manifest.txt $(LIB_DIR)/prism.jar -C $(CLASSES_DIR) . -C . $(IMAGES_DIR) $(DTDS_DIR)

# Build prism-sources.jar
source-jar:
	@echo "Generating sources JAR file ($(LIB_DIR)/prism-sources.jar)..."
	@find $(SRC_DIR) -type f -name '*.java' -o -name '*.form' -o -name '*.jj' | sed -e "s/^$(SRC_DIR)./-C $(SRC_DIR) /" > prism-sources.txt
	@jar cf $(LIB_DIR)/prism-sources.jar @prism-sources.txt
	@rm -f prism-sources.txt

# Download a local html copy of the manual
#PRISM_MANUAL_WEBSITE = http://prismmodelchecker.localhost/manual/
PRISM_MANUAL_WEBSITE = http://www.prismmodelchecker.org/manual/
doc: clean_doc
	(cd .. && wget -r -np -k -E -nH --no-cookies --header "Cookie: setskin=offline" --restrict-file-names=windows --reject '*action=sourceblock*' $(PRISM_MANUAL_WEBSITE) $(PRISM_MANUAL_WEBSITE)/pub/skins/offline/images/)
clean_doc:
	rm -rf ../manual

# Set up version, in particular for building releases
# Unless VERSION has been passed in (as VERSION=xxx),
# extract version number from Java code using printversion
VERSION = $(shell SRC_DIR=$(SRC_DIR) $(SRC_DIR)/scripts/printversion.sh 2> /dev/null)

# Default branch for building source releases
BRANCH = master

# File/dir names for distribution
SRC_DIST_DIR = prism-$(VERSION)-src
SRC_DIST_FILE = $(SRC_DIST_DIR).tar.gz
BIN_DIST_DIR = prism-$(VERSION)-$(BINDISTSUFFIX)
BIN_DIST_FILE = $(BIN_DIST_DIR).tar.gz
BIN_DIST_INST = $(BIN_DIST_DIR)-installer.exe

# Build a (binary) distribution in the release directory

release: JFLAGS += -source 1.8 -target 1.8
release: all binary release_check_version build_release clean_binary

release_check_version:
	@if [ "$(VERSION)" = "" ]; then echo "Usage: make release VERSION=4.5"; exit 1; fi

build_release:
	@if [ "$(BINDISTSUFFIX)" = "win32" -o "$(BINDISTSUFFIX)" = "win64" ]; then \
		echo Creating Windows installer in "release/$(BIN_DIST_INST)"... && \
		makensis /NOCD /DPRISM_NAME="PRISM $(VERSION)" /DPRISM_BUILD="prism-$(VERSION)" /DPRISM_BINDISTSUFFIX="$(BINDISTSUFFIX)" /DPRISM_DIR=".." src/nsis_script.nsi && \
		mkdir -p release && \
		mv ../"$(BIN_DIST_INST)" release; \
	else \
		echo Creating binary distribution in "release/$(BIN_DIST_FILE)"... && \
		mkdir -p release && \
		rm -rf "release/$(BIN_DIST_DIR)" && \
		rm -f "release/$(BIN_DIST_FILE)" && \
		mkdir -p "release/$(BIN_DIST_DIR)" && \
		tar cf - bin lib install.sh etc -C .. manual prism-examples CHANGELOG.txt COPYING.txt README.md | tar xf - -C "release/$(BIN_DIST_DIR)" && \
		(cd release && tar cfz "$(BIN_DIST_FILE)" "$(BIN_DIST_DIR)") && rm -rf "release/$(BIN_DIST_DIR)"; \
	fi

release_source: release_check_version build_release_source

build_release_source:
	@echo Creating source distribution from branch "$(BRANCH)" in "release/$(SRC_DIST_FILE)"... && \
	mkdir -p release && \
	rm -rf "release/$(SRC_DIST_DIR)" && \
	rm -f "release/$(SRC_DIST_FILE)" && \
	mkdir -p "release/$(SRC_DIST_DIR)" && \
	(cd .. && git archive $(BRANCH) --format=tar) | tar xf - -C "release/$(SRC_DIST_DIR)" && \
	(cd release && tar cfz "$(SRC_DIST_FILE)" "$(SRC_DIST_DIR)") && rm -rf "release/$(SRC_DIST_DIR)"

# Build Javadoc (and put in javadoc directory)

PRISM_CLASSPATH = "$(CLASSES_DIR)$(CLASSPATHSEP)$(LIB_DIR)/*"

javadoc:
	@JAVADOC_DIRS=`echo $(MAKE_DIRS) | sed 's/\//./g' | sed 's/ /:/g'` && \mkdir -p javadoc; javadoc $(JFLAGS) -d javadoc -overview $(SRC_DIR)/overview.html -sourcepath $(SRC_DIR) -classpath $(SRC_DIR)$(CLASSPATHSEP)$(PRISM_CLASSPATH) -subpackages $$JAVADOC_DIRS -exclude parser

###############
# Cleaning up #
###############

# Clean main PRISM build (not CUDD or external libs)
clean: checks
	@(for dir in $(MAKE_DIRS); do \
	  echo Cleaning src/$$dir ...; \
	  (cd src/$$dir && \
	  $(MAKE) -s SRC_DIR="$(SRC_DIR)" CLASSES_DIR="$(CLASSES_DIR)" OBJ_DIR="$(OBJ_DIR)" LIB_DIR="$(LIB_DIR)" EXE="$(EXE)" LIBPREFIX="$(LIBPREFIX)" LIBSUFFIX="$(LIBSUFFIX)" clean) \
	  || exit 1; \
	done; \
	find $(CLASSES_DIR) -name '*.class' -exec rm {} \; ; \
	rm -f $(LIB_DIR)/*jnilib; \
	rm -f $(LIB_DIR)/prism.jar; \
	rm -f $(LIB_DIR)/prism-sources.jar; \
	rm -f $(BIN_PRISM) $(BIN_XPRISM) $(BIN_PRISM_BAT) $(BIN_XPRISM_BAT) )

celan: clean

# Clean PRISM + CUDD and external libs
clean_all: checks clean_cudd clean_ext clean

clean_cudd:
	@(cd $(CUDD_DIR) && $(MAKE) distclean)

clean_ext:
	@(for ext in $(EXT_PACKAGES); do \
	  echo Cleaning $$ext ...; \
	  (cd ext/$$ext && \
	  $(MAKE) -s LIBPREFIX="$(LIBPREFIX)" LIBSUFFIX="$(LIBSUFFIX)" clean) \
	  || exit 1; \
	done )

# Remove just the prism.jar binary
clean_binary:
	@echo "Removing JAR file ($(LIB_DIR)/prism.jar)..."
	@rm -f $(LIB_DIR)/prism.jar

# Clear individual directories (sometimes useful)
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

#################################################
