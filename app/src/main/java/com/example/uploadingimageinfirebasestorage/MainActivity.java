package com.example.uploadingimageinfirebasestorage;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.BitmapCompat;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {
    ImageView imageView;
    Bitmap bitmapImage;
    Button browseBtn;
    Uri uri;
    Button uploadBtn;

    Boolean camera=false;

    static public int imageName=0;


    ActivityResultLauncher<String> openGalleryResultLauncher=
            registerForActivityResult(new ActivityResultContracts.GetContent(), new ActivityResultCallback<Uri>() {
                @Override
                public void onActivityResult(Uri result) {
                    if(result!=null) {
                        //Start from here
                        //bitmapImage=MediaStore.Images.Media.getBitmap();
                        uri=result;
                        try {
                            InputStream inputStream = getContentResolver().openInputStream(result);
                            bitmapImage= BitmapFactory.decodeStream(inputStream);
                            imageView.setImageBitmap(bitmapImage);

                        } catch (Exception e) {

                        }

                    }
                }
            });


    ActivityResultLauncher<String> cameraResultLauncher=
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
                @Override
                public void onActivityResult(Boolean result) {
                    if(result){
                        Intent intent = new Intent();
                        intent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
                        getContentFromCameraResultLauncher.launch(intent);
                    }else{
                        openDialog("Camera Access denied","Not able to access photos");
                    }

                }
            });
    ActivityResultLauncher<Intent> getContentFromCameraResultLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if(result.getResultCode()==RESULT_OK){

                        assert result.getData() != null;
                        if(result.getData().getExtras().get("data")!=null){
                            bitmapImage=(Bitmap) result.getData().getExtras().get("data");
                            imageView.setImageBitmap(bitmapImage);
                        }
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initialize();
        browseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openCustomDialog();
            }
        });
        uploadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(uri!=null){
                    saveInFirebaseStorage();
                }else if(camera){
                    saveInFirebaseStorage();
                }
                else{
                    Toast.makeText(MainActivity.this, "Atfirst select any image", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    private void initialize(){
        saveAndFetchDataFromSharedPreference(0);
        imageView=findViewById(R.id.imageView);
        browseBtn=findViewById(R.id.browseBtn);
        uploadBtn=findViewById(R.id.uploadBtn);
    }

    public void openDialog(String title,String message){
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setTitle(title).setMessage(message);
        alertDialog.setPositiveButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        alertDialog.create();
        alertDialog.show();
    }
    private void saveInFirebaseStorage(){
        final ProgressDialog dialog = new ProgressDialog(this);
        dialog.setTitle("File Uploader");
        dialog.show();
        FirebaseStorage firebaseStorage = FirebaseStorage.getInstance();
        StorageReference storageReference = firebaseStorage.getReference("FrontEndSprint").child(String.valueOf(String.valueOf(imageName)));
        imageName++;
        saveAndFetchDataFromSharedPreference(1);
        if(uri!=null) {
            storageReference.putFile(uri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            dialog.dismiss();
                            Toast.makeText(MainActivity.this, "File uploaded successfully", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(@NonNull UploadTask.TaskSnapshot snapshot) {
                            float percent = (float) (100 * snapshot.getBytesTransferred() / snapshot.getTotalByteCount());
                            dialog.setMessage("Uploaded: " + (int) percent + "%");
                        }
                    });

        }
        else if(bitmapImage!=null){
            if(camera){
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmapImage.compress(Bitmap.CompressFormat.PNG,100,baos);
                InputStream inputStream = new ByteArrayInputStream(baos.toByteArray());

                storageReference.putStream(inputStream)
                        .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                dialog.dismiss();
                                Toast.makeText(MainActivity.this, "File uploaded successfully", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onProgress(@NonNull UploadTask.TaskSnapshot snapshot) {
                                float percent = (float) (100 * snapshot.getBytesTransferred() / snapshot.getTotalByteCount());
                                dialog.setMessage("Uploaded: " + (int) percent + "%");
                            }
                        });
            }
        }
        uri=null;
        bitmapImage=null;
        camera=false;

    }

    private void openCustomDialog(){
        Dialog dialog = new Dialog(this);
        //LayoutInflater layoutInflater = LayoutInflater.from(this);
        //View view = layoutInflater.inflate(R.layout.custom_dialog,null);
        dialog.setContentView(R.layout.custom_dialog);
        ImageView galleryDialogBtn = dialog.findViewById(R.id.galleryDialogBtn);
        ImageView cameraDialogBtn = dialog.findViewById(R.id.cameraDialogBtn);
        galleryDialogBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                camera=false;
                openGalleryResultLauncher.launch("image/*");
                dialog.dismiss();
            }
        });
        cameraDialogBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                camera=true;
                cameraResultLauncher.launch(Manifest.permission.CAMERA);
                dialog.dismiss();
            }
        });
        dialog.create();
        dialog.show();
    }
    private void saveAndFetchDataFromSharedPreference(int flag){
        SharedPreferences sp=getSharedPreferences("IMAGE_NAMES",MODE_PRIVATE);
        if(flag==0) {//flag==0 means fetch data
            if (sp.contains("image_name")) {
                imageName = sp.getInt("image_name", 0);
            }
        }else{ //save data
            SharedPreferences.Editor editor = sp.edit();
            editor.putInt("image_name",imageName);
            editor.apply();
        }
    }
}