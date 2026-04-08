import 'dart:typed_data';

class InstalledApp {
  const InstalledApp({
    required this.packageName,
    required this.label,
    this.icon,
    this.isSystem = false,
  });

  final String packageName;
  final String label;
  final Uint8List? icon;
  final bool isSystem;
}

class DeviceInfo {
  const DeviceInfo({
    required this.manufacturer,
    required this.brand,
    required this.marketName,
    required this.model,
    this.rawModel = '',
    this.product = '',
    this.fingerprint = '',
    this.display = '',
  });

  final String manufacturer;
  final String brand;
  final String marketName;
  final String model;
  final String rawModel;
  final String product;
  final String fingerprint;
  final String display;

  bool get isPixel {
    final String all = '$manufacturer $brand $marketName $model'.toLowerCase();
    return all.contains('google') || all.contains('pixel');
  }

  bool get isSamsung {
    final String all = '$manufacturer $brand'.toLowerCase();
    return all.contains('samsung');
  }

  bool get isAospDevice {
    final String all =
        '$manufacturer $brand $marketName $model $rawModel $product '
                '$fingerprint $display'
            .toLowerCase();
    const List<String> customRomMarkers = <String>[
      'lineage',
      'evolution',
      'evox',
      'crdroid',
      'pixelos',
      'arrowos',
      'risingos',
      'yaap',
      'derpfest',
      'aosp',
    ];
    return isPixel ||
        all.contains('nothing') ||
        all.contains('motorola') ||
        customRomMarkers.any(all.contains);
  }

  bool get shouldHideLiveUpdatesPromotion => isSamsung || isAospDevice;

  String get label {
    if (marketName.isNotEmpty) return marketName;
    if (model.isNotEmpty) return model;
    if (brand.isNotEmpty) return brand;
    if (manufacturer.isNotEmpty) return manufacturer;
    return 'device';
  }
}

enum PackageMode { all, include, exclude }

extension PackageModeId on PackageMode {
  String get id {
    switch (this) {
      case PackageMode.all:
        return 'all';
      case PackageMode.include:
        return 'include';
      case PackageMode.exclude:
        return 'exclude';
    }
  }

  static PackageMode from(String value) {
    switch (value) {
      case 'include':
        return PackageMode.include;
      case 'exclude':
        return PackageMode.exclude;
      default:
        return PackageMode.all;
    }
  }
}

enum NetworkSpeedDisplayMode { total, upload, download }

const String kDefaultNetworkSpeedUploadPrefix = '▲ ';
const String kDefaultNetworkSpeedDownloadPrefix = '▼ ';

extension NetworkSpeedDisplayModeId on NetworkSpeedDisplayMode {
  String get id {
    switch (this) {
      case NetworkSpeedDisplayMode.total:
        return 'total';
      case NetworkSpeedDisplayMode.upload:
        return 'upload';
      case NetworkSpeedDisplayMode.download:
        return 'download';
    }
  }

  static NetworkSpeedDisplayMode from(String? value) {
    switch (value) {
      case 'upload':
        return NetworkSpeedDisplayMode.upload;
      case 'download':
        return NetworkSpeedDisplayMode.download;
      default:
        return NetworkSpeedDisplayMode.total;
    }
  }
}

enum NetworkSpeedUnit { auto, bytes, kilobytes, megabytes, gigabytes }

const List<NetworkSpeedUnit> kNetworkSpeedUnitValues = <NetworkSpeedUnit>[
  NetworkSpeedUnit.auto,
  NetworkSpeedUnit.bytes,
  NetworkSpeedUnit.kilobytes,
  NetworkSpeedUnit.megabytes,
  NetworkSpeedUnit.gigabytes,
];

extension NetworkSpeedUnitId on NetworkSpeedUnit {
  String get id {
    switch (this) {
      case NetworkSpeedUnit.auto:
        return 'auto';
      case NetworkSpeedUnit.bytes:
        return 'b';
      case NetworkSpeedUnit.kilobytes:
        return 'kb';
      case NetworkSpeedUnit.megabytes:
        return 'mb';
      case NetworkSpeedUnit.gigabytes:
        return 'gb';
    }
  }

