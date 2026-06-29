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
import java.util.function.Consumer;

/**
 * Handler for a single CLI switch.
 * Can be implemented as a lambda or via one of the typed concrete classes
 * ({@link FlagSwitch}, {@link StringSwitch}, {@link IntSwitch},
 * {@link DoubleSwitch}, {@link EnumSwitch}) that encapsulate common
 * argument-consumption patterns.
 */
@FunctionalInterface
interface SwitchHandler
{
	void handle(String sw, ArgConsumer args) throws PrismException;
}

// ── Concrete implementations ─────────────────────────────────────────────────

/** Switch with no argument: just runs an action when the switch is seen. */
class FlagSwitch implements SwitchHandler
{
	@FunctionalInterface
	interface Action { void run() throws PrismException; }

	private final Action action;

	FlagSwitch(Action action) { this.action = action; }

	@Override
	public void handle(String sw, ArgConsumer a) throws PrismException { action.run(); }
}

/** Switch that consumes the next argument as a raw string and passes it to an action. */
class StringSwitch implements SwitchHandler
{
	@FunctionalInterface
	interface Action { void accept(String s) throws PrismException; }

	private final Action action;

	StringSwitch(Action action) { this.action = action; }

	@Override
	public void handle(String sw, ArgConsumer a) throws PrismException { action.accept(a.next(sw)); }
}

/** Switch that consumes the next argument, parses it as an {@code int}, and passes it to an action. */
class IntSwitch implements SwitchHandler
{
	@FunctionalInterface
	interface Action { void accept(int n) throws PrismException; }

	private final Action action;

	IntSwitch(Action action) { this.action = action; }

	@Override
	public void handle(String sw, ArgConsumer a) throws PrismException { action.accept(a.nextInt(sw)); }
}

/** Switch that consumes the next argument, parses it as a {@code double}, and passes it to an action. */
class DoubleSwitch implements SwitchHandler
{
	@FunctionalInterface
	interface Action { void accept(double d) throws PrismException; }

	private final Action action;

	DoubleSwitch(Action action) { this.action = action; }

	@Override
	public void handle(String sw, ArgConsumer a) throws PrismException { action.accept(a.nextDouble(sw)); }
}

/**
 * Switch that consumes the next argument, splits it into {@code <files>:<options>},
 * then calls an {@link Action} with the files portion and a {@link ParseCallback} that
 * triggers the stored {@link OptionParser} at whatever point in the action is appropriate.
 * The split respects Windows path separators ({@code :\}) by skipping them.
 *
 * <p>Call {@link #printOptions} to print sub-option help from the stored parser.
 * Call {@link #handleFilesOnly} to process a files-only argument (no options suffix).
 */
class StringPlusOptionsSwitch implements SwitchHandler
{
	@FunctionalInterface
	interface Action { void accept(String files, ParseCallback parse) throws PrismException; }

	/**
	 * Token passed to an {@link Action} so it can trigger options parsing at the right moment.
	 * The options string is the text after the first {@code :} in the switch argument (empty if none).
	 */
	static final class ParseCallback
	{
		private final OptionParser parser;
		private final String sw;
		private final String options;

		ParseCallback(OptionParser parser, String sw, String options)
		{
			this.parser = parser; this.sw = sw; this.options = options;
		}

		/** The options string as split from the switch argument (may be empty). */
		String options() { return options; }

		/** Parse the options with the stored parser under the switch name. */
		void run() throws PrismException { parser.parse(options, sw); }

		/** Parse an alternative options string (e.g. for legacy separator handling). */
		void run(String overrideOptions) throws PrismException { parser.parse(overrideOptions, sw); }
	}

	private final OptionParser parser;
	private final Action action;

	StringPlusOptionsSwitch(OptionParser parser, Action action)
	{
		this.parser = parser; this.action = action;
	}

	@Override
	public void handle(String sw, ArgConsumer a) throws PrismException
	{
		String[] parts = splitFilesAndOptions(a.next(sw));
		action.accept(parts[0], new ParseCallback(parser, sw, parts[1]));
	}

