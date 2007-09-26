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
-I"$(JAVA_JNI_H_DIR)" \
-I"$(JAVA_JNI_MD_H_DIR)" \
-I$(PRISM_DIR_REL)/$(INCLUDE_DIR)

LIBRARIES = \
-lm

JAVA_FILES = $(wildcard *.java)
CLASS_FILES = $(JAVA_FILES:%.java=$(PRISM_DIR_REL)/$(CLASSES_DIR)/$(THIS_DIR)/%.class)

CC_FILES = $(wildcard *.cc)
O_FILES = $(CC_FILES:%.cc=$(PRISM_DIR_REL)/$(OBJ_DIR)/$(THIS_DIR)/%.o)

PRISM_CLASSPATH = "$(THIS_DIR)/$(PRISM_DIR_REL)/$(CLASSES_DIR)$(CLASSPATHSEP)$(THIS_DIR)/$(PRISM_DIR_REL)/lib/jcommon.jar$(CLASSPATHSEP)$(THIS_DIR)/$(PRISM_DIR_REL)/lib/jfreechart.jar$(CLASSPATHSEP)$(THIS_DIR)/$(PRISM_DIR_REL)/lib/epsgraphics.jar"

default: all

all: checks $(CLASS_FILES) $(PRISM_DIR_REL)/$(INCLUDE_DIR)/PrismUtils.h $(PRISM_DIR_REL)/$(LIB_DIR)/$(LIBPREFIX)prism$(LIBSUFFIX)

# Try and prevent accidental makes (i.e. called manually, not from top-level Makefile)
checks:
	@if [ "$(SRC_DIR)" = "" ]; then \
	  (echo "Error: This Makefile is designed to be called from the main PRISM Makefile"; exit 1) \
	fi; 

$(PRISM_DIR_REL)/$(CLASSES_DIR)/$(THIS_DIR)/%.class: %.java
	(cd ..; $(JAVAC) -sourcepath $(THIS_DIR)/$(PRISM_DIR_REL)/$(SRC_DIR) -classpath $(PRISM_CLASSPATH) -d $(THIS_DIR)/$(PRISM_DIR_REL)/$(CLASSES_DIR) $(THIS_DIR)/$<)

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
