package dev.nova.util;

import dev.nova.NovaClient;
import dev.nova.module.Module;

/** Безопасный доступ к модулям из миксинов (миксин может сработать до инициализации мода). */
public final class Hooks {
    private Hooks() {}

    public static <T extends Module> T get(Class<T> type) {
        if (NovaClient.modules() == null) return null;
        return NovaClient.modules().get(type);
    }

    public static boolean on(Class<? extends Module> type) {
        Module m = get(type);
        return m != null && m.enabled();
    }
}
