package com.example.iotnsbm;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private FirebaseDatabase mDatabase;
    private DatabaseReference mDatabaseRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        mDatabase = FirebaseDatabase.getInstance();
        mDatabaseRef = mDatabase.getReference("IP");

        // Button A
        // Button A
        Button buttonA = findViewById(R.id.button);
        buttonA.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG,"Pulse rate pressed");
                // Add a delay of 1 second (1000 milliseconds)
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // Call the method you want to execute after the delay
                        retrieveIPAddressAndMakeRequest("button3", "hr", Oximeter.class);
                    }
                }, 1000); // 1000 milliseconds = 1 second
            }
        });

// Button B
        Button buttonB = findViewById(R.id.button2);
        buttonB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG,"Accelerometer pressed");
                // Add a delay of 1 second (1000 milliseconds)
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // Call the method you want to execute after the delay
                        // retrieveIPAddressAndMakeRequest("buttonB");
                        retrieveIPAddressAndMakeRequest("button3", "axo", Accelerometer.class);
                    }
                }, 1000); // 1000 milliseconds = 1 second
            }
        });

// Button C
        Button buttonC = findViewById(R.id.button3);
        buttonC.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG,"Temperature pressed");
                // Add a delay of 1 second (1000 milliseconds)
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // Call the method you want to execute after the delay
                        retrieveIPAddressAndMakeRequest("button3", "bpm", Temperature.class);
                        Log.i(TAG,"Temperature done");
                    }
                }, 1000); // 1000 milliseconds = 1 second
            }
        });


//        mDatabaseRef.addValueEventListener(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//                // Handle data change
//                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
//                    String ipaddress = snapshot.getValue(String.class);
//                    Log.d(TAG, "Data: " + ipaddress);
//                }
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError databaseError) {
//                // Handle database error
//                Log.e(TAG, "Failed to read value.", databaseError.toException());
//            }});


    }
    private void retrieveIPAddressAndMakeRequest(final String buttonId, String endpoint, final Class<? extends Activity> destinationActivity) {
        mDatabaseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // Handle data change
                Log.i(TAG,"reading IP address");
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    try {
                        String ipAddress = snapshot.getValue(String.class);
                        Log.d(TAG, "IP Address: " + ipAddress);
                        makeGETRequest(ipAddress, endpoint, destinationActivity);
                    } catch (Exception e) {
                        Log.e(TAG, "Error retrieving IP Address: " + e.getMessage());
                    }
                    break;
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Failed to read value.", databaseError.toException());
            }
        });
    }

    private void makeGETRequest(String ipAddress, String endpoint, final Class<? extends Activity> destinationActivity) {
        RequestQueue queue = Volley.newRequestQueue(this);

        String urlStr = "http://"+ipAddress+"/"+endpoint;

        StringRequest stringRequest = new StringRequest(Request.Method.GET, urlStr,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d(TAG, "Response: ");
                        startActivity(new Intent(MainActivity.this, destinationActivity));
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "Request failed: " + error.getMessage());
            }
        });

        queue.add(stringRequest);

//
//
//
//
//
//        OkHttpClient client = new OkHttpClient();
//        Request request = new Request.Builder()
//                .url("http://192.168.1.8/bpm")
//                .build();
//
//        client.newCall(request).enqueue(new Callback() {
//            @Override
//            public void onFailure(Call call, IOException e) {
//                Log.e(TAG, "Request failed: " + e.getMessage());
//            }
//
//            @Override
//            public void onResponse(Call call, Response response) throws IOException {
//                Log.d(TAG, "Response: " + response.body().string());
//                startActivity(new Intent(MainActivity.this, destinationActivity));
//                // Redirect to corresponding UI here
//                // You can start a new activity or update UI elements accordingly
//            }
//        });
    }
}