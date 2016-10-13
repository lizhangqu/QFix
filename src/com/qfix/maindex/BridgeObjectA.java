package com.qfix.maindex;

public class BridgeObjectA {
    public String fun() {
        BugObjectA obj = new BugObjectA();
        return obj.fun();
    }
}