/*******************************************************************************
 * Copyright (c) 2004, 2007 QNX Software Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * QNX Software Systems - Initial API and implementation
 *******************************************************************************/
package com.osp.debugger.launch.simul; 

import java.io.File;
//import java.io.FileNotFoundException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.IBinaryParser.IBinaryObject;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.debug.core.ICDTLaunchConfigurationConstants;
import org.eclipse.cdt.debug.core.cdi.CDIException;
import org.eclipse.cdt.debug.core.cdi.ICDISession;
import org.eclipse.cdt.debug.core.cdi.ICDISessionConfiguration;
import org.eclipse.cdt.debug.core.cdi.model.ICDITarget;
import org.eclipse.cdt.debug.mi.core.AbstractGDBCDIDebugger;
import org.eclipse.cdt.debug.mi.core.IMILaunchConfigurationConstants;
import org.eclipse.cdt.debug.mi.core.MICoreUtils;
import org.eclipse.cdt.debug.mi.core.MIException;
import org.eclipse.cdt.debug.mi.core.MIPlugin;
import org.eclipse.cdt.debug.mi.core.MISession;
import org.eclipse.cdt.debug.mi.core.cdi.Session;
import org.eclipse.cdt.debug.mi.core.cdi.SharedLibraryManager;
import org.eclipse.cdt.debug.mi.core.cdi.model.Target;
import org.eclipse.cdt.debug.mi.core.command.CLIHandle;
import org.eclipse.cdt.debug.mi.core.command.CLITargetAttach;
import org.eclipse.cdt.debug.mi.core.command.CommandFactory;
import org.eclipse.cdt.debug.mi.core.command.MIFileSymbolFile;
import org.eclipse.cdt.debug.mi.core.command.MIGDBSet;
import org.eclipse.cdt.debug.mi.core.command.MIGDBSetNewConsole;
import org.eclipse.cdt.debug.mi.core.output.MIInfo;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.IProcess;

import com.osp.debugger.IDebugConstants;
import com.osp.debugger.launch.IOspLaunchConfigurationConstants;
import com.osp.debugger.mi.BadaCommandFactory;
import com.osp.ide.core.PathResolver;
 
/**
 * Implementing the cdebugger extension point for basic launch configurations.
 */
public class SimulatorDebugger extends AbstractGDBCDIDebugger {

	int pid = -1;
	
	public void setPid(int pid)
	{
		this.pid = pid;;
	}	
	
	public ICDISession createDebuggerSession( ILaunch launch, IBinaryObject exe, IProgressMonitor monitor ) throws CoreException {
		return createSession( launch, exe.getPath().toFile(), monitor );
	}
	
	public ICDISession createSession( ILaunch launch, File executable, IProgressMonitor monitor ) throws CoreException {
		
		boolean failed = false;
		if ( monitor == null ) {
			monitor = new NullProgressMonitor();
		}
		if ( monitor.isCanceled() ) {
			throw new OperationCanceledException();
		}
		boolean verboseMode = verboseMode( launch.getLaunchConfiguration() );
		boolean breakpointsFullPath = getBreakpointsWithFullNameAttribute(launch.getLaunchConfiguration() );
		Session session = createGDBSession( launch, executable, monitor );
		if ( session != null ) {
			try {
				ICDITarget[] targets = session.getTargets();
				for( int i = 0; i < targets.length; i++ ) {
					Process debugger = session.getSessionProcess( targets[i] );
					if ( debugger != null ) {
						IProcess debuggerProcess = createGDBProcess( (Target)targets[i], launch, debugger, renderDebuggerProcessLabel( launch ), null );
						launch.addProcess( debuggerProcess );
					}
					Target target = (Target)targets[i];
					target.enableVerboseMode( verboseMode );
					target.getMISession().setBreakpointsWithFullName(breakpointsFullPath);
					target.getMISession().start();
				
				}
				doStartSession( launch, session, monitor );
			}
			catch( MIException e ) {
				failed = true;
				throw newCoreException( e );
			}
			catch( CoreException e ) {
				failed = true;
				throw e;
			}
			finally {
				try {
					if ( (failed || monitor.isCanceled()) && session != null )
						session.terminate();
				}
				catch( CDIException e1 ) {
				}
			}
		}
		return session;
	}
	
