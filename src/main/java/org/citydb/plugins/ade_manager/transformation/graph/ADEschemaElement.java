package org.citydb.plugins.ade_manager.transformation.graph;

import javax.xml.namespace.QName;

import org.citygml4j.xml.schema.ElementDecl;
import org.citygml4j.xml.schema.Schema;

import com.sun.xml.xsom.XSContentType;
import com.sun.xml.xsom.XSElementDecl;
import com.sun.xml.xsom.XSModelGroup;
import com.sun.xml.xsom.XSParticle;
import com.sun.xml.xsom.XSTerm;
import com.sun.xml.xsom.XSType;

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
		if (typeName.equalsIgnoreCase("ReferenceType") && namespace.indexOf("http://www.opengis.net/gml") > -1) {
			return true;
		} 
		return false;
	}
	
	public boolean isCityGMLnonPorperty() {
		XSType xsType = element.getType();
		
		if (ADEschemaHelper.CityDB_Tables.containsKey(new QName(xsType.getTargetNamespace(), xsType.getName()))) {
			return true;
		}
		
		return false;
	}
	
	public boolean isImplicitGeometryProperty() {
		XSType xsType = element.getType();
		String namespace = xsType.getTargetNamespace();
		String typeName = xsType.getName();
		if (typeName.equalsIgnoreCase("ImplicitRepresentationPropertyType") && (namespace.equalsIgnoreCase("http://www.opengis.net/citygml/1.0") || namespace.equalsIgnoreCase("http://www.opengis.net/citygml/2.0"))) {
			return true;
		} 
		return false;
	}
	
	public boolean isFeatureOrObjectOrComplexDataProperty() {
		XSType xsType = element.getType();
		String localNamespace = xsType.getTargetNamespace();
		if (xsType.isComplexType() && (schema.getNamespaceURI().equalsIgnoreCase(localNamespace) || ADEschemaHelper.CityGML_Namespaces.contains(localNamespace))) {
			XSContentType xsContentType = xsType.asComplexType().getContentType();
	        XSParticle particle = xsContentType.asParticle();       
	        if (particle != null) {
	        	return true;
	        }			
		} 
		return false;
	}
	
	public boolean isFeatureOrObjectProperty() {
		XSType xsType = element.getType();
		String localNamespace = xsType.getTargetNamespace();
		if (xsType.isComplexType() && (schema.getNamespaceURI().equalsIgnoreCase(localNamespace)
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
		if (xsType.isComplexType() && (schema.getNamespaceURI().equalsIgnoreCase(localNamespace)
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
		if (xsType.isComplexType() && (schema.getNamespaceURI().equalsIgnoreCase(localNamespace) || ADEschemaHelper.CityGML_Namespaces.contains(localNamespace))) {
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
		return ADEschemaHelper.BrepGeometryPropertyTypes.containsKey(xsType.getName());
	}
	
	public boolean isPointOrLineGeometryProperty() {
		XSType xsType = element.getType();
		return ADEschemaHelper.PointOrLineGeometryPropertyTypes.containsKey(xsType.getName());
	}
	
	public boolean isHybridGeometryProperty() {
		XSType xsType = element.getType();
		return ADEschemaHelper.HybridGeometryPropertyTypes.containsKey(xsType.getName());
	}

	public String getCitydbTableName() {
		return ADEschemaHelper.CityDB_Tables.get(new QName(element.getType().getTargetNamespace(), element.getType().getName()));
	}
	
	

}
