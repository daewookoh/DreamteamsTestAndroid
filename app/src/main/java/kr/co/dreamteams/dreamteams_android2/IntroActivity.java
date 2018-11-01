package kr.co.dreamteams.dreamteams_android2;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Base64;
import android.util.Log;
import android.webkit.WebView;
import android.widget.ImageView;

import com.google.firebase.iid.FirebaseInstanceId;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by godowondev on 2018. 5. 1..
 */

public class IntroActivity extends Activity {

    ImageView imageView;
    String intro_image_version = null;
    String event_reject_date = null;
    Common common = new Common(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);

        imageView = (ImageView) findViewById(R.id.imageViewIntro);

        //setDefaultIntroImage(); //인트로이미지 초기화 원할경우 실행
        String image =  common.getSP("intro_image");
        event_reject_date =  common.getSP("event_reject_date");
        common.putSP("app_start_yn","Y");

        //프로젝트의 해시키값을 추출하기위한 1회용 코드
        //getHashKey();

        // 저장된 인트로이미지가 없을 경우 강제 생성
        if (image.isEmpty()) {
            setDefaultIntroImage();
        }

        Bitmap intro_image = common.StringToBitMap(image);
        imageView.setImageBitmap(intro_image);
        imageViewFadeIn(intro_image,intro_image);

