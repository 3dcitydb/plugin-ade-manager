/*
 * 3D City Database - The Open Source CityGML Database
 * https://www.3dcitydb.org/
 *
 * Copyright 2013 - 2021
 * Chair of Geoinformatics
 * Technical University of Munich, Germany
 * https://www.lrg.tum.de/gis/
 *
 * The 3D City Database is jointly developed with the following
 * cooperation partners:
 *
 * Virtual City Systems, Berlin <https://vc.systems/>
 * M.O.S.S. Computer Grafik Systeme GmbH, Taufkirchen <http://www.moss.de/>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.citydb.plugins.ade_manager.transformation.graph;

import com.sun.xml.xsom.*;
import org.citygml4j.xml.schema.ElementDecl;
import org.citygml4j.xml.schema.Schema;

import javax.xml.namespace.QName;

public class ADEschemaElement extends ElementDecl {

	private Schema schema;
	private XSElementDecl element;
	
	public ADEschemaElement(XSElementDecl decl, Schema schema) {
		super(decl, schema);
		this.element = decl;
		this.schema = schema;
	}	
	
	public boolean isCityGMLClass() {
		XSType xsType = element.getType();
		return ADEschemaHelper.CityGML_Namespaces.contains(xsType.getTargetNamespace());
	}
	
	public boolean isGMLreferenceProperty() {
		XSType xsType = element.getType();
		String namespace = xsType.getTargetNamespace();
		String typeName = xsType.getName();
		if (typeName != null) {
			if (typeName.equalsIgnoreCase("ReferenceType") && namespace.indexOf("http://www.opengis.net/gml") > -1) {
				return true;
			} 
		}	
		return false;
	}
	
	public boolean isCityGMLnonPorperty() {
		XSType xsType = element.getType();
		String typeName = xsType.getName();
		if (typeName != null) {
			if (ADEschemaHelper.CityDB_Tables.containsKey(new QName(xsType.getTargetNamespace(), xsType.getName()))) {
				return true;
			}
		}	
		
		return false;
	}
	
	public boolean isImplicitGeometryProperty() {
		XSType xsType = element.getType();
		String namespace = xsType.getTargetNamespace();
		String typeName = xsType.getName();
		if (typeName != null) {
			if (typeName.equalsIgnoreCase("ImplicitRepresentationPropertyType") && (namespace.equalsIgnoreCase("http://www.opengis.net/citygml/1.0") || namespace.equalsIgnoreCase("http://www.opengis.net/citygml/2.0"))) {
				return true;
			}
		}
		
		return false;
	}
	
	public boolean isFeatureOrObjectProperty() {
		XSType xsType = element.getType();
		String localNamespace = xsType.getTargetNamespace();
		if (xsType.getName() != null && xsType.isComplexType() && (schema.getNamespaceURI().equalsIgnoreCase(localNamespace)
				|| ADEschemaHelper.CityGML_Namespaces.contains(localNamespace))) {
			XSContentType xsContentType = xsType.asComplexType().getContentType();
		    XSParticle particle = xsContentType.asParticle();       
		    if (particle != null) {
		        XSTerm term = particle.getTerm();
		        if (term.isModelGroup()) {
		            XSModelGroup xsModelGroup = term.asModelGroup();
		            XSParticle[] particles = xsModelGroup.getChildren();
		            for (XSParticle p : particles) {
		                XSTerm pterm = p.getTerm();
		                if (pterm.isElementDecl()) { 
		                    XSElementDecl childElementDecl = (XSElementDecl) pterm;
		                    ADEschemaElement elementDecl = new ADEschemaElement(childElementDecl, schema);
		                    if (elementDecl.isAbstractGML()||elementDecl.isFeature()||elementDecl.isCityObject()) {
		                    	return true;
		                    }
		                }
		            }
		        }
		    }
		}			
		return false;
	}
	
	public boolean isUnionProperty() {
		XSType xsType = element.getType();
		String localNamespace = xsType.getTargetNamespace();
		if (xsType.getName() != null && xsType.isComplexType() && (schema.getNamespaceURI().equalsIgnoreCase(localNamespace)
				|| ADEschemaHelper.CityGML_Namespaces.contains(localNamespace))) {
			XSContentType xsContentType = xsType.asComplexType().getContentType();
		    XSParticle particle = xsContentType.asParticle();       
		    if (particle != null) {
		        XSTerm term = particle.getTerm();
		        if (term.isModelGroup()) {
		            XSModelGroup xsModelGroup = term.asModelGroup();
		            XSParticle[] particles = xsModelGroup.getChildren();
		            for (XSParticle p : particles) {
		                XSTerm pterm = p.getTerm();
		                if (pterm.isElementDecl()) { 
		                    XSElementDecl childElementDecl = (XSElementDecl) pterm;
		                    ADEschemaElement elementDecl = new ADEschemaElement(childElementDecl, schema);
		                    if (elementDecl.isUnion()) {
		                    	return true;
		                    }
		                }
		            }
		        }
		    }
		}		
		return false;
	}
	
	public boolean isComplexDataProperty() {
		XSType xsType = element.getType();
		String localNamespace = xsType.getTargetNamespace();
		if (xsType.getName() != null && xsType.isComplexType() && (schema.getNamespaceURI().equalsIgnoreCase(localNamespace) || ADEschemaHelper.CityGML_Namespaces.contains(localNamespace))) {
			XSContentType xsContentType = xsType.asComplexType().getContentType();
		    XSParticle particle = xsContentType.asParticle();       
		    if (particle != null) {
		        XSTerm term = particle.getTerm();
		        if (term.isModelGroup()) {
		            XSModelGroup xsModelGroup = term.asModelGroup();
		            XSParticle[] particles = xsModelGroup.getChildren();
		            for (XSParticle p : particles) {
		                XSTerm pterm = p.getTerm();
		                if (pterm.isElementDecl()) { 
		                    XSElementDecl childElementDecl = (XSElementDecl) pterm;
		                    ADEschemaElement elementDecl = new ADEschemaElement(childElementDecl, schema);
		                    if (elementDecl.isComplexDataType()) {
		                    	return true;
		                    }
		                }
		            }
		        }
		    }
		}		
		return false;
	}
	
	public boolean isUnion() {
		XSType xsType = element.getType();
		if (xsType.isComplexType() && xsType.getTargetNamespace().equalsIgnoreCase(schema.getNamespaceURI())) {
			XSContentType xsContentType = xsType.asComplexType().getContentType();
			XSParticle particle = xsContentType.asParticle();       
	        if (particle != null) {
	            XSTerm term = particle.getTerm();
	            if (term.isModelGroup()) {
	                XSModelGroup xsModelGroup = term.asModelGroup();
	                if (xsModelGroup.getCompositor() == XSModelGroup.CHOICE) {
	                	return true;
	                };
	            }
	        }
		} 
		return false; 
	}

	public boolean isComplexDataType(){
		XSType type = element.getType();
		
		if (type.isComplexType() && schema.getNamespaceURI().equalsIgnoreCase(type.getTargetNamespace())) {
			if (!this.isAbstractGML() && !this.isFeature() && !this.isCityObject() && !this.isUnion()) {
				return true;
			}
		}	
		
		return false;
	}
	
	public boolean isComplexAttribute() {
		XSType xsType = element.getType();
		
		return ADEschemaHelper.ComplexAttributeTypes.containsKey(xsType.getName());
	}
	
	public boolean isSimpleBasicProperty() {
		
		XSType xsType = element.getType();
		return ADEschemaHelper.SimpleAttributeTypes.containsKey(xsType.getName());
	}
	
	public boolean isEnumerationProperty(){
		XSType xsType = element.getType();
		
		if (xsType.isSimpleType() && schema.getNamespaceURI().equalsIgnoreCase(xsType.getTargetNamespace())) {
			return true;			
		} 
		
		return false;
	}

	public boolean isADEHookElement(){
		XSElementDecl hookXSElementDecl = element.getSubstAffiliation();
		if (hookXSElementDecl != null) {
			if (ADEschemaHelper.CityGML_Hooks.containsKey(hookXSElementDecl.getName()))
				return true;
		}				
		return false;
	}
	
	public boolean isDerivedFromOtherDomains(){
		XSElementDecl substAffiliation = element.getSubstAffiliation();
		if (substAffiliation == null)
			return false;
		
		String namespace = substAffiliation.getTargetNamespace();
		if (ADEschemaHelper.CityGML_Namespaces.contains(namespace) || schema.getNamespaceURI().equalsIgnoreCase(namespace)) {
			return false;
		}			
		
		return true;
	}
	
	public boolean isBrepGeometryProperty() {
		XSType xsType = element.getType();
		return ADEschemaHelper.BrepGeometryPropertyTypes.containsKey(xsType.getName()) && xsType.getTargetNamespace().equalsIgnoreCase("http://www.opengis.net/gml");
	}
	
	public boolean isPointOrLineGeometryProperty() {
		XSType xsType = element.getType();
		return ADEschemaHelper.PointOrLineGeometryPropertyTypes.containsKey(xsType.getName()) && xsType.getTargetNamespace().equalsIgnoreCase("http://www.opengis.net/gml");
	}
	
	public boolean isHybridGeometryProperty() {
		XSType xsType = element.getType();
		return ADEschemaHelper.HybridGeometryPropertyTypes.containsKey(xsType.getName()) && xsType.getTargetNamespace().equalsIgnoreCase("http://www.opengis.net/gml");
	}

	public String getCitydbTableName() {
		return ADEschemaHelper.CityDB_Tables.get(new QName(element.getType().getTargetNamespace(), element.getType().getName()));
	}
	
	

}
