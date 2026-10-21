package com.example.reconocimientofacial

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import androidx.exifinterface.media.ExifInterface
import android.graphics.Matrix
import com.google.gson.annotations.SerializedName
import okhttp3.MediaType.Companion.toMediaTypeOrNull

// Estructura de respuesta de la API adaptada EXACTAMENTE a tu backend Flask
data class ApiResponse(
    @SerializedName("reconocido") val reconocido: Boolean? = null,
    @SerializedName("persona") val persona: String? = null,
    @SerializedName("confianza") val confianza: Double? = null,
    @SerializedName("mensaje") val mensaje: String? = null
) {
    fun obtenerResumen(): String {
        return mensaje ?: "$persona (${(confianza ?: 0.0) * 100}% de confianza)"
    }
}

interface ApiService {
    @Multipart
    @POST("predict") // Coincide exactamente con el endpoint expuesto en Flask
    fun enviarImagen(@Part image: MultipartBody.Part): Call<ApiResponse>
}

class MainActivity : AppCompatActivity() {

    private lateinit var ivFoto: ImageView
    private lateinit var btnTomarFoto: Button
    private lateinit var btnAbrirGaleria: Button
    private lateinit var btnEnviarApi: Button
    private lateinit var tvResultado: TextView

    private var imagenParaApi: Bitmap? = null

    private val pedirPermisoCamara = registerForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        if (concedido) abrirCamara() else Toast.makeText(this, "Permiso denegado", Toast.LENGTH_SHORT).show()
    }

    private val capturarFoto = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { resultado ->
        if (resultado.resultCode == RESULT_OK) {
            val fotoOriginal = resultado.data?.extras?.get("data") as? Bitmap
            if (fotoOriginal != null) procesarImagen(fotoOriginal)
        }
    }

    private val abrirGaleria = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                var fotoOriginal = BitmapFactory.decodeStream(inputStream)

                val inputParaExif = contentResolver.openInputStream(uri)
                if (inputParaExif != null && fotoOriginal != null) {
                    val exif = ExifInterface(inputParaExif)
                    val orientacion = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

                    val matriz = Matrix()
                    when (orientacion) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> matriz.postRotate(90f)
                        ExifInterface.ORIENTATION_ROTATE_180 -> matriz.postRotate(180f)
                        ExifInterface.ORIENTATION_ROTATE_270 -> matriz.postRotate(270f)
                    }
                    fotoOriginal = Bitmap.createBitmap(fotoOriginal, 0, 0, fotoOriginal.width, fotoOriginal.height, matriz, true)
                }

                if (fotoOriginal != null) procesarImagen(fotoOriginal)
            } catch (e: Exception) {
                Toast.makeText(this, "Error al cargar la imagen", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ivFoto = findViewById(R.id.ivFoto)
        btnTomarFoto = findViewById(R.id.btnTomarFoto)
        btnAbrirGaleria = findViewById(R.id.btnAbrirGaleria)
        btnEnviarApi = findViewById(R.id.btnEnviarApi)
        tvResultado = findViewById(R.id.tvResultado)

        btnTomarFoto.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                abrirCamara()
            } else {
                pedirPermisoCamara.launch(Manifest.permission.CAMERA)
            }
        }

        btnAbrirGaleria.setOnClickListener {
            abrirGaleria.launch("image/*")
        }

        btnEnviarApi.setOnClickListener {
            if (imagenParaApi != null) enviarImagenALaApi(imagenParaApi!!)
            else Toast.makeText(this, "Primero toma o elige una foto", Toast.LENGTH_SHORT).show()
        }
    }

    private fun procesarImagen(fotoOriginal: Bitmap) {
        imagenParaApi = recortarCuadradoYEscalar(fotoOriginal, 128)
        ivFoto.setImageBitmap(imagenParaApi)
        tvResultado.text = "¡Foto lista para consultar!"
    }

    private fun recortarCuadradoYEscalar(bitmapOriginal: Bitmap, tamanoDeseado: Int): Bitmap {
        val ancho = bitmapOriginal.width
        val alto = bitmapOriginal.height

        val tamanoCuadrado = Math.min(ancho, alto)
        val x = (ancho - tamanoCuadrado) / 2
        val y = (alto - tamanoCuadrado) / 2

        val bitmapCuadrado = Bitmap.createBitmap(bitmapOriginal, x, y, tamanoCuadrado, tamanoCuadrado)
        return Bitmap.createScaledBitmap(bitmapCuadrado, tamanoDeseado, tamanoDeseado, true)
    }

    private fun abrirCamara() {
        val intentoCamara = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        try { capturarFoto.launch(intentoCamara) } catch (e: Exception) { Toast.makeText(this, "Error cámara", Toast.LENGTH_SHORT).show() }
    }

    private fun enviarImagenALaApi(bitmap: Bitmap) {
        tvResultado.text = "Analizando rostro..."

        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)

        val mediaType = "image/jpeg".toMediaTypeOrNull()
        val requestFile = okhttp3.RequestBody.create(mediaType, stream.toByteArray())

        // Parámetro de envío como "image"
        val body = MultipartBody.Part.createFormData("image", "rostro.jpg", requestFile)

        // URL base de ngrok actual
        val urlNgrokBase = "https://giddiness-sizable-handbook.ngrok-free.dev/"

        val okHttpClient = okhttp3.OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("ngrok-skip-browser-warning", "true")
                    .build()
                chain.proceed(request)
            }
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(urlNgrokBase)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(ApiService::class.java)

        apiService.enviarImagen(body).enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    tvResultado.text = "Resultado: ${response.body()!!.obtenerResumen()}"
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Sin detalle"
                    tvResultado.text = "Error API (${response.code()}): $errorBody"
                }
            }
            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                tvResultado.text = "Error de red: ${t.message}"
            }
        })
    }
}