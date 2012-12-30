package org.codehaus.mojo.rmic;

import static org.junit.Assert.*;
import org.junit.Test;


public class SourceTest
{
    @Test
    public void test_SourceToString() throws Exception
    {
        Source source = new Source();
        source.setKeep( true );
        source.setNowarn( true );
        assertEquals( "Including [**/*]; excluding []\nwith options: -keep -nowarn ", source.toString() );
    }


}
