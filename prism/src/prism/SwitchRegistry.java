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

import java.util.Map;
import java.util.function.Consumer;

/**
 * Accumulator for the unified CLI switch map, shared between {@link PrismCL} and
 * {@link PrismSettings} during one-time initialisation of the switch registry.
 * Tracks the current help section so callers need only call {@link #beginGroup}
 * when the section changes.
 *
 * <p>The visible {@code addSwitch} overloads separate the argument hint (e.g. {@code "<x>"},
 * {@code ""} for flags) from the description text. The primary name and any shown aliases are
 * recorded in the {@link SwitchEntry} so {@code printHelp()} can generate the left column and
 * dot-padding automatically.
 */
class SwitchRegistry
{
	private static final String[] NO_ALIASES = new String[0];

	private final Map<String, SwitchEntry> map;
	private String currentGroup = null;

	SwitchRegistry(Map<String, SwitchEntry> map) { this.map = map; }

	/** Begin a new help section. Subsequent visible entries are listed under this header. */
	void beginGroup(String group) { currentGroup = group; }

	// ── Hidden (not listed in -help) ─────────────────────────────────────────

	void addSwitch(String name, SwitchHandler h)
	{
		map.put(name, new SwitchEntry(h, currentGroup, name, NO_ALIASES, null, null, null));
	}

	void addSwitch(String n1, String n2, SwitchHandler h)
	{
		SwitchEntry e = new SwitchEntry(h, currentGroup, n1, NO_ALIASES, null, null, null);
		map.put(n1, e); map.put(n2, e);
	}

	void addSwitch(String n1, String n2, String n3, SwitchHandler h)
	{
		SwitchEntry e = new SwitchEntry(h, currentGroup, n1, NO_ALIASES, null, null, null);
		map.put(n1, e); map.put(n2, e); map.put(n3, e);
	}

	// ── Visible flag switch (argHint = "", no argument) ──────────────────────

	void addSwitch(String name, SwitchHandler h, String shortText)
	{
		map.put(name, new SwitchEntry(h, currentGroup, name, NO_ALIASES, "", shortText, null));
	}

	void addSwitch(String n1, String n2, SwitchHandler h, String shortText)
	{
		SwitchEntry e = new SwitchEntry(h, currentGroup, n1, new String[]{n2}, "", shortText, null);
		map.put(n1, e); map.put(n2, e);
	}

	void addSwitch(String n1, String n2, String n3, SwitchHandler h, String shortText)
	{
		SwitchEntry e = new SwitchEntry(h, currentGroup, n1, new String[]{n2, n3}, "", shortText, null);
		map.put(n1, e); map.put(n2, e); map.put(n3, e);
	}

	// ── Visible switch with argument ──────────────────────────────────────────

	void addSwitch(String name, SwitchHandler h, String argHint, String shortText)
	{
		map.put(name, new SwitchEntry(h, currentGroup, name, NO_ALIASES, argHint, shortText, null));
	}

	void addSwitch(String n1, String n2, SwitchHandler h, String argHint, String shortText)
	{
		SwitchEntry e = new SwitchEntry(h, currentGroup, n1, new String[]{n2}, argHint, shortText, null);
		map.put(n1, e); map.put(n2, e);
	}

	void addSwitch(String n1, String n2, String n3, SwitchHandler h, String argHint, String shortText)
	{
		SwitchEntry e = new SwitchEntry(h, currentGroup, n1, new String[]{n2, n3}, argHint, shortText, null);
		map.put(n1, e); map.put(n2, e); map.put(n3, e);
	}

	// ── Visible flag with detailed help ──────────────────────────────────────

	void addSwitch(String name, SwitchHandler h, String shortText, Consumer<PrismLog> longDesc)
	{
		map.put(name, new SwitchEntry(h, currentGroup, name, NO_ALIASES, "", shortText, longDesc));
	}

	void addSwitch(String n1, String n2, SwitchHandler h, String shortText, Consumer<PrismLog> longDesc)
	{
		SwitchEntry e = new SwitchEntry(h, currentGroup, n1, new String[]{n2}, "", shortText, longDesc);
		map.put(n1, e); map.put(n2, e);
	}

	// ── Visible switch with argument and detailed help ────────────────────────

	void addSwitch(String name, SwitchHandler h, String argHint, String shortText, Consumer<PrismLog> longDesc)
	{
		map.put(name, new SwitchEntry(h, currentGroup, name, NO_ALIASES, argHint, shortText, longDesc));
	}

	void addSwitch(String n1, String n2, SwitchHandler h, String argHint, String shortText, Consumer<PrismLog> longDesc)
	{
		SwitchEntry e = new SwitchEntry(h, currentGroup, n1, new String[]{n2}, argHint, shortText, longDesc);
		map.put(n1, e); map.put(n2, e);
	}

	/** Insert a blank line in the {@code -help} output at this position. */
	void addBlankLine()
	{
		map.put("__blank_" + map.size(), new SwitchEntry(null, null, null, NO_ALIASES, null, null, null));
	}

	/**
	 * Add a second, doc-only listing of an already-registered flag switch under the current group
	 * (e.g. a switch relevant to two different sections). Purely for {@code -help} output;
	 * does not affect dispatch since it is stored under a synthetic key.
	 */
	void addSwitchAlias(String name, String[] shownAliases, String shortText)
	{
		map.put("__alias_" + map.size(), new SwitchEntry(null, currentGroup, name, shownAliases, "", shortText, null));
	}
}
