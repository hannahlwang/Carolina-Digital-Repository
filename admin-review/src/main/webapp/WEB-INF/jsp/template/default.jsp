<%--

    Copyright 2008 The University of North Carolina at Chapel Hill

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

            http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

--%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles"%>
<?xml version="1.0"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html>
<head>
	<tiles:insertAttribute name="headElements" />
	<title>
		CDR Administration <c:if test="${not empty pageSubtitle}"> - <c:out value="${pageSubtitle}"/></c:if>
	</title>
</head>
<body>
<div id="pagewrap">
	<div id="pagewrap_inside">
		<tiles:insertAttribute name="header" />
		<div id="content">
			<c:choose>
				<tiles:insertAttribute name="body" />
			</c:choose>
		</div>
		<tiles:insertAttribute name="footer" />
	</div>
</div>
</body>
</html>