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

#include "dv.h"
#include <cmath>
#include <new>
#include <cstdint>
#include <limits>

// local function prototypes

static void mtbdd_to_double_vector_rec(DdManager *ddman, DdNode *dd, DdNode **vars, int num_vars, int level, ODDNode *odd, int64_t o, int64_t n, double *res);
static DdNode *double_vector_to_mtbdd_rec(DdManager *ddman, double *vec, DdNode **vars, int num_vars, int level, ODDNode *odd, int64_t o);
static DdNode *double_vector_to_bdd_rec(DdManager *ddman, double *vec, int rel_op, double value1, double value2, DdNode **vars, int num_vars, int level, ODDNode *odd, int64_t o);
static void filter_double_vector_rec(DdManager *ddman, double *vec, DdNode *filter, double d, DdNode **vars, int num_vars, int level, ODDNode *odd, int64_t o);
static void max_double_vector_mtbdd_rec(DdManager *ddman, double *vec, DdNode *vec2, DdNode **vars, int num_vars, int level, ODDNode *odd, int64_t o);
static double get_first_from_bdd_rec(DdManager *ddman, double *vec, DdNode *filter, DdNode **vars, int num_vars, int level, ODDNode *odd, int64_t o);
static double min_double_vector_over_bdd_rec(DdManager *ddman, double *vec, DdNode *filter, DdNode **vars, int num_vars, int level, ODDNode *odd, int64_t o);
static double max_double_vector_over_bdd_rec(DdManager *ddman, double *vec, DdNode *filter, DdNode **vars, int num_vars, int level, ODDNode *odd, int64_t o);
static double max_finite_double_vector_over_bdd_rec(DdManager *ddman, double *vec, DdNode *filter, DdNode **vars, int num_vars, int level, ODDNode *odd, int64_t o);
static double sum_double_vector_over_bdd_rec(DdManager *ddman, double *vec, DdNode *filter, DdNode **vars, int num_vars, int level, ODDNode *odd, int64_t o);
static double sum_double_vector_over_mtbdd_rec(DdManager *ddman, double *vec, DdNode *mult, DdNode **vars, int num_vars, int level, ODDNode *odd, int64_t o);
static void sum_double_vector_over_dd_vars_rec(DdManager *ddman, double *vec, double *vec2, DdNode **vars, int num_vars, int level, int first_var, int last_var, ODDNode *odd, ODDNode *odd2, int64_t o, int64_t o2);

// Threshold for comparison of doubles
static double epsilon_double = 1e-12;

//------------------------------------------------------------------------------

// converts an mtbdd representing a vector to an array of doubles
// it is optional whether or not an array is passed in to be used
// if so, it should be the right size (as determined by row->eoff+row->toff)
// if not, a new one is created
// in either the case, a pointer to the array is returned
// throws std::bad_alloc on out-of-memory

EXPORT double *mtbdd_to_double_vector(DdManager *ddman, DdNode *dd, DdNode **vars, int num_vars, ODDNode *odd)
{
	return mtbdd_to_double_vector(ddman, dd, vars, num_vars, odd, NULL);
}

EXPORT double *mtbdd_to_double_vector(DdManager *ddman, DdNode *dd, DdNode **vars, int num_vars, ODDNode *odd, double *res)
{
	int64_t i, n;

	// determine size
	n = odd->eoff + odd->toff;
	// create array (if not supplied)
	if (!res) res = new double[n];
	// initialise to zero
	for (i = 0; i < n; i++) {
		res[i] = 0.0;
	}
	// build array recursively
	mtbdd_to_double_vector_rec(ddman, dd, vars, num_vars, 0, odd, 0, n, res);

	return res;
}

