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
#include <cmath>  // for std::isinf
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
	explicit MeasureSupNorm(bool relative) : relative(relative) {
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
	 * For relative mode, the first value is used as the divisor.
	 */
	inline void measure(double v1, double v2) {
		double x;

		// compute absolute of difference
		x = fabs(v2 - v1);
		if (relative) {
			// for relative mode: divide by second value
			// We take the absolute value of v1 to ensure that
			// x remains non-negative. v1 can become negative e.g.
			// during iterations with over-relaxation with large
			// omega values.
			// Note: if v1 is 0, then x will become +inf for x>0 and NaN for x=0, i.e., v1=v2=0
			// In the later case, the max computation below will ignore the NaN,
			// as NaN > y is false for all y
			x /= fabs(v1);
		}

		// sup_norm = max { x, sup_norm }
		if (x > sup_norm) {
			sup_norm = x;
		}
	}

	/**
	 * Do the measurement for two value arrays of size n.
	 * For relative mode, the values of the first array are used as the divisors.
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

/**
 * Measure for determining the difference between the upper and lower values in
 * an interval iteration.
 * In relative mode, the difference is scaled by the mid-point between upper and lower value.
 */
class MeasureSupNormInterval {
private:
	// relative mode?
	const bool relative;
	// the current maximal value
	double sup_norm;

public:
	/** Constructor, set relative flag */
	explicit MeasureSupNormInterval(bool relative) : relative(relative) {
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
	 * Do the measurement for an upper and lower value pair.
	 * For relative mode, the lower value is used as the divisor.
	 */
	inline void measure(double lower, double upper) {
		double x;

		// special case: one of the values is infinite (this can happen e.g. for non-converging
		// iterations when the values grow extremely large and overflow to infinity).
		if (std::isinf(lower) || std::isinf(upper)) {
			x = std::numeric_limits<double>::infinity();
		} else {
			// compute difference
			// we don't use fabs like for MeasureSupNorm, as we want x to become negative
			// in situations where upper < lower (should only happen due to numerical inaccuracies / rounding)
			x = upper - lower;
			// we clamp to zero for negative values
			if (x < 0)
				x = 0;

			if (relative && x != 0.0) {
				// for relative mode: divide by lower
				// taking lower ensures that if the actual value should happen to be
				// the lower value, that then the relative precision is satisfied.
				// We take the absolute value of the lower to ensure that
				// x does not flip signs.
				// Note: if lower is 0.0, then x will become +inf, as x!=0
				x /= fabs(lower);
			}
		}

		// sup_norm = max { x, sup_norm }
		if (x > sup_norm) {
			sup_norm = x;
		}
	}

	/**
	 * Do the measurement for a pair of arrays (lower and upper bounds) of size n.
	 * For relative mode, the midpoints are used as the divisors.
	 */
	inline void measure(const double *lower, const double *upper, std::size_t n) {
		for (std::size_t i = 0; i < n; i++) {
			measure(lower[i], upper[i]);
		}
	}

	/** Return the measured value */
	double value() const {
		return sup_norm;
	}

};

#endif
