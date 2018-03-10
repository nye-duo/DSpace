package org.dspace.harvest;

import org.dspace.content.Item;
import org.dspace.core.Context;

public interface BundleVersioningStrategy
{
    public void versionBundles(Context context, Item item) throws OAIHarvester.HarvestingException;
}
