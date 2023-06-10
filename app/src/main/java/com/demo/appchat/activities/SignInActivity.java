package com.demo.appchat.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import com.demo.appchat.R;
import com.demo.appchat.databinding.ActivitySignInBinding;
import com.demo.appchat.utilities.Constants;
import com.demo.appchat.utilities.PreferenceManager;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;

public class SignInActivity extends AppCompatActivity {
private PreferenceManager preferenceManager;
    private ActivitySignInBinding signInBinding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);
        signInBinding = ActivitySignInBinding.inflate(getLayoutInflater());
        setContentView(signInBinding.getRoot());
        preferenceManager = new PreferenceManager(getApplicationContext());
        if(preferenceManager.getBoolean(Constants.KEY_IS_SIGNED_IN)){
            Intent intent = new Intent(getApplicationContext(),MainActivity.class);
            startActivity(intent);
            finish();
        }
        setListener();


    }

    private  void signIn(){
        loading(true);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection(Constants.KEY_COLLECTION_USER)
                .whereEqualTo(Constants.KEY_EMAIL,signInBinding.inputEmail.getText().toString())
                .whereEqualTo(Constants.KEY_PASSWORD,signInBinding.inputPassword.getText().toString())
                .get()
                .addOnCompleteListener(task -> {
                    if(task.isSuccessful() && task.getResult() != null && task.getResult().getDocuments().size() > 0){
                        DocumentSnapshot  documentSnapshot = task.getResult().getDocuments().get(0) ;
                        preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN,true);
                        preferenceManager.putString(Constants.KEY_USER_ID,documentSnapshot.getId() );
                        preferenceManager.putString(Constants.KEY_NAME,documentSnapshot.getString(Constants.KEY_NAME) );
                        preferenceManager.putString(Constants.KEY_IMAGE,documentSnapshot.getString(Constants.KEY_IMAGE) );
                        Intent intent = new Intent(getApplicationContext(),MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);

                    }else {
                        loading(false);
                        showToast("Tài khoản mật khẩu không đúng");
                    }
                });
    }
    private  void setListener() {
        signInBinding.textCreateAccout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(),SignUpActivity.class));
            }
        });

        signInBinding.buttonSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isValid()){
                    signIn();
                }
            }
        });

    }
    private void  showToast(String mess){
        Toast.makeText(this, mess, Toast.LENGTH_SHORT).show();
    }

    private boolean isValid(){
        if(signInBinding.inputEmail.getText().toString().trim().isEmpty()){
            showToast("Email không được để trống");
            return  false;
        }else if(!Patterns.EMAIL_ADDRESS.matcher(signInBinding.inputEmail.getText().toString()).matches()){
            showToast("Email không đúng định dạng");
            return  false;
            
        } else if (signInBinding.inputPassword.getText().toString().trim().isEmpty()) {
            showToast("Mật khẩu không được để trống");
            return  false;
        }else {
            return  true;
        }
    }

    private  void loading(boolean isLoading){
        if(isLoading){
            signInBinding.buttonSignIn.setVisibility(View.INVISIBLE);
            signInBinding.progressBar.setVisibility(View.VISIBLE);
        }else{
            signInBinding.buttonSignIn.setVisibility(View.VISIBLE);
            signInBinding.progressBar.setVisibility(View.INVISIBLE);
        }
    }
}