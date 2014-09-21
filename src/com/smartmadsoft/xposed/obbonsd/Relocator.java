package com.smartmadsoft.xposed.obbonsd;

import java.io.File;

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
		
		realInternal = Environment.getExternalStorageDirectory().getPath();
		realExternal = System.getenv("SECONDARY_STORAGE").split(":")[0];
		
		namespace = lpparam.packageName;
		File obbDir = new File(realExternal + "/Android/obb/" + namespace + "/");
		
		if (obbDir.isDirectory()) {		
			File files[] = obbDir.listFiles();
			boolean obbFound = false;
			
			for (File file : files) {
				if (file.getName().endsWith(namespace + ".obb")) {
					obbFound = true;
					break;
				}
			}
			
			if (!obbFound)
				return;
		} else
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
		
	}
	
	void log(String text) {
		if (DEBUG) {
			XposedBridge.log("[" + TAG + "] " + text);
			Log.d(TAG, text);
		}
	}

}
