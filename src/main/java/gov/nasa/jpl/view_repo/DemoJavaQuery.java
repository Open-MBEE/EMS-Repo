package gov.nasa.jpl.view_repo;

import org.alfresco.repo.processor.BaseProcessorExtension;

/**
 * Simple Java based component that exposes a Javascript library
 * Copy of DemoJavascriptComponent
 *
 */
public class DemoJavaQuery extends BaseProcessorExtension {
	private DemoComponent demoComponent;

	public DemoJavaQuery() {
		
	}
	
	public DemoComponent getDemoComponent() {
	    System.out.println("getDemoComponent()");
		return demoComponent;
	}

	public void setDemoComponent(DemoComponent demoComponent) {
        System.out.println("setDemoComponent()");
		this.demoComponent = demoComponent;
	}
	
}
