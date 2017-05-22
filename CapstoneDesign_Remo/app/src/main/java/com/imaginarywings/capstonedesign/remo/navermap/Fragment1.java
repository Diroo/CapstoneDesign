/*
 * Copyright 2016 NAVER Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.imaginarywings.capstonedesign.remo.navermap;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.location.Geocoder;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.TedPermission;
import com.imaginarywings.capstonedesign.remo.Consts;
import com.imaginarywings.capstonedesign.remo.R;
import com.imaginarywings.capstonedesign.remo.SpotDetailDialog;
import com.imaginarywings.capstonedesign.remo.model.PhotoSpotModel;
import com.nhn.android.maps.NMapActivity;
import com.nhn.android.maps.NMapCompassManager;
import com.nhn.android.maps.NMapContext;
import com.nhn.android.maps.NMapController;
import com.nhn.android.maps.NMapLocationManager;
import com.nhn.android.maps.NMapView;
import com.nhn.android.maps.maplib.NGeoPoint;
import com.nhn.android.maps.nmapmodel.NMapError;
import com.nhn.android.maps.nmapmodel.NMapPlacemark;
import com.nhn.android.maps.overlay.NMapPOIdata;
import com.nhn.android.maps.overlay.NMapPOIitem;
import com.nhn.android.mapviewer.overlay.NMapMyLocationOverlay;
import com.nhn.android.mapviewer.overlay.NMapOverlayManager;
import com.nhn.android.mapviewer.overlay.NMapPOIdataOverlay;

import java.util.ArrayList;
import java.util.Locale;

import butterknife.ButterKnife;
import io.nlopez.smartlocation.SmartLocation;

/**
 * NMapFragment extends Fragment
 * 지도를 프래그먼트로 띄워서 UI 수정을 유동적으로 할 수 있다.
 * 포토스팟 액티비티x, Fragment1의 클래스 이름을 나중에 바꿔줘야함.(현재 혼동옴)
 */

public class Fragment1 extends NMapFragment {

	//네이버맵 클라이언트 아이디(API키)
	private final String CLIENT_ID = "xQ50GyWn_EU3eQE4A1sL";

	//포토스팟 다이얼로그 창 데이터 해시 테이블?
	private static final String TAG_SPOT_DETAIL_DIALOG = "SpotDetailDialog";

	private static final int REQUEST_LOCATION_ENABLE = 717;

	//현재 클래스명 얻어오기
	private final String TAG = getClass().getSimpleName();

	//네이버 맵
	public NMapView mMapView;

	//맵 컨트롤러
	public NMapController mMapController;

	//지도 위에 표시되는 오버레이 객체를 관리
	public NMapOverlayManager mOverlayManager;

	public NMapLocationManager mMapLocationManager;
	public NMapMyLocationOverlay mMyLocationOverlay;       //현재 위치를 표시하기 위한 오버레이 객체
	public NMapCompassManager mMapCompassManager;          //나침반 매니져

	//오버레이의 리소스를 제공하기 위한 객체
	public NMapViewerResourceProvider mMapViewerResourceProvider;

	public NMapContext mMapContext;

