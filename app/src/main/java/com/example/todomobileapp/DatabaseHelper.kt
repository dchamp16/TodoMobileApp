package com.example.todomobileapp

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log


class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_VERSION = 9
        private const val DATABASE_NAME = "taskManager"
        private const val TABLE_TASKS = "tasks"
        private const val KEY_ID = "id"
        private const val KEY_TITLE = "title"
        private const val KEY_DESCRIPTION = "description"
        private const val KEY_CATEGORY = "category"
        private const val KEY_COMPLETED = "isCompleted"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTasksTable = ("""
            CREATE TABLE $TABLE_TASKS (
                $KEY_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $KEY_TITLE TEXT,
                $KEY_DESCRIPTION TEXT,
                $KEY_CATEGORY TEXT,
                $KEY_COMPLETED INTEGER
            )
            """).trimIndent()
        db.execSQL(createTasksTable)
    }

    fun updateTask(task: Task) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(KEY_TITLE, task.title)
            put(KEY_DESCRIPTION, task.description)
            put(KEY_CATEGORY, task.category) // Assuming category can also be edited if needed
            put(KEY_COMPLETED, if (task.isCompleted) 1 else 0)
        }
        db.update(TABLE_TASKS, values, "$KEY_ID = ?", arrayOf(task.id.toString()))
        db.close()
    }




    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE $TABLE_TASKS ADD COLUMN $KEY_CATEGORY TEXT")
        }

        if (oldVersion < 4) {
            try {
                db.execSQL("ALTER TABLE $TABLE_TASKS ADD COLUMN $KEY_COMPLETED INTEGER DEFAULT 0")
            } catch (e: Exception) {
            }
        }
    }



    fun addTask(task: Task) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(KEY_TITLE, task.title)
            put(KEY_DESCRIPTION, task.description)
            put(KEY_CATEGORY, task.category)
            put(KEY_COMPLETED, if (task.isCompleted) 1 else 0)
        }
        db.insert(TABLE_TASKS, null, values)
        db.close()
    }

    fun deleteTask(taskId: Int) {
        val db = this.writableDatabase
        try {
            db.delete(TABLE_TASKS, "$KEY_ID = ?", arrayOf(taskId.toString()))
        } catch (e: Exception) {

            Log.e("DatabaseHelper", "Error deleting task", e)
        } finally {
            db.close()
        }
    }

    fun getAllTasks(): List<Task> {
        val tasksList = mutableListOf<Task>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_TASKS", null)
        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndex(KEY_ID))
                val title = cursor.getString(cursor.getColumnIndex(KEY_TITLE))
                val description = cursor.getString(cursor.getColumnIndex(KEY_DESCRIPTION))
                val category = cursor.getString(cursor.getColumnIndex(KEY_CATEGORY))
                val isCompleted = cursor.getInt(cursor.getColumnIndex(KEY_COMPLETED)) == 1

                // Determine the icon based on the task's category
                val iconResId = when (category) {
                    "Personal" -> R.drawable.personal
                    "Work" -> R.drawable.work
                    "Home" -> R.drawable.home
                    else -> R.drawable.ic_launcher_background // Default icon if no match
                }

                val task = Task(id, title, description, category, isCompleted, iconResId)
                tasksList.add(task)
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return tasksList
    }



    fun populateInitialData() {
        val initialTasks = listOf(
            Task(0, "Buy groceries", "Milk, Eggs, Bread, Butter", "Personal", false, R.drawable.personal),
            Task(0, "Call John", "Discuss the trip plans", "Work", false, R.drawable.work),
            Task(0, "Read book", "Read 2 chapters of 'Sapiens'", "Home", false, R.drawable.home)
        )

        writableDatabase.use { db ->
            db.beginTransaction()
            try {
                db.delete(TABLE_TASKS, null, null)

                initialTasks.forEach { task ->
                    val values = ContentValues().apply {
                        put(KEY_TITLE, task.title)
                        put(KEY_DESCRIPTION, task.description)
                        put(KEY_CATEGORY, task.category)
                        put(KEY_COMPLETED, if (task.isCompleted) 1 else 0)
                    }
                    db.insert(TABLE_TASKS, null, values)
                }
                db.setTransactionSuccessful()
            } catch (e: Exception) {
                Log.e("DatabaseHelper", "Error populating initial data", e)
            } finally {
                db.endTransaction()
            }
        }
    }


    fun clearAllTasks() {
        val db = writableDatabase
        db.execSQL("DELETE FROM $TABLE_TASKS")
        db.close()
    }

    fun printAllData() {
        val tasks = getAllTasks()
        if (tasks.isEmpty()) {
            Log.d("Database", "No tasks found")
        } else {
            for (task in tasks) {
                Log.d("Database", "Task: ${task.title}, Description: ${task.description}")
            }
        }
    }



    fun doQuery(category: String): List<Task> {
        val tasksList = mutableListOf<Task>()
        val db = this.readableDatabase

        db.query(
            TABLE_TASKS,
            arrayOf(KEY_ID, KEY_TITLE, KEY_DESCRIPTION, KEY_CATEGORY, KEY_COMPLETED),
            "$KEY_CATEGORY = ?",
            arrayOf(category),
            null,
            null,
            null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID))
                val title = cursor.getString(cursor.getColumnIndexOrThrow(KEY_TITLE))
                val description = cursor.getString(cursor.getColumnIndexOrThrow(KEY_DESCRIPTION))
                val retrievedCategory = cursor.getString(cursor.getColumnIndexOrThrow(KEY_CATEGORY))
                val isCompleted = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_COMPLETED)) == 1

                // Determine the icon based on the task's category
                val iconResId = when (retrievedCategory) {
                    "Personal" -> R.drawable.personal
                    "Work" -> R.drawable.work
                    "Home" -> R.drawable.home
                    else -> R.drawable.ic_launcher_background // Default icon if no match
                }

                val task = Task(id, title, description, retrievedCategory, isCompleted, iconResId)
                tasksList.add(task)
            }
        }

        return tasksList
    }





}
