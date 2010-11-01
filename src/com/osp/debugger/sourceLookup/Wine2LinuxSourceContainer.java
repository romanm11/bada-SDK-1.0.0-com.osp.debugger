package com.osp.debugger.sourceLookup;

import java.io.File;

import org.eclipse.cdt.debug.core.CDebugCorePlugin;
import org.eclipse.cdt.debug.core.sourcelookup.SourceLookupMessages;
import org.eclipse.cdt.internal.core.resources.ResourceLookup;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.sourcelookup.ISourceContainerType;
import org.eclipse.debug.core.sourcelookup.containers.AbstractSourceContainer;

public class Wine2LinuxSourceContainer extends AbstractSourceContainer
{
	/**
	 * Unique identifier for the absolute source container type
	 * (value <code>com.osp.debugger.sourceLookup.Wine2LinuxSourceContainerType</code>).
	 */
	public static final String TYPE_ID = "com.osp.debugger.sourceLookup.Wine2LinuxSourceContainerType";	 //$NON-NLS-1$

	@Override
	public Object[] findSourceElements(String name) throws CoreException {
		if (!name.startsWith( "z:\\" ) && !name.startsWith( "Z:\\" )) return EMPTY;
		String linuxPath = name.substring( 2 ).replace( '\\', '/' );
		
		try
		{
			File file = new File( linuxPath );
			IFile[] wfiles = ResourceLookup.findFilesForLocation( new Path( file.getAbsolutePath() ) );
			if (wfiles.length > 0) {
	//			ResourceLookup.sortFilesByRelevance(wfiles, getProject());
				return wfiles;
			}
			
			wfiles = ResourceLookup.findFilesForLocation(new Path( file.getCanonicalPath() ));
			if (wfiles.length > 0) {
	//			ResourceLookup.sortFilesByRelevance(wfiles, getProject());
				return wfiles;
			}
			
		// TODO make recursive, checking for links, ...
		// Try to look ignoring case
			IPath path = new Path( linuxPath ).makeAbsolute();
			int segmentCount = path.segmentCount();
			String absolutePath = java.io.File.separator;
			int iSegment;
			for (iSegment = 0; iSegment < segmentCount; iSegment++)
			{
				String pathSegment = path.segment( iSegment );
				String testPath = absolutePath + pathSegment;
				File testFile = new File( testPath );
				if (testFile.exists())
				{
					if (iSegment + 1 < segmentCount)
					{
					// should be directory
						if (testFile.isDirectory())
						{
							absolutePath += pathSegment + java.io.File.separatorChar;
							continue;
						}
					}
					else
					{
					// should be file
						if (testFile.isFile())
						{
							absolutePath += pathSegment;
							continue;
						}
					}
				}
				
				File list[] = new File( absolutePath ).listFiles();
				int filesCount = list.length;
				int iFile;
				for (iFile = 0; iFile < filesCount; iFile++)
				{
					String testName = list[iFile].getName();
					if (testName.compareToIgnoreCase( pathSegment ) == 0)
					{
						if (iSegment + 1 < segmentCount)
						{
						// should be directory
							if (list[iFile].isDirectory()) {
								absolutePath += testName + java.io.File.separatorChar;
								break;
							}
						}
						else
						{
						// should be file
							if (list[iFile].isFile())
							{
								absolutePath += testName;
								break;
							}
						}
					}
				}
				if (iFile == filesCount) break;
			}
			if (iSegment < segmentCount) return EMPTY;

		// look for resources
			wfiles = ResourceLookup.findFilesForLocation(new Path( absolutePath ));
			if (wfiles.length > 0) {
	//			ResourceLookup.sortFilesByRelevance(wfiles, getProject());
				return wfiles;
			}
		}
		catch (Exception e)
		{
			return EMPTY;
		}
		
		return EMPTY;
	}

	@Override
	public String getName() {
		return SourceLookupMessages.getString( "Wine2LinuxSourceContainer.name" ); //$NON-NLS-1$
	}

	@Override
	public ISourceContainerType getType() {
		return getSourceContainerType( TYPE_ID );
	}

}
