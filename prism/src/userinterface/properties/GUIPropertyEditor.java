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

import userinterface.*;
import parser.ast.*;
import prism.*;
import java.awt.event.*;
import javax.swing.*;
import java.awt.*;

public class GUIPropertyEditor extends javax.swing.JDialog implements ActionListener, KeyListener
{
	
	//Constants
	
	private static final int START = 0;
	private static final int CURSOR = 1;
	private static final int END = 2;
	
	//Statics
	private static int noOpen = 0;
	
	//Attributes
	
	private GUIPrism parent;
	private GUIMultiProperties props;
	private ModulesFile parsedModel;
	private boolean dispose = false;
	private String id;
	private int propertyInvalidStrategy = GUIMultiProperties.WARN_INVALID_PROPS;
	
	//Constructors
	
	/** Creates a new GUIPropertyEditor with its parent GUIPrism, a boolean stating
	 * whether the dialog should be modal and a Vector of properties to be displayed
	 * for user browsing/copying.
	 */
	public GUIPropertyEditor(GUIMultiProperties props, ModulesFile parsedModel, int strategy) //Adding constructor
	{
		this(props, parsedModel, null, strategy);
	}
	
	/** Creates a new GUIPropertyEditor with its parent GUIPrism, a boolean stating
	 * whether the dialog should be modal, a Vector of properties to be displayed
	 * for user browsing/copying and a string showing the default value of the
	 * property text box.
	 */
	public GUIPropertyEditor(GUIMultiProperties props, ModulesFile parsedModel, GUIProperty prop, int strategy) //Editing constructor
	{
		super(props.getGUI(), false);
		this.props = props;
		this.parent = props.getGUI();
		this.parsedModel = parsedModel;
		this.propertyInvalidStrategy = strategy;
		initComponents();
		this.getRootPane().setDefaultButton(okayButton);
		setLocationRelativeTo(getParent()); // centre
		//propertyList.setListData(props);
		if(prop == null)
		{
			this.id = "new";
			propertyText.setText("");
			commentTextArea.setText("");
		}
		else
		{
			this.id = prop.getID();
			
			String namePart = (prop.getName() != null) ? ("\"" + prop.getName() + "\" : ") : "";
			
			propertyText.setText(namePart + prop.getPropString());
			commentTextArea.setText(prop.getComment());
		}
		addActionListeners();
		propertyText.setFont(new Font("Monospaced", Font.PLAIN, 12));
		
		setTitle("Property Editor");
		
	}
	
	//Update methods
	
	public void show()
	{
		noOpen++;
		setLocation(getX()+(noOpen*50), getY()+(noOpen*50));
		
		super.show();
		
	}
	
	public void dispose()
	{
		noOpen--;
		super.dispose();
	}
	
	private void addString(String str, int position)
	{
		if(position == START)
		{
			propertyText.setText(str+propertyText.getText());
			propertyText.setCaretPosition(str.length());
		}
		else if(position == END)
		{
			propertyText.setText(propertyText.getText() + str);
		}
		else // position == CURSOR
		{
			int curs = propertyText.getCaretPosition();
			int length = propertyText.getText().length();
			String s = propertyText.getText().substring(0, curs);
			String t = propertyText.getText().substring(curs, length);
			propertyText.setText(s + str + t);
			propertyText.setCaretPosition(s.length()+str.length());
		}
		propertyText.requestFocus();
	}
	
	private void addString(String str, int position, int relativeCursorPosition)
	{
		int curs;
		if(position == START) curs = 0;
		else if(position == END) curs = propertyText.getText().length();
		else curs = propertyText.getCaretPosition(); // must be curs
		addString(str, position);
		propertyText.setCaretPosition(curs+ relativeCursorPosition);
		propertyText.requestFocus();
	}
	private void removeCharAt(int pos)
	{
		String str = propertyText.getText();
		String first = str.substring(0,pos);
		String last = str.substring(pos+1, str.length());
		propertyText.setText(first+last);
	}
	
