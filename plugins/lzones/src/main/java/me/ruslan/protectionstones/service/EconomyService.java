package me.ruslan.protectionstones.service;

import java.lang.reflect.Method;
import java.util.Locale;
import me.ruslan.protectionstones.ProtectionStonesPlugin;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public final class EconomyService {
    private final ProtectionStonesPlugin plugin;

    public EconomyService(ProtectionStonesPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isAvailable() {
        return this.provider() != null;
    }

    public boolean has(Player player, double amount) {
        Object economy = this.provider();
        if (economy == null) {
            return false;
        }
        Boolean modern = this.invokeBoolean(economy, "has", new Class<?>[]{OfflinePlayer.class, double.class}, new Object[]{player, amount});
        if (modern != null) {
            return modern;
        }
        Boolean legacy = this.invokeBoolean(economy, "has", new Class<?>[]{String.class, double.class}, new Object[]{player.getName(), amount});
        return legacy != null && legacy;
    }

    public boolean withdraw(Player player, double amount) {
        Object economy = this.provider();
        if (economy == null) {
            return false;
        }
        Boolean modern = this.invokeTransaction(economy, "withdrawPlayer", new Class<?>[]{OfflinePlayer.class, double.class}, new Object[]{player, amount});
        if (modern != null) {
            return modern;
        }
        Boolean legacy = this.invokeTransaction(economy, "withdrawPlayer", new Class<?>[]{String.class, double.class}, new Object[]{player.getName(), amount});
        return legacy != null && legacy;
    }

    public String format(double amount) {
        Object economy = this.provider();
        if (economy == null) {
            return String.format(Locale.US, "$%,.0f", amount);
        }
        try {
            Method method = economy.getClass().getMethod("format", double.class);
            Object value = method.invoke(economy, amount);
            if (value instanceof String formatted) {
                return formatted;
            }
        }
        catch (ReflectiveOperationException ignored) {
        }
        return String.format(Locale.US, "$%,.0f", amount);
    }

    private Object provider() {
        try {
            Class<?> economyClass = Class.forName("net.milkbowl.vault.economy.Economy");
            RegisteredServiceProvider<?> registration = this.plugin.getServer().getServicesManager().getRegistration(economyClass);
            return registration == null ? null : registration.getProvider();
        }
        catch (ClassNotFoundException exception) {
            return null;
        }
    }

    private Boolean invokeBoolean(Object target, String methodName, Class<?>[] types, Object[] args) {
        try {
            Method method = target.getClass().getMethod(methodName, types);
            Object result = method.invoke(target, args);
            return result instanceof Boolean value ? value : null;
        }
        catch (ReflectiveOperationException exception) {
            return null;
        }
    }

    private Boolean invokeTransaction(Object target, String methodName, Class<?>[] types, Object[] args) {
        try {
            Method method = target.getClass().getMethod(methodName, types);
            Object response = method.invoke(target, args);
            Method success = response.getClass().getMethod("transactionSuccess");
            Object result = success.invoke(response);
            return result instanceof Boolean value ? value : null;
        }
        catch (ReflectiveOperationException exception) {
            return null;
        }
    }
}
