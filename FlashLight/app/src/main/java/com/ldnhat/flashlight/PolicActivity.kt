package com.ldnhat.flashlight

import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class PolicActivity : AppCompatActivity() {

    internal lateinit var tgPolic:ToggleButton
    internal lateinit var handler: Handler
    internal lateinit var timer: Timer
    internal var checked:Boolean = false
    internal lateinit var mediaPlayer: MediaPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.polic_activity)

        mediaPlayer = MediaPlayer.create(this, R.raw.music_polic)
        mediaPlayer.isLooping = true
        mediaPlayer.start()

        tgPolic = findViewById(R.id.tg_polic)

        handler = object : Handler(Looper.getMainLooper()){
            override fun handleMessage(msg: Message) {
                super.handleMessage(msg)
                    if(!checked){
                        checked = true
                    }else{
                        checked = false
                    }
                tgPolic.isChecked = checked
            }
        }

        timer = Timer()
        timer.schedule(object : TimerTask(){
            override fun run() {
                handler.sendEmptyMessage(0)
            }
        }, 1000, 1000)
    }

    override fun onPause() {
        super.onPause()
        mediaPlayer.isLooping = false
        mediaPlayer.stop()
    }

    override fun onRestart() {
        super.onRestart()
        mediaPlayer = MediaPlayer.create(this, R.raw.music_polic)
        mediaPlayer.isLooping = true
        mediaPlayer.start()
    }
}