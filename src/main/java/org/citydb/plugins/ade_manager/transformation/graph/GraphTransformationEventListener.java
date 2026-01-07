/*
 * 3D City Database - The Open Source CityGML Database
 * https://www.3dcitydb.org/
 *
 * Copyright 2013 - 2026
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


import agg.xt_basis.*;
import org.citydb.util.log.Logger;

public class GraphTransformationEventListener implements GraTraEventListener {
	
	private int steps;
	private GraTra graTra;
	private final Logger log = Logger.getInstance();

	public GraphTransformationEventListener(GraTra graTra){

		this.graTra = graTra;
		steps = 0;
	}
	
	@Override
	public void graTraEventOccurred(GraTraEvent g) {
		
		String ruleName = "";
		
		if (g.getMessage() == GraTraEvent.MATCH_VALID) {
			Match currentMatch = g.getMatch();
			String currentMatchName = currentMatch.getName();
			log.debug("[" + g.getMessage() + "] Match <" + currentMatchName + "> is valid !" );
						
		} else if (g.getMessage() == GraTraEvent.STEP_COMPLETED) {
			this.steps++;
			// TODO print this info in progressbar-dialog via event dispatcher
			log.info("Matched transformation rule applied. (Step: " + this.steps + ")");
			
			Match currentMatch = g.getMatch();
			Rule currentRule = currentMatch.getRule();
			String currentRuleName = currentRule.getName();			
			log.debug("[" + g.getMessage() + "] Rule <" + currentRuleName + "> is applied ! Step" + this.steps );					
		} else if (g.getMessage() == GraTraEvent.LAYER_FINISHED) {
			if (graTra instanceof LayeredGraTraImpl){				
				if (((LayeredGraTraImpl) graTra).getCurrentLayer() >= 0) {								
					log.debug("[" + g.getMessage() + "] Layer <" + ((LayeredGraTraImpl) graTra).getCurrentLayer()  + "> finished !" );
				} else if (((LayeredGraTraImpl) graTra).getGraTraOptions().hasOption(GraTraOptions.LOOP_OVER_LAYER)) {
					log.debug("[" + g.getMessage() + "] Loop over layer. First layer will start. !" );
				}
			}			
		} else if (g.getMessage() == GraTraEvent.TRANSFORM_FINISHED) {
			graTra.stop();									
			log.debug("[" + g.getMessage() + "] Transform finished !" );
		
		}			
		else if ((g.getMessage() == GraTraEvent.INPUT_PARAMETER_NOT_SET)) {			
			String s = "<" + g.getMatch().getRule().getName() + "> parameter not set!";
			log.error("[" + g.getMessage() + "] " + s);			
			
		} else if (g.getMessage() == GraTraEvent.NOT_READY_TO_TRANSFORM) {
			ruleName = g.getMessageText();
			String s = " Please check variables of the rule:  " + ruleName; 			
			log.error("[" + g.getMessage() + "] " + s);		
			
		} else if ((g.getMessage() == GraTraEvent.ATTR_TYPE_FAILED)
				|| (g.getMessage() == GraTraEvent.RULE_FAILED)
				|| (g.getMessage() == GraTraEvent.ATOMIC_GC_FAILED)
				|| (g.getMessage() == GraTraEvent.GRAPH_FAILED)) {
			String s = g.getMessageText();
			log.error("[" + g.getMessage() + "] " + s);
			
		} else if (g.getMessage() == GraTraEvent.NEW_MATCH) {
			Match currentMatch = g.getMatch();
			Rule currentRule = currentMatch.getRule();
			String currentRuleName = currentRule.getName();
			log.debug("[" + g.getMessage() + "] new match of <" + currentRuleName + "> created !" );
			
		} else if (g.getMessage() == GraTraEvent.NO_COMPLETION) {
			Match currentMatch = g.getMatch();
			Rule currentRule = currentMatch.getRule();
			String currentRuleName = currentRule.getName();
			log.debug("[" + g.getMessage() + "] no completion of current match of Rule <" + currentRuleName + "> !" );
			
		} else if (g.getMessage() == GraTraEvent.INCONSISTENT) {
			// ruleName = currentRule.getName();
			String msg = "Graph inconsistency after applying rule <"+ruleName+"> !";
			log.error("[" + g.getMessage() + "] " + msg);
		}		
	}

}
