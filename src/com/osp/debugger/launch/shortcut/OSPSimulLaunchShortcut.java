/*******************************************************************************
 * Copyright (c) 2005, 2007 QNX Software Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * QNX Software Systems - Initial API and implementation
 * Ken Ryall (Nokia) - bug 178731
 *******************************************************************************/
package com.osp.debugger.launch.shortcut;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.model.CModelException;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.IBinary;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.debug.core.CDebugCorePlugin;
import org.eclipse.cdt.debug.core.ICDTLaunchConfigurationConstants;
import org.eclipse.cdt.debug.core.ICDebugConfiguration;
import org.eclipse.cdt.debug.mi.core.IMILaunchConfigurationConstants;
import org.eclipse.cdt.debug.ui.CDebugUIPlugin;
import org.eclipse.cdt.debug.ui.ICDebuggerPage;
import org.eclipse.cdt.launch.AbstractCLaunchDelegate;
import org.eclipse.cdt.launch.internal.ui.LaunchUIPlugin;
import org.eclipse.cdt.ui.CElementLabelProvider;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
//import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugModelPresentation;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.dialogs.TwoPaneElementSelector;

import com.osp.debugger.IDebugConstants;
import com.osp.debugger.launch.IOspLaunchConfigurationConstants;
import com.osp.debugger.launch.OspLaunchMessages;
import com.osp.ide.IConstants;

public class OSPSimulLaunchShortcut implements ILaunchShortcut {

	static final boolean bRunProgress = false;
	
	public void launch(IEditorPart editor, String mode) {
		searchAndLaunch(new Object[] { editor.getEditorInput()}, mode);
	}

	public void launch(ISelection selection, String mode) {
		if (selection instanceof IStructuredSelection) {
			searchAndLaunch(((IStructuredSelection) selection).toArray(), mode);
		}
	}

	public void launch(IBinary bin, String mode) {
        ILaunchConfiguration config = findLaunchConfiguration(bin, mode);
        if (config != null) {
            DebugUITools.launch(config, mode);
        }
    }

