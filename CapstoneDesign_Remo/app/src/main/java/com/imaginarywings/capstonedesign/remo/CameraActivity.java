package com.imaginarywings.capstonedesign.remo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.media.Image;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Display;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.bumptech.glide.Glide;
import com.imaginarywings.capstonedesign.remo.utils.CameraHelper;
import com.imaginarywings.capstonedesign.remo.utils.CameraHelper.CameraInfo2;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;
import jp.co.cyberagent.android.gpuimage.GPUImage;
import jp.co.cyberagent.android.gpuimage.GPUImage.OnPictureSavedListener;
import jp.co.cyberagent.android.gpuimage.GPUImageFilter;


public class CameraActivity extends AppCompatActivity implements OnClickListener {
    private final String TAG = getClass().getSimpleName();

    @BindView(R.id.camera_preview)
    GLSurfaceView mCameraPreview;
    @BindView(R.id.camera_guide)
    ImageView mGuideImage;
//    @BindView(R.id.switch_black)
//    ImageView black;


    private GPUImage mGPUImage;
    private CameraHelper mCameraHelper;
    private CameraLoader mCamera;
    //    private GPUImageFilter mFilter;
//    private GPUImageFilterTools.FilterAdjuster mFilterAdjuster;
    ImageButton btnCapture;
    ImageButton btnSwitch;
    ImageButton btnGallery;

    int width;

    private final static int CAMERA_FACING = Camera.CameraInfo.CAMERA_FACING_BACK;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        ButterKnife.bind(this);

        mGPUImage = new GPUImage(this);
        mGPUImage.setGLSurfaceView(mCameraPreview);

        mCameraHelper = new CameraHelper(this);
        mCamera = new CameraLoader();

        btnCapture = (ImageButton) findViewById(R.id.btnCapture);
        btnCapture.setOnClickListener(this);

        btnGallery = (ImageButton) findViewById(R.id.btnGallery);
        btnGallery.setOnClickListener(this);

        //화면 전환
        btnSwitch = (ImageButton) findViewById(R.id.btnSwitch);
        btnSwitch.setOnClickListener(this);
        if (!mCameraHelper.hasFrontCamera() || !mCameraHelper.hasBackCamera()) {
            btnSwitch.setVisibility(View.GONE);
        }

        //화면 가로 크기에 따라 세로 크기 변경
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        width = size.x;

        View cameraLayout = findViewById(R.id.camera_layout);
        cameraLayout.setLayoutParams(new RelativeLayout.LayoutParams(width, width));

        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) cameraLayout.getLayoutParams();
        layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
        cameraLayout.setLayoutParams(layoutParams);
