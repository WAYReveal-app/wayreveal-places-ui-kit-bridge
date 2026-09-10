import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:flutter/widgets.dart';

import 'wayreveal_places_ui_kit_bridge_platform_interface.dart';

@immutable
class PlacesUiKitBridgeCapabilities {
  const PlacesUiKitBridgeCapabilities({
    required this.androidAvailable,
    required this.androidEmbeddedSupported,
    required this.androidModalSupported,
    required this.iosAvailable,
    required this.iosEmbeddedSupported,
    required this.iosModalSupported,
    required this.requiresHostActivityChange,
    required this.requiresHostAppDelegateChange,
    required this.requiresGeneratedSourcePatch,
    required this.iosDependencyTransportSupported,
  });

  factory PlacesUiKitBridgeCapabilities.fromMap(Map<Object?, Object?> map) {
    bool read(String key) => map[key] == true;
    return PlacesUiKitBridgeCapabilities(
      androidAvailable: read('androidAvailable'),
      androidEmbeddedSupported: read('androidEmbeddedSupported'),
      androidModalSupported: read('androidModalSupported'),
      iosAvailable: read('iosAvailable'),
      iosEmbeddedSupported: read('iosEmbeddedSupported'),
      iosModalSupported: read('iosModalSupported'),
      requiresHostActivityChange: read('requiresHostActivityChange'),
      requiresHostAppDelegateChange: read('requiresHostAppDelegateChange'),
      requiresGeneratedSourcePatch: read('requiresGeneratedSourcePatch'),
      iosDependencyTransportSupported: read('iosDependencyTransportSupported'),
    );
  }

  final bool androidAvailable;
  final bool androidEmbeddedSupported;
  final bool androidModalSupported;
  final bool iosAvailable;
  final bool iosEmbeddedSupported;
  final bool iosModalSupported;
  final bool requiresHostActivityChange;
  final bool requiresHostAppDelegateChange;
  final bool requiresGeneratedSourcePatch;
  final bool iosDependencyTransportSupported;

  Map<String, bool> toMap() => <String, bool>{
    'androidAvailable': androidAvailable,
    'androidEmbeddedSupported': androidEmbeddedSupported,
    'androidModalSupported': androidModalSupported,
    'iosAvailable': iosAvailable,
    'iosEmbeddedSupported': iosEmbeddedSupported,
    'iosModalSupported': iosModalSupported,
    'requiresHostActivityChange': requiresHostActivityChange,
    'requiresHostAppDelegateChange': requiresHostAppDelegateChange,
    'requiresGeneratedSourcePatch': requiresGeneratedSourcePatch,
    'iosDependencyTransportSupported': iosDependencyTransportSupported,
  };
}

class PlacesUiKitBridgeEvent {
  const PlacesUiKitBridgeEvent({
    required this.type,
    this.count,
    this.message,
    this.selected = false,
    this.placeId,
  });

  factory PlacesUiKitBridgeEvent.fromMap(Map<Object?, Object?> map) {
    return PlacesUiKitBridgeEvent(
      type: map['type'] as String? ?? 'unknown',
      count: map['count'] as int?,
      message: map['message'] as String?,
      selected: map['selected'] == true,
      placeId: _readPlaceId(map['placeId']),
    );
  }

  final String type;
  final int? count;
  final String? message;
  final bool selected;
  final String? placeId;

  static String? _readPlaceId(Object? value) {
    if (value is! String) return null;
    final candidate = value.trim();
    if (candidate.isEmpty) return null;
    if (candidate.runes.any((codePoint) => codePoint <= 0x20)) return null;
    return candidate;
  }
}

class WayrevealPlacesUiKitBridge {
  const WayrevealPlacesUiKitBridge();

  static const String methodChannelName =
      'wayreveal_places_ui_kit_bridge/methods';
  static const String embeddedViewType =
      'wayreveal_places_ui_kit_bridge/place_search';
  static const String placeDetailsViewType =
      'wayreveal_places_ui_kit_bridge/place_details';
  static const String selectedPlaceMapViewType =
      'wayreveal_places_ui_kit_bridge/selected_place_map';

  Future<PlacesUiKitBridgeCapabilities> getCapabilities() =>
      WayrevealPlacesUiKitBridgePlatform.instance.getCapabilities();

  Future<Map<String, Object?>> probeLifecycle() =>
      WayrevealPlacesUiKitBridgePlatform.instance.probeLifecycle();

  Future<Map<String, Object?>> openModalProbe() =>
      WayrevealPlacesUiKitBridgePlatform.instance.openModalProbe();
}

/// Native embedded host probe. It never configures or sends a Places request.
class PlacesUiKitEmbeddedProbe extends StatefulWidget {
  const PlacesUiKitEmbeddedProbe({
    super.key,
    this.onPlaceSelected,
    this.onPlaceIdSelected,
  });

  /// Backwards-compatible callback fired for every valid selection event.
  final VoidCallback? onPlaceSelected;

  /// Additive callback fired only when native code supplies a valid Place ID.
  final ValueChanged<String>? onPlaceIdSelected;

  @override
  State<PlacesUiKitEmbeddedProbe> createState() =>
      _PlacesUiKitEmbeddedProbeState();
}

class _PlacesUiKitEmbeddedProbeState extends State<PlacesUiKitEmbeddedProbe> {
  static const MethodChannel _channel = MethodChannel(
    WayrevealPlacesUiKitBridge.methodChannelName,
    StandardMethodCodec(),
  );

  @override
  void initState() {
    super.initState();
    _channel.setMethodCallHandler(_handleNativeCall);
  }

  Future<void> _handleNativeCall(MethodCall call) async {
    if (call.method != 'onPlaceSearchEvent' || call.arguments is! Map) return;
    final event = PlacesUiKitBridgeEvent.fromMap(
      Map<Object?, Object?>.from(call.arguments as Map),
    );
    if (event.type != 'selected' || !event.selected) return;

    widget.onPlaceSelected?.call();
    final placeId = event.placeId;
    if (placeId != null) widget.onPlaceIdSelected?.call(placeId);
  }

  @override
  void dispose() {
    _channel.setMethodCallHandler(null);
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    const creationParams = <String, Object?>{'mode': 'preBillingProbe'};
    switch (defaultTargetPlatform) {
      case TargetPlatform.android:
        return const AndroidView(
          viewType: WayrevealPlacesUiKitBridge.embeddedViewType,
          creationParams: creationParams,
          creationParamsCodec: StandardMessageCodec(),
        );
      default:
        return const SizedBox.shrink();
    }
  }
}
