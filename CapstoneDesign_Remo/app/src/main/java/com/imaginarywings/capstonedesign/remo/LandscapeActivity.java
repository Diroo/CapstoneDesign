package com.imaginarywings.capstonedesign.remo;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.TedPermission;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.imaginarywings.capstonedesign.remo.Consts.API_URL;
import static com.imaginarywings.capstonedesign.remo.Consts.DEFAULT_URL;

/**
 * Created by S.JJ on 2017-04-05.
 */

public class LandscapeActivity extends Activity {
    private final String TAG = getClass().getSimpleName();

    @BindView(R.id.images_layout) LinearLayout mImagesLayout;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landscape);
        ButterKnife.bind(this);

        //카메라, 저장소 퍼미션 요청
        checkCameraPermissions();
    }

    //카메라 퍼미션 요청
    private void checkCameraPermissions() {
        new TedPermission(this)
                .setPermissionListener(permissionlistener)
                .setDeniedMessage("If you reject permission,you can not use this service\n\nPlease turn on permissions at [Setting] > [Permission]")
                .setPermissions(Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE)
                .check();
    }

    PermissionListener permissionlistener = new PermissionListener() {
        @Override
        public void onPermissionGranted() {
            getLandscapeGuideList();
        }

        @Override
        public void onPermissionDenied(ArrayList<String> deniedPermissions) {
            Toast.makeText(LandscapeActivity.this, "권한이 허용되지 않아서 앱을 이용할 수 없습니다.", Toast.LENGTH_SHORT).show();
            finish();
        }
    };

    private void getLandscapeGuideList() {
        Ion.with(this)
                .load(API_URL + "/guides?type=land")
                .asJsonObject()
                .setCallback(new FutureCallback<JsonObject>() {
                    @Override
                    public void onCompleted(Exception e, JsonObject result) {
                        if (e != null) {
                            Toast.makeText(LandscapeActivity.this, "가이드 목록을 가져올 수 없습니다.", Toast.LENGTH_SHORT).show();
                            Log.i(TAG, "onCompleted: " + e.getLocalizedMessage());
                            finish();
                        } else {
                            if (result.get("code").getAsInt() != 200) {
                                Toast.makeText(LandscapeActivity.this, "가이드 목록을 가져올 수 없습니다.", Toast.LENGTH_SHORT).show();
                                Log.i(TAG, "onCompleted: " + result.get("code").getAsInt());
                                finish();
                            } else {
                                // 이제야 성공한거임
                                JsonArray imageArray = result.getAsJsonArray("data");
                                for (int i=0; i<imageArray.size(); i++) {
                                    final JsonObject object = imageArray.get(i).getAsJsonObject();
                                    ImageView image = new ImageView(LandscapeActivity.this);
                                    image.setLayoutParams(new LinearLayout.LayoutParams(convertDpToPx(150), ViewGroup.LayoutParams.WRAP_CONTENT));
                                    image.setScaleType(ImageView.ScaleType.FIT_XY);
                                    image.setAdjustViewBounds(true);
                                    image.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            Intent cameraIntent = new Intent(LandscapeActivity.this, CameraActivity.class);
                                            cameraIntent.putExtra("image", object.get("cam_guideline_url").getAsString());
                                            startActivity(cameraIntent);
                                        }
                                    });
                                    String fullImageUrl = DEFAULT_URL + object.get("cam_photo_url").getAsString();
                                    Glide.with(LandscapeActivity.this)
                                            .load(fullImageUrl)
                                            .thumbnail(0.1f)
                                            .into(image);
                                    mImagesLayout.addView(image);
                                }
                            }
                        }
                    }
                });
    }

    public static int convertDpToPx(int dp) {
        return Math.round(dp * (Resources.getSystem().getDisplayMetrics().density));
    }

}