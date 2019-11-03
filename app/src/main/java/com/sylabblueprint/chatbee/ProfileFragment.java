package com.sylabblueprint.chatbee;
import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.security.Key;
import java.util.HashMap;

import static android.app.Activity.RESULT_OK;
import static com.google.firebase.storage.FirebaseStorage.getInstance;


/**
 * A simple {@link Fragment} subclass.
 */
public class ProfileFragment extends Fragment {
    //Firebase
    FirebaseAuth firebaseAuth;
    FirebaseUser user;
    FirebaseDatabase firebaseDatabase;
    DatabaseReference databaseReference;

    //Storage
    StorageReference storageReference;
    //Path where images of user profile would be stored
    String storagePath = "Users_Profile_Cover_Images/";

    //Views from Xml
    ImageView avatarIv, coverIv;
    TextView nameTv, emailTv, phoneTv;
    FloatingActionButton fab;

    //Progress Dialog
    ProgressDialog pd;

    //Permission constants
    private static final int CAMERA_REQUEST_CODE = 100;
    private static final int STORAGE_REQUEST_CODE = 200;
    private static final int IMAGE_PICK_GALLERY_CODE = 300;
    private static final int IMAGE_PICK_CAMERA_CODE = 400;

    //array of permission to be requested
    String cameraPermissions[];
    String storagePermissions[];


    //URI of picked image
    Uri image_uri;

    //For checking profile and cover photo
    String profileOrCoverPhoto;


