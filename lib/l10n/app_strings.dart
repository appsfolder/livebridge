import 'package:flutter/material.dart';

class AppStrings {
  AppStrings({required this.isRu});
  final bool isRu;

  static AppStrings of(BuildContext context) {
    final bool isRu = Localizations.localeOf(
      context,
    ).languageCode.toLowerCase().startsWith('ru');
    return AppStrings(isRu: isRu);
  }

  String get refresh => isRu ? 'Обновить' : 'Refresh';
  String get saved => isRu ? 'Настройки сохранены.' : 'Settings saved.';
  String get saveFailed =>
      isRu ? 'Не удалось сохранить настройки.' : 'Unable to save settings.';
  String get permissionGranted => isRu
      ? 'Разрешение на уведомления выдано.'
      : 'Notification permission granted.';
  String get permissionDenied => isRu
      ? 'Разрешение на уведомления не выдано.'
      : 'Notification permission was not granted.';
  String get listenerOpened => isRu
      ? 'Открыты настройки Notification Listener.'
      : 'Opened Notification Listener settings.';
  String get listenerUnavailable => isRu
      ? 'Не удалось открыть настройки Listener.'
      : 'Unable to open Listener settings on this device.';
  String get notificationsOpened => isRu
      ? 'Открыты настройки уведомлений приложения.'
      : 'Opened app notification settings.';
  String get notificationsUnavailable => isRu
      ? 'Не удалось открыть настройки уведомлений.'
      : 'Unable to open app notification settings.';
  String get liveUpdatesOpened => isRu
      ? 'Открыты настройки Live Updates.'
      : 'Opened Live Updates settings.';
  String get liveUpdatesUnavailable => isRu
      ? 'Не удалось открыть настройки Live Updates.'
      : 'Unable to open Live Updates settings on this device.';
  String get githubOpenFailed => isRu
      ? 'Не удалось открыть ссылку GitHub.'
      : 'Unable to open GitHub link.';
  String get dictionaryEmpty => isRu
      ? 'Словарь пустой или поврежден.'
      : 'Dictionary is empty or invalid.';
  String get dictionaryDownloadFailed =>
      isRu ? 'Не удалось выгрузить словарь.' : 'Failed to export dictionary.';
  String get dictionarySaved =>
      isRu ? 'Словарь сохранен в Загрузки.' : 'Dictionary saved to Downloads.';
  String get dictionaryUploadDone => isRu
      ? 'Пользовательский словарь загружен.'
      : 'Custom dictionary uploaded.';
  String get dictionaryUpdateDone =>
      isRu ? 'Словарь обновлен из GitHub.' : 'Dictionary updated from GitHub.';
  String get dictionaryInvalid =>
      isRu ? 'Невалидный JSON словаря.' : 'Invalid dictionary JSON.';
  String get dictionaryUploadFailed =>
      isRu ? 'Не удалось загрузить словарь.' : 'Failed to upload dictionary.';
  String get dictionaryUpdateFailed => isRu
      ? 'Не удалось обновить словарь из GitHub.'
      : 'Failed to update dictionary from GitHub.';
  String get dictionaryResetDone => isRu
      ? 'Возвращен словарь из приложения.'
      : 'Bundled dictionary restored.';
  String get dictionaryResetFailed =>
      isRu ? 'Не удалось сбросить словарь.' : 'Failed to reset dictionary.';

  String get heroTitle => 'LiveBridge';
  String get masterToggleLockedHint => isRu
      ? 'Сначала выдайте доступ к уведомлениям и разрешение на уведомления.'
      : 'Grant notification listener access and notifications permission first.';
  String get githubUrl => 'github.com/Spottq/livebridge';
  String get githubReleasesUrl => 'github.com/Spottq/livebridge/releases';
  String get reportBug => isRu ? 'Сообщить о баге' : 'Report a bug';
  String get bugReportCopied => isRu
      ? 'Диагностика скопирована в буфер. Вставьте в issue.'
      : 'Diagnostics copied to clipboard. Paste it into the issue.';
  String get bugReportCopyFailed => isRu
      ? 'Не удалось скопировать диагностику.'
      : 'Failed to copy diagnostics.';
  String get hideWarningBanner => isRu ? 'Скрыть' : 'Hide';
  String get backgroundWarningTitle =>
      isRu ? 'Важно для фоновой работы' : 'Background mode warning';
  String backgroundWarningBody(String deviceLabel) => isRu
      ? 'Для $deviceLabel нужно вручную разрешить автозапуск и работу без ограничений в фоне, иначе Live Updates могут не появляться или зависать.'
      : 'On $deviceLabel, allow autostart and unrestricted background activity, otherwise Live Updates may stop appearing or freeze.';
  String get samsungWarningTitle =>
      isRu ? 'Важно для Samsung' : 'Important for Samsung';
  String get samsungWarningBody => isRu
      ? 'Для Samsung не нужно включать Live Updates в приложении. Но в параметрах разработчика обязательно включите "Отображение живых уведомлений для всех приложений".'
      : 'On Samsung, enabling in-app Live Updates is not required. But in Developer options, enable "Live notifications for all apps".';

