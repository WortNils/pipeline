package net.sansa_stack.ml.spark.mining.amieSpark


import org.apache.spark.SparkContext
import org.apache.spark.sql.{DataFrame, SQLContext}

import scala.collection.mutable.{ArrayBuffer, Map}

//import net.sansa_stack.ml.spark.dissect.inference.utils._

import java.io.File

import net.sansa_stack.ml.spark.mining.amieSpark.Rules.RuleContainer
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.functions.udf

object KBObject{
    case class Atom (rdf:RDFTriple)
  class KB() extends Serializable{
    var kbSrc:String = "" 
    
    var kbGraph: RDFGraph = null
    var dfTable:DataFrame = null
    
    var hdfsPath:String = "" 
    
    var subject2predicate2object: Map[String,Map[String,Map[String,Int]]] = Map()
    var predicate2object2subject: Map[String,Map[String,Map[String,Int]]] = Map()
    var object2subject2predicate: Map[String,Map[String,Map[String,Int]]] = Map()
    var predicate2subject2object: Map[String,Map[String,Map[String,Int]]] = Map()
    var object2predicate2subject: Map[String,Map[String,Map[String,Int]]] = Map()
    var subject2object2predicate: Map[String,Map[String,Map[String,Int]]] = Map()
    
    var subjectSize: Map[String,Int] = Map()
	  var objectSize: Map[String,Int] = Map() 
	  var relationSize: Map[String,Int] = Map()
    
	  var subject2subjectOverlap: Map[String, Map[String, Int]] = Map()
    var subject2objectOverlap: Map[String, Map[String, Int]] = Map()  
    var object2objectOverlap: Map[String, Map[String, Int]] = Map() 
    
    /** Identifiers for the overlap maps */
	val SUBJECT2SUBJECT: Int = 0

	val SUBJECT2OBJECT: Int = 2

	val OBJECT2OBJECT: Int = 4
	
	def sethdfsPath (hdfsP:String){
      this.hdfsPath = hdfsP
    }
	
	 def deleteRecursive( path:File){
    var files = path.listFiles()
if(files != null){
for (f <-files){
  if(f.isDirectory()) {
          deleteRecursive(f)
          
          
                     
            f.delete()
            
         }
         else {
           f.delete()
}
}
    path.delete()
 }
 

 
 }
    
 def parquetToDF (path: File, sqlContext:SQLContext): DataFrame={
   var x:DataFrame = null
      
        var tester = path.listFiles()
        if (tester != null){
        for(te <- tester){  
          var part =sqlContext.read.option("mergeSchema", "false").parquet(te.toString)
          if (x == null){
            x = part
          }
          else{
            x = x.union(part)
          }
        }
        }
   return x 
 }
    
      
    
    def getRngSize(rel: String):Double={
      
      return this.predicate2object2subject.get(rel).get.size
    }
    
    def setKbSrc(x:String){
      this.kbSrc = x
    }
    def getKbSrc():String = {
      
      return this.kbSrc
    }
    
    def getKbGraph():RDFGraph={
      return this.kbGraph
      
    }
    
    //TODO: think about Graph representation
    def setKbGraph(x: RDFGraph){
      this.kbGraph = x
      val graph = x.triples.collect
      for (i <- graph){
        add(i)
      }
      buildOverlapTables()
      
    }
    
    def setDFTable(x: DataFrame){
      this.dfTable = x
    }
    
   def add(fst:String, snd:String, thrd: String, toMaps: Map[String,Map[String,Map[String,Int]]]): Boolean ={
     var out = true
     if (toMaps.get(fst).isEmpty){
       toMaps+=(fst-> Map(snd-> Map(thrd -> 0)))
       out = false
     }
     else if (toMaps.get(fst).get.get(snd).isEmpty){
       toMaps.get(fst).get += (snd ->Map(thrd -> 0))
       out = false
     }
     
     else if (toMaps.get(fst).get.get(snd).get.get(thrd).isEmpty){
       toMaps.get(fst).get.get(snd).get += (thrd -> 0)
       out = false
       
     }
      
     
     return out
   }
   
   /**function to fill the Maps subject2predicate2object,... ,sizes and Overlaps
    * 
    * @param tp triple which subject, object and predicate is put in Maps and are counted for the sizes
    * 
    * */
   
   def add (tp:RDFTriple){
     
      val subject = tp.subject
      val relation = tp.predicate
      val o = tp.`object`
      //filling the to to to maps
      if(!(add(subject,relation, o, this.subject2predicate2object))){
       add(relation, o, subject, this.predicate2object2subject)
		   add(o, subject, relation, this.object2subject2predicate)
		   add(relation, subject, o, this.predicate2subject2object)
		   add(o, relation, subject, this.object2predicate2subject)
		   add(subject, o, relation, this.subject2object2predicate)
      }
    
     //filling the sizes
     if (this.subjectSize.get(subject).isEmpty){
       this.subjectSize += (subject -> 1)
     }
     else {
       var subSize:Int = this.subjectSize.remove(subject).get + 1
       
       this.subjectSize += (subject ->subSize)
     }
     
     if (this.relationSize.get(relation).isEmpty){
       this.relationSize += (relation -> 1)
     }
     else {
       var relSize:Int = this.relationSize.remove(relation).get + 1
       
       this.relationSize += (relation ->relSize)
     }
     
      if (this.objectSize.get(o).isEmpty){
       this.objectSize += (o -> 1)
     }
     else {
       var obSize:Int = this.objectSize.remove(o).get + 1
       
       this.objectSize += (o ->obSize)
     }
		
      //filling the overlaps
      
      if (this.subject2subjectOverlap.get(relation).isEmpty){
       subject2subjectOverlap += (relation -> Map())
     }
      
      if (this.subject2objectOverlap.get(relation).isEmpty){
       subject2objectOverlap += (relation -> Map())
     }
      
      if (this.object2objectOverlap.get(relation).isEmpty){
       object2objectOverlap += (relation -> Map())
     }
	
			
		
     
   } 
   
   /**
    * returns number of instantiations of relation
    *
    * @param rel Relation
    * @param number of instantiations 
    * 
    * 
    */
  
   def sizeOfRelation(rel: String): Int={
     if (this.relationSize.get(rel).isEmpty){
       return 0
       
     }
     return this.relationSize.get(rel).get
     
   }
   
   
   /*TO DO 
    * Functionality
    * bulidOverlapTable
    * */
   
   def relationsSize():Int = {
		
     return this.relationSize.size
	}
	
	/**
	 * Returns the number of entities in the database.
	 * @return
	 */
	def entitiesSize(): Int={
	  var x = this.subjectSize.size
	  var y = this.objectSize.size
	
	  return (x+y)
	}
   
   
/**
	 *@author AMIE+ Team
	 */
	
