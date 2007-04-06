//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Andrew Hinton <ug60axh@cs.bham.uc.uk> (University of Birmingham)
//	* Dave Parker <dxp@cs.bham.uc.uk> (University of Birmingham)
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

package chart;
import javax.swing.colorchooser.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 *  Provides a new implementation for the swatches panel that includes
 *  PRISM's built in colours as last recently used colors
 * @author  Andrew
 */
public class PrismColorChooserPanel extends AbstractColorChooserPanel
{
    private static Color[][] recentColours;
    
    
    static
    {
        recentColours = new Color[5][7];
            Color def = UIManager.getColor("ColorChooser.swatchesDefaultRecentColor");
            
            for(int i = 0; i < recentColours.length; i++)
                for(int j = 0; j < recentColours[0].length; j++)
                {
                    recentColours[i][j] = def;
                }
            
            for(int i = 0; i < GraphList.DEFAULT_COLORS.length; i++)
            {
                recentColours[i%5][i/5] = GraphList.DEFAULT_COLORS[i];
            }
    }
    
    /** Creates a new instance of PrismColorChooserPanel */
    public PrismColorChooserPanel()
    {
        super();
    }
    
    public static void main(String[]args)
    {
        JColorChooser cc = new JColorChooser(Color.white);
        AbstractColorChooserPanel[] pans = cc.getChooserPanels();
        AbstractColorChooserPanel[] newpans = { new PrismColorChooserPanel(), pans[1], pans[2] };
        cc.setChooserPanels(newpans);
        JDialog d = JColorChooser.createDialog(null, "cc", true, cc, null, null);
        d.show();
    }
    
    protected void buildChooser()
    {
        
        JPanel thePanel = new JPanel();
        thePanel.setLayout(new BorderLayout());
        
        
        //Do recent colours
        RecentPanel recentColors = new RecentPanel();
        JPanel recentHolder = new JPanel();
        recentHolder.add(recentColors);
        
        //Do color table
        JPanel colorTable = new ColorsPanel(recentColors);
        JPanel colorHolder = new JPanel();
        colorHolder.add(colorTable);
        
        thePanel.add(colorHolder, BorderLayout.CENTER);
        JPanel right = new JPanel();
        right.setLayout(new BorderLayout());
        right.add(recentHolder, BorderLayout.CENTER);
        JLabel l = new JLabel("Recent:");
        right.add(l, BorderLayout.NORTH);
        thePanel.add(right, BorderLayout.EAST);
        add(thePanel);
    }
    
    public String getDisplayName()
    {
        return "Swatches";
    }
    
    public Icon getLargeDisplayIcon()
    {
        return null;
    }
    
    public Icon getSmallDisplayIcon()
    {
        return null;
    }
    
    public void updateChooser()
    {
    }
    
    class ColorsPanel extends JPanel implements MouseListener
    {
        private Color[][] colors;
        public static final int WIDTH = 10;
        private RecentPanel rp;
        
