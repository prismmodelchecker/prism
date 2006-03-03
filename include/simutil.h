//==============================================================================
//	
//	Copyright (c) 2004-2005, Andrew Hinton
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
#ifndef _Included_simulator_Utilheader
#define _Included_simulator_Utilheader

#define debug(a)  //cout a  

#include <cstdio>
#include <string>

using std::string;


//=============================================================================
// Description
//=============================================================================

/*
 *	This interface provides utility constants and functions used throughout
 *	the simulator engine.
 */

//=============================================================================
//	Constants
//=============================================================================

const int ERROR = -1;
const int OUTOFRANGE = -1;
const int NOT_LOADED = 0;
const int PROBABILISTIC = 1;
const int NONDETERMINISTIC = 2;
const int STOCHASTIC = 3;
const int UNDEFINED_INT = -2147483647L;
const double UNDEFINED_DOUBLE = -1.0000000138484279E24;
const int INTEGER = 1;
const int DOUBLE = 2;
const int BOOLEAN = 3;

//=============================================================================
//	Functions
//=============================================================================

/*
 *	Returns the string representation of number.
 */
string Int_To_String(int number);

/*
 *	Returns the string representation of flag.
 */
string Bool_To_String(bool flag);

/*
 *	Returns the string representation of number.
 */
string Double_To_String(double number);

/*
 *	Copies the contents of the int array pointed to by original
 *	into the array pointed to by destination.
 */
void Copy_Int_Array(int* original, int* destination,int length);

/*
 *	Compares the values of the arrays pointed to by arr1 and arr2
 *	and returns true if they are the same.
 */
bool Int_Arrays_Equals(int* arr1, int* arr2, int length);

/*
 *	Outputs the contents of arr to the Command Line Console.
 */
void Print_Array(int* arr, int length);

/*
 *	When an error occurs anywhere in the simulator, this method
 *	can be used to store and track it, for retrieval later.
 */
void Report_Error(string s);

/*
 *	Returns a string representation of the last reported error.
 */
string Get_Last_Error();

#endif
