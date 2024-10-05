package com.example.volumecontroller

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.volumecontroller.Adapter.ApplicationAdapter
import com.example.volumecontroller.databinding.ActivityMainBinding
import com.example.volumecontroller.models.ApplicationsResponse
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var apiService: ApiService
    private lateinit var adapter: ApplicationAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // For View Binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configurer Retrofit
        val retrofit = Retrofit.Builder()
            .baseUrl("http://192.168.1.194:5000/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit.create(ApiService::class.java)

        // Configurer le RecyclerView
        adapter = ApplicationAdapter(apiService)
        binding.applicationsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.applicationsRecyclerView.adapter = adapter

        // Charger les applications
        loadApplications()
    }

    private fun loadApplications() {
        apiService.getApplications().enqueue(object : Callback<ApplicationsResponse> {
            override fun onResponse(call: Call<ApplicationsResponse>, response: Response<ApplicationsResponse>) {
                if (response.isSuccessful) {
                    val applications = response.body()?.applications ?: emptyList()
                    adapter.setApplications(applications)
                    Log.d("MainActivity", "Chargement des applications...")
                }
            }

            override fun onFailure(call: Call<ApplicationsResponse>, t: Throwable) {
                Log.e("API_ERROR", "Erreur lors de l'appel API", t)
            }
        })
    }
}
