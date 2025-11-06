package ch.openbis.drive.gui.i18n;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.Callable;

public class I18n {
    public static final List<String> SUPPORTED_LANGUAGES = List.of("en", "de", "it", "es", "fr");
    private final ObjectProperty<String> language;

    public I18n(String initLanguage) {
        initLanguage = normalizeLanguageLabel(initLanguage);
        this.language = new SimpleObjectProperty<>(initLanguage != null && SUPPORTED_LANGUAGES.contains(initLanguage) ? initLanguage : getDefaultLanguage());
        language.addListener((observable, oldValue, newValue) -> {});
    }

    public static List<String> getSupportedLanguages() {
        return SUPPORTED_LANGUAGES;
    }

    /**
     * get the default language: system default if possibles, else English
     */
    public static String getDefaultLanguage() {
        return getDefaultLocale().getLanguage();
    }

    public static Locale getDefaultLocale() {
        Locale sysDefault = Locale.getDefault();
        return getSupportedLanguages().contains(sysDefault.getLanguage()) ? sysDefault : Locale.ENGLISH;
    }

    public String getLanguage() {
        return language.get();
    }

    public void setLanguage(String language) {
        languageProperty().set(normalizeLanguageLabel(language));
    }

    public ObjectProperty<String> languageProperty() {
        return language;
    }

    /**
     * get string template from key from resource bundle for selected language
     * and format it with MessageFormat.format, supplying optional args
     *
     * @param key
     *         message template key
     * @param args
     *         optional arguments for template
     * @return localized formatted string
     */
    public String get(final String key, final Object... args) {
        ResourceBundle bundle = ResourceBundle.getBundle("message", getLocaleForLanguageTagOrDefault(getLanguage()));
        return MessageFormat.format(bundle.getString(key), args);
    }

    static public Locale getLocaleForLanguageTagOrDefault(String languageTag) {
        try {
            return Locale.forLanguageTag(normalizeLanguageLabel(languageTag));
        } catch (Exception e) {
            System.err.printf("Could not get Locale for language-tag: %s%n", languageTag);
            return getDefaultLocale();
        }
    }

    static public ResourceBundle getResourceBundleForLanguageTagOrDefault(String languageTag) {
        try {
            return ResourceBundle.getBundle("message", getLocaleForLanguageTagOrDefault(languageTag));
        } catch (Exception e) {
            System.err.printf("Could not get ResourceBundle for language-tag: %s%n", languageTag);
            return ResourceBundle.getBundle("message", getDefaultLocale());
        }
    }

    /**
     * creates a binding with a message translation String for the given message resource bundle key
     *
     * @param key
     *         key
     * @return String binding
     */
    public StringBinding createStringBinding(final String key, Object... args) {
        return Bindings.createStringBinding(() -> get(key, args), language);
    }

    /**
     * creates a binding with a message String computed through Callable function
     *
     * @param func
     *         function called on every change
     * @return StringBinding
     */
    public StringBinding createStringBinding(Callable<String> func) {
        return Bindings.createStringBinding(func, language);
    }

    static String normalizeLanguageLabel(String languageLabel) {
        if(languageLabel != null) {
            return languageLabel.trim().toLowerCase();
        } else {
            return null;
        }
    }
}
