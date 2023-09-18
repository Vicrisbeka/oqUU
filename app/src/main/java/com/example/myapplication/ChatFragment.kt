package com.example.myapplication

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.audiofx.DynamicsProcessing.Config
import android.os.Handler
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import java.util.*
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [ChatFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ChatFragment : Fragment() {
    private lateinit var chatAdapter: ChatAdapter
    var progressBar: ProgressBar? = null
    var recyclerView: RecyclerView? = null
    //var editText: EditText? = null
    var startListeningButton: ImageButton? = null
    var isRecording: Boolean = false
    private var param1: String? = null
    private var param2: String? = null

    //Audio vars
    private val outputFile =
        android.os.Environment.getExternalStorageDirectory().absolutePath + "/speech_to_text.mp3"
    private var mediaPlayer: MediaPlayer? = null
    private lateinit var audioRecorder: AudioRecorder
    private val handler = Handler()
    private val maxRecordingTimeMillis = 30_000 // 30 seconds

    //Chat context
    private val recyclerViewMessages = mutableListOf<Message>()
    val messageList: MutableList<Message> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
        audioRecorder = AudioRecorder(outputFile)
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_chat, container, false)
        recyclerView = view?.findViewById(R.id.chatRecyclerView)
        //editText = view?.findViewById(R.id.edit_text)
        startListeningButton = view?.findViewById(R.id.listen_button)
        progressBar = view?.findViewById(R.id.chat_loader)

        chatAdapter = ChatAdapter(recyclerViewMessages)
        recyclerView?.adapter = chatAdapter

        // Set a layout manager (e.g., LinearLayoutManager)
        val layoutManager = LinearLayoutManager(context)
        recyclerView?.layoutManager = layoutManager

        // Notify the adapter that data has changed
        chatAdapter.notifyDataSetChanged()

        setupViews()
        setupConversation()
        return view
    }
    fun setupConversation() {
        val age = Configs.conf[Configuration.AGE]
        val nativeLang = Configs.conf[Configuration.NATIVE_LANG]
        val level = Configs.conf[Configuration.ENGLISH_LEVEL]
        val goal = Configs.conf[Configuration.GOAL]
        val startMessage = "Imagine that you are english teacher and helping me to practice in english.\n" +
                "I'm $age years old.\n" +
                "My native language is $nativeLang. So please explain me in my native lang if something was wrong in my speech.\n" +
                "My english level is at $level.\n" +
                "I'm improving english for $goal.\n" +
                "So, please ask me something to start small talk."
        sendRequestToOpenAI(startMessage, true)
    }

    fun setupViews() {
        // Check and request necessary permissions
        if (
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            val permissions = arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
            ActivityCompat.requestPermissions(requireActivity(), permissions, 0)
        }
        startListeningButton?.setOnClickListener {
            if (checkRecordPermission()) {
                if (!isRecording) {
                    startListeningButton?.background =
                        requireContext().resources.getDrawable(R.drawable.button_on_record)
                    isRecording = true
                    startRecording()
                    handler.postDelayed({
                        stopRecording()
                        showToast("Recording stopped: 30-second limit reached")
                    }, maxRecordingTimeMillis.toLong())
                } else {
                    startListeningButton?.background =
                        requireContext().resources.getDrawable(R.drawable.button_normal)
                    stopRecording()
                }
            }
        }
    }

    fun speak() {
        val apiService = RetrofitClient.speechInstance.create(SpeechService::class.java)
        val textToConvert = messageList.last().content
        val modelId = "eleven_monolingual_v1"
        val optimizeStreamingLatency = 0
        val outputFormat = "mp3_44100_128"
        val voiceSettings = VoiceSettings(0, 0, 0, true)
        val request = TextToSpeechRequest(textToConvert, modelId, voiceSettings)

        val call = RemoteConfigs.conf[RemoteConfiguration.SPEECH_AI]?.let {
            apiService.textToSpeech(
                it,
                request,
                optimizeStreamingLatency,
                outputFormat
            )
        }
        Log.d("SPEECH AI REQUEST", "start")
        call?.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                progressBar?.visibility = View.INVISIBLE
                if (response.isSuccessful) {
                    Log.d("SPEECH AI REQUEST", "success")
                    val responseBody = response.body()
                    if (responseBody != null) {
                        try {
                            // Download the audio content
                            val audioBytes = responseBody.bytes()

                            // Initialize MediaPlayer and play the downloaded audio
                            mediaPlayer = MediaPlayer()
                            val tempFile =
                                File.createTempFile("temp_audio", ".mp3", requireContext().cacheDir)
                            FileOutputStream(tempFile).use { outputStream ->
                                outputStream.write(audioBytes)
                            }
                            mediaPlayer?.setDataSource(tempFile.absolutePath)
                            mediaPlayer?.prepare()
                            mediaPlayer?.start()

                            // Optionally, set an onCompletionListener to release MediaPlayer when playback ends
                            mediaPlayer?.setOnCompletionListener {
                                mediaPlayer?.release()
                                // Delete the temporary file if needed
                                tempFile.delete()
                            }
                        } catch (e: IOException) {
                            Log.d("SPEECH AI REQUEST", "IOException")
                        }
                    }
                } else {
                    Log.d("SPEECH AI REQUEST", "error " + response.toString())
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.d("SPEECH AI REQUEST", "onFailure: " + call.toString())
            }
        })
    }

    fun sendRequestToOpenAI(text: String, initialRequest: Boolean = false) {
        progressBar?.visibility = View.VISIBLE
        messageList.add(Message("user", text))
        if (!initialRequest) {
            sendMessage(text)
        }
        Log.d("OPEN AI REQUEST", "Start")
        val apiService = RetrofitClient.instance.create(OpenAIService::class.java)
        val request = ChatCompletionRequest(
            model = "gpt-3.5-turbo",
            messages = messageList,
            temperature = 0.7
        )
        val call = RemoteConfigs.conf[RemoteConfiguration.OPEN_AI]?.let {
            apiService.postChatCompletion(
                "Bearer $it",
                request
            )
        }
        call?.enqueue(object : Callback<ChatCompletionResponse> {
            override fun onResponse(
                call: Call<ChatCompletionResponse>,
                response: Response<ChatCompletionResponse>
            ) {
                Log.d("OPEN AI REQUEST", response.toString())
                if (response.isSuccessful) {
                    Log.d("OPEN AI REQUEST", "SUCCESS")
                    val chatCompletionResponse = response.body()
                    chatCompletionResponse?.choices?.first()?.let {
                        messageList.add(it.message)
                        receiveMessage(it.message.content)
                        //editText?.setText(it.message.content)
                        speak()
                    }
                } else {
                    Log.d("OPEN AI REQUEST", "ERROR")
                }
            }

            override fun onFailure(call: Call<ChatCompletionResponse>, t: Throwable) {
                Log.d("OPEN AI REQUEST", "ERROR - " + call.toString())
            }
        })
    }

    private fun checkRecordPermission(): Boolean {
        if (
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            val permissions = arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
            ActivityCompat.requestPermissions(requireActivity(), permissions,0)
            return false
        }
        return true
    }

    private fun startRecording() {
        startListeningButton?.background =
            requireContext().resources.getDrawable(R.drawable.button_on_record)
        audioRecorder.startRecording()
    }

    private fun stopRecording() {
        progressBar?.visibility = View.VISIBLE
        startListeningButton?.background =
            requireContext().resources.getDrawable(R.drawable.button_normal)
        audioRecorder.stopRecording()
        handler.removeCallbacksAndMessages(null) // Remove the time limit handler

        // Prepare the audio file for uploading
        val audioFile = File(outputFile)
        val requestFile = RequestBody.create("audio/mp3".toMediaTypeOrNull(), audioFile)
        val audioPart = MultipartBody.Part.createFormData("file", audioFile.name, requestFile)

        // Create the "model" field
        val modelField = RequestBody.create("text/plain".toMediaTypeOrNull(), "whisper-1")

        // Make the API call to upload the audio file
        val apiService = RetrofitClient.instance.create(OpenAIService::class.java)
        val call = RemoteConfigs.conf[RemoteConfiguration.OPEN_AI]?.let {
            apiService.uploadAudio(
                "Bearer $it",
                audioPart,
                modelField
            )
        }
        call?.enqueue(object : Callback<SpeechToTextResponse> {
            override fun onResponse(
                call: Call<SpeechToTextResponse>,
                response: Response<SpeechToTextResponse>
            ) {
                isRecording = false
                if (response.isSuccessful) {
                    response.body()?.text?.let {
                        //editText?.setText(it)
                        sendRequestToOpenAI(it)
                    }
                } else {
                    Log.d("OPEN AI SPEECH TO TEXT", "ERROR")
                }
            }

            override fun onFailure(call: Call<SpeechToTextResponse>, t: Throwable) {
                Log.d("OPEN AI SPEECH TO TEXT", "ERROR - " + call.toString())
            }
        })
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    // Function to add a new message to the chat
    private fun addMessage(message: Message) {
        recyclerViewMessages.add(message)
        chatAdapter.notifyItemInserted(recyclerViewMessages.size - 1) // Notify the adapter of the new message
    }

    // Example of how to add a new message when a message is received (replace this with your actual logic)
    private fun receiveMessage(messageText: String) {
        val newMessage = Message("assistant", messageText)
        addMessage(newMessage)
    }

    // Example of how to send a message (replace this with your actual logic)
    private fun sendMessage(messageText: String) {
        val newMessage = Message("user", messageText)
        addMessage(newMessage)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment ChatFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ChatFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}