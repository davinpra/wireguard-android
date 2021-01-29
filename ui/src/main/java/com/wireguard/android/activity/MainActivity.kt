/*
 * Copyright © 2017-2019 WireGuard LLC. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.activity

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import com.google.android.material.navigation.NavigationView
import com.wireguard.android.Application
import com.wireguard.android.GeneralString
import com.wireguard.android.R
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import com.wireguard.android.model.ObservableTunnel
import com.wireguard.android.util.ErrorMessages
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext


/**
 * CRUD interface for WireGuard tunnels. This activity serves as the main entry point to the
 * WireGuard application, and contains several fragments for listing, viewing details of, and
 * editing the configuration and interface state of WireGuard tunnels.
 */
class MainActivity : BaseActivity(), FragmentManager.OnBackStackChangedListener, CoroutineScope {
    private val permissionActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { toggleTunnelWithPermissionsResult() }

    private var actionBar: ActionBar? = null
    private var isTwoPaneLayout = false

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
        val backStackEntries = supportFragmentManager.backStackEntryCount
        // If the two-pane layout does not have an editor open, going back should exit the app.
        if (isTwoPaneLayout && backStackEntries <= 1) {
            finish()
            return
        }
        // Deselect the current tunnel on navigating back from the detail pane to the one-pane list.
        if (!isTwoPaneLayout && backStackEntries == 1) {
            supportFragmentManager.popBackStack()
            selectedTunnel = null
            return
        }
        super.onBackPressed()
    }

    override fun onBackStackChanged() {
        if (actionBar == null) return
        // Do not show the home menu when the two-pane layout is at the detail view (see above).
        val backStackEntries = supportFragmentManager.backStackEntryCount
        val minBackStackEntries = if (isTwoPaneLayout) 2 else 1
        actionBar!!.setDisplayHomeAsUpEnabled(backStackEntries >= minBackStackEntries)
    }
    var lastConnectClick : Long = 0
    var lastServerClick : Long = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)


        // As we're using a Toolbar, we should retrieve it and set it
        // to be our ActionBar
        val toolbar = findViewById<Toolbar>(R.id.main_toolbar);
        setSupportActionBar(toolbar);

        // Now retrieve the DrawerLayout so that we can set the status bar color.
        // This only takes effect on Lollipop, or when using translucentStatusBar
        // on KitKat.
        val drawerLayout = findViewById<DrawerLayout>(R.id.main_activity_container);
        drawerLayout.setStatusBarBackgroundColor(resources.getColor(R.color.secondary_color));
        val navView = findViewById<NavigationView>(R.id.nav_view);
        navView.setNavigationItemSelectedListener(NavigationView.OnNavigationItemSelectedListener { menuItem ->
            drawerLayout.closeDrawers()
            menuItem.isChecked = true
            when (menuItem.itemId) {
                R.id.feedback_menu_item -> {
                }
                R.id.share_menu_item -> {
                }
                R.id.setting_menu_item -> {
                }
                R.id.faq_menu_item -> {
                }
            }
            true
        })

        supportFragmentManager.addOnBackStackChangedListener(this)
        onBackStackChanged()

        val connectBtn = findViewById<Button>(R.id.connect_btn)
        connectBtn.setOnClickListener(View.OnClickListener {
            if (System.currentTimeMillis() < lastConnectClick + 1000) {
                ShowToast(this@MainActivity, "You tap too fast, please wait", Toast.LENGTH_LONG)
                return@OnClickListener
            }
            lastConnectClick = System.currentTimeMillis()
            if (GeneralString.currTunelInitialized()) {
                launch {
                    if (Application.getBackend() is GoBackend) {
                        val intent = GoBackend.VpnService.prepare(this@MainActivity)
                        if (intent != null) {
                            permissionActivityResultLauncher.launch(intent)
                            return@launch
                        }
                    }
                    toggleTunnelWithPermissionsResult()
                    //Application.getTunnelManager().setTunnelState(GeneralString.currTunel,Tunnel.State.TOGGLE)
                    GeneralString.currTunel.setStateAsync(Tunnel.State.of(!GeneralString.currTunel.state.equals(Tunnel.State.UP)))
                    if (GeneralString.currTunel.state.equals(Tunnel.State.UP)) {
                        connectBtn.text = getString(R.string.btn_txt_connected)
                        //connectBtn.setBackgroundTintList(ColorStateList.valueOf(resources.getColor(R.color.secondary_color)))
                    } else {
                        connectBtn.text = getString(R.string.btn_txt_connect)
                        //connectBtn.setBackgroundTintList(ColorStateList.valueOf(resources.getColor(R.color.primary_color)))
                    }
                }
            } else {
                ShowToast(this@MainActivity, "Please choose a server.", Toast.LENGTH_LONG)
            }
        })
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
                onBackPressed()
                true
            }
            /*
            R.id.menu_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            R.id.choose_server_btn -> {
                startActivity(Intent(this, ChooseServerActivity::class.java))
                true
            }
             */
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
}
