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
-I"$(JAVA_JNI_H_DIR)" \
-I"$(JAVA_JNI_MD_H_DIR)" \
-I$(PRISM_DIR_REL)/$(INCLUDE_DIR) \
-I$(PRISM_DIR_REL)/ext/lpsolve55/include

LIBRARIES = \
-L$(PRISM_DIR_REL)/$(LIB_DIR) \
-lodd \
-ldd \
-ldv \
-lprism \
$(LIBMATH) \
-llpsolve55

JAVA_FILES_ALL = $(wildcard *.java)
JAVA_FILES = $(subst package-info.java,,$(JAVA_FILES_ALL))
CLASS_FILES = $(JAVA_FILES:%.java=$(PRISM_DIR_REL)/$(CLASSES_DIR)/$(THIS_DIR)/%.class)

PRISM_CLASSPATH = "$(THIS_DIR)/$(PRISM_DIR_REL)/$(CLASSES_DIR)$(CLASSPATHSEP)$(THIS_DIR)/$(PRISM_DIR_REL)/lib/*"

CC_FILES = $(wildcard *.cc)
O_FILES = $(CC_FILES:%.cc=$(PRISM_DIR_REL)/$(OBJ_DIR)/$(THIS_DIR)/%.o)

default: all

all: checks $(CLASS_FILES) $(PRISM_DIR_REL)/$(INCLUDE_DIR)/PrismSparse.h $(PRISM_DIR_REL)/$(INCLUDE_DIR)/NDSparseMatrix.h $(PRISM_DIR_REL)/$(LIB_DIR)/$(LIBPREFIX)prismsparse$(LIBSUFFIX)

# Try and prevent accidental makes (i.e. called manually, not from top-level Makefile)
checks:
	@if [ "$(SRC_DIR)" = "" ]; then \
	  (echo "Error: This Makefile is designed to be called from the main PRISM Makefile"; exit 1) \
	fi; 

$(PRISM_DIR_REL)/$(CLASSES_DIR)/$(THIS_DIR)/%.class: %.java
	(cd ..; $(JAVAC) -sourcepath $(THIS_DIR)/$(PRISM_DIR_REL)/$(SRC_DIR) -classpath $(PRISM_CLASSPATH) -d $(THIS_DIR)/$(PRISM_DIR_REL)/$(CLASSES_DIR) $(THIS_DIR)/$<)

$(PRISM_DIR_REL)/$(INCLUDE_DIR)/PrismSparse.h: $(PRISM_DIR_REL)/$(CLASSES_DIR)/$(THIS_DIR)/PrismSparse.class
	($(JAVAH) -classpath $(PRISM_DIR_REL)/$(CLASSES_DIR) -jni -o $@ $(THIS_DIR).PrismSparse; touch $@)
	@if [ "$(LIBSUFFIX)" = ".dll" ]; then \
	  (dos2unix $@) \
	fi; 

$(PRISM_DIR_REL)/$(INCLUDE_DIR)/NDSparseMatrix.h: $(PRISM_DIR_REL)/$(CLASSES_DIR)/$(THIS_DIR)/NDSparseMatrix.class
	($(JAVAH) -classpath $(PRISM_DIR_REL)/$(CLASSES_DIR) -jni -o $@ $(THIS_DIR).NDSparseMatrix; touch $@)
	@if [ "$(LIBSUFFIX)" = ".dll" ]; then \
	  (dos2unix $@) \
	fi; 

$(PRISM_DIR_REL)/$(LIB_DIR)/$(LIBPREFIX)prismsparse$(LIBSUFFIX): $(O_FILES)
	$(LD) $(SHARED) $(LDFLAGS) -o $@ $(O_FILES) $(LIBRARIES)

$(PRISM_DIR_REL)/$(OBJ_DIR)/$(THIS_DIR)/%.o: %.cc
	$(CPP) $(CPPFLAGS) -c $< -o $@ $(INCLUDES)

clean: checks
	@rm -f $(CLASS_FILES) $(PRISM_DIR_REL)/$(LIB_DIR)/$(LIBPREFIX)prismsparse$(LIBSUFFIX) $(O_FILES)

celan: clean

#################################################
