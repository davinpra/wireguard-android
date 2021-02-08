/*
 * Copyright © 2021 WireGuard LLC. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.wireguard.android.activity


import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.wireguard.android.APIService
import com.wireguard.android.GeneralString
import com.wireguard.android.R


class SplashActivity : AppCompatActivity(){

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        val pref1 = getSharedPreferences("key1", Context.MODE_PRIVATE)
        GeneralString.authKey = pref1.getString("authkey", "").toString()
        if (GeneralString.authKey.isEmpty()) {
            startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
        } else {
            APIService.refreshTunnelListData()
            startActivity(Intent(this@SplashActivity, MainActivity::class.java))
        }
    }
}
