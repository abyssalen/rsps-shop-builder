package org.bitbucket.shaigem.rssb.plugin;

import net.xeoh.plugins.base.Plugin;
import net.xeoh.plugins.base.PluginInformation;
import net.xeoh.plugins.base.impl.PluginManagerFactory;
import net.xeoh.plugins.base.options.addpluginsfrom.OptionReportAfter;
import net.xeoh.plugins.base.util.PluginManagerUtil;

import java.io.File;
import java.net.URI;
import java.util.Collection;
import java.util.Optional;

/**
 * Created on 30/08/16.
 */
public final class RSSBPluginManager {

    private RSSBPluginManager() {

    }

    public static final RSSBPluginManager INSTANCE = new RSSBPluginManager();
    public static final boolean DEBUG = true;

    private PluginManagerUtil pluginManager;

    private PluginInformation pluginInformation;

    private static URI DEFAULT_SHOP_PLUGINS_URI = RSSBPluginManager.DEBUG ?
            new File("rssb-plugin-matrix/target/classes/").toURI() :
            new File("./plugins/").toURI();


    public void initialize(URI... pluginURIs) {
        final net.xeoh.plugins.base.PluginManager pm = PluginManagerFactory.createPluginManager();
        for (URI pluginURI : pluginURIs) {
            pm.addPluginsFrom(pluginURI, new OptionReportAfter());
        }
        pluginManager = new PluginManagerUtil(pm);
        pluginInformation = pluginManager.getPlugin(PluginInformation.class);
    }

    public void initializeShopFormatPlugins() {
        initialize(DEFAULT_SHOP_PLUGINS_URI);

    }

    public boolean refreshShopFormatPlugins() {
        int pluginSize = pluginManager.getPlugins(ShopFormatPlugin.class).size();
        pluginManager.addPluginsFrom(DEFAULT_SHOP_PLUGINS_URI, new OptionReportAfter());
        int newSize = pluginManager.getPlugins(ShopFormatPlugin.class).size();
        return pluginSize != newSize;
    }

    public Optional<String> getAuthorForPlugin(Plugin plugin) {
        return pluginInformation.getInformation
                (PluginInformation.Information.AUTHORS, plugin).stream().findFirst();
    }

    public Optional<String> getVersionForPlugin(Plugin plugin) {
        return pluginInformation.getInformation
                (PluginInformation.Information.VERSION, plugin).stream().findFirst();
    }

    public Collection<ShopFormatPlugin> getLoadedPlugins() {
        return pluginManager.getPlugins(ShopFormatPlugin.class);
    }

    public void shutdown() {
        if (pluginManager != null)
            pluginManager.shutdown();
    }

}
