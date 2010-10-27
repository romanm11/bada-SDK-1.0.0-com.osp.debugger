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
package com.osp.debugger.launch.target; 

import java.io.File;
import java.util.Collections;
import java.util.List;

import org.eclipse.cdt.core.IBinaryParser.IBinaryObject;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.debug.core.ICDTLaunchConfigurationConstants;
import org.eclipse.cdt.debug.core.cdi.CDIException;
import org.eclipse.cdt.debug.core.cdi.ICDISession;
import org.eclipse.cdt.debug.core.cdi.model.ICDITarget;
import org.eclipse.cdt.debug.mi.core.GDBCDIDebugger;
import org.eclipse.cdt.debug.mi.core.IGDBServerMILaunchConfigurationConstants;
import org.eclipse.cdt.debug.mi.core.IMIConstants;
import org.eclipse.cdt.debug.mi.core.IMILaunchConfigurationConstants;
import org.eclipse.cdt.debug.mi.core.MIException;
import org.eclipse.cdt.debug.mi.core.MIPlugin;
import org.eclipse.cdt.debug.mi.core.MISession;
import org.eclipse.cdt.debug.mi.core.RxThread;
import org.eclipse.cdt.debug.mi.core.cdi.Session;
import org.eclipse.cdt.debug.mi.core.cdi.SharedLibraryManager;
import org.eclipse.cdt.debug.mi.core.cdi.model.Target;
import org.eclipse.cdt.debug.mi.core.command.CLIHandle;
import org.eclipse.cdt.debug.mi.core.command.CommandFactory;
import org.eclipse.cdt.debug.mi.core.command.MIGDBSet;
import org.eclipse.cdt.debug.mi.core.command.MITargetSelect;
import org.eclipse.cdt.debug.mi.core.output.MIInfo;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.IProcess;

import com.osp.debugger.IDebugConstants;
import com.osp.debugger.mi.BadaCommandFactory;
import com.osp.debugger.mi.CLISet;
import com.osp.ide.core.PathResolver;

/**
 * Implementing the cdebugger extension point for basic launch configurations.
 */
public class TargetDebugger extends GDBCDIDebugger {

	private String comPort = "invalid";
	private String applicationId = "";
	private Process broker = null;
	private IProject project;
	
	public void setData(int portNum, String appId, Process broker, IProject project)
	{
		if(portNum > 0) comPort = "//./COM" + Integer.toString(portNum);
		//if(portNum > 0) comPort = "COM" + Integer.toString(portNum);
		applicationId = appId;
		this.broker = broker;
		this.project = project;
	}
	
	public ICDISession createDebuggerSession(ILaunch launch, IBinaryObject exe, IProgressMonitor monitor)
	throws CoreException {
		
		Session session = (Session) super.createDebuggerSession(launch, exe,monitor);
		
		String remote = comPort;
		
		MIPlugin plugin = MIPlugin.getDefault();
		Preferences prefs = plugin.getPluginPreferences();
		int launchTimeout = prefs.getInt(IMIConstants.PREF_REQUEST_LAUNCH_TIMEOUT);
		
		boolean verboseMode = verboseMode( launch.getLaunchConfiguration() );
		
		boolean failed = false;
		try {
			ICDITarget[] targets = session.getTargets();
			for (int i = 0; i < targets.length; ++i) {
				Target target = (Target)targets[i];
				MISession miSession = target.getMISession();
				CommandFactory factory = miSession.getCommandFactory();
			
				//target.enableVerboseMode( verboseMode );

				CLISet setDebugRemote = BadaCommandFactory.createCLISet("debug remote 1"); //$NON-NLS-1$
				miSession.postCommand(setDebugRemote, launchTimeout);
				MIInfo info = setDebugRemote.getMIInfo();
				if (info == null) {
					throw new MIException ("Cannot set debug remote command"); //$NON-NLS-1$
				}
				
				
				MIGDBSet setRemoteTimeout = factory.createMIGDBSet(new String[]{"remotetimeout", "60"}); //$NON-NLS-1$
				miSession.postCommand(setRemoteTimeout, launchTimeout);
				info = setRemoteTimeout.getMIInfo();
				if (info == null) {
					throw new MIException ("Cannot set remote timeout command"); //$NON-NLS-1$
				}
				
				MIGDBSet setTargetAppId = factory.createMIGDBSet(new String[]{"target-app", applicationId}); //$NON-NLS-1$
				miSession.postCommand(setTargetAppId, launchTimeout);
				info = setTargetAppId.getMIInfo();
				if (info == null) {
					throw new MIException ("Cannot set target application ID command"); //$NON-NLS-1$
				}
				
				try {
					Thread.sleep(1500);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}						
				
				MITargetSelect select = factory.createMITargetSelect(new String[] {"remote", remote}); //$NON-NLS-1$
				RxThread rxThread = miSession.getRxThread();
				if( verboseMode ) rxThread.setEnableConsole(false);					
				miSession.postCommand(select, launchTimeout);
				info = select.getMIInfo();
				if( verboseMode ) rxThread.setEnableConsole(true);
				if (info == null) {
					throw new MIException (MIPlugin.getResourceString("src.common.No_answer")); //$NON-NLS-1$
				}
			}
			
			return session;
			
		} catch (Exception e) {
			// Catch all wrap them up and rethrow
			failed = true;
			if (e instanceof CoreException) {
				throw (CoreException)e;
			}
			throw newCoreException(e);
		} finally {
			if (failed) {
				if (session != null) {
					try {
						session.terminate();
					} catch (Exception ex) {
						// ignore the exception here.
					}
				}
			}
		}
			
}	
	
