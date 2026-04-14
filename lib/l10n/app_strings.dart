import 'package:flutter/material.dart';

class AppStrings {
  AppStrings({required this.locale});
  final Locale locale;

  bool get isRu => locale.languageCode.toLowerCase().startsWith('ru');
  bool get isZhHans {
    final String languageCode = locale.languageCode.toLowerCase();
    if (languageCode != 'zh') return false;
    final String scriptCode = locale.scriptCode?.toLowerCase() ?? '';
    final String countryCode = locale.countryCode?.toLowerCase() ?? '';
    return scriptCode == 'hans' || countryCode == 'cn' || countryCode == 'sg';
  }

  bool get isZhHant {
    final String languageCode = locale.languageCode.toLowerCase();
    if (languageCode != 'zh') return false;
    final String scriptCode = locale.scriptCode?.toLowerCase() ?? '';
    final String countryCode = locale.countryCode?.toLowerCase() ?? '';
    return scriptCode == 'hant' || countryCode == 'tw' || countryCode == 'hk' || countryCode == 'mo';
  }

  String tr({
    required String en,
    required String ru,
    String? zhHans,
    String? zhHant,
  }) {
    if (isRu) return ru;
    if (isZhHant) return zhHant ?? zhHans ?? en;
    if (isZhHans) return zhHans ?? zhHant ?? en;
    return en;
  }

  static AppStrings of(BuildContext context) {
    return AppStrings(locale: Localizations.localeOf(context));
  }

  String get refresh => tr(en: 'Refresh', ru: 'Обновить', zhHans: '刷新', zhHant: '重新整理');
  String get saved => tr(en: 'Settings saved.', ru: 'Настройки сохранены.', zhHans: '设置已保存。', zhHant: '設定已儲存。');
  String get saveFailed =>
      tr(en: 'Unable to save settings.', ru: 'Не удалось сохранить настройки.', zhHans: '无法保存设置。', zhHant: '無法儲存設定。');
  String get permissionGranted =>
      tr(en: 'Notification permission granted.', ru: 'Разрешение на уведомления выдано.', zhHans: '通知权限已授予。', zhHant: '通知權限已授予。');
  String get permissionDenied =>
      tr(en: 'Notification permission was not granted.', ru: 'Разрешение на уведомления не выдано.', zhHans: '未授予通知权限。', zhHant: '未授予通知權限。');
  String get listenerOpened =>
      tr(en: 'Opened Notification Listener settings.', ru: 'Открыты настройки Notification Listener.', zhHans: '已打开通知监听设置。', zhHant: '已開啟通知監聽設定。');
  String get listenerUnavailable =>
      tr(en: 'Unable to open Listener settings on this device.', ru: 'Не удалось открыть настройки Listener.', zhHans: '此设备无法打开监听器设置。', zhHant: '此裝置無法開啟監聽器設定。');
  String get notificationsOpened =>
      tr(en: 'Opened app notification settings.', ru: 'Открыты настройки уведомлений приложения.', zhHans: '已打开应用通知设置。', zhHant: '已開啟應用通知設定。');
  String get notificationsUnavailable =>
      tr(en: 'Unable to open app notification settings.', ru: 'Не удалось открыть настройки уведомлений.', zhHans: '无法打开应用通知设置。', zhHant: '無法開啟應用通知設定。');
  String get liveUpdatesOpened =>
      tr(en: 'Opened Live Updates settings.', ru: 'Открыты настройки Live Updates.', zhHans: '已打开 Live Updates 设置。', zhHant: '已開啟 Live Updates 設定。');
  String get liveUpdatesUnavailable =>
      tr(en: 'Unable to open Live Updates settings on this device.', ru: 'Не удалось открыть настройки Live Updates.', zhHans: '此设备无法打开 Live Updates 设置。', zhHant: '此裝置無法開啟 Live Updates 設定。');
  String get githubOpenFailed =>
      tr(en: 'Unable to open GitHub link.', ru: 'Не удалось открыть ссылку GitHub.', zhHans: '无法打开 GitHub 链接。', zhHant: '無法開啟 GitHub 連結。');
  String get dictionaryEmpty =>
      tr(en: 'Dictionary is empty or invalid.', ru: 'Словарь пустой или поврежден.', zhHans: '词典为空或无效。', zhHant: '字典為空或無效。');
  String get dictionaryDownloadFailed =>
      tr(en: 'Failed to export dictionary.', ru: 'Не удалось выгрузить словарь.', zhHans: '导出词典失败。', zhHant: '匯出字典失敗。');
  String get dictionarySaved =>
      tr(en: 'Dictionary saved to Downloads.', ru: 'Словарь сохранен в Загрузки.', zhHans: '词典已保存到下载。', zhHant: '字典已儲存到下載資料夾。');
  String get dictionaryUploadDone =>
      tr(en: 'Custom dictionary uploaded.', ru: 'Пользовательский словарь загружен.', zhHans: '自定义词典已上传。', zhHant: '自訂字典已上傳。');
  String get dictionaryUpdateDone =>
      tr(en: 'Dictionary updated from GitHub.', ru: 'Словарь обновлен из GitHub.', zhHans: '词典已从 GitHub 更新。', zhHant: '字典已從 GitHub 更新。');
  String get dictionaryInvalid =>
      tr(en: 'Invalid dictionary JSON.', ru: 'Невалидный JSON словаря.', zhHans: '词典 JSON 无效。', zhHant: '字典 JSON 無效。');
  String get dictionaryUploadFailed =>
      tr(en: 'Failed to upload dictionary.', ru: 'Не удалось загрузить словарь.', zhHans: '上传词典失败。', zhHant: '上傳字典失敗。');
  String get dictionaryUpdateFailed =>
      tr(en: 'Failed to update dictionary from GitHub.', ru: 'Не удалось обновить словарь из GitHub.', zhHans: '从 GitHub 更新词典失败。', zhHant: '從 GitHub 更新字典失敗。');
  String get dictionaryResetDone =>
      tr(en: 'Bundled dictionary restored.', ru: 'Возвращен словарь из приложения.', zhHans: '已恢复内置词典。', zhHant: '已還原內建字典。');
  String get dictionaryResetFailed =>
      tr(en: 'Failed to reset dictionary.', ru: 'Не удалось сбросить словарь.', zhHans: '重置词典失败。', zhHant: '重設字典失敗。');