	/**
	 * Locate a configuration to relaunch for the given type.  If one cannot be found, create one.
	 * 
	 * @return a re-useable config or <code>null</code> if none
	 */
	protected ILaunchConfiguration findLaunchConfiguration(IBinary bin, String mode) {
		ILaunchConfiguration configuration = null;
		ILaunchConfigurationType configType = getCLaunchConfigType();
		List candidateConfigs = Collections.EMPTY_LIST;
		try {
			ILaunchConfiguration[] configs = DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurations(configType);
			candidateConfigs = new ArrayList(configs.length);
			for (int i = 0; i < configs.length; i++) {
				ILaunchConfiguration config = configs[i];
				IPath programPath = AbstractCLaunchDelegate.getProgramPath(config);
				String projectName = AbstractCLaunchDelegate.getProjectName(config);
				IPath name = bin.getResource().getProjectRelativePath();
				if (programPath != null && programPath.equals(name)) {
					if (projectName != null && projectName.equals(bin.getCProject().getProject().getName())) {
						candidateConfigs.add(config);
					}
				}
			}
		} catch (CoreException e) {
			LaunchUIPlugin.log(e);
		}

		// If there are no existing configs associated with the IBinary, create one.
		// If there is exactly one config associated with the IBinary, return it.
		// Otherwise, if there is more than one config associated with the IBinary, prompt the
		// user to choose one.
		int candidateCount = candidateConfigs.size();
		if (candidateCount < 1) {
			// Set the default debugger based on the active toolchain on the project (if possible)
			ICDebugConfiguration debugConfig = null;
			IProject project = bin.getResource().getProject();
           	ICProjectDescription projDesc = CoreModel.getDefault().getProjectDescription(project);
           	//ICConfigurationDescription configDesc = projDesc.getActiveConfiguration();
           	ICConfigurationDescription configDesc = getSimulatorConfiguration(projDesc);
           	String configId = configDesc.getId();
       		ICDebugConfiguration[] debugConfigs = CDebugCorePlugin.getDefault().getActiveDebugConfigurations();
       		int matchLength = 0;
       		for (int i = 0; i < debugConfigs.length; ++i) {
       			ICDebugConfiguration dc = debugConfigs[i];
       			String[] patterns = dc.getSupportedBuildConfigPatterns();
       			if (patterns != null) {
       				for (int j = 0; j < patterns.length; ++j) {
       					if (patterns[j].length() > matchLength && configId.matches(patterns[j])) {
       						debugConfig = dc;
       						matchLength = patterns[j].length();
       					}
       				}
       			}
			}

			if ( debugConfig == null ) {
/*				
				// Prompt the user if more then 1 debugger.
				String programCPU = bin.getCPU();
				String os = IDebugConstants.LAUNCH_OS_TYPE;
				debugConfigs = CDebugCorePlugin.getDefault().getActiveDebugConfigurations();
				List debugList = new ArrayList(debugConfigs.length);
				for (int i = 0; i < debugConfigs.length; i++) {
					String platform = debugConfigs[i].getPlatform();
					if (debugConfigs[i].supportsMode(ICDTLaunchConfigurationConstants.DEBUGGER_MODE_RUN)) {
						if (platform.equals("*") || platform.equals(os)) { //$NON-NLS-1$
							if (debugConfigs[i].supportsCPU(programCPU)) 
								debugList.add(debugConfigs[i]);
						}
					}
				}
				debugConfigs = (ICDebugConfiguration[]) debugList.toArray(new ICDebugConfiguration[0]);
				if (debugConfigs.length == 1) {
					debugConfig = debugConfigs[0];
				} else if (debugConfigs.length > 1) {
					debugConfig = chooseDebugConfig(debugConfigs, mode);
				}
*/
				debugConfigs = CDebugCorePlugin.getDefault().getActiveDebugConfigurations();
				List debugList = new ArrayList(debugConfigs.length);
				
				String defaultDebuggerId = getDefaultDebuggerId();
				
				for (int i = 0; i < debugConfigs.length; i++) {
					if (defaultDebuggerId.equals(debugConfigs[i].getID())) {
						debugConfig = debugConfigs[i];
						break;
					}
				}
			}
			
			if (debugConfig != null) {
				configuration = createConfiguration(bin, debugConfig);
			}
		} else if (candidateCount == 1) {
			configuration = (ILaunchConfiguration) candidateConfigs.get(0);
		} else {
			// Prompt the user to choose a config.  A null result means the user
			// cancelled the dialog, in which case this method returns null,
			// since cancelling the dialog should also cancel launching anything.
			configuration = chooseConfiguration(candidateConfigs, mode);
		}
		return configuration;
	}

