################################################
#  NB: This Makefile is designed to be called  #
#      from the main PRISM Makefile. It won't  #
#      work on its own because it needs        #
#      various options to be passed in         #
################################################

.SUFFIXES: .o .c .cc

# Reminder: $@ = target, $* = target without extension, $< = dependency

THIS_DIR = simulator
PRISM_DIR_REL = ../..

INCLUDES = \
-I$(PRISM_DIR_REL)/$(CUDD_DIR)/include \
$(JAVA_INCLUDES) \
-I$(PRISM_DIR_REL)/$(INCLUDE_DIR)

LIBRARIES = \
-L$(PRISM_DIR_REL)/$(LIB_DIR) \
-lm

JAVA_FILES = \
./networking/FileSystem.java ./networking/RemoteHandler.java ./networking/SimulatorResultsFile.java ./networking/SimulatorNetworkHandler.java ./networking/SSHHost.java ./NetTest.java ./SimulatorEngine.java ./SimulatorException.java 

#echo `find . -name '*.java'`
#JAVA_FILES:sh = /bin/ls *.java
#JAVA_FILES = $(wildcard *.java)

CLASS_FILES = $(JAVA_FILES:%.java=$(PRISM_DIR_REL)/$(CLASSES_DIR)/$(THIS_DIR)/%.class)

CC_FILES = \
simexpression.cc simexpressionbuilder.cc \
simutil.cc simstate.cc simmodel.cc simreasoning.cc simupdater.cc simpctl.cc \
simsampling.cc simpath.cc simformulae.cc \
SimulatorEngine.cc \
simtransitionbuilder.cc simrandom.cc simpctlbuilder.cc simengine.cc \
simrewardsbuilder.cc simiohandler.cc simtest.cc simlog.cc
#echo `/bin/ls *.cc`
#CC_FILES:sh = /bin/ls *.cc
#CC_FILES = $(wildcard *.cc)

O_FILES = $(CC_FILES:%.cc=$(PRISM_DIR_REL)/$(OBJ_DIR)/$(THIS_DIR)/%.o)

default: all

all: checks $(CLASS_FILES) $(PRISM_DIR_REL)/$(INCLUDE_DIR)/SimulatorEngine.h $(PRISM_DIR_REL)/$(LIB_DIR)/$(LIBPREFIX)simengine$(LIBSUFFIX) #$(PRISM_DIR_REL)/bin/prismsimulator$(EXE)

# Try and prevent accidental makes (i.e. called manually, not from top-level Makefile)
checks:
	@if [ "$(SRC_DIR)" = "" ]; then \
	  (echo "Error: This Makefile is designed to be called from the main PRISM Makefile"; exit 1) \
	fi; 

$(PRISM_DIR_REL)/$(CLASSES_DIR)/$(THIS_DIR)/%.class: %.java
	(cd ..; $(JAVAC) -sourcepath $(THIS_DIR)/$(PRISM_DIR_REL)/$(SRC_DIR) -classpath $(THIS_DIR)/$(PRISM_DIR_REL)/$(CLASSES_DIR) -d $(THIS_DIR)/$(PRISM_DIR_REL)/$(CLASSES_DIR) $(THIS_DIR)/$<)

$(PRISM_DIR_REL)/$(INCLUDE_DIR)/SimulatorEngine.h: $(PRISM_DIR_REL)/$(CLASSES_DIR)/$(THIS_DIR)/SimulatorEngine.class
	($(JAVAH) -classpath $(PRISM_DIR_REL)/$(CLASSES_DIR) -jni -o $@ $(THIS_DIR).SimulatorEngine; touch $@)

$(PRISM_DIR_REL)/$(LIB_DIR)/$(LIBPREFIX)simengine$(LIBSUFFIX): $(O_FILES)
	$(LD) $(SHARED) $(LDFLAGS) -o $@ $(O_FILES) $(LIBRARIES) 

$(PRISM_DIR_REL)/bin/prismsimulator$(EXE): $(O_FILES)
	$(LD) $(LDFLAGS) -o $@ $(O_FILES) #$(LIBRARIES) LeakTracer.o

$(PRISM_DIR_REL)/$(OBJ_DIR)/$(THIS_DIR)/%.o: %.cc
	$(CPP) $(CPPFLAGS) -c $< -o $@ $(INCLUDES)

clean: checks
	@rm -f $(CLASS_FILES) $(PRISM_DIR_REL)/$(LIB_DIR)/$(LIBPREFIX)simengine$(LIBSUFFIX) $(O_FILES)

celan: clean

#################################################
