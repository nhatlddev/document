package com.ldnhat.flashlight

import android.graphics.drawable.LevelListDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class TrafficActivity : AppCompatActivity() {

    internal lateinit var imgTraffic:ImageView
    internal lateinit var handler: Handler
    internal lateinit var timer: Timer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.traffic_activity)

        imgTraffic = findViewById(R.id.img_traffic)

        handler = object : Handler(Looper.getMainLooper()){
            override fun handleMessage(msg: Message) {
                super.handleMessage(msg)
                var levelList:LevelListDrawable = imgTraffic.drawable as LevelListDrawable

                var currentLevel:Int = levelList.level
                if(currentLevel == 0 || currentLevel == 1){
                    currentLevel++
                }else if(currentLevel == 2){
                    currentLevel = 0
                }

                imgTraffic.setImageLevel(currentLevel)
            }
        }
        timer = Timer()
        timer.scheduleAtFixedRate( object : TimerTask(){
            override fun run() {
                handler.sendEmptyMessage(0)
            }

        }, 1000, 1000)
    }
}