	/**
	 * Method createConfiguration.
	 * @param bin
	 * @return ILaunchConfiguration
	 */
	private ILaunchConfiguration createConfiguration(IBinary bin, ICDebugConfiguration debugConfig) {
		ILaunchConfiguration config = null;
		try {
			
			IProject project = bin.getResource().getProject();
			String name = project.getName();
			ICProjectDescription projDes = CCorePlugin.getDefault().getProjectDescription(project);
			if (projDes != null) {
				//String buildConfigName = projDes.getActiveConfiguration().getName();
				String buildConfigName = getSimulatorConfiguration(projDes).getName();
				//bug 234951
				name = OspLaunchMessages.getFormattedString("CMainTab.Configuration_name", new String[]{name, buildConfigName}); //$NON-NLS-1$
			}
			
			String progName = bin.getResource().getProjectRelativePath().toOSString();
			ILaunchConfigurationType configType = getCLaunchConfigType();
			ILaunchConfigurationWorkingCopy wc =
				//configType.newInstance(null, getLaunchManager().generateUniqueLaunchConfigurationNameFrom(bin.getElementName()));
				configType.newInstance(null, getLaunchManager().generateUniqueLaunchConfigurationNameFrom(name));
			wc.setAttribute(ICDTLaunchConfigurationConstants.ATTR_PROGRAM_NAME, progName);
			wc.setAttribute(ICDTLaunchConfigurationConstants.ATTR_PROJECT_NAME, bin.getCProject().getElementName());
			//wc.setMappedResources(new IResource[] {bin.getResource(), bin.getResource().getProject()});
			wc.setMappedResources(new IResource[] {bin.getResource().getProject()});
			wc.setAttribute(ICDTLaunchConfigurationConstants.ATTR_WORKING_DIRECTORY, (String) null);
//			wc.setAttribute(ICDTLaunchConfigurationConstants.ATTR_DEBUGGER_STOP_AT_MAIN, IOspLaunchConfigurationConstants.DEFAULT_STOP_AT_MAIN);
			wc.setAttribute(ICDTLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, IOspLaunchConfigurationConstants.DEFAULT_SIMUL_ARG);
			
			if( IDebugConstants.ATTACH_MODE ) // for ATTACH
				wc.setAttribute(
						ICDTLaunchConfigurationConstants.ATTR_DEBUGGER_START_MODE,
						ICDTLaunchConfigurationConstants.DEBUGGER_MODE_ATTACH); 
			else
				wc.setAttribute(
						ICDTLaunchConfigurationConstants.ATTR_DEBUGGER_START_MODE,
						ICDTLaunchConfigurationConstants.DEBUGGER_MODE_RUN); // for RUN
			
			wc.setAttribute(ICDTLaunchConfigurationConstants.ATTR_DEBUGGER_ID, debugConfig.getID());
			wc.setAttribute(IOspLaunchConfigurationConstants.ATTR_CODE_BINARY_SDK_PATH, IOspLaunchConfigurationConstants.DEFAULT_SUMUAL_CODE_BINARY_PATH);

			//ICProjectDescription projDes = CCorePlugin.getDefault().getProjectDescription(bin.getCProject().getProject());
			if (projDes != null)
			{
				//String buildConfigID = projDes.getActiveConfiguration().getId();
				String buildConfigID = getSimulatorConfiguration(projDes).getId();
				wc.setAttribute(ICDTLaunchConfigurationConstants.ATTR_PROJECT_BUILD_CONFIG_ID, buildConfigID);				
			}
			wc.setAttribute( IMILaunchConfigurationConstants.ATTR_DEBUG_NAME, IDebugConstants.DEBUGGER_SIMUL_DEBUG_NAME_DEFAULT);
			
			wc.setAttribute( IMILaunchConfigurationConstants.ATTR_DEBUGGER_VERBOSE_MODE, IDebugConstants.DEBUGGER_VERBOSE_MODE_DEFAULT );
			
			// Load up the debugger page to set the defaults. There should probably be a separate
			// extension point for this.
			ICDebuggerPage page = CDebugUIPlugin.getDefault().getDebuggerPage(debugConfig.getID());
			page.setDefaults(wc);
			
			config = wc.doSave();
		} catch (CoreException ce) {
			LaunchUIPlugin.log(ce);
		}
		return config;
	}

	/**
	 * Method getCLaunchConfigType.
	 * @return ILaunchConfigurationType
	 */
	protected ILaunchConfigurationType getCLaunchConfigType() {
		return getLaunchManager().getLaunchConfigurationType(IDebugConstants.ID_LAUNCH_OSP_SIMUAL_APP);
	}
	
	protected String getDefaultDebuggerId()
	{
		return IDebugConstants.ID_LAUNCH_OSP_SIMUAL_DEBUGGER;
	}

	protected ILaunchManager getLaunchManager() {
		return DebugPlugin.getDefault().getLaunchManager();
	}

	/**
	 * Convenience method to get the window that owns this action's Shell.
	 */
	protected Shell getShell() {
		return LaunchUIPlugin.getActiveWorkbenchShell();
	}

	protected String getDebugConfigDialogTitleString(ICDebugConfiguration [] configList, String mode) {
		return OspLaunchMessages.getString("CApplicationLaunchShortcut.LaunchDebugConfigSelection");  //$NON-NLS-1$
	}
	
	protected String getDebugConfigDialogMessageString(ICDebugConfiguration [] configList, String mode) {
		if (mode.equals(ILaunchManager.DEBUG_MODE)) {
			return OspLaunchMessages.getString("CApplicationLaunchShortcut.ChooseConfigToDebug");  //$NON-NLS-1$
		} else if (mode.equals(ILaunchManager.RUN_MODE)) {
			return OspLaunchMessages.getString("CApplicationLaunchShortcut.ChooseConfigToRun");  //$NON-NLS-1$
		}
		return OspLaunchMessages.getString("CApplicationLaunchShortcut.Invalid_launch_mode_1"); //$NON-NLS-1$
	}


