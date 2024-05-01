package com.example.todomobileapp

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.todomobileapp.adapter.TaskAdapter
import com.example.todomobileapp.databinding.ActivityMainBinding
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var weatherService: WeatherService
    private lateinit var databaseHelper: DatabaseHelper

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1
        private const val API_KEY = "41305d93ae2cde7bdf65a0be3193ab4d"
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

    private fun showTaskDetails(task: Task) {
        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.dialog_task_details, null)
        val titleText: TextView = view.findViewById(R.id.tvTaskTitle)
        val descriptionText: TextView = view.findViewById(R.id.tvTaskDescription)

        titleText.text = task.title
        descriptionText.text = task.description

        AlertDialog.Builder(this)
            .setView(view)
            .setPositiveButton("Close") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun setupRetrofit() {
        val loggingInterceptor = HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY)
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
                            val weatherText = "${weather.name}, ${weather.sys?.country ?: "your area"}: ${weather.weather[0].description}. Temperature: ${weather.main.temp}°C."
                            binding.tvWeather.text = weatherText
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
        } ?: Log.e("WeatherApp", "Location is null")
    }

    private fun getLocation(): Location? {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        val locationProvider = when {
            isGpsEnabled -> LocationManager.GPS_PROVIDER
            isNetworkEnabled -> LocationManager.NETWORK_PROVIDER
            else -> null
        }

        return locationProvider?.let {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationManager.getLastKnownLocation(it)
            } else null
        }
    }

    private fun setupRecyclerView() {
        binding.tasksRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.tasksRecyclerView.adapter = TaskAdapter(databaseHelper.getAllTasks().toMutableList(), this::editTask, this::deleteTask, this)
        updateRecyclerView()
    }


    private fun setupSpinners() {
        val categories = arrayOf("Work", "Home", "Personal")
        binding.spinnerCategory.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
            .also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        val queryCategories = arrayOf("Work", "Home", "Personal", "All")
        binding.spinnerQueryCategory.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, queryCategories)
            .also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
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
        val imageResId = when (category) {
            "Personal" -> R.drawable.personal
            "Work" -> R.drawable.work
            "Home" -> R.drawable.home
            else -> R.drawable.ic_launcher_background
        }

        if (title.isNotEmpty() && description.isNotEmpty()) {
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

    private fun deleteTask(task: Task) {
        databaseHelper.deleteTask(task.id)
        updateRecyclerView()
    }

    private fun editTask(task: Task) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_task, null)
        val titleEditText = dialogView.findViewById<EditText>(R.id.editTextTaskTitle)
        val descriptionEditText = dialogView.findViewById<EditText>(R.id.editTextTaskDescription)

        // Set existing values
        titleEditText.setText(task.title)
        descriptionEditText.setText(task.description)

        AlertDialog.Builder(this)
            .setTitle("Edit Task")
            .setView(dialogView)
            .setPositiveButton("Save") { dialog, _ ->
                // Retrieve the updated values from EditText fields
                val updatedTitle = titleEditText.text.toString().trim()
                val updatedDescription = descriptionEditText.text.toString().trim()

                // Update the task object if the title or description has changed
                if (task.title != updatedTitle || task.description != updatedDescription) {
                    task.title = updatedTitle
                    task.description = updatedDescription
                    databaseHelper.updateTask(task)
                    updateRecyclerView()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", { dialog, _ -> dialog.cancel() })
            .create()
            .show()
    }



    private fun updateRecyclerView() {
        binding.tasksRecyclerView.adapter = TaskAdapter(databaseHelper.getAllTasks().toMutableList(), this::editTask, this::deleteTask, this)
    }


}
