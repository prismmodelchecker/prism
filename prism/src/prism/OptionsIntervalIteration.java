//==============================================================================
//	
//	Copyright (c) 2017-
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

package prism;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

/** Option handling for interval iteration */
public class OptionsIntervalIteration
{
	/**
	 * The different methods for computing upper bounds for reward computations,
	 * and DEFAULT to choose the default method.
	 */
	public enum BoundMethod {
		DEFAULT,
		VARIANT_1_COARSE,
		VARIANT_1_FINE,
		VARIANT_2,
		DSMPI,
	};

	/** The method for computing upper bounds for reward computations */
	private BoundMethod boundMethod = BoundMethod.DEFAULT;

	/** Verbose upper bound computations? (default false) */
	private boolean boundComputationVerbose = false;

	/** Select midpoint in results? (default true) */
	private boolean resultSelectMidpoint = true;

	/** Check for monotonicity in iterations? (default false) */
	private boolean checkMonotonicity = false;

	/** Enforce monotonicity in iteration from below? (default true) */
	private boolean enforceMonotonicityBelow = true;

	/** Enforce monotonicity in iteration from above? (default true) */
	private boolean enforceMonotonicityAbove = true;

	/** Manual lower bound (default none) */
	private Double manualLowerBound = null;

	/** Manual upper bound (default none) */
	private Double manualUpperBound = null;

	/* List of valid bound methods, for helper texts */
	final private static String boundMethodsList = "default, variant-1-coarse, variant-1-fine, variant-2, dsmpi";

	/** Constructor from options String, throws PrismException on errors */
	public OptionsIntervalIteration(String options) throws PrismException
	{
		fromOptionsString(options);
	}

	/** Static constructor from PrismComponent settings, throws PrismException on errors */
	public static OptionsIntervalIteration from(PrismComponent parent) throws PrismException
	{
		return from(parent.getSettings());
	}

	/** Static constructor from PrismSettings, throws PrismException on errors */
	public static OptionsIntervalIteration from(PrismSettings settings) throws PrismException
	{
		return new OptionsIntervalIteration(settings.getString(PrismSettings.PRISM_INTERVAL_ITER_OPTIONS));
	}

	/** Validate an options String, throws PrismException on errors */
	public static void validate(String optionsString) throws PrismException
	{
		new OptionsIntervalIteration(optionsString);
	}

	/** The method for computing upper bounds for reward computations */
	public BoundMethod getBoundMethod()
	{
		return boundMethod;
	}

	/** Verbose upper bound computations? */
	public boolean isBoundComputationVerbose()
	{
		return boundComputationVerbose;
	}

	/** Select midpoint in results? */
	public boolean isSelectMidpointForResult()
	{
		return resultSelectMidpoint;
	}

	/** Check for monotonicity in iterations? */
	public boolean isCheckMonotonicity()
	{
		return checkMonotonicity;
	}

	/** Enforce monotonicity in iteration from below? */
	public boolean isEnforceMonotonicityFromBelow()
	{
		return enforceMonotonicityBelow;
	}

	/** Enforce monotonicity in iteration from above? */
	public boolean isEnforceMonotonicityFromAbove()
	{
		return enforceMonotonicityAbove;
	}

	/** Is there a manual lower bound? */
	public boolean hasManualLowerBound()
	{
		return manualLowerBound != null;
	}

	/** Get manual lower bound (or null) */
	public Double getManualLowerBound()
	{
		return manualLowerBound;
	}

	/** Is there a manual upper bound? */
	public boolean hasManualUpperBound()
	{
		return manualUpperBound != null;
	}

	/** Get manual upper bound (or null) */
	public Double getManualUpperBound()
	{
		return manualUpperBound;
	}

