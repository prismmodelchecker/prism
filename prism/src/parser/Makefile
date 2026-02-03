################################################
#  NB: This Makefile is designed to be called  #
#      from the main PRISM Makefile. It won't  #
#      work on its own because it needs        #
#      various options to be passed in         #
################################################

# Reminder: $@ = target, $* = target without extension, $< = dependency

THIS_DIR = parser
PRISM_DIR_REL = ../..

default: all

all: checks PrismParser.java Prism3To4.java

# inhibit building in parallel (-j option)
.NOTPARALLEL:

# Try and prevent accidental makes (i.e. called manually, not from top-level Makefile)
checks:
	@if [ "$(PRISM_SRC_DIR)" = "" ]; then \
	  (echo "Error: This Makefile is designed to be called from the main PRISM Makefile"; exit 1) \
	fi; 

PrismParser.java: PrismParser.jj
	$(JAVACC) $< || echo "Warning: Did not recompile PrismParser.jj"

Prism3To4.java: Prism3To4.jj
	$(JAVACC) $< || echo "Warning: Did not recompile Prism3To4.jj"

clean: checks

touch: checks
	@touch PrismParser.jj

celan: clean

#################################################
