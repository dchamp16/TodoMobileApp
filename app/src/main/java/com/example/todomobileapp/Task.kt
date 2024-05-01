package com.example.todomobileapp

data class Task(
    val id: Int,
    val title: String,
    var description: String,
    val category: String,
    val isCompleted: Boolean,
    val iconResId: Int
)





