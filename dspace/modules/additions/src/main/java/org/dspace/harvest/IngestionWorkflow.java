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

public interface IngestionWorkflow
{
    public Item preUpdate(Context context, Item item, Collection targetCollection,
                          HarvestedItem hi, List<Element> descMd, Element oreREM)
            throws SQLException, IOException, AuthorizeException;

    public void postUpdate(Context context, Item item)
            throws SQLException, IOException, AuthorizeException;;

    public Item postCreate(Context context, WorkspaceItem item, String handle)
            throws SQLException, IOException, AuthorizeException;

    public boolean updateBitstreams(Context context, Item item, HarvestedItem hi)
            throws SQLException, IOException, AuthorizeException;
}
