/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.harvest.dao.impl;

import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;
import org.dspace.harvest.HarvestedItem;
import org.dspace.harvest.dao.HarvestedItemDAO;
import org.dspace.storage.rdbms.DatabaseUtils;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.criterion.Restrictions;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Hibernate implementation of the Database Access Object interface class for the HarvestedItem object.
 * This class is responsible for all database calls for the HarvestedItem object and is autowired by spring
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class HarvestedItemDAOImpl extends AbstractHibernateDAO<HarvestedItem> implements HarvestedItemDAO
{
    protected HarvestedItemDAOImpl()
    {
        super();
    }

    @Override
    public HarvestedItem findByItem(Context context, Item item) throws SQLException {
        Criteria criteria = createCriteria(context, HarvestedItem.class);
        criteria.add(Restrictions.eq("item", item));
        return singleResult(criteria);
    }

    @Override
    public HarvestedItem findByOAIId(Context context, String itemOaiID, Collection collection) throws SQLException {

        // first look the item up in the usual way, looking for it in the archive
        Criteria simpleItem = createCriteria(context, HarvestedItem.class);
        simpleItem.createAlias("item", "i");
        simpleItem.add(
                Restrictions.and(
                        Restrictions.eq("oaiId", itemOaiID),
                        Restrictions.eq("i.owningCollection", collection)
                )
        );
        HarvestedItem result = singleResult(simpleItem);
        if (result != null)
        {
            return result;
        }

        // items that aren't in the archive will not have owning collections, so we need to look for them elsewhere.
        // try a bunch of locations until we locate the item

        // In the regular workflow
        String q1 = String.format("select hi from HarvestedItem as hi, org.dspace.workflowbasic.BasicWorkflowItem as wf " +
                "where hi.item = wf.item and wf.collection = '%s' and hi.oaiId = '%s'",
                collection.getID().toString(),
                itemOaiID);
        Query workflowQuery = this.createQuery(context, q1);
        result = singleResult(workflowQuery);
        if (result != null)
        {
            return result;
        }

        // in the workspace
        String q2 = String.format("select hi from HarvestedItem as hi, org.dspace.content.WorkspaceItem as ws " +
                        "where hi.item = ws.item and ws.collection = '%s' and hi.oaiId = '%s'",
                collection.getID().toString(),
                itemOaiID);
        Query workspaceQuery = this.createQuery(context, q2);
        result = singleResult(workspaceQuery);
        if (result != null)
        {
            return result;
        }

        // in the configurable workflow
        String q3 = String.format("select hi from HarvestedItem as hi, org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem as cwf " +
                        "where hi.item = cwf.item and cwf.collection = '%s' and hi.oaiId = '%s'",
                collection.getID().toString(),
                itemOaiID);
        Query cwfWorkflowQuery = this.createQuery(context, q3);
        result = singleResult(cwfWorkflowQuery);
        if (result != null)
        {
            return result;
        }

        return null;
    }
}
