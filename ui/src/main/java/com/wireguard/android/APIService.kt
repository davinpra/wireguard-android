/*
 * Copyright © 2021 WireGuard LLC. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.wireguard.android

import android.util.Log
import android.widget.Toast
import com.wireguard.android.activity.ChooseServerActivity
import com.wireguard.android.model.TunnelData
import com.wireguard.android.model.TunnelDataList
import kotlinx.android.synthetic.main.activity_choose_tunnel.*
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

class APIService {
    companion object {

        fun refreshTunnelListData() {

            TunnelDataList.data = arrayListOf()
            TunnelDataList.data.add(TunnelData("Best", "Best", -1))

            val builder = OkHttpClient.Builder()
            val client = builder.build()
            val requestBody: RequestBody = FormBody.Builder()
                    .build()
            val request: Request = Request.Builder()
                    .header("Authorization", "Bearer " + GeneralString.authKey)
                    .url(GeneralString.gatewayUrl.toString() + "/api/v1/servers")
                    .get()
                    .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    //Log.d("debuging", "Request Failed."+e.getMessage());
                }

                @Throws(IOException::class)
                override fun onResponse(call: Call, response: Response) {
                    try {

                        if (response.isSuccessful) {
                            val responseString = response.body()!!.string()
                            val resp = JSONObject(responseString)
                            if (resp.getJSONArray("servers").length() > 0) {
                                for (i in 0 until resp.getJSONArray("servers").length()) {
                                    val id = resp.getJSONArray("servers").getJSONObject(i).getInt("id")
                                    val name = resp.getJSONArray("servers").getJSONObject(i).getString("name")
                                    val location = resp.getJSONArray("servers").getJSONObject(i).getString("location")
                                    TunnelDataList.data.add(TunnelData(name, location, id))
                                }
                                if(ChooseServerActivity.currInitialized()){
                                    ChooseServerActivity.curr.buildRecyclerView()
                                    ChooseServerActivity.curr.swipe_container.isRefreshing=false
                                }
                            }
                        } else {
                            //Log.d("debuging", "Error "+ response);
                        }
                    } catch (e: IOException) {
                    }
                }
            })
        }
    }
}