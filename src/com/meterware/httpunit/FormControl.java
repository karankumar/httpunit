package com.meterware.httpunit;
/********************************************************************************************************************
* $Id$
*
* Copyright (c) 2000-2001, Russell Gold
*
* Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
* documentation files (the "Software"), to deal in the Software without restriction, including without limitation
* the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
* to permit persons to whom the Software is furnished to do so, subject to the following conditions:
*
* The above copyright notice and this permission notice shall be included in all copies or substantial portions
* of the Software.
*
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
* THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
* AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
* CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
* DEALINGS IN THE SOFTWARE.
*
*******************************************************************************************************************/
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.Hashtable;
import java.util.Vector;


/**
 * Represents a control in an HTML form.
 **/
abstract class FormControl {

    protected Node _node;
    /** The type of a parameter which accepts files for upload. **/
    final static Integer TYPE_FILE = new Integer(4);
    /** The type of a parameter which accepts any text. **/
    final static Integer TYPE_TEXT = new Integer(1);
    /** The type of a parameter which accepts single predefined values. **/
    final static Integer TYPE_SCALAR = new Integer(2);
    /** The type of a parameter which accepts multiple predefined values. **/
    final static Integer TYPE_MULTI_VALUED = new Integer(3);
    /** A constant set to indicate one parameter. **/
    final static Integer ONE_PARAMETER = new Integer(1);


    FormControl( Node node ) {
        _node = node;
    }


    Node getNamedItem( String attributeName ) {
        return _node.getAttributes().getNamedItem( attributeName );
    }


    /**
     * Returns the name of this control. If no name is specified, defaults to the empty string.
     **/
    String getName() {
        return NodeUtils.getNodeAttribute( _node, "name" );
    }


    /**
     * Returns the value of this control. If no value is specified, defaults to the empty string.
     **/
    String getValue() {
        return NodeUtils.getNodeAttribute( _node, "value" );
    }


    /**
     * Returns true if this control is read-only.
     **/
    boolean isReadOnly() {
        return _node.getAttributes().getNamedItem( "readonly" ) != null;
    }


    void updateParameterDefaults( Hashtable defaults ) {
    }


    void updateRequiredValues( Hashtable required ) {
    }


    void updateParameterOptions( Hashtable options ) {
    }


    void updateParameterOptionValues( Hashtable options ) {
    }


    void updateTextParameterCounts( Hashtable parameterCounts ) {
    }


    abstract void updateParameterTypes( Hashtable types );


    protected void addValue( Hashtable valueMap, String name, String value ) {
        String[] currentValues = (String[]) valueMap.get( name );
        if (currentValues == null) {
            valueMap.put( name, new String[] { value } );
        } else {
            valueMap.put( name, withNewValue( currentValues, value ) );
        }
    }


    protected static String getValue( Node node ) {
        return (node == null) ? "" : emptyIfNull( node.getNodeValue() );
    }


    private static String emptyIfNull( String value ) {
        return (value == null) ? "" : value;
    }


    /**
     * Adds a string to an array of strings and returns the result.
     **/
    private String[] withNewValue( String[] group, String value ) {
        String[] result = new String[ group.length+1 ];
        System.arraycopy( group, 0, result, 0, group.length );
        result[ group.length ] = value;
        return result;
    }


    static FormControl newFormParameter( Node node ) {
        if (node.getNodeType() != Node.ELEMENT_NODE) {
            return null;
        } else if (node.getNodeName().equals( "textarea" )) {
            return new TextAreaFormControl( node );
        } else if (node.getNodeName().equals( "select" )) {
            return new SelectionFormControl( node );
        } else if (!node.getNodeName().equals( "input" )) {
            return null;
        } else {
            final String type = NodeUtils.getNodeAttribute( node, "type", "text" );
            if (type.equalsIgnoreCase( "text" ) || type.equalsIgnoreCase( "hidden" ) || type.equalsIgnoreCase( "password" )) {
                return new TextFieldFormControl( node );
            } else if (type.equalsIgnoreCase( "radio" )) {
                return new RadioButtonFormControl( node );
            } else if (type.equalsIgnoreCase( "checkbox" )) {
                return new CheckboxFormControl( node );
            } else if (type.equalsIgnoreCase( "file" )) {
                return new FileSubmitFormControl( node );
            } else {
                return null;
            }
        }
    }

}


abstract
class BooleanFormControl extends FormControl {
    public BooleanFormControl( Node node ) {
        super( node );
    }


    boolean isChecked() {
        return getNamedItem( "checked" ) != null;
    }


    void updateParameterOptionValues( Hashtable options ) {
        if (isChecked() || !isReadOnly()) {
            addValue( options, getName(), getValue() );
        }
    }
}



class RadioButtonFormControl extends BooleanFormControl {

    public RadioButtonFormControl( Node node ) {
        super( node );
    }


    void updateParameterDefaults( Hashtable defaults ) {
        if (isChecked()) defaults.put( getName(), getValue() );
    }


    void updateRequiredValues( Hashtable required ) {
        if (isReadOnly() && isChecked()) {
            required.put( getName(), getValue() );
        }
    }


