/*******************************************************************************
 * Copyright (c) 2005, 2008 QNX Software Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     QNX Software Systems - initial API and implementation
 *     Ken Ryall (Nokia) - bug 178731
 *	   IBM Corporation
 *******************************************************************************/
package com.osp.debugger.launch.simul;

import java.io.IOException;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.IBinaryParser;
import org.eclipse.cdt.core.ICDescriptor;
import org.eclipse.cdt.core.ICExtensionReference;
import org.eclipse.cdt.core.IBinaryParser.IBinaryObject;
import org.eclipse.cdt.core.model.CModelException;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.IBinary;
import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.debug.core.ICDTLaunchConfigurationConstants;
import org.eclipse.cdt.launch.internal.ui.LaunchImages;
import org.eclipse.cdt.launch.internal.ui.LaunchUIPlugin;
import org.eclipse.cdt.launch.ui.CLaunchConfigurationTab;
import org.eclipse.cdt.launch.ui.ICDTLaunchHelpContextIds;
import org.eclipse.cdt.ui.CElementLabelProvider;
//import org.eclipse.cdt.utils.pty.PTY;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.dialogs.TwoPaneElementSelector;

import com.osp.debugger.launch.IOspLaunchConfigurationConstants;
import com.osp.debugger.launch.OspLaunchMessages;
import com.osp.ide.IConstants;
import com.osp.ide.core.PathResolver;

/**
 * A launch configuration tab that displays and edits project and main type name launch
 * configuration attributes.
 * <p>
 * This class may be instantiated. This class is not intended to be subclassed.
 * </p>
 * 
 * @since 2.0
 */

public class SimulatorMainTab extends CLaunchConfigurationTab {

	
	// Project UI widgets
	protected Label fProjLabel;
	protected Text fProjText;
	protected Button fProjButton;

	// Main class UI widgets
	protected Label fProgLabel;
	protected Text fProgText;
	protected Button fSearchButton;
	
	// Code Binary Path
	protected Label fCodeBinPathSdkLabel;
	protected Text fCodeBinPathSdkText;
	protected Button fCodeBinPathSdkButton;
/*	
	// Resource Binary Path
	protected Label fResBinPathLabel;
	protected Text fResBinPathText;
	protected Button fResBinPathButton;	
*/
	private final boolean fWantsTerminalOption;

	private final boolean dontCheckProgram;
	
	protected static final String EMPTY_STRING = ""; //$NON-NLS-1$

	private String filterPlatform = EMPTY_STRING;

	public static final int WANTS_TERMINAL = 1;
	public static final int DONT_CHECK_PROGRAM = 2;
	
	public SimulatorMainTab() {
		this(0);
	}

	public SimulatorMainTab(boolean terminalOption) {
		this(terminalOption ? WANTS_TERMINAL : 0);
	}

	public SimulatorMainTab(int flags) {
		fWantsTerminalOption = (flags & WANTS_TERMINAL) != 0;
		dontCheckProgram = (flags & DONT_CHECK_PROGRAM) != 0;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		Composite comp = new Composite(parent, SWT.NONE);
		setControl(comp);

		LaunchUIPlugin.getDefault().getWorkbench().getHelpSystem().setHelp(getControl(), ICDTLaunchHelpContextIds.LAUNCH_CONFIGURATION_DIALOG_MAIN_TAB);

		GridLayout topLayout = new GridLayout();
		comp.setLayout(topLayout);

		createVerticalSpacer(comp, 1);
		createProjectGroup(comp, 1);
		createExeFileGroup(comp, 1);
		createVerticalSpacer(comp, 1);
		
		createCodeBinPathSdkGroup(comp, 1);
//		createResourceBinPathGroup(comp, 1);

		
		LaunchUIPlugin.setDialogShell(parent.getShell());
	}

