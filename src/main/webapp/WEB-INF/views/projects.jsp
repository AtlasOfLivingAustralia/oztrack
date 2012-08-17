<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="tags" %>
<tags:page title="Projects">
    <jsp:attribute name="head">
        <script type="text/javascript"> 
            $(document).ready(function() {
                $('#navTrack').addClass('active');
            });
        </script>
    </jsp:attribute>
    <jsp:attribute name="breadcrumbs">
        <a href="<c:url value="/"/>">Home</a>
        &rsaquo; <span class="active">Projects</span>
    </jsp:attribute>
    <jsp:attribute name="sidebar">
        <c:if test="${currentUser != null}">
        <div class="sidebarMenu">
            <ul>
                <li><a href="<c:url value="/projects/new"/>">Create New Project</a></li>
            </ul>
        </div>
        </c:if>
    </jsp:attribute>
    <jsp:body>
        <h1>Projects</h1>
        <c:if test="${currentUser != null}">
            <c:choose>
            <c:when test="${(empty currentUser.projectUsers)}">
                <p>You have no projects to work with yet. You might like to <a href="<c:url value='/projects/new'/>">add a project</a>.</p>
            </c:when>
            <c:otherwise>
                <h2>My Projects</h2>
                <p>
                    You have access to <c:out value="${fn:length(currentUser.projectUsers)}"/> projects.
                    Select a project to work with from the list below, or <a href="<c:url value='/projects/new'/>">create a new project</a>.
                </p>
                <table class="table table-bordered table-condensed">
                    <col style="width: 250px;" />
                    <col style="width: 150px;" />
                    <col style="width: 125px;" />
                    <col style="width: 100px;" />
                    <thead>
                        <tr>
                            <th>Title</th>
                            <th>Spatial Coverage</th>
                            <th>Project Type</th>
                            <th>Created Date</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach items="${currentUser.projectUsers}" var="projectUser">
                        <tr>
                            <td><a href="<c:url value="/projects/${projectUser.project.id}"/>"><c:out value="${projectUser.project.title}"/></a></td>
                            <td><c:out value="${projectUser.project.spatialCoverageDescr}"/></td>
                            <td><c:out value="${projectUser.project.projectType.displayName}"/></td>
                            <td><fmt:formatDate value="${projectUser.project.createDate}" type="date" dateStyle="long"/></td>
                        </tr>
                        </c:forEach>
                    </tbody>
                </table>
            </c:otherwise>
            </c:choose>
        </c:if>
        <h2>Open Access Projects</h2>
        <tags:projects-table projects="${publicProjects}" adjective="open access"/>
        <h2>Restricted Access Projects</h2>
        <tags:projects-table projects="${privateProjects}" adjective="restricted access"/>
    </jsp:body>
</tags:page>
