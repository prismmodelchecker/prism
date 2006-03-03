#!/bin/csh

# genhtml >! images.html

echo '<html><head><title>Images</title></head>'
echo '<body text="#000000" bgcolor="#ffffff">'

#set FILES = `/bin/ls *.gif *.jpg`
set GIF_FILES = `find . -name '*.gif'`
set JPG_FILES = `find . -name '*.jpg'`

foreach file ($GIF_FILES $JPG_FILES)

	echo $file':'
	echo '<img src="'$file'">'

end

echo '</body></html>'
