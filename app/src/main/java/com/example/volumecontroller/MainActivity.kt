package com.example.volumecontroller

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.example.volumecontroller.databinding.ActivityMainBinding
import com.example.volumecontroller.models.*
import com.tbuonomo.viewpagerdotsindicator.DotsIndicator
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    lateinit var apiService: ApiService
    private lateinit var retrofit: Retrofit
    private lateinit var deviceSpinner: Spinner
    private lateinit var viewPager: ViewPager2
    private lateinit var dotsIndicator: DotsIndicator

    private var deviceList: List<String> = emptyList()
    private var defaultDevice: String = ""
    private var isInitialLoad = true // Prevent unnecessary API calls

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initRetrofit()
        setupSpinner()
        loadDevices() // Fetch list of devices
        loadDefaultDevice() // Fetch the default device

        viewPager = binding.viewPager
        dotsIndicator = binding.dotsIndicator

        loadApplications()

        setSupportActionBar(binding.toolbar)
    }

    private fun initRetrofit() {
        val serverAddress = PreferenceManager.getServerAddress(this)
        retrofit = Retrofit.Builder()
            .baseUrl(serverAddress)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit.create(ApiService::class.java)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                loadApplications()
                true
            }
            R.id.action_change_server -> {
                showChangeServerDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun loadApplications() {
        apiService.getApplications().enqueue(object : Callback<ApplicationsResponse> {
            override fun onResponse(call: Call<ApplicationsResponse>, response: Response<ApplicationsResponse>) {
                if (response.isSuccessful) {
                    val applications = response.body()?.applications ?: emptyList()
                    setupViewPager(applications)
                }
            }

            override fun onFailure(call: Call<ApplicationsResponse>, t: Throwable) {
                Log.e("API_ERROR", "Error fetching applications", t)
            }
        })
    }

    private fun setupSpinner() {
        deviceSpinner = findViewById(R.id.deviceSpinner)
        deviceSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (isInitialLoad) {
                    isInitialLoad = false // Prevents unnecessary API call at first load
                    return
                }
                val selectedDevice = deviceList[position]
                if (selectedDevice != defaultDevice) {
                    switchDefaultDevice(selectedDevice)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun loadDevices() {
        apiService.getDevices().enqueue(object : Callback<DeviceListResponse> {
            override fun onResponse(call: Call<DeviceListResponse>, response: Response<DeviceListResponse>) {
                if (response.isSuccessful) {
                    deviceList = response.body()?.devices ?: emptyList()
                    updateSpinner()
                }
            }

            override fun onFailure(call: Call<DeviceListResponse>, t: Throwable) {
                Log.e("API_ERROR", "Error fetching devices", t)
            }
        })
    }

    private fun loadDefaultDevice() {
        apiService.getDefaultDevice().enqueue(object : Callback<DeviceResponse> {
            override fun onResponse(call: Call<DeviceResponse>, response: Response<DeviceResponse>) {
                if (response.isSuccessful) {
                    defaultDevice = response.body()?.device ?: ""
                    updateSpinner()
                }
            }

            override fun onFailure(call: Call<DeviceResponse>, t: Throwable) {
                Log.e("API_ERROR", "Error fetching default device", t)
            }
        })
    }

    private fun updateSpinner() {
        if (deviceList.isNotEmpty()) {
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, deviceList)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            deviceSpinner.adapter = adapter

            val defaultIndex = deviceList.indexOf(defaultDevice)
            if (defaultIndex >= 0) {
                deviceSpinner.setSelection(defaultIndex)
            }
        }
    }

    private fun switchDefaultDevice(newDevice: String) {
        val deviceRequest = DeviceRequest(newDevice)

        apiService.setDefaultDevice(newDevice).enqueue(object : Callback<DeviceRequest> {
            override fun onResponse(call: Call<DeviceRequest>, response: Response<DeviceRequest>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@MainActivity, "Device switched to $newDevice", Toast.LENGTH_SHORT).show()
                    defaultDevice = newDevice
                    isInitialLoad = true // Prevent re-triggering selection event
                    updateSpinner()
                } else {
                    Toast.makeText(this@MainActivity, "Failed to switch device", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<DeviceRequest>, t: Throwable) {
                Toast.makeText(this@MainActivity, "Error switching device", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupViewPager(applications: List<String>) {
        val appsPerPage = 2 * 5 // 2 rows * 5 columns
        val pages = applications.chunked(appsPerPage)

        val pagerAdapter = object : androidx.viewpager2.adapter.FragmentStateAdapter(this) {
            override fun getItemCount(): Int = pages.size
            override fun createFragment(position: Int): Fragment {
                return PageFragment.newInstance(pages[position])
            }
        }

        viewPager.adapter = pagerAdapter
        dotsIndicator.setViewPager2(viewPager)
    }

    private fun showChangeServerDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_change_server, null)
        val serverAddressEditText = dialogView.findViewById<EditText>(R.id.serverAddressEditText)

        serverAddressEditText.setText(PreferenceManager.getServerAddress(this))

        val dialog = AlertDialog.Builder(this)
            .setTitle("Modify Server Address")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val newAddress = serverAddressEditText.text.toString()
                if (newAddress.isNotEmpty()) {
                    PreferenceManager.setServerAddress(this, newAddress)
                    initRetrofit()
                    loadApplications()
                    loadDevices()
                    loadDefaultDevice()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }
}