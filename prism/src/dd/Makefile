################################################
#  NB: This Makefile is designed to be called  #
#      from the main PRISM Makefile. It won't  #
#      work on its own because it needs        #
#      various options to be passed in         #
################################################

.SUFFIXES: .o .c .cc

# Reminder: $@ = target, $* = target without extension, $< = dependency

THIS_DIR = dd
PRISM_DIR_REL = ../..

INCLUDES = \
-I$(PRISM_DIR_REL)/$(CUDD_DIR)/include \
-I$(PRISM_DIR_REL)/$(INCLUDE_DIR)

LIBRARIES = \
-L$(PRISM_DIR_REL)/$(CUDD_DIR)/lib \
-lcudd \
-lutil \
-lmtr \
-lst \
-lepd

CC_FILES = $(wildcard *.cc)
O_FILES = $(CC_FILES:%.cc=$(PRISM_DIR_REL)/$(OBJ_DIR)/$(THIS_DIR)/%.o)

default: all

all: checks $(PRISM_DIR_REL)/$(LIB_DIR)/$(LIBPREFIX)dd$(LIBSUFFIX) $(PRISM_DIR_REL)/$(OBJ_DIR)/$(THIS_DIR)/dd_test$(EXE)

# Try and prevent accidental makes (i.e. called manually, not from top-level Makefile)
checks:
	@if [ "$(SRC_DIR)" = "" ]; then \
	  (echo "Error: This Makefile is designed to be called from the main PRISM Makefile"; exit 1) \
	fi; 

$(PRISM_DIR_REL)/$(LIB_DIR)/$(LIBPREFIX)dd$(LIBSUFFIX): $(O_FILES)
	$(LD) $(SHARED) $(LDFLAGS) -o $@ $(O_FILES) $(LIBRARIES)

$(PRISM_DIR_REL)/$(OBJ_DIR)/$(THIS_DIR)/dd_test$(EXE): $(PRISM_DIR_REL)/$(OBJ_DIR)/$(THIS_DIR)/dd_test.o $(PRISM_DIR_REL)/$(LIB_DIR)/$(LIBPREFIX)dd$(LIBSUFFIX)
	$(LD) $(LDFLAGS) -o $@ $(PRISM_DIR_REL)/$(OBJ_DIR)/$(THIS_DIR)/dd_test.o -L$(PRISM_DIR_REL)/$(LIB_DIR) -ldd

$(PRISM_DIR_REL)/$(OBJ_DIR)/$(THIS_DIR)/%.o: %.cc
	$(CPP) $(CPPFLAGS) -c $< -o $@ $(INCLUDES)

clean: checks
	@rm -f $(PRISM_DIR_REL)/$(LIB_DIR)/$(LIBPREFIX)dd$(LIBSUFFIX) $(O_FILES) $(PRISM_DIR_REL)/$(OBJ_DIR)/$(THIS_DIR)/dd_test$(EXE)

celan:	clean

#################################################
