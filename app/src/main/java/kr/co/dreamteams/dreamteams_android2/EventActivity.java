package kr.co.dreamteams.dreamteams_android2;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by godowondev on 2018. 5. 3..
 */

public class EventActivity extends Activity {

    ImageView imageView;
    TextView textView;
    String image_str;
    String event_link_url;
    String event_open_type;
    String event_title;
    Common common = new Common(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event);

        textView = (TextView) findViewById(R.id.textViewEventTitle);
        imageView = (ImageView) findViewById(R.id.eventImageView);

        image_str = common.getSP("event_image");
        event_title = common.getSP("event_title");
        event_link_url = common.getSP("event_link_url");
        event_open_type = common.getSP("event_open_type");

        Bitmap event_bmp = StringToBitMap(image_str);

        imageView.setImageBitmap(event_bmp);
        textView.setText(event_title);
    }

    public void notTodayBtnClicked(View view) {

        long now = System.currentTimeMillis();
        Date date = new Date(now);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String today = sdf.format(date);

        common.putSP("event_reject_date", today);

        finish();

    }

    public void closeBtnClicked(View view) {
        finish();
    }

    @Override
    public void finish() {
        super.finish();

        this.overridePendingTransition(R.anim.fade_in,R.anim.fade_out);
    }

    public void eventImageClicked(View view) {
        if(event_link_url.isEmpty()) {
            Intent intent = new Intent(EventActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        } else {

            if (event_open_type.equals("out")) {
                callBrowser(event_link_url);
            } else {
                Intent intent = new Intent(EventActivity.this, MainActivity.class);
                intent.putExtra("sUrl", event_link_url);
                startActivity(intent);
                finish();
            }
        }
    }

    public Bitmap StringToBitMap(String encodedString){
        try{
            byte [] encodeByte= Base64.decode(encodedString, Base64.DEFAULT);
            Bitmap bitmap= BitmapFactory.decodeByteArray(encodeByte, 0, encodeByte.length);
            return bitmap;
        }catch(Exception e){
            e.getMessage();
            return null;
        }
    }

    public void callBrowser(String url) {
        try {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(i);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
