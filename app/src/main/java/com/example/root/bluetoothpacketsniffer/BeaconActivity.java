package com.example.root.bluetoothpacketsniffer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.RemoteException;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.opencsv.CSVWriter;

import org.altbeacon.beacon.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


public class BeaconActivity extends Activity implements BeaconConsumer {

    public static final String TAG = "BeaconsEverywhere";
    private BeaconManager beaconManager;
    public int clickCount = 0;
    public long baseTime=0,timeStamp=0;
    public List<String[]> data = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_beacon);
        verifyBluetooth();

        Button startButton = (Button) findViewById(R.id.startButton);
        Button stopButton = (Button) findViewById(R.id.stopButton);
        Button markButton = (Button) findViewById(R.id.markButton);
        Button exportButton = (Button) findViewById(R.id.exportButton);
        Button clearButton = (Button) findViewById(R.id.clearButton);

        beaconManager = BeaconManager.getInstanceForApplication(this);
        beaconManager.getBeaconParsers().add(new BeaconParser().
                setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));
        beaconManager.setBackgroundScanPeriod(1000);
        beaconManager.setBackgroundBetweenScanPeriod(1);

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                binding();
            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                unbinding();
            }
        });

        markButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                marking();
            }
        });

        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearing();
            }
        });

        exportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try
                {
                    EditText filename = (EditText) findViewById(R.id.filename);
                    exportData(filename.getText().toString());
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        });
    }

    public void binding(){
        if(!beaconManager.isBound(this)) {
            baseTime = System.currentTimeMillis();
            raiseAtoast(R.string.startToast);
            data.add(new String[]{"Timestamp", "MAC Address", "RSSI (in dBm)", "Mark"});
            beaconManager.bind(this);
        }
        else {
           raiseAtoast(R.string.startBeforeStop);
        }
    }

    public void unbinding(){
        if(beaconManager.isBound(this)) {
           raiseAtoast(R.string.stopToast);
            clickCount=0;
            beaconManager.unbind(this);
        }
        else{
           raiseAtoast(R.string.stopBeforeStart);
        }
    }

    public void marking(){
        if(beaconManager.isBound(this)) {
            raiseAtoast(R.string.markToast);
            clickCount += 1;
        }
        else{
            raiseAtoast(R.string.markingError);
        }
    }

    public void clearing(){
        TextView editText = (TextView) BeaconActivity.this.findViewById(R.id.rangingText);
        editText.setText("");
        data = new ArrayList<>();
        raiseAtoast(R.string.clearToast);
        if(beaconManager.isBound(this)) {
            data.add(new String[]{"Timestamp", "MAC Address", "RSSI (in dBm)", "Mark"});
        }
    }

    public void exportData(String filename) throws IOException{
        TextView editText = (TextView) BeaconActivity.this.findViewById(R.id.rangingText);
        if(editText.getText().toString().equals("")){
            raiseAtoast(R.string.noData);
        }
        else if(beaconManager.isBound(this)) {
            raiseAtoast(R.string.DataCollectionInterrupt);
        }
        else if(filename.equals("")){
            raiseAtoast(R.string.noFilename);
        }
        else{
            raiseAtoast(R.string.exportToast);

            String dirString = android.os.Environment.getExternalStorageDirectory().getAbsolutePath() + "/BData/";
            File directory = new File(android.os.Environment.getExternalStorageDirectory(),dirString);

            if(!directory.exists()) {
                if(!directory.mkdirs()){
                    raiseAtoast(R.string.noDirectory);
                }
            }

            File file = new File(dirString + filename + ".csv");
            if (!file.exists()) {
                FileWriter fwriter = new FileWriter(file, false);
                CSVWriter writer = new CSVWriter(fwriter);
                writer.writeAll(data);
                writer.close();
                data = new ArrayList<>();
                editText.setText("");
                EditText filen = (EditText) findViewById(R.id.filename);
                filen.setText("");
                raiseAtoast(R.string.success);
            }
            else{
                raiseAtoast(R.string.existingFile);
            }
        }
    }
    protected void onDestroy() {
        super.onDestroy();
        beaconManager.unbind(this);
    }

    @Override
    public void onBeaconServiceConnect() {
        beaconManager.setBackgroundScanPeriod(1000);
        beaconManager.setBackgroundBetweenScanPeriod(1);
        beaconManager.addRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                if (beacons.size() > 0) {
                    timeStamp = System.currentTimeMillis() - baseTime;
                    int s = beacons.toArray().length;
                    Beacon[] beaconArr = beacons.toArray(new Beacon[s]);
                    for(int i=0;i<s;i++) {
                        Log.i(TAG, "Time:"+timeStamp+" Address: " + beaconArr[i].getBluetoothAddress()
                                + " RSSI: " + beaconArr[i].getRssi() + " Marker: " + clickCount);
                        logToDisplay(timeStamp
                                + "         -           " +  beaconArr[i].getBluetoothAddress()
                                + "         -           " + beaconArr[i].getRssi()
                                + "         -           " + clickCount);
                        data.add(new String[]{Long.toString(timeStamp),beaconArr[i].getBluetoothAddress(),
                                Integer.toString(beaconArr[i].getRssi()), Integer.toString(clickCount)});
                    }
                }
            }
        });

        try {
            beaconManager.startRangingBeaconsInRegion(new Region("myRangingUniqueId", null, null, null));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void verifyBluetooth() {
        try {
            if (!BeaconManager.getInstanceForApplication(this).checkAvailability()) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(getApplicationContext().getString(R.string.noBluetooth));
                builder.setMessage(getApplicationContext().getString(R.string.noBluetoothMessage));
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        finish();
                        System.exit(0);
                    }
                });
                builder.show();
            }
        }
        catch (RuntimeException e) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(getApplicationContext().getString(R.string.noBLE));
            builder.setMessage(getApplicationContext().getString(R.string.noBLEMessage));
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                @Override
                public void onDismiss(DialogInterface dialog) {
                    finish();
                    System.exit(0);
                }

            });
            builder.show();
        }
    }

    private void logToDisplay(final String line) {
        runOnUiThread(new Runnable() {
            public void run() {
                TextView editText = (TextView) BeaconActivity.this.findViewById(R.id.rangingText);
                ScrollingMovementMethod scroll = new ScrollingMovementMethod();
                editText.setMovementMethod(scroll);
                editText.append(line + "\n");
            }
        });
    }

    private void raiseAtoast(int resID){
        Toast.makeText(getApplicationContext(), getApplicationContext().getString(resID),
                Toast.LENGTH_SHORT).show();
    }

}
