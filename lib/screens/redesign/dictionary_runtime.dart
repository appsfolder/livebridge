class DictionaryLanguageOption {
  const DictionaryLanguageOption({
    required this.id,
    required this.label,
    required this.assetFileName,
  });

  final String id;
  final String label;
  final String assetFileName;
}

const List<DictionaryLanguageOption> lbDictionaryLanguages =
    <DictionaryLanguageOption>[
      DictionaryLanguageOption(
        id: 'en',
        label: 'English',
        assetFileName: 'liveupdate_dictionary_en.json',
      ),
      DictionaryLanguageOption(
        id: 'pt-br',
        label: 'Português (Brasil)',
        assetFileName: 'liveupdate_dictionary_pt-BR.json',
      ),
      DictionaryLanguageOption(
        id: 'ru',
        label: 'Русский',
        assetFileName: 'liveupdate_dictionary_ru.json',
      ),
      DictionaryLanguageOption(
        id: 'zh',
        label: '中文',
        assetFileName: 'liveupdate_dictionary_zh.json',
      ),
      DictionaryLanguageOption(
        id: 'ko',
        label: '한국어',
        assetFileName: 'liveupdate_dictionary_ko.json',
      ),
    ];

const String lbDictionaryRemoteBaseUrl =
    'https://raw.githubusercontent.com/appsfolder/livebridge/refs/heads/main/android/app/src/main/assets';

String lbDictionaryRemoteUrl(DictionaryLanguageOption language) =>
    '$lbDictionaryRemoteBaseUrl/${language.assetFileName}';

Set<String> lbNormalizeDictionaryLanguageIds(Iterable<String> raw) {
  final Set<String> supported = lbDictionaryLanguages
      .map((DictionaryLanguageOption language) => language.id)
      .toSet();
  return raw
      .map((String value) => value.trim().toLowerCase())
      .where((String value) => supported.contains(value))
      .toSet();
}
