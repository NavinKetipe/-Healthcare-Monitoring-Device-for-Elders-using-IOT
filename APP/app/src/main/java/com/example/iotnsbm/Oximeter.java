package com.example.iotnsbm;


import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class Oximeter extends AppCompatActivity {

        private TextView AvgBPM;
        private  TextView Status;
        private TextView BPM;
        private TextView IR;

        @SuppressLint("MissingInflatedId")
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_oximeter);

            // Initialize TextViews
            AvgBPM = findViewById(R.id.abpm);
            BPM = findViewById(R.id.bpm);
            IR = findViewById(R.id.ir);
            Status = findViewById(R.id.status);

            // Get reference to the Firebase database
            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference().child("oximeter");

            // Add a listener for changes to the data at this location
            databaseReference.addValueEventListener(new ValueEventListener() {
                @SuppressLint("SetTextI18n")
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    // Retrieve values from the dataSnapshot
                    Double apm = dataSnapshot.child("AvgBPM").getValue(Double.class);
                    Double bpm = dataSnapshot.child("BPM").getValue(Double.class);
                    Double ir = dataSnapshot.child("IR").getValue(Double.class);


//                 Update TextViews with retrieved values
                    if (ir >= 50000) {
                        if (apm >= 60 && apm <= 100) {
                            Status.setText("Normal");
                        } else if (apm < 60) {
                            Status.setText("Lower");
                        } else {
                            Status.setText("High");
                        }
                    } else {
                        Status.setText("No Finger Detected");
                    }
                    AvgBPM.setText(String.valueOf(apm));
                    BPM.setText(String.valueOf(bpm));
                    IR.setText(String.valueOf(ir));
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    // Handle errors
                    Toast.makeText(com.example.iotnsbm.Oximeter.this, "Failed to retrieve data: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
