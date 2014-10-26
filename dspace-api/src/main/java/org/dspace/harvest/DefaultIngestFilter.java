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
 * Implementation of the IngestFilter interface, used by DSpace as the default option if no
 * other plugin is provided.  Its only function is to approve everything that it is asked about.
 */
public class DefaultIngestFilter implements IngestFilter
{
    /**
     * Should DSpace accept the given object for ingest.
     *
     * Always responds true.
     *
     * @param descMD    A list of elements in the OAI-PMH metadata element
     * @param oreREM    ORE Resource Map - may be null, depending on how the collection is set up
     * @return  true
     * @throws SQLException
     * @throws IOException
     * @throws AuthorizeException
     */
    @Override
    public boolean acceptIngest(List<Element> descMD, Element oreREM)
            throws SQLException, IOException, AuthorizeException
    {
        return true;
    }
}
