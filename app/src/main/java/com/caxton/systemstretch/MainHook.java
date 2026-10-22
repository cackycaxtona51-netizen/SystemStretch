package com.caxton.systemstretch;

import android.graphics.Rect;
import java.lang.reflect.Method;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainHook implements IXposedHookLoadPackage {

    // SM-A135F Native Hardware Panel Size
    private static final int PHYS_W = 1080;
    private static final int PHYS_H = 2408;

    private static String getProp(String key, String def) {
        try {
            Class<?> sp = Class.forName("android.os.SystemProperties");
            Method m = sp.getMethod("get", String.class, String.class);
            String val = (String) m.invoke(null, key, def);
            if (val != null && !val.isEmpty()) return val;
        } catch (Throwable ignored) {}
        return def;
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!"android".equals(lpparam.packageName)) return;

        XC_MethodHook projectionHook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                String mode = getProp("debug.sys.stretch", "off");
                if ("off".equals(mode)) return;

                int orientation = 0;
                Rect layerStackRect = null;
                Rect displayRect = null;

                for (Object arg : param.args) {
                    if (arg instanceof Integer) {
                        orientation = (Integer) arg;
                    } else if (arg instanceof Rect) {
                        if (layerStackRect == null) {
                            layerStackRect = (Rect) arg;
                        } else if (displayRect == null) {
                            displayRect = (Rect) arg;
                        }
                    }
                }

                // Override the physical destination rectangle so SurfaceFlinger stretches full-screen without black bars
                if (displayRect != null) {
                    if (orientation == 1 || orientation == 3) {
                        // Landscape (90 or 270 deg)
                        displayRect.set(0, 0, PHYS_H, PHYS_W);
                    } else {
                        // Portrait (0 or 180 deg)
                        displayRect.set(0, 0, PHYS_W, PHYS_H);
                    }
                }
            }
        };

        try {
            Class<?> txClass = XposedHelpers.findClass("android.view.SurfaceControl$Transaction", lpparam.classLoader);
            XposedBridge.hookAllMethods(txClass, "setDisplayProjection", projectionHook);
        } catch (Throwable ignored) {}

        try {
            Class<?> scClass = XposedHelpers.findClass("android.view.SurfaceControl", lpparam.classLoader);
            XposedBridge.hookAllMethods(scClass, "setDisplayProjection", projectionHook);
        } catch (Throwable ignored) {}

        try {
            Class<?> ddClass = XposedHelpers.findClass("com.android.server.display.DisplayDevice", lpparam.classLoader);
            XposedBridge.hookAllMethods(ddClass, "setProjectionLocked", projectionHook);
        } catch (Throwable ignored) {}
    }
}
