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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Builder for parsing a comma-separated list of sub-options within a CLI switch argument
 * (e.g. the {@code opt1,key=val,...} portion of {@code -switch files:opt1,key=val,...}).
 * Handles four option styles: bare flags, boolean {@code key=true|false}, fixed-choice
 * {@code key=val}, and raw-string {@code key=anything}.
 *
 * <p>Each registration method has three variants:
 * <ul>
 *   <li><b>Action-only</b> — e.g. {@code flag(name, action)}: registers an execution handler
 *       but no help metadata; use for hidden/alias options.
 *   <li><b>Description-only</b> — e.g. {@code flag(name, desc)}: records help metadata
 *       for {@link #printOptions} but registers no handler; use when building a
 *       description-only parser for {@code -help} output that is separate from the live parser.
 *   <li><b>Combined</b> — e.g. {@code flag(name, desc, action)}: registers both handler
 *       and help metadata in one call; the preferred form when the same parser instance
 *       is used for both parsing and help output.
 * </ul>
 *
 * <p>All error messages include the option name and switch name automatically.
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

	/** One entry in the human-readable option table printed by {@link #printOptions}. */
	private static class HelpEntry
	{
		final String type;         // "flag", "bool", "string", "choice"
		final String name;
		final String argHint;      // null for flag/bool; "<n>" for string; "a/b/c" for choice
		final String description;

		HelpEntry(String type, String name, String argHint, String description)
		{
			this.type = type; this.name = name; this.argHint = argHint; this.description = description;
		}
	}

	private final LinkedHashMap<String, FlagAction>   flagHandlers  = new LinkedHashMap<>();
	private final LinkedHashMap<String, ValueHandler> valueHandlers = new LinkedHashMap<>();
	private final List<HelpEntry>                     helpEntries   = new ArrayList<>();

	// ── Flag options ──────────────────────────────────────────────────────────

	/** Register a bare flag option (action only, no help metadata). */
	OptionParser flag(String name, FlagAction action)
	{
		flagHandlers.put(name, action);
		return this;
	}

	/** Record a bare flag option for help output only (no execution handler). */
	OptionParser flag(String name, String description)
	{
		helpEntries.add(new HelpEntry("flag", name, null, description));
		return this;
	}

	/** Register a bare flag option with both execution handler and help metadata. */
	OptionParser flag(String name, String description, FlagAction action)
	{
		flagHandlers.put(name, action);
		helpEntries.add(new HelpEntry("flag", name, null, description));
		return this;
	}

	// ── String options ────────────────────────────────────────────────────────

	/** Register a {@code key=value} option (action only; action receives the raw value string). */
	OptionParser string(String name, StringAction action)
	{
		valueHandlers.put(name, (key, val, sw) -> action.accept(val));
		return this;
	}

	/** Record a {@code key=<argHint>} string option for help output only. */
	OptionParser string(String name, String argHint, String description)
	{
		helpEntries.add(new HelpEntry("string", name, argHint, description));
		return this;
	}

	/** Register a {@code key=value} option with both execution handler and help metadata. */
	OptionParser string(String name, String argHint, String description, StringAction action)
	{
		valueHandlers.put(name, (key, val, sw) -> action.accept(val));
		helpEntries.add(new HelpEntry("string", name, argHint, description));
		return this;
	}

	// ── Toggle options ────────────────────────────────────────────────────────

	/**
	 * Register a {@code [no]name} boolean toggle: bare {@code name} calls {@code action(true)},
	 * bare {@code noname} calls {@code action(false)}.
	 */
	OptionParser toggle(String name, String description, BoolAction action)
	{
		flagHandlers.put(name,        () -> action.accept(true));
		flagHandlers.put("no" + name, () -> action.accept(false));
		helpEntries.add(new HelpEntry("flag", "[no]" + name, null, description));
		return this;
	}

	// ── Boolean options ───────────────────────────────────────────────────────

	/** Register a {@code key=true|false} option (action only). */
	OptionParser bool(String name, BoolAction action)
	{
		valueHandlers.put(name, (key, val, sw) -> {
			if (val.equals("true"))       action.accept(true);
			else if (val.equals("false")) action.accept(false);
			else throw new PrismException("Unknown value \"" + val + "\" for \"" + key + "\" option of -" + sw
					+ " (expected true or false)");
		});
		return this;
	}

	/** Record a {@code key=true|false} option for help output only. */
	OptionParser bool(String name, String description)
	{
		helpEntries.add(new HelpEntry("bool", name, null, description));
		return this;
	}

	/** Register a {@code key=true|false} option with both execution handler and help metadata. */
	OptionParser bool(String name, String description, BoolAction action)
	{
		bool(name, action);
		helpEntries.add(new HelpEntry("bool", name, null, description));
		return this;
	}

	// ── Choice options ────────────────────────────────────────────────────────

	/** Register a {@code key=val} option where {@code val} must be one of a fixed set (action only). */
	OptionParser choice(String name, Choice c)
	{
		valueHandlers.put(name, c.toHandler());
		return this;
	}

	/** Record a choice option for help output only; {@code validValues} are the displayed accepted values. */
	OptionParser choice(String name, String description, String... validValues)
	{
		helpEntries.add(new HelpEntry("choice", name, String.join("/", validValues), description));
		return this;
	}

	/**
	 * Register a {@code key=val} choice option with both execution handler and help metadata.
	 * The displayed valid values are derived from the primary key of each {@link Choice#when} entry
	 * (the first argument when multiple keys are given as aliases).
	 */
	OptionParser choice(String name, String description, Choice c)
	{
		valueHandlers.put(name, c.toHandler());
		helpEntries.add(new HelpEntry("choice", name, String.join("/", c.primaryKeys()), description));
		return this;
	}

	// ── Help output ───────────────────────────────────────────────────────────

	/**
	 * Print the registered help entries as a human-readable option list.
	 * Only entries added via the description-only or combined overloads appear.
	 * Typical output:
	 * <pre>
	 *  * csv - Export results as comma-separated values
	 *  * format (=explicit/matlab/dot) - model export format
	 *  * precision (=&lt;n&gt;) - use n significant figures
	 *  * rewards (=true/false) - whether to include rewards
	 * </pre>
	 */
	void printOptions(PrismLog log)
	{
		for (HelpEntry e : helpEntries) {
			StringBuilder sb = new StringBuilder(" * ").append(e.name);
			switch (e.type) {
			case "flag":   /* nothing after name */ break;
			case "bool":   sb.append(" (=true/false)"); break;
			case "string": // fall-through
			case "choice": sb.append(" (=").append(e.argHint).append(")"); break;
			}
			sb.append(" - ").append(e.description);
			log.println(sb.toString());
		}
	}

	// ── Parsing ───────────────────────────────────────────────────────────────

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
				FlagAction fa = flagHandlers.get(opt);
				if (fa != null) { fa.run(); continue; }
				if (valueHandlers.containsKey(opt))
					throw new PrismException("No value provided for \"" + opt + "\" option of -" + switchName);
				throw new PrismException("Unknown option \"" + opt + "\" for -" + switchName + " switch");
			} else {
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

	// ── Choice builder ────────────────────────────────────────────────────────

	/**
	 * Builder for a fixed set of enumerated values for a {@link #choice} option.
	 * The first key in each {@code when()} call is the "primary" key shown in help output
	 * via {@link #primaryKeys()}; additional keys in the same call are accepted aliases
	 * but are not displayed.
	 */
	static class Choice
	{
		private final LinkedHashMap<String, FlagAction> map         = new LinkedHashMap<>();
		private final List<String>                      primaryKeys = new ArrayList<>();

		/** Register a single accepted value and its action. */
		Choice when(String key, FlagAction action)
		{
			primaryKeys.add(key);
			map.put(key, action);
			return this;
		}

		/** Register a primary value and one alias; both trigger the same action. */
		Choice when(String primary, String alias, FlagAction action)
		{
			primaryKeys.add(primary);
			map.put(primary, action);
			map.put(alias, action);
			return this;
		}

		/** Register a primary value and two aliases; all three trigger the same action. */
		Choice when(String primary, String alias1, String alias2, FlagAction action)
		{
			primaryKeys.add(primary);
			map.put(primary, action);
			map.put(alias1, action);
			map.put(alias2, action);
			return this;
		}

		/** Returns the primary key of each registered choice, in registration order. */
		List<String> primaryKeys() { return primaryKeys; }

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