   def buildOverlapTables() {
		for (r1 <- this.relationSize.keys) {
			val subjects1 = predicate2subject2object.get(r1).get.keys.toSet
				
			val objects1 = predicate2object2subject.get(r1).get.keys.toSet
					
					
			for (r2 <-this.relationSize.keys) {
				val subjects2 = predicate2subject2object.get(r2).get.keys.toSet
						
				val objects2 = predicate2object2subject.get(r2).get.keys.toSet
						

				if (!r1.equals(r2)) {
					var ssoverlap:Int = computeOverlap(subjects1, subjects2);
					subject2subjectOverlap.get(r1).get.put(r2, ssoverlap);
					subject2subjectOverlap.get(r2).get.put(r1, ssoverlap);
				} else {
					subject2subjectOverlap.get(r1).get.put(r1, subjects2.size);
				}

				var soverlap1:Int = computeOverlap(subjects1, objects2);
				subject2objectOverlap.get(r1).get.put(r2, soverlap1);
				var soverlap2:Int = computeOverlap(subjects2, objects1);
				subject2objectOverlap.get(r2).get.put(r1, soverlap2);

				if (!r1.equals(r2)) {
					var oooverlap:Int = computeOverlap(objects1, objects2);
					object2objectOverlap.get(r1).get.put(r2, oooverlap);
					object2objectOverlap.get(r2).get.put(r1, oooverlap);
				} else {
					object2objectOverlap.get(r1).get.put(r1, objects2.size);
				}
			}
		}
	}
   
   
   /**
    * @author AMIE+ Team
    * 
    * @param s1 set with subjects or objects
    * @param s2 set with subjects or objects
    * @out overlap of s1 and s2 
    * */
   def computeOverlap(s1: Set[String], s2: Set[String]): Int = {
		var overlap:Int = 0
		for (r <- s1) {
			if (s2.contains(r))
				overlap +=1
		} 
		
		return overlap
	}
   
   
   
   
	// ---------------------------------------------------------------------------
	// Functionality
	// ---------------------------------------------------------------------------

	/**
	 * @author AMIE+ Team
	 * It returns the harmonic functionality of a relation, as defined in the PARIS paper 
	 * https://www.lri.fr/~cr/Publications_Master_2013/Brigitte_Safar/p157_fabianmsuchanek_vldb2011.pdf
	 *
	 **/
	def functionality(relation: String): Double= {
		/*if (relation.equals(EQUALSbs)) {
			return 1.0;*/
	  
	  if (this.predicate2subject2object.get(relation).isEmpty) {return 0.0}
		var a:Double = this.predicate2subject2object.get(relation).get.size
		var b:Double = this.relationSize.get(relation).get
		return ( a / b)
		
	}

	

	/**
	 * @author AMIE+ Team
	 * Returns the harmonic inverse functionality, as defined in the PARIS paper
	 * https://www.lri.fr/~cr/Publications_Master_2013/Brigitte_Safar/p157_fabianmsuchanek_vldb2011.pdf
	 * 
	 */
	def inverseFunctionality(relation: String): Double = {
		/*if (relation.equals(EQUALSbs)) {
			return 1.0;
		} */
	  var a:Double = this.predicate2object2subject.get(relation).get.size 
	    var b:Double =this.relationSize.get(relation).get
			return (a/b )	
		
	}

	


	/**
	 * Determines whether a relation is functional, i.e., its harmonic functionality
	 * is greater than its inverse harmonic functionality.
	 * @param relation
	 * @return
	 * @author AMIE+ Team
	 */
	def isFunctional (relation:String):Boolean ={
		return functionality(relation) >= inverseFunctionality(relation);
	}
	
	/**
	 *  @author AMIE+ Team
	 * It returns the functionality or the inverse functionality of a relation.
	 * @param relation
	 * @param inversed If true, the method returns the inverse functionality, otherwise
	 * it returns the standard functionality.
	 * @return
	 *
	 */
	def functionality(relation: String, inversed:Boolean):Double= {
		if (inversed)
			return inverseFunctionality(relation);
		else 
			return functionality(relation);
	}
	
	/**
	 * @author AMIE+ Team
	 * It returns the functionality or the inverse functionality of a relation.
	 * @param inversed If true, the method returns the functionality of a relation,
	 * otherwise it returns the inverse functionality.
	 * @return
	 * 
	 */
	def inverseFunctionality(relation: String, inversed:Boolean):Double= {
		if (inversed)
			return functionality(relation);
		else 
			return inverseFunctionality(relation);
	}

    
    /** returns ArrayBuffer with successful maps of the '?' 
     * placeholders  in a rule to real facts in the KB 
     * length of maplist is the number of instantiations of a rule
     * 
     * @param triplesCard rule as an ArrayBuffer of RDFTriples, triplesCard(0)
     * 										is the head of the rule
     * @param sc spark context  
     * 
     * */
    
  //----------------------------------------------------------------
	// Statistics
	//----------------------------------------------------------------
	
	def overlap(relation1: String, relation2: String, overlap: Int): Double = {
		overlap match {
		case SUBJECT2SUBJECT => if ((!(subject2subjectOverlap.get(relation1).isEmpty))&&(!(subject2subjectOverlap.get(relation1).get.get(relation2).isEmpty)))
			{return subject2subjectOverlap.get(relation1).get.get(relation2).get}
		else return 0.0
		case SUBJECT2OBJECT =>
			
			 if ((!(subject2objectOverlap.get(relation1).isEmpty))&&(!(subject2objectOverlap.get(relation1).get.get(relation2).isEmpty)))
			{return subject2objectOverlap.get(relation1).get.get(relation2).get}
		else return 0.0
		case OBJECT2OBJECT =>
			
			if ((!(object2objectOverlap.get(relation1).isEmpty))&&(!(object2objectOverlap.get(relation1).get.get(relation2).isEmpty)))
			{return object2objectOverlap.get(relation1).get.get(relation2).get}
		else return 0.0
		
		}
	}
	
		/**
		 * @author AMIE+ team
		 * It returns the number of distinct instance of one of the arguments (columns)
	 	 * of a relation.
	 	 * @param relation
	 	 * @param column. Subject or Object
	 	 * @return
	 	 */
	
	def relationColumnSize(rel: String, elem: String): Int={
	  elem match {
		case "subject" =>
			return predicate2subject2object.get(rel).get.size
		
		case "object" =>
			return predicate2object2subject.get(rel).get.size
		
		}
	  
	}
	
	
	
	//TODO: better than cardinality
	
