################################################
#  NB: This Makefile is designed to be called  #
#      from the main PRISM Makefile. It won't  #
#      work on its own because it needs        #
#      various options to be passed in         #
################################################

.SUFFIXES: .o .c .cc

# Reminder: $@ = target, $* = target without extension, $< = dependency

THIS_DIR = mtbdd
PRISM_DIR_REL = ../..

INCLUDES = \
-I$(PRISM_DIR_REL)/$(CUDD_DIR)/include \
$(JAVA_INCLUDES) \
-I$(PRISM_DIR_REL)/$(INCLUDE_DIR)

LIBRARIES = \
-L$(PRISM_DIR_REL)/$(LIB_DIR) \
-lodd \
-ldd \
-lprism \
-lm

JAVA_FILES = \
PrismMTBDD.java
#echo `/bin/ls *.java`
#JAVA_FILES:sh = /bin/ls *.java
#JAVA_FILES = $(wildcard *.java)

CLASS_FILES = $(JAVA_FILES:%.java=$(PRISM_DIR_REL)/$(CLASSES_DIR)/$(THIS_DIR)/%.class)

CC_FILES = \
PM_ExportLabels.cc PM_ExportMatrix.cc PM_ExportVector.cc PM_JOR.cc PM_NondetBoundedUntil.cc PM_NondetReachReward.cc PM_NondetUntil.cc PM_Power.cc PM_Prob0A.cc PM_Prob0.cc PM_Prob0E.cc PM_Prob1A.cc PM_Prob1.cc PM_Prob1E.cc PM_ProbBoundedUntil.cc PM_ProbReachReward.cc PM_ProbUntil.cc PM_Reachability.cc PM_StochBoundedUntil.cc PM_StochCumulReward.cc PM_StochSteadyState.cc PM_StochTransient.cc PrismMTBDD.cc
#echo `/bin/ls *.cc`
#CC_FILES:sh = /bin/ls *.cc
#CC_FILES = $(wildcard *.cc)

O_FILES = $(CC_FILES:%.cc=$(PRISM_DIR_REL)/$(OBJ_DIR)/$(THIS_DIR)/%.o)

default: all

all: checks $(CLASS_FILES) $(PRISM_DIR_REL)/$(INCLUDE_DIR)/PrismMTBDD.h $(PRISM_DIR_REL)/$(LIB_DIR)/$(LIBPREFIX)prismmtbdd$(LIBSUFFIX)

# Try and prevent accidental makes (i.e. called manually, not from top-level Makefile)
checks:
	@if [ "$(SRC_DIR)" = "" ]; then \
	  (echo "Error: This Makefile is designed to be called from the main PRISM Makefile"; exit 1) \
	fi; 

$(PRISM_DIR_REL)/$(CLASSES_DIR)/$(THIS_DIR)/%.class: %.java
	(cd ..; $(JAVAC) -sourcepath $(THIS_DIR)/$(PRISM_DIR_REL)/$(SRC_DIR) -classpath $(THIS_DIR)/$(PRISM_DIR_REL)/$(CLASSES_DIR) -d $(THIS_DIR)/$(PRISM_DIR_REL)/$(CLASSES_DIR) $(THIS_DIR)/$<)

$(PRISM_DIR_REL)/$(INCLUDE_DIR)/PrismMTBDD.h: $(PRISM_DIR_REL)/$(CLASSES_DIR)/$(THIS_DIR)/PrismMTBDD.class
	($(JAVAH) -classpath $(PRISM_DIR_REL)/$(CLASSES_DIR) -jni -o $@ $(THIS_DIR).PrismMTBDD; touch $@)

$(PRISM_DIR_REL)/$(LIB_DIR)/$(LIBPREFIX)prismmtbdd$(LIBSUFFIX): $(O_FILES)
	$(LD) $(SHARED) $(LDFLAGS) -o $@ $(O_FILES) $(PRISM_DIR_REL)/$(OBJ_DIR)/prism/foxglynn.o $(LIBRARIES)

$(PRISM_DIR_REL)/$(OBJ_DIR)/$(THIS_DIR)/%.o: %.cc
	$(CPP) $(CPPFLAGS) -c $< -o $@ $(INCLUDES)

clean: checks
	@rm -f $(CLASS_FILES) $(PRISM_DIR_REL)/$(LIB_DIR)/$(LIBPREFIX)prismmtbdd$(LIBSUFFIX) $(O_FILES)

celan: clean

#################################################
