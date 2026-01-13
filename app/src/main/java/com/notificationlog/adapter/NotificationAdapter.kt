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
    private val onItemClick: (NotificationItem) -> Unit
) : ListAdapter<NotificationItem, NotificationAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemNotificationBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemNotificationBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: NotificationItem) {
            binding.apply {
                textAppName.text = item.appName
                textTitle.text = item.title
                textContent.text = item.content
                textTimestamp.text = formatTimestamp(item.timestamp)
                
                root.setOnClickListener { onItemClick(item) }
            }
        }

        private fun formatTimestamp(timestamp: Long): String {
            val sdf = SimpleDateFormat("MM/dd/yyyy HH:mm:ss", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<NotificationItem>() {
        override fun areItemsTheSame(old: NotificationItem, new: NotificationItem) = 
            old.id == new.id

        override fun areContentsTheSame(old: NotificationItem, new: NotificationItem) = 
            old == new
    }
}