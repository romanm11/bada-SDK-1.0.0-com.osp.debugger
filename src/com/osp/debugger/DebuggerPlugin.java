package com.osp.debugger;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class DebuggerPlugin extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "com.osp.debugger";

	// The shared instance
	private static DebuggerPlugin plugin;
	
	/**
	 * The constructor
	 */
	public DebuggerPlugin() {
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
//		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
//		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static DebuggerPlugin getDefault() {
		if( plugin == null ) plugin = new DebuggerPlugin();
		return plugin;
	}

    public static ImageDescriptor createImageDescriptor(String path) {
	    return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}		
}
