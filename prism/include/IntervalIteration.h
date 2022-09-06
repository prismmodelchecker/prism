//==============================================================================
//
//	Copyright (c) 2016-
//	Authors:
//	* Joachim Klein <klein@tcs.inf.tu-dresden.de> (TU Dresden)
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

#ifndef INTERVAL_ITERATION_H
#define INTERVAL_ITERATION_H

#include <cmath>

// helper class for interval iteration
class IntervalIteration {
private:
	bool ensure_monotonic_from_below;
	bool ensure_monotonic_from_above;
	bool select_midpoint;

public:
	explicit IntervalIteration(int flags) {
		ensure_monotonic_from_below = (flags & 0x01);
		ensure_monotonic_from_above = (flags & 0x02);
		select_midpoint = (flags & 0x04);
	}

	bool flag_ensure_monotonic_from_below() const
	{
		return ensure_monotonic_from_below;
	}

	bool flag_ensure_monotonic_from_above() const
	{
		return ensure_monotonic_from_above;
	}

	bool flag_select_midpoint() const
	{
		return select_midpoint;
	}

	// Store the appropriate value into 'target', optionally ensuring monotonicity
	inline void updateValueFromBelow(double& target, double old_value, double new_value) const
	{
		if (!ensure_monotonic_from_below) {
			target = new_value;
		} else {
			// from below: do max
			// the 'if' part below fails if either old_value or new_value is NaN,
			// in which case new_value is taken
			target = (old_value > new_value ? old_value : new_value);
		}
	}

	// Store the appropriate value into 'target', optionally ensuring monotonicity
	inline void updateValueFromAbove(double& target, double old_value, double new_value) const
	{
		if (!ensure_monotonic_from_above) {
			target = new_value;
		} else {
			// from above: do min
			// the 'if' part below fails if either old_value or new_value is NaN,
			// in which case new_value is taken
			target = (old_value < new_value ? old_value : new_value);
		}
	}

	// store the appropriate value into 'target', optionally ensuring monotonicity
	inline void updateValue(double& target, double old_value, double new_value, bool from_below)
	{
		if (from_below) {
			updateValueFromBelow(target, old_value, new_value);
		} else {
			updateValueFromAbove(target, old_value, new_value);
		}
	}

	// Ensure monotonicity for the given solution vectors (iteration from below).
	// Compares old and new values, overwrites value in 'new_values' if old value was larger.
	// n = number of elements
	inline void ensureMonotonicityFromBelow(double *old_values, double *new_values, int n) const
	{
		for (int i = 0; i < n; i++) {
			updateValueFromBelow(*new_values, *old_values, *new_values);
			old_values++;
			new_values++;
		}
	}

	// Ensure monotonicity for the given solution vectors (iteration from above).
	// Compares old and new values, overwrites value in 'new_values' if old value was smaller.
	// n = number of elements
	inline void ensureMonotonicityFromAbove(double *old_values, double *new_values, int n) const
	{
		for (int i = 0; i < n; i++) {
			updateValueFromAbove(*new_values, *old_values, *new_values);
			old_values++;
			new_values++;
		}
	}

	// Compute midpoint between solution from below and solution from above,
	// store result in soln_below
	// n = number of states / size of solution vectors
	inline void selectMidpoint(double* soln_below, double* soln_above, int n)
	{
		for (int i = 0; i < n; i++) {
			if (soln_above[i] != soln_below[i]) {
				// use x + ( y - x ) / 2 instead of (x+y)/2 for better numerical
				// stability
				double d = soln_below[i] + ( (soln_above[i] - soln_below[i]) / 2.0 );
				if (d <= soln_above[i] && d >= soln_below[i]) {
					// only store result if between soln_below and soln_above to
					// guard against rounding problems,
					// fallback is to simply return soln_below as is
					soln_below[i] = d;
				}
			}
		}
	}

};

#endif
