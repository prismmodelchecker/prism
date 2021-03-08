//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
//	* Vojtech Forejt <forejt@fi.muni.cz> (Masaryk University)
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
#include <cstdio>
#include <cmath>
#include <new>
#include <vector>
#include "jnipointer.h"
#include "PrismNativeGlob.h"

//------------------------------------------------------------------------------

// Get the current time in milliseconds (via Java).
// Returns -1 in case of error obtaining the time.

long get_real_time(JNIEnv *env)
{
	jlong ret = 0;
	if (env) {
		jclass cl = env->FindClass("java/lang/System");
		jmethodID me = env->GetStaticMethodID(cl, "currentTimeMillis", "()J");
		ret = env->CallLongMethod(cl, me);
	} else {
		ret = -1;
	}
	return ret;
}

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

// Compute probabilities for the Poisson distribution efficiently and reliably,
// truncating above and below for a provided error bound. Implements the algorithm from:
// B. Fox and P. Glynn, Computing Poisson Probabilities,
// Communications of the ACM 31(4):440-445, 1988.

// Computes the probabilities for the Poisson distribution with rate q_tmax.
// The ith probability is returned for L<=i<=R, where L and R
// are determined according to the requested accuracy.
// The sum of the probabilities will be greater than equal to 1-accuracy.
// Furthermore, the sum of probabilities for i<L and i>R are both <=accuracy/2.
// Thresholds for underflow and overflow should also be given.
// Note that the Fox-Glynn method requires accuracy to be at least 1e-10.

// The probabilities are given in an array in the form of weights;
// these should be normalised by dividing by the sum of the weights.
// These, and the left/right truncation points are all contained in
// the returned FoxGlynnWeights struct.

// On error, the member 'right' of the returned object is set to -1 and the
// member array 'weights' is not allocated (does not need to be freed).

