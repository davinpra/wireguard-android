/*
 * Copyright © 2021 WireGuard LLC. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.wireguard.android.activity

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.wireguard.android.Application
import com.wireguard.android.GeneralString
import com.wireguard.android.R
import com.wireguard.android.model.FAQData
import com.wireguard.android.model.ObservableTunnel
import com.wireguard.android.model.TunnelData
import com.wireguard.config.Config
import kotlinx.android.synthetic.main.faq_item.view.*
import kotlinx.android.synthetic.main.tunnel_selection_list_item.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
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
import kotlin.jvm.Throws

class TunnelListAdapter(private val tunneldata: List<TunnelData>) : RecyclerView.Adapter<TunnelListHolder>() {

    override fun onCreateViewHolder(viewGroup: ViewGroup, p1: Int): TunnelListHolder {
        return TunnelListHolder(LayoutInflater.from(viewGroup.context).inflate(R.layout.tunnel_selection_list_item, viewGroup, false))
    }

    override fun getItemCount(): Int = tunneldata.size

    override fun onBindViewHolder(holder: TunnelListHolder, position: Int) {
        holder.bindTunnel(tunneldata[position])
    }
}

class TunnelListHolder(view: View) : RecyclerView.ViewHolder(view) {
    private val thisView = view

    fun bindTunnel(tunnelData: TunnelData) {
        thisView.tunnel_name.text = tunnelData.name

        thisView.setOnClickListener(View.OnClickListener {
            GeneralString.selectedTunnel = tunnelData.id
        })
    }
}

class ChooseServerActivity : AppCompatActivity(), CoroutineScope {

    private var job: Job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_choose_tunnel)
        val builder = OkHttpClient.Builder()
        val client = builder.build()
        val requestBody: RequestBody = FormBody.Builder()
                .build()
        val request: Request = Request.Builder()
                .header("Authorization", "Bearer " + GeneralString.authKey)
                .url(GeneralString.gatewayUrl.toString() + "/api/v1/tunnels")
                .post(requestBody)
                .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                ShowToast(this@ChooseServerActivity, "Request Failed." + e.message, Toast.LENGTH_LONG)
                //Log.d("debuging", "Request Failed."+e.getMessage());
            }

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                try {
                    if (response.isSuccessful) {
                        val responseString = response.body()!!.string()
                        val resp = JSONObject(responseString)
                        if (!resp.getBoolean("success")) {
                            ShowToast(this@ChooseServerActivity, "Wrong Username or Password, please try again.", Toast.LENGTH_LONG)
                        } else {

                            val inputStream: InputStream = resp.getString("tunnel_config").byteInputStream()
                            try {
                                launch {
                                    Application.getTunnelManager().create("Best","best", Config.parse(inputStream))
                                }
                            } catch (e: Throwable) {
                                ShowToast(this@ChooseServerActivity, "Request Failed." + e.message, Toast.LENGTH_LONG)
                            }
                        }
                    } else {
                        ShowToast(this@ChooseServerActivity, "Request Failed.$response", Toast.LENGTH_LONG)
                        //Log.d("debuging", "Error "+ response);
                    }
                } catch (e: IOException) {
                    ShowToast(this@ChooseServerActivity, "Request Failed." + e.message, Toast.LENGTH_LONG)
                    //Log.d("debuging", "Exception caught : ", e);
                } catch (e: JSONException) {
                    ShowToast(this@ChooseServerActivity, "Request Failed." + e.message, Toast.LENGTH_LONG)
                }
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

}