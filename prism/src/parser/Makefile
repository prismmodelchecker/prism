################################################
#  NB: This Makefile is designed to be called  #
#      from the main PRISM Makefile. It won't  #
#      work on its own because it needs        #
#      various options to be passed in         #
################################################

# Reminder: $@ = target, $* = target without extension, $< = dependency

THIS_DIR = parser
PRISM_DIR_REL = ../..

JAVA_FILES = \
Command.java ConstantList.java Declaration.java ExpressionAnd.java ExpressionBinary.java ExpressionBrackets.java ExpressionConstant.java ExpressionDivide.java ExpressionDouble.java ExpressionFalse.java ExpressionFormula.java ExpressionFunc.java ExpressionIdent.java ExpressionInt.java ExpressionITE.java Expression.java ExpressionMinus.java ExpressionNary.java ExpressionNot.java ExpressionOr.java ExpressionPlus.java ExpressionRange.java ExpressionRelOp.java ExpressionTimes.java ExpressionTrue.java ExpressionUnary.java ExpressionUnaryMinus.java ExpressionVar.java ForLoop.java FormulaList.java LabelList.java Module.java ModulesFile.java ParseException.java PCTLAnd.java PCTLBrackets.java PCTLExpression.java PCTLFormulaBinary.java PCTLFormula.java PCTLFormulaNary.java PCTLFormulaUnary.java PCTLImplies.java PCTLInit.java PCTLLabel.java PCTLNot.java PCTLOr.java PCTLProbBoundedUntil.java PCTLProb.java PCTLProbNext.java PCTLProbUntil.java PCTLRewardCumul.java PCTLRewardInst.java PCTLReward.java PCTLRewardReach.java PCTLRewardSS.java PCTLSS.java PrismParserConstants.java PrismParser.java PrismParserTokenManager.java PrismSyntaxHighlighter.java PropertiesFile.java RenamedModule.java RewardStructItem.java RewardStruct.java SimpleCharStream.java SystemBrackets.java SystemDefn.java SystemFullParallel.java SystemHide.java SystemInterleaved.java SystemModule.java SystemParallel.java SystemRename.java Token.java TokenMgrError.java Update.java Updates.java Values.java VarList.java
#echo `/bin/ls *.java`
#JAVA_FILES:sh = /bin/ls *.java

#JAVA_FILES = $(wildcard *.java)

CLASS_FILES = $(JAVA_FILES:%.java=$(PRISM_DIR_REL)/$(CLASSES_DIR)/$(THIS_DIR)/%.class)

default: all

all: checks PrismParser.java $(CLASS_FILES)

# Try and prevent accidental makes (i.e. called manually, not from top-level Makefile)
checks:
	@if [ "$(SRC_DIR)" = "" ]; then \
	  (echo "Error: This Makefile is designed to be called from the main PRISM Makefile"; exit 1) \
	fi; 

PrismParser.java: PrismParser.jj
	javacc $< || echo "Warning: Did not recompile PrismParser.jj"

$(PRISM_DIR_REL)/$(CLASSES_DIR)/$(THIS_DIR)/%.class: %.java
	(cd ..; $(JAVAC) -sourcepath $(THIS_DIR)/$(PRISM_DIR_REL)/$(SRC_DIR) -classpath $(THIS_DIR)/$(PRISM_DIR_REL)/$(CLASSES_DIR) -d $(THIS_DIR)/$(PRISM_DIR_REL)/$(CLASSES_DIR) $(THIS_DIR)/$<)

clean: checks
	@rm -f $(CLASS_FILES)

celan: clean

#################################################
