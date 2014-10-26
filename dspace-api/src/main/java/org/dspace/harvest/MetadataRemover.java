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
 * Interface which plugins can implement if they want to do something clever with the metadata
 * on an item during update.
 *
 * This might include, for example, only removing a small subset of the metadata of an item, allowing
 * a remote oai source to provide part of the metadata of an item, but for a DSpace administrator to
 * provide the rest, without them interfering with eachother during update.
 *
 * Implementors should be configured in dspace.cfg, thus:
 *
 * plugin.named.org.dspace.harvest.MetadataRemover = \
 *  com.example.MyMetadataManager = mymetadata
 *
 * and enabled in oai.cfg, thus:
 *
 * harvester.metadata_update.options = all:Remove all existing metadata and replace completely, mymetadata:Remove only a subset of metadata
 */
public interface MetadataRemover
{
    /**
     * Remove all relevant metadata from the item.
     *
     * This will be called before an item is updated.
     *
     * @param context
     * @param item
     * @throws OAIHarvester.HarvestingException
     */
    public void clearMetadata(Context context, Item item) throws OAIHarvester.HarvestingException;
}