	protected void createProjectGroup(Composite parent, int colSpan) {
		Composite projComp = new Composite(parent, SWT.NONE);
		GridLayout projLayout = new GridLayout();
		projLayout.numColumns = 2;
		projLayout.marginHeight = 0;
		projLayout.marginWidth = 0;
		projComp.setLayout(projLayout);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = colSpan;
		projComp.setLayoutData(gd);

		fProjLabel = new Label(projComp, SWT.NONE);
		fProjLabel.setText(OspLaunchMessages.getString("CMainTab.&ProjectColon")); //$NON-NLS-1$
		gd = new GridData();
		gd.horizontalSpan = 2;
		fProjLabel.setLayoutData(gd);

		fProjText = new Text(projComp, SWT.SINGLE | SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		fProjText.setLayoutData(gd);
		fProjText.addModifyListener(new ModifyListener() {

			public void modifyText(ModifyEvent evt) {
				updateLaunchConfigurationDialog();
			}
		});

		fProjButton = createPushButton(projComp, OspLaunchMessages.getString("Launch.common.Browse_1"), null); //$NON-NLS-1$
		fProjButton.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent evt) {
				handleProjectButtonSelected();
				updateLaunchConfigurationDialog();
			}
		});
	}

	protected void createExeFileGroup(Composite parent, int colSpan) {
		Composite mainComp = new Composite(parent, SWT.NONE);
		GridLayout mainLayout = new GridLayout();
		mainLayout.numColumns = 3;
		mainLayout.marginHeight = 0;
		mainLayout.marginWidth = 0;
		mainComp.setLayout(mainLayout);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = colSpan;
		mainComp.setLayoutData(gd);
		fProgLabel = new Label(mainComp, SWT.NONE);
		fProgLabel.setText("&Code binary path:"); //$NON-NLS-1$
		gd = new GridData();
		gd.horizontalSpan = 3;
		fProgLabel.setLayoutData(gd);
		fProgText = new Text(mainComp, SWT.SINGLE | SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		fProgText.setLayoutData(gd);
		fProgText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent evt) {
				updateLaunchConfigurationDialog();
			}
		});

		fSearchButton = createPushButton(mainComp, OspLaunchMessages.getString("CMainTab.Search..."), null); //$NON-NLS-1$
		fSearchButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent evt) {
				handleSearchButtonSelected();
				updateLaunchConfigurationDialog();
			}
		});

		Button fBrowseForBinaryButton;
		fBrowseForBinaryButton = createPushButton(mainComp, OspLaunchMessages.getString("Launch.common.Browse_2"), null); //$NON-NLS-1$
		fBrowseForBinaryButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent evt) {
				handleBinaryBrowseButtonSelected();
				updateLaunchConfigurationDialog();
			}
		});
	}

	protected boolean wantsTerminalOption() {
		return fWantsTerminalOption;
	}

	protected void createCodeBinPathSdkGroup(Composite parent, int colSpan) {
		
		Composite mainComp = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		mainComp.setLayout(layout);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = colSpan;
		mainComp.setLayoutData(gd);

		fCodeBinPathSdkLabel = new Label(mainComp, SWT.NONE);
		fCodeBinPathSdkLabel.setText("C&ode binary path in simulator file system:"); //$NON-NLS-1$
		gd = new GridData();
		gd.horizontalSpan = 2;
		fCodeBinPathSdkLabel.setLayoutData(gd);

		fCodeBinPathSdkText = new Text(mainComp, SWT.SINGLE | SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		fCodeBinPathSdkText.setLayoutData(gd);
		fCodeBinPathSdkText.addModifyListener(new ModifyListener() {

			public void modifyText(ModifyEvent evt) {
				updateLaunchConfigurationDialog();
			}
		});
		

		fCodeBinPathSdkButton = createPushButton(mainComp, OspLaunchMessages.getString("Launch.common.Browse_2"), null); //$NON-NLS-1$
		fCodeBinPathSdkButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent evt) {
				handleCodeBinPathSdkButtonSelected();
				updateLaunchConfigurationDialog();
			}
		});
	}
