package no.uio.duo;

import org.apache.abdera.Abdera;
import org.apache.abdera.factory.Factory;
import org.apache.abdera.model.Document;
import org.apache.abdera.model.Element;
import org.apache.abdera.model.ExtensibleElement;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.BundleService;
import org.dspace.content.service.ItemService;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.sword2.DSpaceSwordException;
import org.dspace.sword2.SimpleDCEntryDisseminator;
import org.dspace.sword2.SwordEntryDisseminator;
import org.swordapp.server.DepositReceipt;
import org.swordapp.server.SwordError;
import org.swordapp.server.SwordServerException;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.List;

/**
 * <p>Implementation of the SwordEntryDisseminator which can provide the embedded metadata
 * for StudentWeb alongside a Dublin Core version of the metadata</p>
 *
 * <p>They key difference between this and the standard SimpleDCEntryDisseminator provided
 * with the SWORDv2 module is that this supports both the DC dissemination and the StudentWeb
 * metadata dissemination in one entry document.</p>
 *
 * <p>The output of this disseminator is used by StudentWeb to retrieve previously deposited
 * metadata for display to the user during updates to the metadata via the web forms.</p>
 *
 * <p><strong>Configuration</strong></p>
 *
 * <p>Use this class to replace the default EntryDisseminator in the modules/swordv2-server.cfg
 * file</p>
 *
 * <pre>
 *     plugin.single.org.dspace.sword2.SwordEntryDisseminator = no.uio.duo.DuoEntryDisseminator
 * </pre>
 */
public class DuoEntryDisseminator extends SimpleDCEntryDisseminator implements SwordEntryDisseminator
{
    /**
     * Create a DepositReceipt object containing the standard DC metadata as required
     * by sword and also the embedded native StudentWeb metadata format
     *
     * @param context
     * @param item
     * @param depositReceipt
     * @return
     * @throws DSpaceSwordException
     * @throws SwordError
     * @throws SwordServerException
     */
    public DepositReceipt disseminate(Context context, Item item, DepositReceipt depositReceipt)
            throws DSpaceSwordException, SwordError, SwordServerException
    {
        try
        {
            // first get a standard DC deposit entry
            DepositReceipt receipt = super.disseminate(context, item, depositReceipt);

            // now include the FS specific metadata
            ItemService itemService = ContentServiceFactory.getInstance().getItemService();
            List<Bundle> bundles = itemService.getBundles(item, DuoConstants.ADMIN_BUNDLE);

            BundleService bundleService = ContentServiceFactory.getInstance().getBundleService();
            BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();
            for (Bundle bundle : bundles)
            {
                // get hold of the bitstream
                Bitstream bitstream = bundleService.getBitstreamByName(bundle, DuoConstants.METADATA_FILE);

                // if there's no bitstream of that name, move on
                if (bitstream == null)
                {
                    continue;
                }

                // parse the bitstream into an abdera element
                InputStream stream = bitstreamService.retrieve(context, bitstream);
                Abdera ab = new Abdera();
                Document<ExtensibleElement> doc = ab.getParserFactory().getParser().parse(stream);
                ExtensibleElement element = doc.getRoot();

                // add the grade and embargo metadata (which can be added separately to the main
                // metadata)
                this.addGradeMetadata(element, item);
                this.addEmbargoMetadata(element, item);

                // add the element as an extension to the receipt.
                receipt.getWrappedEntry().addExtension(element);

                // just do the first one
                break;
            }

            return receipt;
        }
        catch (SQLException | IOException | AuthorizeException e)
        {
            throw new DSpaceSwordException(e);
        }
    }

    private void addGradeMetadata(ExtensibleElement element, Item item)
    {
        ItemService itemService = ContentServiceFactory.getInstance().getItemService();
        String gradeField = ConfigurationManager.getProperty("studentweb.grade.field");

        List<MetadataValue> dcvs = itemService.getMetadataByMetadataString(item, gradeField);
        if (dcvs.size() > 0)
        {
            String grade = dcvs.get(0).getValue();
            Abdera abdera = new Abdera();
            Element gradeElement = abdera.getFactory().newElement(DuoConstants.GRADE_QNAME);
            gradeElement.setText(grade);
            element.addExtension(gradeElement);
        }
    }

    private void addEmbargoMetadata(ExtensibleElement element, Item item)
    {
        ItemService itemService = ContentServiceFactory.getInstance().getItemService();

        String embargoField = ConfigurationManager.getProperty("embargo.field.lift");
        String termsField = ConfigurationManager.getProperty("studentweb.embargo-type.field");

        List<MetadataValue> edcvs = itemService.getMetadataByMetadataString(item, embargoField);
        List<MetadataValue> tdcvs = itemService.getMetadataByMetadataString(item, termsField);

        Abdera abdera = new Abdera();
        Factory factory = abdera.getFactory();

        if (edcvs.size() > 0)
        {
            Element embargoElement = factory.newElement(DuoConstants.EMBARGO_END_DATE_QNAME);
            embargoElement.setText(edcvs.get(0).getValue());
            element.addExtension(embargoElement);
        }

        if (tdcvs.size() > 0)
        {
            Element termsElement = factory.newElement(DuoConstants.EMBARGO_TYPE_QNAME);
            termsElement.setText(tdcvs.get(0).getValue());
            element.addExtension(termsElement);
        }
    }
}
