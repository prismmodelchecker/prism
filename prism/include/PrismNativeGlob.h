//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford)
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

#ifndef PRISMNATIVEGLOB_H
#define PRISMNATIVEGLOB_H

//------------------------------------------------------------------------------
#include <jni.h>

// Flags for building Windows DLLs
#ifdef __MINGW32__
	#define EXPORT __declspec(dllexport)
#else
	#define EXPORT
#endif

//------------------------------------------------------------------------------

// Constants - these need to match the definitions in prism/Prism.java

const int EXPORT_PLAIN = 1;
const int EXPORT_MATLAB = 2;
const int EXPORT_DOT = 3;
const int EXPORT_MRMC = 4;
const int EXPORT_ROWS = 5;
const int EXPORT_DOT_STATES = 6;

const int LIN_EQ_METHOD_POWER = 1;
const int LIN_EQ_METHOD_JACOBI = 2;
const int LIN_EQ_METHOD_GAUSSSEIDEL = 3;
const int LIN_EQ_METHOD_BGAUSSSEIDEL = 4;
const int LIN_EQ_METHOD_PGAUSSSEIDEL = 5;
const int LIN_EQ_METHOD_BPGAUSSSEIDEL = 6;
const int LIN_EQ_METHOD_JOR = 7;
const int LIN_EQ_METHOD_SOR = 8;
const int LIN_EQ_METHOD_BSOR = 9;
const int LIN_EQ_METHOD_PSOR = 10;
const int LIN_EQ_METHOD_BPSOR = 11;

const int TERM_CRIT_ABSOLUTE = 1;
const int TERM_CRIT_RELATIVE = 2;

const int EXPORT_ADV_NONE = 1;
const int EXPORT_ADV_DTMC = 2;
const int EXPORT_ADV_MDP = 3;

const int REACH_BFS = 1;
const int REACH_FRONTIER = 2;

//------------------------------------------------------------------------------

// External refs to global variables

// Prism object
EXPORT extern jclass prism_cls;
EXPORT extern jobject prism_obj;

// Options:
// numerical method stuff
EXPORT extern int lin_eq_method;
EXPORT extern double lin_eq_method_param;
EXPORT extern int term_crit;
EXPORT extern double term_crit_param;
EXPORT extern int max_iters;
// use "compact modified" sparse matrix storage?
EXPORT extern bool compact;
// sparse bits info
EXPORT extern int sb_max_mem;
EXPORT extern int num_sb_levels;
// hybrid sor info
EXPORT extern int sor_max_mem;
EXPORT extern int num_sor_levels;
// use steady-state detection for transient computation?
EXPORT extern bool do_ss_detect;
// adversary EXPORT extern mode
EXPORT extern int export_adv;
// adversary export filename
EXPORT extern const char *export_adv_filename;

//------------------------------------------------------------------------------

#endif

//------------------------------------------------------------------------------
