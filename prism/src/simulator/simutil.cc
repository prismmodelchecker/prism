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

#include "simutil.h"
#include <cstdio>
#include <cmath>
#include <iostream>
#include <string>

using std::string;
using std::cout;
using std::endl;

//=============================================================================
// Description
//=============================================================================

/*
 *	This interface provides utility constants and functions used throughout
 *	the simulator engine.
 */


//=============================================================================
//	Local Data
//=============================================================================

string last_error = ""; //Used for reporting errors


//=============================================================================
//	Functions
//=============================================================================

/*
 *	Returns the string representation of number.
 */
string Int_To_String(int number)
{
	if(number == UNDEFINED_INT) return "-";
	char temp [15];
	sprintf(temp, "%d", number);
	return temp;	
}

/*
 *	Returns the string representation of flag.
 */
string Bool_To_String(bool flag)
{
	if(flag) return "true";
	else return "false";
}

/*
 *	Returns the string representation of number.
 */
string Double_To_String(double number)
{
	if(number == 0.0) return "0.0";
	if(number == UNDEFINED_DOUBLE)
		return "-";
	char temp [15];
	sprintf(temp, "%lf", number);
	return temp;
}

/*
 *	Copies the contents of the int array pointed to by original
 *	into the array pointed to by destination.
 */
void Copy_Int_Array(int* original, int* destination, int length)
{
	for(int i = 0; i < length ; i++)
		destination[i] = original[i];
}

/*
 *	Compares the values of the arrays pointed to by arr1 and arr2
 *	and returns true if they are the same.
 */
bool Int_Arrays_Equals(int* arr1, int* arr2, int length)
{
	for(int i = 0; i < length; i++)
		if(arr1[i] != arr2[i]) return false;
	return true;
}

/*
 *	Outputs the contents of arr to the Command Line Console.
 */
void Print_Array(int* arr, int length)
{
	cout << "[";
	for(int i = 0; i < length; i++)
	{
		cout << arr[i];
		if(i != length-1) cout << ", ";
	}
	cout << "]";
}

/*
 *	When an error occurs anywhere in the simulator, this method
 *	can be used to store and track it, for retrieval later.
 */
void Report_Error(string s)
{
	last_error = s;
}

/*
 *	Returns a string representation of the last reported error.
 */
string Get_Last_Error()
{
	 return last_error;
}

