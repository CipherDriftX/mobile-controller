package app.mobilecontroller;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Phone = Bluetooth keyboard with the five keys that matter. Laptop needs no app. */
public class MainActivity extends Activity {
    // HID usage IDs, keyboard page 0x07 of the USB HID Usage Tables.
    static final int RIGHT = 0x4F, LEFT = 0x50, DOWN = 0x51, UP = 0x52, SPACE = 0x2C;
    static final int KEY_BG = 0xFF1C1C1C;

    TextView status;

    @Override protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);
        root.setFitsSystemWindows(true);
        root.setPadding(dp(4), dp(4), dp(4), dp(4));

        status = new TextView(this);
        status.setTextColor(0xFF999999);
        status.setTextSize(16);
        status.setGravity(Gravity.CENTER);
        status.setPadding(0, dp(12), 0, dp(12));
        root.addView(status);

        root.addView(key("▲", UP), tall(3));
        LinearLayout mid = new LinearLayout(this);
        mid.addView(key("◀", LEFT), wide(1));
        mid.addView(key("▶", RIGHT), wide(1));
        root.addView(mid, tall(2));
        root.addView(key("▼", DOWN), tall(3));
        root.addView(key("SPACE", SPACE), tall(1.5f));
        setContentView(root);

        if (Build.VERSION.SDK_INT >= 31
                && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            say("Allow Bluetooth so the keys can reach the laptop");
            requestPermissions(new String[] {Manifest.permission.BLUETOOTH_CONNECT}, 1);
        } else {
            start();
        }
    }

    @Override public void onRequestPermissionsResult(int req, String[] perms, int[] results) {
        if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) start();
        else say("No Bluetooth permission. Allow it in Settings > Apps > Controller.");
    }

    void start() {
        say("Starting...");
    }

    /** A key fires on finger down and lets go on finger up, like a real key. */
    TextView key(String label, int code) {
        TextView k = new TextView(this);
        k.setText(label);
        k.setTextSize(label.length() > 1 ? 24 : 44);
        k.setGravity(Gravity.CENTER);
        k.setTextColor(Color.WHITE);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(20));
        bg.setColor(KEY_BG);
        k.setBackground(bg);
        k.setOnTouchListener((v, e) -> {
            int a = e.getActionMasked();
            if (a == MotionEvent.ACTION_DOWN) {
                press(code, true);
                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                bg.setColor(Color.WHITE);
                k.setTextColor(Color.BLACK);
            } else if (a == MotionEvent.ACTION_UP || a == MotionEvent.ACTION_CANCEL) {
                press(code, false);
                bg.setColor(KEY_BG);
                k.setTextColor(Color.WHITE);
            }
            return true;
        });
        return k;
    }

    void press(int code, boolean down) {
    }

    void say(String text) { status.setText(text); }

    LinearLayout.LayoutParams tall(float w) { return cell(LinearLayout.LayoutParams.MATCH_PARENT, 0, w); }
    LinearLayout.LayoutParams wide(float w) { return cell(0, LinearLayout.LayoutParams.MATCH_PARENT, w); }

    LinearLayout.LayoutParams cell(int width, int height, float w) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(width, height, w);
        p.setMargins(dp(4), dp(4), dp(4), dp(4));
        return p;
    }

    int dp(float v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