	def bindingExists(triplesCard: ArrayBuffer[RDFTriple]):Boolean={
	  val k = this.kbGraph
	  if (triplesCard.isEmpty){
	    return true
	  }
	  if (triplesCard.length == 1){
	    var last = triplesCard(0)
	    if (last.subject.startsWith("?")){
	      return (k.find(None,Some(last.predicate),Some(last.`object`)).collect.length) >0
	    }
	    else if (triplesCard(0).`object`.startsWith("?")){
	      return (k.find(Some(last.subject),Some(last.predicate),None).collect.length) >0
	    }
	    else {
	      return false
	    }
	  }
	  
	  
	  var mapList:ArrayBuffer[Map[String,String]]= new ArrayBuffer
	  
	  
	  var min:RDFTriple = triplesCard(0)
	  var minSize = this.relationSize.get(triplesCard(0).predicate).get
	  var index = 0
	  
	  for (i <- 1 to triplesCard.length-1){
	    if (this.relationSize.get(triplesCard(i).predicate).get < minSize){
	      minSize = this.relationSize.get(triplesCard(i).predicate).get
	      min = triplesCard(i)
	      index = i
	      
	    }
	  }
	 
	  
	  var a = min._1
	  var b = min._3
	  
	  var x: Array[RDFTriple] = Array()
	  
	  if (!(a.startsWith("?"))){
	     x = k.find(Some(a),Some(min.predicate),None).collect
	  }
	  else if(!(b.startsWith("?"))){
	     x = k.find(None,Some(min.predicate),Some(b)).collect
	  }
	  else {
	     x = k.find(None,Some(min.predicate),None).collect
	  }
	
	 
	 
	   //x.foreach(println)
	   triplesCard.remove(index)
	   
	  
	  
	   
	   
	  for (i <-x){
	     var temp:ArrayBuffer[RDFTriple] = new ArrayBuffer
	    var exploreFurther = true
	    for (j <- triplesCard){ 
	       
	      var test = true
	      var atestLeft = (this.predicate2subject2object.get(j._2).get.get(i._1).isEmpty)
	      var atestRight = (this.predicate2object2subject.get(j._2).get.get(i._1).isEmpty)
	      
	      var btestRight = (this.predicate2object2subject.get(j._2).get.get(i._3).isEmpty)
	      var btestLeft = (this.predicate2subject2object.get(j._2).get.get(i._3).isEmpty)
	      
	      if(((a.startsWith("?")&&(b.startsWith("?"))))&&
	          (((j._1 == a)&&(j._3 == b)&&(!(atestLeft))&&(!(btestRight)))||
	          ((j._1 == b)&&(j._3 == a)&&(!(atestRight))&&(!(btestLeft))))){
	        test = false
	        
	      }
	      
	      if ((!(j._1.startsWith("?")))&&(!(j._3.startsWith("?")))){
	        test = false
	        
	      }
	      
	      
	      if (test){
	      
	      if ((a.startsWith("?"))&&((j._1 == a)&&(!(atestLeft)))){
	        temp += new RDFTriple(i._1,j._2,j._3)
	        
	      }
	       else if ((a.startsWith("?"))&&((j._3 == a)&&(!(atestRight)))){
	        temp += new RDFTriple(j._1,j._2,i._1)
	        
	      }
	      
	     
	      
	      
	      else if ((b.startsWith("?"))&&((j._3 == b)&&(!(btestRight)))){
	        temp += new RDFTriple(j._1,j._2,i._3)
	       
	      }
	      else if ((b.startsWith("?"))&&((j._1 == b)&&(!(btestLeft)))){
	        temp += new RDFTriple(i._3,j._2,j._3)
	      }
	      else if ((b.startsWith("?"))&&(((j._3 == b)&&(btestRight))||((j._1 == b)&&(btestLeft)))){
	        exploreFurther = false
	      }
	      else if ((a.startsWith("?"))&&(((j._1 == a)&&(atestLeft))||((j._3 == a)&&(atestRight)))){
	        exploreFurther = false
	      }
	      else {
	        
	        temp += j
	        
	      } 
	      
	  
	      }
	      
	      
	      
	    
	    
	    }
	    
	   if (exploreFurther){
	     if (bindingExists(temp)){
	       
	       return true
	     
	     }
	   }
	   
	  }
	  
	 
	 
	   
	  
	  
	  
	  
	  return false
	}
	
