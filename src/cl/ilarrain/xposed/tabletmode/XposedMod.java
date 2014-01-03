package cl.ilarrain.xposed.tabletmode;


import android.app.AndroidAppHelper;
import android.os.Build;
import android.view.Display;

import de.robv.android.xposed.IXposedHookCmdInit;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setFloatField;

public class XposedMod implements IXposedHookZygoteInit, IXposedHookLoadPackage, IXposedHookCmdInit {

    public static final String this_package = XposedMod.class.getPackage().getName();

    public static XSharedPreferences prefs;

    @Override
    public void initZygote(IXposedHookZygoteInit.StartupParam startupParam) throws Throwable {
        loadPrefs();

        // Hook to override Physical DPI (globally, including resource load + rendering)
        try {
            if (Build.VERSION.SDK_INT < 17) {
                findAndHookMethod(Display.class, "init", int.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        String packageName = AndroidAppHelper.currentPackageName();

                        if (!isActive(packageName)) {
                            // No overrides for this package
                            return;
                        }

                        float packagePhysDPI = prefs.getFloat(packageName + Common.PREF_PHYSDPI,
                                prefs.getFloat(Common.PREF_DEFAULT + Common.PREF_PHYSDPI, 0));
                        if (packagePhysDPI > 0) {
                            // Density for this package is overridden, change density
                            setFloatField(param.thisObject, "mDpiX", packagePhysDPI);
                            setFloatField(param.thisObject, "mDpiY", packagePhysDPI);
                        }
                    }
                });
            } else {
                findAndHookMethod(Display.class, "updateDisplayInfoLocked", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        String packageName = AndroidAppHelper.currentPackageName();

                        if (!isActive(packageName)) {
                            // No overrides for this package
                            return;
                        }

                        float packagePhysDPI = prefs.getFloat(packageName + Common.PREF_PHYSDPI,
                                prefs.getFloat(Common.PREF_DEFAULT + Common.PREF_PHYSDPI, 0));
                        if (packagePhysDPI > 0) {
                            // Density for this package is overridden, change density
                            Object mDisplayInfo = getObjectField(param.thisObject, "mDisplayInfo");
                            setFloatField(mDisplayInfo, "physicalXDpi", packagePhysDPI);
                            setFloatField(mDisplayInfo, "physicalYDpi", packagePhysDPI);
                        }
                    }

                    ;
                });
            }
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        prefs.reload();

        if (this_package.equals(lpparam.packageName)) {
            findAndHookMethod("cl.ilarrain.xposed.XposedModActivity",
                    lpparam.classLoader, "isModActive", XC_MethodReplacement.returnConstant(true));
        }
    }


    @Override
    public void initCmdApp(de.robv.android.xposed.IXposedHookCmdInit.StartupParam startupParam) throws Throwable {
        loadPrefs();
    }


    public static void loadPrefs() {
        prefs = new XSharedPreferences(Common.MY_PACKAGE_NAME, Common.PREFS);
        prefs.makeWorldReadable();
    }

    public static boolean isActive(String packageName) {
        return prefs.getBoolean(packageName + Common.PREF_ACTIVE, false);
    }

    public static boolean isActive(String packageName, String sub) {
        return prefs.getBoolean(packageName + Common.PREF_ACTIVE, false) && prefs.getBoolean(packageName + sub, false);
    }
}
