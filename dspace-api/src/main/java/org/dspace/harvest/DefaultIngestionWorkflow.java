package org.dspace.harvest;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.InstallItem;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.Context;
import org.jdom.Element;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class DefaultIngestionWorkflow implements IngestionWorkflow
{
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
            return InstallItem.installItem(context, wi, handle);
            //item = InstallItem.installItem(ourContext, wi);
        }
        // clean up the workspace item if something goes wrong before
        catch (SQLException se)
        {
            wi.deleteWrapper();
            throw se;
        }
        catch (IOException ioe)
        {
            wi.deleteWrapper();
            throw ioe;
        }
        catch (AuthorizeException ae)
        {
            wi.deleteWrapper();
            throw ae;
        }
    }

    @Override
    public boolean updateBitstreams(Context context, Item item, HarvestedItem hi)
            throws SQLException, IOException, AuthorizeException
    {
        return true;
    }
}
