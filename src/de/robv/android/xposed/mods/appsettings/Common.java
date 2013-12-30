package de.robv.android.xposed.mods.appsettings;

public class Common {

	public static final String TAG = "AppSettings";
	public static final String MY_PACKAGE_NAME = Common.class.getPackage().getName();

	public static final String PREFS = "ModSettings";

	public static final String PREF_DEFAULT = "default";
	
	public static final String PREF_ACTIVE = "/active";
	public static final String PREF_PHYSDPI = "/physdpi";

	public static final int[] swdp = { 0, 320, 480, 600, 800, 1000 };
	public static final int[] wdp = { 0, 320, 480, 600, 800, 1000 };
	public static final int[] hdp = { 0, 480, 854, 1024, 1280, 1600 };
}
