package com.example.myapplication

import com.google.gson.annotations.SerializedName
import okhttp3.*
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import retrofit2.http.Headers

class AuthInterceptor(private val token: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            //.addHeader("Authorization", "Bearer $token")
            .build()
        return chain.proceed(request)
    }
}

class SpeechAuthInterceptor(private val token: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            //.addHeader("xi-api-key", token)
            .build()
        return chain.proceed(request)
    }
}

interface OpenAIService {
    @POST("chat/completions")
    fun postChatCompletion(
        @Header("Authorization") authorizationHeader: String,
        @Body request: ChatCompletionRequest
    ): Call<ChatCompletionResponse>

    @Multipart
    @POST("audio/transcriptions")
    fun uploadAudio(
        @Header("Authorization") authorizationHeader: String,
        @Part audio: MultipartBody.Part,
        @Part("model") model: RequestBody
    ): Call<SpeechToTextResponse>
}

interface SpeechService {
    //@POST("21m00Tcm4TlvDq8ikWAM?optimize_streaming_latency=0&output_format=mp3_44100_128")
    @POST("21m00Tcm4TlvDq8ikWAM")
    @Headers("Content-Type: application/json")
    @Streaming
    fun textToSpeech(
        @Header("xi-api-key") authorizationHeader: String,
        @Body request: TextToSpeechRequest,
        @Query("optimize_streaming_latency") optimizeStreamingLatency: Int,
        @Query("output_format") outputFormat: String
    ): Call<ResponseBody>
}

object RetrofitClient {
    private const val BASE_URL = "https://api.openai.com/v1/"
    private const val BASE_URL_SPEECH = "https://api.elevenlabs.io/v1/text-to-speech/"

    val okHttpClient = OkHttpClient.Builder()
        .build()
    val okHttpClientSpeech = OkHttpClient.Builder()
        .build()

    val instance: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val speechInstance: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL_SPEECH)
            .client(okHttpClientSpeech)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}

data class TextToSpeechRequest(
    val text: String,
    val model_id: String,
    val voice_settings: VoiceSettings
)

data class VoiceSettings(
    val stability: Int,
    val similarity_boost: Int,
    val style: Int,
    val use_speaker_boost: Boolean
)

data class SpeechToTextResponse(
    val text: String
)

data class ChatCompletionResponse(
    val id: String,
    val `object`: String,
    val created: Long,
    val model: String,
    val choices: List<Choice>,
    val usage: Usage
)

data class Choice(
    val index: Int,
    val message: Message,
    @SerializedName("finish_reason")
    val finishReason: String
)

data class Message(
    val role: String,
    val content: String
)

data class Usage(
    @SerializedName("prompt_tokens")
    val promptTokens: Int,
    @SerializedName("completion_tokens")
    val completionTokens: Int,
    @SerializedName("total_tokens")
    val totalTokens: Int
)

data class ChatCompletionRequest(
    val model: String,
    val messages: List<Message>,
    val temperature: Double
)