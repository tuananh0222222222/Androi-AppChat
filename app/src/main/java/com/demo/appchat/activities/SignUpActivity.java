package com.demo.appchat.activities;

import androidx.activity.result.ActivityResultCaller;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import com.demo.appchat.R;
import com.demo.appchat.databinding.ActivitySignUpBinding;
import com.demo.appchat.utilities.Constants;
import com.demo.appchat.utilities.PreferenceManager;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;

public class SignUpActivity extends AppCompatActivity {
    private ActivitySignUpBinding signUpBinding;
    private  String encodedImage;
    private PreferenceManager preferenceManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);
        signUpBinding = ActivitySignUpBinding.inflate(getLayoutInflater());
        setContentView(signUpBinding.getRoot());
        preferenceManager = new PreferenceManager(getApplicationContext());
        setlistener();

    }

    private void setlistener() {

        signUpBinding.textSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        signUpBinding.buttonSignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isValidSignup()){
                    signUp();
                }

            }
        });

        signUpBinding.layoutImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                pickImage.launch(intent);
            }
        });
    }
// TOAST
    private  void showToast(String mess){
        Toast.makeText(this, mess, Toast.LENGTH_SHORT).show();
    }
//SIGN UP
    private  void  signUp(){
        loading(true);

     FirebaseFirestore db =  FirebaseFirestore.getInstance();
        HashMap<String,Object> user = new HashMap<>();
        user.put(Constants.KEY_NAME,signUpBinding.inputName.getText().toString());
        user.put(Constants.KEY_EMAIL,signUpBinding.inputEmail.getText().toString());
        user.put(Constants.KEY_PASSWORD,signUpBinding.inputPassword.getText().toString());
        user.put(Constants.KEY_IMAGE,encodedImage);
        db.collection(Constants.KEY_COLLECTION_USER).add(user).addOnSuccessListener(documentReference -> {
            loading(false);
            preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN,true);
            preferenceManager.putString(Constants.KEY_USER_ID,documentReference.getId());
            preferenceManager.putString(Constants.KEY_NAME,signUpBinding.inputName.getText().toString());
            preferenceManager.putString(Constants.KEY_IMAGE,encodedImage );
            Intent intent = new Intent(getApplicationContext(),SignInActivity.class);
            startActivity(intent);


        }).addOnFailureListener(exception ->{
            loading(false);
            showToast(exception.getMessage());
        });




    }

    private  String encodeImage(Bitmap bitmap ){
        int previewWidth = 150;
        int previewHeight = bitmap.getHeight() * previewWidth / bitmap.getWidth();
        Bitmap previewBitmap = bitmap.createScaledBitmap(bitmap,previewWidth,previewHeight,false);
        ByteArrayOutputStream arrayOutputStream  = new ByteArrayOutputStream();
        previewBitmap.compress(Bitmap.CompressFormat.JPEG,50,arrayOutputStream);
        byte[] bytes = arrayOutputStream.toByteArray();
        return Base64.encodeToString(bytes,Base64.DEFAULT);
    }

    private final ActivityResultLauncher<Intent> pickImage = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if(result.getResultCode() == RESULT_OK ){
                    if(result.getData()!= null){
                        Uri imageUri = result.getData().getData();
                        try {
                            InputStream  inputStream = getContentResolver().openInputStream(imageUri);
                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                            signUpBinding.imageProfie.setImageBitmap(bitmap);
                            signUpBinding.textAddImage.setVisibility(View.GONE);
                            encodedImage = encodeImage(bitmap);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
    );
    private  boolean isValidSignup(){
        if(encodedImage == null){
            showToast("Chọn ảnh");
            return  false;
        } else if (signUpBinding.inputName.getText().toString().trim().isEmpty()) {
            showToast("Tên không được để trống");
            return  false;
        }else if (signUpBinding.inputName.getText().toString().trim().isEmpty()) {
            showToast("Tên không được để trống");
            return  false;
        }else if (signUpBinding.inputEmail.getText().toString().trim().isEmpty()) {
            showToast("Email không được để trống");
            return  false;
        }else if (!Patterns.EMAIL_ADDRESS.matcher(signUpBinding.inputEmail.getText().toString()).matches()) {
            showToast("Email không đúng định dạng");
            return false;

        }else if (signUpBinding.inputPassword.getText().toString().trim().isEmpty()) {
            showToast("Mật khẩu không được để trống");
            return  false;
        }else if (signUpBinding.inputComfirmPassword.getText().toString().trim().isEmpty()) {
            showToast("Mật khẩu không được để trống");
            return  false;
        }else if (!signUpBinding.inputPassword.getText().toString().equals(signUpBinding.inputComfirmPassword.getText().toString())) {
            showToast("Mật khẩu không khớp");
            return  false;
        }else {
            return  true;
        }

    }

    private  void loading(boolean isLoading){
        if(isLoading){
            signUpBinding.buttonSignup.setVisibility(View.INVISIBLE);
            signUpBinding.progressBar.setVisibility(View.VISIBLE);
        }else{
            signUpBinding.buttonSignup.setVisibility(View.VISIBLE);
            signUpBinding.progressBar.setVisibility(View.INVISIBLE);
        }
    }
}