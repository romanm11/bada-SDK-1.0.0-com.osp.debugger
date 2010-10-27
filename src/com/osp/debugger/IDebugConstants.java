package com.osp.debugger;

import com.osp.ide.IConstants;


public class IDebugConstants {

	public static final String IMG_TAB_CONNECT="icons/obj16/connect.gif";
	public static final String IMG_TAB_DEVICE="icons/obj16/device.gif";
	

	public static final String COMM_PORT[]={"COM1", "COM2", "COM3", "COM4"};
	public static final String COMM_PORT_DEFAULT="COM1";
	public static final String FILE_SEP_BSLASH="\\";
	
	public static final String TARGET_PORT_DEFAULT="6200";
	
	public static final String COMM_BAUDRATE[]={"300", "1200", "2400", "4800", "9600", "19200", "38400", "57600", "115200"};
	public static final String COMM_BAUDRATE_DEFAULT="115200";
	
	public static final String COMM_DATABITS[]={"5", "6", "7", "8"};
	public static final String COMM_DATABITS_DEFAULT="8";
	
	public static final String COMM_STOPBITS[]={"1", "1.5", "2"};
	public static final String COMM_STOPBITS_DEFAULT="1";	
	
	public static final String COMM_PARITY[]={"None", "Even", "Odd", "Mark", "Space"};
	public static final String COMM_PARITY_DEFAULT="None";	
	
	public static final String COMM_FLOWCTRL[]={"None", "RTS/CTS", "Xon/Xoff"};
	public static final String COMM_FLOWCTRL_DEFAULT="None";
	
	
	public static final String ID_LAUNCH_OSP_SIMUAL_APP = "com.osp.debugger.SumulatorLaunch"; //$NON-NLS-1$
	public static final String ID_LAUNCH_OSP_SIMUAL_DEBUGGER = "com.osp.debugger.SumulatorDebugger";
	
	public static final String ID_LAUNCH_OSP_TARGET_APP = "com.osp.debugger.TargetLaunch"; //$NON-NLS-1$
	public static final String ID_LAUNCH_OSP_TARGET_DEBUGGER = "com.osp.debugger.TargetDebugger";
	
	public static final String DEBUGGER_SIMUL_DEBUG_NAME_DEFAULT ="${SDKROOT}" + IConstants.PATH_TOOLS + "\\Toolchains\\Win32\\bin\\i686-mingw32-gdb";
	public static final String DEBUGGER_TARGET_DEBUG_NAME_DEFAULT="${SDKROOT}" + IConstants.PATH_TOOLS + "\\Toolchains\\ARM\\bin\\gdb_arm";
	
	
	public static final boolean DEBUGGER_VERBOSE_MODE_DEFAULT = false;
	
	public static final int	DIAG_KIND_PROCESS 	= 1400;
	public static final int	DIAG_KIND_AGENT 	= 1600;
	
	public static final int	DIAG_PROCESS_EXECUTE = DIAG_KIND_PROCESS + 0;
	public static final int	DIAG_AGENT_TKSHELL = DIAG_KIND_AGENT + 1;
	
	public static final String SIMULATOR_NAME="Simulator.exe";
	
	// for Test
	public static final boolean ATTACH_MODE = false;	
}