	protected Session createGDBSession( ILaunch launch, File executable, IProgressMonitor monitor ) throws CoreException {
		Session session = null;
		IPath gdbPath = getGDBPath( launch );
		ILaunchConfiguration config = launch.getLaunchConfiguration();
		CommandFactory factory = getCommandFactory( config );
		String[] extraArgs = getExtraArguments( config );
		boolean usePty = usePty( config );
		try {
			session = MIPlugin.getDefault().createSession( getSessionType( config ), gdbPath.toOSString(), factory, executable, extraArgs, usePty, monitor );
			ICDISessionConfiguration sessionConfig = getSessionConfiguration( session );
			if ( sessionConfig != null ) {
				session.setConfiguration( sessionConfig );
			}
		}
		catch( OperationCanceledException e ) {
		}
		catch( Exception e ) {
			// Catch all wrap them up and rethrow
			if ( e instanceof CoreException ) {
				throw (CoreException)e;
			}
			throw newCoreException( e );
		}
		return session;
	}	

	protected int getSessionType( ILaunchConfiguration config ) throws CoreException {
		
		if( IDebugConstants.ATTACH_MODE ) // for ATTACH
			return MISession.ATTACH; 
		else
			return MISession.PROGRAM;
	}	
	
	protected String[] getExtraArguments( ILaunchConfiguration config ) throws CoreException {
		return getRunArguments( config );
	}

	protected String[] getRunArguments( ILaunchConfiguration config ) throws CoreException {
		return new String[]{ getWorkingDirectory( config ), getCommandFile( config ) }; 
	}


	protected CommandFactory getCommandFactory( ILaunchConfiguration config ) throws CoreException {
		
		//return new CygwinCommandFactory( getMIVersion( config ) );
//		return new StandardWinCommandFactory( getMIVersion( config ) );
		
		String factoryID = MIPlugin.getCommandFactory( config );
		CommandFactory factory = MIPlugin.getDefault().getCommandFactoryManager().getCommandFactory( factoryID );
		String miVersion = getMIVersion( config );
		if ( factory != null ) {
			factory.setMIVersion( miVersion );
		}
//		return ( factory != null ) ? factory : new CommandFactory( miVersion );
		return ( factory != null ) ? factory : new BadaCommandFactory( miVersion );
		
		
	}

	public static IPath getProjectPath( ILaunchConfiguration configuration ) throws CoreException {
		String projectName = getProjectName( configuration );
		if ( projectName != null ) {
			projectName = projectName.trim();
			if ( projectName.length() > 0 ) {
				IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject( projectName );
				IPath p = project.getLocation();
				if ( p != null ) {
					return p;
				}
			}
		}
		return Path.EMPTY;
	}

	public static String getProjectName( ILaunchConfiguration configuration ) throws CoreException {
		return configuration.getAttribute( ICDTLaunchConfigurationConstants.ATTR_PROJECT_NAME, (String)null );
	}

	protected String getMIVersion( ILaunchConfiguration config ) {
		return MIPlugin.getMIVersion( config );
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.debug.mi.core.AbstractGDBCDIDebugger#doStartSession(org.eclipse.debug.core.ILaunch, org.eclipse.cdt.debug.mi.core.cdi.Session, org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected void doStartSession( ILaunch launch, Session session, IProgressMonitor monitor ) throws CoreException {
/*		
		MISession miSession = getMISession( session );
		try {
			CommandFactory factory = miSession.getCommandFactory();
			MIGDBSetNewConsole newConsole = factory.createMIGDBSetNewConsole();
			miSession.postCommand( newConsole );
			MIInfo info = newConsole.getMIInfo();
			if ( info == null ) {
				throw new MIException( MIPlugin.getResourceString( "src.common.No_answer" ) ); //$NON-NLS-1$
			}
		}
		catch( MIException e ) {
			// We ignore this exception, for example
			// on GNU/Linux the new-console is an error.
		}		
*/		
		
		ILaunchConfiguration config = launch.getLaunchConfiguration();
		//loadSimulatorDll(session, config);
		
		if( IDebugConstants.ATTACH_MODE == false) // for ATTACH
			initializeDefaultOptions(session);   //FOR RUN  
		
		initializeGDBSetCharset(session);    
		initializeLibraries( config, session );
		if ( monitor.isCanceled() ) {
			throw new OperationCanceledException();
		}

		//startLocalGDBSession( config, session, monitor );
		if( pid > 0 )
			startAttachGDBSession( config, session, monitor );
		else
			startLocalGDBSession( config, session, monitor );
	}
	
