package com.ldnhat.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class BaseConfig {

    lateinit var retrofit: Retrofit

    fun getInstance() : Retrofit{

        retrofit = Retrofit.Builder().baseUrl("https://jsonplaceholder.typicode.com/")
            .addConverterFactory(GsonConverterFactory.create()).build()

        return retrofit
    }

}