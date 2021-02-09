/*
 * Copyright © 2017-2019 WireGuard LLC. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.activity

import android.content.Context
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Chronometer.OnChronometerTickListener
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import com.google.android.material.navigation.NavigationView
import com.wireguard.android.APIService
import com.wireguard.android.Application
import com.wireguard.android.GeneralString
import com.wireguard.android.R
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import com.wireguard.android.model.ObservableTunnel
import com.wireguard.android.model.TunnelDataList
import com.wireguard.android.util.Countries
import com.wireguard.android.util.ErrorMessages
import com.wireguard.config.Config
import kotlinx.android.synthetic.main.main_activity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.io.InputStream
import kotlin.coroutines.CoroutineContext


/**
 * CRUD interface for WireGuard tunnels. This activity serves as the main entry point to the
 * WireGuard application, and contains several fragments for listing, viewing details of, and
 * editing the configuration and interface state of WireGuard tunnels.
 */
class MainActivity : BaseActivity(), FragmentManager.OnBackStackChangedListener, CoroutineScope {
    private val permissionActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { toggleTunnelWithPermissionsResult() }

    private lateinit var drawerLayout: DrawerLayout
    private var actionBar: Toolbar? = null
    private var isTwoPaneLayout = false
    private var connectedDuration : Long = 0;

    private var job: Job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    private fun toggleTunnelWithPermissionsResult() {
        val tunnel = Application.getTunnelManager().lastUsedTunnel ?: return
        lifecycleScope.launch {
            try {
                tunnel.setStateAsync(Tunnel.State.TOGGLE)
            } catch (e: Throwable) {
                 val error = ErrorMessages[e]
                val message = getString(R.string.toggle_error, error)
                Log.e("togel", message, e)
                Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
                //finishAffinity()
                return@launch
            }
            //finishAffinity()
        }
    }

    override fun onBackPressed() {
        if(drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        }else{
            finishAffinity();
            finish();
        }
    }

    override fun onBackStackChanged() {
        if (actionBar == null) return
        // Do not show the home menu when the two-pane layout is at the detail view (see above).
        val backStackEntries = supportFragmentManager.backStackEntryCount
        val minBackStackEntries = if (isTwoPaneLayout) 2 else 1
        //actionBar!!.setDisplayHomeAsUpEnabled(backStackEntries >= minBackStackEntries)
    }
    var lastConnectClick : Long = 0

