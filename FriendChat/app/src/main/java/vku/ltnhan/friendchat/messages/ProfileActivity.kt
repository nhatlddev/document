package vku.ltnhan.friendchat.messages

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.android.synthetic.main.activity_new_message.*
import kotlinx.android.synthetic.main.activity_profile.*
import kotlinx.android.synthetic.main.activity_register.*
import kotlinx.android.synthetic.main.user_row_new_message.view.*
import vku.ltnhan.friendchat.R
import vku.ltnhan.friendchat.models.User
import java.io.File
import java.io.IOException
import java.util.*
import kotlin.collections.HashMap

class ProfileActivity : AppCompatActivity() {

    private lateinit var firebaseUser: FirebaseUser
    private lateinit var databaseReference: DatabaseReference

    var filePath: Uri? = null

    private val PICK_IMAGE_REQUEST: Int = 2020

    private lateinit var storage: FirebaseStorage
    private lateinit var storageRef: StorageReference


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        supportActionBar?.title = "Profile User"

        firebaseUser = FirebaseAuth.getInstance().currentUser!!

        databaseReference =
                FirebaseDatabase.getInstance().getReference("/users").child(firebaseUser.uid)

        storage = FirebaseStorage.getInstance()
        storageRef = storage.reference

        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(applicationContext, error.message, Toast.LENGTH_SHORT).show()
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue(User::class.java)
                etUserName.setText(user!!.username)

                if (user.profileImageUrl == "") {
                    userImage.setImageResource(R.drawable.no_image2)
                } else {
                    Glide.with(this@ProfileActivity).load(user.profileImageUrl).into(userImage)
                }
            }
        })
        button_facebook.setOnClickListener {
                
        }
        userImage.setOnClickListener {
            chooseImage()
        }

        btnSave.setOnClickListener {
            uploadImage()
            progressBar.visibility = View.VISIBLE
        }

    }
    private fun chooseImage() {
        val intent: Intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_PICK
        startActivityForResult(Intent.createChooser(intent, "Select Image"), PICK_IMAGE_REQUEST)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && data != null) {
            filePath = data.data

            Log.d("profile_file_path ", filePath?.path.toString())
            try {
                var bitmap: Bitmap = MediaStore.Images.Media.getBitmap(contentResolver, filePath)
                userImage.setImageBitmap(bitmap)
                btnSave.visibility = View.VISIBLE
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
    private fun uploadImage() {
            var ref: StorageReference = storageRef.child("images/" + UUID.randomUUID().toString())
            ref.putFile(filePath!!)
                    .addOnSuccessListener {

                        val hashMap:HashMap<String,String> = HashMap()
                        Log.d("file path ", filePath.toString())
                        hashMap.put("userName",etUserName.text.toString())
                        ref.downloadUrl.addOnSuccessListener {
                            hashMap.put("profileImageUrl",it.toString())
                            databaseReference.updateChildren(hashMap as Map<String, Any>)
                            progressBar.visibility = View.GONE
                            Toast.makeText(applicationContext, "Uploaded", Toast.LENGTH_SHORT).show()
                            btnSave.visibility = View.GONE
                        }

                    }
                    .addOnFailureListener {
                        progressBar.visibility = View.GONE
                        Toast.makeText(applicationContext, "Failed" + it.message, Toast.LENGTH_SHORT)
                                .show()
                    }

        }

}



