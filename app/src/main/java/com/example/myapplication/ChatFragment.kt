package com.example.myapplication

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import retrofit2.Call
import java.util.*
import retrofit2.Callback
import retrofit2.Response

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
    var tts: TextToSpeech? = null
    var speechRecognizer: SpeechRecognizer? = null
    var editText: EditText? = null
    var startListeningButton: ImageButton? = null
    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onDestroy() {
        tts?.let {
            it.stop()
            it.shutdown()
        }
        speechRecognizer?.let {
            it.destroy()
        }

        super.onDestroy()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_chat, container, false)
        editText = view?.findViewById(R.id.edit_text)
        startListeningButton = view?.findViewById(R.id.listen_button)
        setupViews()
        return view
    }

    fun setupViews() {
        tts = TextToSpeech(requireContext()) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.let {
                    val result: Int = it.setLanguage(Locale.US)
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        // Handle language not supported error
                    } else {
                        // TTS is initialized and ready to use
                    }
                }
            } else {
                // Handle TTS initialization error
            }
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(requireContext())
        speechRecognizer?.let { recongnizer ->
            recongnizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle) {
                    // Called when the recognizer is ready to listen
                }

                override fun onBeginningOfSpeech() {
                    // Called when the user starts speaking
                }

                override fun onRmsChanged(p0: Float) {
                    // Check the RMS value to detect silence or inactivity
                    val YOUR_SILENCE_THRESHOLD = -10.0f
                    if (p0 < YOUR_SILENCE_THRESHOLD) {
                        // Silence or inactivity detected, stop listening
                        speechRecognizer?.stopListening()
                    }
                }

                override fun onBufferReceived(p0: ByteArray?) {}

                override fun onEndOfSpeech() {
                    // Called when the user stops speaking
                }

                override fun onResults(results: Bundle) {
                    // Called when speech recognition is successful
                    val matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (matches != null && !matches.isEmpty()) {
                        val recognizedText = matches[0] // Get the recognized text
                        // Handle the recognized text
                        editText?.setText(recognizedText)
                        startListeningButton?.background =
                            requireContext().resources.getDrawable(R.drawable.button_normal)
                        sendRequestToOpenAI(recognizedText)
                    }
                }

                override fun onPartialResults(p0: Bundle?) {}

                override fun onEvent(p0: Int, p1: Bundle?) {}

                override fun onError(error: Int) {
                    // Called when an error occurs during speech recognition
                } // Other speech recognition callback methods...
            })

            // Check and request necessary permissions
            val permission = Manifest.permission.RECORD_AUDIO
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(requireActivity(), arrayOf(permission), 1)
            }
            startListeningButton?.setOnClickListener {
                startListeningButton?.background = requireContext().resources.getDrawable(R.drawable.button_on_record)
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US)
                intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak something...")
                speechRecognizer?.startListening(intent)

            }
        }
    }

    fun speak() {
        val text = editText?.text.toString()
        tts?.let { tts ->
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    fun sendRequestToOpenAI(text: String) {
        Log.d("OPEN AI REQUEST", "Start")
        val apiService = RetrofitClient.instance.create(MyApiService::class.java)
        val request = ChatCompletionRequest(
            model = "gpt-3.5-turbo",
            messages = listOf(Message("user", text)),
            temperature = 0.7
        )
        val call = apiService.postChatCompletion(request)
        call.enqueue(object : Callback<ChatCompletionResponse> {
            override fun onResponse(
                call: Call<ChatCompletionResponse>,
                response: Response<ChatCompletionResponse>
            ) {
                Log.d("OPEN AI REQUEST", response.toString())
                if (response.isSuccessful) {
                    Log.d("OPEN AI REQUEST", "SUCCESS")
                    val chatCompletionResponse = response.body()
                    chatCompletionResponse?.choices?.first()?.let {
                        editText?.setText(it.message.content)
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