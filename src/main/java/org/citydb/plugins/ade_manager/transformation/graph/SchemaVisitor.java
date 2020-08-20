/*
 * 3D City Database - The Open Source CityGML Database
 * http://www.3dcitydb.org/
 *
 * Copyright 2013 - 2020
 * Chair of Geoinformatics
 * Technical University of Munich, Germany
 * https://www.gis.bgu.tum.de/
 *
 * The 3D City Database is jointly developed with the following
 * cooperation partners:
 *
 * virtualcitySYSTEMS GmbH, Berlin <http://www.virtualcitysystems.de/>
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

import com.sun.xml.xsom.XSAnnotation;
import com.sun.xml.xsom.XSAttGroupDecl;
import com.sun.xml.xsom.XSAttributeDecl;
import com.sun.xml.xsom.XSAttributeUse;
import com.sun.xml.xsom.XSComplexType;
import com.sun.xml.xsom.XSContentType;
import com.sun.xml.xsom.XSElementDecl;
import com.sun.xml.xsom.XSFacet;
import com.sun.xml.xsom.XSIdentityConstraint;
import com.sun.xml.xsom.XSModelGroup;
import com.sun.xml.xsom.XSModelGroupDecl;
import com.sun.xml.xsom.XSNotation;
import com.sun.xml.xsom.XSParticle;
import com.sun.xml.xsom.XSSchema;
import com.sun.xml.xsom.XSSimpleType;
import com.sun.xml.xsom.XSTerm;
import com.sun.xml.xsom.XSWildcard;
import com.sun.xml.xsom.XSXPath;
import com.sun.xml.xsom.visitor.XSVisitor;

public abstract class SchemaVisitor implements XSVisitor {
	
	@Override
	public void elementDecl(XSElementDecl decl) {		
		decl.getType().visit(this);		
	}

	@Override
	public void modelGroup(XSModelGroup modelGroup) {
		
	}

	@Override
	public void modelGroupDecl(XSModelGroupDecl arg0) {
		
	}

	@Override
	public void wildcard(XSWildcard arg0) {

	}

	@Override
	public void empty(XSContentType contentType) {

	}

	@Override
	public void particle(XSParticle p) {	
		if (p != null) {
			XSTerm pterm = p.getTerm();		
			pterm.visit(this);
		}				
	}

	@Override
	public void simpleType(XSSimpleType arg0) {
		
	}

	@Override
	public void annotation(XSAnnotation arg0) {
	
	}

	@Override
	public void attGroupDecl(XSAttGroupDecl arg0) {
	
	}

	@Override
	public void attributeDecl(XSAttributeDecl arg0) {
	
	}

	@Override
	public void attributeUse(XSAttributeUse arg0) {
	
	}

	@Override
	public void complexType(XSComplexType type) {
		XSContentType explictContentType = type.getExplicitContent();
		if (explictContentType != null) {
			particle(explictContentType.asParticle());
		} else {
			XSContentType contentType = type.getContentType();
			if (contentType != null)
				particle(contentType.asParticle());
		}
	}

	@Override
	public void facet(XSFacet arg0) {
	
	}

	@Override
	public void identityConstraint(XSIdentityConstraint arg0) {
	
	}

	@Override
	public void notation(XSNotation arg0) {

	}

	@Override
	public void schema(XSSchema schema) {
		
	}

	@Override
	public void xpath(XSXPath arg0) {

	}

}
