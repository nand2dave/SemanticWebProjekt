package org.aksw.fox.binding.java;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;


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

import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFactory;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

public class Examples {

  public static String entityDomain, entityRange, relation, boaPath;
  
  public static void main(String[] a) throws Exception {
    
    //initialisations for RDF output...
    Model model = ModelFactory.createDefaultModel() ;
    model.setNsPrefix("dbp","http://dbpedia.org/resource/");
    boaPath = "";
    
    //get user input
    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    System.out.println("Pfad des BOA-Indexes eingeben: ");
    boaPath = br.readLine();
    boaPath = boaPath.replace("\"", "");//if given path string contains "-characters
    System.out.println("Bitte Text eingeben und Entertaste druecken: ");
    String s = br.readLine();
    String saetze[] = s.split("\\."); //Split sentences by "."

    //for each sententence from input...
    for (int i = 0; i < saetze.length; i++) {
      String input = saetze[i];
      entityDomain = "";
      entityRange = "";
      relation = "";
      
      //Extract Entities from FOX
      String entities[] = foxEntityRecognition(input, 1);

      //SPARQL Queries to get the Class to which the Entities belong
      String classes[] = new String[5];
      HashMap<String, String> mapClassIndividual = new HashMap<String, String>(); //connect entities with their classes
      classes = sparqleQuery(entities,mapClassIndividual);

      //query Lucene BOA-Index for NLR with these Classes in Domain and Range
      System.out.println("Lucene-Query...");
      String[] domains = new String[5];
      String[] ranges = new String[5];
      domains = luceneQuery(classes,1, input);
      ranges = luceneQuery(classes,2, input);

      //find location of entities in input-String (indeces)
      String[] nlr_entities = new String[2];
      String nlr_relation = "";
      nlr_entities = foxEntityRecognition(input, 2);
     
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
          relation = getRelation(domains[k],ranges[l], nlr_relation);
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
      com.hp.hpl.jena.rdf.model.Resource subject = model.createResource("dbp:"+ mapClassIndividual.get(entityDomain).substring(mapClassIndividual.get(entityDomain).lastIndexOf("/")));//("dbp:"+ entities[0]);
      com.hp.hpl.jena.rdf.model.Resource object = model.createResource("dbp:" + mapClassIndividual.get(entityRange).substring(mapClassIndividual.get(entityDomain).lastIndexOf("/")));
      com.hp.hpl.jena.rdf.model.Property predicate = model.createProperty("dbp:" + relation);
      model.add(subject, predicate, object);
      
    }
    System.out.println("\nEctracted RDF-Triples:\n");
    model.write(System.out, "TURTLE");
  }

  public static String[] foxEntityRecognition(String s, int schalter) throws MalformedURLException {

    IFoxApi fox = new FoxApi();

    fox.setTask(FoxParameter.TASK.NER);
    fox.setOutputFormat(FoxParameter.OUTPUT.TURTLE);
    fox.setLang(FoxParameter.LANG.EN);
    fox.setInput(s);

    FoxResponse response = fox.send();

    //URIs der Entitaeten (aus FOX-response) in String-Array speichern
    String[] entities = response.getOutput().split("\n");
    String results[] = new String[5]; //Fuer 5 Entitaeten
    ArrayList<String> result  = new ArrayList<String>();

    //get entities
    if (schalter == 1){
      int j = 0;
      for (int i = 0; i < entities.length; i++){
        if(entities[i].toLowerCase().contains("dbpedia:") && !entities[i].toLowerCase().contains("prefix")) {
          result.add("http://dbpedia.org/resource/" + entities[i].substring(entities[i].lastIndexOf(":") + 1).replace(" ", "").replace(";", ""));
          j++;
        }
      }

      //convert ArrayList to Array
      results = result.toArray(new String[0]); 

      //display results
      System.out.println("FOX-Input:\n" + response.getInput() + "\n");
      System.out.println("FOX-Output:");
      for(int i=0; i < results.length; i++)
        System.out.println(results[i]);      
    }
    //get start and end indices from the entities
    int beginIndex[] = new int[5]; 
    int endIndex[] = new int[5];

    if (schalter == 2){
      int j = 0, k = 0;
      for (int i = 0; i < entities.length; i++){
        if(entities[i].contains("beginIndex")){
          beginIndex[j] = Integer.parseInt(entities[i].substring(indexOf(Pattern.compile("[0-9]+"), entities[i]), entities[i].lastIndexOf("\"")));
          j++;  
        }
        if(entities[i].contains("endIndex")){
          endIndex[k] = Integer.parseInt(entities[i].substring(indexOf(Pattern.compile("[0-9]+"), entities[i]), entities[i].lastIndexOf("\"")));
          k++;  
        }        
      }
      j = 0;
      for(int i = 0; i < beginIndex.length; i++){
        if (endIndex[i] == 0)
          break;
        results[j] = s.substring(beginIndex[i],endIndex[i]);
        j++;
      }
    }
    return results;
  }
  