	def selectDistinctQueries(x: String, triplesCard: ArrayBuffer[RDFTriple]):ArrayBuffer[ArrayBuffer[RDFTriple]]={
	   var result:ArrayBuffer[ArrayBuffer[RDFTriple]]= new ArrayBuffer
	  val k = this.kbGraph
	  
	  
	  var min:RDFTriple = triplesCard(0)
	  var minSize = this.relationSize.get(triplesCard(0).predicate).get
	  var index = 0
	  
	  var mapList = new  ArrayBuffer[Map[String,String]]
	  
	  for (i <- 1 to triplesCard.length-1){
	    if (this.relationSize.get(triplesCard(i).predicate).get < minSize){
	      minSize = this.relationSize.get(triplesCard(i).predicate).get
	      min = triplesCard(i)
	      index = i
	      
	    }
	  }
	  
	  
	  var a = min._1
	  var b = min._3
	  var t = ""
	  var iRight = ""
	  
	 	  var y: Array[RDFTriple] = Array()
	  
	  if (!(a.startsWith("?"))){
	     y = k.find(Some(a),Some(min.predicate),None).collect
	  }
	  else if(!(b.startsWith("?"))){
	     y = k.find(None,Some(min.predicate),Some(b)).collect
	  }
	  else {
	     y = k.find(None,Some(min.predicate),None).collect
	  }
	  
	  if ((x == a)||(x == b)){
	    
	      for (i <-y){
	        if (a == x){
	           t = a
	           iRight = i._1
	        }
	        else {
	          t = b
	           iRight = i._3
	        }
	        var temp:ArrayBuffer[RDFTriple] = new ArrayBuffer
	        var exploreFurther = true
	        for (j <- triplesCard){ 
	       
	          var test = true
	          var atestLeft = (this.predicate2subject2object.get(j._2).get.get(iRight).isEmpty)
	          var atestRight = (this.predicate2object2subject.get(j._2).get.get(iRight).isEmpty)
	      
	                               
	      
	      
	      
	          if ((!(j._1.startsWith("?")))&&(!(j._3.startsWith("?")))){
	            test = false
	        
	          }
	      
	      
	          if (test){
	      
	            if ((t.startsWith("?"))&&((j._1 == t)&&(!(atestLeft)))){
	              temp += new RDFTriple(iRight,j._2,j._3)
	        
	            }
	            else if ((t.startsWith("?"))&&((j._3 == t)&&(!(atestRight)))){
	              temp += new RDFTriple(j._1,j._2,iRight)
	        
	            }
	      
	     
	      
	            else if ((t.startsWith("?"))&&(((j._1 == t)&&(atestLeft))||((j._3 == t)&&(atestRight)))){
	              exploreFurther = false
	            }
	           
	  
	          }
	      
	      
	      
	    
	    
	        }
	    
	        if (exploreFurther){
	          if (bindingExists(temp)){
	       
	            result += temp
	     
	          }
	        }
	  }
	  
	    
	   
	    
	  }
	  else{
	    triplesCard.remove(index)
	    
	     for (i <-y){
	     var temp:ArrayBuffer[RDFTriple] = new ArrayBuffer
	    var exploreFurther = true
	    for (j <- triplesCard){ 
	       
	      var test = true
	      var atestLeft = (this.predicate2subject2object.get(j._2).get.get(i._1).isEmpty)
	      var atestRight = (this.predicate2object2subject.get(j._2).get.get(i._1).isEmpty)
	      
	      var btestRight = (this.predicate2object2subject.get(j._2).get.get(i._3).isEmpty)
	      var btestLeft = (this.predicate2subject2object.get(j._2).get.get(i._3).isEmpty)
	      
	      if(((a.startsWith("?")&&(b.startsWith("?"))))&&
	          (((j._1 == a)&&(j._3 == b)&&(!(atestLeft))&&(!(btestRight)))||
	          ((j._1 == b)&&(j._3 == a)&&(!(atestRight))&&(!(btestLeft))))){
	        test = false
	        
	      }
	      
	      if ((!(j._1.startsWith("?")))&&(!(j._3.startsWith("?")))){
	        test = false
	        
	      }
	      
	      
	      if (test){
	      
	      if ((a.startsWith("?"))&&((j._1 == a)&&(!(atestLeft)))){
	        temp += new RDFTriple(i._1,j._2,j._3)
	        
	      }
	       else if ((a.startsWith("?"))&&((j._3 == a)&&(!(atestRight)))){
	        temp += new RDFTriple(j._1,j._2,i._1)
	        
	      }
	      
	     
	      
	      
	      else if ((b.startsWith("?"))&&((j._3 == b)&&(!(btestRight)))){
	        temp += new RDFTriple(j._1,j._2,i._3)
	       
	      }
	      else if ((b.startsWith("?"))&&((j._1 == b)&&(!(btestLeft)))){
	        temp += new RDFTriple(i._3,j._2,j._3)
	      }
	      else if ((b.startsWith("?"))&&(((j._3 == b)&&(btestRight))||((j._1 == b)&&(btestLeft)))){
	        exploreFurther = false
	      }
	      else if ((a.startsWith("?"))&&(((j._1 == a)&&(atestLeft))||((j._3 == a)&&(atestRight)))){
	        exploreFurther = false
	      }
	      else {
	        
	        temp += j
	        
	      } 
	      
	  
	      }
	      
	      
	      
	    
	    
	    }
	    
	   if (exploreFurther){
	     if (bindingExists(temp)){
	       
	       result ++= selectDistinctQueries(x, temp)
	     
	     }
	   }
	   
	  
	  }
	  }
	    
	    
	   
	  
	  
	  
	  return result
	}
	
	
	def countProjectionQueries(x: String, minHC: Double, tpAr:ArrayBuffer[RDFTriple], RXY: Tuple2[String,String], sc: SparkContext, sqlContext: SQLContext):Map[String,Int] ={
	  var out:Map[String,Int] = Map()
	  val head = tpAr(0)
	  val k = this.kbGraph
	  if ((x == RXY._1)||(x == RXY._2)){
	    
	    this.relationSize.foreach{
	      rel =>
	        var instAr= k.find(None,Some(rel._1),None)
	        
	        instAr.map{ i =>
	          var temp:ArrayBuffer[RDFTriple] = new ArrayBuffer
	           
	          for (t <- tpAr){
	            if (t.subject == RXY._1) {
	              temp += RDFTriple(i.subject, t.predicate, t.`object`)
	            }
	            else if (t.`object` == RXY._2){
	              temp += RDFTriple(t.subject, t.predicate, i.`object`)
	            }
	            else if ((t.subject == RXY._1) &&(t.`object` == RXY._2)){
	              temp += RDFTriple(i.subject, t.predicate, i.`object`)
	            }
	            else {
	              temp += t
	            }
	            
	          }
	          
	          if (bindingExists(temp)){
	            if (x == RXY._1){
	              if (out.get(i.subject).isEmpty){
	                out += (i.subject -> 1)
	              }
	              else{
	                out.put(i.subject, out.get(i.subject).get +1)
	              }
	              
	            }
	            if (x == RXY._2){
	              if (out.get(i.`object`).isEmpty){
	                out += (i.`object` -> 1)
	              }
	              else{
	                out.put(i.`object`, out.get(i.`object`).get +1)
	              }
	            }
	            
	          }
	         i 
	        }
	    }
	    
	  }
	  else{
	    
	  }
	  
	  
	  return out
	  
	  
	}
	
	
	def varCount(tpAr: ArrayBuffer[RDFTriple]):  ArrayBuffer[Tuple2[String,String]]={
	  
	  var out2: ArrayBuffer[Tuple2[String,String]] = new ArrayBuffer
	  
	  for (i <- tpAr){
	    if (!(out2.contains(Tuple2(i.subject,i.predicate)))){
	      out2 += Tuple2(i.subject,i.predicate)
	    }
	    
	    if (!(out2.contains(Tuple2(i.predicate,i.`object`)))){
	      out2 += Tuple2(i.predicate, i.`object`)
	    }
	    
	 
	    
	  }
	  
	  return out2
	}
		def countProjectionQueriesDF(posit: Int, id: Int, operator:String,minHC: Double, tpAr:ArrayBuffer[RDFTriple], RXY: ArrayBuffer[Tuple2[String,String]], sc: SparkContext, sqlContext: SQLContext): ArrayBuffer[Tuple3[RDFTriple, Int, Long]]=
		{
	  
	  val threshold = minHC * this.relationSize.get(tpAr(0).predicate).get
	  var relations:ArrayBuffer[String] = new ArrayBuffer
	 
	  val variables = varCount(tpAr)
	  
	 var whole: ArrayBuffer[Tuple3[RDFTriple, Int, Long]]= new ArrayBuffer()
	 var counter = 0

	 
	 
	 
	  this.relationSize.foreach{ x =>
	    for (i <-RXY){
	      var go = true
	     var temp = tpAr.clone()
	     temp += RDFTriple(i._1, x._1, i._2)
	      
	      
	      if (operator == "OD"){
	        if (variables.contains(Tuple2(i._1,x._1))){
	          go = false
	          
	        }
	        else if (variables.contains(Tuple2(x._1,i._2))){
	          go = false
	        }
	      }
	      else if (operator == "OC"){
	        if(tpAr.contains(temp.last)){
	          go = false
	        }
	      }
	      
	      if ((this.bindingExists(temp.clone()))&&(go)){
	       
	    //    var part = dfFromIndexingTable(temp, sqlContext)
	       
	     /**      var seq:Seq[String] = Seq((temp.last.toString()  + "  " + id.toString()))
            import sqlContext.implicits._
             var key:DataFrame = seq.toDF("key")
   
  
   part.registerTempTable("lastTable")
   key.registerTempTable("keyTable")

   var partWithKey = sqlContext.sql("SELECT * FROM lastTable JOIN keyTable")
	        
	        */
	        var name = calcName(temp)
	        
	        var generatedCount = sqlContext.read.option("mergeSchema", "false").parquet(hdfsPath+"count/"+name)
	        
	        generatedCount.createOrReplaceTempView("generatedCountTable")
	        
	       
	        var predsKey = ""
	        for (t <- temp){
	          predsKey += t.predicate +"_"
	        }
	        
	        predsKey = predsKey.stripSuffix("_")
	         
	        var filter = sqlContext.sql("SELECT count FROM generatedCountTable WHERE predicates='"+predsKey+"'")
	        
	        
	        var count = filter.rdd.map(x=> x(0).toString.toLong).collect
	       var tresh = this.getRngSize(temp(0)predicate) * minHC
	          
	       if ((!(count.isEmpty))&&(count(0) >= tresh)){
	       whole += Tuple3(temp.last, id, count(0))
	       }
	        
	        
   
	       /** if (whole == null){
	          whole = partWithKey
	        }
	        
	        else {
	          whole = whole.union(partWithKey)
	        }
	        */
	      /*  
	      if (whole == null){
	        whole = part
	      }
	        
	      else{
	        whole = whole.unionAll(part)
	      }
	      * 
	      * 
	      */
	       
	      }
	      
	      
	    }
	    
	   
	    
	  }
	
	  

	  return whole
	  
	}

		
		
		
		
		
		
		
		
		
		
