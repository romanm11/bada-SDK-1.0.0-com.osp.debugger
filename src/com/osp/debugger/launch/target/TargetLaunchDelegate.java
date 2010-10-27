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
package com.osp.debugger.launch.target; 


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

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
import org.eclipse.cdt.launch.internal.ui.LaunchUIPlugin;
import org.eclipse.cdt.utils.pty.PTY;
import org.eclipse.cdt.utils.spawner.ProcessFactory;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.IStatusHandler;
import org.eclipse.debug.core.model.IProcess;

import com.osp.debugger.IDebugConstants;
import com.osp.debugger.core.DebuggerMessages;
import com.osp.debugger.launch.IOspLaunchConfigurationConstants;
import com.osp.ide.IConstants;
import com.osp.ide.core.PathResolver;
import com.osp.ide.message.socket.NetManager;
import com.osp.ide.message.socket.SimulatorSocket;
import com.osp.ide.utils.AnalysisUtil;
import com.osp.ide.utils.BrokerUtils;
import com.osp.ide.utils.FileUtil;
import com.osp.ide.utils.WorkspaceUtils;
 

/**
 * The launch configuration delegate for the CDI debugger session types.
 */
public class TargetLaunchDelegate extends AbstractCLaunchDelegate {

	protected ICProject fCProject = null;
	protected static final int PORT_FAIL 	= -1;
	protected static final int MONITOR_ABORTED =  -2;

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
						if( badaLaunchType != null && IOspLaunchConfigurationConstants.BADA_LAUNCH_TARGET.equals(badaLaunchType))
							return true;
					}
				}
			}
		}
		
		return false;
	}	
	
	/* (non-Javadoc)
	 * @see org.eclipse.cdt.launch.AbstractCLaunchDelegate#launch(org.eclipse.debug.core.ILaunchConfiguration, java.lang.String, org.eclipse.debug.core.ILaunch, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void launch( ILaunchConfiguration config, String mode, ILaunch launch, IProgressMonitor monitor ) throws CoreException {
		
		launch.setAttribute(IOspLaunchConfigurationConstants.ATTR_BADA_LAUNCH_TYPE, IOspLaunchConfigurationConstants.BADA_LAUNCH_TARGET);
		
		if( isAlreadyLaunched(launch) )
		{
			abort("Target already launched.", null, //$NON-NLS-1$
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
			runTargetApplication( config, launch, monitor );
		}
		if ( mode.equals( ILaunchManager.DEBUG_MODE ) ) {
			launchDebugger( config, launch, monitor );
			//runLocalApplication( config, launch, monitor );
		}
	}

	protected void runTargetApplication( ILaunchConfiguration config, ILaunch launch, IProgressMonitor monitor ) throws CoreException {
		monitor.beginTask( DebuggerMessages.getString( "TargetLaunchDelegate.0" ), 10 ); //$NON-NLS-1$
		if ( monitor.isCanceled() ) {
			return;
		}
		monitor.worked( 1 );
		try {
/*			
			IPath exePath = verifyProgramPath( config );
			File wd = getWorkingDirectory( config );
			if ( wd == null ) {
				wd = new File( System.getProperty( "user.home", "." ) ); //$NON-NLS-1$ //$NON-NLS-2$
			}
*/
			IPath exePath = verifyProgramPath( config );
			fCProject = verifyCProject( config );
			
			boolean bCopyRes = config.getAttribute(IOspLaunchConfigurationConstants.ATTR_UPDATE_RESOURCE, IOspLaunchConfigurationConstants.DEFAULT_UPDATE_RESOURCE); 
			if( bCopyRes )
				copyResource(fCProject.getProject(), monitor);
			else 
				copyRootDir(fCProject.getProject(), monitor);
			
			boolean bCopyBinary = config.getAttribute(IOspLaunchConfigurationConstants.ATTR_UPDATE_BINARY, IOspLaunchConfigurationConstants.DEFAULT_UPDATE_BINARY);
			if( bCopyBinary )
			{
				copyLibFiles(fCProject.getProject(), monitor);
				
				copyExeFile(config, fCProject.getProject(), exePath.toOSString());
				copyAnaysisFiles(fCProject.getProject(), exePath, monitor);
			}
			
			copyXmlFiles(fCProject.getProject(), monitor);
			
			
			// Show Output View
			WorkspaceUtils.showOutputView();			
			
			ArrayList brokerCommand = new ArrayList();

			File wdBroker = BrokerUtils.getBrokerDirectory(fCProject.getProject());
			if ( wdBroker == null ) {
				abort( DebuggerMessages.getString( "TargetLaunchDelegate.20" ), null, ICDTLaunchConfigurationConstants.ERR_INTERNAL_ERROR ); //$NON-NLS-1$
			}
			
			String brokerExe = BrokerUtils.verifyBrokerFile(fCProject.getProject());
			if ( brokerExe == null || brokerExe.length() == 0 ) {
				abort( DebuggerMessages.getString( "TargetLaunchDelegate.20" ), null, ICDTLaunchConfigurationConstants.ERR_INTERNAL_ERROR ); //$NON-NLS-1$
			}
			
			IFolder infoFolder = FileUtil.createFolder(fCProject.getProject(), IConstants.DIR_DEBUGINFO, monitor);
			if (infoFolder == null){
				abort( "Can not create " + IConstants.DIR_DEBUGINFO + " dictory.", null, ICDTLaunchConfigurationConstants.ERR_INTERNAL_ERROR ); //$NON-NLS-1$
			}
			
			String appId = PathResolver.getApplicatuonID(fCProject.getProject());
			String targetPort = config.getAttribute(IOspLaunchConfigurationConstants.ATTR_TARGET_PORT, IDebugConstants.TARGET_PORT_DEFAULT);
			String targetAppRoot = getTargetFsPath(fCProject.getProject());
			String targetExePath =  targetAppRoot + IConstants.FILE_SEP_BSLASH  + IConstants.BADA_APP_DIR_BIN + IConstants.FILE_SEP_BSLASH + exePath.lastSegment();  
			
			brokerCommand.add(brokerExe);
