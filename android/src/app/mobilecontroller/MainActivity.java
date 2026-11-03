package app.mobilecontroller;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHidDevice;
import android.bluetooth.BluetoothHidDeviceAppSdpSettings;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;

/** Phone = Bluetooth keyboard with the five keys that matter. Laptop needs no app. */
public class MainActivity extends Activity {
    // HID usage IDs, keyboard page 0x07 of the USB HID Usage Tables.
    static final int RIGHT = 0x4F, LEFT = 0x50, DOWN = 0x51, UP = 0x52, SPACE = 0x2C;
    static final int KEY_BG = 0xFF1C1C1C;

    // Standard keyboard report, 8 bytes after report ID 1: [modifiers, reserved, key1..key6].
    static final byte[] DESCRIPTOR = {
        0x05, 0x01,              // Usage Page (Generic Desktop)
        0x09, 0x06,              // Usage (Keyboard)
        (byte) 0xA1, 0x01,       // Collection (Application)
        (byte) 0x85, 0x01,       //   Report ID (1)
        0x05, 0x07,              //   Usage Page (Keyboard)
        0x19, (byte) 0xE0,       //   Usage Minimum (Left Ctrl)
        0x29, (byte) 0xE7,       //   Usage Maximum (Right GUI)
        0x15, 0x00,              //   Logical Minimum (0)
        0x25, 0x01,              //   Logical Maximum (1)
        0x75, 0x01,              //   Report Size (1)
        (byte) 0x95, 0x08,       //   Report Count (8)
        (byte) 0x81, 0x02,       //   Input (Data, Variable, Absolute): modifier bits
        0x75, 0x08,              //   Report Size (8)
        (byte) 0x95, 0x01,       //   Report Count (1)
        (byte) 0x81, 0x01,       //   Input (Constant): reserved byte
        0x75, 0x08,              //   Report Size (8)
        (byte) 0x95, 0x06,       //   Report Count (6)
        0x15, 0x00,              //   Logical Minimum (0)
        0x25, 0x65,              //   Logical Maximum (101)
        0x19, 0x00,              //   Usage Minimum (0)
        0x29, 0x65,              //   Usage Maximum (101)
        (byte) 0x81, 0x00,       //   Input (Data, Array): up to 6 keys held at once
        (byte) 0xC0              // End Collection
    };

    TextView status;
    BluetoothAdapter adapter;
    BluetoothHidDevice hid;   // Android's built-in "be a Bluetooth keyboard" profile
    BluetoothDevice host;     // the laptop, once connected
    final ArrayList<Integer> held = new ArrayList<>();  // keys under a finger right now

    final BluetoothHidDevice.Callback callback = new BluetoothHidDevice.Callback() {
        @Override public void onAppStatusChanged(BluetoothDevice plugged, boolean registered) {
            if (registered) connect(null);  // ignore "plugged": it can be a stale, wrong host
            else say("Keyboard stopped. Open the app again.");
        }

        @Override public void onConnectionStateChanged(BluetoothDevice device, int state) {
            if (state == BluetoothProfile.STATE_CONNECTED) {
                host = device;
                getPreferences(MODE_PRIVATE).edit().putString("host", device.getAddress()).apply();
                say("Connected to " + device.getName());
            } else if (state == BluetoothProfile.STATE_DISCONNECTED) {
                host = null;
                say("Not connected. Tap here to retry.");
            }
        }
    };

    @Override protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
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
        status.setOnClickListener(v -> pick());
        root.addView(status);

