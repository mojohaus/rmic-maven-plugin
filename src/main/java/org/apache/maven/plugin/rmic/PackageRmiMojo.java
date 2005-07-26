package org.apache.maven.plugin.rmic;

/*
 * Copyright (c) 2005 Trygve Laugstol. All Rights Reserved.
 */

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.jar.JarFile;

import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.ArchiverException;

/**
 * @goal package
 *
 * @phase package
 *
 * @requiresDependencyResolution
 *
 * @description Packages the RMI stubs and client classes.
 *
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @version $Id$
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
     * @parameter expression="${project.build.finalname}"
     * @required
     */
    private String finalName;

    /**
     * @parameter expression="${project}"
     */
    private MavenProject project;

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    public void execute()
        throws MojoExecutionException
    {
        File clientJar = new File( target, finalName + "-client.jar" );

        try
        {
            JarArchiver jarArchiver = new JarArchiver();

            jarArchiver.setDestFile( clientJar );

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
        }
        catch ( ArchiverException e )
        {
            throw new MojoExecutionException( "Could not create the client jar", e );
        }
    }
}
