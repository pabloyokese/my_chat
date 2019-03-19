package com.example.oqundojuan.mychat.controller

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.service.autofill.UserData
import android.support.v4.content.LocalBroadcastManager
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import com.example.oqundojuan.mychat.Adapters.MessageAdapter
import com.example.oqundojuan.mychat.Model.Channel
import com.example.oqundojuan.mychat.Model.Message
import com.example.oqundojuan.mychat.services.MessageService
import com.example.oqundojuan.mychat.R
import com.example.oqundojuan.mychat.Utilities.BROADCAST_USER_DATA_CHANGE
import com.example.oqundojuan.mychat.Utilities.SOCKET_URL
import com.example.oqundojuan.mychat.services.AuthService
import com.example.oqundojuan.mychat.services.UserDataService
import io.socket.client.IO
import io.socket.emitter.Emitter
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.android.synthetic.main.nav_header_main.view.*

class MainActivity : AppCompatActivity(){

    val socket = IO.socket(SOCKET_URL)
    lateinit var channelAdapter: ArrayAdapter<Channel>
    lateinit var messageAdapter: MessageAdapter
    var selectedChannel: Channel? = null

    private fun setupAdapters(){
        channelAdapter = ArrayAdapter(this,android.R.layout.simple_list_item_1,
            MessageService.channels)
        channel_list.adapter = channelAdapter
        messageAdapter = MessageAdapter(this,MessageService.messages)
        messageListView.adapter = messageAdapter
        val layoutManager = LinearLayoutManager(this)
        messageListView.layoutManager = layoutManager
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        socket.connect()
        socket.on("channelCreated",onNewChannel)
        socket.on("messageCreated",onNewMessage)

        val toggle = ActionBarDrawerToggle(
            this, drawer_layout, toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()
        setupAdapters()

        channel_list.setOnItemClickListener{ _, _, i, l ->
            selectedChannel = MessageService.channels[i]
            drawer_layout.closeDrawer(GravityCompat.START)
            updateWithChannel()
        }

        if(App.prefs.isLoggedIn){
            AuthService.findUserByEmail(this){}
        }

    }

    override fun onResume() {
        LocalBroadcastManager.getInstance(this).registerReceiver(userDataChangeReceiver,
            IntentFilter(BROADCAST_USER_DATA_CHANGE))
        super.onResume()
    }

    override fun onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(userDataChangeReceiver)
        socket.disconnect()
        super.onDestroy()
    }

    private val userDataChangeReceiver = object :BroadcastReceiver(){
        override fun onReceive(context: Context, intent: Intent?) {
            if (App.prefs.isLoggedIn){
                nav_drawer_header_include.userNameNavHeader.text = UserDataService.name
                nav_drawer_header_include.userEmailNavHeader.text = UserDataService.email
                val resourceId = resources.getIdentifier(UserDataService.avatarName,"drawable",
                    packageName)
                nav_drawer_header_include.userImageNavHeader.setImageResource(resourceId)
                nav_drawer_header_include.userImageNavHeader.setBackgroundColor(UserDataService.returnAvatarColor(UserDataService.avatarColor))
                nav_drawer_header_include.loginBtnNavHeader.text = "Logout"
                MessageService.getChannels{ complete->
                    if(complete){
                        if(MessageService.channels.count()> 0){
                            selectedChannel = MessageService.channels[0]
                            channelAdapter.notifyDataSetChanged()
                            updateWithChannel()
                        }

                    }
                }
            }
        }
    }

    fun updateWithChannel(){
        mainChannelName.text = "#${selectedChannel?.name}"
        // download message for channel
        if(selectedChannel !=null){
            MessageService.getMessages(selectedChannel!!.id){
                complete->
                    if(complete){
                        messageAdapter.notifyDataSetChanged()
                        if(messageAdapter.itemCount > 0){
                            messageListView.smoothScrollToPosition(messageAdapter.itemCount - 1)
                        }
                    }
            }
        }
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    fun loginBtnNavClicked(view: View){
        if(App.prefs.isLoggedIn){
            // log out
            UserDataService.logout()
            channelAdapter.notifyDataSetChanged()
            messageAdapter.notifyDataSetChanged()
            nav_drawer_header_include.userNameNavHeader.text = ""
            nav_drawer_header_include.userEmailNavHeader.text = ""
            nav_drawer_header_include.userImageNavHeader.setImageResource(R.drawable.profiledefault)
            nav_drawer_header_include.userImageNavHeader.setBackgroundColor(Color.TRANSPARENT)
            nav_drawer_header_include.loginBtnNavHeader.text = "Login"
        }else{
            val loginIntent = Intent(this, LoginActivity::class.java)
            startActivity(loginIntent);
        }
    }

    fun addChannelClicked(view: View){
        if(App.prefs.isLoggedIn){
            val builder = AlertDialog.Builder(this)
            val dialogView = layoutInflater.inflate(R.layout.add_channel_dialog,null)
            builder.setView(dialogView)
                .setPositiveButton("Add"){
                    _, _ ->
                    // perform some logic when clicked
                    val nameTextField = dialogView.findViewById<EditText>(R.id.addChannelNameTxt)
                    val descTextField = dialogView.findViewById<EditText>(R.id.addChannelDescTxt)
                    val channelName = nameTextField.text.toString()
                    val channelDesc = descTextField.text.toString()

                    // create channel with the name and description
                    socket.emit("newChannel",channelName,channelDesc)
                }
                .setNegativeButton("cancel"){
                    _, _ ->
                    // cancel and close the dialog
                }
                .show()

        }
    }

    private val onNewChannel = Emitter.Listener { args ->
        if(App.prefs.isLoggedIn){
            runOnUiThread {
                val channelName = args[0] as String
                val channelDesc = args[1] as String
                val channelId = args[2] as String

                val newChannel = Channel(channelName,channelDesc,channelId)
                MessageService.channels.add(newChannel)
                channelAdapter.notifyDataSetChanged()
            }
        }
    }

    private val onNewMessage = Emitter.Listener { args->
        if(App.prefs.isLoggedIn){
            runOnUiThread {
                val channelId = args[2] as String
                if(channelId == selectedChannel?.id){
                    val msgBody = args[0] as String
                    val userName = args[3] as String
                    val userAvatar = args[4] as String
                    val userAvatarColor = args[5] as String
                    val id = args[6] as String
                    val timeStamp = args[7] as String

                    val newMessage = Message(msgBody,userName,channelId,userAvatar,userAvatarColor,id,timeStamp)
                    MessageService.messages.add(newMessage)
                    messageAdapter.notifyDataSetChanged()
                    messageListView.smoothScrollToPosition(messageAdapter.itemCount - 1)
                }
            }
        }
    }

    fun sendMsgBtnClicked(view: View){
        if(App.prefs.isLoggedIn && messageTextField.text.isNotEmpty() && selectedChannel != null){
            val userId = UserDataService.id
            val channelId = selectedChannel!!.id
            socket.emit("newMessage",messageTextField.text.toString(),userId,channelId,
                UserDataService.name,UserDataService.avatarName,UserDataService.avatarColor)
            messageTextField.text.clear()
            hideKeyboard()
        }
    }


    fun hideKeyboard(){
        val inputManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        if(inputManager.isAcceptingText){
            inputManager.hideSoftInputFromWindow(currentFocus.windowToken,0)
        }
    }

}
