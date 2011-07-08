//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
//	* Vojtech Forejt <vojtech.forejt@cs.ox.ac.uk> (University of Oxford)
//	Description: DD export functions
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

#include <stdio.h>
#include <stdlib.h>
#include <math.h>
#include <util.h>
#include <cudd.h>
#include "dd.h"
#include "dd_basics.h"
#include "dd_abstr.h"
#include "dd_info.h"

//------------------------------------------------------------------------------

// local prototypes

static int  DD_ExportDDToDDFile(DdManager *ddman, DdNode *dd, DdNode **done, int count, FILE *fp);
static void DD_ExportMatrixToPPFile(DdManager *ddman, DdNode *dd, DdNode **rvars, int num_rvars, DdNode **cvars, int num_cvars, FILE *fp, int rstart, int cstart);
static void DD_ExportVectorToMatlabFile(DdManager *ddman, DdNode *dd, DdNode **vars, int num_vars, char *name, FILE *fp, int start);
static void DD_ExportMatrixToMatlabFile(DdManager *ddman, DdNode *dd, DdNode **rvars, int num_rvars, DdNode **cvars, int num_cvars, char *name, FILE *fp, int rstart, int cstart);
static void DD_ExportMatrixToPPFile(DdManager *ddman, DdNode *dd, DdNode **rvars, int num_rvars, DdNode **cvars, int num_cvars, FILE *fp, int rstart, int cstart);
static void DD_ExportMatrixToSpyFile(DdManager *ddman, DdNode *dd, DdNode **rvars, int num_rvars, DdNode **cvars, int num_cvars, FILE *fp, int rstart, int cstart, unsigned char **array);

//------------------------------------------------------------------------------

void DD_ExportDDToDotFile
(
DdManager *ddman,
DdNode *dd,
char *filename
)
{
	FILE *fp = fopen(filename, "w");
	if (fp == NULL) {
		// crash out
		return;
	}
	DD_ExportDDToDotFileLabelled(ddman, dd, fp, (char*)"DD", NULL);
	fclose(fp);
}

//------------------------------------------------------------------------------

void DD_ExportDDToDotFileLabelled
(
DdManager *ddman,
DdNode *dd,
char *filename,
char **var_names
)
{
	FILE *fp = fopen(filename, "w");
	if (fp == NULL) {
		// crash out
		return;
	}
	DD_ExportDDToDotFileLabelled(ddman, dd, fp, (char*)"DD", var_names);
	fclose(fp);
}

//------------------------------------------------------------------------------

void DD_ExportDDToDotFile
(
DdManager *ddman,
DdNode *dd,
FILE *fp
)
{
	DD_ExportDDToDotFileLabelled(ddman, dd, fp, (char*)"DD", NULL);
}

//------------------------------------------------------------------------------

void DD_ExportDDToDotFileLabelled
(
DdManager *ddman,
DdNode *dd,
FILE *fp,
char *title,
char **var_names
)
{
	Cudd_DumpDot(ddman, 1, &dd, var_names, &title, fp);
}

//------------------------------------------------------------------------------

void DD_ExportDDToDDFile
(
DdManager *ddman,
DdNode *dd,
char *filename
)
{
	FILE *fp = fopen(filename, "w");
	if (fp == NULL) {
		// crash out
		return;
	}
	DD_ExportDDToDDFile(ddman, dd, fp);
	fclose(fp);
}

//------------------------------------------------------------------------------

void DD_ExportDDToDDFile
(
DdManager *ddman,
DdNode *dd,
FILE *fp
)
{
	int num_nodes;
	DdNode **stored;
	
	num_nodes = DD_GetNumNodes(ddman, dd);
	fprintf(fp, "%d\n", num_nodes);
	stored = new DdNode*[num_nodes];
	
	DD_ExportDDToDDFile(ddman, dd, stored, 0, fp);
	
	delete[] stored;
}

//------------------------------------------------------------------------------

static int DD_ExportDDToDDFile
(
DdManager *ddman,
DdNode *dd,
DdNode **stored,
int count,
FILE *fp
)
{
	int i;
	bool found;
	
	i = 0;
	found = false;
	while (i < count && !found) {
		if (stored[i] == dd) {
			found = true;
		}
		else {
			i++;
		}
	}
	if (!found) {
		stored[count++] = dd;
	}
	
	fprintf(fp, "%d\n", (size_t)dd);
	return 0;
}

//------------------------------------------------------------------------------

