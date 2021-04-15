package com.ldnhat.flashlight

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Bundle
import android.widget.ToggleButton
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity

class FlashActivity : AppCompatActivity() {

    internal lateinit var tgFlash:ToggleButton
    internal lateinit var cameraManager: CameraManager

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.flashcamera_activity)

        tgFlash = findViewById(R.id.tg_flash_camera)
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        var cameraId:String = ""

        try {
            cameraId = cameraManager.cameraIdList[0]
        }catch (e : CameraAccessException){
            e.printStackTrace()
        }

        val finalCameraId:String = cameraId

       tgFlash.setOnCheckedChangeListener { buttonView, isChecked ->
           if(isChecked){
                turnOnFlashCamera(finalCameraId, true)
           }else{
                turnOffFlashCamera(finalCameraId, false)
           }
       }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    internal fun turnOnFlashCamera(cameraId:String, check:Boolean){
        cameraManager.setTorchMode(cameraId, check)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    internal fun turnOffFlashCamera(cameraId:String, check:Boolean){
        cameraManager.setTorchMode(cameraId, check)
    }
}
