package com.demo.appchat.activities.uiChat;


import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;

import android.view.View;

import android.widget.Toast;


import com.demo.appchat.R;
import com.demo.appchat.adapter.RecentConversationAdapter;
import com.demo.appchat.databinding.ActivityMainBinding;
import com.demo.appchat.databinding.ItemContainerRecentConversionBinding;
import com.demo.appchat.databinding.LayoutNavigationHeaderBinding;
import com.demo.appchat.listeners.ConversionListener;
import com.demo.appchat.models.ChatMessage;

import com.demo.appchat.models.User;
import com.demo.appchat.utilities.Constants;
import com.demo.appchat.utilities.PreferenceManager;

import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;

import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.messaging.FirebaseMessaging;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class MainActivity extends BaseActivity implements ConversionListener  {

    private LayoutNavigationHeaderBinding layoutNavigationHeaderBinding;
    private ActivityMainBinding mainBinding;

    private PreferenceManager preferenceManager;
    private List<ChatMessage> conversation;
    private RecentConversationAdapter conversationAdapter;
    private FirebaseFirestore database;









    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mainBinding = ActivityMainBinding.inflate(getLayoutInflater());

        setContentView(mainBinding.getRoot());
        preferenceManager = new PreferenceManager(getApplicationContext());

        init();
        loadUserDetail();
        getToken();
        setListener();
        listenConversation();


    }





    private void init(){
        conversation = new ArrayList<>();
        conversationAdapter = new RecentConversationAdapter(conversation,this);
        mainBinding.conversationRecycleView.setAdapter(conversationAdapter);
        database = FirebaseFirestore.getInstance();

    }
    private  void setListener(){

//        mainBinding.imageSignout.setOnClickListener(v-> signOut());

        mainBinding.fabNewChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(),UserActivity.class));
            }
        });



    }


    // load image
    private void loadUserDetail() {

//        mainBinding.textName.setText(preferenceManager.getString(Constants.KEY_NAME));

        byte[] bytes = Base64.decode(preferenceManager.getString(Constants.KEY_IMAGE),Base64.DEFAULT);

        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes,0,bytes.length);
//       mainBinding.imageProfie.setImageBitmap(bitmap);



    }

    //showtoat
    private  void showToast(String mess){
        Toast.makeText(this, mess, Toast.LENGTH_SHORT).show();
    }

    private  void listenConversation(){
       database.collection(Constants.KEY_CONVERSATIONS)
               .whereEqualTo(Constants.KEY_SENDER_ID,preferenceManager.getString(Constants.KEY_USER_ID))
               .addSnapshotListener(eventListener);
       database.collection(Constants.KEY_CONVERSATIONS)
               .whereEqualTo(Constants.KEY_RECEIVER_ID,preferenceManager.getString(Constants.KEY_USER_ID))
               .addSnapshotListener(eventListener);
    }

    private final  EventListener<QuerySnapshot> eventListener = (value,error) ->{
        if(error!=null){
            return;
        }
        if(value!=null){
            for (DocumentChange documentChange : value.getDocumentChanges()){
                if(documentChange.getType() == DocumentChange.Type.ADDED){
                    String senderId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                    String receiverID = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                    ChatMessage chatMessage = new ChatMessage();
                    chatMessage.senderId = senderId;
                    chatMessage.receiverId = receiverID;
                    if(preferenceManager.getString(Constants.KEY_USER_ID).equals(senderId)){
                        chatMessage.conversionImage = documentChange.getDocument().getString(Constants.KEY_RECEIVER_IMAGE );
                        chatMessage.conversionName = documentChange.getDocument().getString(Constants.KEY_RECEIVER_NAME );
                        chatMessage.conversionId = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID );

                    }else {
                        chatMessage.conversionImage = documentChange.getDocument().getString(Constants.KEY_SENDER_IMAGE);
                        chatMessage.conversionName = documentChange.getDocument().getString(Constants.KEY_SENDER_NAME);
                        chatMessage.conversionId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                    }
                    chatMessage.message = documentChange.getDocument().getString(Constants.KEY_LAST_MESSAGE);
                    chatMessage.dateObject  = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                    conversation.add(chatMessage);
                } else if (documentChange.getType() == DocumentChange.Type.MODIFIED) {
                    for(int i = 0; i< conversation.size();i++){
                        String senderId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                        String receiverId = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                        if(conversation.get(i).senderId.equals(senderId) && conversation.get(i).receiverId.equals(receiverId)){
                           conversation.get(i).message = documentChange.getDocument().getString(Constants.KEY_LAST_MESSAGE);
                           conversation.get(i).dateObject = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                           break;
                        }
                    }
                }
            }
            Collections.sort(conversation,(obj1 , obj2)->obj2.dateObject.compareTo(obj1.dateObject));
            conversationAdapter.notifyDataSetChanged();
            mainBinding.conversationRecycleView.smoothScrollToPosition(0);
            mainBinding.conversationRecycleView.setVisibility(View.VISIBLE );
            mainBinding.progressBar.setVisibility(View.GONE);
        }
    };

    private  void getToken(){
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(this::updateToken);
    }
    private  void updateToken(String token){
        preferenceManager.putString(Constants.KEY_FCM_TOKEN,token);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference documentReference = db.collection(Constants.KEY_COLLECTION_USER).document(
                preferenceManager.getString(Constants.KEY_USER_ID)
        );
        documentReference.update(Constants.KEY_FCM_TOKEN,token);

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


    @Override
    public void onConversionClicked(User user) {
        Intent intent = new Intent(getApplicationContext(),ChatActivity.class);
        intent.putExtra(Constants.KEY_USER,user);
        startActivity(intent);
    }



}