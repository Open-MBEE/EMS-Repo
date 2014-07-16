<?xml version="1.0" encoding="UTF-8"?>

<!--
  Formatting for WS-I Profile Test Assertion Document used by the Testing Tools
 
	Copyright (c) 2002-2004 by The Web Services-Interoperability Organization (WS-I) and 
	Certain of its Members. All Rights Reserved.
	
	Notice
	The material contained herein is not a license, either expressly or impliedly, to any 
	intellectual property owned or controlled by any of the authors or developers of this 
	material or WS-I. The material contained herein is provided on an "AS IS" basis and to 
	the maximum extent permitted by applicable law, this material is provided AS IS AND WITH 
	ALL FAULTS, and the authors and developers of this material and WS-I hereby disclaim all 
	other warranties and conditions, either express, implied or statutory, including, but not 
	limited to, any (if any) implied warranties, duties or conditions of  merchantability, 
	of fitness for a particular purpose, of accuracy or completeness of responses, of results, 
	of workmanlike effort, of lack of viruses, and of lack of negligence. ALSO, THERE IS NO 
	WARRANTY OR CONDITION OF TITLE, QUIET ENJOYMENT, QUIET POSSESSION, CORRESPONDENCE TO 
	DESCRIPTION OR NON-INFRINGEMENT WITH REGARD TO THIS MATERIAL.
	
	IN NO EVENT WILL ANY AUTHOR OR DEVELOPER OF THIS MATERIAL OR WS-I BE LIABLE TO ANY OTHER 
	PARTY FOR THE COST OF PROCURING SUBSTITUTE GOODS OR SERVICES, LOST PROFITS, LOSS OF USE, 
	LOSS OF DATA, OR ANY INCIDENTAL, CONSEQUENTIAL, DIRECT, INDIRECT, OR SPECIAL DAMAGES 
	WHETHER UNDER CONTRACT, TORT, WARRANTY, OR OTHERWISE, ARISING IN ANY WAY OUT OF THIS OR 
	ANY OTHER AGREEMENT RELATING TO THIS MATERIAL, WHETHER OR NOT SUCH PARTY HAD ADVANCE 
	NOTICE OF THE POSSIBILITY OF SUCH DAMAGES.
	
	WS-I License Information
	Use of this WS-I Material is governed by the WS-I Test License and other licenses.  Information on these 
	licenses are contained in the README.txt and ReleaseNotes.txt files.  By downloading this file, you agree 
	to the terms of these licenses.
		
	How To Provide Feedback
	The Web Services-Interoperability Organization (WS-I) would like to receive input, 
	suggestions and other feedback ("Feedback") on this work from a wide variety of 
	industry participants to improve its quality over time. 
	
	By sending email, or otherwise communicating with WS-I, you (on behalf of yourself if 
	you are an individual, and your company if you are providing Feedback on behalf of the 
	company) will be deemed to have granted to WS-I, the members of WS-I, and other parties 
	that have access to your Feedback, a non-exclusive, non-transferable, worldwide, perpetual, 
	irrevocable, royalty-free license to use, disclose, copy, license, modify, sublicense or 
	otherwise distribute and exploit in any manner whatsoever the Feedback you provide regarding 
	the work. You acknowledge that you have no expectation of confidentiality with respect to 
	any Feedback you provide. You represent and warrant that you have rights to provide this 
	Feedback, and if you are providing Feedback on behalf of a company, you represent and warrant 
	that you have the rights to provide Feedback on behalf of your company. You also acknowledge 
	that WS-I is not required to review, discuss, use, consider or in any way incorporate your 
	Feedback into future versions of its work. If WS-I does incorporate some or all of your 
	Feedback in a future version of the work, it may, but is not obligated to include your name 
	(or, if you are identified as acting on behalf of your company, the name of your company) on 
	a list of contributors to the work. If the foregoing is not acceptable to you and any company 
	on whose behalf you are acting, please do not provide any Feedback.
	
	WS-I members should direct feedback on this document to wsi_testing@lists.ws-i.org; 
    non-members should direct feedback to wsi-tools@ws-i.org. 
	
 
  Copyright (c) 2002 - 2004 IBM Corporation.  All rights reserved.
 
  @author Peter Brittenham, peterbr@us.ibm.com
  @version 0.91 
-->

<xsl:stylesheet
	version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:wsi-common="http://www.ws-i.org/testing/2003/03/common/"
	xmlns:wsi-assertions="http://www.ws-i.org/testing/2004/07/assertions/">
<xsl:import href="common.xsl"/>

<xsl:output method="html" indent="yes"/>

<!-- ADD: Need to determine how to handle multiple profile links. -->
<xsl:variable name="profileLink" select="/wsi-assertions:profileAssertions/wsi-assertions:profileList/wsi-assertions:profile/@location"/>

<xsl:variable name="highlightColor1" select="'#009999'"/>
<xsl:variable name="highlightColor2" select="'#7ca8da'"/>
<xsl:variable name="highlightColor3" select="'orange'"/>
<xsl:variable name="highlightColor4" select="'red'"/>
<xsl:variable name="noteColor" select="'#0000cc'"/>

<xsl:key name="enabled" match="@enabled" use="."/>
<xsl:key name="status" match="@status" use="."/>
<xsl:key name="targetRelease" match="@targetRelease" use="."/>
<xsl:key name="type" match="@type" use="."/>

