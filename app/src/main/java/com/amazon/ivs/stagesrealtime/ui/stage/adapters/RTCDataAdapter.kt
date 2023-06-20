package com.amazon.ivs.stagesrealtime.ui.stage.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.amazon.ivs.stagesrealtime.R
import com.amazon.ivs.stagesrealtime.common.extensions.setVisibleOr
import com.amazon.ivs.stagesrealtime.databinding.ItemStreamDataBinding
import com.amazon.ivs.stagesrealtime.ui.stage.models.RTCDataUIItemModel

private val adapterDiff = object : DiffUtil.ItemCallback<RTCDataUIItemModel>() {
    override fun areItemsTheSame(oldItem: RTCDataUIItemModel, newItem: RTCDataUIItemModel) =
        oldItem.participantId == newItem.participantId

    override fun areContentsTheSame(oldItem: RTCDataUIItemModel, newItem: RTCDataUIItemModel) =
        oldItem == newItem
}

class RTCDataAdapter : ListAdapter<RTCDataUIItemModel, RTCDataAdapter.ViewHolder>(adapterDiff) {

    inner class ViewHolder(val binding: ItemStreamDataBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemStreamDataBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        with(holder.binding) {
            val data = currentList[position]
            val usernameText =
                if (data.isHost) root.context.getString(R.string.host_details)
                else if (data.isGuest) root.context.getString(R.string.guest_details)
                else root.context.getString(R.string.details_template, data.username)

            username.text = usernameText
            latency.dataValue = data.latency?.let { root.context.getString(R.string.ms_template, it) } ?: "-"
            fps.dataValue = data.fps ?: "-"
            fps.root.setVisibleOr(!data.isForAudio)
            packetLoss.dataValue = data.packetsLost?.let {
                String.format(root.context.getString(R.string.percentage_template), it)
            } ?: "-"
        }
    }
}
