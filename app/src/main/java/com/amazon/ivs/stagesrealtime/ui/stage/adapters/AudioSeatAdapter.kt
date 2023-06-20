package com.amazon.ivs.stagesrealtime.ui.stage.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.amazon.ivs.stagesrealtime.common.extensions.setVisibleOr
import com.amazon.ivs.stagesrealtime.databinding.ItemAudioSeatBinding
import com.amazon.ivs.stagesrealtime.ui.stage.models.AudioSeatUIModel

private val adapterDiff = object : DiffUtil.ItemCallback<AudioSeatUIModel>() {
    override fun areItemsTheSame(oldItem: AudioSeatUIModel, newItem: AudioSeatUIModel) =
        oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: AudioSeatUIModel, newItem: AudioSeatUIModel) =
        oldItem == newItem
}

class AudioSeatAdapter(
    private val onSeatClicked: (Int) -> Unit
) : ListAdapter<AudioSeatUIModel, AudioSeatAdapter.ViewHolder>(adapterDiff) {
    inner class ViewHolder(val binding: ItemAudioSeatBinding) : RecyclerView.ViewHolder(binding.root)

    private var _isCreator = false

    fun setCreator(isCreator: Boolean) {
        _isCreator = isCreator
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemAudioSeatBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        with(holder.binding) {
            val seat = currentList[position]
            val hasId = seat.participantId.isNotBlank()
            userAvatar.setAvatar(seat.userAvatar)
            speakingIndicator.setVisibleOr(seat.isSpeaking)
            avatarLoading.setVisibleOr(hasId && seat.userAvatar == null)
            muteIcon.setVisibleOr(hasId && seat.isMuted)
            plusIcon.setVisibleOr(!hasId)
            seatButton.setOnClickListener { onSeatClicked(position) }
            seatButton.isEnabled = !_isCreator && seat.id > 0 && !hasId
        }
    }
}
