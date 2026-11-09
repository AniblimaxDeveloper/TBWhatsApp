package com.tblabs.tbwhatsapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;

/**
 * TBWhatsApp 2.0 - independent, offline-first messenger UI prototype.
 * Local chats, statuses and preferences are stored on this device.
 * Real multi-device messaging, calls and push notifications require a backend.
 */
public class MainActivity extends Activity {
    private static final String PREFS = "tbwhatsapp_v2_store";
    private static final String[] TAB_NAMES = {"Chat", "Update", "Panggilan", "Komunitas", "Setelan"};
    private static final String[] TAB_ICONS = {"☏", "◉", "☎", "▦", "⚙"};

    private SharedPreferences prefs;
    private LinearLayout root;
    private LinearLayout pageBody;
    private LinearLayout chatRows;
    private final ArrayList<ChatItem> chats = new ArrayList<>();
    private final HashMap<String, ArrayList<MessageItem>> conversations = new HashMap<>();
    private final ArrayList<String> statuses = new ArrayList<>();
    private final ArrayList<String> communities = new ArrayList<>();

    private boolean darkMode;
    private boolean hidePreview;
    private boolean compactRows;
    private boolean readReceipts;
    private boolean appLockEnabled;
    private boolean lockPassed;
    private int selectedTab = 0;
    private String currentChat;
    private String activeFilter = "Semua";
    private String searchQuery = "";
    private String profileName = "Pengguna TB";
    private String profileAbout = "Tersedia di TBWhatsApp";
    private String profilePinHash = "";
    private String profilePinSalt = "";

