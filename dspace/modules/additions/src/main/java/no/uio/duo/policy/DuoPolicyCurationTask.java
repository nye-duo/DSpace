package no.uio.duo.policy;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.curate.AbstractCurationTask;
import org.dspace.curate.Curator;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Curation task which allows the {@link PolicyPatternManager} to be run from the user interface or
 * the command line
 */
public class DuoPolicyCurationTask extends AbstractCurationTask
{
    // The log4j logger for this class
    private static Logger log = Logger.getLogger(DuoPolicyCurationTask.class);

    /**
     * Execute the {@link PolicyPatternManager} over the given DSpace Object
     *
     * This will only run of the DSpace Object is an item, and it will execute PolicyPatternManager.applyToExistingItem
     *
     * @param dso
     * @return
     * @throws IOException
     */
    @Override
    public int perform(DSpaceObject dso)
            throws IOException
    {
        if (!(dso instanceof Item))
        {
            return Curator.CURATE_SKIP;
        }

        Item item = (Item) dso;

        Context context = null;
        boolean error = false;
        try
        {
            context = Curator.curationContext();
            this.setupEPerson(context);

            // The results that we'll return
            StringBuilder results = new StringBuilder();

            PolicyPatternManager ppm = new PolicyPatternManager();
            ppm.applyToExistingItem(item, context);

            this.setResult(results.toString());
            this.report(results.toString());
        }
        catch (Exception e)
        {
            if (log.isDebugEnabled())
            {
                log.debug(e.getMessage());
            }
            error = true;
        }

        if (error)
        {
            if (context != null) {
                context.abort();
            }
            return Curator.CURATE_ERROR;
        }

        return Curator.CURATE_SUCCESS;
    }

    private void setupEPerson(Context context)
            throws SQLException, IOException, AuthorizeException
    {
        EPerson ePerson = context.getCurrentUser();

        // ugly eperson verification/acquisition/error bit
        if (ePerson == null)
        {
            EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();
            String adminEperson = configurationService.getProperty("duopolicy.admin.eperson");
            ePerson = ePersonService.findByEmail(context, adminEperson);
            if (ePerson == null)
            {
                ePerson = ePersonService.findByNetid(context, adminEperson);
            }
        }
        if (ePerson == null)
        {
            throw new AuthorizeException("No admin eperson defined in duopolicy.admin.eperson, and is required for context of the call - probably need to fix your config");
        }

        context.setCurrentUser(ePerson);
    }

}
