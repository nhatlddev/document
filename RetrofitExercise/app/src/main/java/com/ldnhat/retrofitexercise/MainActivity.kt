package com.ldnhat.retrofitexercise

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.ldnhat.adapter.UserAdapter
import com.ldnhat.api.BaseConfig
import com.ldnhat.api.IUserAPI
import com.ldnhat.model.Users
import kotlinx.android.synthetic.main.activity_main.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {

    private lateinit var userAdapter: UserAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val userAPI = BaseConfig().getInstance().create(IUserAPI::class.java)
        userAPI.findAll().enqueue(object : Callback<MutableList<Users>>{
            override fun onResponse(
                call: Call<MutableList<Users>>,
                response: Response<MutableList<Users>>
            ) {
                val users: MutableList<Users>? = response.body()
                if (users != null) {
                    Log.d("user size", users.size.toString())
                }
                if (users != null) {
                    handle(users)
                }
            }

            override fun onFailure(call: Call<MutableList<Users>>, t: Throwable) {
                t.printStackTrace()
            }

        })
    }

    private fun handle(users:MutableList<Users>){
        userAdapter = UserAdapter(this@MainActivity, users)
        rv_information.layoutManager = LinearLayoutManager(this@MainActivity)
        rv_information.adapter = userAdapter
        userAdapter.notifyDataSetChanged()
    }
}