<xsl:template match="/">
<html>
	<!-- <link type="text/css" rel="stylesheet" href="http://www.ws-i.org/styles.css" /> 
	        font-size : 12px;
  -->
	<head>
    <title>WS-I Profile Test Assertion Document</title>    
    <!-- Need to put the following in a common CSS file -->
    <style type="text/css">
      BODY {
        border-top-style : none;
        border-left-style : none;
        border-right-style : none;
        border-bottom-style : none;
        font-family : Arial,sans-serif;
   	  font-size : 22px;
      }
      H1 {
        color : #336699;
   	  font-size : 24px;
      }
      H2 {
      }
      H3 {
        background-color : #336699;
        PADDING-BOTTOM: 1px;
        PADDING-LEFT: 4px;
        PADDING-RIGHT: 4px;
        PADDING-TOP: 1px;
        color : white;
   	  font-size : 22px;
      }
      H4 {
        background-color : #7ca8da;
        padding-left : 6px;
        padding-right : 6px;
        padding-top : 2px;
        padding-bottom : 2px;
   	  font-size : 20px;
      }
      TABLE {
        margin-left : 1em;
        margin-right : 1em;
   	  font-size : 20px;
      }    
      .contents1 {
        margin-left : 2em;
      }
      .contents2 {
        margin-left : 2.2em;
      }
      .data-type {
        margin-left : 1em;
        margin-right : 1em;
      }    
      .data-content {
        margin-left : 1em;
        margin-right : 1em;
      }    
    </style>
	</head>

	<body>
    <a name="top"/>
    <img align="right" src="http://www.ws-i.org/images/WS-I-logo.gif"/>
    <h1>WS-I Profile Test Assertion Document</h1>
		<xsl:apply-templates />
	</body>
</html>
</xsl:template>

<xsl:template match="wsi-common:description" >
</xsl:template>

<xsl:template match="wsi-assertions:version" >
</xsl:template>

<xsl:template match="wsi-assertions:profileAssertions" >
  <table>
  <tr><td>
  <b>Name:</b>
  </td><td>
  <xsl:value-of select="@name" />
  </td></tr>
  <tr><td>
  <b>Version:</b>
  </td><td>
      <xsl:value-of select="substring(@version,1,3)" />
  </td></tr>
   <xsl:if test="count(@date) &gt; 0">
      <tr>
          <td>
               <b>Date:</b>
          </td>
          <td>
              <xsl:value-of select="@date"/>
           </td>
      </tr>
  </xsl:if>
  <xsl:if test="count(@status) &gt; 0">
      <tr>
          <td>
               <b>Status:</b>
          </td>
          <td>
              <xsl:value-of select="@status"/>
           </td>
      </tr>
  </xsl:if>
  </table>
	<br/>
	<h3>Editors</h3>
<xsl:if test="count(wsi-assertions:editors/wsi-assertions:person) &lt; 1">
    <p>None</p>
</xsl:if>
<table>
<xsl:for-each select="wsi-assertions:editors/wsi-assertions:person">
<tr><td><xsl:value-of select="."/>, (<xsl:value-of select="@affiliation"/>)</td>
        <td><a href="mailto:&lt;xsl:value-of select=&quot;@href&quot;/&gt;"><xsl:value-of select="@href"/></a></td>     
</tr>
</xsl:for-each>
</table>
<xsl:if test="count(wsi-assertions:contributorText) &gt; 0">
    <h4>Other Contributors</h4>
    <p><xsl:value-of select="wsi-assertions:contributorText"/></p>
</xsl:if>    
  <hr style="color : black;"/>
  <p><xsl:value-of select="wsi-common:description" /></p>
  
	<p>A "candidate" element is one that is to be verified for conformance.  
The binding of the tModel if &lt;wsi-analyzerConfig:uddiReference&gt; is given or the 
&lt;wsi-analyzerConfig:wsdlElement&gt; in the configuration file of the Analyzer define a 
candidate element for verification.  A verification on an element also implies that the same 
verification is made for all the elements that it uses.  That is, the elements it uses also 
become candidate elements.  Verification it based on the following transitivity rules, applied recursively.
</p>
<p>For WSDL element references:</p>
<ul>
<li>A verfication on a wsdl:port is inherited by the referenced wsdl:bindings</li> 
<li>A verfication on a wsdl:binding is inherited by the referenced wsdl:portTypes</li> 
<li>A verfication on a wsdl:portType is inherited by the referenced wsdl:operations</li> 
<li>A verfication on a wsdl:operation is inherited by the referenced wsdl:messages</li> 
</ul>

<p>For UDDI references:</p>
<ul>
<li>A verfication on a uddi:bindingTemplate is inherited by the referenced uddi:tModel</li> 
<li>A verfication on a uddi:tModel is inherited by the referenced wsdl:binding</li> 
</ul>
 
