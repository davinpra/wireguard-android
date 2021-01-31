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
    private val faq_title = view.faq_title
    private val faq_text = view.faq_text

    fun bindFAQ(faqData: FAQData) {
        faq_title.text = faqData.title
        faq_text.text = faqData.text
    }
}

class FAQActivity: AppCompatActivity(){
    lateinit var jsonString : String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_faq)
        try {
            var jsonString = applicationContext.assets.open("faq_list.json").bufferedReader().use { it.readText() }

            val klaxon = Klaxon()
            JsonReader(StringReader(jsonString)).use { reader ->
                FAQDataList.data = arrayListOf<FAQData>()
                reader.beginArray {
                    while (reader.hasNext()) {
                        var faqData = klaxon.parse<FAQData>(reader)
                        faqData?.let { FAQDataList.data.add(it) }
                    }
                }
                Log.e("faq", FAQDataList.data.count().toString())
                val faqAdapter = FAQAdapter(FAQDataList.data)

                faq_list.apply {
                    layoutManager = LinearLayoutManager(this@FAQActivity)
                    adapter = faqAdapter
                }
            }
        } catch (ioException: IOException) {
            ioException.printStackTrace()
        }


    }
}