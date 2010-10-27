package com.osp.debugger.launch.target;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.eclipse.cdt.debug.core.ICDTLaunchConfigurationConstants;
import org.eclipse.cdt.debug.mi.core.IMILaunchConfigurationConstants;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchesListener2;
import org.eclipse.debug.internal.ui.views.console.ProcessConsole;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IOConsoleInputStream;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITypedRegion;

import com.osp.debugger.IDebugConstants;
import com.osp.ide.message.socket.NetManager;



public class TargetDebugLaunchListner implements ILaunchesListener2 {

	private ILaunch currLaunch = null;
	
	public TargetDebugLaunchListner(ILaunch launch)
	{
		currLaunch = launch;
		
		DebugPlugin.getDefault().getLaunchManager().addLaunchListener(this);
	}
	

	@Override
	public void launchesTerminated(ILaunch[] launches) {
		// TODO Auto-generated method stub
		if( currLaunch == null ) return;
		
		for( int i = 0; i < launches.length; i++ )
		{
			if( launches[i].equals(currLaunch))
			{
				ILaunch launch = currLaunch;
				currLaunch = null;
				DebugPlugin.getDefault().getLaunchManager().removeLaunchListener(this);				

//				launches[i].setAttribute(IOspLaunchConfigurationConstants.ATTR_BADA_LAUNCH_TYPE, "");

				String message = "Please wait while the gdbserver terminates. During this time do not run or debug the application on the device.";
				displayOutputMessage(message);
				displayConsoleMessage(launch, message);
			}
		}
		
	}

	@Override
	public void launchesAdded(ILaunch[] launches) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void launchesChanged(ILaunch[] launches) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void launchesRemoved(ILaunch[] launches) {
		// TODO Auto-generated method stub
		
	}

	private void displayOutputMessage(String message)
	{
		// Display Net manager
		Date now = new Date();
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd,HH:mm:ss.SSS");
		System.out.println(format.format(now)); // 2010-06-23,10:20:11.296
		
		NetManager.getInstance().addMessage("2,0,0,1," + "2010-06-23,10:20:11.296" + ",Info, " + message);
	}
	
	private void displayConsoleMessage(ILaunch launch, String message)
	{
		
		// find launches console (Broker, gdb, program)
		String name = launch.getLaunchConfiguration().getName();
		ProcessConsole[] consoles = findConsole(name);
		
		if(consoles ==null || consoles.length == 0 ) return;
		
		for( int i = 0; i < consoles.length; i++ )
		{
			writeToConsole(consoles[i], message);
		}
		
		/*
		ProcessConsole displayConsole = null;
		// find gdb console

		String gdb_name="";
		try {
			gdb_name = launch.getLaunchConfiguration().getAttribute(IMILaunchConfigurationConstants.ATTR_DEBUG_NAME, IDebugConstants.DEBUGGER_TARGET_DEBUG_NAME_DEFAULT);
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			gdb_name = IDebugConstants.DEBUGGER_TARGET_DEBUG_NAME_DEFAULT;
		}
		for( int i = 0; i < consoles.length; i++ )
		{
			if( consoles[i].getName().contains(gdb_name))
			{
				displayConsole = consoles[i];
				break;
			}
		}
		
		if( displayConsole != null )
		{
			displayConsoleMessage(displayConsole, message);
			return;
		}
		
		// find exe console
		String exe_name="";
		try {
			exe_name = launch.getLaunchConfiguration().getAttribute(ICDTLaunchConfigurationConstants.ATTR_PROGRAM_NAME, "");
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			exe_name = "";
		}
		
		if( exe_name != null && exe_name.length() > 0 )
		{
			exe_name = exe_name.replaceAll("/", "\\\\");
			
			for( int i = 0; i < consoles.length; i++ )
			{
				if( consoles[i].getName().contains(exe_name))
				{
					displayConsole = consoles[i];
					break;
				}
			}
			
			if( displayConsole != null )
			{
				displayConsoleMessage(displayConsole, message);
				return;
			}			
			
		}
	*/
		
	}
	
	private void writeToConsole(ProcessConsole console, String msg)
	{
		IOConsoleOutputStream stream = console.newOutputStream();
		if( stream != null )
		{
			Color newColor = new Color(Display.getDefault(), 255, 0, 0);
			stream.setColor(newColor);
			try {
				stream.write("\n" + msg);
				stream.flush();			
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				stream.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}
	
	private ProcessConsole[] findConsole(String launchName ) {
		
		String tName = "> " + launchName;
		List<ProcessConsole> consoleList = new ArrayList<ProcessConsole>();
		
		  ConsolePlugin plugin = ConsolePlugin.getDefault();
	      IConsoleManager conMan = plugin.getConsoleManager();
	      IConsole[] existing = conMan.getConsoles();
	      for (int i = 0; i < existing.length; i++) {
	    	  if (existing[i].getName().startsWith(launchName) || existing[i].getName().contains(tName))
	    	  {
	    		  if( existing[i] instanceof ProcessConsole)
	    			  consoleList.add((ProcessConsole)existing[i]);
	    	  }
	      }
	      
	      return (ProcessConsole[])consoleList.toArray( new ProcessConsole[consoleList.size()] );
	}		
}

