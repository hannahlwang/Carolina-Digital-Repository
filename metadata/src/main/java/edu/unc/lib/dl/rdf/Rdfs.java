package edu.unc.lib.dl.rdf; 

import static com.hp.hpl.jena.rdf.model.ResourceFactory.createProperty;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
 
/**
 * Vocabulary definitions from rdf-schemas/rdfs.rdf 
 * @author Auto-generated by schemagen on 21 Apr 2016 12:16 
 */
public class Rdfs {
    
    /** The namespace of the vocabulary as a string */
    public static final String NS = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
    
    /** The namespace of the vocabulary as a string
     *  @see #NS */
    public static String getURI() {return NS;}
    
    /** The namespace of the vocabulary as a resource */
    public static final Resource NAMESPACE = createResource( NS );
    
    /** The first item in the subject RDF list. */
    public static final Property first = createProperty( "http://www.w3.org/1999/02/22-rdf-syntax-ns#first" );
    
    /** The object of the subject RDF statement. */
    public static final Property object = createProperty( "http://www.w3.org/1999/02/22-rdf-syntax-ns#object" );
    
    /** The predicate of the subject RDF statement. */
    public static final Property predicate = createProperty( "http://www.w3.org/1999/02/22-rdf-syntax-ns#predicate" );
    
    /** The rest of the subject RDF list after the first item. */
    public static final Property rest = createProperty( "http://www.w3.org/1999/02/22-rdf-syntax-ns#rest" );
    
    /** The subject of the subject RDF statement. */
    public static final Property subject = createProperty( "http://www.w3.org/1999/02/22-rdf-syntax-ns#subject" );
    
    /** The subject is an instance of a class. */
    public static final Property type = createProperty( "http://www.w3.org/1999/02/22-rdf-syntax-ns#type" );
    
    /** Idiomatic property used for structured values. */
    public static final Property value = createProperty( "http://www.w3.org/1999/02/22-rdf-syntax-ns#value" );
    
    /** The datatype of RDF literals storing fragments of HTML content */
    public static final Resource HTML = createResource( "http://www.w3.org/1999/02/22-rdf-syntax-ns#HTML" );
    
    /** The class of plain (i.e. untyped) literal values, as used in RIF and OWL 2 */
    public static final Resource PlainLiteral = createResource( "http://www.w3.org/1999/02/22-rdf-syntax-ns#PlainLiteral" );
    
    /** The datatype of XML literal values. */
    public static final Resource XMLLiteral = createResource( "http://www.w3.org/1999/02/22-rdf-syntax-ns#XMLLiteral" );
    
    /** The datatype of language-tagged string values */
    public static final Resource langString = createResource( "http://www.w3.org/1999/02/22-rdf-syntax-ns#langString" );
    
}
