################################################
#  NB: This Makefile is designed to be called  #
#      from the main PRISM Makefile. It won't  #
#      work on its own because it needs        #
#      various options to be passed in         #
################################################

.SUFFIXES: .o .c .cc

# Reminder: $@ = target, $* = target without extension, $< = dependency

PRISM_DIR_REL = ..

JAVA_FILES_ALL := $(shell find . -name '*.java')
JAVA_FILES = $(subst package-info.java,,$(JAVA_FILES_ALL))
CLASS_FILES = $(JAVA_FILES:%.java=$(PRISM_DIR_REL)/$(PRISM_CLASSES_DIR)/%.class)

PRISM_CLASSPATH = "$(PRISM_DIR_REL)/$(PRISM_CLASSES_DIR)$(CLASSPATHSEP)$(PRISM_DIR_REL)/$(PRISM_LIB_DIR)/*"

default: all

all: checks class_files

# inhibit building in parallel (-j option)
.NOTPARALLEL:

# Try and prevent accidental makes (i.e. called manually, not from top-level Makefile)
checks:
	@if [ "$(PRISM_SRC_DIR)" = "" ]; then \
	  (echo "Error: This Makefile is designed to be called from the main PRISM Makefile"; exit 1) \
	fi;

class_files:
	@echo "$(JAVA_FILES)" > java_files.txt
	$(JAVAC) $(JFLAGS) -sourcepath "$(PRISM_DIR_REL)/$(PRISM_SRC_DIR)$(CLASSPATHSEP)$(PRISM_DIR_REL)/unit-tests"\
	                   -classpath $(PRISM_CLASSPATH)\
	                   -d $(PRISM_DIR_REL)/$(PRISM_CLASSES_DIR)\
	                   @java_files.txt
	@rm -f java_files.txt

clean: checks
	@rm -f $(CLASS_FILES)

celan: clean

#################################################
