/*
 *  tmpfile -- open an unnamed temporary file
 *
 *  This is the ANSI C standard routine; we have hacks here because many
 *  compilers/systems do not have it yet.
 */

/* LINTLIBRARY */


#include <stdio.h>
#include "util.h"


#ifdef UNIX

extern char *mktemp (char *);

FILE *
tmpfile()
{
    FILE *fp;
    char *filename, *junk;

    junk = strsav((char *)"/usr/tmp/misIIXXXXXX");
    filename = mktemp(junk);
    if ((fp = fopen(filename, "w+")) == NULL) {
	FREE(junk);
	return NULL;
    }
    (void) unlink(filename);
    FREE(junk);
    return fp;
}

#else

FILE *
tmpfile()
{
    return fopen("utiltmp", "w+");
}

#endif
