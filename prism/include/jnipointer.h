//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Alistair John Strachan <alistair@devzero.co.uk> (University of Edinburgh)
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

#ifndef JNIPOINTER_H
#define JNIPOINTER_H

#include <inttypes.h>
#include "jni.h"

/*
 * Currently does nothing, used to annotate jlong's that are pointers
 */

#define __jlongpointer

/*
 * C++ doesn't support implicit conversion from void*
 */

#define jlong_to_FILE(x)		(FILE *)jlong_to_ptr(x)

// for dd
#define jlong_to_DdManager(x)		(DdManager *)jlong_to_ptr(x)
#define jlong_to_DdNode(x)		(DdNode *)jlong_to_ptr(x)
#define jlong_to_DdNode_array(x)	(DdNode **)jlong_to_ptr(x)

// for odd
#define jlong_to_ODDNode(x)		(ODDNode *)jlong_to_ptr(x)
#define jlong_to_double(x)		(double *)jlong_to_ptr(x)

// for sparse matrices
#define jlong_to_NDSparseMatrix(x)	(NDSparseMatrix *)jlong_to_ptr(x)

// void* <-> jlong conversions functions

static inline jlong __jlongpointer ptr_to_jlong(void *ptr)
{
	return (jlong)((intptr_t)ptr);
}

static inline void *jlong_to_ptr(jlong __jlongpointer ptr)
{
	return (void*)((intptr_t)ptr);
}

#endif // JNIPOINTER_H
