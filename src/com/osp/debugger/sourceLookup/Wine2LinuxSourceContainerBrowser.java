package com.osp.debugger.sourceLookup;

import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.ISourceLookupDirector;
import org.eclipse.debug.ui.sourcelookup.AbstractSourceContainerBrowser;
import org.eclipse.swt.widgets.Shell;

public class Wine2LinuxSourceContainerBrowser extends AbstractSourceContainerBrowser
{

	public ISourceContainer[] addSourceContainers(Shell shell, ISourceLookupDirector director) {
		Wine2LinuxSourceContainer wine2LinuxSourceContainer = new Wine2LinuxSourceContainer();
		return new ISourceContainer[] { wine2LinuxSourceContainer };
	}
}
