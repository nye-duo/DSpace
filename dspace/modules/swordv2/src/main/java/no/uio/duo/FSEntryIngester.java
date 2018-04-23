package no.uio.duo;

import org.apache.abdera.model.Element;
import org.apache.abdera.model.Entry;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.DCDate;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.sword2.DSpaceSwordException;
import org.dspace.sword2.DepositResult;
import org.dspace.sword2.SwordEntryIngester;
import org.dspace.sword2.VerboseDescription;
import org.swordapp.server.Deposit;
import org.swordapp.server.SwordAuthException;
import org.swordapp.server.SwordEntry;
import org.swordapp.server.SwordError;
import org.swordapp.server.SwordServerException;
import org.swordapp.server.UriRegistry;

import javax.xml.namespace.QName;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

/**
 * <p>Implementation of the SwordEntryIngester interface which takes the FS metadata as embedded in an
 * atom entry and adds it to an existing object</p>
 *
 * <p>This can deal with only the administrative metadata which may come from StudentWeb:</p>
 *
 * <ul>
 *     <li>grade</li>
 *     <li>embargo end date</li>
 *     <li>embargo type</li>
 * </ul>
 *
 * <p>All other metadata should be provided in a BagIt package with the normal deposit process.
 * See the {@link FSBagItIngester} for more information.</p>
 *
 * <p><strong>Configuration</strong></p>
 *
 * <p>In the modules/swordv2-server.cfg file replace the default SwordEntryIngester with this
 * class</p>
 *
 * <pre>
 * plugin.single.org.dspace.sword2.SwordEntryIngester = no.uio.duo.FSEntryIngester
 * </pre>
 */
public class FSEntryIngester implements SwordEntryIngester
{
    /**
     * Ingest the metadata in the deposit into the supplied DSpace object (Item or Collection)
     *
     * @param context
     * @param deposit
     * @param dso
     * @param verboseDescription
     * @return
     * @throws DSpaceSwordException
     * @throws SwordError
     * @throws SwordAuthException
     * @throws SwordServerException
     */
    public DepositResult ingest(Context context, Deposit deposit, DSpaceObject dso, VerboseDescription verboseDescription)
            throws DSpaceSwordException, SwordError, SwordAuthException, SwordServerException
    {
        return this.ingest(context, deposit, dso, verboseDescription, null, false);
    }

    /**
     * Ingest the metadata in the deposit into the supplied DSpace object (Item or Collection)
     *
     * @param context
     * @param deposit
     * @param dso
     * @param verboseDescription
     * @param result
     * @param replace
     * @return
     * @throws DSpaceSwordException
     * @throws SwordError
     * @throws SwordAuthException
     * @throws SwordServerException
     */
    public DepositResult ingest(Context context, Deposit deposit, DSpaceObject dso, VerboseDescription verboseDescription, DepositResult result, boolean replace)
            throws DSpaceSwordException, SwordError, SwordAuthException, SwordServerException
    {
        if (dso instanceof Collection)
        {
            return this.ingestToCollection(context, deposit, (Collection) dso, verboseDescription, result);
        }
        else if (dso instanceof Item)
        {
            return this.ingestToItem(context, deposit, (Item) dso, verboseDescription, result, replace);
        }
        return null;
    }

    /**
     * Replace the passed item with the metadata content of the deposit.
     *
     * @param context
     * @param deposit
     * @param item
     * @param verboseDescription
     * @param result
     * @param replace
     * @return
     * @throws DSpaceSwordException
     * @throws SwordError
     * @throws SwordAuthException
     * @throws SwordServerException
     */
    public DepositResult ingestToItem(Context context, Deposit deposit, Item item, VerboseDescription verboseDescription, DepositResult result, boolean replace)
                throws DSpaceSwordException, SwordError, SwordAuthException, SwordServerException
    {
        try
        {
            if (result == null)
            {
                result = new DepositResult();
            }
            result.setItem(item);

            // add the metadata to the item
            this.addMetadataToItem(context, deposit, item);

            // update the item metadata to inclue the current time as
            // the updated date
            this.setUpdatedDate(context, item, verboseDescription);

            // in order to write these changes, we need to bypass the
            // authorisation briefly, because although the user may be
            // able to add stuff to the repository, they may not have
            // WRITE permissions on the archive.
            ItemService itemService = ContentServiceFactory.getInstance().getItemService();
            context.turnOffAuthorisationSystem();
            itemService.update(context, item);
            context.restoreAuthSystemState();

            verboseDescription.append("Update successful");

            result.setItem(item);
            result.setTreatment(this.getTreatment());

            return result;
        }
        catch (SQLException | AuthorizeException | DuoException e)
        {
            throw new DSpaceSwordException(e);
        }
    }

