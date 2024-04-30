package com.example.todomobileapp

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.todomobileapp.adapter.TaskAdapter
import com.example.todomobileapp.databinding.ActivityMainBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import android.location.Location



class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var weatherService: WeatherService
    private lateinit var databaseHelper: DatabaseHelper

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1
        private const val API_KEY = "41305d93ae2cde7bdf65a0be3193ab4d"  // Replace with your actual API Key
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        databaseHelper = DatabaseHelper(this)

        setupRetrofit()
        setupRecyclerView()
        setupSpinners()
        setupButtonListeners()
        checkLocationPermissionAndDisplayWeather()
    }

    private fun setupRetrofit() {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val httpClient = OkHttpClient.Builder().addInterceptor(loggingInterceptor).build()
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.openweathermap.org/data/2.5/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(httpClient)
            .build()
        weatherService = retrofit.create(WeatherService::class.java)
    }

    private fun checkLocationPermissionAndDisplayWeather() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), PERMISSION_REQUEST_CODE)
        } else {
            displayWeather()
        }
    }

    private fun displayWeather() {
        val location = getLocation()
        location?.let { loc ->
            Log.d("WeatherApp", "Location found: ${loc.latitude}, ${loc.longitude}")
            weatherService.getWeather(loc.latitude, loc.longitude, API_KEY).enqueue(object : Callback<WeatherResponse> {
                override fun onResponse(call: Call<WeatherResponse>, response: Response<WeatherResponse>) {
                    if (response.isSuccessful) {
                        response.body()?.let { weather ->
                            // Updated text to include location coordinates
                            val weatherText = "It's currently ${weather.main.temp}°C with ${weather.weather[0].description} at " +
                                    "\nlatitude: ${loc.latitude}\n longitude: ${loc.longitude}."
                            binding.tvWeather.text = weatherText
                            Log.d("WeatherApp", "Weather displayed successfully")
                        }
                    } else {
                        Log.e("WeatherApp", "Failed to retrieve weather. Response code: ${response.code()} - ${response.errorBody()?.string()}")
                        Toast.makeText(applicationContext, "Error retrieving weather data: ${response.code()}", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                    Log.e("WeatherApp", "Failed to retrieve weather data", t)
                    Toast.makeText(applicationContext, "Failed to retrieve weather data", Toast.LENGTH_LONG).show()
                }
            })
        } ?: run {
            Log.e("WeatherApp", "Location is null")
            Toast.makeText(applicationContext, "Location not found", Toast.LENGTH_LONG).show()
        }
    }


    private fun getLocation(): Location? {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        val locationProvider = when {
            isGpsEnabled -> LocationManager.GPS_PROVIDER
            isNetworkEnabled -> LocationManager.NETWORK_PROVIDER
            else -> {
                Log.e("WeatherApp", "No location providers available.")
                return null
            }
        }

        return if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.getLastKnownLocation(locationProvider)?.also {
                Log.d("WeatherApp", "Last known location: Lat ${it.latitude}, Lon ${it.longitude}")
            }
        } else {
            Log.e("MainActivity", "Location permission not granted.")
            null
        }
    }


    private fun setupRecyclerView() {
        binding.tasksRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.tasksRecyclerView.adapter = TaskAdapter(mutableListOf()) { task ->
            databaseHelper.deleteTask(task.id)
            updateRecyclerView()
        }
        updateRecyclerView()
    }

    private fun setupSpinners() {
        val categories = arrayOf("Work", "Home", "Personal")
        binding.spinnerCategory.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        val queryCategories = arrayOf("Work", "Home", "Personal", "All")
        binding.spinnerQueryCategory.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, queryCategories).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
    }

    private fun setupButtonListeners() {
        binding.btnPopulate.setOnClickListener {
            databaseHelper.populateInitialData()
            updateRecyclerView()
        }
        binding.btnPrint.setOnClickListener { databaseHelper.printAllData() }
        binding.btnAdd.setOnClickListener { addNewTaskFromInput() }
        binding.btnSort.setOnClickListener { performQuery() }
        binding.btnClear.setOnClickListener { clearTasks() }
    }

    private fun addNewTaskFromInput() {
        val title = binding.etTitle.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()
        val category = binding.spinnerCategory.selectedItem.toString()

        // Assign an icon based on the category - Remove this block if not using icons
        val imageResId = when (category) {
            "Personal" -> R.drawable.personal
            "Work" -> R.drawable.work
            "Home" -> R.drawable.home
            else -> R.drawable.ic_launcher_background // Use a default icon if needed
        }

        if (title.isNotEmpty() && description.isNotEmpty()) {
            // Add the imageResId to the Task constructor
            val newTask = Task(0, title, description, category, false, imageResId)
            databaseHelper.addTask(newTask)
            updateRecyclerView()
        } else {
            Toast.makeText(this, "Title and description cannot be empty.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun performQuery() {
        val selectedCategory = binding.spinnerQueryCategory.selectedItem.toString()
        val tasks = if (selectedCategory == "All") databaseHelper.getAllTasks() else databaseHelper.doQuery(selectedCategory)
        (binding.tasksRecyclerView.adapter as TaskAdapter).updateItems(tasks)
    }

    private fun clearTasks() {
        databaseHelper.clearAllTasks()
        updateRecyclerView()
    }

    private fun updateRecyclerView() {
        val tasks = databaseHelper.getAllTasks()
        (binding.tasksRecyclerView.adapter as TaskAdapter).updateItems(tasks)
    }
}
