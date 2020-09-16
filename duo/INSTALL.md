# DUO Extensions Installation

## How to use this documentation

Before going on to any form of installation, be sure that the Dependencies are satisfied, as documented in the next section

If you are installing a fresh DSpace, you can safely follow the sections **Fresh Installation** section.

If you are installing on an existing DSpace instance, you should follow the section **Update existing DSpace**


## Dependencies

### Java 1.7 or Java 1.8

Both DSpace 6.3 and the Duo extensions are dependent on a Java 1.7 or 1.8 installation.

### Maven 3+

DSpace requires Maven 3+ to compile - it may work on earlier versions, but is not recommended.  The Duo extensions will compile under Maven 3+.

### BagIt

This library depends on the related BagIt library, which must be downloaded and compiled as per the instructions here:

[https://github.com/nye-duo/BagItLibrary](https://github.com/nye-duo/BagItLibrary)

You can do this quickly with:

    git clone https://github.com/nye-duo/BagItLibrary.git
    cd BagItLibrary
    mvn clean package

To use this in the build you should, once you have successfully compiled the library, install it into your local maven repository

    mvn install

### IdService Client

In order to generate URNs for items in the repository using the National Library's API, we need to include the client library which will allow us to connect to it

It is bundled here for your convenience.  Install it into your local maven repository with:

    mvn install:install-file -Dfile=lib/idservice-client/idservice-client-3.0.jar -DpomFile=lib/idservice-client/pom.xml

Note if you wish to use an older version of the IdService you will need to modify the pom.xml for the right version, and
switch the jars in the command above.

### DSpace

It is designed to be installed into the Duo version of DSpace 6.3 here:

[https://github.com/nye-duo/DSpace/tree/duo63](https://github.com/nye-duo/DSpace/tree/duo63)

This can be obtained with the following commands:

    git clone https://github.com/nye-duo/DSpace.git
    git checkout duo63

This will be the source of your ultimate DSpace installation

Note that in previous versions of the Duo extensions, a separate installable library was included.  This is now no
longer required, and all Duo-related code is in this extended version of DSpace.


## Fresh Installation


**1/** Carry out a standard installation of DSpace, as per the DSpace install documentation at: https://wiki.lyrasis.org/display/DSDOC6x/Installing+DSpace

When we perform this installation, replace the standard DSpace codebase with the Duo customised codebase
described above [https://github.com/nye-duo/DSpace/tree/duo63](https://github.com/nye-duo/DSpace/tree/duo63).

During **Step 5** of the actual DSpace installation (**Initial Configuration (local.cfg)**), when you create your
`local.cfg` file from `local.cfg.EXAMPLE`, you must also append your `local.cfg` file with the values from `local.cfg.DUO`
and customise them as needed.

At that point, you may wish to go through the `*.cfg` files in the `config` and `config/modules` directories and 
override any other values which are relevant to your installation environment.  You can find detailed documentation about 
the configuration options here: [https://github.com/nye-duo/Duo-DSpace/blob/master/config/README.md](https://github.com/nye-duo/Duo-DSpace/blob/master/config/README.md)

There is no need to start tomcat at this point, we will do that once the rest of the installation below has completed

**2/** Configure the webapps for tomcat

If you have not done so as part of the DSpace install, be sure to point the tomcat webapps directory at the 
DSpace webapps directory.  For example:

This can be done with a symlink (if your tomcat will allow it):

    ln -s [dspace-live]/webapps [tomcat]/webapps

otherwise, you must copy the webapps directory into your tomcat working directory, as normal.

**3/** Install the custom Duo registries

Duo requires a number of custom metadata schemas to be present.  These can be imported with the following commands:

In the `[dspace]/bin` install directory:

```
./dspace registry-loader -metadata ../config/registries/cristin-metadata.xml
./dspace registry-loader -metadata ../config/registries/duo-metadata.xml 
./dspace registry-loader -metadata ../config/registries/fs-metadata.xml 
```

**4/** Start tomcat

    [tomcat]/bin/catalina.sh start

Once tomcat has started, you should be able to access your DSpace instance, at - for 
example: [http://localhost:8080/xmlui](http://localhost:8080/xmlui)

**5/** Set up the cron job for lifting embargoes, which will need to use the command:

	[dspace]/bin/dspace embargo-lifter

**6/** Set up the Cristin Workflow, as per the next section


## Setting up a Cristin Workflow

Once you have set up a Collection for harvesting from Cristin, you need to enable the correct workflow for it.  
To do this edit the file

	[dspace]/config/workflow.xml

And add a name-map reference in the heading section of the file, mapping your collection's handle to the 
"cristin" workflow, for example:

	<name-map collection="123456789/4404" workflow="cristin"/>

Then edit the file

    [dspace]/config/input-forms.xml

And add a name-map reference in the "form-map" section of the file, mapping your collection's handle to 
the "cristin" metadata form, for example:

    <name-map collection-handle="123456789/4404" form-name="cristin" />

For these changes to take effect, you will need to restart tomcat.

## Running the URN Generator

In order to register the items in the repository with the National Library's URN service, and to 
add the bitstream urls to the item metadata in DSpace, you should regularly run the URNGenerator

To generate the URNs for all items that do not have one, and to add the bitstream URLs 
to those items, run the command with only the -e argument (specifying the username of the user 
the operation should run as - recommended to be an administrator):

    [dspace]/bin/dspace dsrun no.uio.duo.URNGenerator -e [username]

In order to force the regeneration of all bitstream URLs you can run the command with the -f 
argument.  This will still generate URNs for all items that do not already have one:

    [dspace]/bin/dspace dsrun no.uio.duo.URNGenerator -f -e [username]

In order to force the regeneration of all item URLs and to update the URN registry with those 
URLs where they have changed since last time, use the -a option:

    [dspace]/bin/dspace dsrun no.uio.duo.URNGenerator -a -e [username]

The URN generator can be run on the whole archive, or it can be run on a single item as 
identified by its handle:

    [dspace]/bin/dspace dsrun no.uio.duo.URNGenerator -h 12345678/100 -a -e [username]