  String get heroTitle => 'LiveBridge';
  String get masterToggleLockedHint =>
      tr(en: 'Grant notification listener access and notifications permission first.', ru: 'Сначала выдайте доступ к уведомлениям и разрешение на уведомления.', zhHans: '请先授予通知监听访问和通知权限。', zhHant: '請先授予通知監聽存取與通知權限。');
  String get githubUrl => 'github.com/appsfolder/livebridge';
  String get githubReleasesUrl => 'github.com/appsfolder/livebridge/releases';
  String get downloadPageUrl => 'appsfolder.github.io/livebridge';
  String get reportBug => tr(en: 'Report a bug', ru: 'Сообщить о баге', zhHans: '报告问题', zhHant: '回報問題');
  String get bugReportCopied =>
      tr(en: 'Diagnostics copied to clipboard. Paste it into the issue.', ru: 'Диагностика скопирована в буфер. Вставьте в issue.', zhHans: '诊断信息已复制到剪贴板，请粘贴到 issue 中。', zhHant: '診斷資訊已複製到剪貼簿，請貼到 issue。');
  String get bugReportCopyFailed =>
      tr(en: 'Failed to copy diagnostics.', ru: 'Не удалось скопировать диагностику.', zhHans: '复制诊断信息失败。', zhHant: '複製診斷資訊失敗。');
  String get hideWarningBanner => tr(en: 'Hide', ru: 'Скрыть', zhHans: '隐藏', zhHant: '隱藏');
  String get backgroundWarningTitle =>
      tr(en: 'Background mode warning', ru: 'Важно для фоновой работы', zhHans: '后台模式提示', zhHant: '背景模式提醒');
  String backgroundWarningBody(String deviceLabel) =>
      tr(en: 'On $deviceLabel, allow autostart and unrestricted background activity, otherwise Live Updates may stop appearing or freeze.', ru: 'Для $deviceLabel нужно вручную разрешить автозапуск и работу без ограничений в фоне, иначе Live Updates могут не появляться или зависать.', zhHans: '在 $deviceLabel 上，请允许自启动和后台无限制运行，否则 Live Updates 可能不显示或卡住。', zhHant: '在 $deviceLabel 上，請允許自啟動與背景無限制執行，否則 Live Updates 可能不顯示或卡住。');
  String get samsungWarningTitle =>
      tr(en: 'A better build is available for Samsung', ru: 'Для Samsung есть версия лучше', zhHans: '三星有更好的专用版本', zhHant: 'Samsung 有更佳的專用版本');
  String get samsungWarningBody =>
      tr(en: 'There is a dedicated LiveBridge build for Samsung devices with improved Samsung-specific support. It is recommended over the regular build.', ru: 'Для Samsung доступна специальная сборка LiveBridge с улучшенной поддержкой Samsung-функций. Лучше установить ее вместо обычной версии.', zhHans: 'Samsung 设备有专用 LiveBridge 版本，提供更好的三星特性支持，推荐优先安装。', zhHant: 'Samsung 裝置有專用 LiveBridge 版本，提供更好的三星功能支援，建議優先安裝。');
  String get samsungWarningAction =>
      tr(en: 'Get Samsung build', ru: 'Открыть загрузки', zhHans: '获取三星版本', zhHant: '取得 Samsung 版本');

