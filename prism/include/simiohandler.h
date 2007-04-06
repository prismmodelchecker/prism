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

#ifndef _Included_simulator_Ioheader
#define _Included_simulator_Ioheader

//Constants
const int STOP_SAMPLING = 1;
//const int NEXT_DUMMY = 2;
//const int NEXT_DUMMY = 4;

//Function Prototypes

void Write_Length_And_String(char* str, int fd);

int Export_Engine_To_Binary_File(const char* filename);

int Export_Results_To_File(char* filename);

int Import_Engine_From_Binary_File(char* filename);

int Setup_For_Feedback(char* feedbackFile);

int Setup_For_Results_Feedback(char* results_feedback_file);

bool Should_Give_Feedback();

int Write_Feedback(int done, int total, bool finished);

//returns the file descriptor
int Start_Writing_Feedback();

//returns a value which can be queried via logical operators with the above constants
int Poll_Control_File();

int Setup_Control_File(char*);

#endif
