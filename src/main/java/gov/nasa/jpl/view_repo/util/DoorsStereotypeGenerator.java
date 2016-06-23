/*******************************************************************************
 * Copyright (c) <2013>, California Institute of Technology ("Caltech"). U.S.
 * Government sponsorship acknowledged.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer. - Redistributions in binary
 * form must reproduce the above copyright notice, this list of conditions and
 * the following disclaimer in the documentation and/or other materials provided
 * with the distribution. - Neither the name of Caltech nor its operating
 * division, the Jet Propulsion Laboratory, nor the names of its contributors
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/

package gov.nasa.jpl.view_repo.util;

import java.io.InputStream;
import java.sql.SQLException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.json.JSONObject;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import gov.nasa.jpl.view_repo.util.NodeUtil;
import gov.nasa.jpl.mbee.doorsng.DoorsClient;
import gov.nasa.jpl.view_repo.db.PostgresHelper;

/***
 * 
 * @author Bruce Meeks Jr
 * 
 *         Description: This utility class will read in artifact types and their
 *         custom attributes from a specified Doors project then create the
 *         corresponding sysml stereotype profiles and properties
 *
 */
public class DoorsStereotypeGenerator {

	public DoorsStereotypeGenerator() {

	}

	public JSONObject createStereotype(DoorsClient doorsClient, String project) {

		InputStream projectProperties = null;

		JSONObject parentObj = new JSONObject();
		JSONArray elementArray = new JSONArray();

		try {

			projectProperties = doorsClient.getAllArtifactTypes(project);
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(projectProperties);

			NodeList artifactTypes = doc.getElementsByTagName("attribute:objectType");
			JSONObject newStereotype = null;
			String newStereotypeSysmlID = null;
			Node curArtifactType = null;
			Attr curArtifactTypeNodeAttribute = null;

			// iterate through all artifact type nodes found in Doors and create
			// a new JSON stereotype profile for each
			for (int at = 0; at < artifactTypes.getLength(); at++) {

				newStereotype = new JSONObject();
				newStereotypeSysmlID = "";

				curArtifactType = artifactTypes.item(at);

				// find current artifact type's name from its node attributes
				for (int ata = 0; ata < curArtifactType.getAttributes().getLength(); ata++) {

					curArtifactTypeNodeAttribute = (Attr) curArtifactType.getAttributes().item(ata);

					if (!curArtifactTypeNodeAttribute.getName().equals("attribute:name")) {
						continue;

					}
					// create JSON object for the new artifact type / stereotype
					// profile
					else {

						newStereotype.put("name", curArtifactTypeNodeAttribute.getValue());
						newStereotype.put("documentation", "");
						newStereotypeSysmlID = NodeUtil.createId(NodeUtil.getServiceRegistry());
						// map Doors artifact type to sysml stereotype
						mapArtifactTypeStereotype(curArtifactTypeNodeAttribute.getValue(), newStereotypeSysmlID);
						newStereotype.put("sysmlid", newStereotypeSysmlID);
						// todo get project sysml id for owner field
						newStereotype.put("owner", "get current project sysml id?");
						newStereotype.put("appliedMetatypes",
								new JSONArray().put("_9_0_62a020a_1105704941426_574917_9666"));
						newStereotype.put("metatypes", new JSONArray().put("_9_0_62a020a_1105704884807_371561_7741"));
						newStereotype.put("specialization", new JSONObject().put("type", "Untyped"));
						newStereotype.put("isMetatype", true);
						newStereotype.put("ownedAttribute",
								new JSONArray().put("_18_0_5_f4e036d_1460066013862_426264_16853")
										.put("_18_0_5_f4e036d_1460066029197_385662_16857"));

						// add base element for the current new stereotype
						JSONObject baseElement = new JSONObject();
						JSONObject baseElementSpecialization = new JSONObject();
						baseElement.put("name", "base_Element");
						baseElement.put("sysmlid", "_18_0_5_f4e036d_1460066013862_426264_16853");
						baseElement.put("documentation", "");
						baseElement.put("owner", newStereotypeSysmlID);
						baseElement.put("appliedMetatypes",
								new JSONArray().put("_9_0_62a020a_1105704884574_96724_7644"));
						baseElementSpecialization.put("aggregation", "none");
						baseElementSpecialization.put("multiplicityMax", 1);
						baseElementSpecialization.put("propertyType", "_9_0_62a020a_1105704884807_371561_7741");
						baseElementSpecialization.put("isDerived", false);
						baseElementSpecialization.put("isSlot", false);
						baseElementSpecialization.put("value", new JSONArray());
						baseElementSpecialization.put("multiplicityMin", 1);
						baseElementSpecialization.put("type", "Property");
						baseElementSpecialization.put("redefines", new JSONArray());
						baseElement.put("specialization", baseElementSpecialization);
						baseElement.put("isMetatype", false);

						elementArray.put(newStereotype);
						elementArray.put(baseElement);

						break;

					}

				}

				Node curArtifactTypeCustomAttribute = null;
				JSONObject newStereotypeProperty = null;

				// Now iterate through the current doors artifact type's custom
				// attributes to create the corresponding stereotype properties
				for (int atca = 0; atca < curArtifactType.getChildNodes().getLength(); atca++) {

					curArtifactTypeCustomAttribute = curArtifactType.getChildNodes().item(atca);
					Attr customAttributeAttribute = null;

					// iterate through current artifact type's custom
					// attributes' attributes to
					// retrieve their names and create new stereotype property
					for (int atcaa = 0; atcaa < curArtifactTypeCustomAttribute.getAttributes().getLength(); atcaa++) {

						customAttributeAttribute = (Attr) curArtifactTypeCustomAttribute.getAttributes().item(atcaa);

						if (customAttributeAttribute.getName().equals("attribute:name")) {

							// seems like every artifact type has this attribute
							// property; disregard
							if (customAttributeAttribute.getValue().equals("Identifier")) {
								break;
							}

							// create new property for the current artifact type
							// /
							// stereotype profile
							newStereotypeProperty = new JSONObject();
							newStereotypeProperty.put("name", customAttributeAttribute.getValue());
							newStereotypeProperty.put("documentation", "");
							newStereotypeProperty.put("sysmlid", NodeUtil.createId(NodeUtil.getServiceRegistry()));
							newStereotypeProperty.put("owner", newStereotypeSysmlID);
							newStereotypeProperty.put("appliedMetatypes",
									new JSONArray().put("_9_0_62a020a_1105704884574_96724_7644"));
							JSONObject stereotypePropertySpecialization = new JSONObject();
							stereotypePropertySpecialization.put("aggregation", "NONE");
							stereotypePropertySpecialization.put("multiplicityMax", 1);
							stereotypePropertySpecialization.put("propertyType", "null");
							stereotypePropertySpecialization.put("isDerived", false);
							stereotypePropertySpecialization.put("isSlot", false);
							stereotypePropertySpecialization.put("value", new JSONArray());
							stereotypePropertySpecialization.put("multiplicityMin", 1);
							stereotypePropertySpecialization.put("type", "Property");
							stereotypePropertySpecialization.put("redefines", new JSONArray());
							newStereotypeProperty.put("specialization", stereotypePropertySpecialization);
							newStereotypeProperty.put("isMetatype", false);

							elementArray.put(newStereotypeProperty);

						}

					}

				}

			}

			parentObj.put("mmsVersion", "2.3");
			parentObj.put("source", "doors");
			parentObj.put("elements", elementArray);

		} catch (Exception e) {

			e.printStackTrace();

		}

		return parentObj;

	}

	public void mapArtifactTypeStereotype(String artifactTypeName, String stereotypeSysMlID) {

		PostgresHelper pgh = new PostgresHelper("null");

		try {

			pgh.connect();

			pgh.execUpdate(
					"CREATE TABLE doorsartifacttypes (artifacttype text not null, stereotypesysmlid text not null);");

		} catch (SQLException e) {
			e.printStackTrace(); // table may already exists, no prob
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

		try {

			pgh.execUpdate("insert into doorsartifacttypesstereotypes (artifacttype, stereotypesysmlid)" + " VALUES ('"
					+ artifactTypeName + "','" + stereotypeSysMlID + "')");

		} catch (SQLException e) {
			e.printStackTrace();
		}finally {
            pgh.close();
        }

	}

}
