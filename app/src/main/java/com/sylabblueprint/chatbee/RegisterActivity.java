package com.sylabblueprint.chatbee;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class RegisterActivity extends AppCompatActivity {

    //Views
        EditText mEmailEt, mPasswordEt;
        Button mRegisterBtn;
        TextView mHaveAccountTv;

        // Progress bar to display while registering user
        ProgressDialog progressDialog;

        //Declare an instance of FirebaseAuth
        private FirebaseAuth mAuth;


        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_register);

            //Actionbar and Title
            ActionBar actionBar = getSupportActionBar();
            actionBar.setTitle("Create Account");

            // Enable back button
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);

            //init
            mEmailEt = findViewById(R.id.emailEt);
            mPasswordEt = findViewById(R.id.passwordEt);
            mRegisterBtn = findViewById(R.id.register_btn);
            mHaveAccountTv = findViewById(R.id.have_accountTv);

            //In the OnCreate() method, Initialise the FirebaseAuth Instance
            mAuth = FirebaseAuth.getInstance();

            // Initialize Firebase Auth

            progressDialog = new ProgressDialog(this);
            progressDialog.setMessage("Registering User ....");

            // On button click handler
            mRegisterBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    //Input  email, password
                    String email = mEmailEt.getText().toString().trim();
                    String password = mPasswordEt.getText().toString().trim();

                    //Validation
                    if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()){

                        //set error message and focus to email edittext
                        mEmailEt.setError("Invalid email format, Please enter a valid a valid email and try again!");
                        mEmailEt.setFocusable(true);
                    }

                    else if (password.length() < 6) {
                        //Set error and focus to password edittext
                        mPasswordEt.setError("Password length must be at least 6 characters or digits");
                        mPasswordEt.setFocusable(true);
                    }
                    else {
                        registerUser(email, password); // register the User
                    }
                }
            });

            //Login handler
            mHaveAccountTv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                    finish();
                }
            });
        }

        private void registerUser(String email, String password) {
            // email and password pattern is valid, show progress dialog and start registering user
            progressDialog.show();

            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {

                            if (task.isSuccessful()) {
                                // Sign in successful,dismiss dialog and start  register activity
                                progressDialog.dismiss();

                                FirebaseUser user = mAuth.getCurrentUser();
                                // Get user email and uid from auth
                                String email = user.getEmail();
                                String uid = user.getUid();

                                //When user is registered store user info in firebase realtime database too
                                //Using HashMap
                                HashMap<Object, String> hashMap = new HashMap<>();

                                // put info in HashMap
                                hashMap.put("email", email);
                                hashMap.put("uid", uid);
                                hashMap.put("name", "");  // Will add later (eg. edit profile)
                                hashMap.put("phone", "");  // Will add later (eg. edit profile)
                                hashMap.put("image", "");  // Will add later (eg. edit profile)
                                hashMap.put("cover", "");  // Will add later (eg. edit profile)


                                //Firebase database Instance
                                FirebaseDatabase database = FirebaseDatabase.getInstance();
                                //path to store user data
                                DatabaseReference reference = database.getReference("Users");
                                //Put data within Hashmap in database
                                reference.child(uid).setValue(hashMap);

                                Toast.makeText(RegisterActivity.this, "Registered...\n" +user.getEmail(), Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(RegisterActivity.this, DashboardActivity.class));
                                finish();
                            } else {
                                // If sign in fails, display a message to the user.
                                progressDialog.dismiss();
                                Toast.makeText(RegisterActivity.this, "Authentication failed due incorrect email or password...", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {

                    //Error, dismiss progress and get to show the error message
                    progressDialog.dismiss();
                    Toast.makeText(RegisterActivity.this, "" +e.getMessage(), Toast.LENGTH_SHORT).show();

                }
            });

        }

        @Override
        public boolean onSupportNavigateUp() {
            onBackPressed(); // go previous activity
            return super.onSupportNavigateUp();
        }
    }