//			command.add("--ddm");
			brokerCommand.add("-i");
			brokerCommand.add(appId);

			brokerCommand.add("-l");
			if (bCopyBinary) {
				brokerCommand.add(targetExePath);
			} else {
				brokerCommand.add(exePath.toOSString());
			}

			brokerCommand.add("-f");
			brokerCommand.add( targetAppRoot );

			// if debug
			//command.add("-g");
			
			brokerCommand.add("-r");
			brokerCommand.add( getApplicationRoot(config));
			
			brokerCommand.add("-p");
			brokerCommand.add(targetPort);
			
			brokerCommand.add("--dbginfo-folder=" + infoFolder.getLocation().toOSString());
			
			String[] brokerCommandArray = (String[])brokerCommand.toArray( new String[brokerCommand.size()] );
			boolean usePty = config.getAttribute( ICDTLaunchConfigurationConstants.ATTR_USE_TERMINAL, ICDTLaunchConfigurationConstants.USE_TERMINAL_DEFAULT );
			monitor.worked( 2 );
			Process processBroker = exec( brokerCommandArray, getEnvironment( config ), wdBroker, usePty );
			monitor.worked( 6 );
			IProcess process = DebugPlugin.newProcess( launch, processBroker, renderProcessLabel( brokerCommandArray[0] ) );
			
			NetManager.getInstance().setBroker(processBroker);
			
			TargetRunLaunchListner trl = new TargetRunLaunchListner(launch);
		
		}
		finally {
			monitor.done();
		}		
	}

	protected void launchDebugger( ILaunchConfiguration config, ILaunch launch, IProgressMonitor monitor ) throws CoreException {
		monitor.beginTask( DebuggerMessages.getString( "TargetLaunchDelegate.1" ), 14 ); //$NON-NLS-1$
		if ( monitor.isCanceled() ) {
			return;
		}
		try {
			monitor.subTask( DebuggerMessages.getString( "TargetLaunchDelegate.2" ) ); //$NON-NLS-1$
			ICDISession dsession = null;
			Process processBroker = null;
			try {
				
				IPath exePath = verifyProgramPath( config );
				fCProject = verifyCProject( config );
				
				boolean bCopyRes = config.getAttribute(IOspLaunchConfigurationConstants.ATTR_UPDATE_RESOURCE, IOspLaunchConfigurationConstants.DEFAULT_UPDATE_RESOURCE); 
				if( bCopyRes )
					copyResource(fCProject.getProject(), monitor);
				else 
					copyRootDir(fCProject.getProject(), monitor);
				
				boolean bCopyBinary = config.getAttribute(IOspLaunchConfigurationConstants.ATTR_UPDATE_BINARY, IOspLaunchConfigurationConstants.DEFAULT_UPDATE_BINARY);
				if( bCopyBinary )
				{
					copyLibFiles(fCProject.getProject(), monitor);
					
					copyExeFile(config, fCProject.getProject(), exePath.toOSString());
					copyAnaysisFiles(fCProject.getProject(), exePath, monitor);
				}
				
				copyXmlFiles(fCProject.getProject(), monitor);				
				

				// Show Output View
				WorkspaceUtils.showOutputView();			
				
				ArrayList brokerCommand = new ArrayList();

				File wdBroker = BrokerUtils.getBrokerDirectory(fCProject.getProject());
				if ( wdBroker == null ) {
					abort( DebuggerMessages.getString( "TargetLaunchDelegate.20" ), null, ICDTLaunchConfigurationConstants.ERR_INTERNAL_ERROR ); //$NON-NLS-1$
				}
				
				String brokerExe = BrokerUtils.verifyBrokerFile(fCProject.getProject());
				if ( brokerExe == null || brokerExe.length() == 0 ) {
					abort( DebuggerMessages.getString( "TargetLaunchDelegate.20" ), null, ICDTLaunchConfigurationConstants.ERR_INTERNAL_ERROR ); //$NON-NLS-1$
				}
				
				String appId = PathResolver.getApplicatuonID(fCProject.getProject());
				String targetPort = config.getAttribute(IOspLaunchConfigurationConstants.ATTR_TARGET_PORT, IDebugConstants.TARGET_PORT_DEFAULT);
				String targetAppRoot = getTargetFsPath(fCProject.getProject());
				String targetExePath =  targetAppRoot + IConstants.FILE_SEP_BSLASH  + IConstants.BADA_APP_DIR_BIN + IConstants.FILE_SEP_BSLASH + exePath.lastSegment();  
				
				brokerCommand.add(brokerExe);
//				command.add("--ddm");
				brokerCommand.add("-i");
				brokerCommand.add(appId);
				
				brokerCommand.add("-l");
				if (bCopyBinary) {
					brokerCommand.add(targetExePath);
				} else {
					brokerCommand.add(exePath.toOSString());
				}
				
				brokerCommand.add("-f");
				brokerCommand.add( targetAppRoot );

				// if debug
				brokerCommand.add("-g");
				
				brokerCommand.add("-r");
				brokerCommand.add( getApplicationRoot(config));
				
				brokerCommand.add("-p");
				brokerCommand.add(targetPort);

				String[] brokerCommandArray = (String[])brokerCommand.toArray( new String[brokerCommand.size()] );
				boolean usePty = config.getAttribute( ICDTLaunchConfigurationConstants.ATTR_USE_TERMINAL, ICDTLaunchConfigurationConstants.USE_TERMINAL_DEFAULT );
				monitor.worked( 1 );
				processBroker = exec( brokerCommandArray, getEnvironment( config ), wdBroker, usePty );
				monitor.worked( 3 );
				
				DebugPlugin.newProcess( launch, processBroker, renderProcessLabel( brokerCommandArray[0] ) );
				
				NetManager.getInstance().setBroker(processBroker);
				
//				TargetLaunchListner tll = new TargetLaunchListner(launch, processBroker);
				
				IBinaryObject exeFile = null;
				if ( exePath != null ) {
					exeFile = verifyBinary( fCProject, exePath );
				}
				
				int commPort = waitAndGetComport(launch, monitor);
				if( commPort < 0 )
				{
					if( commPort == PORT_FAIL)
					{
						launch.terminate();
						
						abort("Debug port error", null, //$NON-NLS-1$
								ICDTLaunchConfigurationConstants.ERR_INTERNAL_ERROR);						
						
					}
					else if( commPort == MONITOR_ABORTED)
					{
						return;
					}
				}

				
				ICDebugConfiguration debugConfig = getDebugConfig( config );

				setDefaultSourceLocator( launch, config );


				dsession = createCDISession( config, launch, debugConfig, commPort, appId, processBroker, monitor );
				monitor.worked( 6 );

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
					if ( process != null && exePath != null) {
						iprocess = DebugPlugin.newProcess( launch, process, renderProcessLabel( exePath.toOSString() ), getDefaultProcessMap() );
					}
					CDIDebugModel.newDebugTarget( launch, fCProject.getProject(), targets[i], renderTargetLabel( debugConfig ), iprocess, exeFile, true, false, stopSymbol, true );
				}
				
				TargetDebugLaunchListner trl = new TargetDebugLaunchListner(launch);
				
			}
			catch( CoreException e ) {
				try {
					if ( dsession != null )
						dsession.terminate();
					
					if( processBroker != null ) 
						processBroker.destroy();
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
	
	protected int waitAndGetComport(ILaunch launch, IProgressMonitor monitor)
	{
		NetManager netManager = NetManager.getInstance();
		
		netManager.setComPort(-1);
		netManager.setRemoteDebuggingEnabled(false);
		
		while( netManager.isRemoteDebuggingEnabled() == false)
		{
			
			if ( monitor.isCanceled() ) {
				netManager.stopServer();
				return MONITOR_ABORTED;
			}
			
			if( launch.isTerminated() ) {
				netManager.stopServer();
				return MONITOR_ABORTED;
			}
			
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}			
		}
		
		return netManager.getComPort();
		
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
		IPath path = verifyProgramPath( config );
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
			abort( DebuggerMessages.getString( "TargetLaunchDelegate.8" ), e, ICDTLaunchConfigurationConstants.ERR_INTERNAL_ERROR ); //$NON-NLS-1$
		}
		catch( NoSuchMethodError e ) {
			// attempting launches on 1.2.* - no ability to set working
			// directory
			IStatus status = new Status( IStatus.ERROR, LaunchUIPlugin.getUniqueIdentifier(), ICDTLaunchConfigurationConstants.ERR_WORKING_DIRECTORY_NOT_SUPPORTED, DebuggerMessages.getString( "TargetLaunchDelegate.9" ), e ); //$NON-NLS-1$
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
			abort( DebuggerMessages.getString( "TargetLaunchDelegate.10" ), e, ICDTLaunchConfigurationConstants.ERR_INTERNAL_ERROR ); //$NON-NLS-1$
		}
	}

	protected ICDISession createCDISession( ILaunchConfiguration config, ILaunch launch, ICDebugConfiguration debugConfig, int comPort, String appId, Process broker, IProgressMonitor monitor ) throws CoreException {
		ICDISession session = null;
		ICDIDebugger debugger = debugConfig.createDebugger();
		
		if( debugger instanceof TargetDebugger)
			((TargetDebugger) debugger).setData(comPort, appId, broker, fCProject.getProject());
		
		if ( debugger instanceof ICDIDebugger2 )
			session = launchDebugSession( config, launch, (ICDIDebugger2)debugger, monitor );
		else
			// support old debugger types
			session = launchOldDebugSession( config, launch, debugger, monitor );
		return session;
	}
	
	protected String getTargetFsPath(IProject project)
	{
		return PathResolver.getAbsolutePath(IOspLaunchConfigurationConstants.BADA_HOST_TARGET_ROOT, project);		
	}
	
	protected String getApplicationRoot(ILaunchConfiguration config)
	{
		String root="";
		try {
			root = config.getAttribute(IOspLaunchConfigurationConstants.ATTR_CODE_BINARY_SDK_PATH, IConstants.DEFAULT_TARGET_CODE_BINARY_PATH);
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			root= IConstants.DEFAULT_TARGET_CODE_BINARY_PATH;
		}
		
		return root;
	}
	
	protected void copyExeFile(ILaunchConfiguration config, IProject project, String execPath) throws CoreException 
	{
		try {
			String dirRoot = getTargetFsPath(project);
			String fileSysPath = dirRoot + IConstants.FILE_SEP_BSLASH  + IConstants.BADA_APP_DIR_BIN;
			
			FileUtil fu = new FileUtil();
			if( fu.copyFile(execPath, fileSysPath) == false )
				terminateLaunch(fu.getErrorMsg(), fu.getException());
			
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			terminateLaunch("Error copy exe file", e);
		}
	}

	protected void copyRootDir(IProject project, IProgressMonitor monitor) throws CoreException 
	{
		String dirRoot = getTargetFsPath(project);
		String dir = dirRoot + IConstants.FILE_SEP_BSLASH  + IConstants.BADA_APP_DIR_BIN;
		
		FileUtil fu = new FileUtil();
		
		if( fu.rmDir(dirRoot) == false )
		{
			terminateLaunch(fu.getErrorMsg(), fu.getException());
		}
		
		if( fu.mkDir(dir) == false )
			terminateLaunch(fu.getErrorMsg(), fu.getException());
		
		dir = dirRoot + IConstants.FILE_SEP_BSLASH + IConstants.BADA_APP_DIR_INFO;
        if (fu.mkDir(dir) == false)
            terminateLaunch(fu.getErrorMsg(), fu.getException());		
	}
	
	protected void copyLibFiles(IProject project, IProgressMonitor monitor) throws CoreException
	{
		String dirRoot = getTargetFsPath(project);
		String dir = dirRoot + IConstants.FILE_SEP_BSLASH  + IConstants.BADA_APP_DIR_BIN;

		FileUtil fu = new FileUtil();
		
		String libDir = project.getLocation().makeAbsolute() + IConstants.FILE_SEP_BSLASH + IConstants.DIR_LIB;
		if( fu.copyFiles(libDir, dir, IConstants.EXT_SO) == false)
			terminateLaunch(fu.getErrorMsg(), fu.getException());		
		
	}
	
	protected void copyXmlFiles(IProject project, IProgressMonitor monitor) throws CoreException
	{
		String dirRoot = getTargetFsPath(project);
		String dir = dirRoot + IConstants.FILE_SEP_BSLASH  + IConstants.BADA_APP_DIR_INFO;

		FileUtil fu = new FileUtil();
		
		if( fu.copyFile(project.getLocation().makeAbsolute() + IConstants.FILE_SEP_BSLASH + IConstants.MANIFEST_FILE, dir) == false)
			terminateLaunch(fu.getErrorMsg(), fu.getException());	
		
		if( fu.copyFile(project.getLocation().makeAbsolute() + IConstants.FILE_SEP_BSLASH + IConstants.APP_XML_FILE, dir) == false )
			terminateLaunch(fu.getErrorMsg(), fu.getException());		
	}
	
	protected void copyResource(IProject project, IProgressMonitor monitor) throws CoreException 
	{
		String dirRoot = getTargetFsPath(project);
		String dir = dirRoot + IConstants.FILE_SEP_BSLASH  + IConstants.BADA_APP_DIR_BIN;
		
		FileUtil fu = new FileUtil();
		
		if( fu.rmDir(dirRoot) == false )
		{
			terminateLaunch(fu.getErrorMsg(), fu.getException());
		}
		
		if( fu.mkDir(dir) == false )
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
	
	protected void copyAnaysisFiles(IProject project, IPath exePath, IProgressMonitor monitor) throws CoreException
	{
		AnalysisUtil analUtil = new AnalysisUtil();
		
		IFile[] analFiles = analUtil.createAnalysisFile(project, exePath, monitor);
		if( analFiles == null || analUtil.isErrorOccured() )
		{
			terminateLaunch(analUtil.getErrorMsg(), analUtil.getException());
		}

		String dirRoot = getTargetFsPath(project);
		FileUtil fu = new FileUtil();

		String dir = dirRoot + IConstants.FILE_SEP_BSLASH  + IConstants.BADA_APP_DIR_INFO;
		if( fu.copyFile(analFiles[AnalysisUtil.INX_EXT_HTB].getLocation().toOSString(), dir) == false)
			terminateLaunch(fu.getErrorMsg(), fu.getException());
	
		if( fu.copyFile(analFiles[AnalysisUtil.INX_SIGNATURE_XML].getLocation().toOSString(), dirRoot) == false)
			terminateLaunch(fu.getErrorMsg(), fu.getException());
		
		//analFiles[AnalysisUtil.INX_SIGNATURE_XML].delete(true, monitor);
	}
	
	protected void terminateLaunch(String msg, Throwable exception) throws CoreException 
	{
		abort(msg, exception, //$NON-NLS-1$
				ICDTLaunchConfigurationConstants.ERR_INTERNAL_ERROR);
	}
/*	
	protected File getBrokerDirectory()
	{
		String path = IdePlugin.getDefault().getSDKPath(fCProject.getProject()) + IConstants.PATH_BROKER; 
		
		File f = new File(path);
		
		if( f.exists() ) return f;
		
		return null;
	}
	
	protected String verifyBrokerFile()
	{
		String path = IdePlugin.getDefault().getSDKPath(fCProject.getProject()) 
						+ IConstants.PATH_BROKER
						+ IConstants.FILE_SEP_BSLASH
						+ IConstants.BROKER_FILENAME;
		File f = new File(path);
		
		if( f.exists() ) return path;
		
		return null;
	}
*/

	
}
