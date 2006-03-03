################################################
#  NB: This Makefile is designed to be called  #
#      from the main PRISM Makefile. It won't  #
#      work on its own because it needs        #
#      various options to be passed in         #
################################################

# Reminder: $@ = target, $* = target without extension, $< = dependency

THIS_DIR = userinterface
PRISM_DIR_REL = ../..

JAVA_FILES = \
./GUIClipboard.java ./GUIClipboardEvent.java ./GUIConstantsPicker.java ./GUIFileMenu.java ./GUIGeneralOptions.java ./GUINetwork.java ./GUINetworkOptions.java ./GUIOptionsDialog.java ./GUIPlugin.java ./GUIPrism.java ./log/GUILog.java ./log/GUIVisualLogger.java ./log/GUIVisualLogModel.java ./log/GUIWindowLog.java ./GUIPrismSplash.java ./GUISimulationPicker.java ./GUITaskBar.java ./model/computation/BuildModelThread.java ./model/computation/ComputeSteadyStateThread.java ./model/computation/ComputeTransientThread.java ./model/computation/ExportBuiltModelThread.java ./model/computation/LoadGraphicModelThread.java ./model/computation/LoadPEPAModelThread.java ./model/computation/LoadPRISMModelThread.java ./model/computation/ParseModelThread.java ./model/computation/SaveGraphicModelThread.java ./model/computation/SavePEPAModelThread.java ./model/computation/SavePRISMModelThread.java ./model/graphicModel/Decision.java ./model/graphicModel/ExpressionLabel.java ./model/graphicModel/GraphicModuleContainer.java ./model/graphicModel/GraphicSelection.java ./model/graphicModel/GraphicToolBar.java ./model/graphicModel/GUIGraphicModelEditor.java ./model/graphicModel/ModuleDrawingPane.java ./model/graphicModel/ModuleModel.java ./model/graphicModel/ModulesPanel.java ./model/graphicModel/Nail.java ./model/graphicModel/ProbTransition.java ./model/graphicModel/State.java ./model/graphicModel/StringLabel.java ./model/graphicModel/Transition.java ./model/GUIModelEditor.java ./model/GUIModelEvent.java ./model/GUIMultiModel.java ./model/GUIMultiModelHandler.java ./model/GUIMultiModelTree.java ./model/GUITextModelEditor.java ./model/GUITransientTime.java ./model/pepaModel/GUIPepaModelEditor.java ./model/Style.java ./OptionsPanel.java ./properties/computation/ExportResultsThread.java ./properties/computation/LoadPropertiesThread.java ./properties/computation/ModelCheckThread.java ./properties/computation/SavePropertiesThread.java ./properties/computation/SimulateModelCheckThread.java ./properties/ConstantHeader.java ./properties/ConstantLine.java ./properties/ConstantPickerList.java ./properties/GraphConstantHeader.java ./properties/GraphConstantLine.java ./properties/GraphConstantPickerList.java ./properties/GUIExperiment.java ./properties/GUIExperimentPicker.java ./properties/GUIExperimentTable.java ./properties/GUIGraphHandler.java ./properties/GUIGraphPicker.java ./properties/GUILabel.java ./properties/GUIMultiProperties.java ./properties/GUIPropConstantList.java ./properties/GUIPropertiesEvent.java ./properties/GUIPropertiesList.java ./properties/GUIProperty.java ./properties/GUIPropertyEditor.java ./properties/GUIPropertyResultDialog.java ./properties/GUIPropLabelList.java ./properties/GUIResultsTable.java ./SimulationInformation.java ./simulator/GUIInitialStatePicker.java ./simulator/GUISimLabelFormulaeList.java ./simulator/GUISimPathFormulaeList.java ./simulator/GUISimulator.java ./simulator/networking/GUINetworkEditor.java ./simulator/networking/GUISimulatorDistributionDialog.java ./simulator/GUISimulatorPathTable.java ./simulator/GUISimulatorUpdatesTable.java ./util/BooleanProperty.java ./util/ChoiceProperty.java ./util/ColourProperty.java ./util/DoubleProperty.java ./util/ExpressionProperty.java ./util/FontChooser.java ./util/FontColorPair.java ./util/FontColorProperty.java ./util/GUIComputationEvent.java ./util/GUIEvent.java ./util/GUIEventHandler.java ./util/GUIEventListener.java ./util/GUIException.java ./util/GUIExitEvent.java ./util/GUILogEvent.java ./util/GUIPrismFileFilter.java ./util/MultipleProperty.java ./util/PresentationMetalTheme.java ./util/PropertyConstants.java ./util/PropertyException.java ./util/PropertyOwner.java ./util/PropertyTable.java ./util/PropertyTableModel.java ./util/SelectionEvent.java ./util/SelectionListener.java ./util/SelectionModel.java ./util/SeriesDataProperty.java ./util/SingleProperty.java ./util/Test2.java ./GUIComputationThread.java
#echo `find . -name '*.java'`
#JAVA_FILES:sh = find . -name '*.java'
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