//
//        View cameraPreview = findViewById(R.id.camera_preview);
//        cameraPreview.setLayoutParams(new FrameLayout.LayoutParams(width,width));

        Glide.with(this)
                .load(R.drawable.guidetest)
                .thumbnail(0.1f)
                .into(mGuideImage);
    }

    @Override
    protected void onResume() {
        mCamera.onResume();
        super.onResume();
    }

    @Override
    protected void onPause() {
        mCamera.onPause();
        super.onPause();
    }

    //버튼 별 기능
    @Override
    public void onClick(final View v) {
        switch (v.getId()) {
//            case R.id.button_choose_filter:
//                GPUImageFilterTools.showDialog(this, new OnGpuImageFilterChosenListener() {
//
//                    @Override
//                    public void onGpuImageFilterChosenListener(final GPUImageFilter filter) {
//                        switchFilterTo(filter);
//                    }
//                });
//                break;

            case R.id.btnCapture:
                if (mCamera.mCameraInstance.getParameters().getFocusMode().equals(
                        Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                    takePicture();
                } else {
                    mCamera.mCameraInstance.autoFocus(new Camera.AutoFocusCallback() {

                        @Override
                        public void onAutoFocus(final boolean success, final Camera camera) {
                            takePicture();
                        }
                    });
                }
                break;

            case R.id.btnSwitch:
                mCamera.switchCamera();
                break;

            case R.id.btnGallery:
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setType
                        (android.provider.MediaStore.Images.Media.CONTENT_TYPE);
                startActivity(intent);
                break;
        }
    }

    private void takePicture() {
        // TODO get a size that is about the size of the screen
        Camera.Parameters params = mCamera.mCameraInstance.getParameters();
        params.setRotation(90);
        mCamera.mCameraInstance.setParameters(params);
        for (Camera.Size size : params.getSupportedPictureSizes()) {
            Log.i("ASDF", "Supported: " + size.width + "x" + size.height);
        }
        mCamera.mCameraInstance.takePicture(null, null,
                new Camera.PictureCallback() {

                    @Override
                    public void onPictureTaken(byte[] data, final Camera camera) {

                        final File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
                        if (pictureFile == null) {
                            Log.d("ASDF",
                                    "Error creating media file, check storage permissions");
                            return;
                        }

                        //사진 회전 부분
                        //이미지의 너비와 높이 결정
                        int w = camera.getParameters().getPictureSize().width;
                        int h = camera.getParameters().getPictureSize().height;

                        int orientation = setCameraDisplayOrientation(CameraActivity.this, CAMERA_FACING, camera);


                        //byte array를 bitmap으로 변환
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, options);

                        //이미지를 디바이스 방향으로 회전
                        Matrix matrix = new Matrix();
                        matrix.postRotate(orientation);
                        bitmap = Bitmap.createBitmap(bitmap, 0, 0, w, h, matrix, true);


                        try {
                            FileOutputStream fos = new FileOutputStream(pictureFile);
                            fos.write(data);
                            fos.close();
                        } catch (FileNotFoundException e) {
                            Log.d("ASDF", "File not found: " + e.getMessage());
                        } catch (IOException e) {
                            Log.d("ASDF", "Error accessing file: " + e.getMessage());
                        }

//                        data = null;
//                        Bitmap bitmap = BitmapFactory.decodeFile(pictureFile.getAbsolutePath());
//                        //mGPUImage.setImage(bitmap);
                        final GLSurfaceView view = mCameraPreview; //여기 이상

                        view.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
                        mGPUImage.saveToPictures(bitmap, "Remo",
                                System.currentTimeMillis() + ".jpg",
                                new OnPictureSavedListener() {

                                    @Override
                                    public void onPictureSaved(final Uri
                                                                       uri) {
                                        pictureFile.delete();
                                        camera.startPreview();
                                        view.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
                                    }
                                });
                    }
                });
    }

    public static int setCameraDisplayOrientation(Activity activity,
                                                  int cameraId, android.hardware.Camera camera) {
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }

        return result;
    }


    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;

    private static File getOutputMediaFile(final int type) {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "MyCameraApp");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_" + timeStamp + ".jpg");
        } else if (type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "VID_" + timeStamp + ".mp4");
        } else {
            return null;
        }

        return mediaFile;
    }


    private class CameraLoader {

        private int mCurrentCameraId = 0;
        private Camera mCameraInstance;

        public void onResume() {
            setUpCamera(mCurrentCameraId);
        }

        public void onPause() {
            releaseCamera();
        }

        public void switchCamera() {
//            mGuideImage.setVisibility(View.GONE);
            mGuideImage.setImageDrawable(null);
            mGuideImage.requestLayout();
            releaseCamera();
            mCurrentCameraId = (mCurrentCameraId + 1) % mCameraHelper.getNumberOfCameras();
            reInitLayout();
            setUpCamera(mCurrentCameraId);
        }

        private void setUpCamera(final int id) {
            mCameraInstance = getCameraInstance(id);
            Parameters parameters = mCameraInstance.getParameters();
            // TODO adjust by getting supportedPreviewSizes and then choosing
            // the best one for screen size (best fill screen)
            if (parameters.getSupportedFocusModes().contains(
                    Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                parameters.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            }

            mCameraInstance.setParameters(parameters);

            int orientation = mCameraHelper.getCameraDisplayOrientation(
                    CameraActivity.this, mCurrentCameraId);
            CameraInfo2 cameraInfo = new CameraInfo2();
            mCameraHelper.getCameraInfo(mCurrentCameraId, cameraInfo);
            boolean flipHorizontal = cameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT;
            mGPUImage.setUpCamera(mCameraInstance, orientation, flipHorizontal, false);

//            mGuideImage.setImageDrawable(null);
//            mGuideImage.setVisibility(View.VISIBLE);
//            mGuideImage.requestLayout();
//            Glide.with(CameraActivity.this)
//                    .load(R.drawable.guidetest)
//                    .thumbnail(0.1f)
//                    .into(mGuideImage);
        }

        /**
         * A safe way to get an instance of the Camera object.
         */
        private Camera getCameraInstance(final int id) {
            Camera c = null;
            try {
                c = mCameraHelper.openCamera(id);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return c;
        }

        private void releaseCamera() {
            mCameraInstance.stopPreview();
            mCameraInstance.setPreviewCallback(null);
            mCameraInstance.release();
            mCameraInstance = null;
        }

        public void reInitLayout() {
            mCameraPreview.requestLayout();
            mGPUImage.deleteImage();
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

}