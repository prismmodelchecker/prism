#!/bin/sh

# Print the version of PRISM in this directory,
# based on extraction of the info from Java files.
# Mirrors what is done (in Java) in prism.getVersion().

# If given, the first argument to the script is assumed
# to point to the "src" directory in a PRISM source
# distribution. The default is just "src".

PRISM_SRC_DIR="${1:-src}"

if [ -f "$PRISM_SRC_DIR"/prism/Version.java ]; then
	VERSION_NUM=`grep versionString "$PRISM_SRC_DIR"/prism/Version.java | sed -E 's/[^"]+"([^"]+)"[^"]+/\1/'`
	if [ "$VERSION_NUM" != "" ]; then
		/bin/echo -n $VERSION_NUM

		VERSION_SUFFIX=`grep versionSuffixString "$PRISM_SRC_DIR"/prism/Version.java | sed -E 's/[^"]+"([^"]*)"[^"]+/\1/'`
		if [ "$VERSION_SUFFIX" != "" ]; then
			/bin/echo -n ".$VERSION_SUFFIX"
	
			if [ -f "$PRISM_SRC_DIR"/prism/Revision.java ]; then
				REVISION_SVN=`grep svnRevision "$PRISM_SRC_DIR"/prism/Revision.java | sed -E 's/[^"]+"([^"]*)"[^"]+/\1/'`
				if [ "$REVISION_SVN" != "" ]; then
					/bin/echo -n ".r$REVISION_SVN"
				fi
			fi
		fi
		/bin/echo
	else
		/bin/echo unknown
	fi
else
	/bin/echo unknown
fi