	protected void initializeDefaultOptions(Session session)
	{
		MISession miSession = getMISession( session );
		try {
			CommandFactory factory = miSession.getCommandFactory();
			CLIHandle handle = factory.createCLIHandle("SIGTRAP noprint nostop ignore");
			miSession.postCommand( handle );
			MIInfo info = handle.getMIInfo();
			if ( info == null ) {
				throw new MIException( MIPlugin.getResourceString( "src.common.No_answer" ) ); //$NON-NLS-1$
			}
		}
		catch( MIException e ) {
			// We ignore this exception, for example
			// on GNU/Linux the new-console is an error.
			e.printStackTrace();
		}		
		
	}

	protected void startLocalGDBSession( ILaunchConfiguration config, Session session, IProgressMonitor monitor ) throws CoreException {
		// TODO: need a better solution for new-console
		MISession miSession = getMISession( session );
		try {
			CommandFactory factory = miSession.getCommandFactory();
			MIGDBSetNewConsole newConsole = factory.createMIGDBSetNewConsole();
			miSession.postCommand( newConsole );
			MIInfo info = newConsole.getMIInfo();
			if ( info == null ) {
				throw new MIException( MIPlugin.getResourceString( "src.common.No_answer" ) ); //$NON-NLS-1$
			}
		}
		catch( MIException e ) {
			// We ignore this exception, for example
			// on GNU/Linux the new-console is an error.
			e.printStackTrace();
		}		
	}

	protected void startAttachGDBSession( ILaunchConfiguration config, Session session, IProgressMonitor monitor ) throws CoreException {
		MISession miSession = getMISession( session );
		CommandFactory factory = miSession.getCommandFactory();

		if ( pid <= 0 ) {
			throw newCoreException( MIPlugin.getResourceString( "src.GDBCDIDebugger2.1" ), null ); //$NON-NLS-1$
		}
		try {
			CLITargetAttach attach = factory.createCLITargetAttach( pid );
			miSession.postCommand( attach );
			MIInfo info = attach.getMIInfo();
			if ( info == null ) {
				throw new MIException( MIPlugin.getResourceString( "src.common.No_answer" ) ); //$NON-NLS-1$
			}
			miSession.getMIInferior().setInferiorPID( pid );
			// @@@ for attach we nee to manually set the connected state
			// attach does not send the ^connected ack
			miSession.getMIInferior().setConnected();
		}
		catch( MIException e ) {
			throw newCoreException( MessageFormat.format( MIPlugin.getResourceString( "src.GDBCDIDebugger2.2" ), new Integer[] { new Integer( pid ) } ), e ); //$NON-NLS-1$
		}
		// @@@ We have to set the suspended state manually
		
		miSession.getMIInferior().setSuspended();
		miSession.getMIInferior().update();
		
	}

	protected void startCoreGDBSession( ILaunchConfiguration config, Session session, IProgressMonitor monitor ) throws CoreException {
		getMISession( session ).getMIInferior().setSuspended();		
		try {
			session.getSharedLibraryManager().update();
		}
		catch( CDIException e ) {
			throw newCoreException( e );
		}
	}

