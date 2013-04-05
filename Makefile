############################################################
#  Small makefile for building PRISM source distributions  #
############################################################

default: none

none:
	@echo 'Did you want to build PRISM? Do "cd prism" and then "make"'

# By default, extract version number from Version.java
# Can be overridden by passing VERSION=xxx
VERSION_NUM = $(shell grep versionString prism/src/prism/Version.java | sed -E 's/[^"]+"([^"]+)"[^"]+/\1/')
VERSION_SUFFIX = $(shell grep versionSuffixString prism/src/prism/Version.java | sed -E 's/[^"]+"([^"]*)"[^"]+/\1/')
VERSION = $(VERSION_NUM)$(VERSION_SUFFIX)

# Build a source distribution
dist_src: version
	mkdir dontcopy
	@if [ -e prism/examples ]; then \
	  echo "mv prism/examples dontcopy"; mv prism/examples dontcopy; \
	fi
	@if [ -e prism/tests ]; then \
	  echo "mv prism/tests dontcopy"; mv prism/tests dontcopy; \
	fi
	mv prism-examples prism/examples
	mv cudd prism
	mv prism "prism-$(VERSION)-src"
	(cd "prism-$(VERSION)-src"; $(MAKE) dist_src )
	tar --exclude=.svn cfz "prism-$(VERSION)-src.tar.gz" "prism-$(VERSION)-src"

# Display version
version:
	@echo VERSION = $(VERSION)

#################################################
