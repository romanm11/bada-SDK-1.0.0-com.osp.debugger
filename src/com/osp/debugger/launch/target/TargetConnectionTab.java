package com.osp.debugger.launch.target;

import org.eclipse.cdt.debug.mi.core.IGDBServerMILaunchConfigurationConstants;
import org.eclipse.cdt.launch.internal.ui.LaunchUIPlugin;
import org.eclipse.cdt.launch.ui.CLaunchConfigurationTab;
import org.eclipse.cdt.launch.ui.ICDTLaunchHelpContextIds;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.osp.debugger.DebuggerPlugin;
import com.osp.debugger.IDebugConstants;
import com.osp.debugger.launch.IOspLaunchConfigurationConstants;

public class TargetConnectionTab extends CLaunchConfigurationTab {

	// Connection
	protected Text m_Port;
	
	
	protected static final String EMPTY_STRING = ""; //$NON-NLS-1$
	private ImageDescriptor imgdesc_title = DebuggerPlugin.getDefault().createImageDescriptor(IDebugConstants.IMG_TAB_CONNECT);
	private Image img_title=null;
	
	public TargetConnectionTab() {
	}
	
	public void createControl(Composite parent) {
		// TODO Auto-generated method stub
		Composite comp = new Composite(parent, SWT.NONE);
		setControl(comp);

		LaunchUIPlugin.getDefault().getWorkbench().getHelpSystem().setHelp(getControl(), ICDTLaunchHelpContextIds.LAUNCH_CONFIGURATION_DIALOG_MAIN_TAB);

		GridLayout topLayout = new GridLayout();
		topLayout.marginHeight = 10;
		comp.setLayout(topLayout);
		
		createConnectionGroup(comp, 1);
		createVerticalSpacer(comp, 1);
		
		LaunchUIPlugin.setDialogShell(parent.getShell());

	}

	protected void createConnectionGroup(Composite parent, int colSpan) {
		
		Composite mainComp = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		mainComp.setLayout(layout);
		GridData gd = new GridData();
		gd.horizontalSpan = colSpan;
        gd.widthHint = 230;
		mainComp.setLayoutData(gd);
		
		Label ctlLabel = new Label(mainComp, SWT.NONE);
        ctlLabel.setText("Target port:"); //$NON-NLS-1$
        ctlLabel.setLayoutData( new GridData());
        
        m_Port = new Text(mainComp, SWT.BORDER);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        m_Port.setLayoutData(gd);
        m_Port.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateLaunchConfigurationDialog();
			}}); 

        
	}
	
	
	public String getName() {
		// TODO Auto-generated method stub
		return "Connection";
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
		updateConnectonFromConfig(configuration);

	}

	protected void updateConnectonFromConfig(ILaunchConfiguration config) {	
		
		String value = IDebugConstants.TARGET_PORT_DEFAULT;
		try {
			value = config.getAttribute(IOspLaunchConfigurationConstants.ATTR_TARGET_PORT, IDebugConstants.TARGET_PORT_DEFAULT);
		} catch (CoreException ce) {
			LaunchUIPlugin.log(ce);
		}
		m_Port.setText(value);
	}


	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		// TODO Auto-generated method stub
		configuration.setAttribute(IOspLaunchConfigurationConstants.ATTR_TARGET_PORT, m_Port.getText());
		
		configuration.setAttribute(IGDBServerMILaunchConfigurationConstants.ATTR_REMOTE_TCP, false);
		configuration.setAttribute(IGDBServerMILaunchConfigurationConstants.ATTR_DEV_SPEED, IDebugConstants.COMM_BAUDRATE_DEFAULT);
	}

	public boolean isValid(ILaunchConfiguration config) {

		setErrorMessage(null);
		setMessage(null);

		// Check Seral Port
		String port = m_Port.getText().trim();
		if (port.length() == 0) {
			setErrorMessage("Port specified"); //$NON-NLS-1$
			return false;
		}
		
		return true;
	}

	
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		// TODO Auto-generated method stub
		configuration.setAttribute(IOspLaunchConfigurationConstants.ATTR_TARGET_PORT, IDebugConstants.TARGET_PORT_DEFAULT);

		configuration.setAttribute(IGDBServerMILaunchConfigurationConstants.ATTR_REMOTE_TCP, false);		
		configuration.setAttribute(IGDBServerMILaunchConfigurationConstants.ATTR_DEV_SPEED, IDebugConstants.COMM_BAUDRATE_DEFAULT);		
	}

	protected void updateLaunchConfigurationDialog() {
		super.updateLaunchConfigurationDialog();
	}	
}
