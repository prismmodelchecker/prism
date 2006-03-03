/*
 * Apple.java
 *
 * Created on 23 August 2005, 14:41
 */

package settings;
import java.awt.*;
/**
 *
 * @author  ug60axh
 */
public class Apple implements SettingOwner
{
    private SettingDisplay display;
    
    private SingleLineStringSetting a;
    
    private DoubleSetting b;
    private MultipleLineStringSetting c;
    private SingleLineStringSetting d;
    private DoubleSetting e;
    private BooleanSetting f;
    private ColorSetting g;
    private FontColorSetting h;
    private ChoiceSetting i;
    
    /** Creates a new instance of Apple */
    public Apple(String a, double b, String c, String d, double e, boolean f, Color g, int i)
    {
        this.a = new SingleLineStringSetting("a", a, "commenta", this, true);
        this.b = new DoubleSetting("b", new Double(b), "commentb", this, true);
        this.c = new MultipleLineStringSetting("c", c, "commentc", this, true);
        this.d = new SingleLineStringSetting("d", c, "commentd", this, true);
        this.e = new DoubleSetting("e", new Double(e), "commente", this, true);
        this.f = new BooleanSetting("f", new Boolean(f), "commentf", this, true);
        
        this.c.setEnabled(false);
        this.g = new ColorSetting("g", g, "commentg", this, true);
        this.h = new FontColorSetting("h", new FontColorPair(new Font("monospace", Font.BOLD, 12), Color.blue), "commenth", this, true);
        String [] choices = { "choice1", "choice2", "choice3", "choice4" };
        this.i = new ChoiceSetting("i", choices, choices[i], "commenti", this, true);
    }
    
    public void setDisplay(SettingDisplay display)
    {
        this.display = display;
    }
    
    public int getNumSettings()
    {
        return 9;
    }
    
    public Setting getSetting(int index)
    {
        //return a;
        switch(index)
        {
            case 0: return a;
            case 1: return b;
            case 2: return c;
            case 3: return d;
            case 4: return e;
            case 5: return f;
            case 6: return g;
            case 7: return h;
            default: return i;
        }
    }
    
    public String getSettingOwnerClassName()
    {
        return "Apple";
    }
    
    public int getSettingOwnerID()
    {
        return 33;
    }
    
    public String getSettingOwnerName()
    {
        return a.getStringValue();
    }
    
    public void notifySettingChanged(Setting setting)
    {
    }
    
    public int compareTo(Object o)
    {
        if(o instanceof SettingOwner)
        {
            SettingOwner so = (SettingOwner)o;
            if(so.getSettingOwnerID() > getSettingOwnerID()) return 1;
            else if(so.getSettingOwnerID() == getSettingOwnerID()) return 0;
            else return -1;
        }
        return -1;
    }
    
    public SettingDisplay getDisplay()
    {
        return display;
    }
    
}
