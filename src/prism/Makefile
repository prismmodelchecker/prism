################################################
#  NB: This Makefile is designed to be called  #
#      from the main PRISM Makefile. It won't  #
#      work on its own because it needs        #
#      various options to be passed in         #
################################################

.SUFFIXES: .o .c .cc

# Reminder: $@ = target, $* = target without extension, $< = dependency

THIS_DIR = prism
PRISM_DIR_REL = ../..

INCLUDES = \
$(JAVA_INCLUDES) \
-I$(PRISM_DIR_REL)/$(INCLUDE_DIR)

LIBRARIES = \
-lm

JAVA_FILES = \
DefinedConstant.java DisplayableData.java Explicit2MTBDD.java Expression2MTBDD.java ModelChecker.java Model.java Modules2MTBDD.java NondetModelChecker.java NondetModel.java Preprocessor.java PrismCL.java PrismException.java PrismFileLog.java Prism.java PrismLog.java PrismSettings.java PrismSettingsListener.java PrismUtils.java ProbModelChecker.java ProbModel.java PropertyConstants.java RangingConstant.java ResultsCollection.java SCCComputer.java StateList.java StateListMTBDD.java StateProbsDV.java StateProbs.java StateProbsMTBDD.java StochModelChecker.java StochModel.java UndefinedConstants.java
#echo `/bin/ls *.java`


#JAVA_FILES:sh = /bin/ls *.java




#JAVA_FILES = $(wildcard *.java)

CLASS_FILES = $(JAVA_FILES:%.java=$(PRISM_DIR_REL)/$(CLASSES_DIR)/$(THIS_DIR)/%.class)

CC_FILES = \
foxglynn.cc PrismUtils.cc
#echo `/bin/ls *.cc`
#CC_FILES:sh = /bin/ls *.cc
#CC_FILES = $(wildcard *.cc)

O_FILES = $(CC_FILES:%.cc=$(PRISM_DIR_REL)/$(OBJ_DIR)/$(THIS_DIR)/%.o)

default: all

all: checks $(CLASS_FILES) $(PRISM_DIR_REL)/$(INCLUDE_DIR)/PrismUtils.h $(PRISM_DIR_REL)/$(LIB_DIR)/$(LIBPREFIX)prism$(LIBSUFFIX)

# Try and prevent accidental makes (i.e. called manually, not from top-level Makefile)
checks:
	@if [ "$(SRC_DIR)" = "" ]; then \
	  (echo "Error: This Makefile is designed to be called from the main PRISM Makefile"; exit 1) \
	fi; 

$(PRISM_DIR_REL)/$(CLASSES_DIR)/$(THIS_DIR)/%.class: %.java
	(cd ..; $(JAVAC) -sourcepath $(THIS_DIR)/$(PRISM_DIR_REL)/$(SRC_DIR) -classpath $(THIS_DIR)/$(PRISM_DIR_REL)/$(CLASSES_DIR) -d $(THIS_DIR)/$(PRISM_DIR_REL)/$(CLASSES_DIR) $(THIS_DIR)/$<)

$(PRISM_DIR_REL)/$(INCLUDE_DIR)/PrismUtils.h: $(PRISM_DIR_REL)/$(CLASSES_DIR)/$(THIS_DIR)/PrismUtils.class
	($(JAVAH) -classpath $(PRISM_DIR_REL)/$(CLASSES_DIR) -jni -o $@ $(THIS_DIR).PrismUtils; touch $@)

$(PRISM_DIR_REL)/$(LIB_DIR)/$(LIBPREFIX)prism$(LIBSUFFIX): $(O_FILES)
	$(LD) $(SHARED) $(LDFLAGS) -o $@ $(O_FILES) $(LIBRARIES)

$(PRISM_DIR_REL)/$(OBJ_DIR)/$(THIS_DIR)/%.o: %.cc
	$(CPP) $(CPPFLAGS) -c $< -o $@ $(INCLUDES)

clean: checks
	@rm -f $(CLASS_FILES) $(PRISM_DIR_REL)/$(LIB_DIR)/$(LIBPREFIX)prism$(LIBSUFFIX) $(O_FILES)

celan: clean

#################################################
