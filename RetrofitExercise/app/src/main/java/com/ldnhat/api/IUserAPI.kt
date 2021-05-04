package com.ldnhat.api

import com.ldnhat.model.Users
import retrofit2.Call
import retrofit2.http.GET

interface IUserAPI {

    @GET("posts")
    fun findAll() : Call<MutableList<Users>>
}