    override fun onResume() {
        super.onResume()
        if(TunnelDataList.dataInitialized() && !TunnelDataList.data.isEmpty()){
            curr_selected_tunnel_name_txt.text = TunnelDataList.data.find { tunnelData -> tunnelData.id==GeneralString.selectedTunnel }?.location
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        setContentView(R.layout.main_activity)
        connect_layout.visibility = View.VISIBLE
        loading_layout.visibility = View.GONE
        connected_layout.visibility = View.GONE


        curr_selected_tunnel_name_txt.text = "Best"

        // As we're using a Toolbar, we should retrieve it and set it
        // to be our ActionBar

        actionBar = main_toolbar;
        setSupportActionBar(actionBar);
        supportActionBar?.let { ab ->
            ab.setHomeAsUpIndicator(R.drawable.ic_baseline_menu_24)
            ab.setTitle(resources.getString(R.string.app_name));
            ab.setDisplayHomeAsUpEnabled(true)
        }

        // Now retrieve the DrawerLayout so that we can set the status bar color.
        // This only takes effect on Lollipop, or when using translucentStatusBar
        // on KitKat.
        drawerLayout = main_activity_container;
        drawerLayout.setStatusBarBackgroundColor(resources.getColor(R.color.secondary_color));
        nav_view.setNavigationItemSelectedListener(NavigationView.OnNavigationItemSelectedListener { menuItem ->
            drawerLayout.closeDrawers()
            //menuItem.isChecked = true
            when (menuItem.itemId) {
                R.id.share_menu_item -> {

                    val sharingIntent = Intent(Intent.ACTION_SEND)
                    sharingIntent.type = "text/plain"
                    val shareBody = resources.getString(R.string.share_txt)
                    val shareSub = resources.getString(R.string.share_txt)
                    sharingIntent.putExtra(Intent.EXTRA_SUBJECT, shareSub)
                    sharingIntent.putExtra(Intent.EXTRA_TEXT, shareBody)
                    startActivity(Intent.createChooser(sharingIntent, "Share using"))
                }
                R.id.about_menu_item -> {
                    startActivity(Intent(this, AboutUsActivity::class.java))
                }
                /*
                R.id.setting_menu_item -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                }
                R.id.faq_menu_item -> {
                    startActivity(Intent(this, FAQActivity::class.java))
                }*/
                R.id.logout_menu_item -> {
                    disconnectRequest(true)
                }
            }
            true
        })
        curr_selected_tunnel_dur.setText("00 : 00 : 00")
        curr_selected_tunnel_dur.setOnChronometerTickListener(OnChronometerTickListener { chronometer ->
            val text = chronometer.text
            if (text.length == 5) {
                val a = text.toString().split(":".toRegex()).toTypedArray()
                val min = a[0] + " : "
                val sec = a[1]
                chronometer.text = "00 : $min$sec"
            } else if (text.length == 7) {
                val a = text.toString().split(":".toRegex()).toTypedArray()
                val hr = a[0] + " : "
                val min = a[1] + " : "
                val sec = a[2]
                chronometer.text = "0$hr$min$sec"
            }
        })
        supportFragmentManager.addOnBackStackChangedListener(this)
        onBackStackChanged()
        connect_btn.setOnClickListener(View.OnClickListener {
            if (System.currentTimeMillis() < lastConnectClick + 1000) {
                ShowToast(this@MainActivity, "You tap too fast, please wait", Toast.LENGTH_LONG)
                return@OnClickListener
            }
            lastConnectClick = System.currentTimeMillis()
            RetrieveTunnelInfo()
        })

        disconnect_btn.setOnClickListener(View.OnClickListener {
            if (System.currentTimeMillis() < lastConnectClick + 1000) {
                ShowToast(this@MainActivity, "You tap too fast, please wait", Toast.LENGTH_LONG)
                return@OnClickListener
            }
            lastConnectClick = System.currentTimeMillis()
            disconnectRequest()
        })


        val pref1 = getSharedPreferences("key1", Context.MODE_PRIVATE)
        val connectedTime=pref1.getLong("connect_time",0)
        if(connectedTime != 0.toLong()){
            if(GeneralString.connectedTunnelId==-1){
                GeneralString.connectedTunnelId = pref1.getInt("connected_tunnel",-1)
            }
            if(!GeneralString.currTunelInitialized() || !GeneralString.currTunel.state.equals(Tunnel.State.UP)){
                RetrieveTunnelInfo()
            }else if(GeneralString.currTunelInitialized() && GeneralString.currTunel.state.equals(Tunnel.State.UP)){
                connect_layout.visibility = View.GONE
                loading_layout.visibility = View.GONE
                connected_layout.visibility = View.VISIBLE
                setConnectedUIInformation(connectedTime)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        menu.findItem(R.id.choose_server_btn).setIcon(BitmapDrawable(resources, Countries.bitmaps["best"]))
        return true
    }

    fun ShowToast(context: Context?, txt: String?, time: Int) {
        runOnUiThread {
            Toast.makeText(context,
                    txt, time)
                    .show()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                // The back arrow in the action bar should act the same as the back button.
                drawerLayout.openDrawer(GravityCompat.START)
                true
            }
            R.id.choose_server_btn -> {
                startActivity(Intent(this, ChooseServerActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)

        }
    }

    override fun onSelectedTunnelChanged(oldTunnel: ObservableTunnel?,
                                         newTunnel: ObservableTunnel?) {
        val fragmentManager = supportFragmentManager
        val backStackEntries = fragmentManager.backStackEntryCount
        if (newTunnel == null) {
            // Clear everything off the back stack (all editors and detail fragments).
            fragmentManager.popBackStackImmediate(0, FragmentManager.POP_BACK_STACK_INCLUSIVE)
            return
        }
        if (backStackEntries == 2) {
            // Pop the editor off the back stack to reveal the detail fragment. Use the immediate
            // method to avoid the editor picking up the new tunnel while it is still visible.
            fragmentManager.popBackStackImmediate()
        } else if (backStackEntries == 0) {
            // Create and show a new detail fragment.
            fragmentManager.commit {
                setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                addToBackStack(null)
            }
        }
    }

    fun disconnectRequest(isLogOut: Boolean =false){
        launch {
            //Application.getTunnelManager().setTunnelState(GeneralString.currTunel,Tunnel.State.TOGGLE)
            GeneralString.currTunel.setStateAsync(Tunnel.State.DOWN)
            connect_layout.visibility = View.VISIBLE
            connected_layout.visibility = View.GONE
            curr_selected_tunnel_dur.stop();
        }

        if(isLogOut) {
            runOnUiThread {
                loading_layout_txt.text = ""
                loading_layout.visibility = View.VISIBLE
            }
        }
        val pref1 = getSharedPreferences("key1", Context.MODE_PRIVATE)
        val editor = pref1.edit()
        editor.remove("connect_time").apply()

        val builder = OkHttpClient.Builder()
        val client = builder.build()
        val request: Request = Request.Builder()
                .header("Authorization", "Bearer " + GeneralString.authKey)
                .url(GeneralString.gatewayUrl.toString() + "/api/v1/tunnels/"+GeneralString.connectedTunnelId.toString())
                .delete()
                .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("ini", "failed"+e.message)
                disconnectRequest(isLogOut)
            }

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                Log.e("ini",isLogOut.toString()+"wakwaw")
                if(isLogOut){
                    val pref1 = getSharedPreferences("key1", Context.MODE_PRIVATE)
                    val editor = pref1.edit()
                    editor.remove("authkey").apply()
                    val i = Intent(this@MainActivity, LoginActivity::class.java)
                    i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(i)
                }
            }

        })
    }