  public static int indexOf(Pattern pattern, String s) {
    Matcher matcher = pattern.matcher(s);
    return matcher.find() ? matcher.start() : -1;
  }
  
  public static String[] sparqleQuery(String s[],  HashMap<String, String> hmap) {

    String classes[] = new String[5]; //Rueckgabe-Variable, z.B. http://dbpedia.org/page/Leipzig, kann "Place", wie auch "PopulatedPlace" sein
    int arrayIndex = 0;

    for (int k = 0; k < s.length; k++){
      ParameterizedSparqlString qs = new ParameterizedSparqlString(
          "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
              + "SELECT ?v \n FROM <http://dbpedia.org>"
              + "WHERE {\n"
              + "<" + s[k] + "> "
              + "rdf:type ?v" 
              + "\n}"); 

      System.out.println("\nSPARQL-Query:\n" + qs);

      QueryExecution exec = QueryExecutionFactory.sparqlService("http://dbpedia.org/sparql", qs.asQuery());
      ResultSet results = ResultSetFactory.copyResults(exec.execSelect());

      //csv from ResultSet
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      ResultSetFormatter.outputAsCSV(outputStream, results);

      //convert csv to String-Array
      String csv = new String(outputStream.toByteArray());
      String csvArr[] = csv.split("\n");

      //look for BOA-classes and save in return variable
      for (int i = 0; i < csvArr.length; i++ ){ 
        if (csvArr[i].equals("http://dbpedia.org/ontology/Place\r")){
          System.out.println("?v = " + csvArr[i]);
          classes[arrayIndex] = csvArr[i].toString().replace("\r", "");
          hmap.put(classes[arrayIndex], s[k]);
          arrayIndex++;
        }
        if (csvArr[i].equals("http://dbpedia.org/ontology/Person\r")){
          System.out.println("?v = " + csvArr[i]);
          classes[arrayIndex] = csvArr[i].toString().replace("\r", "");
          hmap.put(classes[arrayIndex], s[k]);
          arrayIndex++;
        }
        if (csvArr[i].equals("http://dbpedia.org/ontology/Company\r")){
          System.out.println("?v = " + csvArr[i]);
          classes[arrayIndex] = csvArr[i].toString().replace("\r", "");
          arrayIndex++;
        }
        if (csvArr[i].equals("http://dbpedia.org/ontology/Actor\r")){
          System.out.println("?v = " + csvArr[i]);
          classes[arrayIndex] = csvArr[i].toString().replace("\r", "");
          arrayIndex++;
        }
        if (csvArr[i].equals("http://dbpedia.org/ontology/Work\r")){
          System.out.println("?v = " + csvArr[i]);
          classes[arrayIndex] = csvArr[i].toString().replace("\r", "");
          arrayIndex++;
        }
        if (csvArr[i].equals("http://dbpedia.org/ontology/PopulatedPlace\r")){
          System.out.println("?v = " + csvArr[i]);
          classes[arrayIndex] = csvArr[i].toString().replace("\r", "");
          arrayIndex++;
        }
        if (csvArr[i].equals("http://dbpedia.org/ontology/SportsTeam\r")){
          System.out.println("?v = " + csvArr[i]);
          classes[arrayIndex] = csvArr[i].toString().replace("\r", "");
          arrayIndex++;
        }
        if (csvArr[i].equals("http://dbpedia.org/ontology/Award\r")){
          System.out.println("?v = " + csvArr[i]);
          classes[arrayIndex] = csvArr[i].toString().replace("\r", "");
          arrayIndex++;
        }
        if (csvArr[i].equals("http://dbpedia.org/ontology/City\r")){
          System.out.println("?v = " + csvArr[i]);
          classes[arrayIndex] = csvArr[i].toString().replace("\r", "");
          arrayIndex++;
        }
        if (csvArr[i].equals("http://dbpedia.org/ontology/Organisation\r")){
          System.out.println("?v = " + csvArr[i]);
          classes[arrayIndex] = csvArr[i].toString().replace("\r", "");
          arrayIndex++;
        }
      }
      System.out.println("");
    }
    return classes;
  }
  
