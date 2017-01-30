package SWP.org.aksw.iConnect;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.aksw.fox.binding.java.FoxApi;
import org.aksw.fox.binding.java.FoxParameter;
import org.aksw.fox.binding.java.FoxResponse;
import org.aksw.fox.binding.java.IFoxApi;

public class Fox {
  
  String entities[];
  
  public void getEntities(String s, int schalter) throws MalformedURLException {

    IFoxApi fox = new FoxApi();

    fox.setTask(FoxParameter.TASK.NER);
    fox.setOutputFormat(FoxParameter.OUTPUT.TURTLE);
    fox.setLang(FoxParameter.LANG.EN);
    fox.setInput(s);

    FoxResponse response = fox.send();

    //save URIs of entities in String Arrays
    String[] entities = response.getOutput().split("\n");
    String results[] = new String[5]; //for up to 5 entities
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
    //get start and end indices from the entities in input string
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
    this.entities = results;
  }
  
  public int indexOf(Pattern pattern, String s) {
    Matcher matcher = pattern.matcher(s);
    return matcher.find() ? matcher.start() : -1;
  }


}
