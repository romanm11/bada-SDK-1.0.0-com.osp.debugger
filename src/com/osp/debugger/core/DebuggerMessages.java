package com.osp.debugger.core;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class DebuggerMessages {
	private static final String BUNDLE_NAME = "com.osp.debugger.core.DebuggerMessages";//$NON-NLS-1$

	private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle( BUNDLE_NAME );

	private DebuggerMessages() {
	}

	public static String getString( String key ) {
		try {
			return RESOURCE_BUNDLE.getString( key );
		}
		catch( MissingResourceException e ) {
			return '!' + key + '!';
		}
	}
}
