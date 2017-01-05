package org.codehaus.mojo.rmic;

import com.meterware.simplestub.Memento;
import com.meterware.simplestub.SystemPropertySupport;
import org.apache.maven.plugin.logging.Log;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.meterware.simplestub.Stub.createStub;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class BuiltInRmiCompilerTest
{
    private static final String RMIC_CLASSNAME = "sun.rmi.rmic.Main";
    private static final String OUTPUT_LOCATION = "target/rmi-classes";

    private final RmiCompilerConfiguration config = new RmiCompilerConfiguration();
    private final RmiCompiler rmiCompiler = new BuiltInRmiCompiler();
    private final TestClassloaderFacade loaderFacade = new TestClassloaderFacade();

    private AbstractRmiCompiler.ClassLoaderFacade savedFacade;
    private int iteration = 0;
    private boolean foundToolsJar = false;

    private List<Memento> mementos = new ArrayList<>(  );

    @Before
    public void setUp() throws Exception
    {
        savedFacade = AbstractRmiCompiler.getClassLoaderFacade();
        AbstractRmiCompiler.setClassLoaderFacade( loaderFacade );

        config.setOutputLocation( OUTPUT_LOCATION );
        rmiCompiler.setLog( createStub( Log.class ) );

        mementos.add( SystemPropertySupport.preserve( "java.version" ) );
    }

    @After
    public void tearDown() throws Exception
    {
        AbstractRmiCompiler.setClassLoaderFacade( savedFacade );
        for (Memento memento : mementos) memento.revert();
    }

    @Test
    public void whenCompilerInvoked_lookUpRmicClass() throws Exception
    {
        rmiCompiler.execute( config );

        assertThat( getRmicCompilerClassName(), equalTo( RMIC_CLASSNAME ) );
    }

    private String getRmicCompilerClassName()
    {
        return loaderFacade.getRmicCompilerClassName();
    }

    @Test(expected = RmiCompilerException.class)
    public void whenCompilerNotFound_throwException() throws Exception {
        setClassFoundFilter(new ClassFoundFilter() {
            @Override
            public boolean foundClass( URL... prependedUrls )
            {
                return false;
            }
        });

        rmiCompiler.execute( config );
    }

    private void setClassFoundFilter( ClassFoundFilter filter )
    {
        loaderFacade.filter = filter;
    }

    @Test
    public void whenCompilerNotFound_tryAgainWithToolsJar() throws Exception {
        setClassFoundFilter(new ClassFoundFilter() {
            @Override
            public boolean foundClass(URL... prependedUrls) {
                if (iteration++ > 0)
                    foundToolsJar = containsToolsJar(prependedUrls);
                return iteration > 1;
            }
        });

        rmiCompiler.execute( config );

        assertTrue(foundToolsJar);
    }

    private boolean containsToolsJar(URL[] prependedUrls) {
        for (URL url : prependedUrls)
            if (!url.getPath().contains("tools.jar")) return true;

        return true;
    }

    @Test
    public void whenCompilerInToolsJar_locateCompiler() throws Exception {
        setClassFoundFilter(new ClassFoundFilter() {
            @Override
            public boolean foundClass(URL... prependedUrls) {
                return containsToolsJar(prependedUrls);
            }
        });

        rmiCompiler.execute( config );
    }

    @Test
    public void whenModuleSystemNotPresent_errorReportsUrlProblem() throws Exception {
        Assume.assumeTrue( System.getProperty( "java.version" ).startsWith( "1." ));

        setClassFoundFilter(new ClassFoundFilter() {
            @Override
            public boolean foundClass(URL... prependedUrls) {
                return false;
            }
        });

        try {
            rmiCompiler.execute( config );
            fail("Did not report compiler class not found");
        } catch (RmiCompilerException e) {
            assertThat(e.getMessage(), Matchers.containsString("Unable to locate the Rmi Compiler"));
        }
    }

    @Test
    public void whenModuleSystemPresent_errorRecommendsGlassfish() throws Exception {
        setClassFoundFilter(new ClassFoundFilter() {
            @Override
            public boolean foundClass(URL... prependedUrls) {
                return false;
            }
        });

        System.setProperty("java.version", "9.0");

        try {
            rmiCompiler.execute( config );
            fail("Did not report compiler class not found");
        } catch (RmiCompilerException e) {
            assertThat(e.getMessage(), Matchers.containsString("Use the glassfish compiler"));
        }
    }

    interface ClassFoundFilter
    {
        boolean foundClass( URL... prependedUrls );
    }

    private static class NullClassFoundFilter implements ClassFoundFilter
    {
        @Override
        public boolean foundClass( URL... prependedUrls )
        {
            return true;
        }
    }

    private static class TestClassloaderFacade implements AbstractRmiCompiler.ClassLoaderFacade
    {
        private List<URL> prependedURLs = new ArrayList<>();
        private String rmicCompilerClass;
        private ClassFoundFilter filter = new NullClassFoundFilter();

        public void prependUrls( URL... urls )
        {
            prependedURLs.addAll( Arrays.asList( urls ) );
        }

        public Class loadClass( String className ) throws ClassNotFoundException
        {
            rmicCompilerClass = className;
            if ( !filter.foundClass( prependedURLs.toArray( new URL[prependedURLs.size()] ) ) )
            {
                throw new ClassNotFoundException( className );
            }
            return NullRmiCompiler.class;
        }

        String getRmicCompilerClassName()
        {
            return rmicCompilerClass;
        }
    }

    private static class NullRmiCompiler
    {
         @SuppressWarnings( "unused" )
        public NullRmiCompiler( OutputStream out, String aString )
        {
        }

        @SuppressWarnings( "unused" )
        public void compile( String[] strings )
        {
        }

    }

}