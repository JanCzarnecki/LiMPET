package uk.ac.bbk.REx.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BioCycPathwayNamesUtils
{
    private BioCycPathwayNamesUtils(){}

    public static boolean arePathwayNamesAlternatives(String pathway1, String pathway2)
    {
        Pattern romanNumeralPatt = Pattern.compile("\\b[IVX]+\\b");

        Matcher m1 = romanNumeralPatt.matcher(pathway1);
        Matcher m2 = romanNumeralPatt.matcher(pathway2);

        if(m1.find() && m2.find())
        {
            String name1 = pathway1.substring(0, m1.start()-1);
            String name2 = pathway2.substring(0, m2.start()-1);

            if(name1.equals(name2))
            {
                return true;
            }
            else
            {
                return false;
            }
        }
        else
        {
            return false;
        }
    }

    public static boolean isPathwayInSet(String pathwaySet, String pathway)
    {
        if(pathway.contains(pathwaySet))
        {
            return true;
        }
        else
        {
            return false;
        }
    }
}
