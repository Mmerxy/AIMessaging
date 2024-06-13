package com.example.aimessaging


import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

class MainActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var welcomeTextView: TextView
    private lateinit var messageEditText: EditText
    private lateinit var sendButton: ImageButton
    private lateinit var messageList: MutableList<Messaging>
    private lateinit var messageAdapter: MessageAdapter

    private val client = OkHttpClient()
    private val JSON = "application/json; charset=utf-8".toMediaTypeOrNull()
    private val viewModelScope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        messageList = mutableListOf()

        recyclerView = findViewById(R.id.recycler_view)
        welcomeTextView = findViewById(R.id.welcome_text)
        messageEditText = findViewById(R.id.message_edit_text)
        sendButton = findViewById(R.id.send_btn)

        // Setup recycler view
        messageAdapter = MessageAdapter(messageList)
        recyclerView.adapter = messageAdapter
        val llm = LinearLayoutManager(this)
        llm.stackFromEnd = true
        recyclerView.layoutManager = llm

        sendButton.setOnClickListener {
            val question = messageEditText.text.toString().trim()
            addToChat(question, Messaging.SENT_BY_ME)
            messageEditText.setText("")
            fetchApiResponse(question)
            welcomeTextView.visibility = View.GONE
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun addToChat(message: String, sentBy: String) {
        runOnUiThread {
            messageList.add(Messaging(message, sentBy))
            messageAdapter.notifyDataSetChanged()
            recyclerView.smoothScrollToPosition(messageAdapter.itemCount)
        }
    }

    private fun addResponse(response: JSONObject) {
        messageList.removeAt(messageList.size - 1)
        val content = response.getJSONObject("delta").getString("content")
        addToChat(content, Messaging.SENT_BY_BOT)
    }

    private fun fetchApiResponse(question: String) {
        messageList.add(Messaging("Typing...", Messaging.SENT_BY_BOT))

        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    getApiResponse(question)
                }
                response?.let {
                    Log.d("ApiResponse", "Received response: $it")
                    try {
                        val jsonData = it // Already a JSONObject, no need to parse again
                        val choicesArray = jsonData.getJSONArray("choices")
                        if (choicesArray.length() > 0) {
                            val content = choicesArray.getJSONObject(0)
                                .getJSONObject("delta").getString("content")
                            addResponse(choicesArray.getJSONObject(0))
                        } else {
                            Log.e("ApiResponse", "No choices found in the response")
                            addResponse(JSONObject().apply {
                                put("content", "No choices found in the response")
                            })
                        }
                    } catch (e: JSONException) {
                        e.printStackTrace()
                        Log.e("ApiResponse", "JSONException occurred: ${e.message}")
                        addResponse(JSONObject().apply {
                            put("content", "JSONException occurred: ${e.message}")
                        })
                    }
                } ?: run {
                    Log.e("ApiResponse", "Failed to load response")
                    addResponse(JSONObject().apply {
                        put("content", "Failed to load response")
                    })
                }
            } catch (e: IOException) {
                e.printStackTrace()
                Log.e("ApiResponse", "IOException occurred: ${e.message}")
                addResponse(JSONObject().apply {
                    put("content", "IOException occurred: ${e.message}")
                })
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("ApiResponse", "Unexpected Exception occurred: ${e.message}")
                addResponse(JSONObject().apply {
                    put("content", "Unexpected Exception occurred: ${e.message}")
                })
            }
        }
    }

    private fun getApiResponse(question: String): JSONObject? {
        val url = "http://10.0.0.160:1234/v1/chat/completions"
        val json = """
        {
            "model": "LM Studio Community/Phi-3-mini-4k-instruct-GGUF",
            "messages": [
                { "role": "system", "content": "Always answer in rhymes." },
                { "role": "user", "content": "$question" }
            ],
            "temperature": 0.7,
            "max_tokens": -1,
            "stream": true
        }
        """.trimIndent()

        val requestBody = json.toRequestBody(JSON)
        val request = Request.Builder().url(url).post(requestBody).build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Unexpected code ${response.code}")
                val responseData = response.body?.string()?.trim() ?: return null
                Log.d("ApiResponse", "Response data: $responseData")
                JSONObject(responseData) // Return JSON response directly
            }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
}



