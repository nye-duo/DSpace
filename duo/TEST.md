# Testing

This document describes some manual and automated tests that you can run against DSpace to determine appropriate
behaviour of the Duo extensions.

## Metadata (HTML) Cleanup

In order to test for HTML cleanup, we need to load an item with HTML in the metadata into the system.  The following
script will create a community, collection and a single item which has HTML for the metadata cleanup script to clean
for you:

    [dspace]/bin/dspace dsrun no.uio.duo.cleanup.LiveMetadataCleanupTest -e [admin account email]
    
The item ID and handle will output to the screen, so you can look it up via the DSpace interface.

You should then run the MetadataCleanup script, which can be done with:

    [dspace]/bin/dspace dsrun no.uio.duo.cleanup.MetadataCleanup -e [admin email] -i [item id]
    
Once this has been done, check the item created in the first step to ensure that the HTML has been cleaned correctly.

## Policy Pattern Manager

### IMPORTANT: Before testing

To test the policy pattern manager, we need to create a number of test items in the DSpace instance.  To do this
in such a way as the tests can then successfully run, we need to disable a number of components of DSpace which
act when an item is added to the repository: the embargo setter and the Duo install-consumer.

To do this:

* In [dspace]/local.cfg

replace:

    event.dispatcher.default.consumers = versioning, discovery, eperson, duo

with:

    event.dispatcher.default.consumers = versioning, discovery, eperson

* In [dspace]/config/modules/duo.cfg:

    * specify a duo.embargo.communities value which contains a non-existent community
    * comment out the lines:

```
event.consumer.duo.class = no.uio.duo.DuoEventConsumer
event.consumer.duo.filters = Item+Install|Modify
```

Be sure to restart DSpace after making these changes, and don't forget to put them back after you have finished running
the tests.

### Running the tests

To test the policy pattern manager you can run a live functional test on a running DSpace with the following command:

    [dspace]/bin/dspace dsrun no.uio.duo.livetest.LivePolicyPatternTest -e [eperson email] -b [path to bitstream] -u [dspace base url] -m [test matrix file] -o [output report path]
    
**DO NOT UNDER ANY CIRCUMSTANCES RUN THIS ON A PRODUCTION SYSTEM** - it makes changes to the community and collection 
structure, and adds/removes items from the system.
    
For example in [dspace]/bin:

    ./dspace dsrun no.uio.duo.livetest.LivePolicyPatternTest -e richard@cottagelabs.com -b /home/richard/Code/External/duo-2020/Duo-DSpace/docs/system/TEST.md -u http://localhost:8080/xmlui -m /home/richard/Code/External/duo-2020/DSpace/dspace/modules/additions/src/test/resources/livepolicypattern_ppm_testmatrix.csv -o /home/richard/tmp/livepolicypattern_ppm_check.csv

This will execute the tests as defined in src/test/resources/livepolicypattern_ppm_testmatrix.csv

The output of this process will be a csv file which you can open in Excel, which will give you the test number (from livepolicypattern_ppm_testmatrix.csv) and
a before/after URL which will take you to items which can be compared to show you what the item was like before the policy
system was applied, and then again after.  This can be used for manually checking the results of the process.  Note that the
policy test system does also check the results automatically.


## Event Consumer

In order to ensure that the DuoEventConsumer is behaving correctly with regard to applying the appropriate policies
when an item is installed into the archive, you can use the following Live test.

### IMPORTANT: Before testing

To test the event consumer, we need to ensure that the consumer will run when an item is installed/modified in DSpace.

In `duo.cfg`:

* Comment out duo.embargo.communities - this means it will run on all communities
* Ensure the DuoEventConsumer configuration is set

```
event.consumer.duo.class = no.uio.duo.DuoEventConsumer
event.consumer.duo.filters = Item+Install|Modify
```

In `local.cfg`:

* Set the correct list of event consumers:

```
event.dispatcher.default.consumers = versioning, discovery, eperson, duo
```

Be sure to restart DSpace after making these changes, and don't forget to put them back the way they were after you have finished running
the tests.

### Testing Standard Policy Patterns

To test that policy patterns are appropriately applied by the PolicyPatternManager during a normal install, you can run
the above LivePolicyPatternTest with a special mode.

The above LivePolicyPatternTest is designed to test the PolicyPatternManager itself, and not whether it is applied correctly
during an install of a new item.  The test below ensures that newly submitted items passing through the DuoInstallConsumer
have the PolicyPatternManager applied correctly.

    [dspace]/bin/dspace dsrun no.uio.duo.livetest.LivePolicyPatternTest -e [eperson email] -b [path to bitstream] -u [dspace base url] -m [test matrix file] -o [output report path] -w

Note the addition of the -w option - this causes the test to leave the reference item in the user workspace, so you can
compare the before and after submission items.  Administrator URLs for the item in the user workspace are output in
the final report from the test.

Additionally, the test matrix file is different to the one used before.  Instead we are only testing items which are "new",
and we give them the type "other" instead, to distinguish them from the previous test.  The test resource "livepolicypattern_consumer_testmatrix.csv"
provides the appropriate test parameters.
    