	/**
	 * Show a selection dialog that allows the user to choose one of the specified
	 * launch configurations.  Return the chosen config, or <code>null</code> if the
	 * user cancelled the dialog.
	 */
	protected ILaunchConfiguration chooseConfiguration(List configList, String mode) {
		IDebugModelPresentation labelProvider = DebugUITools.newDebugModelPresentation();
		ElementListSelectionDialog dialog = new ElementListSelectionDialog(getShell(), labelProvider);
		dialog.setElements(configList.toArray());
		dialog.setTitle(getLaunchSelectionDialogTitleString(configList, mode)); 
		dialog.setMessage(getLaunchSelectionDialogMessageString(configList, mode)); 
		dialog.setMultipleSelection(false);
		int result = dialog.open();
		labelProvider.dispose();
		if (result == Window.OK) {
			return (ILaunchConfiguration) dialog.getFirstResult();
		}
		return null;
	}

	protected String getLaunchSelectionDialogTitleString(List configList, String mode) {
		return OspLaunchMessages.getString("CApplicationLaunchShortcut.LaunchConfigSelection");  //$NON-NLS-1$
	}
	
	protected String getLaunchSelectionDialogMessageString(List binList, String mode) {
		if (mode.equals(ILaunchManager.DEBUG_MODE)) {
			return OspLaunchMessages.getString("CApplicationLaunchShortcut.ChooseLaunchConfigToDebug");  //$NON-NLS-1$
		} else if (mode.equals(ILaunchManager.RUN_MODE)) {
			return OspLaunchMessages.getString("CApplicationLaunchShortcut.ChooseLaunchConfigToRun");  //$NON-NLS-1$
		}
		return OspLaunchMessages.getString("CApplicationLaunchShortcut.Invalid_launch_mode_2"); //$NON-NLS-1$
	}

	/**
	 * Prompts the user to select a  binary
	 * 
	 * @return the selected binary or <code>null</code> if none.
	 */
	protected IBinary chooseBinary(List binList, String mode) {
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
		dialog.setElements(binList.toArray());
		dialog.setTitle(getBinarySelectionDialogTitleString(binList, mode));
		dialog.setMessage(getBinarySelectionDialogMessageString(binList, mode));
		dialog.setUpperListLabel(OspLaunchMessages.getString("Launch.common.BinariesColon")); //$NON-NLS-1$
		dialog.setLowerListLabel(OspLaunchMessages.getString("Launch.common.QualifierColon")); //$NON-NLS-1$
		dialog.setMultipleSelection(false);
		if (dialog.open() == Window.OK) {
			return (IBinary) dialog.getFirstResult();
		}

		return null;
	}
	
	protected String getBinarySelectionDialogTitleString(List binList, String mode) {
		return OspLaunchMessages.getString("CApplicationLaunchShortcut.CLocalApplication");  //$NON-NLS-1$
	}
	
	protected String getBinarySelectionDialogMessageString(List binList, String mode) {
		if (mode.equals(ILaunchManager.DEBUG_MODE)) {
			return "Choose a bada Sumulator application to debug";  //$NON-NLS-1$
		} else if (mode.equals(ILaunchManager.RUN_MODE)) {
			return "Choose a bada Sumulator application to run";  //$NON-NLS-1$
		}
		return OspLaunchMessages.getString("CApplicationLaunchShortcut.Invalid_launch_mode_3"); //$NON-NLS-1$
	}

	/**
	 * Method searchAndLaunch.
	 * @param objects
	 * @param mode
	 */
	protected void searchAndLaunch(final Object[] elements, String mode) {
		if( bRunProgress ) searchAndLaunchProgress(elements, mode);
		else searchAndLaunchFunc(elements, mode);
	}

