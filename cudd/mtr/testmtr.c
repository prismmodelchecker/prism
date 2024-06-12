/**
  @file

  @ingroup mtr

  @brief Test program for the mtr package.

  @author Fabio Somenzi
   
  @copyright@parblock
  Copyright (c) 1995-2015, Regents of the University of Colorado

  All rights reserved.

  Redistribution and use in source and binary forms, with or without
  modification, are permitted provided that the following conditions
  are met:

  Redistributions of source code must retain the above copyright
  notice, this list of conditions and the following disclaimer.

  Redistributions in binary form must reproduce the above copyright
  notice, this list of conditions and the following disclaimer in the
  documentation and/or other materials provided with the distribution.

  Neither the name of the University of Colorado nor the names of its
  contributors may be used to endorse or promote products derived from
  this software without specific prior written permission.

  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
  FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
  COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
  INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
  BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
  CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
  LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
  ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
  POSSIBILITY OF SUCH DAMAGE.
  @endparblock

*/

#include "util.h"
#include "mtrInt.h"

/*---------------------------------------------------------------------------*/
/* Variable declarations                                                     */
/*---------------------------------------------------------------------------*/

#ifndef lint
static char rcsid[] MTR_UNUSED = "$Id: testmtr.c,v 1.8 2015/07/01 20:43:45 fabio Exp $";
#endif

/*---------------------------------------------------------------------------*/
/* Constant declarations                                                     */
/*---------------------------------------------------------------------------*/

#define TESTMTR_VERSION\
    "TestMtr Version #0.6, Release date 2/6/12"

/** \cond */


/*---------------------------------------------------------------------------*/
/* Static function prototypes                                                */
/*---------------------------------------------------------------------------*/

static void usage (char *prog);
static FILE * open_file (const char *filename, const char *mode);
static void printHeader(int argc, char **argv);

/** \endcond */


/*---------------------------------------------------------------------------*/
/* Definition of exported functions                                          */
/*---------------------------------------------------------------------------*/