		def cardinalityQueries (id: Int, tpArDF: DataFrame, wholeAr: ArrayBuffer[RDFTriple], sc:SparkContext, sqlContext: SQLContext): DataFrame ={
   val DF = this.dfTable
    
   DF.registerTempTable("table")
   tpArDF.registerTempTable("tpArTable")
    
  
        var w = sqlContext.sql("SELECT sub0 AS sub"+(wholeAr.length-1)+", pr0 AS pr"+(wholeAr.length-1)+", ob0 AS ob"+(wholeAr.length-1)+" FROM table WHERE pr0 = '"+(wholeAr.last).predicate+"'")
        w.registerTempTable("newColumn")
        
   
        
      
        
        
        
        
      var tpMap:Map[String, ArrayBuffer[String]] = Map()
   
   
   var checkMap: Map[Int, Tuple2[String,String]] = Map()
  var lastA = wholeAr.last.subject
  var lastB = wholeAr.last.`object`
   var varAr = ArrayBuffer(lastA, lastB)
   
   var joinAB = Tuple2("", "")
   
   for(i <- 0 to wholeAr.length-1){
     var a = wholeAr(i).subject
     var b = wholeAr(i)._3
   
       
       
     
     
     
     checkMap += (i -> Tuple2(a,b))
     if ((a == lastA)||(a==lastB)){ 
       if(!(tpMap.contains(a))){
         tpMap += ((a) -> ArrayBuffer("sub"+ i))
       
       }
       else {
         var temp = tpMap.get(a).get 
         temp += "sub"+i
         tpMap.put(a, temp)
       }
     }
     
     if ((b == lastA)||(b == lastB)){
       if(!(tpMap.contains(b))){
         tpMap += ((b) -> ArrayBuffer("ob"+i))
         
       }
    
       else{
         var temp = tpMap.get(b).get 
         temp += "ob"+i
         tpMap.put(b, temp)
       
       }
     }
   }
   
   
   var cloneTpAr = wholeAr.clone()
   
   var removedMap:Map[String, ArrayBuffer[Tuple2[Int,String]]] = Map()
   
    
   
   var checkSQLWHERE = ""
   checkMap.foreach{ab =>
     var a = ab._2._1
     var b = ab._2._2
     
     if (varAr.contains(a)){
      
       varAr -= a
       var x = tpMap.get(a).get
       
       
       for (k <- x){
         if(k.takeRight(1).toInt != ab._1){
           if ((joinAB == Tuple2("", ""))&&((k.takeRight(1).toInt == wholeAr.length-1)||(ab._1 == wholeAr.length-1))){
             if ((k.takeRight(1).toInt == wholeAr.length-1)){
               joinAB = ("sub"+ab._1,k)
             }
             if (ab._1 == wholeAr.length-1){
               joinAB = (k, "sub"+ab._1)
             }
           }
           else{
             checkSQLWHERE += "sub"+ab._1+" = "+k+" AND "
           }
         }
         
       }
       
       
     }
     
    
     
     if (varAr.contains(b)){
       
       varAr -= b
       
       var y = tpMap.get(b).get
       var counter = 0
       for (k <- y){
         if(k.takeRight(1).toInt != ab._1){
           if ((joinAB == Tuple2("", ""))&&((k.takeRight(1).toInt == wholeAr.length-1)||(ab._1 == wholeAr.length-1))){
             if ((k.takeRight(1).toInt == wholeAr.length-1)){
               joinAB = ("ob"+ab._1,k)
             }
             if (ab._1 == wholeAr.length-1){
               joinAB = (k, "ob"+ab._1)
             }
           }
           
           else{
             checkSQLWHERE += "ob"+ab._1+" = "+k+" AND "
           }
         }
       }
                
     }
     
   }
   
   checkSQLWHERE = checkSQLWHERE.stripSuffix(" AND ")
   var seq:Seq[String] = Seq((wholeAr.last.toString()  + "  " + id.toString()))
    import sqlContext.implicits._
   var key:DataFrame = seq.toDF("key")
   
   
   //var  v = sqlContext.sql("SELECT * FROM tpArTable JOIN newColumn  ON "+ checkSQLWHERE)
   var v = w.join(tpArDF, tpArDF(joinAB._1) === w(joinAB._2))
   
   v.registerTempTable("t")
   var last = sqlContext.sql("SELECT * FROM t WHERE "+checkSQLWHERE)
   last.registerTempTable("lastTable")
   key.registerTempTable("keyTable")
   var out = sqlContext.sql("SELECT * FROM lastTable JOIN keyTable")
   
   return out
   
   
 }
		
		
		
