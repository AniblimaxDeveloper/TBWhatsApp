package com.tblabs.tbwhatsapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
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
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final String PREFS = "tbwhatsapp_settings";
    private LinearLayout root;
    private LinearLayout pageBody;
    private LinearLayout chatListContainer;
    private SharedPreferences preferences;
    private final ArrayList<ChatItem> chats = new ArrayList<>();
    private final HashMap<String, ArrayList<MessageItem>> conversations = new HashMap<>();
    private boolean darkMode;
    private boolean hidePreview;
    private boolean compactRows;
    private int selectedTab = 0;
    private String currentChat = null;
    private String activeFilter = "Semua";
    private String searchQuery = "";

    private int bg() { return color(darkMode ? 0xFF101A1C : 0xFFF4F7F7); }
    private int surface() { return color(darkMode ? 0xFF1B292B : 0xFFFFFFFF); }
    private int textColor() { return color(darkMode ? 0xFFEAF2F2 : 0xFF172629); }
    private int muted() { return color(darkMode ? 0xFF9BAEAF : 0xFF718184); }
    private int accent() { return color(0xFF087F73); }
    private int line() { return color(darkMode ? 0xFF304144 : 0xFFE5ECEC); }
    private int bubbleMine() { return color(darkMode ? 0xFF075E54 : 0xFFD9F4EA); }
    private int bubbleOther() { return color(darkMode ? 0xFF233437 : 0xFFFFFFFF); }

    private int color(int value) { return value; }
    private int dp(float value) { return (int) (value * getResources().getDisplayMetrics().density + 0.5f); }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = getSharedPreferences(PREFS, MODE_PRIVATE);
        darkMode = preferences.getBoolean("dark_mode", false);
        hidePreview = preferences.getBoolean("hide_preview", false);
        compactRows = preferences.getBoolean("compact_rows", false);
        seedData();
        buildScreen();
    }

    private void seedData() {
        addSeedChat("Nadia Putri", "Oke, sampai nanti ya!", "18.42", 2, true, false, "NP", 0xFFE7BFA9);
        addSeedChat("Raka Pratama", "Foto kemarin sudah aku kirim", "17.56", 0, true, false, "RP", 0xFFB7C9E8);
        addSeedChat("Keluarga Besar", "Ibu: Jangan lupa makan", "16.20", 4, false, true, "KB", 0xFFE7D49E);
        addSeedChat("Dina Aulia", "Makasih banyak ✨", "Kemarin", 0, false, false, "DA", 0xFFE8B9CC);
        addSeedChat("Tim Project", "Bima: Meeting jam 8 besok", "Senin", 1, false, true, "TP", 0xFFB4D9C9);
        addSeedChat("Fajar", "Voice note", "Minggu", 0, true, false, "F", 0xFFC8B8E7);

        putMessages("Nadia Putri", new MessageItem(false, "Hai! Lagi di mana?"), new MessageItem(true, "Lagi di rumah. Kenapa?"), new MessageItem(false, "Oke, sampai nanti ya!"));
        putMessages("Raka Pratama", new MessageItem(false, "Bro, file-nya sudah jadi."), new MessageItem(true, "Mantap, kirim ke sini ya."), new MessageItem(false, "Foto kemarin sudah aku kirim"));
        putMessages("Keluarga Besar", new MessageItem(false, "Besok kumpul jam berapa?"), new MessageItem(true, "Sekitar jam 10 ya."), new MessageItem(false, "Ibu: Jangan lupa makan"));
        putMessages("Dina Aulia", new MessageItem(true, "Terima kasih sudah membantu."), new MessageItem(false, "Makasih banyak ✨"));
        putMessages("Tim Project", new MessageItem(false, "Bima: Meeting jam 8 besok"));
        putMessages("Fajar", new MessageItem(false, "Voice note"));
    }

    private void addSeedChat(String name, String preview, String time, int unread, boolean favorite, boolean group, String initials, int avatarColor) {
        chats.add(new ChatItem(name, preview, time, unread, favorite, group, initials, avatarColor));
    }

    private void putMessages(String name, MessageItem... items) {
        ArrayList<MessageItem> list = new ArrayList<>();
        Collections.addAll(list, items);
        conversations.put(name, list);
    }

    private void buildScreen() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(bg());
        setContentView(root);
        if (currentChat != null) {
            buildConversationScreen();
        } else {
            buildMainHeader();
            pageBody = new LinearLayout(this);
            pageBody.setOrientation(LinearLayout.VERTICAL);
            root.addView(pageBody, new LinearLayout.LayoutParams(-1, 0, 1));
            renderSelectedTab();
            buildBottomNavigation();
        }
        getWindow().setStatusBarColor(color(darkMode ? 0xFF0B4943 : 0xFF075E54));
        getWindow().setNavigationBarColor(surface());
        getWindow().getDecorView().setSystemUiVisibility(darkMode ? 0 : View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
    }

    private void buildMainHeader() {
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(18), dp(15), dp(18), dp(13));
        header.setBackgroundColor(surface());

        TextView logo = new TextView(this);
        logo.setText("TB");
        logo.setTextColor(0xFFFFFFFF);
        logo.setTextSize(15);
        logo.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        logo.setGravity(Gravity.CENTER);
        logo.setBackground(round(accent(), 16));
        header.addView(logo, new LinearLayout.LayoutParams(dp(43), dp(43)));

        LinearLayout titleBox = new LinearLayout(this);
        titleBox.setOrientation(LinearLayout.VERTICAL);
        titleBox.setPadding(dp(11), 0, 0, 0);
        TextView title = label("TBWhatsApp", 20, textColor(), true);
        TextView subtitle = label("Independent messenger prototype", 11, muted(), false);
        titleBox.addView(title);
        titleBox.addView(subtitle);
        header.addView(titleBox, new LinearLayout.LayoutParams(0, -2, 1));

        TextView add = label("＋", 27, accent(), false);
        add.setGravity(Gravity.CENTER);
        add.setBackground(round(bg(), 14));
        add.setOnClickListener(v -> showNewChatDialog());
        header.addView(add, new LinearLayout.LayoutParams(dp(43), dp(43)));
        root.addView(header);
        View divider = new View(this);
        divider.setBackgroundColor(line());
        root.addView(divider, new LinearLayout.LayoutParams(-1, dp(1)));
    }

    private void renderSelectedTab() {
        pageBody.removeAllViews();
        if (selectedTab == 0) renderChatsTab();
        else if (selectedTab == 1) renderUpdatesTab();
        else if (selectedTab == 2) renderCallsTab();
        else renderSettingsTab();
    }

    private void renderChatsTab() {
        LinearLayout searchWrap = new LinearLayout(this);
        searchWrap.setPadding(dp(16), dp(12), dp(16), dp(8));
        EditText search = new EditText(this);
        search.setSingleLine(true);
        search.setTextSize(14);
        search.setTextColor(textColor());
        search.setHintTextColor(muted());
        search.setHint("⌕  Cari nama atau percakapan");
        search.setPadding(dp(14), 0, dp(14), 0);
        search.setBackground(round(surface(), 14));
        search.setText(searchQuery);
        searchWrap.addView(search, new LinearLayout.LayoutParams(-1, dp(46)));
        pageBody.addView(searchWrap);

        HorizontalScrollView filtersScroll = new HorizontalScrollView(this);
        filtersScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout filters = new LinearLayout(this);
        filters.setPadding(dp(16), dp(2), dp(16), dp(10));
        String[] options = {"Semua", "Belum dibaca", "Favorit", "Grup"};
        for (String option : options) {
            TextView chip = label(option, 12, option.equals(activeFilter) ? 0xFFFFFFFF : muted(), true);
            chip.setPadding(dp(14), dp(9), dp(14), dp(9));
            chip.setBackground(round(option.equals(activeFilter) ? accent() : surface(), 30));
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-2, -2);
            cp.rightMargin = dp(8);
            filters.addView(chip, cp);
            chip.setOnClickListener(v -> {
                activeFilter = option;
                renderChatsTab();
            });
        }
        filtersScroll.addView(filters);
        pageBody.addView(filtersScroll);

        ScrollView listScroll = new ScrollView(this);
        listScroll.setFillViewport(true);
        chatListContainer = new LinearLayout(this);
        chatListContainer.setOrientation(LinearLayout.VERTICAL);
        listScroll.addView(chatListContainer);
        pageBody.addView(listScroll, new LinearLayout.LayoutParams(-1, 0, 1));
        renderChatRows();

        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString();
                renderChatRows();
            }
            @Override public void afterTextChanged(Editable s) { }
        });
    }

    private void renderChatRows() {
        if (chatListContainer == null) return;
        chatListContainer.removeAllViews();
        String needle = searchQuery.toLowerCase(Locale.ROOT).trim();
        int count = 0;
        for (ChatItem item : chats) {
            if (!needle.isEmpty() && !item.name.toLowerCase(Locale.ROOT).contains(needle) && !item.preview.toLowerCase(Locale.ROOT).contains(needle)) continue;
            if ("Belum dibaca".equals(activeFilter) && item.unread == 0) continue;
            if ("Favorit".equals(activeFilter) && !item.favorite) continue;
            if ("Grup".equals(activeFilter) && !item.group) continue;
            chatListContainer.addView(createChatRow(item));
            count++;
        }
        if (count == 0) {
            TextView empty = label("Tidak ada percakapan yang cocok.", 14, muted(), false);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(20), dp(36), dp(20), dp(36));
            chatListContainer.addView(empty);
        }
    }

    private View createChatRow(ChatItem item) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        int vertical = compactRows ? 9 : 13;
        row.setPadding(dp(16), dp(vertical), dp(16), dp(vertical));
        row.setBackgroundColor(surface());

        TextView avatar = label(item.initials, 15, 0xFFFFFFFF, true);
        avatar.setGravity(Gravity.CENTER);
        avatar.setBackground(round(item.avatarColor, 40));
        row.addView(avatar, new LinearLayout.LayoutParams(dp(49), dp(49)));

        LinearLayout details = new LinearLayout(this);
        details.setOrientation(LinearLayout.VERTICAL);
        details.setPadding(dp(12), 0, dp(7), 0);
        TextView name = label(item.name + (item.favorite ? "  ★" : ""), 15, textColor(), true);
        TextView preview = label(hidePreview ? "Pesan baru" : item.preview, 12, muted(), false);
        preview.setMaxLines(1);
        preview.setEllipsize(android.text.TextUtils.TruncateAt.END);
        details.addView(name);
        LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(-1, -2);
        previewParams.topMargin = dp(4);
        details.addView(preview, previewParams);
        row.addView(details, new LinearLayout.LayoutParams(0, -2, 1));

        LinearLayout meta = new LinearLayout(this);
        meta.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        meta.setOrientation(LinearLayout.VERTICAL);
        TextView time = label(item.time, 10, muted(), false);
        time.setGravity(Gravity.RIGHT);
        meta.addView(time);
        if (item.unread > 0) {
            TextView badge = label(String.valueOf(item.unread), 10, 0xFFFFFFFF, true);
            badge.setGravity(Gravity.CENTER);
            badge.setBackground(round(accent(), 30));
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(dp(21), dp(21));
            bp.topMargin = dp(5);
            meta.addView(badge, bp);
        }
        row.addView(meta);
        row.setOnClickListener(v -> {
            item.unread = 0;
            currentChat = item.name;
            buildScreen();
        });
        View divider = new View(this);
        divider.setBackgroundColor(line());
        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.addView(row);
        wrap.addView(divider, new LinearLayout.LayoutParams(-1, dp(1)));
        return wrap;
    }

    private void renderUpdatesTab() {
        addSectionTitle("Pembaruan", "Status dan kabar terbaru");
        LinearLayout own = new LinearLayout(this);
        own.setGravity(Gravity.CENTER_VERTICAL);
        own.setPadding(dp(16), dp(12), dp(16), dp(12));
        own.setBackgroundColor(surface());
        TextView avatar = label("TB", 14, 0xFFFFFFFF, true);
        avatar.setGravity(Gravity.CENTER);
        avatar.setBackground(round(accent(), 40));
        own.addView(avatar, new LinearLayout.LayoutParams(dp(52), dp(52)));
        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.setPadding(dp(12), 0, 0, 0);
        labels.addView(label("Status saya", 15, textColor(), true));
        labels.addView(label("Ketuk untuk menambahkan pembaruan", 12, muted(), false));
        own.addView(labels);
        own.setOnClickListener(v -> toast("Fitur status masih berupa rancangan UI."));
        pageBody.addView(own);
        addSectionTitle("Terbaru", "Contoh tampilan status");
        String[][] statuses = {{"Nadia Putri", "35 menit yang lalu", "NP"}, {"Raka Pratama", "2 jam yang lalu", "RP"}, {"Dina Aulia", "Kemarin", "DA"}};
        for (String[] status : statuses) {
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(16), dp(10), dp(16), dp(10));
            row.setBackgroundColor(surface());
            TextView ring = label(status[2], 14, 0xFFFFFFFF, true);
            ring.setGravity(Gravity.CENTER);
            ring.setBackground(round(accent(), 40));
            row.addView(ring, new LinearLayout.LayoutParams(dp(48), dp(48)));
            LinearLayout text = new LinearLayout(this);
            text.setOrientation(LinearLayout.VERTICAL);
            text.setPadding(dp(12), 0, 0, 0);
            text.addView(label(status[0], 14, textColor(), true));
            text.addView(label(status[1], 11, muted(), false));
            row.addView(text);
            row.setOnClickListener(v -> toast("Status contoh — belum terhubung ke layanan pesan."));
            pageBody.addView(row);
        }
    }

    private void renderCallsTab() {
        addSectionTitle("Panggilan", "Riwayat panggilan contoh");
        String[][] calls = {{"Nadia Putri", "Panggilan suara keluar", "18.05", "↗"}, {"Raka Pratama", "Panggilan suara masuk", "Kemarin", "↙"}, {"Dina Aulia", "Tidak terjawab", "Senin", "↙"}};
        for (String[] call : calls) {
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(16), dp(13), dp(16), dp(13));
            row.setBackgroundColor(surface());
            TextView avatar = label(call[0].substring(0, 1), 16, 0xFFFFFFFF, true);
            avatar.setGravity(Gravity.CENTER);
            avatar.setBackground(round(0xFF8EAFA9, 40));
            row.addView(avatar, new LinearLayout.LayoutParams(dp(47), dp(47)));
            LinearLayout details = new LinearLayout(this);
            details.setOrientation(LinearLayout.VERTICAL);
            details.setPadding(dp(12), 0, 0, 0);
            details.addView(label(call[0], 14, textColor(), true));
            details.addView(label(call[1], 11, muted(), false));
            row.addView(details, new LinearLayout.LayoutParams(0, -2, 1));
            TextView time = label(call[2], 11, muted(), false);
            row.addView(time);
            row.setOnClickListener(v -> toast("Panggilan belum tersedia pada versi offline."));
            pageBody.addView(row);
        }
    }

    private void renderSettingsTab() {
        addSectionTitle("Pengaturan", "Atur pengalaman TBWhatsApp");
        LinearLayout profile = new LinearLayout(this);
        profile.setGravity(Gravity.CENTER_VERTICAL);
        profile.setPadding(dp(16), dp(15), dp(16), dp(15));
        profile.setBackground(round(surface(), 18));
        TextView avatar = label("TB", 17, 0xFFFFFFFF, true);
        avatar.setGravity(Gravity.CENTER);
        avatar.setBackground(round(accent(), 40));
        profile.addView(avatar, new LinearLayout.LayoutParams(dp(56), dp(56)));
        LinearLayout profileText = new LinearLayout(this);
        profileText.setOrientation(LinearLayout.VERTICAL);
        profileText.setPadding(dp(13), 0, 0, 0);
        profileText.addView(label("Pengguna TB", 16, textColor(), true));
        profileText.addView(label("Profil lokal • belum disinkronkan", 11, muted(), false));
        profile.addView(profileText);
        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(-1, -2);
        pp.setMargins(dp(16), dp(4), dp(16), dp(14));
        pageBody.addView(profile, pp);

        addSettingSwitch("Mode gelap", "Ubah tampilan menjadi tema gelap", "dark_mode", darkMode, value -> {
            darkMode = value;
            preferences.edit().putBoolean("dark_mode", value).apply();
            buildScreen();
        });
        addSettingSwitch("Sembunyikan pratinjau", "Sembunyikan isi pesan di daftar chat", "hide_preview", hidePreview, value -> {
            hidePreview = value;
            preferences.edit().putBoolean("hide_preview", value).apply();
            buildScreen();
        });
        addSettingSwitch("Tampilan ringkas", "Kurangi jarak antar percakapan", "compact_rows", compactRows, value -> {
            compactRows = value;
            preferences.edit().putBoolean("compact_rows", value).apply();
            buildScreen();
        });
        addSettingAction("Privasi & keamanan", "Ringkasan penyimpanan dan izin aplikasi", () -> showInfoDialog("Privasi & keamanan", "Versi ini tidak meminta izin membaca SMS, kontak, notifikasi, atau database WhatsApp. Percakapan contoh hanya disimpan sementara di memori aplikasi."));
        addSettingAction("Tentang TBWhatsApp", "Versi 1.0.0 • Independent prototype", () -> showInfoDialog("Tentang TBWhatsApp", "TBWhatsApp adalah prototipe messenger independen. Aplikasi ini bukan produk resmi WhatsApp dan tidak terhubung ke server WhatsApp."));
    }

    private interface BoolAction { void run(boolean value); }
    private void addSettingSwitch(String title, String description, String key, boolean checked, BoolAction action) {
        LinearLayout row = settingRow(title, description);
        Switch toggle = new Switch(this);
        toggle.setButtonTintList(android.content.res.ColorStateList.valueOf(accent()));
        toggle.setChecked(checked);
        row.addView(toggle);
        toggle.setOnCheckedChangeListener((button, value) -> action.run(value));
        pageBody.addView(row);
    }

    private void addSettingAction(String title, String description, Runnable action) {
        LinearLayout row = settingRow(title, description);
        TextView arrow = label("›", 26, muted(), false);
        row.addView(arrow);
        row.setOnClickListener(v -> action.run());
        pageBody.addView(row);
    }

    private LinearLayout settingRow(String title, String description) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), dp(13), dp(16), dp(13));
        row.setBackgroundColor(surface());
        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.addView(label(title, 14, textColor(), true));
        TextView sub = label(description, 11, muted(), false);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, -2);
        sp.topMargin = dp(4);
        texts.addView(sub, sp);
        row.addView(texts, new LinearLayout.LayoutParams(0, -2, 1));
        View divider = new View(this);
        divider.setBackgroundColor(line());
        pageBody.addView(divider, new LinearLayout.LayoutParams(-1, dp(1)));
        return row;
    }

    private void addSectionTitle(String title, String subtitle) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(18), dp(18), dp(18), dp(13));
        box.addView(label(title, 21, textColor(), true));
        TextView sub = label(subtitle, 12, muted(), false);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.topMargin = dp(3);
        box.addView(sub, p);
        pageBody.addView(box);
    }

    private void buildBottomNavigation() {
        LinearLayout nav = new LinearLayout(this);
        nav.setGravity(Gravity.CENTER);
        nav.setBackgroundColor(surface());
        nav.setPadding(dp(4), dp(7), dp(4), dp(7));
        String[] labels = {"Chat", "Update", "Panggilan", "Setelan"};
        String[] icons = {"◉", "◌", "☎", "⚙"};
        for (int i = 0; i < labels.length; i++) {
            final int index = i;
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER);
            TextView icon = label(icons[i], 19, selectedTab == i ? accent() : muted(), true);
            TextView title = label(labels[i], 10, selectedTab == i ? accent() : muted(), selectedTab == i);
            item.addView(icon);
            item.addView(title);
            item.setPadding(0, dp(3), 0, dp(3));
            item.setOnClickListener(v -> {
                selectedTab = index;
                buildScreen();
            });
            nav.addView(item, new LinearLayout.LayoutParams(0, -2, 1));
        }
        View divider = new View(this);
        divider.setBackgroundColor(line());
        root.addView(divider, new LinearLayout.LayoutParams(-1, dp(1)));
        root.addView(nav);
    }

    private void buildConversationScreen() {
        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(dp(8), dp(9), dp(12), dp(9));
        top.setBackgroundColor(surface());
        TextView back = label("‹", 31, textColor(), false);
        back.setGravity(Gravity.CENTER);
        top.addView(back, new LinearLayout.LayoutParams(dp(37), dp(45)));
        back.setOnClickListener(v -> {
            currentChat = null;
            buildScreen();
        });
        ChatItem item = findChat(currentChat);
        TextView avatar = label(item == null ? "TB" : item.initials, 12, 0xFFFFFFFF, true);
        avatar.setGravity(Gravity.CENTER);
        avatar.setBackground(round(item == null ? accent() : item.avatarColor, 40));
        LinearLayout.LayoutParams avp = new LinearLayout.LayoutParams(dp(40), dp(40));
        avp.setMargins(dp(3), 0, dp(10), 0);
        top.addView(avatar, avp);
        LinearLayout nameBox = new LinearLayout(this);
        nameBox.setOrientation(LinearLayout.VERTICAL);
        nameBox.addView(label(currentChat, 15, textColor(), true));
        nameBox.addView(label("chat lokal • demo", 10, muted(), false));
        top.addView(nameBox, new LinearLayout.LayoutParams(0, -2, 1));
        TextView call = label("☎", 20, accent(), true);
        call.setGravity(Gravity.CENTER);
        top.addView(call, new LinearLayout.LayoutParams(dp(40), dp(42)));
        call.setOnClickListener(v -> toast("Panggilan belum tersedia pada versi offline."));
        root.addView(top);
        View divider = new View(this);
        divider.setBackgroundColor(line());
        root.addView(divider, new LinearLayout.LayoutParams(-1, dp(1)));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout messages = new LinearLayout(this);
        messages.setOrientation(LinearLayout.VERTICAL);
        messages.setPadding(dp(12), dp(15), dp(12), dp(15));
        messages.setBackgroundColor(bg());
        ArrayList<MessageItem> conversation = conversations.get(currentChat);
        if (conversation != null) {
            for (MessageItem message : conversation) messages.addView(messageBubble(message));
        }
        scroll.addView(messages);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout composer = new LinearLayout(this);
        composer.setGravity(Gravity.CENTER_VERTICAL);
        composer.setPadding(dp(10), dp(8), dp(10), dp(8));
        composer.setBackgroundColor(surface());
        TextView attach = label("＋", 23, muted(), false);
        attach.setGravity(Gravity.CENTER);
        composer.addView(attach, new LinearLayout.LayoutParams(dp(38), dp(44)));
        attach.setOnClickListener(v -> toast("Lampiran belum aktif karena belum ada backend pesan."));
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setTextSize(14);
        input.setTextColor(textColor());
        input.setHintTextColor(muted());
        input.setHint("Tulis pesan...");
        input.setPadding(dp(13), 0, dp(13), 0);
        input.setBackground(round(bg(), 25));
        composer.addView(input, new LinearLayout.LayoutParams(0, dp(43), 1));
        TextView send = label("➤", 18, 0xFFFFFFFF, true);
        send.setGravity(Gravity.CENTER);
        send.setBackground(round(accent(), 40));
        LinearLayout.LayoutParams sendParams = new LinearLayout.LayoutParams(dp(43), dp(43));
        sendParams.setMargins(dp(8), 0, 0, 0);
        composer.addView(send, sendParams);
        send.setOnClickListener(v -> {
            String value = input.getText().toString().trim();
            if (value.isEmpty()) return;
            ArrayList<MessageItem> list = conversations.get(currentChat);
            if (list == null) {
                list = new ArrayList<>();
                conversations.put(currentChat, list);
            }
            list.add(new MessageItem(true, value));
            ChatItem chat = findChat(currentChat);
            if (chat != null) {
                chat.preview = value;
                chat.time = "Baru saja";
            }
            input.setText("");
            ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(input.getWindowToken(), 0);
            buildScreen();
            toast("Pesan tersimpan sementara di sesi demo ini.");
        });
        root.addView(composer);
    }

    private View messageBubble(MessageItem item) {
        LinearLayout lineBox = new LinearLayout(this);
        lineBox.setGravity(item.mine ? Gravity.RIGHT : Gravity.LEFT);
        lineBox.setPadding(0, dp(3), 0, dp(3));
        TextView bubble = label(item.text, 14, item.mine && !darkMode ? 0xFF173A35 : textColor(), false);
        bubble.setPadding(dp(13), dp(10), dp(13), dp(10));
        bubble.setMaxWidth((int) (getResources().getDisplayMetrics().widthPixels * 0.78f));
        bubble.setBackground(round(item.mine ? bubbleMine() : bubbleOther(), 16));
        lineBox.addView(bubble);
        return lineBox;
    }

    private void showNewChatDialog() {
        EditText nameInput = new EditText(this);
        nameInput.setSingleLine(true);
        nameInput.setHint("Nama kontak");
        nameInput.setPadding(dp(12), dp(8), dp(12), dp(8));
        new AlertDialog.Builder(this)
                .setTitle("Percakapan baru")
                .setMessage("Buat percakapan lokal untuk demo.")
                .setView(nameInput)
                .setNegativeButton("Batal", null)
                .setPositiveButton("Buat", (dialog, which) -> {
                    String name = nameInput.getText().toString().trim();
                    if (name.isEmpty()) return;
                    if (findChat(name) == null) {
                        chats.add(0, new ChatItem(name, "Mulai percakapan", "Baru", 0, false, false, initials(name), 0xFF88B9AD));
                        conversations.put(name, new ArrayList<>());
                    }
                    currentChat = name;
                    buildScreen();
                }).show();
    }

    private void showInfoDialog(String title, String message) {
        new AlertDialog.Builder(this).setTitle(title).setMessage(message).setPositiveButton("Tutup", null).show();
    }

    private ChatItem findChat(String name) {
        for (ChatItem item : chats) if (item.name.equals(name)) return item;
        return null;
    }

    private String initials(String name) {
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, 1).toUpperCase(Locale.ROOT);
        return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase(Locale.ROOT);
    }

    private TextView label(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private GradientDrawable round(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));
        return drawable;
    }

    private void toast(String message) { Toast.makeText(this, message, Toast.LENGTH_SHORT).show(); }

    @Override
    public void onBackPressed() {
        if (currentChat != null) {
            currentChat = null;
            buildScreen();
        } else if (selectedTab != 0) {
            selectedTab = 0;
            buildScreen();
        } else {
            super.onBackPressed();
        }
    }

    private static class ChatItem {
        String name;
        String preview;
        String time;
        int unread;
        boolean favorite;
        boolean group;
        String initials;
        int avatarColor;
        ChatItem(String name, String preview, String time, int unread, boolean favorite, boolean group, String initials, int avatarColor) {
            this.name = name; this.preview = preview; this.time = time; this.unread = unread;
            this.favorite = favorite; this.group = group; this.initials = initials; this.avatarColor = avatarColor;
        }
    }

    private static class MessageItem {
        boolean mine;
        String text;
        MessageItem(boolean mine, String text) { this.mine = mine; this.text = text; }
    }
}
