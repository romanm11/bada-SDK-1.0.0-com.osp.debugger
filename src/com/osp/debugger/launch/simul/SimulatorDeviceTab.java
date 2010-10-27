package com.osp.debugger.launch.simul;

//import org.eclipse.cdt.core.model.ICProject;
//import org.eclipse.cdt.debug.core.ICDTLaunchConfigurationConstants;
import org.eclipse.cdt.launch.internal.ui.LaunchUIPlugin;
import org.eclipse.cdt.launch.ui.CLaunchConfigurationTab;
import org.eclipse.cdt.launch.ui.ICDTLaunchHelpContextIds;
import org.eclipse.core.runtime.CoreException;
//import org.eclipse.core.runtime.IPath;
//import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.osp.debugger.DebuggerPlugin;
import com.osp.debugger.IDebugConstants;
import com.osp.debugger.launch.IOspLaunchConfigurationConstants;
import com.osp.debugger.launch.OspLaunchMessages;

public class SimulatorDeviceTab extends CLaunchConfigurationTab {
	
	protected static final String EMPTY_STRING = ""; //$NON-NLS-1$
	private ImageDescriptor imgdesc_title = DebuggerPlugin.getDefault().createImageDescriptor(IDebugConstants.IMG_TAB_DEVICE);
	private Image img_title=null;

	final String DEFAULT_INFOFILE = "$(SDKROOT}\\dbi\\ospphone.dbi";
	
	// Device Info File
	protected Label fDevInfoFileLabel;
	protected Text fDevInfoFileText;
	protected Button fDevInfoFileButton;	
	
	// Device Info
	private Text fTargetInfoText;
	
	// Command Line Option
	protected Label fCmdLineOptionLabel;
	protected Text fCmdLineOptionText;	
	
	public SimulatorDeviceTab() {
		
	}

	public void createControl(Composite parent) {
		// TODO Auto-generated method stub
		Composite comp = new Composite(parent, SWT.NONE);
		setControl(comp);

		LaunchUIPlugin.getDefault().getWorkbench().getHelpSystem().setHelp(getControl(), ICDTLaunchHelpContextIds.LAUNCH_CONFIGURATION_DIALOG_MAIN_TAB);

		GridLayout topLayout = new GridLayout();
		topLayout.marginHeight = 10;
		comp.setLayout(topLayout);
		
		createDeviceInfoFileGroup(comp, 1);
		createVerticalSpacer(comp, 1);
		createTargetInfoGroup(comp, 1);
		createCmdLineOptionGroup(comp, 1);
		
		LaunchUIPlugin.setDialogShell(parent.getShell());
	}
	