	 def cardinality (wholeAr: ArrayBuffer[RDFTriple], sc:SparkContext, sqlContext: SQLContext): DataFrame ={
  val DF = this.dfTable
    
   DF.registerTempTable("table")
   var wholeTPARBackup = wholeAr.clone()
    wholeAr.remove(0)
  
    var complete = sqlContext.sql("SELECT * FROM table WHERE pr0 = '"+(wholeAr(0)).predicate+"'")
        
    
    for (i <- 1 to wholeAr.length-1){
      var w = sqlContext.sql("SELECT sub0 AS sub"+i+", pr0 AS pr"+i+", ob0 AS ob"+i+" FROM table WHERE pr0 = '"+(wholeAr(i)).predicate+"'")
        w.registerTempTable("newColumn")
        
        complete.registerTempTable("previousTable")
        complete = sqlContext.sql("SELECT * FROM previousTable JOIN newColumn")
    }
    
  var name = ""
  for (rdftp <-wholeAr){
    name+=rdftp.toString
  }
    
    name = name.replace(" ", "_").replace("?", "_")     
        complete.write.parquet(hdfsPath + "cardinalityBody/" + name)
   
        
       var abString = ""
      
        
        
        
      

  
      var tpMap:Map[String, ArrayBuffer[String]] = Map()
   
   
   var checkMap: Map[Int, Tuple2[String,String]] = Map()
  var lastA = wholeAr.last.subject
  var lastB = wholeAr.last.`object`
   var varAr = new ArrayBuffer[String]
   for(i <- 0 to wholeAr.length-1){
     var a = wholeAr(i).subject
     var b = wholeAr(i)._3
   
     varAr ++= ArrayBuffer(a,b)  
          if ((abString == "") && (a == "?a")){
       abString = "sub"+1
     }
     
     if ((abString == "") && (b == "?a")){
       abString = "ob"+i
     }
     
     
     
     
     checkMap += (i -> Tuple2(a,b))
    
       if(!(tpMap.contains(a))){
         tpMap += ((a) -> ArrayBuffer("sub"+ i))
       
       }
       else {
         var temp = tpMap.get(a).get 
         temp += "sub"+i
         tpMap.put(a, temp)
       }
     
     
     
       if(!(tpMap.contains(b))){
         tpMap += ((b) -> ArrayBuffer("ob"+i))
         
       }
    
       else{
         var temp = tpMap.get(b).get 
         temp += "ob"+i
         tpMap.put(b, temp)
       
       }
     
   }
   
   
   var cloneTpAr = wholeAr.clone()
   
   var removedMap:Map[String, ArrayBuffer[Tuple2[Int,String]]] = Map()
   
    
   
   var checkSQLWHERE = ""
   checkMap.foreach{ab =>
     var a = ab._2._1
     var b = ab._2._2
     
     if (varAr.contains(a)){
      
       varAr -= a
       var x = tpMap.get(a).get
       for (k <- x){
         if(k.takeRight(1).toInt != ab._1){
           checkSQLWHERE += "sub"+ab._1+" = "+k+" AND "
           
         }
         
       }
       
       
     }
     
    
     
     if (varAr.contains(b)){
       
       varAr -= b
       
       var y = tpMap.get(b).get
       
       for (k <- y){
         if(k.takeRight(1).toInt != ab._1){
           checkSQLWHERE += "ob"+ab._1+" = "+k+" AND "
          
         }
       }
                
     }
     
   }
   
   checkSQLWHERE = checkSQLWHERE.stripSuffix(" AND ")
   
   complete.registerTempTable("t")
   var out = sqlContext.sql("SELECT * FROM t WHERE "+checkSQLWHERE)
   return out
   
   
 }
	
	 def cardPlusnegativeExamplesLength(tpAr: ArrayBuffer[RDFTriple], support: Double, sc:SparkContext, sqlContext: SQLContext):Double={
	   this.dfTable.registerTempTable("kbTable")
	  
	 // var card = cardinality(tpAr, sc, sqlContext)
	   var go = false
	   var outCount: Double = 0.0
	   
	    for (i <- 1 to tpAr.length-1){
	      if ((tpAr(i)._1=="?a")||(tpAr(i)._3=="?a")){
	        go = true
	      }
	    }
	   
	   
	   
	   if (go){
	     
	     
	   var tpArString = ""
	   for (tp <-tpAr){
	     tpArString += tp.toString()
	   
	   }
	   
	   tpArString = tpArString.replace(" ", "_").replace("?", "_") 
	   var ex = new File (hdfsPath+"precomputed"+(tpAr.length-2)+"/"+tpArString)
	   var files = ex.listFiles()
	   if (files == null){
	     return 0.0
	   }
	   
	   
	   
	   
	   var card = sqlContext.read.option("mergeSchema", "false").parquet(hdfsPath+"precomputed"+(tpAr.length-2)+"/"+tpArString)
	  
	   card.registerTempTable("cardTable")
	   
	   
	   
	 

	   

	   
	    var h = sqlContext.sql("SELECT DISTINCT sub0 AS sub FROM cardTable")
	   
	  
	   
	   
	   var out:DataFrame = null
	   
	   

	   if (tpAr.length >2){
	   
	   
	   out = tester(h, tpAr, sc, sqlContext)
	   }
	   else {
	     var abString = ""
	      if (tpAr(1)._1 == "?a"){
       abString = "sub0"
       }
     
	      else{
       abString = "ob0"
       }
	     
	     var o = sqlContext.sql("SELECT * FROM kbTable WHERE pr0='"+(tpAr(1)).predicate+"'")
	      o.registerTempTable("twoLengthT")
	      h.registerTempTable("subjects")
	      out = sqlContext.sql("SELECT twoLengthT.sub0, twoLengthT.pr0, twoLengthT.ob0 FROM twoLengthT JOIN subjects ON twoLengthT."+abString+"=subjects.sub")
	  
	      /*
	   if ((tpAr(0).predicate == "directed")&&(tpAr(1).predicate== "produced")&&(tpAr(1).subject== "?a")&&(tpAr(1)._3== "?b")){
	     h.show(800, false)
	     
	     var fjgf = sqlContext.sql("SELECT ")
	   }
	   
	   
	   */
	   
	   }
	  outCount= out.count()
	   }
	   return outCount
	   
	   
	   
	 }
	 
	 
	
	 
	 
	 
	 
