package com.example.wanboremote;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.*;
import android.graphics.Typeface;
import android.text.InputType;
import java.net.*;

/** UI and connectivity diagnostics only. Does not claim projector control. */
public class MainActivity extends Activity {
    LinearLayout root;
    TextView status;
    EditText ip;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(22,25,22,20); root.setBackgroundColor(Color.rgb(17,19,24));
        ScrollView scroll = new ScrollView(this); scroll.addView(root); setContentView(scroll);
        TextView title = new TextView(this); title.setText("WANBO  •  WPC024"); title.setTextColor(Color.WHITE); title.setTextSize(23); title.setTypeface(null, Typeface.BOLD); title.setGravity(Gravity.CENTER); root.addView(title);
        status = new TextView(this); status.setText("Prototype: control protocol not verified"); status.setTextColor(0xffcbd5e1); status.setGravity(Gravity.CENTER); status.setPadding(0,16,0,18); root.addView(status);
        addRow("⏻ Power", "▣ Input");
        addRow("◉ Assistant", "⚙ Settings");
        button("▲"); addRow("◀", "OK", "▶"); button("▼");
        addRow("↩ Back", "⌂ Home", "☷ Menu");
        addRow("＋ Volume", "－ Volume", "Mute");
        addRow("YouTube", "Netflix"); addRow("Prime Video", "Disney+");
        TextView help = new TextView(this); help.setText("Projector IP address (Settings → Network)"); help.setTextColor(Color.WHITE); help.setPadding(0,20,0,8); root.addView(help);
        ip = new EditText(this); ip.setSingleLine(true); ip.setTextColor(Color.WHITE); ip.setHintTextColor(0xff94a3b8); ip.setHint("192.168.1.100"); ip.setInputType(InputType.TYPE_CLASS_PHONE); root.addView(ip);
        Button test = button("Test network reachability"); test.setOnClickListener(v -> probe());
        TextView disclaimer = new TextView(this); disclaimer.setText("This starter project does not send remote commands. Wi-Fi access alone is insufficient: the WPC024 control protocol must be identified and implemented."); disclaimer.setTextColor(0xff94a3b8); disclaimer.setPadding(0,16,0,12); root.addView(disclaimer);
    }
    Button button(String label) {
        Button b = new Button(this); b.setText(label); b.setTextColor(Color.WHITE); b.setAllCaps(false); b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xff293241));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1,58); p.setMargins(4,5,4,5); root.addView(b,p);
        b.setOnClickListener(v -> status.setText("“"+label+"” is a UI placeholder — no command sent")); return b;
    }
    void addRow(String... names) {
        LinearLayout row = new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL); root.addView(row);
        for(String name:names) {
            Button b = new Button(this); b.setText(name); b.setAllCaps(false); b.setTextColor(Color.WHITE); b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xff293241));
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0,58,1); p.setMargins(3,5,3,5); row.addView(b,p);
            b.setOnClickListener(v -> status.setText("“"+name+"” is a UI placeholder — no command sent"));
        }
    }
    void probe() {
        final String host = ip.getText().toString().trim();
        if(host.isEmpty()) { status.setText("Enter the projector IP first"); return; }
        status.setText("Testing reachability…");
        new Thread(() -> {
            StringBuilder found = new StringBuilder();
            for(int port:new int[]{5555,6466,6467,8008,8009}) {
                try(Socket s = new Socket()) { s.connect(new InetSocketAddress(host,port),650); found.append(port).append(" "); } catch(Exception ignored){}
            }
            final String result = found.length()>0 ? "Open ports: "+found+"(not proof of remote compatibility)" : "No tested ports responded. This does not prove the projector is offline.";
            runOnUiThread(() -> status.setText(result));
        }).start();
    }
}
