package com.osp.debugger.launch.simul;

import org.eclipse.cdt.core.IBinaryParser.IBinaryObject;
import org.eclipse.cdt.debug.core.CDebugCorePlugin;
import org.eclipse.cdt.debug.core.cdi.model.ICDITarget;
import org.eclipse.cdt.debug.internal.core.model.CDebugTarget;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IProcess;

public class SumulatorDebugModel {

	public static IDebugTarget newDebugTarget( ILaunch launch, IProject project, ICDITarget cdiTarget, final String name, IProcess debuggeeProcess, IBinaryObject file, boolean allowTerminate, boolean allowDisconnect, boolean resumeTarget ) throws DebugException {
		return newDebugTarget( launch, project, cdiTarget, name, debuggeeProcess, file, allowTerminate, allowDisconnect, null, resumeTarget );
	}
	
	public static IDebugTarget newDebugTarget( final ILaunch launch, final IProject project, final ICDITarget cdiTarget, final String name, final IProcess debuggeeProcess, final IBinaryObject file, final boolean allowTerminate, final boolean allowDisconnect, final String stopSymbol, final boolean resumeTarget ) throws DebugException {
		final IDebugTarget[] target = new IDebugTarget[1];
		IWorkspaceRunnable r = new IWorkspaceRunnable() {

			public void run( IProgressMonitor m ) throws CoreException {
				target[0] = new SumulatorDebugTarget( launch, project, cdiTarget, name, debuggeeProcess, file, allowTerminate, allowDisconnect );
				((CDebugTarget)target[0]).start( stopSymbol, resumeTarget );
			}
		};
		try {
			ResourcesPlugin.getWorkspace().run( r, null );
		}
		catch( CoreException e ) {
			CDebugCorePlugin.log( e );
			throw new DebugException( e.getStatus() );
		}
		return target[0];
	}
	
	
}
