/*******************************************************************************
 * Copyright (c) 2005, 2007 QNX Software Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * QNX Software Systems - Initial API and implementation
 *******************************************************************************/
package com.osp.debugger.launch.target;

import org.eclipse.cdt.launch.ui.CArgumentsTab;
//import org.eclipse.cdt.launch.ui.CDebuggerTab;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.EnvironmentTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.debug.ui.sourcelookup.SourceLookupTab;

public class TargetLaunchConfigurationTabGroup extends AbstractLaunchConfigurationTabGroup {
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTabGroup#createTabs(org.eclipse.debug.ui.ILaunchConfigurationDialog, java.lang.String)
	 */
	public void createTabs(ILaunchConfigurationDialog dialog, String mode)  {
		
		ILaunchConfigurationTab[] tabs =null;
		if( mode.equals(ILaunchManager.DEBUG_MODE))
		{
			 tabs = new ILaunchConfigurationTab[] {
				new TargetMainTab(true),
				new TargetConnectionTab(),
				new CArgumentsTab(),
				new EnvironmentTab(),
				new TargetDebuggerTab(false),
				new SourceLookupTab(),
//				new TargetDeviceTab(),
				new CommonTab() 
			};
		}
		else  // run
		{
			 tabs = new ILaunchConfigurationTab[] {
				new TargetMainTab(true),
				new TargetConnectionTab(),
				new CArgumentsTab(),
				new EnvironmentTab(),
				new TargetDebuggerTab(false), // 20100625 added by bson: to get rid of "No such debugger" error
//				new TargetDeviceTab(),
				new CommonTab() 
				};			
		}
		setTabs(tabs);
	}
	
}
