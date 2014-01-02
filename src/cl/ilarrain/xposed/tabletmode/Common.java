package cl.ilarrain.xposed.tabletmode;

public class Common {

    public static final String TAG = "TabletMode";
    public static final String MY_PACKAGE_NAME = Common.class.getPackage().getName();

    public static final String ACTION_PERMISSIONS = "update_permissions";


    public static final String PREFS = "ModSettings";

    public static final String PREF_DEFAULT = "default";

    public static final String PREF_ACTIVE = "/active";
    public static final String PREF_PHYSDPI = "/physdpi";
}
