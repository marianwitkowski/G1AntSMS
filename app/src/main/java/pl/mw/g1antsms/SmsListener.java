package pl.mw.g1antsms;

/**
 * Created by marian on 28.12.2018.
 */

public interface SmsListener {

        public void messageReceived(String sender, String messageText);


}
