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

#ifndef _Included_simulator_Randomheader
#define _Included_simulator_Randomheader

//=============================================================================
//	Functions
//=============================================================================

//Use this function to instantiate implementations of CRandomGenerator
void Set_Generator(int generatorID);

//start the generator's pseudo-random number stream with the given seed
void Seed_Generator(unsigned long seed_value);
//start the generator's pseudo-random number stream with the system
//clock
void Seed_Generator_With_System_Clock();

//Uniformly select an integer inclusively from this range
//using the current generator
int Random_Uniform_From_Range(int start, int end);

//Select an index from the given probability distribution
//using the current generator
int Random_From_Prob_Distribution(double* distribution, int size);

//Return a uniformly generated random double between 0 and 1
//using the current generator
double Random_Uniform();

//============================================================================
//	Class Definitions
//============================================================================

//Interface for random number generator implementations
class CRandomGenerator
{
public:
	//start the Pseudo-random number stream with the given seed
	virtual void Seed(unsigned long seed_value)=0;

	//uniformly select an integer inclusively from this range
	virtual int Random_Uniform_From_Range(int start, int end)=0;

	//Return a uniformly generated random double between 0 and 1
	virtual double Random_Uniform()=0;

	//Select an index from the given probability distribution
	virtual int Random_From_Prob_Distribution(double* distribution, int size)=0;

};

class CStandardRandomGenerator : public CRandomGenerator
{
public:
	void Seed(unsigned long seed_value);

	int Random_Uniform_From_Range(int start, int end);

	double Random_Uniform();

	int Random_From_Prob_Distribution(double* distribution, int size);
private:
	int Prob_Distribution_Helper(int start, double max_prob, double* distribution, int size);
};

#endif
