package no.uio.duo;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.crosswalk.IngestionCrosswalk;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.*;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.factory.CoreServiceFactory;
import org.dspace.core.service.PluginService;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.List;

/**
 * Class for providing utilities for Metadata management in the Duo module
 *
 */
public class MetadataManager
{
    /**
     * Add metadata from the provided bitstream to the provided item.  Metadata must
     * be an FS formatted metadata document
     *
     * @param context
     * @param item
     * @param bitstream
     * @throws AuthorizeException
     * @throws IOException
     * @throws SQLException
     * @throws DuoException
     */
    public void addMetadataFromBitstream(Context context, Item item, Bitstream bitstream)
            throws AuthorizeException, IOException, SQLException, DuoException
    {
        try
        {
            PluginService pluginService = CoreServiceFactory.getInstance().getPluginService();
            BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();
            ItemService itemService = ContentServiceFactory.getInstance().getItemService();

            // prep up the ingestion kit
            IngestionCrosswalk inxwalk = (IngestionCrosswalk) pluginService.getNamedPlugin(IngestionCrosswalk.class, "FS");
            if (inxwalk == null)
            {
                throw new DuoException("No IngestionCrosswalk configured for FS");
            }
            InputStream stream = bitstreamService.retrieve(context, bitstream);
            SAXBuilder builder = new SAXBuilder();
            Document document = builder.build(stream);
            Element element = document.getRootElement();

            // before we do the ingest, we need to preserve some fields

            // get the fields which need special treatment
            String embargoEndMd = ConfigurationManager.getProperty("embargo.field.terms");
            String embargoTypeMd = ConfigurationManager.getProperty("studentweb.embargo-type.field");
            String gradeMd = ConfigurationManager.getProperty("studentweb.grade.field");

            List<MetadataValue> embargoEnds = null;
            List<MetadataValue> embargoType = null;
            List<MetadataValue> grade = null;

            // take copies of and then remove these fields
            if (embargoEndMd != null && !"".equals(embargoEndMd))
            {
                embargoEnds = itemService.getMetadataByMetadataString(item, embargoEndMd);
                MetadataFieldRepresentation dcv = this.makeDCValue(embargoEndMd, null);
                itemService.clearMetadata(context, item, dcv.schema, dcv.element, dcv.qualifier, dcv.language);
            }
            if (embargoTypeMd != null && !"".equals(embargoTypeMd))
            {
                embargoType = itemService.getMetadataByMetadataString(item, embargoTypeMd);
                MetadataFieldRepresentation dcv = this.makeDCValue(embargoTypeMd, null);
                itemService.clearMetadata(context, item, dcv.schema, dcv.element, dcv.qualifier, dcv.language);
            }
            if (gradeMd != null && !"".equals(gradeMd))
            {
                grade = itemService.getMetadataByMetadataString(item, gradeMd);
                MetadataFieldRepresentation dcv = this.makeDCValue(gradeMd, null);
                itemService.clearMetadata(context, item, dcv.schema, dcv.element, dcv.qualifier, dcv.language);
            }

            // now we can do the ingest
            inxwalk.ingest(context, item, element, true);

            // now check to see if any of the above fields have been replaced.  If they have
            // not, then write the old value back in again

            boolean noEmbargoEndMd = embargoEndMd != null && itemService.getMetadataByMetadataString(item, embargoEndMd).size() == 0;
            boolean noEmbargoTypeMd = embargoTypeMd != null && itemService.getMetadataByMetadataString(item, embargoTypeMd).size() == 0;
            boolean noGradeMd = gradeMd != null && itemService.getMetadataByMetadataString(item, gradeMd).size() == 0;

            if (noEmbargoEndMd && (embargoEnds != null && embargoEnds.size() > 0))
            {
                MetadataValue mdv = embargoEnds.get(0);
                itemService.addMetadata(context, item, mdv.getMetadataField().getMetadataSchema().getName(), mdv.getMetadataField().getElement(), mdv.getMetadataField().getQualifier(), mdv.getLanguage(), mdv.getValue());
            }
            if (noEmbargoTypeMd && (embargoType != null && embargoType.size() > 0))
            {
                MetadataValue mdv = embargoType.get(0);
                itemService.addMetadata(context, item, mdv.getMetadataField().getMetadataSchema().getName(), mdv.getMetadataField().getElement(), mdv.getMetadataField().getQualifier(), mdv.getLanguage(), mdv.getValue());
            }
            if (noGradeMd && (grade != null && grade.size() > 0))
            {
                MetadataValue mdv = grade.get(0);
                itemService.addMetadata(context, item, mdv.getMetadataField().getMetadataSchema().getName(), mdv.getMetadataField().getElement(), mdv.getMetadataField().getQualifier(), mdv.getLanguage(), mdv.getValue());
            }

            // finally, write the changes
            itemService.update(context, item);
        }
        catch (JDOMException | CrosswalkException e)
        {
            throw new DuoException(e);
        }
    }