**DO NOT UNDER ANY CIRCUMSTANCES RUN THIS ON A PRODUCTION SYSTEM** - it makes changes to the community and collection 
structure, and adds/removes items from the system.
    
For example in [dspace]/bin:

    ./dspace dsrun no.uio.duo.livetest.LivePolicyPatternTest -e richard@cottagelabs.com -b /home/richard/Code/External/duo-2020/Duo-DSpace/docs/system/TEST.md -u http://localhost:8080/xmlui -m /home/richard/Code/External/duo-2020/DSpace/dspace/modules/additions/src/test/resources/livepolicypattern_consumer_testmatrix.csv -o /home/richard/tmp/livepolicypattern_consumer_check.csv

### Testing FS Policies - Install

To test the event consumer for item installs you can run a live functional test on a running DSpace with the following command:

    [dspace]/bin/dspace dsrun no.uio.duo.livetest.LiveFSInstallTest -e [eperson email] -b [path to bitstream] -u [dspace base url] -m [test matrix file] -o [output report path]
    
**DO NOT UNDER ANY CIRCUMSTANCES RUN THIS ON A PRODUCTION SYSTEM** - it makes changes to the community and collection 
structure, and adds/removes items from the system.
    
For example in [dspace]/bin:

    ./dspace dsrun no.uio.duo.livetest.LiveFSInstallTest -e richard@cottagelabs.com -b /home/richard/Code/External/duo-2020/Duo-DSpace/docs/system/TEST.md -u http://localhost:8080/xmlui -m /home/richard/Code/External/duo-2020/DSpace/dspace/modules/additions/src/test/resources/livefsinstall_testmatrix.csv -o /home/richard/tmp/livefsinstall_check.csv

This will execute the tests as defined in src/test/resources/livefsinstall_testmatrix.csv

The output of this process will be a csv file which you can open in Excel, which will give you the test number (from livefsinstall_testmatrix.csv) and
a before/after URL which will take you to items which can be compared to show you what the item was like before the install to the repo
was applied, and then again after.  This can be used for manually checking the results of the process.  Note that the
install test system does also check the results automatically.


### Testing FS Policies - Reinstate

To test the event consumer for item reinstates, you can run a live functional test on a running DSpace with the following command:

    [dspace]/bin/dspace dsrun no.uio.duo.livetest.LiveFSReinstateTest -e [eperson email] -b [path to bitstream] -u [dspace base url] -m [test matrix file] -o [output report path]

**DO NOT UNDER ANY CIRCUMSTANCES RUN THIS ON A PRODUCTION SYSTEM** - it makes changes to the community and collection 
structure, and adds/removes items from the system.
    
For example in [dspace]/bin:

    ./dspace dsrun no.uio.duo.livetest.LiveFSReinstateTest -e richard@cottagelabs.com -b /home/richard/Code/External/duo-2020/Duo-DSpace/docs/system/TEST.md -u http://localhost:8080/xmlui -m /home/richard/Code/External/duo-2020/DSpace/dspace/modules/additions/src/test/resources/livefsreinstate_testmatrix.csv -o /home/richard/tmp/livefsreinstate_check.csv

This will execute the tests as defined in src/test/resources/livefsreinstate_testmatrix.csv

The output of this process will be a csv file which you can open in Excel, which will give you the test number (from livefsreinstate_testmatrix.csv) and
a before/after URL which will take you to items which can be compared to show you what the item was like before the reinstate
was applied, and then again after.  This can be used for manually checking the results of the process.  Note that the test 
system does also check the results automatically.


### Testing FS Policies - Modify Metadata

**IMPORTANT: there is no need to run this test, as Modify events are no longer handled by this system.  This section remains as a reminder, in case we choose to re-enable them later** 

To test the event consumer for metadata modifies, you can run a live functional test on a running DSpace with the following command:

    [dspace]/bin/dspace dsrun no.uio.duo.livetest.LiveFSModifyMetadataTest -e [eperson email] -b [path to bitstream] -u [dspace base url] -m [test matrix file] -o [output report path]

**DO NOT UNDER ANY CIRCUMSTANCES RUN THIS ON A PRODUCTION SYSTEM** - it makes changes to the community and collection 
structure, and adds/removes items from the system.
    
For example in [dspace]/bin:

    ./dspace dsrun no.uio.duo.livetest.LiveFSModifyMetadataTest -e richard@cottagelabs.com -b /home/richard/Code/External/Duo-DSpace/docs/system/TEST.md -u http://localhost:8080/xmlui -m /home/richard/Code/External/Duo-DSpace/src/test/resources/livefsmodifymetadata_testmatrix.csv -o /home/richard/Code/External/Duo-DSpace/src/test/resources/check.csv

This will execute the tests as defined in src/test/resources/livefsmodifymetadata_testmatrix.csv

The output of this process will be a csv file which you can open in Excel, which will give you the test number (from livefsmodifymetadata_testmatrix.csv) and
a before/after URL which will take you to items which can be compared to show you what the item was like before the modification
was applied, and then again after.  This can be used for manually checking the results of the process.  Note that the test 
system does also check the results automatically.