	public Fragment1()
	{

	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mMapContext = new NMapContext(super.getActivity());
		mMapContext.onCreate();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment1, container, false);
		return view;
	}


	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		ButterKnife.bind(getActivity());

		mMapView = (NMapView)getView().findViewById(R.id.mapView);
		mMapView.setClientId(CLIENT_ID);

		mMapContext.setupMapView(mMapView);

		checkGPSPermissions();

		// initialize map view
		mMapView.setOnMapStateChangeListener(mStateChangeListener);

		//자세한 확대 (수치는 적절히 조정하도록)
		mMapView.setScalingFactor(2.0f);

		//줌인아웃 버튼
		mMapView.setBuiltInZoomControls(true, null);

		//네이버 로고 위치 변경 왼쪽 상단을 기준으로 x축이 오른쪽으로 갈 수록 양수 , y축이 아래로 갈수록 양수
		mMapView.setLogoImageOffset(35, 170);

		//화면 터치 옵션 활성화
		mMapView.setClickable(true);

		mMapView.setEnabled(true);
		mMapView.setFocusable(true);
		mMapView.setFocusableInTouchMode(true);
		mMapView.requestFocus();

		mMapViewerResourceProvider = new NMapViewerResourceProvider(getActivity());
		mOverlayManager = new NMapOverlayManager(getActivity(), mMapView, mMapViewerResourceProvider);

		//NMapView를 생성하면서 자동으로 컨트롤러도 생성되므로 NMapView로부터 얻어온다.
		mMapController = mMapView.getMapController();

		//현재위치 매니저
		mMapLocationManager = new NMapLocationManager(getActivity());
		mMapLocationManager.setOnLocationChangeListener(mLocationChangeListener);

		//compass manager
		mMapCompassManager = new NMapCompassManager(getActivity());

		//create my location overlay
		mMyLocationOverlay = mOverlayManager.createMyLocationOverlay(mMapLocationManager, mMapCompassManager);

		// 데이터 제공 리스너 제대로 동작하는지 확인해야함
		mMapContext.setMapDataProviderListener(onDataProviderListener);

		createSpotMarker();
	}

	@Override
	public void onStart() {
		super.onStart();
		mMapContext.onStart();
	}

	@Override
	public void onResume() {
		super.onResume();
		mMapContext.onResume();
	}

	@Override
	public void onPause() {
		super.onPause();
		mMapContext.onPause();
	}

	@Override
	public void onDestroyView() {
		mMapContext.onDestroy();
		super.onDestroyView();
	}

	@Override
	public void onDestroy() {
		if(mMapLocationManager.isMyLocationEnabled())
		{
			mMapLocationManager.disableMyLocation();
			mMapLocationManager.setOnLocationChangeListener(null);
		}

		mMapView.setOnMapStateChangeListener((NMapView.OnMapStateChangeListener) null);

		super.onDestroy();
	}

	// 맵 상태변화 리스너
	private NMapView.OnMapStateChangeListener mStateChangeListener = new NMapView.OnMapStateChangeListener() {

		//지도가 초기화 된 후 호출된다.
		@Override
		public void onMapInitHandler(NMapView mapView, NMapError errorInfo) {
			if (errorInfo == null) {
				// 초기 위치 설정
				mMapController.setMapCenter(new NGeoPoint(127.131342, 35.847532), 11);

			} else {
				Toast.makeText(getActivity(), "지도를 초기화하는데 실패하였습니다.\n message: " + errorInfo.message, Toast.LENGTH_SHORT).show();
				getActivity().finish();
			}
		}

		//지도 중심 변경 시 호출되며 변경된 중심 좌표가 파라미터로 전달된다.
		@Override
		public void onMapCenterChange(NMapView mapView, NGeoPoint center) {

		}

		@Override
		public void onMapCenterChangeFine(NMapView mapView) {

		}

		//지도 레벨 변경 시 호출되며 변경된 지도 레벨이 파라미터로 전달된다.
		@Override
		public void onZoomLevelChange(NMapView mapView, int level) {

		}

		//지도 애니메이션 상태 변경 시 호출된다.
		@Override
		public void onAnimationStateChange(NMapView mapView, int animType, int animState) {

		}
	};

	//지도 라이브러리에서 제공하는 서버 API 호출 시 응답에 대한 콜백 인터페이스
	public NMapActivity.OnDataProviderListener onDataProviderListener =
			new NMapActivity.OnDataProviderListener()
			{
				@Override	//좌표를 주소로 변환하는 콜백 인터페이스
				public void onReverseGeocoderResponse(NMapPlacemark nMapPlacemark, NMapError nMapError) {

					if (false) {
						Log.i("My Log", "onReverseGeocoderResponse: placeMark="
								+ ((nMapPlacemark != null) ? nMapPlacemark.toString() : null));
					}

					if (nMapError != null) {
						Log.e("My Log", "Failed to findPlacemarkAtLocation: error=" + nMapError.toString());

						Toast.makeText(getActivity(), nMapError.toString(), Toast.LENGTH_LONG).show();
						return;
					}

					/*
					if (mFloatingPOIitem != null && mFloatingPOIdataOverlay != null) {
						mFloatingPOIdataOverlay.deselectFocusedPOIitem();

						if (placeMark != null) {
							mFloatingPOIitem.setTitle(placeMark.toString());
						}
						mFloatingPOIdataOverlay.selectPOIitemBy(mFloatingPOIitem.getId(), false);
					*/
				}
			};


	//단말기의 현재 위치 상태 변경 시 호출되는 콜백 인터페이스.
	public NMapLocationManager.OnLocationChangeListener mLocationChangeListener =
			new NMapLocationManager.OnLocationChangeListener() {

		//현재 위치 변경시 호출.
		@Override
		public boolean onLocationChanged(NMapLocationManager locationManager, NGeoPoint myLocation) {

			if (mMapController != null) {

				//맵컨트롤러 void animateTo(NGeoPoint) : 지도 중심점을 전달된 좌표로 변경한다. 지도 중심점 이동 시 패닝 애니메이션이 수행된다.
				//myLocation 객체에 변경된 좌표를 전달
				mMapController.animateTo(myLocation);
			}

			//위치 로그 확인
			Log.d("myLog", "my Location latitude " + myLocation.getLatitude());
			Log.d("myLog", "my Location Longitude " + myLocation.getLongitude());

			//위도 경도를 주소로 변환
			mMapContext.findPlacemarkAtLocation(myLocation.getLongitude(), myLocation.getLatitude());

			//현재 위치를 계속 탐색하려면 true를 반환
			return true;
		}

		@Override
		public void onLocationUpdateTimeout(NMapLocationManager locationManager) {
			Log.e(TAG, "onLocationUpdateTimeout: ");
		}

		@Override
		public void onLocationUnavailableArea(NMapLocationManager locationManager, NGeoPoint myLocation) {
			Log.e(TAG, "onLocationUnavailableArea: " + myLocation.toString());

			stopMyLocation();
		}
	};



	//포토스팟 마커 생성
	public void createSpotMarker() {
		int markerId = NMapPOIflagType.PIN;

		//포토스팟을 생성(id, type, android id, suject, address, 이미지경로, latitude, longitude)
		PhotoSpotModel model1 = new PhotoSpotModel(1, "MAIN", "aid", "main1", "주소1", Consts.IMAGE_URL, 35.852548, 127.100824);
		PhotoSpotModel model2 = new PhotoSpotModel(2, "MAIN", "aid", "main2", "주소2", Consts.IMAGE_URL, 35.852037, 127.101738);
		PhotoSpotModel model3 = new PhotoSpotModel(3, "MAIN", "aid", "main3", "주소3", Consts.IMAGE_URL, 35.847722, 127.123735);

		//여러 개의 오버레이 아이템을 하나의 오버레이 객체에서 관리하기 위한 오버레이 클래스의 객체 생성
		NMapPOIdata poiData = new NMapPOIdata(2, mMapViewerResourceProvider);

		// 경도, 위도, 표시문구, 표시할 마커 이미지의 id값, 객체, 오버레이를 관리하기 위한 id값
		//poiData.beginPOIdata(3);
		poiData.addPOIitem(127.130746, 35.847756, null, markerId, model1, 1);
		poiData.addPOIitem(127.132028, 35.847834, null, markerId, model2, 2);
		poiData.addPOIitem(127.123735, 35.847722, null, markerId, model3, 3);
		//poiData.endPOIdata();

		// create POI data overlay
		NMapPOIdataOverlay poiDataOverlay = mOverlayManager.createPOIdataOverlay(poiData, null);
		poiDataOverlay.setOnStateChangeListener(new NMapPOIdataOverlay.OnStateChangeListener() {

			//오버레이 아이템의 선택 상태 변경 시 호출되는 콜백 인터페이스를 정의합니다.
			@Override
			public void onFocusChanged(NMapPOIdataOverlay poiDataOverlay, NMapPOIitem item) {
				if (item != null) {
					Log.i(TAG, "onFocusChanged: " + item.getTag().toString());

					PhotoSpotModel model = (PhotoSpotModel) item.getTag();
					FragmentManager manager = getFragmentManager();
					Fragment frag = manager.findFragmentByTag(TAG_SPOT_DETAIL_DIALOG);
					if (frag != null) {
						manager.beginTransaction().remove(frag).commit();
					}

					//포토스팟을 눌렀을 때 나오는 팝업창
					SpotDetailDialog dialog = new SpotDetailDialog();
					Bundle data = new Bundle();
					data.putParcelable("detail", model);
					dialog.setArguments(data);
					dialog.show(manager, TAG_SPOT_DETAIL_DIALOG);
				}
			}

			@Override
			public void onCalloutClick(NMapPOIdataOverlay poiDataOverlay, NMapPOIitem item) {
				if (item != null) {
					Log.i(TAG, "onCalloutClick: " + item.toString());
				}
			}
		});

		mOverlayManager.addOverlay(poiDataOverlay);
	}

	//시작했을때 나의 현재 위치 보여주록 함.
	public void startMyLocation()
	{
		if(mMyLocationOverlay != null)
		{
			if(!mOverlayManager.hasOverlay(mMyLocationOverlay))
			{
				mOverlayManager.addOverlay(mMyLocationOverlay);
			}
		}

		boolean isMyLocationEnabled = mMapLocationManager.enableMyLocation(true);

		if (!isMyLocationEnabled)
		{
			Toast.makeText(getActivity(), "현재 위치를 가져올 수 없습니다. GPS 상태를 확인해주세요!", Toast.LENGTH_SHORT).show();
			stopMyLocation();
		}
	}

	private void stopMyLocation() {
		if (mMyLocationOverlay != null) {
			mMapLocationManager.disableMyLocation();

			if (mMapView.isAutoRotateEnabled()) {
				mMyLocationOverlay.setCompassHeadingVisible(false);

				mMapCompassManager.disableCompass();

				mMapView.setAutoRotateEnabled(false, false);
			}
		}
	}

	//퍼미션 리스터 인터페이스 오버라이드
	PermissionListener permissionlistener = new PermissionListener() {
		@Override
		public void onPermissionGranted() {
			checkLocationEnabled();
			startMyLocation();
		}

		@Override
		public void onPermissionDenied(ArrayList<String> deniedPermissions) {
			Toast.makeText(getActivity(), "권한이 허용되지 않아서 앱을 이용할 수 없습니다.", Toast.LENGTH_SHORT).show();
		}
	};

	//GPS 퍼미션 요청
	private void checkGPSPermissions() {
		new TedPermission(getActivity())
				.setPermissionListener(permissionlistener)
				.setDeniedMessage("요청을 거절하면 해당 서비스를 이용할 수 없습니다.\n\n 환경설정에서 권한요청을 허용해 주십시오.")
				.setPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
				.check();
	}

	//GPS 기능 확인 -> 설정창으로 이동
	private void checkLocationEnabled() {
		if (!SmartLocation.with(getActivity()).location().state().isGpsAvailable()) {
			Toast.makeText(getActivity(), "GPS기능을 활성화 해주시기 바랍니다.", Toast.LENGTH_SHORT).show();
			Intent goToSettings = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
			startActivityForResult(goToSettings, REQUEST_LOCATION_ENABLE);

		}
		else {
		}
	}

	//현재 위치 정보 반환
	public NGeoPoint checkMyLocationInfo() {
		NGeoPoint location = mMapLocationManager.getMyLocation();
		return location;
	}

}
