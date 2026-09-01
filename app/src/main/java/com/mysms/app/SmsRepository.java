package com.mysms.app;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.telephony.SmsManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Android platform boundary for real SMS and contact data. UI code never queries a provider directly. */
public final class SmsRepository {
    public interface Callback<T> { void onSuccess(T value); void onError(Exception error); }
    public static final class Conversation { public final long threadId; public final String address,name,preview,time; public final int messageCount; public final boolean unread; public Conversation(long id,String a,String n,String p,String t,int c,boolean u){threadId=id;address=a;name=n;preview=p;time=t;messageCount=c;unread=u;} }
    public static final class Message { public final long id,threadId,date; public final String address,body; public final int type,status; public Message(long i,long t,long d,String a,String b,int ty,int s){id=i;threadId=t;date=d;address=a;body=b;type=ty;status=s;} public boolean isOutgoing(){return type==Telephony.Sms.MESSAGE_TYPE_SENT||type==Telephony.Sms.MESSAGE_TYPE_OUTBOX;} }

    private final ContentResolver resolver;
    public SmsRepository(Context context) { resolver=context.getApplicationContext().getContentResolver(); }

    public void loadConversations(Callback<List<Conversation>> callback) {
        new Thread(() -> { try {
            Map<Long, Conversation> result = new HashMap<>();
            Uri uri = Telephony.Sms.CONTENT_URI;
            String[] projection = {Telephony.Sms.THREAD_ID, Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE, Telephony.Sms.READ};
            try (Cursor cursor = resolver.query(uri, projection, null, null, Telephony.Sms.DATE + " DESC")) {
                if (cursor != null) while (cursor.moveToNext()) {
                    long thread = cursor.getLong(0); if (result.containsKey(thread)) continue;
                    String address = cursor.getString(1); String preview = cursor.getString(2);
                    String name = resolveName(address); String time = String.valueOf(cursor.getLong(3));
                    boolean unread = cursor.getInt(4) == 0;
                    result.put(thread, new Conversation(thread,address,name,preview,time,1,unread));
                }
            }
            callback.onSuccess(new ArrayList<>(result.values()));
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

    public void send(String destination, String body, Callback<Void> callback) {
        new Thread(() -> { try { SmsManager.getDefault().sendTextMessage(destination,null,body,null,null); callback.onSuccess(null); } catch(Exception e){callback.onError(e);} }).start();
    }

    private String resolveName(String phone) {
        if(phone==null) return "Unknown sender";
        try (Cursor c=resolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME},ContactsContract.CommonDataKinds.Phone.NUMBER+"=?",new String[]{phone},null)) { if(c!=null&&c.moveToFirst()) return c.getString(0); } catch(Exception ignored){}
        return phone;
    }
}
