/*
 * Copyright © 2021 WireGuard LLC. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.wireguard.android.activity

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.beust.klaxon.JsonReader
import com.beust.klaxon.Klaxon
import com.wireguard.android.R
import com.wireguard.android.model.FAQData
import com.wireguard.android.model.FAQDataList
import kotlinx.android.synthetic.main.activity_faq.*
import java.io.IOException
import java.io.StringReader
import kotlinx.android.synthetic.main.faq_item.view.*

class FAQAdapter(private val faqdata: List<FAQData>) : RecyclerView.Adapter<FAQDataHolder>() {

    override fun onCreateViewHolder(viewGroup: ViewGroup, p1: Int): FAQDataHolder {
        return FAQDataHolder(LayoutInflater.from(viewGroup.context).inflate(R.layout.faq_item, viewGroup, false))
    }

    override fun getItemCount(): Int = faqdata.size

    override fun onBindViewHolder(holder: FAQDataHolder, position: Int) {
        holder.bindFAQ(faqdata[position])
    }
}

class FAQDataHolder(view: View) : RecyclerView.ViewHolder(view) {
    private val thisView = view

    fun bindFAQ(faqData: FAQData) {
        thisView.faq_title.text = faqData.title
        thisView.faq_text.text = faqData.text
        thisView.faq_icon.setColorFilter(R.color.secondary_color)
        thisView.faq_text.visibility=View.GONE
        thisView.setOnClickListener(View.OnClickListener {
            if(thisView.faq_text.visibility==View.VISIBLE){
                thisView.faq_icon.setColorFilter(R.color.secondary_color)
                thisView.faq_text.visibility=View.GONE
            }else{
                thisView.faq_icon.setColorFilter(R.color.dark_gray)
                thisView.faq_text.visibility=View.VISIBLE
            }
        })
    }
}

class FAQActivity: AppCompatActivity(){
    lateinit var jsonString : String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_faq)

        val faqAdapter = FAQAdapter(FAQDataList.data)

        faq_list.apply {
            layoutManager = LinearLayoutManager(this@FAQActivity)
            adapter = faqAdapter
        }


    }
}