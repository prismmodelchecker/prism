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
