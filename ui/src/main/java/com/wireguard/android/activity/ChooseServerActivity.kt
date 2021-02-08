/*
 * Copyright © 2021 WireGuard LLC. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.wireguard.android.activity

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wireguard.android.APIService
import com.wireguard.android.GeneralString
import com.wireguard.android.R
import com.wireguard.android.model.TunnelData
import com.wireguard.android.model.TunnelDataList
import com.wireguard.android.util.Countries
import kotlinx.android.synthetic.main.activity_choose_tunnel.*
import kotlinx.android.synthetic.main.activity_faq.*
import kotlinx.android.synthetic.main.faq_item.view.*
import kotlinx.android.synthetic.main.tunnel_selection_list_item.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext


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
        thisView.tunnel_name.text = tunnelData.location
        thisView.flag_img.setImageBitmap(Countries.bitmaps["best"])
        if(tunnelData.id==GeneralString.selectedTunnel){
            thisView.setBackgroundColor(thisView.context.resources.getColor(R.color.light_gray))
        }else{
            thisView.setBackgroundColor(thisView.context.resources.getColor(R.color.white))
        }
        thisView.setOnClickListener(View.OnClickListener {
            GeneralString.selectedTunnel = tunnelData.id
            thisView.setBackgroundColor(thisView.context.resources.getColor(R.color.light_gray))
            (thisView.context as Activity).finish()
        })
    }
}

class ChooseServerActivity : AppCompatActivity(), CoroutineScope {

    companion object {
        @JvmStatic
        lateinit var curr: ChooseServerActivity
        @JvmStatic
        fun currInitialized() = ::curr.isInitialized
    }
    private var job: Job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        curr=this
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_choose_tunnel)
        buildRecyclerView()
        swipe_container.setOnRefreshListener {
            APIService.refreshTunnelListData()
        }
    }

    fun buildRecyclerView(){
        runOnUiThread {

            // Stuff that updates the UI
            server_available_list.apply {
                layoutManager = LinearLayoutManager(this@ChooseServerActivity)
                adapter = TunnelListAdapter(TunnelDataList.data)
            }
        }
    }

}