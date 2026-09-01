package com.mysms.app;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.Holder> {
    public interface Listener { void onConversationClicked(SmsRepository.Conversation conversation); }
    private final Listener listener;
    private final List<SmsRepository.Conversation> items = new ArrayList<>();
    private final int ink=Color.rgb(30,35,48), muted=Color.rgb(112,119,137), primary=Color.rgb(75,76,190);
    public ConversationAdapter(Listener listener){this.listener=listener;setHasStableIds(true);}
    public void submitList(List<SmsRepository.Conversation> values){items.clear();if(values!=null)items.addAll(values);notifyDataSetChanged();}
    public SmsRepository.Conversation getItem(int position){return items.get(position);}
    @Override public long getItemId(int position){return items.get(position).threadId;}
    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent,int viewType){LinearLayout row=new LinearLayout(parent.getContext());row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(parent,0),dp(parent,10),dp(parent,0),dp(parent,10));row.setMinimumHeight(dp(parent,78));row.setBackgroundResource(android.R.drawable.list_selector_background);return new Holder(row);}
    @Override public void onBindViewHolder(@NonNull Holder h,int position){SmsRepository.Conversation c=items.get(position);LinearLayout row=(LinearLayout)h.itemView;row.removeAllViews();TextView avatar=avatar(row,c.name);row.addView(avatar,lp(row,54,54,0,0,14,0));LinearLayout body=column(row);LinearLayout top=new LinearLayout(row.getContext());top.setGravity(Gravity.CENTER_VERTICAL);TextView name=label(row,c.name,16,ink,c.unread?Typeface.BOLD:Typeface.NORMAL);top.addView(name,weight(row,1));top.addView(label(row,formatTime(c.time),12,muted,Typeface.NORMAL));body.addView(top);TextView preview=label(row,c.preview.isEmpty()?"No message content":c.preview,14,c.unread?ink:muted,c.unread?Typeface.BOLD:Typeface.NORMAL);preview.setMaxLines(1);body.addView(preview,lp(row,-1,-2,0,5,0,0));row.addView(body,weight(row,1));if(c.unread)row.addView(label(row,"●",13,primary,Typeface.BOLD),lp(row,14,24,10,0,0,0));row.setContentDescription(c.name+", "+c.preview+(c.unread?", unread":""));row.setOnClickListener(v->listener.onConversationClicked(c));}
    @Override public int getItemCount(){return items.size();}
    static final class Holder extends RecyclerView.ViewHolder{Holder(@NonNull View v){super(v);}}
    private TextView avatar(View parent,String name){TextView a=label(parent,initials(name),17,ink,Typeface.BOLD);a.setGravity(Gravity.CENTER);GradientDrawable shape=new GradientDrawable();shape.setShape(GradientDrawable.OVAL);shape.setColor(colorFor(name));a.setBackground(shape);return a;}
    private LinearLayout column(View parent){LinearLayout x=new LinearLayout(parent.getContext());x.setOrientation(LinearLayout.VERTICAL);return x;}
    private TextView label(View p,String s,int size,int color,int style){TextView v=new TextView(p.getContext());v.setText(s);v.setTextSize(size);v.setTextColor(color);v.setTypeface(Typeface.DEFAULT,style);v.setGravity(Gravity.CENTER_VERTICAL);return v;}
    private LinearLayout.LayoutParams weight(View p,float w){return new LinearLayout.LayoutParams(0,-2,w);}
    private LinearLayout.LayoutParams lp(View p,int w,int h,int l,int t,int r,int b){LinearLayout.LayoutParams x=new LinearLayout.LayoutParams(w<0?-1:dp(p,w),h<0?-2:dp(p,h));x.setMargins(dp(p,l),dp(p,t),dp(p,r),dp(p,b));return x;}
    private int dp(View p,int v){return(int)(v*p.getResources().getDisplayMetrics().density+.5f);}
    private String initials(String n){if(n==null||n.trim().isEmpty())return "?";String[] a=n.trim().split(" ");return(a.length>1?(a[0].substring(0,1)+a[a.length-1].substring(0,1)):a[0].substring(0,Math.min(2,a[0].length()))).toUpperCase(Locale.ROOT);}
    private int colorFor(String n){int[] colors={0xffdfc4b9,0xffc0d4e8,0xffc9dfc8,0xffe8cb91,0xffc9c3e9};return colors[Math.abs(n==null?0:n.hashCode())%colors.length];}
    private String formatTime(String raw){try{return DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(Long.parseLong(raw)));}catch(Exception e){return raw;}}
}
