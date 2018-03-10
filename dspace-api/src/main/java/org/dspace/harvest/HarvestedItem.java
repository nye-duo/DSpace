/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.harvest;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

/**
 * @author Alexey Maslov
 */

public class HarvestedItem 
{
	private Context context;
	private TableRow harvestRow;

	
	HarvestedItem(Context c, TableRow row)
    {
        context = c;
        harvestRow = row;
    }
    
    
    public static void exists(Context c) throws SQLException {
    	DatabaseManager.queryTable(c, "harvested_item", "SELECT COUNT(*) FROM harvested_item");    	
    }
	
	
    /**
     * Find the harvest parameters corresponding to the specified DSpace item 
     * @return a HarvestedItem object corresponding to this item, null if not found.
     */
    public static HarvestedItem find(Context c, int item_id) throws SQLException 
    {
    	TableRow row = DatabaseManager.findByUnique(c, "harvested_item", "item_id", item_id);
    	
    	if (row == null) {
    		return null;
    	}
    	
    	return new HarvestedItem(c, row);
    }
    
    
    /*
     * select foo.item_id from (select item.item_id, item.owning_collection from item join item2bundle on item.item_id=item2bundle.item_id where item2bundle.bundle_id=22) as foo join collection on foo.owning_collection=collection.collection_id where collection.collection_id=5;
     */
    
    /**
     * Retrieve a DSpace Item that corresponds to this particular combination of owning collection and OAI ID. 
     * @param context 
     * @param itemOaiID the string used by the OAI-PMH provider to identify the item
     * @param collectionID id of the local collection that the item should be found in
     * @return DSpace Item or null if no item was found
     */
    public static Item getItemByOAIId(Context context, String itemOaiID, int collectionID) throws SQLException
    {
    	/*
         * FYI: This method has to be scoped to a collection. Otherwise, we could have collisions as more 
         * than one collection might be importing the same item. That is OAI_ID's might be unique to the 
         * provider but not to the harvester.
         */
   	 	Item resolvedItem = null;

        String[] queries = {
            "SELECT dsi.item_id FROM " +
        	"(SELECT item.item_id, item.owning_collection FROM item JOIN harvested_item ON item.item_id=harvested_item.item_id WHERE harvested_item.oai_id=?) " + 
        	"dsi JOIN collection ON dsi.owning_collection=collection.collection_id WHERE collection.collection_id=?",

            "SELECT dsi.item_id FROM " +
        	"(SELECT workflowitem.item_id, workflowitem.collection_id FROM workflowitem JOIN harvested_item ON workflowitem.item_id=harvested_item.item_id WHERE harvested_item.oai_id=?) " +
        	"dsi JOIN collection ON dsi.collection_id=collection.collection_id WHERE collection.collection_id=?",

            "SELECT dsi.item_id FROM " +
        	"(SELECT workspaceitem.item_id, workspaceitem.collection_id FROM workspaceitem JOIN harvested_item ON workspaceitem.item_id=harvested_item.item_id WHERE harvested_item.oai_id=?) " +
        	"dsi JOIN collection ON dsi.collection_id=collection.collection_id WHERE collection.collection_id=?",

            // check the configurable workflow too
            "SELECT dsi.item_id FROM " +
        	"(SELECT cwf_workflowitem.item_id, cwf_workflowitem.collection_id FROM cwf_workflowitem JOIN harvested_item ON cwf_workflowitem.item_id=harvested_item.item_id WHERE harvested_item.oai_id=?) " +
        	"dsi JOIN collection ON dsi.collection_id=collection.collection_id WHERE collection.collection_id=?",
        };

        int itemID = -1;
        for (String query : queries)
        {
            itemID = HarvestedItem.lookup(context, query, itemOaiID, collectionID);
            if (itemID != -1)
            {
                break;
            }
        }

        if (itemID != -1)
        {
            resolvedItem = Item.find(context, itemID);
        }

        return resolvedItem;
    }

    private static int lookup(Context context, String query, String itemOaiID, int collectionID)
            throws SQLException
    {
        TableRowIterator tri = null;
        try
        {
            tri = DatabaseManager.query(context, query, itemOaiID, collectionID);

            if (tri.hasNext())
            {
                TableRow row = tri.next();
                int itemID = row.getIntColumn("item_id");
                return itemID;
            }
            else
            {
                return -1;
            }
        }
        finally
        {
            if (tri != null)
            {
                tri.close();
            }
        }
    }

    /**
     * Create a new harvested item row for a specified item id.  
     * @return a new HarvestedItem object
     */
    public static HarvestedItem create(Context c, int itemId, String itemOAIid) throws SQLException {
    	TableRow row = DatabaseManager.row("harvested_item");
    	row.setColumn("item_id", itemId);
    	row.setColumn("oai_id", itemOAIid);
    	DatabaseManager.insert(c, row);
    	
    	return new HarvestedItem(c, row);    	
    }
    
    
    public String getItemID()
    {
        String oai_id = harvestRow.getStringColumn("item_id");

        return oai_id;
    }

    public void setItemID(int itemID)
    {
        harvestRow.setColumn("item_id", itemID);
    }

    /**
     * Get the oai_id associated with this item 
     */
    public String getOaiID()
    {
        String oai_id = harvestRow.getStringColumn("oai_id");

        return oai_id;
    }
    
    /**
     * Set the oai_id associated with this item 
     */
    public void setOaiID(String itemOaiID)
    {
    	harvestRow.setColumn("oai_id",itemOaiID);
        return;
    }
    
    
    public void setHarvestDate(Date date) {
    	if (date == null) {    	
    		date = new Date();
    	}
    	harvestRow.setColumn("last_harvested", date);
    }
    
    public Date getHarvestDate() {
    	return harvestRow.getDateColumn("last_harvested");
    }
    
    
    
    public void delete() throws SQLException {
    	DatabaseManager.delete(context, harvestRow);
    }
    
    
    
    public void update() throws SQLException, IOException, AuthorizeException {
        DatabaseManager.update(context, harvestRow);
    }

}
