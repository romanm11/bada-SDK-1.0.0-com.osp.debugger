package com.osp.debugger.sourceLookup;

//import java.util.ArrayList;
//import java.util.Iterator;
//import java.util.List;

//import org.eclipse.cdt.debug.core.sourcelookup.MappingSourceContainer;
//import org.eclipse.cdt.debug.internal.core.sourcelookup.InternalSourceLookupMessages;
//import org.eclipse.cdt.debug.internal.core.sourcelookup.MapEntrySourceContainer;
import org.eclipse.core.runtime.CoreException;
//import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
//import org.eclipse.debug.core.sourcelookup.ISourceContainerType;
import org.eclipse.debug.core.sourcelookup.containers.AbstractSourceContainerTypeDelegate;
//import org.w3c.dom.Document;
//import org.w3c.dom.Element;
//import org.w3c.dom.Node;



public class Wine2LinuxSourceContainerType
	extends AbstractSourceContainerTypeDelegate
{
//	org.eclipse.cdt.debug.internal.ui.sourcelookup.NewMappingSourceContainerBrowser
	//org.eclipse.cdt.debug.internal.core.sourcelookup.MapEntrySourceContainer
//	org.eclipse.cdt.debug.internal.core.sourcelookup.CSourcePathComputerDelegate
//	org.eclipse.cdt.debug.internal.core.sourcelookup.CSourceLookupDirector
//	org.eclipse.debug.internal.core.sourcelookup.containers.ProjectSourceContainerType
//	org.eclipse.debug.internal.ui.sourcelookup.AddSourceContainerDialog
//	org.eclipse.cdt.debug.internal.ui.sourcelookup.AbsolutePathSourceContainerBrowser
//	org.eclipse.cdt.debug.internal.core.sourcelookup.AbsolutePathSourceContainerType
	


	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.sourcelookup.ISourceContainerTypeDelegate#createSourceContainer(java.lang.String)
	 */
	public ISourceContainer createSourceContainer( String memento ) throws CoreException {
		return new Wine2LinuxSourceContainer();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.sourcelookup.ISourceContainerTypeDelegate#getMemento(org.eclipse.debug.core.sourcelookup.ISourceContainer)
	 */
	public String getMemento( ISourceContainer container ) throws CoreException {
		return "Wine2Linux";
	}
}
