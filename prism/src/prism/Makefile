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

default: all

all: checks $(PRISM_DIR_REL)/$(PRISM_OBJ_DIR)/$(THIS_DIR)/ngprism$(EXE)

# inhibit building in parallel (-j option)
.NOTPARALLEL:

# Try and prevent accidental makes (i.e. called manually, not from top-level Makefile)
checks:
	@if [ "$(PRISM_SRC_DIR)" = "" ]; then \
	  (echo "Error: This Makefile is designed to be called from the main PRISM Makefile"; exit 1) \
	fi; 

$(PRISM_DIR_REL)/$(PRISM_OBJ_DIR)/$(THIS_DIR)/ngprism$(EXE): ngprism.c
# For ngprism on Windows, link winsocket library (ws2_32)
	@if [ "$(EXE)" = "" ]; then \
	  $(CC) $(LDFLAGS) $(CFLAGS) -o $@ $<; \
	else \
	  $(CC) $(LDFLAGS) $(CFLAGS) -o $@ $< -lws2_32; \
	fi;

clean: checks
	@rm -f $(PRISM_DIR_REL)/$(PRISM_OBJ_DIR)/$(THIS_DIR)/ngprism$(EXE)

celan: clean

#################################################