    private int bg() { return darkMode ? 0xFF101A1C : 0xFFF3F7F6; }
    private int surface() { return darkMode ? 0xFF1B292B : 0xFFFFFFFF; }
    private int textColor() { return darkMode ? 0xFFEAF2F2 : 0xFF172629; }
    private int muted() { return darkMode ? 0xFF9BAEAF : 0xFF718184; }
    private int accent() { return 0xFF087F73; }
    private int dividerColor() { return darkMode ? 0xFF304144 : 0xFFE5ECEC; }
    private int mineBubble() { return darkMode ? 0xFF075E54 : 0xFFD9F4EA; }
    private int otherBubble() { return darkMode ? 0xFF233437 : 0xFFFFFFFF; }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        loadData();
        if (appLockEnabled) showUnlockDialog();
        else lockPassed = true;
        buildScreen();
    }

    private void loadData() {
        darkMode = prefs.getBoolean("dark_mode", false);
        hidePreview = prefs.getBoolean("hide_preview", false);
        compactRows = prefs.getBoolean("compact_rows", false);
        readReceipts = prefs.getBoolean("read_receipts", true);
        appLockEnabled = prefs.getBoolean("app_lock", false);
        profileName = prefs.getString("profile_name", "Pengguna TB");
        profileAbout = prefs.getString("profile_about", "Tersedia di TBWhatsApp");
        profilePinHash = prefs.getString("app_pin_hash", "");
        profilePinSalt = prefs.getString("app_pin_salt", "");
        appLockEnabled = !profilePinHash.isEmpty() && !profilePinSalt.isEmpty();

        try {
            JSONArray chatArray = new JSONArray(prefs.getString("chats_json", "[]"));
            for (int i = 0; i < chatArray.length(); i++) {
                JSONObject o = chatArray.getJSONObject(i);
                chats.add(new ChatItem(o.optString("name"), o.optString("preview"), o.optString("time"),
                        o.optInt("unread"), o.optBoolean("favorite"), o.optBoolean("group"),
                        o.optBoolean("pinned"), o.optBoolean("muted"), o.optBoolean("archived"),
                        o.optString("initials"), o.optInt("avatar", 0xFFB7C9E8)));
            }
            JSONObject messages = new JSONObject(prefs.getString("messages_json", "{}"));
            Iterator<String> keys = messages.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                JSONArray array = messages.optJSONArray(key);
                ArrayList<MessageItem> list = new ArrayList<>();
                if (array != null) for (int i = 0; i < array.length(); i++) {
                    JSONObject m = array.optJSONObject(i);
                    if (m != null) list.add(new MessageItem(m.optBoolean("mine"), m.optString("text"), m.optString("time")));
                }
                conversations.put(key, list);
            }
            JSONArray statusArray = new JSONArray(prefs.getString("statuses_json", "[]"));
            for (int i = 0; i < statusArray.length(); i++) statuses.add(statusArray.optString(i));
            JSONArray communityArray = new JSONArray(prefs.getString("communities_json", "[]"));
            for (int i = 0; i < communityArray.length(); i++) communities.add(communityArray.optString(i));
        } catch (JSONException ignored) { }

        if (chats.isEmpty()) seedData();
        if (communities.isEmpty()) {
            communities.add("Teman TBWhatsApp");
            communities.add("Komunitas Kreator");
        }
        saveData();
    }

    private void seedData() {
        addSeed("Nadia Putri", "Oke, sampai nanti ya!", "18.42", 2, true, false, false, false, false, "NP", 0xFFE7BFA9);
        addSeed("Raka Pratama", "Foto kemarin sudah aku kirim", "17.56", 0, true, false, true, false, false, "RP", 0xFFB7C9E8);
        addSeed("Keluarga Besar", "Ibu: Jangan lupa makan", "16.20", 4, false, true, false, false, false, "KB", 0xFFE7D49E);
        addSeed("Dina Aulia", "Makasih banyak ✨", "Kemarin", 0, false, false, false, false, false, "DA", 0xFFE8B9CC);
        addSeed("Tim Project", "Bima: Meeting jam 8 besok", "Senin", 1, false, true, false, true, false, "TP", 0xFFB4D9C9);
        addSeed("Fajar", "Voice note (contoh)", "Minggu", 0, true, false, false, false, false, "F", 0xFFC8B8E7);
        putMessages("Nadia Putri", new MessageItem(false, "Hai! Lagi di mana?", "18.39"), new MessageItem(true, "Lagi di rumah. Kenapa?", "18.40"), new MessageItem(false, "Oke, sampai nanti ya!", "18.42"));
        putMessages("Raka Pratama", new MessageItem(false, "Bro, file-nya sudah jadi.", "17.50"), new MessageItem(true, "Mantap, kirim ke sini ya.", "17.52"), new MessageItem(false, "Foto kemarin sudah aku kirim", "17.56"));
        putMessages("Keluarga Besar", new MessageItem(false, "Besok kumpul jam berapa?", "16.15"), new MessageItem(true, "Sekitar jam 10 ya.", "16.18"), new MessageItem(false, "Ibu: Jangan lupa makan", "16.20"));
        putMessages("Dina Aulia", new MessageItem(true, "Terima kasih sudah membantu.", "Kemarin"), new MessageItem(false, "Makasih banyak ✨", "Kemarin"));
        putMessages("Tim Project", new MessageItem(false, "Bima: Meeting jam 8 besok", "Senin"));
        putMessages("Fajar", new MessageItem(false, "Voice note (contoh)", "Minggu"));
    }

    private void addSeed(String name, String preview, String time, int unread, boolean favorite, boolean group,
                         boolean pinned, boolean muted, boolean archived, String initials, int avatar) {
        chats.add(new ChatItem(name, preview, time, unread, favorite, group, pinned, muted, archived, initials, avatar));
    }

    private void putMessages(String name, MessageItem... items) {
        ArrayList<MessageItem> list = new ArrayList<>();
        for (MessageItem item : items) list.add(item);
        conversations.put(name, list);
    }

    private void saveData() {
        try {
            JSONArray chatArray = new JSONArray();
            for (ChatItem c : chats) {
                JSONObject o = new JSONObject();
                o.put("name", c.name); o.put("preview", c.preview); o.put("time", c.time);
                o.put("unread", c.unread); o.put("favorite", c.favorite); o.put("group", c.group);
                o.put("pinned", c.pinned); o.put("muted", c.muted); o.put("archived", c.archived);
                o.put("initials", c.initials); o.put("avatar", c.avatarColor);
                chatArray.put(o);
            }
            JSONObject messageObject = new JSONObject();
            for (String key : conversations.keySet()) {
                JSONArray arr = new JSONArray();
                for (MessageItem m : conversations.get(key)) {
                    JSONObject item = new JSONObject();
                    item.put("mine", m.mine); item.put("text", m.text); item.put("time", m.time);
                    arr.put(item);
                }
                messageObject.put(key, arr);
            }
            JSONArray statusArray = new JSONArray();
            for (String s : statuses) statusArray.put(s);
            JSONArray communityArray = new JSONArray();
            for (String s : communities) communityArray.put(s);
            prefs.edit().putString("chats_json", chatArray.toString())
                    .putString("messages_json", messageObject.toString())
                    .putString("statuses_json", statusArray.toString())
                    .putString("communities_json", communityArray.toString()).apply();
        } catch (JSONException ignored) { }
    }

    private void buildScreen() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(bg());
        setContentView(root);
        if (currentChat != null) buildConversationScreen();
        else {
            buildMainHeader();
            pageBody = new LinearLayout(this);
            pageBody.setOrientation(LinearLayout.VERTICAL);
            root.addView(pageBody, new LinearLayout.LayoutParams(-1, 0, 1));
            renderSelectedTab();
            buildBottomNavigation();
        }
        getWindow().setStatusBarColor(darkMode ? 0xFF0B4943 : 0xFF075E54);
        getWindow().setNavigationBarColor(surface());
        getWindow().getDecorView().setSystemUiVisibility(darkMode ? 0 : View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
    }

    private void buildMainHeader() {
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(17), dp(13), dp(17), dp(12));
        header.setBackgroundColor(surface());
        TextView logo = label("TB", 15, 0xFFFFFFFF, true);
        logo.setGravity(Gravity.CENTER);
        logo.setBackground(round(accent(), 15));
        header.addView(logo, new LinearLayout.LayoutParams(dp(43), dp(43)));
        LinearLayout titleBox = new LinearLayout(this);
        titleBox.setOrientation(LinearLayout.VERTICAL);
        titleBox.setPadding(dp(11), 0, 0, 0);
        titleBox.addView(label("TBWhatsApp", 20, textColor(), true));
        titleBox.addView(label("V2.0 • Independent Messenger", 10, muted(), false));
        header.addView(titleBox, new LinearLayout.LayoutParams(0, -2, 1));
        TextView add = label("＋", 26, accent(), false);
        add.setGravity(Gravity.CENTER);
        add.setBackground(round(bg(), 14));
        add.setOnClickListener(v -> showCreateMenu());
        header.addView(add, new LinearLayout.LayoutParams(dp(43), dp(43)));
        root.addView(header);
        addDivider(root);
    }

    private void buildBottomNavigation() {
        LinearLayout nav = new LinearLayout(this);
        nav.setBackgroundColor(surface());
        nav.setPadding(dp(3), dp(7), dp(3), dp(7));
        for (int i = 0; i < TAB_NAMES.length; i++) {
            final int index = i;
            LinearLayout item = new LinearLayout(this);
            item.setGravity(Gravity.CENTER);
            item.setOrientation(LinearLayout.VERTICAL);
            TextView icon = label(TAB_ICONS[i], 19, selectedTab == i ? accent() : muted(), true);
            TextView title = label(TAB_NAMES[i], 9, selectedTab == i ? accent() : muted(), selectedTab == i);
            item.addView(icon);
            item.addView(title);
            item.setPadding(0, dp(3), 0, dp(3));
            item.setOnClickListener(v -> {
                selectedTab = index;
                searchQuery = "";
                buildScreen();
            });
            nav.addView(item, new LinearLayout.LayoutParams(0, -2, 1));
        }
        addDivider(root);
        root.addView(nav);
    }

    private void renderSelectedTab() {
        pageBody.removeAllViews();
        if (selectedTab == 0) renderChatsTab();
        else if (selectedTab == 1) renderUpdatesTab();
        else if (selectedTab == 2) renderCallsTab();
        else if (selectedTab == 3) renderCommunitiesTab();
        else renderSettingsTab();
    }

    private void renderChatsTab() {
        LinearLayout searchWrap = new LinearLayout(this);
        searchWrap.setPadding(dp(15), dp(11), dp(15), dp(7));
        EditText search = new EditText(this);
        search.setSingleLine(true); search.setTextSize(14); search.setTextColor(textColor());
        search.setHintTextColor(muted()); search.setHint("⌕  Cari kontak atau pesan");
        search.setPadding(dp(14), 0, dp(14), 0); search.setBackground(round(surface(), 14));
        search.setText(searchQuery);
        searchWrap.addView(search, new LinearLayout.LayoutParams(-1, dp(45)));
        pageBody.addView(searchWrap);

        HorizontalScrollView filtersScroll = new HorizontalScrollView(this);
        filtersScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout filters = new LinearLayout(this);
        filters.setPadding(dp(15), dp(2), dp(15), dp(10));
        String[] choices = {"Semua", "Belum dibaca", "Favorit", "Grup", "Diarsipkan"};
        for (String option : choices) {
            TextView chip = label(option, 11, option.equals(activeFilter) ? 0xFFFFFFFF : muted(), true);
            chip.setPadding(dp(13), dp(8), dp(13), dp(8));
            chip.setBackground(round(option.equals(activeFilter) ? accent() : surface(), 30));
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-2, -2);
            cp.rightMargin = dp(7); filters.addView(chip, cp);
            chip.setOnClickListener(v -> { activeFilter = option; renderChatRows(); refreshFilterChips(filters, choices); });
        }
        filtersScroll.addView(filters);
        pageBody.addView(filtersScroll);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        chatRows = new LinearLayout(this); chatRows.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(chatRows);
        pageBody.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        renderChatRows();
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { searchQuery = s.toString(); renderChatRows(); }
            @Override public void afterTextChanged(Editable s) { }
        });
    }

    private void refreshFilterChips(LinearLayout filters, String[] choices) {
        for (int i = 0; i < filters.getChildCount(); i++) {
            TextView chip = (TextView) filters.getChildAt(i);
            String option = choices[i];
            chip.setTextColor(option.equals(activeFilter) ? 0xFFFFFFFF : muted());
            chip.setBackground(round(option.equals(activeFilter) ? accent() : surface(), 30));
        }
    }

    private void renderChatRows() {
        if (chatRows == null) return;
        chatRows.removeAllViews();
        ArrayList<ChatItem> visible = new ArrayList<>();
        for (ChatItem c : chats) {
            boolean filter = true;
            if ("Diarsipkan".equals(activeFilter)) filter = c.archived;
            else if (c.archived) filter = false;
            if ("Belum dibaca".equals(activeFilter)) filter = filter && c.unread > 0;
            if ("Favorit".equals(activeFilter)) filter = filter && c.favorite;
            if ("Grup".equals(activeFilter)) filter = filter && c.group;
            String q = searchQuery.toLowerCase(Locale.ROOT).trim();
            if (!q.isEmpty()) filter = filter && (c.name.toLowerCase(Locale.ROOT).contains(q) || c.preview.toLowerCase(Locale.ROOT).contains(q));
            if (filter) visible.add(c);
        }
        visible.sort((a, b) -> Boolean.compare(b.pinned, a.pinned));
        if (visible.isEmpty()) {
            addEmptyState(chatRows, "Tidak ada percakapan", "Buat chat baru untuk mulai menyimpan percakapan di perangkat ini.", "＋ Buat chat", this::showNewChatDialog);
            return;
        }
        for (ChatItem c : visible) chatRows.addView(chatRow(c));
    }

    private View chatRow(ChatItem c) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        int pad = compactRows ? 9 : 13;
        row.setPadding(dp(15), dp(pad), dp(13), dp(pad));
        row.setBackgroundColor(bg());
        TextView avatar = label(c.initials, 15, 0xFF243334, true);
        avatar.setGravity(Gravity.CENTER); avatar.setBackground(round(c.avatarColor, 50));
        row.addView(avatar, new LinearLayout.LayoutParams(dp(49), dp(49)));
        LinearLayout details = new LinearLayout(this); details.setOrientation(LinearLayout.VERTICAL);
        details.setPadding(dp(11), 0, dp(7), 0);
        LinearLayout titleLine = new LinearLayout(this); titleLine.setGravity(Gravity.CENTER_VERTICAL);
        TextView name = label(c.name, 14, textColor(), true);
        titleLine.addView(name, new LinearLayout.LayoutParams(0, -2, 1));
        titleLine.addView(label(c.pinned ? "📌" : "", 10, muted(), false));
        titleLine.addView(label(c.time, 10, muted(), false));
        details.addView(titleLine);
        LinearLayout previewLine = new LinearLayout(this); previewLine.setGravity(Gravity.CENTER_VERTICAL);
        String preview = hidePreview ? "Pratinjau disembunyikan" : c.preview;
        TextView snippet = label((c.group ? "▦ " : "") + (c.muted ? "🔕 " : "") + preview, 11, muted(), false);
        snippet.setMaxLines(1); snippet.setEllipsize(android.text.TextUtils.TruncateAt.END);
        previewLine.addView(snippet, new LinearLayout.LayoutParams(0, -2, 1));
        if (c.unread > 0) {
            TextView badge = label(String.valueOf(c.unread), 10, 0xFFFFFFFF, true);
            badge.setGravity(Gravity.CENTER); badge.setBackground(round(accent(), 20));
            previewLine.addView(badge, new LinearLayout.LayoutParams(dp(20), dp(20)));
        } else if (c.favorite) previewLine.addView(label("★", 12, 0xFFE0A52B, true));
        details.addView(previewLine);
        row.addView(details, new LinearLayout.LayoutParams(0, -2, 1));
        row.setOnClickListener(v -> openChat(c));
        row.setOnLongClickListener(v -> { showChatOptions(c); return true; });
        return row;
    }

    private void openChat(ChatItem c) {
        c.unread = 0;
        currentChat = c.name;
        if (!conversations.containsKey(currentChat)) conversations.put(currentChat, new ArrayList<>());
        saveData(); buildScreen();
    }

    private void showChatOptions(ChatItem c) {
        String[] actions = {c.pinned ? "Lepas pin" : "Sematkan chat", c.favorite ? "Hapus dari favorit" : "Tambah ke favorit",
                c.muted ? "Aktifkan notifikasi (lokal)" : "Bisukan notifikasi (lokal)", c.archived ? "Keluarkan dari arsip" : "Arsipkan chat", "Ekspor percakapan", "Hapus percakapan"};
        new AlertDialog.Builder(this).setTitle(c.name).setItems(actions, (d, which) -> {
            if (which == 0) c.pinned = !c.pinned;
            else if (which == 1) c.favorite = !c.favorite;
            else if (which == 2) c.muted = !c.muted;
            else if (which == 3) c.archived = !c.archived;
            else if (which == 4) { shareConversation(c.name); return; }
            else if (which == 5) {
                new AlertDialog.Builder(this).setTitle("Hapus percakapan?").setMessage("Pesan lokal untuk " + c.name + " akan dihapus.")
                        .setNegativeButton("Batal", null).setPositiveButton("Hapus", (dialog, w) -> {
                            chats.remove(c); conversations.remove(c.name); saveData(); renderChatRows();
                        }).show();
                return;
            }
            saveData(); renderChatRows();
        }).show();
    }

    private void renderUpdatesTab() {
        addSectionTitle(pageBody, "Pembaruan", "Status teks lokal pada perangkat ini");
        LinearLayout own = new LinearLayout(this); own.setGravity(Gravity.CENTER_VERTICAL); own.setPadding(dp(15), dp(12), dp(15), dp(12)); own.setBackgroundColor(surface());
        TextView avatar = label("＋", 23, accent(), true); avatar.setGravity(Gravity.CENTER); avatar.setBackground(round(bg(), 50));
        own.addView(avatar, new LinearLayout.LayoutParams(dp(54), dp(54)));
        LinearLayout labelBox = new LinearLayout(this); labelBox.setOrientation(LinearLayout.VERTICAL); labelBox.setPadding(dp(12), 0, 0, 0);
        labelBox.addView(label("Status saya", 14, textColor(), true)); labelBox.addView(label("Ketuk untuk menambahkan pembaruan", 11, muted(), false));
        own.addView(labelBox, new LinearLayout.LayoutParams(0, -2, 1));
        TextView create = label("＋", 22, accent(), true); own.addView(create); own.setOnClickListener(v -> showStatusDialog());
        pageBody.addView(own); addDivider(pageBody);
        addSectionTitle(pageBody, "Pembaruan terbaru", "Status contoh dan status yang kamu buat");
        ScrollView scroll = new ScrollView(this); LinearLayout list = new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL); scroll.addView(list);
        pageBody.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        if (statuses.isEmpty()) addEmptyState(list, "Belum ada status", "Tambahkan status teks untuk melihat tampilannya di sini.", "Buat status", this::showStatusDialog);
        else for (int i = statuses.size() - 1; i >= 0; i--) {
            final int idx = i;
            LinearLayout r = new LinearLayout(this); r.setGravity(Gravity.CENTER_VERTICAL); r.setPadding(dp(16), dp(12), dp(16), dp(12));
            TextView a = label("TB", 12, 0xFFFFFFFF, true); a.setGravity(Gravity.CENTER); a.setBackground(round(accent(), 50)); r.addView(a, new LinearLayout.LayoutParams(dp(45), dp(45)));
            LinearLayout t = new LinearLayout(this); t.setOrientation(LinearLayout.VERTICAL); t.setPadding(dp(12), 0, 0, 0);
            t.addView(label(profileName + " • Status saya", 13, textColor(), true)); TextView body = label(statuses.get(i), 11, muted(), false); body.setMaxLines(2); t.addView(body);
            r.addView(t, new LinearLayout.LayoutParams(0, -2, 1)); r.setOnClickListener(v -> showInfoDialog("Status", statuses.get(idx)));
            r.setOnLongClickListener(v -> { new AlertDialog.Builder(this).setItems(new String[]{"Hapus status"}, (d, w) -> { statuses.remove(idx); saveData(); renderUpdatesTab(); }).show(); return true; });
            list.addView(r); addDivider(list);
        }
    }

    private void renderCallsTab() {
        addSectionTitle(pageBody, "Panggilan", "Riwayat contoh • panggilan online belum terhubung");
        ScrollView scroll = new ScrollView(this); LinearLayout list = new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL); scroll.addView(list);
        pageBody.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        String[][] records = {{"Nadia Putri", "↗ Panggilan suara keluar", "Hari ini • 18.20"}, {"Raka Pratama", "↙ Panggilan video masuk", "Hari ini • 15.10"}, {"Keluarga Besar", "↗ Panggilan suara grup", "Kemarin • 20.42"}, {"Dina Aulia", "↙ Panggilan tidak terjawab", "Kemarin • 09.15"}};
        for (String[] record : records) {
            LinearLayout row = new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL); row.setPadding(dp(16), dp(13), dp(16), dp(13));
            TextView avatar = label(initials(record[0]), 13, textColor(), true); avatar.setGravity(Gravity.CENTER); avatar.setBackground(round(0xFFBBD7CE, 50));
            row.addView(avatar, new LinearLayout.LayoutParams(dp(47), dp(47)));
            LinearLayout info = new LinearLayout(this); info.setOrientation(LinearLayout.VERTICAL); info.setPadding(dp(11), 0, 0, 0);
            info.addView(label(record[0], 14, textColor(), true)); info.addView(label(record[1], 11, muted(), false)); info.addView(label(record[2], 10, muted(), false));
            row.addView(info, new LinearLayout.LayoutParams(0, -2, 1));
            TextView call = label("☎", 20, accent(), true); row.addView(call); call.setOnClickListener(v -> toast("Fitur panggilan memerlukan backend dan akun pengguna."));
            list.addView(row); addDivider(list);
        }
        TextView note = label("Riwayat di atas adalah data contoh, bukan log panggilan perangkat.", 10, muted(), false);
        note.setPadding(dp(16), dp(12), dp(16), dp(12)); list.addView(note);
    }

    private void renderCommunitiesTab() {
        addSectionTitle(pageBody, "Komunitas", "Ruang kelompok lokal untuk mengatur grup");
        LinearLayout create = actionCard("＋  Buat komunitas", "Buat ruang komunitas contoh di aplikasi ini");
        create.setOnClickListener(v -> showCommunityDialog()); pageBody.addView(create);
        ScrollView scroll = new ScrollView(this); LinearLayout list = new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL); scroll.addView(list);
        pageBody.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        for (int i = 0; i < communities.size(); i++) {
            String name = communities.get(i); final int index = i;
            LinearLayout row = new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL); row.setPadding(dp(16), dp(14), dp(16), dp(14));
            TextView icon = label("▦", 20, accent(), true); icon.setGravity(Gravity.CENTER); icon.setBackground(round(0xFFDCEDE8, 14)); row.addView(icon, new LinearLayout.LayoutParams(dp(49), dp(49)));
            LinearLayout info = new LinearLayout(this); info.setOrientation(LinearLayout.VERTICAL); info.setPadding(dp(12), 0, 0, 0);
            info.addView(label(name, 14, textColor(), true)); info.addView(label("Komunitas lokal • belum tersinkron", 10, muted(), false));
            row.addView(info, new LinearLayout.LayoutParams(0, -2, 1));
            row.setOnClickListener(v -> showInfoDialog(name, "Ini adalah ruang komunitas lokal. Sinkronisasi anggota dan pesan memerlukan backend."));
            row.setOnLongClickListener(v -> { new AlertDialog.Builder(this).setItems(new String[]{"Hapus komunitas"}, (d, w) -> { communities.remove(index); saveData(); renderCommunitiesTab(); }).show(); return true; });
            list.addView(row); addDivider(list);
        }
    }

    private void renderSettingsTab() {
        addSectionTitle(pageBody, "Pengaturan", "Kelola pengalaman TBWhatsApp di perangkat ini");
        ScrollView scroll = new ScrollView(this); LinearLayout list = new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL); list.setPadding(0, 0, 0, dp(18)); scroll.addView(list);
        pageBody.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout profile = new LinearLayout(this); profile.setGravity(Gravity.CENTER_VERTICAL); profile.setPadding(dp(16), dp(14), dp(16), dp(14)); profile.setBackgroundColor(surface());
        TextView avatar = label(initials(profileName), 15, 0xFF173A35, true); avatar.setGravity(Gravity.CENTER); avatar.setBackground(round(0xFFBBD7CE, 50)); profile.addView(avatar, new LinearLayout.LayoutParams(dp(55), dp(55)));
        LinearLayout p = new LinearLayout(this); p.setOrientation(LinearLayout.VERTICAL); p.setPadding(dp(12), 0, 0, 0); p.addView(label(profileName, 15, textColor(), true)); p.addView(label(profileAbout, 11, muted(), false));
        profile.addView(p, new LinearLayout.LayoutParams(0, -2, 1)); TextView edit = label("Edit", 12, accent(), true); profile.addView(edit); profile.setOnClickListener(v -> showEditProfileDialog()); list.addView(profile);
        addSectionTitle(list, "Tampilan", "Personalisasi antarmuka");
        addSwitchRow(list, "Mode gelap", "Gunakan warna gelap", darkMode, value -> { darkMode = value; prefs.edit().putBoolean("dark_mode", value).apply(); buildScreen(); });
        addSwitchRow(list, "Tampilan ringkas", "Kurangi jarak antar percakapan", compactRows, value -> { compactRows = value; prefs.edit().putBoolean("compact_rows", value).apply(); buildScreen(); });
        addSectionTitle(list, "Privasi lokal", "Pengaturan ini berlaku di aplikasi pada perangkat ini");
        addSwitchRow(list, "Sembunyikan pratinjau", "Sembunyikan isi pesan di daftar chat", hidePreview, value -> { hidePreview = value; prefs.edit().putBoolean("hide_preview", value).apply(); buildScreen(); });
        addSwitchRow(list, "Tanda baca lokal", "Preferensi tampilan tanda baca (belum dikirim ke server)", readReceipts, value -> { readReceipts = value; prefs.edit().putBoolean("read_receipts", value).apply(); });
        addSettingRow(list, "Kunci aplikasi dengan PIN", appLockEnabled ? "PIN lokal aktif" : "Belum diatur", "▣", this::showPinSetupDialog);
        addSectionTitle(list, "Data & dukungan", "Kelola data aplikasi");
        addSettingRow(list, "Ekspor semua data", "Bagikan salinan chat dan status lokal", "⇧", this::exportAllData);
        addSettingRow(list, "Bersihkan data contoh", "Hapus percakapan dan status demo", "⌫", this::confirmResetDemo);
        addSettingRow(list, "Tentang TBWhatsApp", "Versi 2.0.0 • Independent messenger", "ⓘ", () -> showInfoDialog("Tentang TBWhatsApp 2.0", "TBWhatsApp adalah aplikasi messenger mandiri untuk demo dan penyimpanan lokal. Aplikasi ini tidak terafiliasi dengan WhatsApp/Meta. Pesan, status, komunitas, dan riwayat panggilan belum tersinkron ke pengguna lain. Untuk layanan online diperlukan backend, autentikasi, keamanan server, dan push notification."));
    }

    private void addSwitchRow(LinearLayout parent, String title, String subtitle, boolean checked, SwitchListener listener) {
        LinearLayout row = new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL); row.setPadding(dp(16), dp(10), dp(16), dp(10)); row.setBackgroundColor(surface());
        LinearLayout text = new LinearLayout(this); text.setOrientation(LinearLayout.VERTICAL); text.addView(label(title, 13, textColor(), true)); TextView sub = label(subtitle, 10, muted(), false); sub.setMaxLines(2); text.addView(sub);
        row.addView(text, new LinearLayout.LayoutParams(0, -2, 1)); Switch toggle = new Switch(this); toggle.setChecked(checked); row.addView(toggle); toggle.setOnCheckedChangeListener((button, value) -> listener.onChanged(value));
        parent.addView(row); addDivider(parent);
    }

    private void addSettingRow(LinearLayout parent, String title, String subtitle, String icon, Runnable action) {
        LinearLayout row = new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL); row.setPadding(dp(16), dp(12), dp(16), dp(12)); row.setBackgroundColor(surface());
        TextView glyph = label(icon, 20, accent(), true); glyph.setGravity(Gravity.CENTER); row.addView(glyph, new LinearLayout.LayoutParams(dp(35), dp(35)));
        LinearLayout info = new LinearLayout(this); info.setOrientation(LinearLayout.VERTICAL); info.setPadding(dp(10), 0, 0, 0); info.addView(label(title, 13, textColor(), true)); info.addView(label(subtitle, 10, muted(), false));
        row.addView(info, new LinearLayout.LayoutParams(0, -2, 1)); row.addView(label("›", 22, muted(), false)); row.setOnClickListener(v -> action.run()); parent.addView(row); addDivider(parent);
    }

    private void buildConversationScreen() {
        ChatItem chat = findChat(currentChat);
        LinearLayout top = new LinearLayout(this); top.setGravity(Gravity.CENTER_VERTICAL); top.setPadding(dp(8), dp(8), dp(10), dp(8)); top.setBackgroundColor(surface());
        TextView back = label("‹", 31, accent(), false); back.setGravity(Gravity.CENTER); top.addView(back, new LinearLayout.LayoutParams(dp(38), dp(44))); back.setOnClickListener(v -> { currentChat = null; buildScreen(); });
        TextView avatar = label(chat == null ? "TB" : chat.initials, 12, 0xFF173A35, true); avatar.setGravity(Gravity.CENTER); avatar.setBackground(round(chat == null ? 0xFFBBD7CE : chat.avatarColor, 50)); top.addView(avatar, new LinearLayout.LayoutParams(dp(39), dp(39)));
        LinearLayout info = new LinearLayout(this); info.setOrientation(LinearLayout.VERTICAL); info.setPadding(dp(9), 0, 0, 0); info.addView(label(currentChat, 14, textColor(), true)); info.addView(label("Percakapan lokal • perangkat ini", 10, muted(), false)); top.addView(info, new LinearLayout.LayoutParams(0, -2, 1));
        TextView call = label("☎", 19, accent(), true); top.addView(call, new LinearLayout.LayoutParams(dp(38), dp(42))); call.setOnClickListener(v -> toast("Panggilan online belum tersedia."));
        TextView more = label("⋮", 22, muted(), true); top.addView(more, new LinearLayout.LayoutParams(dp(30), dp(42))); more.setOnClickListener(v -> showConversationMenu());
        root.addView(top); addDivider(root);

        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true);
        LinearLayout messages = new LinearLayout(this); messages.setOrientation(LinearLayout.VERTICAL); messages.setPadding(dp(12), dp(13), dp(12), dp(15)); messages.setBackgroundColor(bg());
        ArrayList<MessageItem> list = conversations.get(currentChat);
        if (list == null || list.isEmpty()) addEmptyState(messages, "Mulai percakapan", "Pesan yang kamu tulis akan tersimpan secara lokal di perangkat ini.", null, null);
        else for (int i = 0; i < list.size(); i++) messages.addView(messageBubble(list.get(i), i));
        scroll.addView(messages); root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout composer = new LinearLayout(this); composer.setGravity(Gravity.CENTER_VERTICAL); composer.setPadding(dp(8), dp(7), dp(8), dp(7)); composer.setBackgroundColor(surface());
        TextView emoji = label("☺", 22, muted(), true); emoji.setGravity(Gravity.CENTER); composer.addView(emoji, new LinearLayout.LayoutParams(dp(35), dp(42))); emoji.setOnClickListener(v -> showEmojiDialog());
        EditText input = new EditText(this); input.setSingleLine(true); input.setTextSize(14); input.setTextColor(textColor()); input.setHintTextColor(muted()); input.setHint("Tulis pesan..."); input.setPadding(dp(13), 0, dp(13), 0); input.setBackground(round(bg(), 25));
        composer.addView(input, new LinearLayout.LayoutParams(0, dp(43), 1));
        TextView attach = label("＋", 22, muted(), true); attach.setGravity(Gravity.CENTER); composer.addView(attach, new LinearLayout.LayoutParams(dp(35), dp(42))); attach.setOnClickListener(v -> showAttachmentInfo());
        TextView send = label("➤", 18, 0xFFFFFFFF, true); send.setGravity(Gravity.CENTER); send.setBackground(round(accent(), 40)); LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(dp(42), dp(42)); sp.leftMargin = dp(5); composer.addView(send, sp);
        send.setOnClickListener(v -> sendMessage(input));
        input.setOnEditorActionListener((v, actionId, event) -> { sendMessage(input); return true; });
        root.addView(composer);
    }

    private void sendMessage(EditText input) {
        String value = input.getText().toString().trim(); if (value.isEmpty()) return;
        ArrayList<MessageItem> list = conversations.get(currentChat); if (list == null) { list = new ArrayList<>(); conversations.put(currentChat, list); }
        String time = nowTime(); list.add(new MessageItem(true, value, time));
        ChatItem c = findChat(currentChat); if (c != null) { c.preview = value; c.time = time; c.unread = 0; }
        input.setText(""); ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(input.getWindowToken(), 0);
        saveData(); buildScreen();
    }

    private View messageBubble(MessageItem item, int index) {
        LinearLayout lineBox = new LinearLayout(this); lineBox.setGravity(item.mine ? Gravity.RIGHT : Gravity.LEFT); lineBox.setPadding(0, dp(3), 0, dp(3));
        LinearLayout bubbleBox = new LinearLayout(this); bubbleBox.setOrientation(LinearLayout.VERTICAL); bubbleBox.setPadding(dp(12), dp(8), dp(12), dp(6)); bubbleBox.setBackground(round(item.mine ? mineBubble() : otherBubble(), 15));
        TextView content = label(item.text, 14, item.mine && !darkMode ? 0xFF173A35 : textColor(), false); bubbleBox.addView(content);
        TextView stamp = label(item.time + (item.mine ? "  ✓" : ""), 9, muted(), false); stamp.setGravity(Gravity.RIGHT); bubbleBox.addView(stamp);
        bubbleBox.setOnLongClickListener(v -> { showMessageOptions(index, item); return true; }); lineBox.addView(bubbleBox); return lineBox;
    }

    private void showMessageOptions(int index, MessageItem item) {
        String[] actions = {"Salin pesan", "Bagikan pesan", "Hapus pesan"};
        new AlertDialog.Builder(this).setItems(actions, (d, which) -> {
            if (which == 0) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                clipboard.setPrimaryClip(ClipData.newPlainText("TBWhatsApp", item.text)); toast("Pesan disalin.");
            } else if (which == 1) shareText(item.text);
            else {
                ArrayList<MessageItem> list = conversations.get(currentChat);
                if (list != null && index >= 0 && index < list.size()) {
                    list.remove(index); ChatItem c = findChat(currentChat); if (c != null) { c.preview = list.isEmpty() ? "Mulai percakapan" : list.get(list.size() - 1).text; c.time = nowTime(); }
                    saveData(); buildScreen();
                }
            }
        }).show();
    }

    private void showConversationMenu() {
        String[] actions = {"Info percakapan", "Ekspor percakapan", "Opsi chat", "Tandai belum dibaca"};
        new AlertDialog.Builder(this).setItems(actions, (d, which) -> {
            ChatItem c = findChat(currentChat);
            if (which == 0) showInfoDialog("Info percakapan", "Nama: " + currentChat + "\nPesan tersimpan lokal: " + (conversations.get(currentChat) == null ? 0 : conversations.get(currentChat).size()));
            else if (which == 1) shareConversation(currentChat);
            else if (which == 2 && c != null) showChatOptions(c);
            else if (which == 3 && c != null) { c.unread = 1; saveData(); currentChat = null; buildScreen(); }
        }).show();
    }

    private void showCreateMenu() {
        String[] actions = {"Chat baru", "Grup baru", "Status baru", "Komunitas baru"};
        new AlertDialog.Builder(this).setTitle("Buat baru").setItems(actions, (d, which) -> {
            if (which == 0) showNewChatDialog(false);
            else if (which == 1) showNewChatDialog(true);
            else if (which == 2) showStatusDialog();
            else showCommunityDialog();
        }).show();
    }

    private void showNewChatDialog(boolean group) {
        EditText input = new EditText(this); input.setSingleLine(true); input.setHint(group ? "Nama grup" : "Nama kontak"); input.setPadding(dp(12), dp(8), dp(12), dp(8));
        new AlertDialog.Builder(this).setTitle(group ? "Buat grup lokal" : "Percakapan baru")
                .setMessage("Buat ruang percakapan lokal. Belum mengundang pengguna lain.").setView(input)
                .setNegativeButton("Batal", null).setPositiveButton("Buat", (d, w) -> {
                    String name = input.getText().toString().trim(); if (name.isEmpty()) return;
                    ChatItem found = findChat(name);
                    if (found == null) { int color = group ? 0xFFB4D9C9 : 0xFFB7C9E8; chats.add(0, new ChatItem(name, "Mulai percakapan", "Baru", 0, false, group, false, false, false, initials(name), color)); conversations.put(name, new ArrayList<>()); }
                    currentChat = name; saveData(); buildScreen();
                }).show();
    }

    private void showNewChatDialog() { showNewChatDialog(false); }

    private void showStatusDialog() {
        EditText input = new EditText(this); input.setHint("Apa yang ingin kamu bagikan?"); input.setMinLines(2); input.setPadding(dp(12), dp(8), dp(12), dp(8));
        new AlertDialog.Builder(this).setTitle("Status teks baru").setView(input).setNegativeButton("Batal", null)
                .setPositiveButton("Bagikan", (d, w) -> { String value = input.getText().toString().trim(); if (!value.isEmpty()) { statuses.add(value); saveData(); selectedTab = 1; buildScreen(); } }).show();
    }

    private void showCommunityDialog() {
        EditText input = new EditText(this); input.setSingleLine(true); input.setHint("Nama komunitas"); input.setPadding(dp(12), dp(8), dp(12), dp(8));
        new AlertDialog.Builder(this).setTitle("Komunitas baru").setMessage("Membuat ruang komunitas lokal.").setView(input).setNegativeButton("Batal", null)
                .setPositiveButton("Buat", (d, w) -> { String name = input.getText().toString().trim(); if (!name.isEmpty()) { communities.add(0, name); saveData(); selectedTab = 3; buildScreen(); } }).show();
    }

    private void showEditProfileDialog() {
        LinearLayout box = new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(8), dp(4), dp(8), dp(4));
        EditText name = new EditText(this); name.setSingleLine(true); name.setHint("Nama profil"); name.setText(profileName); box.addView(name);
        EditText about = new EditText(this); about.setSingleLine(true); about.setHint("Tentang"); about.setText(profileAbout); box.addView(about);
        new AlertDialog.Builder(this).setTitle("Edit profil lokal").setView(box).setNegativeButton("Batal", null).setPositiveButton("Simpan", (d, w) -> {
            profileName = name.getText().toString().trim().isEmpty() ? "Pengguna TB" : name.getText().toString().trim();
            profileAbout = about.getText().toString().trim(); prefs.edit().putString("profile_name", profileName).putString("profile_about", profileAbout).apply(); buildScreen();
        }).show();
    }

    private void showPinSetupDialog() {
        EditText pin = new EditText(this); pin.setSingleLine(true); pin.setInputType(2); pin.setHint("PIN baru, minimal 4 digit"); pin.setPadding(dp(12), dp(8), dp(12), dp(8));
        AlertDialog.Builder builder = new AlertDialog.Builder(this).setTitle("Kunci aplikasi")
                .setMessage("PIN diproses menjadi hash PBKDF2 dan disimpan lokal. Jangan gunakan PIN penting yang sama.")
                .setView(pin).setNegativeButton("Batal", null).setPositiveButton("Simpan PIN", (d, w) -> {
                    String value = pin.getText().toString().trim();
                    if (value.length() < 4) { toast("PIN minimal 4 digit."); return; }
                    byte[] salt = new byte[16];
                    new SecureRandom().nextBytes(salt);
                    String saltHex = toHex(salt);
                    String hash = hashPin(value, salt);
                    if (hash.isEmpty()) { toast("Tidak dapat membuat hash PIN."); return; }
                    profilePinHash = hash; profilePinSalt = saltHex; appLockEnabled = true; lockPassed = true;
                    prefs.edit().putString("app_pin_hash", hash).putString("app_pin_salt", saltHex).putBoolean("app_lock", true).remove("app_pin").apply();
                    toast("PIN lokal berhasil diaktifkan."); renderSettingsTab();
                });
        if (appLockEnabled) {
            builder.setNeutralButton("Matikan", (d, w) -> {
                profilePinHash = ""; profilePinSalt = ""; appLockEnabled = false;
                prefs.edit().remove("app_pin_hash").remove("app_pin_salt").remove("app_pin").putBoolean("app_lock", false).apply();
                renderSettingsTab();
            });
        }
        builder.show();
    }

    private void showUnlockDialog() {
        EditText input = new EditText(this); input.setSingleLine(true); input.setInputType(2); input.setHint("Masukkan PIN");
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("TBWhatsApp terkunci").setMessage("Masukkan PIN lokal untuk melanjutkan.").setView(input)
                .setCancelable(false).setPositiveButton("Buka", null).create();
        dialog.setOnShowListener(v -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(btn -> {
            byte[] salt = fromHex(profilePinSalt);
            String enteredHash = hashPin(input.getText().toString(), salt);
            if (!profilePinHash.isEmpty() && constantTimeEquals(profilePinHash, enteredHash)) { lockPassed = true; dialog.dismiss(); }
            else { input.setError("PIN salah"); }
        }));
        dialog.show();
    }

    private String hashPin(String pin, byte[] salt) {
        try {
            KeySpec spec = new PBEKeySpec(pin.toCharArray(), salt, 120000, 256);
            byte[] encoded = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).getEncoded();
            return toHex(encoded);
        } catch (Exception error) { return ""; }
    }

    private String toHex(byte[] data) {
        StringBuilder out = new StringBuilder();
        for (byte b : data) out.append(String.format(Locale.ROOT, "%02x", b & 0xff));
        return out.toString();
    }

    private byte[] fromHex(String value) {
        if (value == null || value.length() % 2 != 0) return new byte[0];
        byte[] result = new byte[value.length() / 2];
        try { for (int i = 0; i < result.length; i++) result[i] = (byte) Integer.parseInt(value.substring(i * 2, i * 2 + 2), 16); }
        catch (Exception error) { return new byte[0]; }
        return result;
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        return java.security.MessageDigest.isEqual(a.getBytes(java.nio.charset.StandardCharsets.UTF_8), b.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }


    private void showEmojiDialog() {
        String[] emojis = {"😀", "😁", "😂", "🥰", "😍", "😎", "😭", "🙏", "👍", "❤️", "🔥", "✨", "🎉", "🤝", "☕", "🌙"};
        new AlertDialog.Builder(this).setTitle("Pilih emoji").setItems(emojis, (d, which) -> {
            if (currentChat == null) return;
            ArrayList<MessageItem> list = conversations.get(currentChat); if (list == null) { list = new ArrayList<>(); conversations.put(currentChat, list); }
            list.add(new MessageItem(true, emojis[which], nowTime())); ChatItem c = findChat(currentChat); if (c != null) { c.preview = emojis[which]; c.time = nowTime(); }
            saveData(); buildScreen();
        }).show();
    }

    private void showAttachmentInfo() {
        new AlertDialog.Builder(this).setTitle("Lampiran").setItems(new String[]{"Foto", "Video", "Dokumen", "Lokasi"}, (d, which) ->
                showInfoDialog("Lampiran belum aktif", "Pengiriman " + new String[]{"foto", "video", "dokumen", "lokasi"}[which] + " membutuhkan penyimpanan media, izin Android, dan backend komunikasi." )).show();
    }

    private void shareConversation(String name) {
        StringBuilder text = new StringBuilder("Percakapan TBWhatsApp: ").append(name).append("\n\n");
        ArrayList<MessageItem> list = conversations.get(name);
        if (list != null) for (MessageItem m : list) text.append(m.mine ? profileName : name).append(" [").append(m.time).append("]: ").append(m.text).append("\n");
        shareText(text.toString());
    }

    private void exportAllData() {
        StringBuilder out = new StringBuilder("TBWhatsApp V2 - Ekspor data lokal\n\n");
        for (ChatItem c : chats) {
            out.append("=== ").append(c.name).append(" ===\n");
            ArrayList<MessageItem> list = conversations.get(c.name);
            if (list != null) for (MessageItem m : list) out.append(m.mine ? profileName : c.name).append(" [").append(m.time).append("]: ").append(m.text).append("\n");
            out.append("\n");
        }
        out.append("STATUS\n"); for (String s : statuses) out.append("- ").append(s).append("\n");
        shareText(out.toString());
    }

    private void shareText(String text) {
        Intent intent = new Intent(Intent.ACTION_SEND); intent.setType("text/plain"); intent.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(Intent.createChooser(intent, "Bagikan dari TBWhatsApp"));
    }

    private void confirmResetDemo() {
        new AlertDialog.Builder(this).setTitle("Bersihkan data lokal?").setMessage("Semua chat, status, dan komunitas pada aplikasi ini akan dihapus lalu data contoh dimuat ulang.")
                .setNegativeButton("Batal", null).setPositiveButton("Bersihkan", (d, w) -> {
                    chats.clear(); conversations.clear(); statuses.clear(); communities.clear(); seedData(); communities.add("Teman TBWhatsApp"); communities.add("Komunitas Kreator"); saveData(); currentChat = null; selectedTab = 0; buildScreen();
                }).show();
    }

    private void addSectionTitle(LinearLayout parent, String title, String subtitle) {
        LinearLayout box = new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(16), dp(15), dp(16), dp(9));
        box.addView(label(title, 18, textColor(), true)); box.addView(label(subtitle, 10, muted(), false)); parent.addView(box);
    }

    private LinearLayout actionCard(String title, String subtitle) {
        LinearLayout card = new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL); card.setPadding(dp(16), dp(13), dp(16), dp(13)); card.setBackground(round(surface(), 14));
        card.addView(label(title, 14, accent(), true)); card.addView(label(subtitle, 10, muted(), false));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2); lp.setMargins(dp(14), dp(6), dp(14), dp(12)); card.setLayoutParams(lp); return card;
    }

    private void addEmptyState(LinearLayout parent, String title, String subtitle, String action, Runnable callback) {
        LinearLayout box = new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setGravity(Gravity.CENTER); box.setPadding(dp(26), dp(36), dp(26), dp(36));
        TextView icon = label("☏", 33, accent(), true); icon.setGravity(Gravity.CENTER); box.addView(icon);
        TextView h = label(title, 15, textColor(), true); h.setGravity(Gravity.CENTER); h.setPadding(0, dp(8), 0, dp(4)); box.addView(h);
        TextView sub = label(subtitle, 11, muted(), false); sub.setGravity(Gravity.CENTER); box.addView(sub);
        if (action != null && callback != null) { TextView btn = label(action, 12, 0xFFFFFFFF, true); btn.setGravity(Gravity.CENTER); btn.setPadding(dp(16), dp(10), dp(16), dp(10)); btn.setBackground(round(accent(), 22)); LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2); lp.topMargin = dp(15); box.addView(btn, lp); btn.setOnClickListener(v -> callback.run()); }
        parent.addView(box);
    }

    private void addDivider(LinearLayout parent) {
        View divider = new View(this); divider.setBackgroundColor(dividerColor()); parent.addView(divider, new LinearLayout.LayoutParams(-1, dp(1)));
    }

    private TextView label(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this); view.setText(value); view.setTextSize(size); view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD); return view;
    }

    private GradientDrawable round(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable(); drawable.setColor(color); drawable.setCornerRadius(dp(radius)); return drawable;
    }

    private ChatItem findChat(String name) { for (ChatItem item : chats) if (item.name.equals(name)) return item; return null; }

    private String initials(String name) {
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 0 || parts[0].isEmpty()) return "TB";
        if (parts.length == 1) return parts[0].substring(0, 1).toUpperCase(Locale.ROOT);
        return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase(Locale.ROOT);
    }

    private String nowTime() { return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date()); }
    private void showInfoDialog(String title, String message) { new AlertDialog.Builder(this).setTitle(title).setMessage(message).setPositiveButton("Tutup", null).show(); }
    private void toast(String message) { Toast.makeText(this, message, Toast.LENGTH_SHORT).show(); }

    @Override
    public void onBackPressed() {
        if (currentChat != null) { currentChat = null; buildScreen(); }
        else if (selectedTab != 0) { selectedTab = 0; buildScreen(); }
        else super.onBackPressed();
    }

    private interface SwitchListener { void onChanged(boolean value); }

    private static class ChatItem {
        String name, preview, time, initials; int unread, avatarColor;
        boolean favorite, group, pinned, muted, archived;
        ChatItem(String name, String preview, String time, int unread, boolean favorite, boolean group,
                 boolean pinned, boolean muted, boolean archived, String initials, int avatarColor) {
            this.name = name; this.preview = preview; this.time = time; this.unread = unread; this.favorite = favorite;
            this.group = group; this.pinned = pinned; this.muted = muted; this.archived = archived;
            this.initials = initials; this.avatarColor = avatarColor;
        }
    }

    private static class MessageItem {
        boolean mine; String text, time;
        MessageItem(boolean mine, String text, String time) { this.mine = mine; this.text = text; this.time = time; }
    }
}