    /**
     * Remove all of the authority controlled metadata from the item, using the swordv2-server
     * configuration
     *
     * @param context
     * @param item
     * @throws DuoException
     */
    public void removeAuthorityMetadata(Context context, Item item)
            throws DuoException
    {
        this.removeAuthorityMetadata(context, item, "swordv2-server.metadata.replaceable");
    }

    /**
     * Remove all of the metadata from the item based on the configuration
     *
     * @param context
     * @param item
     * @param config
     * @throws DuoException
     */
    public void removeAuthorityMetadata(Context context, Item item, String config)
            throws DuoException
    {
        try
        {
            String raw = ConfigurationManager.getProperty(config);
            if (raw == null || "".equals(raw))
            {
                return;
            }
            String[] parts = raw.split(",");
            for (String part : parts)
            {
                MetadataFieldRepresentation dcv = this.makeDCValue(part.trim(), null);
                ItemService itemService = ContentServiceFactory.getInstance().getItemService();
                itemService.clearMetadata(context, item, dcv.schema, dcv.element, dcv.qualifier, Item.ANY);
            }
        }
        catch (SQLException e)
        {
            throw new DuoException(e);
        }
    }

    /**
     * Make a DCValue object out of the string representation (e.g. dc.title.alternative)
     *
     * @param field
     * @param value
     * @return
     * @throws DuoException
     */
    public MetadataFieldRepresentation makeDCValue(String field, String value)
            throws DuoException
    {
        String[] bits = field.split("\\.");
        if (bits.length < 2 || bits.length > 3)
        {
            throw new DuoException("invalid DC value: " + field);
        }

        MetadataFieldRepresentation dcv = new MetadataFieldRepresentation();
        dcv.schema = bits[0];
        dcv.element = bits[1];
        if (bits.length == 3)
        {
            dcv.qualifier = bits[2];
        }
        dcv.value = value;
        return dcv;
    }

    public List<MetadataValue> allMetadata(Item item)
    {
        ItemService itemService = ContentServiceFactory.getInstance().getItemService();
        return itemService.getMetadata(item, Item.ANY, Item.ANY, Item.ANY, Item.ANY);
    }

    public String makeFieldString(MetadataValue dcv)
    {
        String field = dcv.getMetadataField().getMetadataSchema().getName() + "." + dcv.getMetadataField().getElement();
        if (dcv.getMetadataField().getQualifier() != null)
        {
            field += "." + dcv.getMetadataField().getQualifier();
        }
        return field;
    }

    public void replaceMetadata(Context context, Item item, List<MetadataValue> md)
            throws DuoException
    {
        try
        {
            ItemService itemService = ContentServiceFactory.getInstance().getItemService();
            itemService.clearMetadata(context, item, Item.ANY, Item.ANY, Item.ANY, Item.ANY);
            for (MetadataValue dcv : md)
            {
                itemService.addMetadata(context, item, dcv.getMetadataField().getMetadataSchema().getName(), dcv.getMetadataField().getElement(), dcv.getMetadataField().getQualifier(), dcv.getLanguage(), dcv.getValue());
            }
        }
        catch (SQLException e)
        {
            throw new DuoException(e);
        }
    }
}
