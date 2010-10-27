package com.osp.debugger.mi;

import org.eclipse.cdt.debug.mi.core.command.CLICommand;

public class CLISet extends CLICommand {

	public CLISet(String param) {
		super("set " + param); //$NON-NLS-1$
	}	
}
