//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Andrew Hinton <ug60axh@cs.bham.uc.uk> (University of Birmingham)
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

#include "simengine.h"
#include "simiohandler.h"
#include "simsampling.h"
#include "simpctl.h"
#include <stdio.h>
#include <cstdlib>

using std::cout;
using std::endl;

int main(int argc, char* argv[])
{

	cout << "Welcome to the PRISM simulator!!!!" << endl;
	if(argc < 6 || argc > 8)
	{
		cout << "usage: prismsimulator <inputfile> <outputfile> <no_iterations> <max_path_length> <controlfile> (<progressfile>) (<resultsprogressfile>)" << endl;
	}
	else if(argc == 6)
	{
		if (Import_Engine_From_Binary_File(argv[1]) != -1)
		{
			int no_iterations = atoi(argv[3]);
			int max_path_length = atoi(argv[4]);
			Setup_Control_File(argv[5]);
			Engine_Do_Sampling(no_iterations, max_path_length);
			Export_Results_To_File(argv[2]);
			Engine_Tidy_Up_Everything();
		}
	}
	else if(argc == 7)
	{
		if (Import_Engine_From_Binary_File(argv[1]) != -1)
		{
			int no_iterations = atoi(argv[3]);
			int max_path_length = atoi(argv[4]);
			Setup_Control_File(argv[5]);
			Setup_For_Feedback(argv[6]);
			Engine_Do_Sampling(no_iterations, max_path_length);
			Export_Results_To_File(argv[2]);
			Engine_Tidy_Up_Everything();
		}
	}
	else if(argc == 8)
	{
		if (Import_Engine_From_Binary_File(argv[1]) != -1)
		{
			int no_iterations = atoi(argv[3]);
			int max_path_length = atoi(argv[4]);
			Setup_Control_File(argv[5]);
			Setup_For_Feedback(argv[6]);
			Setup_For_Results_Feedback(argv[7]);
			Engine_Do_Sampling(no_iterations, max_path_length);
			Export_Results_To_File(argv[2]);
			Engine_Tidy_Up_Everything();
		}
	}
}