EXPORT FoxGlynnWeights fox_glynn(double q_tmax, double underflow, double overflow, double accuracy)
{
	// construct result struct and zero-initialise
	// this ensures that fgw.weights = nullptr in case we return early on error
	FoxGlynnWeights fgw{};

	if (q_tmax == 0.0) {
		printf("Overflow: TA parameter qtmax = time * maxExitRate = 0.");
		fgw.right = -1;
		return fgw;
	}

	if (accuracy < 1e-10) {
		printf("Overflow: Accuracy is smaller than Fox Glynn can handle (must be at least 1e-10).");
		fgw.right = -1;
		return fgw;
	}

	if (q_tmax < 400)
	{ //here naive approach should have better performance than Fox Glynn
		const double expcoef = exp(-q_tmax); //the "e^-lambda" part of p.m.f. of Poisson dist.
		int k; //denotes that we work with event "k steps occur"
		double lastval; //(probability that exactly k events occur)/expcoef
		double accum; //(probability that 0 to k events occur)/expcoef
		double desval = (1-(accuracy/2.0)) / expcoef; //value that we want to accumulate in accum before we stop
		std::vector<double> w; //stores weights computed so far.
		
		//k=0 is simple
		lastval = 1;
		accum = lastval;
		w.push_back(lastval * expcoef);
		
		//add further steps until you have accumulated enough
		k = 1;
		do {
			// TODO: catch case where lastval gets so small that
			// accnum never reaches desval due to rounding/floating point precision errors (infinite loop)

			lastval *= q_tmax / k; // invariant: lastval = q_tmax^k / k!
			accum += lastval;
			w.push_back(lastval * expcoef);
			k++;
		} while (accum < desval);

		//store all data to fgw
		fgw.left=0;
		fgw.right=k-1;
		fgw.weights = new double[k];

		for(int i = 0; i < w.size(); i++)
		{
			fgw.weights[i] = w[i];			
		}

		//we return actual weights, so no reweighting should be done
		fgw.total_weight = 1.0;
	}
	else
	{ //use actual Fox Glynn for q_tmax>400
		const double factor = 1e+10;

		const long m = (long) q_tmax; //mode
		//run FINDER to get left, right and weight[m]
		{
			const double sqrtpi = 1.7724538509055160; //square root of PI
			const double sqrt2 = 1.4142135623730950; //square root of 2
			const double sqrtq = sqrt(q_tmax);
			const double aq = (1.0 + 1.0/q_tmax) * exp(0.0625) * sqrt2; //a_\lambda from the paper			
			const double bq = (1.0 + 1.0/q_tmax) * exp(0.125/q_tmax); //b_\lambda from the paper

			//use Corollary 1 to find right truncation point
			const double lower_k_1 = 1.0 / (2.0*sqrt2*q_tmax); //lower bound on k from Corollary 1
			const double upper_k_1 = sqrtq / (2.0*sqrt2); //upper bound on k from Corollary 1
			double k;

			//justification for increment is in the paper:
			//"increase k through the positive integers greater than 3"
			for(k=lower_k_1; k <= upper_k_1;
				k=(k==lower_k_1)? k+4 : k+1 )
			{
				double dkl = 1.0/(1 - exp(-(2.0/9.0)*(k*sqrt2*sqrtq+1.5))); //d(k,\lambda) from the paper
				double res = aq*dkl*exp(-k*k/2.0)/(k*sqrt2*sqrtpi); //right hand side of the equation in Corollary 1
				if (res <= accuracy/2.0)
				{
					break;
				}
			}

			if (k>upper_k_1)
				k=upper_k_1;

			const double right_d = ceil(m+k*sqrt2*sqrtq + 1.5);
			fgw.right = (long) right_d; 

			//use Corollary 2 to find left truncation point
			//NOTE: the original implementation used some upper bound on k,
			//      however, I didn't find it in the paper and I think it is not needed
			const double lower_k_2 = 1.0/(sqrt2*sqrtq); //lower bound on k from Corollary 2

			double res;
			k=lower_k_2;
			do
			{
				res = bq*exp(-k*k/2.0)/(k*sqrt2*sqrtpi); //right hand side of the equation in Corollary 2
				k++;			
			}
			while (res > accuracy/2.0);
			
			fgw.left = (long) (m - k*sqrtq - 1.5);
			
			//According to the paper, we should check underflow of lower bound.
			//However, it seems that for no reasonable values this can happen.
			//And neither the original implementation checked it
			
			double wm = overflow / (factor*(fgw.right - fgw.left));

			fgw.weights = new double[fgw.right-fgw.left+1];
			fgw.weights[m-fgw.left] = wm;
		}
		//end of FINDER

		//compute weights
		//(at this point fgw.left, fgw.right and fgw.weight[m] is known)
		
		//Down from m
		for(long j=m; j>fgw.left; j--)
			fgw.weights[j-1-fgw.left] = (j/q_tmax)*fgw.weights[j-fgw.left];
		//Up from m
		for(long j=m; j<fgw.right; j++)
			fgw.weights[j+1-fgw.left] = (q_tmax/(j+1))*fgw.weights[j-fgw.left];

		//Compute total_weight (i.e. W in the paper)
		//instead of summing from left to right, start from smallest
		//and go to highest weights to prevent roundoff
		fgw.total_weight = 0.0;
		long s = fgw.left;
		long t = fgw.right;
		while (s<t)
		{
			if(fgw.weights[s - fgw.left] <= fgw.weights[t - fgw.left])
			{
				fgw.total_weight += fgw.weights[s-fgw.left];
				s++;
			}
			else
			{
				fgw.total_weight += fgw.weights[t-fgw.left];
				t--;
			}
		}
		fgw.total_weight += fgw.weights[s-fgw.left];
	}
	return fgw;
}

//------------------------------------------------------------------------------

const char* get_export_iterations_filename() {
	return export_iterations_filename;
}
//------------------------------------------------------------------------------
