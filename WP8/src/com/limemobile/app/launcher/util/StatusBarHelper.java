
package com.limemobile.app.launcher.util;

import android.content.Context;

import java.lang.reflect.Method;

public final class StatusBarHelper {
    public static void expand(Context context) {
        try {
            Object service = context.getSystemService("statusbar");
            Class<?> statusBarManager = Class
                    .forName("android.app.StatusBarManager");
            Method expand = statusBarManager.getMethod("expand");
            expand.invoke(service);
        } catch (Exception e) {
        }
    }

    public static void contractStatusBar(Context context) {
        try {
            Object service = context.getSystemService("statusbar");
            Class<?> statusBarManager = Class
                    .forName("android.app.StatusBarManager");
            Method contract = statusBarManager.getMethod("contract");
            contract.invoke(service);
        } catch (Exception e) {
        }
    }
}
