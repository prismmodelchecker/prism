//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Andrew Hinton <ug60axh@cs.bham.ac.uk> (University of Birmingham)
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
//	* Vojtech Forejt <vojtech.forejt@cs.ox.ac.uk> (University of Oxford)
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

package userinterface.properties;

import java.io.*;
import java.util.*;
import java.util.List;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import parser.*;
import parser.ast.*;
import prism.*;

public class GUIPropertiesList extends JList implements KeyListener
{
	//STATICS

	private static int counter = 0;

	//ATTRIBUTES

	private Prism prism;
	private GUIMultiProperties parent;

	private DefaultListModel listModel;

	private PictureCellRenderer rend;

	//CONSTRUCTORS

	/** Creates a new instance of GUIPropertiesList */
	public GUIPropertiesList(Prism prism, GUIMultiProperties parent)
	{
		this.prism = prism;
		this.parent = parent;

		listModel = new DefaultListModel();
		setModel(listModel);

		rend = new PictureCellRenderer();
		setCellRenderer(rend);

		addKeyListener(this);
		setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

		getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), "none");
	}

	/** Override set font to update row heights at same time */
	public void setFont(Font font)
	{
		super.setFont(font);
		// Note: minimum of 20 since icons are 16x16
		if (font != null)
			setFixedCellHeight(Math.max(20, getFontMetrics(font).getHeight() + 4));
	}

	//ACCESS METHODS

	public int getNumProperties()
	{
		return listModel.size();
	}

	/**
	 * Get the ith property in the list.
	 */
	public GUIProperty getProperty(int i)
	{
		return (GUIProperty) listModel.getElementAt(i);
	}
	
	/**
	 * Returns all properties in this list that have
	 * non-null name.
	 */
	public List<GUIProperty> getAllNamedProperties() {
		ArrayList<GUIProperty> ret = new ArrayList<GUIProperty>();
		for (int i = 0; i < getNumProperties(); i++) {
			if (getProperty(i).getName() != null)
				ret.add(getProperty(i));
		}
		
		return ret;
	}
	
	/**
	 * Looks up a property with the specified name and returns it. If
	 * such a property does not exist, returns null;
	 */
	public GUIProperty getPropertyByName(String s) {
		for (int i = 0; i < getNumProperties(); i++) {
			GUIProperty p = getProperty(i);
			if (p.getName() != null && p.getName().equals(s)) {
				return p;
			}
		}
		
		return null;
	}

	/**
	 * Check that all properties in the list are valid.
	 */
	public boolean allPropertiesAreValid()
	{
		for (int i = 0; i < getNumProperties(); i++) {
			if (!getProperty(i).isValid())
				return false;
		}
		return true;
	}

	/**
	 * Get the number of properties currently selected in the list.
	 */
	public int getNumSelectedProperties()
	{
		return getSelectedIndices().length;
	}

	/**
	 * Get a list of the properties currently selected in the list.
	 */
	public ArrayList<GUIProperty> getSelectedProperties()
	{
		ArrayList<GUIProperty> gps = new ArrayList<GUIProperty>();
		int[] ind = getSelectedIndices();
		for (int i = 0; i < ind.length; i++) {
			gps.add(getProperty(ind[i]));
		}
		return gps;
	}

	/**
	 * Check if there are any valid properties currently selected in the list.
	 */
	public boolean existsValidSelectedProperties()
	{
		if (parent.getParsedModel() == null)
			return false;
		int[] ind = getSelectedIndices();
		for (int i = 0; i < ind.length; i++) {
			if (getProperty(ind[i]).isValid()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Get a list of the valid properties currently selected in the list.
	 */
	public ArrayList<GUIProperty> getValidSelectedProperties()
	{
		ArrayList<GUIProperty> gps = new ArrayList<GUIProperty>();
		if (parent.getParsedModel() == null)
			return gps;
		int[] ind = getSelectedIndices();
		for (int i = 0; i < ind.length; i++) {
			GUIProperty gp = getProperty(ind[i]);
			if (gp.isValid()) {
				gps.add(gp);
			}
		}
		return gps;
	}

	/**
	 * Get a string comprising concatenation of all valid properties currently selected in the list
	 * together with all properties these reference (even indirectly). The properties which are not
	 * selected, but referenced, are guarranteed to be first in the string.
	 */
	public String getValidSelectedAndReferencedString()
	{
		String str = "";
		ArrayList<GUIProperty> gps = getValidSelectedProperties();
		
		//strings will contain all relevant named properties, first selected, then refernced
		Vector<String> strings = new Vector<String>(); 
		
		for (GUIProperty p : gps) { 
			//add even null
			strings.add(p.getName());
		}
		
		for (GUIProperty p : gps) { 
			for (String s : p.getReferencedNames())
				if (!strings.contains(s))
					strings.add(s);
		}
		
		Vector<GUIProperty> referencedProps = new Vector<GUIProperty>();

		//turn referenced strings to props.
		int i = gps.size();
		while (i < strings.size()) {
			GUIProperty p = getPropertyByName(strings.get(i));
			if (p != null) {
				referencedProps.add(p);
				for (String s : p.getReferencedNames())
					if (!strings.contains(s))
						strings.add(s);
			} //we don't need to care about null case, parser will find an error later.
			i++;
		}
		
		//add all named properties
		String namedString = "";
		//Add named properties
		for (GUIProperty p : referencedProps) {
				namedString += "\"" + p.getName() + "\" : " + p.getPropString() + "\n";
		}
		
		for (GUIProperty gp : gps) {
			if (gp.getName() != null) {
				str += "\"" + gp.getName() + "\" : ";
			}
			str += gp.getPropString() + "\n";
		}
		return namedString + str;
	}

	/**
	 * Check if there are any valid and simulate-able properties currently selected in the list.
	 */
	public boolean existsValidSimulatableSelectedProperties()
	{
		if (parent.getParsedModel() == null)
			return false;
		int[] ind = getSelectedIndices();
		for (int i = 0; i < ind.length; i++) {
			if (getProperty(ind[i]).isValidForSimulation()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Get a list of the valid and simulate-able properties currently selected in the list.
	 */
	public ArrayList<GUIProperty> getValidSimulatableSelectedProperties()
	{
		ArrayList<GUIProperty> gps = new ArrayList<GUIProperty>();
		if (parent.getParsedModel() == null)
			return gps;
		int[] ind = getSelectedIndices();
		for (int i = 0; i < ind.length; i++) {
			GUIProperty gp = getProperty(ind[i]);
			if (gp.isValidForSimulation()) {
				gps.add(gp);
			}
		}
		return gps;
	}

	public int getIndexOf(String id)
	{
		int index = -1;
		for (int i = 0; i < getNumProperties(); i++) {
			String str = getProperty(i).getID();
			if (id.equals(str)) {
				index = i;
				break;
			}
		}
		return index;
	}

	//Used for cut and copy
	public String getClipboardString()
	{
		int[] ind = getSelectedIndices();
		String str = "";
		for (int i = 0; i < ind.length; i++) {
			GUIProperty gp = getProperty(i);
			str += gp.getPropString();
			if (i != ind.length - 1)
				str += "\n";
		}
		return str;
	}

	/* UPDATE METHODS */
	public void addProperty(String propString, String comment)
	{
		if (propString.matches("\"[^\"]*\"[ ]*:.*")) {
			//the string contains property name
			int start = propString.indexOf('"') + 1;
			int end = propString.indexOf('"', start);
			String name = propString.substring(start,end);
			int colon = propString.indexOf(':') + 1;
			String actualPropString = propString.substring(colon).trim();
			
			addProperty(name, actualPropString, comment);
		} else {
			addProperty(null, propString, comment);
		}
	}

	public void addProperty(String name, String propString, String comment)
	{
		counter++;
		GUIProperty gp = new GUIProperty(prism, this, "PROPERTY" + counter, propString, name, comment);
		listModel.addElement(gp);
		validateProperties();
	}

	public void setProperty(int index, String name, String propString, String comment)
	{
		counter++;
		GUIProperty gp = new GUIProperty(prism, this, "PROPERTY" + counter, propString, name, comment);
		listModel.setElementAt(gp, index);
		validateProperties();
	}

	/** Used for pasting */
	public void pastePropertiesString(String str)
	{
		StringTokenizer sto = new StringTokenizer(str, "\n");
		while (sto.hasMoreTokens()) {
			String token = sto.nextToken();

			// Make sure it isn't comment we are pasting
			if (token.indexOf("//") != 0)
				addProperty(token, "");
		}
	}

	public void addPropertiesFile(PropertiesFile pf)
	{
		for (int i = 0; i < pf.getNumProperties(); i++) {
			String nam = pf.getPropertyName(i);
			String str = pf.getProperty(i).toString();
			String com = pf.getPropertyComment(i);
			addProperty(nam, str, com);
		}
	}

	public boolean deleteProperty(int index)
	{
		GUIProperty gp = getProperty(index);
		if (!gp.isBeingEdited()) {
			listModel.removeElementAt(index);
			validateProperties();
			return true;
		} else
			return false;
	}

	public void deleteSelected()
	{
		while (!isSelectionEmpty()) {
			boolean deleted = deleteProperty(getSelectedIndex());
			if (!deleted) {
				//if not deleted, unselect, so the rest can!!
				int[] ind = getSelectedIndices();
				int[] newInd = new int[ind.length - 1];
				int c = 0;
				for (int i = 0; i < ind.length; i++) {
					if (ind[i] != getSelectedIndex()) {
						newInd[c] = ind[i];
						c++;
					}
				}
				setSelectedIndices(newInd);
			}
		}
	}

	public void deleteAll()
	{
		selectAll();
		deleteSelected();
	}

	public void selectAll()
	{
		if (getNumProperties() > 0) {
			setSelectionInterval(0, getNumProperties() - 1);
		}
	}

	/** Validate all the properties in the list
	    NB: Don't call it "validate()" to avoid overwriting Swing methods */

	public void validateProperties()
	{
		List<GUIProperty> list = new ArrayList<GUIProperty>();
		for (int i = 0; i < getNumProperties(); i++) {
			GUIProperty p = getProperty(i);
			p.makeInvalid();
			list.add(p);
		}
		
		boolean changed;
		do {
			changed = false;
			int i = 0;
			while (i < list.size()) {
				GUIProperty p = list.get(i);
				p.parse(parent.getParsedModel(), parent.getConstantsString(), parent.getLabelsString());
				if (p.isValid()) {
					list.remove(i);
					changed = true;
				} else {
					i++;
				}
			}
		} while (changed && list.size() > 0);
		// Force repaint because we modified a GUIProperty directly
		repaint();
	}

	// convert to string which can be written to a file

	public String toFileString(File f, GUIPropConstantList consList, GUIPropLabelList labList)
	{
		int numProp;
		String s;
		int i;

		s = "";
		if (consList.getNumConstants() > 0) {
			s += consList.getConstantsString() + "\n";
		}
		if (labList.getNumLabels() > 0) {
			s += labList.getLabelsString() + "\n";
		}
		numProp = getNumProperties();
		for (i = 0; i < numProp; i++) {
			GUIProperty gp = getProperty(i);
			if (gp.getComment().length() > 0)
				s += PrismParser.slashCommentBlock(gp.getComment());
			s += gp.getPropString() + "\n\n";
		}

		return s;
	}

	//REQUIRED TO IMPLEMENT KEYLISTENER

	public void keyPressed(KeyEvent e)
	{
		if (e.getModifiers() == KeyEvent.CTRL_MASK) {
			if (e.getKeyCode() == KeyEvent.VK_C) {
				parent.a_copy();
			} else if (e.getKeyCode() == KeyEvent.VK_V) {
				parent.a_paste();
			} else if (e.getKeyCode() == KeyEvent.VK_X) {
				parent.a_cut();
			} else if (e.getKeyCode() == KeyEvent.VK_D) {
				parent.a_delete();
			} else if (e.getKeyCode() == KeyEvent.VK_A) {
				parent.a_selectAll();
			}
		}
		if (e.getKeyCode() == KeyEvent.VK_DELETE) {
			parent.a_delete();
		}
	}

	public void keyReleased(KeyEvent e)
	{
	}

	public void keyTyped(KeyEvent e)
	{
	}

	//RENDERERS

	class PictureCellRenderer extends JLabel implements ListCellRenderer
	{
		String toolTip;

		public PictureCellRenderer()
		{
			toolTip = "";
			setOpaque(true);
		}

		public String getToolTipText()
		{
			return toolTip;
		}

		public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
		{
			setBorder(new BottomBorder());
			GUIProperty p = getProperty(index);

			// tooltip
			toolTip = p.getToolTipText();

			// text
			setText(p.toString());

			// icon
			setIcon(p.getImage());

			// foreground/background colours
			if (isSelected) {
				setBackground(parent.getSelectionColor());
				setForeground(p.isValid() ? Color.black : Color.red);
			} else {
				if (!p.isValid()) {
					setBackground(parent.getWarningColor());
					setForeground(Color.red);
				} else {
					setBackground(Color.white);
					setForeground(Color.black);
				}
			}

			if (p.isBeingEdited()) {
				setBackground(Color.lightGray);
			}

			return this;
		}
	}

	class BottomBorder implements javax.swing.border.Border
	{
		public Insets getBorderInsets(Component c)
		{
			return new Insets(0, 0, 0, 0);
		}

		public boolean isBorderOpaque()
		{
			return true;
		}

		public void paintBorder(Component c, Graphics g, int x, int y, int width, int height)
		{
			g.setColor(Color.lightGray);
			g.drawLine(x, (y + height - 1), (x + width), (y + height - 1));

		}
	}
}
