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

package settings;

import java.awt.*;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.event.*;

public class FontChooser extends javax.swing.JDialog implements ListSelectionListener, ActionListener, FocusListener, ChangeListener
{
    
    protected Font lastFont;
    protected Color lastColor;
    
    
    protected Font defaultFont;
    protected Color defaultColor;
    
    protected boolean shouldReturn;
    
    
    /** Creates new form FontChooser */
    public FontChooser(java.awt.Frame parent)
    {
        super(parent, true);
        initComponents();
        this.getRootPane().setDefaultButton(okayButton);
        previewLabel.setBackground(Color.white);
        doListModels();
        doListeners();
        shouldReturn = true;
		setLocationRelativeTo(getParent()); // centre
    }
    
    public FontChooser(Dialog parent)
    {
        super(parent, true);
        initComponents();
        previewLabel.setBackground(Color.white);
        doListModels();
        doListeners();
        shouldReturn = true;
    }
    
    public static FontColorPair getFont(Dialog parent, Font startFont, Color startColor, Font defaultFont, Color defaultColor)
    {
        FontChooser choose = new FontChooser(parent);
        
        choose.shouldReturn = true;
        choose.defaultFont = defaultFont;
        choose.defaultColor = defaultColor;
        choose.lastColor = startColor;
        choose.lastFont = startFont;
        choose.colorChooser.setColor(choose.lastColor);
        choose.setFont(choose.lastFont);
        choose.updatePreview();
        
        choose.show();
        
        FontColorPair pair = new FontColorPair();
        pair.f = choose.lastFont;
        pair.c = choose.lastColor;
        
        if(choose.shouldReturn)return pair;
        else return null;
    }
    
    public static FontColorPair getFont(Frame parent, Font startFont, Color startColor, Font defaultFont, Color defaultColor)
    {
        FontChooser choose = new FontChooser(parent);
        
        choose.shouldReturn = true;
        choose.defaultFont = defaultFont;
        choose.defaultColor = defaultColor;
        choose.lastColor = startColor;
        choose.lastFont = startFont;
        choose.colorChooser.setColor(choose.lastColor);
        choose.setFont(choose.lastFont);
        choose.updatePreview();
        
        choose.show();
        
        FontColorPair pair = new FontColorPair();
        pair.f = choose.lastFont;
        pair.c = choose.lastColor;
        
        if(choose.shouldReturn)return pair;
        else return null;
    }
    
    private void doListModels()
    {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String [] allFonts = ge.getAvailableFontFamilyNames();
        //Font[]allFonts = ge.getAllFonts();
        String [] styles =
        { "Plain", "Bold", "Italic", "Bold Italic" };
        String [] sizes =
        { "8","9","10","11","12","14","16","18","20" };
        
        DefaultComboBoxModel fontModel = new DefaultComboBoxModel(allFonts);
        DefaultComboBoxModel styleModel = new DefaultComboBoxModel(styles);
        DefaultComboBoxModel sizeModel = new DefaultComboBoxModel(sizes);
        
        fontList.setModel(fontModel);
        styleList.setModel(styleModel);
        sizeList.setModel(sizeModel);
    }
    