/*	
	protected void createResourceBinPathGroup(Composite parent, int colSpan) {
		
		Composite mainComp = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		mainComp.setLayout(layout);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = colSpan;
		mainComp.setLayoutData(gd);

		fResBinPathLabel = new Label(mainComp, SWT.NONE);
		fResBinPathLabel.setText("&Resource Binary Path in Simulator File System"); //$NON-NLS-1$
		gd = new GridData();
		gd.horizontalSpan = 2;
		fResBinPathLabel.setLayoutData(gd);

		fResBinPathText = new Text(mainComp, SWT.SINGLE | SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		fResBinPathText.setLayoutData(gd);
		fResBinPathText.addModifyListener(new ModifyListener() {

			public void modifyText(ModifyEvent evt) {
				updateLaunchConfigurationDialog();
			}
		});
		

		fResBinPathButton = createPushButton(mainComp, OspLaunchMessages.getString("Launch.common.Browse_2"), null); //$NON-NLS-1$
		fResBinPathButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent evt) {
				handleResourceBinPathButtonSelected();
				updateLaunchConfigurationDialog();
			}
		});
	}	
*/
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#initializeFrom(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public void initializeFrom(ILaunchConfiguration config) {
		filterPlatform = getPlatform(config);
		updateProjectFromConfig(config);
		updateProgramFromConfig(config);
		updateBinPathSdkFromConfig(config);
//		updateBinPathResourceFromConfig(config);
	}

	protected void updateBinPathSdkFromConfig(ILaunchConfiguration config) {
		String binPathName = IOspLaunchConfigurationConstants.DEFAULT_SUMUAL_CODE_BINARY_PATH;
		
		try {
			binPathName = config.getAttribute(IOspLaunchConfigurationConstants.ATTR_CODE_BINARY_SDK_PATH, IOspLaunchConfigurationConstants.DEFAULT_SUMUAL_CODE_BINARY_PATH);
		} catch (CoreException ce) {
			LaunchUIPlugin.log(ce);
		}
		fCodeBinPathSdkText.setText(binPathName);
	}
	
