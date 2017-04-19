package com.project.pervsys.picaround;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.app.NavUtils;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
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
import com.google.firebase.database.ValueEventListener;
import com.project.pervsys.picaround.domain.Picture;
import com.squareup.picasso.Picasso;

import java.util.HashMap;

public class PictureActivity extends AppCompatActivity {

    private static final String TAG = "PictureActivity";
    private static final String PICTURE_ID = "pictureId";
    private FirebaseUser mUser;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private DatabaseReference mDatabaseRef = null;
    private Picture mPicture;
    private String mPictureId;
    private boolean mLike = false;
    private HashMap<String,String> mLikesList;
    private HashMap<String,String> mViewsList;
    private int mViewsNumber;
    private int mLikesNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_picture);

        // Set toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Set status bar color
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            Window window = this.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));
        }

        mAuth = FirebaseAuth.getInstance();
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // User is signed in
                    Log.d(TAG, "onAuthStateChanged:signed_in:" + user.getUid());
                } else {
                    // User is signed out
                    Log.d(TAG, "onAuthStateChanged:signed_out");
                }
            }
        };
        final ImageView pictureView = (ImageView) findViewById(R.id.picture);
        final ImageView userIcon = (ImageView) findViewById(R.id.user_icon);
        final TextView username = (TextView) findViewById(R.id.username);
        final TextView description = (TextView) findViewById(R.id.description);
        final TextView viewsTextView = (TextView) findViewById(R.id.views);
        final TextView likesTextView = (TextView) findViewById(R.id.likes);
        final TextView popularityTextView = (TextView) findViewById(R.id.popularity);
        final ImageButton likeButton = (ImageButton) findViewById(R.id.like_button);

        mUser = FirebaseAuth.getInstance().getCurrentUser();
        if (mUser != null) {
            likeButton.setVisibility(View.VISIBLE);
            Log.i(TAG, "Logged with Firebase, UID: " + mUser.getUid());
        }
        else {
            Log.i(TAG, "Not logged with Firebase");
        }

        Intent intent = getIntent();
        mPictureId = intent.getStringExtra(PICTURE_ID);

        mDatabaseRef = FirebaseDatabase.getInstance().getReference();
        mDatabaseRef.child("pictures").keepSynced(true);
        mDatabaseRef.child("pictures").orderByKey().equalTo(mPictureId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        for (DataSnapshot pictureSnap : dataSnapshot.getChildren()) {
                            mPicture = pictureSnap.getValue(Picture.class);
                            username.setText(mPicture.getUsername());
                            description.setText(mPicture.getDescription());

                            mLikesNumber = mPicture.getLikes();
                            mViewsNumber = mPicture.getViews();
                            setTextView(mLikesNumber,likesTextView);
                            setTextView(mViewsNumber,viewsTextView);

                            setPopularity(popularityTextView);

                            mViewsList = mPicture.getViewsList();
                            mLikesList = mPicture.getLikesList();

                            if (mViewsList == null)
                                mViewsList = new HashMap<>();
                            if (mLikesList == null)
                                mLikesList = new HashMap<>();

                            if (mUser != null){
                                if(!mViewsList.containsValue(mUser.getUid()))
                                    mDatabaseRef.child("pictures").child(mPictureId).child("viewsList").push().setValue(mUser.getUid());
                                if(mLikesList.containsValue(mUser.getUid()))
                                    mLike = true;
                            }

                            if (mLike)
                                likeButton.setColorFilter(ContextCompat.getColor(PictureActivity.this, R.color.colorAccent));
                            else
                                likeButton.setColorFilter(ContextCompat.getColor(PictureActivity.this, R.color.secondary_text_black));

                            Picasso.with(PictureActivity.this)
                                    .load(mPicture.getUserIcon())
                                    .into(userIcon);

                            Picasso.with(PictureActivity.this)
                                    .load(mPicture.getPath())
                                    .resize(getApplicationContext().getResources().getDisplayMetrics().widthPixels,
                                            getApplicationContext().getResources().getDisplayMetrics().heightPixels)
                                    .centerInside()
                                    .into(pictureView);

                        }

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        //database error, e.g. permission denied (not logged with Firebase)
                        Log.e(TAG, databaseError.toString());
                    }
                });

        likeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mLike) {
                    mLike = false;
                    mDatabaseRef.child("pictures").child(mPictureId).child("likesList")
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    for (DataSnapshot child : dataSnapshot.getChildren()) {
                                        if (child.getValue().equals(mUser.getUid()))
                                            child.getRef().removeValue();
                                    }
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {
                                    Log.e(TAG, databaseError.toString());
                                }
                            });

                    ((ImageButton) v).setColorFilter(ContextCompat.getColor(PictureActivity.this, R.color.secondary_text_black));
                    mLikesNumber--;
                    setTextView(mLikesNumber, likesTextView);
                    setPopularity(popularityTextView);
                }
                else {
                    mLike = true;
                    mDatabaseRef.child("pictures").child(mPictureId).child("likesList").push().setValue(mUser.getUid());
                    ((ImageButton) v).setColorFilter(ContextCompat.getColor(PictureActivity.this, R.color.colorAccent));
                    mLikesNumber++;
                    setTextView(mLikesNumber, likesTextView);
                    setPopularity(popularityTextView);
                }

            }
        });
    }

    private void setPopularity(TextView popularityTextView) {
        String popularityString = getString(R.string.popularity);
        int popularity;
        if (mViewsNumber != 0)
            popularity = (mLikesNumber/mViewsNumber);
        else
            popularity = 0;
        popularityTextView.setText(popularity*100 + "% " + popularityString);
        mDatabaseRef.child("pictures").child(mPictureId).child("popularity").setValue(1-popularity);
    }

    private void setTextView(int number, TextView textView){
        if (textView.getId() == R.id.likes) {
            String likesString;
            if (number == 1) likesString = getString(R.string.like);
            else likesString = getString(R.string.likes);
            textView.setText(number + " " + likesString);
        }
        else {
            String viewsString;
            if (number == 1) viewsString = getString(R.string.view);
            else viewsString = getString(R.string.views);
            textView.setText(number + " " + viewsString);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.settings:
                Log.i(TAG, "Settings has been selected");
                Toast.makeText(this, "Selected settings", Toast.LENGTH_SHORT).show();
                //Settings activity
                return true;
            case R.id.contact:
                Log.i(TAG, "Contact has been selected");
                Toast.makeText(this, "Selected contact", Toast.LENGTH_SHORT).show();
                //Contact activity
                return true;
            case R.id.help:
                Log.i(TAG, "Help has been selected");
                Toast.makeText(this, "Selected help", Toast.LENGTH_SHORT).show();
                //Help activity
                return true;
            case R.id.info:
                Log.i(TAG, "Info has been selected");
                Toast.makeText(this, "Selected info", Toast.LENGTH_SHORT).show();
                //Info activity
                return true;
            case R.id.profile:
                Log.i(TAG, "Profile has been selected");
                Toast.makeText(this, "Selected profile", Toast.LENGTH_SHORT).show();
                //Profile activity
                return true;
            case R.id.search:
                Log.i(TAG, "Search has been selected");
                Toast.makeText(this, "Selected search", Toast.LENGTH_SHORT).show();
                //Profile activity
                return true;
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
            default:
                return true;
        }
    }

    private HashMap<String,String> getViewsList(Picture picture){
        final HashMap<String,String> result = new HashMap<>();
        mDatabaseRef.child("pictures").child(picture.getId()).child("viewsList")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for (DataSnapshot child : dataSnapshot.getChildren()) {
                            result.put(child.getKey(), child.getValue().toString());
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        //database error, e.g. permission denied (not logged with Firebase)
                        Log.e(TAG, databaseError.toString());
                    }
                });

        return result;
    }

    private HashMap<String,String> getLikesList(Picture picture){
        final HashMap<String,String> result = new HashMap<>();
        mDatabaseRef.child("pictures").child(picture.getId()).child("likesList")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for (DataSnapshot child : dataSnapshot.getChildren()) {
                            result.put(child.getKey(), child.getValue().toString());
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        //database error, e.g. permission denied (not logged with Firebase)
                        Log.e(TAG, databaseError.toString());
                    }
                });

        return result;
    }
}