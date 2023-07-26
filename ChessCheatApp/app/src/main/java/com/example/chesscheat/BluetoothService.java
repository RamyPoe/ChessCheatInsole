package com.example.chesscheat;


import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.move.Move;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

import nl.s22k.chess.*;
import nl.s22k.chess.engine.UciOut;
import nl.s22k.chess.move.MoveWrapper;
import nl.s22k.chess.search.SearchUtil;
import nl.s22k.chess.search.TTUtil;
import nl.s22k.chess.search.TimeUtil;


public class BluetoothService extends Service {

    // Config constant
    public static final String BLUETOOTH_DEVICE_NAME = "Stinky";

    // Thread that will run in background
    private Thread mainThread;
    private boolean stopThread = false;
    PowerManager.WakeLock wakeLock;

    // For bluetooth
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothSocket mmSocket;
    private BluetoothDevice mmDevice;
    private OutputStream mmOutputStream;
    private InputStream mmInputStream;

    // Chess engine
    public static ChessBoard chessBoard;
    public static Board checkBoard;
    private final String STARTING_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";



    // Setup
    @Override
    @RequiresApi(Build.VERSION_CODES.O)
    public void onCreate() {
        super.onCreate();

        // Setup as Foreground service
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = createNotificationChannel(notificationManager);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, channelId);
        Notification notification = notificationBuilder.setOngoing(true)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
//                .setContentTitle("THIS IS THE TITLE")
//                .setContentText("THIS IS THE TEXT")
                .build();

        startForeground(MainActivity.SERVICE_ID, notification);

        // Final log
        Log.i("[CHESS-CHEAT]", "Registered Service");

        // Check permission
        while (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {}

        // Start Bluetooth thread
        while (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {}
        startBluetoothSerialThread();

        // Acquire wake lock
        PowerManager mgr = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
        wakeLock = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "chesscheat:wakelock");
        wakeLock.acquire();

    }