/**
  @brief Main program for testmtr.

  @details Performs initialization.  Reads command line options and
  network(s).  Builds some simple trees and prints them out.

  @sideeffect None

*/
int
main(
  int  argc,
  char ** argv)
{
    MtrNode *root,
	    *node;
    int	    i,
	    pr = 0;
    FILE    *fp;
    const char *file = NULL;

    for (i = 1; i < argc; i++) {
      if (strcmp("-M", argv[i]) == 0) {
        continue;
      } else if (strcmp("-p", argv[i]) == 0) {
        pr = atoi(argv[++i]);
      } else if (strcmp("-h", argv[i]) == 0) {
        printHeader(argc, argv);
        usage(argv[0]);
      } else if (i == argc - 1) {
        file = argv[i];
      } else {
        printHeader(argc, argv);
        usage(argv[0]);
      }
    }
    if (file == NULL) {
      file = "-";
    }
    if (pr > 0)
        printHeader(argc, argv);

    /* Create and print a simple tree. */
    root = Mtr_InitTree();
    root->flags = 0;
    node = Mtr_CreateFirstChild(root);
    node->flags = 1;
    node = Mtr_CreateLastChild(root);
    node->flags = 2;
    node = Mtr_CreateFirstChild(root);
    node->flags = 3;
    node = Mtr_AllocNode();
    node->child = NULL;
    node->flags = 4;
    Mtr_MakeNextSibling(root->child,node);
    if (pr > 0) {
        Mtr_PrintTree(root);
        (void) printf("#------------------------\n");
    }
    Mtr_FreeTree(root);

    /* Create an initial tree in which all variables belong to one group. */
    root = Mtr_InitGroupTree(0,12);
    if (pr > 0) {
        Mtr_PrintTree(root); (void) printf("#  ");
        Mtr_PrintGroups(root,pr == 0); (void) printf("\n");
    }
    (void) Mtr_MakeGroup(root,0,6,MTR_DEFAULT);
    (void) Mtr_MakeGroup(root,6,6,MTR_DEFAULT);
    if (pr > 0) {
        Mtr_PrintTree(root); (void) printf("#  ");
        Mtr_PrintGroups(root,pr == 0); (void) printf("\n");
    }
    for (i = 0; i < 6; i+=2) {
      (void) Mtr_MakeGroup(root,(unsigned) i,(unsigned) 2,MTR_DEFAULT);
    }
    (void) Mtr_MakeGroup(root,0,12,MTR_FIXED);
    if (pr > 0) {
        Mtr_PrintTree(root); (void) printf("#  ");
        Mtr_PrintGroups(root,pr == 0); (void) printf("\n");
        /* Print a partial tree. */
        (void) printf("#  ");
        Mtr_PrintGroups(root->child,pr == 0); (void) printf("\n");
    }
    node = Mtr_FindGroup(root,0,6);
    (void) Mtr_DissolveGroup(node);
    if (pr > 0) {
        Mtr_PrintTree(root); (void) printf("#  ");
        Mtr_PrintGroups(root,pr == 0); (void) printf("\n");
    }
    node = Mtr_FindGroup(root,4,2);
    if (!Mtr_SwapGroups(node,node->younger)) {
	(void) printf("error in Mtr_SwapGroups\n");
	return 3;
    }
    if (pr > 0) {
        Mtr_PrintTree(root); (void) printf("#  ");
        Mtr_PrintGroups(root,pr == 0);
        (void) printf("#------------------------\n");
    }
    Mtr_FreeTree(root);

    /* Create a group tree with fixed subgroups. */
    root = Mtr_InitGroupTree(0,4);
    if (pr > 0) {
        Mtr_PrintTree(root); (void) printf("#  ");
        Mtr_PrintGroups(root,pr == 0); (void) printf("\n");
    }
    (void) Mtr_MakeGroup(root,0,2,MTR_FIXED);
    (void) Mtr_MakeGroup(root,2,2,MTR_FIXED);
    if (pr > 0) {
        Mtr_PrintTree(root); (void) printf("#  ");
        Mtr_PrintGroups(root,pr == 0); (void) printf("\n");
    }
    Mtr_FreeTree(root);
    if (pr > 0) {
        (void) printf("#------------------------\n");
    }

    /* Open input file. */
    fp = open_file(file, "r");
    root = Mtr_ReadGroups(fp,12);
    fclose(fp);
    if (pr > 0) {
        if (root) {
            Mtr_PrintTree(root); (void) printf("#  ");
            Mtr_PrintGroups(root,pr == 0); (void) printf("\n");
        } else {
            (void) printf("error in group file\n");
        }
    }
    Mtr_FreeTree(root);

    return 0;

} /* end of main */


/**
  @brief Prints usage message and exits.

  @sideeffect none

*/
static void
usage(
  char * prog)
{
    (void) fprintf(stderr, "usage: %s [options] [file]\n", prog);
    (void) fprintf(stderr, "   -M\t\tturns off memory allocation recording\n");
    (void) fprintf(stderr, "   -h\t\tprints this message\n");
    (void) fprintf(stderr, "   -p n\t\tcontrols verbosity\n");
    exit(2);

} /* end of usage */


/**
  @brief Opens a file.

  @details Opens a file, or fails with an error message and exits.
  Allows '-' as a synonym for standard input.

  @sideeffect None

*/
static FILE *
open_file(
  const char * filename,
  const char * mode)
{
    FILE *fp;

    if (strcmp(filename, "-") == 0) {
        return mode[0] == 'r' ? stdin : stdout;
    } else if ((fp = fopen(filename, mode)) == NULL) {
        perror(filename);
        exit(1);
    }
    return(fp);

} /* end of open_file */


/**
  @brief Prints the header of the program output.

  @sideeffect None

*/
static void
printHeader(
  int argc,
  char **argv)
{
    int i;

    (void) printf("# %s\n", TESTMTR_VERSION);
    /* Echo command line and arguments. */
    (void) printf("#");
    for(i = 0; i < argc; i++) {
	(void) printf(" %s", argv[i]);
    }
    (void) printf("\n");
    (void) fflush(stdout);
}