  String get accessTitle => tr(en: 'Permissions', ru: 'Разрешения', zhHans: '权限', zhHant: '權限');
  String get accessSubtitle =>
      tr(en: 'Conversion reliability depends on these three permissions.', ru: 'Без этих трёх разрешений конвертация будет работать нестабильно.', zhHans: '转换稳定性取决于这三个权限。', zhHant: '轉換穩定性取決於這三個權限。');
  String get listenerAccess =>
      tr(en: 'Notification Listener access', ru: 'Доступ к уведомлениям', zhHans: '通知监听访问', zhHant: '通知監聽存取');
  String get postNotifications =>
      tr(en: 'Post notifications permission', ru: 'Отправка уведомлений', zhHans: '发送通知权限', zhHant: '發送通知權限');
  String get liveUpdatesAccess =>
      tr(en: 'Live Updates promotion', ru: 'Продвижение Live Updates', zhHans: 'Live Updates 推送权限', zhHant: 'Live Updates 推送權限');
  String get open => tr(en: 'Open', ru: 'Открыть', zhHans: '打开', zhHant: '開啟');
  String get request => tr(en: 'Request', ru: 'Запросить', zhHans: '请求', zhHant: '請求');
  String get grant => tr(en: 'Grant', ru: 'Выдать', zhHans: '授予', zhHant: '授權');
  String get manage => tr(en: 'Manage', ru: 'Управлять', zhHans: '管理', zhHant: '管理');
  String get settingsTitle => tr(en: 'Settings', ru: 'Настройки', zhHans: '设置', zhHant: '設定');
  String get keepAliveForegroundTitle =>
      tr(en: 'Alt background mode', ru: 'Альтернативный фоновый режим', zhHans: '替代后台模式', zhHant: '替代背景模式');
  String get keepAliveForegroundSubtitle =>
      tr(en: 'Runs a persistent foreground service for better background stability.', ru: 'Держит foreground-сервис для более стабильной работы в фоне.', zhHans: '通过常驻前台服务提升后台稳定性。', zhHant: '透過常駐前景服務提升背景穩定性。');
  String get keepAliveForegroundInactiveSubtitle =>
      tr(en: 'Enable the LiveBridge for this mode to take effect.', ru: 'Включите LiveBridge, чтобы режим начал работать.', zhHans: '请启用 LiveBridge 以使此模式生效。', zhHant: '請啟用 LiveBridge 以使此模式生效。');
  String get networkSpeedTitle => tr(en: 'Network speed', ru: 'Скорость сети', zhHans: '网速', zhHant: '網速');
  String get networkSpeedSubtitle =>
      tr(en: 'Shows current download and upload as a separate Live Update in the status bar.', ru: 'Показывает загрузку и отдачу как отдельный Live Update в статус-баре.', zhHans: '在状态栏中以独立 Live Update 显示当前上下行网速。', zhHant: '在狀態列以獨立 Live Update 顯示目前上下行網速。');
  String get networkSpeedInactiveSubtitle =>
      tr(en: 'Enable LiveBridge for the network speed monitor to start working.', ru: 'Включите LiveBridge, чтобы монитор скорости начал работать.', zhHans: '请启用 LiveBridge 以启动网速监控。', zhHant: '請啟用 LiveBridge 以啟動網速監控。');
  String get networkSpeedThresholdTitle =>
      tr(en: 'Minimum speed to show', ru: 'Минимальная скорость для показа', zhHans: '最小显示速度', zhHant: '最小顯示速度');
  String get networkSpeedThresholdSubtitle =>
      tr(en: 'The live element appears when combined download and upload reach this threshold.', ru: 'Лайв-элемент появится, когда суммарная скорость загрузки и отдачи достигнет этого порога.', zhHans: '当下载和上传总速度达到该阈值时显示。', zhHant: '當下載與上傳總速率達到此門檻時顯示。');
  String get networkSpeedThresholdAlways =>
      tr(en: 'Always show', ru: 'Показывать всегда', zhHans: '始终显示', zhHant: '永遠顯示');
  String get smartExternalDevicesIgnoreDebuggingTitle =>
      tr(en: 'Ignore debugging', ru: 'Игнорировать отладку', zhHans: '忽略调试', zhHant: '忽略偵錯');
  String get smartExternalDevicesIgnoreDebuggingSubtitle =>
      tr(en: 'Skip Live updates for USB debugging, wireless debugging, ADB, and similar system notifications.', ru: 'Не показывать Live для USB debugging, wireless debugging, ADB и похожих системных уведомлений.', zhHans: '跳过 USB 调试、无线调试、ADB 等系统通知的 Live 更新。', zhHant: '略過 USB 偵錯、無線偵錯、ADB 等系統通知的 Live 更新。');
  String get syncDndTitle => tr(en: 'Sync DnD', ru: 'Синхронизировать DnD', zhHans: '同步勿扰', zhHant: '同步勿擾');
  String get syncDndSubtitle =>
      tr(en: 'When Do Not Disturb is enabled on the phone, LiveBridge notifications are hidden.', ru: 'Если на смартфоне включен режим Не беспокоить, уведомления LiveBridge не показываются.', zhHans: '当手机开启勿扰模式时，隐藏 LiveBridge 通知。', zhHant: '當手機開啟勿擾模式時，隱藏 LiveBridge 通知。');
  String get updateChecksTitle =>
      tr(en: 'Update checking', ru: 'Проверка обновлений', zhHans: '检查更新', zhHant: '檢查更新');
  String get updateChecksSubtitle =>
      tr(en: 'Check updates on app start, and no more than once every 6 hours.', ru: 'Проверять обновления при входе и не чаще одного раза в 6 часов.', zhHans: '应用启动时检查更新，且最多每 6 小时一次。', zhHant: '應用啟動時檢查更新，且最多每 6 小時一次。');
  String updateAvailableBanner(String version) => tr(
      en: 'Update available${version.isNotEmpty ? ': $version' : ''}',
      ru: 'Доступно обновление${version.isNotEmpty ? ': $version' : ''}',
      zhHans: '有可用更新${version.isNotEmpty ? ': $version' : ''}',
      zhHant: '有可用更新${version.isNotEmpty ? ': $version' : ''}',
    );
  String get experimentalTitle => tr(en: 'Experimental', ru: 'Экспериментальное', zhHans: '实验功能', zhHant: '實驗功能');
  String get notificationDedupTitle =>
      tr(en: 'Notification dedup', ru: 'Notification dedup', zhHans: '通知去重', zhHant: '通知去重');
  String get notificationDedupSubtitle =>
      tr(en: 'Dismisses original clearable notifications after LiveBridge mirrors an OTP or status update.', ru: 'Убирает оригинальные clearable-уведомления, если LiveBridge уже показал свой OTP или статус.', zhHans: '当 LiveBridge 已镜像 OTP 或状态更新后，自动清除原始可清除通知。', zhHant: '當 LiveBridge 已鏡像 OTP 或狀態更新後，自動清除原始可清除通知。');
  String get notificationDedupModeLabel =>
      tr(en: 'Dedup mode', ru: 'Режим dedup', zhHans: '去重模式', zhHant: '去重模式');
  String get notificationDedupModeOtpStatus =>
      tr(en: 'OTP and statuses', ru: 'OTP и статусы', zhHans: 'OTP 与状态', zhHant: 'OTP 與狀態');
  String get notificationDedupModeOtpOnly =>
      tr(en: 'OTP only', ru: 'Только OTP', zhHans: '仅 OTP', zhHant: '僅 OTP');
  String get notificationDedupStatusesTitle =>
      tr(en: 'Also dedup statuses', ru: 'Также статусы', zhHans: '同时去重状态通知', zhHant: '同時去重狀態通知');
  String get notificationDedupStatusesSubtitle =>
      tr(en: 'When disabled, dedup is applied only to OTP notifications.', ru: 'Если выключено, dedup применяется только к OTP-кодам.', zhHans: '关闭后，仅对 OTP 通知去重。', zhHant: '關閉後，僅對 OTP 通知去重。');
  String get animatedIslandTitle =>
      tr(en: 'Animated island', ru: 'Анимированный остров', zhHans: '动态岛动画', zhHant: '動態島動畫');
  String get animatedIslandSubtitle =>
      tr(en: 'Rotates compact island text every 2-3 seconds for smart notifications (may be unstable).', ru: 'Меняет короткий текст острова каждые 2-3 секунды для smart-уведомлений (может работать нестабильно).', zhHans: '为智能通知每 2-3 秒轮换一次紧凑岛文本（可能不稳定）。', zhHant: '智慧通知每 2-3 秒輪換一次精簡島文字（可能不穩定）。');
  String get hyperBridgeTitle => 'Xiaomi Hyper Island';
  String get hyperBridgeSubtitle =>
      tr(en: 'For Xiaomi Hyper OS 3.1 Global: injects HyperOS Focus parameters for native island behavior.', ru: 'Для Xiaomi Hyper OS 3.1 Глобальной: добавляет HyperOS Focus-параметры для нативного острова.', zhHans: '适用于小米 Hyper OS 3.1 国际版：注入 HyperOS Focus 参数以实现原生岛效果。', zhHant: '適用小米 Hyper OS 3.1 國際版：注入 HyperOS Focus 參數以實現原生島效果。');
  String get aospCuttingTitle => tr(en: 'AOSP cutting', ru: 'Обрезка AOSP', zhHans: 'AOSP 裁剪', zhHant: 'AOSP 裁切');
  String get aospCuttingSubtitle =>
      tr(en: 'Trim island text to 7 characters for cleaner rendering on AOSP ROMs.', ru: 'Обрезать информацию в острове до 7 символов для красивого отображения в AOSP-прошивках.', zhHans: '将岛内文本裁剪到 7 个字符，以便在 AOSP ROM 上显示更整洁。', zhHant: '將島內文字裁切到 7 個字元，讓 AOSP ROM 顯示更整潔。');
  String get appPresentationSettings =>
      tr(en: 'Per-app behavior', ru: 'Поведение приложений', zhHans: '按应用行为', zhHant: '各應用行為');
  String get appPresentationSubtitle =>
      tr(en: 'Choose text and icon behavior separately for different applications.', ru: 'Настройте источник текста и иконки отдельно для разных приложений.', zhHans: '为不同应用分别设置文本与图标行为。', zhHant: '為不同應用分別設定文字與圖示行為。');
  String get appPresentationScreenTitle =>
      tr(en: 'Per-app behavior', ru: 'Поведение приложений', zhHans: '按应用行为', zhHant: '各應用行為');
  String get appPresentationLoadFailed =>
      tr(en: 'Unable to load per-app settings.', ru: 'Не удалось загрузить настройки приложений.', zhHans: '无法加载按应用设置。', zhHant: '無法載入各應用設定。');
  String get appPresentationSaveFailed =>
      tr(en: 'Unable to save per-app settings.', ru: 'Не удалось сохранить настройки приложений.', zhHans: '无法保存按应用设置。', zhHant: '無法儲存各應用設定。');
  String get appPresentationDownloadFailed =>
      tr(en: 'Failed to save settings JSON.', ru: 'Не удалось сохранить JSON настроек.', zhHans: '保存设置 JSON 失败。', zhHant: '儲存設定 JSON 失敗。');
  String get appPresentationSaved =>
      tr(en: 'Settings saved to Downloads.', ru: 'Настройки сохранены в Загрузки.', zhHans: '设置已保存到下载目录。', zhHant: '設定已儲存到下載資料夾。');
  String get appPresentationUploadDone =>
      tr(en: 'Per-app settings imported.', ru: 'Настройки приложений загружены.', zhHans: '已导入按应用设置。', zhHant: '已匯入各應用設定。');
  String get appPresentationUploadFailed =>
      tr(en: 'Failed to import settings JSON.', ru: 'Не удалось загрузить JSON настроек.', zhHans: '导入设置 JSON 失败。', zhHant: '匯入設定 JSON 失敗。');
  String get appPresentationInvalidJson =>
      tr(en: 'Invalid per-app settings JSON.', ru: 'Невалидный JSON настроек приложений.', zhHans: '按应用设置 JSON 无效。', zhHant: '各應用設定 JSON 無效。');
  String get appPresentationDefaultSummary =>
      tr(en: 'Default behavior', ru: 'Стандартное поведение', zhHans: '默认行为', zhHant: '預設行為');
  String get appPresentationTextSourceLabel =>
      tr(en: 'Island text source', ru: 'Источник текста для острова', zhHans: '岛文本来源', zhHant: '島文字來源');
  String get appPresentationIconSourceLabel =>
      tr(en: 'Icon source', ru: 'Источник иконки', zhHans: '图标来源', zhHant: '圖示來源');
  String get appPresentationTextTitle =>
      tr(en: 'Notification title', ru: 'Title уведомления', zhHans: '通知标题', zhHant: '通知標題');
  String get appPresentationTextNotification =>
      tr(en: 'Notification text', ru: 'Текст уведомления', zhHans: '通知内容', zhHant: '通知內容');
  String get appPresentationIconNotification =>
      tr(en: 'Notification icon', ru: 'Иконка уведомления', zhHans: '通知图标', zhHant: '通知圖示');
  String get appPresentationIconApp =>
      tr(en: 'Application icon', ru: 'Иконка приложения', zhHans: '应用图标', zhHant: '應用圖示');
  String get downloadSettings =>
      tr(en: 'Download settings', ru: 'Скачать настройки', zhHans: '下载设置', zhHant: '下載設定');
  String get uploadSettings => tr(en: 'Upload settings', ru: 'Загрузить настройки', zhHans: '上传设置', zhHant: '上傳設定');
  String get defaultLabel => tr(en: 'Default', ru: 'По умолчанию', zhHans: '默认', zhHant: '預設');
  String get resetToDefault =>
      tr(en: 'Reset to default', ru: 'Сбросить к стандарту', zhHans: '重置为默认', zhHant: '重設為預設');
  String get save => tr(en: 'Save', ru: 'Сохранить', zhHans: '保存', zhHant: '儲存');
  String get downloadDictionary =>
      tr(en: 'Download dictionary', ru: 'Скачать словарь', zhHans: '下载词典', zhHant: '下載字典');
  String get updateDictionary =>
      tr(en: 'Update dictionary', ru: 'Обновить словарь', zhHans: '更新词典', zhHant: '更新字典');
  String get uploadDictionary =>
      tr(en: 'Upload dictionary', ru: 'Загрузить словарь', zhHans: '上传词典', zhHant: '上傳字典');
  String get resetDictionary => tr(en: 'Reset dictionary', ru: 'Сбросить словарь', zhHans: '重置词典', zhHant: '重設字典');
  String get pickApps => tr(en: 'Pick applications', ru: 'Выбрать приложения', zhHans: '选择应用', zhHant: '選擇應用程式');
  String get pickerTitle =>
      tr(en: 'Choose apps for conversion', ru: 'Приложения для конвертации', zhHans: '选择用于转换的应用', zhHant: '選擇用於轉換的應用程式');
  String get otpPickerTitle =>
      tr(en: 'Choose apps for code detection', ru: 'Приложения для кодов', zhHans: '选择用于验证码识别的应用', zhHant: '選擇用於驗證碼辨識的應用程式');
  String get bypassPickerTitle =>
      tr(en: 'Choose apps for bypass', ru: 'Приложения bypass', zhHans: '选择用于绕过的应用', zhHant: '選擇用於繞過的應用程式');
  String get notificationDedupPickerTitle =>
      tr(en: 'Choose apps for notification dedup', ru: 'Приложения для dedup', zhHans: '选择用于通知去重的应用', zhHant: '選擇用於通知去重的應用程式');
  String get applySelection => tr(en: 'Apply selection', ru: 'Применить выбор', zhHans: '应用选择', zhHant: '套用選擇');
  String get searchAppHint =>
      tr(en: 'Search by app or package', ru: 'Поиск по названию или пакету', zhHans: '按应用名或包名搜索', zhHant: '依應用名稱或套件名稱搜尋');
  String get showSystemApps =>
      tr(en: 'Show system applications', ru: 'Показать системные приложения', zhHans: '显示系统应用', zhHant: '顯示系統應用程式');
  String get hideSystemApps =>
      tr(en: 'Hide system applications', ru: 'Скрыть системные приложения', zhHans: '隐藏系统应用', zhHant: '隱藏系統應用程式');
  String get appsLoadFailed =>
      tr(en: 'Unable to load installed apps list.', ru: 'Не удалось загрузить список приложений.', zhHans: '无法加载已安装应用列表。', zhHant: '無法載入已安裝應用清單。');
  String get appsAccessTitle =>
      tr(en: 'App list access', ru: 'Доступ к списку приложений', zhHans: '应用列表访问', zhHant: '應用清單存取');
  String get appsAccessMessage =>
      tr(en: 'Allow LiveBridge to read installed apps so you can pick apps for rules?', ru: 'Разрешить LiveBridge читать список установленных приложений для выбора правил?', zhHans: '允许 LiveBridge 读取已安装应用列表，以便为规则选择应用吗？', zhHant: '允許 LiveBridge 讀取已安裝應用清單，以便為規則選擇應用程式嗎？');
  String get appsAccessSaveFailed =>
      tr(en: 'Unable to save access preference.', ru: 'Не удалось сохранить выбор доступа.', zhHans: '无法保存访问偏好。', zhHant: '無法儲存存取偏好。');
  String get cancel => tr(en: 'Cancel', ru: 'Отмена', zhHans: '取消', zhHant: '取消');
  String get allow => tr(en: 'Allow', ru: 'Разрешить', zhHans: '允许', zhHant: '允許');
  String selectedAppsCount(int value) =>
      tr(en: 'Selected apps: $value', ru: 'Выбрано приложений: $value', zhHans: '已选择应用：$value', zhHant: '已選擇應用程式：$value');
  String get noAppsSelected =>
      tr(en: 'No applications selected', ru: 'Приложения не выбраны', zhHans: '未选择任何应用', zhHant: '未選擇任何應用程式');

