//==============================================================================
//	
//	File:		dv.cc
//	Date:		02/04/01
//	Author:		Dave Parker
//	Desc:		C++ code for double vector stuff
//	
//------------------------------------------------------------------------------
//	
//	Copyright (c) 2002-2004, Dave Parker
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

#include "dv.h"
#include <math.h>

// local function prototypes

static void mtbdd_to_double_vector_rec(DdManager *ddman, DdNode *dd, DdNode **vars, int num_vars, int level, ODDNode *odd, long o, double *res);
static DdNode *double_vector_to_mtbdd_rec(DdManager *ddman, double *vec, DdNode **vars, int num_vars, int level, ODDNode *odd, long o);
static DdNode *double_vector_to_bdd_rec(DdManager *ddman, double *vec, int rel_op, double bound1, double bound2, DdNode **vars, int num_vars, int level, ODDNode *odd, long o);
static double filter_double_vector_rec(DdManager *ddman, double *vec, DdNode *filter, DdNode **vars, int num_vars, int level, ODDNode *odd, long o);
static double get_first_from_bdd_rec(DdManager *ddman, double *vec, DdNode *filter, DdNode **vars, int num_vars, int level, ODDNode *odd, long o);
static double min_double_vector_over_bdd_rec(DdManager *ddman, double *vec, DdNode *filter, DdNode **vars, int num_vars, int level, ODDNode *odd, long o);
static double max_double_vector_over_bdd_rec(DdManager *ddman, double *vec, DdNode *filter, DdNode **vars, int num_vars, int level, ODDNode *odd, long o);
static double sum_double_vector_over_bdd_rec(DdManager *ddman, double *vec, DdNode *filter, DdNode **vars, int num_vars, int level, ODDNode *odd, long o);
static double sum_double_vector_over_mtbdd_rec(DdManager *ddman, double *vec, DdNode *mult, DdNode **vars, int num_vars, int level, ODDNode *odd, long o);

//------------------------------------------------------------------------------

// converts an mtbdd representing a vector to an array of doubles
// it is optional whether or not an array is passed in to be used
// if so, it should be the right size (as determined by row->eoff+row->toff)
// if not, a new one is created
// in either the case, a pointer to the array is returned

EXPORT double *mtbdd_to_double_vector(DdManager *ddman, DdNode *dd, DdNode **vars, int num_vars, ODDNode *odd)
{
	return mtbdd_to_double_vector(ddman, dd, vars, num_vars, odd, NULL);
}

EXPORT double *mtbdd_to_double_vector(DdManager *ddman, DdNode *dd, DdNode **vars, int num_vars, ODDNode *odd, double *res)
{
	int i, n;
	
	// determine size
	n = odd->eoff + odd->toff;
	// create array (if not supplied)
	if (!res) res = new double[n];
	// initialise to zero
	for (i = 0; i < n; i++) {
		res[i] = 0.0;
	}
	// build array recursively
	mtbdd_to_double_vector_rec(ddman, dd, vars, num_vars, 0, odd, 0, res);
	
	return res;
}

void mtbdd_to_double_vector_rec(DdManager *ddman, DdNode *dd, DdNode **vars, int num_vars, int level, ODDNode *odd, long o, double *res)
{
	DdNode *e, *t;

	if (dd == Cudd_ReadZero(ddman)) return;

	if (level == num_vars) {
		res[o] = Cudd_V(dd);
		return;
	}
	else if (dd->index > vars[level]->index) {
		e = t = dd;
	}
	else {
		e = Cudd_E(dd);
		t = Cudd_T(dd);
	}
	
	mtbdd_to_double_vector_rec(ddman, e, vars, num_vars, level+1, odd->e, o, res);
	mtbdd_to_double_vector_rec(ddman, t, vars, num_vars, level+1, odd->t, o+odd->eoff, res);
}	

//------------------------------------------------------------------------------

// converts an array of doubles to an mtbdd

EXPORT DdNode *double_vector_to_mtbdd(DdManager *ddman, double *vec, DdNode **vars, int num_vars, ODDNode *odd)
{
	return double_vector_to_mtbdd_rec(ddman, vec, vars, num_vars, 0, odd, 0);
}

