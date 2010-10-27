package com.osp.debugger.launch.target;

import org.eclipse.cdt.debug.core.cdi.CDIException;
import org.eclipse.cdt.debug.core.cdi.ICDISessionConfiguration;
import org.eclipse.cdt.debug.mi.core.MISession;
import org.eclipse.cdt.debug.mi.core.cdi.Session;


public class badaSession extends Session {
	
	Process pBroker = null;

	public badaSession(MISession miSession, ICDISessionConfiguration configuration) {
		super(miSession,configuration);
	}

	// Why do we need this?
	public badaSession(MISession miSession, boolean attach, Process broker) {
		super(miSession,attach);
		
		this.pBroker = broker;
	}

	public badaSession(MISession miSession) {
		super(miSession);
	}
	
	public void terminate() throws CDIException {
		super.terminate();
		
		if( pBroker != null ) pBroker.destroy();
	}

}
