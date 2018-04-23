package no.uio.duo.policy;

import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PolicyApplicationFilter
{
    /**
     * Determine if the policy pattern can be run on this item, according to the configuration
     *
     * @param context
     * @param item
     * @return
     * @throws SQLException
     */
    public static boolean allow(Context context, Item item)
            throws SQLException
    {
        // don't apply policies to items that are in the workflow:
        if (!item.isArchived() && !item.isWithdrawn())
        {
            return false;
        }

        String scopeCfg = ConfigurationManager.getProperty("duo.embargo.communities");
        if (scopeCfg == null)
        {
            return true;
        }

        HandleService handleService = HandleServiceFactory.getInstance().getHandleService();

        List<UUID> communityIDs = new ArrayList<UUID>();
        String[] handles = scopeCfg.split(",");
        for (String handle : handles)
        {
            handle = handle.trim();
            DSpaceObject dso = handleService.resolveToObject(context, handle);
            if (dso instanceof Community)
            {
                communityIDs.add(dso.getID());
            }
        }

        ItemService itemService = ContentServiceFactory.getInstance().getItemService();
        List<Community> coms = itemService.getCommunities(context, item);
        for (Community com : coms)
        {
            if (communityIDs.contains(com.getID()))
            {
                return true;
            }
        }

        return false;
    }
}
