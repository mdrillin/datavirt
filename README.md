# Data Virtualization Web App

## Summary

The Data Virtualization web application is for use with RedHat JBoss DataVirtualization 6.  You can manager drivers, sources and virtual databases (VDBs) on your DataVirtualization instance.  You can also run test queries against your jdbc sources and VDBs.

This application is provided as a Tech Preview and is therefore not supported.  It is intended as a simple interface to management and testing of dynamic Virtual Databases.  For a more a more comprehensive application, you can use Teiid Designer to build and test VDBs.

For more information on Teiid Designer, including getting started guides, reference guides, and downloadable binaries, visit the project's website at [http://www.jboss.org/teiiddesigner/](http://www.jboss.org/teiiddesigner/)
or follow us on our [blog](http://teiid.blogspot.com/) or on [Twitter](https://twitter.com/teiiddesigner). Or hop into our [IRC chat room](http://www.jboss.org/teiiddesigner/chat)
and talk our community of contributors and users.

## Building the Application

Clone this repo to your system, then build the application war 

$mvn clean install -s settings.xml

This will generate the .war file into the target directory, which you can then drop into your Data Virtualization deployments directory.
Note : The settings.xml file is included, but you will need to modifiy it.  First, install the EAP 6.1 repo locally - then modify settings.xml to reference it - (see Dependencies section)

## Dependencies

The pom.xml provided has dependencies to JBoss EAP 6.1 and Teiid 8.4.1 currently.

 - EAP 6.1 - the maven repo for EAP 6.1 Final can be downloaded from http://www.jboss.org/products/eap.html and installed into your local maven repo
 - Teiid   - the public maven repos for Teiid are located at https://repository.jboss.org/nexus/content/groups/public/org/jboss/teiid/

## Access the application

Once deployed you may access the application in your browser at:

http://[host]/datavirtualization

for example: 

http://localhost:8080/datavirtualization

