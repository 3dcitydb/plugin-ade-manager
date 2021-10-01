Change Log
==========

### 2.0.0 - tbd

##### Breaking changes
* This version is implemented against the new Plugin API introduced with **version 5.0.0** of the
  [Importer/Exporter](https://github.com/3dcitydb/importer-exporter). It *cannot be used with previous versions*
  of the Importer/Exporter anymore.

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
  * Do not create SEQEUNCE for tables of sub data types.
* Fixed check of GML geometry property types by evaluating the XML namespace.
* Fixed parsing of complex types whose super type has empty contents.
* Create table node for CityGML standard classes only once.
* Fixed issues when another config file is loaded for the Importer/Exporter.

##### Miscellaneous
* This version works with version 4.3.x of the [3D City Database Importer/Exporter](https://github.com/3dcitydb/importer-exporter)