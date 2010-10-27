package com.osp.debugger.mi;

import org.eclipse.cdt.debug.mi.core.IMITTY;
import org.eclipse.cdt.debug.mi.core.MIException;
import org.eclipse.cdt.debug.mi.core.MIInferior;
import org.eclipse.cdt.debug.mi.core.MISession;
import org.eclipse.cdt.debug.mi.core.command.CLIExecAbort;
import org.eclipse.cdt.debug.mi.core.command.CommandFactory;

public class BadaMIInferior extends MIInferior {

	MISession session;
	
	public BadaMIInferior(MISession mi, IMITTY p) {
		super(mi, p);
		
		session = mi;
	}
	
	public void terminate() throws MIException {
		if ((session.isAttachSession() && isConnected()) || (session.isProgramSession() && !isTerminated())) {
			// Try to interrupt the inferior, first.
			if (isRunning()) {
				interrupt();
			}

			if (isSuspended()) {
				try {
					CLID cmdD = BadaCommandFactory.createCLID();
					session.postCommand0(cmdD, 1000);
				} catch (MIException e) {
					// ignore the error
				}
			}
		}
		super.terminate();
	}
}
