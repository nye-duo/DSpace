package no.uio.duo;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.dspace.workflow.WorkflowItem;
import org.dspace.workflow.WorkflowItemService;
import org.dspace.workflow.factory.WorkflowServiceFactory;
import org.dspace.xmlworkflow.factory.XmlWorkflowServiceFactory;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.dspace.xmlworkflow.storedcomponents.service.XmlWorkflowItemService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Utility base class for scripts that want to be able to traverse the whole of DSpace, or parts of it.
 *
 * This class solves the issue of handling workflow and workspace items too.  Choose your entry point, and then
 * this class will iterate down from there, passing through each community/sub-community/collection/item/workflow item/workspace item
 *
 * Subclasses should at least implement processItem, but may override any of the other methods too.
 *
 * This class also handles aborting and completing the context, if desired
 */
public abstract class TraverseDSpace
{
    protected Context context;
    protected EPerson eperson;

    protected boolean manageContext = false;
    protected String contextEntryPoint = null;

    protected int communityCount = 0;
    protected int collectionCount = 0;
    protected int workspaceCount = 0;
    protected int workflowCount = 0;
    protected int itemCount = 0;

    /**
     * Create a new utility which traverses DSpace.  This will not manage the context for you, so if you use this
     * one you need to manage the context yourself.
     *
     * @param epersonEmail
     * @throws Exception
     */
    public TraverseDSpace(String epersonEmail)
            throws Exception
    {
        this(epersonEmail, false);
    }

    /**
     * Create an instance of the object, where the context will be initialised around the eperson account provided.
     *
     * if manageContext is true, the utility will also abort or complete the context at the end of the run
     *
     * @param epersonEmail
     * @throws Exception
     */
    public TraverseDSpace(String epersonEmail, boolean manageContext)
            throws Exception
    {
        this.manageContext = manageContext;
        this.context = new Context();

        EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();
        this.eperson = ePersonService.findByEmail(this.context, epersonEmail);
        this.context.setCurrentUser(this.eperson);
    }

    /**
     * Record the entry point the caller has used to the utility.  This allows us to track at what level to complete
     * or abort the context at the end
     *
     * @param entryPoint
     */
    private void setContextEntryPoint(String entryPoint)
    {
        if (this.contextEntryPoint == null)
        {
            this.contextEntryPoint = entryPoint;
        }
    }

    /**
     * Is the context being managed for the given entry point?  Methods use this to determine whether it is their
     * responsibility to commit or abort the context
     *
     * @param entryPoint
     * @return
     */
    private boolean contextManaged(String entryPoint)
    {
        return this.manageContext && entryPoint.equals(this.contextEntryPoint);
    }

    /**
     * Hit every object in the whole of DSpace
     *
     * @throws Exception
     */
    public void doDSpace()
            throws Exception
    {
        String entryPoint = "DSpace";
        this.setContextEntryPoint(entryPoint);

        try
        {
            CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();

            List<Community> comms = communityService.findAllTop(this.context);
            for (int i = 0; i < comms.size(); i++)
            {
                this.doCommunity(comms.get(i));
            }
        }
        catch (Exception e)
        {
            System.out.println(e.getClass().getName());
            if (this.contextManaged(entryPoint))
            {
                this.context.abort();
            }
            throw e;
        }
        finally
        {
            if (this.contextManaged(entryPoint))
            {
                if (this.context.isValid())
                {
                    this.context.complete();
                }
                this.contextEntryPoint = null;
            }
        }
    }

    /**
     * Do community and everything therein
     *
     * @param handle
     * @throws Exception
     */
    public void doCommunity(String handle)
            throws Exception
    {
        String entryPoint = "CommunityHandle";
        this.setContextEntryPoint(entryPoint);

        try
        {
            HandleService handleService = HandleServiceFactory.getInstance().getHandleService();
            DSpaceObject dso = handleService.resolveToObject(this.context, handle);
            if (!(dso instanceof Community))
            {
                throw new Exception(handle + " does not resolve to a Community");
            }
            this.doCommunity((Community) dso);
        }
        catch (Exception e)
        {
            if (this.contextManaged(entryPoint))
            {
                this.context.abort();
            }
            throw e;
        }
        finally
        {
            if (this.contextManaged(entryPoint))
            {
                if (this.context.isValid())
                {
                    this.context.complete();
                }
                this.contextEntryPoint = null;
            }
        }
    }

