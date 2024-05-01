package com.example.todomobileapp

data class Task(
    val id: Int,
    var title: String,
    var description: String,
    val category: String,
    val isCompleted: Boolean,
    val iconResId: Int
)