  static NetworkSpeedUnit from(String? value) {
    switch (value) {
      case 'b':
        return NetworkSpeedUnit.bytes;
      case 'kb':
        return NetworkSpeedUnit.kilobytes;
      case 'mb':
        return NetworkSpeedUnit.megabytes;
      case 'gb':
        return NetworkSpeedUnit.gigabytes;
      default:
        return NetworkSpeedUnit.auto;
    }
  }
}

class NetworkSpeedUnitSelection {
  const NetworkSpeedUnitSelection._();

  static Set<NetworkSpeedUnit> parse(String? raw) {
    final Set<NetworkSpeedUnit> selected = <NetworkSpeedUnit>{};
    for (final String token in (raw ?? '').split(',')) {
      final NetworkSpeedUnit? unit = tryParse(token.trim());
      if (unit != null) {
        selected.add(unit);
      }
    }
    return selected;
  }

  static String encode(Iterable<NetworkSpeedUnit> units) {
    final Set<NetworkSpeedUnit> selected = units.toSet();
    if (selected.isEmpty) {
      return '';
    }
    if (selected.contains(NetworkSpeedUnit.auto)) {
      return NetworkSpeedUnit.auto.id;
    }
    return kNetworkSpeedUnitValues
        .where(
          (NetworkSpeedUnit unit) =>
              unit != NetworkSpeedUnit.auto && selected.contains(unit),
        )
        .map((NetworkSpeedUnit unit) => unit.id)
        .join(',');
  }

  static bool usesAuto(Set<NetworkSpeedUnit> units) {
    return units.isEmpty || units.contains(NetworkSpeedUnit.auto);
  }

  static NetworkSpeedUnit? tryParse(String? value) {
    switch (value) {
      case 'auto':
        return NetworkSpeedUnit.auto;
      case 'b':
        return NetworkSpeedUnit.bytes;
      case 'kb':
        return NetworkSpeedUnit.kilobytes;
      case 'mb':
        return NetworkSpeedUnit.megabytes;
      case 'gb':
        return NetworkSpeedUnit.gigabytes;
      default:
        return null;
    }
  }
}

enum AppCompactTextSource { title, text }

extension AppCompactTextSourceId on AppCompactTextSource {
  String get id {
    switch (this) {
      case AppCompactTextSource.title:
        return 'title';
      case AppCompactTextSource.text:
        return 'text';
    }
  }

  static AppCompactTextSource from(String? value) {
    switch (value) {
      case 'text':
        return AppCompactTextSource.text;
      default:
        return AppCompactTextSource.title;
    }
  }
}

enum AppNotificationIconSource { notification, app }

extension AppNotificationIconSourceId on AppNotificationIconSource {
  String get id {
    switch (this) {
      case AppNotificationIconSource.notification:
        return 'notification';
      case AppNotificationIconSource.app:
        return 'app';
    }
  }

  static AppNotificationIconSource from(String? value) {
    switch (value) {
      case 'app':
        return AppNotificationIconSource.app;
      default:
        return AppNotificationIconSource.notification;
    }
  }
}

class AppPresentationOverride {
  const AppPresentationOverride({
    this.compactTextSource = AppCompactTextSource.title,
    this.iconSource = AppNotificationIconSource.notification,
  });

  final AppCompactTextSource compactTextSource;
  final AppNotificationIconSource iconSource;

  bool get isDefault =>
      compactTextSource == AppCompactTextSource.title &&
      iconSource == AppNotificationIconSource.notification;

  Map<String, String> toJsonEntry() {
    return <String, String>{
      'compact_text': compactTextSource.id,
      'icon_source': iconSource.id,
    };
  }

  static AppPresentationOverride fromJsonEntry(Map<String, dynamic> json) {
    return AppPresentationOverride(
      compactTextSource: AppCompactTextSourceId.from(
        json['compact_text'] as String?,
      ),
      iconSource: AppNotificationIconSourceId.from(
        json['icon_source'] as String?,
      ),
    );
  }
}