        root.addView(key("▲", UP), tall(3));
        LinearLayout mid = new LinearLayout(this);
        mid.addView(key("◀\uFE0E", LEFT), wide(1));  // FE0E: plain arrow, not emoji
        mid.addView(key("▶\uFE0E", RIGHT), wide(1));
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
        BluetoothManager bm = getSystemService(BluetoothManager.class);
        adapter = bm == null ? null : bm.getAdapter();
        if (adapter == null || !adapter.isEnabled()) {
            say("Turn Bluetooth on, then open the app again");
            return;
        }
        boolean ok = adapter.getProfileProxy(this, new BluetoothProfile.ServiceListener() {
            @Override public void onServiceConnected(int profile, BluetoothProfile proxy) {
                hid = (BluetoothHidDevice) proxy;
                BluetoothHidDeviceAppSdpSettings sdp = new BluetoothHidDeviceAppSdpSettings("Controller",
                        "Phone keys", "mobile-controller", BluetoothHidDevice.SUBCLASS1_KEYBOARD, DESCRIPTOR);
                if (!hid.registerApp(sdp, null, null, getMainExecutor(), callback))
                    say("Another keyboard app is running. Close it, then open this one again.");
            }

            @Override public void onServiceDisconnected(int profile) {
                hid = null;
                host = null;
                say("Bluetooth stopped");
            }
        }, BluetoothProfile.HID_DEVICE);
        if (!ok) say("This phone can't act as a Bluetooth keyboard");
        else say("Starting keyboard...");
    }

    void connect(BluetoothDevice target) {
        if (hid == null) return;
        if (target == null) target = laptop();
        if (target == null) {
            say(adapter.getBondedDevices().isEmpty() ? "Pair with the laptop first (see README)"
                    : "Tap here to pick your laptop");
            return;
        }
        say("Connecting to " + target.getName() + "...");
        hid.connect(target);
    }

    /** List paired devices; the one tapped becomes the laptop. */
    void pick() {
        if (hid == null) return;
        final BluetoothDevice[] all = adapter.getBondedDevices().toArray(new BluetoothDevice[0]);
        String[] names = new String[all.length];
        for (int i = 0; i < all.length; i++) names[i] = all[i].getName();
        new AlertDialog.Builder(this).setTitle("Which one is your laptop?").setItems(names, (d, i) -> {
            for (BluetoothDevice other : hid.getDevicesMatchingConnectionStates(new int[] {
                    BluetoothProfile.STATE_CONNECTING, BluetoothProfile.STATE_CONNECTED}))
                if (!other.equals(all[i])) hid.disconnect(other);  // drop a stuck or wrong host
            getPreferences(MODE_PRIVATE).edit().putString("host", all[i].getAddress()).apply();
            connect(all[i]);
        }).show();
    }

    /** Last laptop used, else the only paired computer. Never guess between two. */
    BluetoothDevice laptop() {
        String last = getPreferences(MODE_PRIVATE).getString("host", "");
        BluetoothDevice computer = null;
        int computers = 0;
        for (BluetoothDevice d : adapter.getBondedDevices()) {
            if (d.getAddress().equals(last)) return d;
            BluetoothClass c = d.getBluetoothClass();
            if (c != null && c.getMajorDeviceClass() == BluetoothClass.Device.Major.COMPUTER) {
                computer = d;
                computers++;
            }
        }
        return computers == 1 ? computer : null;
    }

    @Override protected void onResume() {
        super.onResume();
        if (hid != null && host == null) connect(null);  // back on screen = wants to use it
    }

    @Override protected void onDestroy() {
        if (hid != null) {
            hid.unregisterApp();
            adapter.closeProfileProxy(BluetoothProfile.HID_DEVICE, hid);
        }
        super.onDestroy();
    }

    /** A key fires on finger down and lets go on finger up, like a real key. */
    TextView key(String label, int code) {
        TextView k = new TextView(this);
        k.setText(label);
        k.setTextSize(label.equals("SPACE") ? 24 : 44);
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

    /** Send which keys are held now. Laptop repeats a held key itself, like a real keyboard. */
    void press(int code, boolean down) {
        if (down) {
            if (!held.contains(code)) held.add(code);
        } else {
            held.remove((Integer) code);
        }
        if (hid == null || host == null) return;
        byte[] report = new byte[8];
        for (int i = 0; i < held.size() && i < 6; i++) report[2 + i] = (byte) (int) held.get(i);
        hid.sendReport(host, 1, report);
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