	//Listener Methods
	
	/** Needed to implement ActionListener interface.  Is called when a button is
	 * pressed.  According to which button is pressed, this adds content to the text
	 * box in an "intelligent" way.
	 * @param e generated by a button press.
	 */
	public void actionPerformed(ActionEvent e)
	{
		if(e.getSource() == trueButton)
		{
			int num = propertyText.getCaretPosition();
			
			if(num > 0 && propertyText.getText().charAt(num-1) == '(')
			{
				if(propertyText.getText().charAt(num) == ')')
				{
					removeCharAt(num-1);
					removeCharAt(num-1);
					propertyText.setCaretPosition(num-1);
					addString("true", CURSOR);
					propertyText.setCaretPosition(num+3);
				}
			}
			else
			{
				propertyText.setCaretPosition(num);
				addString("true", CURSOR);
				propertyText.setCaretPosition(num+4);
			}
		}
		else if(e.getSource() == falseButton)
		{
			int num = propertyText.getCaretPosition();
			
			if(num > 0 && propertyText.getText().charAt(num-1) == '(')
			{
				if(propertyText.getText().charAt(num) == ')')
				{
					removeCharAt(num-1);
					removeCharAt(num-1);
					propertyText.setCaretPosition(num-1);
					addString("false", CURSOR);
					propertyText.setCaretPosition(num+4);
				}
			}
			else
			{
				propertyText.setCaretPosition(num);
				addString("false", CURSOR);
				propertyText.setCaretPosition(num+5);
			}
		}
		else if(e.getSource() == andButton)
		{
			if(propertyText.getSelectedText() == null)
			{
				addString("() & ()", CURSOR, 1);
			}
			else
			{
				int start = propertyText.getSelectionStart();
				int end = propertyText.getSelectionEnd();
				int length = propertyText.getSelectedText().length();
				boolean shouldDo = false;
				int open = 0;
				int closed = 0;
				int opensq = 0;
				int closedsq = 0;
				for(int i = 0; i < end-start; i++)
				{
					if(propertyText.getSelectedText().charAt(i) == '(') open++;
					else if (propertyText.getSelectedText().charAt(i) == ')') closed++;
					else	  if(propertyText.getSelectedText().charAt(i) == '[') opensq++;
					else	  if(propertyText.getSelectedText().charAt(i) == ']') closedsq++;
				}
				if((open == closed) && (opensq == closedsq)) shouldDo = true;
				if(shouldDo)
				{
					propertyText.setCaretPosition(start);
					addString("(", CURSOR, 0);
					propertyText.setCaretPosition(end+1);
					addString(") & ()",CURSOR, 0);
					
					int caret = propertyText.getCaretPosition();
					boolean found = false;
					int i = caret;
					for(i = caret; i < propertyText.getText().length(); i++)
					{
						if(propertyText.getText().charAt(i) == '(')
						{found = true; break;}
					}
					if(found)
					{
						propertyText.setCaretPosition(i+1);
					}
				}
			}
		}
		else if(e.getSource() == orButton)
		{
			if(propertyText.getSelectedText() == null)
			{
				addString("() | ()", CURSOR, 1);
			}
			else
			{
				int start = propertyText.getSelectionStart();
				int end = propertyText.getSelectionEnd();
				int length = propertyText.getSelectedText().length();
				boolean shouldDo = false;
				int open = 0;
				int closed = 0;
				int opensq = 0;
				int closedsq = 0;
				for(int i = 0; i < end-start; i++)
				{
					if(propertyText.getSelectedText().charAt(i) == '(') open++;
					else if (propertyText.getSelectedText().charAt(i) == ')') closed++;
					else	  if(propertyText.getSelectedText().charAt(i) == '[') opensq++;
					else	  if(propertyText.getSelectedText().charAt(i) == ']') closedsq++;
				}
				if((open == closed) && (opensq == closedsq)) shouldDo = true;
				if(shouldDo)
				{
					propertyText.setCaretPosition(start);
					addString("(", CURSOR, 0);
					propertyText.setCaretPosition(end+1);
					addString(") | ()",CURSOR, 0);
					
					int caret = propertyText.getCaretPosition();
					boolean found = false;
					int i = caret;
					for(i = caret; i < propertyText.getText().length(); i++)
					{
						if(propertyText.getText().charAt(i) == '(')
						{found = true; break;}
					}
					if(found)
					{
						propertyText.setCaretPosition(i+1);
					}
				}
			}
		}
		else if(e.getSource() == notButton)
		{
			if(propertyText.getSelectedText() == null)
			{
				addString("!()", CURSOR, 2);
			}
			else
			{
				int start = propertyText.getSelectionStart();
				int end = propertyText.getSelectionEnd();
				int length = propertyText.getSelectedText().length();
				boolean shouldDo = false;
				int open = 0;
				int closed = 0;
				int opensq = 0;
				int closedsq = 0;
				for(int i = 0; i < end-start; i++)
				{
					if(propertyText.getSelectedText().charAt(i) == '(') open++;
					else if (propertyText.getSelectedText().charAt(i) == ')') closed++;
					else	  if(propertyText.getSelectedText().charAt(i) == '[') opensq++;
					else	  if(propertyText.getSelectedText().charAt(i) == ']') closedsq++;
				}
				if((open == closed) && (opensq == closedsq)) shouldDo = true;
				if(shouldDo)
				{
					propertyText.setCaretPosition(start);
					addString("!(", CURSOR, 0);
					propertyText.setCaretPosition(end+2);
					addString(")",CURSOR, 0);
				}
			}
		}
		else if(e.getSource() == nextButton)
		{
			int curs = propertyText.getCaretPosition();
			addString("P><p [ X () ]", CURSOR, 1);
			propertyText.select(curs+1, curs+4);
		}
		else if(e.getSource() == untilButton)
		{
			int curs = propertyText.getCaretPosition();
			addString("P><p [ () U () ]", CURSOR, 1);
			propertyText.select(curs+1, curs+4);
		}
		else if(e.getSource() == boundedUntilButton)
		{
			int curs = propertyText.getCaretPosition();
			addString("P><p [ () U<=k () ]", CURSOR, 1);
			propertyText.select(curs+1, curs+4);
		}
		else if(e.getSource() == steadyStateButton)
		{
			int curs = propertyText.getCaretPosition();
			addString("S><p [ () ]", CURSOR, 1);
			propertyText.select(curs+1, curs+4);
		}
		else if(e.getSource() == cut)
		{
			if(propertyText.getSelectedText() != null)
			{
				propertyText.cut();
				propertyText.requestFocus();
			}
			else
			{
			}
		}
		else if(e.getSource() == copy)
		{
			if(propertyText.getSelectedText() != null)
			{
				propertyText.copy();
				propertyText.requestFocus();
			}
			else
			{
			}
		}
		else if(e.getSource() == paste)
		{
			
			propertyText.paste();
			propertyText.requestFocus();
		}
		if (e.getSource() == okayButton)
		{
			//okayButtonActionPerformed(e);
		}
		else if(e.getSource() == cancelButton)
		{
			setVisible(false);
			props.cancelProperty(id);
			dispose();
		}
		
	}
	