	/** Process a files-only argument (empty options). Equivalent to handle with no {@code :options} suffix. */
	void handleFilesOnly(String sw, String files) throws PrismException
	{
		action.accept(files, new ParseCallback(parser, sw, ""));
	}

	/** Print the sub-options registered in the stored parser (for use in help blocks). */
	void printOptions(PrismLog log) { parser.printOptions(log); }

	/**
	 * Split a {@code <files>:<options>} argument on the first {@code :} that is not a
	 * Windows path separator ({@code :\}). Returns {@code {files, options}}; options is
	 * empty if no {@code :} is found.
	 */
	static String[] splitFilesAndOptions(String arg)
	{
		int i = arg.indexOf(':');
		while (arg.length() > i + 1 && arg.charAt(i + 1) == '\\')
			i = arg.indexOf(':', i + 1);
		if (i != -1)
			return new String[]{arg.substring(0, i), arg.substring(i + 1)};
		else
			return new String[]{arg, ""};
	}
}

/**
 * Bundles a {@link SwitchHandler} with its help metadata for the unified CLI switch map.
 * Visible entries have a non-null {@code argHint}; hidden entries have {@code argHint == null};
 * blank-line sentinels have {@code primaryName == null}. {@code group} is the section header
 * printed above the first entry in a group. {@code longDesc} is invoked by {@code -help <sw>}.
 */
class SwitchEntry
{
	final SwitchHandler handler;
	final String group;
	final String primaryName;      // null = blank-line sentinel
	final String[] shownAliases;   // aliases displayed in -help output (subset of all registered names)
	final String argHint;          // null = hidden; "" = flag switch; e.g. "<x>", "<file>"
	final String shortText;        // description text; null for hidden/blank-line sentinels
	final Consumer<PrismLog> longDesc;

	SwitchEntry(SwitchHandler handler, String group, String primaryName,
	            String[] shownAliases, String argHint, String shortText,
	            Consumer<PrismLog> longDesc)
	{
		this.handler = handler;
		this.group = group;
		this.primaryName = primaryName;
		this.shownAliases = shownAliases;
		this.argHint = argHint;
		this.shortText = shortText;
		this.longDesc = longDesc;
	}

	/** Print detailed help: auto-generates the "Switch: -name [aliases] [argHint]" header, then delegates to {@code longDesc}. */
	void printLongDesc(PrismLog log)
	{
		StringBuilder header = new StringBuilder("Switch: -").append(primaryName);
		if (shownAliases != null && shownAliases.length > 0) {
			header.append(" (or");
			for (String alias : shownAliases) header.append(" -").append(alias);
			header.append(")");
		}
		if (argHint != null && !argHint.isEmpty())
			header.append(" ").append(argHint);
		log.println(header + "\n");
		longDesc.accept(log);
	}
}

/**
 * Switch that consumes the next argument and dispatches to one of a fixed set of named choices.
 * Throws a descriptive error listing valid options on unknown input.
 * Choices are registered via {@link #when(String, FlagSwitch.Action)} which supports chaining.
 * Aliases (multiple keys for the same action) are supported by calling {@code when()} multiple times.
 */
class EnumSwitch implements SwitchHandler
{
	private final LinkedHashMap<String, FlagSwitch.Action> choices = new LinkedHashMap<>();

	EnumSwitch when(String key, FlagSwitch.Action action)
	{
		choices.put(key, action);
		return this;
	}

	EnumSwitch when(String key1, String key2, FlagSwitch.Action action)
	{
		choices.put(key1, action);
		choices.put(key2, action);
		return this;
	}

	@Override
	public void handle(String sw, ArgConsumer a) throws PrismException
	{
		String v = a.next(sw);
		FlagSwitch.Action action = choices.get(v);
		if (action == null)
			throw new PrismException("Unrecognised option \"" + v + "\" for -" + sw +
				" (options are: " + String.join(", ", choices.keySet()) + ")");
		action.run();
	}
}
