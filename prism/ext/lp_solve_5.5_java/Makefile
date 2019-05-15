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
	@echo Making sure lpsolve55 Java wrapper is compatible with $(OSTYPE) $(ARCH)
	@(if [ "$(OSTYPE)" = "linux" ]; then \
	  echo "Rebuild lpsolve55 Java wrapper for Linux"; \
	  (cd lib ; ARCH=$(ARCH) JAVA_JNI_H_DIR="$(JAVA_JNI_H_DIR)" JAVA_JNI_MD_H_DIR="$(JAVA_JNI_MD_H_DIR)" sh build); \
	  if [ "$(ARCH)" = "" ]; then \
	    cp lib/lpsolve55j.jar lib/ux32/liblpsolve55j.so ../../$(PRISM_LIB_DIR)/ ; \
	  else \
	    cp lib/lpsolve55j.jar lib/ux64/liblpsolve55j.so ../../$(PRISM_LIB_DIR)/ ; \
	  fi; \
	fi)
	@(if [ "$(OSTYPE)" = "darwin" ]; then \
	  echo "Rebuild lpsolve55 Java wrapper for MacOS"; \
	  (cd lib/mac; ARCH=$(ARCH) JAVA_JNI_H_DIR="$(JAVA_JNI_H_DIR)" JAVA_JNI_MD_H_DIR="$(JAVA_JNI_MD_H_DIR)" sh -x build-osx); \
	  cp lib/lpsolve55j.jar lib/mac/liblpsolve55j.jnilib ../../$(PRISM_LIB_DIR)/ ; \
	fi)
	@(if [ "$(OSTYPE)" = "cygwin" ]; then \
	  if [ "$(ARCH)" = "x86_64" ]; then \
	    cp lib/lpsolve55j.jar lib/win64/lpsolve55j.dll ../../$(PRISM_LIB_DIR)/ ; \
	  else \
	    cp lib/lpsolve55j.jar lib/win32/lpsolve55j.dll ../../$(PRISM_LIB_DIR)/ ; \
	  fi; \
	fi)
clean: checks
	rm -f $(LPSOLVE55JAVA_DIR)/lib/lpsolve55j.jar

celan:	clean


#################################################
