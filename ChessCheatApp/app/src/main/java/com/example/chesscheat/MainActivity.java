package com.example.chesscheat;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.GameState;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Set;

import nl.s22k.chess.eval.EvalUtil;
import nl.s22k.chess.search.ThreadData;

public class MainActivity extends AppCompatActivity {

    // Used when creating
    public static final int SERVICE_ID = 438911;

    // For service
    public static Intent serviceIntent;

    // For logs
    public static String boardTableFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
    private TextView multiView;
    private TextView fenView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        Log.d("[CHESS-CHEAT]", "Started");

        //List of required permissions needed from user
        String[] permissions = {
                Manifest.permission.FOREGROUND_SERVICE,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.WAKE_LOCK
        };

        //Request permissions if one of them is missing
        for (String perm : permissions) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, permissions, 1);
                break;
            }
        }

        // Make sure that bluetooth is enabled
        if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
            BluetoothAdapter.getDefaultAdapter().enable();
        }

        // Create the service intent
        serviceIntent = new Intent(this, BluetoothService.class);


        // Get the view so we can edit
        multiView = (TextView) findViewById(R.id.multiTextView);
        multiView.setClickable(false);
        multiView.setTypeface(Typeface.MONOSPACE);    //all characters the same width

        fenView = (TextView) findViewById(R.id.fenTextView);

        // Thread to keep updating board
        Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            private long startTime = System.currentTimeMillis();
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(1000);
                    }
                    catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    handler.post(new Runnable(){
                        public void run() {
                            multiView.setText(fenBoardToString(boardTableFen));

                            fenView.setText(boardTableFen);
                        }
                    });
                }
            }
        };
        Thread t = new Thread(runnable);
        t.setDaemon(true);
        t.start();

    }



    public void startBtn(View view) {
        startService(serviceIntent);
    }

    public void endBtn(View view) {
        stopService(serviceIntent);
    }

    public void clearBtn(View view) {

    }

    public String fenBoardToString(String fen) {

        // Split into rows
        String[] rows = fen.split(" ")[0].split("/");

        // 8x8 table
        String[][] table = new String[8][8];

        // Fill table using FEN
        for (int i = 0; i < rows.length; i++) {

            int k = 0;

            for (int j = 0; j < rows[i].length(); j++) {
                char c = rows[i].charAt(j);

                if ("12345678".contains(String.valueOf(c))) {
                    int n = Character.getNumericValue(c);

                    for (int l = 0; l < n; l++) {
                        table[i][k] = "--";
                        k++;
                    }
                }

                else if (c == 'p') {
                    table[i][k] = "bp";
                    k++;
                }

                else if (c == 'P') {
                    table[i][k] = "wp";
                    k++;
                }

                else if (c < 'Z') {
                    table[i][k] = "w" + Character.toLowerCase(c);
                    k++;
                } else {
                    table[i][k] = "b" + c;
                    k++;
                }

            }
        }

        // Create the ASCII table
        String temp = "+----+----+----+----+----+----+----+----+\n| @ | @ | @ | @ | @ | @ | @ | @ |\n";
        String formatTable = "";

        for (int i = 0; i < 8; i++) {
            formatTable += temp;
        }
        formatTable += "+----+----+----+----+----+----+----+----+";

        // Make replacements into table
        for (int i = 0; i < table.length; i++) {
            for (int j = 0; j < table[i].length; j++) {
                formatTable = formatTable.replaceFirst("@", table[i][j]);
            }
        }

        // Return table string
        return formatTable;
    }

}