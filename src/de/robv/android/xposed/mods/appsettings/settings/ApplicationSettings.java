package de.robv.android.xposed.mods.appsettings.settings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import de.robv.android.xposed.mods.appsettings.Common;
import de.robv.android.xposed.mods.appsettings.R;

@SuppressLint("WorldReadableFiles")
public class ApplicationSettings extends Activity {

	private boolean dirty = false;


	Switch swtActive;

    private String pkgName;
    SharedPreferences prefs;
    private Set<String> disabledPermissions;
    private boolean allowRevoking;
    private Intent parentIntent;

    LocaleList localeList;
	int selectedLocalePos;
    
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	
        super.onCreate(savedInstanceState);
        
        swtActive = new Switch(this);
        getActionBar().setCustomView(swtActive);
        getActionBar().setDisplayShowCustomEnabled(true);

        setContentView(R.layout.app_settings);
        
        Intent i = getIntent();
        parentIntent = i;

        prefs = getSharedPreferences(Common.PREFS, Context.MODE_WORLD_READABLE);        
        
        ApplicationInfo app;
        try {
            app = getPackageManager().getApplicationInfo(i.getStringExtra("package"), 0);
            pkgName = app.packageName;
        } catch (NameNotFoundException e) {
			// Close the dialog gracefully, package might have been uninstalled
			finish();
			return;
        }
        
        // Display app info
        ((TextView) findViewById(R.id.app_label)).setText(app.loadLabel(getPackageManager()));
        ((TextView) findViewById(R.id.package_name)).setText(app.packageName);
        ((ImageView) findViewById(R.id.app_icon)).setImageDrawable(app.loadIcon(getPackageManager()));
        
