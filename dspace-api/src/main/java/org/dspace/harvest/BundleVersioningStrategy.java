package org.dspace.harvest;

import org.dspace.content.Item;

public interface BundleVersioningStrategy
{
    public void versionBundles(Item item);
}
