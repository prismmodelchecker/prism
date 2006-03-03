################################################
#  NB: This Makefile is designed to be called  #
#      from the main PRISM Makefile. It won't  #
#      work on its own because it needs        #
#      various options to be passed in         #
################################################

# Reminder: $@ = target, $* = target without extension, $< = dependency

THIS_DIR = settings
PRISM_DIR_REL = ../..

JAVA_FILES = \
Apple.java BooleanConstraint.java BooleanEditor.java BooleanRenderer.java BooleanSetting.java ChoiceEditor.java ChoiceRenderer.java ChoiceSetting.java ColorConstraint.java ColorEditor.java ColorRenderer.java ColorSetting.java DefaultSettingOwner.java DoubleEditor.java DoubleRangeConstraint.java DoubleRenderer.java DoubleSetting.java FileEditor.java FileRenderer.java FileSelector.java FileSetting.java FontChooser.java FontColorConstraint.java FontColorEditor.java FontColorPair.java FontColorRenderer.java FontColorSetting.java IntegerEditor.java IntegerRenderer.java IntegerSetting.java MultipleLineStringEditor.java MultipleLineStringRenderer.java MultipleLineStringSetting.java NumericConstraint.java Orange.java RangeConstraint.java SettingConstraint.java SettingDisplay.java SettingEditor.java SettingException.java Setting.java SettingOwner.java SettingRenderer.java SettingTable.java SingleLineStringEditor.java SingleLineStringRenderer.java SingleLineStringSetting.java StringConstraint.java TableResizer.java Test2.java Test.java

#echo `/bin/ls *.java`








#JAVA_FILES:sh = /bin/ls *.java
#JAVA_FILES = $(wildcard *.java)

CLASS_FILES = $(JAVA_FILES:%.java=$(PRISM_DIR_REL)/$(CLASSES_DIR)/$(THIS_DIR)/%.class)

default: all

all: checks $(CLASS_FILES)

# Try and prevent accidental makes (i.e. called manually, not from top-level Makefile)
checks:
	@if [ "$(SRC_DIR)" = "" ]; then \
	  (echo "Error: This Makefile is designed to be called from the main PRISM Makefile"; exit 1) \
	fi; 

$(PRISM_DIR_REL)/$(CLASSES_DIR)/$(THIS_DIR)/%.class: %.java
	(cd ..; $(JAVAC) -sourcepath $(THIS_DIR)/$(PRISM_DIR_REL)/$(SRC_DIR) -classpath $(THIS_DIR)/$(PRISM_DIR_REL)/$(CLASSES_DIR) -d $(THIS_DIR)/$(PRISM_DIR_REL)/$(CLASSES_DIR) $(THIS_DIR)/$<)

clean: checks
	@rm -f $(CLASS_FILES)

celan: clean

#################################################