void DD_ExportVectorToMatlabFile
(
DdManager *ddman,
DdNode *dd,
DdNode **vars,
int num_vars,
char *name,
char *filename
)
{
	FILE *fp = fopen(filename, "w");
	if (fp == NULL) {
		// crash out
		return;
	}
	DD_ExportVectorToMatlabFile(ddman, dd, vars, num_vars, name, fp);
	fclose(fp);
}

//------------------------------------------------------------------------------

void DD_ExportVectorToMatlabFile
(
DdManager *ddman,
DdNode *dd,
DdNode **vars,
int num_vars,
char *name,
FILE *fp
)
{
	fprintf(fp, "%s = sparse(1, %d);\n", name, (int)pow(2.0, num_vars));
	DD_ExportVectorToMatlabFile(ddman, dd, vars, num_vars, name, fp, 0);
}

//------------------------------------------------------------------------------

static void DD_ExportVectorToMatlabFile
(
DdManager *ddman,
DdNode *dd,
DdNode **vars,
int num_vars,
char *name,
FILE *fp,
int start
)
{
	DdNode *n, *s;
	
	if (dd == Cudd_addConst(ddman, 0)) {
		return;
	}
			
	if (num_vars == 0) {
		fprintf(fp, "%s(1, %d) = %f;\n", name, start+1, Cudd_V(dd));
	}
	else {
		// split into 2 cases
		Cudd_Ref(dd);
		Cudd_Ref(vars[0]);
		n = DD_Restrict(ddman, dd, DD_Not(ddman, vars[0]));
		Cudd_Ref(dd);
		Cudd_Ref(vars[0]);
		s = DD_Restrict(ddman, dd, vars[0]);
		
		DD_ExportVectorToMatlabFile(ddman, n, &vars[1], num_vars-1, name, fp, start);
		DD_ExportVectorToMatlabFile(ddman, s, &vars[1], num_vars-1, name, fp, start+(int)pow(2.0, num_vars-1));
		
		Cudd_RecursiveDeref(ddman, n);
		Cudd_RecursiveDeref(ddman, s);
	}
}

//------------------------------------------------------------------------------

void DD_ExportMatrixToMatlabFile
(
DdManager *ddman,
DdNode *dd,
DdNode **rvars,
int num_rvars,
DdNode **cvars,
int num_cvars,
char *name,
char *filename
)
{
	FILE *fp = fopen(filename, "w");
	if (fp == NULL) {
		// crash out
		return;
	}
	DD_ExportMatrixToMatlabFile(ddman, dd, rvars, num_rvars, cvars, num_cvars, name, fp);
	fclose(fp);
}

//------------------------------------------------------------------------------

void DD_ExportMatrixToMatlabFile
(
DdManager *ddman,
DdNode *dd,
DdNode **rvars,
int num_rvars,
DdNode **cvars,
int num_cvars,
char *name,
FILE *fp
)
{
	fprintf(fp, "%s = sparse(%d, %d);\n", name, (int)pow(2.0, num_rvars), (int)pow(2.0, num_cvars));
	DD_ExportMatrixToMatlabFile(ddman, dd, rvars, num_rvars, cvars, num_cvars, name, fp, 0, 0);
}

//------------------------------------------------------------------------------

