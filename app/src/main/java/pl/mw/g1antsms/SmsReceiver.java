package pl.mw.g1antsms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;

/**
 * Created by marian on 28.12.2018.
 */

public class SmsReceiver extends BroadcastReceiver {

    //interface
    private static SmsListener mListener;

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle data  = intent.getExtras();

        Object[] pdus = (Object[]) data.get("pdus");

        for(int i=0;i<pdus.length;i++){
            SmsMessage smsMessage = SmsMessage.createFromPdu((byte[]) pdus[i]);

            String sender = smsMessage.getDisplayOriginatingAddress();
            String messageBody = smsMessage.getMessageBody();
            mListener.messageReceived(sender, messageBody);
        }

    }

    public static void bindListener(SmsListener listener) {
        mListener = listener;
    }

    public static void unbindListener() {
        mListener = null;
    }
}
