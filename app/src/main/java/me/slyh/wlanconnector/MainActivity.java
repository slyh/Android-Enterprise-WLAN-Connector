package me.slyh.wlanconnector;

import android.content.Context;
import android.database.DataSetObserver;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.Toast;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    public List<String> DNList = new ArrayList<String>();
    public List<String> aliasesList = new ArrayList<String>();
    public KeyStore ks = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        try {
            Log.i("KeyStore", "Opening keystore");
            ks = KeyStore.getInstance("androidCAStore");
            ks.load(null, null);
            Enumeration<?> aliases = ks.aliases();
            while (aliases.hasMoreElements()) {
                Log.i("KeyStore", "Debug");
                String alias = (String) aliases.nextElement();
                X509Certificate cert = (X509Certificate) ks.getCertificate(alias);
                Matcher m = Pattern.compile("CN=([^,]*),").matcher(cert.getSubjectDN().getName());
                if(m.find()) {
                    DNList.add(m.group(1));
                } else {
                    DNList.add(cert.getSubjectDN().getName());
                }
                aliasesList.add(alias);
            }
            /*
            Collections.sort(DNList, new Comparator<String>() {
                @Override
                public int compare(String value1, String value2) {
                    return value1.compareTo(value2);
                }
            });
            */
            Spinner caList = (Spinner) findViewById(R.id.caList);
            ArrayAdapter<String> caDNList = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, DNList);
            caDNList.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            Log.i("KeyStore", "Debug2");
            caList.setAdapter(caDNList);
            Log.i("KeyStore", DNList.toString());
            Log.i("KeyStore", aliasesList.toString());
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
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_save:
                saveSettings();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void saveSettings() {
        EditText mSSID = (EditText) findViewById(R.id.SSID);
        EditText mIdentity = (EditText) findViewById(R.id.Identity);
        EditText mAnonymousIdentity = (EditText) findViewById(R.id.AnonymousIdentity);
        EditText mPassword = (EditText) findViewById(R.id.Password);
        EditText mSubjectNameCN = (EditText) findViewById(R.id.SubjectNameCN);
        Spinner caSpinner = (Spinner) findViewById(R.id.caList);
        Spinner eapSpinner = (Spinner) findViewById(R.id.EapMethod);
        Spinner p2Spinner = (Spinner) findViewById(R.id.Phase2Method);

        String SSID = mSSID.getText().toString();
        String Identity = mIdentity.getText().toString();
        String AnonymousIdentity = mAnonymousIdentity.getText().toString();
        String Password = mPassword.getText().toString();
        String SubjectNameCN = mSubjectNameCN.getText().toString();
        String caAlias = this.aliasesList.get(caSpinner.getSelectedItemPosition());
        Integer eapMethod = getResources().getIntArray(R.array.EapMethodValues)[eapSpinner.getSelectedItemPosition()];
        Integer p2Method = getResources().getIntArray(R.array.Phase2MethodValues)[p2Spinner.getSelectedItemPosition()];

        WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        WifiConfiguration wc = new WifiConfiguration();
        wc.SSID = SSID;
        wc.enterpriseConfig.setEapMethod(0);
        wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_EAP);
        wc.enterpriseConfig.setIdentity(Identity);
        wc.enterpriseConfig.setAnonymousIdentity(AnonymousIdentity);
        wc.enterpriseConfig.setPassword(Password);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            wc.enterpriseConfig.setAltSubjectMatch(SubjectNameCN);
        } else {
            wc.enterpriseConfig.setSubjectMatch(SubjectNameCN);
        }

        X509Certificate caCert = null;
        try {
            caCert = (X509Certificate) ks.getCertificate(caAlias);
            Log.i("caCert", caCert.getSubjectDN().toString());
            try {
                Toast toast = Toast.makeText(getApplicationContext(), "CA cert "+caCert.getSubjectDN()+" (Alias: "+caAlias+") loaded!", Toast.LENGTH_LONG);
                toast.show();
                wc.enterpriseConfig.setCaCertificate(caCert);
            } catch (IllegalArgumentException e) {
                Toast toast = Toast.makeText(getApplicationContext(), "Failed to load CA cert!", Toast.LENGTH_LONG);
                toast.show();
                return;
            }
        } catch (KeyStoreException e) {
            e.printStackTrace();
            Log.i("caCert", "DEBUG2");
        }

        wc.enterpriseConfig.setEapMethod(eapMethod);
        wc.enterpriseConfig.setPhase2Method(p2Method);
        wc.status = WifiConfiguration.Status.ENABLED;
        Integer res = wifi.addNetwork(wc);
        wifi.enableNetwork(wc.networkId, false);
        wifi.enableNetwork(res, true);

        Toast toast = Toast.makeText(getApplicationContext(), "WLAN config saved!", Toast.LENGTH_LONG);
        //toast.show();
    }
}