/*	
	protected void updateBinPathResourceFromConfig(ILaunchConfiguration config) {
		String binPathName = EMPTY_STRING;
		
		try {
			binPathName = config.getAttribute(IOspLaunchConfigurationConstants.ATTR_RESOURCE_BINARY_PATH, EMPTY_STRING);
		} catch (CoreException ce) {
			LaunchUIPlugin.log(ce);
		}
		fResBinPathText.setText(binPathName);
	}	
*/

	protected void updateProjectFromConfig(ILaunchConfiguration config) {
		String projectName = EMPTY_STRING;
		try {
			projectName = config.getAttribute(ICDTLaunchConfigurationConstants.ATTR_PROJECT_NAME, EMPTY_STRING);
		} catch (CoreException ce) {
			LaunchUIPlugin.log(ce);
		}
		fProjText.setText(projectName);
	}

	protected void updateProgramFromConfig(ILaunchConfiguration config) {
		String programName = EMPTY_STRING;
		try {
			programName = config.getAttribute(ICDTLaunchConfigurationConstants.ATTR_PROGRAM_NAME, EMPTY_STRING);
		} catch (CoreException ce) {
			LaunchUIPlugin.log(ce);
		}
		fProgText.setText(programName);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#performApply(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	public void performApply(ILaunchConfigurationWorkingCopy config) {
		ICProject cProject = this.getCProject();
		if (cProject != null)
		{
			config.setMappedResources(new IResource[] { cProject.getProject() });
			try { // Only initialize the build config ID once.
				if (config.getAttribute(ICDTLaunchConfigurationConstants.ATTR_PROJECT_BUILD_CONFIG_ID, "").length() == 0)//$NON-NLS-1$
				{
					ICProjectDescription projDes = CCorePlugin.getDefault().getProjectDescription(cProject.getProject());
					if (projDes != null)
					{
						//String buildConfigID = projDes.getActiveConfiguration().getId();
						String buildConfigID = getSimulatorConfiguration(projDes).getId();
						config.setAttribute(ICDTLaunchConfigurationConstants.ATTR_PROJECT_BUILD_CONFIG_ID, buildConfigID);			
					}				
				}
			} catch (CoreException e) { e.printStackTrace(); }
		}
		config.setAttribute(ICDTLaunchConfigurationConstants.ATTR_PROJECT_NAME, fProjText.getText());
		config.setAttribute(ICDTLaunchConfigurationConstants.ATTR_PROGRAM_NAME, fProgText.getText());

		config.setAttribute(IOspLaunchConfigurationConstants.ATTR_CODE_BINARY_SDK_PATH, fCodeBinPathSdkText.getText());
//		config.setAttribute(IOspLaunchConfigurationConstants.ATTR_RESOURCE_BINARY_PATH, fResBinPathText.getText());

	}

	/**
	 * Show a dialog that lists all main types
	 */
	protected void handleSearchButtonSelected() {

		if (getCProject() == null) {
			MessageDialog.openInformation(getShell(), OspLaunchMessages.getString("CMainTab.Project_required"), //$NON-NLS-1$
					OspLaunchMessages.getString("CMainTab.Enter_project_before_searching_for_program")); //$NON-NLS-1$
			return;
		}

		ILabelProvider programLabelProvider = new CElementLabelProvider() {

			public String getText(Object element) {
				if (element instanceof IBinary) {
					IBinary bin = (IBinary)element;
					StringBuffer name = new StringBuffer();
					name.append(bin.getPath().lastSegment());
					return name.toString();
				}
				return super.getText(element);
			}

			public Image getImage(Object element) {
				if (! (element instanceof ICElement)) {
					return super.getImage(element);
				}
				ICElement celement = (ICElement)element;

				if (celement.getElementType() == ICElement.C_BINARY) {
					IBinary belement = (IBinary)celement;
					if (belement.isExecutable()) {
						return DebugUITools.getImage(IDebugUIConstants.IMG_ACT_RUN);
					}
				}

				return super.getImage(element);
			}
		};

		ILabelProvider qualifierLabelProvider = new CElementLabelProvider() {

			public String getText(Object element) {
				if (element instanceof IBinary) {
					IBinary bin = (IBinary)element;
					StringBuffer name = new StringBuffer();
					name.append(bin.getCPU() + (bin.isLittleEndian() ? "le" : "be")); //$NON-NLS-1$ //$NON-NLS-2$
					name.append(" - "); //$NON-NLS-1$
					name.append(bin.getPath().toString());
					return name.toString();
				}
				return super.getText(element);
			}
		};

		TwoPaneElementSelector dialog = new TwoPaneElementSelector(getShell(), programLabelProvider, qualifierLabelProvider);
		dialog.setElements(getBinaryFiles(getCProject()));
		dialog.setMessage(OspLaunchMessages.getString("CMainTab.Choose_program_to_run")); //$NON-NLS-1$
		dialog.setTitle(OspLaunchMessages.getString("CMainTab.Program_Selection")); //$NON-NLS-1$
		dialog.setUpperListLabel(OspLaunchMessages.getString("Launch.common.BinariesColon")); //$NON-NLS-1$
		dialog.setLowerListLabel(OspLaunchMessages.getString("Launch.common.QualifierColon")); //$NON-NLS-1$
		dialog.setMultipleSelection(false);
		// dialog.set
		if (dialog.open() == Window.OK) {
			IBinary binary = (IBinary)dialog.getFirstResult();
			fProgText.setText(binary.getResource().getProjectRelativePath().toOSString());
		}

	}

	/**
	 * Show a dialog that lets the user select a project. This in turn provides context for the main
	 * type, allowing the user to key a main type name, or constraining the search for main types to
	 * the specified project.
	 */
	protected void handleBinaryBrowseButtonSelected() {
		final ICProject cproject = getCProject();
		if (cproject == null) {
			MessageDialog.openInformation(getShell(), OspLaunchMessages.getString("CMainTab.Project_required"), //$NON-NLS-1$
					OspLaunchMessages.getString("CMainTab.Enter_project_before_browsing_for_program")); //$NON-NLS-1$
			return;
		}
		FileDialog fileDialog = new FileDialog(getShell(), SWT.NONE);
		fileDialog.setFileName(fProgText.getText());
		String text= fileDialog.open();
		if (text != null) {
			fProgText.setText(text);
		}
	}
	
	protected void handleCodeBinPathSdkButtonSelected() {
		final ICProject cproject = getCProject();
		if (cproject == null) {
			MessageDialog.openInformation(getShell(), OspLaunchMessages.getString("CMainTab.Project_required"), //$NON-NLS-1$
					OspLaunchMessages.getString("CMainTab.Enter_project_before_browsing_for_program")); //$NON-NLS-1$
			return;
		}

		DirectoryDialog dialog = new DirectoryDialog(getShell(), SWT.OPEN|SWT.APPLICATION_MODAL);
		String text = fCodeBinPathSdkText.getText();
		if(text != null && text.trim().length() != 0) dialog.setFilterPath(text);
		dialog.setMessage("Select a folder from file system:");
		text = dialog.open();
		
		if (text != null) {
			fCodeBinPathSdkText.setText(text);
		}
	}	
