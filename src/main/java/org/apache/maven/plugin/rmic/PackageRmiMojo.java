package org.apache.maven.plugin.rmic;

/*
 * Copyright (c) 2005 Trygve Laugstol. All Rights Reserved.
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
