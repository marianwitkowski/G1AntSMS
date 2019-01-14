package pl.mw.g1antsms;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {


    private static final String TAG = "SMSMainActivity";
    private static final int SMS_PERMISSION_CODE = 0;

    private static EditText etHost;
    private static EditText etPort;
    private static EditText etRetry;
    private static Button btnStart;
    private static Context ctx;

    private static boolean isRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ctx = this;
        etHost = (EditText)findViewById(R.id.etHost);
        etPort = (EditText)findViewById(R.id.etPort);
        etRetry = (EditText)findViewById(R.id.etRetry);
        btnStart = (Button)findViewById(R.id.btnStart);
        if (btnStart!=null) {
            btnStart.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    startStopForward();
                }
            });
        }

        if (!hasReadSmsPermission()) {
            showRequestPermissionsInfoAlertDialog();
        }
    }


    @Override
    protected void onDestroy() {
        SmsReceiver.unbindListener();
        super.onDestroy();
    }


    class RequestTask extends AsyncTask<String, String, String> {

        private String url;
        String responseString = null;
        private Integer retry;

        RequestTask(String url, Integer retry) {
            this.url = url;
            this.retry = retry;
        }

        @Override
        protected String doInBackground(String... params) {
            HttpClient httpclient = new DefaultHttpClient();
            HttpConnectionParams.setConnectionTimeout(httpclient.getParams(), 10000);
            HttpConnectionParams.setSoTimeout(httpclient.getParams(), 10000);
            HttpResponse response;

            while (this.retry>0) {
                try {
                    response = httpclient.execute(new HttpGet(this.url));
                    StatusLine statusLine = response.getStatusLine();
                    if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        response.getEntity().writeTo(out);
                        responseString = out.toString();
                        out.close();
                        if (responseString!=null && responseString.toUpperCase().trim().equals("OK")) {
                            responseString = responseString.toUpperCase().trim();
                            break;
                        } else {
                            responseString = null;
                            Thread.sleep(5000);
                        }
                    } else {
                        response.getEntity().getContent().close();
                        throw new IOException(statusLine.getReasonPhrase());
                    }
                } catch (Exception e) {
                    this.retry--;
                    try {
                        Thread.sleep(5000);
                    } catch (Exception exc) {}
                }
            }
            return responseString;
        }

        @Override
        protected void onPostExecute(String result) {

            String msg = null;
            if (responseString!=null && responseString.equals("OK")) {
                msg = "Wiadomość SMS została przekazana";
            } else {
                msg = "Wiadomość SMS nie została przekazana";
            }
            Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show();
        }
    }

    private void startStopForward() {
        if (isRunning) {
            deactivateGUI();
            SmsReceiver.unbindListener();
            isRunning = false;
            btnStart.setText("START");
        } else {
            if (!hasReadSmsPermission()) {
                Toast.makeText(this, "Nie wyraziłeś zgody na dostęp przez aplikację do wiadomości SMS", Toast.LENGTH_LONG).show();
                return;
            }
            SmsReceiver.bindListener(new SmsListener() {
                @Override
                public void messageReceived(String sender, String messageText) {

                    Log.e("Message",messageText);
                    //Toast.makeText(MainActivity.this,"Wiadomośc od " +sender+ " o treści: "+messageText,Toast.LENGTH_SHORT).show();
                    sendData(sender, messageText);

                }
            });
            activateGUI();
            isRunning = true;
            btnStart.setText("STOP");
        }
    }

    private void sendData(String sender, String message) {
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
            String url = String.format("http://%s:%s/?sender=%s&msg=%s&ts=%d",
                    etHost.getText().toString(), etPort.getText().toString(),
                    URLEncoder.encode(sender, "UTF-8"), URLEncoder.encode(message, "UTF-8"),
                    format.format(new Date()));
            new RequestTask(url, Integer.parseInt(etRetry.getText().toString())).execute();
        } catch (Exception exc) {
            Toast.makeText(MainActivity.this, exc.getMessage(),Toast.LENGTH_SHORT).show();
        }
    }


    private void deactivateGUI() {
        etHost.setEnabled(true);
        etPort.setEnabled(true);
        etRetry.setEnabled(true);
    }


    private void activateGUI() {
        etHost.setEnabled(false);
        etPort.setEnabled(false);
        etRetry.setEnabled(false);
    }

    private void showRequestPermissionsInfoAlertDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.permission_alert_dialog_title);
        builder.setMessage(R.string.permission_dialog_message);
        builder.setPositiveButton(R.string.action_ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                requestReadAndSendSmsPermission();
            }
        });
        builder.show();
    }

    private boolean hasReadSmsPermission() {
        return ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestReadAndSendSmsPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.READ_SMS)) {
            Log.d(TAG, "No permission requested");
            return;
        }
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS},
                SMS_PERMISSION_CODE);
    }

}
