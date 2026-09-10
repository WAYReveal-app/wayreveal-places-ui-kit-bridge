import 'dart:io';

import 'package:flutter/services.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';
import 'package:wayreveal_places_ui_kit_bridge/wayreveal_places_ui_kit_bridge.dart';
import 'package:wayreveal_places_ui_kit_bridge/wayreveal_places_ui_kit_bridge_platform_interface.dart';

class UnsupportedTestPlatform extends WayrevealPlacesUiKitBridgePlatform
    with MockPlatformInterfaceMixin {}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  test('capabilities serialize through StandardMessageCodec primitives', () {
    const value = PlacesUiKitBridgeCapabilities(
      androidAvailable: true,
      androidEmbeddedSupported: false,
      androidModalSupported: true,
      iosAvailable: false,
      iosEmbeddedSupported: false,
      iosModalSupported: false,
      requiresHostActivityChange: true,
      requiresHostAppDelegateChange: false,
      requiresGeneratedSourcePatch: false,
      iosDependencyTransportSupported: false,
    );
    final encoded = const StandardMessageCodec().encodeMessage(value.toMap());
    final decoded = const StandardMessageCodec().decodeMessage(encoded!);
    final roundTrip = PlacesUiKitBridgeCapabilities.fromMap(
      decoded! as Map<Object?, Object?>,
    );
    expect(roundTrip.toMap(), value.toMap());
  });

  test('channel and platform view names are stable and namespaced', () {
    expect(
      WayrevealPlacesUiKitBridge.methodChannelName,
      'wayreveal_places_ui_kit_bridge/methods',
    );
    expect(
      WayrevealPlacesUiKitBridge.embeddedViewType,
      'wayreveal_places_ui_kit_bridge/place_search',
    );
    expect(
      WayrevealPlacesUiKitBridge.placeDetailsViewType,
      'wayreveal_places_ui_kit_bridge/place_details',
    );
    expect(
      WayrevealPlacesUiKitBridge.selectedPlaceMapViewType,
      'wayreveal_places_ui_kit_bridge/selected_place_map',
    );
  });

  test('callback and error payloads remain primitive', () {
    final loaded = PlacesUiKitBridgeEvent.fromMap(<String, Object?>{
      'type': 'load',
      'count': 2,
    });
    final failed = PlacesUiKitBridgeEvent.fromMap(<String, Object?>{
      'type': 'error',
      'message': 'unavailable',
    });
    expect(loaded.type, 'load');
    expect(loaded.count, 2);
    expect(failed.type, 'error');
    expect(failed.message, 'unavailable');
  });

  test('selection payload keeps legacy state and adds a valid Place ID', () {
    final selected = PlacesUiKitBridgeEvent.fromMap(<String, Object?>{
      'type': 'selected',
      'selected': true,
      'placeId': 'place-id-wayreveal-test',
    });

    expect(selected.type, 'selected');
    expect(selected.selected, isTrue);
    expect(selected.placeId, 'place-id-wayreveal-test');
  });

  test('missing, wrong-type, blank and malformed Place IDs are ignored', () {
    for (final value in <Object?>[
      null,
      42,
      '',
      '   ',
      'contains whitespace',
      'line\nbreak',
    ]) {
      final selected = PlacesUiKitBridgeEvent.fromMap(<String, Object?>{
        'type': 'selected',
        'selected': true,
        'placeId': value,
      });
      expect(selected.selected, isTrue);
      expect(selected.placeId, isNull);
    }
  });

  testWidgets('legacy and Place ID callbacks are additive and defensive', (
    tester,
  ) async {
    var legacySelections = 0;
    final placeIds = <String>[];
    await tester.pumpWidget(
      Directionality(
        textDirection: TextDirection.ltr,
        child: SizedBox(
          width: 320,
          height: 480,
          child: PlacesUiKitEmbeddedProbe(
            onPlaceSelected: () => legacySelections += 1,
            onPlaceIdSelected: placeIds.add,
          ),
        ),
      ),
    );

    Future<void> send(
      Object? arguments, {
      String method = 'onPlaceSearchEvent',
    }) {
      return TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .handlePlatformMessage(
            WayrevealPlacesUiKitBridge.methodChannelName,
            const StandardMethodCodec().encodeMethodCall(
              MethodCall(method, arguments),
            ),
            (_) {},
          );
    }

    await send(<String, Object?>{'type': 'selected', 'selected': true});
    expect(legacySelections, 1);
    expect(placeIds, isEmpty);

    await send(<String, Object?>{
      'type': 'selected',
      'selected': true,
      'placeId': 'place-id-wayreveal-test',
    });
    expect(legacySelections, 2);
    expect(placeIds, <String>['place-id-wayreveal-test']);

    await send(<String, Object?>{
      'type': 'selected',
      'selected': true,
      'placeId': 7,
    });
    await send(null);
    await send('malformed');
    await send(<String, Object?>{'type': 'unknown', 'selected': true});
    await send(<String, Object?>{
      'type': 'selected',
      'selected': true,
    }, method: 'unknown');
    expect(legacySelections, 3);
    expect(placeIds, <String>['place-id-wayreveal-test']);

    await tester.pumpWidget(const SizedBox.shrink());
  });

  test('Android native payload is bounded to approved identity data', () {
    final android = File(
      'android/src/main/kotlin/com/wayreveal/'
      'wayreveal_places_ui_kit_bridge/PlacesUiKitEmbeddedView.kt',
    ).readAsStringSync();

    expect(android, contains('place.id'));
    expect(android, contains('put("placeId", it)'));
    expect(android, contains('"latitude" to place.location?.latitude'));
    expect(android, contains('"longitude" to place.location?.longitude'));
    expect(android, isNot(contains('Place.Field.DISPLAY_NAME')));
    expect(android, isNot(contains('Place.Field.PHONE_NUMBER')));
    expect(android, isNot(contains('Place.Field.RATING')));

    final details = File(
      'android/src/main/kotlin/com/wayreveal/'
      'wayreveal_places_ui_kit_bridge/PlaceDetailsUiKitEmbeddedView.kt',
    ).readAsStringSync();
    expect(details, contains('PlaceDetailsCompactFragment'));
    expect(details, contains('val selectedPlaceId = placeId ?: return'));
    expect(details, contains('loadWithPlaceId(selectedPlaceId)'));
    expect(details, contains('"onPlaceDetailsEvent"'));
    expect(
      'if (disposed) return'.allMatches(details).length,
      greaterThanOrEqualTo(2),
    );
    expect(
      details,
      contains('setPlaceLoadListener(DetachedPlaceLoadListener)'),
    );
    expect(details, isNot(contains('setPlaceLoadListener(null)')));
    expect(details, isNot(contains('Place.Field.DISPLAY_NAME')));
    expect(details, isNot(contains('Place.Field.RATING')));
    expect(details, isNot(contains('place.name')));
    expect(details, isNot(contains('place.address')));

    final map = File(
      'android/src/main/kotlin/com/wayreveal/'
      'wayreveal_places_ui_kit_bridge/SelectedPlaceMapEmbeddedView.kt',
    ).readAsStringSync();
    expect(map, contains('MapView(context)'));
    expect(map, contains('params["latitude"]'));
    expect(map, contains('params["longitude"]'));
    expect(map, contains('MarkerOptions().position(selectedLocation)'));
    expect(map, contains('CameraUpdateFactory.newLatLngZoom'));
    expect(map, contains('map.uiSettings.isZoomControlsEnabled = true'));
    expect(map, isNot(contains('Place.Field.')));
    expect(map, isNot(contains('DISPLAY_NAME')));
    final gradle = File('android/build.gradle').readAsStringSync();
    expect(
      gradle,
      contains(
        'implementation("com.google.android.gms:play-services-maps:20.0.0")',
      ),
    );
  });

  test('Android embedded fragment ownership is attach-driven and lossless', () {
    final android = File(
      'android/src/main/kotlin/com/wayreveal/'
      'wayreveal_places_ui_kit_bridge/PlacesUiKitEmbeddedView.kt',
    ).readAsStringSync();
    final activity = File(
      'android/src/main/kotlin/com/wayreveal/'
      'wayreveal_places_ui_kit_bridge/WayRevealFlutterFragmentActivity.kt',
    ).readAsStringSync();

    expect(android, contains('container.addOnAttachStateChangeListener'));
    expect(android, contains('onViewAttachedToWindow'));
    expect(android, contains('.add(container, fragment, fragmentTag)'));
    expect(android, contains('findFragmentByTag(fragmentTag)'));
    expect(android, contains('unregisterListener()'));
    expect(android, contains('listenerFragment = null'));
    expect(android, contains('isExpectedUninitializedPlacesCleanup'));
    expect(android, contains('Places.isInitialized()'));
    expect(android, contains('Places must be initialized first'));
    expect(android, isNot(contains('commitNowAllowingStateLoss')));
    expect(android, isNot(contains('.replace(')));
    expect(activity, contains('prepareForStateSave(this)'));
    expect(activity, contains('prepareForHostPause(this)'));
    expect(activity, contains('onHostPostResume(this)'));
    expect(activity, contains('PlaceDetailsUiKitFragmentOwnerRegistry'));
  });

  test('embedded and modal capability reporting are independent', () {
    const value = PlacesUiKitBridgeCapabilities(
      androidAvailable: true,
      androidEmbeddedSupported: false,
      androidModalSupported: true,
      iosAvailable: true,
      iosEmbeddedSupported: true,
      iosModalSupported: true,
      requiresHostActivityChange: true,
      requiresHostAppDelegateChange: false,
      requiresGeneratedSourcePatch: false,
      iosDependencyTransportSupported: true,
    );
    expect(value.androidEmbeddedSupported, isFalse);
    expect(value.androidModalSupported, isTrue);
    expect(value.iosEmbeddedSupported, isTrue);
  });

  test('unsupported platforms fail explicitly', () async {
    final previous = WayrevealPlacesUiKitBridgePlatform.instance;
    addTearDown(() => WayrevealPlacesUiKitBridgePlatform.instance = previous);
    WayrevealPlacesUiKitBridgePlatform.instance = UnsupportedTestPlatform();
    expect(
      () => const WayrevealPlacesUiKitBridge().getCapabilities(),
      throwsA(isA<UnsupportedError>()),
    );
  });

  test('production bridge sources contain no secrets or production data', () {
    final sources = <String>[
      'lib',
      'android/src',
      'pubspec.yaml',
    ].expand(_readProductionSources).join('\n');

    expect(RegExp(r'AIza[0-9A-Za-z_-]{20,}').hasMatch(sources), isFalse);
    expect(RegExp(r'ChIJ[0-9A-Za-z_-]+').hasMatch(sources), isFalse);
    expect(sources.toLowerCase(), isNot(contains('supabase')));
    expect(sources, isNot(contains('tourist-plate-login-378tqo')));
    expect(sources, isNot(contains('redeem_access_code')));
    expect(sources, isNot(contains('pendingAccessCode')));
    expect(sources, isNot(contains('harbor-table-sample')));
  });
}

Iterable<String> _readProductionSources(String path) sync* {
  final type = FileSystemEntity.typeSync(path);
  if (type == FileSystemEntityType.file) {
    yield File(path).readAsStringSync();
    return;
  }
  if (type != FileSystemEntityType.directory) return;
  for (final entity in Directory(path).listSync(recursive: true)) {
    if (entity is File) yield entity.readAsStringSync();
  }
}