    void updateParameterTypes( Hashtable types ) {
        types.put( getName(), FormControl.TYPE_SCALAR );
    }
}


class CheckboxFormControl extends BooleanFormControl {
    public CheckboxFormControl( Node node ) {
        super( node );
    }


    void updateParameterDefaults( Hashtable defaults ) {
        if (isChecked()) addValue( defaults, getName(), getValue() );
    }


    void updateRequiredValues( Hashtable required ) {
        if (isReadOnly() && isChecked()) {
            addValue( required, getName(), getValue() );
        }
    }


    void updateParameterTypes( Hashtable types ) {
        types.put( getName(), FormControl.TYPE_MULTI_VALUED );
    }


    String getValue() {
        final String value = super.getValue();
        return value.length() == 0 ? "on" : value;
    }
}


class TextFormControl extends FormControl {


    public TextFormControl( Node node ) {
        super( node );
    }


    void updateParameterDefaults( Hashtable defaults ) {
        defaults.put( getName(), getValue() );
    }


    void updateTextParameterCounts( Hashtable parameterCounts ) {
        Integer oldCount = (Integer) parameterCounts.get( getName() );
        if (oldCount == null) {
            parameterCounts.put( getName(), ONE_PARAMETER );
        } else {
            parameterCounts.put( getName(), new Integer( oldCount.intValue() + 1 ) );
        }
    }


    void updateRequiredValues( Hashtable required ) {
        if (isReadOnly()) required.put( getName(), getValue() );
    }


    void updateParameterTypes( Hashtable types ) {
        types.put( getName(), FormControl.TYPE_TEXT );
    }
}


class TextFieldFormControl extends TextFormControl {
    public TextFieldFormControl( Node node ) {
        super( node );
    }

}


class TextAreaFormControl extends TextFormControl {

    public TextAreaFormControl( Node node ) {
        super( node );

        if (!node.getNodeName().equalsIgnoreCase( "textarea" )) {
            throw new RuntimeException( "Not a textarea element" );
        }
    }


    String getValue() {
        return NodeUtils.asText( _node.getChildNodes() );
    }

}


class FileSubmitFormControl extends FormControl {

    public FileSubmitFormControl( Node node ) {
        super( node );
    }


    void updateParameterTypes( Hashtable types ) {
        types.put( getName(), FormControl.TYPE_FILE );
    }
}


class SelectionFormControl extends FormControl {

    SelectionFormControl( Node node ) {
        super( node );
        if (!node.getNodeName().equalsIgnoreCase( "select" )) {
            throw new RuntimeException( "Not a select element" );
        }
    }


     void updateParameterDefaults( Hashtable defaults ) {
        defaults.put( getName(), getSelected() );
    }


    void updateRequiredParameters( Hashtable required ) {
        if (isDisabled()) required.put( getName(), getSelected() );
    }


    void updateParameterOptions( Hashtable options ) {
        options.put( getName(), getOptions() );
    }


    void updateParameterOptionValues( Hashtable options ) {
        options.put( getName(), getOptionValues() );
    }


    void updateParameterTypes( Hashtable types ) {
        types.put( getName(), isMultiSelect() ? FormControl.TYPE_MULTI_VALUED : FormControl.TYPE_SCALAR );
    }


    String[] getSelected() {
        Vector selected = new Vector();
        NodeList nl = ((Element) _node).getElementsByTagName( "option" );
        for (int i = 0; i < nl.getLength(); i++) {
            if (nl.item(i).getAttributes().getNamedItem( "selected" ) != null) {
                selected.addElement( getOptionValue( nl.item(i) ) );
            }
        }

        if (!isMultiSelect() && selected.size() == 0 && nl.getLength() > 0) {
            selected.addElement( getOptionValue( nl.item(0) ) );
        }

        String[] result = new String[ selected.size() ];
        selected.copyInto( result );
        return result;
    }


    String[] getOptions() {
        Vector options = new Vector();
        NodeList nl = ((Element) _node).getElementsByTagName( "option" );
        for (int i = 0; i < nl.getLength(); i++) {
            options.addElement( getValue( nl.item(i).getFirstChild() ) );
        }
        String[] result = new String[ options.size() ];
        options.copyInto( result );
        return result;
    }


    String[] getOptionValues() {
        Vector options = new Vector();
        NodeList nl = ((Element) _node).getElementsByTagName( "option" );
        for (int i = 0; i < nl.getLength(); i++) {
            options.addElement( getOptionValue( nl.item(i) ) );
        }
        String[] result = new String[ options.size() ];
        options.copyInto( result );
        return result;
    }


    boolean isMultiSelect() {
        return _node.getAttributes().getNamedItem( "multiple" ) != null;
    }


    boolean isDisabled() {
        return _node.getAttributes().getNamedItem( "disabled" ) != null;
    }


    private String getOptionValue( Node optionNode ) {
        NamedNodeMap nnm = optionNode.getAttributes();
        if (nnm.getNamedItem( "value" ) != null) {
            return getValue( nnm.getNamedItem( "value" ) );
        } else {
            return getValue( optionNode.getFirstChild() );
        }
    }


}


