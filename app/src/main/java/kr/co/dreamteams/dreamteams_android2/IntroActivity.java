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
    WebView hiddenWebView;
    String intro_image_version = null;
    String event_reject_date = null;
    Common common = new Common(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);

        imageView = (ImageView) findViewById(R.id.imageViewIntro);
        hiddenWebView = (WebView) findViewById(R.id.webViewIntro);

        String image =  common.getSP("intro_image");
        event_reject_date =  common.getSP("event_reject_date");

        //프로젝트의 해시키값을 추출하기위한 1회용 코드
        //getHashKey();

        //hiddenWebView를 이용한 sendDeviceInfo
        String checkUrl = checkDeviceInfo();
        common.log("sendDeviceInfo - " + checkUrl);
        hiddenWebView.loadUrl(checkUrl);

        // 저장된 인트로이미지가 없을 경우 강제 생성
        if (image.isEmpty()) {
            setDefaultIntroImage();
        }

        Bitmap intro_image = common.StringToBitMap(image);
        //imageView.setImageBitmap(intro_image);
        imageViewFadeIn(intro_image,intro_image);

        if(common.isNetWorkAvailable(this)==false) {
            Intent intent = new Intent(IntroActivity.this, NetworkDisabledActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.fade_out, R.anim.fade_in);
        }
        else {

            getIntroImage();

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
                    getEventData();
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

        String image = "iVBORw0KGgoAAAANSUhEUgAABDgAAAeACAIAAACkA3BdAAAAA3NCSVQICAjb4U/gAAAgAElEQVR4\n" +
                "        nOy9aZrruq4lCPjGy5xqTaLGUPPNr66QP0iCaNlIchM7jLNP2JZIEOxALLDD/+f//f/+1//8D8BB\n" +
                "        QACAiBARAjwe5hUiAJD8Id+ZwFSYICIA5sHq00f6S3AzKTrRGj0ADoBHSQ0BEQLZOFbJVXsEaETE\n" +
                "        8qdkncpvy67ERhYDAY4mSQ+MOqUStqaGPhfwMPIg/7RFD3QwA8SHE1AyRgB4jIrSvsVaSkJOEvIA\n" +
                "        kgkJnmQD4Eeo3pCOGXPB+CVp1hkHEi9RPe5Fii58f0M1MHL9FnpI2VYoLafymOI3LPZBXtYmx6oA\n" +
                "        eeoioUQyL2LGbp9EOVYhiQCA8GFLpfW5QcH3N+QfZRF8UB+nlPTR3vWfLfhDJMrPH1BKisZdpESl\n" +
                "                /jVQSP1le+q7PMofPaLSO02h4AMAqHX1+pg7PgICEtag/W39C/17+Xjwq/oCEPGBDxEMerCHZdLj\n" +
                "        gvneBZffdTaj4vxSpQ8plreL8WIBQo3+9kJYJ+ofxI+oaqli/rRv1D6ofFL9W17Xn0B0yAAqpPtZ\n" +
                "        Q4J8xakQ824ikJDNSgcss8xVDUuynojEj1jn5o8orPAbabXt+HDrrY50NnhkpqgEegBtNTEv/y0U\n" +
                "        DB8PAPg//+f///nf/+t//vf/+p/jOIgHTDWi1SENnelVBy5t5NvEhKFDRMLGVqO+xQLWUH7IMAQA\n" +
                "                cDQOzUZDLXtgKT26zEnttLEYuGVFIEXIjzXco5mE5ScRlSJriaqWinpMraBGGOwqCaiDODrrngEK\n" +
                "        tvIhovqCDs4oqngm4w1IqY80s/JhyTP6ZqEYR3zMU+anWpNvSUm0EtckHdiWCb/ooe1ZOi53S2tJ\n" +
                "        SgmQyyzTA6r1Sn4SdxLoUAmzSA0u5XePRuY0tQzF6SzighUBesHJJiaHzIVEdsRoWVPJhQz+E//k\n" +
                "        piurE8X/mZ4HYNTPPhEJVAIhQogeWfAog0mUAoCPZvpTaY4dGwgE0jFD+6pwRfDl8VBMHo848OOB\n" +
                "        oLENIjwYI1mgMqUeaN1akIyfbWOMU/93klqlDxTpSyeIiLBDhQ5NJOQ4GtxgQFK+a1iisIqBKwGG\n" +
                "        AfEEAMRD6KDJYxUBZoInHBgARBs1Q29mEzwdqORCrL+bvSUuAGUK4gSojHlG1pQZqwDg5z+PHzqO\n" +
                "        4zjoIMKDipnUrPQiRhmwZJ1htQVJmQsUSCYGyDIHwmLx7Ey105WE9Wd1PR5iaBdw5ejRZVrN7cqF\n" +
                "        2ZgeAAjIkx1R4dZxVPG1znVhgCLVSqL/dhZN5AciHNBeuHknNFXjgMpDm8qMAxGx22l1hua/0Az9\n" +
                "        8pIxIQ/nFtd1vq1spLnCHw4NY2OJ8lUrVIQHiEhs55jSMSJAF6Gl4ContEmY8yFtP0SgpmhaIxAy\n" +
                "        hDaITVSXhCo06siSZ1SMU4GrycrLTMw7g3JMNInwNTMOYMTOg46CZRFUT5XPooo6whSZSNvfIykT\n" +
                "        ubgypLeLRel9d6J0FbewgepHpLBR6fdrhrISsiTJOqwUhO36XUT+jjy0PtmOk4iviSl/V+0qHS3s\n" +
                "        sEKROQJA6gMJIRad3J+00UOqCwDA4yBEOLBqvaJBiCfCyxiFJBQ+a7k7S4FVwhsNZ3pb6vd4Fvbp\n" +
                "        jYX9pRsomK8wEIWxgsEe4CEKPwERt30B+7ZJoFCBxhviDYm/SVYm77M4jd7WlIdCDCHTWHxbeDXc\n" +
                "        UhkJQBKXS+TE64/++9///gARHAR0QG0IylQiqSuFxcIwamYPqGxgl0M9b6ZYk5YA4NFtEmUustF2\n" +
                "        yCigI4sXPBtR0VaLVdZpsMlQDdsHAtGDV9MgPIihlBD5UQ0isuvlGAc1eDUsGVS/Ksyo0wNHzz4S\n" +
                "        CUc7J1URCgEAPIBITG8QARJKoOJnS1qRNF4oYCTYLDckVfNLiPDfsnYFEIB44RM1tIJHUxAlKuMm\n" +
                "        EZLT6n9EzamWHfYv1K2oTTVSj92+ypAY9K3Qbu6ilLwfLSyynQUAyJ4GZvLwPFEICLogwD5ztijq\n" +
                "        eWcvOsofIl6xIHl6DYJgDXAmLJpgawgIRImh/JU5XYSoK7amyKuBhrr/gVKIUY70KCTLIxLOBTw4\n" +
                "        MaN4tZqQ0UnIY0BOE3qiMDhpq5C1uh5TqUzbTmaxpBhNbxE0xeTSJqF5oIGW+oVRDVHFG1B7kwEt\n" +
                "        8m/lj6yGqGur8pz0RNM+bkljvNf4uJK6gYAz9nGAa1n+XcBjXBq/Ky+vJDEA9kkMADoMRAGxAKxh\n" +
                "        lWxuJJlC6RCF+TD6cRMpEiOxmHIlmJ9RabYmPwPZLMw4bN6aQpFvlqz5J5BVHwNDYkDKAHcpkAk3\n" +
                "        CzV6TgAA/wVAtgcRAeAg+sFUDqcjkzyOsuGCLdMxDyGg0vpyfE3kfuJK0qlIAFBgTGxYr/OA3TyJ\n" +
                "        wugDfDEIUJgINoL6vVlFjE8EnpGkTEW7tD4T5dRwIDO/FlZ/dShuj1CU3qAf9BKWMmztZjlB0dxO\n" +
                "        fWMsWx8uRi0BjVRD9YANYmuVLwLlSKh9xaFqSl64pn7SH++SthA4BEuuwsPEreCRTpEAx4GWc2RB\n" +
                "        0CgUqS/xxGnCSs4Ri4fIr6K3Ytr5TUbj7wItt0j43ix/En1LYoWQ/Ukkn5SV6WFwUDG4iwe2bXcv\n" +
                "        NZsGiuOa53FKWg6jo2OXwXcSf++r5I9oOufB0mpMB4vkjyuJEsAPYJu90Ot/agresoK+0uUhLLRQ\n" +
                "        Fky+NwuJW6WCVg/LTKV+AIh9q83GKa7MVBINipNyQ+F4N2ulCKhvhVeWVGAnVeFILJGqSZdUpFST\n" +
                "        KsRe3y273ZUvLJ6DXY2uoPsH5UYrNWlEenIFYC/bEqA7kzmD1JAVHggA+CgqQ5aAyj+jKSUTkTR4\n" +
                "        8uWPMm82t7IMtMFD0ApQ7PrpZnRvjjoxin5KGbAsAwQAeMjX2naLuwiXX9x4L+g1kdP4/eC3yJ3W\n" +
                "        2jN5UDcYxbjXQ8STTOhaH6GxPxBFNa9MRi3slEh5ltGk7jPanjL+CsFXBgqiNl/aj9ZdURnX+b6I\n" +
                "        bSCCKWHxyP0FaAuuqjBFIrHuisrWLJRLtsR3+dNKJve99OyKWFzoUVQWEmUWpjQMNR+bPwS0XEw6\n" +
                "        jJ5l/iPsrY+gb0lI6hiDKnQgtu/qsNaWzmJb/9nmLhRiqICiRenMWak1hEJAiG3qhJNqPKnBlRaZ\n" +
                "        iLnXsbpKWlRqjSDXCRFCPvsgRVNPwl/O6JYFt0tXWtt5qBIzG8misrk8L9vCkSrcAlS2yZV0tw49\n" +
                "        Bri9F9OovJ1hcGvyBwTnj6ViuK8XhVnhRD1gM5tBN6l9edIhkcazAUdoysWskyDUUslZ5Az7o+Vy\n" +
                "        b1rqjkoLbMWJ+chhKIc1N0mSBcp/W9LYYuIcQBMochWk8pk0FuFKC9rUXRxqrXGIXgUW+k5itTim\n" +
                "        Rc2jm2yyPlubaQ8kmKaYkY8pR5FaGP2jjPFqLZZkIjCSwiHlUf9rhdBgqj3JQfg2XRrE32i3vivp\n" +
                "        r6ne6FsShawyRT7ep6rihl+a06UPC1RRRfO2oBgEpfZVqEZjiRLHuBQ7YtHRNzu7NJhvreFf3nRW\n" +
                "        i3E0FK4y/9ksoIGJjN3ImotxhtS6d/013nAMQGViAAHOLw8LMtK28heXrHgtN546wBRU0aiIju54\n" +
                "        QEB4uIwTFv99Ya7ma5oS4JmnKqbaZU5VfisFez/kAvTmuLDgWEh1ACCpnKMMVAWU+dCzS9CKzScR\n" +
                "        gwdM+4oyzl1UZ/aQfEsi1gi0RIYzlya1LOsdVskZaZHEXSZj15cfWBUzwsDzk8iJ+vsI+7si9kOB\n" +
                "        ee4yaOs4grxxR6gtWr/jTWhZSYbP0gzqcgnXG5gpNbLlN6dTmjrkgKKsVtAeoqn+1kJ509tsPVWb\n" +
                "        OinVJNNG7q31DYrATsMpZGLQHk+zMPwwEykiJ4Gc8sld0OU0cPmClj9Mf7QkJAYxA4RU+hyiGSkN\n" +
                "        XHSjhaLIWJ0f9RSx+qyNldjHSwY6CHXpjF/0pcUaJRlmVC5gw5EJskt/pO";

        SharedPreferences put_pref = getSharedPreferences("shared_pref", MODE_PRIVATE);
        SharedPreferences.Editor put_editor = put_pref.edit();
        put_editor.putString("intro_image", image);
        put_editor.putString("intro_image_version", "20170105113946");
        put_editor.commit();

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

    public String checkDeviceInfo(){

        String device_id;
        String device_token;
        String device_model;
        String app_version;

        device_id = common.getSP("device_id");

        if(device_id.isEmpty())
        {
            String new_device_id = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
            String new_device_token = FirebaseInstanceId.getInstance().getToken();
            String new_device_model = Build.BRAND + "/" + Build.MODEL + "/" + Build.ID + "/" + Build.VERSION.RELEASE;
            String new_app_version = BuildConfig.VERSION_NAME;

            common.putSP("device_id", new_device_id);
            common.putSP("device_token", new_device_token);
            common.putSP("device_model", new_device_model);
            common.putSP("app_version", new_app_version);
        }

        device_id = common.getSP("device_id");
        device_token = common.getSP("device_token");
        device_model = common.getSP("device_model");
        app_version = common.getSP("app_version");

        String url = getResources().getString(R.string.api_url)
                + "?action=sendDeviceInfo"
                + "&device_type=Android"
                + "&device_id=" + device_id
                + "&device_token=" + device_token
                + "&device_model=" + device_model
                + "&app_version=" + app_version
                ;
        return url;
    }
}