	/** Get helper text for the options */
	public static String getOptionsDescription()
	{
		StringBuffer sb = new StringBuffer();
		sb.append(" boundmethod=<x>     Select upper bound heuristic for reward computations\n");
		sb.append("   <x> is one of " + boundMethodsList + "\n");
		sb.append(" lower=<d>           Manually specify lower bound for reward computations (double value)\n");
		sb.append(" upper=<d>           Manually specify upper bound for reward computations (double value)\n");
		sb.append(" [no]boundverbose    Verbose output for upper bound computations (default = no)\n");
		sb.append(" [no]selectmidpoint  Select midpoint between upper and lower as the result (default = yes)\n");
		sb.append(" [no]monotonicbelow  Enforce monotonicity in iteration from below (default = yes)\n");
		sb.append(" [no]monotonicabove  Enforce monotonicity in iteration from above (default = yes)\n");
		sb.append(" [no]checkmonotonic  Check monotonicity, for testing (default = no)\n");
		sb.append("\nExample: boundmethod=default,upper=3.0,noselectmidpoint,checkmonotonic\n");

		return sb.toString();
	}

	/** Parse options string, throw on error */
	private void fromOptionsString(String options) throws PrismException
	{
		for (Entry<String, String> entry : splitOptionsString(options)){
			String option = entry.getKey();
			String extra = entry.getValue();

			boolean isBooleanOption = true;
			switch (option) {
			case   "boundverbose":
			case "noboundverbose":
				boundComputationVerbose = !option.startsWith("no");
				break;
			case   "selectmidpoint":
			case "noselectmidpoint":
				resultSelectMidpoint = !option.startsWith("no");
				break;
			case   "checkmonotonic":
			case "nocheckmonotonic":
				checkMonotonicity = !option.startsWith("no");
				break;
			case   "monotonicbelow":
			case "nomonotonicbelow":
				enforceMonotonicityBelow = !option.startsWith("no");
				break;
			case   "monotonicabove":
			case "nomonotonicabove":
				enforceMonotonicityAbove = !option.startsWith("no");
				break;
			case "lower":
			case "upper": {
				if (extra == null)
					throw new PrismException("Missing argument to interval iteration option '" + option + "'");
				try {
					Double value = Double.parseDouble(extra);
					if (option.equals("lower")) {
						manualLowerBound = value;
					} else {
						manualUpperBound = value;
					}
				} catch (NumberFormatException e) {
					throw new PrismException("Illegal argument to interval iteration option '" + option + "': " + e.getMessage());
				}
				isBooleanOption = false;
				break;
			}
			case "boundmethod": {
				if (extra == null)
					throw new PrismException("Missing argument to interval iteration option '" + option + "'");
				switch (extra) {
				case "default":
					boundMethod = BoundMethod.DEFAULT;
					break;
				case "variant-1-coarse":
					boundMethod = BoundMethod.VARIANT_1_COARSE;
					break;
				case "variant-1-fine":
					boundMethod = BoundMethod.VARIANT_1_FINE;
					break;
				case "variant-2":
					boundMethod = BoundMethod.VARIANT_2;
					break;
				case "dsmpi":
					boundMethod = BoundMethod.DSMPI;
					break;
				default:
					throw new PrismException("Unknown argument to interval iteration option '" + option + "', expected one of "
							+ boundMethodsList);
				}
				isBooleanOption = false;
				break;
			}
			default:
				throw new PrismException("Unknown interval iteration option '" + option + "'");
			}

			if (isBooleanOption) {
				if (extra != null) {
					throw new PrismException("Interval iteration option '" + option + "' has additional argument (" + extra + "), but is boolean option");
				}
			}
		}
	}

	/** Split options string into list of pairs */
	private static List<Pair<String,String>> splitOptionsString(String options)
	{
		List<Pair<String,String>> list = new ArrayList<>();
		if ("".equals(options))
			return list;

		for (String option : options.split(",")) {
			int j = option.indexOf("=");
			if (j == -1) {
				list.add(new Pair<>(option.trim(), null));
			} else {
				list.add(new Pair<>(option.substring(0,j).trim(), option.substring(j+1).trim()));
			}
		}
		return list;
	}
}