	/* (non-Javadoc)
	 * @see org.eclipse.cdt.debug.mi.core.GDBCDIDebugger#createLaunchSession(org.eclipse.debug.core.ILaunchConfiguration, org.eclipse.cdt.core.IBinaryParser.IBinaryExecutable, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public Session createLaunchSession(ILaunchConfiguration config, IBinaryObject exe, IProgressMonitor monitor)
			throws CoreException {
		Session session = null;
		boolean failed = false;
		try {
			//String gdb = config.getAttribute(IMILaunchConfigurationConstants.ATTR_DEBUG_NAME, IDebugConstants.DEBUGGER_TARGET_DEBUG_NAME_DEFAULT); //$NON-NLS-1$
			String gdb = getGDBPath(config, project);
			String miVersion = getMIVersion(config);
			//File cwd = getProjectPath(config).toFile();
			File cwd = exe.getPath().removeLastSegments(1).toFile();
			
			String gdbinit = config.getAttribute(IMILaunchConfigurationConstants.ATTR_GDB_INIT, IMILaunchConfigurationConstants.DEBUGGER_GDB_INIT_DEFAULT);
			if (config.getAttribute(IGDBServerMILaunchConfigurationConstants.ATTR_REMOTE_TCP, false)) {
				String remote = config.getAttribute(IGDBServerMILaunchConfigurationConstants.ATTR_HOST, "invalid"); //$NON-NLS-1$
				remote += ":"; //$NON-NLS-1$
				remote += config.getAttribute(IGDBServerMILaunchConfigurationConstants.ATTR_PORT, "invalid"); //$NON-NLS-1$
				String[] args = new String[] {"remote", remote}; //$NON-NLS-1$
				session = MIPlugin.getDefault().createCSession(gdb, miVersion, exe.getPath().toFile(), 0, args, cwd, gdbinit, monitor);
			} else {
				MIPlugin plugin = MIPlugin.getDefault();
				Preferences prefs = plugin.getPluginPreferences();
				//int launchTimeout = prefs.getInt(IMIConstants.PREF_REQUEST_LAUNCH_TIMEOUT);

				//String remote = config.getAttribute(IGDBServerMILaunchConfigurationConstants.ATTR_DEV, "invalid"); //$NON-NLS-1$
				//String remote = comPort;
				//String remoteBaud = config.getAttribute(IGDBServerMILaunchConfigurationConstants.ATTR_DEV_SPEED, IDebugConstants.COMM_BAUDRATE_DEFAULT); //$NON-NLS-1$
				//session = MIPlugin.getDefault().createCSession(gdb, miVersion, exe.getPath().toFile(), -1, null, cwd, gdbinit, monitor);
				session = CreateCSession.getDefault().createCSession(gdb, miVersion, null, -1, null, cwd, gdbinit, broker, monitor);
				ICDITarget[] targets = session.getTargets();
				
				boolean verboseMode = verboseMode( config );
				
				for (int i = 0; i < targets.length; ++i) {
					Target target = (Target)targets[i];
					MISession miSession = target.getMISession();
					CommandFactory factory = miSession.getCommandFactory();
					
					target.enableVerboseMode( verboseMode );
/*					
					MIGDBSet setRemoteBaud = factory.createMIGDBSet(new String[]{"remotebaud", remoteBaud}); //$NON-NLS-1$
					// Set serial line parameters
					miSession.postCommand(setRemoteBaud, launchTimeout);
					MIInfo info = setRemoteBaud.getMIInfo();
					if (info == null) {
						throw new MIException (MIPlugin.getResourceString("src.GDBServerDebugger.Can_not_set_Baud")); //$NON-NLS-1$
					}
					
					MITargetSelect select = factory.createMITargetSelect(new String[] {"remote", remote}); //$NON-NLS-1$
					miSession.postCommand(select, launchTimeout);
					select.getMIInfo();
					if (info == null) {
						throw new MIException (MIPlugin.getResourceString("src.common.No_answer")); //$NON-NLS-1$
					}
*/					
				}
			}
			//initializeDefaultOptions(session);
			initializeLibraries(config, session);
			return session;
		} catch (Exception e) {
			// Catch all wrap them up and rethrow
			failed = true;
			if (e instanceof CoreException) {
				throw (CoreException)e;
			}
			throw newCoreException(e);
		} finally {
			if (failed) {
				if (session != null) {
					try {
						session.terminate();
					} catch (Exception ex) {
						// ignore the exception here.
					}
				}
			}
		}

	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.debug.mi.core.GDBCDIDebugger#createAttachSession(org.eclipse.debug.core.ILaunchConfiguration, org.eclipse.cdt.core.IBinaryParser.IBinaryExecutable, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public Session createAttachSession(ILaunchConfiguration config, IBinaryObject exe, IProgressMonitor monitor)
			throws CoreException {
		String msg = MIPlugin.getResourceString("src.GDBServerDebugger.GDBServer_attaching_unsupported"); //$NON-NLS-1$
		throw newCoreException(msg, null);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.debug.mi.core.GDBCDIDebugger#createCoreSession(org.eclipse.debug.core.ILaunchConfiguration, org.eclipse.cdt.core.IBinaryParser.IBinaryExecutable, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public Session createCoreSession(ILaunchConfiguration config, IBinaryObject exe, IProgressMonitor monitor)
			throws CoreException {
		String msg = MIPlugin.getResourceString("src.GDBServerDebugger.GDBServer_corefiles_unsupported"); //$NON-NLS-1$
		throw newCoreException(msg, null);
	}

	protected boolean verboseMode( ILaunchConfiguration config ) {
		boolean result = IMILaunchConfigurationConstants.DEBUGGER_VERBOSE_MODE_DEFAULT; 
		try {
			return config.getAttribute( IMILaunchConfigurationConstants.ATTR_DEBUGGER_VERBOSE_MODE, result );
		}
		catch( CoreException e ) {
			// use default
		}
		return result;
	}
	
	protected MISession getMISession( Session session ) {
		ICDITarget[] targets = session.getTargets();
		if ( targets.length == 0 || !(targets[0] instanceof Target) )
			return null;
		return ((Target)targets[0]).getMISession();
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

	protected void initializeLibraries(ILaunchConfiguration config, Session session) throws CoreException {
		try {
			SharedLibraryManager sharedMgr = session.getSharedLibraryManager();
			boolean autolib = config.getAttribute(IMILaunchConfigurationConstants.ATTR_DEBUGGER_AUTO_SOLIB, IMILaunchConfigurationConstants.DEBUGGER_AUTO_SOLIB_DEFAULT);
			boolean stopOnSolibEvents = config.getAttribute(IMILaunchConfigurationConstants.ATTR_DEBUGGER_STOP_ON_SOLIB_EVENTS, IMILaunchConfigurationConstants.DEBUGGER_STOP_ON_SOLIB_EVENTS_DEFAULT);
			List p = config.getAttribute(IMILaunchConfigurationConstants.ATTR_DEBUGGER_SOLIB_PATH, Collections.EMPTY_LIST);
			ICDITarget[] dtargets = session.getTargets();
			for (int i = 0; i < dtargets.length; ++i) {
				Target target = (Target)dtargets[i];
				try {
					sharedMgr.setAutoLoadSymbols(target, autolib);
					sharedMgr.setStopOnSolibEvents(target, stopOnSolibEvents);
					// The idea is that if the user set autolib, by default
					// we provide with the capability of deferred breakpoints
					// And we set setStopOnSolib events for them(but they should not see those things.
					//
					// If the user explicitly set stopOnSolibEvents well it probably
					// means that they wanted to see those events so do no do deferred breakpoints.
					if (autolib && !stopOnSolibEvents) {
						sharedMgr.setStopOnSolibEvents(target, true);
						sharedMgr.setDeferredBreakpoint(target, true);
					}
				} catch (CDIException e) {
					// Ignore this error
					// it seems to be a real problem on many gdb platform
				}
				if (p.size() > 0) {
					String[] oldPaths = sharedMgr.getSharedLibraryPaths(target);
					String[] paths = new String[oldPaths.length + p.size()];
					
					//System.arraycopy(p.toArray(new String[p.size()]), 0, paths, 0, p.size());
					//System.arraycopy(oldPaths, 0, paths, p.size(), oldPaths.length);
					for( int pos = 0; pos < p.size(); pos++ )
					{
						paths[pos] = PathResolver.getAbsoluteCygwinPath((String)p.get(pos), project);
					}
					
					for( int pos = p.size(); pos < p.size()+oldPaths.length; pos++ )
					{
						paths[pos] = oldPaths[pos-p.size()];
					}
					
					sharedMgr.setSharedLibraryPaths(target, paths);
				}
			}
		} catch (CDIException e) {
			throw newCoreException(MIPlugin.getResourceString("src.GDBDebugger.Error_initializing_shared_lib_options") + e.getMessage(), e); //$NON-NLS-1$
		}
	}
	
	   protected String getGDBPath(ILaunchConfiguration config, IProject prj) throws CoreException {
			String gdb ="";
			try {

				gdb = config.getAttribute(IMILaunchConfigurationConstants.ATTR_DEBUG_NAME, IDebugConstants.DEBUGGER_TARGET_DEBUG_NAME_DEFAULT); //$NON-NLS-1$
				gdb = PathResolver.getAbsolutePath(gdb, prj);
			} catch (Exception e) {
				MIPlugin.log(e);
				// take value of command as it
			}
			return gdb;
		}		
	
}
