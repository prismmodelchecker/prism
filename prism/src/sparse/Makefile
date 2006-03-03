################################################
#  NB: This Makefile is designed to be called  #
#      from the main PRISM Makefile. It won't  #
#      work on its own because it needs        #
#      various options to be passed in         #
################################################

.SUFFIXES: .o .c .cc

# Reminder: $@ = target, $* = target without extension, $< = dependency

THIS_DIR = sparse
PRISM_DIR_REL = ../..

INCLUDES = \
-I$(PRISM_DIR_REL)/$(CUDD_DIR)/include \
$(JAVA_INCLUDES) \
-I$(PRISM_DIR_REL)/$(INCLUDE_DIR)

LIBRARIES = \
-L$(PRISM_DIR_REL)/$(LIB_DIR) \
-lodd \
-ldd \
-ldv \
-lprism \
-lm

JAVA_FILES = \
PrismSparse.java
#echo `/bin/ls *.java`
#JAVA_FILES:sh = /bin/ls *.java
#JAVA_FILES = $(wildcard *.java)

CLASS_FILES = $(JAVA_FILES:%.java=$(PRISM_DIR_REL)/$(CLASSES_DIR)/$(THIS_DIR)/%.class)

CC_FILES = \
PrismSparse.cc PS_JOR.cc PS_NondetBoundedUntil.cc PS_NondetExport.cc PS_NondetReachReward.cc PS_NondetUntil.cc PS_Power.cc PS_ProbBoundedUntil.cc PS_ProbExport.cc PS_ProbReachReward.cc PS_ProbUntil.cc PS_SOR.cc PS_StochBoundedUntil.cc PS_StochCumulReward.cc PS_StochExport.cc PS_StochSteadyState.cc PS_StochTransient.cc sparse.cc
#echo `/bin/ls *.cc`
#CC_FILES:sh = /bin/ls *.cc
#CC_FILES = $(wildcard *.cc)

O_FILES = $(CC_FILES:%.cc=$(PRISM_DIR_REL)/$(OBJ_DIR)/$(THIS_DIR)/%.o)

default: all

all: checks $(CLASS_FILES) $(PRISM_DIR_REL)/$(INCLUDE_DIR)/PrismSparse.h $(PRISM_DIR_REL)/$(LIB_DIR)/$(LIBPREFIX)prismsparse$(LIBSUFFIX)

# Try and prevent accidental makes (i.e. called manually, not from top-level Makefile)
checks:
	@if [ "$(SRC_DIR)" = "" ]; then \
	  (echo "Error: This Makefile is designed to be called from the main PRISM Makefile"; exit 1) \
	fi; 

$(PRISM_DIR_REL)/$(CLASSES_DIR)/$(THIS_DIR)/%.class: %.java
	(cd ..; $(JAVAC) -sourcepath $(THIS_DIR)/$(PRISM_DIR_REL)/$(SRC_DIR) -classpath $(THIS_DIR)/$(PRISM_DIR_REL)/$(CLASSES_DIR) -d $(THIS_DIR)/$(PRISM_DIR_REL)/$(CLASSES_DIR) $(THIS_DIR)/$<)

$(PRISM_DIR_REL)/$(INCLUDE_DIR)/PrismSparse.h: $(PRISM_DIR_REL)/$(CLASSES_DIR)/$(THIS_DIR)/PrismSparse.class
	($(JAVAH) -classpath $(PRISM_DIR_REL)/$(CLASSES_DIR) -jni -o $@ $(THIS_DIR).PrismSparse; touch $@)

$(PRISM_DIR_REL)/$(LIB_DIR)/$(LIBPREFIX)prismsparse$(LIBSUFFIX): $(O_FILES)
	$(LD) $(SHARED) $(LDFLAGS) -o $@ $(O_FILES) $(PRISM_DIR_REL)/$(OBJ_DIR)/prism/foxglynn.o $(PRISM_DIR_REL)/$(OBJ_DIR)/dv/dv.o $(LIBRARIES)

$(PRISM_DIR_REL)/$(OBJ_DIR)/$(THIS_DIR)/%.o: %.cc
	$(CPP) $(CPPFLAGS) -c $< -o $@ $(INCLUDES)

clean: checks
	@rm -f $(CLASS_FILES) $(PRISM_DIR_REL)/$(LIB_DIR)/$(LIBPREFIX)prismsparse$(LIBSUFFIX) $(O_FILES)

celan: clean

#################################################
