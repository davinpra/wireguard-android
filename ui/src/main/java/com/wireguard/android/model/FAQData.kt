/*
 * Copyright © 2021 WireGuard LLC. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.wireguard.android.model

class FAQDataList {
    companion object {
        @JvmStatic lateinit var data: ArrayList<FAQData>
    }
}
data class FAQData(val title: String, val text: String)