    fun setConnectedUIInformation(startTime:Long){
        val endpointStr=GeneralString.currTunel.config?.peers?.get(0)?.endpoint.toString()
        val finalIP = endpointStr.split("[")[1].split(":")[0]
        curr_selected_tunnel_ip_txt.text = finalIP
        curr_selected_tunnel_dur.setBase(startTime);
        curr_selected_tunnel_dur.start();
    }

    fun tryToConnect(){
        launch {
            if (Application.getBackend() is GoBackend) {
                val intent = GoBackend.VpnService.prepare(this@MainActivity)
                if (intent != null) {
                    permissionActivityResultLauncher.launch(intent)
                    GeneralString.askedPermission=true
                    loading_layout.visibility = View.GONE
                    return@launch
                }
            }
            toggleTunnelWithPermissionsResult()
            //Application.getTunnelManager().setTunnelState(GeneralString.currTunel,Tunnel.State.TOGGLE)
            GeneralString.currTunel.setStateAsync(Tunnel.State.UP)
            connectedDuration = SystemClock.elapsedRealtime()
            val pref1 = getSharedPreferences("key1", Context.MODE_PRIVATE)
            val connectTime = pref1.getLong("connect_time",0)
            if(connectTime == 0.toLong()) {
                connect_layout.visibility = View.GONE
                loading_layout.visibility = View.GONE
                connected_layout.visibility = View.VISIBLE
                val pref1 = getSharedPreferences("key1", Context.MODE_PRIVATE)
                val editor = pref1.edit()
                editor.putLong("connect_time", connectedDuration)
                editor.putInt("connected_tunnel", GeneralString.connectedTunnelId)
                editor.apply()
            }else{
                connectedDuration = connectTime
            }
            setConnectedUIInformation(connectedDuration)
        }

    }

    fun RetrieveTunnelInfo(){
        if(GeneralString.askedPermission && GeneralString.currTunelInitialized()){
            GeneralString.askedPermission=false
            tryToConnect()
            return
        }
        loading_layout.visibility = View.VISIBLE
        var serverid=""
        if(GeneralString.selectedTunnel!=-1){
            serverid="?server_id="+GeneralString.selectedTunnel.toString()
        }
        val builder = OkHttpClient.Builder()
        val client = builder.build()
        val requestBody: RequestBody = FormBody.Builder()
                .build()
        val request: Request = Request.Builder()
                .header("Authorization", "Bearer " + GeneralString.authKey)
                .url(GeneralString.gatewayUrl.toString() + "/api/v1/tunnels"+serverid)
                .post(requestBody)
                .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {

            }

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                try {
                    if (response.isSuccessful) {
                        val responseString = response.body()!!.string()
                        val resp = JSONObject(responseString)
                        if (resp.getBoolean("success")) {
                            GeneralString.connectedTunnelId = resp.getInt("tunnel_id")
                            val inputStream: InputStream = resp.getString("tunnel_config").byteInputStream()
                            try {
                                launch {
                                    GeneralString.currTunel = Application.getTunnelManager().create("Best", "best", Config.parse(inputStream))
                                    tryToConnect()
                                }
                            } catch (e: Throwable) {

                            }
                        }
                    }
                } catch (e: IOException) {

                } catch (e: JSONException) {

                }
            }
        })
    }
}
