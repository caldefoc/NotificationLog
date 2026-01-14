package com.notificationlog.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.notificationlog.databinding.ItemNotificationBinding
import com.notificationlog.model.NotificationItem
import java.text.SimpleDateFormat
import java.util.*

class NotificationAdapter(
    private val onItemClick: (NotificationItem) -> Unit,
    private val onDeleteClick: (NotificationItem) -> Unit
) : ListAdapter<NotificationItem, NotificationAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemNotificationBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    inner class ViewHolder(private val binding: ItemNotificationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: NotificationItem) {
            binding.notification = item
            
            // Format date
            val dateFormat = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
            binding.textDate.text = dateFormat.format(java.util.Date(item.timestamp))

            binding.root.setOnClickListener {
                onItemClick(item)
            }
            
            binding.btnDelete.setOnClickListener {
                onDeleteClick(item)
            }
            
            binding.executePendingBindings()
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<NotificationItem>() {
        override fun areItemsTheSame(old: NotificationItem, new: NotificationItem) = 
            old.id == new.id

        override fun areContentsTheSame(old: NotificationItem, new: NotificationItem) = 
            old == new
    }
}