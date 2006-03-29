package org.apache.maven.plugin.rmic;

/*
 * Copyright (c) 2005 Trygve Laugstol. All Rights Reserved.
 */

import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.StringTokenizer;

import org.apache.maven.plugin.AbstractMojo;

/**
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @version $Id$
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
