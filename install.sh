#!/bin/sh

# Installation of a compiled PRISM distribution
# All this does is edit the "PRISM_DIR=..." lines in the program startup scripts
# so if you have any problems, just do this manually
# NB: This installation script should be run from within the PRISM directory

# You are supposed to run this from the main PRISM directory
# but in case someone is in the bin directory, change...
PRISM_DIR=`pwd`
if [ `basename $PRISM_DIR` = bin ]; then
  PRISM_DIR=`cd ..;pwd`
fi

# Now start the 'installation'
echo "Installing PRISM (directory=$PRISM_DIR)"
TEMP_FILE=tmp
for FILE_TO_CHANGE in bin/prism bin/xprism
do
  if [ -f $PRISM_DIR/$FILE_TO_CHANGE ]; then
    echo "Modifying script $PRISM_DIR/$FILE_TO_CHANGE..."
    if sed -e "s|PRISM_DIR=.*|PRISM_DIR=$PRISM_DIR|g" $PRISM_DIR/$FILE_TO_CHANGE > $PRISM_DIR/$TEMP_FILE ; then
      /bin/mv $PRISM_DIR/$TEMP_FILE $PRISM_DIR/$FILE_TO_CHANGE
      chmod 755 $PRISM_DIR/$FILE_TO_CHANGE
    else
      echo "Installation failed."
      exit 0
    fi
  else
    echo "Error: Could not locate script $PRISM_DIR/$FILE_TO_CHANGE"
    exit
  fi
done
echo "Installation complete."