DdNode *double_vector_to_mtbdd_rec(DdManager *ddman, double *vec, DdNode **vars, int num_vars, int level, ODDNode *odd, long o)
{
	DdNode *e, *t;

	if (level == num_vars) {
		return DD_Constant(ddman, vec[o]);
	}
	else {
		if (odd->eoff > 0) {
			e = double_vector_to_mtbdd_rec(ddman, vec, vars, num_vars, level+1, odd->e, o);
		}
		else {
			e = DD_Constant(ddman, 0);
		}
		if (odd->toff > 0) {
			t = double_vector_to_mtbdd_rec(ddman, vec, vars, num_vars, level+1, odd->t, o+odd->eoff);
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

// converts an array of doubles to a bdd using a relational operator and one or more bounds

EXPORT DdNode *double_vector_to_bdd(DdManager *ddman, double *vec, int rel_op, double bound, DdNode **vars, int num_vars, ODDNode *odd)
{
	return double_vector_to_bdd(ddman, vec, rel_op, bound, 0, vars, num_vars, odd);
}

EXPORT DdNode *double_vector_to_bdd(DdManager *ddman, double *vec, int rel_op, double bound1, double bound2, DdNode **vars, int num_vars, ODDNode *odd)
{
	return double_vector_to_bdd_rec(ddman, vec, rel_op, bound1, bound2, vars, num_vars, 0, odd, 0);
}

DdNode *double_vector_to_bdd_rec(DdManager *ddman, double *vec, int rel_op, double bound1, double bound2, DdNode **vars, int num_vars, int level, ODDNode *odd, long o)
{
	DdNode *e, *t;

	if (level == num_vars) {
		switch (rel_op) {
		case DV_GREATER_THAN_EQUALS: return (vec[o] >= bound1) ? DD_Constant(ddman, 1) : DD_Constant(ddman, 0); break;
		case DV_GREATER_THAN: return (vec[o] > bound1) ? DD_Constant(ddman, 1) : DD_Constant(ddman, 0); break;
		case DV_LESS_THAN_EQUALS: return (vec[o] <= bound1) ? DD_Constant(ddman, 1) : DD_Constant(ddman, 0); break;
		case DV_LESS_THAN: return (vec[o] < bound1) ? DD_Constant(ddman, 1) : DD_Constant(ddman, 0); break;
		case DV_INTERVAL: return (vec[o] >= bound1 && vec[o] <= bound2) ? DD_Constant(ddman, 1) : DD_Constant(ddman, 0); break;
		}
	}
	else {
		if (odd->eoff > 0) {
			e = double_vector_to_bdd_rec(ddman, vec, rel_op, bound1, bound2, vars, num_vars, level+1, odd->e, o);
		}
		else {
			e = DD_Constant(ddman, 0);
		}
		if (odd->toff > 0) {
			t = double_vector_to_bdd_rec(ddman, vec, rel_op, bound1, bound2, vars, num_vars, level+1, odd->t, o+odd->eoff);
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

// filter vector using a bdd (set elements not in filter to 0)

EXPORT void filter_double_vector(DdManager *ddman, double *vec, DdNode *filter, DdNode **vars, int num_vars, ODDNode *odd)
{
	filter_double_vector_rec(ddman, vec, filter, vars, num_vars, 0, odd, 0);
}

double filter_double_vector_rec(DdManager *ddman, double *vec, DdNode *filter, DdNode **vars, int num_vars, int level, ODDNode *odd, long o)
{
	DdNode *dd;
	
	if (level == num_vars) {
		if (Cudd_V(filter) == 0) {
			vec[o] = 0;
		}
	}
	else {
		if (odd->eoff > 0) {
			dd = (filter->index > vars[level]->index) ? filter : Cudd_E(filter);
			filter_double_vector_rec(ddman, vec, dd, vars, num_vars, level+1, odd->e, o);
		}
		if (odd->toff > 0) {
			dd = (filter->index > vars[level]->index) ? filter : Cudd_T(filter);
			filter_double_vector_rec(ddman, vec, dd, vars, num_vars, level+1, odd->t, o+odd->eoff);
		}
	}
}	

//------------------------------------------------------------------------------

// get value of first element in BDD filter

EXPORT double get_first_from_bdd(DdManager *ddman, double *vec, DdNode *filter, DdNode **vars, int num_vars, ODDNode *odd)
{
	// should never be called with an empty filter - we trap this case and return -1
	if (filter == Cudd_ReadZero(ddman)) return -1;
	else return get_first_from_bdd_rec(ddman, vec, filter, vars, num_vars, 0, odd, 0);
}

double get_first_from_bdd_rec(DdManager *ddman, double *vec, DdNode *filter, DdNode **vars, int num_vars, int level, ODDNode *odd, long o)
{
	DdNode *dd;
	
	if (level == num_vars) {
		return vec[o];
	}
	else {
		// go down filter along first non-zero path
		dd = (filter->index > vars[level]->index) ? filter : Cudd_E(filter);
		if (dd != Cudd_ReadZero(ddman)) {
			return get_first_from_bdd_rec(ddman, vec, dd, vars, num_vars, level+1, odd->e, o);
		}
		else {
			dd = (filter->index > vars[level]->index) ? filter : Cudd_T(filter);
			return get_first_from_bdd_rec(ddman, vec, dd, vars, num_vars, level+1, odd->t, o+odd->eoff);
		}
	}
}	

//------------------------------------------------------------------------------

// compute the minimum value of those in the bdd passed in

EXPORT double min_double_vector_over_bdd(DdManager *ddman, double *vec, DdNode *filter, DdNode **vars, int num_vars, ODDNode *odd)
{
	return min_double_vector_over_bdd_rec(ddman, vec, filter, vars, num_vars, 0, odd, 0);
}

double min_double_vector_over_bdd_rec(DdManager *ddman, double *vec, DdNode *filter, DdNode **vars, int num_vars, int level, ODDNode *odd, long o)
{
	double d1, d2;
	DdNode *dd;
	
	if (level == num_vars) {
		if (Cudd_V(filter) > 0) {
			return vec[o];
		}
		else {
			return HUGE_VAL;
		}
	}
	else {
		d1 = d2 = HUGE_VAL;
		if (odd->eoff > 0) {
			dd = (filter->index > vars[level]->index) ? filter : Cudd_E(filter);
			d1 = min_double_vector_over_bdd_rec(ddman, vec, dd, vars, num_vars, level+1, odd->e, o);
		}
		if (odd->toff > 0) {
			dd = (filter->index > vars[level]->index) ? filter : Cudd_T(filter);
			d2 = min_double_vector_over_bdd_rec(ddman, vec, dd, vars, num_vars, level+1, odd->t, o+odd->eoff);
		}
		return (d1 < d2) ? d1 : d2;
	}
}

//------------------------------------------------------------------------------

// compute the maximum value of those in the bdd passed in

EXPORT double max_double_vector_over_bdd(DdManager *ddman, double *vec, DdNode *filter, DdNode **vars, int num_vars, ODDNode *odd)
{
	return max_double_vector_over_bdd_rec(ddman, vec, filter, vars, num_vars, 0, odd, 0);
}

double max_double_vector_over_bdd_rec(DdManager *ddman, double *vec, DdNode *filter, DdNode **vars, int num_vars, int level, ODDNode *odd, long o)
{
	double d1, d2;
	DdNode *dd;
	
	if (level == num_vars) {
		if (Cudd_V(filter) > 0) {
			return vec[o];
		}
		else {
			return -HUGE_VAL;
		}
	}
	else {
		d1 = d2 = -HUGE_VAL;
		if (odd->eoff > 0) {
			dd = (filter->index > vars[level]->index) ? filter : Cudd_E(filter);
			d1 = max_double_vector_over_bdd_rec(ddman, vec, dd, vars, num_vars, level+1, odd->e, o);
		}
		if (odd->toff > 0) {
			dd = (filter->index > vars[level]->index) ? filter : Cudd_T(filter);
			d2 = max_double_vector_over_bdd_rec(ddman, vec, dd, vars, num_vars, level+1, odd->t, o+odd->eoff);
		}
		return (d1 > d2) ? d1 : d2;
	}
}

//------------------------------------------------------------------------------

// sums up the elements of a double array - but only those in the bdd passed in

EXPORT double sum_double_vector_over_bdd(DdManager *ddman, double *vec, DdNode *filter, DdNode **vars, int num_vars, ODDNode *odd)
{
	return sum_double_vector_over_bdd_rec(ddman, vec, filter, vars, num_vars, 0, odd, 0);
}

double sum_double_vector_over_bdd_rec(DdManager *ddman, double *vec, DdNode *filter, DdNode **vars, int num_vars, int level, ODDNode *odd, long o)
{
	double d;
	DdNode *dd;
	
	if (level == num_vars) {
		if (Cudd_V(filter) > 0) {
			return vec[o];
		}
		else {
			return 0;
		}
	}
	else {
		d = 0;
		if (odd->eoff > 0) {
			dd = (filter->index > vars[level]->index) ? filter : Cudd_E(filter);
			d += sum_double_vector_over_bdd_rec(ddman, vec, dd, vars, num_vars, level+1, odd->e, o);
		}
		if (odd->toff > 0) {
			dd = (filter->index > vars[level]->index) ? filter : Cudd_T(filter);
			d += sum_double_vector_over_bdd_rec(ddman, vec, dd, vars, num_vars, level+1, odd->t, o+odd->eoff);
		}
		return d;
	}
}	

//------------------------------------------------------------------------------

// sums up the elements of a double array - multiplied by the corresponding values in the mtbdd passed in

EXPORT double sum_double_vector_over_mtbdd(DdManager *ddman, double *vec, DdNode *mult, DdNode **vars, int num_vars, ODDNode *odd)
{
	return sum_double_vector_over_mtbdd_rec(ddman, vec, mult, vars, num_vars, 0, odd, 0);
}

double sum_double_vector_over_mtbdd_rec(DdManager *ddman, double *vec, DdNode *mult, DdNode **vars, int num_vars, int level, ODDNode *odd, long o)
{
	double d;
	DdNode *dd;
	
	if (level == num_vars) {
		return Cudd_V(mult) * vec[o];
	}
	else {
		d = 0;
		if (odd->eoff > 0) {
			dd = (mult->index > vars[level]->index) ? mult : Cudd_E(mult);
			d += sum_double_vector_over_mtbdd_rec(ddman, vec, dd, vars, num_vars, level+1, odd->e, o);
		}
		if (odd->toff > 0) {
			dd = (mult->index > vars[level]->index) ? mult : Cudd_T(mult);
			d += sum_double_vector_over_mtbdd_rec(ddman, vec, dd, vars, num_vars, level+1, odd->t, o+odd->eoff);
		}
		return d;
	}
}	

//-----------------------------------------------------------------------------

// Converts an array of doubles to a a DistVector, which stores only one copy of each distinct double
// and an array of short pointers to them. Fails and returns NULL if there are more distinct values
// than can be indexed by a short or if we run out of memory.

EXPORT DistVector *double_vector_to_dist(double *v, int n)
{
	double *buffer, *tmp;
	int i, j, num_dist, buffer_size, buffer_inc;
	unsigned int max_size;
	unsigned short s, *ptrs;
	DistVector *dv;
	
	// create buffer to store distinct values
	buffer_inc = 256;
	buffer_size = 256;
	max_size = 1 << (8*sizeof(short));
	buffer = (double *)calloc(buffer_size, sizeof(double));
	if (!buffer) { return NULL; }
	ptrs = (unsigned short*)malloc(n * sizeof(unsigned short));
	if (!ptrs) { free(buffer); return NULL; }
	
	num_dist = 0;
	// go thru vector
	for (i = 0; i < n; i++) {
		// see if we have this distinct val already
		for (s = 0; s < num_dist; s++)
			if (buffer[s] == v[i]) break;
		// add a new val if necessary...
		if (s == num_dist) {
			// ...increasing buffer size if required...
			if (num_dist >= buffer_size) {
				// ...and bailing out if there are too many
				if (buffer_size+buffer_inc > max_size) {
					free(buffer);
					free(ptrs);
					return NULL;
				}
				tmp = (double *)calloc(buffer_size+buffer_inc, sizeof(double));
				if (!tmp) { free(buffer); free(ptrs); return NULL; }
				for (j = 0; j < buffer_size; j++) {
					tmp[j] = buffer[j];
				}
				free(buffer);
				buffer = tmp;
				buffer_size += buffer_inc;
			}
			// add val
			buffer[s] = v[i];
			num_dist++;
		}
		ptrs[i] = s;
	}
	
	// create data str to store result
	dv = (DistVector *)calloc(1, sizeof(DistVector));
	if (!dv) { free(buffer); free(ptrs); return NULL; }
	dv->dist = buffer;
	dv->num_dist = num_dist;
	dv->ptrs = ptrs;
	
	return dv;
}

//-----------------------------------------------------------------------------------

// free distinct vector struct

EXPORT void free_dist_vector(DistVector *&dv)
{
	free(dv->dist);
	free(dv->ptrs);
	free(dv);
	dv = NULL;
}

//------------------------------------------------------------------------------

// delete double array, distinct vector struct, or both

EXPORT void free_dv_or_dist_vector(double *&v, DistVector *&dv)
{
	if (v) { free(v); v = NULL; }
	if (dv) { free_dist_vector(dv); dv = NULL; }
}

//------------------------------------------------------------------------------