    /* Connect to the named bluetooth device and open streams */
    private void startBluetoothSerialThread() {

        // Create the main thread
        mainThread = new Thread(new Runnable() {

            // Thread variables
            final byte delimiter = 33; //This is the ASCII code for a ! character
            int buffer_index = 0;
            byte[] readBuffer = new byte[1024];
            String data = "";

            // Thread start
            @SuppressLint("MissingPermission")
            public void run() {

                // Setup bluetooth connection
                boolean success = bluetoothSetup();
                if (!success) { return; }

                // Keep trying until we connect
                while (!connectBluetooth()) { if (stopThread) return; }
                Log.d("[CHESS-CHEAT]", "Bluetooth connected");

                // Setup chess board
                chessBoard = ChessBoardInstances.get(0);
                checkBoard = new Board();
                resetBoards();

                // Empty buffer
                try {
                    while (mmInputStream.available() > 0) { mmInputStream.read(); }
                } catch (Exception e) { e.printStackTrace(); }

                // ==========================
                // +++++++++ CHESS ++++++++++
                // ==========================


                // Main loop
                while (!Thread.currentThread().isInterrupted() && !stopThread) {



                    // Slow down so bluetooth can keep up
                    delay(400);



                    try {

                        // Read opponent move and verify
                        boolean validMove = false;
                        do {

                            try {

                                // Read the next packet
                                data = readPacket();
                                delay(400);

                                // Check for a start signal depending on which side we are playing
                                if ("wss".equals(data)) {
                                    resetBoards();

                                    ChessMove m = getBestMove(chessBoard, 3000);
                                    chessBoard.doMove(m.move);
                                    sendPacket(m.moveStr);

                                    // Update board on screen
                                    MainActivity.boardTableFen = ChessBoardUtil.toString(chessBoard);

                                    // Get next move
                                    data = readPacket();
                                    delay(400);

                                }
                                if ("bss".equals(data)) {
                                    resetBoards();

                                    // Get next move
                                    data = readPacket();
                                    delay(400);
                                }

                                // Try to apply move
                                applyMove(chessBoard, data);

                                // Good move
                                sendPacket("ok");

                                // Exit loop
                                validMove = true;

                            } catch (Exception e) {

                                e.printStackTrace();

                                // Acknowledge bad move
                                sendPacket("bad");

                                // Not valid
                                validMove = false;
                            }

                        } while (!validMove && !stopThread);

                        // Update board on screen
                        MainActivity.boardTableFen = ChessBoardUtil.toString(chessBoard);

                        // Make sure thread hasn't been stopped
                        if (stopThread) { break; }

                        // Get best move and send back
                        ChessMove m = getBestMove(chessBoard, 5000);
                        chessBoard.doMove(m.move);
                        sendPacket(m.moveStr);

                        // Update board on screen
                        MainActivity.boardTableFen = ChessBoardUtil.toString(chessBoard);


                    }
                    catch (Exception e)
                    {
                        Log.e("[CHESS-CHEAT]", "Service thread crashed");
                        e.printStackTrace();
                    }


                } // Main loop end

                // Log end
                Log.d("[CHESS-CHEAT]", "Thread stopped");

            }

            /* Pauses thread for duration */
            public void delay(int ms) {
                try {
                    Thread.sleep(ms);
                } catch (Exception e) { e.printStackTrace(); }
            }

            /* Applies the move to the chess board */
            public void applyMove(ChessBoard cb, String moveStr) throws Exception {

                // Check if valid
                checkBoard.loadFromFen(ChessBoardUtil.toString(chessBoard));
                Move m = new Move(moveStr, checkBoard.getSideToMove());
                if (!checkBoard.legalMoves().contains(m)) {
                    throw new Exception("Invalid Move");
                }

                // Turn into engine move
                MoveWrapper move = new MoveWrapper(moveStr, cb);

                // Apply to position
                cb.doMove(move.move);

            }

            /* Reset the chess boards */
            public void resetBoards() {
                ChessBoardUtil.setFen(STARTING_FEN, chessBoard);
                TTUtil.init(false);
                checkBoard.loadFromFen(STARTING_FEN);

                // Update board on screen
                MainActivity.boardTableFen = ChessBoardUtil.toString(chessBoard);
            }

            /* Get best engine move for the current board */
            public ChessMove getBestMove(ChessBoard cb, int timeMs) {

                // Run the engine for given time
                TimeUtil.reset();
                TimeUtil.setSimpleTimeWindow(timeMs);
                SearchUtil.start(cb);

                // Wrap the move into something usable
                String moveStr = UciOut.lastLineSent.split(" ")[0];
                MoveWrapper move = new MoveWrapper(moveStr, cb);

                // Return the move
                ChessMove m = new ChessMove(move.move, moveStr);
                return m;
            }

            /* Blocks until packet is sent */
            public String readPacket() {

                // To be returned
                String out = "";

                // Keep trying
                while (!stopThread) {

                    try {

                        // Wait for packet
                        while (mmInputStream.available() == 0) { if (stopThread) return null; }
                        Thread.sleep(300);

                        // Read the packet
                        byte[] packetBytes = new byte[mmInputStream.available()];
                        mmInputStream.read(packetBytes);

                        // Parse into string
                        final String d = decodePacket(packetBytes);
                        if (d == null || "".equals(d)) {
                            Log.e("[CHESS-CHEAT]", "Bad packet was received");
                        } else {
                            Log.w("[CHESS-CHEAT]", "GOT: " + d);
                            return d;
                        }

                    } catch (Exception e) {
                        Log.e("[CHESS-CHEAT]", "Bad packet was received");
                    }
                }

                return "";
            }

            public String decodePacket(byte[] packet) {

                // Variables
                buffer_index = 0;

                // Read bytes until delimiter
                for (int i = 0; i < packet.length; i++) {
                    byte b = packet[i];
                    if (b == delimiter) {
                        byte[] encodedBytes = new byte[buffer_index];
                        System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);

                        try {
                            String out = new String(encodedBytes, "US-ASCII");
                            out = out.replace(".", "");
                            return out;
                        } catch (Exception e) {
                            return null;
                        }

                    } else {
                        readBuffer[buffer_index++] = b;
                    }
                }

                // Bad packet
                Log.i("[CHESS-CHEAT]", "Bad here");
                return null;
            }

            public void sendPacket(String d) {
                Log.w("[CHESS-CHEAT]", "SENT: " + d);
                try {
                    d = ".." + d + "!";
                    mmOutputStream.write(d.getBytes());
                } catch (IOException e) {
                    Log.e("[CHESS-CHEAT]", "Error while sending packet");
                }
            }

            @SuppressLint("MissingPermission")
            public boolean bluetoothSetup() {
                // Get the adapter
                mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                if (mBluetoothAdapter == null) {
                    Log.e("[CHESS-CHEAT]", "No bluetooth adapter available");
                }


                // Check list of all paired for the right one
                Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
                if (pairedDevices.size() > 0) {
                    Log.i("[CHESS-CHEAT]", "Paired devices by name:");
                    for (BluetoothDevice device : pairedDevices) {
                        Log.i("[CHESS-CHEAT]", device.getName());
                        if (device.getName().equals(BLUETOOTH_DEVICE_NAME)) {
                            mmDevice = device;
                            Log.i("[CHESS-CHEAT]", "Bluetooth device name found");
                            break;
                        }
                    }
                    if (mmDevice == null) {
                        Log.e("[CHESS-CHEAT]", "Bluetooth device not found");
                        return false;
                    }
                }

                // No errors
                return true;
            }

            @SuppressLint("MissingPermission")
            public boolean connectBluetooth() {
                // Make connection to the found device (Standard SerialPortService ID)
                UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
                try {
                    mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
                    mmSocket.connect();
                    mmOutputStream = mmSocket.getOutputStream();
                    mmInputStream = mmSocket.getInputStream();

                } catch (Exception e) {
                    Log.e("[CHESS-CHEAT]", "IO Error opening bluetooth stream");
                    return false;
                }

                // No errors
                return true;
            }

        });

        // Start the thread
        mainThread.start();


    }

    @Override
    public void onDestroy() {

        // Release wake lock
        wakeLock.release();

        // Stop the thread
        mainThread.interrupt();
        stopThread = true;

        // End bluetooth connection
        try { mmInputStream.close();  } catch (Exception e) {}
        try { mmOutputStream.close(); } catch (Exception e) {}
        try { mmSocket.close();       } catch (Exception e) {}

        // Reset board for the ui
        try { checkBoard.loadFromFen(STARTING_FEN);            } catch (Exception e) {}
        try { ChessBoardUtil.setFen(STARTING_FEN, chessBoard); } catch (Exception e) {}
        try { MainActivity.boardTableFen = STARTING_FEN;       } catch (Exception e) {}

        // Log
        Log.d("[CHESS-CHEAT]", "Service stopped");

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private String createNotificationChannel(NotificationManager notificationManager){
        String channelId = "ChessCheatChannelId";
        String channelName = "Bluetooth Service";
        NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH);

        channel.setImportance(NotificationManager.IMPORTANCE_NONE);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        notificationManager.createNotificationChannel(channel);
        return channelId;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

class ChessMove {

    int move;
    String moveStr;

    public ChessMove(int move, String moveStr) {
        this.move = move;
        this.moveStr = moveStr;
    }

}
