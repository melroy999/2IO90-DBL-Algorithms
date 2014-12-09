public class Clause {
	ClauseValue firstValue;
	ClauseValue secondValue;
	
	public Clause(boolean neg1, String value1, boolean neg2, String value2){
		firstValue = new ClauseValue(value1, neg1);
		secondValue = new ClauseValue(value2, neg2);
	}
	
	public Clause(ClauseValue first, ClauseValue second){
		firstValue = first;
		secondValue = second;
	}
	
	public ClauseValue getFirstValue(){
		return firstValue;
	}
	
	public String getFirstStringValue(){
		return firstValue.getValue();
	}
	
	public ClauseValue getSecondValue(){
		return secondValue;
	}
	
	public String getSecondStringValue(){
		return secondValue.getValue();
	}
	
	public String toString(){
		return firstValue + ":" + secondValue;
		//gives a string of the format firstValue:secondValue
	}
}
