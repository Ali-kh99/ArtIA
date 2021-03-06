package com.moisegui.artia;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.moisegui.artia.data.model.Motif;
import com.moisegui.artia.services.MotifCallback;
import com.moisegui.artia.services.MotifService;

import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private int REQUEST_CODE_PERMISSIONS = 101;
    private String[] REQUIRED_PERMISSIONS = new String[]{"Manifest.permission.CAMERA",
            "Manifest.permission.READ_EXTERNAL_STORAGE", "Manifest.permission.WRITE_EXTERNAL_STORAGE"};

    // Get Started Button
    private Button getStartedBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Hide the Action bar
        try {
            this.getSupportActionBar().hide();
        } catch (NullPointerException e) {
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

        // To navigate from Main Acticity to Search Activity (for pattern detection)
        getStarted();

    }

    // To navigate from Main Acticity to Search Activity (for pattern detection)
    public void getStarted() {
        final Intent searchAct = new Intent(this, HomeActivity.class);
        getStartedBtn = findViewById(R.id.get_started);
        getStartedBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(searchAct);
            }
        });

        // Récupération des motifs
        MotifService.findAll(new MotifCallback() {
            @Override
            public void onCallback(List<Motif> motifs) {
                MotifDbHelper helper = new MotifDbHelper(getApplicationContext());
                helper.deleteDb();
                for (Motif motif : motifs) {
                    try {
                        MotifDbHelper.downloadMotifAndSave(getApplicationContext(), motif);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

    }

}