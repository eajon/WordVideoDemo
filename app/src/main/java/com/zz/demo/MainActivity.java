package com.zz.demo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.qingmei2.rximagepicker.core.RxImagePicker;
import com.qingmei2.rximagepicker.entity.Result;
import com.qingmei2.rximagepicker_extension.MimeType;
import com.qingmei2.rximagepicker_extension_zhihu.ZhihuConfigurationBuilder;
import com.sendtion.xrichtext.RichTextEditor;
import com.sendtion.xrichtext.SDCardUtil;
import com.tbruyelle.rxpermissions2.Permission;
import com.tbruyelle.rxpermissions2.RxPermissions;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;


public class MainActivity extends AppCompatActivity implements ActionSheet.ActionSheetListener {

    @BindView(R.id.bt_main)
    Button btMain;
    @BindView(R.id.et_main)
    RichTextEditor etMain;

    private ProgressDialog insertDialog;
    public final static int REQUEST_TAKE_PHOTO = 1001;
    private ZhihuImagePicker rxImagePicker;
    ArrayList<String> photos;
    //动态申请权限
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        initView();
        rxImagePicker = RxImagePicker.INSTANCE
                .create(ZhihuImagePicker.class);
    }

    private void initView() {
        insertDialog = new ProgressDialog(this);
        insertDialog.setMessage("正在插入图片...");
        insertDialog.setCanceledOnTouchOutside(false);
        btMain.setVisibility(View.VISIBLE);
        btMain.setText("添加");
        btMain.setOnClickListener(v -> ActionSheet.createBuilder(MainActivity.this, getSupportFragmentManager())
                .setCancelButtonTitle("取消")
                .setOtherButtonTitles("相机", "相册")
                .setCancelableOnTouchOutside(true)
                .setListener(MainActivity.this).show());

    }

    @Override
    public void onDismiss(ActionSheet actionSheet, boolean isCancel) {
        actionSheet.dismiss();
    }

    @Override
    public void onOtherButtonClick(ActionSheet actionSheet, int index) {
        switch (index) {
            case 0:
                requestCameraPermissions();
                actionSheet.dismiss();
                break;
            case 1:
                requestGalleryPermissions();
                actionSheet.dismiss();
                break;
        }
    }

    private void requestCameraPermissions() {
        new RxPermissions(this)
                .requestEachCombined(PERMISSIONS_STORAGE)
                .subscribe(new Consumer<Permission>() {
                    @Override
                    public void accept(Permission permission) throws Exception {
                        if (permission.granted) {
                            Intent intent = new Intent(MainActivity.this, TakePhotoActivity.class);
                            startActivityForResult(intent, REQUEST_TAKE_PHOTO);
                        } else if (permission.shouldShowRequestPermissionRationale) {
                            // 用户拒绝了该权限，没有选中『不再询问』（Never ask again）,那么下次再次启动时，还会提示请求权限的对话框
                            Log.d("permission", permission.name + " is denied. More info should be provided.");
                        } else {

                            // 用户拒绝了该权限，并且选中『不再询问』
                            Log.d("permission", permission.name + " is denied.");
                        }
                    }
                });
    }

    @SuppressLint("CheckResult")
    private void requestGalleryPermissions() {
        new RxPermissions(this)
                .requestEachCombined(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .subscribe(new Consumer<Permission>() {
                    @Override
                    public void accept(Permission permission) throws Exception {
                        if (permission.granted) {
                            rxImagePicker.openGallery(MainActivity.this,
                                    new ZhihuConfigurationBuilder(MimeType.INSTANCE.ofAll(), false)
                                            .maxSelectable(9)
                                            .countable(true)
                                            .spanCount(3)
                                            .theme(R.style.Zhihu_Normal)
                                            .build())
                                    .subscribe(new Observer<Result>() {
                                        @Override
                                        public void onSubscribe(Disposable d) {
                                            photos = new ArrayList<String>();
                                        }

                                        @Override
                                        public void onNext(Result result) {
                                            photos.add(PhotoUtils.getPath(MainActivity.this, result.getUri()));
                                        }

                                        @Override
                                        public void onError(Throwable e) {

                                        }

                                        @Override
                                        public void onComplete() {
                                            insertImagesSync(photos);
                                        }
                                    });
                        } else if (permission.shouldShowRequestPermissionRationale) {

                            // 用户拒绝了该权限，没有选中『不再询问』（Never ask again）,那么下次再次启动时，还会提示请求权限的对话框
                            Log.d("permission", permission.name + " is denied. More info should be provided.");
                        } else {

                            // 用户拒绝了该权限，并且选中『不再询问』
                            Log.d("permission", permission.name + " is denied.");
                        }
                    }
                });
    }

    /**
     * 异步方式插入图片
     *
     * @param
     */
    private void insertImagesSync(final ArrayList<String> photos) {
        Flowable.create(( FlowableOnSubscribe<String> ) emitter -> {
            etMain.measure(0, 0);
            int width = ScreenUtils.getScreenWidth(MainActivity.this);
            int height = ScreenUtils.getScreenHeight(MainActivity.this);
//                    ArrayList<String> photos = data.getStringArrayListExtra(PhotoPicker.KEY_SELECTED_PHOTOS);
//                    //可以同时插入多张图片
            for (String imagePath : photos) {
                if (imagePath.contains("jpg") || imagePath.contains("png")) {
                    //Log.i("NewActivity", "###path=" + imagePath);
                    Bitmap bitmap = ImageUtils.getSmallBitmap(imagePath, width, height);//压缩图片
                    //bitmap = BitmapFactory.decodeFile(imagePath);
                    imagePath = SDCardUtil.saveToSdCard(bitmap);
                    //Log.i("NewActivity", "###imagePath="+imagePath);
                }

                emitter.onNext(imagePath);
            }
        }, BackpressureStrategy.BUFFER)
                .onBackpressureBuffer()
                .subscribeOn(Schedulers.io())//生产事件在io
                .observeOn(AndroidSchedulers.mainThread())//消费事件在UI线程
                .subscribe(new Subscriber<String>() {
                    @Override
                    public void onSubscribe(Subscription s) {
                        insertDialog.show();
                    }


                    @Override
                    public void onNext(String imagePath) {
                        if (imagePath.contains("jpg") || imagePath.contains("png")) {
                            etMain.insertImage(imagePath, etMain.getMeasuredWidth());
                        } else {
                            int width = ScreenUtils.getScreenWidth(MainActivity.this);
                            int height = ScreenUtils.getScreenHeight(MainActivity.this);
                            etMain.insertVideo(imagePath, VideoThumbLoader.getInstance().
                                    showThumb(imagePath, width, height), MainActivity.this);
                        }
                    }
                    @Override
                    public void onError(Throwable e) {
                        insertDialog.dismiss();
                        showToast(MainActivity.this, "图片/视频插入失败");
                    }


                    @Override
                    public void onComplete() {
                        insertDialog.dismiss();
                        etMain.addEditTextAtIndex(etMain.getLastIndex(), " ");
                        showToast(MainActivity.this, "图片/视频插入成功");
                    }

                });
    }

    @Override
    protected void onPause() {
        super.onPause();
        etMain.releaseVideo();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_TAKE_PHOTO:
                if (resultCode == RESULT_OK) {
                    String path = data.getStringExtra("path");
                    ArrayList<String> list = new ArrayList<>();
                    list.add(path);
                    if (data.getBooleanExtra("take_photo", true)) {
                        //照片
                        insertImagesSync(list);
                    } else {
                        //小视频
                        insertImagesSync(list);
                    }
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);

    }

    public void showToast(Context context, String msg) {
        Toast mToast;
        mToast = Toast.makeText(context, "", Toast.LENGTH_SHORT);
        mToast.setText(msg);
        mToast.show();
    }
}
