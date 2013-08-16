<?xml version="1.0" encoding="UTF-8"?>
<kml xmlns="http://www.opengis.net/kml/2.2">
  <Document>
    <Schema name="Overall" id="Overall">
      <#list analysis.analysisType.parameterTypes as parameterType>
      <SimpleField name="${parameterType.identifier}" type="${parameterType.dataType}">
        <displayName>${parameterType.displayName}</displayName>
      </SimpleField>
      </#list>
      <#list analysis.analysisType.overallResultAttributeTypes as resultAttributeType>
      <SimpleField name="${resultAttributeType.identifier}" type="${resultAttributeType.dataType}">
        <displayName>${resultAttributeType.displayName}</displayName>
      </SimpleField>
      </#list>
    </Schema>
    <ExtendedData>
      <SchemaData schemaUrl="#Overall">
        <#list analysis.analysisType.parameterTypes as parameterType>
        <SimpleData name="${parameterType.identifier}"><#rt>
        <#if analysis.getParameterValue(parameterType.identifier, false)??><#t>
        ${analysis.getParameterValue(parameterType.identifier, false)?string}<#t>
        </#if><#t>
        </SimpleData><#lt>
        </#list>
        <#list analysis.analysisType.overallResultAttributeTypes as resultAttributeType>
        <SimpleData name="${resultAttributeType.identifier}"><#rt>
        <#if (analysis.getResultAttributeValue(resultAttributeType.identifier)??)><#t>
        ${analysis.getResultAttributeValue(resultAttributeType.identifier)?c}<#t>
        </#if><#t>
        </SimpleData><#lt>
        </#list>
      </SchemaData>
    </ExtendedData>
    <name>${analysis.analysisType.displayName}</name>
    <description>
      <![CDATA[
        <div style="min-width: 500px;">
        <p>Generated by <a href="${baseUrl}/">OzTrack</a></p>
        <p><a href="${baseUrl}/projects/${analysis.project.id}">${analysis.project.title}</a></p>
        <#if (analysis.analysisType.parameterTypes?size > 0)>
        <table style="float: left; margin-right: 20px; border-collapse: collapse;">
          <tr>
            <th style="border: 2px ridge; padding: 2px 4px; min-width: 100px; text-align: left; background-color: #ddd;">Parameter</th>
            <th style="border: 2px ridge; padding: 2px 4px; min-width: 100px; text-align: left; background-color: #ddd;">Value</th>
          </tr>
          <#list analysis.analysisType.parameterTypes as parameterType>
          <tr>
            <td style="border: 2px ridge; padding: 2px 4px;">$[Overall/${parameterType.identifier}/displayName]</td>
            <td style="border: 2px ridge; padding: 2px 4px;">$[Overall/${parameterType.identifier}]</td>
          </tr>
          </#list>
        </table>
        </#if>
        <#if (analysis.analysisType.overallResultAttributeTypes?size > 0)>
        <table style="float: left; border-collapse: collapse;">
          <tr>
            <th style="border: 2px ridge; padding: 2px 4px; min-width: 100px; text-align: left; background-color: #ddd;">Attribute</th>
            <th style="border: 2px ridge; padding: 2px 4px; min-width: 100px; text-align: left; background-color: #ddd;">Value</th>
          </tr>
          <#list analysis.analysisType.overallResultAttributeTypes as resultAttributeType>
          <tr>
            <td style="border: 2px ridge; padding: 2px 4px;">$[Overall/${resultAttributeType.identifier}/displayName]</td>
            <td style="border: 2px ridge; padding: 2px 4px;">$[Overall/${resultAttributeType.identifier}]</td>
          </tr>
          </#list>
        </table>
        </#if>
        <div style="clear: both;"></div>
        </div>
      ]]>
    </description>
    <open>1</open>
    <Schema name="Feature" id="Feature">
      <SimpleField name="animalId" type="string">
        <displayName>Animal ID</displayName>
      </SimpleField>
      <SimpleField name="animalName" type="string">
        <displayName>Animal name</displayName>
      </SimpleField>
      <#list analysis.analysisType.featureResultAttributeTypes as resultAttributeType>
      <SimpleField name="${resultAttributeType.identifier}" type="${resultAttributeType.dataType}">
        <displayName>${resultAttributeType.displayName}</displayName>
      </SimpleField>
      </#list>
    </Schema>
    <#list analysis.animals as animal>
    <Style id="animal-${animal.id?c}">
      <BalloonStyle>
        <text>
          <![CDATA[
            <p style="font-weight: bold;">$[name]</p>
            <p>Animal: <a href="${baseUrl}/animals/$[Feature/animalId]">$[Feature/animalName]</a></p>
            <#if (analysis.analysisType.featureResultAttributeTypes?size > 0)>
            <table style="border-collapse: collapse;">
              <tr>
                <th style="border: 2px ridge; padding: 2px 4px; min-width: 100px; text-align: left; background-color: #ddd;">Attribute</th>
                <th style="border: 2px ridge; padding: 2px 4px; min-width: 100px; text-align: left; background-color: #ddd;">Value</th>
              </tr>
              <#list analysis.analysisType.featureResultAttributeTypes as resultAttributeType>
              <tr>
                <td style="border: 2px ridge; padding: 2px 4px;">$[Feature/${resultAttributeType.identifier}/displayName]</td>
                <td style="border: 2px ridge; padding: 2px 4px;">$[Feature/${resultAttributeType.identifier}]</td>
              </tr>
              </#list>
            </table>
            </#if>
          ]]>
        </text>
      </BalloonStyle>
      <#assign animalKmlColour="${animal.colour?substring(5,7)}${animal.colour?substring(3,5)}${animal.colour?substring(1,3)}"/>
      <LineStyle>
        <color>cc${animalKmlColour}</color>
        <width>2</width>
      </LineStyle>
      <PolyStyle>
        <color>7f${animalKmlColour}</color>
        <fill><#if fill>1<#else>0</#if></fill>
        <outline>1</outline>
      </PolyStyle>
    </Style>
    </#list>
    <#list analysis.resultFeatures as resultFeature>
    <Placemark>
      <styleUrl>#animal-${resultFeature.animal.id?c}</styleUrl>
      <name>${resultFeature.animal.animalName}</name>
      <visibility>1</visibility>
      <ExtendedData>
        <SchemaData schemaUrl="#Feature">
          <SimpleData name="animalId">${resultFeature.animal.id?c}</SimpleData>
          <SimpleData name="animalName">${resultFeature.animal.animalName}</SimpleData>
          <#list analysis.analysisType.featureResultAttributeTypes as resultAttributeType>
          <SimpleData name="${resultAttributeType.identifier}">${resultFeature.getAttributeValue(resultAttributeType.identifier)?c}</SimpleData>
          </#list>
        </SchemaData>
      </ExtendedData>
      <MultiGeometry>
        <#list 0..(resultFeature.geometry.numGeometries - 1) as n>
        <Polygon>
          <outerBoundaryIs>
            <LinearRing>
              <coordinates>
                <#list resultFeature.geometry.getGeometryN(n).coordinates as coordinate>
                ${coordinate.x?c},${coordinate.y?c}
                </#list>
              </coordinates>
            </LinearRing>
          </outerBoundaryIs>
        </Polygon>
        </#list>
      </MultiGeometry>
    </Placemark>
    </#list>
  </Document>
</kml>