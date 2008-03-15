################################################
#  NB: This Makefile is designed to be called  #
#      from the main PRISM Makefile. It won't  #
#      work on its own because it needs        #
#      various options to be passed in         #
################################################

# Reminder: $@ = target, $* = target without extension, $< = dependency

THIS_DIR = parser
PRISM_DIR_REL = ../..

JAVA_FILES = $(wildcard *.java ast/*.java visitor/*.java)
CLASS_FILES = $(JAVA_FILES:%.java=$(PRISM_DIR_REL)/$(CLASSES_DIR)/$(THIS_DIR)/%.class)

default: all

all: checks PrismParser.java Prism3To4.java $(CLASS_FILES)

# Try and prevent accidental makes (i.e. called manually, not from top-level Makefile)
checks:
	@if [ "$(SRC_DIR)" = "" ]; then \
	  (echo "Error: This Makefile is designed to be called from the main PRISM Makefile"; exit 1) \
	fi; 

PrismParser.java: PrismParser.jj
	@javacc $< || echo "Warning: Did not recompile PrismParser.jj"

Prism3To4.java: Prism3To4.jj
	@javacc $< || echo "Warning: Did not recompile Prism3To4.jj"

$(PRISM_DIR_REL)/$(CLASSES_DIR)/$(THIS_DIR)/%.class: %.java
	(cd ..; $(JAVAC) -sourcepath $(THIS_DIR)/$(PRISM_DIR_REL)/$(SRC_DIR) -classpath $(THIS_DIR)/$(PRISM_DIR_REL)/$(CLASSES_DIR) -d $(THIS_DIR)/$(PRISM_DIR_REL)/$(CLASSES_DIR) $(THIS_DIR)/$<)

clean: checks
	@rm -f $(CLASS_FILES)

celan: clean

#################################################
