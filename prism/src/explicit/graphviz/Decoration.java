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
	/** An indication of how the node or edge's label should be rendered by Graphviz */
	public static enum LabelType
	{
		/**
		 * Plain text
		 * */
		PLAIN("\"", "\"", "\\n"),

		/**
		 * HTML-like (a restricted subset of HTML supported by Graphviz, as documented in the
		 * <a href="https://graphviz.gitlab.io/_pages/doc/info/shapes.html#html">Node Shapes</a>
		 * section of the Graphviz manual)
		 */
		 HTML("<", ">", "<br/>");

		/** The symbol to start the label text (for this label type) */
		private final String labelOpen;
		/** The symbol to end the label text (for this label type) */
		private final String labelClose;
		/** The symbol for creating a new line (for this label type) */
		private final String newLine;

		/** Constructor, set the correct delimiter symbols */
		private LabelType(String labelOpen, String labelClose, String newLine)
		{
			 this.labelOpen = labelOpen;
			 this.labelClose = labelClose;
			 this.newLine = newLine;
		 }

		/** Returns the symbol for starting the label text */
		public String getOpen()
		{
			return labelOpen;
		}

		/** Returns the symbol for ending the label text */
		public String getClose()
		{
			return labelClose;
		}

		/** Returns the symbol for creating a new line in the label text */
		public String getNewLine()
		{
			return newLine;
		}
	}

	/** The label */
	private String label;
	/** How the label should be rendered by Graphviz (by plain text, as default) */
	private LabelType labelType = LabelType.PLAIN;
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

		append(buf, "label", labelType.getOpen() + label + labelType.getClose());

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

	/** Get the label text */
	public String getLabel()
	{
		return label;
	}

	/** Set the label text. Note: Ensure that the label text is properly quoted for the used label type. */
	public void setLabel(String label)
	{
		this.label = label;
	}

	/** Get the label type */
	public LabelType getLabelType()
	{
		return labelType;
	}

	/**
	 * Set the label type (this should be done before calling the {@link labelAddBelow(String) labelAddBelow}
	 * or {@link labelAddAbove(String) labelAddAbove} methods, to ensure that the correct line delimiter is
	 * inserted)
	 */
	public void setLabelType(LabelType labelType)
	{
		this.labelType = labelType;
	}

	/**
	 * Add some additional information below the currently existing label.
	 * Note: Ensure that the label text is properly quoted for the used label type.
	 */
	public void labelAddBelow(String additional)
	{
		setLabel(getLabel() + labelType.getNewLine() + additional);
	}

	/**
	 * Add some additional information above the currently existing label.
	 * Note: Ensure that the label text is properly quoted for the used label type.
	 * @param additional the additional text
	 */
	public void labelAddAbove(String additional)
	{
		setLabel(additional + labelType.getNewLine() + getLabel());
	}

	/**
	 * Add some additional information to the right of the currently existing label.
	 * Note: Ensure that the label text is properly quoted for the used label type.
	 * @param additional the additional text
	 * @param separator the separator between the old label and the additional text (may be {@code null})
	 */
	public void labelAddRight(String additional, String separator)
	{
		setLabel(getLabel() + (separator == null ? " " : separator) + additional);
	}

	/**
	 * Add some additional information to the left of the currently existing label.
	 * Note: Ensure that the label text is properly quoted for the used label type.
	 * @param additional the additional text
	 * @param separator the separator between the old label and the additional text (may be {@code null})
	 */
	public void labelAddLeft(String additional, String separator)
	{
		setLabel(additional + (separator == null ? " " : separator) + getLabel());
	}

}
