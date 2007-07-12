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
import java.util.List;
import java.util.ArrayList;
import java.util.StringTokenizer;

import org.apache.maven.plugin.AbstractMojo;

/**
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @version $Id: AbstractRmiMojo.java 4528 2007-07-12 14:18:26Z kismet $
 */
public abstract class AbstractRmiMojo
    extends AbstractMojo
{
    // ----------------------------------------------------------------------
    // Configurable parameters
    // ----------------------------------------------------------------------

    /**
     * The classes to compile with the RMI compiler.
     *
     * @parameter
     * @required
     */
    private String remoteClasses;

    /**
     * @parameter expression="sun"
     * @required
     */
    private String compilerId;

    /**
     * @parameter expression="${project.build.directory}/rmi-stub-classes"
     * @required
     */
    private File outputClasses;

    // ----------------------------------------------------------------------
    // Constant parameters
    // ----------------------------------------------------------------------

    /**
     * @parameter expression="${project.build.outputDirectory}"
     * @required
     * @readonly
     */
    private File classes;

    /**
     * @parameter expression="${project.compileClasspathElements}"
     * @required
     * @readonly
     */
    private List compileClasspath;

    public String getRemoteClasses()
    {
        return remoteClasses;
    }

    public String getCompilerId()
    {
        return compilerId;
    }

    public File getOutputClasses()
    {
        return outputClasses;
    }

    public File getClasses()
    {
        return classes;
    }

    public List getCompileClasspath()
    {
        return compileClasspath;
    }

    protected List getSourceClasses()
    {
        List sourceClasses = new ArrayList();

        StringTokenizer tokenizer = new StringTokenizer( getRemoteClasses(), "," );

        while ( tokenizer.hasMoreElements() )
        {
            String s = (String) tokenizer.nextElement();

            s = s.trim();

            sourceClasses.add( s );
        }

        return sourceClasses;
    }
}
