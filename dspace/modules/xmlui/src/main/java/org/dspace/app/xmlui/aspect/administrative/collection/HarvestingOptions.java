package org.dspace.app.xmlui.aspect.administrative.collection;

import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

import java.util.HashMap;
import java.util.Map;

public class HarvestingOptions
{
    public static String getDisplayValue(String plugin, String key)
    {
        Map<String, String> options = HarvestingOptions.getOptions(plugin);
        if (options.containsKey(key))
        {
            return options.get(key);
        }
        return "";
    }

    public static Map<String, String> getOptions(String plugin)
    {
        Map<String, String> options = new HashMap<String, String>();
        ConfigurationService cfgService = DSpaceServicesFactory.getInstance().getConfigurationService();
        String[] cfgs = cfgService.getArrayProperty("oai.harvester." + plugin + ".option");
        for (String cfg : cfgs)
        {
            String[] parts = cfg.split("\\:");
            if (parts.length != 2)
            {
                continue;
            }
            options.put(parts[0].trim(), parts[1].trim());
        }
        return options;
    }
}
