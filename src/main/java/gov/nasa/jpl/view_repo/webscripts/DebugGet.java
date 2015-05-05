/*******************************************************************************
 * Copyright (c) <2013>, California Institute of Technology ("Caltech").  
 * U.S. Government sponsorship acknowledged.
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are 
 * permitted provided that the following conditions are met:
 * 
 *  - Redistributions of source code must retain the above copyright notice, this list of 
 *    conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice, this list 
 *    of conditions and the following disclaimer in the documentation and/or other materials 
 *    provided with the distribution.
 *  - Neither the name of Caltech nor its operating division, the Jet Propulsion Laboratory, 
 *    nor the names of its contributors may be used to endorse or promote products derived 
 *    from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS 
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY 
 * AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER  
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR 
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON 
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE 
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package gov.nasa.jpl.view_repo.webscripts;

import gov.nasa.jpl.mbee.util.Debug;

//import java.util.Date;
//import java.util.HashMap;
//import java.util.Map;
//
//import org.springframework.extensions.webscripts.Cache;
//import org.springframework.extensions.webscripts.DeclarativeWebScript;
//import org.springframework.extensions.webscripts.Status;
//import org.springframework.extensions.webscripts.WebScriptRequest;

/**
 * Web service to turn debug output on off.
 * @author cinyoung
 *
 */
public class DebugGet extends FlagSet {

    @Override
    /**
     * This is called by FlagSet executeImpl
     */
    protected void set( boolean val ) {
        if ( val ) {
            Debug.turnOn();
            AbstractJavaWebScript.alwaysTurnOffDebugOut = false;
        } else {
            Debug.turnOff();
            AbstractJavaWebScript.alwaysTurnOffDebugOut = true;
        }
    }

    @Override
    protected boolean get() {
        return Debug.isOn();
    }

    @Override
    protected String flagName() {
        return "debug";
    }

}
