package org.codehaus.mojo.rmic;

/*
 * Copyright (c) 2012-2017, Codehaus.org
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
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Based on CompilerConfiguration, but since that class has too much javac-specific properties,
 * create an custom version.
 * 
 * @author Robert Scholte
 * @since 1.3
 */
public class RmiCompilerConfiguration
{
    private String outputLocation;

    private List<String> classpathEntries = new LinkedList<String>();

    // ----------------------------------------------------------------------
    // Source Files
    // ----------------------------------------------------------------------

    private Set<File> sourceFiles = new HashSet<File>();

    private List<String> sourceLocations = new LinkedList<String>();

    private Set<String> includes = new HashSet<String>();

    private Set<String> excludes = new HashSet<String>();
    

    // ----------------------------------------------------------------------
    // Compiler Settings
    // ----------------------------------------------------------------------
    
    private String version;
    
    private boolean iiop;
    
    private boolean poa;
    
    private boolean noLocalStubs;
    
    private boolean idl;
    
    private boolean noValueMethods;
    
    private boolean keep;
    
    private boolean verbose;
    
    private boolean nowarn;
    
    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    public void setOutputLocation( String outputLocation )
    {
        this.outputLocation = outputLocation;
    }

    public String getOutputLocation()
    {
        return outputLocation;
    }
    
    // ----------------------------------------------------------------------
    // Class path
    // ----------------------------------------------------------------------

    public void addClasspathEntry( String classpathEntry )
    {
        classpathEntries.add( classpathEntry );
    }

    public void setClasspathEntries( List<String> classpathEntries )
    {
        if ( classpathEntries == null )
        {
            this.classpathEntries = Collections.emptyList();
        }
        else
        {
            this.classpathEntries = new LinkedList<String>( classpathEntries );
        }
    }

    public List<String> getClasspathEntries()
    {
        return Collections.unmodifiableList( classpathEntries );
    }
    
 // ----------------------------------------------------------------------
    // Source files
    // ----------------------------------------------------------------------

    public void setSourceFiles( Set<File> sourceFiles )
    {
        if ( sourceFiles == null )
        {
            this.sourceFiles = Collections.emptySet();
        }
        else
        {
            this.sourceFiles = new HashSet<File>( sourceFiles );
        }
    }

    public Set<File> getSourceFiles()
    {
        return sourceFiles;
    }

    public void addSourceLocation( String sourceLocation )
    {
        sourceLocations.add( sourceLocation );
    }

    public void setSourceLocations( List<String> sourceLocations )
    {
        if ( sourceLocations == null )
        {
            this.sourceLocations = Collections.emptyList();
        }
        else
        {
            this.sourceLocations = new LinkedList<String>( sourceLocations );
        }
    }

    public List<String> getSourceLocations()
    {
        return Collections.unmodifiableList( sourceLocations );
    }

    public void addInclude( String include )
    {
        includes.add( include );
    }

    public void setIncludes( Set<String> includes )
    {
        if ( includes == null )
        {
            this.includes = Collections.emptySet();
        }
        else
        {
            this.includes = new HashSet<String>( includes );
        }
    }

    public Set<String> getIncludes()
    {
        return Collections.unmodifiableSet( includes );
    }

    public void addExclude( String exclude )
    {
        excludes.add( exclude );
    }

    public void setExcludes( Set<String> excludes )
    {
        if ( excludes == null )
        {
            this.excludes = Collections.emptySet();
        }
        else
        {
            this.excludes = new HashSet<String>( excludes );
        }
    }

    public Set<String> getExcludes()
    {
        return Collections.unmodifiableSet( excludes );
    }
    
    // ----------------------------------------------------------------------
    // Compiler Settings
    // ----------------------------------------------------------------------
    
    public void setVersion( String version )
    {
        this.version = version;
    }
    
    public String getVersion()
    {
        return version;
    }
    
    public void setIiop( boolean iiop )
    {
        this.iiop = iiop;
    }
    
    public boolean isIiop()
    {
        return iiop;
    }
    
    public void setPoa( boolean poa )
    {
        this.poa = poa;
    }
    
    public boolean isPoa()
    {
        return poa;
    }
    
    public void setNoLocalStubs( boolean noLocalStubs )
    {
        this.noLocalStubs = noLocalStubs;
    }
    
    public boolean isNoLocalStubs()
    {
        return noLocalStubs;
    }
    
    public void setIdl( boolean idl )
    {
        this.idl = idl;
    }
    
    public boolean isIdl()
    {
        return idl;
    }
    
    public void setNoValueMethods( boolean noValueMethods )
    {
        this.noValueMethods = noValueMethods;
    }
    
    public boolean isNoValueMethods()
    {
        return noValueMethods;
    }
    
    public void setKeep( boolean keep )
    {
        this.keep = keep;
    }
    
    public boolean isKeep()
    {
        return keep;
    }
    
    public void setVerbose( boolean verbose )
    {
        this.verbose = verbose;
    }
    
    public boolean isVerbose()
    {
        return verbose;
    }
    
    public void setNowarn( boolean nowarn )
    {
        this.nowarn = nowarn;
    }
    
    public boolean isNowarn()
    {
        return nowarn;
    }
}