static void DD_ExportMatrixToMatlabFile
(
DdManager *ddman,
DdNode *dd,
DdNode **rvars,
int num_rvars,
DdNode **cvars,
int num_cvars,
char *name,
FILE *fp,
int rstart,
int cstart
)
{
	DdNode *n, *s, *ne, *nw, *sw, *se;
	
	if (dd == Cudd_addConst(ddman, 0)) {
		return;
	}
			
	if (num_rvars == 0) {
		fprintf(fp, "%s(%d, %d) = %f;\n", name, rstart+1, cstart+1, Cudd_V(dd));
	}
	else {
		// split into 4 cases
		Cudd_Ref(dd);
		Cudd_Ref(rvars[0]);
		n = DD_Restrict(ddman, dd, DD_Not(ddman, rvars[0]));
		Cudd_Ref(dd);
		Cudd_Ref(rvars[0]);
		s = DD_Restrict(ddman, dd, rvars[0]);
		Cudd_Ref(n);
		Cudd_Ref(cvars[0]);
		nw = DD_Restrict(ddman, n, DD_Not(ddman, cvars[0]));
		Cudd_Ref(n);
		Cudd_Ref(cvars[0]);
		ne = DD_Restrict(ddman, n, cvars[0]);
		Cudd_Ref(s);
		Cudd_Ref(cvars[0]);
		sw = DD_Restrict(ddman, s, DD_Not(ddman, cvars[0]));
		Cudd_Ref(s);
		Cudd_Ref(cvars[0]);
		se = DD_Restrict(ddman, s, cvars[0]);
		
		DD_ExportMatrixToMatlabFile(ddman, nw, &rvars[1], num_rvars-1, &cvars[1], num_cvars-1, name, fp, rstart, cstart);
		DD_ExportMatrixToMatlabFile(ddman, ne, &rvars[1], num_rvars-1, &cvars[1], num_cvars-1, name, fp, rstart, cstart+(int)pow(2.0, num_cvars-1));
		DD_ExportMatrixToMatlabFile(ddman, sw, &rvars[1], num_rvars-1, &cvars[1], num_cvars-1, name, fp, rstart+(int)pow(2.0, num_rvars-1), cstart);
		DD_ExportMatrixToMatlabFile(ddman, se, &rvars[1], num_rvars-1, &cvars[1], num_cvars-1, name, fp, rstart+(int)pow(2.0, num_rvars-1), cstart+(int)pow(2.0, num_cvars-1));
		
		Cudd_RecursiveDeref(ddman, n);
		Cudd_RecursiveDeref(ddman, s);
		Cudd_RecursiveDeref(ddman, nw);
		Cudd_RecursiveDeref(ddman, ne);
		Cudd_RecursiveDeref(ddman, sw);
		Cudd_RecursiveDeref(ddman, se);
	}
}

//------------------------------------------------------------------------------

void DD_ExportMatrixToPPFile
(
DdManager *ddman,
DdNode *dd,
DdNode **rvars,
int num_rvars,
DdNode **cvars,
int num_cvars,
char *filename
)
{
	FILE *fp = fopen(filename, "w");
	if (fp == NULL) {
		// crash out
		return;
	}
	DD_ExportMatrixToPPFile(ddman, dd, rvars, num_rvars, cvars, num_cvars, fp);
	fclose(fp);
}

//------------------------------------------------------------------------------

void DD_ExportMatrixToPPFile
(
DdManager *ddman,
DdNode *dd,
DdNode **rvars,
int num_rvars,
DdNode **cvars,
int num_cvars,
FILE *fp
)
{
	fprintf(fp, "%d\n", (int)pow(2.0, num_rvars));
	DD_ExportMatrixToPPFile(ddman, dd, rvars, num_rvars, cvars, num_cvars, fp, 0, 0);
}

//------------------------------------------------------------------------------

static void DD_ExportMatrixToPPFile
(
DdManager *ddman,
DdNode *dd,
DdNode **rvars,
int num_rvars,
DdNode **cvars,
int num_cvars,
FILE *fp,
int rstart,
int cstart
)
{
	DdNode *n, *s, *ne, *nw, *sw, *se;
	
	if (dd == Cudd_addConst(ddman, 0)) {
		return;
	}
	
	// handle non square bit (if r < c ...)
	// (if i can be bothered)
		
	if (num_rvars == 0) {
		fprintf(fp, "%d %d %f\n", rstart, cstart, Cudd_V(dd));
	}
	else {
		// split into 4 cases
		Cudd_Ref(dd);
		Cudd_Ref(rvars[0]);
		n = DD_Restrict(ddman, dd, DD_Not(ddman, rvars[0]));
		Cudd_Ref(dd);
		Cudd_Ref(rvars[0]);
		s = DD_Restrict(ddman, dd, rvars[0]);
		Cudd_Ref(n);
		Cudd_Ref(cvars[0]);
		nw = DD_Restrict(ddman, n, DD_Not(ddman, cvars[0]));
		Cudd_Ref(n);
		Cudd_Ref(cvars[0]);
		ne = DD_Restrict(ddman, n, cvars[0]);
		Cudd_Ref(s);
		Cudd_Ref(cvars[0]);
		sw = DD_Restrict(ddman, s, DD_Not(ddman, cvars[0]));
		Cudd_Ref(s);
		Cudd_Ref(cvars[0]);
		se = DD_Restrict(ddman, s, cvars[0]);
		
		DD_ExportMatrixToPPFile(ddman, nw, &rvars[1], num_rvars-1, &cvars[1], num_cvars-1, fp, rstart, cstart);
		DD_ExportMatrixToPPFile(ddman, ne, &rvars[1], num_rvars-1, &cvars[1], num_cvars-1, fp, rstart, cstart+(int)pow(2.0, num_cvars-1));
		DD_ExportMatrixToPPFile(ddman, sw, &rvars[1], num_rvars-1, &cvars[1], num_cvars-1, fp, rstart+(int)pow(2.0, num_rvars-1), cstart);
		DD_ExportMatrixToPPFile(ddman, se, &rvars[1], num_rvars-1, &cvars[1], num_cvars-1, fp, rstart+(int)pow(2.0, num_rvars-1), cstart+(int)pow(2.0, num_cvars-1));
		
		Cudd_RecursiveDeref(ddman, n);
		Cudd_RecursiveDeref(ddman, s);
		Cudd_RecursiveDeref(ddman, nw);
		Cudd_RecursiveDeref(ddman, ne);
		Cudd_RecursiveDeref(ddman, sw);
		Cudd_RecursiveDeref(ddman, se);
	}
}

