package com.qfix.tool;

import android.app.Application;
import android.util.Log;

public class HotPatchTool {
	private static final String TAG = "HotPatchTool";
	
    public static final int CODE_RESOLVE_PATCH_ALL_SUCCESS = 0;
    public static final int CODE_JAVA_PARAMETER_ERROR = 1;
    public static final int CODE_NATIVE_INIT_PARAMETER_ERROR = 2;
    public static final int CODE_LOAD_DALVIK_SO_ERROR = 3;
    public static final int CODE_FIND_LOADED_CLASS_ERROR = 4;
    public static final int CODE_REFERRER_CLASS_OBJECT_ERROR = 5;
    public static final int CODE_RESOLVE_CLASS_ERROR = 6;
    public static final int CODE_NATIVE_ITEM_PARAMETER_ERROR = 7;
    public static final int CODE_PATCH_CLASS_OBJECT_ERROR = 8;
    public static final int NUM_FACTOR_PATCH = 10;

    public static native int nativeResolvePatchClass(String[] referrerClassList, long[] classIdxList, int size);
    public static boolean sIsLibLoaded = false;
    
    public static boolean loadPatchToolLib() {
		boolean result = false;
		try {
			System.loadLibrary("ResolvePatch");
			result = true;
		} catch (Throwable e) {
			result = false;
			Log.d(TAG, "loadPatchToolLib exception=" + e);
		}
		return result;
	}
    
    public static void resolvePatchClass(Application app, String[] referrerClassList, long[] classIdxList, int size) {
    	if (!HotPatchTool.sIsLibLoaded) {
			HotPatchTool.sIsLibLoaded = loadPatchToolLib();
		}
    	if (!HotPatchTool.sIsLibLoaded) {
			boolean unloadResult = InjectUtil.unloadDexElement(app, 0);
			Log.d(TAG, "load lib failed, unload patch result=" + unloadResult);
		} else {
			int resolveResult = nativeResolvePatchClass(referrerClassList, classIdxList, size);
			if (resolveResult != CODE_RESOLVE_PATCH_ALL_SUCCESS) {
				boolean unloadResult = InjectUtil.unloadDexElement(app, 0);
				Log.d(TAG, "resolve patch class failed, unload patch result=" + unloadResult);
			} else {
				Log.d(TAG, "resolve patch class success");
			}
		}
    }
}