package org.codehaus.mojo.rmic;

/*
 * Copyright (c) 2004-2007, Codehaus.org
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
import java.io.IOException;
import java.util.Iterator;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;

import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.ArchiverException;

/**
 * @goal package
 *
 * @phase package
 *
 * @description Packages the RMI stub and client classes.
 *
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @version $Id: PackageRmiMojo.java 4528 2007-07-12 14:18:26Z kismet $
 */
public class PackageRmiMojo
    extends AbstractRmiMojo
{
    /**
     * @parameter expression="${project.build.directory}"
     * @required
     */
    private File target;

    /**
     * @parameter expression="${project.build.finalName}"
     * @required
     */
    private String finalName;

    /**
     * @parameter expression="${project}"
     * @required
     */
    private MavenProject project;

    /**
     * @component
     */
    private MavenProjectHelper projectHelper;

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    public void execute()
        throws MojoExecutionException
    {
        String classifier = "client";

        File stubJar = new File( target, finalName + "-" + classifier + ".jar" );

        try
        {
            JarArchiver jarArchiver = new JarArchiver();

            jarArchiver.setDestFile( stubJar );

            // ----------------------------------------------------------------------
            // Add the *_Stub classes
            // ----------------------------------------------------------------------

            for ( Iterator it = getSourceClasses().iterator(); it.hasNext(); )
            {
                String clazz = (String) it.next();

                String[] includes = new String[] {
                    clazz.replace( '.', '/' ) + "_Stub.class",
                };

                jarArchiver.addDirectory( getOutputClasses(), includes, new String[ 0 ] );
            }

            getLog().info( "Building RMI stub jar: " + stubJar.getAbsolutePath() );

            jarArchiver.createArchive();

            projectHelper.attachArtifact( project, "jar", classifier, stubJar );
        }
        catch ( ArchiverException e )
        {
            throw new MojoExecutionException( "Could not create the RMI stub jar", e );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Could not create the RMI stub jar", e );
        }
    }
}
