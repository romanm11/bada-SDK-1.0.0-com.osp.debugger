package com.osp.debugger.launch.target;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchesListener2;
import org.eclipse.debug.internal.core.LaunchManager;

import com.osp.debugger.launch.IOspLaunchConfigurationConstants;
import com.osp.ide.message.socket.NetManager;



public class TargetLaunchListner implements ILaunchesListener2 {
	
	private ILaunch currLaunch = null;
	
	public TargetLaunchListner(ILaunch launch)
	{
		currLaunch = launch;
		
		DebugPlugin.getDefault().getLaunchManager().addLaunchListener(this);
	}
	

	@Override
	public void launchesTerminated(ILaunch[] launches) {
		// TODO Auto-generated method stub
		if( currLaunch == null ) return;
		
		for( int i = 0; i < launches.length; i++ )
		{
			if( launches[i].equals(currLaunch))
			{
				currLaunch = null;
				
				NetManager.getInstance().stopServer();
				
				DebugPlugin.getDefault().getLaunchManager().removeLaunchListener(this);
//				launches[i].setAttribute(IOspLaunchConfigurationConstants.ATTR_BADA_LAUNCH_TYPE, "");
			}
		}
		
	}

	@Override
	public void launchesAdded(ILaunch[] launches) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void launchesChanged(ILaunch[] launches) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void launchesRemoved(ILaunch[] launches) {
		// TODO Auto-generated method stub
		
	}
	
}

