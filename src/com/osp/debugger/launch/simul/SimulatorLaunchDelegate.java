/*******************************************************************************
 * Copyright (c) 2004, 2008 QNX Software Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * QNX Software Systems - Initial API and implementation
 * Anton Leherbauer (Wind River Systems) - bugs 205108, 212632, 224187
 *******************************************************************************/
package com.osp.debugger.launch.simul; 

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.cdt.core.IBinaryParser.IBinaryObject;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.debug.core.CDIDebugModel;
import org.eclipse.cdt.debug.core.ICDIDebugger;
import org.eclipse.cdt.debug.core.ICDIDebugger2;
import org.eclipse.cdt.debug.core.ICDTLaunchConfigurationConstants;
import org.eclipse.cdt.debug.core.ICDebugConfiguration;
import org.eclipse.cdt.debug.core.cdi.CDIException;
import org.eclipse.cdt.debug.core.cdi.ICDISession;
import org.eclipse.cdt.debug.core.cdi.model.ICDIRuntimeOptions;
import org.eclipse.cdt.debug.core.cdi.model.ICDITarget;
import org.eclipse.cdt.launch.AbstractCLaunchDelegate;
import org.eclipse.cdt.launch.internal.ui.LaunchMessages;
import org.eclipse.cdt.launch.internal.ui.LaunchUIPlugin;
import org.eclipse.cdt.utils.pty.PTY;
import org.eclipse.cdt.utils.spawner.ProcessFactory;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.IStatusHandler;
import org.eclipse.debug.core.model.IProcess;

import com.osp.ide.IConstants;
import com.osp.debugger.IDebugConstants;
import com.osp.debugger.core.DebuggerMessages;
import com.osp.debugger.launch.IOspLaunchConfigurationConstants;
import com.osp.debugger.launch.OspLaunchMessages;
import com.osp.debugger.launch.OspLaunchUtils;
import com.osp.ide.core.PathResolver;
import com.osp.ide.message.socket.NetManager;
import com.osp.ide.message.socket.SimulatorSocket;
import com.osp.ide.utils.FileUtil;
import com.osp.ide.utils.WorkspaceUtils;


/**
 * The launch configuration delegate for the CDI debugger session types.
 */
public class SimulatorLaunchDelegate extends AbstractCLaunchDelegate {

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.launch.AbstractCLaunchDelegate#launch(org.eclipse.debug.core.ILaunchConfiguration, java.lang.String, org.eclipse.debug.core.ILaunch, org.eclipse.core.runtime.IProgressMonitor)
	 */
	
	protected ICProject project = null;
	
	public boolean buildForLaunch(ILaunchConfiguration configuration, String mode, IProgressMonitor monitor) throws CoreException {
		return super.buildForLaunch(configuration, mode, monitor);
	}
	
	protected boolean isAlreadyLaunched(ILaunch thisLaunch)
	{
		ILaunch[] launches = DebugPlugin.getDefault().getLaunchManager().getLaunches();
		if( launches != null && launches.length > 0 )
		{
			for(int i = 0; i < launches.length; i++)
			{
				if( thisLaunch != launches[i] )
				{
					if( !launches[i].isTerminated() )
					{
						String badaLaunchType = launches[i].getAttribute(IOspLaunchConfigurationConstants.ATTR_BADA_LAUNCH_TYPE);
						if( badaLaunchType != null && IOspLaunchConfigurationConstants.BADA_LAUNCH_SIMULATOR.equals(badaLaunchType))
							return true;
					}
				}
			}
		}
		
		return false;
	}
	
	public void launch( ILaunchConfiguration config, String mode, ILaunch launch, IProgressMonitor monitor ) throws CoreException {

		launch.setAttribute(IOspLaunchConfigurationConstants.ATTR_BADA_LAUNCH_TYPE, IOspLaunchConfigurationConstants.BADA_LAUNCH_SIMULATOR);
		
		if( isAlreadyLaunched(launch) )
		{
			abort("Simulator already launched.", null, //$NON-NLS-1$
					ICDTLaunchConfigurationConstants.ERR_UNSPECIFIED_PROGRAM);			
		}
		
		NetManager netManager = NetManager.getInstance();
		SimulatorSocket simSocket = netManager.getSimulatorSocket();
		if( simSocket != null )
		{
			if(simSocket.IsRunning())
				simSocket.stop();
		}
		
		
		netManager.startServer();
		
		if ( monitor == null ) {
			monitor = new NullProgressMonitor();
		}
		if ( mode.equals( ILaunchManager.RUN_MODE ) ) {
			runLocalApplication( config, launch, monitor );
		}
		if ( mode.equals( ILaunchManager.DEBUG_MODE ) ) {
			if( IDebugConstants.ATTACH_MODE ) // for ATTACH
				attachSimulatorDebugger( config, launch, monitor );
			else
				launchDebugger( config, launch, monitor );  // for RUN
		}
	}
	
