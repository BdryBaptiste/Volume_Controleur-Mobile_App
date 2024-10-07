package com.example.volumecontroller.Adapter

import android.app.AlertDialog
import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.volumecontroller.ApiService
import com.example.volumecontroller.R
import com.example.volumecontroller.databinding.ItemApplicationBinding
import com.example.volumecontroller.databinding.LayoutVolumeControlPopupBinding
import com.example.volumecontroller.models.*
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
            // Charger l'icône de l'application
            val iconUrl = "http://192.168.1.35:5000/applications/$appName/icon"

            Glide.with(binding.root.context)
                .load(iconUrl)
                .placeholder(R.drawable.placeholder_icon)
                .error(R.drawable.error_icon)
                .into(binding.applicationIconButton)

            // Gérer le clic sur l'icône
            binding.applicationIconButton.setOnClickListener {
                showControlsPopup(binding.root.context, appName)
            }
        }

        private fun showControlsPopup(context: Context, appName: String) {
            val popupBinding = LayoutVolumeControlPopupBinding.inflate(LayoutInflater.from(context))

            val dialog = AlertDialog.Builder(context)
                .setView(popupBinding.root)
                .create()

            popupBinding.applicationNameTextView.text = appName

            // Récupérer le volume actuel
            apiService.getVolume(appName).enqueue(object : Callback<VolumeResponse> {
                override fun onResponse(call: Call<VolumeResponse>, response: Response<VolumeResponse>) {
                    if (response.isSuccessful) {
                        val volume = response.body()?.volume ?: 0f
                        val progress = (volume * 100).toInt()
                        popupBinding.volumeSeekBar.progress = progress
                    }
                }

                override fun onFailure(call: Call<VolumeResponse>, t: Throwable) {
                    Log.e("API_ERROR", "Erreur lors de la récupération du volume pour $appName", t)
                }
            })

            // Récupérer le statut mute
            apiService.getMuteStatus(appName).enqueue(object : Callback<MuteStatusResponse> {
                override fun onResponse(call: Call<MuteStatusResponse>, response: Response<MuteStatusResponse>) {
                    if (response.isSuccessful) {
                        val isMuted = response.body()?.muted ?: false
                        val iconRes = if (isMuted) R.drawable.ic_volume_off else R.drawable.ic_volume_on
                        popupBinding.muteButton.setImageResource(iconRes)
                    }
                }

                override fun onFailure(call: Call<MuteStatusResponse>, t: Throwable) {
                    Log.e("API_ERROR", "Erreur lors de la récupération du statut mute pour $appName", t)
                }
            })

            // Gérer le changement de volume
            popupBinding.volumeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        val volumeFraction = progress / 100.0f
                        apiService.setVolume(appName, VolumeRequest(volumeFraction)).enqueue(object : Callback<VolumeResponse> {
                            override fun onResponse(call: Call<VolumeResponse>, response: Response<VolumeResponse>) {
                                Log.d("ApplicationAdapter", "Volume changé pour $appName à $volumeFraction")
                            }

                            override fun onFailure(call: Call<VolumeResponse>, t: Throwable) {
                                Log.e("API_ERROR", "Erreur lors du changement du volume pour $appName", t)
                            }
                        })
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })

            // Gérer le clic sur le bouton mute/unmute
            popupBinding.muteButton.setOnClickListener {
                // Récupérer le statut actuel du bouton (l'icône affichée)
                val currentIconRes = (popupBinding.muteButton.drawable as? BitmapDrawable)?.bitmap
                val isMuted = currentIconRes == (ContextCompat.getDrawable(context, R.drawable.ic_volume_off) as? BitmapDrawable)?.bitmap

                // Inverser le statut mute localement
                val newIsMuted = !isMuted
                val newIconRes = if (newIsMuted) R.drawable.ic_volume_off else R.drawable.ic_volume_on
                popupBinding.muteButton.setImageResource(newIconRes)

                // Envoyer la requête au serveur
                val action = if (newIsMuted) "mute" else "unmute"
                val actionRequest = MuteActionRequest(action)

                apiService.changeMuteStatus(appName, actionRequest).enqueue(object : Callback<MuteResponse> {
                    override fun onResponse(call: Call<MuteResponse>, response: Response<MuteResponse>) {
                        if (!response.isSuccessful) {
                            // En cas d'erreur, rétablir l'icône précédente
                            popupBinding.muteButton.setImageResource(if (newIsMuted) R.drawable.ic_volume_on else R.drawable.ic_volume_off)
                            Log.e("API_ERROR", "Erreur lors du changement du statut mute pour $appName")
                        }
                    }

                    override fun onFailure(call: Call<MuteResponse>, t: Throwable) {
                        // En cas d'échec, rétablir l'icône précédente
                        popupBinding.muteButton.setImageResource(if (newIsMuted) R.drawable.ic_volume_on else R.drawable.ic_volume_off)
                        Log.e("API_ERROR", "Erreur lors du changement du statut mute pour $appName", t)
                    }
                })
            }

            // Afficher la popup
            dialog.show()
        }
    }
}
