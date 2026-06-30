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

	private OptionsIntervalIteration() {} // use field defaults (for buildParser dummy target)

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

	/** Get helper text for the options (used by GUI settings tooltip). */
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

	/** Print the options using {@link OptionParser} formatting (for CLI -help output). */
	public static void printOptions(PrismLog log)
	{
		parser().printOptions(log);
		log.println("\nExample: boundmethod=default,upper=3.0,noselectmidpoint,checkmonotonic");
	}

	/**
	 * An {@link OptionParser} for parsing/validating an interval-iteration options string
	 * (built with a throwaway dummy target; for CLI switch registration and -help output).
	 */
	static OptionParser parser()
	{
		return buildParser(new OptionsIntervalIteration());
	}

	/** Parse options string, throw on error */
	private void fromOptionsString(String options) throws PrismException
	{
		buildParser(this).parse(options, "intervaliter");
	}

	/** Build an {@link OptionParser} whose actions set fields on {@code t}. */
	private static OptionParser buildParser(OptionsIntervalIteration t)
	{
		return new OptionParser()
			.choice("boundmethod", "Select upper bound heuristic for reward computations",
				new OptionParser.Choice()
					.when("default",          () -> t.boundMethod = BoundMethod.DEFAULT)
					.when("variant-1-coarse", () -> t.boundMethod = BoundMethod.VARIANT_1_COARSE)
					.when("variant-1-fine",   () -> t.boundMethod = BoundMethod.VARIANT_1_FINE)
					.when("variant-2",        () -> t.boundMethod = BoundMethod.VARIANT_2)
					.when("dsmpi",            () -> t.boundMethod = BoundMethod.DSMPI))
			.string("lower", "<d>", "Manually specify lower bound for reward computations", v -> {
				try { t.manualLowerBound = Double.parseDouble(v); }
				catch (NumberFormatException e) { throw new PrismException("Invalid value for 'lower' option: " + v); }
			})
			.string("upper", "<d>", "Manually specify upper bound for reward computations", v -> {
				try { t.manualUpperBound = Double.parseDouble(v); }
				catch (NumberFormatException e) { throw new PrismException("Invalid value for 'upper' option: " + v); }
			})
			.toggle("boundverbose",   "Verbose output for upper bound computations (default = no)",           v -> t.boundComputationVerbose = v)
			.toggle("selectmidpoint", "Select midpoint between upper and lower as the result (default = yes)", v -> t.resultSelectMidpoint = v)
			.toggle("monotonicbelow", "Enforce monotonicity in iteration from below (default = yes)",          v -> t.enforceMonotonicityBelow = v)
			.toggle("monotonicabove", "Enforce monotonicity in iteration from above (default = yes)",          v -> t.enforceMonotonicityAbove = v)
			.toggle("checkmonotonic", "Check monotonicity, for testing (default = no)",                       v -> t.checkMonotonicity = v);
	}
}
