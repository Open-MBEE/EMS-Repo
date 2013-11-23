package gov.nasa.jpl.view_repo.webscripts;

import org.json.JSONArray;
import org.json.JSONException;

/**
 * Function pointer for handling transactions based on JSONArray inputs
 * @author cinyoung
 *
 */
public interface JwsFunctor {
	/**
	 * Execute the transaction for creating from an JSON type
	 * @param jsonArray		Input JSON array to generate content from
	 * @param index			Index into jsonArray to create content from
	 * @param flags			Var args of boolean flags (e.g., force, product, etc.)
	 * @return				Optional return of what is created
	 * @throws JSONException
	 */
	public Object execute(JSONArray jsonArray, int index, Boolean... flags) throws JSONException;
}
