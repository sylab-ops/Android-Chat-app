package com.sylabblueprint.chatbee.adapters;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;
import com.sylabblueprint.chatbee.MainActivity;
import com.sylabblueprint.chatbee.R;
import com.sylabblueprint.chatbee.models.ModelChat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    //Views from XML
    Toolbar toolbar;
    RecyclerView recyclerView;
    ImageView profileIv;
    TextView nameTv, userStatusTv;
    EditText messageEt;
    ImageButton sendBtn;


    //Firebase Auth
    FirebaseAuth firebaseAuth;

    FirebaseDatabase firebaseDatabase;
    DatabaseReference  usersDbRef;

    //For checking if user has seen the message or not
    ValueEventListener seenListener;
    DatabaseReference userRefForSeen;

    List<ModelChat> chatList;
     AdapterChat adapterChat;

    String hisUid;
    String myUid;
    String hisImage;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        //Init views
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle("");
        recyclerView = findViewById(R.id.chat_recyclerView);
        profileIv = findViewById(R.id.profileIv);
        nameTv = findViewById(R.id.nameTv);
        userStatusTv = findViewById(R.id.userStatusTv);
        messageEt = findViewById(R.id.messageEt);
        sendBtn = findViewById(R.id.sendBtn);

        //Layout (LinearLayout) For  RecyclerView
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(true);

        //recyclerview properties
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(linearLayoutManager);


        //On clicking user from the user list, we have passed the user ID using intent
        Intent intent = getIntent();
        hisUid = intent.getStringExtra("hisUid");


        //Firebase Auth instance
        firebaseAuth = FirebaseAuth.getInstance();


        firebaseDatabase = FirebaseDatabase.getInstance();
        usersDbRef  = firebaseDatabase.getReference("Users");

        //search user to get user's info
        Query userQuery = usersDbRef.orderByChild("uid").equalTo(hisUid);

        //get user  picture and name
        userQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                //Check until required info is received
                for (DataSnapshot ds:  dataSnapshot.getChildren()) {
                    //Get all data
                    String name =" " + ds.child("name").getValue();
                    hisImage =" " + ds.child("image").getValue();

                    //Set data
                    nameTv.setText(name);
                    try {
                        //image received, set it to imageView in toolbar
                        Picasso.get().load(hisImage).placeholder(R.drawable.ic_default_img_white).into(profileIv);

                    }
                    catch (Exception e) {

                        //There is an exception getting picture, set default  picture
                        Picasso.get().load(R.drawable.ic_default_img_white).into(profileIv);

                    }

                }


            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        //Click button to send message
        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Get text from editText field
                String message = messageEt.getText().toString().trim();
                //Check if text field is empty or not
                if (TextUtils.isEmpty(message)) {
                    //Text field is empty
                    Toast.makeText(ChatActivity.this, "Cannot send an empty message...", Toast.LENGTH_SHORT).show();

                } else {
                    //Text field not empty
                    sendMessage(message);
                }
            }
        });

            readMessage();

            seenMessage();

    }

    private void seenMessage() {

        userRefForSeen = FirebaseDatabase.getInstance().getReference("Chats");
        seenListener = userRefForSeen.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot ds: dataSnapshot.getChildren()) {
                    ModelChat chat  = ds.getValue(ModelChat.class);
                    if (chat.getReceiver().equals(myUid) && chat.getSender().equals(hisUid)) {
                        HashMap<String, Object> hasSeenHashMap = new HashMap<>();
                        hasSeenHashMap.put("isSeen", true);
                        ds.getRef().updateChildren(hasSeenHashMap);

                    }
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private void readMessage(){
         chatList = new ArrayList<>();
         DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("Chats");
         dbRef.addValueEventListener(new ValueEventListener() {
             @Override
             public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    chatList.clear();
                    for (DataSnapshot ds: dataSnapshot.getChildren()){
                        ModelChat chat = ds.getValue(ModelChat.class);
                        if (chat.getReceiver().equals(myUid) &&  chat.getSender().equals(hisUid) ||
                                chat.getReceiver().equals(hisUid) &&  chat.getSender().equals(myUid)) {
                            chatList.add(chat);
                        }
                        //Adapter
                        adapterChat = new AdapterChat(ChatActivity.this, chatList, hisImage);
                        adapterChat.notifyDataSetChanged();
                        //set adapter to recyclerview
                        recyclerView.setAdapter(adapterChat);
                    }
             }

             @Override
             public void onCancelled(@NonNull DatabaseError databaseError) {

             }
         });

    }

    private void sendMessage(String message) {
        //Chat node will be created  that will contain all chats history

        DatabaseReference databaseReference = FirebaseDatabase .getInstance().getReference();

        String timestamp = String.valueOf(System.currentTimeMillis());

        HashMap<String, Object> hashMap  = new HashMap<>();
        hashMap.put("sender", myUid);
        hashMap.put("receiver", hisUid);
        hashMap.put("message", message);
        hashMap.put("timestamp", timestamp);
        hashMap.put("isSeen", false);

        databaseReference.child("Chats").push().setValue(hashMap);

        //Reset edit text after sending message
        messageEt.setText("");
    }

    private void checkUserStatus(){
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if(user != null){
            //user is signed in stay here
            //Set email of Logged in User

            //mProfileTv.setText(user.getEmail());
            myUid = user.getUid(); // Currently signed in user's uid



        }else {
            //User not signed in, go to main activity
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    @Override
    protected void onStart() {
        checkUserStatus();
        super.onStart();
    }

    @Override
    protected void onPause() {
        super.onPause();
        userRefForSeen.removeEventListener(seenListener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        //hide searchView, as we don't need it here!
        menu.findItem(R.id.action_search).setVisible(false);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        //Get item id
        int id = item.getItemId();
        if(id == R.id.action_logout) {
            firebaseAuth.signOut();
            checkUserStatus();
        }
        return super.onOptionsItemSelected(item);
    }
}
