<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@ page import="org.oztrack.data.model.types.MapQueryType" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="tags" %>
<tags:page title="${project.title}: Data Cleansing">
    <jsp:attribute name="head">
        <link rel="stylesheet" href="<c:url value="/js/openlayers/theme/default/style.css"/>" type="text/css">
        <link rel="stylesheet" href="<c:url value="/js/openlayers/theme/default/google.css"/>" type="text/css">
        <style type="text/css">
            ul#cleanse-list {
                margin: 10px 0;
                padding-left: 0;
            }
            ul#cleanse-list li {
                margin: 2px 0;
                list-style: none;
                background-repeat: no-repeat;
                background-position: left middle;
                line-height: 22px;
                padding: 0 0 5px 28px;
            }
            ul#cleanse-list li {
                background-image: url(/js/openlayers/theme/default/img/draw_polygon_on.png);
            }
        </style>
        <script src="http://maps.google.com/maps/api/js?v=3.9&sensor=false"></script>
        <script type="text/javascript" src="${pageContext.request.contextPath}/js/proj4js/proj4js-compressed.js"></script>
        <script type="text/javascript" src="${pageContext.request.contextPath}/js/openlayers/OpenLayers.js"></script>
        <script type="text/javascript" src="${pageContext.request.contextPath}/js/openlayers/LoadingPanel.js"></script>
        <script type="text/javascript" src="<c:url value="/js/project-cleanse.js"/>"></script>
        <script type="text/javascript">
            function submitCleanseForm(operation) {
                jQuery('.cleanse-response').hide();
                jQuery.ajax({
                    url: '<c:url value="/projects/${project.id}/cleanse"/>',
                    type: 'POST',
                    data: 'operation=' + operation + '&' + jQuery('#cleanseForm').serialize(),
                    success: function(data, textStatus, jqXHR) {
                        cleanseMap.reset();
                        if (operation == 'delete') {
                            var numDeleted = jQuery(data).find('num-deleted').text();
                            jQuery('#cleanse-response-deleted').text(numDeleted + " points deleted").fadeIn();
                        }
                        else if ((operation == 'undelete') || (operation == 'undelete-all')) {
                            var numUndeleted = jQuery(data).find('num-undeleted').text();
                            jQuery('#cleanse-response-undeleted').text(numUndeleted + " points restored").fadeIn();
                        }
                    },
                    error: function(jqXHR, textStatus, errorThrown) {
                        var message = jQuery(jqXHR.responseXML).find('error').text() || 'Error processing request';
                        jQuery('#cleanse-response-error').text(message).fadeIn();
                    }
                });
                return false;
            }
            $(document).ready(function() {
                $('#navTrack').addClass('active');
                $("#projectMapOptions").accordion({fillSpace: true});
                cleanseMap = createCleanseMap('projectMap', {
                    projectId: <c:out value="${project.id}"/>,
                    onReset: function() {
                        jQuery('#cleanse-select').children().remove();
                        jQuery('#cleanse-list').children().remove();
                    },
                    onPolygonFeatureAdded: function(id, title, wkt) {
                        jQuery('#cleanse-list').append(
                            jQuery('<li>')
                                .attr('id', 'cleanse-li-' + id)
                                .append(title)
                                .append(' (')
                                .append(
                                    jQuery('<a>')
                                        .attr('href', 'javascript:void(0)')
                                        .attr('onclick', 'cleanseMap.deletePolygonFeature(\'' + id + '\');')
                                        .attr('onmouseover', 'cleanseMap.selectPolygonFeature(\'' + id + '\', true);')
                                        .attr('onmouseout', 'cleanseMap.selectPolygonFeature(\'' + id + '\', false);')
                                        .append('unselect')
                                )
                                .append(')')
                        );
                        jQuery('#cleanse-select').append(
                            jQuery('<option>')
                                .attr('id', 'cleanse-option-' + id)
                                .attr('value', wkt)
                                .attr('selected', 'selected')
                                .append(title)
                        );
                    },
                    onDeletePolygonFeature: function(id) {
                        jQuery('*[id=\'cleanse-li-' + id + '\']').remove();
                        jQuery('*[id=\'cleanse-option-' + id + '\']').remove();
                    }
                });
            });
        </script>
    </jsp:attribute>
    <jsp:attribute name="breadcrumbs">
        <a href="<c:url value="/"/>">Home</a>
        &rsaquo; <a href="<c:url value="/projects"/>">Animal Tracking</a>
        &rsaquo; <a href="<c:url value="/projects/${project.id}"/>">${project.title}</a>
        &rsaquo; <span class="active">Data Cleansing</span>
    </jsp:attribute>
    <jsp:body>
        <div class="mapTool">
        <div id="projectMapOptions">
            <h3 id="projectTitle"><a href="#"><c:out value="${project.title}"/></a></h3>
            <div style="padding: 0;">
                <form id="cleanseForm" onsubmit="return false;">
                <p style="font-weight: bold;">Data cleansing</p>
                <p style="font-size: 0.9em; font-style: italic;">
                    Select points for removal from the project by drawing polygons around them.
                    Click to start drawing and click again to draw each side of your selected area.
                    Double-click to finish drawing. You can draw as many polygons as are required.
                </p>
                <select id="cleanse-select" name="polygon" multiple="multiple" style="display: none;">
                </select>
                <ul id="cleanse-list">
                </ul>
                <p id="cleanse-response-deleted" class="cleanse-response" style="font-weight: bold; color: red;"></p>
                <p id="cleanse-response-undeleted" class="cleanse-response" style="font-weight: bold; color: green;"></p>
                <p id="cleanse-response-error" class="cleanse-response" style="font-weight: bold; color: gray;"></p>
                <div style="margin: 1em 0; padding: 0;">
                    <button onclick="submitCleanseForm('delete');">Delete selected</button>
                    <button onclick="submitCleanseForm('undelete');">Restore selected</button>
                </div>
                <div style="margin: 1em 0; padding: 0;">
                    <button onclick="submitCleanseForm('undelete-all');">Restore all points</button>
                </div>
                </form>
            </div>
            <h3><a href="#">Project Menu</a></h3>
            <tags:project-menu project="${project}"/>
        </div>
        <div id="projectMap"></div>
        <div class="clearboth">&nbsp;</div>
        </div>
    </jsp:body>
</tags:page>
