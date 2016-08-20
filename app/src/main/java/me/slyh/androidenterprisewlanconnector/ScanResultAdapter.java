package me.slyh.androidenterprisewlanconnector;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by slyh on 16/8/2016.
 */
public class ScanResultAdapter extends ArrayAdapter<String> {

    private final Activity context;
    private final String[] SSID;
    private final String[] description;
    private final Integer[] signal_icon;

    public ScanResultAdapter(Activity context, ArrayList<Integer> signal_icon, ArrayList<String> SSID, ArrayList<String> description) {
        super(context, R.layout.custom_wifi_scanresult_row, SSID);
        this.context = context;
        this.SSID = SSID.toArray(new String[SSID.size()]);
        this.signal_icon = signal_icon.toArray(new Integer[signal_icon.size()]);
        this.description = description.toArray(new String[description.size()]);
    }

    public View getView(int position,View view,ViewGroup parent) {
        LayoutInflater inflater = context.getLayoutInflater();
        View rowView=inflater.inflate(R.layout.custom_wifi_scanresult_row, null,true);

        TextView tv_SSID = (TextView) rowView.findViewById(R.id.row_ssid);
        ImageView iv_signal_icon = (ImageView) rowView.findViewById(R.id.row_signal_icon);
        TextView tv_description = (TextView) rowView.findViewById(R.id.row_description);

        tv_SSID.setText(SSID[position]);
        iv_signal_icon.setImageResource(signal_icon[position]);
        tv_description.setText(description[position]);
        return rowView;
    }
}
