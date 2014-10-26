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
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.Context;
import org.jdom.Element;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * Interface that plugins can implement if they want to change the way that items enter the workflow
 * during oai harvest.
 *
 * Examples might be to have updated items create new items instead of overwriting old ones, or
 * to go into the DSpace workflow rather than directly into the archive
 *
 * Implementers must be configured in dspace.cfg, thus:
 *
 * plugin.named.org.dspace.harvest.IngestionWorkflow = \
 *  com.example.MyIngestWorkflow = myworkflow
 *
 * and enabled in oai.cfg, thus
 *
 * harvester.ingest_workflow.options = archive:All items go directly to the DSpace archive, myworkflow: Do something custom during ingest
 *
 */
public interface IngestionWorkflow
{
    /**
     * In the event that an item is being updated, this will be called before any changes are made to
     * the existing item.  This will allow the implementer to carry out any preliminary tasks before
     * changes take place.  As this function returns an Item, it means that the item passed in does not
     * have to be the same item that is returned.  The item that is returned is the one that will be updated
     *
     * @param context
     * @param item
     * @param targetCollection
     * @param hi
     * @param descMd    A list of elements in the OAI-PMH metadata element
     * @param oreREM    ORE Resource Map - may be null, depending on how the collection is set up
     * @return  A DSpace Item that can be updated with the new incoming content
     * @throws SQLException
     * @throws IOException
     * @throws AuthorizeException
     */
    public Item preUpdate(Context context, Item item, Collection targetCollection,
                          HarvestedItem hi, List<Element> descMd, Element oreREM)
            throws SQLException, IOException, AuthorizeException;

    /**
     * If this is an update operation (the item already existed), then this method will be called
     * after the item has been modified by the new incoming data.
     *
     * In this operation implementers might choose to start or re-start any workflows the item is in,
     * for example.
     *
     * @param context
     * @param item
     * @throws SQLException
     * @throws IOException
     * @throws AuthorizeException
     */
    public void postUpdate(Context context, Item item)
            throws SQLException, IOException, AuthorizeException;

    /**
     * If this is a create operation (the item did not exist before), then this method will be called after
     * the item has been created.
     *
     * In this operation implementers might choose to start the workflow, for example
     *
     * @param context
     * @param item
     * @param handle
     * @return
     * @throws SQLException
     * @throws IOException
     * @throws AuthorizeException
     */
    public Item postCreate(Context context, WorkspaceItem item, String handle)
            throws SQLException, IOException, AuthorizeException;

    /**
     * In the event that this is an ORE bitstream harvest operation, this method allows implementers
     * to prevent overwrite of bitstreams based on whatever criteria it chooses.
     *
     * For example, in an update operation, if the external bitstreams do not appear to have changed, it would be wasteful
     * to re-import them, so this operation could return false.
     *
     * @param context
     * @param item
     * @param hi
     * @return
     * @throws SQLException
     * @throws IOException
     * @throws AuthorizeException
     */
    public boolean updateBitstreams(Context context, Item item, HarvestedItem hi)
            throws SQLException, IOException, AuthorizeException;
}
