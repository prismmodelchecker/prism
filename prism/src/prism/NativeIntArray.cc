//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Vojtech Forejt <vojtech.forejt@cs.ox.ac.uk> (University of Oxford)
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

#include "NativeIntArray.h"
#include "jnipointer.h"

JNIEXPORT jlong JNICALL Java_prism_NativeIntArray_NIA_1CreateArray
(JNIEnv * env, jclass cls, jint size)
{
    int* a = new int[size];
    return (long) a;
}

JNIEXPORT void JNICALL Java_prism_NativeIntArray_NIA_1DeleteArray
(JNIEnv * env, jclass cls, jlong ptr)
{
    delete ((int *) jlong_to_ptr(ptr));
}

JNIEXPORT jint JNICALL Java_prism_NativeIntArray_NIA_1Get
(JNIEnv * env, jclass cls, jlong ptr, jint index)
{
    return  ((int *) jlong_to_ptr(ptr))[index];
}

JNIEXPORT void JNICALL Java_prism_NativeIntArray_NIA_1Set
(JNIEnv * env, jclass cls, jlong ptr, jint index, jint value)
{
    ((int *) jlong_to_ptr(ptr))[index] = value;
}

JNIEXPORT void JNICALL Java_prism_NativeIntArray_NIA_1SetAll
(JNIEnv * env, jclass cls, jlong ptr, jint index, jint count, jint value)
{
    int* a = (int *) jlong_to_ptr(ptr);
    for(int i = 0; i < count; i++)
        a[i+index] = value;
}