<p>The <a href="../docs/AnalyzerSpecification.pdf">analyzer specification</a> contains a detailed explanation of all of the fields listed in this document.</p>
  <hr style="color : black;"/>
  <p><b>NOTE: </b>Test assertion headings that have 
  this <span style="background-color : {$highlightColor4};border-width : 1px 1px 1px 1px;border-style : solid solid solid solid;border-color : black black black black;">background color</span> are 
  disabled and will not be processed by the analyzer.
  </p>
    
  <xsl:call-template name="copyright"/>

  <hr style="color : black;"/>
  <h2>Contents</h2>
  <p/>
  <p class="contents1"><a href="#profileDefinitions">Profile Definitions</a><br/>
  <a href="#artifacts">Test Assertion Artifacts</a><br/>
  <xsl:for-each select="wsi-assertions:artifact">
    <span class="contents2">
      <xsl:variable name="linkName" select="@type"/>
      <a href="#artifact{$linkName}"><xsl:value-of select="@type"/></a><br/>
    </span>
  </xsl:for-each>
  <a href="#counts">Test Assertion Counts</a><br/>
  <a href="#requirementsIndex">Profile Requirements Index</a><br/>
	<xsl:if test="//wsi-assertions:testAssertion/wsi-assertions:additionalInfo">
  <a href="#oldIdIndex">Old Test Assertion ID Index</a><br/>
  </xsl:if>
  </p>
   
  <hr style="color : black;"/>
  <h2><a name="profileDefinitions">Profile Definitions</a></h2>
  <p/>
  <table cellpadding="4" bgcolor="#000000"  cellspacing="1" valign="top">
  <tr bgcolor="{$titleRowColor}"><td>
  <b>ID</b>
  </td><td>
  <b>Name</b>
  </td><td>
  <b>Version</b>
  </td><td>
  <b>Revision</b>
  </td><td>
  <b>Location</b>
  </td></tr>
  <xsl:for-each select="wsi-assertions:profileList/wsi-assertions:profile">
    <tr bgcolor="#ffffff"><td>
    <xsl:variable name="profileID" select="@id"/>
    <a name="{$profileID}"><xsl:value-of select="$profileID"/></a>
    </td><td>
    <xsl:value-of select="@name"/>
    </td><td>
    <xsl:value-of select="@version"/>
    </td><td>
    <xsl:value-of select="@revision"/>
    </td><td>
    <xsl:variable name="location" select="@location"/>
    <a href="{$location}"><xsl:value-of select="$location"/></a>
    </td></tr>
  </xsl:for-each>
  </table>
  
  <p/>

  <hr style="color : black;"/>
  <h2><a name="artifacts">Test Assertion Artifacts</a></h2>
  <ul>
  <xsl:for-each select="wsi-assertions:artifact">
    <li>
    <xsl:variable name="linkName" select="@type"/>
    <a href="#artifact{$linkName}"><xsl:value-of select="@type"/></a>
    </li>
  </xsl:for-each>
  </ul>

  <hr style="color : black;"/>
  <xsl:apply-templates select="wsi-assertions:artifact"/>

	<br/> 
  <h2><a name="counts">Test Assertion Counts</a></h2>
  <p><b>Total Count: </b>
  <span style="border-width : 1px 1px 1px 1px;border-style : solid solid solid solid;border-color : black black black black;"> <xsl:value-of select="count(//wsi-assertions:testAssertion)"/> </span></p>

  <p><b>Count By Type:</b></p>
  <table cellpadding="4" bgcolor="#000000"  cellspacing="1" valign="top" width="30%">
    <col span="1" width="70%"/>
    <col span="2" width="30%"/>
    <tr bgcolor="{$titleRowColor}"><td>
    <b>Type</b>
    </td><td>
    <b>Count</b>
    </td></tr>
    <xsl:for-each select="//wsi-assertions:testAssertion/@type[generate-id()=generate-id(key('type', .))]">
    <tr bgcolor="#ffffff"><td>
    <xsl:value-of select="."/>
    </td><td>
    <xsl:value-of select="count(//wsi-assertions:testAssertion[@type=current()])"/>
    </td></tr>
  </xsl:for-each>
  </table>

  <p><b>Count By Enabled Indicator:</b></p>
  <table cellpadding="4" bgcolor="#000000"  cellspacing="1" valign="top" width="30%">
    <col span="1" width="70%"/>
    <col span="2" width="30%"/>
    <tr bgcolor="{$titleRowColor}"><td>
    <b>Enabled</b>
    </td><td>
    <b>Count</b>
    </td></tr>
    <xsl:for-each select="//wsi-assertions:testAssertion/@enabled[generate-id()=generate-id(key('enabled', .))]">
    <tr bgcolor="#ffffff"><td>
    <xsl:value-of select="."/>
    </td><td>
    <xsl:value-of select="count(//wsi-assertions:testAssertion[@enabled=current()])"/>
    </td></tr>
  </xsl:for-each>
  </table>

	<xsl:if test="//wsi-assertions:testAssertion/wsi-assertions:additionalInfo">
  <p><b>Count By Status:</b></p>
  <table cellpadding="4" bgcolor="#000000"  cellspacing="1" valign="top" width="30%">
    <col span="1" width="70%"/>
    <col span="2" width="30%"/>
    <tr bgcolor="{$titleRowColor}"><td>
    <b>Status</b>
    </td><td>
    <b>Count</b>
    </td></tr>
    <xsl:for-each select="//wsi-assertions:testAssertion/wsi-assertions:additionalInfo/@status[generate-id()=generate-id(key('status', .))]">
    <tr bgcolor="#ffffff"><td>
    <xsl:value-of select="."/>
    </td><td>
    <xsl:value-of select="count(//wsi-assertions:testAssertion/wsi-assertions:additionalInfo[@status=current()])"/>
    </td></tr>
  </xsl:for-each>
  </table>

  <p><b>Count By Target Release:</b></p>
  <table cellpadding="4" bgcolor="#000000"  cellspacing="1" valign="top" width="30%">
    <col span="1" width="70%"/>
    <col span="2" width="30%"/>
    <tr bgcolor="{$titleRowColor}"><td>
    <b>Target Release</b>
    </td><td>
    <b>Count</b>
    </td></tr>
    <xsl:for-each select="//wsi-assertions:testAssertion/wsi-assertions:additionalInfo/@targetRelease[generate-id()=generate-id(key('targetRelease', .))]">
    <xsl:sort select="." data-type="text"/>
    <tr bgcolor="#ffffff"><td>
    <xsl:value-of select="."/>
    </td><td>
    <xsl:value-of select="count(//wsi-assertions:testAssertion/wsi-assertions:additionalInfo[@targetRelease=current()])"/>
    </td></tr>
  </xsl:for-each>
  </table>
  </xsl:if>
  <br/>

  <hr style="color : black;"/>
  <h2><a name="requirementsIndex">Profile Requirement Index</a></h2>
  <p>This index contains a list of all of the requirements listed in the test assertion document.</p>

  <table cellpadding="4" bgcolor="#000000" cellspacing="1" valign="top">
  <tr bgcolor="{$titleRowColor}"><td>
  <b>Profile Requirement</b>
  </td><td>
  <b>Test Assertion</b>
  </td></tr>
  <xsl:for-each select="//wsi-assertions:testAssertion">
    <xsl:sort select="wsi-assertions:referenceList/wsi-assertions:reference" data-type="text"/>
    <xsl:variable name="taID" select="@id"/>
    <xsl:for-each select="wsi-assertions:referenceList/wsi-assertions:reference">
    <xsl:choose>
    <xsl:when test="current()=''"/>
    <xsl:otherwise>
    <tr bgcolor="#ffffff"><td>
    <xsl:variable name="requirement" select="."/>
    <a href="{$profileLink}#{$requirement}"><xsl:value-of select="."/></a>
    </td><td>
    <a href="#{$taID}"><xsl:value-of select="$taID"/></a>
    </td></tr>
    </xsl:otherwise>
    </xsl:choose>
    </xsl:for-each>
  </xsl:for-each>
  </table>

	<xsl:if test="//wsi-assertions:testAssertion/wsi-assertions:additionalInfo">
  <hr style="color : black;"/>
  <h2><a name="oldIdIndex">Old Test Assertion ID Index</a></h2>
  <p>This index contains a sorted list of OLD test assertions IDs linked to the new ID.</p>

  <table cellpadding="4" bgcolor="#000000" cellspacing="1" valign="top">
  <tr bgcolor="{$titleRowColor}"><td>
  <b>Old ID</b>
  </td><td>
  <b>New ID</b>
  </td></tr>
  <xsl:for-each select="//wsi-assertions:testAssertion">
    <xsl:sort select="wsi-assertions:additionalInfo/@oldId" data-type="text"/>
    <xsl:choose>
    <xsl:when test="wsi-assertions:additionalInfo/@oldId=''"/>
    <xsl:otherwise>
    <tr bgcolor="#ffffff"><td>
    <xsl:value-of select="wsi-assertions:additionalInfo/@oldId"/>
    </td><td>
    <xsl:variable name="linkName" select="@id"/>
    <a href="#{$linkName}"><xsl:value-of select="@id"/></a>
    </td></tr>
    </xsl:otherwise>
    </xsl:choose>
  </xsl:for-each>
  </table>
  </xsl:if>
	
	<p/>
	<xsl:call-template name="notice"/>