void mtbdd_to_double_vector_rec(DdManager *ddman, DdNode *dd, DdNode **vars, int num_vars, int level, ODDNode *odd, int64_t o, int64_t n, double *res)
{
	DdNode *e, *t;

	if (dd == Cudd_ReadZero(ddman)) return;

	if (level == num_vars) {
		if (o < 0 || o >= n) {
			printf("Internal error: Can not convert MTBDD to double vector: Value out of range of the ODD (does the MTBDD encode non-reachable states?)\n");
			exit(1);
		}
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

	mtbdd_to_double_vector_rec(ddman, e, vars, num_vars, level+1, odd->e, o, n, res);
	mtbdd_to_double_vector_rec(ddman, t, vars, num_vars, level+1, odd->t, o+odd->eoff, n, res);
}

//------------------------------------------------------------------------------

// converts an array of doubles to an mtbdd

EXPORT DdNode *double_vector_to_mtbdd(DdManager *ddman, double *vec, DdNode **vars, int num_vars, ODDNode *odd)
{
	return double_vector_to_mtbdd_rec(ddman, vec, vars, num_vars, 0, odd, 0);
}

DdNode *double_vector_to_mtbdd_rec(DdManager *ddman, double *vec, DdNode **vars, int num_vars, int level, ODDNode *odd, int64_t o)
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

// Converts an array of doubles to a BDD using a relational operator and a value (or values)
// Options for rel_op:
// * DV_GREATER_THAN_EQUALS: >= value
// * DV_GREATER_THAN: > value
// * DV_LESS_THAN_EQUALS <= value
// * DV_LESS_THAN: < value
// * DV_INTERVAL: in [value1,value2]
// * DV_CLOSE_ABS: =value1 (with absolute error <= value2)
// * DV_CLOSE_REL: =value1 (with relative error <= value2)


EXPORT DdNode *double_vector_to_bdd(DdManager *ddman, double *vec, int rel_op, double value, DdNode **vars, int num_vars, ODDNode *odd)
{
	return double_vector_to_bdd(ddman, vec, rel_op, value, 0, vars, num_vars, odd);
}

EXPORT DdNode *double_vector_to_bdd(DdManager *ddman, double *vec, int rel_op, double value1, double value2, DdNode **vars, int num_vars, ODDNode *odd)
{
	return double_vector_to_bdd_rec(ddman, vec, rel_op, value1, value2, vars, num_vars, 0, odd, 0);
}

// Recursive call for double_vector_to_bdd methods

DdNode *double_vector_to_bdd_rec(DdManager *ddman, double *vec, int rel_op, double value1, double value2, DdNode **vars, int num_vars, int level, ODDNode *odd, int64_t o)
{
	DdNode *e, *t;

	if (level == num_vars) {
		switch (rel_op) {
		case DV_GREATER_THAN_EQUALS: return (vec[o] >= value1) ? DD_Constant(ddman, 1) : DD_Constant(ddman, 0); break;
		case DV_GREATER_THAN: return (vec[o] > value1) ? DD_Constant(ddman, 1) : DD_Constant(ddman, 0); break;
		case DV_LESS_THAN_EQUALS: return (vec[o] <= value1) ? DD_Constant(ddman, 1) : DD_Constant(ddman, 0); break;
		case DV_LESS_THAN: return (vec[o] < value1) ? DD_Constant(ddman, 1) : DD_Constant(ddman, 0); break;
		case DV_INTERVAL: return (vec[o] >= value1 && vec[o] <= value2) ? DD_Constant(ddman, 1) : DD_Constant(ddman, 0); break;
		case DV_CLOSE_ABS: return doubles_are_close_abs(vec[o], value1, value2) ? DD_Constant(ddman, 1) : DD_Constant(ddman, 0); break;
		case DV_CLOSE_REL: return doubles_are_close_rel(vec[o], value1, value2) ? DD_Constant(ddman, 1) : DD_Constant(ddman, 0); break;
		}
	}
	else {
		if (odd->eoff > 0) {
			e = double_vector_to_bdd_rec(ddman, vec, rel_op, value1, value2, vars, num_vars, level+1, odd->e, o);
		}
		else {
			e = DD_Constant(ddman, 0);
		}
		if (odd->toff > 0) {
			t = double_vector_to_bdd_rec(ddman, vec, rel_op, value1, value2, vars, num_vars, level+1, odd->t, o+odd->eoff);
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
	// Never get here:
	return NULL;
}	

//------------------------------------------------------------------------------

// filter vector using a bdd (set elements not in filter to constant d)

EXPORT void filter_double_vector(DdManager *ddman, double *vec, DdNode *filter, double d, DdNode **vars, int num_vars, ODDNode *odd)
{
	filter_double_vector_rec(ddman, vec, filter, d, vars, num_vars, 0, odd, 0);
}

void filter_double_vector_rec(DdManager *ddman, double *vec, DdNode *filter, double d, DdNode **vars, int num_vars, int level, ODDNode *odd, int64_t o)
{
	DdNode *dd;
	
	if (level == num_vars) {
		if (Cudd_V(filter) == 0) {
			vec[o] = d;
		}
	}
	else {
		if (odd->eoff > 0) {
			dd = (filter->index > vars[level]->index) ? filter : Cudd_E(filter);
			filter_double_vector_rec(ddman, vec, dd, d, vars, num_vars, level+1, odd->e, o);
		}
		if (odd->toff > 0) {
			dd = (filter->index > vars[level]->index) ? filter : Cudd_T(filter);
			filter_double_vector_rec(ddman, vec, dd, d, vars, num_vars, level+1, odd->t, o+odd->eoff);
		}
	}
}

//------------------------------------------------------------------------------

// apply max operator, i.e. vec[i] = max(vec[i], vec2[i]), where vec2 is an mtbdd

EXPORT void max_double_vector_mtbdd(DdManager *ddman, double *vec, DdNode *vec2, DdNode **vars, int num_vars, ODDNode *odd)
{
	max_double_vector_mtbdd_rec(ddman, vec, vec2, vars, num_vars, 0, odd, 0);
}

void max_double_vector_mtbdd_rec(DdManager *ddman, double *vec, DdNode *filter, DdNode **vars, int num_vars, int level, ODDNode *odd, int64_t o)
{
	DdNode *dd;
	double d;
	
	if (level == num_vars) {
	
		d = Cudd_V(filter);
		if (d > vec[o]) {
			vec[o] = d;
		}
	}
	else {
		if (odd->eoff > 0) {
			dd = (filter->index > vars[level]->index) ? filter : Cudd_E(filter);
			max_double_vector_mtbdd_rec(ddman, vec, dd, vars, num_vars, level+1, odd->e, o);
		}
		if (odd->toff > 0) {
			dd = (filter->index > vars[level]->index) ? filter : Cudd_T(filter);
			max_double_vector_mtbdd_rec(ddman, vec, dd, vars, num_vars, level+1, odd->t, o+odd->eoff);
		}
	}
}	

//------------------------------------------------------------------------------

// get value of first element in BDD filter

EXPORT double get_first_from_bdd(DdManager *ddman, double *vec, DdNode *filter, DdNode **vars, int num_vars, ODDNode *odd)
{
	// This shouldn't really be called with an empty filter.
	// But we check for this anyway and return NaN.
	// This is unfortunately indistinguishable from the case
	// where the vector does actually contain NaN. Ho hum.
	if (filter == Cudd_ReadZero(ddman)) return NAN;
	// Recurse...
	else return get_first_from_bdd_rec(ddman, vec, filter, vars, num_vars, 0, odd, 0);
}

double get_first_from_bdd_rec(DdManager *ddman, double *vec, DdNode *filter, DdNode **vars, int num_vars, int level, ODDNode *odd, int64_t o)
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

double min_double_vector_over_bdd_rec(DdManager *ddman, double *vec, DdNode *filter, DdNode **vars, int num_vars, int level, ODDNode *odd, int64_t o)
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

double max_double_vector_over_bdd_rec(DdManager *ddman, double *vec, DdNode *filter, DdNode **vars, int num_vars, int level, ODDNode *odd, int64_t o)
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


EXPORT double max_finite_double_vector_over_bdd(DdManager *ddman, double *vec, DdNode *filter, DdNode **vars, int num_vars, ODDNode *odd)
{
	return max_finite_double_vector_over_bdd_rec(ddman, vec, filter, vars, num_vars, 0, odd, 0);
}

double max_finite_double_vector_over_bdd_rec(DdManager *ddman, double *vec, DdNode *filter, DdNode **vars, int num_vars, int level, ODDNode *odd, int64_t o)
{
	double d1, d2;
	DdNode *dd;

	if (level == num_vars) {
		if (Cudd_V(filter) > 0) {
			double v = vec[o];
			if (v < std::numeric_limits<double>::infinity()) {
				return -std::numeric_limits<double>::infinity();
			} else {
				return v;
			}
		}
		else {
			return -std::numeric_limits<double>::infinity();
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

double sum_double_vector_over_bdd_rec(DdManager *ddman, double *vec, DdNode *filter, DdNode **vars, int num_vars, int level, ODDNode *odd, int64_t o)
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

double sum_double_vector_over_mtbdd_rec(DdManager *ddman, double *vec, DdNode *mult, DdNode **vars, int num_vars, int level, ODDNode *odd, int64_t o)
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

//------------------------------------------------------------------------------

// sum up the elements of a double array, over a subset of its dd vars
// the dd var subset must be a continuous range of vars, identified by indices: first_var, last_var
// store the result in the vector vec2

EXPORT void sum_double_vector_over_dd_vars(DdManager *ddman, double *vec, double *vec2, DdNode **vars, int num_vars, int first_var, int last_var, ODDNode *odd, ODDNode *odd2)
{
	return sum_double_vector_over_dd_vars_rec(ddman, vec, vec2, vars, num_vars, 0, first_var, last_var, odd, odd2, 0, 0);
}

void sum_double_vector_over_dd_vars_rec(DdManager *ddman, double *vec, double *vec2, DdNode **vars, int num_vars, int level, int first_var, int last_var, ODDNode *odd, ODDNode *odd2, int64_t o, int64_t o2)
{
	if (level == num_vars) {
		vec2[o2] += vec[o];
	}
	else {
		if (odd->eoff > 0) {
			if (vars[level]->index >= first_var && vars[level]->index <= last_var) {
				sum_double_vector_over_dd_vars_rec(ddman, vec, vec2, vars, num_vars, level+1, first_var, last_var, odd->e, odd2, o, o2);
			} else {
				sum_double_vector_over_dd_vars_rec(ddman, vec, vec2, vars, num_vars, level+1, first_var, last_var, odd->e, odd2->e, o, o2);
			}
		}
		if (odd->toff > 0) {
			if (vars[level]->index >= first_var && vars[level]->index <= last_var) {
				sum_double_vector_over_dd_vars_rec(ddman, vec, vec2, vars, num_vars, level+1, first_var, last_var, odd->t, odd2, o+odd->eoff, o2);
			} else {
				sum_double_vector_over_dd_vars_rec(ddman, vec, vec2, vars, num_vars, level+1, first_var, last_var, odd->t, odd2->t, o+odd->eoff, o2+odd2->eoff);
			}
		}
	}
}	

//-----------------------------------------------------------------------------

// Converts an array of doubles to a a DistVector, which stores only one copy of each distinct double
// and an array of short pointers to them. Fails and returns NULL if there are more distinct values
// than can be indexed by a short or if we run out of memory.

EXPORT DistVector *double_vector_to_dist(double *v, int n)
{
	double *buffer = NULL, *tmp = NULL;
	int i, j, num_dist, buffer_size, buffer_inc;
	unsigned int max_size, s;
	unsigned short *ptrs = NULL;
	DistVector *dv = NULL;
	
	// try/catch for memory allocation/deallocation
	try {
	
	// create buffer to store distinct values
	buffer_inc = 256;
	buffer_size = 256;
	max_size = 1 << (8*sizeof(short));
	buffer = new double[buffer_size];
	ptrs = new unsigned short[n];
	
	num_dist = 0;
	// go thru vector
	for (i = 0; i < n; i++) {
		for (s = 0; s < num_dist; s++) {
			if (buffer[s] == v[i]) break;
		}
		// add a new val if necessary...
		if (s == num_dist) {
			// ...increasing buffer size if required...
			if (num_dist >= buffer_size) {
				// ...and bailing out if there are too many
				if (buffer_size+buffer_inc > max_size) {
					throw std::bad_alloc();
				}
				tmp = new double[buffer_size+buffer_inc];
				for (j = 0; j < buffer_size; j++) {
					tmp[j] = buffer[j];
				}
				delete[] buffer;
				buffer = tmp;
				tmp = NULL;
				buffer_size += buffer_inc;
			}
			// add val
			buffer[s] = v[i];
			num_dist++;
		}
		ptrs[i] = s;
	}
	
	// create data str to store result
	dv = new DistVector();
	dv->dist = buffer;
	dv->num_dist = num_dist;
	dv->ptrs = ptrs;
	
	// try/catch for memory allocation/deallocation
	} catch(std::bad_alloc e) {
		if (buffer) delete[] buffer;
		if (tmp) delete[] tmp;
		if (ptrs) delete[] ptrs;
		if (dv) delete dv;
		return NULL;
	}
	
	return dv;
}

//-----------------------------------------------------------------------------------

// free distinct vector struct

EXPORT DistVector::DistVector()
{
	dist = NULL;
	num_dist = 0;
	ptrs = NULL;
}

EXPORT DistVector::~DistVector()
{
	if (dist) delete[] dist;
	if (ptrs) delete[] ptrs;
}

//------------------------------------------------------------------------------

// Utililty methods for checking whether two doubles are close
// (based on code in prism.PrismUtils.java)

EXPORT bool doubles_are_close_abs(double d1, double d2, double epsilon)
{
	// Deal with infinite cases
	if (std::isinf(d1)) {
		return std::isinf(d2) && (d1 > 0) == (d2 > 0);
	} else if (std::isinf(d2)) {
		return false;
	}
	// Compute/check error
	return (fabs(d1 - d2) < epsilon);
}

EXPORT bool doubles_are_close_rel(double d1, double d2, double epsilon)
{
	// Deal with infinite cases
	if (std::isinf(d1)) {
		return std::isinf(d2) && (d1 > 0) == (d2 > 0);
	} else if (std::isinf(d2)) {
		return false;
	}
	// Compute/check error
	d1 = fabs(d1);
	d2 = fabs(d2);
	// For two (near) zero values, return true, for just one, return false
	if (d1 < epsilon_double)
		return (d2 < epsilon_double);
	return (fabs(d1 - d2) / d1 < epsilon);
}

//------------------------------------------------------------------------------

