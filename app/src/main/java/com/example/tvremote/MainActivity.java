package com.example.tvremote;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiManager;
import android.view.*;
import android.widget.*;
import java.util.*;

/** Polished remote interface and Wi-Fi service discovery. No pairing or command transport yet. */
public class MainActivity extends Activity {
    static final int BG=0xff0c1018, PANEL=0xff181e2a, SURFACE=0xff242c3b, WHITE=0xfff4f6fc, MUTED=0xff98a6bc, ACCENT=0xff8cafff;
    private LinearLayout page, deviceList;
    private TextView deviceName, message;
    private NsdManager nsd;
    private WifiManager.MulticastLock multicast;
    private final Map<String,NsdServiceInfo> found=new LinkedHashMap<>();
    private final ArrayList<NsdManager.DiscoveryListener> listeners=new ArrayList<>();
    private final String[] types={"_androidtvremote2._tcp.","_androidtvremote._tcp.","_googlecast._tcp."};
    private SharedPreferences prefs;
    private final Handler handler=new Handler();
    private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private GradientDrawable bg(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private TextView text(String value,int size,int color,boolean bold){TextView v=new TextView(this);v.setText(value);v.setTextSize(size);v.setTextColor(color);v.setGravity(Gravity.CENTER_VERTICAL);if(bold)v.setTypeface(null,1);return v;}
    private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);return l;}
    private LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(0);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    private void gap(LinearLayout l,int h){View v=new View(this);l.addView(v,new LinearLayout.LayoutParams(1,dp(h)));}
    private void add(LinearLayout parent,View v,int height){parent.addView(v,new LinearLayout.LayoutParams(-1,height<0?height:dp(height)));}
    private TextView key(String label,int height,Runnable action){TextView t=text(label,16,WHITE,true);t.setGravity(Gravity.CENTER);t.setBackground(bg(SURFACE,17));t.setOnClickListener(v->action.run());t.setMinimumHeight(dp(height));return t;}
    private void keyRow(LinearLayout parent,String[] labels,int height){LinearLayout r=row();for(String s:labels){TextView t=key(s,height,()->unavailable(s));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(height),1);p.setMargins(dp(4),0,dp(4),0);r.addView(t,p);}add(parent,r,height);}
    @Override public void onCreate(Bundle saved){super.onCreate(saved);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
        prefs=getSharedPreferences("tvremote",0);nsd=(NsdManager)getSystemService(NSD_SERVICE);
        WifiManager wifi=(WifiManager)getApplicationContext().getSystemService(WIFI_SERVICE);if(wifi!=null){multicast=wifi.createMulticastLock("TVRemoteDiscovery");multicast.setReferenceCounted(false);}
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(BG);page=column();page.setPadding(dp(20),dp(22),dp(20),dp(28));scroll.addView(page);setContentView(scroll);
        LinearLayout top=row();TextView title=text("TV Remote",27,WHITE,true);top.addView(title,new LinearLayout.LayoutParams(0,dp(48),1));
        TextView scan=text("SCAN",12,ACCENT,true);scan.setGravity(Gravity.CENTER);scan.setBackground(bg(PANEL,15));scan.setOnClickListener(v->showDiscovery());top.addView(scan,new LinearLayout.LayoutParams(dp(78),dp(44)));add(page,top,50);
        gap(page,15);
        LinearLayout device=row();device.setPadding(dp(15),dp(12),dp(15),dp(12));device.setBackground(bg(PANEL,18));
        TextView icon=text("▣",25,ACCENT,true);device.addView(icon,new LinearLayout.LayoutParams(dp(38),dp(46)));
        LinearLayout info=column();deviceName=text(prefs.getString("last_device","No TV connected"),16,WHITE,true);add(info,deviceName,23);TextView sub=text("Tap to find a Smart TV  •  Wi-Fi",12,MUTED,false);add(info,sub,20);device.addView(info,new LinearLayout.LayoutParams(0,dp(46),1));TextView arrow=text("›",28,MUTED,false);device.addView(arrow);device.setOnClickListener(v->showDiscovery());add(page,device,72);
        gap(page,22);
        LinearLayout power=row();TextView p=key("⏻",55,()->unavailable("Power"));p.setTextColor(0xffff8888);p.setTextSize(29);p.setBackground(bg(0xff39242b,18));power.addView(p,new LinearLayout.LayoutParams(dp(64),dp(55)));
        View spacer=new View(this);power.addView(spacer,new LinearLayout.LayoutParams(0,1,1));TextView input=key("▣  INPUT",55,()->unavailable("Input"));input.setTextSize(13);power.addView(input,new LinearLayout.LayoutParams(dp(115),dp(55)));add(page,power,55);
        gap(page,16);
        keyRow(page,new String[]{"◉  Assistant","⚙  Settings"},50);
        gap(page,20);
        TextView navLabel=text("NAVIGATION",11,MUTED,true);navLabel.setGravity(Gravity.CENTER);add(page,navLabel,20);gap(page,8);
        Dpad dpad=new Dpad(this);LinearLayout center=row();center.setGravity(Gravity.CENTER);center.addView(dpad,new LinearLayout.LayoutParams(dp(245),dp(245)));add(page,center,245);
        gap(page,20);
        keyRow(page,new String[]{"↶  Back","⌂  Home","☷  Menu"},55);
        gap(page,14);
        keyRow(page,new String[]{"＋  Volume","◀)  Mute","－  Volume"},58);
        gap(page,22);
        TextView shortcuts=text("QUICK ACCESS",11,MUTED,true);add(page,shortcuts,25);gap(page,6);
        keyRow(page,new String[]{"YouTube","NETFLIX"},52);gap(page,9);keyRow(page,new String[]{"prime video","Disney+"},52);
        gap(page,24);
        message=text("Connect to a compatible TV to get started.",12,MUTED,false);message.setGravity(Gravity.CENTER);add(page,message,45);
    }
    private void unavailable(String name){message.setText(name+" requires a paired TV. Command transport is not yet implemented.");Toast.makeText(this,"Remote commands not yet available",Toast.LENGTH_SHORT).show();}
    private class Dpad extends View{
        Paint paint=new Paint(3);Dpad(Context c){super(c);setLayerType(View.LAYER_TYPE_SOFTWARE,null);setOnTouchListener((v,event)->{if(event.getAction()==MotionEvent.ACTION_UP){float x=event.getX()-getWidth()/2f,y=event.getY()-getHeight()/2f;float a=Math.abs(x),b=Math.abs(y);String k=(a<dp(42)&&b<dp(42))?"OK":a>b?(x>0?"Right":"Left"):(y>0?"Down":"Up");unavailable(k);return true;}return true;});}
        @Override protected void onDraw(Canvas c){super.onDraw(c);float cx=getWidth()/2f,cy=getHeight()/2f,r=Math.min(cx,cy)-dp(3);
            paint.setStyle(Paint.Style.FILL);paint.setColor(PANEL);paint.setShadowLayer(dp(8),0,dp(4),0x55000000);c.drawCircle(cx,cy,r,paint);paint.clearShadowLayer();
            paint.setColor(0xff303a4c);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(1));c.drawCircle(cx,cy,r-dp(2),paint);paint.setStyle(Paint.Style.FILL);
            paint.setColor(SURFACE);c.drawCircle(cx,cy,dp(52),paint);paint.setColor(ACCENT);c.drawCircle(cx,cy,dp(39),paint);
            paint.setTextAlign(Paint.Align.CENTER);paint.setTypeface(Typeface.create("sans-serif-medium",Typeface.BOLD));paint.setTextSize(dp(18));paint.setColor(BG);c.drawText("OK",cx,cy+dp(6),paint);
            paint.setColor(WHITE);paint.setTextSize(dp(26));c.drawText("⌃",cx,cy-dp(75),paint);c.drawText("⌄",cx,cy+dp(91),paint);c.drawText("‹",cx-dp(83),cy+dp(9),paint);c.drawText("›",cx+dp(83),cy+dp(9),paint);
        }
    }
    private void showDiscovery(){LinearLayout layout=column();layout.setPadding(dp(20),dp(12),dp(20),dp(12));TextView tip=text("Find compatible TVs on the same Wi-Fi network. Discovery does not guarantee pairing support.",13,MUTED,false);add(layout,tip,75);
        deviceList=column();add(layout,deviceList,-2);AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Find a TV").setView(layout).setNegativeButton("Close",(d,w)->stopScan()).setPositiveButton("Scan again",null).create();dialog.setOnShowListener(v->{dialog.getButton(-1).setOnClickListener(w->scan());scan();});dialog.setOnDismissListener(v->stopScan());dialog.show();}
    private void scan(){stopScan();found.clear();if(deviceList!=null){deviceList.removeAllViews();TextView searching=text("Scanning nearby devices…",14,WHITE,false);add(deviceList,searching,55);}try{if(multicast!=null&&!multicast.isHeld())multicast.acquire();}catch(Exception ignored){}
        for(String type:types){NsdManager.DiscoveryListener l=new NsdManager.DiscoveryListener(){public void onDiscoveryStarted(String t){}public void onDiscoveryStopped(String t){}public void onStartDiscoveryFailed(String t,int e){runOnUiThread(()->message.setText("Network discovery failed: "+e));}public void onStopDiscoveryFailed(String t,int e){}public void onServiceLost(NsdServiceInfo s){runOnUiThread(()->{found.remove(s.getServiceName()+s.getServiceType());showFound();});}public void onServiceFound(NsdServiceInfo s){runOnUiThread(()->{found.put(s.getServiceName()+s.getServiceType(),s);showFound();});}};
            try{nsd.discoverServices(type,NsdManager.PROTOCOL_DNS_SD,l);listeners.add(l);}catch(Exception ignored){}
        }
        handler.postDelayed(()->{if(found.isEmpty()&&deviceList!=null){deviceList.removeAllViews();add(deviceList,text("No compatible advertised services found. Check that both devices are on the same Wi-Fi.",13,MUTED,false),85);}},9000);
    }
    private void showFound(){if(deviceList==null)return;deviceList.removeAllViews();for(NsdServiceInfo s:found.values()){String name=s.getServiceName();TextView item=key("▣   "+name+"\n      "+s.getServiceType(),66,()->{prefs.edit().putString("last_device",name).apply();deviceName.setText(name);message.setText("Selected "+name+". Pairing and command sending are not implemented yet.");Toast.makeText(this,"Device selected; pairing unavailable",Toast.LENGTH_LONG).show();});item.setTextSize(13);item.setGravity(Gravity.CENTER_VERTICAL);item.setPadding(dp(12),0,0,0);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(66));p.setMargins(0,dp(4),0,dp(4));deviceList.addView(item,p);}}
    private void stopScan(){for(NsdManager.DiscoveryListener l:listeners){try{nsd.stopServiceDiscovery(l);}catch(Exception ignored){}}listeners.clear();try{if(multicast!=null&&multicast.isHeld())multicast.release();}catch(Exception ignored){}}
    @Override protected void onDestroy(){stopScan();super.onDestroy();}
}
