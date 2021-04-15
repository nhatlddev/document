package com.ldnhat.flashlight

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity

class MenuActivity : AppCompatActivity(), View.OnClickListener{

    internal lateinit var btnLight:ImageButton
    internal lateinit var btnNeon:ImageButton
    internal lateinit var btnWarring:ImageButton
    internal lateinit var btnTraffic:ImageButton
    internal lateinit var btnPolic:ImageButton
    internal lateinit var btnTurnFlash:ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_menu)

        btnLight = findViewById(R.id.btn_flashlight)
        btnNeon = findViewById(R.id.btn_neon)
        btnWarring = findViewById(R.id.btn_warring)
        btnTraffic = findViewById(R.id.btn_traffic)
        btnPolic = findViewById(R.id.btn_polic)
        btnTurnFlash = findViewById(R.id.btn_turn_flash)

        btnLight.setOnClickListener(this)
        btnNeon.setOnClickListener(this)
        btnWarring.setOnClickListener(this)
        btnTraffic.setOnClickListener(this)
        btnPolic.setOnClickListener(this)
        btnTurnFlash.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        var intent:Intent
        when(v.id){
            R.id.btn_flashlight -> {
                intent = Intent(this, LightActivity::class.java)
                startActivity(intent)
            }
            R.id.btn_neon -> {
                intent = Intent(this, NeonActivity::class.java)
                startActivity(intent)
            }
            R.id.btn_warring -> {
                intent = Intent(this, WarringActivity::class.java)
                startActivity(intent)
            }
            R.id.btn_traffic -> {
                intent = Intent(this, TrafficActivity::class.java)
                startActivity(intent)
            }
            R.id.btn_polic -> {
                intent = Intent(this, PolicActivity::class.java)
                startActivity(intent)
            }
            R.id.btn_turn_flash -> {
                intent = Intent(this, FlashActivity::class.java)
                startActivity(intent)
            }
        }
    }


}