        // Update switch of active/inactive tweaks
        if (prefs.getBoolean(pkgName + Common.PREF_ACTIVE, false)) {
            swtActive.setChecked(true);
            findViewById(R.id.viewTweaks).setVisibility(View.VISIBLE);
        } else {
            swtActive.setChecked(false);
            findViewById(R.id.viewTweaks).setVisibility(View.GONE);
        }
        // Toggle the visibility of the lower panel when changed
        swtActive.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            	dirty = true;
                findViewById(R.id.viewTweaks).setVisibility(isChecked ? View.VISIBLE : View.GONE);
            }
        });
        
        // Update Phys DPI field
        if (prefs.getBoolean(pkgName + Common.PREF_ACTIVE, false)) {
        	((EditText) findViewById(R.id.txtPhysDPI)).setText(String.valueOf(
        		prefs.getFloat(pkgName + Common.PREF_PHYSDPI, 0)));
        } else {        
        	((EditText) findViewById(R.id.txtPhysDPI)).setText("0");
        }
        // Track changes to the Phys DPI field to know if the settings were changed
        ((EditText) findViewById(R.id.txtPhysDPI)).addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
                dirty = true;
			}
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}
			@Override
			public void afterTextChanged(Editable s) {
			}
		});

		// Helper to list all apk folders under /res
		((Button) findViewById(R.id.btnListRes)).setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
		        AlertDialog.Builder builder = new AlertDialog.Builder(ApplicationSettings.this);
		        
		        ScrollView scrollPane = new ScrollView(ApplicationSettings.this);
		        TextView txtPane = new TextView(ApplicationSettings.this);
		        StringBuilder contents = new StringBuilder();
		        JarFile jar = null;
		        TreeSet<String> resEntries = new TreeSet<String>();
		        Matcher m = Pattern.compile("res/(.+)/[^/]+").matcher("");
		        try {
		            ApplicationInfo app = getPackageManager().getApplicationInfo(pkgName, 0);
		            jar = new JarFile(app.publicSourceDir);
		            Enumeration<JarEntry> entries = jar.entries();
		            while (entries.hasMoreElements()) {
		            	JarEntry entry = entries.nextElement();
		            	m.reset(entry.getName());
		            	if (m.matches())
		            		resEntries.add(m.group(1));
		            }
		            if (resEntries.size() == 0)
		            	resEntries.add(getString(R.string.res_noentries));
		            jar.close();
			        for (String dir : resEntries) {
			        	contents.append('\n');
			        	contents.append(dir);
			        }
			        contents.deleteCharAt(0);
		        } catch (Exception e) {
		            contents.append(getString(R.string.res_failedtoload));
		            if (jar != null) {
			            try {
			            	jar.close();
			            } catch (Exception ex) { }
		            }
		        }
		        txtPane.setText(contents);
		        scrollPane.addView(txtPane);
		        builder.setView(scrollPane);
		        builder.setTitle(R.string.res_title);
		        builder.show();
			}
		});
		

	}


    @Override
    public void onBackPressed() {
    	// If form wasn't changed, exit without prompting
    	if (!dirty) {
    		finish();
    		return;
    	}
    	
    	// Require confirmation to exit the screen and lose configuration changes
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.settings_unsaved_title);
        builder.setIconAttribute(android.R.attr.alertDialogIcon);
        builder.setMessage(R.string.settings_unsaved_detail);
        builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ApplicationSettings.this.finish();
            }
        });
        builder.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        builder.show();
    }    
	
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        setResult(RESULT_OK, parentIntent);
    }
    

    
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_app, menu);
        updateMenuEntries(getApplicationContext(), menu, pkgName);
        return true;
    }

	public static void updateMenuEntries(Context context, Menu menu, String pkgName) {
		if (context.getPackageManager().getLaunchIntentForPackage(pkgName) == null) {
			menu.findItem(R.id.menu_app_launch).setEnabled(false);
			Drawable icon = menu.findItem(R.id.menu_app_launch).getIcon().mutate();
			icon.setColorFilter(Color.GRAY, Mode.SRC_IN);
			menu.findItem(R.id.menu_app_launch).setIcon(icon);
		}

		boolean hasMarketLink = false;
		try {
			PackageManager pm = context.getPackageManager();
			String installer = pm.getInstallerPackageName(pkgName);
			if (installer != null)
				hasMarketLink = installer.equals("com.android.vending") || installer.contains("google");
		} catch (Exception e) {
		}

		menu.findItem(R.id.menu_app_store).setEnabled(hasMarketLink);
		try {
			Resources res = context.createPackageContext("com.android.vending", 0).getResources();
			int id = res.getIdentifier("ic_launcher_play_store", "mipmap", "com.android.vending");
			Drawable icon = res.getDrawable(id);
			if (!hasMarketLink) {
				icon = icon.mutate();
				icon.setColorFilter(Color.GRAY, Mode.SRC_IN);
			}
			menu.findItem(R.id.menu_app_store).setIcon(icon);
		} catch (Exception e) {
		}
	}

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        
        if (item.getItemId() == R.id.menu_save) {
            Editor prefsEditor = prefs.edit();
            if (swtActive.isChecked()) {
                prefsEditor.putBoolean(pkgName + Common.PREF_ACTIVE, true);
                float physDpi;
                try {
                	physDpi = Float.parseFloat(((EditText) findViewById(R.id.txtPhysDPI)).getText().toString());
                } catch (Exception ex) {
                	physDpi = 0;
                }
                if (physDpi != 0) {
                    prefsEditor.putInt(pkgName + Common.PREF_PHYSDPI, physDpi);
                } else {
                    prefsEditor.remove(pkgName + Common.PREF_PHYSDPI);
                }

            } else {
                prefsEditor.remove(pkgName + Common.PREF_PHYSDPI);
            }
            prefsEditor.commit();
            
            dirty = false;
            
            // Check if in addition so saving the settings, the app should also be killed
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.settings_apply_title);
            builder.setMessage(R.string.settings_apply_detail);
            builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                	// Send the broadcast requesting to kill the app
                    Intent applyIntent = new Intent(Common.MY_PACKAGE_NAME + ".UPDATE_PERMISSIONS");
                    applyIntent.putExtra("action", Common.ACTION_PERMISSIONS);
                    applyIntent.putExtra("Package", pkgName);
                    applyIntent.putExtra("Kill", true);
                    sendBroadcast(applyIntent, Common.MY_PACKAGE_NAME + ".BROADCAST_PERMISSION");
                    
                    dialog.dismiss();
                }
            });
            builder.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                	// Send the broadcast but not requesting kill
                    Intent applyIntent = new Intent(Common.MY_PACKAGE_NAME + ".UPDATE_PERMISSIONS");
                    applyIntent.putExtra("action", Common.ACTION_PERMISSIONS);
                    applyIntent.putExtra("Package", pkgName);
                    applyIntent.putExtra("Kill", false);
                    sendBroadcast(applyIntent, Common.MY_PACKAGE_NAME + ".BROADCAST_PERMISSION");

                    dialog.dismiss();
                }
            });
            builder.create().show();
            
        } else if (item.getItemId() == R.id.menu_app_launch) {
            Intent LaunchIntent = getPackageManager().getLaunchIntentForPackage(pkgName);
            startActivity(LaunchIntent);
        } else if (item.getItemId() == R.id.menu_app_settings) {
            startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                     Uri.parse("package:" + pkgName)));
		} else if (item.getItemId() == R.id.menu_app_store) {
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + pkgName)));
        }
        return super.onOptionsItemSelected(item);
    }
    
    
    
}
