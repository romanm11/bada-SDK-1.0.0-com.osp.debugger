package com.osp.debugger.launch.simul;

import org.eclipse.core.resources.IProject;

import com.osp.debugger.IDebugConstants;
import com.osp.ide.IConstants;
import com.osp.ide.IdePlugin;
import com.osp.ide.core.PathResolver;

public class SimulatorPathHelper {

	public static String getSimulatorPath(String sdkName, IProject proj)
	{
		return PathResolver.getAbsolutePath(IConstants.ENV_SIMULATOR_LIB_PATH_VAR, proj) + "\\" + IDebugConstants.SIMULATOR_NAME;
	}
	
	public static String getSimulatorDirectory(String sdkName, IProject proj)
	{
		return PathResolver.getAbsolutePath(IConstants.ENV_SIMULATOR_LIB_PATH_VAR, proj);
	}
	
}