	/** Needed to implement the KeyListener interface.  This method looks for the
	 * 'enter' key.  If the 'enter' key is pressed, the cursor position shifts to the
	 * next sensible location in the property expression.
	 * @param e generated by a key press in the text box
	 */
	public void keyPressed(KeyEvent e)
	{
		
		if(e.getKeyCode() == KeyEvent.VK_ENTER)
		{
			int caret = propertyText.getCaretPosition();
			boolean found = false;
			boolean k	 = false;
			int i = caret;
			for(i = caret; i < propertyText.getText().length(); i++)
			{
				if(propertyText.getText().charAt(i) == '(')
				{found = true; break;}
				if(propertyText.getText().charAt(i) == 'k')
				{found = true; k = true; break;}
			}
			if(found)
			{
				if(k)
				{
					propertyText.setCaretPosition(i);
					propertyText.select(i, i+1);
				}
				else
				{
					propertyText.setCaretPosition(i+1);
				}
			}
		}
		if(e.getKeyCode() == KeyEvent.VK_ESCAPE)
		{
			setVisible(false);
			props.cancelProperty(id);
			dispose();
		}
	}
	
	/** Needed to implement the KeyListener interface.
	 * @param e generated by a key press in the text box
	 */
	public void keyReleased(KeyEvent e)
	{
		
	}
	
