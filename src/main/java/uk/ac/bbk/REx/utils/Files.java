package uk.ac.bbk.REx.utils;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

public class Files
{
    public static File getParentDirectory() throws UnsupportedEncodingException
    {
        String path = Files.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        File jarFile = new File(URLDecoder.decode(path, "UTF-8"));
        File parent = jarFile.getParentFile();
        return parent;
    }
}
