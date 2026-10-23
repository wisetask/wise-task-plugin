package ru.leti.wise.task.plugin.error;

public class PluginExecutionException extends RuntimeException {
    private final String pluginLogs;

    public PluginExecutionException(String pluginLogs) {
        super(pluginLogs);
        this.pluginLogs = pluginLogs;
    }

    public String getPluginLogs() {
        return pluginLogs;
    }
}
