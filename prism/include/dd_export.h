//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
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

#include <util.h>
#include <cudd.h>

//------------------------------------------------------------------------------

void DD_ExportDDToDotFile(DdManager *ddman, DdNode *dd, char *filename);
void DD_ExportDDToDotFileLabelled(DdManager *ddman, DdNode *dd, char *filename, char **var_names);
void DD_ExportDDToDotFile(DdManager *ddman, DdNode *dd, FILE *fp_out);
void DD_ExportDDToDotFileLabelled(DdManager *ddman, DdNode *dd, FILE *fp_out, char *title, char **var_names);

void DD_ExportDDToDDFile(DdManager *ddman, DdNode *dd, FILE *fp_out);
void DD_ExportDDToDDFile(DdManager *ddman, DdNode *dd, char *filename);
void DD_ExportVectorToMatlabFile(DdManager *ddman, DdNode *dd, DdNode **vars, int num_vars, char *name, char *filename);
void DD_ExportVectorToMatlabFile(DdManager *ddman, DdNode *dd, DdNode **vars, int num_vars, char *name, FILE *fp_out);
void DD_ExportMatrixToMatlabFile(DdManager *ddman, DdNode *dd, DdNode **rvars, int num_rvars, DdNode **cvars, int num_cvars, char *name, char *filename);
void DD_ExportMatrixToMatlabFile(DdManager *ddman, DdNode *dd, DdNode **rvars, int num_rvars, DdNode **cvars, int num_cvars, char *name, FILE *fp_out);
void DD_ExportMatrixToPPFile(DdManager *ddman, DdNode *dd, DdNode **rvars, int num_rvars, DdNode **cvars, int num_cvars, char *filename);
void DD_ExportMatrixToPPFile(DdManager *ddman, DdNode *dd, DdNode **rvars, int num_rvars, DdNode **cvars, int num_cvars, FILE *fp_out);
void DD_Export3dMatrixToPPFile(DdManager *ddman, DdNode *dd, DdNode **rvars, int num_rvars, DdNode **cvars, int num_cvars, DdNode **nvars, int num_nvars, char *filename);
void DD_Export3dMatrixToPPFile(DdManager *ddman, DdNode *dd, DdNode **rvars, int num_rvars, DdNode **cvars, int num_cvars, DdNode **nvars, int num_nvars, FILE *fp);
void DD_ExportMatrixToSpyFile(DdManager *ddman, DdNode *dd, DdNode **rvars, int num_rvars, DdNode **cvars, int num_cvars, int depth, char *filename);
void DD_ExportMatrixToSpyFile(DdManager *ddman, DdNode *dd, DdNode **rvars, int num_rvars, DdNode **cvars, int num_cvars, int depth, FILE *fp_out);

//------------------------------------------------------------------------------
