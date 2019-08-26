//  Copyright (c) 2018 Loup Inc.
//  Licensed under Apache License v2.0

part of beacons;

class _Channels {
  static const String _loggingTag = 'beacons';

  static const MethodChannel _channel = const MethodChannel('beacons');

  static final StreamsChannel _rangingChannel =
      new StreamsChannel('beacons/ranging');

  static final StreamsChannel _monitoringChannel =
      new StreamsChannel('beacons/monitoring');

  static final StreamsChannel _backgroundMonitoringChannel =
      new StreamsChannel('beacons/backgroundMonitoring');

  Future<BeaconsResult> checkStatus(_StatusRequest request) async {
    final response = await _invokeChannelMethod(
      _loggingTag,
      _channel,
      'checkStatus',
      Codec.encodeStatusRequest(request),
    );
    return Codec.decodeResult(response);
  }

  Future<BeaconsResult> requestPermission(LocationPermission permission) async {
    final response = await _invokeChannelMethod(
      _loggingTag,
      _channel,
      'requestPermission',
      Codec.encodePermission(permission),
    );
    return Codec.decodeResult(response);
  }

  Future<void> configure(BeaconsSettings settings) async {
    await _invokeChannelMethod(
      _loggingTag,
      _channel,
      'configure',
      Codec.encodeSettings(settings),
    );
    return;
  }

  Future<BeaconsResult> startMonitoring(_DataRequest request) async {
    final response = await _invokeChannelMethod(
      _loggingTag,
      _channel,
      'startMonitoring',
      Codec.encodeDataRequest(request),
    );
    return Codec.decodeResult(response);
  }

  Future<void> stopMonitoring(BeaconRegion region) async {
    await _invokeChannelMethod(
      _loggingTag,
      _channel,
      'stopMonitoring',
      Codec.encodeRegion(region),
    );
    return;
  }

  Stream<RangingResult> ranging(_DataRequest request) {
    final String json = Codec.encodeDataRequest(request);
    _log(json, tag: 'ranging');
    return _rangingChannel.receiveBroadcastStream(json).map((data) {
      _log(data, tag: 'ranging');
      return Codec.decodeRangingResult(data);
    });
  }

  Stream<MonitoringResult> monitoring(_DataRequest request) {
    final String json = Codec.encodeDataRequest(request);
    _log(json, tag: 'monitoring');
    return _monitoringChannel.receiveBroadcastStream(json).map((data) {
      _log(data, tag: 'monitoring');
      return Codec.decodeMonitoringResult(data);
    });
  }

  Stream<BackgroundMonitoringEvent> backgroundMonitoringEvents() {
    _log('add listener', tag: 'backgroundMonitoringEvents');
    return _backgroundMonitoringChannel.receiveBroadcastStream().map((data) {
      _log(data, tag: 'backgroundMonitoringEvents');
      return Codec.decodeBackgroundMonitoringEvent(data);
    });
  }
}
