package com.qfix.seconddex;

public class BugObjectB {
	
	private static final String TAG = "B";
	
	public BugObjectB() {
	}
	
	public String fun() {
		return TAG + " 未加载";
		//return TAG + " 已加载";
	}
}