  String get rulesTitle => tr(en: 'Conversion behavior', ru: 'Режим конвертации', zhHans: '转换行为', zhHant: '轉換行為');
  String get rulesSubtitle =>
      tr(en: 'Define what should be converted into Live Updates.', ru: 'Настройте, что именно превращать в Live Updates.', zhHans: '设置哪些通知应转换为 Live Updates。', zhHant: '設定哪些通知應轉換為 Live Updates。');
  String get modeLabel => tr(en: 'Application mode', ru: 'Режим работы', zhHans: '应用模式', zhHant: '應用模式');
  String get modeAll => tr(en: 'All applications', ru: 'Все приложения', zhHans: '所有应用', zhHant: '所有應用程式');
  String get modeInclude =>
      tr(en: 'Only listed applications', ru: 'Только указанные', zhHans: '仅指定应用', zhHant: '僅指定應用程式');
  String get modeExclude =>
      tr(en: 'Exclude listed applications', ru: 'Исключить указанные', zhHans: '排除指定应用', zhHant: '排除指定應用程式');
  String get pickAppsHint =>
      tr(en: 'Selected app list is used only for include/exclude modes.', ru: 'Список используется только в режимах "Только указанные" или "Исключить".', zhHans: '所选应用列表仅用于包含/排除模式。', zhHant: '所選應用清單僅用於包含/排除模式。');
  String get bypassRulesTitle => tr(en: 'Bypass apps', ru: 'Bypass-приложения', zhHans: '绕过应用', zhHant: '繞過應用程式');
  String get bypassRulesSubtitle =>
      tr(en: 'Listed apps are always converted to Live independently of settings.', ru: 'Приложения из списка всегда конвертируются в Live вне зависимости от настроек.', zhHans: '列表中的应用会始终转换为 Live，不受其他设置影响。', zhHant: '清單中的應用程式會始終轉換為 Live，不受其他設定影響。');
  String get saveRules => tr(en: 'Save', ru: 'Сохранить', zhHans: '保存', zhHant: '儲存');

