package com.qfix.maindex;

import com.qfix.tool.HotPatchTool;
import com.qfix.tool.InjectUtil;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

public class MainApplication extends Application {
	
	public static final String SP_PATCH = "sp_patch";
	public static final String SP_KEY_PATCH_A = "sp_key_patch_a";
	public static final String SP_KEY_PATCH_B = "sp_key_patch_b";
	
	public static final String PATCH_DEX_NAME_A = "patchA.jar";
	public static final String PATCH_DEX_NAME_B = "patchB.jar";
	
	@Override
    protected void attachBaseContext(Context base) {
    	super.attachBaseContext(base);
    	Log.d("QFixDemo", "MainApplication attachBaseContext");
    	
    	String dexName = "classes2.jar";
    	boolean result = InjectUtil.inject(this, dexName, "Foo2", true);
    	Log.d("QFixDemo", "MainApplication inject second dex result=" + result);
    	
    	SharedPreferences sp = getSharedPreferences(SP_PATCH, Context.MODE_PRIVATE);
    	
    	boolean doPatchA = sp.getBoolean(SP_KEY_PATCH_A, false);
    	Log.d("QFixDemo", "MainApplication doPatchA=" + doPatchA);
    	if (doPatchA) {
        	result = InjectUtil.inject(this, PATCH_DEX_NAME_A, "", false);
        	Log.d("QFixDemo", "MainApplication inject patchA dex result=" + result);
			if (result) {
				if (Build.VERSION.SDK_INT < 21) {
					String[] referrerList = new String[] {"Lcom/qfix/maindex/MainApplication;"};
					long[] classIdList = new long[] {23};
					HotPatchTool.resolvePatchClass(this, referrerList, classIdList, 1);
				}
			}
    	}
    	
    	boolean doPatchB = sp.getBoolean(SP_KEY_PATCH_B, false);
    	Log.d("QFixDemo", "MainApplication doPatchB=" + doPatchB);
    	if (doPatchB) {
        	result = InjectUtil.inject(this, PATCH_DEX_NAME_B, "", false);
        	Log.d("QFixDemo", "MainApplication inject patchB dex result=" + result);
        	if (result) {
				if (Build.VERSION.SDK_INT < 21) {
					String[] referrerList = new String[] {"LFoo2;"};
					long[] classIdList = new long[] {2};
					HotPatchTool.resolvePatchClass(this, referrerList, classIdList, 1);
				}
			}
    	}
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		Log.d("QFixDemo", "MainApplication onCreate");
	}
	
	
	
}