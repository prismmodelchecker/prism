//==============================================================================
//
//	Copyright (c) 2016-
//	Authors:
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham)
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

#ifndef MEASURE_H
#define MEASURE_H

#include <cstddef>  // for std::size_t type
#include <math.h>  // for isinf
#include <limits>

/**
 * Measure: Computes maximum element-wise difference between value vectors.
 * In relative mode, the difference is scaled by the value.
 */
class MeasureSupNorm {
private:
	// relative mode?
	const bool relative;
	// the current maximal value
	double sup_norm;

public:
	/** Constructor, set relative flag */
	MeasureSupNorm(bool relative) : relative(relative) {
		reset();
	}

	/** Reset for new measurement */
	void reset() {
		sup_norm = 0.0;
	}

	/** Relative mode? */
	bool isRelative() const {
		return relative;
	}

	/**
	 * Do the measurement for a single pair of values.
	 * For relative mode, the second value is used as the divisor.
	 */
	inline void measure(double v1, double v2) {
		double x;

		// special case: one of the values is infinite (this can happen e.g. for non-converging
		// iterations when the values grow extremely large and overflow to infinity).
		if (isinf(v1) || isinf(v2)) {
			x = std::numeric_limits<double>::infinity();
		} else {
			// compute absolute of difference
			x = fabs(v2 - v1);
			if (relative) {
				// for relative mode: divide by second value
				// We take the absolute value of v2 to ensure that
				// x remains non-negative. v2 can become negative e.g.
				// during iterations with over-relaxation with large
				// omega values.
				// Note: if v2 is 0, then x will become +inf for x>0 and NaN for x=0, i.e., v1=v2=0
				// In the later case, the max computation below will ignore the NaN,
				// as NaN > y is false for all y
				x /= fabs(v2);
			}
		}

		// sup_norm = max { x, sup_norm }
		if (x > sup_norm) {
			sup_norm = x;
		}
	}

	/**
	 * Do the measurement for two value arrays of size n.
	 * For relative mode, the values of the second array are used as the divisors.
	 */
	inline void measure(const double *soln, const double *soln2, std::size_t n) {
		for (std::size_t i = 0; i < n; i++) {
			measure(soln[i], soln2[i]);
		}
	}

	/** Return the measured value */
	double value() const {
		return sup_norm;
	}

};

#endif
