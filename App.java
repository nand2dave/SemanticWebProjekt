package SWP.org.aksw.iConnect;


import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.aksw.fox.binding.java.FoxApi;
import org.aksw.fox.binding.java.FoxParameter;
import org.aksw.fox.binding.java.FoxResponse;
import org.aksw.fox.binding.java.IFoxApi;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.apache.jena.riot.Lang;

import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFactory;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;


public class App {

  public static String entityDomain, entityRange, relation, boaPath, filePath;
  
  public static void main(String[] a) throws Exception {
    
    //initializations for RDF output...
    Model model = ModelFactory.createDefaultModel() ;
    model.setNsPrefix("dbp","http://dbpedia.org/resource/");
    boaPath = "";
    
    //get user input
    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    System.out.println("Pfad des BOA-Indexes eingeben: ");
    boaPath = br.readLine();
    boaPath = boaPath.replace("\"", "");//if given path string contains "-characters
    System.out.println("Pfad der Text-Datei eingeben: ");
    filePath = br.readLine();
    filePath = filePath.replace("\"", "");//if given path string contains "-characters
    String s = new Scanner(new File(filePath)).useDelimiter("\\Z").next();
    String saetze[] = s.split("\\."); //Split sentences by "."

    Fox fox;
    SPARQLQuery sparql;
    LuceneQuery lucene;
    
    //for each sententence from input...
    for (int i = 0; i < saetze.length; i++) {
      String input = saetze[i];
      entityDomain = "";
      entityRange = "";
      relation = "";
      
      //Extract Entities from FOX
      String entities[];
      fox = new Fox();
      fox.getEntities(input, 1);
      entities = fox.entities;
          
      //SPARQL Queries to get the Class to which the Entities belong
      String classes[] = new String[5];
      HashMap<String, String> mapClassIndividual = new HashMap<String, String>(); //connect entities with their classes
      sparql = new SPARQLQuery();
      sparql.sparqleQuery(entities, mapClassIndividual);
      classes = sparql.classes;
      
      //query Lucene BOA-Index for NLR with these Classes in Domain and Range
      System.out.println("Lucene-Query...");
      String[] domains = new String[5];
      String[] ranges = new String[5];
      lucene = new LuceneQuery();
      lucene.luceneQuery(classes, 1, input, boaPath);
      domains = lucene.entities;
      lucene.luceneQuery(classes, 2, input, boaPath);
      ranges = lucene.entities;
      

      //find location of entities in input-String (indeces)
      String[] nlr_entities = new String[2];
      String nlr_relation = "";
      fox.getEntities(input, 2);
      nlr_entities = fox.entities;
      
      int x = input.lastIndexOf(nlr_entities[0]);
      int y = input.indexOf(nlr_entities[1]);

      if (x > y){
        int z;
        z = x;
        x = y;
        y = z;
      }

      //get character string between entities in sentence
      nlr_relation = input.substring(x,y).replace(nlr_entities[0], "").replace(nlr_entities[1], "");
      String test[] = new String[1];
      test[0]=nlr_relation;

      //output extracted RDF predicate
      //BOA contains wrong order of domain and range for birthplace and deathplace predicate...
      boolean boaBug = false;
      for (String j : domains){
        if (j == null)
          break;
        if (j.toLowerCase().contains("place")){
          //change domain and range
          String[] z;
          z = ranges.clone();
          ranges = Arrays.copyOfRange(domains, 0, domains.length); 
          domains = z;
          boaBug = true;
        }
        
      }
       
      for (int k = 0; k < domains.length; k++){
        if (domains[k] == null)
          break;
        for (int l = 0; l < ranges.length; l++){
          if (ranges[l] == null)
            break;
          lucene.getTriple(domains[k],ranges[l], nlr_relation, boaPath);
          relation = lucene.relation;// getTriple(domains[k],ranges[l], nlr_relation);
          entityDomain = lucene.entityDomain;
          entityRange = lucene.entityRange;
        }
      }
      
      if (boaBug == true){
        //change domain and range back...
        String[] z;
        z = ranges.clone();
        ranges = Arrays.copyOfRange(domains, 0, domains.length); 
        ranges = z;
      }

      //output triple as RDF
      com.hp.hpl.jena.rdf.model.Resource subject = model.createResource(mapClassIndividual.get(entityDomain));
      com.hp.hpl.jena.rdf.model.Resource object = model.createResource(mapClassIndividual.get(entityRange));
      com.hp.hpl.jena.rdf.model.Property predicate = model.createProperty(relation);
      model.add(subject, predicate, object);
      
    }
    System.out.println("\nEctracted RDF-Triples:\n");
    model.write(System.out, "TURTLE");
  }

}