    /**
     * Do community and everything therein
     *
     * @param community
     * @throws Exception
     */
    public void doCommunity(Community community)
            throws Exception
    {
        String entryPoint = "Community";
        this.setContextEntryPoint(entryPoint);

        try
        {
            List<Community> comms = community.getSubcommunities();
            for (int i = 0; i < comms.size(); i++)
            {
                this.doCommunity(comms.get(i));
            }

            List<Collection> cols = community.getCollections();
            for (int i = 0; i < cols.size(); i++)
            {
                this.doCollection(cols.get(i));
            }

            this.communityCount++;
        }
        catch (Exception e)
        {
            if (this.contextManaged(entryPoint))
            {
                this.context.abort();
            }
            throw e;
        }
        finally
        {
            if (this.contextManaged(entryPoint))
            {
                if (this.context.isValid())
                {
                    this.context.complete();
                }
                this.contextEntryPoint = null;
            }
        }
    }

    /**
     * Do collection and everything therein
     *
     * @param handle
     * @throws SQLException
     * @throws Exception
     */
    public void doCollection(String handle)
            throws SQLException, Exception
    {
        String entryPoint = "CollectionHandle";
        this.setContextEntryPoint(entryPoint);

        try
        {
            HandleService handleService = HandleServiceFactory.getInstance().getHandleService();
            DSpaceObject dso = handleService.resolveToObject(this.context, handle);
            if (!(dso instanceof Collection))
            {
                throw new Exception(handle + " does not resolve to a Collection");
            }
            this.doCollection((Collection) dso);
        }
        catch (Exception e)
        {
            if (this.contextManaged(entryPoint))
            {
                this.context.abort();
            }
            throw e;
        }
        finally
        {
            if (this.contextManaged(entryPoint))
            {
                if (this.context.isValid())
                {
                    this.context.complete();
                }
                this.contextEntryPoint = null;
            }
        }
    }

    /**
     * Do collection and everything therein
     *
     * @param collection
     * @throws SQLException
     * @throws Exception
     */
    public void doCollection(Collection collection)
            throws SQLException, Exception
    {
        String entryPoint = "Collection";
        this.setContextEntryPoint(entryPoint);

        try
        {
            ItemService itemService = ContentServiceFactory.getInstance().getItemService();
            Iterator<Item> ii = itemService.findAllByCollection(this.context, collection);

            // do all the items in the collection, withdrawn or not
            while (ii.hasNext())
            {
                Item item = ii.next();
                this.doItem(item);
            }

            // do all the items in the collection's workflow (both normal and XML)
            WorkflowItemService workflowItemService = WorkflowServiceFactory.getInstance().getWorkflowItemService();
            List<WorkflowItem> wfis = workflowItemService.findByCollection(this.context, collection);
            for (WorkflowItem wfi : wfis)
            {
                Item item = wfi.getItem();
                this.doItem(item);
            }

            XmlWorkflowItemService xmlWorkflowItemService = XmlWorkflowServiceFactory.getInstance().getXmlWorkflowItemService();
            List<XmlWorkflowItem> xwfis = xmlWorkflowItemService.findByCollection(this.context, collection);
            for (XmlWorkflowItem xwfi : xwfis)
            {
                Item item = xwfi.getItem();
                this.doItem(item);
            }

            // do all the items in the collection's workspace
            WorkspaceItemService workspaceItemService = ContentServiceFactory.getInstance().getWorkspaceItemService();
            List<WorkspaceItem> wsis = workflowItemService.findByCollection(this.context, collection);
            for (WorkspaceItem wsi : wsis)
            {
                Item item = wsi.getItem();
                this.doItem(item);
            }

            this.collectionCount++;
        }
        catch (Exception e)
        {
            if (this.contextManaged(entryPoint))
            {
                this.context.abort();
            }
            throw e;
        }
        finally
        {
            if (this.contextManaged(entryPoint))
            {
                if (this.context.isValid())
                {
                    this.context.complete();
                }
                this.contextEntryPoint = null;
            }
        }
    }

    /**
     * Do item by handle
     *
     * @param handle
     * @throws SQLException
     * @throws Exception
     */
    public void doItem(String handle)
            throws SQLException, Exception
    {
        String entryPoint = "ItemHandle";
        this.setContextEntryPoint(entryPoint);

        try
        {
            HandleService handleService = HandleServiceFactory.getInstance().getHandleService();
            DSpaceObject dso = handleService.resolveToObject(this.context, handle);
            if (!(dso instanceof Item))
            {
                throw new Exception(handle + " does not resolve to an Item");
            }
            this.doItem((Item) dso);
        }
        catch (Exception e)
        {
            if (this.contextManaged(entryPoint))
            {
                this.context.abort();
            }
            throw e;
        }
        finally
        {
            if (this.contextManaged(entryPoint))
            {
                if (this.context.isValid())
                {
                    this.context.complete();
                }
                this.contextEntryPoint = null;
            }
        }
    }

