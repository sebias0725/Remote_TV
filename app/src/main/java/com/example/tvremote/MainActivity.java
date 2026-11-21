package com.example.tvremote;

import android.app.Activity;
import android.os.Bundle;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiManager;
import android.view.Gravity;
import android.widget.*;
import java.util.*;

/** Device discovery UI. Pairing and remote command transport are not yet implemented. */
public class MainActivity extends Activity {
    private LinearLayout root, devices;
    private TextView status;
    private NsdManager nsd;
    private WifiManager.MulticastLock lock;
    private final Map<String,NsdServiceInfo> discovered = new LinkedHashMap<>();
    private final ArrayList<NsdManager.DiscoveryListener> listeners = new ArrayList<>();
    private final String[] types = {"_androidtvremote2._tcp.", "_androidtvremote._tcp.", "_googlecast._tcp."};
    private SharedPreferences prefs;

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        prefs = getSharedPreferences("tvremote", MODE_PRIVATE);
        nsd = (NsdManager)getSystemService(Context.NSD_SERVICE);
        WifiManager wifi = (WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        lock = wifi.createMulticastLock("TVRemoteDiscovery"); lock.setReferenceCounted(false);
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(18,20,18,24); root.setBackgroundColor(0xff10131b);
        ScrollView scroll = new ScrollView(this); scroll.addView(root); setContentView(scroll);
        TextView heading = new TextView(this); heading.setText("TV Remote"); heading.setTextSize(29);
        heading.setTypeface(null,Typeface.BOLD); heading.setTextColor(Color.WHITE);
        heading.setGravity(Gravity.CENTER); root.addView(heading);
        status = label("Ready to scan compatible TVs on Wi-Fi",16);
        status.setGravity(Gravity.CENTER); status.setPadding(0,12,0,16);
        button("Scan nearby Smart TVs", () -> scan());
        label("Discovered devices",19).setTypeface(null,Typeface.BOLD);
        devices = new LinearLayout(this); devices.setOrientation(LinearLayout.VERTICAL); root.addView(devices);
        String last = prefs.getString("last_device", "");
        if (!last.isEmpty()) label("Last selected: " + last,13);
        label("Remote layout",19).setTypeface(null,Typeface.BOLD);
        row("⏻ Power", "▣ Input");
        row("◉ Assistant", "⚙ Settings");
        button("▲", () -> unavailable("Up"));
        row("◀", "OK", "▶");
        button("▼", () -> unavailable("Down"));
        row("↩ Back", "⌂ Home", "☷ Menu");
        row("Vol +", "Mute", "Vol −");
        row("YouTube", "Netflix"); row("Prime Video", "Disney+");
        TextView notice = label("Compatibility notice: this version discovers advertised network services, but does not yet pair or send commands. Android TV Remote v2 requires an authenticated pairing protocol. A discovered Cast device is not necessarily remote-controllable.",13);
        notice.setPadding(0,20,0,10);
    }
    private TextView label(String s,int size){
        TextView v = new TextView(this);v.setText(s);v.setTextSize(size);v.setTextColor(0xffd7deeb);
        v.setPadding(4,10,4,8);root.addView(v);return v;
    }
    private Button button(String s,Runnable action){
        Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextColor(Color.WHITE);
        b.setBackgroundTintList(ColorStateList.valueOf(0xff303c54));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,54);p.setMargins(2,4,2,4);
        root.addView(b,p);b.setOnClickListener(v->action.run());return b;
    }
    private void row(String... names){
        LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.HORIZONTAL);root.addView(r);
        for(String name:names){
            Button b=new Button(this);b.setText(name);b.setAllCaps(false);b.setTextColor(Color.WHITE);
            b.setBackgroundTintList(ColorStateList.valueOf(0xff303c54));
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,54,1);p.setMargins(2,4,2,4);
            r.addView(b,p);b.setOnClickListener(v->unavailable(name));
        }
    }
    private void unavailable(String name){status.setText(name+": not connected. Pairing/command protocol is not implemented yet.");}
    private void scan(){
        stopScan(); discovered.clear();devices.removeAllViews();status.setText("Scanning Wi-Fi services…");
        try {if(lock!=null&&!lock.isHeld())lock.acquire();}catch(Exception ignored){}
        for(String type:types){
            NsdManager.DiscoveryListener listener=new NsdManager.DiscoveryListener(){
                public void onDiscoveryStarted(String t){}
                public void onDiscoveryStopped(String t){}
                public void onStartDiscoveryFailed(String t,int e){runOnUiThread(()->status.setText("Discovery failed ("+e+"). Check Wi-Fi."));}
                public void onStopDiscoveryFailed(String t,int e){}
                public void onServiceLost(NsdServiceInfo service){runOnUiThread(()->{discovered.remove(service.getServiceName()+service.getServiceType());showDevices();});}
                public void onServiceFound(NsdServiceInfo service){
                    runOnUiThread(()->{discovered.put(service.getServiceName()+service.getServiceType(),service);showDevices();});
                }
            };
            try{nsd.discoverServices(type,NsdManager.PROTOCOL_DNS_SD,listener);listeners.add(listener);}
            catch(Exception e){status.setText("Discovery error: "+e.getMessage());}
        }
        new android.os.Handler(getMainLooper()).postDelayed(()->{
            if(discovered.isEmpty())status.setText("No advertised compatible services found. The TV may use a different protocol.");
            else status.setText(discovered.size()+" advertised service(s) found. Select one to inspect.");
        },8000);
    }
    private void showDevices(){
        devices.removeAllViews();
        for(NsdServiceInfo info:discovered.values()){
            Button b=new Button(this);b.setAllCaps(false);
            b.setText(info.getServiceName()+"\n"+info.getServiceType());
            b.setTextColor(Color.WHITE);b.setBackgroundTintList(ColorStateList.valueOf(0xff263a42));
            devices.addView(b);
            b.setOnClickListener(v->{
                prefs.edit().putString("last_device",info.getServiceName()).apply();
                status.setText("Selected "+info.getServiceName()+". Pairing is not yet implemented; no commands can be sent.");
            });
        }
    }
    private void stopScan(){
        if(nsd!=null)for(NsdManager.DiscoveryListener l:listeners){try{nsd.stopServiceDiscovery(l);}catch(Exception ignored){}}
        listeners.clear();try{if(lock!=null&&lock.isHeld())lock.release();}catch(Exception ignored){}
    }
    @Override protected void onDestroy(){stopScan();super.onDestroy();}
}
