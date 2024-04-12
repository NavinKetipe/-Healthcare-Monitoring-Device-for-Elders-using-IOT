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

public class Accelerometer extends AppCompatActivity {

        private TextView AX;
        private TextView AY;
        private TextView AZ;
        private TextView GX;
        private TextView GY;
        private TextView GZ;

        @SuppressLint("MissingInflatedId")
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_accelerometer);

            // Initialize TextViews
            AX = findViewById(R.id.ax);
            AY = findViewById(R.id.ay);
            AZ = findViewById(R.id.az);
            GX = findViewById(R.id.gx);
            GY = findViewById(R.id.gy);
            GZ = findViewById(R.id.gz);

            // Get reference to the Firebase database
            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference().child("accelerometer");

            // Add a listener for changes to the data at this location
            databaseReference.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    // Retrieve values from the dataSnapshot
                    Double accelX = dataSnapshot.child("AccelX").getValue(Double.class);
                    Double accelY = dataSnapshot.child("AccelY").getValue(Double.class);
                    Double accelZ = dataSnapshot.child("AccelZ").getValue(Double.class);

                    Double gyroX = dataSnapshot.child("GyroX").getValue(Double.class);
                    Double gyroY = dataSnapshot.child("GyroY").getValue(Double.class);
                    Double gyroZ = dataSnapshot.child("GyroZ").getValue(Double.class);


//                 Update TextViews with retrieved values

                    if (accelX != null)
                        AX.setText(String.valueOf(accelX));
                    if (accelY != null)
                        AY.setText(String.valueOf(accelY));
                    if (accelZ != null)
                        AZ.setText(String.valueOf(accelZ));

                    if (gyroX != null)
                        GX.setText(String.valueOf(gyroX));
                    if (gyroY != null)
                        GY.setText(String.valueOf(gyroY));
                    if (gyroZ != null)
                        GZ.setText(String.valueOf(gyroZ));
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    // Handle errors
                    Toast.makeText(com.example.iotnsbm.Accelerometer.this, "Failed to retrieve data: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
