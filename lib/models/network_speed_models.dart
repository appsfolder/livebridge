class NetworkSpeedSettings {
  const NetworkSpeedSettings({
    this.enabled = false,
    this.displayMode = NetworkSpeedDisplayMode.total,
    this.uploadPrefix = defaultUploadPrefix,
    this.downloadPrefix = defaultDownloadPrefix,
    this.prioritizeUploadSpeed = true,
    this.chipBackgroundDisabled = false,
    this.units = const <NetworkSpeedUnit>{NetworkSpeedUnit.auto},
  });

  static const String defaultUploadPrefix = '\u25B2 ';
  static const String defaultDownloadPrefix = '\u25BC ';

  final bool enabled;
  final NetworkSpeedDisplayMode displayMode;
  final String uploadPrefix;
  final String downloadPrefix;
  final bool prioritizeUploadSpeed;
  final bool chipBackgroundDisabled;
  final Set<NetworkSpeedUnit> units;

  NetworkSpeedSettings copyWith({
    bool? enabled,
    NetworkSpeedDisplayMode? displayMode,
    String? uploadPrefix,
    String? downloadPrefix,
    bool? prioritizeUploadSpeed,
    bool? chipBackgroundDisabled,
    Set<NetworkSpeedUnit>? units,
  }) {
    return NetworkSpeedSettings(
      enabled: enabled ?? this.enabled,
      displayMode: displayMode ?? this.displayMode,
      uploadPrefix: uploadPrefix ?? this.uploadPrefix,
      downloadPrefix: downloadPrefix ?? this.downloadPrefix,
      prioritizeUploadSpeed:
          prioritizeUploadSpeed ?? this.prioritizeUploadSpeed,
      chipBackgroundDisabled:
          chipBackgroundDisabled ?? this.chipBackgroundDisabled,
      units: Set<NetworkSpeedUnit>.from(units ?? this.units),
    );
  }

  Map<String, dynamic> toJson() {
    return <String, dynamic>{
      'enabled': enabled,
      'display_mode': displayMode.id,
      'upload_prefix': uploadPrefix,
      'download_prefix': downloadPrefix,
      'prioritize_upload_speed': prioritizeUploadSpeed,
      'chip_background_disabled': chipBackgroundDisabled,
      'unit': NetworkSpeedUnitSelection.encode(units),
    };
  }

  static NetworkSpeedSettings fromMap(Map<String, dynamic> map) {
    final Object? rawUploadPrefix = map['upload_prefix'];
    final Object? rawDownloadPrefix = map['download_prefix'];
    return NetworkSpeedSettings(
      enabled: map['enabled'] == true,
      displayMode: NetworkSpeedDisplayModeId.from(
        map['display_mode'] as String?,
      ),
      uploadPrefix: rawUploadPrefix is String
          ? rawUploadPrefix
          : defaultUploadPrefix,
      downloadPrefix: rawDownloadPrefix is String
          ? rawDownloadPrefix
          : defaultDownloadPrefix,
      prioritizeUploadSpeed: map['prioritize_upload_speed'] != false,
      chipBackgroundDisabled: map['chip_background_disabled'] == true,
      units: Set<NetworkSpeedUnit>.from(
        NetworkSpeedUnitSelection.parse(map['unit'] as String?),
      ),
    );
  }
}

enum NetworkSpeedDisplayMode { total, uploadOnly, downloadOnly }

extension NetworkSpeedDisplayModeId on NetworkSpeedDisplayMode {
  String get id {
    switch (this) {
      case NetworkSpeedDisplayMode.total:
        return 'total';
      case NetworkSpeedDisplayMode.uploadOnly:
        return 'upload_only';
      case NetworkSpeedDisplayMode.downloadOnly:
        return 'download_only';
    }
  }

  static NetworkSpeedDisplayMode from(String? value) {
    switch (value) {
      case 'upload_only':
        return NetworkSpeedDisplayMode.uploadOnly;
      case 'download_only':
        return NetworkSpeedDisplayMode.downloadOnly;
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
        return 'bytes';
      case NetworkSpeedUnit.kilobytes:
        return 'kilobytes';
      case NetworkSpeedUnit.megabytes:
        return 'megabytes';
      case NetworkSpeedUnit.gigabytes:
        return 'gigabytes';
    }
  }

  static NetworkSpeedUnit? tryFrom(String? value) {
    switch (value) {
      case 'auto':
        return NetworkSpeedUnit.auto;
      case 'bytes':
        return NetworkSpeedUnit.bytes;
      case 'kilobytes':
        return NetworkSpeedUnit.kilobytes;
      case 'megabytes':
        return NetworkSpeedUnit.megabytes;
      case 'gigabytes':
        return NetworkSpeedUnit.gigabytes;
      default:
        return null;
    }
  }
}

class NetworkSpeedUnitSelection {
  static Set<NetworkSpeedUnit> parse(String? raw) {
    final Set<NetworkSpeedUnit> parsed = (raw ?? '')
        .split(',')
        .map((String token) => NetworkSpeedUnitId.tryFrom(token.trim()))
        .whereType<NetworkSpeedUnit>()
        .toSet();
    if (parsed.isEmpty || parsed.contains(NetworkSpeedUnit.auto)) {
      return const <NetworkSpeedUnit>{NetworkSpeedUnit.auto};
    }
    return parsed;
  }

  static String encode(Set<NetworkSpeedUnit> units) {
    final Set<NetworkSpeedUnit> normalized = parse(
      units.map((NetworkSpeedUnit unit) => unit.id).join(','),
    );
    if (normalized.contains(NetworkSpeedUnit.auto)) {
      return NetworkSpeedUnit.auto.id;
    }
    return kNetworkSpeedUnitValues
        .where(
          (NetworkSpeedUnit unit) =>
              unit != NetworkSpeedUnit.auto && normalized.contains(unit),
        )
        .map((NetworkSpeedUnit unit) => unit.id)
        .join(',');
  }

  static bool usesAuto(Set<NetworkSpeedUnit> units) {
    return parse(encode(units)).contains(NetworkSpeedUnit.auto);
  }

  static bool equals(Set<NetworkSpeedUnit> left, Set<NetworkSpeedUnit> right) {
    return encode(left) == encode(right);
  }
}
