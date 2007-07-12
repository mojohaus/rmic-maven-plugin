package org.codehaus.mojo.rmic;

/*
 * Copyright (c) 2004, Codehaus.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.maven.plugin.MojoExecutionException;

/**
 * @goal process-classes
 *
 * @phase process-classes
 *
 * @requiresDependencyResolution
 *
 * @description Enhances the application data objects.
 *
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @version $Id: ProcessClassesRmiMojo.java 1755 2006-03-29 20:32:53Z trygvis $
 */
public class ProcessClassesRmiMojo
    extends AbstractRmiMojo
{
    // ----------------------------------------------------------------------
    // Configurable parameters
    // ----------------------------------------------------------------------

    /**
     * @parameter expression="sun"
     * @required
     */
    private String compilerId;

    // ----------------------------------------------------------------------
    // Constant parameters
    // ----------------------------------------------------------------------

    /**
     * @component org.apache.maven.plugin.rmic.RmiCompilerManager
     */
    private RmiCompilerManager rmiCompilerManager;

    public void execute()
        throws MojoExecutionException
    {
        RmiCompiler rmiCompiler;

        try
        {
            rmiCompiler = rmiCompilerManager.getRmiCompiler( compilerId );
        }
        catch ( NoSuchRmiCompilerException e )
        {
            throw new MojoExecutionException( "No such RMI compiler installed '" + compilerId + "'." );
        }

        if ( !getOutputClasses().isDirectory() )
        {
            if ( !getOutputClasses().mkdirs() )
            {
                throw new MojoExecutionException( "Could not make output directory: " +
                                                  "'" + getOutputClasses().getAbsolutePath() + "'." );
            }
        }

        try
        {
            File[] compileClasspath = new File[ getCompileClasspath().size() + 1 ];

            compileClasspath[ 0 ] = getClasses();

            Iterator it;

            int i;

            for ( it = getCompileClasspath().iterator(), i = 1; it.hasNext(); i++ )
            {
                compileClasspath[ i ] = new File( (String) it.next() );
            }

            List sourceClasses = getSourceClasses();

            rmiCompiler.execute( compileClasspath, sourceClasses, getOutputClasses() );
        }
        catch ( RmiCompilerException e )
        {
            throw new MojoExecutionException( "Error while executing the RMI compiler.", e );
        }
    }
}
