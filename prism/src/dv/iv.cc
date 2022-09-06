//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
//	Description: C++ code for double vector stuff
//	
//------------------------------------------------------------------------------
//	
//	This file is part of PRISM.
//	
//	PRISM is free software; you can redistribute it and/or modify
//	it under the terms of the GNU General Public License as published by
//	the Free Software Foundation; either version 2 of the License, or
//	(at your option) any later version.
//	
//	PRISM is distributed in the hope that it will be useful,
//	but WITHOUT ANY WARRANTY; without even the implied warranty of
//	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//	GNU General Public License for more details.
//	
//	You should have received a copy of the GNU General Public License
//	along with PRISM; if not, write to the Free Software Foundation,
//	Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//	
//==============================================================================

#include "iv.h"
#include <cmath>
#include <new>
#include <cstdint>

// local function prototypes

static void mtbdd_to_integer_vector_rec(DdManager *ddman, DdNode *dd, DdNode **vars, int num_vars, int level, ODDNode *odd, int64_t o, int64_t n, int *res);
static DdNode *integer_vector_to_mtbdd_rec(DdManager *ddman, int *vec, DdNode **vars, int num_vars, int level, ODDNode *odd, int64_t o);

//------------------------------------------------------------------------------

// converts an mtbdd representing a vector to an array of integers
// values are cast from double to integer when extracting from the mtbdd
// it is optional whether or not an array is passed in to be used
// if so, it should be the right size (as determined by row->eoff+row->toff)
// if not, a new one is created
// in either the case, a pointer to the array is returned
// throws std::bad_alloc on out-of-memory

EXPORT int *mtbdd_to_integer_vector(DdManager *ddman, DdNode *dd, DdNode **vars, int num_vars, ODDNode *odd)
{
	return mtbdd_to_integer_vector(ddman, dd, vars, num_vars, odd, NULL);
}

EXPORT int *mtbdd_to_integer_vector(DdManager *ddman, DdNode *dd, DdNode **vars, int num_vars, ODDNode *odd, int *res)
{
	int64_t i, n;

	// determine size
	n = odd->eoff + odd->toff;
	// create array (if not supplied)
	if (!res) res = new int[n];
	// initialise to zero
	for (i = 0; i < n; i++) {
		res[i] = 0.0;
	}
	// build array recursively
	mtbdd_to_integer_vector_rec(ddman, dd, vars, num_vars, 0, odd, 0, n, res);
	
	return res;
}

void mtbdd_to_integer_vector_rec(DdManager *ddman, DdNode *dd, DdNode **vars, int num_vars, int level, ODDNode *odd, int64_t o, int64_t n, int *res)
{
	DdNode *e, *t;

	if (dd == Cudd_ReadZero(ddman)) return;

	if (level == num_vars) {
		if (o < 0 || o >= n) {
			printf("Internal error: Can not convert MTBDD to integer vector: Value out of range of the ODD (does the MTBDD encode non-reachable states?)\n");
			exit(1);
		}
		res[o] = (int) Cudd_V(dd);
		return;
	}
	else if (dd->index > vars[level]->index) {
		e = t = dd;
	}
	else {
		e = Cudd_E(dd);
		t = Cudd_T(dd);
	}

	mtbdd_to_integer_vector_rec(ddman, e, vars, num_vars, level+1, odd->e, o, n, res);
	mtbdd_to_integer_vector_rec(ddman, t, vars, num_vars, level+1, odd->t, o+odd->eoff, n, res);
}

//------------------------------------------------------------------------------

// converts an array of integers to an mtbdd

EXPORT DdNode *integer_vector_to_mtbdd(DdManager *ddman, int *vec, DdNode **vars, int num_vars, ODDNode *odd)
{
	return integer_vector_to_mtbdd_rec(ddman, vec, vars, num_vars, 0, odd, 0);
}

DdNode *integer_vector_to_mtbdd_rec(DdManager *ddman, int *vec, DdNode **vars, int num_vars, int level, ODDNode *odd, int64_t o)
{
	DdNode *e, *t;

	if (level == num_vars) {
		return DD_Constant(ddman, vec[o]);
	}
	else {
		if (odd->eoff > 0) {
			e = integer_vector_to_mtbdd_rec(ddman, vec, vars, num_vars, level+1, odd->e, o);
		}
		else {
			e = DD_Constant(ddman, 0);
		}
		if (odd->toff > 0) {
			t = integer_vector_to_mtbdd_rec(ddman, vec, vars, num_vars, level+1, odd->t, o+odd->eoff);
		}
		else {
			t = DD_Constant(ddman, 0);
		}
		if (e == t) {
			Cudd_RecursiveDeref(ddman, t);
			return e;
		}
		else {
			Cudd_Ref(vars[level]);
			return DD_ITE(ddman, vars[level], t, e);
		}
	}
}	

//------------------------------------------------------------------------------

