package com.routz.fabricdemo.integration.util;

import org.hyperledger.fabric.sdk.helper.Config;

import java.lang.reflect.Field;

public class ConfigUtils {
    public static final String CONFIG_OVERRIDES = "FABRICSDKOVERRIDES";

    /**
     * Reset config.
     */
    public static void resetConfig() {
        try {
            final Field field = Config.class.getDeclaredField("config");
            field.setAccessible(true);
            field.set(Config.class, null);
            Config.getConfig();
        } catch (Exception e) {
            throw new RuntimeException("Cannot reset config", e);
        }
    }

    /**
     * customizeConfig() sets up the properties listed by env var CONFIG_OVERRIDES The value of the env var is
     * <i>property1=value1,property2=value2</i> and so on where each <i>property</i> is a property from the SDK's config
     * file.
     *
     * @throws NoSuchFieldException
     * @throws SecurityException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     */
    public static void customizeConfig()
            throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        String fabricSdkConfig = System.getenv(CONFIG_OVERRIDES);
        if (fabricSdkConfig != null && fabricSdkConfig.length() > 0) {
            String[] configs = fabricSdkConfig.split(",");
            String[] configKeyValue;
            for (String config : configs) {
                configKeyValue = config.split("=");
                if (configKeyValue != null && configKeyValue.length == 2) {
                    System.setProperty(configKeyValue[0], configKeyValue[1]);
                }
            }
        }
    }
}