    /**
     * Simulator를 구동시키도  프로그램을  실행 한다.
     * 
     * @see com.osp.debugger.launch.simul
     */
	
	protected void runLocalApplication( ILaunchConfiguration config, ILaunch launch, IProgressMonitor monitor ) throws CoreException {
		monitor.beginTask( DebuggerMessages.getString( "SumulatorLaunchDelegate.0" ), 20 ); //$NON-NLS-1$
		if ( monitor.isCanceled() ) {
			NetManager.getInstance().stopServer();
			return;
		}
		monitor.worked( 1 );
		try {

			// Copy Exe to Simulator fileSystem
			IPath exePath = verifyProgramPath( config );
			project = verifyCProject( config );
		
			copyResource(project.getProject(), monitor);
			copyExeFile(config, project.getProject(), exePath.toOSString());
			
			monitor.worked( 2 );
			
			// Show Output View
			WorkspaceUtils.showOutputView();
			monitor.worked( 2 );
			
			// Run Simulator
//			String sdkRoot = IdePlugin.getDefault().getSDKPath(project.getProject());
 
			File wd = new File(SimulatorPathHelper.getSimulatorDirectory("", project.getProject()));
			String arguments[] = getProgramArgumentsArray( config );
			
			ArrayList command = new ArrayList( 1 + arguments.length );
			
			command.add( SimulatorPathHelper.getSimulatorPath("", project.getProject()) );
			command.addAll( Arrays.asList( arguments ) );

			String[] commandArray = (String[])command.toArray( new String[command.size()] );
			boolean usePty = config.getAttribute( ICDTLaunchConfigurationConstants.ATTR_USE_TERMINAL, ICDTLaunchConfigurationConstants.USE_TERMINAL_DEFAULT );
			monitor.worked( 2 );
			Process process = exec( commandArray, getEnvironment( config ), wd, usePty );
			monitor.worked( 3 );
			DebugPlugin.newProcess( launch, process, renderProcessLabel( commandArray[0] ) );
			
			String appId = PathResolver.getApplicatuonID(project.getProject());
			runSimulatorProgram(launch, appId, exePath.lastSegment(), monitor);
			
			monitor.worked( 12 );
			
		}
		finally {
			monitor.done();
		}		
	}

    /**
     * Simulator에 attach 후 디버깅한다.
     * 
     * @see com.osp.debugger.launch.simul
     */
		
