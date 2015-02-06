package uk.ac.bbk.REx.program;

import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import java.util.ListIterator;

public class ExtendedParser extends PosixParser
{
    private boolean ignoreUnrecognizedOption;

    public ExtendedParser(final boolean ignoreUnrecognizedOption)
    {
        this.ignoreUnrecognizedOption = ignoreUnrecognizedOption;
    }

    @Override
    protected void processOption(final String arg, final ListIterator iter) throws ParseException
    {
        boolean hasOption = getOptions().hasOption(arg);

        if (hasOption || !ignoreUnrecognizedOption) {
            super.processOption(arg, iter);
        }
    }
}