    private void doListeners()
    {
        fontList.addListSelectionListener(this);
        styleList.addListSelectionListener(this);
        sizeList.addListSelectionListener(this);
        
        fontBox.addActionListener(this);
        styleBox.addActionListener(this);
        sizeBox.addActionListener(this);
        
        okayButton.addActionListener(this);
        cancelButton.addActionListener(this);
        defaultButton.addActionListener(this);
        
        fontPanel.addFocusListener(this);
        theTabs.addChangeListener(this);
        
    }
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    private void initComponents()//GEN-BEGIN:initComponents
    {
        theTabs = new javax.swing.JTabbedPane();
        fontPanel = new javax.swing.JPanel();
        jPanel35 = new javax.swing.JPanel();
        jPanel36 = new javax.swing.JPanel();
        jPanel37 = new javax.swing.JPanel();
        jPanel38 = new javax.swing.JPanel();
        jPanel39 = new javax.swing.JPanel();
        fontBox = new javax.swing.JTextField();
        jPanel40 = new javax.swing.JPanel();
        jPanel41 = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        jPanel42 = new javax.swing.JPanel();
        jPanel43 = new javax.swing.JPanel();
        jScrollPane4 = new javax.swing.JScrollPane();
        fontList = new javax.swing.JList();
        jPanel44 = new javax.swing.JPanel();
        jPanel45 = new javax.swing.JPanel();
        jPanel46 = new javax.swing.JPanel();
        jPanel47 = new javax.swing.JPanel();
        styleBox = new javax.swing.JTextField();
        jPanel48 = new javax.swing.JPanel();
        jPanel49 = new javax.swing.JPanel();
        jLabel5 = new javax.swing.JLabel();
        jPanel50 = new javax.swing.JPanel();
        jPanel51 = new javax.swing.JPanel();
        jScrollPane5 = new javax.swing.JScrollPane();
        styleList = new javax.swing.JList();
        jPanel52 = new javax.swing.JPanel();
        jPanel53 = new javax.swing.JPanel();
        jPanel54 = new javax.swing.JPanel();
        sizeBox = new javax.swing.JTextField();
        jPanel55 = new javax.swing.JPanel();
        jPanel56 = new javax.swing.JPanel();
        jLabel6 = new javax.swing.JLabel();
        jPanel57 = new javax.swing.JPanel();
        jPanel58 = new javax.swing.JPanel();
        jScrollPane6 = new javax.swing.JScrollPane();
        sizeList = new javax.swing.JList();
        jPanel59 = new javax.swing.JPanel();
        jPanel60 = new javax.swing.JPanel();
        previewLabel = new javax.swing.JLabel();
        colorChooser = new javax.swing.JColorChooser();
        jPanel32 = new javax.swing.JPanel();
        jPanel33 = new javax.swing.JPanel();
        defaultButton = new javax.swing.JButton();
        jPanel34 = new javax.swing.JPanel();
        okayButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();

        setResizable(false);
        addWindowListener(new java.awt.event.WindowAdapter()
        {
            public void windowClosing(java.awt.event.WindowEvent evt)
            {
                closeDialog(evt);
            }
        });

        fontPanel.setLayout(new java.awt.BorderLayout());

        jPanel35.setLayout(new java.awt.BorderLayout());

        jPanel35.setMaximumSize(new java.awt.Dimension(450, 2147483647));
        jPanel36.setLayout(new java.awt.BorderLayout());

        jPanel36.setMaximumSize(new java.awt.Dimension(450, 100));
        jPanel36.setPreferredSize(new java.awt.Dimension(450, 100));
        jPanel37.setLayout(new javax.swing.BoxLayout(jPanel37, javax.swing.BoxLayout.X_AXIS));

        jPanel38.setLayout(new java.awt.BorderLayout());

        jPanel38.setPreferredSize(new java.awt.Dimension(200, 183));
        jPanel39.setLayout(new java.awt.BorderLayout());

        jPanel39.add(fontBox, java.awt.BorderLayout.CENTER);

        jPanel40.setMinimumSize(new java.awt.Dimension(5, 10));
        jPanel40.setPreferredSize(new java.awt.Dimension(5, 10));
        jPanel39.add(jPanel40, java.awt.BorderLayout.WEST);

        jPanel41.setLayout(new java.awt.BorderLayout());

        jLabel4.setDisplayedMnemonic('F');
        jLabel4.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel4.setLabelFor(fontList);
        jLabel4.setText("Font:");
        jPanel41.add(jLabel4, java.awt.BorderLayout.CENTER);

        jPanel42.setMinimumSize(new java.awt.Dimension(5, 10));
        jPanel42.setPreferredSize(new java.awt.Dimension(5, 10));
        jPanel41.add(jPanel42, java.awt.BorderLayout.WEST);

        jPanel39.add(jPanel41, java.awt.BorderLayout.NORTH);

        jPanel38.add(jPanel39, java.awt.BorderLayout.NORTH);

        jPanel43.setLayout(new java.awt.BorderLayout());

        jPanel43.setPreferredSize(new java.awt.Dimension(269, 100));
        jScrollPane4.setViewportView(fontList);

        jPanel43.add(jScrollPane4, java.awt.BorderLayout.CENTER);

        jPanel43.add(jPanel44, java.awt.BorderLayout.WEST);

        jPanel38.add(jPanel43, java.awt.BorderLayout.CENTER);

        jPanel37.add(jPanel38);

        jPanel45.setLayout(new javax.swing.BoxLayout(jPanel45, javax.swing.BoxLayout.X_AXIS));

        jPanel45.setPreferredSize(new java.awt.Dimension(100, 163));
        jPanel46.setLayout(new java.awt.BorderLayout());

        jPanel47.setLayout(new java.awt.BorderLayout());

        jPanel47.add(styleBox, java.awt.BorderLayout.CENTER);

        jPanel48.setMinimumSize(new java.awt.Dimension(5, 10));
        jPanel48.setPreferredSize(new java.awt.Dimension(5, 10));
        jPanel47.add(jPanel48, java.awt.BorderLayout.WEST);

        jPanel49.setLayout(new java.awt.BorderLayout());

        jLabel5.setDisplayedMnemonic('y');
        jLabel5.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel5.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel5.setLabelFor(styleList);
        jLabel5.setText("Font style:");
        jPanel49.add(jLabel5, java.awt.BorderLayout.CENTER);

        jPanel50.setMinimumSize(new java.awt.Dimension(5, 10));
        jPanel50.setPreferredSize(new java.awt.Dimension(5, 10));
        jPanel49.add(jPanel50, java.awt.BorderLayout.WEST);

        jPanel47.add(jPanel49, java.awt.BorderLayout.NORTH);

        jPanel46.add(jPanel47, java.awt.BorderLayout.NORTH);

        jPanel51.setLayout(new java.awt.BorderLayout());

        jPanel51.setPreferredSize(new java.awt.Dimension(269, 100));
        jScrollPane5.setViewportView(styleList);

        jPanel51.add(jScrollPane5, java.awt.BorderLayout.CENTER);

        jPanel51.add(jPanel52, java.awt.BorderLayout.WEST);

        jPanel46.add(jPanel51, java.awt.BorderLayout.CENTER);

        jPanel45.add(jPanel46);

        jPanel37.add(jPanel45);

        jPanel53.setLayout(new java.awt.BorderLayout());

        jPanel53.setPreferredSize(new java.awt.Dimension(100, 163));
        jPanel54.setLayout(new java.awt.BorderLayout());

        jPanel54.add(sizeBox, java.awt.BorderLayout.CENTER);

        jPanel55.setMinimumSize(new java.awt.Dimension(5, 10));
        jPanel55.setPreferredSize(new java.awt.Dimension(5, 10));
        jPanel54.add(jPanel55, java.awt.BorderLayout.WEST);

        jPanel56.setLayout(new java.awt.BorderLayout());

        jLabel6.setDisplayedMnemonic('S');
        jLabel6.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel6.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel6.setLabelFor(sizeList);
        jLabel6.setText("Size:");
        jPanel56.add(jLabel6, java.awt.BorderLayout.CENTER);

        jPanel57.setMinimumSize(new java.awt.Dimension(5, 10));
        jPanel57.setPreferredSize(new java.awt.Dimension(5, 10));
        jPanel56.add(jPanel57, java.awt.BorderLayout.WEST);

        jPanel54.add(jPanel56, java.awt.BorderLayout.NORTH);

        jPanel53.add(jPanel54, java.awt.BorderLayout.NORTH);

        jPanel58.setLayout(new java.awt.BorderLayout());

        jPanel58.setPreferredSize(new java.awt.Dimension(269, 100));
        jScrollPane6.setViewportView(sizeList);

        jPanel58.add(jScrollPane6, java.awt.BorderLayout.CENTER);

        jPanel58.add(jPanel59, java.awt.BorderLayout.WEST);

        jPanel53.add(jPanel58, java.awt.BorderLayout.CENTER);

        jPanel37.add(jPanel53);

        jPanel36.add(jPanel37, java.awt.BorderLayout.CENTER);

        jPanel35.add(jPanel36, java.awt.BorderLayout.CENTER);

        jPanel60.setLayout(new java.awt.BorderLayout());

        jPanel60.setBorder(new javax.swing.border.TitledBorder(null, "Preview", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 0, 12)));
        previewLabel.setBackground(new java.awt.Color(255, 255, 255));
        previewLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        previewLabel.setText("AaBbCcDdEeFf123456789!\"\u00a3$%^");
        previewLabel.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 0, 0)));
        previewLabel.setMaximumSize(new java.awt.Dimension(207, 75));
        previewLabel.setMinimumSize(new java.awt.Dimension(207, 75));
        previewLabel.setPreferredSize(new java.awt.Dimension(207, 75));
        jPanel60.add(previewLabel, java.awt.BorderLayout.NORTH);

        jPanel35.add(jPanel60, java.awt.BorderLayout.SOUTH);

        fontPanel.add(jPanel35, java.awt.BorderLayout.CENTER);

        theTabs.addTab("Font", fontPanel);

        theTabs.addTab("Colour", colorChooser);

        getContentPane().add(theTabs, java.awt.BorderLayout.NORTH);

        jPanel32.setLayout(new java.awt.GridLayout(1, 0));

        jPanel33.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        defaultButton.setFont(new java.awt.Font("Dialog", 0, 12));
        defaultButton.setMnemonic('D');
        defaultButton.setText("Default...");
        jPanel33.add(defaultButton);

        jPanel32.add(jPanel33);

        jPanel34.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

        okayButton.setFont(new java.awt.Font("Dialog", 0, 12));
        okayButton.setText("OK");
        okayButton.setMaximumSize(new java.awt.Dimension(89, 25));
        okayButton.setMinimumSize(new java.awt.Dimension(89, 25));
        okayButton.setPreferredSize(new java.awt.Dimension(89, 25));
        jPanel34.add(okayButton);

        cancelButton.setFont(new java.awt.Font("Dialog", 0, 12));
        cancelButton.setText("Cancel");
        cancelButton.setMaximumSize(new java.awt.Dimension(89, 25));
        cancelButton.setMinimumSize(new java.awt.Dimension(89, 25));
        cancelButton.setPreferredSize(new java.awt.Dimension(89, 25));
        jPanel34.add(cancelButton);

        jPanel32.add(jPanel34);

        getContentPane().add(jPanel32, java.awt.BorderLayout.SOUTH);
        
        pack();
    }//GEN-END:initComponents
    
    /** Closes the dialog */
    private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
    {
        setVisible(false);
        dispose();
    }//GEN-LAST:event_closeDialog
    
    /**
     * @param args the command line arguments
     */
    public static void main(String args[])
    {
        new FontChooser(new javax.swing.JFrame()).show();
    }
    
    public void caretUpdate(CaretEvent e)
    {/*
        if(e.getSource() == fontBox)
        {
            String str = fontBox.getText();
            for(int i = 0; i < fontList.getModel().getSize(); i++)
            {
                String listStr = (String)fontList.getModel().getElementAt(i);
                if(listStr.startsWith(str))
                {
                    fontList.setSelectedIndex(i);
                    break;
                }
            }
        }*/
    }
    
    public void actionPerformed(ActionEvent e)
    {
        if(e.getSource() == fontBox)
        {
            String str = fontBox.getText();
            for(int i = 0; i < fontList.getModel().getSize(); i++)
            {
                String listStr = ((String)fontList.getModel().getElementAt(i)).toLowerCase();
                
                if(listStr.startsWith(str.toLowerCase()))
                {
                    Object value = fontList.getModel().getElementAt(i);
                    fontList.setSelectedValue(value, true);
                    break;
                }
            }
            tempValue = (String)fontList.getSelectedValue();
            if(tempValue != null)
            {
                fontBox.setText(tempValue);
            }
            else fontBox.setText("");
        }
        else if(e.getSource() == styleBox)
        {
            String str = styleBox.getText();
            for(int i = 0; i < styleList.getModel().getSize(); i++)
            {
                String listStr = ((String)styleList.getModel().getElementAt(i)).toLowerCase();
                
                if(listStr.startsWith(str.toLowerCase()))
                {
                    Object value = styleList.getModel().getElementAt(i);
                    styleList.setSelectedValue(value, true);
                    break;
                }
            }
            tempValue = (String)styleList.getSelectedValue();
            if(tempValue != null)
            {
                fontBox.setText(tempValue);
            }
            else fontBox.setText("");
        }
        else if(e.getSource() == sizeBox)
        {
            String str = sizeBox.getText();
            boolean found = false;
            for(int i = 0; i < sizeList.getModel().getSize(); i++)
            {
                String listStr = ((String)sizeList.getModel().getElementAt(i)).toLowerCase();
                
                if(listStr.startsWith(str.toLowerCase()))
                {
                    found = true;
                    Object value = sizeList.getModel().getElementAt(i);
                    sizeList.setSelectedValue(value, true);
                    tempValue = (String)sizeList.getSelectedValue();
                    if(tempValue != null)
                    {
                        sizeBox.setText(tempValue);
                    }
                    else sizeBox.setText("");
                    break;
                }
            }
            if(!found)
            {
                sizeList.setSelectedIndex(-1);
            }
            
            
        }
        else if(e.getSource() == defaultButton)
        {
            if(defaultFont != null && defaultColor != null)
            {
                setFont(defaultFont);
                colorChooser.setColor(defaultColor);
                lastColor = colorChooser.getColor();
                
                updatePreview();
            }
        }
        else if(e.getSource() == okayButton)
        {
            hide();
        }
        else if(e.getSource() == cancelButton)
        {
            shouldReturn = false;
            hide();
        }
        
        if(fontValid())
        {
            this.lastFont = new Font(getFontName(), getFontStyle(), getFontSize());
            this.lastColor = colorChooser.getColor();
            
            updatePreview();
        }
    }
    
    public String getFontName()
    {
        return fontBox.getText();
    }
    
    public int getFontStyle()
    {
        int style = Font.PLAIN;
        switch(styleList.getSelectedIndex())
        {
            case 0: style = Font.PLAIN;break;
            case 1: style = Font.BOLD;break;
            case 2: style = Font.ITALIC;break;
            case 3: style = Font.BOLD|Font.ITALIC;
        }
        return style;
    }
    
    
    public int getFontSize()
    {
        int size = 12;
        try
        {
            size = Integer.parseInt(sizeBox.getText());
        }
        catch(NumberFormatException e)
        {
        }
        return size;
    }
    
    public boolean fontValid()
    {
        boolean valid = false;
        
        String str = fontBox.getText();
        for(int i = 0; i < fontList.getModel().getSize(); i++)
            {
                String listStr = ((String)fontList.getModel().getElementAt(i)).toLowerCase();
                
                if(listStr.startsWith(str.toLowerCase()))
                {
                    valid = true;
                }
            }
        if(!valid) return false;
        
        return valid;
    }
    
    public void setFont(Font f)
    {
        if(f != null)
        {
            String str = f.getName();
            int style = f.getStyle();
            String size = ""+f.getSize();
            
            for(int i = 0; i < fontList.getModel().getSize(); i++)
            {
                String listStr = ((String)fontList.getModel().getElementAt(i)).toLowerCase();
                
                if(listStr.equals(str.toLowerCase()))
                {
                    Object value = fontList.getModel().getElementAt(i);
                    fontList.setSelectedValue(value, true);
                    fontBox.setText((String)value);
                    break;
                }
            }
            
            switch(style)
            {
                case Font.PLAIN: styleList.setSelectedIndex(0);break;
                case Font.ITALIC: styleList.setSelectedIndex(2);break;
                case Font.BOLD: styleList.setSelectedIndex(1); break;
                case Font.BOLD | Font.ITALIC: styleList.setSelectedIndex(3); break;
            }
            
            boolean found = false;
            for(int i = 0; i < sizeList.getModel().getSize(); i++)
            {
                String listStr = ((String)sizeList.getModel().getElementAt(i)).toLowerCase();
                
                if(listStr.equals(size.toLowerCase()))
                {
                    Object value = sizeList.getModel().getElementAt(i);
                    sizeList.setSelectedValue(value, true);
                    sizeBox.setText((String)value);
                    found = true;
                    break;
                }
            }
            if(!found)
            {
                sizeBox.setText(size);
            }
            
        }
    }
    
    String tempValue;
    public void valueChanged(ListSelectionEvent e)
    {
        if(e.getSource() == fontList)
        {
            tempValue = (String)fontList.getSelectedValue();
            if(tempValue != null)
            {
                fontBox.setText(tempValue);
            }
            else fontBox.setText("");
        }
        else if(e.getSource() == styleList)
        {
            tempValue = (String)styleList.getSelectedValue();
            if(tempValue != null)
            {
                styleBox.setText(tempValue);
            }
            else styleBox.setText("");
        }
        else if(e.getSource() == sizeList)
        {
            tempValue = (String)sizeList.getSelectedValue();
            if(tempValue != null)
            {
                sizeBox.setText(tempValue);
            }
            else sizeBox.setText("");
        }
        
        if(fontValid())
        {
            this.lastFont = new Font(getFontName(), getFontStyle(), getFontSize());
            this.lastColor = colorChooser.getColor();
            
            updatePreview();
        }
    }
    
    public void updatePreview()
    {
        if(lastFont != null && lastColor !=null)
        {
            previewLabel.setForeground(lastColor);
            previewLabel.setFont(lastFont);
        }
    }
    
    public void focusGained(FocusEvent e)
    {
        if(fontValid())
        {
            this.lastFont = new Font(getFontName(), getFontStyle(), getFontSize());
            this.lastColor = colorChooser.getColor();
            
            updatePreview();
        }
    }    
    
    public void focusLost(FocusEvent e)
    {
        if(fontValid())
        {
            this.lastFont = new Font(getFontName(), getFontStyle(), getFontSize());
            this.lastColor = colorChooser.getColor();
            
            updatePreview();
        }
    }
    
    public void stateChanged(ChangeEvent e)
    {
        if(fontValid())
        {
            this.lastFont = new Font(getFontName(), getFontStyle(), getFontSize());
            this.lastColor = colorChooser.getColor();
            
            updatePreview();
        }
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton cancelButton;
    protected javax.swing.JColorChooser colorChooser;
    private javax.swing.JButton defaultButton;
    private javax.swing.JTextField fontBox;
    private javax.swing.JList fontList;
    private javax.swing.JPanel fontPanel;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JPanel jPanel32;
    private javax.swing.JPanel jPanel33;
    private javax.swing.JPanel jPanel34;
    private javax.swing.JPanel jPanel35;
    private javax.swing.JPanel jPanel36;
    private javax.swing.JPanel jPanel37;
    private javax.swing.JPanel jPanel38;
    private javax.swing.JPanel jPanel39;
    private javax.swing.JPanel jPanel40;
    private javax.swing.JPanel jPanel41;
    private javax.swing.JPanel jPanel42;
    private javax.swing.JPanel jPanel43;
    private javax.swing.JPanel jPanel44;
    private javax.swing.JPanel jPanel45;
    private javax.swing.JPanel jPanel46;
    private javax.swing.JPanel jPanel47;
    private javax.swing.JPanel jPanel48;
    private javax.swing.JPanel jPanel49;
    private javax.swing.JPanel jPanel50;
    private javax.swing.JPanel jPanel51;
    private javax.swing.JPanel jPanel52;
    private javax.swing.JPanel jPanel53;
    private javax.swing.JPanel jPanel54;
    private javax.swing.JPanel jPanel55;
    private javax.swing.JPanel jPanel56;
    private javax.swing.JPanel jPanel57;
    private javax.swing.JPanel jPanel58;
    private javax.swing.JPanel jPanel59;
    private javax.swing.JPanel jPanel60;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JScrollPane jScrollPane6;
    private javax.swing.JButton okayButton;
    private javax.swing.JLabel previewLabel;
    private javax.swing.JTextField sizeBox;
    private javax.swing.JList sizeList;
    private javax.swing.JTextField styleBox;
    private javax.swing.JList styleList;
    private javax.swing.JTabbedPane theTabs;
    // End of variables declaration//GEN-END:variables
    
}


