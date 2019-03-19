package com.example.oqundojuan.mychat.Adapters

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.example.oqundojuan.mychat.Model.Message
import com.example.oqundojuan.mychat.R
import com.example.oqundojuan.mychat.services.UserDataService

class MessageAdapter(val context: Context, val messages: ArrayList<Message>): RecyclerView.Adapter<MessageAdapter.ViewHolder>(){

    override fun onCreateViewHolder(parent: ViewGroup, p1: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.message_list_view,parent,false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return messages.count()
    }

    override fun onBindViewHolder(holder: ViewHolder , position: Int) {
        holder?.bindMessage(context, messages[position])
    }

    inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        val userImage = itemView?.findViewById<ImageView>(R.id.messageUserImage)
        val timeStamp = itemView?.findViewById<TextView>(R.id.timeStampLbl)
        val userName = itemView?.findViewById<TextView>(R.id.messageUsernameLbl)
        val messageBody = itemView?.findViewById<TextView>(R.id.messageBodyLbl)

        fun bindMessage(context: Context, message: Message){
            val resourceId = context.resources.getIdentifier(message.userAvatar,"drawable",context.packageName)
            userImage?.setImageResource(resourceId)
            userImage?.setBackgroundColor(UserDataService.returnAvatarColor(message.userAvantarColor))
            userName?.text = message.userName
            timeStamp?.text = message.timeStamp
            messageBody?.text = message.message
        }
    }
}