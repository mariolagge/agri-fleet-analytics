// English and German display text. Every visible word on the page comes from here,
// looked up with t(key). Static markup carries data-i18n="key"; the scripts call t()
// while they render. German terms are listed next to the English ones in GLOSSARY.md.

const TRANSLATIONS = {
  en: {
    'asset.TRACTOR': 'Tractor',
    'asset.MOWER': 'Mower',
    'asset.BALER': 'Baler',
    'asset.MIXER': 'Feed mixer',
    'asset.all': 'All machines',

    'status.GOOD': 'Good',
    'status.WATCH': 'Watch',
    'status.CRITICAL': 'Critical',

    'recommendation.GOOD': 'No inspection recommended at this time.',
    'recommendation.WATCH': 'Plan inspection within the next 15–20 engine hours.',
    'recommendation.CRITICAL': 'Priority check recommended within the next 5 engine hours.',

    'live.NORMAL': 'Normal',
    'live.CAUTION': 'Caution',
    'live.ALERT': 'Alert',
    'live.STALE': 'No signal',

    'flag.HEAVY_RECENT_USE': 'Heavy recent use',
    'flag.THERMAL_LOAD': 'Thermal load',
    'flag.MECHANICAL_ATTENTION': 'Mechanical attention',
    'flag.HIGH_CUMULATIVE_LOAD': 'High cumulative load',
    'flag.none': 'No active flags',

    'scope.LIFETIME': 'lifetime',
    'scope.ROLLING_24H': '24 h',

    'counter.engineHours': 'Engine',
    'counter.totalWorkingHours': 'Working',
    'counter.highLoadHours': 'High load',
    'counter.tempExcursionHours': 'Temp excursion',
    'counter.vibrationAlertHours': 'Vibration alert',

    'vibration.NORMAL': 'Normal',
    'vibration.ELEVATED': 'Elevated',
    'vibration.HIGH': 'High',

    'common.yes': 'Yes',
    'common.no': 'No',
    'common.close': 'Close',

    'fleet.loading': 'Loading fleet ranking…',
    'fleet.utilSuffix': 'util',
    'fleet.error': 'Could not load the fleet.',
    'fleet.alert': 'Could not load the fleet. {error}. Check that OperatorApi is running behind the /api proxy.',

    'detail.title': 'Asset detail',
    'detail.empty': 'Select a machine in the fleet ranking.',
    'detail.priorityRank': 'Priority rank',
    'detail.liveCondition': 'Live condition',
    'detail.utilization': 'Utilization',
    'detail.productiveShare': 'Productive share',
    'detail.windowCoverage': 'Window coverage',
    'detail.whyFlagged': 'Why it is flagged',
    'detail.noFlags': 'No active flags.',
    'detail.valueOfThreshold': '{value} of {threshold}',

    'counters.rolling': 'Rolling 24 h',
    'counters.lifetime': 'Lifetime',
    'counters.noMachine': 'No machine selected.',

    'telemetry.title': 'Live telemetry',
    'telemetry.loading': 'Loading live telemetry…',
    'telemetry.error': 'Could not load live telemetry.',
    'telemetry.noReading': 'No reading in this run',
    'telemetry.pto': 'PTO',
    'telemetry.speed': 'Speed',
    'telemetry.load': 'Load',
    'telemetry.temp': 'Temp',
    'telemetry.vibration': 'Vibration',
    'telemetry.at': 'At',

    'history.title': 'Hourly history',
    'history.from': 'From',
    'history.to': 'To',
    'history.machine': 'Machine',
    'history.hour': 'Hour',
    'history.load': 'Load',
    'history.prompt': 'Choose a period and click Load.',
    'history.loading': 'Loading hourly history…',
    'history.empty': 'No hourly data for this period.',
    'history.error.fromBeforeTo': 'from must be before to',
    'history.error.rangeTooLong': 'the requested range must not exceed 7 days',
  },

  de: {
    'asset.TRACTOR': 'Traktor',
    'asset.MOWER': 'Mähwerk',
    'asset.BALER': 'Ballenpresse',
    'asset.MIXER': 'Mischwagen',
    'asset.all': 'Alle Maschinen',

    'status.GOOD': 'Gut',
    'status.WATCH': 'Beobachten',
    'status.CRITICAL': 'Kritisch',

    'recommendation.GOOD': 'Derzeit keine Inspektion empfohlen.',
    'recommendation.WATCH': 'Inspektion innerhalb der nächsten 15–20 Motorstunden einplanen.',
    'recommendation.CRITICAL': 'Vorrangige Prüfung innerhalb der nächsten 5 Motorstunden empfohlen.',

    'live.NORMAL': 'Normal',
    'live.CAUTION': 'Vorsicht',
    'live.ALERT': 'Alarm',
    'live.STALE': 'Kein Signal',

    'flag.HEAVY_RECENT_USE': 'Hohe aktuelle Nutzung',
    'flag.THERMAL_LOAD': 'Thermische Belastung',
    'flag.MECHANICAL_ATTENTION': 'Mechanische Auffälligkeit',
    'flag.HIGH_CUMULATIVE_LOAD': 'Hohe Gesamtbelastung',
    'flag.none': 'Keine aktiven Warnungen',

    'scope.LIFETIME': 'gesamt',
    'scope.ROLLING_24H': '24 h',

    'counter.engineHours': 'Motor',
    'counter.totalWorkingHours': 'Arbeit',
    'counter.highLoadHours': 'Hohe Last',
    'counter.tempExcursionHours': 'Übertemperatur',
    'counter.vibrationAlertHours': 'Vibrationsalarm',

    'vibration.NORMAL': 'Normal',
    'vibration.ELEVATED': 'Erhöht',
    'vibration.HIGH': 'Hoch',

    'common.yes': 'Ja',
    'common.no': 'Nein',
    'common.close': 'Schließen',

    'fleet.loading': 'Flottenrangliste wird geladen…',
    'fleet.utilSuffix': 'Ausl.',
    'fleet.error': 'Die Flotte konnte nicht geladen werden.',
    'fleet.alert': 'Die Flotte konnte nicht geladen werden. {error}. Prüfen Sie, ob OperatorApi hinter dem /api-Proxy läuft.',

    'detail.title': 'Maschinendetails',
    'detail.empty': 'Wählen Sie eine Maschine in der Flottenrangliste.',
    'detail.priorityRank': 'Prioritätsrang',
    'detail.liveCondition': 'Live-Zustand',
    'detail.utilization': 'Auslastung',
    'detail.productiveShare': 'Produktiver Anteil',
    'detail.windowCoverage': 'Abgedeckter Zeitraum',
    'detail.whyFlagged': 'Warum markiert',
    'detail.noFlags': 'Keine aktiven Warnungen.',
    'detail.valueOfThreshold': '{value} von {threshold}',

    'counters.rolling': 'Letzte 24 h',
    'counters.lifetime': 'Gesamtlaufzeit',
    'counters.noMachine': 'Keine Maschine ausgewählt.',

    'telemetry.title': 'Live-Telemetrie',
    'telemetry.loading': 'Live-Telemetrie wird geladen…',
    'telemetry.error': 'Live-Telemetrie konnte nicht geladen werden.',
    'telemetry.noReading': 'Kein Messwert in diesem Lauf',
    'telemetry.pto': 'Zapfwelle',
    'telemetry.speed': 'Geschw.',
    'telemetry.load': 'Last',
    'telemetry.temp': 'Temp.',
    'telemetry.vibration': 'Vibration',
    'telemetry.at': 'Um',

    'history.title': 'Stündlicher Verlauf',
    'history.from': 'Von',
    'history.to': 'Bis',
    'history.machine': 'Maschine',
    'history.hour': 'Stunde',
    'history.load': 'Laden',
    'history.prompt': 'Zeitraum wählen und auf Laden klicken.',
    'history.loading': 'Stündlicher Verlauf wird geladen…',
    'history.empty': 'Keine Stundendaten für diesen Zeitraum.',
    'history.error.fromBeforeTo': '„Von“ muss vor „Bis“ liegen.',
    'history.error.rangeTooLong': 'Der Zeitraum darf höchstens 7 Tage umfassen.',
  },
};

