package com.example.tvremote;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.bluetooth.*;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.ParcelUuid;
import java.util.concurrent.Executor;
import android.view.*;
import android.widget.*;
import java.util.*;

/** Bluetooth-only remote. HID initialization is deferred until user action. */
public class MainActivity extends Activity {
    static final int BG=0xff0c1018, PANEL=0xff181e2a, SURFACE=0xff242c3b, WHITE=0xfff4f6fc, MUTED=0xff98a6bc, ACCENT=0xff8cafff;
    private LinearLayout page;
    private TextView deviceName, message;
    private BluetoothAdapter adapter;
    private BluetoothHidDevice hid;
    private BluetoothDevice connected;
    private boolean registered=false, requestingProfile=false, destroyed=false;
    private final Executor executor=command->runOnUiThread(command);
    // Standard boot keyboard (report 1) and consumer control (report 2).
    private static final byte[] DESCRIPTOR=new byte[]{
      0x05,0x01,0x09,0x06,(byte)0xA1,0x01,(byte)0x85,0x01,
      0x05,0x07,0x19,(byte)0xE0,0x29,(byte)0xE7,0x15,0x00,0x25,0x01,
      0x75,0x01,(byte)0x95,0x08,(byte)0x81,0x02,
      0x75,0x08,(byte)0x95,0x01,(byte)0x81,0x01,
      0x19,0x00,0x29,0x65,0x15,0x00,0x25,0x65,
      0x75,0x08,(byte)0x95,0x06,(byte)0x81,0x00,(byte)0xC0,
      0x05,0x0C,0x09,0x01,(byte)0xA1,0x01,(byte)0x85,0x02,
      0x19,0x00,0x2A,(byte)0xFF,0x03,0x15,0x00,0x26,(byte)0xFF,0x03,
      0x75,0x10,(byte)0x95,0x01,(byte)0x81,0x00,(byte)0xC0
    };
    private BluetoothHidDevice.Callback makeCallback(){return new BluetoothHidDevice.Callback(){
      @Override public void onAppStatusChanged(BluetoothDevice plugged,boolean isRegistered){registered=isRegistered;if(isRegistered&&!destroyed)showDiscovery();status(isRegistered?"Bluetooth remote ready. Pair from projector settings.":"Bluetooth HID registration unavailable.");}
      @Override public void onConnectionStateChanged(BluetoothDevice d,int state){
        if(state==BluetoothProfile.STATE_CONNECTED){connected=d;deviceName.setText(safeName(d));status("Connected — try the arrow keys.");}
        else if(connected!=null&&connected.equals(d)){connected=null;deviceName.setText("Not connected");status("Disconnected. Reconnect in Bluetooth settings.");}
      }
    };
    }
    private BluetoothProfile.ServiceListener makeServiceListener(){return new BluetoothProfile.ServiceListener(){
      @Override public void onServiceConnected(int profile,BluetoothProfile proxy){if(profile==BluetoothProfile.HID_DEVICE){requestingProfile=false; if(destroyed){try{adapter.closeProfileProxy(profile,proxy);}catch(Exception ignored){} return;} hid=(BluetoothHidDevice)proxy;registerHid();}}
      @Override public void onServiceDisconnected(int profile){hid=null;registered=false;connected=null;status("Bluetooth HID service disconnected.");}
    };
    }
    private void status(String t){runOnUiThread(()->{if(message!=null)message.setText(t);});}
    private String safeName(BluetoothDevice d){try{String n=d.getName();return n==null?"Paired projector":n;}catch(Exception e){return "Paired projector";}}
    private boolean hasPermissions(){return Build.VERSION.SDK_INT<31 || (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)==PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.BLUETOOTH_ADVERTISE)==PackageManager.PERMISSION_GRANTED);}
    private boolean permission(){if(!hasPermissions()){requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT,Manifest.permission.BLUETOOTH_ADVERTISE},200);return false;}return true;}
    @Override public void onRequestPermissionsResult(int req,String[] perms,int[] grants){super.onRequestPermissionsResult(req,perms,grants);if(req==200){if(hasPermissions())startBluetooth();else status("Bluetooth permission denied. Tap PAIR to try again.");}}
    private void startBluetooth(){
      try{
      if(!permission())return;
      if(Build.VERSION.SDK_INT<28){status("Bluetooth HID requires Android 9 or newer.");return;}
      BluetoothManager manager=(BluetoothManager)getSystemService(BLUETOOTH_SERVICE);adapter=manager==null?null:manager.getAdapter();
      if(adapter==null){status("Bluetooth hardware unavailable.");return;}
      if(!adapter.isEnabled()){startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),201);return;}
      if(registered){showDiscovery();return;}
      if(hid!=null){registerHid();return;}
      if(requestingProfile){status("Waiting for Bluetooth service…");return;}
      requestingProfile=adapter.getProfileProxy(this,makeServiceListener(),BluetoothProfile.HID_DEVICE);
      status(requestingProfile?"Starting Bluetooth HID…":"This phone does not expose Bluetooth HID Device support.");
      handler.postDelayed(()->{if(requestingProfile){requestingProfile=false;status("Bluetooth HID service did not respond. Tap PAIR to retry.");}},12000);
      }catch(Exception | LinkageError e){requestingProfile=false;status("Bluetooth initialization failed: "+e);}
    }
    @Override protected void onActivityResult(int req,int result,Intent data){super.onActivityResult(req,result,data);if(req==201){if(result==RESULT_OK)startBluetooth();else status("Bluetooth was not enabled.");}}
    private void registerHid(){
      if(hid==null||!hasPermissions())return;
      try {
      BluetoothHidDeviceAppSdpSettings sdp=new BluetoothHidDeviceAppSdpSettings("TV Remote","Bluetooth TV remote keyboard","TV Remote",BluetoothHidDevice.SUBCLASS1_COMBO,DESCRIPTOR);
      if(!hid.registerApp(sdp,null,null,executor,makeCallback()))status("HID registration rejected by Android.");}
      catch(Exception | LinkageError e){status("HID unavailable: "+e.getClass().getSimpleName());}
    }
    private void showDiscovery(){
      if(destroyed)return;
      if(!registered){startBluetooth();return;}
      new AlertDialog.Builder(this).setTitle("Pair Bluetooth remote")
        .setMessage("1. Tap Make Discoverable.\n2. On the Wanbo projector, open Settings → Bluetooth → Search for accessories.\n3. Select your phone and accept pairing on both devices.\n4. Return here and test the arrow keys.\n\nSome Android phones cannot emulate Bluetooth remotes. A paired connection is not guaranteed.")
        .setPositiveButton("Make Discoverable",(d,w)->{try{Intent i=new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);i.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,120);startActivity(i);}catch(Exception e){status("Cannot request Bluetooth discoverability.");}})
        .setNeutralButton("Reconnect paired",(d,w)->reconnect())
        .setNegativeButton("Close",null).show();
    }
    private void reconnect(){try{if(hid==null||!hasPermissions())return;for(BluetoothDevice d:hid.getDevicesMatchingConnectionStates(new int[]{BluetoothProfile.STATE_DISCONNECTED,BluetoothProfile.STATE_CONNECTED})){if(hid.connect(d)){status("Connecting to "+d.getName());return;}}status("Pair from the projector first, then reconnect.");}catch(Exception e){status("Reconnect failed: "+e);}}
    private void sendKeyboard(int code){
      if(hid==null||connected==null){status("Not connected. Tap PAIR and pair your projector.");return;}
      byte[] press=new byte[8];press[2]=(byte)code;
      try{hid.sendReport(connected,1,press);handler.postDelayed(()->{if(hid!=null&&connected!=null)try{hid.sendReport(connected,1,new byte[8]);}catch(Exception ignored){}},85);}catch(Exception e){status("Send failed: "+e.getMessage());}
    }
    private void sendConsumer(int usage){
      if(hid==null||connected==null){status("Not connected. Pair your projector first.");return;}
      byte[] report=new byte[]{(byte)(usage&255),(byte)((usage>>8)&255)};
      try{hid.sendReport(connected,2,report);handler.postDelayed(()->{if(hid!=null&&connected!=null)try{hid.sendReport(connected,2,new byte[2]);}catch(Exception ignored){}},85);}catch(Exception e){status("Send failed: "+e.getMessage());}
    }
    private void command(String label){switch(label){
      case "Up":sendKeyboard(0x52);break;case "Down":sendKeyboard(0x51);break;
      case "Left":sendKeyboard(0x50);break;case "Right":sendKeyboard(0x4F);break;
      case "OK":sendKeyboard(0x28);break;case "Back":sendKeyboard(0x29);break;
      case "Home":sendConsumer(0x0223);break;case "Menu":sendKeyboard(0x65);break;
      case "Volume+":sendConsumer(0xE9);break;case "Volume-":sendConsumer(0xEA);break;
      case "Mute":sendConsumer(0xE2);break;case "Power":sendConsumer(0x30);break;
      default:status(label+" is not supported by the standard Bluetooth keyboard profile.");
    }}
    private SharedPreferences prefs;
    private final Handler handler=new Handler(android.os.Looper.getMainLooper());
    private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private GradientDrawable bg(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private TextView text(String value,int size,int color,boolean bold){TextView v=new TextView(this);v.setText(value);v.setTextSize(size);v.setTextColor(color);v.setGravity(Gravity.CENTER_VERTICAL);if(bold)v.setTypeface(null,1);return v;}
    private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);return l;}
    private LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(0);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    private void gap(LinearLayout l,int h){View v=new View(this);l.addView(v,new LinearLayout.LayoutParams(1,dp(h)));}
    private void add(LinearLayout parent,View v,int height){parent.addView(v,new LinearLayout.LayoutParams(-1,height<0?height:dp(height)));}
    private TextView key(String label,int height,Runnable action){TextView t=text(label,16,WHITE,true);t.setGravity(Gravity.CENTER);t.setBackground(bg(SURFACE,17));t.setOnClickListener(v->action.run());t.setMinimumHeight(dp(height));return t;}
    private void keyRow(LinearLayout parent,String[] labels,int height){LinearLayout r=row();for(String s:labels){TextView t=key(s,height,()->command(mapLabel(s)));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(height),1);p.setMargins(dp(4),0,dp(4),0);r.addView(t,p);}add(parent,r,height);}
    @Override public void onCreate(Bundle saved){super.onCreate(saved);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
        prefs=getSharedPreferences("tvremote",0);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(BG);page=column();page.setPadding(dp(20),dp(22),dp(20),dp(28));scroll.addView(page);setContentView(scroll);
        LinearLayout top=row();TextView title=text("TV Remote",27,WHITE,true);top.addView(title,new LinearLayout.LayoutParams(0,dp(48),1));
        TextView scan=text("PAIR",12,ACCENT,true);scan.setGravity(Gravity.CENTER);scan.setBackground(bg(PANEL,15));scan.setOnClickListener(v->showDiscovery());top.addView(scan,new LinearLayout.LayoutParams(dp(78),dp(44)));add(page,top,50);
        TextView version=text("Version 0.5 • Bluetooth only",12,MUTED,false);add(page,version,24);
        gap(page,15);
        LinearLayout device=row();device.setPadding(dp(15),dp(12),dp(15),dp(12));device.setBackground(bg(PANEL,18));
        TextView icon=text("▣",25,ACCENT,true);device.addView(icon,new LinearLayout.LayoutParams(dp(38),dp(46)));
        LinearLayout info=column();deviceName=text("Not connected",16,WHITE,true);add(info,deviceName,23);TextView sub=text("Bluetooth HID  •  Tap to pair",12,MUTED,false);add(info,sub,20);device.addView(info,new LinearLayout.LayoutParams(0,dp(46),1));TextView arrow=text("›",28,MUTED,false);device.addView(arrow);device.setOnClickListener(v->showDiscovery());add(page,device,72);
        gap(page,22);
        LinearLayout power=row();TextView p=key("⏻",55,()->command("Power"));p.setTextColor(0xffff8888);p.setTextSize(29);p.setBackground(bg(0xff39242b,18));power.addView(p,new LinearLayout.LayoutParams(dp(64),dp(55)));
        View spacer=new View(this);power.addView(spacer,new LinearLayout.LayoutParams(0,1,1));TextView input=key("▣  INPUT",55,()->command("Input"));input.setTextSize(13);power.addView(input,new LinearLayout.LayoutParams(dp(115),dp(55)));add(page,power,55);
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
        message=text("Bluetooth only • Pair using projector accessory search",12,MUTED,false);message.setGravity(Gravity.CENTER);add(page,message,65);
        TextView diagnostics=key("Bluetooth diagnostics",48,()->new AlertDialog.Builder(this).setTitle("Bluetooth diagnostics").setMessage("Android API: "+Build.VERSION.SDK_INT+"\nPermissions: "+hasPermissions()+"\nHID registered: "+registered+"\nConnected: "+(connected!=null)+"\nLast status: "+message.getText()).setPositiveButton("Close",null).show());add(page,diagnostics,48);
    }
    private String mapLabel(String label){
      if(label.contains("Back"))return "Back";if(label.contains("Home"))return "Home";
      if(label.contains("Menu"))return "Menu";if(label.contains("Mute"))return "Mute";
      if(label.contains("＋"))return "Volume+";if(label.contains("－"))return "Volume-";
      return label;
    }
    private class Dpad extends View{
        Paint paint=new Paint(3);Dpad(Context c){super(c);setLayerType(View.LAYER_TYPE_SOFTWARE,null);setOnTouchListener((v,event)->{if(event.getAction()==MotionEvent.ACTION_UP){float x=event.getX()-getWidth()/2f,y=event.getY()-getHeight()/2f;float a=Math.abs(x),b=Math.abs(y);String k=(a<dp(42)&&b<dp(42))?"OK":a>b?(x>0?"Right":"Left"):(y>0?"Down":"Up");command(k);return true;}return true;});}
        @Override protected void onDraw(Canvas c){super.onDraw(c);float cx=getWidth()/2f,cy=getHeight()/2f,r=Math.min(cx,cy)-dp(3);
            paint.setStyle(Paint.Style.FILL);paint.setColor(PANEL);paint.setShadowLayer(dp(8),0,dp(4),0x55000000);c.drawCircle(cx,cy,r,paint);paint.clearShadowLayer();
            paint.setColor(0xff303a4c);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(1));c.drawCircle(cx,cy,r-dp(2),paint);paint.setStyle(Paint.Style.FILL);
            paint.setColor(SURFACE);c.drawCircle(cx,cy,dp(52),paint);paint.setColor(ACCENT);c.drawCircle(cx,cy,dp(39),paint);
            paint.setTextAlign(Paint.Align.CENTER);paint.setTypeface(Typeface.create("sans-serif-medium",Typeface.BOLD));paint.setTextSize(dp(18));paint.setColor(BG);c.drawText("OK",cx,cy+dp(6),paint);
            paint.setColor(WHITE);paint.setTextSize(dp(26));c.drawText("⌃",cx,cy-dp(75),paint);c.drawText("⌄",cx,cy+dp(91),paint);c.drawText("‹",cx-dp(83),cy+dp(9),paint);c.drawText("›",cx+dp(83),cy+dp(9),paint);
        }
    }
    @Override protected void onDestroy(){
      destroyed=true;
      try{if(hid!=null){if(registered)hid.unregisterApp();if(adapter!=null)adapter.closeProfileProxy(BluetoothProfile.HID_DEVICE,hid);}}catch(Exception ignored){}
      destroyed=true; handler.removeCallbacksAndMessages(null); super.onDestroy();
    }
}
