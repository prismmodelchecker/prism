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

package explicit.graphviz;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 * A Decoration stores the attributes (label and style information)
 * that can be attached to a GraphViz node or edge.
 */
public class Decoration
{
	/** The label */
	private String label;
	/** Map from attribute keys to values (apart from the label attribute) */
	private TreeMap<String, String> attributes;

	/** A Decoration that provides default attributes, e.g., node shape */
	private Decoration defaults;

	/** Constructor, no defaults */
	public Decoration()
	{
		this.defaults = null;
		this.label = "";
	}

	/** Constructor, with defaults decoration */
	public Decoration(Decoration defaults)
	{
		this.defaults = defaults;
	}

	/**
	 * Returns a map for accessing attributes (apart from the label attribute)
	 */
	public Map<String, String> attributes()
	{
		if (attributes == null) {
			// If there have been no attributes before, initialise empty map
			attributes = new TreeMap<String, String>();
		}
		return attributes;
	}

	/**
	 * Returns a read-only map for accessing attributes (apart from the label attribute).
	 * <br>
	 * Note: If you only need read access, it might be more efficient to use this
	 * method instead of {@code attributes()}.
	 */
	public Map<String, String> attributesRO()
	{
		if (attributes == null) {
			// If there have been no attributes before, return empty map
			return Collections.emptyMap();
		} else {
			return Collections.unmodifiableMap(attributes);
		}
	}

	/**
	 * Get the effective value for the given key.
	 * <br>
	 * If the key equals "label", returns the label.
	 * Otherwise, if there is an attribute with the given key,
	 * return its value. If there is none, try the defaults
	 * decoration.
	 * @param key the attribute key
	 * @return the value, if there is one ({@code null} otherwise)
	 */
	public String getEffectiveValue(String key)
	{
		if (key.equals("label")) {
			return label;
		}

		String value = attributesRO().get(key);
		if (value == null && defaults != null) {
			return defaults.getEffectiveValue(key);
		}
		return value;
	}

	@Override
	public String toString()
	{
		StringBuffer buf = new StringBuffer();

		append(buf, "label", "\"" + label + "\"");

		for (Entry<String, String> e : attributesRO().entrySet()) {
			if (defaults != null) {
				String defaultValue = defaults.getEffectiveValue(e.getKey());
				if (defaultValue != null && defaultValue.equals(e.getValue())) {
					// skip, as it's the default value and we don't want to pollute the output
					continue;
				}
			}

			String value = e.getValue();
			value = "\"" + value + "\"";
			append(buf, e.getKey(), value);
		}

		if (buf.length() == 0) {
			return "";
		} else {
			return "[" + buf.toString() + "]";
		}
	}

	/** Append the given key/value pair to the StringBuffer, with a ',' if necessary */
	protected void append(StringBuffer buffer, String key, String value)
	{
		if (buffer.length() != 0) {
			buffer.append(",");
		}
		buffer.append(key);
		buffer.append("=");
		buffer.append(value);
	}

	/** Get the label */
	public String getLabel()
	{
		return label;
	}

	/** Set the label */
	public void setLabel(String label)
	{
		this.label = label;
	}

	/** Add some additional information below the currently existing label */
	public void labelAddBelow(String additional)
	{
		setLabel(getLabel() + "\\n" + additional);
	}

	/**
	 * Add some additional information above the currently existing label.
	 * @param additional the additional text
	 */
	public void labelAddAbove(String additional)
	{
		setLabel(additional + "\\n" + getLabel());
	}

	/**
	 * Add some additional information to the right of the currently existing label.
	 * @param additional the additional text
	 * @param separator the separator between the old label and the additional text (may be {@code null})
	 */
	public void labelAddRight(String additional, String separator)
	{
		setLabel(getLabel() + (separator == null ? " " : separator) + additional);
	}

	/**
	 * Add some additional information to the left of the currently existing label.
	 * @param additional the additional text
	 * @param separator the separator between the old label and the additional text (may be {@code null})
	 */
	public void labelAddLeft(String additional, String separator)
	{
		setLabel(additional + (separator == null ? " " : separator) + getLabel());
	}

}