        if(common.isNetWorkAvailable(this)==false) {
            Intent intent = new Intent(IntroActivity.this, NetworkDisabledActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.fade_out, R.anim.fade_in);
        }
        else {

            //getIntroImage();

            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Intent intent = new Intent(IntroActivity.this, MainActivity.class);
                    startActivity(intent);
                    overridePendingTransition(R.anim.fade_in,R.anim.fade_out);
                    finish();

                }
            }, 2000);

        }

    }

    @Override
    public void finish() {
        super.finish();

        this.overridePendingTransition(R.anim.fade_in,R.anim.fade_out);
    }

    private void getIntroImage(){
        String sUrl = getResources().getString(R.string.api_url);

        ContentValues values = new ContentValues();
        values.put("action", "getIntroImage");
        values.put("device_type", "Android");

        IntroActivity.HttpAsyncRequest httpAssyncRequest = new IntroActivity.HttpAsyncRequest(sUrl, values, "INTRO");
        httpAssyncRequest.execute();
    }

    private void getEventData(){
        String sUrl = getResources().getString(R.string.api_url);

        ContentValues values = new ContentValues();
        values.put("action", "getEventData");
        values.put("device_type", "Android");

        IntroActivity.HttpAsyncRequest httpAssyncRequest = new IntroActivity.HttpAsyncRequest(sUrl, values, "EVENT");
        httpAssyncRequest.execute();
    }

    // 비동기식 http 통신
    public class HttpAsyncRequest extends AsyncTask<Void, Void, String> {

        private String url;
        private ContentValues values;
        private String type;

        public HttpAsyncRequest(String url, ContentValues values, String type) {

            common.log(type + " 비동기식 http 접속");
            this.url = url;
            this.values = values;
            this.type = type;
        }

        @Override
        protected String doInBackground(Void... params) {

            String result; // 요청 결과를 저장할 변수.
            result = common.httpRequest(url, values);

            return result;
        }

        @Override
        protected void onPostExecute(String result) {
            //super.onPostExecute(result);

            try {
                result = URLDecoder.decode(result, "utf-8");

                JSONObject jObject1 = new JSONObject(result);
                String res_code = jObject1.getString("result_code");
                //String res_msg = jObject1.getString("result_msg");
                //String res_data = jObject1.getString("data");

                // 성공시에만 실행
                if(res_code.equals("0000")) {

                    if(type=="INTRO") {
                        //JSONObject jObject2 = new JSONObject(res_data);
                        String version = jObject1.optString("version", "");
                        String img_url = jObject1.optString("img_url", "");

                        String old_version = common.getSP("intro_image_version");

                        intro_image_version = version;

                        common.log("intro_img_url : " + img_url);
                        common.log("intro_new_version : " + version);
                        common.log("intro_old_version : " + old_version);

                        if (version.compareTo(old_version) > 0) {
                            IntroActivity.DownImage downImage = new IntroActivity.DownImage(img_url,"INTRO");
                            downImage.execute();
                        } else {
                            getEventData();
                        }
                    }else if(type=="EVENT"){
                        //JSONObject jObject2 = new JSONObject(res_data);
                        String img_url = jObject1.optString("img_url","");

                        if(!img_url.isEmpty()) {
                            String link_url = jObject1.optString("link_url","");
                            String open_type = jObject1.optString("open_type","");
                            String title = jObject1.optString("title","");

                            common.putSP("event_img_url", img_url);
                            common.putSP("event_link_url", link_url);
                            common.putSP("event_open_type", open_type);
                            common.putSP("event_title", title);
                            common.putSP("event_image", "");

                            IntroActivity.DownImage downImage = new IntroActivity.DownImage(img_url,"EVENT");
                            downImage.execute();
                        }
                    }
                }else {
                    if(type=="INTRO") {
                        getEventData();
                    }
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (JSONException e){
                e.printStackTrace();
            }
        }
    }

    // 비동기식 DownImage
    private class DownImage extends AsyncTask<String, Void, Bitmap> {

        private String url;
        private String type;

        public DownImage(String url, String type) {

            common.log(type + " 비동기식 DownImage " + type + " 접속");
            this.url = url;
            this.type = type;
        }

        @Override
        protected Bitmap doInBackground(String... urls) {
            String urldisplay = url;
            Bitmap bitmap = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                bitmap = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
            return bitmap;
        }

        protected void onPostExecute(Bitmap result) {
            if(type=="INTRO") {
                if (result != null) {

                    // 인트로 이미지 변경시 fade 처리
                    SharedPreferences get_pref = getSharedPreferences("shared_pref", MODE_PRIVATE);
                    String first_image = get_pref.getString("intro_image", "");
                    Bitmap firstBitmap = common.StringToBitMap(first_image);
                    imageViewFadeIn(firstBitmap, result);
                    // 인트로 이미지 변경시 fade 처리끝

                    String image = common.BitMapToString(result);
                    common.putSP("intro_image", image);
                    common.putSP("intro_image_version", intro_image_version);

                }

                long now = System.currentTimeMillis();
                Date date = new Date(now);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                String today = sdf.format(date);

                if (!event_reject_date.equals(today)) {
                    getEventData();
                }
            }else if(type=="EVENT"){
                if(result!=null){
                    Bitmap emptyBitmap = Bitmap.createBitmap(result.getWidth(), result.getHeight(), result.getConfig());
                    String image = common.BitMapToString(result);

                    if(!image.isEmpty()) {
                        common.log("event_img_save_success");
                        common.putSP("event_image", image);

                        Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                Intent intent = new Intent(IntroActivity.this, EventActivity.class);
                                startActivity(intent);
                            }
                        }, 3000);
                    }
                }

            }
        }
    }

    // 초기 인트로 이미지 강제 생성
    public void setDefaultIntroImage(){

        Bitmap bm = BitmapFactory.decodeResource(getResources(), R.mipmap.dreamteams_intro_android);
        String image = common.BitMapToString(bm);

        common.putSP("intro_image", image);
        common.putSP("intro_image_version", "20170105113946");

    }

    public void imageViewFadeIn(Bitmap first, Bitmap second){
        Drawable[] layers = new Drawable[2];
        layers[0] = new BitmapDrawable(getResources(), first);
        layers[1] = new BitmapDrawable(getResources(), second);

        TransitionDrawable transitionDrawable = new TransitionDrawable(layers);
        imageView.setImageDrawable(transitionDrawable);
        transitionDrawable.startTransition(500);
    }

    private void getHashKey(){
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_SIGNATURES);
            for (Signature signature : info.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                common.log("key_hash="+Base64.encodeToString(md.digest(), Base64.DEFAULT));
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

}