  String get accessTitle => isRu ? 'Разрешения' : 'Permissions';
  String get accessSubtitle => isRu
      ? 'Без этих трёх разрешений конвертация будет работать нестабильно.'
      : 'Conversion reliability depends on these three permissions.';
  String get listenerAccess =>
      isRu ? 'Доступ к уведомлениям' : 'Notification Listener access';
  String get postNotifications =>
      isRu ? 'Отправка уведомлений' : 'Post notifications permission';
  String get liveUpdatesAccess =>
      isRu ? 'Продвижение Live Updates' : 'Live Updates promotion';
  String get open => isRu ? 'Открыть' : 'Open';
  String get request => isRu ? 'Запросить' : 'Request';
  String get grant => isRu ? 'Выдать' : 'Grant';
  String get manage => isRu ? 'Управлять' : 'Manage';
  String get settingsTitle => isRu ? 'Настройки' : 'Settings';
  String get keepAliveForegroundTitle =>
      isRu ? 'Альтернативный фоновый режим' : 'Alt background mode';
  String get keepAliveForegroundSubtitle => isRu
      ? 'Держит foreground-сервис для более стабильной работы в фоне.'
      : 'Runs a persistent foreground service for better background stability.';
  String get keepAliveForegroundInactiveSubtitle => isRu
      ? 'Включите LiveBridge, чтобы режим начал работать.'
      : 'Enable the LiveBridge for this mode to take effect.';
  String get syncDndTitle => isRu ? 'Синхронизировать DnD' : 'Sync DnD';
  String get syncDndSubtitle => isRu
      ? 'Если на смартфоне включен режим Не беспокоить, уведомления LiveBridge не показываются.'
      : 'When Do Not Disturb is enabled on the phone, LiveBridge notifications are hidden.';
  String get updateChecksTitle =>
      isRu ? 'Проверка обновлений' : 'Update checking';
  String get updateChecksSubtitle => isRu
      ? 'Проверять обновления при входе и не чаще одного раза в 6 часов.'
      : 'Check updates on app start, and no more than once every 6 hours.';
  String get samsungRemoteParserTitle =>
      isRu ? 'Samsung RemoteViews репарсер' : 'Samsung RemoteViews reparser';
  String get samsungRemoteParserSubtitle => isRu
      ? 'Использует Samsung ongoingActivity extras и RemoteViews для более точного парсинга уведомлений.'
      : 'Uses Samsung ongoingActivity extras and RemoteViews for improved parsing on One UI.';
  String updateAvailableBanner(String version) => isRu
      ? 'Доступно обновление${version.isNotEmpty ? ': $version' : ''}'
      : 'Update available${version.isNotEmpty ? ': $version' : ''}';
  String get experimentalTitle => isRu ? 'Экспериментальное' : 'Experimental';
  String get animatedIslandTitle =>
      isRu ? 'Анимированный остров' : 'Animated island';
  String get animatedIslandSubtitle => isRu
      ? 'Меняет короткий текст острова каждые 2-3 секунды для smart-уведомлений (может работать нестабильно).'
      : 'Rotates compact island text every 2-3 seconds for smart notifications (may be unstable).';
  String get hyperBridgeTitle => 'Xiaomi Hyper Island';
  String get hyperBridgeSubtitle => isRu
      ? 'Для Xiaomi Hyper OS 3.1 Глобальной: добавляет HyperOS Focus-параметры для нативного острова.'
      : 'For Xiaomi Hyper OS 3.1 Global: injects HyperOS Focus parameters for native island behavior.';
  String get aospCuttingTitle => isRu ? 'Обрезка AOSP' : 'AOSP cutting';
  String get aospCuttingSubtitle => isRu
      ? 'Обрезать информацию в острове до 7 символов для красивого отображения в AOSP-прошивках.'
      : 'Trim island text to 7 characters for cleaner rendering on AOSP ROMs.';
  String get appPresentationSettings =>
      isRu ? 'Поведение приложений' : 'Per-app behavior';
  String get appPresentationSubtitle => isRu
      ? 'Настройте источник текста и иконки отдельно для разных приложений.'
      : 'Choose text and icon behavior separately for different applications.';
  String get appPresentationScreenTitle =>
      isRu ? 'Поведение приложений' : 'Per-app behavior';
  String get appPresentationLoadFailed => isRu
      ? 'Не удалось загрузить настройки приложений.'
      : 'Unable to load per-app settings.';
  String get appPresentationSaveFailed => isRu
      ? 'Не удалось сохранить настройки приложений.'
      : 'Unable to save per-app settings.';
  String get appPresentationDownloadFailed => isRu
      ? 'Не удалось сохранить JSON настроек.'
      : 'Failed to save settings JSON.';
  String get appPresentationSaved =>
      isRu ? 'Настройки сохранены в Загрузки.' : 'Settings saved to Downloads.';
  String get appPresentationUploadDone =>
      isRu ? 'Настройки приложений загружены.' : 'Per-app settings imported.';
  String get appPresentationUploadFailed => isRu
      ? 'Не удалось загрузить JSON настроек.'
      : 'Failed to import settings JSON.';
  String get appPresentationInvalidJson => isRu
      ? 'Невалидный JSON настроек приложений.'
      : 'Invalid per-app settings JSON.';
  String get appPresentationDefaultSummary =>
      isRu ? 'Стандартное поведение' : 'Default behavior';
  String get appPresentationTextSourceLabel =>
      isRu ? 'Источник текста для острова' : 'Island text source';
  String get appPresentationIconSourceLabel =>
      isRu ? 'Источник иконки' : 'Icon source';
  String get appPresentationTextTitle =>
      isRu ? 'Title уведомления' : 'Notification title';
  String get appPresentationTextNotification =>
      isRu ? 'Текст уведомления' : 'Notification text';
  String get appPresentationIconNotification =>
      isRu ? 'Иконка уведомления' : 'Notification icon';
  String get appPresentationIconApp =>
      isRu ? 'Иконка приложения' : 'Application icon';
  String get downloadSettings =>
      isRu ? 'Скачать настройки' : 'Download settings';
  String get uploadSettings => isRu ? 'Загрузить настройки' : 'Upload settings';
  String get defaultLabel => isRu ? 'По умолчанию' : 'Default';
  String get resetToDefault =>
      isRu ? 'Сбросить к стандарту' : 'Reset to default';
  String get save => isRu ? 'Сохранить' : 'Save';
  String get downloadDictionary =>
      isRu ? 'Скачать словарь' : 'Download dictionary';
  String get updateDictionary =>
      isRu ? 'Обновить словарь' : 'Update dictionary';
  String get uploadDictionary =>
      isRu ? 'Загрузить словарь' : 'Upload dictionary';
  String get resetDictionary => isRu ? 'Сбросить словарь' : 'Reset dictionary';
  String get pickApps => isRu ? 'Выбрать приложения' : 'Pick applications';
  String get pickerTitle =>
      isRu ? 'Приложения для конвертации' : 'Choose apps for conversion';
  String get otpPickerTitle =>
      isRu ? 'Приложения для кодов' : 'Choose apps for code detection';
  String get bypassPickerTitle =>
      isRu ? 'Приложения bypass' : 'Choose apps for bypass';
  String get applySelection => isRu ? 'Применить выбор' : 'Apply selection';
  String get searchAppHint =>
      isRu ? 'Поиск по названию или пакету' : 'Search by app or package';
  String get showSystemApps =>
      isRu ? 'Показать системные приложения' : 'Show system applications';
  String get hideSystemApps =>
      isRu ? 'Скрыть системные приложения' : 'Hide system applications';
  String get appsLoadFailed => isRu
      ? 'Не удалось загрузить список приложений.'
      : 'Unable to load installed apps list.';
  String get appsAccessTitle =>
      isRu ? 'Доступ к списку приложений' : 'App list access';
  String get appsAccessMessage => isRu
      ? 'Разрешить LiveBridge читать список установленных приложений для выбора правил?'
      : 'Allow LiveBridge to read installed apps so you can pick apps for rules?';
  String get appsAccessSaveFailed => isRu
      ? 'Не удалось сохранить выбор доступа.'
      : 'Unable to save access preference.';
  String get cancel => isRu ? 'Отмена' : 'Cancel';
  String get allow => isRu ? 'Разрешить' : 'Allow';
  String selectedAppsCount(int value) =>
      isRu ? 'Выбрано приложений: $value' : 'Selected apps: $value';
  String get noAppsSelected =>
      isRu ? 'Приложения не выбраны' : 'No applications selected';