	/** Needed to implement the KeyListener interface.
	 * @param e generated by a key press in the text box
	 */
	public void keyTyped(KeyEvent e)
	{
		//System.out.println("keytyped");
		
	}
	
	private void addActionListeners()
	{
		trueButton.addActionListener(this);
		falseButton.addActionListener(this);
		andButton.addActionListener(this);
		orButton.addActionListener(this);
		notButton.addActionListener(this);
		nextButton.addActionListener(this);
		boundedUntilButton.addActionListener(this);
		untilButton.addActionListener(this);
		steadyStateButton.addActionListener(this);
		propertyText.addKeyListener(this);
		this.addKeyListener(this);
		
		cut.addActionListener(this);
		copy.addActionListener(this);
		paste.addActionListener(this);
		
		okayButton.addActionListener(this);
		cancelButton.addActionListener(this);
	}
	
	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    private void initComponents() {//GEN-BEGIN:initComponents
        java.awt.GridBagConstraints gridBagConstraints;

        jPanel1 = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        jPanel5 = new javax.swing.JPanel();
        jPanel6 = new javax.swing.JPanel();
        jToolBar1 = new javax.swing.JToolBar();
        cut = new javax.swing.JButton();
        copy = new javax.swing.JButton();
        paste = new javax.swing.JButton();
        jPanel7 = new javax.swing.JPanel();
        jPanel9 = new javax.swing.JPanel();
        jPanel10 = new javax.swing.JPanel();
        propertyText = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        jPanel8 = new javax.swing.JPanel();
        trueButton = new javax.swing.JButton();
        andButton = new javax.swing.JButton();
        notButton = new javax.swing.JButton();
        untilButton = new javax.swing.JButton();
        steadyStateButton = new javax.swing.JButton();
        falseButton = new javax.swing.JButton();
        orButton = new javax.swing.JButton();
        nextButton = new javax.swing.JButton();
        boundedUntilButton = new javax.swing.JButton();
        jPanel11 = new javax.swing.JPanel();
        jPanel12 = new javax.swing.JPanel();
        jPanel13 = new javax.swing.JPanel();
        jPanel14 = new javax.swing.JPanel();
        jPanel15 = new javax.swing.JPanel();
        jPanel16 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        commentTextArea = new javax.swing.JTextArea();
        jPanel2 = new javax.swing.JPanel();
        okayButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();

        setResizable(false);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                closeDialog(evt);
            }
        });

        jPanel1.setLayout(new java.awt.GridBagLayout());

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        jPanel1.add(jPanel3, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        jPanel1.add(jPanel4, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        jPanel1.add(jPanel5, gridBagConstraints);

        jPanel6.setLayout(new java.awt.BorderLayout());

        jToolBar1.setFloatable(false);
        cut.setIcon(GUIPrism.getIconFromImage("smallCut.png"));
        jToolBar1.add(cut);

        copy.setIcon(GUIPrism.getIconFromImage("smallCopy.png"));
        jToolBar1.add(copy);

        paste.setIcon(GUIPrism.getIconFromImage("smallPaste.png"));
        jToolBar1.add(paste);

        jPanel6.add(jToolBar1, java.awt.BorderLayout.NORTH);

        jPanel7.setLayout(new java.awt.GridBagLayout());

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 0;
        jPanel7.add(jPanel9, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        jPanel7.add(jPanel10, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 10;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        jPanel7.add(propertyText, gridBagConstraints);

        jLabel1.setLabelFor(propertyText);
        jLabel1.setText("Property:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel7.add(jLabel1, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        jPanel7.add(jPanel8, gridBagConstraints);

        trueButton.setText("true");
        trueButton.setPreferredSize(new java.awt.Dimension(123, 25));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.2;
        jPanel7.add(trueButton, gridBagConstraints);

        andButton.setText("And");
        andButton.setPreferredSize(new java.awt.Dimension(123, 25));
        andButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                andButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.2;
        jPanel7.add(andButton, gridBagConstraints);

        notButton.setText("Not");
        notButton.setPreferredSize(new java.awt.Dimension(123, 25));
        notButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                notButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 7;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.2;
        jPanel7.add(notButton, gridBagConstraints);

        untilButton.setText("Until");
        untilButton.setPreferredSize(new java.awt.Dimension(123, 25));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 9;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.2;
        jPanel7.add(untilButton, gridBagConstraints);

        steadyStateButton.setText("Steady-state");
        steadyStateButton.setPreferredSize(new java.awt.Dimension(123, 25));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 11;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.2;
        jPanel7.add(steadyStateButton, gridBagConstraints);

        falseButton.setText("false");
        falseButton.setPreferredSize(new java.awt.Dimension(123, 25));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.2;
        jPanel7.add(falseButton, gridBagConstraints);

        orButton.setText("Or");
        orButton.setPreferredSize(new java.awt.Dimension(123, 25));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.2;
        jPanel7.add(orButton, gridBagConstraints);

        nextButton.setText("Next");
        nextButton.setPreferredSize(new java.awt.Dimension(123, 25));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 7;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.2;
        jPanel7.add(nextButton, gridBagConstraints);

        boundedUntilButton.setText("Bounded Until");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 9;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.2;
        jPanel7.add(boundedUntilButton, gridBagConstraints);

        jPanel7.add(jPanel11, new java.awt.GridBagConstraints());

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 6;
        gridBagConstraints.gridy = 0;
        jPanel7.add(jPanel12, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 8;
        gridBagConstraints.gridy = 0;
        jPanel7.add(jPanel13, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 10;
        gridBagConstraints.gridy = 0;
        jPanel7.add(jPanel14, gridBagConstraints);

        jPanel15.setPreferredSize(new java.awt.Dimension(10, 5));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        jPanel7.add(jPanel15, gridBagConstraints);

        jPanel16.setPreferredSize(new java.awt.Dimension(10, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
        jPanel7.add(jPanel16, gridBagConstraints);

        jLabel2.setText("Comment:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        jPanel7.add(jLabel2, gridBagConstraints);

        commentTextArea.setRows(3);
        jScrollPane1.setViewportView(commentTextArea);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 10;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        jPanel7.add(jScrollPane1, gridBagConstraints);

        jPanel6.add(jPanel7, java.awt.BorderLayout.CENTER);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jPanel1.add(jPanel6, gridBagConstraints);

        getContentPane().add(jPanel1, java.awt.BorderLayout.CENTER);

        jPanel2.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

        okayButton.setMnemonic('O');
        okayButton.setText("Okay");
        okayButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okayButtonActionPerformed(evt);
            }
        });

        jPanel2.add(okayButton);

        cancelButton.setMnemonic('C');
        cancelButton.setText("Cancel");
        jPanel2.add(cancelButton);

        getContentPane().add(jPanel2, java.awt.BorderLayout.SOUTH);
        
        pack();
    }//GEN-END:initComponents

    private void andButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_andButtonActionPerformed
        // Add your handling code here:
    }//GEN-LAST:event_andButtonActionPerformed

    private void notButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_notButtonActionPerformed
        // Add your handling code here:
    }//GEN-LAST:event_notButtonActionPerformed

	private void okayButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_okayButtonActionPerformed
	{//GEN-HEADEREND:event_okayButtonActionPerformed
		boolean valid = false;
		int noLabels = 0;
		int noConstants = 0;
		
		try
		{
			//Parse constants and labels
			PropertiesFile fConLab = props.getPrism().parsePropertiesString(parsedModel,  props.getLabelsString()+"\n"+props.getConstantsString());
			noConstants = fConLab.getConstantList().size();
			noLabels = fConLab.getLabelList().size();
			
			String namedString = "";
			int namedCount = 0;
			//Add named properties
			for (GUIProperty namedProp : this.props.getPropList().getAllNamedProperties()) {
				if (namedProp.isValid() && this.id != null && !this.id.equals(namedProp.getID())) {
					namedCount++;
					namedString += "\"" + namedProp.getName() + "\" : " + namedProp.getPropString() + "\n";
				}
			}
			
			//Parse all together
			String withConsLabs = props.getConstantsString()+"\n"+props.getLabelsString()+namedString+propertyText.getText();
			PropertiesFile ff = props.getPrism().parsePropertiesString(parsedModel, withConsLabs);
			
			//Validation of number of properties
			if(ff.getNumProperties() <= namedCount) throw new PrismException("Empty property");
			else if(ff.getNumProperties() > namedCount + 1) throw new PrismException("Contains multiple properties");
			
			//Validation of constants and labels
			if(ff.getConstantList().size() != noConstants) throw new PrismException("Contains constants");
			if(ff.getLabelList().size() != noLabels) throw new PrismException("Contains labels");
			
			valid = true;
		}
		// catch and deal with exceptions
		catch(prism.PrismException ex)
		{
			switch(propertyInvalidStrategy)
			{
				case GUIMultiProperties.WARN_INVALID_PROPS:
				{
					String[] choices = {"Yes", "No"};
					int choice = -1;
					choice = props.optionPane("Error: "+ex.getMessage()+"\nAre you sure you want to continue?", "Question", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, choices, choices[0]);
					valid = (choice == 0);
					break;
				}
				case GUIMultiProperties.NEVER_INVALID_PROPS:
				{
					parent.errorDialog("Error: "+ex.getMessage());
					valid = false;
					break;
				}
			}
		}
		
		if(valid)
		{
			setVisible(false);
			props.changeProperty(propertyText.getText(), commentTextArea.getText(), id);
			dispose();
		}
	}//GEN-LAST:event_okayButtonActionPerformed
	
	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		props.cancelProperty(id);
		dispose();
	}//GEN-LAST:event_closeDialog

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton andButton;
    private javax.swing.JButton boundedUntilButton;
    private javax.swing.JButton cancelButton;
    private javax.swing.JTextArea commentTextArea;
    private javax.swing.JButton copy;
    private javax.swing.JButton cut;
    private javax.swing.JButton falseButton;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel10;
    private javax.swing.JPanel jPanel11;
    private javax.swing.JPanel jPanel12;
    private javax.swing.JPanel jPanel13;
    private javax.swing.JPanel jPanel14;
    private javax.swing.JPanel jPanel15;
    private javax.swing.JPanel jPanel16;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JToolBar jToolBar1;
    private javax.swing.JButton nextButton;
    private javax.swing.JButton notButton;
    private javax.swing.JButton okayButton;
    private javax.swing.JButton orButton;
    private javax.swing.JButton paste;
    private javax.swing.JTextField propertyText;
    private javax.swing.JButton steadyStateButton;
    private javax.swing.JButton trueButton;
    private javax.swing.JButton untilButton;
    // End of variables declaration//GEN-END:variables
	
}
