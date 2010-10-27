package com.osp.debugger.mi;

import org.eclipse.cdt.debug.mi.core.command.CommandFactory;
import org.eclipse.cdt.debug.mi.core.command.MIEnvironmentCD;
import org.eclipse.cdt.debug.mi.core.command.MIEnvironmentDirectory;
import org.eclipse.cdt.debug.mi.core.command.factories.win32.WinMIEnvironmentCD;

public class BadaCommandFactory extends CommandFactory{

	protected BadaCommandFactory() {
	}

	public BadaCommandFactory(String miVersion) {
		super(miVersion);
		
	}
	
	static public CLID createCLID() {
		return new CLID();
	}	

	static public CLISet createCLISet(String param) {
		return new CLISet(param);
	}
	
	public MIEnvironmentDirectory createMIEnvironmentDirectory(boolean reset, String[] pathdirs) {
		return new WinMIEnvironmentDirectory( getMIVersion(), reset, pathdirs );
	}
	
	public MIEnvironmentCD createMIEnvironmentCD( String pathdir ) {
		return new WinMIEnvironmentCD( getMIVersion(), pathdir );
	}	
}
