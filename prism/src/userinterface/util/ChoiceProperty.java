//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Andrew Hinton <ug60axh@cs.bham.ac.uk> (University of Birmingham)
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
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

package userinterface.util;

import java.util.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class ChoiceProperty extends SingleProperty
{
	private ArrayList<String> properties;
	private int currIndex;

	//if this variable is set to false, this means extra choices can be added.
	private boolean fixedSize;

	//Editor
	private DefaultComboBoxModel listModel;
	private JComboBox editor;

	/** Creates a new instance of DoubleProperty */
	public ChoiceProperty(PropertyOwner owner, String name, String[] props, int startIndex)
	{
		this(owner, name, props, startIndex, "");
	}

	public ChoiceProperty(PropertyOwner owner, String name, String[] props, int startIndex, String comment)
	{
		super(owner, name, props[startIndex], "", false, comment);
		properties = new ArrayList<String>();
		for (int i = 0; i < props.length; i++) {
			properties.add(props[i]);
		}
		this.currIndex = startIndex;
		this.fixedSize = true;

		listModel = new DefaultComboBoxModel(props);
		editor = new JComboBox(listModel);
		editor.setEditable(false);
		Font f = editor.getFont();
		editor.setFont(new Font(f.getName(), Font.PLAIN, f.getSize()));
		editor.setBackground(Color.WHITE);
		editor.setBorder(null);

		pan.setLayout(new BorderLayout());
		pan.add(editor, BorderLayout.CENTER);

	}

	public String getValue()
	{
		String val;
		try {
			val = (String) super.getProperty();
		} catch (Exception e) {
			val = properties.get(currIndex);
			try {
				setProperty(val);
			} catch (Exception ex) {
				//do nothing
			}
		}
		return val;
	}

	public int getNumChoices()
	{
		if (properties == null)
			return 0;
		return properties.size();
	}

	public String getChoice(int i)
	{
		return properties.get(i);
	}

	public int getCurrentIndex()
	{
		return currIndex;
	}

	public void setSelectedIndex(int i) throws PropertyException
	{
		currIndex = i;
		setProperty(properties.get(i), true);
	}

	public void setEnabled(boolean enabled)
	{
		super.setEnabled(enabled);

		if (editor != null)
			editor.setEnabled(enabled);
	}

	/*  
	 *  This method first checks whether the property is a string.
	 *  If the string is a valid choice, then that choice is selected
	 *  If not, if this ChoiceProperty is not a fixed size, the new 
	 *  choice is added.  Any problems cause a PropertyException
	 */
	public void setProperty(Object property, boolean notifyObservers) throws PropertyException
	{
		if (properties == null)
			properties = new ArrayList<String>();
		if (listModel == null)
			listModel = new DefaultComboBoxModel();
		if (property instanceof String) {
			String comp = (String) property;
			boolean found = false;
			for (int i = 0; i < getNumChoices(); i++) {
				if (getChoice(i).equals(comp)) {
					currIndex = i;

					super.setProperty(getChoice(i), notifyObservers);
					found = true;
					break;
				}
			}
			if (!found) {
				if (isFixedSize()) {
					throw new PropertyException("Unknown property String");
				} else {
					properties.add(comp);
					currIndex = properties.size() - 1;
					listModel.addElement(comp);
					super.setProperty(getChoice(currIndex), notifyObservers);
				}
			}
		}

		else {
			throw new PropertyException("Value must be a String");
		}

	}

	/**
	 * Getter for property fixedSize.
	 * @return Value of property fixedSize.
	 */
	public boolean isFixedSize()
	{
		return fixedSize;
	}

	/**
	 * Setter for property fixedSize.
	 * @param fixedSize New value of property fixedSize.
	 */
	public void setFixedSize(boolean fixedSize)
	{
		this.fixedSize = fixedSize;
		editor.setEditable(!fixedSize);
	}

	JPanel pan = new JPanel();

	//THIS WILL NEED TO OVERRIDE THE EDITOR
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column)
	{
		pan.setBorder(UIManager.getBorder("Table.focusCellHighlightBorder"));
		return pan;
	}

	public Component getTableCellEditorComponentMulti(JTable table, Object value, boolean isSelected, int row, int column, boolean allTheSame)
	{
		if (allTheSame) {
			return getTableCellEditorComponent(table, value, isSelected, row, column);
		}
		pan.setBorder(UIManager.getBorder("Table.focusCellHighlightBorder"));
		editor.setBackground(new Color(240, 240, 240));

		return pan;
	}

	public String getEditorValue()
	{

		return (String) editor.getSelectedItem();
	}

	public void addListenerToEditor(ActionListener e)
	{
		editor.addActionListener(e);
	}

	public void removeListenerFromEditor(ActionListener e)
	{
		editor.removeActionListener(e);
	}
}
