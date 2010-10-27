package com.osp.debugger.launch.target;

import org.eclipse.cdt.launch.internal.ui.LaunchUIPlugin;
import org.eclipse.cdt.launch.ui.CLaunchConfigurationTab;
import org.eclipse.cdt.launch.ui.ICDTLaunchHelpContextIds;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;

import com.osp.debugger.DebuggerPlugin;
import com.osp.debugger.IDebugConstants;

public class TargetDeviceTab extends CLaunchConfigurationTab {
	
	protected static final String EMPTY_STRING = ""; //$NON-NLS-1$
	private ImageDescriptor imgdesc_title = DebuggerPlugin.getDefault().createImageDescriptor(IDebugConstants.IMG_TAB_DEVICE);
	private Image img_title=null;

	private Text fTargetInfoText; 
	
	public TargetDeviceTab() {
		
	}

	public void createControl(Composite parent) {
		// TODO Auto-generated method stub
		Composite comp = new Composite(parent, SWT.NONE);
		setControl(comp);

		LaunchUIPlugin.getDefault().getWorkbench().getHelpSystem().setHelp(getControl(), ICDTLaunchHelpContextIds.LAUNCH_CONFIGURATION_DIALOG_MAIN_TAB);

		GridLayout topLayout = new GridLayout();
		topLayout.marginHeight = 10;
		comp.setLayout(topLayout);
		
		createTargetCommGroup(comp, 1);
		createVerticalSpacer(comp, 1);
		createTargetInfoGroup(comp, 1);
		
		LaunchUIPlugin.setDialogShell(parent.getShell());
	}
	
	protected void createTargetCommGroup(Composite parent, int colSpan) {

		Group mainGroup = new Group(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		mainGroup.setLayout(layout);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.horizontalSpan = colSpan;
		mainGroup.setLayoutData(gd);
		mainGroup.setText("Target Server");

		Text testText =  new Text(mainGroup, SWT.MULTI | SWT.READ_ONLY);
		testText.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		testText.setText("  ");
		
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
		updateTargetInfoFromConfig(configuration);
	}
	
	protected void updateTargetInfoFromConfig(ILaunchConfiguration config) {
		
	}

	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		// TODO Auto-generated method stub

	}
	
	public boolean isValid(ILaunchConfiguration config) {

		setErrorMessage(null);
		setMessage(null);
		
		return true;
	}

	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		// TODO Auto-generated method stub

	}

	protected void updateLaunchConfigurationDialog() {
		super.updateLaunchConfigurationDialog();
	}	
}