</xsl:template>

<xsl:template match="wsi-assertions:artifact" >
  <xsl:variable name="linkName" select="@type"/>
	<a name="artifact{$linkName}"><h3>Profile Artifact: <xsl:value-of select="@type"/></h3></a>

  <p><xsl:value-of select="wsi-common:description" /></p>

  <p><b>Specification Reference List:</b></p>
  <ul>
  <xsl:for-each select="wsi-assertions:specificationReferenceList/wsi-assertions:specification">
    <li>
    <xsl:variable name="specLink" select="@location"/>
	  <a href="{$specLink}"><xsl:value-of select="@name"/></a>
    </li>
  </xsl:for-each>
  </ul>

  <hr style="color : black;"/>
  <b>Test Assertions [as they appear in the document]:</b>
  <p/>
  <table cellpadding="4" bgcolor="#000000" cellspacing="1" valign="top">
  <tr bgcolor="{$titleRowColor}"><td>
  <b>ID</b>
  </td>
 	<xsl:if test="//wsi-assertions:testAssertion/wsi-assertions:additionalInfo">
  <td>
  <b>Old ID</b>
  </td>
  </xsl:if>
  <td>
  <b>Entry Type</b>
  </td><td>
  <b>Test Type</b>
  </td><td>
  <b>Enabled</b>
  </td>
	<xsl:if test="//wsi-assertions:testAssertion/wsi-assertions:additionalInfo">
  <td>
  <b>Priority</b>
  </td><td>
  <b>Status</b>
  </td><td>
  <b>Needed By<br/>Sample Application WG</b>
  </td><td>
  <b>Target Release</b>
  </td>
  </xsl:if>
  </tr>
  <xsl:for-each select="wsi-assertions:testAssertion">
    <tr bgcolor="#ffffff"><td>
    <xsl:variable name="taID" select="@id"/>
    <a href="#{$taID}"><xsl:value-of select="@id"/></a>
    </td>
    <xsl:if test="//wsi-assertions:testAssertion/wsi-assertions:additionalInfo">
    <td>
    <xsl:value-of select="wsi-assertions:additionalInfo/@oldId"/>
    </td>
    </xsl:if>
    <td>
    <xsl:value-of select="@entryType"/>
    </td><td>
    <xsl:value-of select="@type"/>
    </td><td>
    <xsl:choose>
    <xsl:when test="@enabled='false'">
      <p style="color : {$highlightColor4};"><b>false</b></p>    
    </xsl:when>
    <xsl:otherwise>
       <p>true</p>    
    </xsl:otherwise>
    </xsl:choose>
    </td>
  	<xsl:if test="//wsi-assertions:testAssertion/wsi-assertions:additionalInfo">
    <td>
    <xsl:value-of select="wsi-assertions:additionalInfo/@priority"/>
    </td><td>
    <xsl:variable name="status" select="wsi-assertions:additionalInfo/@status"/>
    <xsl:choose>
    <xsl:when test="$status='bothDone'">
      <p><xsl:value-of select="$status"/></p>    
    </xsl:when>
    <xsl:when test="$status='csDone'">
      <p style="color : {$highlightColor1}"><b><xsl:value-of select="$status"/></b></p>    
    </xsl:when>
    <xsl:when test="$status='javaDone'">
      <p style="color : {$highlightColor2}"><b><xsl:value-of select="$status"/></b></p>    
    </xsl:when>
    <xsl:when test="$status='proposedUpdate'">
      <p style="color : {$highlightColor3}"><b><xsl:value-of select="$status"/></b></p>    
    </xsl:when>
    <xsl:when test="$status='clarificationNeeded'">
      <p style="color : {$highlightColor3}"><b><xsl:value-of select="$status"/></b></p>    
    </xsl:when>
    <xsl:otherwise>
      <p style="color : {$highlightColor4}"><b><xsl:value-of select="$status"/></b></p>    
    </xsl:otherwise>
    </xsl:choose>
    </td><td>
    <xsl:value-of select="wsi-assertions:additionalInfo/@sampleApp"/>
    </td><td>
    <xsl:value-of select="wsi-assertions:additionalInfo/@targetRelease"/>
		</td>
		</xsl:if>
    </tr>
  </xsl:for-each>
  </table>

  <p/>
  <b>Test Assertions [sorted by ID]:</b>
  <br/>
  <br/>
  <table cellpadding="4" bgcolor="#000000" cellspacing="1" valign="top">
  <tr bgcolor="{$titleRowColor}"><td>
  <b>ID</b>
  </td>
  <xsl:if test="//wsi-assertions:testAssertion/wsi-assertions:additionalInfo">
  <td>
  <b>Old ID</b>
  </td>
  </xsl:if>
  <td>
  <b>Entry Type</b>
  </td><td>
  <b>Test Type</b>
  </td><td>
  <b>Enabled</b>
	</td>
	<xsl:if test="//wsi-assertions:testAssertion/wsi-assertions:additionalInfo">
  <td>
  <b>Priority</b>
  </td><td>
  <b>Status</b>
  </td><td>
  <b>Needed By<br/>Sample Application WG</b>
  </td><td>
  <b>Target Release</b>
  </td>
  </xsl:if>
  </tr>
  <xsl:for-each select="wsi-assertions:testAssertion">
    <xsl:sort select="@id" data-type="text"/>
    <tr bgcolor="#ffffff"><td>
    <xsl:variable name="taID" select="@id"/>
    <a href="#{$taID}"><xsl:value-of select="@id"/></a>
    </td>
    <xsl:if test="//wsi-assertions:testAssertion/wsi-assertions:additionalInfo">
    <td>
    <xsl:value-of select="wsi-assertions:additionalInfo/@oldId"/>
    </td>
    </xsl:if>
    <td>
    <xsl:value-of select="@entryType"/>
    </td><td>
    <xsl:value-of select="@type"/>
    </td><td>
    <xsl:choose>
    <xsl:when test="@enabled='false'">
      <p style="color : {$highlightColor4};"><b>false</b></p>    
    </xsl:when>
    <xsl:otherwise>
      <p>true</p>    
    </xsl:otherwise>
    </xsl:choose>
  	</td>
  	<xsl:if test="//wsi-assertions:testAssertion/wsi-assertions:additionalInfo">
    <td>
    <xsl:value-of select="wsi-assertions:additionalInfo/@priority"/>
    </td><td>
    <xsl:variable name="status" select="wsi-assertions:additionalInfo/@status"/>
    <xsl:choose>
    <xsl:when test="$status='bothDone'">
      <p><xsl:value-of select="$status"/></p>    
    </xsl:when>
    <xsl:when test="$status='csDone'">
      <p style="color : {$highlightColor1}"><b><xsl:value-of select="$status"/></b></p>    
    </xsl:when>
    <xsl:when test="$status='javaDone'">
      <p style="color : {$highlightColor2}"><b><xsl:value-of select="$status"/></b></p>    
    </xsl:when>
    <xsl:when test="$status='proposedUpdate'">
      <p style="color : {$highlightColor3}"><b><xsl:value-of select="$status"/></b></p>    
    </xsl:when>
    <xsl:when test="$status='clarificationNeeded'">
      <p style="color : {$highlightColor3}"><b><xsl:value-of select="$status"/></b></p>    
    </xsl:when>
    <xsl:otherwise>
      <p style="color : {$highlightColor4}"><b><xsl:value-of select="$status"/></b></p>    
    </xsl:otherwise>
    </xsl:choose>
    </td><td>
    <xsl:value-of select="wsi-assertions:additionalInfo/@sampleApp"/>
    </td><td>
    <xsl:value-of select="wsi-assertions:additionalInfo/@targetRelease"/>
    </td>
    </xsl:if>
    </tr>
  </xsl:for-each>
  </table>

  <hr style="color : black;"/>
  <xsl:apply-templates select="wsi-assertions:testAssertion"/>
  <hr style="color : black;"/>

