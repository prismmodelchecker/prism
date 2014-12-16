#!/bin/sh

# Installation of a compiled PRISM distribution
# All this does is edit the "PRISM_DIR=..." lines in the program startup scripts
# so if you have any problems, just do this manually
# NB: This installation script should be run from within the PRISM directory

# You are supposed to run this from the main PRISM directory
# but in case someone is in the bin directory, change...
PRISM_DIR=`pwd`
if [ `basename "$PRISM_DIR"` = bin ]; then
  PRISM_DIR=`cd ..;pwd`
fi

# Now start the 'installation'
if [ ! "$1" = "silent" ] ; then
    echo "Installing PRISM (directory=$PRISM_DIR)"
fi
TEMP_FILE=tmp
FILES_TO_CHANGE=`find bin -maxdepth 1 ! -type d ! -name '*.bat'`
for FILE_TO_CHANGE in $FILES_TO_CHANGE
do
  if [ -f "$PRISM_DIR"/$FILE_TO_CHANGE ]; then
    if [ ! "$1" = "silent" ] ; then
        echo "Setting path in startup script $PRISM_DIR/$FILE_TO_CHANGE..."
    fi
    if sed -e "s|PRISM_DIR=.*|PRISM_DIR=\"$PRISM_DIR\"|g" "$PRISM_DIR"/$FILE_TO_CHANGE > "$PRISM_DIR"/$TEMP_FILE ; then
      /bin/mv "$PRISM_DIR"/$TEMP_FILE "$PRISM_DIR"/$FILE_TO_CHANGE
      chmod 755 "$PRISM_DIR"/$FILE_TO_CHANGE
    else
      echo "Error: Failed to modify startup scripts."
      exit 0
    fi
  else
    echo "Error: Could not locate startup script $PRISM_DIR/$FILE_TO_CHANGE"
    exit
  fi
done
if [ ! "$1" = "silent" ] ; then
    echo "Installation complete."
fi
