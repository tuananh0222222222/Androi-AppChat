package com.demo.appchat.activities;



import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.demo.appchat.R;
import com.demo.appchat.adapter.ChatAdapter;
import com.demo.appchat.databinding.ActivityChatBinding;

import com.demo.appchat.models.ChatMessage;
import com.demo.appchat.models.User;
import com.demo.appchat.network.ApiClient;
import com.demo.appchat.network.ApiService;
import com.demo.appchat.utilities.Constants;
import com.demo.appchat.utilities.PreferenceManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatActivity extends BaseActivity {

    private ActivityChatBinding binding;
    private User receivedUser;
    private List<ChatMessage> chatMessages;
    private ChatAdapter chatAdapter;
    private  PreferenceManager preferenceManager;
    private FirebaseFirestore db ;
    private String conversionId = null;
    private boolean isReceiverAvailable = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        loadReceiverDetails();
        serListener();
        init();
        listenMessages();

    }

    private void init(){
        preferenceManager = new PreferenceManager(getApplicationContext());
        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(
                getBitMapEncode(receivedUser.image),
                chatMessages,
                preferenceManager.getString(Constants.KEY_USER_ID)
        );
        binding.chatRecycleView.setAdapter(chatAdapter);
        db = FirebaseFirestore.getInstance();
    }

    private  void sendMessage(){
        HashMap<String,Object> message = new HashMap<>();
        message.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
        message.put(Constants.KEY_RECEIVER_ID, receivedUser.id);
        message.put(Constants.KEY_MESSAGE, binding.inputMess.getText().toString());
        message.put(Constants.KEY_TIMESTAMP, new Date());
       db.collection(Constants.KEY_COLLECTION_CHAT).add(message);
       if(conversionId != null){
           updateConversion(binding.inputMess.getText().toString());
       }else {
           HashMap<String ,Object> conversion = new HashMap<>();
           conversion.put(Constants.KEY_SENDER_ID,preferenceManager.getString(Constants.KEY_USER_ID));
           conversion.put(Constants.KEY_SENDER_NAME,preferenceManager.getString(Constants.KEY_NAME));
           conversion.put(Constants.KEY_SENDER_IMAGE,preferenceManager.getString(Constants.KEY_IMAGE));
           conversion.put(Constants.KEY_RECEIVER_ID,receivedUser.id);
           conversion.put(Constants.KEY_RECEIVER_NAME,receivedUser.name);
           conversion.put(Constants.KEY_RECEIVER_IMAGE,receivedUser.image);
           conversion.put(Constants.KEY_LAST_MESSAGE,binding.inputMess.getText().toString());
           conversion.put(Constants.KEY_TIMESTAMP,new Date());
           addConversion(conversion);
       }
       if(!isReceiverAvailable){
           try {
            JSONArray token = new JSONArray();
            token.put(receivedUser.token);

            JSONObject data = new JSONObject();
               data.put(Constants.KEY_USER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
               data.put(Constants.KEY_NAME, preferenceManager.getString(Constants.KEY_NAME));
               data.put(Constants.KEY_FCM_TOKEN, preferenceManager.getString(Constants.KEY_FCM_TOKEN));
               data.put(Constants.KEY_MESSAGE, binding.inputMess.getText().toString());

               JSONObject body = new JSONObject();
               body.put(Constants.REMOTE_MSG_DATA,data);
               body.put(Constants.REMOTE_MSG_REGISTRATIONS_ID,token);

               sendNotification(body.toString());
           }catch (Exception e){
               showToast(e.getMessage());
           }
       }
       binding.inputMess.setText(null );
    }

    private void showToast(String mess){
        Toast.makeText(this, mess, Toast.LENGTH_SHORT).show();
    }
    private void sendNotification(String messageBody){
        ApiClient.getclient().create(ApiService.class).sendMessage(
                Constants.getRemoteMsgHeaders(),
                messageBody
        ).enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull Call<String> call,@NonNull  Response<String> response) {
                if(response.isSuccessful()){
                    try {
                       if(response.body() != null ){
                           JSONObject responseJson = new JSONObject(response.body());
                            JSONArray results  = responseJson.getJSONArray("results");
                            if(responseJson.getInt("failure") == 1){
                                JSONObject err = (JSONObject) results.get(0);
                                showToast(err.getString("error"));
                                return;
                            }
                       }
                    }catch (JSONException e){
                        e.printStackTrace();
                    }
                    showToast("nofication sent succes");
                }else {
                    showToast("error" + response.code());
                }

            }

            @Override
            public void onFailure(@NonNull  Call<String> call,@NonNull  Throwable t) {
                showToast(t.getMessage());
            }
        });
    }
    private void listenAvaibilityOfReceiver(){
        db.collection(Constants.KEY_COLLECTION_USER).document(
                receivedUser.id
        ).addSnapshotListener(ChatActivity.this ,(value,error) -> {
            if(error != null){
                return;
            }
            if(value != null){
                if(value.getLong(Constants.KEY_AVAILABILITY) != null){
                    int availability = Objects.requireNonNull(
                            value.getLong(Constants.KEY_AVAILABILITY)
                    ).intValue();
                    isReceiverAvailable = availability == 1;
                }
                receivedUser.token = value.getString(Constants.KEY_FCM_TOKEN);
                if(receivedUser.image == null){
                    receivedUser.image = value.getString(Constants.KEY_IMAGE);
                    chatAdapter.setReceiverProfileImage(getBitMapEncode(receivedUser.image));
                    chatAdapter.notifyItemChanged(0,chatMessages.size());
                }
            }
            if(isReceiverAvailable){
                binding.textAvability.setVisibility(View.VISIBLE);
            }else {
                binding.textAvability.setVisibility(View.GONE);
            }
        });
    }
    private void listenMessages(){
        db.collection(Constants.KEY_COLLECTION_CHAT)
                .whereEqualTo(Constants.KEY_SENDER_ID,preferenceManager.getString(Constants.KEY_USER_ID))
                .whereEqualTo(Constants.KEY_RECEIVER_ID,receivedUser.id)
                .addSnapshotListener(eventListener);
        db.collection(Constants.KEY_COLLECTION_CHAT)
                .whereEqualTo(Constants.KEY_SENDER_ID,receivedUser.id)
                .whereEqualTo(Constants.KEY_RECEIVER_ID,preferenceManager.getString(Constants.KEY_USER_ID))
                .addSnapshotListener(eventListener);
    }

    private final EventListener<QuerySnapshot> eventListener = (value , error) -> {
        if(error != null){
            return ;
        }
        if(value != null){
            int count = chatMessages.size();
            for (DocumentChange documentChange : value.getDocumentChanges()){
                if(documentChange.getType() == DocumentChange.Type.ADDED){
                    ChatMessage chatMessage = new ChatMessage();
                    chatMessage.senderId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                    chatMessage.receiverId = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                    chatMessage.message = documentChange.getDocument().getString(Constants.KEY_MESSAGE);
                    chatMessage.dateTime = getReadDateTime(documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP));
                    chatMessage.dateObject = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                    chatMessages.add(chatMessage);
                }
            }
            Collections.sort(chatMessages,(obj1 , obj2)->obj1.dateObject.compareTo(obj2.dateObject));
            if(count == 0){
                chatAdapter.notifyDataSetChanged();
            }else {
                chatAdapter.notifyItemRangeInserted(chatMessages.size(),chatMessages.size());
                binding.chatRecycleView.smoothScrollToPosition(chatMessages.size());
            }
            binding.chatRecycleView.setVisibility(View.VISIBLE);
        }
        binding.progressBar.setVisibility(View.GONE);
        if(conversionId == null){
            checkForConvertion();
        }
    };

    private Bitmap getBitMapEncode(String encodeImage){
        if(encodeImage != null){
            byte[] bytes = Base64.decode(encodeImage, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes,0,bytes.length);
        }
        else {
            return null;
        }

    }


    private void loadReceiverDetails() {
        receivedUser =(User) getIntent().getSerializableExtra(Constants.KEY_USER);
        binding.textName.setText(receivedUser.name);
    }

    private void serListener() {
        binding.imageBack.setOnClickListener(v -> onBackPressed());
        binding.layoutSend.setOnClickListener(v -> sendMessage());

    }

    private String getReadDateTime(Date date){
        return new SimpleDateFormat("dd MMMM yyyy - hh:mm ", Locale.getDefault()).format(date);
    }

    private  void addConversion(HashMap<String,Object> conversion){
        db.collection(Constants.KEY_CONVERSATIONS)
                .add(conversion)
                .addOnSuccessListener(documentReference -> conversionId = documentReference.getId());
    }

    private  void updateConversion(String mess){
        DocumentReference documentReference= db.collection(Constants.KEY_CONVERSATIONS).document(conversionId);
        documentReference.update(
                Constants.KEY_LAST_MESSAGE,mess,
                Constants.KEY_TIMESTAMP,new Date()
        );

    }
    private  void checkForConvertion(){
        if(chatMessages.size() != 0) {
            checkForConversationRemotely(
                    preferenceManager.getString(Constants.KEY_USER_ID),
                    receivedUser.id

            );
            checkForConversationRemotely(
                    receivedUser.id,
                    preferenceManager.getString(Constants.KEY_USER_ID)
            );
        }
    }
    private void checkForConversationRemotely(String senderId,String receiverId){
        db.collection(Constants.KEY_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_SENDER_ID,senderId)
                .whereEqualTo(Constants.KEY_RECEIVER_ID,receiverId)
                .get()
                .addOnCompleteListener(conversionOnComplete);
    }

    private  final OnCompleteListener<QuerySnapshot> conversionOnComplete = task -> {
        if(task.isSuccessful() && task.getResult() != null && task.getResult().getDocuments().size() > 0) {
            DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
            conversionId = documentSnapshot.getId();
        }
    };

    @Override
    protected void onResume() {
            super.onResume();
        listenAvaibilityOfReceiver();
    }
}