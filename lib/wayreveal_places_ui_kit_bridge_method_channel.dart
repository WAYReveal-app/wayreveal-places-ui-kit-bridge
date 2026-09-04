import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'wayreveal_places_ui_kit_bridge.dart';
import 'wayreveal_places_ui_kit_bridge_platform_interface.dart';

class MethodChannelWayrevealPlacesUiKitBridge
    extends WayrevealPlacesUiKitBridgePlatform {
  @visibleForTesting
  final methodChannel = const MethodChannel(
    WayrevealPlacesUiKitBridge.methodChannelName,
    StandardMethodCodec(),
  );

  @override
  Future<PlacesUiKitBridgeCapabilities> getCapabilities() async {
    final value = await methodChannel.invokeMethod<Map<Object?, Object?>>(
      'getCapabilities',
    );
    if (value == null) {
      throw const FormatException('Native capability response was null.');
    }
    return PlacesUiKitBridgeCapabilities.fromMap(value);
  }

  @override
  Future<Map<String, Object?>> probeLifecycle() async {
    final value = await methodChannel.invokeMethod<Map<Object?, Object?>>(
      'probeLifecycle',
    );
    return _stringKeyed(value);
  }

  @override
  Future<Map<String, Object?>> openModalProbe() async {
    final value = await methodChannel.invokeMethod<Map<Object?, Object?>>(
      'openModalProbe',
    );
    return _stringKeyed(value);
  }

  static Map<String, Object?> _stringKeyed(Map<Object?, Object?>? value) {
    if (value == null) return const <String, Object?>{};
    return <String, Object?>{
      for (final entry in value.entries)
        if (entry.key is String) entry.key! as String: entry.value,
    };
  }
}
