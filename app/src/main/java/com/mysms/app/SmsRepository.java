package com.mysms.app;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Android platform boundary for real SMS and contact data. UI code never queries a provider directly. */
public final class SmsRepository {
    public interface Callback<T> { void onSuccess(T value); void onError(Exception error); }
    public static final class Conversation { public final long threadId; public final String address,name,preview,time; public final int messageCount; public final boolean unread; public Conversation(long id,String a,String n,String p,String t,int c,boolean u){threadId=id;address=a;name=n;preview=p;time=t;messageCount=c;unread=u;} }
    public static final class Message { public final long id,threadId,date; public final String address,body; public final int type,status; public Message(long i,long t,long d,String a,String b,int ty,int s){id=i;threadId=t;date=d;address=a;body=b;type=ty;status=s;} public boolean isOutgoing(){return type==Telephony.Sms.MESSAGE_TYPE_SENT||type==Telephony.Sms.MESSAGE_TYPE_OUTBOX;} }

    private final ContentResolver resolver; private final Context context;
    public SmsRepository(Context context) { this.context=context.getApplicationContext(); resolver=this.context.getContentResolver(); }
    public static final class SimOption { public final int subscriptionId,slot; public final String label,placeholder; public SimOption(int id,int s,String carrier){subscriptionId=id;slot=s;label="SIM "+(s+1)+(TextUtils.isEmpty(carrier)?"":" — "+carrier);placeholder=TextUtils.isEmpty(carrier)?"Text message":carrier+" • Text message";} }
    public List<SimOption> getActiveSubscriptions() { List<SimOption> out=new ArrayList<>(); try { List<SubscriptionInfo> infos=SubscriptionManager.from(context).getActiveSubscriptionInfoList(); if(infos!=null) for(SubscriptionInfo info:infos){CharSequence name=info.getCarrierName();out.add(new SimOption(info.getSubscriptionId(),info.getSimSlotIndex(),name==null?null:name.toString()));} } catch(Exception ignored){} if(out.isEmpty()) out.add(new SimOption(SubscriptionManager.getDefaultSmsSubscriptionId(),0,null)); return out; }

    public void loadConversations(Callback<List<Conversation>> callback) {
        new Thread(() -> { try {
            Map<Long, ConversationAccumulator> result = new LinkedHashMap<>();
            Uri uri = Telephony.Sms.CONTENT_URI;
            String[] projection = {Telephony.Sms.THREAD_ID, Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE, Telephony.Sms.READ};
            try (Cursor cursor = resolver.query(uri, projection, null, null, Telephony.Sms.DATE + " DESC")) {
                if (cursor != null) while (cursor.moveToNext()) {
                    if (cursor.isNull(0) || cursor.isNull(1) || cursor.isNull(3)) continue;
                    long thread = cursor.getLong(0); String address = cursor.getString(1);
                    String preview = cursor.isNull(2) ? "" : cursor.getString(2); long date = cursor.getLong(3);
                    if (thread <= 0 || TextUtils.isEmpty(address) || date <= 0) continue;
                    ConversationAccumulator item = result.get(thread);
                    if (item == null) { item = new ConversationAccumulator(thread,address,resolveName(address)); result.put(thread,item); }
                    item.count++; item.unread |= cursor.getInt(4) == 0;
                    if (date >= item.date) { item.date=date; item.preview=preview; }
                }
            }
            List<Conversation> conversations = new ArrayList<>();
            for (ConversationAccumulator item : result.values()) conversations.add(item.build());
            conversations.sort((a,b) -> Long.compare(parseDate(b.time), parseDate(a.time)));
            callback.onSuccess(conversations);
        } catch (Exception e) { callback.onError(e); } }).start();
    }

    public void loadMessages(long threadId, Callback<List<Message>> callback) {
        new Thread(() -> { try {
            List<Message> result = new ArrayList<>();
            String[] p = {Telephony.Sms._ID,Telephony.Sms.THREAD_ID,Telephony.Sms.DATE,Telephony.Sms.ADDRESS,Telephony.Sms.BODY,Telephony.Sms.TYPE,Telephony.Sms.STATUS};
            try (Cursor c=resolver.query(Telephony.Sms.CONTENT_URI,p,Telephony.Sms.THREAD_ID+"=?",new String[]{String.valueOf(threadId)},Telephony.Sms.DATE+" ASC")) {
                if(c!=null) while(c.moveToNext()) result.add(new Message(c.getLong(0),c.getLong(1),c.getLong(2),c.getString(3),c.getString(4),c.getInt(5),c.getInt(6)));
            }
            callback.onSuccess(result);
        } catch(Exception e){callback.onError(e);} }).start();
    }

    public void markThreadRead(long threadId) { try { resolver.update(Telephony.Sms.CONTENT_URI, valuesRead(), Telephony.Sms.THREAD_ID+"=?", new String[]{String.valueOf(threadId)}); } catch (Exception ignored) {} }
    private android.content.ContentValues valuesRead(){ android.content.ContentValues v=new android.content.ContentValues();v.put(Telephony.Sms.READ,1);return v; }

    public void send(String destination, String body, Callback<Void> callback) { send(destination,body,SubscriptionManager.getDefaultSmsSubscriptionId(),callback); }
    public void send(String destination, String body, int subscriptionId, Callback<Void> callback) { new Thread(() -> { try { SmsManager manager=SmsManager.getSmsManagerForSubscriptionId(subscriptionId); manager.sendTextMessage(destination,null,body,null,null); callback.onSuccess(null); } catch(Exception e){callback.onError(e);} }).start(); }

    private String resolveName(String phone) {
        if (TextUtils.isEmpty(phone)) return "Unknown sender";
        try {
            Uri lookup = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phone));
            try (Cursor c = resolver.query(lookup, new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME}, null, null, null)) {
                if (c != null && c.moveToFirst() && !TextUtils.isEmpty(c.getString(0))) return c.getString(0);
            }
        } catch (Exception ignored) { }
        return phone;
    }
    private long parseDate(String raw) { try { return Long.parseLong(raw); } catch (Exception e) { return 0; } }
    private static final class ConversationAccumulator {
        final long thread; final String address,name; String preview=""; long date=0; int count=0; boolean unread=false;
        ConversationAccumulator(long t,String a,String n){thread=t;address=a;name=n;}
        Conversation build(){return new Conversation(thread,address,name,preview,String.valueOf(date),count,unread);}
    }
}