  public static String[] luceneQuery(String entities[], int schalter, String satz) throws IOException, ParseException {
    
    File path = new File(boaPath);
    Directory index = FSDirectory.open(path);
    
    String[] returnString = new String[5];
    Document d;
    TermQuery q;
    int j = 0;
    for (int k=0; k < entities.length; k++){
      
      if (entities[k] == null)
        break;
      
      // Query-String
      String querystr = entities[k]; 

      //Entity in Domain?
      if(schalter == 1){
        q = new TermQuery(new Term("domain", querystr));
        d = queryParser(q, index, satz);
        if (d != null) {
          returnString[j] = d.get("domain");
          j++;
        }
      }
      //Entity in Range?
      if(schalter == 2) {
        q = new TermQuery(new Term("range", querystr));
        d = queryParser(q, index, satz);
        try{
          returnString[j] = d.get("range");
          j++;     } catch(Exception e){}
      }
    }  
    return returnString;
  }
  
  public static Document queryParser(TermQuery q, Directory index, String satz) throws IOException, ParseException, NullPointerException {

    // search
    int hitsPerPage = 10;
    IndexReader reader = DirectoryReader.open(index);
    IndexSearcher searcher = new IndexSearcher(reader);
    TopDocs docs = searcher.search(q, hitsPerPage);
    ScoreDoc[] hits = docs.scoreDocs;

    //display results
    Document d = null;
    for (int i = 0; i < hits.length; ++i) {
      int docId = hits[i].doc;
      d = searcher.doc(docId);
    }

    reader.close();

    return d;
  }
  
  public static String getRelation(String domain, String range, String nlr_relation) throws IOException, ParseException{

    Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_47);
    File path = new File(boaPath);
    Directory index = FSDirectory.open(path);    
    // Query 
    MultiFieldQueryParser queryParser = new MultiFieldQueryParser(Version.LUCENE_47, new String[] {"domain","nlr-no-var","range"}, analyzer);
    Query q = queryParser.parse(Version.LUCENE_47, new String[] {domain,nlr_relation,range}, new String[] {"domain","nlr-no-var","range"}, analyzer);
    int hitsPerPage = 10;
    IndexReader reader = DirectoryReader.open(index);
    IndexSearcher searcher = new IndexSearcher(reader);
    TopDocs docs = searcher.search(q, hitsPerPage);
    ScoreDoc[] hits = docs.scoreDocs;

    List<Double> score = new ArrayList<Double>();//save variable number of scores
    HashMap<Double, String> relations_uri = new HashMap<Double, String>();//key-value pair: score-relation

    //display results
    Document d = null;
    for (int i = 0; i < hits.length; ++i) {
      int docId = hits[i].doc;
      d = searcher.doc(docId);
      if (nlr_relation.toLowerCase().contains(d.get("nlr-no-var").toLowerCase())){ 
        score.add(Double.parseDouble(d.get("SUPPORT_NUMBER_OF_PAIRS_LEARNED_FROM")));
        relations_uri.put(Double.parseDouble(d.get("SUPPORT_NUMBER_OF_PAIRS_LEARNED_FROM")), d.get("uri"));
        entityDomain = d.get("domain");
        entityRange = d.get("range");
      }
    }


    reader.close();

    //return URI with max score
    return relations_uri.get(Collections.max(score)).toString();

  }

}
