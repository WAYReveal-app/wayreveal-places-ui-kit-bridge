import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'wayreveal_places_ui_kit_bridge.dart';
import 'wayreveal_places_ui_kit_bridge_method_channel.dart';

abstract class WayrevealPlacesUiKitBridgePlatform extends PlatformInterface {
  WayrevealPlacesUiKitBridgePlatform() : super(token: _token);

  static final Object _token = Object();
  static WayrevealPlacesUiKitBridgePlatform _instance =
      MethodChannelWayrevealPlacesUiKitBridge();

  static WayrevealPlacesUiKitBridgePlatform get instance => _instance;

  static set instance(WayrevealPlacesUiKitBridgePlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<PlacesUiKitBridgeCapabilities> getCapabilities() {
    throw UnsupportedError('Places UI Kit is not available on this platform.');
  }

  Future<Map<String, Object?>> probeLifecycle() {
    throw UnsupportedError('Places UI Kit is not available on this platform.');
  }

  Future<Map<String, Object?>> openModalProbe() {
    throw UnsupportedError('Places UI Kit is not available on this platform.');
  }
}