const SUPPORTED_LANGUAGES = ['en', 'de'];
const LANGUAGE_STORAGE_KEY = 'agrifleet-language';

// Storage can be blocked (private window, cleared site data); the page then starts in English.
function loadSavedLanguage() {
  try {
    const saved = localStorage.getItem(LANGUAGE_STORAGE_KEY);
    return SUPPORTED_LANGUAGES.includes(saved) ? saved : 'en';
  } catch (ignored) {
    return 'en';
  }
}

let currentLanguage = loadSavedLanguage();

// Looks up a text in the current language, falling back to English, then to the key itself.
// {name} placeholders are filled from the values object.
function t(key, values = {}) {
  const text = TRANSLATIONS[currentLanguage][key] ?? TRANSLATIONS.en[key] ?? key;
  return text.replace(/\{(\w+)\}/g, (placeholder, name) => values[name] ?? placeholder);
}

// The locale for number and date formatting: German decimals use a comma.
function currentLocale() {
  return currentLanguage === 'de' ? 'de-AT' : 'en-GB';
}

function applyStaticTranslations() {
  document.documentElement.lang = currentLanguage;
  document.querySelectorAll('[data-i18n]').forEach((element) => {
    element.textContent = t(element.dataset.i18n);
  });
  document.querySelectorAll('.lang-btn').forEach((button) => {
    const isActive = button.dataset.language === currentLanguage;
    button.classList.toggle('btn-primary', isActive);
    button.setAttribute('aria-pressed', String(isActive));
  });
}

function setLanguage(language, onLanguageChange) {
  currentLanguage = language;
  try {
    localStorage.setItem(LANGUAGE_STORAGE_KEY, language);
  } catch (ignored) {
    // Not saved — the choice still holds until the page is reloaded.
  }
  applyStaticTranslations();
  onLanguageChange();
}

function initLanguageToggle(onLanguageChange) {
  document.querySelectorAll('.lang-btn').forEach((button) => {
    button.addEventListener('click', () => setLanguage(button.dataset.language, onLanguageChange));
  });
  applyStaticTranslations();
}
