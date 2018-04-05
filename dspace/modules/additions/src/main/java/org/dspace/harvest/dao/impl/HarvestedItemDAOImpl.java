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
        List<Criteria> lookups = new ArrayList<Criteria>();

        Criteria simpleItem = createCriteria(context, HarvestedItem.class);
        simpleItem.createAlias("item", "i");
        simpleItem.add(
                Restrictions.and(
                        Restrictions.eq("oaiId", itemOaiID),
                        Restrictions.eq("i.owningCollection", collection)
                )
        );
        lookups.add(simpleItem);


        Criteria workflowItem = createCriteria(context, HarvestedItem.class);
        workflowItem.createAlias("wf", "workflowitem");
        workflowItem.createAlias("c", "collection");
        workflowItem.add(
                Restrictions.eqProperty("wf.item_id", "item_id")
        );
        workflowItem.add(
                Restrictions.eqProperty("wf.collection_id", "c.collection_id")
        );
        workflowItem.add(
                Restrictions.and(
                        Restrictions.eq("oaiId", itemOaiID),
                        Restrictions.eq("c.collection_id", collection)
                )
        );
        lookups.add(workflowItem);

        Criteria workspaceItem = createCriteria(context, HarvestedItem.class);
        workspaceItem.createAlias("ws", "workspaceitem");
        workspaceItem.createAlias("c", "collection");
        workspaceItem.add(
                Restrictions.eqProperty("ws.item_id", "item_id")
        );
        workspaceItem.add(
                Restrictions.eqProperty("ws.collection_id", "c.collection_id")
        );
        workflowItem.add(
                Restrictions.and(
                        Restrictions.eq("oaiId", itemOaiID),
                        Restrictions.eq("c.collection_id", collection)
                )
        );
        lookups.add(workspaceItem);

        // TODO: does this work if the configurable workflow is not activated?
        Criteria cwfItem = createCriteria(context, HarvestedItem.class);
        cwfItem.createAlias("cwf", "cwf_workflowitem");
        cwfItem.createAlias("c", "collection");
        cwfItem.add(
                Restrictions.eqProperty("cwf.item_id", "item_id")
        );
        cwfItem.add(
                Restrictions.eqProperty("cwf.collection_id", "c.collection_id")
        );
        cwfItem.add(
                Restrictions.and(
                        Restrictions.eq("oaiId", itemOaiID),
                        Restrictions.eq("c.collection_id", collection)
                )
        );
        lookups.add(cwfItem);

        for (Criteria criteria : lookups)
        {
            HarvestedItem result = singleResult(criteria);
            if (result != null)
            {
                return result;
            }
        }

        return null;
    }
}
