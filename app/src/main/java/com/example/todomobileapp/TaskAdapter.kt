package com.example.todomobileapp.adapter

import android.view.LayoutInflater
import android.widget.ImageView
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.todomobileapp.R
import com.example.todomobileapp.Task

class TaskAdapter(private var tasksList: MutableList<Task>, private val onDeleteClick: (Task) -> Unit) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    class TaskViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleTextView: TextView = view.findViewById(R.id.taskTitle)
        val descriptionTextView: TextView = view.findViewById(R.id.taskDescription)
        val categoryTextView: TextView = view.findViewById(R.id.taskCategory)
        val deleteButton: Button = view.findViewById(R.id.deleteButton)
        val categoryImageView: ImageView = view.findViewById(R.id.taskCategoryImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.task_item, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasksList[position]
        holder.titleTextView.text = task.title
        holder.descriptionTextView.text = task.description
        holder.categoryImageView.setImageResource(task.iconResId)
        // Set the actual category of the task
        holder.categoryTextView.text = task.category
        holder.deleteButton.setOnClickListener { onDeleteClick(task) }
    }

    override fun getItemCount() = tasksList.size


    fun updateItems(newTasks: List<Task>) {
        tasksList.clear()
        tasksList.addAll(newTasks)
        notifyDataSetChanged()
    }


}
