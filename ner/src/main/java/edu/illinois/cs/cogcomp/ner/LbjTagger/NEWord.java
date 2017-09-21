package edu.illinois.cs.cogcomp.ner.LbjTagger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import edu.illinois.cs.cogcomp.lbjava.nlp.SentenceSplitter;
import edu.illinois.cs.cogcomp.lbjava.nlp.Word;
import edu.illinois.cs.cogcomp.lbjava.nlp.WordSplitter;
import edu.illinois.cs.cogcomp.lbjava.parse.LinkedVector;
import edu.illinois.cs.cogcomp.lbjava.parse.Parser;
import edu.illinois.cs.cogcomp.ner.StringStatisticsUtils.CharacteristicWords;


public class NEWord extends Word
{

	public String[] wikifierfeats;
	public Double[] wordvec;

	public enum LabelToLookAt {PredictionLevel2Tagger,PredictionLevel1Tagger,GoldLabel}

    /** This field is used to store a computed named entity type tag. */
	public String neTypeLevel1;
	public String neTypeLevel2;
	public NamedEntity predictedEntity=null;//if non-null it keeps the named entity the tagger annotated this word with
	public CharacteristicWords predictionConfidencesLevel1Classifier=null;
	public double predictionConfLevel1=0;//how confident are we in the top prediction for level 1 - this is just max over predictionConfidencesLevel1Classifier
	public CharacteristicWords predictionConfidencesLevel2Classifier=null;
	public double predictionConfLevel2=0;//how confident are we in the top prediction for level 2 - this is just max over predictionConfidencesLevel2Classifier 
	public NamedEntity goldEntity=null;//if non-null it keeps the named entity the tagger annotated this word with
	/** This field stores the named entity type tag found in labeled data. */
	public String neLabel=null;


	public double weight = 1.0;


	public String originalSpelling="";
	public String[] parts;


//	private ArrayList<DiscreteFeature> generatedDiscreteFeaturesNonConjunctive=null;
//	public ArrayList<DiscreteFeature> getGeneratedDiscreteFeaturesNonConjunctive() {
//		if (generatedDiscreteFeaturesNonConjunctive == null)
//			generatedDiscreteFeaturesNonConjunctive = new ArrayList<>(0);
//		return generatedDiscreteFeaturesNonConjunctive;
//	}
//
//	private ArrayList<DiscreteFeature> generatedDiscreteFeaturesConjunctive=null;
//	public ArrayList<DiscreteFeature> getGeneratedDiscreteFeaturesConjunctive() {
//		if (generatedDiscreteFeaturesConjunctive == null)
//			generatedDiscreteFeaturesConjunctive = new ArrayList<>(0);
//		return generatedDiscreteFeaturesConjunctive;
//	}
//	private ArrayList<RealFeature> generatedRealFeaturesNonConjunctive=null;
//	public ArrayList<RealFeature> getGeneratedRealFeaturesNonConjunctive() {
//		if (generatedRealFeaturesNonConjunctive == null)
//			generatedRealFeaturesNonConjunctive = new ArrayList<>(0);
//		return generatedRealFeaturesNonConjunctive;
//	}
//	private ArrayList<RealFeature> generatedRealFeaturesConjunctive=null;
//	public ArrayList<RealFeature> getGeneratedRealFeaturesConjunctive() {
//		if (generatedRealFeaturesConjunctive == null)
//			generatedRealFeaturesConjunctive = new ArrayList<>(0);
//		return generatedRealFeaturesConjunctive;
//	}

	public String normalizedMostLinkableExpression=null;
	public ArrayList<String> gazetteers; 

	private HashMap<String,Integer> nonLocalFeatures = null;
	public HashMap<String, Integer> getNonLocalFeatures() {
		if (nonLocalFeatures == null) 
			nonLocalFeatures = new HashMap<>(0);
		return nonLocalFeatures;
	}

	private String[] nonLocFeatArray=null;
	public NEWord nextIgnoreSentenceBoundary=null;
	public NEWord previousIgnoreSentenceBoundary=null;
	
	public ArrayList<RealFeature> level1AggregationFeatures=null;
	public ArrayList<RealFeature> getLevel1AggregationFeatures() {
		if (level1AggregationFeatures == null) 
			level1AggregationFeatures = new ArrayList<>(0);
		return level1AggregationFeatures;
	}
	public ArrayList<RealFeature>  resetLevel1AggregationFeatures() {
		level1AggregationFeatures = new ArrayList<>(0);
		return level1AggregationFeatures;
	}

	/*
	 * This stuff was added for form normalization purposes.
	 */
	
	public String form=null;//override the Word.form field!
	public String originalForm=null;//what was the form that we read from the file
	public String normalizedForm=null;//after the title normalization stage
	public boolean isCaseNormalized=false;

//	public double maxStartLinkabilityScore=0;
//	public double maxEndLinkabilityScore=0;
//	public double maxStartLinkabilityScorePrevalent=0;
//	public double maxEndLinkabilityScorePrevalent=0;
//	public double maxStartLinkabilityScoreVeryPrevalent=0;
//	public double maxEndLinkabilityScoreVeryPrevalent=0;
//	public double maxLinkability=0;
//	public double maxLinkabilityPrevalent=0;
//	public double maxLinkabilityVeryPrevalent=0;
//	public double maxStartLinkabilityScore_IC=0;
//	public double maxEndLinkabilityScore_IC=0;
//	public double maxStartLinkabilityScorePrevalent_IC=0;
//	public double maxEndLinkabilityScorePrevalent_IC=0;
//	public double maxStartLinkabilityScoreVeryPrevalent_IC=0;
//	public double maxEndLinkabilityScoreVeryPrevalent_IC=0;
//	public double maxLinkability_IC=0;
//	public double maxLinkabilityPrevalent_IC=0;
//	public double maxLinkabilityVeryPrevalent_IC=0;


