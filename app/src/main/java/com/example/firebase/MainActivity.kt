package com.example.firebase

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.example.firebase.modul.Weather
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : AppCompatActivity() {
    private lateinit var launcher: ActivityResultLauncher<Intent>
    private lateinit var auth: FirebaseAuth
    private lateinit var fireStore: FirebaseFirestore
    private lateinit var button: Button
    private lateinit var button2: Button
    lateinit var doc: DocumentReference
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        button = findViewById(R.id.main_button)
        button2 = findViewById(R.id.second_button)
        button.setOnClickListener {
            signInWithGoogle()
        }
        button2.setOnClickListener {
            auth.signOut()
            button.visibility = View.VISIBLE
            button2.visibility = View.INVISIBLE
        }
        launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
            val task = GoogleSignIn.getSignedInAccountFromIntent(it.data)
            try {
                val account = task.getResult(ApiException::class.java)
                if (account != null){
                    firebaseAuthWithGoogle(account.idToken!!)
                }
            }catch (e:ApiException){
                Log.e("error", e.message.toString())
            }
        }
        auth = FirebaseAuth.getInstance()
        if (auth.currentUser != null){
            button.visibility = View.INVISIBLE
            button2.visibility = View.VISIBLE
        }
        fireStore = FirebaseFirestore.getInstance()
        doc = fireStore.document("data/link")
        getData {
            Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            getWeather(it.toString())
        }
    }

    fun getWeather(url: String){
        val retrofit = ApiInterface.create(url).getWeather(
            key = "17445aad957a4d719a3222356221204",
            q = "Худжанд",
            aqi = "no"
        )
        retrofit.enqueue(object: Callback<Weather>{
            override fun onResponse(call: Call<Weather>, response: Response<Weather>) {
                val weather = response.body()?.current
                Toast.makeText(this@MainActivity, weather?.temp_c.toString(), Toast.LENGTH_SHORT).show()
            }

            override fun onFailure(call: Call<Weather>, t: Throwable) {
            }

        })
    }

    fun getData(onComplete:(String?) -> Unit){
        doc.get().addOnSuccessListener {
            onComplete(it["name"].toString())
        }
    }

    private fun getClient(): GoogleSignInClient{
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(this, gso)
    }

    private fun signInWithGoogle(){
        val client = getClient()
        launcher.launch(client.signInIntent)
    }

    private fun firebaseAuthWithGoogle(token: String){
        val credential = GoogleAuthProvider.getCredential(token, null)
        auth.signInWithCredential(credential).addOnCompleteListener {
            if (it.isSuccessful){
                Toast.makeText(this, "Успешно выполнено вход с аккаунта" + auth.currentUser?.photoUrl, Toast.LENGTH_SHORT)
                    .show()
                button.visibility = View.INVISIBLE
                button2.visibility = View.VISIBLE
            }
            else if (it.isCanceled){
                Toast.makeText(this, "Отменен", Toast.LENGTH_SHORT).show()
            }
            else{
                Toast.makeText(this, "Ошибка входа", Toast.LENGTH_SHORT).show()
            }
        }
    }
}