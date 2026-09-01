package com.mysms.app;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private final int ink = Color.rgb(30, 35, 48);
    private final int muted = Color.rgb(112, 119, 137);
    private final int canvas = Color.rgb(248, 249, 252);
    private final int primary = Color.rgb(75, 76, 190);
    private final int line = Color.rgb(228, 230, 238);
    private final List<Conversation> conversations = new ArrayList<>(Arrays.asList(
            new Conversation("Ava Morgan", "AM", "Dinner still on for tonight?", "9:42 AM", true, Color.rgb(224, 188, 173)),
            new Conversation("Design Circle", "DC", "Maya: The new direction feels great.", "Yesterday", false, Color.rgb(183, 202, 225)),
            new Conversation("Noah Williams", "NW", "Thanks! Talk soon.", "Mon", false, Color.rgb(197, 220, 195)),
            new Conversation("Mila Chen", "MC", "Photo attached", "Sun", false, Color.rgb(228, 196, 142))
    ));
    private LinearLayout root;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(canvas);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        showInbox("");
    }

    private void showInbox(String query) {
        root = base();
        LinearLayout content = column(20, 22, 20, 0);
        LinearLayout header = row();
        header.addView(label("MySMS", 30, ink, Typeface.BOLD), weight(1));
        header.addView(iconButton("⌕", v -> showSearch()));
        header.addView(iconButton("＋", v -> showCompose()));
        header.addView(iconButton("⋯", v -> showSettings()));
        content.addView(header);
        TextView eyebrow = label("YOUR MESSAGES", 11, muted, Typeface.BOLD); eyebrow.setLetterSpacing(.14f);
        content.addView(eyebrow, margin(0, 28, 0, 10));
        EditText search = input("Search conversations");
        content.addView(search, margin(0, 0, 0, 18));
        LinearLayout list = column(0, 0, 0, 0); content.addView(list);
        populateConversations(list, query);
        search.setText(query);
        search.addTextChangedListener(new TextWatcher() { public void beforeTextChanged(CharSequence s,int a,int b,int c){} public void onTextChanged(CharSequence s,int a,int b,int c){ populateConversations(list,s.toString()); } public void afterTextChanged(Editable e){} });
        root.addView(content, new LinearLayout.LayoutParams(-1, -2));
        setContentView(root);
    }

    private void populateConversations(LinearLayout list, String query) {
        list.removeAllViews(); String q = query.toLowerCase(Locale.ROOT); int count = 0;
        for (Conversation c : conversations) if (q.isEmpty() || c.name.toLowerCase(Locale.ROOT).contains(q) || c.preview.toLowerCase(Locale.ROOT).contains(q)) { list.addView(conversationRow(c)); count++; }
        if (count == 0) { TextView empty = label("No conversations found\nTry a name or message keyword.", 16, muted, Typeface.NORMAL); empty.setGravity(Gravity.CENTER); list.addView(empty, margin(0, 48, 0, 0)); }
    }

    private View conversationRow(Conversation c) {
        LinearLayout row = row(); row.setPadding(0, 12, 0, 12); row.setOnClickListener(v -> showChat(c));
        row.addView(avatar(c.initials, c.color, 52), margin(0,0,16,0));
        LinearLayout text = column(0,0,0,0); LinearLayout top = row(); TextView name = label(c.name, 16, ink, c.unread ? Typeface.BOLD : Typeface.NORMAL); top.addView(name, weight(1)); top.addView(label(c.time, 12, muted, Typeface.NORMAL)); text.addView(top);
        TextView preview = label(c.preview, 14, c.unread ? ink : muted, c.unread ? Typeface.BOLD : Typeface.NORMAL); preview.setMaxLines(1); text.addView(preview, margin(0,5,0,0)); row.addView(text, weight(1));
        if (c.unread) row.addView(label("●", 13, primary, Typeface.BOLD), margin(10,0,0,0));
        return row;
    }

    private void showSearch() { showInbox(""); Toast.makeText(this, "Search is ready — type in the field", Toast.LENGTH_SHORT).show(); }

    private void showChat(Conversation c) {
        root = base(); LinearLayout screen = column(18, 14, 18, 0);
        LinearLayout bar = row(); bar.addView(iconButton("‹", v -> showInbox(""))); bar.addView(avatar(c.initials,c.color,42), margin(8,0,12,0)); LinearLayout who=column(0,0,0,0); who.addView(label(c.name,17,ink,Typeface.BOLD)); who.addView(label("Mobile · available",12,muted,Typeface.NORMAL)); bar.addView(who,weight(1)); bar.addView(iconButton("⋯", v -> Toast.makeText(this,"Conversation actions",Toast.LENGTH_SHORT).show())); screen.addView(bar);
        ScrollView messages = new ScrollView(this); LinearLayout thread = column(0, 4, 0, 20); thread.addView(label("TODAY",11,muted,Typeface.BOLD), margin(0,18,0,16)); addBubble(thread,"Hey! Are we still on for dinner tonight?",false,"9:38 AM"); addBubble(thread,"Absolutely. I found that little place near the park.",true,"9:40 AM"); addBubble(thread,"Dinner still on for tonight?",false,"9:42 AM"); messages.addView(thread); screen.addView(messages, new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout composer = row(); composer.setGravity(Gravity.CENTER_VERTICAL); composer.setPadding(8,8,8,8); MaterialCardView card = new MaterialCardView(this); card.setRadius(28); card.setCardElevation(0); card.setStrokeColor(line); card.setStrokeWidth(1); EditText input = input("Message"); card.addView(input,new ViewGroup.LayoutParams(-1,-2)); composer.addView(card,new LinearLayout.LayoutParams(0,56,1)); MaterialButton send=button("↑",primary); send.setMinWidth(56); send.setOnClickListener(v->{ if(input.getText().length()>0){ addBubble(thread,input.getText().toString(),true,"Now"); input.setText(""); messages.post(()->messages.fullScroll(View.FOCUS_DOWN)); }}); composer.addView(send,margin(8,0,0,0)); screen.addView(composer); root.addView(screen,new LinearLayout.LayoutParams(-1,-1)); setContentView(root);
    }

    private void addBubble(LinearLayout thread,String text,boolean sent,String time){ LinearLayout lineRow=row(); lineRow.setGravity(sent?Gravity.END:Gravity.START); LinearLayout bubble=column(0,14,0,0); bubble.setPadding(dp(16),dp(12),dp(16),dp(10)); bubble.setBackgroundColor(sent?primary:Color.WHITE); TextView body=label(text,16,sent?Color.WHITE:ink,Typeface.NORMAL); body.setMaxWidth(dp(300)); bubble.addView(body); TextView stamp=label(time,11,sent?Color.rgb(218,218,255):muted,Typeface.NORMAL); bubble.addView(stamp,margin(0,5,0,0)); lineRow.addView(bubble); thread.addView(lineRow,margin(0,4,0,4)); }

    private void showCompose(){ root=base(); LinearLayout screen=column(20,18,20,0); LinearLayout bar=row(); bar.addView(iconButton("‹",v->showInbox(""))); bar.addView(label("New message",22,ink,Typeface.BOLD),weight(1)); screen.addView(bar); screen.addView(label("TO",11,muted,Typeface.BOLD),margin(0,28,0,8)); screen.addView(input("Name or phone number"),margin(0,0,0,18)); screen.addView(label("MESSAGE",11,muted,Typeface.BOLD),margin(0,0,0,8)); screen.addView(input("Write something kind…"),margin(0,0,0,22)); MaterialButton send=button("Start conversation",primary); send.setOnClickListener(v->Toast.makeText(this,"Recipient selection will connect to Contacts in Milestone 2",Toast.LENGTH_LONG).show()); screen.addView(send); root.addView(screen,new LinearLayout.LayoutParams(-1,-1)); setContentView(root); }

    private void showSettings(){ root=base(); LinearLayout screen=column(20,18,20,0); LinearLayout bar=row(); bar.addView(iconButton("‹",v->showInbox(""))); bar.addView(label("Settings",22,ink,Typeface.BOLD),weight(1)); screen.addView(bar); screen.addView(label("PERSONALIZE YOUR EXPERIENCE",11,muted,Typeface.BOLD),margin(0,30,0,12)); addSetting(screen,"Appearance","System default"); addSetting(screen,"Notifications","Messages and previews"); addSetting(screen,"Privacy","Blocked contacts and safety"); screen.addView(label("MySMS 1.0 · Preview data",12,muted,Typeface.NORMAL),margin(0,34,0,0)); root.addView(screen,new LinearLayout.LayoutParams(-1,-1)); setContentView(root); }
    private void addSetting(LinearLayout parent,String title,String sub){ MaterialCardView card=new MaterialCardView(this); card.setRadius(18);card.setCardElevation(0);card.setStrokeColor(line);card.setStrokeWidth(1); LinearLayout r=column(16,15,16,15);r.addView(label(title,16,ink,Typeface.BOLD));r.addView(label(sub,13,muted,Typeface.NORMAL),margin(0,4,0,0));card.addView(r);parent.addView(card,margin(0,0,0,10)); }

    private LinearLayout base(){ LinearLayout b=column(0,0,0,0); b.setBackgroundColor(canvas); return b; }
    private LinearLayout column(int l,int t,int r,int bot){LinearLayout x=new LinearLayout(this);x.setOrientation(LinearLayout.VERTICAL);x.setPadding(dp(l),dp(t),dp(r),dp(bot));return x;}
    private LinearLayout row(){LinearLayout x=new LinearLayout(this);x.setOrientation(LinearLayout.HORIZONTAL);return x;}
    private TextView label(String s,int size,int color,int style){TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setTextColor(color);v.setTypeface(Typeface.DEFAULT,style);v.setGravity(Gravity.CENTER_VERTICAL);return v;}
    private TextView avatar(String s,int color,int size){TextView v=label(s, size/3,ink,Typeface.BOLD);v.setGravity(Gravity.CENTER);v.setBackgroundColor(color);return v;}
    private EditText input(String hint){EditText e=new EditText(this);e.setHint(hint);e.setTextSize(15);e.setSingleLine(false);e.setPadding(dp(16),dp(7),dp(16),dp(7));e.setBackgroundColor(Color.WHITE);e.setTextColor(ink);e.setHintTextColor(muted);return e;}
    private TextView iconButton(String text,View.OnClickListener l){TextView v=label(text,28,ink,Typeface.NORMAL);v.setGravity(Gravity.CENTER);v.setContentDescription(text);v.setOnClickListener(l);v.setPadding(dp(7),0,dp(7),0);return v;}
    private MaterialButton button(String text,int color){MaterialButton b=new MaterialButton(this);b.setText(text);b.setTextColor(Color.WHITE);b.setTextSize(14);b.setBackgroundColor(color);return b;}
    private LinearLayout.LayoutParams weight(float w){return new LinearLayout.LayoutParams(0,-2,w);}
    private LinearLayout.LayoutParams margin(int l,int t,int r,int b){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,-2);p.setMargins(dp(l),dp(t),dp(r),dp(b));return p;}
    private int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}

    private static class Conversation { String name,initials,preview,time; boolean unread; int color; Conversation(String n,String i,String p,String t,boolean u,int c){name=n;initials=i;preview=p;time=t;unread=u;color=c;} }
}
