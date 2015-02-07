package net.lightbody.bmp.l10n;

import java.util.ResourceBundle;

/**
 * Convenience class to retrieve messages from the localized BrowserMob Proxy resources file. Loads messages for the default locale.
 */
public class MessagesUtil {
    private static final String BROWSERMOB_MESSAGE_BUNDLE_NAME = "net.lightbody.bmp.l10n.messages";

    private static final ResourceBundle MESSAGES_BUNDLE = ResourceBundle.getBundle(BROWSERMOB_MESSAGE_BUNDLE_NAME);

    /**
     * Retrieves the message with the given key from the locale-specific properties file. If there are any String.format()-style
     * parameters in the message, this method can optionally apply formatting parameters.
     *
     * @param key message key
     * @param parameters formatting parameters to apply to the message
     * @return formatted, locale-specific message
     */
    public static String getMessage(String key, Object... parameters) {
        String response = MESSAGES_BUNDLE.getString(key);

        if (parameters == null || parameters.length == 0) {
            return response;
        } else {
            return String.format(response, parameters);
        }
    }
}
