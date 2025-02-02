package com.example.volumecontroller

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.example.volumecontroller.Adapter.ApplicationAdapter
import com.example.volumecontroller.databinding.ActivityMainBinding
import com.example.volumecontroller.models.ApplicationsResponse
import com.tbuonomo.viewpagerdotsindicator.DotsIndicator
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory

class
MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    lateinit var apiService: ApiService // Rendue accessible à PageFragment
    private lateinit var viewPager: ViewPager2
    private lateinit var dotsIndicator: DotsIndicator
    private lateinit var retrofit: Retrofit

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // For View Binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configurer Retrofit
        initRetrofit()

        // Initialiser ViewPager2 et DotsIndicator
        viewPager = binding.viewPager
        dotsIndicator = binding.dotsIndicator

        // Charger les applications
        loadApplications()

        setSupportActionBar(binding.toolbar)
    }

    private fun initRetrofit() {
        val serverAddress = PreferenceManager.getServerAddress(this)
        retrofit = Retrofit.Builder()
            .baseUrl(serverAddress)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit.create(ApiService::class.java)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                // Action lorsque le bouton Actualiser est cliqué
                loadApplications()
                true
            }
            R.id.action_change_server -> {
                // Action pour modifier l'adresse du serveur
                showChangeServerDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun loadApplications() {
        apiService.getApplications().enqueue(object : Callback<ApplicationsResponse> {
            override fun onResponse(call: Call<ApplicationsResponse>, response: Response<ApplicationsResponse>) {
                if (response.isSuccessful) {
                    val applications = response.body()?.applications ?: emptyList()
                    setupViewPager(applications)
                    Log.d("MainActivity", "Chargement des applications...")
                }
            }

            override fun onFailure(call: Call<ApplicationsResponse>, t: Throwable) {
                Log.e("API_ERROR", "Erreur lors de l'appel API", t)
            }
        })
    }

    private fun setupViewPager(applications: List<String>) {
        // Diviser les applications en pages
        val appsPerPage = 2 * 5 // 2 lignes * 5 colonnes
        val pages = applications.chunked(appsPerPage)

        val pagerAdapter = object : androidx.viewpager2.adapter.FragmentStateAdapter(this) {
            override fun getItemCount(): Int = pages.size

            override fun createFragment(position: Int): Fragment {
                return PageFragment.newInstance(pages[position])
            }
        }

        viewPager.adapter = pagerAdapter
        dotsIndicator.setViewPager2(viewPager)
    }

    private fun showChangeServerDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_change_server, null)
        val serverAddressEditText = dialogView.findViewById<EditText>(R.id.serverAddressEditText)

        // Pré-remplir avec l'adresse actuelle
        serverAddressEditText.setText(PreferenceManager.getServerAddress(this))

        val dialog = AlertDialog.Builder(this)
            .setTitle("Modifier l'adresse du serveur")
            .setView(dialogView)
            .setPositiveButton("Enregistrer") { _, _ ->
                val newAddress = serverAddressEditText.text.toString()
                if (newAddress.isNotEmpty()) {
                    PreferenceManager.setServerAddress(this, newAddress)
                    initRetrofit() // Réinitialiser Retrofit avec la nouvelle adresse
                    loadApplications() // Recharger les applications
                }
            }
            .setNegativeButton("Annuler", null)
            .create()

        dialog.show()
    }
}