/*	
	protected void handleResourceBinPathButtonSelected() {
		final ICProject cproject = getCProject();
		if (cproject == null) {
			MessageDialog.openInformation(getShell(), OspLaunchMessages.getString("CMainTab.Project_required"), //$NON-NLS-1$
					OspLaunchMessages.getString("CMainTab.Enter_project_before_browsing_for_program")); //$NON-NLS-1$
			return;
		}

		DirectoryDialog dialog = new DirectoryDialog(getShell(), SWT.OPEN|SWT.APPLICATION_MODAL);
		String text = fResBinPathText.getText();
		if(text != null && text.trim().length() != 0) dialog.setFilterPath(text);
		dialog.setMessage("Select a folder from file system:");
		text = dialog.open();

		if (text != null) {
			fResBinPathText.setText(text);
		}
	}		
*/
	/**
	 * Iterate through and suck up all of the executable files that we can find.
	 */

	protected IBinary[] getBinaryFiles_internal(final ICProject cproject) {
		final Display display;
		if (cproject == null || !cproject.exists()) {
			return null;
		}
		if (getShell() == null) {
			display = LaunchUIPlugin.getShell().getDisplay();
		} else {
			display = getShell().getDisplay();
		}
		final Object[] ret = new Object[1];
		BusyIndicator.showWhile(display, new Runnable() {

			public void run() {
				try {
					ret[0] = cproject.getBinaryContainer().getBinaries();
				} catch (CModelException e) {
					LaunchUIPlugin.errorDialog("Launch UI internal error", e); //$NON-NLS-1$
				}
			}
		});

		return (IBinary[])ret[0];
	}

	protected IBinary[] getBinaryFiles(final ICProject cproject) {
		
		IBinary[] bins = getBinaryFiles_internal(cproject);
		if( bins != null && bins.length > 0 )
		{
			List results = new ArrayList();
			for( int j = 0; j < bins.length; j++)
			{
				IPath path = ((IBinary)bins[j]).getPath().removeLastSegments(1);
				if( path != null )
				{
					String dirName = path.lastSegment();
					if( dirName != null && dirName.equals(IConstants.CONFIG_SIMUAL_DEBUG_DIR))
						results.add(bins[j]);
				}
			}
			
			if( results.size() > 0 )
			{
				return (IBinary[])results.toArray(new IBinary[results.size()]);
			}
		}

		return bins;
	}

	/**
	 * Show a dialog that lets the user select a project. This in turn provides context for the main
	 * type, allowing the user to key a main type name, or constraining the search for main types to
	 * the specified project.
	 */
	protected void handleProjectButtonSelected() {
		ICProject project = chooseCProject();
		if (project == null) {
			return;
		}

		String projectName = project.getElementName();
		fProjText.setText(projectName);
	}

	/**
	 * Realize a C Project selection dialog and return the first selected project, or null if there
	 * was none.
	 */
	protected ICProject chooseCProject() {
		try {
			ICProject[] projects = getCProjects();

			ILabelProvider labelProvider = new CElementLabelProvider();
			ElementListSelectionDialog dialog = new ElementListSelectionDialog(getShell(), labelProvider);
			dialog.setTitle(OspLaunchMessages.getString("CMainTab.Project_Selection")); //$NON-NLS-1$
			dialog.setMessage(OspLaunchMessages.getString("CMainTab.Choose_project_to_constrain_search_for_program")); //$NON-NLS-1$
			dialog.setElements(projects);

			ICProject cProject = getCProject();
			if (cProject != null) {
				dialog.setInitialSelections(new Object[]{cProject});
			}
			if (dialog.open() == Window.OK) {
				return (ICProject)dialog.getFirstResult();
			}
		} catch (CModelException e) {
			LaunchUIPlugin.errorDialog("Launch UI internal error", e); //$NON-NLS-1$			
		}
		return null;
	}

	/**
	 * Return an array a ICProject whose platform match that of the runtime env.
	 */
	protected ICProject[] getCProjects() throws CModelException {
		ICProject cproject[] = CoreModel.getDefault().getCModel().getCProjects();
		List<ICProject> list = new ArrayList<ICProject>(cproject.length);

		for (int i = 0; i < cproject.length; i++) {
			ICDescriptor cdesciptor = null;
			try {
				cdesciptor = CCorePlugin.getDefault().getCProjectDescription((IProject)cproject[i].getResource(), false);
				if (cdesciptor != null) {
					String projectPlatform = cdesciptor.getPlatform();
					if (filterPlatform.equals("*") //$NON-NLS-1$
							|| projectPlatform.equals("*") //$NON-NLS-1$
							|| filterPlatform.equalsIgnoreCase(projectPlatform) == true) {
						list.add(cproject[i]);
					}
				} else {
					list.add(cproject[i]);
				}
			} catch (CoreException e) {
				list.add(cproject[i]);
			}
		}
		return list.toArray(new ICProject[list.size()]);
	}

	/**
	 * Return the ICProject corresponding to the project name in the project name text field, or
	 * null if the text does not match a project name.
	 */
	protected ICProject getCProject() {
		String projectName = fProjText.getText().trim();
		if (projectName.length() < 1) {
			return null;
		}
		return CoreModel.getDefault().getCModel().getCProject(projectName);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#isValid(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public boolean isValid(ILaunchConfiguration config) {

		setErrorMessage(null);
		setMessage(null);

		if (dontCheckProgram)
			return true;

		String name = fProjText.getText().trim();
		if (name.length() == 0) {
			setErrorMessage(OspLaunchMessages.getString("CMainTab.Project_not_specified")); //$NON-NLS-1$
			return false;
		}
		if (!ResourcesPlugin.getWorkspace().getRoot().getProject(name).exists()) {
			setErrorMessage(OspLaunchMessages.getString("Launch.common.Project_does_not_exist")); //$NON-NLS-1$
			return false;
		}
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(name);
		if (!project.isOpen()) {
			setErrorMessage(OspLaunchMessages.getString("CMainTab.Project_must_be_opened")); //$NON-NLS-1$
			return false;
		}

		name = fProgText.getText().trim();
		if (name.length() == 0) {
			setErrorMessage(OspLaunchMessages.getString("CMainTab.Program_not_specified")); //$NON-NLS-1$
			return false;
		}
		if (name.equals(".") || name.equals("..")) { //$NON-NLS-1$ //$NON-NLS-2$
			setErrorMessage(OspLaunchMessages.getString("CMainTab.Program_does_not_exist")); //$NON-NLS-1$
			return false;
		}
		IPath exePath = new Path(name);
		if (!exePath.isAbsolute()) {
			IPath location = project.getLocation();
			if (location == null) {
				setErrorMessage(OspLaunchMessages.getString("CMainTab.Program_does_not_exist")); //$NON-NLS-1$
				return false;
			}

			exePath = location.append(name);
			if (!exePath.toFile().exists()) {
				// Try the old way, which is required to support linked resources.
				IFile projFile = null;					
				try {
					projFile = project.getFile(name);
				}
				catch (IllegalArgumentException exc) {}	// thrown if relative path that resolves to a root file ("..\somefile")
				if (projFile == null || !projFile.exists()) {
					setErrorMessage(OspLaunchMessages.getString("CMainTab.Program_does_not_exist")); //$NON-NLS-1$
					return false;
				}
				else {
					exePath = projFile.getLocation();
				}
			}
		} 
		if (!exePath.toFile().exists()) {
			setErrorMessage(OspLaunchMessages.getString("CMainTab.Program_does_not_exist")); //$NON-NLS-1$
			return false;
		}
		try {
			if (!isBinary(project, exePath)) {
				setErrorMessage(OspLaunchMessages.getString("CMainTab.Program_is_not_a_recongnized_executable")); //$NON-NLS-1$
				return false;
			}
		} catch (CoreException e) {
			LaunchUIPlugin.log(e);
			setErrorMessage(e.getLocalizedMessage());
			return false;
		}
		

		// check code binary path in Simulator file system
		String skdPath = fCodeBinPathSdkText.getText().trim();

		if (skdPath.length() == 0) {
			setErrorMessage("Code Binary Path in Simulator File System not specified"); //$NON-NLS-1$
			return false;
		}
		if (skdPath.equals(".") || skdPath.equals("..")) { //$NON-NLS-1$ //$NON-NLS-2$
			setErrorMessage("Code Binary Path in Simulator File System does not exist"); //$NON-NLS-1$
			return false;
		}
		
		IPath binSdkPath = new Path(PathResolver.getAbsolutePath(skdPath, project));
		if (!binSdkPath.isAbsolute()) {
			IPath location = project.getLocation();
			if (location == null) {
				setErrorMessage("Code Binary Path in Simulator File System does not exist"); //$NON-NLS-1$
				return false;
			}

			binSdkPath = location.append(skdPath);
			if (!exePath.toFile().exists()) {
				// Try the old way, which is required to support linked resources.
				IFile projFile = null;					
				try {
					projFile = project.getFile(skdPath);
				}
				catch (IllegalArgumentException exc) {}	// thrown if relative path that resolves to a root file ("..\somefile")
				if (projFile == null || !projFile.exists()) {
					setErrorMessage("Code Binary Path in Simulator File System does not exist"); //$NON-NLS-1$
					return false;
				}
				else {
					binSdkPath = projFile.getLocation();
				}
			}
		} 
//		if (!binSdkPath.toFile().exists()) {
//			setErrorMessage("Code Binary Path in Simulator File System does not exist"); //$NON-NLS-1$
//			return false;
//		}
		
/*		
		// check code Resource Binary Path in Simulator File System
		String resourcePath = fResBinPathText.getText().trim();

		if (resourcePath.length() == 0) {
			setErrorMessage("Resource Binary Path in Simulator File System not specified"); //$NON-NLS-1$
			return false;
		}
		if (resourcePath.equals(".") || resourcePath.equals("..")) { //$NON-NLS-1$ //$NON-NLS-2$
			setErrorMessage("Resource Binary Path in Simulator File System does not exist"); //$NON-NLS-1$
			return false;
		}
		
		IPath binResPath = new Path(resourcePath);
		if (!binResPath.isAbsolute()) {
			IPath location = project.getLocation();
			if (location == null) {
				setErrorMessage("Resource Binary Path in Simulator File System does not exist"); //$NON-NLS-1$
				return false;
			}

			binResPath = location.append(resourcePath);
			if (!exePath.toFile().exists()) {
				// Try the old way, which is required to support linked resources.
				IFile projFile = null;					
				try {
					projFile = project.getFile(resourcePath);
				}
				catch (IllegalArgumentException exc) {}	// thrown if relative path that resolves to a root file ("..\somefile")
				if (projFile == null || !projFile.exists()) {
					setErrorMessage("Resource Binary Path in Simulator File System does not exist"); //$NON-NLS-1$
					return false;
				}
				else {
					binResPath = projFile.getLocation();
				}
			}
		} 
		if (!binResPath.toFile().exists()) {
			setErrorMessage("Resource Binary Path in Simulator File System does not exist"); //$NON-NLS-1$
			return false;
		}
*/		
		return true;
	}

	/**
	 * @param project
	 * @param exePath
	 * @return
	 * @throws CoreException
	 */
	protected boolean isBinary(IProject project, IPath exePath) throws CoreException {
		ICExtensionReference[] parserRef = CCorePlugin.getDefault().getBinaryParserExtensions(project);
		for (int i = 0; i < parserRef.length; i++) {
			try {
				IBinaryParser parser = (IBinaryParser)parserRef[i].createExtension();
				IBinaryObject exe = (IBinaryObject)parser.getBinary(exePath);
				if (exe != null) {
					return true;
				}
			} catch (ClassCastException e) {
				e.printStackTrace();
			} catch (IOException e) {
				// Do not need check
			}
		}
		IBinaryParser parser = CCorePlugin.getDefault().getDefaultBinaryParser();
		try {
			IBinaryObject exe = (IBinaryObject)parser.getBinary(exePath);
			return exe != null;
		} catch (ClassCastException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#setDefaults(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	public void setDefaults(ILaunchConfigurationWorkingCopy config) {
		// We set empty attributes for project & program so that when one config
		// is
		// compared to another, the existence of empty attributes doesn't cause
		// an
		// incorrect result (the performApply() method can result in empty
		// values
		// for these attributes being set on a config if there is nothing in the
		// corresponding text boxes)
		// plus getContext will use this to base context from if set.
		config.setAttribute(ICDTLaunchConfigurationConstants.ATTR_PROJECT_NAME, EMPTY_STRING);
		ICElement cElement = null;
		cElement = getContext(config, getPlatform(config));
		if (cElement != null) {
			initializeCProject(cElement, config);
			initializeProgramName(cElement, config);
		}
		if (wantsTerminalOption()) {
			config.setAttribute(ICDTLaunchConfigurationConstants.ATTR_USE_TERMINAL, ICDTLaunchConfigurationConstants.USE_TERMINAL_DEFAULT);
		}
		
		config.setAttribute(IOspLaunchConfigurationConstants.ATTR_CODE_BINARY_SDK_PATH, IOspLaunchConfigurationConstants.DEFAULT_SUMUAL_CODE_BINARY_PATH);
		config.setAttribute(IOspLaunchConfigurationConstants.ATTR_RESOURCE_BINARY_PATH, EMPTY_STRING);
	}

	/**
	 * Set the program name attributes on the working copy based on the ICElement
	 */
	protected void initializeProgramName(ICElement cElement, ILaunchConfigurationWorkingCopy config) {

		boolean renamed = false;

		if (!(cElement instanceof IBinary))
		{
			cElement = cElement.getCProject();
		}
		
		if (cElement instanceof ICProject) {

			IProject project = cElement.getCProject().getProject();
			String name = project.getName();
			ICProjectDescription projDes = CCorePlugin.getDefault().getProjectDescription(project);
			if (projDes != null) {
				//String buildConfigName = projDes.getActiveConfiguration().getName();
				String buildConfigName = getSimulatorConfiguration(projDes).getName();
				//bug 234951
				name = OspLaunchMessages.getFormattedString("CMainTab.Configuration_name", new String[]{name, buildConfigName}); //$NON-NLS-1$
			}
			name = getLaunchConfigurationDialog().generateName(name);
			config.rename(name);
			renamed = true;
		}

		IBinary binary = null;
		if (cElement instanceof ICProject) {
			IBinary[] bins = getBinaryFiles((ICProject)cElement);
			if (bins != null && bins.length == 1) {
				binary = bins[0];
			}
		} else if (cElement instanceof IBinary) {
			binary = (IBinary)cElement;
		}

		if (binary != null) {
			String path;
			path = binary.getResource().getProjectRelativePath().toOSString();
			config.setAttribute(ICDTLaunchConfigurationConstants.ATTR_PROGRAM_NAME, path);
			if (!renamed)
			{
				String name = binary.getElementName();
				int index = name.lastIndexOf('.');
				if (index > 0) {
					name = name.substring(0, index);
				}
				name = getLaunchConfigurationDialog().generateName(name);
				config.rename(name);
				renamed = true;				
			}
		}
		
		if (!renamed)
		{
			String name = getLaunchConfigurationDialog().generateName(cElement.getCProject().getElementName());
			config.rename(name);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getName()
	 */
	public String getName() {
		return OspLaunchMessages.getString("CMainTab.Main"); //$NON-NLS-1$
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getImage()
	 */
	public Image getImage() {
		return LaunchImages.get(LaunchImages.IMG_VIEW_MAIN_TAB);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.debug.ui.AbstractLaunchConfigurationTab#updateLaunchConfigurationDialog()
	 */
	protected void updateLaunchConfigurationDialog() {
		super.updateLaunchConfigurationDialog();
	}
	
	private ICConfigurationDescription getSimulatorConfiguration(ICProjectDescription projDesc)
	{
		if( projDesc == null ) return null;
		
		ICConfigurationDescription configDescs[] = projDesc.getConfigurations();
		
		for( int i = 0; i < configDescs.length; i++)
		{
			if( IConstants.CONFIG_SIMUAL_DEBUG_NAME.equals(configDescs[i].getName()))
				return 	configDescs[i];
		}
		
		return projDesc.getActiveConfiguration();
	}
	
}
