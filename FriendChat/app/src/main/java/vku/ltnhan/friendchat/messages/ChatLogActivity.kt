package vku.ltnhan.friendchat.messages

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_chat_log.*
import kotlinx.android.synthetic.main.activity_new_message.*
import kotlinx.android.synthetic.main.chat_from_row.view.*
import kotlinx.android.synthetic.main.chat_to_row.view.*
import vku.ltnhan.friendchat.NewMessageActivity
import vku.ltnhan.friendchat.R
import vku.ltnhan.friendchat.models.ChatMessage
import vku.ltnhan.friendchat.models.User


class ChatLogActivity : AppCompatActivity() {
    companion object{
        val TAG = "ChatLog"
    }
    val adapter = GroupAdapter<ViewHolder>()
    var toUser: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_log)

        recyclerview_chat_log.adapter = adapter
//       get data about chatlog user
        toUser = intent.getParcelableExtra<User>(NewMessageActivity.USER_KEY)
        supportActionBar?.title = toUser?.username
//        setupDummyData()
        listenForMessage()
        send_button_chat_log.setOnClickListener {
            Log.d(TAG,"Attempt to send message...")
            performSendMessage()
        }
    }
    private fun listenForMessage(){
        val fromId = FirebaseAuth.getInstance().uid
        val toId = toUser!!.uid
        val ref = FirebaseDatabase.getInstance().getReference("/user-messages/$fromId/$toId")

        ref.addChildEventListener(object: ChildEventListener{
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val chatMessage = snapshot.getValue(ChatMessage::class.java)
                if (chatMessage != null){
                    Log.d(TAG, chatMessage!!.text)

                    if (chatMessage.fromId == FirebaseAuth.getInstance().uid){
                        val currentUser = LatestMessagesActivity.currentUser
                        adapter.add(ChatFromItem(chatMessage.text, currentUser!!))
                    }else{
                        adapter.add(ChatToItem(chatMessage.text, toUser!!))
                    }
                }
                recyclerview_chat_log.scrollToPosition(adapter.itemCount -1 )
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                TODO("Not yet implemented")
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                TODO("Not yet implemented")
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                TODO("Not yet implemented")
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        })
    }
    private fun performSendMessage(){
        //how to we actually send a message to firebase...
        val text = edittext_chat_log.text.toString()

        val fromId = FirebaseAuth.getInstance().uid
        val user = intent.getParcelableExtra<User>(NewMessageActivity.USER_KEY)
        val toId = user!!.uid

        if(fromId == null) return
//        val reference = FirebaseDatabase.getInstance().getReference("/messages").push()
        val fromReference = FirebaseDatabase.getInstance().getReference("/user-messages/$fromId/$toId").push()
        val toReference = FirebaseDatabase.getInstance().getReference("/user-messages/$toId/$fromId").push()

        val chatMessage = ChatMessage(fromReference.key!!, text, fromId, toId, System.currentTimeMillis()/1000)
        fromReference.setValue(chatMessage)
            .addOnCompleteListener {
                Log.d(TAG,"Save our chat message: ${fromReference.key}")
                edittext_chat_log.text.clear()
                recyclerview_chat_log.scrollToPosition(adapter.itemCount - 1)
            }
        toReference.setValue(chatMessage)
            .addOnCompleteListener {
                Log.d(TAG,"Save our chat message: ${toReference.key}")
                edittext_chat_log.text.clear()
                recyclerview_chat_log.scrollToPosition(adapter.itemCount - 1)
            }
        val latestMessageFromRef = FirebaseDatabase.getInstance().getReference("latest-messages/$fromId/$toId")
        latestMessageFromRef.setValue(chatMessage)

        val latestMessageToRef = FirebaseDatabase.getInstance().getReference("latest-messages/$toId/$fromId")
        latestMessageToRef.setValue(chatMessage)
    }
}
class ChatFromItem(val text: String, val user: User): Item<ViewHolder>() {
    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.itemView.textview_from_row.text = text

        //load our user image into the star
        val uri = user.profileImageUrl
        val targetImage = viewHolder.itemView.imageview_chat_from_row
        Picasso.get().load(uri).into(targetImage)
    }

    override fun getLayout(): Int {
        return R.layout.chat_from_row
    }
}
class ChatToItem(val text: String, val user: User): Item<ViewHolder>() {
    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.itemView.textview_to_row.text = text

        //load our user image into the star
        val uri = user.profileImageUrl
        val targetImage = viewHolder.itemView.imageview_chat_to_row
        Picasso.get().load(uri).into(targetImage)
    }

    override fun getLayout(): Int {
        return R.layout.chat_to_row
    }
}