//------------------------------------------------------------------------------

// Given a BDD that represents a MDP transition matrices, this method
// outputs one matrix for every action.
// note that the output is in fact not a PP file, but
// several PP files concatenated into one file.
//
// For example, for a model with the variable
//  x : [0..2];
// and transitions
//  [a] (x=0) -> 0.3:(x'=1) + 0.7:(x'=2);
//  [b] (x=0) -> 1:(x'=2);
//  [a] (x=2) -> (x'=1);
//  [a] (x=1) -> (x'=0);
// the output would be (e.g.)
//  4
//  4
//  0 2 1.000000
//  4
//  0 1 0.300000
//  1 0 1.000000
//  0 2 0.700000
//  2 1 1.000000
//  4
void DD_Export3dMatrixToPPFile
(
 DdManager *ddman,
 DdNode *dd,
 DdNode **rvars,
 int num_rvars,
 DdNode **cvars,
 int num_cvars,
 DdNode **nvars,
 int num_nvars,
 char *filename
 )
{
	FILE *fp = fopen(filename, "w");
	if (fp == NULL) {
		// crash out
		return;
	}
	DD_Export3dMatrixToPPFile(ddman, dd, rvars, num_rvars, cvars, num_cvars, nvars, num_nvars, fp);
	fclose(fp);
}

void DD_Export3dMatrixToPPFile
(
 DdManager *ddman,
 DdNode *dd,
 DdNode **rvars,
 int num_rvars,
 DdNode **cvars,
 int num_cvars,
 DdNode **nvars,
 int num_nvars,
 FILE *fp
 )
{
	DdNode *n, *s;
	if (num_nvars == 0)
	{   //base step: if there are no nondeterministic variables, print output as for normal PP file
		fprintf(fp, "%d\n", (int)pow(2.0, num_rvars));
		DD_ExportMatrixToPPFile(ddman, dd, rvars, num_rvars, cvars, num_cvars, fp, 0, 0);
	}
	else
	{ //if there are some nondeterministic variables, we remove one of them, splitting into two cases
		Cudd_Ref(dd);
		Cudd_Ref(nvars[0]);
		n = DD_Restrict(ddman, dd, DD_Not(ddman, nvars[0]));
		Cudd_Ref(dd);
		Cudd_Ref(nvars[0]);
		s = DD_Restrict(ddman, dd, nvars[0]);
		
		DD_Export3dMatrixToPPFile(ddman, n, rvars, num_rvars, cvars, num_cvars, &nvars[1], num_nvars-1, fp);
		DD_Export3dMatrixToPPFile(ddman, s, rvars, num_rvars, cvars, num_cvars, &nvars[1], num_nvars-1, fp);
		
		Cudd_RecursiveDeref(ddman, n);
		Cudd_RecursiveDeref(ddman, s);
	}
}

//------------------------------------------------------------------------------

void DD_ExportMatrixToSpyFile
(
DdManager *ddman,
DdNode *dd,
DdNode **rvars,
int num_rvars,
DdNode **cvars,
int num_cvars,
int depth,
char *filename
)
{
	FILE *fp = fopen(filename, "w");
	if (fp == NULL) {
		// crash out
		return;
	}
	DD_ExportMatrixToSpyFile(ddman, dd, rvars, num_rvars, cvars, num_cvars, depth, fp);
	fclose(fp);
}

//------------------------------------------------------------------------------

