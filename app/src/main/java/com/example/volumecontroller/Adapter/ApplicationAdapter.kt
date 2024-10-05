// Adapter/ApplicationAdapter.kt
package com.example.volumecontroller.Adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.recyclerview.widget.RecyclerView
import com.example.volumecontroller.ApiService
import com.example.volumecontroller.VolumeRequest
import com.example.volumecontroller.databinding.ItemApplicationBinding
import com.example.volumecontroller.models.VolumeResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ApplicationAdapter(private val apiService: ApiService) :
    RecyclerView.Adapter<ApplicationAdapter.ApplicationViewHolder>() {

    private var applications = listOf<String>()

    fun setApplications(apps: List<String>) {
        applications = apps
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ApplicationViewHolder {
        val binding = ItemApplicationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ApplicationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ApplicationViewHolder, position: Int) {
        val appName = applications[position]
        holder.bind(appName)
    }

    override fun getItemCount(): Int = applications.size

    inner class ApplicationViewHolder(private val binding: ItemApplicationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(appName: String) {
            binding.applicationNameTextView.text = appName

            // Récupérer le volume actuel
            apiService.getVolume(appName).enqueue(object : Callback<VolumeResponse> {
                override fun onResponse(call: Call<VolumeResponse>, response: Response<VolumeResponse>) {
                    if (response.isSuccessful) {
                        val volume = response.body()?.volume ?: 0f
                        binding.volumeSeekBar.progress = volume.toInt()
                        Log.d("MainActivity", "Volume récupéré")
                    }
                }

                override fun onFailure(call: Call<VolumeResponse>, t: Throwable) {
                    Log.e("API_ERROR", "Erreur lors de la récupération du volume", t)
                }
            })

            // Gérer le changement de volume
            binding.volumeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        // Envoyer le nouveau volume au serveur
                        apiService.setVolume(appName, VolumeRequest(progress.toFloat())).enqueue(object : Callback<VolumeResponse> {
                            override fun onResponse(call: Call<VolumeResponse>, response: Response<VolumeResponse>) {
                                Log.d("MainActivity", "Volume changé")
                            }

                            override fun onFailure(call: Call<VolumeResponse>, t: Throwable) {
                                Log.e("API_ERROR", "Erreur lors du changement du volume", t)
                            }
                        })
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
    }
}