</xsl:template>

<xsl:template match="wsi-assertions:testAssertion" >
  <xsl:variable name="linkName" select="@id"/>
	<a name="#{$linkName}">
    <xsl:choose>
    <xsl:when test="wsi-assertions:referenceList/wsi-assertions:reference=''"/>
    <xsl:otherwise>
        <xsl:for-each select="wsi-assertions:referenceList/wsi-assertions:reference">
        <a name="reference{text()}"/>
		  </xsl:for-each>
    </xsl:otherwise>
    </xsl:choose>
  <xsl:choose>
  <xsl:when test="@enabled='false'">
    <h4 style="background-color : {$highlightColor4};">Test Assertion: <xsl:value-of select="@id"/>
    <!-- REMOVE: -->
   	<xsl:if test="//wsi-assertions:testAssertion/wsi-assertions:additionalInfo">
    [<xsl:value-of select="wsi-assertions:additionalInfo/@oldId"/>]
    </xsl:if>
    </h4>    
  </xsl:when>
  <xsl:otherwise>
    <h4>Test Assertion: <xsl:value-of select="@id"/>
    <!-- REMOVE: -->
   	<xsl:if test="//wsi-assertions:testAssertion/wsi-assertions:additionalInfo">
    [<xsl:value-of select="wsi-assertions:additionalInfo/@oldId"/>]
    </xsl:if>
    </h4>
  </xsl:otherwise>
  </xsl:choose>
  </a>
  
  <table cellpadding="4" bgcolor="#000000" cellspacing="1" valign="top" width="80%">
  <tbody>
    <tr  bgcolor="{$titleRowColor}">
      <td rowspan="2"><b>Entry Type</b></td>
      <td rowspan="2"><b>Test Type</b></td>
      <td rowspan="2"><b>Enabled</b></td>
      <td colspan="2"><b>Additional Entry Types</b></td>
      <td rowspan="2"><b>Prerequisites</b></td>
      <td colspan="2"><b>Profile Requirements</b></td>
    </tr>
    <tr bgcolor="{$titleRowColor}">
      <td><b>Message Input</b></td>
      <td><b>WSDL Input</b></td>
      <td><b>Target</b></td>
      <td><b>Collateral</b></td>
      <!-- REMOVE:
      <td><b>UDDI Input</b></td>
      -->
    </tr>
    <tr bgcolor="#ffffff">
      <td><xsl:value-of select="@entryType" /></td>
      <td><xsl:value-of select="@type" /></td>
      <td>
        <xsl:choose>
        <xsl:when test="@enabled='false'">
          <p style="color : {$highlightColor4};"><b>false</b></p>    
        </xsl:when>
        <xsl:otherwise>
          <p>true</p>    
        </xsl:otherwise>
        </xsl:choose>
      </td>
      <td>
        <xsl:choose>
        <xsl:when test="wsi-assertions:additionalEntryTypeList/wsi-assertions:messageInput=''">
          <p style="color: {$noteColor};"><b>NOTE: Need to define message input.</b></p>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="wsi-assertions:additionalEntryTypeList/wsi-assertions:messageInput"/>
        </xsl:otherwise>
        </xsl:choose>
      </td>
      <td>
        <xsl:choose>
        <xsl:when test="wsi-assertions:additionalEntryTypeList/wsi-assertions:wsdlInput=''">
          <p style="color: {$noteColor};"><b>NOTE: Need to define WSDL input.</b></p>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="wsi-assertions:additionalEntryTypeList/wsi-assertions:wsdlInput"/>
        </xsl:otherwise>
        </xsl:choose>
      </td>
