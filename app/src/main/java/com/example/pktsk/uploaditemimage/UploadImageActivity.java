package com.example.pktsk.uploaditemimage;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class UploadImageActivity extends AppCompatActivity {
    private Uri filePath;
    private final int PICK_IMAGE_REQUEST = 71;
    private ImageView imageView;
    private EditText imageName, quantity;

    private static final String IMAGE_URL_KEY = "image_url";
    private static final String ITEMS_ISSUED_BY_KEY = "item_issued_by";
    private static final String QUANTITY_AVAILABLE_KEY = "quantity_available";

    //Firebase
    FirebaseStorage storage;
    StorageReference storageReference;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_upload_image);
        imageView = findViewById(R.id.imgView);
        imageName = findViewById(R.id.img_name);
        quantity = findViewById(R.id.quantity);

        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        db = FirebaseFirestore.getInstance();
    }

    public void browseImage(View v){
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK
                && data != null && data.getData() != null )
        {
            filePath = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), filePath);
                imageView.setImageBitmap(bitmap);
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    public void uploadImage(View v){
        if(filePath != null) {
            final ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setTitle("Uploading...");
            progressDialog.show();
            final String item_name = imageName.getText().toString();
            StorageReference ref = storageReference.child("components/" + item_name);
            ref.putFile(filePath)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            progressDialog.dismiss();
                            Uri downloadUrl = taskSnapshot.getDownloadUrl();
                            String quantity_available = quantity.getText().toString();
                            Toast.makeText(UploadImageActivity.this, "Image uploaded", Toast.LENGTH_LONG).show();
                            Log.d("UploadImageActivity", "item_name: " + item_name + " quantity_avaailable: " + quantity_available + " downloadUrl: " + downloadUrl);
                            storeInFireStore(item_name, downloadUrl.toString(), quantity_available);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            progressDialog.dismiss();
                            Toast.makeText(UploadImageActivity.this, "Failed " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                            double progress = (100.0 * taskSnapshot.getBytesTransferred() / taskSnapshot
                                    .getTotalByteCount());
                            progressDialog.setMessage("Uploaded " + (int) progress + "%");
                        }
                    });
        }else{
                Toast.makeText(UploadImageActivity.this, "Image Name required", Toast.LENGTH_SHORT).show();
            }
        }

    //bug fixed
    public void storeInFireStore(String item_name, String downloadUrl, String quantity_available) {
        Log.d("storeInFireStore", item_name + " " + downloadUrl + " " + quantity_available);
        Map < String, Object > item_info = new HashMap < > ();
        item_info.put(IMAGE_URL_KEY, downloadUrl);
        item_info.put(ITEMS_ISSUED_BY_KEY, "");
        item_info.put(QUANTITY_AVAILABLE_KEY, quantity_available);
        db.collection("components").document(item_name).set(item_info)
                .addOnSuccessListener(new OnSuccessListener < Void > () {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(UploadImageActivity.this, "Item Added",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(UploadImageActivity.this, "ERROR" + e.toString(),
                                Toast.LENGTH_SHORT).show();
                        Log.d("TAG", e.toString());
                    }

    });
    }

}
