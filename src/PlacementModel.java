/*
 * 2IO90 DBL Algorithms
 * 14-11-2014 Stephan Oostveen
 * This enumeration represents one of the three placement models
 * as specified in the problem description.
 */
public enum PlacementModel {
	TWOPOS("2pos"), FOURPOS("4pos"), ONESLIDER("1slider");

	private String value;

	PlacementModel(String text){
		this.value = text;
	}

	/**
	 * Returns a textual representation of the placement model as 
	 * specified in the problem description i.e lowercase and starting
	 * with a number.
	 * 
	 * @return String representing the placement model
	 */
	public String toString(){
		return value;
	}

	/**
	 * Returns a PlacementModel enumeration associated with a string
	 * @param text the textual representation of the enumeration as specified
	 * in the documentation.
	 * @return One of: TWOPOS, FOURPOS, ONESLIDER.
	 * @throws IllegalArgumentException if the input does not match with an enumeration
	 */
	public static PlacementModel fromString(String text){
		for(PlacementModel p : PlacementModel.values()){
			if(text.equals(p.value)){
				return p;
			}
		}
		throw new IllegalArgumentException("No constant with text "+ text + " found");
	}
}
