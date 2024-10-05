package com.example.volumecontroller

import com.example.volumecontroller.models.ApplicationsResponse
import com.example.volumecontroller.models.VolumeResponse
import retrofit2.Call
import retrofit2.http.*

interface ApiService {

    @GET("applications")
    fun getApplications(): Call<ApplicationsResponse>

    @GET("applications/{process_name}/volume")
    fun getVolume(@Path("process_name") processName: String): Call<VolumeResponse>

    @POST("applications/{process_name}/volume")
    fun setVolume(
        @Path("process_name") processName: String,
        @Body volumeRequest: VolumeRequest
    ): Call<VolumeResponse>
}

data class VolumeRequest(val volume: Float)
