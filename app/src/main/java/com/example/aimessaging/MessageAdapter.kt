package com.example.aimessaging

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray
import org.json.JSONObject

class MessageAdapter(private val messageList: MutableList<Messaging>) :
    RecyclerView.Adapter<MessageAdapter.MyViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_message, parent, false)
        return MyViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val message = messageList[position]

        // Depending on who sent the message, show the appropriate chat view
        if (message.sentBy == Messaging.SENT_BY_ME) {
            holder.leftChatView.visibility = View.GONE
            holder.rightChatView.visibility = View.VISIBLE
            holder.rightTextView.text = message.message
        } else {
            holder.rightChatView.visibility = View.GONE
            holder.leftChatView.visibility = View.VISIBLE
            holder.leftTextView.text = message.message
        }
    }

    override fun getItemCount(): Int {
        return messageList.size
    }

    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val leftChatView: LinearLayout = itemView.findViewById(R.id.left_chat_view)
        val rightChatView: LinearLayout = itemView.findViewById(R.id.right_chat_view)
        val leftTextView: TextView = itemView.findViewById(R.id.left_chat_text_view)
        val rightTextView: TextView = itemView.findViewById(R.id.right_chat_text_view)
    }

    fun addMessagesFromJson(jsonData: String) {
        try {
            val jsonObject = JSONObject(jsonData)
            val choicesArray = jsonObject.getJSONArray("choices")

            for (i in 0 until choicesArray.length()) {
                val choiceObject = choicesArray.getJSONObject(i)
                val contentObject = choiceObject.getJSONObject("delta")
                val content = contentObject.getString("content")

                // Add the message to the list
                messageList.add(Messaging(content, Messaging.SENT_BY_BOT))
            }

            notifyDataSetChanged()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
