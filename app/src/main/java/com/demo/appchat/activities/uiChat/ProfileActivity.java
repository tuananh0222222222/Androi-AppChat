package com.demo.appchat.activities.uiChat;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Toast;

import com.demo.appchat.R;
import com.demo.appchat.databinding.ActivityChatBinding;
import com.demo.appchat.databinding.ActivityMainBinding;
import com.demo.appchat.databinding.ActivityProfileBinding;
import com.demo.appchat.utilities.Constants;
import com.demo.appchat.utilities.PreferenceManager;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;

public class ProfileActivity extends AppCompatActivity {

    private ActivityProfileBinding binding;
    private PreferenceManager preferenceManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager = new PreferenceManager(getApplicationContext());


        loadUser();
        logout();
        Back();
    }

    private void logout(){
        binding.logout.setOnClickListener(v -> {
            signOut();
        });
    }
    private  void Back(){
        binding.imageBack.setOnClickListener(v ->{
            onBackPressed();
        });
    }

    private void loadUser() {

//        binding.textNameUser.setText(preferenceManager.getString(Constants.KEY_NAME));
         binding.textNameUser.setText(preferenceManager.getString(Constants.KEY_NAME));
        byte[] bytes = Base64.decode(preferenceManager.getString(Constants.KEY_IMAGE),Base64.DEFAULT);

        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes,0,bytes.length);
        binding.imageUser.setImageBitmap(bitmap);



    }
    private void showToast(String name){
        Toast.makeText(this, name, Toast.LENGTH_SHORT).show();
    }

    private  void signOut(){

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference documentReference = db.collection(Constants.KEY_COLLECTION_USER).document(
                preferenceManager.getString(Constants.KEY_USER_ID)
        );
        HashMap<String,Object> updates = new HashMap<>();
        updates.put(Constants.KEY_FCM_TOKEN, FieldValue.delete());
        documentReference.update(updates)
                .addOnSuccessListener(unused -> {
                    preferenceManager.clearPreference();
                    startActivity(new Intent(getApplicationContext(),SignInActivity.class));
                    finish();
                }).addOnFailureListener(e->showToast("co loi xay ra"));
    }

}