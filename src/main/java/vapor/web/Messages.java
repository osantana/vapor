package vapor.web;

import java.util.Locale;
import java.util.ResourceBundle;

public class Messages {
    private ResourceBundle bundle;

    public static Messages forLanguage(String language) {
        Locale locale = Locale.ENGLISH;
        if (language.equals("pt_BR")) {
            locale = Locale.of("pt", "BR");
        }
        ResourceBundle.Control control = ResourceBundle.Control.getNoFallbackControl(
                ResourceBundle.Control.FORMAT_PROPERTIES);
        Messages messages = new Messages();
        messages.bundle = ResourceBundle.getBundle("messages", locale, control);
        return messages;
    }

    public String get(String key) {
        if (!bundle.containsKey(key)) {
            return key;
        }
        return bundle.getString(key);
    }

    public String get(String key, String argument) {
        return get(key).replace("{0}", argument);
    }
}