<!-- REMOVE:
      <td>
        <xsl:choose>
        <xsl:when test="wsi-assertions:additionalEntryTypeList/wsi-assertions:uddiInput=''">
          <p style="color: {$noteColor};"><b>NOTE: Need to define UDDI input.</b></p>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="wsi-assertions:additionalEntryTypeList/wsi-assertions:uddiInput"/>
        </xsl:otherwise>
        </xsl:choose>
      </td>
-->
      <td>
        <xsl:choose>
        <xsl:when test="wsi-assertions:prereqList/wsi-assertions:testAssertionID">
          <xsl:for-each select="wsi-assertions:prereqList/wsi-assertions:testAssertionID">
            <a href="#{text()}"><xsl:value-of select="text()"/></a>
            <xsl:if test="position()!=last()">
              <br/>
            </xsl:if>
          </xsl:for-each>
        </xsl:when>
        <xsl:otherwise>
          <xsl:text>[Not specified]</xsl:text>
        </xsl:otherwise>
        </xsl:choose>
      </td>
      <td>
        <xsl:choose>
        <xsl:when test="wsi-assertions:referenceList/wsi-assertions:reference=''">
          <xsl:text>[Not specified]</xsl:text>
        </xsl:when>
        <xsl:otherwise>
          <xsl:for-each select="wsi-assertions:referenceList/wsi-assertions:reference[not(@role) or (@role = 'target')]">
            <a name="profile-{text()}"/><a href="{$profileLink}#{text()}"><xsl:value-of select="text()"/></a>
            <xsl:if test="@refid!=''">
              (<a href="#{@refid}"><xsl:value-of select="@refid"/></a>)
            </xsl:if>
            <xsl:if test="position()!=last()">
              <br/>
            </xsl:if>
    		  </xsl:for-each>
        </xsl:otherwise>
        </xsl:choose>
      </td>
      <td>
        <xsl:choose>
        <xsl:when test="wsi-assertions:referenceList/wsi-assertions:reference=''">
          <xsl:text>[Not specified]</xsl:text>
        </xsl:when>
        <xsl:otherwise>
          <xsl:for-each select="wsi-assertions:referenceList/wsi-assertions:reference[@role = 'collateral']">
            <a name="profile-{text()}"/><a href="{$profileLink}#{text()}"><xsl:value-of select="text()"/></a>
            <xsl:if test="@refid!=''">
              (<a href="#{@refid}"><xsl:value-of select="@refid"/></a>)
            </xsl:if>
            <xsl:if test="position()!=last()">
              <br/>
            </xsl:if>
    		  </xsl:for-each>
        </xsl:otherwise>
        </xsl:choose>
      </td>
    </tr>
  </tbody>
  </table>

  <br/>

	<p class="data-type"><b>Context:</b><br/>
	<span class="data-content"><xsl:value-of select="wsi-assertions:context" /></span></p>
	<p class="data-type"><b>Assertion Description:</b><br/>
	<span class="data-content">
	  <xsl:choose>
    <xsl:when test="wsi-assertions:assertionDescription=''">
      <p style="color: {$noteColor};"><b>NOTE: Need to define assertion description.</b></p>
    </xsl:when>
    <xsl:otherwise>
      <xsl:value-of select="wsi-assertions:assertionDescription"/>
    </xsl:otherwise>
    </xsl:choose>
  </span></p>
  <p class="data-type"><b>Failure Message:</b><br/>
  <span class="data-content">
    <xsl:choose>
      <xsl:when test="wsi-assertions:failureMessage=''">
        <p style="color: {$noteColor};"><b>NOTE: Need to define failure message.</b></p>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="wsi-assertions:failureMessage"/>
      </xsl:otherwise>
    </xsl:choose>
  </span></p>
  <p class="data-type"><b>Failure Detail Description:</b><br/>
  <span class="data-content">  
    <xsl:choose>
      <xsl:when test="wsi-assertions:failureDetailDescription=''">
        <xsl:text>[Not specified]</xsl:text>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="wsi-assertions:failureDetailDescription"/>
      </xsl:otherwise>
    </xsl:choose>
  </span></p>
  <p class="data-type"><b>Comments:</b><br/>
  <span class="data-content">  
    <xsl:choose>
    <xsl:when test="wsi-assertions:comments=''">
      <xsl:text>[Not specified]</xsl:text>
    </xsl:when>
    <xsl:when test="wsi-assertions:comments">
      <xsl:value-of select="wsi-assertions:comments"/>
    </xsl:when>
    <xsl:otherwise>
      <xsl:text>[Not specified]</xsl:text>
    </xsl:otherwise>
    </xsl:choose>
  </span></p>    
	<br/>
	
	<!--
	<table width="80%">
  <col span="1" width="20%"/>
  <col span="2" width="80%"/>
	<tr valign="top"><td>
	  <b>Context:</b>
  </td><td>
  <xsl:value-of select="wsi-assertions:context" />
  </td></tr>
  <tr></tr>
  <tr valign="top"><td>
  <b>Assertion<br/>Description:</b>
  </td><td>
    <xsl:choose>
    <xsl:when test="wsi-assertions:assertionDescription=''">
      <p style="color: {$noteColor};"><b>NOTE: Need to define assertion description.</b></p>
    </xsl:when>
    <xsl:otherwise>
      <xsl:value-of select="wsi-assertions:assertionDescription"/>
    </xsl:otherwise>
    </xsl:choose>
  </td></tr>
  <tr></tr>
  <tr valign="top"><td>
  <b>Failure<br/>Message:</b>
  </td><td>
    <xsl:choose>
      <xsl:when test="wsi-assertions:failureMessage=''">
        <p style="color: {$noteColor};"><b>NOTE: Need to define failure message.</b></p>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="wsi-assertions:failureMessage"/>
      </xsl:otherwise>
    </xsl:choose>
  </td></tr>
  <tr></tr>
  <tr valign="top"><td>
  <b>Failure Detail<br/>Description:</b>
  </td><td>
    <xsl:choose>
      <xsl:when test="wsi-assertions:failureDetailDescription=''">
        <xsl:text>[Not specified]</xsl:text>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="wsi-assertions:failureDetailDescription"/>
      </xsl:otherwise>
    </xsl:choose>
  </td></tr>
  <tr></tr>
  <tr valign="top"><td>
  <b>Comments:</b>
  </td><td>
    <xsl:choose>
    <xsl:when test="wsi-assertions:comments=''">
      <xsl:text>[Not specified]</xsl:text>
    </xsl:when>
    <xsl:when test="wsi-assertions:comments">
      <xsl:value-of select="wsi-assertions:comments"/>
    </xsl:when>
    <xsl:otherwise>
      <xsl:text>[Not specified]</xsl:text>
    </xsl:otherwise>
    </xsl:choose>
  </td></tr>
  </table>  
  -->
  <!--
  <table cellpadding="4" bgcolor="#000000" cellspacing="1" valign="top">
  <col span="1" width="20%"/>
  <col span="2" width="80%"/>
  <tbody>
  <tr bgcolor="#ffffff"><td>
  <b>Entry Type</b>
  </td><td>
  <xsl:value-of select="@entryType" />
  </td></tr>
  <tr bgcolor="#ffffff"><td>
  <b>Test Type</b>
  </td><td>
  <xsl:value-of select="@type" />
  </td></tr>
  <tr bgcolor="#ffffff"><td>
  <b>Enabled</b>
  </td><td>
  <xsl:choose>
  <xsl:when test="@enabled='false'">
    <p style="color : {$highlightColor4};"><b>false</b></p>    
  </xsl:when>
   <xsl:otherwise>
    <p>true</p>    
  </xsl:otherwise>
  </xsl:choose>
  </td></tr>
  <tr bgcolor="#ffffff"><td>
  <b>Context</b>
  </td><td>
  <xsl:value-of select="wsi-assertions:context" />
  </td></tr>
  <tr bgcolor="#ffffff"><td>
  <b>Assertion Description</b>
  </td><td>
    <xsl:choose>
    <xsl:when test="wsi-assertions:assertionDescription=''">
      <p style="color: {$noteColor};"><b>NOTE: Need to define assertion description.</b></p>
    </xsl:when>
    <xsl:otherwise>
      <xsl:value-of select="wsi-assertions:assertionDescription"/>
    </xsl:otherwise>
    </xsl:choose>
  </td></tr>
  <tr bgcolor="#ffffff"><td>
  <b>Failure Message</b>
  </td><td>
    <xsl:choose>
      <xsl:when test="wsi-assertions:failureMessage=''">
        <p style="color: {$noteColor};"><b>NOTE: Need to define failure message.</b></p>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="wsi-assertions:failureMessage"/>
      </xsl:otherwise>
    </xsl:choose>
  </td></tr>
  <tr bgcolor="#ffffff"><td>
  <b>Failure Detail Description</b>
  </td><td>
    <xsl:choose>
      <xsl:when test="wsi-assertions:failureDetailDescription=''">
        <xsl:text>[Not specified]</xsl:text>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="wsi-assertions:failureDetailDescription"/>
      </xsl:otherwise>
    </xsl:choose>
  </td></tr>
  <xsl:if test="wsi-assertions:additionalEntryTypeList/wsi-assertions:logInput">
  <tr bgcolor="#ffffff"><td>
  <b>Addtional Entry Type:<br/>Log Input</b>
  </td><td>
    <xsl:choose>
    <xsl:when test="wsi-assertions:additionalEntryTypeList/wsi-assertions:logInput=''">
      <p style="color: {$noteColor};"><b>NOTE: Need to define log input.</b></p>
    </xsl:when>
    <xsl:otherwise>
      <xsl:value-of select="wsi-assertions:additionalEntryTypeList/wsi-assertions:logInput"/>
    </xsl:otherwise>
    </xsl:choose>
  </td></tr>
  </xsl:if>
  <xsl:if test="wsi-assertions:additionalEntryTypeList/wsi-assertions:wsdlInput">
  <tr bgcolor="#ffffff"><td>
  <b>Additional Entry Type:<br/>WSDL Input</b>
  </td><td>
    <xsl:choose>
    <xsl:when test="wsi-assertions:additionalEntryTypeList/wsi-assertions:wsdlInput=''">
      <p style="color: {$noteColor};"><b>NOTE: Need to define WSDL input.</b></p>
    </xsl:when>
    <xsl:otherwise>
      <xsl:value-of select="wsi-assertions:additionalEntryTypeList/wsi-assertions:wsdlInput"/>
    </xsl:otherwise>
    </xsl:choose>
  </td></tr>
  </xsl:if>
  <xsl:if test="wsi-assertions:additionalEntryTypeList/wsi-assertions:uddiInput">
  <tr bgcolor="#ffffff"><td>
  <b>Additional Entry Type:<br/>UDDI Input</b>
  </td><td>
    <xsl:choose>
      <xsl:when test="wsi-assertions:additionalEntryTypeList/wsi-assertions:uddiInput=''">
        <p style="color: {$noteColor};"><b>NOTE: Need to define UDDI input.</b></p>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="wsi-assertions:additionalEntryTypeList/wsi-assertions:uddiInput"/>
      </xsl:otherwise>
    </xsl:choose>
  </td></tr>
  </xsl:if>
  <tr bgcolor="#ffffff"><td>
  <b>Prereqs</b>
  </td><td>                                                             
    <xsl:choose>
    <xsl:when test="wsi-assertions:prereqList/wsi-assertions:testAssertionID">
      <xsl:for-each select="wsi-assertions:prereqList/wsi-assertions:testAssertionID">
        <a href="#{text()}"><xsl:value-of select="text()"/></a>
        <xsl:if test="position()!=last()">
          <xsl:text>, </xsl:text>
        </xsl:if>
      </xsl:for-each>
    </xsl:when>
    <xsl:otherwise>
      <xsl:text>[Not specified]</xsl:text>
    </xsl:otherwise>
    </xsl:choose>
	</td></tr>
  <tr bgcolor="#ffffff"><td>
  <b>Profile References</b>
  </td><td>                                                             
    <xsl:choose>
    <xsl:when test="wsi-assertions:referenceList/wsi-assertions:reference=''">
      <xsl:text>[Not specified]</xsl:text>
    </xsl:when>
    <xsl:otherwise>
        <xsl:for-each select="wsi-assertions:referenceList/wsi-assertions:reference">
        <a name="profile-{text()}"/><a href="{$profileLink}#{text()}"><xsl:value-of select="text()"/></a>
        <xsl:if test="@refid!=''">
          (<a href="#{@refid}"><xsl:value-of select="@refid"/></a>)
        </xsl:if>
        <xsl:if test="position()!=last()">
          <xsl:text>, </xsl:text>
        </xsl:if>
		  </xsl:for-each>
    </xsl:otherwise>
    </xsl:choose>
	</td></tr>
  <tr bgcolor="#ffffff"><td>
  <b>Comments</b>
  </td><td>
    <xsl:choose>
    <xsl:when test="wsi-assertions:comments">
      <xsl:value-of select="wsi-assertions:comments"/>
    </xsl:when>
    <xsl:otherwise>
      <xsl:text>[Not specified]</xsl:text>
    </xsl:otherwise>
    </xsl:choose>
  </td></tr>
  </tbody>
  </table>
-->
  <p><a href="#top">Return to top of document.</a></p>
  <p/>
</xsl:template>

</xsl:stylesheet>
