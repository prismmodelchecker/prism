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
-I$(PRISM_DIR_REL)/$(PRISM_INCLUDE_DIR) \
-I$(PRISM_DIR_REL)/ext/lpsolve55/include

LIBRARIES = \
-L$(PRISM_DIR_REL)/$(PRISM_LIB_DIR) \
-lodd \
-ldd \
-ldv \
-lprism \
$(LIBMATH) \
-llpsolve55

CC_FILES = $(wildcard *.cc)
O_FILES = $(CC_FILES:%.cc=$(PRISM_DIR_REL)/$(PRISM_OBJ_DIR)/$(THIS_DIR)/%.o)

default: all

all: checks $(PRISM_DIR_REL)/$(PRISM_LIB_DIR)/$(LIBPREFIX)prismsparse$(LIBSUFFIX)

# inhibit building in parallel (-j option)
.NOTPARALLEL:

# Try and prevent accidental makes (i.e. called manually, not from top-level Makefile)
checks:
	@if [ "$(PRISM_SRC_DIR)" = "" ]; then \
	  (echo "Error: This Makefile is designed to be called from the main PRISM Makefile"; exit 1) \
	fi; 

$(PRISM_DIR_REL)/$(PRISM_LIB_DIR)/$(LIBPREFIX)prismsparse$(LIBSUFFIX): $(O_FILES)
	$(LD) $(SHARED) $(LDFLAGS) -o $@ $(O_FILES) $(LIBRARIES)

$(PRISM_DIR_REL)/$(PRISM_OBJ_DIR)/$(THIS_DIR)/%.o: %.cc
	$(CXX) $(CXXFLAGS) -c $< -o $@ $(INCLUDES)

clean: checks
	@rm -f $(PRISM_DIR_REL)/$(PRISM_LIB_DIR)/$(LIBPREFIX)prismsparse$(LIBSUFFIX) $(O_FILES)

celan: clean

#################################################
