package org.oztrack.controller;

import java.util.Arrays;
import java.util.HashSet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.oztrack.data.access.OaiPmhEntityProducer;
import org.oztrack.data.access.OaiPmhSetDao;
import org.oztrack.data.model.types.OaiPmhSet;
import org.oztrack.util.OaiPmhException;
import org.oztrack.view.OaiPmhListSetsView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.View;

// Implements ListSets verb request handling
// http://www.openarchives.org/OAI/2.0/openarchivesprotocol.htm#ListSets
@Controller
public class OaiPmhListSetsController extends OaiPmhController {
    @Autowired
    private OaiPmhSetDao setDao;

    @RequestMapping(value="/oai-pmh", method={RequestMethod.GET, RequestMethod.POST}, produces="text/xml", params="verb=ListSets")
    public View handleRequest(HttpServletRequest request, HttpServletResponse response) throws OaiPmhException {
        super.preHandleRequest(request, response);

        // Return badArgument error code if request includes illegal arguments.
        // http://www.openarchives.org/OAI/2.0/openarchivesprotocol.htm#ErrorConditions
        HashSet<String> legalArguments = new HashSet<String>(Arrays.asList("verb", "resumptionToken"));
        if (!legalArguments.containsAll(request.getParameterMap().keySet())) {
            throw new OaiPmhException("badArgument", "Request includes illegal arguments.");
        }

        String resumptionToken = request.getParameter("resumptionToken");
        if (resumptionToken != null) {
            throw new OaiPmhException("badResumptionToken", "resumptionToken is invalid or expired.");
        }

        OaiPmhEntityProducer<OaiPmhSet> sets = setDao.getSets();
        if (!sets.iterator().hasNext()) {
            throw new OaiPmhException("noSetHierarchy", "This repository does not support sets.");
        }

        return new OaiPmhListSetsView(sets);
    }
}
