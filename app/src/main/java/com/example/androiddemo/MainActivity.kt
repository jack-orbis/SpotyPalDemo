package com.example.androiddemo

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.androiddemo.adapter.ListOfDevicesAdapter
import com.example.androiddemo.interfaces.OnClickConnect
import com.spotypal.spotypalconnect.service.BleInterface
import com.spotypal.spotypalconnect.service.BleScanner

import io.reactivex.rxjava3.disposables.Disposable


const val REQUEST_ENABLE_BT = 101
const val REQUEST_LOCATION = 100



class MainActivity : AppCompatActivity(), OnClickConnect {

    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var bluetoothAdapter: BluetoothAdapter

    private lateinit var bleScannerInterface: BleScanner
    private lateinit var bleDeviceConnection: BleInterface
    private lateinit var connectionStatus: TextView

    private lateinit var listOfDevicesAdapter: ListOfDevicesAdapter

    private var isConnected: Boolean = false
    private var listofDevices : ArrayList<String> = ArrayList()
    private var connection: Disposable? = null
    private var scanningDisposable: Disposable? = null
    private var tag: String? = MainActivity::class.simpleName
    private val BLE_PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    private val ANDROID_12_BLE_PERMISSIONS = arrayOf(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    fun requestBlePermissions(activity: Activity?, requestCode: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) ActivityCompat.requestPermissions(
            activity!!,
            ANDROID_12_BLE_PERMISSIONS,
            requestCode
        ) else ActivityCompat.requestPermissions(
            activity!!, BLE_PERMISSIONS, requestCode
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

//        checkLocationPermission()

        bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        bleScannerInterface = BleScanner(this, bluetoothAdapter)

        /* Commenting out the static connection */
        //bleDeviceConnection = BleInterface(this, bluetoothAdapter, "C0:C0:97:E7:A0:1B")

        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }

        /* adding a list that shows scanned devices that are available to connect */
        val listRecyclerView = findViewById<RecyclerView>(R.id.list_recycler_view)

        connectionStatus = findViewById<TextView>(R.id.connection_status_TV)
        val scanButton = findViewById<Button>(R.id.scanButton)

        /* Initializing the variable for the location & bluetooth permission */
        val locationPermissionButton = findViewById<Button>(R.id.locationPermissionButton)
        val bluetoothPermissionButton = findViewById<Button>(R.id.bluetoothConnectPermissionButton)

        /* Location permission button functionality */
        locationPermissionButton.setOnClickListener {
            checkLocationPermission()
        }

        /* Bluetooth permission button functionality */
        bluetoothPermissionButton.setOnClickListener {
            requestBlePermissions(this, 2)
        }

        /* Scan button functionality */
        scanButton.setOnClickListener {
            if (!bleScannerInterface.isScanning()) {
                scanningDisposable = bleScannerInterface.startScanForDevices().subscribe({ macAddress ->

                    /* Initializing the bleDeviceConnection with the mac address available */
                    bleDeviceConnection = BleInterface(this, bluetoothAdapter, macAddress)
                    if(!listofDevices.contains(macAddress)){
                        listofDevices.add(macAddress)
                    }

                    /* Updating the list of SpotyPal Devices Available */
                    listOfDevicesAdapter = ListOfDevicesAdapter(this, listofDevices, onClickConnect = this)
                    listRecyclerView.setAdapter(listOfDevicesAdapter)
                    listRecyclerView.layoutManager = LinearLayoutManager(this)


                    Log.i(tag, "New scanned device: $macAddress")
//                    this.toast("New scanned device: $macAddress")
                }, { error ->
                    Log.e(tag, "An error occurred: ${error.localizedMessage}")
                    this.toast("An error occurred: ${error.localizedMessage}")
                })
            } else {
                if (scanningDisposable != null) {
                    scanningDisposable?.dispose()
                    scanningDisposable = null
                }
            }
        }
    }

    private fun checkLocationPermission(): Boolean {
        return if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                )
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                            this,
                            Manifest.permission.ACCESS_FINE_LOCATION
                    )) {

                AlertDialog.Builder(this)
                        .setTitle(R.string.title_location_permission)
                        .setMessage(R.string.text_location_permission)
                        .setPositiveButton(R.string.confirm) { _, _ -> //Prompt the user once explanation has been shown
                            ActivityCompat.requestPermissions(
                                    this@MainActivity,
                                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                                    REQUEST_LOCATION
                            )
                        }
                        .create()
                        .show()
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        REQUEST_LOCATION
                )
            }
            false
        } else {
            true
        }
    }

    private fun toast(message: String) {
        runOnUiThread { Toast.makeText(this, message, Toast.LENGTH_SHORT).show() }
    }

    /* Callback when click on button(Connect/Disconnect) in the listView */
    override fun onClickConnectOrDisconnect() {
        if(isConnected && connection != null) {
            /* Ring when disconnect */
            bleDeviceConnection.toggleDeviceRing(true)
            Thread.sleep(500)
            bleDeviceConnection.toggleDeviceRing(false)
            connection?.dispose()
            this.toast("New connection state: Disconnected")
            connection = null
            runOnUiThread {
                /* Updating the connectionStatus on UI */
                connectionStatus.setText("Connection Status : Disconnected")
            }
        } else {
            connection = bleDeviceConnection.connect(true).subscribe({ connectionState ->
                Log.i(tag, "Connection state is: $connectionState")
                this.toast("New connection state: $connectionState")
                isConnected = connectionState.equals("connected", true)

                /* Updating the connectionStatus on UI */
                runOnUiThread {
                    if (connectionState.equals("Connected")) {
                        connectionStatus.setText("Connection Status : Connected")
                    }else{
                        connectionStatus.setText("Connection Status : Disconnected")
                    }
                }
            }, { error ->
                Log.e(tag, "Connection error occurred: ${error.localizedMessage}")
                this.toast("Connection error occurred: ${error.localizedMessage}")
            })
        }
    }


}