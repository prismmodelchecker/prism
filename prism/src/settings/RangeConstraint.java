//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Andrew Hinton <ug60axh@cs.bham.ac.uk> (University of Birmingham)
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

import java.util.*;

public class RangeConstraint extends NumericConstraint
{
    private double lower, upper;
    private boolean inclusiveLower, inclusiveUpper;
    
    /** Creates a new instance of DoubleRangeConstraint */
    public RangeConstraint(String parseThis)
    {
		StringTokenizer tokens = new StringTokenizer(parseThis, ",");
        int count = tokens.countTokens();
        
        if(!parseThis.startsWith(","))
        {
            //lower and upper
            if(count == 2)
            {
                try
                {
                    lower = Double.parseDouble(tokens.nextToken());
                    upper = Double.parseDouble(tokens.nextToken());
                }
                catch(NumberFormatException e)
                {
                    lower = Double.NEGATIVE_INFINITY;
					upper = Double.POSITIVE_INFINITY;
                }
            }
            //lower only
            else if(count == 1)
            {
                try
                {
                    lower = Double.parseDouble(tokens.nextToken());
					upper = Double.POSITIVE_INFINITY;
                }
                catch(NumberFormatException e)
                {
                    lower = Double.NEGATIVE_INFINITY;
					upper = Double.POSITIVE_INFINITY;
                }
            }
            else
            {
                lower = Double.NEGATIVE_INFINITY;
				upper = Double.POSITIVE_INFINITY;
            }
        }
        else // should start with , and then a number
        {
            try
            {
                upper = Double.parseDouble(tokens.nextToken());
				lower = Double.NEGATIVE_INFINITY;
            }
            catch(NumberFormatException e)
            {
                lower = Double.NEGATIVE_INFINITY;
				upper = Double.POSITIVE_INFINITY;
            }
        }
		
		inclusiveLower = true;
		inclusiveUpper = true;
    }
    
    public RangeConstraint(double lower, double upper, boolean inclusiveLower, boolean inclusiveUpper)
    {
        this.lower = lower;
        this.upper = upper;
        this.inclusiveLower = inclusiveLower;
        this.inclusiveUpper = inclusiveUpper;
    }
	
	public RangeConstraint(int lower, int upper, boolean inclusiveLower, boolean inclusiveUpper)
	{
		this.lower = lower;
        this.upper = upper;
        this.inclusiveLower = inclusiveLower;
        this.inclusiveUpper = inclusiveUpper;
	}
    
	public void checkValueDouble(double value) throws SettingException
	{
		if(inclusiveLower)
		{
			if(value < lower) throw new SettingException("The value: "+value+" should be >="+lower);
		}
		else
		{
			if(value <= lower) throw new SettingException("The value: "+value+" should be >"+lower);
		}
		
		if(inclusiveUpper)
		{
			if(value > upper) throw new SettingException("The value: "+value+"should be <="+upper);
		}
		else
		{
			if(value >= upper) throw new SettingException("The value: "+value+"should be <"+upper);
		}
	}
	
	public void checkValueInteger(int value) throws SettingException
	{
		if(inclusiveLower)
		{
			if(value < lower) throw new SettingException("The value: "+value+" should be >="+(int)lower);
		}
		else
		{
			if(value <= lower) throw new SettingException("The value: "+value+" should be >"+(int)lower);
		}
		
		if(inclusiveUpper)
		{
			if(value > upper) throw new SettingException("The value: "+value+"should be <="+(int)upper);
		}
		else
		{
			if(value >= upper) throw new SettingException("The value: "+value+"should be <"+(int)upper);
		}
	}
	
	public void checkValueLong(long value) throws SettingException
	{
		if(inclusiveLower)
		{
			if(value < lower) throw new SettingException("The value: "+value+" should be >="+(long)lower);
		}
		else
		{
			if(value <= lower) throw new SettingException("The value: "+value+" should be >"+(long)lower);
		}
		
		if(inclusiveUpper)
		{
			if(value > upper) throw new SettingException("The value: "+value+"should be <="+(long)upper);
		}
		else
		{
			if(value >= upper) throw new SettingException("The value: "+value+"should be <"+(long)upper);
		}
	}
}
