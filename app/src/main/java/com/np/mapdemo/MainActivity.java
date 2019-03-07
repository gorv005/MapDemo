package com.np.mapdemo;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.services.s3.AmazonS3Client;
import com.np.mapdemo.aws.UploadService;

import net.alhazmy13.mediapicker.Image.ImagePicker;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int FILE_SELECT_CODE = 0;

    Button select;
    Button interrupt;
    ProgressBar progress;
    TextView status;
    List<String> mImageList;
    ProgressBar pb;
    Button btn_upload;
    TextView _status;

    AmazonS3Client s3;
    BasicAWSCredentials credentials;
    TransferUtility transferUtility;
    TransferObserver observer;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        select = (Button) findViewById(R.id.btn_select);
        interrupt = (Button) findViewById(R.id.btn_interrupt);
        pb = (ProgressBar) findViewById(R.id.progress);
        status = (TextView) findViewById(R.id.status);
        mImageList = new ArrayList<>();

        select.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // start file chooser
//                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
//                intent.setType("*/*");
//                intent.addCategory(Intent.CATEGORY_OPENABLE);
//                startActivityForResult(
//                        Intent.createChooser(intent, "Select a file to upload"),
//                        FILE_SELECT_CODE);
                init();
            }
        });

        interrupt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // interrupt any active upload
                Intent intent = new Intent(UploadService.UPLOAD_CANCELLED_ACTION);
                sendBroadcast(intent);
            }
        });

    }

    void upload(String path){
        credentials = new BasicAWSCredentials(getString(R.string.s3_access_key),getString(R.string.s3_secret));
        s3 = new AmazonS3Client(credentials);
        transferUtility = new TransferUtility(s3, MainActivity.this);


        File file = new File(path);
        if(!file.exists()) {
            Toast.makeText(MainActivity.this, "File Not Found!", Toast.LENGTH_SHORT).show();
            return;
        }
        observer = transferUtility.upload(
                getString(R.string.s3_bucket),
                "test",
                file
        );

        observer.setTransferListener(new TransferListener() {
            @Override
            public void onStateChanged(int id, TransferState state) {

                if (state.COMPLETED.equals(observer.getState())) {

                    Toast.makeText(MainActivity.this, "File Upload Complete", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {



                long _bytesCurrent = bytesCurrent;
                long _bytesTotal = bytesTotal;

                float percentage =  ((float)_bytesCurrent /(float)_bytesTotal * 100);
                Log.d("percentage","" +percentage);
                pb.setProgress((int) percentage);
                _status.setText(percentage + "%");
            }

            @Override
            public void onError(int id, Exception ex) {

                Toast.makeText(MainActivity.this, "" + ex.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

    }



    void init() {
        new ImagePicker.Builder(this)
                .mode(ImagePicker.Mode.CAMERA_AND_GALLERY)
                .compressLevel(ImagePicker.ComperesLevel.MEDIUM)
                .directory(ImagePicker.Directory.DEFAULT)
                .extension(ImagePicker.Extension.JPG)
                .scale(600, 600)
                .allowMultipleImages(false)
                .enableDebuggingMode(true)
                .build();
    }
    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter f = new IntentFilter();
        f.addAction(UploadService.UPLOAD_STATE_CHANGED_ACTION);
        registerReceiver(uploadStateReceiver, f);
    }

    @Override
    protected void onStop() {
        unregisterReceiver(uploadStateReceiver);
        super.onStop();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == ImagePicker.IMAGE_PICKER_REQUEST_CODE && resultCode == RESULT_OK) {
            List<String> mPaths = data.getStringArrayListExtra(ImagePicker.EXTRA_IMAGE_PATH);
            mImageList.add(mPaths.get(0));
            Intent intent = new Intent(this, UploadService.class);
            intent.putExtra(UploadService.ARG_FILE_PATH, mPaths.get(0));
            startService(intent);
         //  upload(mPaths.get(0));
            //Your Code
        }
       else if (requestCode == FILE_SELECT_CODE) {
            if (resultCode == RESULT_OK) {
                // get path of selected file
                Uri uri = data.getData();
                String path = getPathFromContentUri(uri);
                Log.d("S3", "uri=" + uri.toString());
                Log.d("S3", "path=" + path);
                // initiate the upload
                Intent intent = new Intent(this, UploadService.class);
                intent.putExtra(UploadService.ARG_FILE_PATH, path);
                startService(intent);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private String getPathFromContentUri(Uri uri) {
        String path = uri.getPath();
        if (uri.toString().startsWith("content://")) {
            String[] projection = { MediaStore.MediaColumns.DATA };
            ContentResolver cr = getApplicationContext().getContentResolver();
            Cursor cursor = cr.query(uri, projection, null, null, null);
            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        path = cursor.getString(0);
                    }
                } finally {
                    cursor.close();
                }
            }

        }
        return path;
    }

    private BroadcastReceiver uploadStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle b = intent.getExtras();
            status.setText(b.getString(UploadService.MSG_EXTRA));
            int percent = b.getInt(UploadService.PERCENT_EXTRA);
            progress.setIndeterminate(percent < 0);
            progress.setProgress(percent);
        }
    };
}
