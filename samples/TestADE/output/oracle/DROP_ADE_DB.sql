-- This document was automatically created by the ADE-Manager tool of 3DCityDB (https://www.3dcitydb.org) on 2017-11-07 07:58:55 
-- ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ 
-- ***********************************  Drop foreign keys ********************************* 
-- ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ 
-- -------------------------------------------------------------------- 
-- test_BuildingU_to_address 
-- -------------------------------------------------------------------- 
ALTER TABLE test_BuildingU_to_address
    DROP CONSTRAINT test_Buildin_to_addres_FK1;

ALTER TABLE test_BuildingU_to_address
    DROP CONSTRAINT test_Buildin_to_addres_FK2;

-- -------------------------------------------------------------------- 
-- test_BuildingUnit 
-- -------------------------------------------------------------------- 
ALTER TABLE test_BuildingUnit
    DROP CONSTRAINT test_BuildingU_Objectcl_FK;

ALTER TABLE test_BuildingUnit
    DROP CONSTRAINT test_BuildingUnit_FK;

ALTER TABLE test_BuildingUnit
    DROP CONSTRAINT test_Buildi_build_build_FK;

ALTER TABLE test_BuildingUnit
    DROP CONSTRAINT test_BuildingUni_Parent_FK;

ALTER TABLE test_BuildingUnit
    DROP CONSTRAINT test_BuildingUnit_Root_FK;

ALTER TABLE test_BuildingUnit
    DROP CONSTRAINT test_BuildingU_lod1Mult_FK;

ALTER TABLE test_BuildingUnit
    DROP CONSTRAINT test_BuildingU_lod2Mult_FK;

ALTER TABLE test_BuildingUnit
    DROP CONSTRAINT test_BuildingU_lod3Mult_FK;

ALTER TABLE test_BuildingUnit
    DROP CONSTRAINT test_BuildingU_lod4Mult_FK;

ALTER TABLE test_BuildingUnit
    DROP CONSTRAINT test_BuildingU_lod1Soli_FK;

ALTER TABLE test_BuildingUnit
    DROP CONSTRAINT test_BuildingU_lod2Soli_FK;

ALTER TABLE test_BuildingUnit
    DROP CONSTRAINT test_BuildingU_lod3Soli_FK;

ALTER TABLE test_BuildingUnit
    DROP CONSTRAINT test_BuildingU_lod4Soli_FK;

-- -------------------------------------------------------------------- 
-- test_EnergyPerformanceCer 
-- -------------------------------------------------------------------- 
ALTER TABLE test_EnergyPerformanceCer
    DROP CONSTRAINT test_Energy_Build_energ_FK;

-- -------------------------------------------------------------------- 
-- test_Facilities 
-- -------------------------------------------------------------------- 
ALTER TABLE test_Facilities
    DROP CONSTRAINT test_Facilitie_Objectcl_FK;

ALTER TABLE test_Facilities
    DROP CONSTRAINT test_Facilities_FK;

ALTER TABLE test_Facilities
    DROP CONSTRAINT test_Facili_Build_equip_FK;

-- -------------------------------------------------------------------- 
-- test_IndustrialBuilding 
-- -------------------------------------------------------------------- 
ALTER TABLE test_IndustrialBuilding
    DROP CONSTRAINT test_IndustrialBuilding_FK;

-- -------------------------------------------------------------------- 
-- test_IndustrialBuildingPa 
-- -------------------------------------------------------------------- 
ALTER TABLE test_IndustrialBuildingPa
    DROP CONSTRAINT test_IndustrialBuildi_FK_1;

-- -------------------------------------------------------------------- 
-- test_IndustrialBuildingRo 
-- -------------------------------------------------------------------- 
ALTER TABLE test_IndustrialBuildingRo
    DROP CONSTRAINT test_IndustrialBuildi_FK_2;

-- -------------------------------------------------------------------- 
-- test_OtherConstruction 
-- -------------------------------------------------------------------- 
ALTER TABLE test_OtherConstruction
    DROP CONSTRAINT test_OtherConstruction_FK;

-- -------------------------------------------------------------------- 
-- test_Other_to_thema_surfa 
-- -------------------------------------------------------------------- 
ALTER TABLE test_Other_to_thema_surfa
    DROP CONSTRAINT test_Othe_to_them_surf_FK1;

ALTER TABLE test_Other_to_thema_surfa
    DROP CONSTRAINT test_Othe_to_them_surf_FK2;

-- -------------------------------------------------------------------- 
-- test_building 
-- -------------------------------------------------------------------- 
ALTER TABLE test_building
    DROP CONSTRAINT test_building_FK;

-- ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ 
-- ***********************************  Drop tables  ************************************** 
-- ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ 
-- -------------------------------------------------------------------- 
-- test_BuildingU_to_address 
-- -------------------------------------------------------------------- 
DROP TABLE test_BuildingU_to_address;

-- -------------------------------------------------------------------- 
-- test_BuildingUnit 
-- -------------------------------------------------------------------- 
DROP TABLE test_BuildingUnit;

-- -------------------------------------------------------------------- 
-- test_EnergyPerformanceCer 
-- -------------------------------------------------------------------- 
DROP TABLE test_EnergyPerformanceCer;

-- -------------------------------------------------------------------- 
-- test_Facilities 
-- -------------------------------------------------------------------- 
DROP TABLE test_Facilities;

-- -------------------------------------------------------------------- 
-- test_IndustrialBuilding 
-- -------------------------------------------------------------------- 
DROP TABLE test_IndustrialBuilding;

-- -------------------------------------------------------------------- 
-- test_IndustrialBuildingPa 
-- -------------------------------------------------------------------- 
DROP TABLE test_IndustrialBuildingPa;

-- -------------------------------------------------------------------- 
-- test_IndustrialBuildingRo 
-- -------------------------------------------------------------------- 
DROP TABLE test_IndustrialBuildingRo;

-- -------------------------------------------------------------------- 
-- test_OtherConstruction 
-- -------------------------------------------------------------------- 
DROP TABLE test_OtherConstruction;

-- -------------------------------------------------------------------- 
-- test_Other_to_thema_surfa 
-- -------------------------------------------------------------------- 
DROP TABLE test_Other_to_thema_surfa;

-- -------------------------------------------------------------------- 
-- test_building 
-- -------------------------------------------------------------------- 
DROP TABLE test_building;

-- ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ 
-- *********************************  Drop Sequences  ************************************* 
-- ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ 

DROP SEQUENCE test_EnergyPerformanc_SEQ;

PURGE RECYCLEBIN;
