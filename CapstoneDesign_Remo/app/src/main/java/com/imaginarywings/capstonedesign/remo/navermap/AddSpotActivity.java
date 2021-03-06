package com.imaginarywings.capstonedesign.remo.navermap;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonObject;
import com.imaginarywings.capstonedesign.remo.R;
import com.imaginarywings.capstonedesign.remo.model.PhotoSpotModel;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.async.http.body.FilePart;
import com.koushikdutta.async.http.body.Part;
import com.koushikdutta.async.http.body.StringPart;
import com.koushikdutta.ion.Ion;
import com.nhn.android.maps.maplib.NGeoPoint;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.imaginarywings.capstonedesign.remo.Consts.API_URL;

public class AddSpotActivity extends AppCompatActivity {

    private final int REQUEST_SPOT_SEARCH = 1005;

    private static final int PICK_FROM_ALBUM = 1;
    private static final int CROP_FROM_IMAGE = 2;

    private final String TAG = getClass().getSimpleName();

    public static Context mContext;
    public static boolean mMarkerEnable;
    public PhotoSpotModel mSpotModel;
    public LocationManager locationManager;
    private String mSelectedImagePath;
    private String absoultePath;
    public NGeoPoint mSavePoint;
    public Uri uri;


    @BindView(R.id.id_SpotAddress)
    TextView mtext_SpotAddress;
    @BindView(R.id.btn_AddSpot_Search)
    Button mbtnSearch;
    @BindView(R.id.id_editTextAddressSearch)
    EditText mEditText_AddressSearch;
    @BindView(R.id.Btn_AddSpot_Image)
    Button mbtnAddSpotImage;
    @BindView(R.id.id_ImgView_PhotoSpot)
    ImageView mPhotospotImage;
    @BindView(R.id.id_imgbtnSaveSpot)
    Button mSaveSpot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_spot);

        ButterKnife.bind(this);

        mContext = this;
        mMarkerEnable = false;
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        //GPS ON/OFF 유무 확인
        boolean isEnable = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        //활성화 되어 있다면 처음 생성할 때 현재 위치에 대한 주소를 기본적인 주소로 지정한다.
        if (isEnable) {

            //FragmentMapActivity 의 함수 호출 방법
            //포토스팟 위도 경도 삽입
            mSavePoint = ((FragmentMapActivity) FragmentMapActivity.mContext).getAddress();

            if(mSavePoint != null)
            {
                String Address =
                        ((FragmentMapActivity) FragmentMapActivity.mContext).ConvertAddress(this, mSavePoint.getLatitude(), mSavePoint.getLongitude());

                mtext_SpotAddress.setText(Address);
            }

        } else {

            Toast.makeText(mContext, "GPS기능이 비활성화 되어 있습니다. 기능을 활성화 해주십시오.", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            startActivity(intent);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != RESULT_OK) {
                mSaveSpot.setEnabled(false);
                mSelectedImagePath = null;
            return;
        }

        switch (requestCode) {
            case PICK_FROM_ALBUM: {
                uri = data.getData();

                if (data != null) {
                    uri = data.getData();

                    Intent intent = new Intent("com.android.camera.action.CROP");
                    intent.setDataAndType(uri, "image/*");

                    //CROP 할 이미지를 300*300 크기로 저장
                    intent.putExtra("outputX", 300);
                    intent.putExtra("outputY", 300);
                    intent.putExtra("aspectX", 1);
                    intent.putExtra("aspectY", 1);
                    intent.putExtra("scale", true);
                    intent.putExtra("return-data", true);

                    startActivityForResult(intent, CROP_FROM_IMAGE);

                     /*
                        Glide.with(this)
                                .load(uri)
                                .bitmapTransform(new CropTransformation(Glide.get(mContext).getBitmapPool(), 500, 500))
                                //.centerCrop()
                                .thumbnail(0.1f)
                                .into(mPhotospotImage);
                     */

                    mSaveSpot.setEnabled(true);
                    mSelectedImagePath = getPathFromUri(uri);


                } else {
                    mSaveSpot.setEnabled(false);
                    mSelectedImagePath = null;
                }

                break;
            }

            case CROP_FROM_IMAGE: {
                //크롭이 된 이후의 이미지를 넘겨 받는다.
                //이미지뷰에 이미지를 보여준다거나 부가적인 작업 이후에 임시파일을 삭제한다
                if (resultCode != RESULT_OK) {
                    return;
                }

                final Bundle extras = data.getExtras();

                //CROP된 이미지를 저장하기 위한 FILE 경로
                String filePath = Environment.getExternalStorageDirectory().getAbsolutePath()
                        + "/RemoCrop/" + System.currentTimeMillis() + ".jpg";

                if (extras != null) {
                    Bitmap photo = extras.getParcelable("data");     //CROP된 BITMAP
                    mPhotospotImage.setImageBitmap(photo);           //레이아웃의 이미지뷰에 CROP된 BITMAP을 보여줌

                    storeCropImage(photo, filePath);                 //CROP된 이미지를 외부 저장소, 앨범에 저장한다.
                    absoultePath = filePath;
                    mSelectedImagePath = filePath;
                    break;
                }

                File f = new File(uri.getPath());

                if (f.exists()) {
                    f.delete();
                }

            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /**
     * 외부저장소에 크롭된 이미지를 저장하는 함수
     * 비트맵을 저장하는 부분
     *
     * @param bitmap
     * @param filePath
     */
    private void storeCropImage(Bitmap bitmap, String filePath) {
        String dirPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/RemoCrop";
        File directory = new File(dirPath);

        //디렉터리에 폴더가 없다면 (새로 이미지를 저장하는 경우에 속한다.)
        if (!directory.exists())
            directory.mkdir();

        File copyFile = new File(filePath);
        BufferedOutputStream out = null;

        try {
            copyFile.createNewFile();
            out = new BufferedOutputStream(new FileOutputStream(copyFile));
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);

            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            Uri contentUri = Uri.fromFile(copyFile);
            mediaScanIntent.setData(contentUri);
            this.sendBroadcast(mediaScanIntent);

            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @OnClick(R.id.Btn_AddSpot_Image)
    public void clickSpotImageUpload() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType(MediaStore.Images.Media.CONTENT_TYPE);
        startActivityForResult(intent, PICK_FROM_ALBUM);
    }

    //지도 검색
    @OnClick(R.id.btn_AddSpot_Search)
    public void selecteBtnAddSpotSearch() {
        NGeoPoint nPoint;

        String searchAddress = String.valueOf(mEditText_AddressSearch.getText());

        nPoint = ((FragmentMapActivity) FragmentMapActivity.mContext).ConvertLatLng(searchAddress);

        if (nPoint != null) {

            //위도
            String latitude = String.valueOf(nPoint.getLatitude());

            //경도
            String longitude = String.valueOf(nPoint.getLongitude());

            Log.e("위도", latitude);
            Log.e("경도", longitude);

            Intent AddSpotMap = new Intent(getApplicationContext(), AddSpotFragmentActivity.class);
            AddSpotMap.putExtra("result_point", latitude);
            setResult(REQUEST_SPOT_SEARCH, AddSpotMap);
            startActivityForResult(AddSpotMap, REQUEST_SPOT_SEARCH);
        } else {
            Toast.makeText(mContext, "위치를 검색할 수 없습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 포토스팟 등록
     */
    @OnClick(R.id.id_imgbtnSaveSpot)
    public void selectBtnSaveSpot() {
        if (mSelectedImagePath == null) {
            Toast.makeText(this, "사진을 선택해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setMessage("잠시만 기다려주세요.")
                .setCancelable(false).show();

        //서버 전송을 위한 데이터
        List<Part> parts = new ArrayList<>();

        parts.add(new FilePart("image", new File(mSelectedImagePath)));

        //주소
        if (mtext_SpotAddress.getText() != null)
            parts.add(new StringPart("spot_address", String.valueOf(mtext_SpotAddress.getText())));
        else
            Toast.makeText(this, "주소를 입력해주세요.", Toast.LENGTH_SHORT).show();

        //이름
        parts.add(new StringPart("spot_name", "임시이름"));

        //uuid
        String uuid = ((FragmentMapActivity) FragmentMapActivity.mContext).mUUID;
        parts.add(new StringPart("user_uuid", uuid));

        //위,경도
        if (mSavePoint != null) {
            parts.add(new StringPart("spot_latitude", String.valueOf(mSavePoint.getLatitude())));
            parts.add(new StringPart("spot_longitude", String.valueOf(mSavePoint.getLongitude())));
        } else
            Toast.makeText(this, "잘못된 위치입니다. 다시 확인해보세요.", Toast.LENGTH_SHORT).show();

        //포토스팟모델 생성
        mSpotModel = new PhotoSpotModel(1, "TEST", "uuid", "test1", String.valueOf(mtext_SpotAddress.getText()),
                mSelectedImagePath, mSavePoint.getLatitude(), mSavePoint.getLongitude());

        //서버에 데이터 전송
        Ion.with(this)
                .load(API_URL + "/spot")
                .addMultipartParts(parts)
                .asJsonObject()
                .setCallback(new FutureCallback<JsonObject>() {
                    @Override
                    public void onCompleted(Exception e, JsonObject result) {
                        dialog.cancel();
                        if (e != null) {
                            Toast.makeText(AddSpotActivity.this, "업로드에 실패하였습니다.", Toast.LENGTH_SHORT).show();
                            Log.i(TAG, "onCompleted: " + e.getLocalizedMessage());
                        } else {

                            Log.d(TAG, "onCompleted: " + result.toString());
                            int code = result.get("code").getAsInt();
                            if (code != 201) {
                                Toast.makeText(AddSpotActivity.this, "업로드에 실패하였습니다.", Toast.LENGTH_SHORT).show();
                                Log.i(TAG, "onCompleted: " + code);
                            } else {
                                Toast.makeText(AddSpotActivity.this, "업로드 성공", Toast.LENGTH_SHORT).show();
                                Log.d(TAG, "onCompleted: " + result.get("data").toString());
                                setResult(RESULT_OK);

                                mMarkerEnable = true;
                                finish();
                            }
                        }
                    }
                });

    }

    //사진 uri 경로 얻어오기
    public String getPathFromUri(Uri uri) {
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        cursor.moveToNext();
        String path = cursor.getString(cursor.getColumnIndex("_data"));
        cursor.close();

        return path;
    }

    public PhotoSpotModel getSpotModel() {
        return mSpotModel;
    }
}
