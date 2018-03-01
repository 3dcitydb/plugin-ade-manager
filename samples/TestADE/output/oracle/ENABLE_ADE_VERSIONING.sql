-- This document was automatically created by the ADE-Manager tool of 3DCityDB (https://www.3dcitydb.org) on 2017-11-07 07:58:55 
-- ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ 
-- *********************************  Enable Versioning  *********************************** 
-- ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ 

exec DBMS_WM.EnableVersioning('test_BuildingU_to_address,test_BuildingUnit,test_EnergyPerformanceCer,test_Facilities,test_IndustrialBuilding,test_IndustrialBuildingPa,test_IndustrialBuildingRo,test_OtherConstruction,test_Other_to_thema_surfa,test_building,','VIEW_WO_OVERWRITE');