	protected void createDeviceInfoFileGroup(Composite parent, int colSpan) {
		Composite mainComp = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		mainComp.setLayout(layout);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = colSpan;
		mainComp.setLayoutData(gd);

		fDevInfoFileLabel = new Label(mainComp, SWT.NONE);
		fDevInfoFileLabel.setText("Device Information File"); //$NON-NLS-1$
		gd = new GridData();
		gd.horizontalSpan = 2;
		fDevInfoFileLabel.setLayoutData(gd);

		fDevInfoFileText = new Text(mainComp, SWT.SINGLE | SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		fDevInfoFileText.setLayoutData(gd);
		fDevInfoFileText.addModifyListener(new ModifyListener() {

			public void modifyText(ModifyEvent evt) {
				updateLaunchConfigurationDialog();
			}
		});
		

		fDevInfoFileButton = createPushButton(mainComp, OspLaunchMessages.getString("Launch.common.Browse_2"), null); //$NON-NLS-1$
		fDevInfoFileText.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent evt) {
				handleDevInfoFileTextButtonSelected();
				updateLaunchConfigurationDialog();
			}
		});
	}
	
	
	protected void createTargetInfoGroup(Composite parent, int colSpan) {

		Group mainGroup = new Group(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		mainGroup.setLayout(layout);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.horizontalSpan = colSpan;
		mainGroup.setLayoutData(gd);
		mainGroup.setText("Target Information");
		
		fTargetInfoText =  new Text(mainGroup, SWT.MULTI | SWT.READ_ONLY);
		fTargetInfoText.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		fTargetInfoText.setText("LCD Size:\r\nKeypad:\r\n");
	}
	
	protected void createCmdLineOptionGroup(Composite parent, int colSpan) {

		Composite mainComp = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		mainComp.setLayout(layout);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = colSpan;
		mainComp.setLayoutData(gd);

		fCmdLineOptionLabel = new Label(mainComp, SWT.NONE);
		fCmdLineOptionLabel.setText("Additional Simulator Command Line Options"); //$NON-NLS-1$
		gd = new GridData();
		fCmdLineOptionLabel.setLayoutData(gd);

		fCmdLineOptionText = new Text(mainComp, SWT.SINGLE | SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		fCmdLineOptionText.setLayoutData(gd);
		fCmdLineOptionText.addModifyListener(new ModifyListener() {

			public void modifyText(ModifyEvent evt) {
				updateLaunchConfigurationDialog();
			}
		});
	}
	
	

	public String getName() {
		// TODO Auto-generated method stub
		return "Device";
	}

	public Image getImage() {
		if( img_title == null ) 
		{
			if( imgdesc_title != null ) 
				img_title = imgdesc_title.createImage();
		}
		
		return img_title;
	}		

	public void initializeFrom(ILaunchConfiguration configuration) {
		// TODO Auto-generated method stub
		updateDeviceInfoFileFromConfig(configuration);
		updateCmdLineOptionFromConfig(configuration);
	}
	
	protected void updateDeviceInfoFileFromConfig(ILaunchConfiguration config) {
		String fileName = DEFAULT_INFOFILE;
		
		try {
			fileName = config.getAttribute(IOspLaunchConfigurationConstants.ATTR_DEVICE_INFOFILE, DEFAULT_INFOFILE);
		} catch (CoreException ce) {
			LaunchUIPlugin.log(ce);
		}
		fDevInfoFileText.setText(fileName);		
	}
	
	protected void updateCmdLineOptionFromConfig(ILaunchConfiguration config) {
		String option = EMPTY_STRING;
		
		try {
			option = config.getAttribute(IOspLaunchConfigurationConstants.ATTR_DEVICE_CMDLINE_OPT, EMPTY_STRING);
		} catch (CoreException ce) {
			LaunchUIPlugin.log(ce);
		}
		fCmdLineOptionText.setText(option);		
	}	

	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		// TODO Auto-generated method stub

		configuration.setAttribute(IOspLaunchConfigurationConstants.ATTR_DEVICE_INFOFILE, fDevInfoFileText.getText());
		configuration.setAttribute(IOspLaunchConfigurationConstants.ATTR_DEVICE_CMDLINE_OPT, fCmdLineOptionText.getText());
	}
	
	protected void handleDevInfoFileTextButtonSelected() {
		FileDialog fileDialog = new FileDialog(getShell(), SWT.NONE);
		fileDialog.setFileName(fDevInfoFileText.getText());
		String text= fileDialog.open();
		if (text != null) {
			fDevInfoFileText.setText(text);
		}
	}	
	
	public boolean isValid(ILaunchConfiguration config) {

		setErrorMessage(null);
		setMessage(null);
		
		// check code resource binary path
		String devFile = fDevInfoFileText.getText().trim();

		if (devFile.length() == 0) {
			setErrorMessage("Device information file not specified"); //$NON-NLS-1$
			return false;
		}
		if (devFile.equals(".") || devFile.equals("..")) { //$NON-NLS-1$ //$NON-NLS-2$
			setErrorMessage("Device information file does not exist"); //$NON-NLS-1$
			return false;
		}
/*		
		IPath binResPath = new Path(devFile);
		if (!binResPath.isAbsolute()) {
		} 
		if (!binResPath.toFile().exists()) {
			setErrorMessage("Device information file does not exist"); //$NON-NLS-1$
			return false;
		}
*/		
		
		return true;
	}


	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		// TODO Auto-generated method stub
		configuration.setAttribute(IOspLaunchConfigurationConstants.ATTR_DEVICE_INFOFILE, DEFAULT_INFOFILE);
		configuration.setAttribute(IOspLaunchConfigurationConstants.ATTR_DEVICE_CMDLINE_OPT, EMPTY_STRING);
	}

	protected void updateLaunchConfigurationDialog() {
		super.updateLaunchConfigurationDialog();
	}	
}
