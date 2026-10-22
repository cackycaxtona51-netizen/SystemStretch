package com.caxton.systemstretch;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataOutputStream;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.parseColor("#0B0F17"));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        int pad = dp(24);
        root.setPadding(pad, dp(48), pad, pad);

        TextView title = new TextView(this);
        title.setText("SYSTEM STRETCH ENGINE");
        title.setTextColor(Color.parseColor("#00E676"));
        title.setTextSize(22f);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        TextView sub = new TextView(this);
        sub.setText("SM-A135F • SurfaceFlinger Hardware Stretch\nZero Black Bars • 100% Native Touch");
        sub.setTextColor(Color.parseColor("#94A3B8"));
        sub.setTextSize(13f);
        sub.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        subLp.setMargins(0, dp(8), 0, dp(28));
        root.addView(sub, subLp);

        // 16:10 Supersampled HD (Zero quality loss)
        root.addView(makeBtn(
                "16:10 WIDE • ULTRA HD (1505x2408)",
                "#1E293B", "#00E676",
                () -> applyStretch("on", "1505x2408", false)
        ));

        // 16:10 Fast 1080p
        root.addView(makeBtn(
                "16:10 WIDE • 1080P (1080x1728)",
                "#1E293B", "#38BDF8",
                () -> applyStretch("on", "1080x1728", false)
        ));

        // 4:3 iPad Pro Supersampled HD
        root.addView(makeBtn(
                "4:3 iPAD PRO • ULTRA HD (1440x1920)",
                "#1E293B", "#A855F7",
                () -> applyStretch("on", "1440x1920", false)
        ));

        // 4:3 iPad Fast 1080p
        root.addView(makeBtn(
                "4:3 iPAD • 1080P (1080x1440)",
                "#1E293B", "#F59E0B",
                () -> applyStretch("on", "1080x1440", false)
        ));

        // 5:4 Ultra Hitbox HD
        root.addView(makeBtn(
                "5:4 ULTRA HITBOX • HD (1440x1800)",
                "#1E293B", "#EC4899",
                () -> applyStretch("on", "1440x1800", false)
        ));

        // 16:10 HD + Launch CODM
        root.addView(makeBtn(
                "ENGAGE 16:10 HD & LAUNCH CODM",
                "#065F46", "#FFFFFF",
                () -> applyStretch("on", "1505x2408", true)
        ));

        // Reset back to stock 20:9
        root.addView(makeBtn(
                "RESET TO NATIVE 20:9 (1080x2408)",
                "#7F1D1D", "#FCA5A5",
                () -> applyStretch("off", "reset", false)
        ));

        scroll.addView(root);
        setContentView(scroll);
    }

    private Button makeBtn(String text, String bgHex, String strokeHex, Runnable action) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        b.setTextSize(14f);
        b.setTypeface(Typeface.DEFAULT_BOLD);

        GradientDrawable gd = new GradientDrawable();
        gd.setColor(Color.parseColor(bgHex));
        gd.setCornerRadius(dp(14));
        gd.setStroke(dp(2), Color.parseColor(strokeHex));
        b.setBackground(gd);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(58));
        lp.setMargins(0, 0, 0, dp(14));
        b.setLayoutParams(lp);
        b.setOnClickListener(v -> action.run());
        return b;
    }

    private void applyStretch(String hookState, String wmSize, boolean launchCodm) {
        new Thread(() -> {
            try {
                Process su = Runtime.getRuntime().exec("su");
                DataOutputStream os = new DataOutputStream(su.getOutputStream());

                os.writeBytes("setprop debug.sys.stretch " + hookState + "\n");
                os.writeBytes("wm size " + wmSize + "\n");

                if ("off".equals(hookState)) {
                    os.writeBytes("settings put global policy_control null\n");
                } else {
                    os.writeBytes("settings put global policy_control immersive.full=*\n");
                }

                if (launchCodm) {
                    os.writeBytes("for pkg in com.activision.callofduty.shooter com.garena.game.codm; do " +
                            "if pm path $pkg >/dev/null 2>&1; then monkey -p $pkg -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1; break; fi; done\n");
                }

                os.writeBytes("exit\n");
                os.flush();
                su.waitFor();

                runOnUiThread(() -> Toast.makeText(this, "Applied: " + wmSize, Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Root Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
