//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Andrew Hinton <ug60axh@cs.bham.uc.uk> (University of Birmingham)
//	* Dave Parker <dxp@cs.bham.uc.uk> (University of Birmingham)
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

#define VERSION "3.1"

#include "simiohandler.h"
#include "simstate.h"
#include "simmodel.h"
#include "simpctl.h"
#include "simsampling.h"
#include "simengine.h"
#ifdef _WIN32
#include <io.h>
#else
#include <unistd.h>
#endif
#include <fcntl.h>

//==============================================================================
//	Local Data
//==============================================================================

char* feedback_file;
bool do_feedback = false;

char* results_feedback_file;
bool do_results_feedback = false;

char* control_file_name;

//==============================================================================
//	Functions
//==============================================================================

int Export_Results_To_File(char* filename)
{
	int fd;
	char buf_str[256];

	//open file
	fd = open(filename, O_WRONLY | O_CREAT | O_TRUNC 
#ifdef _WIN32
		| O_BINARY
#endif
		, 00600);

	Write_Sampling_Results(fd);

	close(fd);

	return 0;
}

int Export_Engine_To_Binary_File(const char* filename)
{
	int fd; //file descriptor;
	char buf_str[256]; //buffer for writing strings to

	//open file
	fd = open(filename, O_WRONLY | O_CREAT | O_TRUNC
#ifdef _WIN32
		| O_BINARY
#endif
		, 00600);

	//write header
	write(fd, "PRISM\0", 5+1);
	Write_Length_And_String(VERSION, fd);
	Write_Length_And_String("SimEngine", fd);

	//write state information
	Write_State_Space(fd);
	//write model information
	Write_Model(fd);
	//write pctl information
	Write_Pctl_Manager(fd);
	//write sampling information
	Write_Sampling(fd);
	//write path information

	//write formulae information

	close(fd);

	return 0;
}

int Import_Engine_From_Binary_File(char* filename)
{
	//cout << "Attempting to import engine" << endl;
	try
	{
		//get a clean slate to work with
		//Engine_Tidy_Up_Everything();
		//cout << "import1" << endl;
		//cout << "Engine tidied" << endl;
		int fd;
		int buf_int;
		char buf_str[256];
		//cout << "import2" << endl;
		fd = open(filename, O_RDONLY 
#ifdef _WIN32
			| O_BINARY
#endif 
			);
		//cout << "import3" << endl;
		//cout << "file open: " << fd << endl;

		//read header
		read(fd, &buf_str, 5+1);
		if(strcmp(buf_str, "PRISM") != 0)
		{
			//corrupt header
			throw "Error when importing binary file: corrupt header";
		}
		//cout << "import4" << endl;
		//read version
		read(fd, &buf_int, sizeof(int));
		read(fd, &buf_str, buf_int+1);
		if(strcmp(buf_str, VERSION) != 0)
		{
			//wrong versions
			throw "Error when importing binary file: incorrect versions";
		}
		//cout << "version: " << buf_str << endl;
		//read file identifier
		read(fd, &buf_int, sizeof(int));
		read(fd, &buf_str, buf_int+1);
		if(strcmp(buf_str, "SimEngine") != 0)
		{
			//wrong filetype
			throw "Error when importing binary file: this file is not a simulator engine binary";
		}
		//cout << "import5" << endl;
		//Read State Space
		Read_State_Space(fd);
		//cout << "import5a" << endl;
		//Read Model
		Read_Model(fd);
		//cout << "import5b" << endl;
		//Read PCTL information
		Read_Pctl(fd);
		//cout << "import5c" << endl;
		//Read Sampling information
		Read_Sampling(fd);
		//cout << "import6" << endl;
		close(fd);
		//cout << "import7" << endl;
		Allocate_Reasoning();
		Allocate_Updater();
		//cout << "import8" << endl;
	}
	catch(char* str)
	{
		cout << "Error caught" << endl;
		cout << str << endl;
		return -1;
	}


	return 0;
}

int Setup_For_Feedback(char* feedback)
{
	feedback_file = feedback;
	do_feedback = true;
}

int Setup_For_Results_Feedback(char* results_feedback_f)
{
	results_feedback_file = results_feedback_f;
	do_results_feedback = true;
	cout << "called setup" << endl;
}


bool Should_Give_Feedback()
{
	return do_feedback;
}

int Write_Feedback(int done, int total, bool feedback)
{
	if(do_feedback)
	{
		try
		{
			cout << "Writing normal feedback" << done << "/" << total << endl;

			int fd; //file descriptor;

			//open file
			fd = open(feedback_file, O_WRONLY | O_CREAT | O_TRUNC
#ifdef _WIN32
				| O_BINARY
#endif
				, 00600);

			/*int buf = done;

			write(fd, &buf, sizeof(int));
			buf = total;
			write(fd, &buf, sizeof(int));
			buf = feedback?1:0;
			write(fd, &buf, sizeof(int));  */


			char str[256];
			
				sprintf(str, "%d", done);
				write(fd, str, strlen(str));
				write(fd, "\n", 1);

				sprintf(str, "%d", total);
				write(fd, str, strlen(str));
				write(fd, "\n", 1);
			
				sprintf(str, "%d", feedback?1:0);
				write(fd, str, strlen(str));
				write(fd, "\n", 1);


			close(fd);

			
		}
		catch(char* str)
		{
			cout << "Error caught" << endl;
			cout << str << endl;
			do_feedback = false;
			return -1;
		}
	}

	if(do_results_feedback)
	{
		 try
		{
			cout << "Writing results feedback" << results_feedback_file << "/" << total << endl;

			int fd; //file descriptor;

			//open file
			fd = open(results_feedback_file, O_WRONLY | O_CREAT | O_TRUNC
#ifdef _WIN32
				| O_BINARY
#endif
				, 00600);

			/*int buf = done;

			write(fd, &buf, sizeof(int));
			buf = total;
			write(fd, &buf, sizeof(int));
			buf = feedback?1:0;
			write(fd, &buf, sizeof(int));  */

			Write_Sampling_Results(fd);

			close(fd);

			return 0;
		}
		catch(char* str)
		{
			cout << "Error caught" << endl;
			cout << str << endl;
			do_feedback = false;
			return -1;
		}
	}

	return 0;
}

int Setup_Control_File(char* control_file)
{
	control_file_name = control_file;
	return 0;
}

int Poll_Control_File()
{
	// if there is no control file set up, don't poll
	if (!control_file_name) return  0;
	// otherwise poll
	try
	{
		//get a clean slate to work with
		//Engine_Tidy_Up_Everything();
		//cout << "import1" << endl;
		//cout << "Engine tidied" << endl;
		int fd;
		int buf_int;
		char buf_str[256];
		//cout << "import2" << endl;
		fd = open(control_file_name, O_RDONLY 
#ifdef _WIN32
			| O_BINARY
#endif 
			);
		//cout << "import3" << endl;
		//cout << "file open: " << fd << endl;

		//read four letter word
		read(fd, &buf_str, 4);
                buf_str[4] = '\0';
		if(strcmp(buf_str, "STOP") == 0)
		{
			cout << "I SPOTTED THE STOP COMMAND" << endl;
			return STOP_SAMPLING;
		}
		else return 0;
		
	}
	catch(char* str)
	{
		return 0;
	}

}

//Helper functions

void Write_Length_And_String(char* str, int fd)
{
	char buf_str[256];
	strcpy(buf_str, str);
	int string_length = strlen(buf_str);
	write(fd, &string_length, sizeof(int)); //write the length of the string
	write(fd, &buf_str, string_length); //write the string itself
	write(fd, "\0", 1); // finish will a null byte

}


