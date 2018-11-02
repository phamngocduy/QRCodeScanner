package com.blikoon.qrcodescannerlibrary;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.blikoon.qrcodescanner.QrCodeActivity;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.util.ArrayList;
import java.util.List;

import vn.istech.login.MainActivity;

public class ScanActivity extends AppCompatActivity {
    private Button button;
    private static final int REQUEST_CODE_QR_SCAN = 101;
    private final String LOGTAG = "QRCScanner-MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);
        button = (Button) findViewById(R.id.button_start_scan);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Start the qr scan activity
                Intent i = new Intent(ScanActivity.this, QrCodeActivity.class);
                startActivityForResult(i, REQUEST_CODE_QR_SCAN);
            }
        });

    }

    public String ViewBag = null;
    private boolean isProcessing = false;
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if(resultCode != Activity.RESULT_OK)
        {
            Log.d(LOGTAG,"COULD NOT GET A GOOD RESULT.");
            if(data==null)
                return;
            //Getting the passed result
            String result = data.getStringExtra("com.blikoon.qrcodescanner.error_decoding_image");
            if( result!=null)
            {
                AlertDialog alertDialog = new AlertDialog.Builder(ScanActivity.this).create();
                alertDialog.setTitle("Scan Error");
                alertDialog.setMessage("QR Code could not be scanned");
                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                alertDialog.show();
            }
            return;

        }
        if(requestCode == REQUEST_CODE_QR_SCAN)
        {
            if(data==null)
                return;
            //Getting the passed result
            String result = data.getStringExtra("com.blikoon.qrcodescanner.got_qr_scan_relult");
            Log.d(LOGTAG,"Have scan result in your app activity :"+ result);

            class Runner implements Runnable {
                ScanActivity activity;
                Runner(ScanActivity activity) {
                    this.activity = activity;
                }
                public void run() {
                    String url = "http://login.is-tech.vn/login/authenticate?client=";
                    DefaultHttpClient client = new DefaultHttpClient();
                    HttpPost post = new HttpPost(activity.ViewBag.substring(0, activity.ViewBag.indexOf("?client=")));
                    try {
                        List<NameValuePair> form = new ArrayList<NameValuePair>(2);
                        form.add(new BasicNameValuePair("client", activity.ViewBag.replace(url, "")));
                        form.add(new BasicNameValuePair("cookie", MainActivity.COOKIE));
                        post.setEntity(new UrlEncodedFormEntity(form));
                        HttpResponse response = client.execute(post);
                        activity.ViewBag = EntityUtils.toString(response.getEntity());
                        if (activity.ViewBag.startsWith("OK"))
                            activity.ViewBag = "Authorized successfully";
                    } catch (Exception e) {
                        activity.ViewBag = e.toString() + "\n" + e.getMessage();
                    } finally {
                        client.getConnectionManager().shutdown();
                    }
                }
            }
            if (isProcessing == false) {
                isProcessing = true;
                ViewBag = result;
                Thread thread = new Thread(new Runner(this));
                try {
                    thread.start();
                    thread.join();
                    result = ViewBag;
                } catch (Exception e) {
                    result = e.toString() + "\n" + e.getMessage();
                } finally {
                    isProcessing = false;
                }
            }

            AlertDialog alertDialog = new AlertDialog.Builder(ScanActivity.this).create();
            alertDialog.setTitle("Scan result");
            alertDialog.setMessage(result);
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
            alertDialog.show();
        }
    }
}
