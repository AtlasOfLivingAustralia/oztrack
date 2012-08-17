<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="tags" %>
<tags:page title="${project.title}: Update Animal Details">
    <jsp:attribute name="head">
        <script type="text/javascript"> 
            $(document).ready(function() {
            	$('#navTrack').addClass('active');
            });
        </script>
    </jsp:attribute>
    <jsp:attribute name="breadcrumbs">
        <a href="<c:url value="/"/>">Home</a>
        &rsaquo; <a href="<c:url value="/projects"/>">Animal Tracking</a>
        &rsaquo; <a href="<c:url value="/projects/${project.id}"/>">${project.title}</a>
        &rsaquo; <a href="<c:url value="/projects/${project.id}/animals"/>">Animals</a>
        &rsaquo; <a href="<c:url value="/animals/${animal.id}"/>">${animal.animalName}</a>
        &rsaquo; <span class="active">Edit</span> 
    </jsp:attribute>
    <jsp:attribute name="sidebar">
        <tags:project-menu project="${project}"/>
    </jsp:attribute>
    <jsp:body>
		<h1 id="projectTitle"><c:out value="${project.title}"/></h1>
		<form:form cssClass="form-horizontal" action="/animals/${animal.id}" commandName="animal" method="PUT">
            <fieldset>
            <legend>Update animal details</legend>
			<div class="control-group">
				<label class="control-label" for="projectAnimalId">Animal ID</label>
                <div class="controls">
    				<form:input path="projectAnimalId" id="projectAnimalId"/>
                    <span class="help-inline">
    				    <form:errors path="projectAnimalId" cssClass="formErrors"/>
                    </span>
                </div>
			</div>
			<div class="control-group">
				<label class="control-label" for="animalName">Name</label>
                <div class="controls">
    				<form:input path="animalName" id="animalName"/>
                    <span class="help-inline">
    				    <form:errors path="animalName" cssClass="formErrors"/>
                    </span>
                </div>
			</div>
			<div class="control-group">
				<label class="control-label" for="animalDescription">Description</label>
                <div class="controls">
    				<form:input path="animalDescription" id="animalDescription"/>
                    <span class="help-inline">
    				    <form:errors path="animalDescription" cssClass="formErrors"/>
                    </span>
                </div>
			</div>
			<div class="control-group">
				<label class="control-label" for="speciesName">Species</label>
                <div class="controls">
    				<form:input path="speciesName" id="speciesName"/>
                    <span class="help-inline">
    				    <form:errors path="speciesName" cssClass="formErrors"/>
                    </span>
                </div>
			</div>
			<div class="control-group">
				<label class="control-label" for="transmitterTypeCode">Transmitter Type Code</label>
                <div class="controls">
    				<form:input path="transmitterTypeCode" id="transmitterTypeCode"/>
                    <span class="help-inline">
    				    <form:errors path="transmitterTypeCode" cssClass="formErrors"/>
                    </span>
                </div>
			</div>
			<div class="control-group">
				<label class="control-label" for="transmitterId">Transmitter ID</label>
                <div class="controls">
    				<form:input path="transmitterId" id="transmitterId"/>
                    <span class="help-inline">
    				    <form:errors path="transmitterId" cssClass="formErrors"/>
                    </span>
                </div>
			</div>
			<div class="control-group">
				<label class="control-label" for="pingIntervalSeconds">Ping Interval (seconds)</label>
                <div class="controls">
    				<form:input path="pingIntervalSeconds" id="pingIntervalSeconds"/>
                    <span class="help-inline">
    				    <form:errors path="pingIntervalSeconds" cssClass="formErrors"/>
                    </span>
                </div>
			</div>
            </fieldset>
			<div class="form-actions">
                <input class="btn btn-primary" type="submit" value="Update"/>
                <a class="btn" href="<c:url value="/animals/${animal.id}"/>">Cancel</a>
            </div>
		</form:form>
    </jsp:body>
</tags:page>
