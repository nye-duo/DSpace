package no.uio.duo;

import org.dspace.app.util.Util;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.BundleService;
import org.dspace.core.Context;
import org.dspace.workflow.WorkflowException;
import org.dspace.xmlworkflow.state.Step;
import org.dspace.xmlworkflow.state.actions.ActionResult;
import org.dspace.xmlworkflow.state.actions.processingaction.ProcessingAction;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

/**
 * <p>Processing action to handle the change of bitstream orders in the
 * customisable workflow</p>
 *
 * <p>This presents to the user a page
 * offering javascript based re-sequencing of bitstreams, and the option to move bitstreams
 * between bundles, and to finally save the result.</p>
 *
 * <p><strong>Configuration</strong></p>
 *
 * <p>In the spring/api/workflow-actions.xml definition file add a new bean for this class:</p>
 *
 * <pre>
 *     &lt;bean id="bitstreamactionAPI" class="no.uio.duo.XmlUIBitstreamReorder" scope="prototype"/&gt;
 * </pre>
 *
 */
public class XmlUIBitstreamReorder extends ProcessingAction
{
    @Override
    public void activate(Context context, XmlWorkflowItem xmlWorkflowItem)
            throws SQLException, IOException, AuthorizeException, WorkflowException
    {
        // do nothing
    }

    /**
     * Execute a bitstream reorder
     *
     * @param context
     * @param wfi
     * @param step
     * @param request
     * @return
     * @throws SQLException
     * @throws AuthorizeException
     * @throws IOException
     * @throws WorkflowException
     */
    @Override
    public ActionResult execute(Context context, XmlWorkflowItem wfi, Step step, HttpServletRequest request)
            throws SQLException, AuthorizeException, IOException, WorkflowException
    {
        String button = Util.getSubmitButton(request, "submit_leave");

        if ("submit_move".equals(button))
        {
            this.move(context, wfi, request);
            return new ActionResult(ActionResult.TYPE.TYPE_PAGE);
        }
        else if ("submit_update".equals(button))
        {
            this.reorder(context, wfi, request);
        }
        else if ("submit_update_finish".equals(button))
        {
            this.reorder(context, wfi, request);
            return new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, ActionResult.OUTCOME_COMPLETE);
        }

        return new ActionResult(ActionResult.TYPE.TYPE_CANCEL);


    }

    private void move(Context context, XmlWorkflowItem wfi, HttpServletRequest request)
            throws SQLException, AuthorizeException, IOException
    {
        Map<UUID, UUID> moves = new HashMap<UUID, UUID>();
        Enumeration params = request.getParameterNames();
        while (params.hasMoreElements())
        {
            String key = (String) params.nextElement();
            if (key.startsWith("move_"))
            {
                String val = request.getParameter(key);
                if (!"-1".equals(val))
                {
                    UUID bsid = UUID.fromString(key.substring("move_".length()));
                    UUID bundleid = UUID.fromString(val);
                    moves.put(bsid, bundleid);
                }
            }
        }

        BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();
        BundleService bundleService = ContentServiceFactory.getInstance().getBundleService();

        for (UUID bsid : moves.keySet())
        {
            Bitstream bitstream = bitstreamService.find(context, bsid);
            Bundle target = bundleService.find(context, moves.get(bsid));

            List<Bundle> existing = bitstream.getBundles();
            bundleService.addBitstream(context, target, bitstream);
            bundleService.update(context, target);
            for (Bundle b : existing)
            {
                bundleService.removeBitstream(context, b, bitstream);
                bundleService.update(context, b);
            }
        }
    }

    private void reorder(Context context, XmlWorkflowItem wfi, HttpServletRequest request)
            throws SQLException, AuthorizeException
    {
        Item item = wfi.getItem();
        List<Bundle> bundles = item.getBundles();
        for (Bundle bundle : bundles)
        {
            List<Bitstream> bitstreams = bundle.getBitstreams();

            UUID[] newBitstreamOrder = new UUID[bitstreams.size()];

            for (Bitstream bitstream : bitstreams)
            {
                //The order is determined by javascript
                //For each of our bitstream retrieve the order value
                int order = Util.getIntParameter(request, "order_" + bitstream.getID());
                //-1 the order since the order needed to start from one
                order--;
                //Place the bitstream identifier in the correct order
                newBitstreamOrder[order] = bitstream.getID();
            }

            BundleService bundleService = ContentServiceFactory.getInstance().getBundleService();

            if (newBitstreamOrder.length > 0)
            {
                //Set the new order in our bundle !
                bundleService.setOrder(context, bundle, newBitstreamOrder);
                bundleService.update(context, bundle);
            }
        }
    }
}
