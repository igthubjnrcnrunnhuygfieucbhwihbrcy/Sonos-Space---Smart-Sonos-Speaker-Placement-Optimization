package com.sonosequencer.alternative.network

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

// Sonos API service for controlling Sonos speakers
// Note: Sonos uses UPnP for discovery and SOAP for control
// This is a simplified interface for common operations

interface SonosApiService {
    
    // Discover speakers on the network
    @GET("households")
    fun discoverHouseholds(
        @Query("api_key") apiKey: String
    ): Call<HouseholdResponse>
    
    @GET("households/{householdId}/groups")
    fun getGroups(
        @Path("householdId") householdId: String,
        @Query("api_key") apiKey: String
    ): Call<GroupsResponse>
    
    @GET("households/{householdId}/groups/{groupId}/players")
    fun getPlayersInGroup(
        @Path("householdId") householdId: String,
        @Path("groupId") groupId: String,
        @Query("api_key") apiKey: String
    ): Call<PlayersResponse>
    
    @POST("households/{householdId}/groups/{groupId}/playback/play")
    fun play(
        @Path("householdId") householdId: String,
        @Path("groupId") groupId: String,
        @Query("api_key") apiKey: String
    ): Call<ResponseBody>
    
    @POST("households/{householdId}/groups/{groupId}/playback/pause")
    fun pause(
        @Path("householdId") householdId: String,
        @Path("groupId") groupId: String,
        @Query("api_key") apiKey: String
    ): Call<ResponseBody>
    
    @POST("households/{householdId}/groups/{groupId}/playback/stop")
    fun stop(
        @Path("householdId") householdId: String,
        @Path("groupId") groupId: String,
        @Query("api_key") apiKey: String
    ): Call<ResponseBody>
    
    @POST("households/{householdId}/groups/{groupId}/volume")
    fun setVolume(
        @Path("householdId") householdId: String,
        @Path("groupId") groupId: String,
        @Query("api_key") apiKey: String,
        @Body volumeRequest: VolumeRequest
    ): Call<ResponseBody>
    
    @POST("households/{householdId}/groups/{groupId}/playback/seek")
    fun seek(
        @Path("householdId") householdId: String,
        @Path("groupId") groupId: String,
        @Query("api_key") apiKey: String,
        @Body seekRequest: SeekRequest
    ): Call<ResponseBody>
    
    @POST("households/{householdId}/groups/{groupId}/playback/previous")
    fun previousTrack(
        @Path("householdId") householdId: String,
        @Path("groupId") groupId: String,
        @Query("api_key") apiKey: String
    ): Call<ResponseBody>
    
    @POST("households/{householdId}/groups/{groupId}/playback/next")
    fun nextTrack(
        @Path("householdId") householdId: String,
        @Path("groupId") groupId: String,
        @Query("api_key") apiKey: String
    ): Call<ResponseBody>
    
    @GET("households/{householdId}/groups/{groupId}/playback")
    fun getPlaybackState(
        @Path("householdId") householdId: String,
        @Path("groupId") groupId: String,
        @Query("api_key") apiKey: String
    ): Call<PlaybackStateResponse>
}

data class HouseholdResponse(
    val households: List<Household>
)

data class Household(
    val id: String,
    val name: String
)

data class GroupsResponse(
    val groups: List<Group>
)

data class Group(
    val id: String,
    val name: String,
    val playerIds: List<String>
)

data class PlayersResponse(
    val players: List<Player>
)

data class Player(
    val id: String,
    val name: String,
    val model: String,
    val ipAddress: String,
    val macAddress: String,
    val isConnected: Boolean
)

data class VolumeRequest(
    val volume: Int // 0-100
)

data class SeekRequest(
    val position: Int // milliseconds
)

data class PlaybackStateResponse(
    val playbackState: String, // PLAYING, PAUSED, STOPPED
    val currentTrack: Track?,
    val volume: Int,
    val muted: Boolean
)

data class Track(
    val id: String,
    val name: String,
    val artist: String,
    val album: String,
    val duration: Int
)

// For local network discovery (UPnP)
data class SonosDevice(
    val ipAddress: String,
    val port: Int,
    val deviceDescriptionUrl: String,
    val modelName: String,
    val modelNumber: String,
    val friendlyName: String,
    val udn: String
)
