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

<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a" %>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r" %>

<%@ page buffer="16kb" contentType="text/html;charset=UTF-8" %>
<%@ page isELIgnored="false" %>
<%@ page import="org.alfresco.web.app.Application" %>

<body style="background-image: url(<%=request.getContextPath()%>/scripts/vieweditor/images/europa-bg.png); background-repeat: no-repeat; background-attachment: fixed; background-color:#000000;">

<r:page titleId="title_noaccess">


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
<span class='mainSubTitle'><%=Application.getMessage(session, "no_access")%></span>
</td>
</tr>

</table>

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

</r:page>

</body>
