package me.slyh.androidenterprisewlanconnector;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    public boolean isWifiEnabledinScan;
    View dialog_addwifi_view;
    public KeyStore ks = null;
    public TreeMap<String, String> DNList = new TreeMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final WifiManager WifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        // Check if user enabled Wi-Fi
        if(!WifiManager.isWifiEnabled()) {
            /*
            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            builder.setMessage(R.string.dialog_enablewifi_message)
                    .setTitle(R.string.dialog_enablewifi_title);

            builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    WifiManager.setWifiEnabled(true);
                }
            });
            builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // Do nothing
                }
            });

            AlertDialog dialog_enablewifi = builder.create();
            dialog_enablewifi.show();
            */
            scanWifiAP();
        } else {
            scanWifiAP();
        }

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab_addnetwork);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showAddNetworkDialog("");
            }
        });

        loadKeyStore();

        // Ask for permission
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && !ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {

            Snackbar.make(findViewById(R.id.fab_addnetwork),
                    getResources().getText(R.string.sb_ask_user_location_permission),
                    Snackbar.LENGTH_INDEFINITE)
                    .setAction(getResources().getText(R.string.string_continue), new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 0);
                        }
                    }).show();

        }
    }

    ArrayList<String> scanresult_SSID = new ArrayList<>();
    ArrayList<String> scanresult_capabilities = new ArrayList<>();
    ArrayList<Integer> scanresult_signal_icon = new ArrayList<>();
    public void scanWifiAP() {
        //ArrayAdapter<String> scanResult = new ArrayAdapter(this, android.R.layout.simple_list_item_1);
        final WifiManager WifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        if(WifiManager.isWifiEnabled()) {
            scanresult_SSID.clear();
            scanresult_capabilities.clear();
            scanresult_signal_icon.clear();
            /*
            Snackbar.make(findViewById(android.R.id.content), "Scanning...", Snackbar.LENGTH_SHORT)
                    .setAction("Action", null).show();
            */
            WifiManager.startScan();
            List<ScanResult> list_scanresult = WifiManager.getScanResults();
            Integer sizeOfList = list_scanresult.size();

            // Put result in TreeMap to sort it (by the signal strength)
            TreeMap<Integer, String> map_scanresult = new TreeMap<>();
            TreeMap<String, String> map_capabilities = new TreeMap<>();
            for(int i = 0; i < sizeOfList; i++) {
                if(!list_scanresult.get(i).SSID.isEmpty()) {
                //if(!list_scanresult.get(i).SSID.isEmpty() && list_scanresult.get(i).capabilities.contains("EAP")) {
                    //Log.d("WIFIINFO", list_scanresult.get(i).capabilities);
                    map_scanresult.put(list_scanresult.get(i).level*-1, list_scanresult.get(i).SSID);
                    map_capabilities.put(list_scanresult.get(i).SSID, list_scanresult.get(i).capabilities);
                }
            }
            Set set = map_scanresult.entrySet();
            Iterator iterator = set.iterator();
            while(iterator.hasNext()) {
                Map.Entry map_entry = (Map.Entry) iterator.next();
                scanresult_SSID.add(map_entry.getValue().toString());
                scanresult_capabilities.add(map_capabilities.get(map_entry.getValue().toString()));
                if((Integer) map_entry.getKey() < 67) {
                    scanresult_signal_icon.add(R.drawable.wifi_excellent_signal);
                } else if((Integer) map_entry.getKey() < 70) {
                    scanresult_signal_icon.add(R.drawable.wifi_good_signal);
                } else if((Integer) map_entry.getKey() < 80) {
                    scanresult_signal_icon.add(R.drawable.wifi_acceptable_signal);
                } else {
                    scanresult_signal_icon.add(R.drawable.wifi_bad_signal);
                }
            }
            isWifiEnabledinScan = true;
            findViewById(R.id.layout_content_main).setVisibility(View.VISIBLE);
            findViewById(R.id.layout_wifi_disabled).setVisibility(View.GONE);
            if(scanresult_SSID.size() == 0) {
                TextView tv_wifi_disabled_hint = (TextView) findViewById(R.id.wifi_disabled_hint);
                tv_wifi_disabled_hint.setText(getResources().getText(R.string.no_wifi_found));
                findViewById(R.id.layout_content_main).setVisibility(View.GONE);
                findViewById(R.id.layout_wifi_disabled).setVisibility(View.VISIBLE);
            }
        } else {
            //scanResult.add("Wi-Fi is disabled.");
            findViewById(R.id.layout_content_main).setVisibility(View.GONE);
            findViewById(R.id.layout_wifi_disabled).setVisibility(View.VISIBLE);
            TextView tv_wifi_disabled_hint = (TextView) findViewById(R.id.wifi_disabled_hint);
            tv_wifi_disabled_hint.setText(getResources().getText(R.string.tell_user_turn_on_wifi_1));
            isWifiEnabledinScan = false;
        }
        ScanResultAdapter scan_result_adapter = new ScanResultAdapter(this, scanresult_signal_icon, scanresult_SSID, scanresult_capabilities);
        final ListView lv_scan_result = (ListView) findViewById(R.id.lv_scan_result);
        lv_scan_result.setAdapter(scan_result_adapter);
        lv_scan_result.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView parent, View view, int position, long id) {
                ListView lv_scan_result = (ListView) findViewById(R.id.lv_scan_result);
                //Log.d("DEBUG", "Position: " + position + ", GetChildCount: " + lv_scan_result.getChildCount() + ", Value: " + lv_scan_result.getItemAtPosition(position).toString());
                if(isWifiEnabledinScan) {
                    if(!scanresult_capabilities.get(position).contains("EAP")) {
                        final String tmp_SSID = scanresult_SSID.get(position);
                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

                        builder.setMessage(R.string.dialog_capabilities_mismatch)
                                .setTitle(R.string.app_name);

                        builder.setPositiveButton(R.string.string_continue, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                showAddNetworkDialog(tmp_SSID);
                            }
                        });
                        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // Do nothing
                            }
                        });

                        AlertDialog dialog_enablewifi = builder.create();
                        dialog_enablewifi.show();
                    } else {
                        showAddNetworkDialog(scanresult_SSID.get(position));
                    }
                }
            }
        });
        Handler handler = new Handler();
        handler.postDelayed(new Runnable(){
            @Override
            public void run() {
                scanWifiAP();
            }}, 5000);
    }

    public void showAddNetworkDialog(String SSID) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        // Get the layout inflater
        LayoutInflater inflater = MainActivity.this.getLayoutInflater();

        dialog_addwifi_view = inflater.inflate(R.layout.dialog_addwifi, null);

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setView(dialog_addwifi_view)
                // Add action buttons
                .setPositiveButton(R.string.connect, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable(){
                        @Override
                        public void run() {
                            addNetwork();
                        }}, 1);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // Do nothing
                    }
                });

        AlertDialog dialog_addnetwork = builder.create();
        dialog_addnetwork.show();

        Spinner caList = (Spinner) dialog_addwifi_view.findViewById(R.id.sp_addnetwork_cacertificate);
        Set DNSet = DNList.entrySet();
        Iterator iterator = DNSet.iterator();
        ArrayAdapter<String> caDNList = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        while(iterator.hasNext()) {
            Map.Entry map_entry = (Map.Entry)iterator.next();
            caDNList.add(map_entry.getKey().toString());
        }
        caDNList.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        caList.setAdapter(caDNList);

        EditText et_ssid = (EditText) dialog_addnetwork.findViewById(R.id.et_addnetwork_ssid);
        if(et_ssid!=null) {
            et_ssid.setText(SSID);
        } else {
            Log.d("DEBUG", "et_ssid is null");
        }
    }

    //boolean enable_wifi_approved = true;
    public void addNetwork() {
        EditText mSSID = (EditText) dialog_addwifi_view.findViewById(R.id.et_addnetwork_ssid);
        EditText mIdentity = (EditText) dialog_addwifi_view.findViewById(R.id.et_addnetwork_identity);
        EditText mAnonymousIdentity = (EditText) dialog_addwifi_view.findViewById(R.id.et_addnetwork_anonymousidentity);
        EditText mPassword = (EditText) dialog_addwifi_view.findViewById(R.id.et_addnetwork_password);
        EditText mSubjectNameCN = (EditText) dialog_addwifi_view.findViewById(R.id.et_addnetwork_subjectname);
        Spinner caSpinner = (Spinner) dialog_addwifi_view.findViewById(R.id.sp_addnetwork_cacertificate);
        Spinner eapSpinner = (Spinner) dialog_addwifi_view.findViewById(R.id.sp_addnetwork_eapmethod);
        Spinner p2Spinner = (Spinner) dialog_addwifi_view.findViewById(R.id.sp_addnetwork_phase2authentication);

        String SSID = mSSID.getText().toString();
        String Identity = mIdentity.getText().toString();
        String AnonymousIdentity = mAnonymousIdentity.getText().toString();
        String Password = mPassword.getText().toString();
        String SubjectNameCN = mSubjectNameCN.getText().toString();
        String caAlias = DNList.get(caSpinner.getSelectedItem().toString());
        Integer eapMethod = getResources().getIntArray(R.array.EapMethodValues)[eapSpinner.getSelectedItemPosition()];
        Integer p2Method = getResources().getIntArray(R.array.Phase2MethodValues)[p2Spinner.getSelectedItemPosition()];

        WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        WifiConfiguration wc = new WifiConfiguration();
        wc.SSID = SSID;
        wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_EAP);
        wc.enterpriseConfig.setIdentity(Identity);
        wc.enterpriseConfig.setAnonymousIdentity(AnonymousIdentity);
        wc.enterpriseConfig.setPassword(Password);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            wc.enterpriseConfig.setDomainSuffixMatch(SubjectNameCN);
        } else {
            wc.enterpriseConfig.setSubjectMatch(SubjectNameCN);
        }

        //String tmp_loaded_ca_info = "";
        X509Certificate caCert = null;
        try {
            caCert = (X509Certificate) ks.getCertificate(caAlias);
            Log.i("caCert", caCert.getSubjectDN().toString());
            try {
                wc.enterpriseConfig.setCaCertificate(caCert);
                //tmp_loaded_ca_info = caCert.getSubjectDN().toString();
            } catch (IllegalArgumentException e) {
                Snackbar.make(findViewById(R.id.fab_addnetwork), getResources().getText(R.string.loadcafailed), Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                return;
            }
        } catch (KeyStoreException e) {
            e.printStackTrace();
            Log.i("caCert", "DEBUG2");
        }

        wc.enterpriseConfig.setEapMethod(eapMethod);
        wc.enterpriseConfig.setPhase2Method(p2Method);

        /*
        // Originally I want to ask user before turning the Wi-Fi on
        enable_wifi_approved = true;
        if(!wifi.isWifiEnabled()) {
            enable_wifi_approved = false;
            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            builder.setMessage(R.string.dialog_enablewifi_message2)
                    .setTitle(R.string.dialog_enablewifi_title);

            builder.setPositiveButton(R.string.string_continue, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // Continue
                    enable_wifi_approved = true;
                }
            });
            builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // Do nothing
                }
            });

            AlertDialog dialog_enablewifi = builder.create();
            dialog_enablewifi.show();
        }
        */
        boolean enablenetwork_result = false;
        // Store the Wi-Fi state so we can restore to the original status
        Integer tmp_WifiState = wifi.getWifiState();
        if(tmp_WifiState != wifi.WIFI_STATE_ENABLED) {
            wifi.setWifiEnabled(true);
        }
        // Wait for the Wi-Fi to turn on
        Integer spent_time = 0;
        while(wifi.getWifiState() != wifi.WIFI_STATE_ENABLED && spent_time < 5000) {
            SystemClock.sleep(100);
            spent_time += 100;
        }
        if(wifi.getWifiState() != wifi.WIFI_STATE_ENABLED) {
            Snackbar.make(findViewById(R.id.fab_addnetwork),
                    getResources().getText(R.string.turnon_wifi_failed),
                    Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
            return;
        }
        wc.status = WifiConfiguration.Status.ENABLED;
        //Log.v("WifiConfiguration", wc.toString());
        Integer res = wifi.addNetwork(wc);

        // Turn off the Wi-Fi if it's disabled previously
        if(tmp_WifiState == wifi.WIFI_STATE_DISABLED || tmp_WifiState == wifi.WIFI_STATE_DISABLING) {
            enablenetwork_result = wifi.enableNetwork(res, false);
            wifi.setWifiEnabled(false);
        } else {
            enablenetwork_result = wifi.enableNetwork(res, true);
        }

        if(enablenetwork_result) {
            Snackbar.make(findViewById(R.id.fab_addnetwork),
                    getResources().getText(R.string.configsaved).toString(),
                    //getResources().getText(R.string.configsaved).toString() + getResources().getText(R.string.loadedcaname) + tmp_loaded_ca_info,
                    Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
        } else {
            Snackbar.make(findViewById(R.id.fab_addnetwork),
                    getResources().getText(R.string.configsave_failed),
                    Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
        }
    }

    public void loadKeyStore() {
        try {
            Log.i("KeyStore", "Opening keystore");
            ks = KeyStore.getInstance("androidCAStore");
            ks.load(null, null);
            Enumeration<?> aliases = ks.aliases();
            while (aliases.hasMoreElements()) {
                Log.i("KeyStore", "Debug");
                String alias = (String) aliases.nextElement();
                X509Certificate cert = (X509Certificate) ks.getCertificate(alias);
                Matcher m = Pattern.compile("CN=([^,]*)").matcher(cert.getSubjectDN().getName());
                if(m.find()) {
                    DNList.put(m.group(1), alias);
                } else {
                    DNList.put(cert.getSubjectDN().getName(), alias);
                }
            }
        } catch (KeyStoreException e) {
            e.printStackTrace();
            Log.i("KeyStore", "Failed to open keystore");
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        /*
        if (id == R.id.action_settings) {
            return true;
        }
        */
        if (id == R.id.action_refresh) {
            //scanWifiAP();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