    public ProfileFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_profile, container, false);


        //Init firebase
        firebaseAuth = FirebaseAuth.getInstance();
        user = firebaseAuth.getCurrentUser();
        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference("Users");
        storageReference = getInstance().getReference(); // Firebase storage reference


        //init arrays of permissions
        cameraPermissions = new String[] {Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermissions = new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE};

        //Init views
        avatarIv = view.findViewById(R.id.avatarIv);
        coverIv = view.findViewById(R.id.coverIv);
        nameTv = view.findViewById(R.id.nameTv);
        emailTv = view.findViewById(R.id.emailTv);
        phoneTv = view.findViewById(R.id.phoneTv);
        fab = view.findViewById(R.id.fab);

        //Init  progress dialog
        pd = new ProgressDialog(getActivity());

        Query query = databaseReference.orderByChild("email").equalTo(user.getEmail());
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                //Check until required data get
                for (DataSnapshot ds : dataSnapshot.getChildren()){
                    //get data
                    String name = ""+ ds.child("name").getValue();
                    String email = ""+ ds.child("email").getValue();
                    String phone = ""+ ds.child("phone").getValue();
                    String image = ""+ ds.child("image").getValue();
                    String cover = ""+ ds.child("cover").getValue();

                    //Set data
                    nameTv.setText(name);
                    emailTv.setText(email);
                    phoneTv.setText(phone);

                    try {
                        //if image is received then set
                        Picasso.get().load(image).into(avatarIv);

                    } catch (Exception e) {
                        //if there is any error.exception while getting the image then set default
                        Picasso.get().load(R.drawable.ic_default_img_white).into(avatarIv);

                    }

                    try {
                        //if image is received then set
                        Picasso.get().load(cover).into(coverIv);

                    } catch (Exception e) {
                        //if there is any error.exception while getting the image then set default

                    }

                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


        //fab button click
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showEditProfileDialog();
            }
        });


        return view;

    }

    private boolean checkStoragePermissions() {
        boolean result = ContextCompat.checkSelfPermission(getActivity(),Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);

        return  result;
    }

    private void requestStoragePermission(){
        //Request runtime storage permission
        requestPermissions(storagePermissions, STORAGE_REQUEST_CODE);
    }

    private boolean checkCameraPermissions(){
        boolean result = ContextCompat.checkSelfPermission(getActivity(),Manifest.permission.CAMERA) == (PackageManager.PERMISSION_GRANTED);

        boolean result1 = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);
        return  result && result1;
    }

    private void requestCameraPermission(){
        //Request runtime storage permission
        requestPermissions(cameraPermissions,CAMERA_REQUEST_CODE);
    }

    private void showEditProfileDialog() {

        //Option to show in dialog
        String options[ ] = {"Edit profile picture", "Edit cover photo", "Edit Name", "Edit Phone"};

        //alert dialog
        AlertDialog.Builder builder  = new AlertDialog.Builder(getActivity());

        //Set title
        builder.setTitle("Choose Action");

        //Set  items to dialog
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //Handle dialog items clicks
                if (which == 0){

                    //Edit profile clicked
                    pd.setMessage("Updating Profile Picture...");
                    profileOrCoverPhoto = "image"; //Changing profile picture
                    showImagePicDialog();
                    
                } else if (which == 1) {
                    //Edit Cover clicked
                    pd.setMessage("Updating Cover Photo...");
                    profileOrCoverPhoto = "cover"; // changing cover photo
                    showImagePicDialog();
                }
                else if (which == 2) {
                    //Edit Name clicked
                    pd.setMessage("Updating Name...");
                    //caliing method  to pass key  "name"  as parameter to update  it's value in database
                    showNamePhoneUpdateDialog("name");
                }
                else if (which == 3) {
                    //Edit Phone clicked
                    pd.setMessage("Updating Phone...");
                    //calling method  to pass key  "phone"  as parameter to update  it's value in database
                    showNamePhoneUpdateDialog("phone");

                }
            }
        });

        //Create and show Dialog
        builder.create().show();

    }

    private void showNamePhoneUpdateDialog(final String key) {
        //Parameter key wil contain value, either name or phone

        //Custom dialog
        AlertDialog.Builder builder   = new AlertDialog.Builder(getActivity());
        builder.setTitle("Update "+ key); //e.g update  name OR phone
        //set layout of dialog
        LinearLayout linearLayout = new LinearLayout(getActivity());
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setPadding(10,10,10,10);

        //add edit  text
        final EditText  editText =  new EditText(getActivity());
        editText.setHint("Enter "+ key); //hint edit name or phone

        linearLayout.addView(editText);

        builder.setView(linearLayout);

        //Add button in dialog to update
        builder.setPositiveButton("Update", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                //input text  from edit
                String value = editText.getText().toString().trim();

                //validate if user has entered something or not
                if (!TextUtils.isEmpty(value)){
                  pd.show();
                  HashMap<String, Object> result = new HashMap<>();
                  result.put(key, value);

                  databaseReference.child(user.getUid()).updateChildren(result)
                          .addOnSuccessListener(new OnSuccessListener<Void>() {
                              @Override
                              public void onSuccess(Void aVoid) {
                                  //updated, dismiss progress bar
                                  pd.dismiss();
                                  Toast.makeText(getActivity(), "Updated", Toast.LENGTH_SHORT).show();
                              }
                          })

                          .addOnFailureListener(new OnFailureListener() {
                              @Override
                              public void onFailure(@NonNull Exception e) {
                                  //failed, dismiss progress bar, get and show error message
                                  pd.dismiss();
                                  Toast.makeText(getActivity(), ""+e.getMessage(), Toast.LENGTH_SHORT).show();


                              }
                          });

                } else {
                    Toast.makeText(getContext(), "Please enter "+key, Toast.LENGTH_SHORT).show();

                }

            }
        });

        //add button in dialog to cancel
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            dialog.dismiss();

            }
        });

        //Create and show dialog
        builder.create().show();


    }

    private void showImagePicDialog() {
        //Show  dialog containing  options  Camera  and Gallery  to Pick the image
        //Option to show in dialog
        String options[ ] = {"Camera", "Gallery"};
        //alert dialog

        AlertDialog.Builder builder  = new AlertDialog.Builder(getActivity());
        //Set title
        builder.setTitle("Pick Image From");
        //Set  items to dialog
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //Handle dialog items click
                if (which == 0){

                    //Camera clicked
                    if (!checkCameraPermissions()){
                        requestCameraPermission();
                    } else {
                        pickFromCamera();
                    }

                } else if (which == 1) {

                    //Gallery clicked
                    if (!checkStoragePermissions()){
                        requestStoragePermission();
                    } else {
                        pickFromGallery();
                    }

                }
            }
        });

        //Create and show Dialog
        builder.create().show();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        //This method is called whenever the user press Allow or Deny from permission

        switch (requestCode){
            case CAMERA_REQUEST_CODE: {
               //Picking from camera, first check if camera permission is allowed  or not

                if (grantResults.length >0){
                    boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean writeStorageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;

                    if (cameraAccepted && writeStorageAccepted){
                        //Permissions enabled
                        pickFromCamera();

                    } else {
                        //Permissions denied
                        Toast.makeText(getActivity(), "Please enable camera and storage Permission ", Toast.LENGTH_SHORT).show();
                    }

                }
            }
            break;

            case STORAGE_REQUEST_CODE: {

                //Picking from gallery, first check if storage permission is allowed  or not

                if (grantResults.length >0){
                    boolean writeStorageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;

                    if (writeStorageAccepted){
                        //Permission enabled
                        pickFromGallery();
                    } else {
                        //Permission denied
                        Toast.makeText(getActivity(), "Please enable storage Permission ", Toast.LENGTH_SHORT).show();
                    }

                }

            }
            break;
        }

    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        //This method would be called after picking image from either camera or Gallery
        if (resultCode == RESULT_OK){
            if (requestCode == IMAGE_PICK_GALLERY_CODE){

                //image is picked from Gallery, get uri of the image
                image_uri = data.getData();
                uploadProfileCoverPhoto(image_uri);

            }
            if (requestCode == IMAGE_PICK_CAMERA_CODE){
                //image is picked from device,get uri of the image

                uploadProfileCoverPhoto(image_uri);


            }

        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void uploadProfileCoverPhoto(final Uri uri) {
        //Show progress
        pd.show();

        //Path and name of image to be stored in firebase storage
        String filePathAndName = storagePath + ""+ profileOrCoverPhoto +"_"+ user.getUid();
        StorageReference storageReference2nd = storageReference.child(filePathAndName);
        storageReference2nd.putFile(uri)

                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        //Image is uploaded to storage, now it gets Url and stored it on the user database
                        Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                        while (!uriTask.isSuccessful());
                        Uri downloadUri = uriTask.getResult();

                        //check if image is uploaded or not and url is removed
                        if (uriTask.isSuccessful()){

                            //image uploaded
                            //Add updated url to user database
                            HashMap<String, Object> results = new HashMap<>();
                            results.put(profileOrCoverPhoto, downloadUri.toString());

                            databaseReference.child(user.getUid()).updateChildren(results)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            //url added successfully to user database
                                            //Dismiss progress bar
                                            pd.dismiss();
                                            Toast.makeText(getActivity(), "Image updated successfully...", Toast.LENGTH_SHORT).show();

                                        }
                                    })

                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            //Error adding url in database of user
                                            //dismiss progress dialog bar

                                            pd.dismiss();
                                            Toast.makeText(getActivity(), "Error Updating Image...", Toast.LENGTH_SHORT).show();
                                            
                                        }
                                    });

                        } else {
                            //error
                            pd.dismiss();
                            Toast.makeText(getActivity(), "Some error occurred", Toast.LENGTH_SHORT).show();
                        }


                    }
                })

        .addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                //There is some error(s), get and show error message , dismiss progress  dialog
                pd.dismiss();
                Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();

            }
        });


    }

    private void pickFromCamera() {
        //Intent of picking image from Device
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "Temp Pic");
        values.put(MediaStore.Images.Media.DESCRIPTION, "Temp Description");

        //Put image uri
        image_uri = getActivity().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values);

        //Intent to start Camera
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri);
        startActivityForResult(cameraIntent,IMAGE_PICK_CAMERA_CODE);

    }

    private void pickFromGallery() {
        //Pick from gallery
        Intent galleryIntent = new Intent(Intent.ACTION_PICK);
        galleryIntent.setType("image/*");
        startActivityForResult(galleryIntent, IMAGE_PICK_GALLERY_CODE);

    }

    private void checkUserStatus(){
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if(user != null){
            //user is signed in stay here
            //Set email of Logged in User

            //mProfileTv.setText(user.getEmail());

        }else {
            //User not signed in, go to main activity
            startActivity(new Intent(getActivity(), MainActivity.class));
            getActivity().finish();
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        setHasOptionsMenu(true); // to show menu option in fragment
        super.onCreate(savedInstanceState);
    }

    //    Inflate option menu
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        //Inflating menu
        inflater.inflate(R.menu.main_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }


    // Menu option handler
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        //        get item id
        int id = item.getItemId();
        if(id == R.id.action_logout) {
            firebaseAuth.signOut();
            checkUserStatus();
        }
        return super.onOptionsItemSelected(item);
    }

}