	protected MISession getMISession( Session session ) {
		ICDITarget[] targets = session.getTargets();
		if ( targets.length == 0 || !(targets[0] instanceof Target) )
			return null;
		return ((Target)targets[0]).getMISession();
	}

	protected void initializeLibraries( ILaunchConfiguration config, Session session ) throws CoreException {
		try {
			SharedLibraryManager sharedMgr = session.getSharedLibraryManager();
			boolean autolib = config.getAttribute( IMILaunchConfigurationConstants.ATTR_DEBUGGER_AUTO_SOLIB, IMILaunchConfigurationConstants.DEBUGGER_AUTO_SOLIB_DEFAULT );
			boolean stopOnSolibEvents = config.getAttribute( IMILaunchConfigurationConstants.ATTR_DEBUGGER_STOP_ON_SOLIB_EVENTS, IMILaunchConfigurationConstants.DEBUGGER_STOP_ON_SOLIB_EVENTS_DEFAULT );
			List p = config.getAttribute( IMILaunchConfigurationConstants.ATTR_DEBUGGER_SOLIB_PATH, Collections.EMPTY_LIST );
			ICDITarget[] dtargets = session.getTargets();
			for( int i = 0; i < dtargets.length; ++i ) {
				Target target = (Target)dtargets[i];
				try {
//					sharedMgr.setAutoLoadSymbols( target, autolib );
					sharedMgr.setStopOnSolibEvents( target, stopOnSolibEvents );
					sharedMgr.setDeferredBreakpoint( target, false );
					// The idea is that if the user set autolib, by default
					// we provide with the capability of deferred breakpoints
					// And we set setStopOnSolib events for them(but they should not see those things.
					//
					// If the user explicitly set stopOnSolibEvents well it probably
					// means that they wanted to see those events so do no do deferred breakpoints.
					if ( autolib && !stopOnSolibEvents ) {
						sharedMgr.setStopOnSolibEvents( target, true );
						sharedMgr.setDeferredBreakpoint( target, true );
					}
				}
				catch( CDIException e ) {
					// Ignore this error
					// it seems to be a real problem on many gdb platform
				}
				if ( p.size() > 0 ) {
					String[] oldPaths = sharedMgr.getSharedLibraryPaths( target );
					String[] paths = new String[oldPaths.length + p.size()];
					System.arraycopy( p.toArray( new String[p.size()] ), 0, paths, 0, p.size() );
					System.arraycopy( oldPaths, 0, paths, p.size(), oldPaths.length );
					sharedMgr.setSharedLibraryPaths( target, paths );
				}
				// use file names instead of full paths
				File[] autoSolibs = MICoreUtils.getAutoSolibs( config );
				ArrayList libs = new ArrayList( autoSolibs.length );
				for ( int j = 0; j < autoSolibs.length; ++j )
					libs.add( new File( autoSolibs[j].getName() ) );
				sharedMgr.autoLoadSymbols( (File[])libs.toArray( new File[libs.size()] ) );
				if ( !autolib && !stopOnSolibEvents )
					sharedMgr.setDeferredBreakpoint( target, libs.size() > 0 );
			}
		}
		catch( CDIException e ) {
			throw newCoreException( MIPlugin.getResourceString( "src.GDBDebugger.Error_initializing_shared_lib_options" ) + e.getMessage(), e ); //$NON-NLS-1$
		}
	}

	protected String getWorkingDirectory( ILaunchConfiguration config ) throws CoreException {
		File cwd = getProjectPath( config ).toFile();
		CommandFactory factory = getCommandFactory( config );
		return factory.getWorkingDirectory(cwd);
	}

