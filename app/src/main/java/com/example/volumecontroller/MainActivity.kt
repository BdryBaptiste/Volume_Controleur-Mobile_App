package com.example.volumecontroller

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.example.volumecontroller.Adapter.ApplicationAdapter
import com.example.volumecontroller.databinding.ActivityMainBinding
import com.example.volumecontroller.models.ApplicationsResponse
import com.tbuonomo.viewpagerdotsindicator.DotsIndicator
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    lateinit var apiService: ApiService // Rendue accessible à PageFragment
    private lateinit var viewPager: ViewPager2
    private lateinit var dotsIndicator: DotsIndicator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // For View Binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configurer Retrofit
        val retrofit = Retrofit.Builder()
            .baseUrl("http://192.168.1.35:5000/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit.create(ApiService::class.java)

        // Initialiser ViewPager2 et DotsIndicator
        viewPager = binding.viewPager
        dotsIndicator = binding.dotsIndicator

        // Charger les applications
        loadApplications()

        setSupportActionBar(binding.toolbar)
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
}
