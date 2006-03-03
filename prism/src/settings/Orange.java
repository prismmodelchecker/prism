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
public class Orange implements SettingOwner
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
    public Orange(String a, double b)
    {
        this.a = new SingleLineStringSetting("a", a, "commenta", this, true);
        this.b = new DoubleSetting("b", new Double(b), "commentb", this, true);
    }
    
    public void setDisplay(SettingDisplay display)
    {
        this.display = display;
    }
    
    public int getNumSettings()
    {
        return 2;
    }
    
    public Setting getSetting(int index)
    {
        //return a;
        switch(index)
        {
            case 0: return a;
            default: return b;
        }
    }
    
    public String getSettingOwnerClassName()
    {
        return "Orange";
    }
    
    public int getSettingOwnerID()
    {
        return 323;
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
