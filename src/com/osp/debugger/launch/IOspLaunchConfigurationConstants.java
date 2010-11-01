package com.osp.debugger.launch;


public class IOspLaunchConfigurationConstants {
	public static final String OSP_LAUNCH_ID = "com.osp.launch";
	
	public static final String ATTR_CODE_BINARY_SDK_PATH = OSP_LAUNCH_ID + ".CODE_BINARY_SDK_PATH";
	public static final String ATTR_RESOURCE_BINARY_PATH = OSP_LAUNCH_ID + ".RESOURCE_BINARY_PATH";
	
	public static final String ATTR_SERIAL_PORT = OSP_LAUNCH_ID + ".SERIAL_PORT";
	public static final String ATTR_SERIAL_BAUDRATE = OSP_LAUNCH_ID + ".SERIAL_BAUDRATE";
	public static final String ATTR_SERIAL_DATABITS = OSP_LAUNCH_ID + ".SERIAL_DATABITS";
	public static final String ATTR_SERIAL_STOPBITS = OSP_LAUNCH_ID + ".SERIAL_STOPBITS";
	public static final String ATTR_SERIAL_PARITY = OSP_LAUNCH_ID + ".SERIAL_PARITY";
	public static final String ATTR_SERIAL_FLOWCTRL = OSP_LAUNCH_ID + ".SERIAL_FLOWCTRL";
	
	public static final String ATTR_TARGET_PORT = OSP_LAUNCH_ID + ".TARGET_PORT";
	
	public static final String ATTR_DEVICE_INFOFILE = OSP_LAUNCH_ID + ".DEVICE_INFO_FILE";
	public static final String ATTR_DEVICE_CMDLINE_OPT = OSP_LAUNCH_ID + ".DEVICE_CMDLINE_OPT";
	
	public static final String ATTR_BADA_LAUNCH_TYPE = OSP_LAUNCH_ID + ".BADA_LAUNCH_TYPE";
	
	public static final String ATTR_UPDATE_RESOURCE = OSP_LAUNCH_ID + ".UPDATE_RES";
	public static final String ATTR_UPDATE_BINARY = OSP_LAUNCH_ID + ".UPDATE_BINARY";
	
	public static final String BADA_LAUNCH_SIMULATOR = "Simulator";
	public static final String BADA_LAUNCH_TARGET = "Target";
	
	// Default Value
	//public static final String BADA_SIMULATOR_ROOT="${SDKROOT}\\ShpRsrc\\FS\\Win32FS\\S8000_Generic_FS\\User\\Osp\\Applications\\${APPLICATION_ID}";
	//public static final String BADA_SIMULATOR_ROOT="${SDKROOT}\\Rsrc\\FS\\User\\Osp\\Applications\\${APPLICATION_ID}";
	//public static final String BADA_SIMULATOR_ROOT="${SDKROOT}\\Rsrc\\bada_${SCREEN_DIR}\\Win32FS\\Osp\\Applications\\${APPLICATION_ID}";
	public static final String BADA_SIMULATOR_ROOT="${SDKROOT}" + java.io.File.separatorChar + "Model" + java.io.File.separatorChar + "${MODEL_NAME}" + java.io.File.separatorChar + "Simulator" + java.io.File.separatorChar + "FS" + java.io.File.separatorChar + "Win32FS" + java.io.File.separatorChar + "Osp" + java.io.File.separatorChar + "Applications" + java.io.File.separatorChar + "${APPLICATION_ID}";
	public static final String DEFAULT_SUMUAL_CODE_BINARY_PATH = BADA_SIMULATOR_ROOT + java.io.File.separatorChar + "Bin";
	//public static final String DEFAULT_SIMUL_ARG="-s ${SDKROOT}\\lib\\WinSgpp\\PhoneShell.dll -d S8000_Generic.dbi -i ${APPLICATION_ID} -l ${PROJECT_NAME}";
	//public static final String DEFAULT_SIMUL_ARG="-s \"${SDKROOT}\\lib\\WinSgpp\\PhoneShell.dll\" -d S8000_Generic.dbi";
	public static final String DEFAULT_SIMUL_ARG="-s \"${SIMULATOR_LIB_PATH}" + java.io.File.separatorChar + "PhoneShell.dll\" -d Generic.dbi";
	public static final boolean DEFAULT_STOP_AT_MAIN=false;
	
	
	public static final String BADA_HOST_TARGET_ROOT="${SDKROOT}" + java.io.File.separatorChar + "Temp" + java.io.File.separatorChar + "${APPLICATION_ID}";
	//public static final String DEFAULT_TARGET_CODE_BINARY_PATH = "/Osp/Applications";
	public static final boolean DEFAULT_UPDATE_RESOURCE = true;
	public static final boolean DEFAULT_UPDATE_BINARY = true;
}
