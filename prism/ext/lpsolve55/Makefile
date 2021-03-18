################################################
#  NB: This Makefile is designed to be called  #
#      from the main PRISM Makefile. It won't  #
#      work on its own because it needs        #
#      various options to be passed in         #
################################################

ifeq ($(OSTYPE),linux)
	  LPSOLVE55_BUILD=ccc
endif
ifeq ($(OSTYPE),solaris)
	  LPSOLVE55_BUILD=ccc.solaris
endif
ifeq ($(OSTYPE),cygwin)
	  LPSOLVE55_BUILD=ccc.cygwin
endif
ifeq ($(OSTYPE),darwin)
	ifeq ($(ARCH),x86_64)
	  LPSOLVE55_BUILD=ccc.osx64
	else
	ifeq ($(ARCH),arm64)
	  LPSOLVE55_BUILD=ccc.osx64
	else
	  LPSOLVE55_BUILD=ccc.osx
	endif
	endif
endif

default: all

all: checks ../../$(PRISM_LIB_DIR)/$(LIBPREFIX)lpsolve55$(LIBSUFFIX)

# Try and prevent accidental makes (i.e. called manually, not from top-level Makefile)
checks:
	@if [ "$(LIBSUFFIX)" = "" ]; then \
	  (echo "Error: This Makefile is designed to be called from the main PRISM Makefile"; exit 1) \
	fi; 

../../$(PRISM_LIB_DIR)/$(LIBPREFIX)lpsolve55$(LIBSUFFIX):
	mkdir -p lib
	@(if [ "$(OSTYPE)" = "cygwin" ]; then \
	  cp bin/$(BINDISTSUFFIX)/$(LIBPREFIX)lpsolve55$(LIBSUFFIX) ../../$(PRISM_LIB_DIR); \
	else \
	  (cd src/lp_solve_5.5/lpsolve55 && \
	  ARCH=$(ARCH) c=$(CC) sh -x $(LPSOLVE55_BUILD) \
	  ) && mv lib/$(LIBPREFIX)lpsolve55$(LIBSUFFIX) ../../$(PRISM_LIB_DIR)/; \
	fi)

clean: checks
	rm -f lib/$(LIBPREFIX)lpsolve55$(LIBSUFFIX) ../../$(PRISM_LIB_DIR)/$(LIBPREFIX)lpsolve55$(LIBSUFFIX)

celan:	clean

#################################################
