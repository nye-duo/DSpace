package no.uio.duo;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.workflow.WorkflowItem;
import org.dspace.workflow.WorkflowItemService;
import org.dspace.workflow.WorkflowService;
import org.dspace.workflow.factory.WorkflowServiceFactory;
import org.dspace.xmlworkflow.WorkflowConfigurationException;
import org.dspace.workflow.WorkflowException;
import org.dspace.xmlworkflow.factory.XmlWorkflowServiceFactory;
import org.dspace.xmlworkflow.service.XmlWorkflowService;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.dspace.xmlworkflow.storedcomponents.service.XmlWorkflowItemService;

import javax.mail.MessagingException;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Wraper class providing an abstraction over the DSpace workflow manager.
 *
 * DSpace has two workflow managers, depending on whether the configurable workflow is in use or
 * not, and they have different API calls.  This class provides a single API which maps to
 * the appropriate call to the correct workflow manager, in contexts which are relevant to the
 * Duo application
 */
public class WorkflowManagerWrapper
{
    // workflow manager methods that we want across both implementations
    ////////////////////////////////////////////////////////////////////

    /**
     * Start the workflow for the workspace item
     *
     * Equivalent to:
     *
     * XmlWorkflowManager.start
     * WorkflowManager.start
     * @param context
     * @param wsItem
     * @throws SQLException
     * @throws AuthorizeException
     * @throws IOException
     * @throws WorkflowException
     * @throws WorkflowConfigurationException
     * @throws MessagingException
     */
    public static void start(Context context, WorkspaceItem wsItem)
            throws SQLException, AuthorizeException, IOException, WorkflowException, WorkflowConfigurationException, MessagingException
    {
        if (ConfigurationManager.getProperty("duo.workflowmanagerwrapper").equals("xmlworkflow"))
        {
            XmlWorkflowService xmlWorkflowService = XmlWorkflowServiceFactory.getInstance().getXmlWorkflowService();
            xmlWorkflowService.start(context, wsItem);
        }
        else
        {
            WorkflowService workflowService = WorkflowServiceFactory.getInstance().getWorkflowService();
            workflowService.start(context, wsItem);
        }
    }

    /**
     * Start the workflow on the workspace item, but do not notify the user by email
     *
     * Equivalent to
     *
     * XmlWorkflowManager.startWithoutNotify
     * WorkflowManager.startWithoutNotify
     *
     * @param context
     * @param wsItem
     * @throws SQLException
     * @throws AuthorizeException
     * @throws IOException
     * @throws WorkflowException
     * @throws WorkflowConfigurationException
     * @throws MessagingException
     */
    public static void startWithoutNotify(Context context, WorkspaceItem wsItem)
            throws SQLException, AuthorizeException, IOException, WorkflowException, WorkflowConfigurationException, MessagingException
    {
        if (ConfigurationManager.getProperty("duo.workflowmanagerwrapper").equals("xmlworkflow"))
        {
            XmlWorkflowService xmlWorkflowService = XmlWorkflowServiceFactory.getInstance().getXmlWorkflowService();
            xmlWorkflowService.startWithoutNotify(context, wsItem);
        }
        else
        {
            WorkflowService workflowService = WorkflowServiceFactory.getInstance().getWorkflowService();
            workflowService.startWithoutNotify(context, wsItem);
        }
    }

    /**
     * Abort the workflow on the InProgressSubmission item
     *
     * Equivalent to
     *
     * XmlWorkflowManager.setWorkflowItemBackSubmission
     * WorkflowManager.abort
     *
     * @param context
     * @param wfItem
     * @param ePerson
     * @throws SQLException
     * @throws AuthorizeException
     * @throws IOException
     * @throws WorkflowException
     * @throws WorkflowConfigurationException
     * @throws MessagingException
     */
    public static void abort(Context context, InProgressSubmission wfItem, EPerson ePerson)
            throws SQLException, AuthorizeException, IOException, WorkflowException, WorkflowConfigurationException, MessagingException
    {
        // ugly eperson verification/acquisition/error bit
        if (ePerson == null)
        {
            String adminEperson = ConfigurationManager.getProperty("cristin.admin.eperson");
            EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();
            ePerson = ePersonService.findByEmail(context, adminEperson);
            if (ePerson == null)
            {
                ePerson = ePersonService.findByNetid(context, adminEperson);
            }
        }
        if (ePerson == null)
        {
            throw new WorkflowException("No admin eperson defined, and passed eperson is null - probably need to fix your config");
        }

        if (ConfigurationManager.getProperty("duo.workflowmanagerwrapper").equals("xmlworkflow"))
        {
            XmlWorkflowService xmlWorkflowService = XmlWorkflowServiceFactory.getInstance().getXmlWorkflowService();
            xmlWorkflowService.sendWorkflowItemBackSubmission(context, (XmlWorkflowItem) wfItem, ePerson, "", "");
        }
        else
        {
            WorkflowService workflowService = WorkflowServiceFactory.getInstance().getWorkflowService();
            workflowService.abort(context, (WorkflowItem) wfItem, ePerson);
        }
    }

