package com.osp.debugger.mi;

import org.eclipse.cdt.debug.mi.core.IMITTY;
import org.eclipse.cdt.debug.mi.core.MIException;
import org.eclipse.cdt.debug.mi.core.MIInferior;
import org.eclipse.cdt.debug.mi.core.MIProcess;
import org.eclipse.cdt.debug.mi.core.MISession;
import org.eclipse.cdt.debug.mi.core.command.CommandFactory;
import org.eclipse.core.runtime.IProgressMonitor;

public class BadaMISession extends MISession {

	public BadaMISession(MIProcess process, IMITTY tty, int type, int commandTimeout, int launchTimeout, String miVersion, IProgressMonitor monitor) throws MIException {
		//super(process, null, type, new CommandFactory(miVersion), commandTimeout, launchTimeout, monitor);
		super(process, null, type, new BadaCommandFactory(miVersion), commandTimeout, launchTimeout, monitor);
		
		MIInferior miInferior = new BadaMIInferior(this, tty);
		setMIInferior(miInferior);
	}

}
