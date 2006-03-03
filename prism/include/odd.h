//==============================================================================
//
//	File:		odd.h
//	Date:		29/03/01
//	Author:		Dave Parker
//	Desc:		Header file for ODD stuff
//
//------------------------------------------------------------------------------
//
//	Copyright (c) 2002, Dave Parker
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

#ifndef ODD_H
#define ODD_H

//------------------------------------------------------------------------------

#include <util.h>
#include <cudd.h>
#include <dd.h>

// odd definitions

typedef struct ODDNode ODDNode;

struct ODDNode 
{
	//int index;
	DdNode *dd;
	ODDNode *next;
    	ODDNode *e;
    	ODDNode *t;
	long eoff;
	long toff;
};

// function prototypes

ODDNode *build_odd(DdManager *ddman, DdNode *dd, DdNode **vars, int num_vars);
int get_num_odd_nodes();

//------------------------------------------------------------------------------

#endif

//------------------------------------------------------------------------------
