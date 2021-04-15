package com.ldnhat.flashlight

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class WarringActivity : AppCompatActivity() {

    internal lateinit var tgWarring:ToggleButton
    internal lateinit var handler: Handler
    internal var checkWarring:Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.warring_activity)

        tgWarring = findViewById(R.id.tg_warring)

        handler = object : Handler(Looper.getMainLooper()){
            override fun handleMessage(msg: Message) {
                super.handleMessage(msg)
                if(!checkWarring){
                    checkWarring = true
                }else{
                    checkWarring = false
                }
                tgWarring.isChecked = checkWarring
            }
        }

        var timer = Timer()
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                handler.sendEmptyMessage(0)
            }

        }, 1000, 1000)
    }
}