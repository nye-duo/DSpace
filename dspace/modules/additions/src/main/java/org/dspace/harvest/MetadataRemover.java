package org.dspace.harvest;

import org.dspace.content.Item;
import org.dspace.core.Context;

public interface MetadataRemover
{
    public void clearMetadata(Context context, Item item) throws HarvestingException;
}