	 def tester (subjects: DataFrame, wholeAr: ArrayBuffer[RDFTriple], sc:SparkContext, sqlContext: SQLContext): DataFrame ={
   val DF = this.dfTable
    
   DF.registerTempTable("table")
   var wholeTPARBackup = wholeAr.clone()
    wholeAr.remove(0)
  
    var name = ""
  for (rdftp <-wholeAr){
    name+=rdftp.toString
  }
    
    name = name.replace(" ", "_").replace("?", "_")     
        var complete = sqlContext.read.option("mergeSchema", "false").parquet(hdfsPath + "cardinalityBody/" + name)
    
        
        
   
        
       var abString = ""
      
        
        
        
      

  
      var tpMap:Map[String, ArrayBuffer[String]] = Map()
   
   
   var checkMap: Map[Int, Tuple2[String,String]] = Map()
  var lastA = wholeAr.last.subject
  var lastB = wholeAr.last.`object`
   var varAr = new ArrayBuffer[String]
   for(i <- 0 to wholeAr.length-1){
     var a = wholeAr(i).subject
     var b = wholeAr(i)._3
   
     varAr ++= ArrayBuffer(a,b)  
          if ((abString == "") && (a == "?a")){
       abString = "sub"+1
     }
     
     if ((abString == "") && (b == "?a")){
       abString = "ob"+i
     }
     
     
     
     
     checkMap += (i -> Tuple2(a,b))
    
       if(!(tpMap.contains(a))){
         tpMap += ((a) -> ArrayBuffer("sub"+ i))
       
       }
       else {
         var temp = tpMap.get(a).get 
         temp += "sub"+i
         tpMap.put(a, temp)
       }
     
     
     
       if(!(tpMap.contains(b))){
         tpMap += ((b) -> ArrayBuffer("ob"+i))
         
       }
    
       else{
         var temp = tpMap.get(b).get 
         temp += "ob"+i
         tpMap.put(b, temp)
       
       }
     
   }
   
   
   var cloneTpAr = wholeAr.clone()
   
   var removedMap:Map[String, ArrayBuffer[Tuple2[Int,String]]] = Map()
   
    
   
   var checkSQLWHERE = ""
   checkMap.foreach{ab =>
     var a = ab._2._1
     var b = ab._2._2
     
     if (varAr.contains(a)){
      
       varAr -= a
       var x = tpMap.get(a).get
       for (k <- x){
         if(k.takeRight(1).toInt != ab._1){
           checkSQLWHERE += "sub"+ab._1+" = "+k+" AND "
           
         }
         
       }
       
       
     }
     
    
     
     if (varAr.contains(b)){
       
       varAr -= b
       
       var y = tpMap.get(b).get
       
       for (k <- y){
         if(k.takeRight(1).toInt != ab._1){
           checkSQLWHERE += "ob"+ab._1+" = "+k+" AND "
          
         }
       }
                
     }
     
   }
   
   checkSQLWHERE = checkSQLWHERE.stripSuffix(" AND ")
   
   complete.registerTempTable("t")
   var last = sqlContext.sql("SELECT * FROM t WHERE "+checkSQLWHERE)
   last.registerTempTable("lastTable")
 
   subjects.registerTempTable("keyTable")
   
   var out = sqlContext.sql("SELECT * FROM lastTable JOIN keyTable ON lastTable."+abString+"=keyTable.sub")
   
   return out
   
   
 }
		
	 
	 
	 
	 
	 
	 
	 
	 
	 

		def cardPlusnegativeExamplesLength(triplesCard: ArrayBuffer[RDFTriple], sc:SparkContext):Double={
      
    
      
       val k = this.kbGraph
       
       /**in every RDD is an Array with facts that are corresponding to the relations in the rule,
        * the index of triplesCard correspond with the index of arbuf
        * 
        * */
             var arbuf = new ArrayBuffer[RDD[RDFTriple]]
          
          var mapList = new  ArrayBuffer[Map[String,String]]
          
         
           
           for (i <- triplesCard){
             
            var z = i.predicate
              var x = k.find(None,Some(z),None)
              arbuf += x
             
           }
           
       /**initializing maplist with head of the rule*/
           for(ii <- arbuf(0).collect()){
             mapList += Map(triplesCard(0).subject -> ii._1, triplesCard(0).`object` -> ii._3)
          
           }
           
           
         var temp = mapList.clone()
           
          
           for(tripleCount <- 1 to triplesCard.length-1){
            
           
             val rdd1 = sc.parallelize(mapList.toSeq)
             val rdd2 = arbuf(tripleCount)
             val comb = rdd1.cartesian(rdd2)// cartesian() to get every possible combination
             
             /**the combinations._1 is always the ArrayBuffer with the maps,
              * it represents all correctly mapped placeholders to facts to this point.
              * combinations._2 is the current part of the rule we are mapping
              * 
              *  */
             val combinations = comb.distinct.collect()
         
             mapList = new  ArrayBuffer[Map[String,String]]
          
              
               for (i <- combinations){ 
                 var ltrip =i._2
                 var elem1 = ltrip._1 //subject from combination
                 var elem2 = ltrip._3
                 var trip1 = triplesCard(tripleCount)._1// subject from Rule
                 var trip2 = triplesCard(tripleCount)._3
                 
                      /**checking map for placeholder for the subject*/
                     if (!(i._1.contains(trip1))){ 
                       i._1 += (trip1 -> elem1)
                     }
                  
                   /**checking map for placeholder for the object*/
                     if (!(i._1.contains(trip2))){
                       i._1 += (trip2 -> elem2)
                     }
                   
                    
                      if ((i._1.get(trip1)==Some(elem1)) && (i._1.get(trip2)==Some(elem2))){
                        mapList += i._1 
                      } 
      
               }
          
            
             
           
        
          
           
           
           
     } 
         var rightOnes = sc.parallelize(mapList.toSeq).map(y => y.get(triplesCard(0).subject).get).distinct.collect   
         
         var as = sc.parallelize(temp.toSeq).map{
           x => 
             (x.get(triplesCard(0).subject).get,1)
         
             }.reduceByKey(_ + _).collect
             
             
       
           var out: Double = 0.0
           for (i <- as){
             if(rightOnes.contains(i._1))
             out += (i._2 - 1)
         
           }
           
       
          return ((mapList.length) + out)
     
      
      
      
    }
    
