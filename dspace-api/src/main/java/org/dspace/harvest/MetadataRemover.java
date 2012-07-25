package org.dspace.harvest;

import org.dspace.content.Item;

public interface MetadataRemover
{
    public void clearMetadata(Item item);
}
