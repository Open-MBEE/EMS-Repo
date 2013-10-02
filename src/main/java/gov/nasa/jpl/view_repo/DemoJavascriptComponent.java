package gov.nasa.jpl.view_repo;

import org.alfresco.repo.processor.BaseProcessorExtension;

/**
 * Simple Java based component that exposes a Javascript library
 * @author cinyoung
 *
 */
public class DemoJavascriptComponent extends BaseProcessorExtension {
	private DemoComponent demoComponent;

	public DemoJavascriptComponent() {
		
	}
	
	public DemoComponent getDemoComponent() {
		return demoComponent;
	}

	public void setDemoComponent(DemoComponent demoComponent) {
		this.demoComponent = demoComponent;
	}
	
}
