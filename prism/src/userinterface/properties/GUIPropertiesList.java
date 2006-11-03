//==============================================================================
//	
//	Copyright (c) 2002-2004, Andrew Hinton, Dave Parker
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
import parser.*;
import prism.*;
import javax.swing.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;

/**
 *
 * @author  ug60axh
 */
public class GUIPropertiesList extends JList implements KeyListener
{
	//STATICS
	
	private static int counter = 0;
	
	//ATTRIBUTES
	
	private Prism prism;
	private ModulesFile parsedModel;
	private GUIMultiProperties parent;
	
	private DefaultListModel listModel;
	
	private PictureCellRenderer rend;
	private Font displayFontFast;
	private Color foregroundFast, backgroundFast, warningFast;
	
	//CONSTRUCTORS
	
	/** Creates a new instance of GUIPropertiesList */
	public GUIPropertiesList(Prism prism, GUIMultiProperties parent)
	{
		this.prism = prism;
		this.parsedModel = null;
		this.parent = parent;
		
		listModel = new DefaultListModel();
		setModel(listModel);
		
		rend = new PictureCellRenderer();
		setCellRenderer(rend);
		
		notifySettings(prism.getSettings());
		
		addKeyListener(this);
		setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		
		getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_MASK), "none");
	}
	
	//ACCESS METHODS
	
	public int getNumProperties()
	{
		return listModel.size();
	}
	
	public GUIProperty getProperty(int index)
	{
		return (GUIProperty)listModel.getElementAt(index);
	}
	
	public int getNumValidProperties()
	{
		int total = 0;
		for(int i = 0; i < getNumProperties(); i++)
		{
			GUIProperty gp = getProperty(i);
			if(gp.isValid()) total++;
		}
		return total;
	}
	
	public int getNumSelectedProperties()
	{
		return getSelectedIndices().length;
	}
	
	public ArrayList getSelectedProperties()
	{
		int[] ind = getSelectedIndices();
		ArrayList ps = new ArrayList();
		for(int i = 0; i < ind.length; i++)
			ps.add(getProperty(ind[i]));
		return ps;
	}
	
	public ArrayList getValidSelectedProperties()
	{
		ArrayList gps = new ArrayList();
		ArrayList prs = getSelectedProperties();
		for(int i = 0; i < prs.size(); i++)
		{
			GUIProperty gp = (GUIProperty)prs.get(i);
			if(gp.isValid())
			{
				gps.add(gp);
			}
		}
		return gps;
	}
	
	public ArrayList getValidSimulatableSelectedProperties()
	{
		ArrayList gps = new ArrayList();
		if(parsedModel == null) return gps;
		ArrayList prs = getSelectedProperties();
		for(int i = 0; i < prs.size(); i++)
		{
			GUIProperty gp = (GUIProperty)prs.get(i);
			if(gp.isValidForSimulation())
			{
				gps.add(gp);
			}
		} 

		return gps;
	}
	
	public String getValidSelectedString()
	{
		String str = "";
		ArrayList prs = getValidSelectedProperties();
		for(int i = 0; i < prs.size(); i++)
		{
			str += ((GUIProperty)prs.get(i)).getPropString()+"\n";
		}
		return str;
	}
	
	public int getIndexOf(String id)
	{
		int index = -1;
		for(int i = 0; i < getNumProperties(); i++)
		{
			String str = getProperty(i).getID();
			if(id.equals(str))
			{
				index = i;
				break;
			}
		}
		return index;
	}
	
	//Used for cut and copy
	public String getClipboardString()
	{
		int[]ind = getSelectedIndices();
		String str = "";
		for(int i = 0 ; i < ind.length; i++)
		{
			GUIProperty gp = getProperty(i);
			str+=gp.getPropString();
			if(i != ind.length-1) str+="\n";
		}
		return str;
	}
	   
	public Font getListFont()
	{
		return displayFontFast;
	}
	
	public Color getListFontColor()
	{
		return foregroundFast;
	}
	
	public Color getWarningColor()
	{
		return warningFast;
	}
	
	public Color getSelectionColor()
	{
		return backgroundFast;
	}
	
	//UPDATE METHODS
	
	public void addProperty(String propString, String comment)
	{
		counter++;
		GUIProperty gp = new GUIProperty(prism, "PROPERTY"+counter, propString, comment);
		listModel.addElement(gp);
		reValidate();
	}
	
	
	public void setProperty(int index, String propString, String comment)
	{
		counter++;
		GUIProperty gp = new GUIProperty(prism, "PROPERTY"+counter, propString, comment);
		listModel.setElementAt(gp, index);
		reValidate();
	}
	
	/** Used for pasting */
	public void pastePropertiesString(String str)
	{
		StringTokenizer sto = new StringTokenizer(str, "\n");
		while(sto.hasMoreTokens())
		{
			String token = sto.nextToken();
			
			// Make sure it isn't comment we are pasting
			if (token.indexOf("//") != 0)
				addProperty(token, "");
		}
	}
	
	public void addPropertiesFile(PropertiesFile pf)
	{
		for(int i = 0; i < pf.getNumProperties(); i++)
		{
			String str = pf.getProperty(i).toString();
			String com = pf.getPropertyComment(i);
			addProperty(str, com);
		}
	}
	
	public boolean deleteProperty(int index)
	{
		GUIProperty gp = getProperty(index);
		if(!gp.isBeingEdited())
		{
			listModel.removeElementAt(index);
			return true;
		}
		else return false;
	}
	
	public void deleteSelected()
	{
		while(!isSelectionEmpty())
		{
			boolean deleted = deleteProperty(getSelectedIndex());
			if(!deleted)
			{
				//if not deleted, unselect, so the rest can!!
				int[]ind = getSelectedIndices();
				int[]newInd = new int[ind.length-1];
				int c = 0;
				for(int i = 0; i < ind.length; i++)
				{
					if(ind[i] != getSelectedIndex())
					{
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
		if(getNumProperties() > 0)
		{
			setSelectionInterval(0, getNumProperties()-1);
		}
	}
	
	public void setModulesFile(ModulesFile parsedModel)
	{
		this.parsedModel = parsedModel;
		reValidate();
	}
	
	public void reValidate()
	{
		for(int i = 0; i < getNumProperties(); i++)
		{
			GUIProperty p = getProperty(i);
			p.parse(parsedModel, parent.getConstantsString(), parent.getLabelString());
		}
		repaint();
	}
	
	
	/*public void setListFont(Font f)
	{
		super.setFont(f);
		displayFont = f;
		repaint();
	}
	
	public void setListFontColor(Color c)
	{
		foreground = c;
		repaint();
	}
	
	public void setWarningColor(Color c)
	{
		warning = c;
		repaint();
	}
	
	public void setSelectionColor(Color c)
	{
		background = c;
		repaint();
	}*/
	
	public void resetResults()
	{
		for(int i = 0; i < this.getNumProperties(); i++)
		{
			GUIProperty p = (GUIProperty)listModel.get(i);
			p.setResult(null);
			p.setMethodString(null);
			p.setConstants(null, null);
		}
		repaint();
	}
	
	// convert to string which can be written to a file
	
	public String toFileString(File f, GUIPropConstantList consList, GUIPropLabelList labList)
	{
		int numProp;
		String s, s2[];
		int i, j;
		
		s = "";
		if (consList.getNumConstants() > 0) {
			s += consList.getConstantsString() + "\n";
		}
		if (labList.getNumLabels() > 0) {
			s += labList.getLabelsString() + "\n";
		}
		numProp = getNumProperties();
		for(i = 0; i < numProp; i++)
		{
			GUIProperty gp = getProperty(i);
			if (gp.getComment().length()>0)
				s += PrismParser.slashCommentBlock(gp.getComment());
			s += gp.getPropString() + "\n\n";
		}
		
		return s;
	}
	
	//REQUIRED TO IMPLEMENT KEYLISTENER
	
	public void keyPressed(KeyEvent e)
	{
		if(e.getModifiers() == KeyEvent.CTRL_MASK)
		{
			if(e.getKeyCode() == KeyEvent.VK_C)
			{					
				parent.a_copy();				
			}			
			else if(e.getKeyCode() == KeyEvent.VK_V)
			{
				parent.a_paste();				
			}
			else if(e.getKeyCode() == KeyEvent.VK_X)
			{
				parent.a_cut();				
			}
			else if(e.getKeyCode() == KeyEvent.VK_D)
			{
				parent.a_delete();				
			}
			else if(e.getKeyCode() == KeyEvent.VK_A)
			{
				parent.a_selectAll();
			}			
		}
		if(e.getKeyCode() == KeyEvent.VK_DELETE)
		{
			parent.a_delete();			
		}
	}
	
	public void keyReleased(KeyEvent e)
	{
	}
	
	public void keyTyped(KeyEvent e)
	{
	}
	
	public void notifySettings(PrismSettings settings)
	{
		displayFontFast = settings.getFontColorPair(PrismSettings.PROPERTIES_FONT).f;//new Font("Monospaced", Font.PLAIN, 12);
		super.setFont(displayFontFast);
		foregroundFast = Color.black;
		backgroundFast = new Color(202,225, 255);
		
		warningFast = settings.getColor(PrismSettings.PROPERTIES_WARNING_COLOUR);
		repaint();
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
			setFont(displayFontFast);
			setText(p.getPropString());
			
			// icon
			setIcon(p.getImage());
			
			// foreground/background colours
			if(isSelected)
			{
				setBackground(backgroundFast);
				if(!p.isValid()) setForeground(Color.red);
				else setForeground(foregroundFast);
			}
			else
			{
				if(!p.isValid())
				{
					setBackground(warningFast);
					setForeground(Color.red);
				}
				else
				{
					setBackground(Color.white);
					setForeground(foregroundFast);
				}
			}
			
			if(p.isBeingEdited())
			{
				setBackground(Color.lightGray);
			}
			
			repaint();
			return this;
		}
	}
	
	class BottomBorder implements javax.swing.border.Border
	{
		public Insets getBorderInsets(Component c)
		{
			return new Insets(0,0,0,0);
		}
		
		public boolean isBorderOpaque()
		{
			return true;
		}
		
		public void paintBorder(Component c, Graphics g, int x, int y, int width, int height)
		{
			g.setColor(Color.lightGray);
			g.drawLine(x,(y+height-1), (x+width), (y+height-1));
			
		}
	}
}
