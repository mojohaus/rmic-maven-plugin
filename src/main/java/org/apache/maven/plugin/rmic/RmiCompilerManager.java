package org.apache.maven.plugin.rmic;

/**
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @version $Id$
 */
public interface RmiCompilerManager
{
    RmiCompiler getRmiCompiler( String id )
        throws NoSuchRmiCompilerException;
}
