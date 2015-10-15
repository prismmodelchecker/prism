############################################################
#  Small makefile for building PRISM source distributions  #
############################################################

default: none

none:
	@echo 'Did you want to build PRISM? Do "cd prism" and then "make"'

# By default, extract version number from Java code using printversion
# Can be overridden by passing VERSION=xxx
VERSION = $(shell SRC_DIR=prism/src prism/src/scripts/printversion.sh 2> /dev/null)

# Build a (development) source distribution
dist_src: add_rev version do_build

# Build a (public) source distribution
dist_src_pub: version do_build

# Do the build
do_build:
	mkdir dontcopy
	@if [ -e prism/examples ]; then \
	  echo "mv prism/examples dontcopy"; mv prism/examples dontcopy; \
	fi
	@if [ -e prism/tests ]; then \
	  echo "mv prism/tests dontcopy"; mv prism/tests dontcopy; \
	fi
	echo "mv prism-examples prism/examples"; mv prism-examples prism/examples
	@if [ -e prism/examples-distr ]; then \
	  echo "mv prism/examples-distr/* prism/examples"; mv prism/examples-distr/* prism/examples; \
	fi
	mv cudd prism
	mv prism "prism-$(VERSION)-src"
	(cd "prism-$(VERSION)-src"; $(MAKE) dist_src VERSION=$(VERSION))
	tar cfz "prism-$(VERSION)-src.tar.gz" --exclude=.svn "prism-$(VERSION)-src"

# Get svn revision (only works if done before dist_src)
add_rev:
	(cd "prism"; $(MAKE) add_rev)

# Display version
version:
	@echo VERSION = $(VERSION)

#################################################
