/*
 * Copyright © 2021 WireGuard LLC. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.wireguard.android

import com.wireguard.android.model.ObservableTunnel
import kotlinx.coroutines.Deferred

class GeneralString {
    companion object {
        @JvmStatic lateinit var authKey : String
        @JvmStatic var gatewayUrl = "http://68.183.227.131"
        @JvmStatic
        lateinit var currTunel :ObservableTunnel

        @JvmStatic
        fun currTunelInitialized() = ::currTunel.isInitialized
    }
}