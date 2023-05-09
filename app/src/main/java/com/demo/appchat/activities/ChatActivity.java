package com.demo.appchat.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.demo.appchat.R;
import com.demo.appchat.databinding.ActivityChatBinding;

import com.demo.appchat.models.User;
import com.demo.appchat.utilities.Constants;

public class ChatActivity extends AppCompatActivity {

    private ActivityChatBinding binding;
    private User receivedUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        loadReceiverDetails();
        serListener();

    }

    private void loadReceiverDetails() {
        receivedUser =(User) getIntent().getSerializableExtra(Constants.KEY_USER);
        binding.textName.setText(receivedUser.name);
    }

    private void serListener() {
        binding.imageBack.setOnClickListener(v -> onBackPressed());
    }

}