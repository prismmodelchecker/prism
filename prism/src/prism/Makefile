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
$(LIBMATH)

JNI_GEN_HEADER_DIR=$(THIS_DIR)/$(PRISM_DIR_REL)/$(INCLUDE_DIR)/jni

JAVA_FILES_ALL = $(wildcard *.java)
JAVA_FILES = $(subst package-info.java,,$(JAVA_FILES_ALL))
CLASS_FILES = $(JAVA_FILES:%.java=$(PRISM_DIR_REL)/$(CLASSES_DIR)/$(THIS_DIR)/%.class)

PRISM_CLASSPATH = "$(THIS_DIR)/$(PRISM_DIR_REL)/$(CLASSES_DIR)$(CLASSPATHSEP)$(THIS_DIR)/$(PRISM_DIR_REL)/lib/*"

CC_FILES = $(wildcard *.cc)
O_FILES = $(CC_FILES:%.cc=$(PRISM_DIR_REL)/$(OBJ_DIR)/$(THIS_DIR)/%.o)

default: all

all: checks $(CLASS_FILES) $(PRISM_DIR_REL)/$(LIB_DIR)/$(LIBPREFIX)prism$(LIBSUFFIX) $(PRISM_DIR_REL)/$(OBJ_DIR)/$(THIS_DIR)/ngprism$(EXE)

# inhibit building in parallel (-j option)
.NOTPARALLEL:

# Try and prevent accidental makes (i.e. called manually, not from top-level Makefile)
checks:
	@if [ "$(SRC_DIR)" = "" ]; then \
	  (echo "Error: This Makefile is designed to be called from the main PRISM Makefile"; exit 1) \
	fi; 

$(PRISM_DIR_REL)/$(CLASSES_DIR)/$(THIS_DIR)/%.class: %.java
	(cd ..; $(JAVAC) -sourcepath $(THIS_DIR)/$(PRISM_DIR_REL)/$(SRC_DIR) -classpath $(PRISM_CLASSPATH) -h $(JNI_GEN_HEADER_DIR) -d $(THIS_DIR)/$(PRISM_DIR_REL)/$(CLASSES_DIR) $(THIS_DIR)/$<)

$(PRISM_DIR_REL)/$(LIB_DIR)/$(LIBPREFIX)prism$(LIBSUFFIX): $(O_FILES)
	$(LD) $(SHARED) $(LDFLAGS) -o $@ $(O_FILES) $(LIBRARIES)

$(PRISM_DIR_REL)/$(OBJ_DIR)/$(THIS_DIR)/%.o: %.cc
	$(CPP) $(CPPFLAGS) -c $< -o $@ $(INCLUDES)

$(PRISM_DIR_REL)/$(OBJ_DIR)/$(THIS_DIR)/ngprism$(EXE): ngprism.c
# For ngprism on Windows, link winsocket library (ws2_32)
	@if [ "$(EXE)" = "" ]; then \
	  $(C) $(LDFLAGS) $(CFLAGS) -o $@ $<; \
	else \
	  $(C) $(LDFLAGS) $(CFLAGS) -o $@ $< -lws2_32; \
	fi;

clean: checks
	@rm -f $(CLASS_FILES) $(PRISM_DIR_REL)/$(LIB_DIR)/$(LIBPREFIX)prism$(LIBSUFFIX) $(O_FILES) $(PRISM_DIR_REL)/$(OBJ_DIR)/$(THIS_DIR)/ngprism$(EXE)

celan: clean

#################################################
