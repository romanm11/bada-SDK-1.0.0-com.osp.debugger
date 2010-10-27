/*
 * Created on 2004. 11. 23.
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.osp.debugger.launch.target;

import java.io.File;
import java.io.IOException;

import org.eclipse.cdt.debug.mi.core.IMIConstants;
import org.eclipse.cdt.debug.mi.core.IMITTY;
import org.eclipse.cdt.debug.mi.core.MIException;
import org.eclipse.cdt.debug.mi.core.MIPlugin;
import org.eclipse.cdt.debug.mi.core.MIProcess;
import org.eclipse.cdt.debug.mi.core.MIProcessAdapter;
import org.eclipse.cdt.debug.mi.core.MISession;
import org.eclipse.cdt.debug.mi.core.cdi.Session;
import org.eclipse.cdt.debug.mi.core.command.CLITargetAttach;
import org.eclipse.cdt.debug.mi.core.command.CommandFactory;
import org.eclipse.cdt.debug.mi.core.command.MITargetSelect;
import org.eclipse.cdt.debug.mi.core.output.MIInfo;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Preferences;

import com.osp.debugger.IDebugConstants;
import com.osp.debugger.mi.BadaMISession;

/**
 * @author COSMO
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */

public class CreateCSession {

	private static CreateCSession fCreateCSession = null;
	
	
	public static CreateCSession getDefault()
	{
		if(fCreateCSession == null ) fCreateCSession = new CreateCSession();
		
		return fCreateCSession;
	}
	
	public MISession createMISession(MIProcess process, IMITTY pty, int timeout, int type, int launchTimeout, String miVersion, IProgressMonitor monitor) throws MIException {
		//return new MISession(process, pty, type, timeout, launchTimeout, miVersion, monitor);
		return new BadaMISession(process, pty, type, timeout, launchTimeout, miVersion, monitor);
	}
	
	public MISession createMISession(MIProcess process, IMITTY pty, int type, String miVersion, IProgressMonitor monitor) throws MIException {
		MIPlugin miPlugin = MIPlugin.getDefault();
		Preferences prefs = miPlugin.getPluginPreferences();
		int timeout = prefs.getInt(IMIConstants.PREF_REQUEST_TIMEOUT);
		int launchTimeout = prefs.getInt(IMIConstants.PREF_REQUEST_LAUNCH_TIMEOUT);
		return createMISession(process, pty, timeout, type, launchTimeout, miVersion, monitor);
	}	

	public Session createCSession(String gdb, String miVersion, File program, int pid, String[] targetParams, File cwd, String gdbinit, Process broker, IProgressMonitor monitor) throws IOException, MIException {
		if (gdb == null || gdb.length() == 0) {
			gdb =  IDebugConstants.DEBUGGER_TARGET_DEBUG_NAME_DEFAULT;
		}

		String commandFile = (gdbinit != null && gdbinit.length() > 0) ? "--command="+gdbinit : "--nx"; //$NON-NLS-1$ //$NON-NLS-2$

		if (monitor == null) {
			monitor = new NullProgressMonitor();
		}

		String[] args;
		if (program == null) {
			args = new String[] {gdb, "--cd="+cwd.getAbsolutePath(), commandFile, "--quiet", "-nw", "-i", miVersion}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		} else {
			args = new String[] {gdb, "--cd="+cwd.getAbsolutePath(), commandFile, "--quiet", "-nw", "-i", miVersion, program.getAbsolutePath()}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		}

		int launchTimeout = MIPlugin.getDefault().getPluginPreferences().getInt(IMIConstants.PREF_REQUEST_LAUNCH_TIMEOUT);		
		MIProcess pgdb = new MIProcessAdapter(args, launchTimeout, monitor);
		
		if (MIPlugin.getDefault().isDebugging()) {
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < args.length; ++i) {
				sb.append(args[i]);
				sb.append(' ');
			}
			MIPlugin.getDefault().debugLog(sb.toString());
		}
		
		MISession session;
		try {
			session = createMISession(pgdb, null, MISession.ATTACH, miVersion, monitor);
		} catch (MIException e) {
			pgdb.destroy();
			throw e;
		}
		CommandFactory factory = session.getCommandFactory();
		try {
			if (targetParams != null && targetParams.length > 0) {
				MITargetSelect target = factory.createMITargetSelect(targetParams);
				session.postCommand(target);
				MIInfo info = target.getMIInfo();
				if (info == null) {
					throw new MIException(MIPlugin.getDefault().getResourceString("src.common.No_answer")); //$NON-NLS-1$
				}
			}
			if (pid > 0) {
				CLITargetAttach attach = factory.createCLITargetAttach(pid);
				session.postCommand(attach);
				MIInfo info = attach.getMIInfo();
				if (info == null) {
					throw new MIException(MIPlugin.getDefault().getResourceString("src.common.No_answer")); //$NON-NLS-1$
				}
				session.getMIInferior().setInferiorPID(pid);
				// @@@ for attach we nee to manually set the connected state
				// attach does not send the ^connected ack
				session.getMIInferior().setConnected();
			}
		} catch (MIException e) {
			if(session != null)
				session.terminate();
			
			pgdb.destroy();
			throw e;
		}
		//@@@ We have to manually set the suspended state when we attach
		session.getMIInferior().setSuspended();
		//session.getMIInferior().update();
		session.getMIInferior().setInferiorPID(-1);
		return new badaSession(session, true, broker);
	}
	
	
	
}