	/**
	 * An <code>NEWord</code> can be constructed from a <code>Word</code>
	 * object representing the same word, an <code>NEWord</code> representing
	 * the previous word in the sentence, and the named entity type label found
	 * in the data.
	 *
	 * @param w    Represents the same word as the <code>NEWord</code> being
	 *             constructed.
	 * @param p    The previous word in the sentence.
	 * @param type The named entity type label for this word from the data.
	 **/
	public NEWord(Word w, NEWord p, String type)
	{
		super(w.form, w.partOfSpeech, w.lemma, w.wordSense, p, w.start, w.end);
		form=w.form;
		originalForm=w.form;
		neLabel = type;
		neTypeLevel1=null;
	}

	public NEWord(Word w, NEWord p, String type, int start, int end)
	{
		super(w.form, w.partOfSpeech, w.lemma, w.wordSense, p, start, end);
		form=w.form;
		originalForm=w.form;
		neLabel = type;
		neTypeLevel1=null;
	}

	public double getWeight(){
		return this.weight;
	}

	public void setWeight(double w){
		this.weight = w;
	}

	/**
	 * Produces a simple <code>String</code> representation of this word in
	 * which the <code>neLabel</code> field appears followed by the word's part
	 * of speech and finally the form (i.e., spelling) of the word all
	 * surrounded by parentheses.
	 **/
	public String toString()
	{
		return "(" + neLabel + " " + partOfSpeech + " " + form + ")";
	}

	/**
	 * Added only from special types of files...
	 * @param feats
     */
	public void addWikifierFeatures(String[] feats){
		this.wikifierfeats = feats;
	}

	public String[] getAllNonlocalFeatures(){
		if(nonLocFeatArray==null){
			Vector<String> v=new Vector<>();
			for(Iterator<String> i=getNonLocalFeatures().keySet().iterator();i.hasNext();v.addElement(i.next()));
			nonLocFeatArray=new String[v.size()];
			for(int i=0;i<v.size();i++)
				nonLocFeatArray[i]=v.elementAt(i);
		}
		return nonLocFeatArray;
	}

	public int getNonLocFeatCount(String nonLocFeat)
	{
		return this.getNonLocalFeatures().get(nonLocFeat);
	}

	/**
	 * Convenience method
	 * @param sentence to add word to
	 * @param token word
	 * @param tag nelabel for word
     */
	public static void addTokenToSentence(LinkedVector sentence, String token, String tag) {
		NEWord word=new NEWord(new Word(token),null,tag);
		addTokenToSentence(sentence, word);
	}

	/**
	 * We do this in order to allow the word.partOfSpeech to pass through
	 * @param sentence
	 * @param word
     */
	public static void addTokenToSentence(LinkedVector sentence, NEWord word){

		Vector<NEWord> v=NEWord.splitWord(word);
		if(ParametersForLbjCode.currentParameters.tokenizationScheme.equals(ParametersForLbjCode.TokenizationScheme.DualTokenizationScheme)){
			sentence.add(word);
			word.parts=new String[v.size()];
			for(int j=0;j<v.size();j++)
				word.parts[j]=v.elementAt(j).form;
		}
		else{
			if(ParametersForLbjCode.currentParameters.tokenizationScheme.equals(ParametersForLbjCode.TokenizationScheme.LbjTokenizationScheme)){
				for(int j=0;j<v.size();j++)
					sentence.add(v.elementAt(j));
			}
			else{
				System.err.println("Fatal error in BracketFileManager.readAndAnnotate - unrecognized tokenization scheme: "+ParametersForLbjCode.currentParameters.tokenizationScheme);
				System.exit(0);
			}					  
		}		
	}
	
	
	/*
	 * Used for some tokenization schemes. 
	 */
	private static Vector<NEWord> splitWord(NEWord word){
		String[] sentence={word.form+" "};
		Parser parser = new WordSplitter(new SentenceSplitter(sentence));
		LinkedVector words=(LinkedVector) parser.next();
		Vector<NEWord> res=new Vector<>();
		String label=word.neLabel;
		for(int i=0;i<words.size();i++){
			if(label.contains("B-") &&i>0)
				label="I-"+label.substring(2);
			NEWord w=new NEWord(new Word(((Word)words.get(i)).form),null,label);
			w.originalSpelling=word.form;
			res.addElement(w);
		}
		return res;
	}

	public String getPrediction(LabelToLookAt labelType) {
		if (labelType==LabelToLookAt.GoldLabel) 
			return this.neLabel;
		if (labelType==LabelToLookAt.PredictionLevel1Tagger) 
			return this.neTypeLevel1;
		if (labelType==LabelToLookAt.PredictionLevel2Tagger) 
			return this.neTypeLevel2;
		return null;
	}
	

	public void setPrediction(String label,LabelToLookAt labelType) {
		if (labelType==LabelToLookAt.GoldLabel) 
			this.neLabel=label;
		if (labelType==LabelToLookAt.PredictionLevel1Tagger) 
			this.neTypeLevel1=label;
		if (labelType==LabelToLookAt.PredictionLevel2Tagger) 
			this.neTypeLevel2=label;
	}
	
	public static class DiscreteFeature{
		public String featureValue;
		public String featureGroupName;
		public boolean useWithinTokenWindow=false; //generate this feature for a window of +-2 tokens
	}
	
	public static class RealFeature{
		public double featureValue;
		public String featureGroupName;
		public boolean useWithinTokenWindow=false; //generate this feature for a window of +-2 tokens
		public RealFeature(double _featureValue,String _featureGroupName){
			this.featureValue=_featureValue;
			this.featureGroupName=_featureGroupName;
		}
	}
}

