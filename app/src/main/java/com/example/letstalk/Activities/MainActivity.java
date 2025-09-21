package com.example.letstalk.Activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.letstalk.Adapters.TopStatusAdapter;
import com.example.letstalk.Models.Status;
import com.example.letstalk.Models.UserStatus;
import com.example.letstalk.R;
import com.example.letstalk.Models.User;
import com.example.letstalk.Adapters.UsersAdapter;
import com.example.letstalk.databinding.ActivityMainBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {
    ActivityMainBinding binding;
    FirebaseDatabase database;
    ArrayList<User> users;
    ArrayList<User> filteredUsers;
    UsersAdapter usersAdapter;
    TopStatusAdapter statusAdapter;
    ArrayList<UserStatus> userStatuses;
    ProgressDialog dialog;
    User user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        MaterialToolbar topAppBar = findViewById(R.id.topAppBar);
        setSupportActionBar(topAppBar);

        dialog = new ProgressDialog(this);
        dialog.setMessage("Uploading Image...");
        dialog.setCancelable(false);
        database = FirebaseDatabase.getInstance();
        users = new ArrayList<>();
        userStatuses = new ArrayList<>();
        filteredUsers = new ArrayList<>();
//        usersAdapter = new UsersAdapter(this, filteredUsers);
        binding.recyclerView.setAdapter(usersAdapter);

        // ✅ Safe null check for UID
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid != null) {
            database.getReference().child("users").child(uid)
                    .addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            user = snapshot.getValue(User.class);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });
        }

        usersAdapter = new UsersAdapter(this, users);
        statusAdapter = new TopStatusAdapter(this, userStatuses);
//      binding.recyclerView.setLayoutManager(new LinearLayoutManager(this)); : as done in XML

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(RecyclerView.HORIZONTAL);
//        binding.statusList.setLayoutManager(layoutManager);
//        binding.statusList.setAdapter(statusAdapter);

        binding.recyclerView.setAdapter(usersAdapter);

        binding.recyclerView.showShimmerAdapter();

        database.getReference().child("users").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                users.clear();
                for (DataSnapshot snapshot1 : snapshot.getChildren()) {
                    User user = snapshot1.getValue(User.class);
                    if (user != null && user.getUid() != null &&
                            !user.getUid().equals(FirebaseAuth.getInstance().getUid())) {
                        users.add(user);
                    }
                }
                filteredUsers.clear();
                filteredUsers.addAll(users);
                binding.recyclerView.hideShimmerAdapter();
                usersAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        database.getReference().child("stories").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    userStatuses.clear();
                    for (DataSnapshot storySnapshot : snapshot.getChildren()) {
                        UserStatus status = new UserStatus();
                        status.setName(storySnapshot.child("name").getValue(String.class));
                        status.setLastUpdated(storySnapshot.child("lastUpdated").getValue(Long.class));
                        status.setProfileImage(storySnapshot.child("profileImage").getValue(String.class));

                        ArrayList<Status> statuses = new ArrayList<>();

                        for (DataSnapshot statusSnapshot : storySnapshot.child("statuses").getChildren()) {
                            Status sampleStatus = statusSnapshot.getValue(Status.class);
                            statuses.add(sampleStatus);
                        }
                        status.setStatuses(statuses);

                        userStatuses.add(status);
                    }
                    statusAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        binding.bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();

                if (itemId == R.id.status) {
                    Intent intent = new Intent();
                    intent.setType("image/*");
                    intent.setAction(Intent.ACTION_GET_CONTENT);
                    startActivityForResult(intent, 75);
                    return true;
                }
//                else if (itemId == R.id.settings) {
//                    Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
//                    startActivity(intent);
//                    return true;
//                }
                return false;
            }
        });
    }

    // ------------------------------------------------------------------------------------- //

    @Override
    protected void onStart() {
        super.onStart();
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Intent intent = new Intent(MainActivity.this, OTPActivity.class);
            startActivity(intent);
            finish();
        }
    }

    // ------------------------------------------------------------------------------------- //

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (data != null) {
            if (data.getData() != null) {
                dialog.show();
                FirebaseStorage storage = FirebaseStorage.getInstance();
                Date date = new Date();
                StorageReference reference = storage.getReference().child("status").child(date.getTime() + "");

                reference.putFile(data.getData()).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                        if (task.isSuccessful()) {
                            reference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    UserStatus userStatus = new UserStatus();
                                    if (user != null) {
                                        userStatus.setName(user.getName());
                                        userStatus.setProfileImage(user.getProfileImage());
                                    }
                                    userStatus.setLastUpdated(date.getTime());

                                    HashMap<String, Object> obj = new HashMap<>();
                                    obj.put("name", userStatus.getName());
                                    obj.put("profileImage", userStatus.getProfileImage());
                                    obj.put("lastUpdated", userStatus.getLastUpdated());

                                    String imageUrl = uri.toString();

                                    Status status = new Status(imageUrl, userStatus.getLastUpdated());

                                    String uid = FirebaseAuth.getInstance().getUid();
                                    if (uid != null) {
                                        database.getReference()
                                                .child("stories")
                                                .child(uid)
                                                .updateChildren(obj);

                                        database.getReference()
                                                .child("stories")
                                                .child(uid)
                                                .child("statuses")
                                                .push()
                                                .setValue(status);
                                    }

                                    dialog.dismiss();
                                }
                            });
                        }
                    }
                });
            }
        }
    }

    // ------------------------------------------------------------------------------------- //

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.search) {
            Toast.makeText(this, "Search clicked.", Toast.LENGTH_SHORT).show();
            return true;
        }
//        else if (id == R.id.settings) {
//            Toast.makeText(this, "Settings clicked.", Toast.LENGTH_SHORT).show();
//            return true;
//        }
        else if (id == R.id.group) {
            Toast.makeText(this, "Groups clicked.", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.invite) {
            Toast.makeText(this, "Invite clicked.", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.logout) {
            FirebaseAuth.getInstance().signOut();

            Intent intent = new Intent(MainActivity.this, PhoneActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish(); // prevent user from coming back with back button
            return true;
        } else if (id == R.id.about) {
            Toast.makeText(this, "Invite clicked.", Toast.LENGTH_SHORT).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // ------------------------------------------------------------------------------------- //

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.topmenu, menu);

        MenuItem searchItem = menu.findItem(R.id.search);
        androidx.appcompat.widget.SearchView searchView =
                (androidx.appcompat.widget.SearchView) searchItem.getActionView();

        searchView.setQueryHint("Search users...");

        searchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterUsers(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterUsers(newText);
                return false;
            }
        });

        return true;
    }

    private void filterUsers(String query) {
        ArrayList<User> temp = new ArrayList<>();
        for (User u : users) {
            if (u.getName() != null && u.getName().toLowerCase().contains(query.toLowerCase())) {
                temp.add(u);
            }
        }
        filteredUsers.clear();
        filteredUsers.addAll(temp);
        usersAdapter.notifyDataSetChanged();
    }
}
