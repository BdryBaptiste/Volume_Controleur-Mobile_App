package com.example.volumecontroller

import com.example.volumecontroller.models.*
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

    @GET("applications/{process_name}/mute_status")
    fun getMuteStatus(@Path("process_name") processName: String): Call<MuteStatusResponse>

    @POST("applications/{process_name}/mute")
    fun changeMuteStatus(
        @Path("process_name") processName: String,
        @Body actionRequest: MuteActionRequest
    ): Call<MuteResponse>
}
