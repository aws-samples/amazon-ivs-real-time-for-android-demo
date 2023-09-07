package com.amazon.ivs.stagesrealtime.ui.stage.adapters

import android.text.Html
import android.text.Html.FROM_HTML_MODE_LEGACY
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.amazon.ivs.stagesrealtime.databinding.ItemSystemMessageBinding
import com.amazon.ivs.stagesrealtime.databinding.ItemUserMessageBinding
import com.amazon.ivs.stagesrealtime.ui.stage.models.ChatUIMessage

private val adapterDiff = object : DiffUtil.ItemCallback<ChatUIMessage>() {
    override fun areItemsTheSame(oldItem: ChatUIMessage, newItem: ChatUIMessage) =
        oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: ChatUIMessage, newItem: ChatUIMessage) =
        oldItem == newItem
}

class ChatMessageAdapter : ListAdapter<ChatUIMessage, RecyclerView.ViewHolder>(adapterDiff) {
    override fun getItemViewType(position: Int) =
        when (currentList[position]) {
            is ChatUIMessage.UserMessage -> ChatMessageType.USER_MESSAGE.ordinal
            is ChatUIMessage.SystemMessage -> ChatMessageType.SYSTEM_MESSAGE.ordinal
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        when (viewType) {
            ChatMessageType.USER_MESSAGE.ordinal -> UserMessageViewHolder(
                ItemUserMessageBinding.inflate(
                    LayoutInflater.from(
                        parent.context
                    ), parent, false
                )
            )
            else -> SystemMessageViewHolder(
                ItemSystemMessageBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
            )
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val currentMessage = currentList[position]
        when (holder.itemViewType) {
            ChatMessageType.USER_MESSAGE.ordinal -> {
                (holder as UserMessageViewHolder).bind(currentMessage as ChatUIMessage.UserMessage)
            }
            else -> (holder as SystemMessageViewHolder).bind(currentMessage as ChatUIMessage.SystemMessage)
        }
    }

    inner class UserMessageViewHolder(val binding: ItemUserMessageBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(messageItem: ChatUIMessage.UserMessage) {
            with(binding) {
                message.text = messageItem.message
                username.text = messageItem.username
                userAvatar.setAvatar(messageItem.avatar)
            }
        }
    }

    inner class SystemMessageViewHolder(val binding: ItemSystemMessageBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(messageItem: ChatUIMessage.SystemMessage) {
            with(binding) {
                message.text = Html.fromHtml(messageItem.message, FROM_HTML_MODE_LEGACY)
            }
        }
    }

    enum class ChatMessageType {
        USER_MESSAGE, SYSTEM_MESSAGE
    }
}
