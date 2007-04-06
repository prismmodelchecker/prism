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

#include "simrandom.h"
#include <cstdlib>
#include <ctime>

//==============================================================================
//	Local variables
//==============================================================================

CRandomGenerator* generator = new CStandardRandomGenerator();

//==============================================================================
//	Local functions
//==============================================================================

void Set_Default_Generator()
{
	generator = new CStandardRandomGenerator();
}

//==============================================================================
//	Functions
//==============================================================================

//Use this function to instantiate implementations of CRandomGenerator
void Set_Generator(int generatorID)
{
	switch(generatorID)
	{
		case 0: Set_Default_Generator();
		//case 1: generator = new NewImplementation1();
		//case 2: generator = new NewImplementation2();
		default: return;//do nothing
	}
}

//start the generator's pseudo-random number stream with the given seed
void Seed_Generator(unsigned long seed_value)
{
	generator->Seed(seed_value);
}
//start the generator's pseudo-random number stream with the system
//clock
void Seed_Generator_With_System_Clock()
{
	//use the time function to get system_clock value
	generator->Seed((unsigned)time(NULL));
}

//Uniformly select an integer inclusively from this range
//using the current generator
int Random_Uniform_From_Range(int start, int end)
{
	return generator->Random_Uniform_From_Range(start, end);
}

//Select an index from the given probability distribution
//using the current generator
int Random_From_Prob_Distribution(double* distribution, int size)
{
	return generator->Random_From_Prob_Distribution(distribution, size);
}

//Return a uniformly generated random double between 0 and 1
//using the current generator
double Random_Uniform()
{
	return generator->Random_Uniform();
}

//===============================================================================
//	Class Definitions
//===============================================================================

//CStandardRandomGenerator
//This provides a default implementation of a random number generator using
//rand and srand from the C Standard Library.  (This may suffer from low 
//period)


inline void CStandardRandomGenerator::Seed(unsigned long seed_value)
{
	srand(seed_value);
}

inline int CStandardRandomGenerator::Random_Uniform_From_Range(int start, int end)
{
	return (int)(start + (this->Random_Uniform()*(end-start)));
}

inline double CStandardRandomGenerator::Random_Uniform()
{
	return (double) rand()/(RAND_MAX+1.0);
}

inline int CStandardRandomGenerator::Random_From_Prob_Distribution
(double* distribution, int size)
{
	return Prob_Distribution_Helper(0, 1.0, distribution, size);
}

int CStandardRandomGenerator::Prob_Distribution_Helper
(int start, double max_prob, double* distribution, int size)
{
	if(start >= size) 
		return start-1; //base case - we are at the end, select last one
	else if(this->Random_Uniform()*max_prob < distribution[start])
		return start;//base case - select this
	else 
		return Prob_Distribution_Helper(start+1, max_prob-distribution[start], 
										distribution, size);	//recursive step, next 
																//index.
}
