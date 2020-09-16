# DUO-DSpace Configuration

In order to successfully deploy the Duo extensions to DSpace, you will need all of the following configuration files installed.  You may wish to update the configuration values for your particular requirements, and you should look in each of the configuration files for their own detailed documentation.  Here we will provide a brief overview of the configuration files, and where necessary go on to look at them in slightly more detail.

## Brief Overview

**inputforms.xml** - DSpace metadata entry forms which comply to the metadata standards used by StudentWeb and Cristin.

**workflow.xml** - XML workflow definition which encodes the Duo ingest workflow.  See the detailed documentation below for more details.

**xmlui.xconf** - XML UI configuration which turns on the XML Workflow required by Duo.  See the detailed documentation below for more details.

**crosswalks/cristin-generic.xsl** - Catch-all/generic crosswalk to handle all incoming content from Cristin.

**crosswalks/fs-submission.xsl** - Crosswalk to handle all incoming content from StudentWeb/FS

**emails/unitcodes** - Email template to send to administrators to alert them of a change to the unit codes of an updated item.  See the functional overview for details of where this appears in the workflow: [https://docs.google.com/a/cottagelabs.com/document/d/17Iiswcz_LkSMgdhEZIesV1BTrijRNsPWAyH29-L1rQg/edit](https://docs.google.com/a/cottagelabs.com/document/d/17Iiswcz_LkSMgdhEZIesV1BTrijRNsPWAyH29-L1rQg/edit)

**modules/cristin.cfg** - Configuration specific to the Cristin ingest.

**modules/curate.cfg** - Updated configuration to add our curation task to the list of available tasks

**modules/duo.cfg** - Main Duo configuration

**modules/swordv2-server.cfg** - Enhanced SWORDv2 configuration, containing the specific values required by the Duo extensions.

**registries/cristin-metadata.xml** - Metadata registry of fields required by the Cristin metadata schema.

**registries/fs-metadata.xml** - Metadata registry of fields required by the StudentWeb/FS metadata schema.

**registries/duo-metadata.xml** - Metadata registry of fields required by the general Duo metadata schema.

**spring/api/workflow-actions.xml** - Bindings of workflow actions to supporting classes, required to support the Duo XML workflow.  See the detailed documentation below for more details.

**spring/xmlui/workflow-actions-xmlui.xml** - Bindings of workflow actions to supporting user interface components, required to support the Duo XML workflow.  See the detailed docmentation below for more details.

## modules/duo.cfg

This file contains all of the configuration for the various parts of the duo extensions that don't belong elsewhere
in the configurations.

As part of the new functionality added to the Duo version of DSpace, we have added new configurable user 
interface components to allow administrators to select the desired behaviour.  This configuration then gives 
the user a choice between the default DSpace behaviour, and the behaviour required to support harvesting from 
Cristin:

```
oai.harvester.ingest_filter.option = none:No filtering of incoming items
oai.harvester.ingest_filter.option = cristin:Core Cristin types with full-text only

oai.harvester.metadata_update.option = all:Remove all existing metadata and replace completely
oai.harvester.metadata_update.option = cristin:Update only Cristin authority controlled metadata

oai.harvester.bundle_versioning.option = all:Remove all existing bundles and replace completely
oai.harvester.bundle_versioning.option = cristin:Synchronise bitstreams with Cristin

oai.harvester.ingest_workflow.option = archive:All items go directly to the DSpace archive
oai.harvester.ingest_workflow.option = cristin:All items go through the DSpace Workflow
```

The form of the configuration options is:

```
oai.harvester.<plugin>.option = <option name>:<human readable option text>
```

the "option name" is then used to load the appropriate plugin, as defined in dspace.cfg (see above).

Finally we also added an administrator eperson account which will provide a context for asynchronous 
harvesting (i.e. harvesting operations run by the scheduler, rather than on request by an administrator)

    oai.admin.eperson = richard

This can be the administrators email address or netid.


This file also provides the configuration for the URN Generator command line script.  This script 
connects to the National Library's API for generating URNs for repository items.

The most important configuration options are your institutional username and password:

    urn.idservice.username = username
    urn.idservice.password = password

You must then specify the URN series that you wish to generate identifiers within.  Using URN:NBN:no is equivalent to
not using a series for your institution, and is the default.

    urn.idservice.series = URN:NBN:no

You can then also specify the fields in which URNs and full-text file/bitstream links are stored:

    urn.urn.field = dc.identifier.urn
    urn.fulltext.field = dc.identifier.fulltext

See the config file itself for more details and configuration options

## workflow.xml

XML Workflow definitions which allow us to define the specific workflow used for items coming in from Cristin.  It is important that this configuration be correctly set for your repository before Cristin harvesting begins.

There are two key sections to this file which we are adding to the default workflow.xml.  The first is the workflow map, which maps collections to ingest workflows:

    <workflow-map>
        <name-map collection="default" workflow="default"/>
        <name-map collection="123456789/4404" workflow="cristin"/>
    </workflow-map>

This example maps the collection identified by the handle 123456789/4404 to the workflow id "cristin" (defined below).

The workflow itself is defined by this section of the file:
    
    <workflow start="bitstreamstep" id="cristin">
    
        <roles>
            <role id="filemanager" name="File Manager" 
                    description="The people responsible for this step are able to edit the
                                ordering of bitstreams and content of bundles" />
            <role id="editor" name="Editor" 
                    description="The people responsible for this step are able to edit the 
                                metadata of incoming submissions, and then accept or reject them." />
            <role id="assigner" name="Collection Assigner" 
                    description="people responsible for assigning the item to collections"/>
        </roles>

        <step id="bitstreamstep" role="filemanager" userSelectionMethod="claimaction">
            <outcomes>
                <step status="0">editstep</step>
            </outcomes>
            <actions>
                <action id="bitstreamaction"/>
            </actions>
        </step>
        
        <step id="editstep" role="editor" userSelectionMethod="claimaction">
            <outcomes>
                <step status="0">assignment</step>
            </outcomes>
            <actions>
                <action id="editaction"/>
            </actions>
        </step>
        
        <step id="assignment" role="assigner" userSelectionMethod="claimaction">
            <actions>
                <action id="assignmentaction"/>
            </actions>
        </step>
        
    </workflow>
    
The workflow starts with the "bitstreamstep" (where the user will reorganise the bitstreams), proceeds then to the "editstep" (where the user will update the metadata) and finally to the "assignment" step (where the user will assign the item to the relevant collections).  The underlying code for each of these stages can be found configured in **spring/api/workflow-actions.xml** and **spring/xmlui/workflow-actions-xmlui.xml**.

## xmlui.conf

This file allows us to configure the Aspects used by the XML UI.  For the Duo extensions, this allows us to activate the relevant Aspect for the XML Workflow.

Where we would originally find:

    <aspect name="Original Workflow" path="resource://aspects/Workflow/" />

we replace it with:

    <aspect name="XMLWorkflow" path="resource://aspects/XMLWorkflow/" />

## spring/api/workflow-actions.xml

This spring configuration provides the mappings from the actions defined in **workflow.xml** for each workflow stage to underlying Java classes that will handle the behaviour.

We first define the classes which will handle the actions:

    <bean id="assignmentactionAPI" class="no.uio.duo.XmlUICollectionAssignment" scope="prototype"/>
    <bean id="bitstreamactionAPI" class="no.uio.duo.XmlUIBitstreamReorder" scope="prototype"/>

We can then go on and define the relationship between the action defined in **workflow.xml** and the way that the actions (shown immediately above) are invoked.  We indicate that each action requires a UI, and this will ensure that the user interface components defined in **spring/xmlui/workflow-actions-xmlui.xml** are used.

    <bean id="assignmentaction" class="org.dspace.xmlworkflow.state.actions.WorkflowActionConfig" scope="prototype">
        <constructor-arg type="java.lang.String" value="assignmentaction"/>
        <property name="processingAction" ref="assignmentactionAPI"/>
        <property name="requiresUI" value="true"/>
    </bean>

    <bean id="bitstreamaction" class="org.dspace.xmlworkflow.state.actions.WorkflowActionConfig" scope="prototype">
        <constructor-arg type="java.lang.String" value="bitstreamaction"/>
        <property name="processingAction" ref="bitstreamactionAPI"/>
        <property name="requiresUI" value="true"/>
    </bean>

## spring/xmlui/workflow-actions-xmlui.xml

This spring confiruation provides the user interface components which are loaded by the XML Workflow actions (defined in the previous section)

    <bean id="assignmentaction_xmlui" class="no.uio.duo.XmlUICollectionAssignmentUI" scope="singleton"/>
    <bean id="bitstreamaction_xmlui" class="no.uio.duo.XmlUIBitstreamReorderUI" scope="singleton"/>

