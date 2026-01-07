Change Log
==========

## 2.3.3 - 2026-01-07

### Miscellaneous
* Updated to [Importer/Exporter](https://github.com/3dcitydb/importer-exporter) version 5.5.3.

## 2.3.2 - 2025-04-30

### Fixes
* Fixed duplicated variable names in aggregation delete functions. [#26](https://github.com/3dcitydb/plugin-ade-manager/pull/26)

## 2.3.1 - 2025-02-28

### Fixes
* Fixed references to sequences in create scripts to be schema-aware (PostgreSQL). [3dcitydb/energy-ade-citydb#11](https://github.com/3dcitydb/energy-ade-citydb/issues/11) 
* Fixed creation of delete scripts when the same ADE is installed in multiple schemas (PostgreSQL). [3dcitydb/energy-ade-citydb#11](https://github.com/3dcitydb/energy-ade-citydb/issues/11)
* Fixed creation of envelope scripts to prevent setting the envelope of sub-features to `NULL`.

### Miscellaneous
* Updated to [Importer/Exporter](https://github.com/3dcitydb/importer-exporter) version 5.5.1.

## 2.3.0 - 2024-09-19

### Changes
* **Breaking:** Java 11 is now the minimum required version for using the ADE Manager Plugin.
* ADE tables requiring their own sequence are now created with an auto-increment statement for their ID column.
  [#7](https://github.com/3dcitydb/plugin-ade-manager/issues/7)
* ADE XSD schema errors are now reported to the user while processing the schema. [impexp #306](https://github.com/3dcitydb/importer-exporter/issues/306)
* Updated to [Importer/Exporter](https://github.com/3dcitydb/importer-exporter) version 5.5.0.

### Fixes
* ADE-specific delete functions are now correctly dropped when removing an ADE. [#19](https://github.com/3dcitydb/plugin-ade-manager/issues/19)
* Fixed mapping of ADE hook properties of boundary surface classes.
* Fixed mapping of ADE hook properties of TIN relief.

## 2.2.0 - 2022-12-15

* Updated to [Importer/Exporter](https://github.com/3dcitydb/importer-exporter) version 5.3.0.

## 2.1.0 - 2022-05-23

### Changes
* Use the new default view implementations of the Plugin API introduced with **version 5.2.0** of the
  [Importer/Exporter](https://github.com/3dcitydb/importer-exporter).

## 2.0.1 - 2022-02-07

### Changes
* Removed the hardcoded hyperlink of the online documentation from the plugin description.
* Removed the unnecessary old version of log4j library which contains security vulnerabilities.

## 2.0.0 - 2021-10-08

### Breaking changes
* This version is implemented against the new Plugin API introduced with **version 5.0.0** of the
  [Importer/Exporter](https://github.com/3dcitydb/importer-exporter). It *cannot be used with previous versions*
  of the Importer/Exporter anymore.
* The generated PostgreSQL-based ADE database schemas now use 64-bit bigint as data type instead of 32-bit integer 
  for all primary key columns and therefore *cannot be used with previous versions (< 4.2.0)* of the 3DCityDB anymore.

### Fixes
* Fixed logging of some error messages.

## 1.2.0 - 2021-04-28

### Additions
* Added support for processing inline `<xs:choice>` elements.
* Added possibility to show an ADE info dialog for registered CityGML ADEs.
* Update SRID of geometry column after registering CityGML ADEs.
* Updated the graphical user interface to the new look&feel of the Importer/Exporter.
* Completely updated user manual at https://3dcitydb-docs.readthedocs.io/en/release-v4.3.0/

### Fixes
* Fixed graph transformation rules:
  * Create OBJECTCLASS_ID column for merged table mapped from complex <DataType>.
  * Do not create SEQUENCE for tables of sub data types.
* Fixed check of GML geometry property types by evaluating the XML namespace.
* Fixed parsing of complex types whose super type has empty contents.
* Create table node for CityGML standard classes only once.
* Fixed issues when another config file is loaded for the Importer/Exporter.

### Miscellaneous
* This version works with version 4.3.x of the [3D City Database Importer/Exporter](https://github.com/3dcitydb/importer-exporter)