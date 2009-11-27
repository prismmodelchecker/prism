//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
//	* Joachim Meyer-Kayser <Joachim.Meyer-Kayser@informatik.uni-erlangen.de> (University of Erlangen)
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

// includes
#include "prism.h"
#include <stdio.h>
#include <math.h>
#include <new>

//------------------------------------------------------------------------------

// convert a list of strings (from java/jni) to an array of c strings.
// actually stores arrays of both jstring objects and c strings, and also size
// (because need these to free memory afterwards).

void get_string_array_from_java(JNIEnv *env, jobject strings_list, jstring *&strings_jstrings, const char **&strings, int &size)
{
	int i, j;
	jclass vn_cls;
	jmethodID vn_mid;
	// get size of vector of strings
	vn_cls = env->GetObjectClass(strings_list);
	vn_mid = env->GetMethodID(vn_cls, "size", "()I");
	if (vn_mid == 0) {
		return;
	}
	size = (int)env->CallIntMethod(strings_list,vn_mid);
	// put strings from vector into array
	strings_jstrings = new jstring[size];
	strings = new const char*[size];
	vn_mid = env->GetMethodID(vn_cls, "get", "(I)Ljava/lang/Object;");
	if (vn_mid == 0) {
		return;
	}
	for (i = 0; i < size; i++) {
		strings_jstrings[i] = (jstring)env->CallObjectMethod(strings_list, vn_mid, i);
		strings[i] = env->GetStringUTFChars(strings_jstrings[i], 0);
	}
}

//------------------------------------------------------------------------------

// release the memory from a list of strings created by get_string_array_from_java

void release_string_array_from_java(JNIEnv *env, jstring *strings_jstrings, const char **strings, jint size)
{
	// release memory
	for (int i = 0; i < size; i++) {
		env->ReleaseStringUTFChars(strings_jstrings[i], strings[i]);
	}
	delete[] strings_jstrings;
	delete[] strings;
}

//------------------------------------------------------------------------------

// Compute poisson probabilities for uniformisation (Fox-Glynn method)
// NB: This function was written by Joachim Meyer-Kayser (converted from Java)

EXPORT FoxGlynnWeights fox_glynn(double q_tmax, double underflow, double overflow, double accuracy)
{
	int m;
	double q;
	FoxGlynnWeights fgw;
	
	m = (int)floor(q_tmax);
	
	{
		double m2 = m;
		double k;
		
		if (q_tmax == 0.0) {
			printf("Overflow: TA parameter qtmax = time * maxExitRate = 0.");
		}
		if (q_tmax < 25.0) {
			fgw.left = 0;
		}
		if (q_tmax < 400.0) {
			// Find right using Corollary 1 with q_tmax=400
			double sqrt2  = sqrt(2.0);
			double sqrtl  = 20;
			double a = 1.0025 * exp (0.0625) * sqrt2;
			double b = 1.0025 * exp (0.125/400); 	//exp (0.0003125)
			double startk = 1.0/(2.0 * sqrt2 * 400);
			double stopk = sqrtl/(2*sqrt2);
			
			for (k = startk; k <= stopk; k += 3.0) {
			double d = 1.0/(1 - exp ((-2.0/9.0)*(k*sqrt2*sqrtl + 1.5)));
			double f = a * d * exp (-0.5*k*k) / (k * sqrt (2.0 * 3.1415926));
			
			if (f <= accuracy/2.0)
				break;
			}
			
			if (k > stopk) 
				k = stopk;
			
			fgw.right = (int) ceil(m2 + k*sqrt2*sqrtl + 1.5);
		}
		if (q_tmax >= 400.0) {
			// Find right using Corollary 1 using actual q_tmax 
			double sqrt2  = sqrt (2.0);
			double sqrtl  = sqrt (q_tmax);
			double a = (1.0 + 1.0/q_tmax) * exp (0.0625) * sqrt2;
			double b = (1.0 + 1.0/q_tmax) * exp (0.125/q_tmax);
			double startk = 1.0/(2.0 * sqrt2 * q_tmax);
			double stopk = sqrtl/(2*sqrt2);
			
			for (k = startk; k <= stopk; k += 3.0) {
				double d = 1.0/(1 - exp ((-2.0/9.0)*(k*sqrt2*sqrtl + 1.5)));
				double f = a * d * exp (-0.5*k*k) / (k * sqrt (2.0 * 3.1415926));
				
				if (f <= accuracy/2.0)
					break;
			}
			
			if (k > stopk) 
				k = stopk;
			
			fgw.right = (int) ceil(m2 + k*sqrt2*sqrtl + 1.5);
		}
		if (q_tmax >= 25.0) {
			// Find left using Corollary 2 using actual q_tmax 
			double sqrt2  = sqrt (2.0);
			double sqrtl  = sqrt (q_tmax);
			double a = (1.0 + 1.0/q_tmax) * exp (0.0625) * sqrt2;
			double b = (1.0 + 1.0/q_tmax) * exp (0.125/q_tmax);
			double startk = 1.0/(sqrt2*sqrtl);
			double stopk =  (m2 - 1.5)/(sqrt2*sqrtl);
			
			for (k = startk; k <= stopk; k += 3.0) {
			if (b * exp(-0.5*k*k)/(k * sqrt (2.0 * 3.1415926)) <= accuracy/2.0)
				break;
			}
			
			if (k > stopk) 
				k = stopk;
				
			fgw.left = (int) floor(m2 - k*sqrtl - 1.5);
		}
		
		if (fgw.left < 0) {
			fgw.left = 0;
			//printf("Weighter: negative left truncation point found. Ignored.\n");
		}
		
		q = overflow / (pow(10.0, 10.0) * (fgw.right - fgw.left));
	}
	
	fgw.weights = new double[fgw.right-fgw.left+1];
	fgw.weights[m-fgw.left] = q;
	
	// down
	for (int j=m; j>fgw.left; j--) {
		fgw.weights[j-1-fgw.left] = (j/q_tmax) * fgw.weights[j-fgw.left];
	}
	
	//up
	if (q_tmax < 400) {
		
		if (fgw.right > 600) {
			printf("Overflow: right truncation point > 600.");
		} 
		
		for (int j=m; j<fgw.right; ) {
			q = q_tmax / (j+1);
			
			if (fgw.weights[j-fgw.left] > underflow/q) {
				fgw.weights[j+1-fgw.left] = q * fgw.weights[j-fgw.left];
				j++;
			}
			else {
				fgw.right = j;
			}
		} 
	}
	else {
		for (int j=m; j<fgw.right; j++) {
			fgw.weights[j+1-fgw.left] = (q_tmax/(j+1)) * fgw.weights[j-fgw.left];
		}
	}
	
	{
		int l = fgw.left;
		int r = fgw.right;
		fgw.total_weight = 0.0;
		
		while (l < r) {
			if (fgw.weights[l-fgw.left] <= fgw.weights[r-fgw.left]) {
				fgw.total_weight += fgw.weights[l-fgw.left];
				++l;
			}
			else {
				fgw.total_weight += fgw.weights[r-fgw.left];
				--r;
			}
		}
		fgw.total_weight += fgw.weights[l-fgw.left];
	}
	
	return fgw;
}

//------------------------------------------------------------------------------
