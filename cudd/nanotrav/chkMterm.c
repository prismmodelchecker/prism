/**
  @file

  @ingroup nanotrav

  @brief Functions to check that the minterm counts have not changed
  during reordering.

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

#include "ntr.h"

/*---------------------------------------------------------------------------*/
/* Constant declarations                                                     */
/*---------------------------------------------------------------------------*/

/*---------------------------------------------------------------------------*/
/* Stucture declarations                                                     */
/*---------------------------------------------------------------------------*/

/*---------------------------------------------------------------------------*/
/* Type declarations                                                         */
/*---------------------------------------------------------------------------*/

/*---------------------------------------------------------------------------*/
/* Variable declarations                                                     */
/*---------------------------------------------------------------------------*/


/*---------------------------------------------------------------------------*/
/* Macro declarations                                                        */
/*---------------------------------------------------------------------------*/

/** \cond */

/*---------------------------------------------------------------------------*/
/* Static function prototypes                                                */
/*---------------------------------------------------------------------------*/

static enum st_retval stFree (void *key, void *value, void *arg);

/** \endcond */


/*---------------------------------------------------------------------------*/
/* Definition of exported functions                                          */
/*---------------------------------------------------------------------------*/

/*---------------------------------------------------------------------------*/
/* Definition of internal functions                                          */
/*---------------------------------------------------------------------------*/


/**
  @brief Check that minterm counts have not changed.

  @details Counts the minterms in the global functions of the
  primary outputs of the network passed as argument.
  When it is calld with the second argument set to NULL, it allocates
  a symbol table and stores, for each output, the minterm count. If
  an output does not have a %BDD, it stores a NULL pointer for it.
  If it is called with a non-null second argument, it assumes that
  the symbol table contains the minterm counts measured previously
  and it compares the new counts to the old ones. Finally, it frees
  the symbol table.
  check_minterms is designed so that it can be called twice: once before
  reordering, and once after reordering.

  @return a pointer to the symbol table on the first invocation and
  NULL on the second invocation.

  @sideeffect None

*/
st_table *
checkMinterms(
  BnetNetwork * net,
  DdManager * dd,
  st_table * previous)
{
    BnetNode *po;
    int numPi;
    char *name;
    double *count, newcount, *oldcount;
    int flag,err,i;

    numPi = net->ninputs;

    if (previous == NULL) {
      previous = st_init_table((st_compare_t) strcmp, st_strhash);
	if (previous == NULL) {
	    (void) printf("checkMinterms out-of-memory\n");
	    return(NULL);
	}
	for (i = 0; i < net->noutputs; i++) {
	    if (!st_lookup(net->hash,net->outputs[i],(void **)&po)) {
		exit(2);
	    }
	    name = net->outputs[i];
	    if (po->dd != NULL) {
		count = ALLOC(double,1);
		*count = Cudd_CountMinterm(dd,po->dd,numPi);
		err = st_insert(previous, name, (void *) count);
	    } else {
		err = st_insert(previous, name, NULL);
	    }
	    if (err) {
		(void) printf("Duplicate input name (%s)\n",name);
		return(NULL);
	    }
	}
	return(previous);
    } else {
	flag = 0;
	if (st_count(previous) != net->noutputs) {
	    (void) printf("Number of outputs has changed from %d to %d\n",
	    st_count(previous), net->noutputs);
	    flag = 1;
	}
	for (i = 0; i < net->noutputs; i++) {
	    if (!st_lookup(net->hash,net->outputs[i],(void **)&po)) {
		exit(2);
	    }
	    name = net->outputs[i];
	    if (st_lookup(previous,name,(void **)&oldcount)) {
		if (po->dd != NULL) {
		    newcount = Cudd_CountMinterm(dd,po->dd,numPi);
		    if (newcount != *oldcount) {
			(void) printf("Number of minterms of %s has changed from %g to %g\n",name,*oldcount,newcount);
			flag = 1;
		    }
		} else {
		    if (oldcount != NULL) {
			(void) printf("Output %s lost its BDD!\n",name);
			flag = 1;
		    }
		}
	    } else {
		(void) printf("Output %s is new!\n",name);
		flag = 1;
	    }
	}
	/*st_foreach(previous,(enum st_retval)stFree,NULL);*/
	st_foreach(previous,stFree,NULL);
	st_free_table(previous);
	if (flag) {
	    return((st_table *) 1);
	} else {
	    return(NULL);
	}
    }

} /* end of checkMinterms */


/*---------------------------------------------------------------------------*/
/* Definition of static functions                                            */
/*---------------------------------------------------------------------------*/


/**
  @brief Frees the data of the symbol table.

  @sideeffect None

*/
static enum st_retval
stFree(
  void *key,
  void *value,
  void *arg)
{
    (void) key; /* avoid warning */
    (void) arg; /* avoid warning */
    if (value != NULL) {
	FREE(value);
    }
    return(ST_CONTINUE);

} /* end of stFree */
