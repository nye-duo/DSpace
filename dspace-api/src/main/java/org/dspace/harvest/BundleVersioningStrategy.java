/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.harvest;

import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * Interface which plugins can implement if they wish to be responsible for versioning the
 * content of bundles during the update of an item via the OAI harvested ingest.
 *
 * Operations that implementers might like to take could include operations like moving all
 * the contents of the ORIGINAL bundle to a BACKUP bundle
 *
 * Implementors should be configured in dspace.cfg, thus:
 *
 * plugin.named.org.dspace.harvest.BundleVersioningStrategy = \
 *  com.example.BundleVersionImpl = mybundleversioner
 *
 * and enabled in oai.cfg, thus:
 *
 * harvester.bundle_versioning.options = all:Remove all existing bundles and replace completely, mybundleversioner:version my bundles appropriately
 */
public interface BundleVersioningStrategy
{
    /**
     * Carry out any operations on the item which satisfy your bundle-versioning requirements
     *
     * This method will be called by the OAIHarvester prior to any new incoming content being applied
     * to the item.
     *
     * @param context
     * @param item
     * @throws OAIHarvester.HarvestingException
     */
    public void versionBundles(Context context, Item item) throws OAIHarvester.HarvestingException;
}
