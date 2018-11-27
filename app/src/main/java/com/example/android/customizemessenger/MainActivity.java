package com.example.android.customizemessenger;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.io.File;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final int PICK_IMAGE = 1;

    private EditText editText;
    private ImageView imageView;
    private TextView textView;
    private Button button;
    private ProgressDialog mApkGenerationDialog;
    private String path;
    private final Handler handlerToast = new Handler() {
        public void handleMessage(Message message) {
            if (message.arg1 == -1) {
                Toast.makeText(MainActivity.this, "Build unsuccessful", Toast.LENGTH_SHORT).show();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editText = findViewById(R.id.edit_text);
        imageView = findViewById(R.id.image_view);
        textView = findViewById(R.id.text_view);
        button = findViewById(R.id.create_apk);

        imageView.setOnClickListener(this);
        textView.setOnClickListener(this);
        button.setOnClickListener(this);

    }

    private void save_apk() {
        String savedFilePath;
        savedFilePath = Environment.getExternalStorageDirectory() + File.separator + "Files" + File.separator+"Application.apk";
        if (savedFilePath == null || savedFilePath.length() == 0) {
            return;
        }
        String keyPassword = getString(R.string.key_password);
        String aliasName = getString(R.string.alias_name);
        String aliaspassword = getString(R.string.alias_password);
        KeyStoreDetails keyStoreDetails = new KeyStoreDetails(keyPassword, aliasName, aliaspassword);
        SignerThread signer = new SignerThread(getApplicationContext(), "App.apk"
                , savedFilePath, keyStoreDetails
                , path, editText.getText().toString());

        mApkGenerationDialog = new ProgressDialog(MainActivity.this, R.style.AppDialogTheme);
        mApkGenerationDialog.setTitle(R.string.apk_progress_dialog);
        mApkGenerationDialog.setMessage(getString(R.string.apk_msg));
        mApkGenerationDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mApkGenerationDialog.setCancelable(false);
        mApkGenerationDialog.setProgress(0);
        mApkGenerationDialog.show();

        signer.setSignerThreadListener(new SignerThread.OnSignComplete() {
            @Override
            public void onSuccess(final String path) {
                mApkGenerationDialog.dismiss();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        AlertDialog dialog = new AlertDialog.Builder(MainActivity.this)
                                .setTitle("Apk Generated")
                                .setMessage("Apk file saved at " + path)
                                .setPositiveButton("okay", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                })
                                .create();
                        dialog.show();
                    }
                });


            }

            @Override
            public void onFail(Exception e) {
                if (e != null) {
                    e.printStackTrace();
                    mApkGenerationDialog.dismiss();
                    Message message = handlerToast.obtainMessage();
                    message.arg1 = -1;
                    handlerToast.sendMessage(message);
                }
            }
        });

        signer.start();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.create_apk:
                save_apk();
                break;
            case R.id.text_view:
            case R.id.image_view:
                if (!checkIfAlreadyHavePermission())
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE
                                    , Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            1);
                else {
                    pickFile();
                }
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    pickFile();
                } else {
                    Toast.makeText(this, "Provide Permission First", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void pickFile() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE);
    }

    private boolean checkIfAlreadyHavePermission() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                        && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case PICK_IMAGE:
                    Uri uri = null;
                    if (data != null) {
                        uri = data.getData();
                        try {
                            path = FilePicker.getPath(this, uri);
                            Glide.with(this)
                                    .load(new File(path)) // Uri of the picture
                                    .into(imageView);
                        } catch (Exception e) {
                            path = null;
                            e.printStackTrace();
                        }
                    }
                    break;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}