    // our own workflow control
    ///////////////////////////

    /**
     * Restar the workflow.  If the item is in the workspace the workflow will be started, if it is in the
     * workflow it will go back to the first step
     *
     * @param context
     * @param item
     * @throws SQLException
     * @throws AuthorizeException
     * @throws IOException
     */
    public static void restartWorkflow(Context context, Item item)
                throws SQLException, AuthorizeException, IOException
    {
        // stop the workflow
        WorkflowManagerWrapper.stopWorkflow(context, item);

        // now start the workflow again
        WorkflowManagerWrapper.startWorkflow(context, item);
    }

    /**
     * Start the workflow on the item if it is in the workspace
     *
     * @param context
     * @param item
     * @throws SQLException
     * @throws AuthorizeException
     * @throws IOException
     */
    public static void startWorkflow(Context context, Item item)
            throws SQLException, AuthorizeException, IOException
    {
        WorkspaceItem wsi = WorkflowManagerWrapper.getWorkspaceItem(context, item);
        WorkflowManagerWrapper.startWorkflow(context, wsi);
    }

    /**
     * Start the workflow on the workflow item
     *
     * @param context
     * @param wsi
     * @throws SQLException
     * @throws AuthorizeException
     * @throws IOException
     */
    public static void startWorkflow(Context context, WorkspaceItem wsi)
            throws SQLException, AuthorizeException, IOException
    {
        try
        {
            WorkflowManagerWrapper.startWithoutNotify(context, wsi);
        }
        catch (WorkflowException | WorkflowConfigurationException | MessagingException e)
        {
            throw new IOException(e);
        }
    }

    /**
     * Stop the workflow on the item if it is in the workflow.  Will send the item back to the workspace
     *
     * @param context
     * @param item
     * @throws SQLException
     * @throws AuthorizeException
     * @throws IOException
     */
    public static void stopWorkflow(Context context, Item item)
            throws SQLException, AuthorizeException, IOException
    {
        try
        {
            // find the item in the workflow if it exists
            InProgressSubmission wfi = WorkflowManagerWrapper.getWorkflowItem(context, item);

            // abort the workflow
            if (wfi != null)
            {
                WorkflowManagerWrapper.abort(context, wfi, context.getCurrentUser());
            }
        }
        catch (WorkflowException | WorkflowConfigurationException | MessagingException e)
        {
            throw new IOException(e);
        }
    }

    //////////////////////////////////////////////
	// item access methods
	//////////////////////////////////////////////

    /**
     * Determine if the item is in the workflow
     *
     * @param context
     * @param item
     * @return
     * @throws SQLException
     */
    public static boolean isItemInWorkflow(Context context, Item item)
            throws SQLException
    {
        if (ConfigurationManager.getProperty("duo.workflowmanagerwrapper").equals("xmlworkflow"))
        {
            return WorkflowManagerWrapper.isItemInXmlWorkflow(context, item);
        }
        else
        {
            return WorkflowManagerWrapper.isItemInOriginalWorkflow(context, item);
        }
    }

    /**
     * Is the item in the standard DSpace workflow (as opposed to the Xml workflow)
     *
     * @param context
     * @param item
     * @return
     * @throws SQLException
     */
    public static boolean isItemInOriginalWorkflow(Context context, Item item)
            throws SQLException
    {
        WorkflowItemService workflowItemService = WorkflowServiceFactory.getInstance().getWorkflowItemService();
        WorkflowItem wfi = workflowItemService.findByItem(context, item);
        return wfi != null;

        /*
        String hql = String.format("select wf from org.dspace.workflowbasic.BasicWorkflowItem as wf where wf.item = '%s'", item.getID().toString());
        PassThroughDAO dao = new PassThroughDAO();
        Query query = dao.createQuery(context, hql);
        Object result = dao.singleResult(query);
        return result != null;
        */

        /*
        String query = "SELECT workflow_id FROM workflowitem WHERE item_id = ?";
        Object[] params = { item.getID() };
        TableRowIterator tri = DatabaseManager.query(context, query, params);
        if (tri.hasNext())
        {
            tri.close();
            return true;
        }
        return false;
        */
    }

    /**
     * Is the item in the newer Xml workflow (as opposed to the standard DSpace workflow)
     *
     * @param context
     * @param item
     * @return
     * @throws SQLException
     */
    public static boolean isItemInXmlWorkflow(Context context, Item item)
            throws SQLException
    {
        XmlWorkflowItemService xmlWorkflowItemService = XmlWorkflowServiceFactory.getInstance().getXmlWorkflowItemService();
        XmlWorkflowItem wfi = xmlWorkflowItemService.findByItem(context, item);
        return wfi != null;

        /*
        String hql = String.format("select wf from org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem as wf where wf.item = '%s'", item.getID().toString());
        PassThroughDAO dao = new PassThroughDAO();
        Query query = dao.createQuery(context, hql);
        Object result = dao.singleResult(query);
        return result != null;
        */

        /*
        String query = "SELECT workflowitem_id FROM cwf_workflowitem WHERE item_id = ?";
        Object[] params = { item.getID() };
        TableRowIterator tri = DatabaseManager.query(context, query, params);
        if (tri.hasNext())
        {
            tri.close();
            return true;
        }
        return false;
        */
    }