  String get rulesTitle => isRu ? 'Режим конвертации' : 'Conversion behavior';
  String get rulesSubtitle => isRu
      ? 'Настройте, что именно превращать в Live Updates.'
      : 'Define what should be converted into Live Updates.';
  String get modeLabel => isRu ? 'Режим работы' : 'Application mode';
  String get modeAll => isRu ? 'Все приложения' : 'All applications';
  String get modeInclude =>
      isRu ? 'Только указанные' : 'Only listed applications';
  String get modeExclude =>
      isRu ? 'Исключить указанные' : 'Exclude listed applications';
  String get pickAppsHint => isRu
      ? 'Список используется только в режимах "Только указанные" или "Исключить".'
      : 'Selected app list is used only for include/exclude modes.';
  String get bypassRulesTitle => isRu ? 'Bypass-приложения' : 'Bypass apps';
  String get bypassRulesSubtitle => isRu
      ? 'Приложения из списка всегда конвертируются в Live вне зависимости от настроек.'
      : 'Listed apps are always converted to Live independently of settings.';
  String get saveRules => isRu ? 'Сохранить' : 'Save';

  String get smartDetectionTitle =>
      isRu ? 'Умное распознавание' : 'Smart status detection';
  String get smartCardTitle =>
      isRu ? 'Умное преобразование' : 'Smart conversion';
  String get smartCardSubtitle => isRu
      ? 'Преобразование текстовых этапов в один Live-прогресс.'
      : 'Converts text-only stage updates into one Live progress flow.';
  String get smartDetectionSubtitle => isRu
      ? 'Преобразует текстовые статусы еды, такси и навигации в единый Live-прогресс.'
      : 'Converts text-only food/taxi/navigation status notifications into a single Live.';
  String get smartMediaPlaybackTitle =>
      isRu ? 'Media Playback' : 'Media Playback';
  String get smartMediaPlaybackSubtitle => isRu
      ? 'Преобразует уведомления медиаплеера в Live. На некоторых OEM может дублировать нативный плеер.'
      : 'Converts media playback notifications into Live. On some OEMs this may duplicate native media UI.';
  String get smartNavigationTitle =>
      isRu ? 'Навигация (карты)' : 'Navigation (maps)';
  String get smartNavigationSubtitle => isRu
      ? 'Распознавание уведомлений навигации.'
      : 'Navigation notification detection.';
  String get smartWeatherTitle => isRu ? 'Погода' : 'Weather';
  String get smartWeatherSubtitle => isRu
      ? 'Распознавание погодных уведомлений (температура в острове).'
      : 'Weather notification detection (temperature in island).';
  String get smartExternalDevicesTitle =>
      isRu ? 'Внешние устройства' : 'External devices';
  String get smartExternalDevicesSubtitle => isRu
      ? 'Показывает статус connected/connecting и имя устройства в острове.'
      : 'Shows connected/connecting status and device name in island.';
  String get smartVpnTitle => isRu ? 'VPN-сервисы' : 'VPN services';
  String get smartVpnSubtitle => isRu
      ? 'Показывает входящий/исходящий трафик в формате *b/s.'
      : 'Shows incoming/outgoing traffic speed in *b/s format.';
  String get networkSpeedCardTitle =>
      isRu ? 'Скорость интернета' : 'Network speed';
  String get networkSpeedEnabledTitle => isRu
      ? 'Показывать скорость интернета в Now Bar'
      : 'Show network speed in Now Bar';
  String get networkSpeedEnabledSubtitle => isRu
      ? 'Запускает отдельное уведомление с текущей скоростью сети и выводит его в Now Bar.'
      : 'Runs a dedicated ongoing notification with current network speed and surfaces it in the Now Bar.';
  String get networkSpeedDisplayContentTitle =>
      isRu ? 'Отображаемый контент' : 'Display content';
  String get networkSpeedDisplayModeTotal =>
      isRu ? 'Общая скорость' : 'Total speed';
  String get networkSpeedDisplayModeUpload =>
      isRu ? 'Только upload' : 'Upload only';
  String get networkSpeedDisplayModeDownload =>
      isRu ? 'Только download' : 'Download only';
  String get networkSpeedUploadPrefixTitle =>
      isRu ? 'Префикс upload' : 'Upload prefix';
  String get networkSpeedDownloadPrefixTitle =>
      isRu ? 'Префикс download' : 'Download prefix';
  String get networkSpeedUnitTitle => isRu ? 'Единица скорости' : 'Speed unit';
  String get networkSpeedUnitAuto => isRu ? 'Авто' : 'Auto';
  String get networkSpeedUnitBytes => isRu ? 'B/s' : 'B/s';
  String get networkSpeedUnitKilobytes => isRu ? 'KB/s' : 'KB/s';
  String get networkSpeedUnitMegabytes => isRu ? 'MB/s' : 'MB/s';
  String get networkSpeedUnitGigabytes => isRu ? 'GB/s' : 'GB/s';
  String networkSpeedCurrentValue(String value) =>
      isRu ? 'Сейчас: "$value"' : 'Current: "$value"';
  String get networkSpeedPrioritizeUploadTitle =>
      isRu ? 'Сначала показывать upload' : 'Prioritize upload speed';
  String get networkSpeedPrioritizeUploadSubtitle => isRu
      ? 'В режиме общей скорости upload будет стоять перед download.'
      : 'In total mode, upload speed is shown before download.';
  String get networkSpeedLockscreenOnlyTitle =>
      isRu ? 'Отображать только на локскрине' : 'Display only on lock screen';
  String get networkSpeedLockscreenOnlySubtitle => isRu
      ? 'При разблокированном экране индикатор скорости полностью скрывается и возвращается только на локскрине.'
      : 'When the device is unlocked, network speed is hidden completely and returns only on the lock screen.';
  String get networkSpeedDisableChipBackgroundTitle =>
      isRu ? 'Отключить фон chips' : 'Disable chip background';
  String get networkSpeedDisableChipBackgroundSubtitle => isRu
      ? 'Убирает плашку у чипов скорости в Now Bar, но оставляет статичный цвет у иконки в развернутом уведомлении.'
      : 'Removes the pill background from the network speed chips in Now Bar while keeping a fixed accent behind the icon in the expanded notification.';
  String get smartNavigationDisabledSubtitle => isRu
      ? 'Сначала включите умное распознавание.'
      : 'Enable smart status detection first.';
  String get smartDetectionDisabledSubtitle => isRu
      ? 'Отключено в режиме "Нативный прогресс".'
      : 'Disabled while "Native progress" mode is enabled.';
  String get conflictingModesHint => isRu
      ? 'Чтобы работали текстовые статусы, отключите режим "Нативный прогресс".'
      : 'Turn off "Native progress" mode to enable food/taxi/navigation text status recognition.';
  String get onlyProgressTitle =>
      isRu ? 'Нативный прогресс' : 'Native progress';
  String get onlyProgressSubtitle => isRu
      ? 'Если включено, конвертируются только уведомления с системным прогрессбаром.'
      : 'When enabled, only notifications with a system progress bar are converted.';
  String get textProgressTitle =>
      isRu ? 'Текстовые прогрессы' : 'Text progress';
  String get textProgressSubtitle => isRu
      ? 'Если в тексте есть %, и это не скидка/акция, считать как прогресс и обновлять остров.'
      : 'If text contains % and it is not discount-related, treat it as progress and update island.';