	private void searchAndLaunchFunc(final Object[] elements, String mode) {
		if (elements != null && elements.length > 0) {
			IBinary bin = null;
			if (elements.length == 1 && elements[0] instanceof IBinary) {
				bin = (IBinary)elements[0];
			} else {
				List results = new ArrayList();
				int nElements = elements.length;
				try {
					for (int i = 0; i < nElements; i++) {
						if (elements[i] instanceof IAdaptable) {
							IResource r = (IResource) ((IAdaptable) elements[i]).getAdapter(IResource.class);
							if (r != null) {
								ICProject cproject = CoreModel.getDefault().create(r.getProject());
								if (cproject != null) {
									try {
										IBinary[] bins = cproject.getBinaryContainer().getBinaries();

										for (int j = 0; j < bins.length; j++) {
											if (bins[j].isSharedLib()) {
												IPath path = bins[j].getPath().removeLastSegments(1);
												if( path != null )
												{
													String dirName = path.lastSegment();
													if( dirName != null && dirName.equals(IConstants.CONFIG_SIMUAL_DEBUG_DIR))
														results.add(bins[j]);
												}
											}
										}
									} catch (CModelException e) {
										e.printStackTrace();
									}
								}
							}
						}
					}
				} finally {
				}

				int count = results.size();
				if (count == 0) {					
					MessageDialog.openError(getShell(), OspLaunchMessages.getString("CApplicationLaunchShortcut.Application_Launcher"), OspLaunchMessages.getString("CApplicationLaunchShortcut.Launch_failed_no_binaries")); //$NON-NLS-1$ //$NON-NLS-2$
				} else if (count > 1) {
					bin = chooseBinary(results, mode);
				} else {
					bin = (IBinary)results.get(0);
				}
			}
			if (bin != null) {
				launch(bin, mode);
			}
		} else {
			MessageDialog.openError(getShell(), OspLaunchMessages.getString("CApplicationLaunchShortcut.Application_Launcher"), OspLaunchMessages.getString("CApplicationLaunchShortcut.Launch_failed_no_project_selected")); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}
	
	
	
	private void searchAndLaunchProgress(final Object[] elements, String mode) {
		if (elements != null && elements.length > 0) {
			IBinary bin = null;
			if (elements.length == 1 && elements[0] instanceof IBinary) {
				bin = (IBinary)elements[0];
			} else {
				final List results = new ArrayList();
				ProgressMonitorDialog dialog = new ProgressMonitorDialog(getShell());
				IRunnableWithProgress runnable = new IRunnableWithProgress() {
					public void run(IProgressMonitor pm) throws InterruptedException {
						int nElements = elements.length;
						pm.beginTask("Looking for executables", nElements); //$NON-NLS-1$
						try {
							IProgressMonitor sub = new SubProgressMonitor(pm, 1);
							for (int i = 0; i < nElements; i++) {
								if (elements[i] instanceof IAdaptable) {
									IResource r = (IResource) ((IAdaptable) elements[i]).getAdapter(IResource.class);
									if (r != null) {
										ICProject cproject = CoreModel.getDefault().create(r.getProject());
										if (cproject != null) {
											try {
												IBinary[] bins = cproject.getBinaryContainer().getBinaries();

												for (int j = 0; j < bins.length; j++) {
													if (bins[j].isSharedLib()) {
														IPath path = bins[j].getPath().removeLastSegments(1);
														if( path != null )
														{
															String dirName = path.lastSegment();
															if( dirName != null && dirName.equals(IConstants.CONFIG_SIMUAL_DEBUG_DIR))
																results.add(bins[j]);
														}
													}
												}
											} catch (CModelException e) {
												e.printStackTrace();
											}
										}
									}
								}
								if (pm.isCanceled()) {
									throw new InterruptedException();
								}
								sub.done();
							}
						} finally {
							pm.done();
						}
					}
				};
				try {
					dialog.run(true, true, runnable);
				} catch (InterruptedException e) {
					return;
				} catch (InvocationTargetException e) {
					MessageDialog.openError(getShell(), OspLaunchMessages.getString("CApplicationLaunchShortcut.Application_Launcher"), e.getMessage()); //$NON-NLS-1$
					return;
				}
				int count = results.size();
				if (count == 0) {					
					MessageDialog.openError(getShell(), OspLaunchMessages.getString("CApplicationLaunchShortcut.Application_Launcher"), OspLaunchMessages.getString("CApplicationLaunchShortcut.Launch_failed_no_binaries")); //$NON-NLS-1$ //$NON-NLS-2$
				} else if (count > 1) {
					bin = chooseBinary(results, mode);
				} else {
					bin = (IBinary)results.get(0);
				}
			}
			if (bin != null) {
				launch(bin, mode);
			}
		} else {
			MessageDialog.openError(getShell(), OspLaunchMessages.getString("CApplicationLaunchShortcut.Application_Launcher"), OspLaunchMessages.getString("CApplicationLaunchShortcut.Launch_failed_no_project_selected")); //$NON-NLS-1$ //$NON-NLS-2$
		}
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