  String get smartDetectionTitle =>
      tr(en: 'Smart status detection', ru: 'Умное распознавание', zhHans: '智能状态识别', zhHant: '智慧狀態辨識');
  String get smartCardTitle =>
      tr(en: 'Smart conversion', ru: 'Умное преобразование', zhHans: '智能转换', zhHant: '智慧轉換');
  String get smartCardSubtitle =>
      tr(en: 'Converts text-only stage updates into one Live progress flow.', ru: 'Преобразование текстовых этапов в один Live-прогресс.', zhHans: '将纯文本阶段更新转换为一个 Live 进度流。', zhHant: '將純文字階段更新轉換為一個 Live 進度流程。');
  String get smartDetectionSubtitle =>
      tr(en: 'Converts text-only food/taxi/navigation status notifications into a single Live.', ru: 'Преобразует текстовые статусы еды, такси и навигации в единый Live-прогресс.', zhHans: '将外卖/打车/导航等纯文本状态通知转换为单一 Live。', zhHant: '將外送/叫車/導航等純文字狀態通知轉換為單一 Live。');
  String get smartMediaPlaybackTitle =>
      tr(en: 'Media Playback', ru: 'Media Playback', zhHans: '媒体播放', zhHant: '媒體播放');
  String get smartMediaPlaybackSubtitle =>
      tr(en: 'Converts media playback notifications into Live. On some OEMs this may duplicate native media UI.', ru: 'Преобразует уведомления медиаплеера в Live. На некоторых OEM может дублировать нативный плеер.', zhHans: '将媒体播放通知转换为 Live；在某些 OEM 上可能与原生媒体界面重复。', zhHant: '將媒體播放通知轉換為 Live；在某些 OEM 上可能與原生媒體介面重複。');
  String get smartNavigationTitle =>
      tr(en: 'Navigation (maps)', ru: 'Навигация (карты)', zhHans: '导航（地图）', zhHant: '導航（地圖）');
  String get smartNavigationSubtitle =>
      tr(en: 'Navigation notification detection.', ru: 'Распознавание уведомлений навигации.', zhHans: '导航通知识别。', zhHant: '導航通知辨識。');
  String get smartWeatherTitle => tr(en: 'Weather', ru: 'Погода', zhHans: '天气', zhHant: '天氣');
  String get smartWeatherSubtitle =>
      tr(en: 'Weather notification detection (temperature in island).', ru: 'Распознавание погодных уведомлений (температура в острове).', zhHans: '天气通知识别（岛内显示温度）。', zhHant: '天氣通知辨識（島內顯示溫度）。');
  String get smartExternalDevicesTitle =>
      tr(en: 'External devices', ru: 'Внешние устройства', zhHans: '外接设备', zhHant: '外接裝置');
  String get smartExternalDevicesSubtitle =>
      tr(en: 'Shows connected/connecting status and device name in island.', ru: 'Показывает статус connected/connecting и имя устройства в острове.', zhHans: '在岛内显示已连接/连接中状态及设备名称。', zhHant: '在島內顯示已連線/連線中狀態與裝置名稱。');
  String get smartVpnTitle => tr(en: 'VPN services', ru: 'VPN-сервисы', zhHans: 'VPN 服务', zhHant: 'VPN 服務');
  String get smartVpnSubtitle =>
      tr(en: 'Shows incoming/outgoing traffic speed in *b/s format.', ru: 'Показывает входящий/исходящий трафик в формате *b/s.', zhHans: '以 *b/s 格式显示上/下行流量速度。', zhHant: '以 *b/s 格式顯示上/下行流量速度。');
  String get smartNavigationDisabledSubtitle =>
      tr(en: 'Enable smart status detection first.', ru: 'Сначала включите умное распознавание.', zhHans: '请先启用智能状态识别。', zhHant: '請先啟用智慧狀態辨識。');
  String get smartDetectionDisabledSubtitle =>
      tr(en: 'Disabled while "Progress" mode is enabled.', ru: 'Отключено в режиме "Прогресс".', zhHans: '启用“进度”模式时此项会禁用。', zhHant: '啟用「進度」模式時此項會停用。');
  String get conflictingModesHint =>
      tr(en: 'Turn off "Progress" mode to enable food/taxi/navigation text status recognition.', ru: 'Чтобы работали текстовые статусы, отключите режим "Прогресс".', zhHans: '要启用外卖/打车/导航文本状态识别，请关闭“进度”模式。', zhHant: '要啟用外送/叫車/導航文字狀態辨識，請關閉「進度」模式。');
  String get onlyProgressTitle => tr(en: 'Progress', ru: 'Прогресс', zhHans: '进度', zhHant: '進度');
  String get onlyProgressSubtitle =>
      tr(en: 'When enabled, only notifications with a system progress bar are converted.', ru: 'Если включено, конвертируются только уведомления с системным прогрессбаром.', zhHans: '开启后，仅转换带系统进度条的通知。', zhHant: '開啟後，僅轉換帶有系統進度條的通知。');
  String get textProgressTitle =>
      tr(en: 'Text progress', ru: 'Текстовые прогрессы', zhHans: '文本进度', zhHant: '文字進度');
  String get textProgressSubtitle =>
      tr(en: 'If text contains % and it is not discount-related, treat it as progress and update island.', ru: 'Если в тексте есть %, и это не скидка/акция, считать как прогресс и обновлять остров.', zhHans: '若文本包含 % 且并非折扣/促销信息，则视为进度并更新岛。', zhHant: '若文字包含 % 且非折扣/促銷資訊，則視為進度並更新島。');

