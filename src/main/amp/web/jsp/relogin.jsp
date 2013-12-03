<%--
* Copyright (C) 2005-2010 Alfresco Software Limited.
*
* This file is part of Alfresco
*
* Alfresco is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* Alfresco is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
--%>
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h" %>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a" %>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r" %>

<%@ page import="org.alfresco.web.app.servlet.AuthenticationHelper" %>
<%@ page import="javax.servlet.http.Cookie" %>

<%@ page buffer="16kb" contentType="text/html;charset=UTF-8" %>
<%@ page isELIgnored="false" %>

<%
// remove the username cookie value if explicit logout was requested by the user
if (session.getAttribute(AuthenticationHelper.SESSION_INVALIDATED) != null)
{
Cookie authCookie = AuthenticationHelper.getAuthCookie(request);
if (authCookie != null)
{
authCookie.setMaxAge(0);
response.addCookie(authCookie);
}
}
%>

<body style="background-image: url(<%=request.getContextPath()%>/scripts/vieweditor/images/europa-bg.png); background-repeat: no-repeat; background-attachment: fixed; background-color:#000000;">

<r:page titleId="title_relogin">

<f:view>
<%-- load a bundle of properties I18N strings here --%>
<r:loadBundle var="msg"/>

<h:form acceptcharset="UTF-8" id="loggedOutForm" >

<table width=100% height=90% align=center>
<tr width=100% align=center>
<td valign=middle align=center width=100%>

<table cellspacing=0 cellpadding=0 border=0>
<tr><td width=7><img src='<%=request.getContextPath()%>/images/parts/white_01.gif' width=7 height=7 alt=''></td>
<td background='<%=request.getContextPath()%>/images/parts/white_02.gif'>
<img src='<%=request.getContextPath()%>/images/parts/white_02.gif' width=7 height=7 alt=''></td>
<td width=7><img src='<%=request.getContextPath()%>/images/parts/white_03.gif' width=7 height=7 alt=''></td>
</tr>
<tr><td background='<%=request.getContextPath()%>/images/parts/white_04.gif'>
<img src='<%=request.getContextPath()%>/images/parts/white_04.gif' width=7 height=7 alt=''></td><td bgcolor='white'>

<table border=0 cellspacing=4 cellpadding=2>
<tr>
<td align=center>
<img src='<%=request.getContextPath()%>/scripts/vieweditor/images/europa-icon.png' width=60 height=52 alt="Europa EMS" title="Europa EMS">
</td>
</tr>

<tr>
<td align=center>
<span class='mainSubTitle'><h:outputText value="#{msg.loggedout_details}" /></span>
</td>
</tr>

<tr>
<td align=center>
<a:actionLink href="/faces/jsp/browse/browse.jsp" value="#{msg.relogin}" />
</td>
</tr>
</table>

<%-- messages tag to show messages not handled by other specific message tags --%>
<h:messages style="padding-top:8px; color:red; font-size:10px" layout="table" />

</td><td background='<%=request.getContextPath()%>/images/parts/white_06.gif'>
<img src='<%=request.getContextPath()%>/images/parts/white_06.gif' width=7 height=7 alt=''></td></tr>
<tr><td width=7><img src='<%=request.getContextPath()%>/images/parts/white_07.gif' width=7 height=7 alt=''></td>
<td background='<%=request.getContextPath()%>/images/parts/white_08.gif'>
<img src='<%=request.getContextPath()%>/images/parts/white_08.gif' width=7 height=7 alt=''></td>
<td width=7><img src='<%=request.getContextPath()%>/images/parts/white_09.gif' width=7 height=7 alt=''></td></tr>
</table>

</td>
</tr>
</table>

</h:form>
</f:view>

</r:page>

</body>