    /**
     * Attempt to create a new item with a metadata only record
     *
     * The StudentWeb/FS integration does not permit this, so a call to this method will always throw an exception
     *
     * @param context
     * @param deposit
     * @param collection
     * @param verboseDescription
     * @param result
     * @return
     * @throws DSpaceSwordException
     * @throws SwordError
     * @throws SwordAuthException
     * @throws SwordServerException
     */
    public DepositResult ingestToCollection(Context context, Deposit deposit, Collection collection, VerboseDescription verboseDescription, DepositResult result)
                throws DSpaceSwordException, SwordError, SwordAuthException, SwordServerException
    {
        // entry documents cannot be used to deposit items afresh.
        throw new SwordError(UriRegistry.ERROR_METHOD_NOT_ALLOWED, "You are not allowed to create items with only at Atom Entry");
    }

    private void addMetadataToItem(Context context, Deposit deposit, Item item)
            throws DuoException
    {
        SwordEntry se = deposit.getSwordEntry();
        if (se == null)
        {
            return;
        }

        // deal with the grade
        String gradeField = ConfigurationManager.getProperty("studentweb.grade.field");
        if (gradeField == null || "".equals(gradeField))
        {
            throw new DuoException("No configuration, or configuration is invalid for: studentweb:grade.field");
        }
        this.addFieldToItem(context, se.getEntry(), item, DuoConstants.GRADE_QNAME, gradeField);

        // deal with the embargo end date
        String embargoEndField = ConfigurationManager.getProperty("embargo.field.terms");
        if (embargoEndField == null || "".equals(embargoEndField))
        {
            throw new DuoException("No configuration, or configuration is invalid for: embargo.field.lift");
        }
        this.addFieldToItem(context, se.getEntry(), item, DuoConstants.EMBARGO_END_DATE_QNAME, embargoEndField);

        // deal with the embargo type
        String embargoTypeField = ConfigurationManager.getProperty("studentweb.embargo-type.field");
        if (embargoTypeField == null || "".equals(embargoTypeField))
        {
            throw new DuoException("No configuration, or configuration is invalid for: embargo-type.field");
        }
        this.addFieldToItem(context, se.getEntry(), item, DuoConstants.EMBARGO_TYPE_QNAME, embargoTypeField);
    }

    private void addFieldToItem(Context context, Entry entry, Item item, QName qname, String field)
            throws DuoException
    {
        try
        {
            List<Element> elements = entry.getExtensions(qname);
            if (elements.size() != 0)
            {
                MetadataManager mdm = new MetadataManager();
                Element element = elements.get(0);
                String text = element.getText();
                if (text != null)
                {
                    MetadataFieldRepresentation dc = mdm.makeDCValue(field, null);
                    ItemService itemService = ContentServiceFactory.getInstance().getItemService();
                    itemService.clearMetadata(context, item, dc.schema, dc.element, dc.qualifier, Item.ANY);
                    itemService.addMetadata(context, item, dc.schema, dc.element, dc.qualifier, null, text.trim());
                }
            }
        }
        catch (SQLException e)
        {
            throw new DuoException(e);
        }
    }

    /**
     * Add the current date to the item metadata.  This looks up
     * the field in which to store this metadata in the configuration
     * sword.updated.field
     *
     * @param item
     * @throws DSpaceSwordException
     */
    protected void setUpdatedDate(Context context, Item item, VerboseDescription verboseDescription)
            throws DuoException
    {
        try
        {
            String field = ConfigurationManager.getProperty("swordv2-server.updated.field");
            if (field == null || "".equals(field))
            {
                throw new DuoException("No configuration, or configuration is invalid for: swordv2-server.updated.field");
            }

            MetadataManager mdm = new MetadataManager();
            MetadataFieldRepresentation dc = mdm.makeDCValue(field, null);
            ItemService itemService = ContentServiceFactory.getInstance().getItemService();
            itemService.clearMetadata(context, item, dc.schema, dc.element, dc.qualifier, Item.ANY);
            DCDate date = new DCDate(new Date());
            itemService.addMetadata(context, item, dc.schema, dc.element, dc.qualifier, null, date.toString());

            verboseDescription.append("Updated date added to response from item metadata where available");
        }
        catch (SQLException e)
        {
            throw new DuoException(e);
        }
    }

    /**
     * The human readable description of the treatment this ingester has
     * put the deposit through
     *
     * @return
     * @throws DSpaceSwordException
     */
    private String getTreatment() throws DSpaceSwordException
    {
        return "The grade and/or embargo date have been updated";
    }
}
