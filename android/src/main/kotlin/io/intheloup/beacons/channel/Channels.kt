//  Copyright (c) 2018 Loup Inc.
//  Licensed under Apache License v2.0

package io.intheloup.beacons.channel

import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.intheloup.beacons.BeaconsPlugin
import io.intheloup.beacons.data.Permission
import io.intheloup.beacons.data.RegionModel
import io.intheloup.beacons.data.Settings
import io.intheloup.beacons.logic.BeaconsClient
import io.intheloup.beacons.logic.SharedMonitor
import io.intheloup.streamschannel.StreamsChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class Channels(private val beaconsClient: BeaconsClient) : MethodChannel.MethodCallHandler {

    fun register(plugin: BeaconsPlugin) {
        val methodChannel = MethodChannel(plugin.registrar.messenger(), "beacons")
        methodChannel.setMethodCallHandler(this)

        val rangingChannel = StreamsChannel(plugin.registrar.messenger(), "beacons/ranging")
        rangingChannel.setStreamHandlerFactory { Handler(beaconsClient, BeaconsClient.Operation.Kind.Ranging) }

        val monitoringChannel = StreamsChannel(plugin.registrar.messenger(), "beacons/monitoring")
        monitoringChannel.setStreamHandlerFactory { Handler(beaconsClient, BeaconsClient.Operation.Kind.Monitoring) }

        val backgroundMonitoringChannel = StreamsChannel(plugin.registrar.messenger(), "beacons/backgroundMonitoring")
        backgroundMonitoringChannel.setStreamHandlerFactory { BackgroundMonitoringHandler(beaconsClient) }
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result): Unit {
        when (call.method) {
            "configure" -> configure(Codec.decodeSettings(call.arguments), result)
            "startMonitoring" -> startMonitoring(Codec.decodeDataRequest(call.arguments), result)
            "stopMonitoring" -> stopMonitoring(Codec.decodeRegion(call.arguments), result)
            else -> result.notImplemented()
        }
    }

    private fun configure(settings: Settings, result: MethodChannel.Result) {
        beaconsClient.configure(settings)
        result.success(null)
    }

    private fun startMonitoring(request: DataRequest, result: MethodChannel.Result) {
        GlobalScope.launch(Dispatchers.Main) {
            result.success(beaconsClient.startMonitoring(request))
        }
    }

    private fun stopMonitoring(region: RegionModel, result: MethodChannel.Result) {
        beaconsClient.stopMonitoring(region)
        result.success(null)
    }

    class Handler(private val beaconsClient: BeaconsClient,
                  private val kind: BeaconsClient.Operation.Kind) : EventChannel.StreamHandler {

        private var request: BeaconsClient.Operation? = null

        override fun onListen(arguments: Any?, eventSink: EventChannel.EventSink) {
            val dataRequest = Codec.decodeDataRequest(arguments)
            request = BeaconsClient.Operation(kind, dataRequest.region, dataRequest.inBackground) { result ->
                eventSink.success(Codec.encodeResult(result))
            }
            beaconsClient.addRequest(request!!, dataRequest.permission)
        }

        override fun onCancel(arguments: Any?) {
            beaconsClient.removeRequest(request!!)
            request = null
        }
    }

    class BackgroundMonitoringHandler(private val beaconsClient: BeaconsClient) : EventChannel.StreamHandler {

        private var listener: SharedMonitor.BackgroundListener? = null

        override fun onListen(arguments: Any?, eventSink: EventChannel.EventSink) {
            listener = SharedMonitor.BackgroundListener { result ->
                eventSink.success(Codec.encodeBackgroundMonitoringEvent(result))
            }
            beaconsClient.addBackgroundMonitoringListener(listener!!)
        }

        override fun onCancel(arguments: Any?) {
            beaconsClient.removeBackgroundMonitoringListener(listener!!)
            listener = null
        }
    }
}