        public ColorsPanel(RecentPanel rp)
        {
             this.rp =rp;
            colors = new Color[31][9];
            int[][] rawValues =
            {
                {255, 255, 255, 
                204, 255, 255,
                204, 204, 255,
                204, 204, 255,
                204, 204, 255,
                204, 204, 255,
                204, 204, 255,
                204, 204, 255,
                204, 204, 255,
                204, 204, 255,
                204, 204, 255,
                255, 204, 255,
                255, 204, 204,
                255, 204, 204,
                255, 204, 204,
                255, 204, 204,
                255, 204, 204,
                255, 204, 204,
                255, 204, 204,
                255, 204, 204,
                255, 204, 204,
                255, 255, 204,
                204, 255, 204,
                204, 255, 204,
                204, 255, 204,
                204, 255, 204,
                204, 255, 204,
                204, 255, 204,
                204, 255, 204,
                204, 255, 204,
                204, 255, 204},
                {204, 204, 204,  
                153, 255, 255,
                153, 204, 255,
                153, 153, 255,
                153, 153, 255,
                153, 153, 255,
                153, 153, 255,
                153, 153, 255,
                153, 153, 255,
                153, 153, 255,
                204, 153, 255,
                255, 153, 255,
                255, 153, 204,
                255, 153, 153,
                255, 153, 153,
                255, 153, 153,
                255, 153, 153,
                255, 153, 153,
                255, 153, 153,
                255, 153, 153,
                255, 204, 153,
                255, 255, 153,
                204, 255, 153,
                153, 255, 153,
                153, 255, 153,
                153, 255, 153,
                153, 255, 153,
                153, 255, 153,
                153, 255, 153,
                153, 255, 153,
                153, 255, 204},
                {204, 204, 204,  
                102, 255, 255,
                102, 204, 255,
                102, 153, 255,
                102, 102, 255,
                102, 102, 255,
                102, 102, 255,
                102, 102, 255,
                102, 102, 255,
                153, 102, 255,
                204, 102, 255,
                255, 102, 255,
                255, 102, 204,
                255, 102, 153,
                255, 102, 102,
                255, 102, 102,
                255, 102, 102,
                255, 102, 102,
                255, 102, 102,
                255, 153, 102,
                255, 204, 102,
                255, 255, 102,
                204, 255, 102,
                153, 255, 102,
                102, 255, 102,
                102, 255, 102,
                102, 255, 102,
                102, 255, 102,
                102, 255, 102,
                102, 255, 153,
                102, 255, 204},
                {153, 153, 153,
                51, 255, 255,
                51, 204, 255,
                51, 153, 255,
                51, 102, 255,
                51, 51, 255,
                51, 51, 255,
                51, 51, 255,
                102, 51, 255,
                153, 51, 255,
                204, 51, 255,
                255, 51, 255,
                255, 51, 204,
                255, 51, 153,
                255, 51, 102,
                255, 51, 51,
                255, 51, 51,
                255, 51, 51,
                255, 102, 51,
                255, 153, 51,
                255, 204, 51,
                255, 255, 51,
                204, 255, 51,
                153, 244, 51,
                102, 255, 51,
                51, 255, 51,
                51, 255, 51,
                51, 255, 51,
                51, 255, 102,
                51, 255, 153,
                51, 255, 204},
                {153, 153, 153,
                0, 255, 255,
                0, 204, 255,
                0, 153, 255,
                0, 102, 255,
                0, 51, 255,
                0, 0, 255,
                51, 0, 255,
                102, 0, 255,
                153, 0, 255,
                204, 0, 255,
                255, 0, 255,
                255, 0, 204,
                255, 0, 153,
                255, 0, 102,
                255, 0, 51,
                255, 0 , 0,
                255, 51, 0,
                255, 102, 0,
                255, 153, 0,
                255, 204, 0,
                255, 255, 0,
                204, 255, 0,
                153, 255, 0,
                102, 255, 0,
                51, 255, 0,
                0, 255, 0,
                0, 255, 51,
                0, 255, 102,
                0, 255, 153,
                0, 255, 204},
                {102, 102, 102,
                0, 204, 204,
                0, 204, 204,
                0, 153, 204,
                0, 102, 204,
                0, 51, 204,
                0, 0, 204,
                51, 0, 204,
                102, 0, 204,
                153, 0, 204,
                204, 0, 204,
                204, 0, 204,
                204, 0, 204,
                204, 0, 153,
                204, 0, 102,
                204, 0, 51,
                204, 0, 0,
                204, 51, 0,
                204, 102, 0,
                204, 153, 0,
                204, 204, 0,
                204, 204, 0,
                204, 204, 0,
                153, 204, 0,
                102, 204, 0,
                51, 204, 0,
                0, 204, 0,
                0, 204, 51,
                0, 204, 102,
                0, 204, 153,
                0, 204, 204},
                {102, 102, 102, 
                0, 153, 153,
                0, 153, 153,
                0, 153, 153,
                0, 102, 153,
                0, 51, 153,
                0, 0, 153,
                51, 0, 153,
                102, 0, 153,
                153, 0, 153,
                153, 0, 153,
                153, 0, 153,
                153, 0, 153,
                153, 0, 153,
                153, 0, 102,
                153, 0, 51,
                153, 0, 0,
                153, 51, 0,
                153, 102, 0,
                153, 153, 0,
                153, 153, 0,
                153, 153, 0,
                153, 153, 0,
                153, 153, 0,
                102, 153, 0,
                51, 153, 0,
                0, 153, 0,
                0, 153, 51,
                0, 153, 102,
                0, 153, 153,
                0, 153, 153},
                {51, 51, 51, 
                0, 102, 102,
                0, 102, 102,
                0, 102, 102,
                0, 102, 102,
                0, 51, 102,
                0, 0, 102,
                51, 0, 102,
                102, 0, 102,
                102, 0, 102,
                102, 0, 102,
                102, 0, 102,
                102, 0, 102,
                102, 0, 102,
                102, 0, 102,
                102, 0, 51,
                102, 0, 0,
                102, 51, 0,
                102, 102, 0,
                102, 102, 0,
                102, 102, 0,
                102, 102, 0,
                102, 102, 0,
                102, 102, 0,
                102, 102, 0,
                51, 102, 0,
                0, 102, 0,
                0, 102, 51,
                0, 102, 102,
                0, 102, 102,
                0, 102, 102},
                {0, 0, 0, 
                0, 51, 51,
                0, 51, 51,
                0, 51, 51,
                0, 51, 51,
                0, 51, 51,
                0, 0, 51,
                51, 0, 51,
                51, 0, 51,
                51, 0, 51,
                51, 0, 51,
                51, 0, 51,
                51, 0, 51,
                51, 0, 51,
                51, 0, 51,
                51, 0, 51,
                51, 0, 0,
                51, 51, 0,
                51, 51, 0,
                51, 51, 0,
                51, 51, 0,
                51, 51, 0,
                51, 51, 0,
                51, 51, 0,
                51, 51, 0,
                0, 51, 0,
                0, 51, 51,
                0, 51, 51,
                0, 51, 51,
                0, 51, 51,
                51, 51, 51} };
                
                for(int i = 0; i < rawValues.length; i++)
                    for(int j = 0; j < rawValues[0].length; j=j+3)
                    {
                        colors[j/3][i] = new Color(rawValues[i][j], rawValues[i][j+1], rawValues[i][j+2]);
                    }
                setPreferredSize(new Dimension((31*WIDTH)+1, (9*WIDTH)+1));
                
                addMouseListener(this);
               
        }
        
