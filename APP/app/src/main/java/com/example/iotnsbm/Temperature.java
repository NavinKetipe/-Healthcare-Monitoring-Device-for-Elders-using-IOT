package com.example.iotnsbm;

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

public class Temperature extends AppCompatActivity {

    private TextView approxTextView;
    private TextView pressureTextView;
    private TextView tempTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_temperature);

        // Initialize TextViews
        approxTextView = findViewById(R.id.approx);
        pressureTextView = findViewById(R.id.pressure);
        tempTextView = findViewById(R.id.temp);

        // Get reference to the Firebase database
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference().child("bpmsensor");

        // Add a listener for changes to the data at this location
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // Retrieve values from the dataSnapshot
                Double approxAltitude = dataSnapshot.child("Approxaltitude").getValue(Double.class);
                Double pressure = dataSnapshot.child("Pressure").getValue(Double.class);
                Double temperature = dataSnapshot.child("Temperature").getValue(Double.class);

                Log.i("TAG", "onDataChange: "+approxAltitude);

//                 Update TextViews with retrieved values
                approxTextView.setText(approxAltitude + "a");
                pressureTextView.setText(pressure + "Pa");
                tempTextView.setText(temperature+"°C");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle errors
                Toast.makeText(Temperature.this, "Failed to retrieve data: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
