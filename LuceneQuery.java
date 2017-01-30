package SWP.org.aksw.iConnect;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

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

public class LuceneQuery {
  
  String[] entities = new String[5];
  String entityDomain, entityRange, relation;

  
  public void luceneQuery(String entities[], int schalter, String satz, String boaPath) throws IOException, ParseException {
    
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
    //return returnString;
    this.entities = returnString;
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
  
  public void getTriple(String domain, String range, String nlr_relation, String boaPath) throws IOException, ParseException{

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
      if (nlr_relation.toLowerCase().contains(d.get("nlr-no-var").toLowerCase())){ //d.get("nlr-no-var").toLowerCase().contains(nlr_relation.toLowerCase())
        score.add(Double.parseDouble(d.get("SUPPORT_NUMBER_OF_PAIRS_LEARNED_FROM")));
        relations_uri.put(Double.parseDouble(d.get("SUPPORT_NUMBER_OF_PAIRS_LEARNED_FROM")), d.get("uri"));
        entityDomain = d.get("domain");
        entityRange = d.get("range");
      }
    }


    reader.close();

    //return URI with max score
    relation = relations_uri.get(Collections.max(score)).toString();

  }
 

}