void DD_ExportMatrixToSpyFile
(
DdManager *ddman,
DdNode *dd,
DdNode **rvars,
int num_rvars,
DdNode **cvars,
int num_cvars,
int depth,
FILE *fp
)
{
	DdNode *new_dd;
	int num_states, i, j;
	unsigned char **array;
	
	if (depth > num_rvars || depth < 3) {
		return;
	}
	
	// abstract away bottom of input dd to leave required depth
	Cudd_Ref(dd);
	new_dd = DD_MaxAbstract(ddman, dd, &rvars[depth], num_rvars - depth);
	new_dd = DD_MaxAbstract(ddman, new_dd, &cvars[depth], num_cvars - depth);
	new_dd = DD_StrictThreshold(ddman, new_dd, 0);
	
	// create array to store 0-1 spy info
	num_states = (int)pow(2.0, depth);
	array = new unsigned char*[num_states];
	for (i = 0; i < num_states; i++) {
		array[i] = new unsigned char[num_states/8];
		for (j = 0; j < num_states/8; j++) {
			array[i][j] = 0;
		}
	}
	
	DD_ExportMatrixToSpyFile(ddman, new_dd, rvars, depth, cvars, depth, fp, 0, 0, array);
	
	fwrite(&num_rvars, sizeof(num_rvars), 1, fp);
	fwrite(&depth, sizeof(depth), 1, fp);
	for (i = 0; i < num_states; i++) {
		fwrite(array[i], num_states/8, 1, fp);
	}
	
	Cudd_RecursiveDeref(ddman, new_dd);
	for (i = 0; i < num_states; i++) delete[] array[i];
	delete[] array;
}

//------------------------------------------------------------------------------

static void DD_ExportMatrixToSpyFile
(
DdManager *ddman,
DdNode *dd,
DdNode **rvars,
int num_rvars,
DdNode **cvars,
int num_cvars,
FILE *fp,
int rstart,
int cstart,
unsigned char **array
)
{
	DdNode *n, *s, *ne, *nw, *sw, *se;
	
	if (dd == Cudd_addConst(ddman, 0)) {
		return;
	}
	
	// handle non square bit (if r < c ...)
	// (if i can be bothered)
		
	if (num_rvars == 0) {
		array[rstart][cstart/8] |= (int)pow(2.0,(cstart%8));
	}
	else {
		// split into 4 cases
		Cudd_Ref(dd);
		Cudd_Ref(rvars[0]);
		n = DD_Restrict(ddman, dd, DD_Not(ddman, rvars[0]));
		Cudd_Ref(dd);
		Cudd_Ref(rvars[0]);
		s = DD_Restrict(ddman, dd, rvars[0]);
		Cudd_Ref(n);
		Cudd_Ref(cvars[0]);
		nw = DD_Restrict(ddman, n, DD_Not(ddman, cvars[0]));
		Cudd_Ref(n);
		Cudd_Ref(cvars[0]);
		ne = DD_Restrict(ddman, n, cvars[0]);
		Cudd_Ref(s);
		Cudd_Ref(cvars[0]);
		sw = DD_Restrict(ddman, s, DD_Not(ddman, cvars[0]));
		Cudd_Ref(s);
		Cudd_Ref(cvars[0]);
		se = DD_Restrict(ddman, s, cvars[0]);
		
		DD_ExportMatrixToSpyFile(ddman, nw, &rvars[1], num_rvars-1, &cvars[1], num_cvars-1, fp, rstart, cstart, array);
		DD_ExportMatrixToSpyFile(ddman, ne, &rvars[1], num_rvars-1, &cvars[1], num_cvars-1, fp, rstart, cstart+(int)pow(2.0, num_cvars-1), array);
		DD_ExportMatrixToSpyFile(ddman, sw, &rvars[1], num_rvars-1, &cvars[1], num_cvars-1, fp, rstart+(int)pow(2.0, num_rvars-1), cstart, array);
		DD_ExportMatrixToSpyFile(ddman, se, &rvars[1], num_rvars-1, &cvars[1], num_cvars-1, fp, rstart+(int)pow(2.0, num_rvars-1), cstart+(int)pow(2.0, num_cvars-1), array);
		
		Cudd_RecursiveDeref(ddman, n);
		Cudd_RecursiveDeref(ddman, s);
		Cudd_RecursiveDeref(ddman, nw);
		Cudd_RecursiveDeref(ddman, ne);
		Cudd_RecursiveDeref(ddman, sw);
		Cudd_RecursiveDeref(ddman, se);
	}
}

//------------------------------------------------------------------------------