        public void paintComponent(Graphics g)
        {
            g.setColor(getBackground());
            g.fillRect(0,0,getWidth(), getHeight());
            
            for(int i = 0; i < colors.length; i++)
                for(int j = 0; j < colors[0].length; j++)
                {
                    int x = i*WIDTH;
                    int y = j*WIDTH;
                    
                    g.setColor(colors[i][j]);
                    g.fillRect(x,y, WIDTH, WIDTH);
                    g.setColor(Color.black);
                    g.drawRect(x,y,WIDTH,WIDTH);
                }
        }
        
        public void mouseClicked(MouseEvent e)
        {
            Color color = colors[e.getX()/WIDTH][e.getY()/WIDTH];
            getColorSelectionModel().setSelectedColor(color);
            rp.recentColour(color);
        }
        
        public void mouseEntered(MouseEvent e)
        {
        }
        
        public void mouseExited(MouseEvent e)
        {
        }
        
        public void mousePressed(MouseEvent e)
        {
        }
        
        public void mouseReleased(MouseEvent e)
        {
        }
        
    }
    
    class RecentPanel extends JPanel implements MouseListener
    {
        
        
        public static final int WIDTH = 10;
        public RecentPanel()
        {
            
            
            setPreferredSize(new Dimension(1+(5*WIDTH), 1+(7*WIDTH)));
                
            addMouseListener(this);
            
           
        }
        
        public void paintComponent(Graphics g)
        {
            g.setColor(getBackground());
            g.fillRect(0,0,getWidth(), getHeight());
            
            for(int i = 0; i < recentColours.length; i++)
                for(int j = 0; j < recentColours[0].length; j++)
                {
                    int x = i*WIDTH;
                    int y = j*WIDTH;
                    
                    g.setColor(recentColours[i][j]);
                    g.fillRect(x,y, WIDTH, WIDTH);
                    g.setColor(Color.black);
                    g.drawRect(x,y,WIDTH,WIDTH);
                }
        }
        
        public void recentColour(Color c)
        {
            //first check that the colour is not clready there
            for(int i = 0; i < recentColours.length; i++)
                for(int j = 0; j < recentColours[0].length ; j++)
                    if(c.equals(recentColours[i][j])) return;
            
            
            int numColours = recentColours.length * recentColours[0].length;
            
            for(int i = numColours-1; i > 0; i--)
                recentColours[getIOf(i)][getJOf(i)] = recentColours[getIOf(i-1)][getJOf(i-1)];
            
            recentColours[0][0] = c;
            
            repaint();
        }
        
        private  int getIOf(int n)
        {
            return n%5;
        }
        
        private  int getJOf(int n)
        {
            return n/5;
        }
        
        public void mouseClicked(MouseEvent e)
        {
            try
            {
                Color color = recentColours[e.getX()/WIDTH][e.getY()/WIDTH];
                getColorSelectionModel().setSelectedColor(color);
            }
            catch(ArrayIndexOutOfBoundsException ep)
            {
                
            }
        }
        
        public void mouseEntered(MouseEvent e)
        {
        }
        
        public void mouseExited(MouseEvent e)
        {
        }
        
        public void mousePressed(MouseEvent e)
        {
        }
        
        public void mouseReleased(MouseEvent e)
        {
        }
        
    }
    
}
