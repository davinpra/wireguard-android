/*
 * Copyright © 2021 WireGuard LLC. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.wireguard.android.model

class TunnelDataList {
    companion object {
        @JvmStatic lateinit var data: ArrayList<TunnelData>
    }
}
data class TunnelData(val name: String,val location: String, val id: Int)