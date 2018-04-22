package no.uio.duo;

import no.uio.duo.policy.ContextualBitstream;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Convenience class to allow us to iterate through all bitstreams in an item, retrieving
 * both the bitstream and the bundle within which we are viewing it.
 */

public class BitstreamIterator implements Iterator<ContextualBitstream>
{
    private Item item;
    private List<ContextualBitstream> entries = new ArrayList<ContextualBitstream>();
    private int index = 0;

    /**
     * Create a new iterator around an item
     *
     * @param item
     * @throws SQLException
     */
    public BitstreamIterator(Item item)
            throws SQLException
    {
        this(item, null);
    }

    public BitstreamIterator(Item item, String bundleName)
            throws SQLException
    {
        ItemService itemService = ContentServiceFactory.getInstance().getItemService();

        this.item = item;

        List<Bundle> bundles = null;
        if (bundleName != null)
        {
            bundles = itemService.getBundles(this.item, bundleName);
        }
        else
        {
            bundles = this.item.getBundles();
        }

        for (Bundle bundle : bundles)
        {
            List<Bitstream> bitstreams = bundle.getBitstreams();
            for (Bitstream bitstream : bitstreams)
            {
                this.entries.add(new ContextualBitstream(bitstream, bundle));
            }
        }
    }

    @Override
    public boolean hasNext()
    {
        return this.entries.size() > this.index;
    }

    @Override
    public ContextualBitstream next()
    {
        return this.entries.get(this.index++);
    }
}
