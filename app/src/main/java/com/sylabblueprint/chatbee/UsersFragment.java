package com.sylabblueprint.chatbee;


import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
//import androidx.appcompat.widget.SearchView;
import androidx.core.view.MenuItemCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.sylabblueprint.chatbee.adapters.AdapterUsers;
import com.sylabblueprint.chatbee.models.ModelUsers;

import java.util.ArrayList;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 */
public class UsersFragment extends Fragment {

    RecyclerView recyclerView;
    AdapterUsers adapterUsers;
    List<ModelUsers> usersList;

    //Firebase Auth
    FirebaseAuth firebaseAuth;


    public UsersFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_users, container, false);

        //Init
        firebaseAuth = FirebaseAuth.getInstance();


        //Init recyclerView
        recyclerView =   view.findViewById(R.id.users_recyclerView);

        //Set it porperties
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        //Init user list
        usersList = new ArrayList<>();

        //Get all users
        getAllUser();


        return view;

    }

    private void getAllUser() {
        //Get all current users
        final FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();
        //Get path of database table name Users"
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        //Get all data from Path
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                usersList.clear();
                for (DataSnapshot ds: dataSnapshot.getChildren()){
                    ModelUsers modelUsers  = ds.getValue(ModelUsers.class);

                    //Get all users except currently signed in  user
                    if (!modelUsers.getUid().equals(fUser.getUid())){
                        usersList.add(modelUsers);


                    }
                    //adapter
                    adapterUsers = new AdapterUsers(getActivity(), usersList);
                    // Set adapter to recycler view
                    recyclerView.setAdapter(adapterUsers);

                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }


    private void searchUsers(final String query) {

        //Get all current users
        final FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();
        //Get path of database table name Users"
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        //Get all data from Path
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                usersList.clear();
                for (DataSnapshot ds: dataSnapshot.getChildren()){
                    ModelUsers modelUsers  = ds.getValue(ModelUsers.class);

                    //Get all searched users except currently signed in  user
                    if (!modelUsers.getUid().equals(fUser.getUid())){

                        if (modelUsers.getName().toLowerCase().contains(query.toLowerCase()) ||
                                modelUsers.getEmail().toLowerCase().contains(query.toLowerCase())) {
                            usersList.add(modelUsers);

                        }
                    }

                    //adapter
                    adapterUsers = new AdapterUsers(getActivity(), usersList);
                    //refresh adapter
                    adapterUsers.notifyDataSetChanged();
                    // Set adapter to recycler view
                    recyclerView.setAdapter(adapterUsers);

                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
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
    public void onCreate(@Nullable Bundle savedInstanceState){
        setHasOptionsMenu(true); // to show menu option in fragment
        super.onCreate(savedInstanceState);
    }

    //    Inflate option menu
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)   {
        //Inflating menu
        inflater.inflate(R.menu.main_menu, menu);

        //SearchView
        MenuItem item = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(item);

        //search listener
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                //Called when user pressed search button from the keyboard
                //if search Query is not  empty then search
                if (!TextUtils.isEmpty(s.trim())){
                    //search text contains  text
                    searchUsers(s);


                } else {
                    //Search text  empty,  get all users
                    getAllUser();

                }

                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                //Called whenever a user pressed a single letter
                //if search Query is not  empty then search
                if (!TextUtils.isEmpty(s.trim())){
                    //search text contains  text
                    searchUsers(s);

                } else {
                    //Search text  empty,  get all users
                    getAllUser();
                }

                return false;
            }
        });

        super.onCreateOptionsMenu(menu, inflater);
    }


    // Menu option handler
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item)    {
        //Get item id
        int id = item.getItemId();
        if(id == R.id.action_logout) {
            firebaseAuth.signOut();
            checkUserStatus();
        }
        return super.onOptionsItemSelected(item);
    }

}

// Model class for  recyclerView
