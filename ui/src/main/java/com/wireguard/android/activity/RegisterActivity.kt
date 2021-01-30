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
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
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


class RegisterActivity : AppCompatActivity(){

    var lastRegisterClick: Long = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        val registerBtn = findViewById<Button>(R.id.RegisterBtn)
        registerBtn.setOnClickListener(View.OnClickListener {
            if (System.currentTimeMillis() < lastRegisterClick + 2000) {
                return@OnClickListener
            }
            lastRegisterClick = System.currentTimeMillis()
            Register()
        })
    }

    fun Register() {
        val nameTxt = findViewById<EditText>(R.id.nameInputTxt)
        val emailTxt = findViewById<EditText>(R.id.emailInputTxt)
        val usernameTxt = findViewById<EditText>(R.id.usernameInputTxt)
        val passwordTxt = findViewById<EditText>(R.id.passwordInputTxt)
        val name = nameTxt.text.toString()
        val email = emailTxt.text.toString()
        val username = usernameTxt.text.toString()
        val password = passwordTxt.text.toString()
        if (!name.isEmpty() && !email.isEmpty() && !username.isEmpty() && !password.isEmpty()) {
            val builder = OkHttpClient.Builder()
            val client = builder.build()
            val requestBody: RequestBody = FormBody.Builder()
                    .add("name", name)
                    .add("email", email)
                    .add("username", username)
                    .add("password", password)
                    .build()
            val request: Request = Request.Builder()
                    .url(GeneralString.gatewayUrl.toString() + "/api/v1/registrations")
                    .post(requestBody)
                    .build()
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Toast.makeText(this@RegisterActivity, "Request Failed." + e.message, Toast.LENGTH_LONG).show()
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
                                startActivity(Intent(this@RegisterActivity, MainActivity::class.java))
                            } else {
                                ShowToast(this@RegisterActivity, "Something wrong happened, please try again later.", Toast.LENGTH_LONG)
                            }
                        } else {
                            ShowToast(this@RegisterActivity, "Request Failed.$response", Toast.LENGTH_LONG)
                            //Log.d("debuging", "Error "+ response);
                        }
                    } catch (e: IOException) {
                        ShowToast(this@RegisterActivity, "Request Failed." + e.message, Toast.LENGTH_LONG)
                        //Log.d("debuging", "Exception caught : ", e);
                    } catch (e: JSONException) {
                        ShowToast(this@RegisterActivity, "Request Failed." + e.message, Toast.LENGTH_LONG)
                    }
                }
            })
        } else {
            ShowToast(this, "Please fill all the fields!", Toast.LENGTH_LONG)
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