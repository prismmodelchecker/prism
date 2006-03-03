/*
 * Test.java
 *
 * Created on 23 August 2005, 11:13
 */

package settings;
import javax.swing.*;
import java.awt.*;

/**
 *
 * @author  ug60axh
 */
public class Test
{
    
    /** Creates a new instance of Test */
    public Test()
    {
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
       JFrame frame = new JFrame("Test");
        
        String [][] data = { {"a", "b"}, {"c", "d"}, {"e","f"}};
        String [] columns = { "1", "2"};
        
        JTable tab = new JTable(data, columns);
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(tab, BorderLayout.CENTER);
        
        tab.setPreferredSize(new Dimension(200,200));
        
        TableResizer lll = new TableResizer(tab);
        
        tab.addMouseListener(lll);
        tab.addMouseMotionListener(lll);
        
        frame.pack();
        frame.show();
    }
    
}
