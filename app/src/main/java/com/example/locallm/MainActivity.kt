package com.example.locallm

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.gson.Gson
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var tvTranscript: TextView
    private lateinit var tvResponse: TextView
    private lateinit var svTranscript: ScrollView
    private lateinit var svResponse: ScrollView
    private lateinit var btnGenerate: Button
    private lateinit var swLanguage: SwitchCompat

    private var speechRecognizer: SpeechRecognizer? = null
    
    // Set timeouts to 60 seconds for slow local LLMs
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
        
    private val gson = Gson()
    
    private val lmStudioUrl = AppConfig.LM_STUDIO_URL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        tvStatus = findViewById(R.id.tvStatus)
        tvTranscript = findViewById(R.id.tvTranscript)
        tvResponse = findViewById(R.id.tvResponse)
        svTranscript = findViewById(R.id.svTranscript)
        svResponse = findViewById(R.id.svResponse)
        btnGenerate = findViewById(R.id.btnGenerate)
        swLanguage = findViewById(R.id.swLanguage)

        if (checkPermissions()) {
            initSpeechRecognizer()
        }

        swLanguage.setOnCheckedChangeListener { _, _ ->
            restartListening()
        }

        btnGenerate.setOnClickListener {
            val contextText = tvTranscript.text.toString()
            if (contextText.isNotBlank()) {
                generateAIAnswer(contextText)
            } else {
                Toast.makeText(this, "Transcript is empty!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkPermissions(): Boolean {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 100)
            return false
        }
        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initSpeechRecognizer()
        }
    }

    private fun initSpeechRecognizer() {
        if (speechRecognizer != null) {
            speechRecognizer?.destroy()
        }
        
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                val currentLang = if (swLanguage.isChecked) "EN" else "RU"
                tvStatus.text = "Status: Listening ($currentLang)..."
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val text = matches[0]
                    tvTranscript.append("\n— $text")
                    scrollToBottom(svTranscript)
                }
                startListening() 
            }

            override fun onError(error: Int) {
                tvStatus.postDelayed({ startListening() }, 500)
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
            override fun onRmsChanged(rmsdB: Float) {}
        })

        startListening()
    }

    private fun startListening() {
        val langTag = if (swLanguage.isChecked) "en-US" else "ru-RU"
        
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, langTag)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, langTag)
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, true)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
            if (!swLanguage.isChecked) {
                putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("ru-RU"))
            }
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        
        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            tvStatus.text = "Start error: ${e.message}"
        }
    }

    private fun restartListening() {
        speechRecognizer?.cancel()
        startListening()
    }

    private fun generateAIAnswer(context: String) {
        tvResponse.text = "Thinking..."
        btnGenerate.isEnabled = false

        val systemPrompt = if (swLanguage.isChecked) {
            AppConfig.SYSTEM_PROMPT_EN
        } else {
            AppConfig.SYSTEM_PROMPT_RU
        }

        val requestBody = mapOf(
            "messages" to listOf(
                mapOf("role" to "system", "content" to systemPrompt),
                mapOf("role" to "user", "content" to "Context:\n$context")
            ),
            "temperature" to 0.7
        )

        val body = gson.toJson(requestBody).toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(lmStudioUrl).post(body).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    tvResponse.text = "Error: ${e.message}"
                    btnGenerate.isEnabled = true
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()
                runOnUiThread {
                    btnGenerate.isEnabled = true
                    if (response.isSuccessful && responseData != null) {
                        try {
                            val apiResponse = gson.fromJson(responseData, LMStudioResponse::class.java)
                            tvResponse.text = apiResponse.choices[0].message.content
                            scrollToBottom(svResponse)
                        } catch (e: Exception) {
                            tvResponse.text = "Response parsing error."
                        }
                    } else {
                        tvResponse.text = "Server error: ${response.code}"
                    }
                }
            }
        })
    }

    private fun scrollToBottom(scrollView: ScrollView) {
        scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
    }
}

data class LMStudioResponse(val choices: List<Choice>)
data class Choice(val message: Message)
data class Message(val content: String)
