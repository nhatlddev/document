package com.ldnhat.flashlight

import android.os.Bundle
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity

class LightActivity : AppCompatActivity() {

    internal lateinit var toggleTurn:ToggleButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_light)

        toggleTurn = findViewById(R.id.tg_light)

    }
}