    /**
     * Do item by either id or handle
     *
     * @param id
     * @param handle
     * @throws SQLException
     * @throws Exception
     */
    public void doItem(UUID id, String handle)
            throws SQLException, Exception
    {
        String entryPoint = "ItemIDHandle";
        this.setContextEntryPoint(entryPoint);

        try
        {
            if (id != null)
            {
                ItemService itemService = ContentServiceFactory.getInstance().getItemService();
                Item item = itemService.find(this.context, id);
                if (item != null)
                {
                    this.doItem(item);
                }
            }
            else if (handle != null)
            {
                this.doItem(handle);
            }
            else
            {
                throw new Exception("You must provide one of id or handle");
            }
        }
        catch (Exception e)
        {
            if (this.contextManaged(entryPoint))
            {
                this.context.abort();
            }
            throw e;
        }
        finally
        {
            if (this.contextManaged(entryPoint))
            {
                if (this.context.isValid())
                {
                    this.context.complete();
                }
                this.contextEntryPoint = null;
            }
        }
    }

    /**
     * Do workflow item
     *
     * @param wfid
     * @throws SQLException
     * @throws Exception
     */
    public void doWorkflowItem(int wfid)
            throws SQLException, Exception
    {
        String entryPoint = "WorkflowID";
        this.setContextEntryPoint(entryPoint);

        try
        {
            InProgressSubmission wfi = null;
            WorkflowItemService workflowItemService = WorkflowServiceFactory.getInstance().getWorkflowItemService();
            wfi = workflowItemService.find(this.context, wfid);
            if (wfi == null)
            {
                XmlWorkflowItemService xmlWorkflowItemService = XmlWorkflowServiceFactory.getInstance().getXmlWorkflowItemService();
                wfi = xmlWorkflowItemService.find(this.context, wfid);
            }
            if (wfi == null)
            {
                throw new Exception(Integer.toString(wfid) + " does not resolve to a workflow item");
            }
            this.doItem(wfi.getItem());

            this.workflowCount++;
        }
        catch (Exception e)
        {
            if (this.contextManaged(entryPoint))
            {
                this.context.abort();
            }
            throw e;
        }
        finally
        {
            if (this.contextManaged(entryPoint))
            {
                if (this.context.isValid())
                {
                    this.context.complete();
                }
                this.contextEntryPoint = null;
            }
        }
    }

    /**
     * Do workspace item
     *
     * @param wsid
     * @throws SQLException
     * @throws Exception
     */
    public void doWorkspaceItem(int wsid)
            throws SQLException, Exception
    {
        String entryPoint = "WorkspaceID";
        this.setContextEntryPoint(entryPoint);

        try
        {
            WorkspaceItemService workspaceItemService = ContentServiceFactory.getInstance().getWorkspaceItemService();
            WorkspaceItem wsi = workspaceItemService.find(this.context, wsid);
            if (wsi == null)
            {
                throw new Exception(Integer.toString(wsid) + " does not resolve to a workspace item");
            }
            this.doItem(wsi.getItem());

            this.workspaceCount++;
        }
        catch (Exception e)
        {
            if (this.contextManaged(entryPoint))
            {
                this.context.abort();
            }
            throw e;
        }
        finally
        {
            if (this.contextManaged(entryPoint))
            {
                if (this.context.isValid())
                {
                    this.context.complete();
                }
                this.contextEntryPoint = null;
            }
        }
    }

    /**
     * Do item.  Internally, this calls processItem, which is the abstract method subclasses should implement.
     *
     * @param item
     * @throws SQLException
     * @throws AuthorizeException
     * @throws IOException
     * @throws Exception
     */
    public void doItem(Item item)
            throws SQLException, AuthorizeException, IOException, Exception
    {
        String entryPoint = "WorkspaceID";
        this.setContextEntryPoint(entryPoint);

        try
        {
            this.processItem(item);
            this.itemCount++;
        }
        catch (Exception e)
        {
            if (this.contextManaged(entryPoint))
            {
                this.context.abort();
            }
            throw e;
        }
        finally
        {
            if (this.contextManaged(entryPoint))
            {
                if (this.context.isValid())
                {
                    this.context.complete();
                }
                this.contextEntryPoint = null;
            }
        }
    }

    /**
     * Output the counts of objects that have been touched by the utility so far
     */
    public void report()
    {
        System.out.println("Processed " + this.communityCount + " Communities");
        System.out.println("Processed " + this.collectionCount + " Collections");
        System.out.println("Processed " + this.itemCount + " Items");
        System.out.println("\tof which " + this.workflowCount + " Workflow Items");
        System.out.println("\tof which " + this.workspaceCount + " Workspace Items");
    }

    /**
     * This is the method to implement for actions that should be performed on your item
     * 
     * @param item
     * @throws SQLException
     * @throws AuthorizeException
     * @throws IOException
     * @throws Exception
     */
    protected abstract void processItem(Item item) throws SQLException, AuthorizeException, IOException, Exception;
}