	protected void attachSimulatorDebugger( ILaunchConfiguration config, ILaunch launch, IProgressMonitor monitor ) throws CoreException {
		if ( monitor.isCanceled() ) {
			return;
		}
		
		monitor.worked( 1 );
		
		IPath exePath = verifyProgramPath( config );
		project = verifyCProject( config );
		
		IPath sdkPath = verifySimulatorPath( "" );
		IBinaryObject sdkFile = null;
		if ( sdkPath != null ) {
			sdkFile = verifyBinary( project, sdkPath );
		}
	
		copyResource(project.getProject(), monitor);
		copyExeFile(config, project.getProject(), exePath.toOSString());
		
		monitor.worked( 2 );
		
		// Show Output View
		WorkspaceUtils.showOutputView();
		monitor.worked( 2 );
		
		// Run Simulator
		File wdSim = new File(SimulatorPathHelper.getSimulatorDirectory("", project.getProject()));
		String argumentsSim[] = getProgramArgumentsArray( config );
		
		ArrayList commandSim = new ArrayList( 1 + argumentsSim.length );
		
		commandSim.add( SimulatorPathHelper.getSimulatorPath("", project.getProject()) );
		commandSim.addAll( Arrays.asList( argumentsSim ) );

		String[] commandArray = (String[])commandSim.toArray( new String[commandSim.size()] );
		boolean usePty = config.getAttribute( ICDTLaunchConfigurationConstants.ATTR_USE_TERMINAL, ICDTLaunchConfigurationConstants.USE_TERMINAL_DEFAULT );
		monitor.worked( 2 );
		Process process = exec( commandArray, getEnvironment( config ), wdSim, usePty );
		monitor.worked( 3 );
		DebugPlugin.newProcess( launch, process, renderProcessLabel( commandArray[0] ) );
		
		String appId = PathResolver.getApplicatuonID(project.getProject());
		waitAndInstallPackage(launch, appId, monitor);
		
		
		int pid = getSimulatorPid();
		if ( pid == -1 ) {
			cancel( LaunchMessages.getString( "LocalCDILaunchDelegate.4" ), ICDTLaunchConfigurationConstants.ERR_NO_PROCESSID ); //$NON-NLS-1$
		}
		
//		if(false)
//		{
		
		IBinaryObject exeFile = null;
		if ( exePath != null ) {
			exeFile = verifyBinary( project, exePath );
		}

		ICDebugConfiguration debugConfig = getDebugConfig( config );

		setDefaultSourceLocator( launch, config );

		ICDISession dsession = createAttachSession( config, launch, debugConfig, sdkFile, pid, monitor );
		monitor.worked( 6 );

		try {
			ICDITarget[] targets = dsession.getTargets();
			for( int i = 0; i < targets.length; i++ ) {
				CDIDebugModel.newDebugTarget( launch, project.getProject(), targets[i], renderTargetLabel( debugConfig ), null, sdkFile, true, true, false );
			}
			
			monitor.worked( 1 );	
			resumeAndSendStartMessage( dsession, appId, exePath.lastSegment());
			monitor.worked( 1 );
		}
		catch( CoreException e ) {
			try {
				dsession.terminate();
				
				if( process != null ) process.destroy();
			}
			catch( CDIException e1 ) {
				// ignore
			}
			throw e;
		}
		finally {
			monitor.done();
		}		
	}
	
    /**
     * Simulator를 구동 후 디버깅한다.
     * 
     * @see com.osp.debugger.launch.simul
     */

	
	protected void launchDebugger( ILaunchConfiguration config, ILaunch launch, IProgressMonitor monitor ) throws CoreException {
		monitor.beginTask( DebuggerMessages.getString( "SumulatorLaunchDelegate.1" ), 17 ); //$NON-NLS-1$
		if ( monitor.isCanceled() ) {
			NetManager.getInstance().stopServer();
			return;
		}
		try {
			OspLaunchUtils.changeConsoleViewOption();
			
			monitor.subTask( DebuggerMessages.getString( "SumulatorLaunchDelegate.2" ) ); //$NON-NLS-1$
			ICDISession dsession = null;
			try {
				IPath exePath = verifyProgramPath( config );
				project = verifyCProject( config );
				IBinaryObject exeFile = null;
				if ( exePath != null ) {
					exeFile = verifyBinary( project, exePath );
				}
				
				IPath sdkPath = verifySimulatorPath( "" );
				IBinaryObject sdkFile = null;
				if ( sdkPath != null ) {
					sdkFile = verifyBinary( project, sdkPath );
				}
				
				copyResource(project.getProject(), monitor);
				if ( exePath != null ) 
					copyExeFile(config, project.getProject(), exePath.toOSString());
				monitor.worked( 2 );				
				
				// Show Output View
				WorkspaceUtils.showOutputView();
				monitor.worked( 1 );
				

				ICDebugConfiguration debugConfig = getDebugConfig( config );

				setDefaultSourceLocator( launch, config );


				dsession = createCDISession( config, launch, debugConfig, monitor );
				monitor.worked( 5 );

				setRuntimeOptions( config, dsession );
				monitor.worked( 1 );

				//boolean stopInMain = config.getAttribute( ICDTLaunchConfigurationConstants.ATTR_DEBUGGER_STOP_AT_MAIN, false );
				String stopSymbol = null;
				//if ( stopInMain )
				//	stopSymbol = launch.getLaunchConfiguration().getAttribute( ICDTLaunchConfigurationConstants.ATTR_DEBUGGER_STOP_AT_MAIN_SYMBOL, ICDTLaunchConfigurationConstants.DEBUGGER_STOP_AT_MAIN_SYMBOL_DEFAULT );
				ICDITarget[] targets = dsession.getTargets();
				for( int i = 0; i < targets.length; i++ ) {
					Process process = targets[i].getProcess();
					IProcess iprocess = null;
					if ( process != null ) {
//						iprocess = DebugPlugin.newProcess( launch, process, renderProcessLabel( exePath.toOSString() ), getDefaultProcessMap() );
						iprocess = DebugPlugin.newProcess( launch, process, renderProcessLabel( sdkPath.toOSString() ), getDefaultProcessMap() );
					}
					//CDIDebugModel.newDebugTarget( launch, project.getProject(), targets[i], renderTargetLabel( debugConfig ), iprocess, sdkFile, true, false, stopSymbol, true );
					SumulatorDebugModel.newDebugTarget( launch, project.getProject(), targets[i], renderTargetLabel( debugConfig ), iprocess, sdkFile, true, false, stopSymbol, true );
				}
				
				monitor.worked( 1 );
				String appId = PathResolver.getApplicatuonID(project.getProject());
				runSimulatorProgram(launch, appId, exePath.lastSegment(), monitor);
			}
			catch( CoreException e ) {
				try {
					if ( dsession != null )
						dsession.terminate();
				}
				catch( CDIException e1 ) {
					// ignore
					e.printStackTrace();
				}
				throw e;
			}
			finally {
				monitor.done();
			}		

		}
		finally {
			monitor.done();
		}		
	}


