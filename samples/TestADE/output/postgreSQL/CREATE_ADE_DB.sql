-- This document was automatically created by the ADE-Manager tool of 3DCityDB (https://www.3dcitydb.org) on 2017-11-07 07:58:55 
-- ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ 
-- ***********************************  Create tables ************************************* 
-- ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ 
-- -------------------------------------------------------------------- 
-- test_BuildingU_to_address 
-- -------------------------------------------------------------------- 
CREATE TABLE test_BuildingU_to_address
(
    BuildingUnit_ID INTEGER NOT NULL,
    address_ID INTEGER NOT NULL,
    PRIMARY KEY (BuildingUnit_ID, address_ID)
);

-- -------------------------------------------------------------------- 
-- test_BuildingUnit 
-- -------------------------------------------------------------------- 
CREATE TABLE test_BuildingUnit
(
    ID INTEGER NOT NULL,
    OBJECTCLASS_ID INTEGER,
    building_buildingUnit_ID INTEGER,
    BuildingUnit_Parent_ID INTEGER,
    BuildingUnit_Root_ID INTEGER,
    lod2MultiCurve geometry(GEOMETRYZ),
    lod3MultiCurve geometry(GEOMETRYZ),
    lod4MultiCurve geometry(GEOMETRYZ),
    class_codespace VARCHAR(1000),
    class VARCHAR(1000),
    usage_codespace VARCHAR(1000),
    usage VARCHAR(1000),
    function_codespace VARCHAR(1000),
    function VARCHAR(1000),
    lod1MultiSurface_ID INTEGER,
    lod2MultiSurface_ID INTEGER,
    lod3MultiSurface_ID INTEGER,
    lod4MultiSurface_ID INTEGER,
    lod1Solid_ID INTEGER,
    lod2Solid_ID INTEGER,
    lod3Solid_ID INTEGER,
    lod4Solid_ID INTEGER,
    PRIMARY KEY (ID)
);

-- -------------------------------------------------------------------- 
-- test_EnergyPerformanceCer 
-- -------------------------------------------------------------------- 
CREATE TABLE test_EnergyPerformanceCer
(
    ID INTEGER NOT NULL,
    BuildingUnit_energyPerfor_ID INTEGER,
    certificationName VARCHAR(1000),
    certificationid VARCHAR(1000),
    PRIMARY KEY (ID)
);

-- -------------------------------------------------------------------- 
-- test_Facilities 
-- -------------------------------------------------------------------- 
CREATE TABLE test_Facilities
(
    ID INTEGER NOT NULL,
    OBJECTCLASS_ID INTEGER,
    BuildingUnit_equippedWith_ID INTEGER,
    totalValue_uom VARCHAR(1000),
    totalValue NUMERIC,
    PRIMARY KEY (ID)
);

-- -------------------------------------------------------------------- 
-- test_IndustrialBuilding 
-- -------------------------------------------------------------------- 
CREATE TABLE test_IndustrialBuilding
(
    ID INTEGER NOT NULL,
    remark VARCHAR(1000),
    PRIMARY KEY (ID)
);

-- -------------------------------------------------------------------- 
-- test_IndustrialBuildingPa 
-- -------------------------------------------------------------------- 
CREATE TABLE test_IndustrialBuildingPa
(
    ID INTEGER NOT NULL,
    remark VARCHAR(1000),
    PRIMARY KEY (ID)
);

-- -------------------------------------------------------------------- 
-- test_IndustrialBuildingRo 
-- -------------------------------------------------------------------- 
CREATE TABLE test_IndustrialBuildingRo
(
    ID INTEGER NOT NULL,
    remark VARCHAR(1000),
    PRIMARY KEY (ID)
);

-- -------------------------------------------------------------------- 
-- test_OtherConstruction 
-- -------------------------------------------------------------------- 
CREATE TABLE test_OtherConstruction
(
    ID INTEGER NOT NULL,
    PRIMARY KEY (ID)
);

-- -------------------------------------------------------------------- 
-- test_Other_to_thema_surfa 
-- -------------------------------------------------------------------- 
CREATE TABLE test_Other_to_thema_surfa
(
    OtherConstruction_ID INTEGER NOT NULL,
    thematic_surface_ID INTEGER NOT NULL,
    PRIMARY KEY (OtherConstruction_ID, thematic_surface_ID)
);

-- -------------------------------------------------------------------- 
-- test_building 
-- -------------------------------------------------------------------- 
CREATE TABLE test_building
(
    ID INTEGER NOT NULL,
    EnergyPerforma_certification VARCHAR(1000),
    EnergyPerform_certificatio_1 VARCHAR(1000),
    floorArea_uom VARCHAR(1000),
    floorArea NUMERIC,
    ownerName VARCHAR(1000),
    PRIMARY KEY (ID)
);

-- ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ 
-- *********************************  Create foreign keys  ******************************** 
-- ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ 
-- -------------------------------------------------------------------- 
-- test_BuildingU_to_address 
-- -------------------------------------------------------------------- 
ALTER TABLE test_BuildingU_to_address
    ADD CONSTRAINT test_Buildin_to_addres_FK1 FOREIGN KEY (BuildingUnit_ID) REFERENCES test_BuildingUnit (ID);

ALTER TABLE test_BuildingU_to_address
    ADD CONSTRAINT test_Buildin_to_addres_FK2 FOREIGN KEY (address_ID) REFERENCES address (ID);

-- -------------------------------------------------------------------- 
-- test_BuildingUnit 
-- -------------------------------------------------------------------- 
ALTER TABLE test_BuildingUnit
    ADD CONSTRAINT test_BuildingU_Objectcl_FK FOREIGN KEY (OBJECTCLASS_ID) REFERENCES objectclass (ID);

ALTER TABLE test_BuildingUnit
    ADD CONSTRAINT test_BuildingUnit_FK FOREIGN KEY (ID) REFERENCES cityobject (ID);

ALTER TABLE test_BuildingUnit
    ADD CONSTRAINT test_Buildi_build_build_FK FOREIGN KEY (building_buildingUnit_ID) REFERENCES test_building (ID);

ALTER TABLE test_BuildingUnit
    ADD CONSTRAINT test_BuildingUni_Parent_FK FOREIGN KEY (BuildingUnit_Parent_ID) REFERENCES test_BuildingUnit (ID);

ALTER TABLE test_BuildingUnit
    ADD CONSTRAINT test_BuildingUnit_Root_FK FOREIGN KEY (BuildingUnit_Root_ID) REFERENCES test_BuildingUnit (ID);

ALTER TABLE test_BuildingUnit
    ADD CONSTRAINT test_BuildingU_lod1Mult_FK FOREIGN KEY (lod1MultiSurface_ID) REFERENCES SURFACE_GEOMETRY (ID);

ALTER TABLE test_BuildingUnit
    ADD CONSTRAINT test_BuildingU_lod2Mult_FK FOREIGN KEY (lod2MultiSurface_ID) REFERENCES SURFACE_GEOMETRY (ID);

ALTER TABLE test_BuildingUnit
    ADD CONSTRAINT test_BuildingU_lod3Mult_FK FOREIGN KEY (lod3MultiSurface_ID) REFERENCES SURFACE_GEOMETRY (ID);

ALTER TABLE test_BuildingUnit
    ADD CONSTRAINT test_BuildingU_lod4Mult_FK FOREIGN KEY (lod4MultiSurface_ID) REFERENCES SURFACE_GEOMETRY (ID);

ALTER TABLE test_BuildingUnit
    ADD CONSTRAINT test_BuildingU_lod1Soli_FK FOREIGN KEY (lod1Solid_ID) REFERENCES SURFACE_GEOMETRY (ID);

ALTER TABLE test_BuildingUnit
    ADD CONSTRAINT test_BuildingU_lod2Soli_FK FOREIGN KEY (lod2Solid_ID) REFERENCES SURFACE_GEOMETRY (ID);

ALTER TABLE test_BuildingUnit
    ADD CONSTRAINT test_BuildingU_lod3Soli_FK FOREIGN KEY (lod3Solid_ID) REFERENCES SURFACE_GEOMETRY (ID);

ALTER TABLE test_BuildingUnit
    ADD CONSTRAINT test_BuildingU_lod4Soli_FK FOREIGN KEY (lod4Solid_ID) REFERENCES SURFACE_GEOMETRY (ID);

-- -------------------------------------------------------------------- 
-- test_EnergyPerformanceCer 
-- -------------------------------------------------------------------- 
ALTER TABLE test_EnergyPerformanceCer
    ADD CONSTRAINT test_Energy_Build_energ_FK FOREIGN KEY (BuildingUnit_energyPerfor_ID) REFERENCES test_BuildingUnit (ID);

-- -------------------------------------------------------------------- 
-- test_Facilities 
-- -------------------------------------------------------------------- 
ALTER TABLE test_Facilities
    ADD CONSTRAINT test_Facilitie_Objectcl_FK FOREIGN KEY (OBJECTCLASS_ID) REFERENCES objectclass (ID);

ALTER TABLE test_Facilities
    ADD CONSTRAINT test_Facilities_FK FOREIGN KEY (ID) REFERENCES cityobject (ID);

ALTER TABLE test_Facilities
    ADD CONSTRAINT test_Facili_Build_equip_FK FOREIGN KEY (BuildingUnit_equippedWith_ID) REFERENCES test_BuildingUnit (ID);

-- -------------------------------------------------------------------- 
-- test_IndustrialBuilding 
-- -------------------------------------------------------------------- 
ALTER TABLE test_IndustrialBuilding
    ADD CONSTRAINT test_IndustrialBuilding_FK FOREIGN KEY (ID) REFERENCES building (ID);

-- -------------------------------------------------------------------- 
-- test_IndustrialBuildingPa 
-- -------------------------------------------------------------------- 
ALTER TABLE test_IndustrialBuildingPa
    ADD CONSTRAINT test_IndustrialBuildi_FK_1 FOREIGN KEY (ID) REFERENCES building (ID);

-- -------------------------------------------------------------------- 
-- test_IndustrialBuildingRo 
-- -------------------------------------------------------------------- 
ALTER TABLE test_IndustrialBuildingRo
    ADD CONSTRAINT test_IndustrialBuildi_FK_2 FOREIGN KEY (ID) REFERENCES thematic_surface (ID);

-- -------------------------------------------------------------------- 
-- test_OtherConstruction 
-- -------------------------------------------------------------------- 
ALTER TABLE test_OtherConstruction
    ADD CONSTRAINT test_OtherConstruction_FK FOREIGN KEY (ID) REFERENCES cityobject (ID);

-- -------------------------------------------------------------------- 
-- test_Other_to_thema_surfa 
-- -------------------------------------------------------------------- 
ALTER TABLE test_Other_to_thema_surfa
    ADD CONSTRAINT test_Othe_to_them_surf_FK1 FOREIGN KEY (OtherConstruction_ID) REFERENCES test_OtherConstruction (ID);

ALTER TABLE test_Other_to_thema_surfa
    ADD CONSTRAINT test_Othe_to_them_surf_FK2 FOREIGN KEY (thematic_surface_ID) REFERENCES thematic_surface (ID);

-- -------------------------------------------------------------------- 
-- test_building 
-- -------------------------------------------------------------------- 
ALTER TABLE test_building
    ADD CONSTRAINT test_building_FK FOREIGN KEY (ID) REFERENCES building (ID);

-- ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ 
-- *********************************  Create Indexes  ************************************* 
-- ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ 
-- -------------------------------------------------------------------- 
-- test_BuildingUnit 
-- -------------------------------------------------------------------- 
CREATE INDEX test_Building_Objectcl_FKX ON test_BuildingUnit
    USING btree
    (
      OBJECTCLASS_ID ASC NULLS LAST
    )   WITH (FILLFACTOR = 90);

CREATE INDEX test_Build_build_build_FKX ON test_BuildingUnit
    USING btree
    (
      building_buildingUnit_ID ASC NULLS LAST
    )   WITH (FILLFACTOR = 90);

CREATE INDEX test_BuildingUn_Parent_FKX ON test_BuildingUnit
    USING btree
    (
      BuildingUnit_Parent_ID ASC NULLS LAST
    )   WITH (FILLFACTOR = 90);

CREATE INDEX test_BuildingUnit_Root_FKX ON test_BuildingUnit
    USING btree
    (
      BuildingUnit_Root_ID ASC NULLS LAST
    )   WITH (FILLFACTOR = 90);

CREATE INDEX test_Building_lod2Mult_SPX ON test_BuildingUnit
    USING gist
    (
      lod2MultiCurve
    );

CREATE INDEX test_Building_lod3Mult_SPX ON test_BuildingUnit
    USING gist
    (
      lod3MultiCurve
    );

CREATE INDEX test_Building_lod4Mult_SPX ON test_BuildingUnit
    USING gist
    (
      lod4MultiCurve
    );

CREATE INDEX test_Building_lod1Mult_FKX ON test_BuildingUnit
    USING btree
    (
      lod1MultiSurface_ID ASC NULLS LAST
    )   WITH (FILLFACTOR = 90);

CREATE INDEX test_Building_lod2Mult_FKX ON test_BuildingUnit
    USING btree
    (
      lod2MultiSurface_ID ASC NULLS LAST
    )   WITH (FILLFACTOR = 90);

CREATE INDEX test_Building_lod3Mult_FKX ON test_BuildingUnit
    USING btree
    (
      lod3MultiSurface_ID ASC NULLS LAST
    )   WITH (FILLFACTOR = 90);

CREATE INDEX test_Building_lod4Mult_FKX ON test_BuildingUnit
    USING btree
    (
      lod4MultiSurface_ID ASC NULLS LAST
    )   WITH (FILLFACTOR = 90);

CREATE INDEX test_Building_lod1Soli_FKX ON test_BuildingUnit
    USING btree
    (
      lod1Solid_ID ASC NULLS LAST
    )   WITH (FILLFACTOR = 90);

CREATE INDEX test_Building_lod2Soli_FKX ON test_BuildingUnit
    USING btree
    (
      lod2Solid_ID ASC NULLS LAST
    )   WITH (FILLFACTOR = 90);

CREATE INDEX test_Building_lod3Soli_FKX ON test_BuildingUnit
    USING btree
    (
      lod3Solid_ID ASC NULLS LAST
    )   WITH (FILLFACTOR = 90);

CREATE INDEX test_Building_lod4Soli_FKX ON test_BuildingUnit
    USING btree
    (
      lod4Solid_ID ASC NULLS LAST
    )   WITH (FILLFACTOR = 90);

-- -------------------------------------------------------------------- 
-- test_EnergyPerformanceCer 
-- -------------------------------------------------------------------- 
CREATE INDEX test_Energ_Build_energ_FKX ON test_EnergyPerformanceCer
    USING btree
    (
      BuildingUnit_energyPerfor_ID ASC NULLS LAST
    )   WITH (FILLFACTOR = 90);

-- -------------------------------------------------------------------- 
-- test_Facilities 
-- -------------------------------------------------------------------- 
CREATE INDEX test_Faciliti_Objectcl_FKX ON test_Facilities
    USING btree
    (
      OBJECTCLASS_ID ASC NULLS LAST
    )   WITH (FILLFACTOR = 90);

CREATE INDEX test_Facil_Build_equip_FKX ON test_Facilities
    USING btree
    (
      BuildingUnit_equippedWith_ID ASC NULLS LAST
    )   WITH (FILLFACTOR = 90);

-- ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ 
-- *********************************  Create Sequences  *********************************** 
-- ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ 

CREATE SEQUENCE test_EnergyPerformanc_SEQ
INCREMENT BY 1
MINVALUE 0
MAXVALUE 2147483647
START WITH 1
CACHE 1
NO CYCLE
OWNED BY NONE;


