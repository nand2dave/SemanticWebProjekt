package SWP.org.aksw.iConnect;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;

import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFactory;
import com.hp.hpl.jena.query.ResultSetFormatter;

public class SPARQLQuery {
  
  String classes[] = new String[5]; //return variable, i.e. http://dbpedia.org/page/Leipzig is a "Place" or "PopulatedPlace"
  
  public void sparqleQuery(String s[],  HashMap<String, String> hmap) {

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
    //return classes;
  }

}
