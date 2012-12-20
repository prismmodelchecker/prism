################################################
#  NB: This Makefile is designed to be called  #
#      from the main PRISM Makefile. It won't  #
#      work on its own because it needs        #
#      various options to be passed in         #
################################################

default: all

all: checks lpsolve55java

# Try and prevent accidental makes (i.e. called manually, not from top-level Makefile)
checks:
	@if [ "$(LIBSUFFIX)" = "" ]; then \
	  (echo "Error: This Makefile is designed to be called from the main PRISM Makefile"; exit 1) \
	fi; 

lpsolve55java:
	@echo Making sure lpsolve55 Java wrapper is compatible with $(OSTYPE) $(EARCH)
	@(if [ "$(OSTYPE)" = "linux" ]; then \
	  echo "Rebuild lpsolve55 Java wrapper for Linux"; \
	  pushd lib/ ; \
	  JAVA_JNI_H_DIR="$(JAVA_JNI_H_DIR)" sh build; \
	  popd; \
	  if [ "$(ARCH)" = "" ]; then \
	    cp lib/lpsolve55j.jar lib/ux32/liblpsolve55j.so ../../lib/ ; \
	  else \
	    cp lib/lpsolve55j.jar lib/ux64/liblpsolve55j.so ../../lib/ ; \
	  fi; \
	fi)
	@(if [ "$(OSTYPE)" = "darwin" ]; then \
	  echo "Rebuild lpsolve55 Java wrapper for MacOS"; \
	  pushd lib/mac/ ; \
	  sh build-osx; \
	  popd; \
	  cp lib/lpsolve55j.jar lib/mac/liblpsolve55j.jnilib ../../lib/ ; \
	fi)
	@(if [ "$(OSTYPE)" = "cygwin" ]; then \
	  cp lib/lpsolve55j.jar lib/win32/lpsolve55j.dll ../../lib/ ; \
	fi)
clean: checks
	rm -f $(LPSOLVE55JAVA_DIR)/lib/lpsolve55j.jar

celan:	clean


#################################################
