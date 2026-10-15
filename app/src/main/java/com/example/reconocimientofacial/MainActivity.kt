package com.example.reconocimientofacial

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import java.io.ByteArrayOutputStream

data class ApiResponse(val nombre: String)

interface ApiService {
    @Multipart
    @POST("predict")
    fun enviarImagen(@Part file: MultipartBody.Part): Call<ApiResponse>
}

class MainActivity : AppCompatActivity() {

    private lateinit var ivFoto: ImageView
    private lateinit var btnTomarFoto: Button
    private lateinit var btnEnviarApi: Button
    private lateinit var tvResultado: TextView
    private var imagenBitmap: Bitmap? = null

    private val pedirPermisoCamara = registerForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        if (concedido) abrirCamara() else Toast.makeText(this, "Permiso denegado", Toast.LENGTH_SHORT).show()
    }

    private val capturarFoto = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { resultado ->
        if (resultado.resultCode == RESULT_OK) {
            val fotoOriginal = resultado.data?.extras?.get("data") as? Bitmap
            if (fotoOriginal != null) {
                imagenBitmap = Bitmap.createScaledBitmap(fotoOriginal, 128, 128, true)
                ivFoto.setImageBitmap(imagenBitmap)
                tvResultado.text = "¡Foto lista para consultar!"
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ivFoto = findViewById(R.id.ivFoto)
        btnTomarFoto = findViewById(R.id.btnTomarFoto)
        btnEnviarApi = findViewById(R.id.btnEnviarApi)
        tvResultado = findViewById(R.id.tvResultado)

        btnTomarFoto.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                abrirCamara()
            } else {
                pedirPermisoCamara.launch(Manifest.permission.CAMERA)
            }
        }

        btnEnviarApi.setOnClickListener {
            if (imagenBitmap != null) enviarImagenALaApi(imagenBitmap!!)
            else Toast.makeText(this, "Primero toma una foto", Toast.LENGTH_SHORT).show()
        }
    }

    private fun abrirCamara() {
        val intentoCamara = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        try { capturarFoto.launch(intentoCamara) } catch (e: Exception) { Toast.makeText(this, "Error cámara", Toast.LENGTH_SHORT).show() }
    }

    private fun enviarImagenALaApi(bitmap: Bitmap) {
        tvResultado.text = "Analizando rostro..."

        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        val mediaType = okhttp3.MediaType.parse("image/jpeg")
        val requestFile = okhttp3.RequestBody.create(mediaType, stream.toByteArray())
        val body = MultipartBody.Part.createFormData("file", "rostro.jpg", requestFile)

        // ¡AQUÍ PEGAS LA URL QUE TE DA GOOGLE COLAB!
        val urlNgrok = "https://frying-unfilled-scowling.ngrok-free.dev/"

        val retrofit = Retrofit.Builder()
            .baseUrl(urlNgrok)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(ApiService::class.java)

        apiService.enviarImagen(body).enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    tvResultado.text = "Resultado: ${response.body()!!.nombre}"
                } else tvResultado.text = "Error en servidor API."
            }
            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                tvResultado.text = "Error de red: ${t.message}"
            }
        })
    }
}