  String get blockedTitle =>
      tr(en: 'AOSP is partially supported', ru: 'AOSP поддерживается частично', zhHans: 'AOSP 仅部分支持', zhHant: 'AOSP 僅部分支援');
  String get blockedSubtitle =>
      tr(en: 'LiveBridge is not designed for AOSP. You can continue, but i am not responsible for any bugs.', ru: 'LiveBridge плохо работает на устройствах с AOSP. Можете продолжить, но за последствия я не отвечаю.', zhHans: 'LiveBridge 并非为 AOSP 设计。你可以继续，但出现问题需自行承担。', zhHant: 'LiveBridge 並非為 AOSP 設計。你可以繼續，但出現問題需自行承擔。');
  String get blockedBypassAction =>
      tr(en: 'Continue anyway', ru: 'Все равно родолжить', zhHans: '仍然继续', zhHant: '仍要繼續');
  String get blockedBypassSaveFailed =>
      tr(en: 'Unable to save your choice.', ru: 'Не удалось сохранить выбор.', zhHans: '无法保存你的选择。', zhHant: '無法儲存你的選擇。');

  String get otpTitle => tr(en: 'Verification codes', ru: 'Коды подтверждения', zhHans: '验证码', zhHant: '驗證碼');
  String get otpSubtitle =>
      tr(en: 'Shows the code in compact island.', ru: 'Показывает код компактно в острове.', zhHans: '在紧凑岛中显示验证码。', zhHant: '在精簡島中顯示驗證碼。');
  String get otpEnabledTitle =>
      tr(en: 'Detect verification codes', ru: 'Распознавать 2FA коды', zhHans: '识别验证码', zhHant: '辨識驗證碼');
  String get otpEnabledSubtitle =>
      tr(en: 'Shows the numeric code in the compact island.', ru: 'В свернутом Live-острове показывается сам код.', zhHans: '在紧凑岛中显示数字验证码。', zhHant: '在精簡島中顯示數字驗證碼。');
  String get otpAutoCopyTitle =>
      tr(en: 'Auto-copy code', ru: 'Автокопирование кода', zhHans: '自动复制验证码', zhHant: '自動複製驗證碼');
  String get otpAutoCopySubtitle =>
      tr(en: 'Code is copied to clipboard automatically.', ru: 'Код сразу копируется в буфер обмена.', zhHans: '验证码会自动复制到剪贴板。', zhHant: '驗證碼會自動複製到剪貼簿。');
  String get otpAutoCopyDisabledSubtitle =>
      tr(en: 'Enable code detection first.', ru: 'Сначала включите распознавание кодов.', zhHans: '请先启用验证码识别。', zhHant: '請先啟用驗證碼辨識。');
  String get otpModeLabel => tr(en: 'Code apps mode', ru: 'Режим для кодов', zhHans: '验证码应用模式', zhHant: '驗證碼應用模式');
  String get saveOtpRules => tr(en: 'Save', ru: 'Сохранить', zhHans: '保存', zhHant: '儲存');
}