    def addDanglingAtom(c: Int, id: Int, minHC:Double, rule: RuleContainer, sc: SparkContext, sqlContext:SQLContext): ArrayBuffer[Tuple3[RDFTriple, Int, Long]]= 
    {
      val tpAr = rule.getRule()
      var RXY:ArrayBuffer[Tuple2[String,String]] = new ArrayBuffer
     
      val notC = rule.notClosed()
      
      val variables = rule.getVariableList()
      val freshVar = "?" + (rule.getHighestVariable() + 1).toChar.toString
      
      if (notC.isEmpty){
        for(v <- variables){
          RXY ++= ArrayBuffer(Tuple2(v,freshVar), Tuple2(freshVar, v))
          
        }
      }
      else{
        for (nc <- notC.get){
          RXY ++= ArrayBuffer(Tuple2(nc,freshVar), Tuple2(freshVar, nc))
        }
      }
      
      var x = this.countProjectionQueriesDF(c, id, "OD", minHC, tpAr, RXY, sc, sqlContext)
      
      return x
    }
    
    
    
    
    def addClosingAtom(c: Int, id: Int, minHC: Double, rule: RuleContainer, sc: SparkContext, sqlContext:SQLContext):  ArrayBuffer[Tuple3[RDFTriple, Int, Long]] =
    {
      val tpAr = rule.getRule()
      var RXY:ArrayBuffer[Tuple2[String,String]] = new ArrayBuffer
    
      val notC = rule.notClosed()
      
      val variables = rule.getVariableList()
      
      
      if (notC.isEmpty){
      
        for(v <- variables){
         for (w <-variables){
           if (!(v == w)){
             RXY += Tuple2(v,w)
           }
         }
          
       }
      }
      else{
        var notCVars = notC.get
        
        if (notCVars.length == 1){
          for (v <- variables){
            RXY ++= ArrayBuffer(Tuple2(notCVars(0),v), Tuple2(v, notCVars(0)))
          }
        }
        else{
          for (a <- notCVars){
            for (b <- notCVars){
              if (!(a == b)){
                RXY += Tuple2(a,b)
              }
            }
          }
        }
      
           
    }
    var x = this.countProjectionQueriesDF(c, id, "OC", minHC, tpAr, RXY, sc, sqlContext)
    
    return x
    }
    
    
    /*
    
    def addInstantiatedAtom(id: Int, minHC:Double, rule: RuleContainer, sc: SparkContext, sqlContext:SQLContext): DataFrame ={
      val tpAr = rule.getRule()
     
      var RXY:ArrayBuffer[Tuple2[String,String]] = new ArrayBuffer
     
      val notC = rule.notClosed()
      
      val variables = rule.getVariableList()
      val freshVar = "?" + (rule.getHighestVariable() + 1).toChar.toString
      
      if (notC.isEmpty){
        for(v <- variables){
          RXY ++= ArrayBuffer(Tuple2(v,freshVar), Tuple2(freshVar, v))
          
        }
      }
      else{
        for (nc <- notC.get){
          RXY ++= ArrayBuffer(Tuple2(nc,freshVar), Tuple2(freshVar, nc))
        }
      }
       
       var x = this.countProjectionQueriesDFOI(id, minHC, tpAr, RXY, sc, sqlContext)
      return x

    }
    

*/
   def dfFromIndexingTable(wholeTpAr:ArrayBuffer[RDFTriple], sqlContext: SQLContext):DataFrame = {
    
    var name = calcName(wholeTpAr)
    var df: DataFrame = sqlContext.read.option("mergeSchema", "false").parquet(hdfsPath+"precomputed"+(wholeTpAr.length - 1)+"/"+name)
    
    df.createOrReplaceTempView("dfTable")
    
    var checkSQLWHERE:String = ""
    
    for (w <- 0 to wholeTpAr.length -1){
      checkSQLWHERE += "pr"+ w + "='" + wholeTpAr(w)._2 +"'"+ " AND "
    }
    
    checkSQLWHERE = checkSQLWHERE.stripSuffix(" AND ")
    
    var filter = sqlContext.sql("SELECT * FROM dfTable WHERE "+ checkSQLWHERE)
   
    
    return filter
    
    
   }
   
   
   
    
    def calcName(tpAr: ArrayBuffer[RDFTriple]):String={
     var whole = tpAr.clone() 
      
      
      var countMap: Map[String,Int] = Map()
      var numberMap: Map[String, Int] = Map()
      var counter:Int = 1
      for (w <- whole){
        if (countMap.contains(w._1)){
          var temp = countMap.remove(w._1).get +1
          countMap += (w._1 -> temp)
        }
        else {
          countMap += (w._1 -> 1)
        }
         if (countMap.contains(w._3)){
          var temp = countMap.remove(w._3).get +1
          countMap += (w._3 -> temp)
        }
        else {
          countMap += (w._3 -> 1)
        }
         if (!(numberMap.contains(w._1))){
           numberMap += (w._1 -> counter)
           counter+= 1
         }
         if (!(numberMap.contains(w._3))){
           numberMap += (w._3 -> counter)
           counter+= 1
         }
        
      }
      
      var out = ""
      for (wh <- whole){
        var a = ""
        var b = ""
        if (countMap(wh._1)>1){
          a = numberMap(wh._1).toString
        }
        else {
          a="0"
        }
        
        if (countMap(wh._3)>1){
          b = numberMap(wh._3).toString
        }
        else {
          b="0"
        }
        
        out += a + b + "_"
      }
      out = out.stripSuffix("_")
      return out
    } 
 
   def plusNegativeExamplesIndexingTLength(tpAr: ArrayBuffer[RDFTriple], support: Double, sc:SparkContext, sqlContext: SQLContext):Double={
	   this.dfTable.registerTempTable("kbTable")
	  
	 // var card = cardinality(tpAr, sc, sqlContext)
	   var go = false
	   var outCount: Double = 0.0
	   var tpArBody = tpAr.clone()
	   tpArBody.remove(0)
	   
	   var tpArBodyName = calcName(tpArBody)
	   
	   
	   
	   
	    for (i <- 0 to tpArBody.length-1){
	      if ((tpAr(i)._1=="?a")||(tpAr(i)._3=="?a")){
	        go = true
	      }
	    }
	   
	   
	   
	   if (go){
	     
	     
	   
	   var bodyDf = dfFromIndexingTable(tpArBody, sqlContext)
	   bodyDf.createOrReplaceTempView("bodyDfTable")
	   
	   
	   
	   var card = dfFromIndexingTable(tpAr, sqlContext)
	   card.createOrReplaceTempView("cardTable")
	   
	   
	   
	 

	   

	   
	    var subjects = sqlContext.sql("SELECT DISTINCT sub0 AS sub FROM cardTable")
	   
	  
	   
	   
	
	   
	   

	  
	     var abString = ""
	     
	   for (i <- 0 to tpArBody.length -1){
	     if ((abString == "") && (tpAr(i)._1 == "?a")){
         abString = "sub"+i
       }
     
       if ((abString == "") && (tpAr(i)._3 == "?a")){
         abString = "ob"+i
       }
	   }
     
	   
	   //h
	    subjects.registerTempTable("keyTable")
   
    var out = sqlContext.sql("SELECT * FROM bodyDfTable JOIN keyTable ON bodyDfTable."+abString+"=keyTable.sub")
	   //h
     /*
	     var o = sqlContext.sql("SELECT * FROM kbTable WHERE pr0='"+(tpAr(1)).predicate+"'")
	      o.registerTempTable("twoLengthT")
	      subjects.registerTempTable("subjects")
	      out = sqlContext.sql("SELECT twoLengthT.sub0, twoLengthT.pr0, twoLengthT.ob0 FROM twoLengthT JOIN subjects ON twoLengthT."+abString+"=subjects.sub")
	  
	      
	   if ((tpAr(0).predicate == "directed")&&(tpAr(1).predicate== "produced")&&(tpAr(1).subject== "?a")&&(tpAr(1)._3== "?b")){
	     h.show(800, false)
	     
	     var fjgf = sqlContext.sql("SELECT ")
	   }
	   
	   
	   */
	   
	   
	  outCount= out.count()
	   }
	   return outCount
	   
	   
	   
	 }
    
    
    def nameString(tpAr: ArrayBuffer[RDFTriple]): String = {
      	   var tpArString = ""
	   for (tp <-tpAr){
	     tpArString += tp.toString()
	   
	   }
	   
    tpArString = tpArString.replace(" ", "_").replace("?", "_") 
    return tpArString
    }
    
    
    
    
  }
  
  
}