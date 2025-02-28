ADE Manager Plugin
======================================

The ADE Manager is a plugin for the [3D City Database Importer/Exporter](https://github.com/3dcitydb/importer-exporter)
and allows to dynamically extend a 3D City Database (3DCityDB) instance for high-performance storage and management of
CityGML Application Domain Extensions (ADE). It is implemented based on the open-source
[graph transformation engine (AGG)](http://www.user.tu-berlin.de/o.runge/agg/) for realizing the automatic
transformation of the object-oriented data model from a given ADE's application schema (XSD) to a compact relational
database schema (including tables, indexes, and constrains etc.). In addition, an XML-based schema-mapping file can
also be automatically generated which contains the relevant meta-information about the derived database schema as well
as the explicit mapping relationships between the two schemas and allows developers to implement applications for
managing and processing the ADE data contents stored in a 3DCityDB instance.

Transformation Workflow
-------
<p align="center">
<img src="resources/figure/transformation_workflow.jpg" width="800" />
</p>

License
-------
The ADE Manager Plugin is licensed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).
See the `LICENSE` file for more details.

Latest release
--------------
The latest stable release of the ADE Manager Plugin is 2.3.1.

Download the software [here](https://github.com/3dcitydb/plugin-ade-manager/releases/download/v2.3.1/plugin-ade-manager-2.3.1.zip).
Previous releases are available from the [releases section](https://github.com/3dcitydb/plugin-ade-manager/releases).

Installation
------------
The ADE Manager Plugin is shipped with the [3D City Database Suite installer](https://github.com/3dcitydb/3dcitydb-suite/releases).
When running the installer, a setup wizard will let you choose to install the plugin together with the required
version of the Importer/Exporter tool. This is the recommended and easiest way to install the plugin.

Alternatively, you may download the software from the [releases section](https://github.com/3dcitydb/plugin-ade-manager/releases)
(or build it from source) and unzip it into the `plugins` folder within the installation folder of the
Importer/Exporter. Please note that in this case you have to make sure yourself that the plugin version you
download can be used together with your installation of the Importer/Exporter. After a restart of the
Importer/Exporter, the ADE Manager will be ready to use.

System requirements
-------------------
Each version of the ADE Manager plugin is built against a specific version of the Importer/Exporter.
Please check the release notes of the version you want to use for more information. 

Please note that the ADE Manager Plugin may not work with Java 17 or later versions.

Documentation
-------------
A complete and comprehensive user manual on the ADE Manager Plugin is available
[online](https://3dcitydb-docs.readthedocs.io/en/latest/plugins/ade-manager).

Contributing
------------
* To file bugs found in the software create a GitHub issue.
* To contribute code for fixing filed issues create a pull request with the issue id.
* To propose a new feature create a GitHub issue and open a discussion.

Building
--------
The ADE Manager plugin uses [Gradle](https://gradle.org/) as build system. To build the plugin from source,
clone the repository to your local machine and run the following command from the root of the repository.

    > gradlew installDist
    
The build process will produce the plugin software package under `build/install`. Simply copy the contents of this
folder into the `plugins` folder of your Importer/Exporter installation to use the plugin.

Developers
----------
The ADE Manager plugin has been developed by and with the support from the following cooperation partners:

* [Chair of Geoinformatics, Technical University of Munich](https://www.gis.bgu.tum.de/)
* [Virtual City Systems, Berlin](https://vc.systems/)
* [M.O.S.S. Computer Grafik Systeme GmbH, Taufkirchen](http://www.moss.de/)

More information
----------------
[OGC CityGML](https://www.ogc.org/standard/citygml/) is an open data model and XML-based format for the
storage and exchange of semantic 3D city models. It is an application schema for the
[Geography Markup Language version 3.1.1 (GML3)](https://www.ogc.org/standard/gml/), the extensible
international standard for spatial data exchange issued by the Open Geospatial Consortium (OGC) and the ISO TC211.
The aim of the development of CityGML is to reach a common definition of the basic entities, attributes,
and relations of a 3D city model. Particularly, CityGML provides an extension mechanism called “Application 
Domain Extension (ADE)” which allows third parties to dynamically extend the existing CityGML data models with
application specific extensions for facilitating the application fields e.g. energy demand calculations,
utility network analysis, facility management, noise propagation simulations etc. 

CityGML is an international OGC standard and can be used free of charge.