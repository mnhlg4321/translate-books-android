package com.ml.tblandroidtxt;
public final class SettingsNavigationPolicy {
    public static boolean shouldSwitch(String current,String requested){return requested!=null&&!requested.trim().isEmpty()&&!requested.equals(current);}
    public static boolean shouldStackFields(int widthDp, float fontScale) {
        return widthDp < 360 || fontScale >= 1.30f;
    }
    private SettingsNavigationPolicy(){}
}
