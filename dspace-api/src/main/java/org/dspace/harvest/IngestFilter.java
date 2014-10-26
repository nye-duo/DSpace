/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.harvest;

import org.dspace.authorize.AuthorizeException;
import org.jdom.Element;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * Interface which plugins can implement if they want to decide whether to accept an item
 * via an OAI harvest process based on the content of the item.
 *
 * This might include, for example, only ingesting records which have a specified type (e.g. theses),
 * or are in a particular format (e.g. PDF)
 *
 * Implementors should be configured in dspace.cfg, thus:
 *
 * plugin.named.org.dspace.harvest.IngestFilter = \
 *  com.example.MyIngestFilter = myfilter
 *
 * and enabled in oai.cfg, thus:
 *
 * harvester.ingest_filter.options = none:No filtering of incoming items, myfilter:Filter items based on my criteria
 */
public interface IngestFilter
{
    /**
     * Determine whether an incoming object should be imported or not.
     *
     * This will be called after the item has been retrieved from the remote service, but before any attempt to create a DSpace item
     *
     * @param descMD    A list of elements in the OAI-PMH metadata element
     * @param oreREM    ORE Resource Map - may be null, depending on how the collection is set up
     * @return      true to accept the item, false to reject
     * @throws SQLException
     * @throws IOException
     * @throws AuthorizeException
     */
    public boolean acceptIngest(List<Element> descMD, Element oreREM)
            throws SQLException, IOException, AuthorizeException;;
}
