package com.osp.debugger.launch.simul;

import org.eclipse.cdt.core.IAddress;
import org.eclipse.cdt.core.IBinaryParser.IBinaryObject;
import org.eclipse.cdt.debug.core.ICDTLaunchConfigurationConstants;
import org.eclipse.cdt.debug.core.cdi.CDIException;
import org.eclipse.cdt.debug.core.cdi.ICDILocation;
import org.eclipse.cdt.debug.core.cdi.model.ICDITarget;
import org.eclipse.cdt.debug.core.model.CDebugElementState;
import org.eclipse.cdt.debug.internal.core.model.CDebugTarget;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

public class SumulatorDebugTarget extends CDebugTarget {

	public SumulatorDebugTarget(ILaunch launch, IProject project,
			ICDITarget cdiTarget, String name, IProcess debuggeeProcess,
			IBinaryObject file, boolean allowsTerminate,
			boolean allowsDisconnect) {
		super(launch, project, cdiTarget, name, debuggeeProcess, file, allowsTerminate,
				allowsDisconnect);
		// TODO Auto-generated constructor stub
	}

//	public boolean canRestart() {
//		return false;
//	}
	
	
	public void badaRestart(final ILaunch launch) {
		final IWorkbench workbench = PlatformUI.getWorkbench();
		if (workbench == null)
			return;
		final Display display = workbench.getDisplay();
		display.asyncExec(new Runnable() {
			public void run() {
				try {
					ILaunchConfiguration launchConfig = launch.getLaunchConfiguration();
					
					launch.terminate();
					
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}					
					
					int timeout = 20000; // 20sec
					int timeVal = 0;
					boolean bTimeout = false;
					while( launch.isTerminated() == false )
					{
						try {
							Thread.sleep(500);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						
						if( timeVal > timeout )
						{
							bTimeout = true;
							break;
						}
						
						timeVal += 500;
						
					}
					
					if( bTimeout )
					{
						MessageDialog.openError(display.getActiveShell(), "Error", "Can't not terminate Simulator.");						
					}
					else
					{
						DebugUITools.launch(launchConfig, ILaunchManager.DEBUG_MODE);
					}
					
				} catch (DebugException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});

	}
	
	
	public void restart() throws DebugException {
		if ( !canRestart() ) {
			return;
		}
		
		badaRestart(getLaunch());
	}
		
}
