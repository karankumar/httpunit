package com.meterware.httpunit;

import java.net.URL;

import java.util.Vector;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This class represents a link in an HTML page. Users of this class may examine the 
 * structure of the link (as a DOM), or create a {@tag WebRequest} to simulate clicking
 * on the link.
 **/
public class WebLink {


    /**
     * Creates and returns a web request which will simulate clicking on this link.
     **/
    public WebRequest getRequest() {
        return new GetMethodWebRequest( _baseURL, getURLString(), getTarget() );
    }


    /**
     * Returns the URL referenced by this link. This may be a relative URL.
     **/
    public String getURLString() {
        return getValue( _node.getAttributes().getNamedItem( "href" ) );
    }


    /**
     * Returns the target for this link.
     **/
    public String getTarget() {
        if (getSpecifiedTarget().length() == 0) {
            return _parentTarget;
        } else if (getSpecifiedTarget().equalsIgnoreCase( "_self" )) {
            return _parentTarget;
        } else {
            return getSpecifiedTarget();
        }
    }


    private String getSpecifiedTarget() {
        return NodeUtils.getNodeAttribute( _node, "target" );
    }


    /**
     * Returns a copy of the domain object model subtree associated with this link.
     **/
    public Node getDOMSubtree() {
        return _node.cloneNode( /* deep */ true );
    }


    /**
     * Returns the text value of this link.
     **/
    public String asText() {
        if (!_node.hasChildNodes()) {
            return "";
        } else {
            return NodeUtils.asText( _node.getChildNodes() );
        }
    }


//---------------------------------- package members --------------------------------


    /**
     * Contructs a web link given the URL of its source page and the DOM extracted
     * from that page.
     **/
    WebLink( URL baseURL, String parentTarget, Node node ) {
        if (node == null) throw new IllegalArgumentException( "node must not be null" );
        _node         = node;
        _baseURL      = baseURL;
        _parentTarget = parentTarget;
    }


//---------------------------------- private members --------------------------------


    /** The URL of the page containing this link. **/
    private URL            _baseURL;

    /** The DOM node representing the link. **/
    private Node           _node;

    /** The target window to which the parent response was directed. **/
    private String         _parentTarget;


    private String getValue( Node node ) {
        return (node == null) ? "" : node.getNodeValue();
    }


}

