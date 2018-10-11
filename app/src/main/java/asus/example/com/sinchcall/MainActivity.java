package asus.example.com.sinchcall;

import android.content.pm.LabeledIntent;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import org.apache.http.params.HttpConnectionParams;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class MainActivity extends AppCompatActivity {

    //review it
    public static final int BASE64_FLAGS = Base64.NO_WRAP;
    private Button bt;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bt=(Button)findViewById(R.id.callbtn);


        //####################################### call
        bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        test();
                        Log.i("main", "click");
                    }
                }).start();
            }
        });

        //####################################### call

    }



    //( MD5 ( UTF8 ( [BODY] ) ) ) for hash

    private static byte[] md5Digest(String body) {
        MessageDigest md = null;
        byte[] bytesOfBody = null;
        try {
            Log.i("main", "md5");
            // UTF8 ( [BODY]
            bytesOfBody = body.getBytes("UTF-8");
            md = MessageDigest.getInstance("MD5");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return md.digest(bytesOfBody);
    }


    //Signature = Base64 ( HMAC-SHA256 ( applicationSecret, UTF8 ( StringToSign ) ) );
    private static String signature(String secret, String message) {
        String signature = "";
        try {
            Log.i("main", "signiture");
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(Base64.decode(secret.getBytes(), BASE64_FLAGS), "HmacSHA256");
            sha256_HMAC.init(secret_key);
            signature = Base64.encodeToString(sha256_HMAC.doFinal(message.getBytes()), BASE64_FLAGS);
        } catch (Exception e) {
            Log.e("Error", "Signature");
        }
        return signature;
    }

    public static void test() {
        try {

            String to = "Your Number";
            String key = "Your Key";
            String secret = "Your secret Key";


            // Timestamp
            Date date = new java.util.Date();
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            String timestamp = dateFormat.format(date);
            //key and secrect taking from sinch
            String httpVerb = "POST";
            String path = "/v1/callouts";
            String contentType = "application/json; charset=UTF-8";
            String canonicalizedHeaders = "x-timestamp:" + timestamp;
            String body = "{\n" + "\"method\":" + "\"ttsCallout\",\n" +
                    "\"ttsCallout\":{\n" + "\"cli\":\"+966\",\n" +
                    "\"destination\":{\n" + "\"type\":\"number\",\n" + "\"endpoint\":\"" + to + "\"\n},\n" +
                    "\"domain\":\"pstn\",\n" + "\"custom\":\"test\",\n" +
                    "\"locale\":\"ar-SA\",\n" + "\"text\":\"Hi\"\n"
                    + "}\n" + "}";

            Log.i("Main activity", body);

            String contentmd5=Base64.encodeToString(md5Digest(body),BASE64_FLAGS);

            String stringTosign =httpVerb+ "\n"+contentmd5+"\n"+contentType+"\n"+canonicalizedHeaders+"\n"+path;

            String signiture =signature(secret,stringTosign);
            String authorization="Application "+key+":"+signiture;

            //http request
            URL url=new URL("https://callingapi.sinch.com/v1/callouts");
            HttpURLConnection connection=(HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("content-type",contentType);
            connection.setRequestProperty("x-timestamp",timestamp);
            connection.setRequestProperty("authorization",authorization);

            OutputStream os=connection.getOutputStream();
            os.write(body.getBytes());
            StringBuilder response=new StringBuilder();
            BufferedReader br=new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            while ((line=br.readLine())!=null)
                response.append(line);
            br.close();
            os.close();
            Log.i("read",response.toString());


        }
        catch(IOException e){

            e.printStackTrace();

        }
    }

}