	protected String getCommandFile( ILaunchConfiguration config ) throws CoreException {
		String gdbinit = config.getAttribute( IMILaunchConfigurationConstants.ATTR_GDB_INIT, IMILaunchConfigurationConstants.DEBUGGER_GDB_INIT_DEFAULT );
		return (gdbinit != null && gdbinit.length() > 0) ? "--command=" + gdbinit : "--nx"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	protected void loadSimulatorDll(Session session, ILaunchConfiguration config)
	{
		try {
			ICProject cproject = getCProject(config);
			
			String prgName= getObjectName( config );

			String objPath = config.getAttribute(IOspLaunchConfigurationConstants.ATTR_CODE_BINARY_SDK_PATH, IOspLaunchConfigurationConstants.DEFAULT_SUMUAL_CODE_BINARY_PATH);
			objPath = PathResolver.getAbsolutePath(objPath, cproject.getProject());
			
			String libPath = "";
			if( objPath.endsWith(IDebugConstants.FILE_SEP_BSLASH))
				libPath = objPath + prgName;
			else
				libPath = objPath + IDebugConstants.FILE_SEP_BSLASH + prgName;
			
			MISession miSession = getMISession( session );
			try {
				CommandFactory factory = miSession.getCommandFactory();
				MIFileSymbolFile fileSymbolFile = factory.createMIFileSymbolFile(libPath);
				miSession.postCommand( fileSymbolFile );
				MIInfo info = fileSymbolFile.getMIInfo();
				if ( info == null ) {
					throw new MIException( MIPlugin.getResourceString( "src.common.No_answer" ) ); //$NON-NLS-1$
				}
			}
			catch( MIException e ) {
				// We ignore this exception, for example
				// on GNU/Linux the new-console is an error.
				e.printStackTrace();
			}		
			
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static IPath getProgramPath(ILaunchConfiguration configuration) throws CoreException {
		String path = configuration.getAttribute(ICDTLaunchConfigurationConstants.ATTR_PROGRAM_NAME, (String)null);
		if (path == null || path.trim().length() == 0) {
			return null;
		}
		return new Path(path);
	}	
	
	public static ICProject getCProject(ILaunchConfiguration configuration) throws CoreException {
		String projectName = getProjectName(configuration);
		if (projectName != null) {
			projectName = projectName.trim();
			if (projectName.length() > 0) {
				IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
				ICProject cProject = CCorePlugin.getDefault().getCoreModel().create(project);
				if (cProject != null && cProject.exists()) {
					return cProject;
				}
			}
		}
		return null;
	}
	
	protected String getObjectName(ILaunchConfiguration config) throws CoreException {

		String projectName = getProjectName(config);
		if (projectName == null) return null;
		projectName = projectName.trim();
		
		ICProject cproject = null;
		IProject project = null;
		
		if (projectName.length() > 0) {
			project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
			cproject = CCorePlugin.getDefault().getCoreModel().create(project);
			if (cproject == null || !cproject.exists()) {
				return null;
			}
		}		

		IPath programPath = getProgramPath(config);
		if (programPath == null || programPath.isEmpty()) {
			return null;
		}
		
		return programPath.lastSegment().toString();
	}

	protected void initializeGDBSetCharset(Session session) {
		MISession miSession = getMISession(session);

		try {
			CommandFactory factory = miSession.getCommandFactory();
			MIGDBSet setWideCharset = factory.createMIGDBSet(new String[] {
					"target-wide-charset", "UCS-2" });
			miSession.postCommand(setWideCharset);
			MIInfo info = setWideCharset.getMIInfo();

			if (info == null) {
				throw new MIException(MIPlugin.getResourceString("src.common.No_answer")); //$NON-NLS-1$
			}
		} catch (MIException e) {
			// We ignore this exception, for example
			// on GNU/Linux the new-console is an error.
			e.printStackTrace();
		}
	}
	
	   protected IPath getGDBPath(ILaunch launch) throws CoreException {
			ILaunchConfiguration config = launch.getLaunchConfiguration();
			String command = config.getAttribute(IMILaunchConfigurationConstants.ATTR_DEBUG_NAME,
					IDebugConstants.DEBUGGER_SIMUL_DEBUG_NAME_DEFAULT);
			try {
				
				ICProject cproject = getCProject(config);
				command = PathResolver.getAbsolutePath(command, cproject.getProject());
				
				command = VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution(command, false);
			} catch (Exception e) {
				MIPlugin.log(e);
				// take value of command as it
			}
			return new Path(command);
		}	
}
