Change Log
==========

### 2.2.0 - 2022-12-15

* Updated to [Importer/Exporter](https://github.com/3dcitydb/importer-exporter) version 5.3.0.

### 2.1.0 - 2022-05-23

#### Changes
* Use the new default view implementations of the Plugin API introduced with **version 5.2.0** of the
  [Importer/Exporter](https://github.com/3dcitydb/importer-exporter).

### 2.0.1 - 2022-02-07

##### Changes
* Removed the hardcoded hyperlink of the online documentation from the plugin description.
* Removed the unnecessary old version of log4j library which contains security vulnerabilities.

### 2.0.0 - 2021-10-08

##### Breaking changes
* This version is implemented against the new Plugin API introduced with **version 5.0.0** of the
  [Importer/Exporter](https://github.com/3dcitydb/importer-exporter). It *cannot be used with previous versions*
  of the Importer/Exporter anymore.
* The generated PostgreSQL-based ADE database schemas now use 64-bit bigint as data type instead of 32-bit integer 
  for all primary key columns and therefore *cannot be used with previous versions (< 4.2.0)* of the 3DCityDB anymore.

##### Fixes
* Fixed logging of some error messages

### 1.2.0 - 2021-04-28

##### Additions
* Added support for processing inline `<xs:choice>` elements.
* Added possibility to show an ADE info dialog for registered CityGML ADEs.
* Update SRID of geometry column after registering CityGML ADEs.
* Updated the graphical user interface to the new look&feel of the Importer/Exporter.
* Completely updated user manual at https://3dcitydb-docs.readthedocs.io/en/release-v4.3.0/

##### Fixes
* Fixed graph transformation rules:
  * Create OBJECTCLASS_ID column for merged table mapped from complex <DataType>.
  * Do not create SEQUENCE for tables of sub data types.
* Fixed check of GML geometry property types by evaluating the XML namespace.
* Fixed parsing of complex types whose super type has empty contents.
* Create table node for CityGML standard classes only once.
* Fixed issues when another config file is loaded for the Importer/Exporter.

##### Miscellaneous
* This version works with version 4.3.x of the [3D City Database Importer/Exporter](https://github.com/3dcitydb/importer-exporter)