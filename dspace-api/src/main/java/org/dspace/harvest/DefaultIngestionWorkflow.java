/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
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

/**
 * DSpace default implementation of the OAI harvester ingestion workflow
 */
public class DefaultIngestionWorkflow implements IngestionWorkflow
{
    /**
     * Called before the item to be updated is updated.  Default workflow updates the existing item
     * directly
     *
     * @param context
     * @param item
     * @param targetCollection
     * @param hi
     * @param descMd    A list of elements in the OAI-PMH metadata element
     * @param oreREM    ORE Resource Map - may be null, depending on how the collection is set up
     * @return  The item that was passed in.
     */
    @Override
    public Item preUpdate(Context context, Item item, Collection targetCollection, HarvestedItem hi, List<Element> descMd, Element oreREM)
    {
        return item;
    }

    /**
     * Called after update.  Has no effect
     *
     * @param context
     * @param item
     */
    @Override
    public void postUpdate(Context context, Item item) { }

    /**
     * Called after creation of a new item.  It installs the item directly into the DSpace archive
     * @param context
     * @param wi
     * @param handle
     * @return
     * @throws SQLException
     * @throws IOException
     * @throws AuthorizeException
     */
    @Override
    public Item postCreate(Context context, WorkspaceItem wi, String handle)
            throws SQLException, IOException, AuthorizeException
    {
        try
        {
            return InstallItem.installItem(context, wi, handle);
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

    /**
     * Called to determine if bitstreams should be updated.  Always returns true.
     *
     * @param context
     * @param item
     * @param hi
     * @return  true
     * @throws SQLException
     * @throws IOException
     * @throws AuthorizeException
     */
    @Override
    public boolean updateBitstreams(Context context, Item item, HarvestedItem hi)
            throws SQLException, IOException, AuthorizeException
    {
        return true;
    }
}