    // FIXME: this may become useful when we have a proper treatment for licences

    /**
     * Is the item in the workspace
     *
     * @param context
     * @param item
     * @return
     * @throws SQLException
     */
    public static boolean isItemInWorkspace(Context context, Item item)
            throws SQLException
    {
        WorkspaceItemService workspaceItemService = ContentServiceFactory.getInstance().getWorkspaceItemService();
        WorkspaceItem wsi = workspaceItemService.findByItem(context, item);
        return wsi != null;

        /*
        String query = "SELECT workspace_item_id FROM workspaceitem WHERE item_id = ?";
        Object[] params = { item.getID() };
        TableRowIterator tri = DatabaseManager.query(context, query, params);
        if (tri.hasNext())
        {
            tri.close();
            return true;
        }
        return false;
        */
    }

    /**
     * Get the workflow item for the given item
     *
     * @param context
     * @param item
     * @return
     * @throws SQLException
     * @throws AuthorizeException
     * @throws IOException
     */
    public static InProgressSubmission getWorkflowItem(Context context, Item item)
            throws SQLException, AuthorizeException, IOException
    {
        if (ConfigurationManager.getProperty("duo.workflowmanagerwrapper").equals("xmlworkflow"))
        {
            return WorkflowManagerWrapper.getXmlWorkflowItem(context, item);
        }
        else
        {
            return WorkflowManagerWrapper.getOriginalWorkflowItem(context, item);
        }
    }

    /**
     * Get the Xml workflow item from the xml workflow
     *
     * @param context
     * @param item
     * @return
     * @throws SQLException
     * @throws AuthorizeException
     * @throws IOException
     */
    public static XmlWorkflowItem getXmlWorkflowItem(Context context, Item item)
			throws SQLException, AuthorizeException, IOException
	{
        XmlWorkflowItemService xmlWorkflowItemService = XmlWorkflowServiceFactory.getInstance().getXmlWorkflowItemService();
        XmlWorkflowItem wfi = xmlWorkflowItemService.findByItem(context, item);
        return wfi;
        /*
        String query = "SELECT workflowitem_id FROM cwf_workflowitem WHERE item_id = ?";
        Object[] params = { item.getID() };
        TableRowIterator tri = DatabaseManager.query(context, query, params);
        if (tri.hasNext())
        {
            TableRow row = tri.next();
            int wfid = row.getIntColumn("workflowitem_id");
            XmlWorkflowItem.find(context, wfid);
            XmlWorkflowItem wfi = XmlWorkflowItem.find(context, wfid);
            tri.close();
            return wfi;
        }
        return null;
        */
	}

    /**
     * Get the workflow item from the standard DSpace workflow
     *
     * @param context
     * @param item
     * @return
     * @throws SQLException
     */
	public static WorkflowItem getOriginalWorkflowItem(Context context, Item item)
			throws SQLException
	{
        WorkflowItemService workflowItemService = WorkflowServiceFactory.getInstance().getWorkflowItemService();
        WorkflowItem wfi = workflowItemService.findByItem(context, item);
        return wfi;

        /*
        String query = "SELECT workflow_id FROM workflowitem WHERE item_id = ?";
        Object[] params = { item.getID() };
        TableRowIterator tri = DatabaseManager.query(context, query, params);
        if (tri.hasNext())
        {
            TableRow row = tri.next();
            int wfid = row.getIntColumn("workflow_id");
            WorkflowItem wfi = WorkflowItem.find(context, wfid);
            tri.close();
            return wfi;
        }
        return null;
        */
	}

    /**
     * Get the workspace item for the given item
     *
     * @param context
     * @param item
     * @return
     * @throws SQLException
     */
	public static WorkspaceItem getWorkspaceItem(Context context, Item item)
			throws SQLException
	{
        WorkspaceItemService workspaceItemService = ContentServiceFactory.getInstance().getWorkspaceItemService();
        WorkspaceItem wsi = workspaceItemService.findByItem(context, item);
        return wsi;

        /*
        String query = "SELECT workspace_item_id FROM workspaceitem WHERE item_id = ?";
        Object[] params = { item.getID() };
        TableRowIterator tri = DatabaseManager.query(context, query, params);
        if (tri.hasNext())
        {
            TableRow row = tri.next();
            int wsid = row.getIntColumn("workspace_item_id");
            WorkspaceItem wsi = WorkspaceItem.find(context, wsid);
            tri.close();
            return wsi;
        }
        return null;
        */
	}
}
