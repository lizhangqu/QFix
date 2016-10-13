package com.qfix.seconddex;

public class BridgeObjectB {
    public String fun() {
        BugObjectB obj = new BugObjectB();
        return obj.fun();
    }
}