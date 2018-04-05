package org.dspace.harvest;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
// import org.dspace.content.InstallItem;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.InstallItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Context;
import org.jdom.Element;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class DefaultIngestionWorkflow implements IngestionWorkflow
{
    @Autowired(required = true)
    protected InstallItemService installItemService;

    @Autowired(required = true)
    protected WorkspaceItemService workspaceItemService;

    @Override
    public Item preUpdate(Context context, Item item, Collection targetCollection, HarvestedItem hi, List<Element> descMd, Element oreREM)
    {
        return item;
    }

    @Override
    public void postUpdate(Context context, Item item)
    {

    }

    @Override
    public Item postCreate(Context context, WorkspaceItem wi, String handle)
            throws SQLException, IOException, AuthorizeException
    {
        try
        {
            return installItemService.installItem(context, wi, handle);
            //return InstallItem.installItem(context, wi, handle);
        }
        // clean up the workspace item if something goes wrong before
        catch (SQLException | IOException | AuthorizeException e)
        {
            workspaceItemService.deleteWrapper(context, wi);
            // wi.deleteWrapper();
            throw e;
        }
        /*
        catch (IOException ioe)
        {
            workspaceItemService.deleteWrapper(context, wi);
            // wi.deleteWrapper();
            throw ioe;
        }
        catch (AuthorizeException ae)
        {
            workspaceItemService.deleteWrapper(context, wi);
            // wi.deleteWrapper();
            throw ae;
        }*/
    }

    @Override
    public boolean updateBitstreams(Context context, Item item, HarvestedItem hi)
            throws SQLException, IOException, AuthorizeException
    {
        return true;
    }
}
