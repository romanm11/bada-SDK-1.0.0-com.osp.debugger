package com.osp.debugger.launch;

import java.util.ArrayList;

import org.eclipse.cdt.debug.core.ICDTLaunchConfigurationConstants;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.preferences.IDebugPreferenceConstants;
import org.eclipse.jface.preference.IPreferenceStore;

import com.osp.ide.IConstants;
import com.osp.ide.IdePlugin;
import com.osp.ide.core.PathResolver;

public class OspLaunchUtils {
	
	
	
	
	private static class ArgumentParser {

		private String fArgs;

		private int fIndex = 0;

		private int ch = -1;

		public ArgumentParser( String args ) {
			fArgs = args;
		}

		public String[] parseArguments() {
			ArrayList v = new ArrayList();
			ch = getNext();
			while( ch > 0 ) {
				while( Character.isWhitespace( (char)ch ) )
					ch = getNext();
				if ( ch == '"' ) {
					v.add(parseString() );
				}
				else {
					v.add( parseToken() );
				}
			}
			String[] result = new String[v.size()];
			v.toArray( result );
			return result;
		}

		private int getNext() {
			if ( fIndex < fArgs.length() )
				return fArgs.charAt( fIndex++ );
			return -1;
		}

		private String parseString() {
			StringBuffer buf = new StringBuffer();
			ch = getNext();
			while( ch > 0 && ch != '"' ) {
				if ( ch == '\\' ) {
					ch = getNext();
					if ( ch != '"' ) { // Only escape double quotes
						buf.append( '\\' );
					}
				}
				if ( ch > 0 ) {
					buf.append( (char)ch );
					ch = getNext();
				}
			}
			ch = getNext();
			return buf.toString();
		}

		private String parseToken() {
			StringBuffer buf = new StringBuffer();
			while( ch > 0 && !Character.isWhitespace( (char)ch ) ) {
				if ( ch == '\\' ) {
					ch = getNext();
					if ( ch > 0 ) {
						if ( ch != '"' ) { // Only escape double quotes
							buf.append( '\\' );
						}
						buf.append( (char)ch );
						ch = getNext();
					}
					else if ( ch == -1 ) { // Don't lose a trailing backslash
						buf.append( '\\' );
					}
				}
				else if ( ch == '"' ) {
					buf.append( parseString() );
				}
				else {
					buf.append( (char)ch );
					ch = getNext();
				}
			}
			return buf.toString();
		}
	}

	public static IStringVariableManager getStringVariableManager() {
		return VariablesPlugin.getDefault().getStringVariableManager();
	}
	
	public static String[] getProgramArgumentsArray( ILaunchConfiguration config, IProject project ) throws CoreException {
		return parseArguments( getProgramArguments( config, project ) );
	}
	
	/**
	 * Returns the program arguments as a String.
	 */
	public static String getProgramArguments(ILaunchConfiguration config, IProject project) throws CoreException {
		String args = config.getAttribute(ICDTLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, (String)null);
		if (args != null) {
			args = PathResolver.getAbsolutePath(args, project);
			
			int port = IdePlugin.getDefault().getSimulatorConnectPort();
			if( port > 0 )
				args += (" -p " + Integer.toString(port));
			
			args = getStringVariableManager().performStringSubstitution(args);
		}
		return args;
	}	
	
	private static String[] parseArguments( String args ) {
		if ( args == null )
			return new String[0];
		ArgumentParser parser = new ArgumentParser( args );
		String[] res = parser.parseArguments();
		return res;
	}
	
	
	static public void changeConsoleViewOption()
	{
		IPreferenceStore store = DebugUIPlugin.getDefault().getPreferenceStore();
		store.setValue(IDebugPreferenceConstants.CONSOLE_OPEN_ON_ERR, false);
		store.setValue(IDebugPreferenceConstants.CONSOLE_OPEN_ON_OUT, false);
	}
	
	static public ArrayList getDefaultTargetSharedLibSearchPath()
	{
		ArrayList values = new ArrayList(1 );
		values.add(IConstants.ENV_TARGRT_LIB_PATH_VAR);
		
		return values;
	}
	
}