	protected ICDISession launchOldDebugSession( ILaunchConfiguration config, ILaunch launch, ICDIDebugger debugger, IProgressMonitor monitor ) throws CoreException {
		IBinaryObject exeFile = null;
		IPath exePath = verifyProgramPath( config );
		ICProject project = verifyCProject( config );
		if ( exePath != null ) {
			exeFile = verifyBinary( project, exePath );
		}
		return debugger.createDebuggerSession( launch, exeFile, monitor );
	}

	protected ICDISession launchDebugSession( ILaunchConfiguration config, ILaunch launch, ICDIDebugger2 debugger, IProgressMonitor monitor ) throws CoreException {
		//IPath path = verifyProgramPath( config );
		IPath path = verifySimulatorPath( "" );
		File exeFile = path != null ? path.toFile() : null;
		return debugger.createSession( launch, exeFile, monitor );
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.launch.AbstractCLaunchDelegate#getPluginID()
	 */
	protected String getPluginID() {
		return LaunchUIPlugin.getUniqueIdentifier();
	}

	/**
	 * Performs a runtime exec on the given command line in the context of the
	 * specified working directory, and returns the resulting process. If the
	 * current runtime does not support the specification of a working
	 * directory, the status handler for error code
	 * <code>ERR_WORKING_DIRECTORY_NOT_SUPPORTED</code> is queried to see if
	 * the exec should be re-executed without specifying a working directory.
	 * 
	 * @param cmdLine
	 *            the command line
	 * @param workingDirectory
	 *            the working directory, or <code>null</code>
	 * @return the resulting process or <code>null</code> if the exec is
	 *         cancelled
	 * @see Runtime
	 */
	protected Process exec( String[] cmdLine, String[] environ, File workingDirectory, boolean usePty ) throws CoreException {
		Process p = null;
		try {
			if ( workingDirectory == null ) {
				p = ProcessFactory.getFactory().exec( cmdLine, environ );
			}
			else {
				if ( usePty && PTY.isSupported() ) {
					p = ProcessFactory.getFactory().exec( cmdLine, environ, workingDirectory, new PTY() );
				}
				else {
					p = ProcessFactory.getFactory().exec( cmdLine, environ, workingDirectory );
				}
			}
		}
		catch( IOException e ) {
			if ( p != null ) {
				p.destroy();
			}
			abort( DebuggerMessages.getString( "SumulatorLaunchDelegate.8" ), e, ICDTLaunchConfigurationConstants.ERR_INTERNAL_ERROR ); //$NON-NLS-1$
		}
		catch( NoSuchMethodError e ) {
			// attempting launches on 1.2.* - no ability to set working
			// directory
			IStatus status = new Status( IStatus.ERROR, LaunchUIPlugin.getUniqueIdentifier(), ICDTLaunchConfigurationConstants.ERR_WORKING_DIRECTORY_NOT_SUPPORTED, DebuggerMessages.getString( "SumulatorLaunchDelegate.9" ), e ); //$NON-NLS-1$
			IStatusHandler handler = DebugPlugin.getDefault().getStatusHandler( status );
			if ( handler != null ) {
				Object result = handler.handleStatus( status, this );
				if ( result instanceof Boolean && ((Boolean)result).booleanValue() ) {
					p = exec( cmdLine, environ, null, usePty );
				}
			}
		}
		return p;
	}

	
	protected Process exec2( String[] cmdLine, String[] environ, File workingDirectory, boolean usePty ) throws CoreException {
		Process p = null;
		try {
			if ( workingDirectory == null ) {
				p = Runtime.getRuntime().exec( cmdLine, environ );
			}
			else {
				if ( usePty && PTY.isSupported() ) {
					p = Runtime.getRuntime().exec( cmdLine, environ, workingDirectory);
				}
				else {
					p = Runtime.getRuntime().exec( cmdLine, environ, workingDirectory );
				}
			}
		}
		catch( IOException e ) {
			if ( p != null ) {
				p.destroy();
			}
			abort( DebuggerMessages.getString( "SumulatorLaunchDelegate.8" ), e, ICDTLaunchConfigurationConstants.ERR_INTERNAL_ERROR ); //$NON-NLS-1$
		}
		catch( NoSuchMethodError e ) {
			// attempting launches on 1.2.* - no ability to set working
			// directory
			IStatus status = new Status( IStatus.ERROR, LaunchUIPlugin.getUniqueIdentifier(), ICDTLaunchConfigurationConstants.ERR_WORKING_DIRECTORY_NOT_SUPPORTED, DebuggerMessages.getString( "SumulatorLaunchDelegate.9" ), e ); //$NON-NLS-1$
			IStatusHandler handler = DebugPlugin.getDefault().getStatusHandler( status );
			if ( handler != null ) {
				Object result = handler.handleStatus( status, this );
				if ( result instanceof Boolean && ((Boolean)result).booleanValue() ) {
					p = exec( cmdLine, environ, null, usePty );
				}
			}
		}
		return p;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.cdt.launch.AbstractCLaunchDelegate#preLaunchCheck(org.eclipse.debug.core.ILaunchConfiguration, java.lang.String, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public boolean preLaunchCheck( ILaunchConfiguration config, String mode, IProgressMonitor monitor ) throws CoreException {
		// no pre launch check for core file
		if ( mode.equals( ILaunchManager.DEBUG_MODE ) ) {
			if ( ICDTLaunchConfigurationConstants.DEBUGGER_MODE_CORE.equals( config.getAttribute( ICDTLaunchConfigurationConstants.ATTR_DEBUGGER_START_MODE, ICDTLaunchConfigurationConstants.DEBUGGER_MODE_RUN ) ) )
					return true; 
		}
		return super.preLaunchCheck( config, mode, monitor );
	}

	protected void setRuntimeOptions( ILaunchConfiguration config, ICDISession session ) throws CoreException {
		String arguments[] = getProgramArgumentsArray( config );
		try {
			ICDITarget[] dtargets = session.getTargets();
			for( int i = 0; i < dtargets.length; ++i ) {
				ICDIRuntimeOptions opt = dtargets[i].getRuntimeOptions();
				opt.setArguments( arguments );
				File wd = getWorkingDirectory( config );
				if ( wd != null ) {
					opt.setWorkingDirectory( wd.getAbsolutePath() );
				}
				opt.setEnvironment( getEnvironmentAsProperty( config ) );
			}
		}
		catch( CDIException e ) {
			abort( DebuggerMessages.getString( "SumulatorLaunchDelegate.10" ), e, ICDTLaunchConfigurationConstants.ERR_INTERNAL_ERROR ); //$NON-NLS-1$
		}
	}

	protected ICDISession createCDISession( ILaunchConfiguration config, ILaunch launch, ICDebugConfiguration debugConfig, IProgressMonitor monitor ) throws CoreException {
		ICDISession session = null;
		ICDIDebugger debugger = debugConfig.createDebugger();
		if ( debugger instanceof ICDIDebugger2 )
			session = launchDebugSession( config, launch, (ICDIDebugger2)debugger, monitor );
		else
			// support old debugger types
			session = launchOldDebugSession( config, launch, debugger, monitor );
		return session;
	}
	
	protected ICDISession createAttachSession( ILaunchConfiguration config, ILaunch launch, ICDebugConfiguration debugConfig, IBinaryObject sdkFile, int pid, IProgressMonitor monitor ) throws CoreException {
		ICDISession session = null;
		ICDIDebugger debugger = debugConfig.createDebugger();
		
		if ( debugger instanceof SimulatorDebugger )
		{
			((SimulatorDebugger)debugger).setPid(pid);
		}

		if ( debugger instanceof ICDIDebugger2 )
			session = launchDebugSession( config, launch, (ICDIDebugger2)debugger, monitor );
		else
			// support old debugger types
			session = launchOldDebugSession( config, launch, debugger, monitor );

		return session;
	}	
	
	
	
	protected void waitAndInstallPackage(ILaunch launch, String appId, IProgressMonitor monitor )
	{
		// wait for simulator run
		NetManager netManager = NetManager.getInstance();

		boolean sendInstallCommand = false;
		while( true )
		{
			SimulatorSocket socket = netManager.getSimulatorSocket();

			if (netManager.getPhoneStatus().equals(com.osp.ide.message.Constants.BADA_PHONE_STATUS_04)
			    && netManager.getPhoneStatus04ErrorType() == 0
				&& socket!= null) //socket.applicationRunStatus())
				break;

			else if (netManager.getPhoneStatus().equals(com.osp.ide.message.Constants.BADA_PHONE_STATUS_00)
					&& !sendInstallCommand && socket!= null ) {
				String message = Integer.toString(IDebugConstants.DIAG_KIND_AGENT)
						 + "|" + Integer.toString(IDebugConstants.DIAG_AGENT_TKSHELL)
						 + "|" + "AppPkgInstall /OSP/Applications/" + appId;
				netManager.sendMessage(message);
				
				sendInstallCommand = true;
				
				netManager.sendMessage(com.osp.ide.message.Constants.BADA_RM_PROCESS_REQUEST);
			}
			
			if ( monitor.isCanceled() ) {
				netManager.stopServer();
				return;
			}

			if( launch.isTerminated() ) {
				netManager.stopServer();
				return;
			}
			
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}
	
	protected void resumeAndSendStartMessage(ICDISession dsession, String appId, String execFile) throws CoreException
	{
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
		
		// FOR ATTACH : resume
		try {
			ICDITarget[] targets = dsession.getTargets();
			for( int i = 0; i < targets.length; i++ ) {
				targets[i].resume();
			}
		} catch (CDIException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			abort("resume error", null, //$NON-NLS-1$
					ICDTLaunchConfigurationConstants.ERR_INTERNAL_ERROR);
		}
		
		try {
			Thread.sleep(300);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		
		NetManager netManager = NetManager.getInstance();
		
		String message = Integer.toString(IDebugConstants.DIAG_KIND_PROCESS)
		 + "|" + Integer.toString(IDebugConstants.DIAG_PROCESS_EXECUTE)
		 + "|" + "/OSP/Applications/" + appId + "/bin/" + execFile + ",/OSP/Applications/" + appId + "/bin";
		netManager.sendMessage(message);
		
		System.out.println("Sent execute command...");
	}
	
	protected void runSimulatorProgram(ILaunch launch, String appId, String execFile, IProgressMonitor monitor )
	{
/*		
		// wait for simulator run
		while( true )
		{
			if( OutputPlugin.getDefault().getServerSocket().canSend()) break;
			if ( monitor.isCanceled() ) {
				return;
			}
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		String message = path + "/" + execFile + "," + path;
		OutputPlugin.getDefault().getServerSocket().sendMessage(message);
*/
		// wait for simulator run
		NetManager netManager = NetManager.getInstance();

		boolean sendInstallCommand = false;
		while( true )
		{
			SimulatorSocket socket = netManager.getSimulatorSocket();

			if (netManager.getPhoneStatus().equals(com.osp.ide.message.Constants.BADA_PHONE_STATUS_04)
				    && netManager.getPhoneStatus04ErrorType() == 0
					&& socket!= null) //socket.applicationRunStatus())
					break;
			else if (netManager.getPhoneStatus().equals(com.osp.ide.message.Constants.BADA_PHONE_STATUS_00)
					&& !sendInstallCommand && socket!= null ) {
				String message = Integer.toString(IDebugConstants.DIAG_KIND_AGENT)
						 + "|" + Integer.toString(IDebugConstants.DIAG_AGENT_TKSHELL)
						 + "|" + "AppPkgInstall /OSP/Applications/" + appId;
				netManager.sendMessage(message);
				
				sendInstallCommand = true;
				
				netManager.sendMessage(com.osp.ide.message.Constants.BADA_RM_PROCESS_REQUEST);
			}
			
			if ( monitor.isCanceled() ) {
				netManager.stopServer();
				return;
			}

			if( launch.isTerminated() ) {
				netManager.stopServer();
				return;
			}
			
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		String message = Integer.toString(IDebugConstants.DIAG_KIND_PROCESS)
				 + "|" + Integer.toString(IDebugConstants.DIAG_PROCESS_EXECUTE)
				 + "|" + "/OSP/Applications/" + appId + "/bin/" + execFile + ",/OSP/Applications/" + appId + "/bin";
		netManager.sendMessage(message);		
		System.out.println("Sent execute command...");
	}
	
	protected IPath verifySimulatorPath(String sdkName) throws CoreException {
		IPath programPath = new Path( SimulatorPathHelper.getSimulatorPath("", project.getProject()));
		if (programPath == null || programPath.isEmpty()) {
			abort("Simulator path not specified", null, //$NON-NLS-1$
					ICDTLaunchConfigurationConstants.ERR_UNSPECIFIED_PROGRAM);
		}

		if (programPath.toFile() == null || !programPath.toFile().exists()) {
			abort(
					"Simulator Program does not exist", //$NON-NLS-1$
					new FileNotFoundException(
							OspLaunchMessages.getFormattedString(
																"AbstractCLaunchDelegate.PROGRAM_PATH_not_found", programPath.toOSString())), //$NON-NLS-1$
					ICDTLaunchConfigurationConstants.ERR_PROGRAM_NOT_EXIST);
		}
		return programPath;
	}
	
	public String[] getProgramArgumentsArray( ILaunchConfiguration config )  throws CoreException
	{
		return OspLaunchUtils.getProgramArgumentsArray(config, project.getProject());
	}
	
	
	protected void copyExeFile(ILaunchConfiguration config, IProject project, String execPath) throws CoreException 
	{
		try {
			String fileSysPath = config.getAttribute(IOspLaunchConfigurationConstants.ATTR_CODE_BINARY_SDK_PATH, IOspLaunchConfigurationConstants.DEFAULT_SUMUAL_CODE_BINARY_PATH);
			fileSysPath = PathResolver.getAbsolutePath(fileSysPath, project.getProject());
			
			FileUtil fu = new FileUtil();
			if( fu.copyFile(execPath, fileSysPath) == false )
				terminateLaunch(fu.getErrorMsg(), fu.getException());
			
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			terminateLaunch("Error copy exe file", e);
		}
	}
	
	protected void copyResource(IProject project, IProgressMonitor monitor) throws CoreException 
	{
		String dirRoot = PathResolver.getAbsolutePath(IOspLaunchConfigurationConstants.BADA_SIMULATOR_ROOT, project);
		String dir = dirRoot + IConstants.FILE_SEP_BSLASH  + IConstants.BADA_APP_DIR_BIN;
		
		FileUtil fu = new FileUtil();
		
		if( fu.mkDir(dir) == false )
			terminateLaunch(fu.getErrorMsg(), fu.getException());
		
		String libDir = project.getLocation().makeAbsolute() + IConstants.FILE_SEP_BSLASH + IConstants.DIR_LIB;
		if( fu.copyFiles(libDir, dir, IConstants.EXT_DLL) == false)
			terminateLaunch(fu.getErrorMsg(), fu.getException());
		
		// Copy Info 
		dir = dirRoot + IConstants.FILE_SEP_BSLASH  + IConstants.BADA_APP_DIR_INFO;
		if( fu.mkDir(dir) == false )
			terminateLaunch(fu.getErrorMsg(), fu.getException());
		
		if( fu.copyFile(project.getLocation().makeAbsolute() + IConstants.FILE_SEP_BSLASH + IConstants.MANIFEST_FILE, dir) == false )
			terminateLaunch(fu.getErrorMsg(), fu.getException());

		if( fu.copyFile(project.getLocation().makeAbsolute() + IConstants.FILE_SEP_BSLASH + IConstants.APP_XML_FILE, dir) == false )
			terminateLaunch(fu.getErrorMsg(), fu.getException());

		// Copy Data 		
		dir = dirRoot + IConstants.FILE_SEP_BSLASH  + IConstants.BADA_APP_DIR_DATA;
		if (fu.mkDir(dir) == false )
			terminateLaunch(fu.getErrorMsg(), fu.getException());

/*		
		if( fu.copyFile(project.getLocation().makeAbsolute() + IConstants.FILE_SEP_BSLASH + IConstants.DIR_ICON 
				+ IConstants.FILE_SEP_BSLASH + project.getName() + IConstants.EXT_ICON, dir) == false )
			terminateLaunch(fu.getErrorMsg(), fu.getExcettion());
*/
		// Copy icons
        dir = dirRoot + IConstants.FILE_SEP_BSLASH  + IConstants.BADA_APP_DIR_RES;
        String iconDir = project.getLocation().makeAbsolute() + IConstants.FILE_SEP_BSLASH + IConstants.DIR_ICON;

		if( (new File(iconDir)).exists() )
		{
			if( fu.mkDir(dir) == false )
				terminateLaunch(fu.getErrorMsg(), fu.getException());
			if( fu.copyDirectory(iconDir, dir) == false )
				terminateLaunch(fu.getErrorMsg(), fu.getException());
		}
		
		// Copy Res
		dir = dirRoot + IConstants.FILE_SEP_BSLASH  + IConstants.BADA_APP_DIR_RES;
		String resDir = project.getLocation().makeAbsolute() + IConstants.FILE_SEP_BSLASH + IConstants.DIR_RESOURCE;
		
		if( (new File(resDir)).exists() )
		{
			if( fu.mkDir(dir) == false )
				terminateLaunch(fu.getErrorMsg(), fu.getException());
			if( fu.copyDirectory(resDir, dir) == false )
				terminateLaunch(fu.getErrorMsg(), fu.getException());
		}

		// Copy Home		
		dir = dirRoot + IConstants.FILE_SEP_BSLASH  + IConstants.BADA_APP_DIR_DATA;
		String homeDir = project.getLocation().makeAbsolute() + IConstants.FILE_SEP_BSLASH + IConstants.DIR_HOME;
		if( (new File(homeDir)).exists() )
		{
			if( fu.copyHomeFolder(homeDir, dir) == false )
				terminateLaunch(fu.getErrorMsg(), fu.getException());
		}
		
	}

	protected void terminateLaunch(String msg, Throwable exception) throws CoreException 
	{
		abort(msg, exception, //$NON-NLS-1$
				ICDTLaunchConfigurationConstants.ERR_INTERNAL_ERROR);
	}
	
	protected int getSimulatorPid()
	{
		try {
			String line, name;
			Process p = Runtime.getRuntime().exec("tasklist.exe /fo csv /nh");
			BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
			
			while ((line = input.readLine()) != null) {
				if (!line.trim().equals("")) {
					// keep only the process name
					// "dgdersvc.exe","2036","Console","0","1,500 K"
					line = line.substring(1);
					name = line.substring(0, line.indexOf("\""));
					if( name.equals(IDebugConstants.SIMULATOR_NAME))
					{
						input.close();
						
						line = line.substring(line.indexOf("\"")+1);
						// skip ."
						line = line.substring(line.indexOf("\"")+1);
						String pid = line.substring(0, line.indexOf("\""));
						return Integer.parseInt(pid);
					}
				}
			}
			input.close();
		}
		catch (Exception err) {
			err.printStackTrace();
		}
		
		return -1;
		
	}

	
	
}
