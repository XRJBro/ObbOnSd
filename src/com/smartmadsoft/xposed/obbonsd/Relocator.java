package com.smartmadsoft.xposed.obbonsd;

import java.io.File;
import java.util.ArrayList;

import android.os.Environment;
import android.util.Log;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class Relocator implements IXposedHookLoadPackage {

	public static boolean DEBUG = false;
	public static final String TAG = "ObbOnSd";
	
	String namespace;
	String realInternal;
	String realExternal;
	
	@Override
	public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
		
		if (lpparam.packageName.equals("android"))
			return;		
		
		if (isExcludedPackage(lpparam.packageName))
			return;
		
		realInternal = Environment.getExternalStorageDirectory().getPath();
		realExternal = System.getenv("SECONDARY_STORAGE").split(":")[0];
		
		namespace = lpparam.packageName;
		File obbDir = new File(realExternal + "/Android/obb/" + namespace + "/");
		File dataDir = new File(realExternal + "/Android/data/" + namespace + "/");

		boolean obbFound = false;
		boolean dataFound = false;
		
		log(namespace + ": before data dir check");
		if (dataDir.isDirectory()) {
			if (containsFile(new File(dataDir.getPath()))) 
				dataFound = true;
		}
		
		log(namespace + ": before obb dir check");		
		if (obbDir.isDirectory()) {		
			File files[] = obbDir.listFiles();			
			
			for (File file : files) {
				if (file.getName().endsWith(namespace + ".obb")) {
					obbFound = true;
					break;
				}
			}
		}
		
		//log(namespace + ": obbFound=" + Boolean.toString(obbFound));
		//log(namespace + ": dataFound=" + Boolean.toString(dataFound));
		
		if (!obbFound && !dataFound)
			return;
		
		log(namespace + " hooked");
		
		XposedBridge.hookAllMethods(XposedHelpers.findClass("android.os.Environment", lpparam.classLoader), "getExternalStorageDirectory", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				//String path = param.getResult().toString();
				//log("getExternalStorageDirectory: " + path);
				//log("getExternalStorageDirectory (new): " + realExternal);
				param.setResult(new File(realExternal));
			}
	    });
		
		
		XposedBridge.hookAllMethods(XposedHelpers.findClass("android.app.ContextImpl", lpparam.classLoader), "getObbDir", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				File dir = (File) param.getResult();
				//log("getObbDir: " + dir.getPath());
				String path = dir.getPath().replaceFirst("^" + realInternal, realExternal);
				log("getObbDir (new): " + path);
				param.setResult(new File(realExternal));				
			}
	    });
		
		XposedBridge.hookAllMethods(XposedHelpers.findClass("android.app.ContextImpl", lpparam.classLoader), "getExternalFilesDir", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				File file = (File) param.getResult();
				//log("getExternalFilesDir: " + file.getPath());
				String path = file.getPath().replaceFirst("^" + realInternal, realExternal);
				param.setResult(new File(path));
			}
	    });
		
		/*
		XposedHelpers.findAndHookConstructor("java.io.File", lpparam.classLoader, String.class, new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				//log("File (String)");
				if (param.args[0].toString().startsWith(realInternal)) {
					log("File (String): " + param.args[0].toString());
					param.args[0] = param.args[0].toString().replaceFirst("^" + realInternal, realExternal);
				}
			}			
		});
		
		XposedHelpers.findAndHookConstructor("java.io.File", lpparam.classLoader, String.class, String.class, new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				//log("File (String, String)");
				if (param.args[0].toString().startsWith(realInternal)) {
					log("File (String): " + param.args[0].toString());
					param.args[0] = param.args[0].toString().replaceFirst("^" + realInternal, realExternal);
				}
			}			
		});
		*/
		
	}
	
	void log(String text) {
		if (DEBUG) {
			XposedBridge.log("[" + TAG + "] " + text);
			Log.d(TAG, text);
		}
	}
	
	boolean containsFile(File directory) {
		//log("dir: " + directory.getPath());
		for (File file : directory.listFiles()) {
			//log("file: " + file.getPath());
	        if (file.isFile())
	        	return true;
	        else if (containsFile(file))
	        		return true;
		}
		return false;
	}
	
	boolean isExcludedPackage(String namespace) {
		ArrayList<String> excluded = new ArrayList<String>();
		// these apps natively support extSdCard
		excluded.add("cz.seznam.mapy");
		excluded.add("com.skobbler.forevermapng");
		
		if (excluded.contains(namespace))
			return true;
		
		return false;
	}

}
