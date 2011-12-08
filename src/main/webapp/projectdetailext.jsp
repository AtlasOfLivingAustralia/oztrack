<%@ include file="header.jsp" %>
<script src="http://maps.google.com/maps/api/js?v=3.2&sensor=false"></script>
<script type="text/javascript" src="js/openlayers/OpenLayers.js"></script>
<script type="text/javascript" src="js/coveragemap.js"></script>

<h1 id="projectTitle"><c:out value="${project.title}"/></h1>

<h2>Project Details</h2>

<table class="projectListTable">
<tr><td class="projectFieldName">Title:</td><td><c:out value="${project.title}"/></td></tr>
<tr><td class="projectFieldName">Description:</td><td><c:out value="${project.description}"/></td></tr>
<tr><td class="projectFieldName">Project Type:</td><td><c:out value="${project.projectType.displayName}"/></td></tr>
<tr><td class="projectFieldName">Species:</td><td><c:out value="${project.speciesCommonName}"/>
	<c:if test="${!empty project.speciesScientificName}"><i><br><c:out value="${project.speciesScientificName}"/></i></c:if>
</td></tr>
<tr><td class="projectFieldName">Temporal Coverage:</td><td>
	<c:choose>
	<c:when test="${empty project.firstDetectionDate}">
		No data has been uploaded for this project yet.
	</c:when>
	<c:otherwise>
		<fmt:formatDate pattern="${shortDateFormatPattern}" value="${project.firstDetectionDate}"/> to <fmt:formatDate pattern="${shortDateFormatPattern}" value="${project.lastDetectionDate}"/>
	</c:otherwise>
	</c:choose>
	</td></tr>
<tr><td class="projectFieldName">Spatial Coverage:</td><td><c:out value="${project.spatialCoverageDescr}"/><br/>
<div id="coverageMap" style="width:240px;height:200px;"></div>
</td></tr>

<tr><td class="projectFieldName">Contact:</td><td><c:out value="${project.contactGivenName}"/>&nbsp;<c:out value="${project.contactFamilyName}"/><br><c:out value="${project.contactEmail}"/><br><c:out value="${project.contactUrl}"/></td></tr>
<tr><td class="projectFieldName">Contact Organisation:</td><td><c:out value="${project.contactOrganisation}"/><br><c:out value="${project.contactUrl}"/></td></tr>
<tr><td class="projectFieldName">Publications:</td><td><i><c:out value="${project.publicationTitle}"/></i><br> <c:out value="${project.publicationUrl}"/></td></tr>

</table>

<span id="bbWKT"><c:out value="${project.boundingBox}"/></span>
<p><a href="home">Return to Home page</a></p>

<%@ include file="footer.jsp" %>
