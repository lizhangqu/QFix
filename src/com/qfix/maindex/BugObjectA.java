package com.qfix.maindex;

public class BugObjectA {
	
	private static final String TAG = "A";
	
	public BugObjectA() {
	}
	
	public String fun() {
		return TAG + " 未加载";
		//return TAG + " 已加载";
	}
}