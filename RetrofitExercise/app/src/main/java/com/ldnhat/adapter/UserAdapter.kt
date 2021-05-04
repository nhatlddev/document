package com.ldnhat.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ldnhat.model.Users
import com.ldnhat.retrofitexercise.R

class UserAdapter : RecyclerView.Adapter<UserAdapter.UserItemViewHolder>{

    private var context:Context
    private var users:MutableList<Users>

    constructor(context: Context, users: MutableList<Users>) : super() {
        this.context = context
        this.users = users
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserItemViewHolder {
        val v:View = LayoutInflater.from(context).inflate(R.layout.information_recyclerview, parent, false)

        return UserItemViewHolder(v)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: UserItemViewHolder, position: Int) {
       val user:Users = users[position]

        holder.userId.text = "userId : "+user.userId.toString()
        holder.id.text = "id : "+user.id.toString()
        holder.title.text = "title: "+user.title
        holder.body.text = "body: "+user.body

    }

    override fun getItemCount(): Int {
        return users.size
    }

    class UserItemViewHolder : RecyclerView.ViewHolder{

        var userId:TextView
        var id:TextView
        var title:TextView
        var body:TextView

        constructor(v: View) : super(v){
            userId = v.findViewById(R.id.user_id)
            id = v.findViewById(R.id.id)
            title = v.findViewById(R.id.title)
            body = v.findViewById(R.id.body)
        }
    }
}