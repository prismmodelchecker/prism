//==============================================================================
//
//	Copyright (c) 2026-
//	Authors:
//	* Dave Parker <david.parker@cs.ox.ac.uk> (University of Oxford)
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

import java.util.LinkedHashMap;

/**
 * Builder for parsing a comma-separated list of sub-options within a CLI switch argument
 * (e.g. the {@code opt1,key=val,...} portion of {@code -switch files:opt1,key=val,...}).
 * Handles three option styles: bare flags, boolean {@code key=true|false}, fixed-choice
 * {@code key=val}, and raw-string {@code key=anything}.
 * All error messages include the option name and switch name automatically.
 */
class OptionParser
{
	@FunctionalInterface interface FlagAction   { void run()             throws PrismException; }
	@FunctionalInterface interface StringAction { void accept(String v)  throws PrismException; }
	@FunctionalInterface interface BoolAction   { void accept(boolean b) throws PrismException; }

	/** Internal handler for key=value options. */
	@FunctionalInterface
	private interface ValueHandler
	{
		void accept(String key, String val, String switchName) throws PrismException;
	}

	private final LinkedHashMap<String, FlagAction>   flagHandlers  = new LinkedHashMap<>();
	private final LinkedHashMap<String, ValueHandler> valueHandlers = new LinkedHashMap<>();

	/** Register a bare flag option (no {@code =value}). */
	OptionParser flag(String name, FlagAction action)
	{
		flagHandlers.put(name, action);
		return this;
	}

	/** Register a {@code key=value} option; action receives the raw value string. */
	OptionParser string(String name, StringAction action)
	{
		valueHandlers.put(name, (key, val, sw) -> action.accept(val));
		return this;
	}

	/** Register a {@code key=true|false} option. */
	OptionParser bool(String name, BoolAction action)
	{
		valueHandlers.put(name, (key, val, sw) -> {
			if (val.equals("true"))
				action.accept(true);
			else if (val.equals("false"))
				action.accept(false);
			else
				throw new PrismException("Unknown value \"" + val + "\" for \"" + key + "\" option of -" + sw
						+ " (expected true or false)");
		});
		return this;
	}

	/** Register a {@code key=val} option where {@code val} must be one of a fixed set. */
	OptionParser choice(String name, Choice c)
	{
		valueHandlers.put(name, c.toHandler());
		return this;
	}

	/**
	 * Parse a comma-separated options string, dispatching each token to the registered handler.
	 * @param optionsString comma-separated options (may be empty or null)
	 * @param switchName    CLI switch name used in error messages (without leading {@code -})
	 */
	void parse(String optionsString, String switchName) throws PrismException
	{
		if (optionsString == null || optionsString.isEmpty()) return;
		for (String opt : optionsString.split(",")) {
			if (opt.isEmpty()) continue;
			int eq = opt.indexOf('=');
			if (eq == -1) {
				// Bare option — must be a registered flag
				FlagAction fa = flagHandlers.get(opt);
				if (fa != null) { fa.run(); continue; }
				if (valueHandlers.containsKey(opt))
					throw new PrismException("No value provided for \"" + opt + "\" option of -" + switchName);
				throw new PrismException("Unknown option \"" + opt + "\" for -" + switchName + " switch");
			} else {
				// key=value option
				String key = opt.substring(0, eq);
				String val = opt.substring(eq + 1);
				ValueHandler vh = valueHandlers.get(key);
				if (vh != null) { vh.accept(key, val, switchName); continue; }
				if (flagHandlers.containsKey(key))
					throw new PrismException("Option \"" + key + "\" for -" + switchName + " does not take a value");
				throw new PrismException("Unknown option \"" + key + "\" for -" + switchName + " switch");
			}
		}
	}

	/**
	 * Builder for a fixed set of enumerated values for a {@link #choice} option.
	 * Mirrors the fluent {@code .when()} style of {@link EnumSwitch}.
	 */
	static class Choice
	{
		private final LinkedHashMap<String, FlagAction> map = new LinkedHashMap<>();

		/** Register a single accepted value and its action. */
		Choice when(String key, FlagAction action)
		{
			map.put(key, action);
			return this;
		}

		/** Register two accepted values (aliases) that trigger the same action. */
		Choice when(String k1, String k2, FlagAction action)
		{
			map.put(k1, action);
			map.put(k2, action);
			return this;
		}

		/** Register three accepted values (aliases) that trigger the same action. */
		Choice when(String k1, String k2, String k3, FlagAction action)
		{
			map.put(k1, action);
			map.put(k2, action);
			map.put(k3, action);
			return this;
		}

		/** Build the {@link ValueHandler} that dispatches on the registered values. */
		ValueHandler toHandler()
		{
			return (key, val, sw) -> {
				FlagAction action = map.get(val);
				if (action == null)
					throw new PrismException("Unknown value \"" + val + "\" for \"" + key + "\" option of -" + sw
							+ " (options are: " + String.join(", ", map.keySet()) + ")");
				action.run();
			};
		}
	}
}
