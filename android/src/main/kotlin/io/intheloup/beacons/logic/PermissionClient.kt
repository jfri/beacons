//  Copyright (c) 2018 Loup Inc.
//  Licensed under Apache License v2.0

package io.intheloup.beacons.logic

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.flutter.plugin.common.PluginRegistry
import io.intheloup.beacons.BeaconsPlugin
import io.intheloup.beacons.data.Permission
import io.intheloup.beacons.data.Result
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class PermissionClient {

    val listener: PluginRegistry.RequestPermissionsResultListener = PluginRegistry.RequestPermissionsResultListener { id, _, grantResults ->
        if (id == BeaconsPlugin.Intents.PermissionRequestId) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                permissionCallbacks.forEach { it.success(Unit) }
            } else {
                permissionCallbacks.forEach { it.failure(Unit) }
            }
            permissionCallbacks.clear()
            return@RequestPermissionsResultListener true
        }

        return@RequestPermissionsResultListener false
    }

    private var activity: Activity? = null
    private val permissionCallbacks = ArrayList<Callback<Unit, Unit>>()


    fun bind(activity: Activity) {
        this.activity = activity
    }

    fun unbind() {
        activity = null
    }

    fun check(permission: Permission): PermissionResult {
        if (!checkGranted()) {
            return PermissionResult.Denied
        }

        return PermissionResult.Granted
    }

    suspend fun request(permission: Permission): PermissionResult = suspendCoroutine { cont ->
        val current = check(permission)
        when (current) {
            is PermissionResult.Granted -> cont.resume(current)
            is PermissionResult.Denied -> {
                val callback = Callback<Unit, Unit>(
                        success = { _ -> cont.resume(PermissionResult.Granted) },
                        failure = { _ -> cont.resume(PermissionResult.Denied) }
                )
                permissionCallbacks.add(callback)
                ActivityCompat.requestPermissions(activity!!, arrayOf(permission.manifestValue), BeaconsPlugin.Intents.PermissionRequestId)
            }
        }

    }

    private fun checkGranted() =
            ContextCompat.checkSelfPermission(activity!!, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(activity!!, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED


    class Callback<in T, in E>(val success: (T) -> Unit, val failure: (E) -> Unit)

    sealed class PermissionResult(val result: Result) {
        object Denied : PermissionResult(Result.failure(Result.Error.Type.PermissionDenied))
        object Granted : PermissionResult(Result.success(true))
    }
}
