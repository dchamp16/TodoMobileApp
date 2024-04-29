package com.example.todomobileapp

import DatabaseHelper
import android.graphics.Color
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.todomobileapp.adapter.TaskAdapter
import com.example.todomobileapp.databinding.ActivityMainBinding

//remove
import android.widget.PopupWindow
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.ColorDrawable
import android.os.CountDownTimer
import android.widget.ImageView
import android.widget.LinearLayout
import android.view.Gravity
import android.util.Log
import android.view.LayoutInflater
import androidx.core.content.ContextCompat





class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var databaseHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        databaseHelper = DatabaseHelper(this)
        setupRecyclerView()
        setupSpinner()
        setupButtonListeners()
        setupQuerySpinner()
    }


    private fun setupRecyclerView() {
        binding.tasksRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.tasksRecyclerView.adapter = TaskAdapter(mutableListOf()) { task ->
            databaseHelper.deleteTask(task.id)
            updateRecyclerView()
        }
        updateRecyclerView()
    }


    private fun setupSpinner() {
        val categories = arrayOf("Work", "Home", "Personal")
        val spinnerArrayAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCategory.adapter = spinnerArrayAdapter
    }


    private fun setupQuerySpinner() {
        val queryCategories = arrayOf("Work", "Home", "Personal", "All")
        val spinnerArrayAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, queryCategories)
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerQueryCategory.adapter = spinnerArrayAdapter
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

        // New button for showing animation
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
        val tasks = if (selectedCategory == "All") {
            databaseHelper.getAllTasks()
        } else {
            databaseHelper.doQuery(selectedCategory)
        }
        (binding.tasksRecyclerView.adapter as TaskAdapter).updateItems(tasks)
    }

    private fun clearTasks() {
        databaseHelper.clearAllTasks()
        updateRecyclerView()
    }

    private fun updateRecyclerView() {
        // Update the RecyclerView with potentially new icons
        val tasks = databaseHelper.getAllTasks().map { task ->
            task.copy(
                iconResId = when (task.category) {
                    "Personal" -> R.drawable.personal
                    "Work" -> R.drawable.work
                    "Home" -> R.drawable.home
                    else -> R.drawable.ic_launcher_background // Use a default icon if needed
                }
            )
        }
        (binding.tasksRecyclerView.adapter as TaskAdapter).updateItems(tasks)
    }

}