  String get blockedTitle => isRu
      ? 'Пожалуйста, купите нормальный смартфон'
      : 'Buy a proper phone first';
  String get blockedSubtitle => isRu
      ? 'LiveBridge плохо работает на устройствах с AOSP. Можете продолжить, но за последствия я не отвечаю.'
      : 'LiveBridge is not designed for AOSP. You can continue, but i am not responsible for any bugs.';
  String get blockedBypassAction =>
      isRu ? 'Все равно родолжить' : 'Continue anyway';
  String get blockedBypassSaveFailed =>
      isRu ? 'Не удалось сохранить выбор.' : 'Unable to save your choice.';

  String get otpTitle => isRu ? 'Коды подтверждения' : 'Verification codes';
  String get otpSubtitle => isRu
      ? 'Показывает код компактно в острове.'
      : 'Shows the code in compact island.';
  String get otpEnabledTitle =>
      isRu ? 'Распознавать 2FA коды' : 'Detect verification codes';
  String get otpEnabledSubtitle => isRu
      ? 'В свернутом Live-острове показывается сам код.'
      : 'Shows the numeric code in the compact island.';
  String get otpAutoCopyTitle =>
      isRu ? 'Автокопирование кода' : 'Auto-copy code';
  String get otpAutoCopySubtitle => isRu
      ? 'Код сразу копируется в буфер обмена.'
      : 'Code is copied to clipboard automatically.';
  String get otpAutoCopyDisabledSubtitle => isRu
      ? 'Сначала включите распознавание кодов.'
      : 'Enable code detection first.';
  String get otpModeLabel => isRu ? 'Режим для кодов' : 'Code apps mode';
  String get saveOtpRules => isRu ? 'Сохранить' : 'Save';
}
