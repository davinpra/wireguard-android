/*
 * Copyright © 2021 WireGuard LLC. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.wireguard.android.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.wireguard.android.APIService
import com.wireguard.android.GeneralString
import com.wireguard.android.R
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
import kotlin.jvm.Throws


class LoginActivity : AppCompatActivity(){

    var lastRegisterClick: Long = 0
    var lastLoginClick: Long = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        val registerTxt = findViewById<View>(R.id.registerTxtBtn) as TextView
        registerTxt.setOnClickListener(View.OnClickListener {
            if (System.currentTimeMillis() < lastRegisterClick + 2000) {
                return@OnClickListener
            }
            lastRegisterClick = System.currentTimeMillis()
            startActivity(Intent(this@LoginActivity, RegisterActivity::class.java))
        })

        val loginBtn = findViewById<Button>(R.id.LoginBtn)
        loginBtn.setOnClickListener(View.OnClickListener {
            if (System.currentTimeMillis() < lastLoginClick + 2000) {
                return@OnClickListener
            }
            lastLoginClick = System.currentTimeMillis()
            Login()
        })
    }

    fun Login() {
        val usernameTxt = findViewById<EditText>(R.id.usernameInputTxt)
        val passwordTxt = findViewById<EditText>(R.id.passwordInputTxt)
        val username = usernameTxt.text.toString()
        val password = passwordTxt.text.toString()
        if (!username.isEmpty() && !password.isEmpty()) {
            val builder = OkHttpClient.Builder()
            val client = builder.build()
            val requestBody: RequestBody = FormBody.Builder()
                    .add("username", username)
                    .add("password", password)
                    .build()
            val request: Request = Request.Builder()
                    .url(GeneralString.gatewayUrl.toString() + "/api/v1/sessions")
                    .post(requestBody)
                    .build()
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    ShowToast(this@LoginActivity, "Request Failed." + e.message, Toast.LENGTH_LONG)
                    //Log.d("debuging", "Request Failed."+e.getMessage());
                }

                @Throws(IOException::class)
                override fun onResponse(call: Call, response: Response) {
                    try {
                        if (response.isSuccessful) {
                            val responseString = response.body()!!.string()
                            val resp = JSONObject(responseString)
                            if (!resp.getString("access_token").isEmpty()) {
                                GeneralString.authKey = resp.getString("access_token")
                                val pref1 = getSharedPreferences("key1", Context.MODE_PRIVATE)
                                val editor = pref1.edit()
                                editor.putString("authkey", GeneralString.authKey).apply()
                                APIService.refreshTunnelListData()
                                startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                            } else {
                                ShowToast(this@LoginActivity, "Wrong Username or Password, please try again.", Toast.LENGTH_LONG)
                            }
                        } else {
                            ShowToast(this@LoginActivity, "Request Failed.$response", Toast.LENGTH_LONG)
                            //Log.d("debuging", "Error "+ response);
                        }
                    } catch (e: IOException) {
                        ShowToast(this@LoginActivity, "Request Failed." + e.message, Toast.LENGTH_LONG)
                        //Log.d("debuging", "Exception caught : ", e);
                    } catch (e: JSONException) {
                        ShowToast(this@LoginActivity, "Request Failed." + e.message, Toast.LENGTH_LONG)
                    }
                }
            })
        } else {
            ShowToast(this,
                    "Please enter the credentials!", Toast.LENGTH_LONG)
        }
    }

    fun ShowToast(context: Context?, txt: String?, time: Int) {
        runOnUiThread {
            Toast.makeText(context,
                    txt, time)
                    .show()
        }
    }
}